#!/usr/bin/env python3
import csv
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import concurrent.futures
import re
import urllib3
import argparse
import sys
import os
import signal

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# 全域中止標記
stop_flag = False

def signal_handler(sig, frame):
    global stop_flag
    if not stop_flag:
        print("\n[收到中斷訊號 (Ctrl+C)] 正在準備安全中止，等待當前處理完成...")
        stop_flag = True
    else:
        # 第二次按 Ctrl+C，使用 os._exit 強制結束，避免 thread join 卡死
        print("\n[強制中斷] 放棄儲存，強制退出。")
        os._exit(1)

signal.signal(signal.SIGINT, signal_handler)

# 關鍵字
TEXT_KEYWORDS = ["最新消息", "校務公告", "學校公告", "公告訊息", "公告事項", "行政公告", "NEWS", "公告", "更多", "MORE", "+", "ALL"]
URL_KEYWORDS = ['site_news', '/p/4', 'news', 'bulletin', 'announcement', 'board', 'main2.php']

def is_valid_url(url):
    try:
        result = urlparse(url)
        return all([result.scheme, result.netloc])
    except ValueError:
        return False

def check_is_announcement_page(html, url):
    url_lower = url.lower()
    soup = BeautifulSoup(html, 'html.parser')
    text = soup.get_text(separator=' ')
    
    # ── 絕對排除特徵 ──
    links = soup.find_all('a', href=True)
    img_link_count = sum(1 for a in links if a['href'].lower().endswith(('.jpg', '.jpeg', '.png', '.gif')))
    total_links = len(links)
    if total_links > 10 and (img_link_count / total_links) > 0.5:
        return False

    text_nospace = text.replace(" ", "")
    if "分機表" in text_nospace or "教職員名冊" in text_nospace or "聯絡方式" in text_nospace:
        if "校長室" in text_nospace or "教務處" in text_nospace:
            return False 
            
    tables = soup.find_all('table')
    for table in tables:
        table_text = table.get_text(separator='', strip=True)
        if ("分機" in table_text or "電話" in table_text) and ("職稱" in table_text or "姓名" in table_text or "處室" in table_text):
            return False

        merged_cells = table.find_all(['td', 'th'], attrs={'colspan': True}) + table.find_all(['td', 'th'], attrs={'rowspan': True})
        merged_cells = [cell for cell in merged_cells if int(cell.get('colspan', 1)) > 1 or int(cell.get('rowspan', 1)) > 1]
        rowspan_count = sum(1 for cell in table.find_all(['td', 'th'], attrs={'rowspan': True}) if int(cell.get('rowspan', 1)) > 1)
        
        if len(merged_cells) >= 10 or rowspan_count >= 3: 
            return False

    # ── 強烈特徵 ──
    if 'site_news/main' in url_lower or re.search(r'/p/4\d{2}-\d+-\d+\.php', url_lower):
        return True

    # ── 評分機制 ──
    score = 0
    headers1 = ["標題", "日期", "發布單位", "點閱", "類別", "點擊", "發布者"]
    if sum(1 for h in headers1 if h in text) >= 2: score += 2
    
    headers2 = ["標題", "日期", "發布", "點閱", "類別", "點擊"]
    if sum(1 for h in headers2 if h in text) >= 2: score += 2
    
    units = ["教務處", "學務處", "總務處", "輔導室", "圖書館", "人事室", "教學組", "註冊組", "訓育組", "資訊組", "設備組", "人事", "教學", "教務", "學務", "總務"]
    if sum(1 for u in units if u in text) >= 2: score += 2
    
    date_pattern = re.compile(r'((?:11[0-9]|202[0-9])[/\-\.][0-1][0-9][/\-\.][0-3][0-9])')
    dates_found = date_pattern.findall(text)
    if len(set(dates_found)) >= 3: score += 2
        
    has_large_table = False
    for table in tables:
        if len(table.find_all('tr')) >= 5:
            has_large_table = True
            break
    if has_large_table: score += 1
            
    if soup.find(class_=re.compile(r'news|announcement|list|cg-list', re.I)):
        score += 1

    return score >= 3

def process_school(school_name, base_url):
    global stop_flag
    if stop_flag:
        return school_name, base_url, None

    if base_url.startswith('http://'):
        base_url = base_url.replace('http://', 'https://')

    if not base_url or not is_valid_url(base_url):
        return school_name, base_url, None

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    try:
        res = requests.get(base_url, headers=headers, timeout=10, verify=False)
        actual_url = res.url 
        res.encoding = res.apparent_encoding 
        soup = BeautifulSoup(res.text, 'html.parser')
        
        meta_refresh = soup.find('meta', attrs={'http-equiv': re.compile(r'^refresh$', re.I)})
        if meta_refresh and meta_refresh.get('content'):
            content = meta_refresh['content']
            match = re.search(r'url=([^;]+)', content, re.I)
            if match:
                refresh_url = match.group(1).strip(' \'"')
                actual_url = urljoin(actual_url, refresh_url)
                try:
                    res = requests.get(actual_url, headers=headers, timeout=10, verify=False)
                    res.encoding = res.apparent_encoding
                    soup = BeautifulSoup(res.text, 'html.parser')
                except:
                    pass

        # 紀錄首頁本身是否符合公告頁特徵，作為最後的「保險」
        homepage_is_announcement = check_is_announcement_page(res.text, actual_url)
        
        candidates = []
        base_netloc = urlparse(actual_url).netloc
        
        for a in soup.find_all('a', href=True):
            href = a['href']
            href_lower = href.lower()
            
            if href.startswith(('javascript:', 'mailto:', '#', 'tel:')): continue
            if href_lower.endswith(('.jpg', '.jpeg', '.png', '.gif', '.pdf', '.doc', '.docx', '.zip', '.rar')): continue
            if 'login' in href_lower or 'signin' in href_lower: continue
            
            text = a.get_text(strip=True).upper()
            title = a.get('title', '').upper()
            alt_text = ""
            img = a.find('img')
            if img and img.get('alt'):
                alt_text = img.get('alt').upper()

            combined_text = f"{text} {title} {alt_text}"
            
            kw_match = any(kw in combined_text for kw in TEXT_KEYWORDS)
            url_match = any(kw in href_lower for kw in URL_KEYWORDS)
            
            if kw_match or url_match:
                full_url = urljoin(actual_url, href)
                candidate_netloc = urlparse(full_url).netloc
                base_domain = base_netloc.replace('www.', '')
                
                if base_domain in candidate_netloc and is_valid_url(full_url) and full_url not in candidates:
                    if 'site_news' in full_url.lower() or '/p/4' in full_url.lower():
                        candidates.insert(0, full_url)
                    else:
                        candidates.append(full_url)
                    
        for candidate_url in candidates[:15]:
            if stop_flag:
                break
            try:
                c_res = requests.get(candidate_url, headers=headers, timeout=5, verify=False)
                c_res.encoding = c_res.apparent_encoding
                if check_is_announcement_page(c_res.text, candidate_url):
                    return school_name, base_url, candidate_url
            except:
                continue 
                
        # 如果所有的候選網址都失敗了，但首頁本身符合公告特徵，就將首頁當作保險備案
        if homepage_is_announcement:
            return school_name, base_url, actual_url

        return school_name, base_url, None # 找不到或失敗都回傳 None，這樣就不會寫入備註

    except Exception as e:
        return school_name, base_url, None

def main():
    parser = argparse.ArgumentParser(description="從學校清單 CSV 檔爬取學校公告網址。支援 Ctrl+C 中斷並儲存。")
    parser.add_argument("input_file", help="輸入的 CSV 檔案路徑 (例如: docs/high.csv)")
    parser.add_argument("-o", "--output", default="school_announcements.csv", help="輸出的 CSV 檔案路徑")
    parser.add_argument("-w", "--workers", type=int, default=5, help="同時執行的執行緒數量")
    args = parser.parse_args()

    # 1. 如果 output_file 存在，我們就讀取它作為基礎，實現斷點續傳。否則讀取 input_file。
    load_file = args.output if os.path.exists(args.output) else args.input_file
    
    header = []
    all_rows = []
    
    try:
        with open(load_file, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            try:
                header = next(reader)
            except StopIteration:
                pass
            for row in reader:
                all_rows.append(row)
    except FileNotFoundError:
        print(f"錯誤: 找不到檔案 {load_file}")
        sys.exit(1)
        
    name_idx, url_idx, note_idx = -1, -1, -1
    for i, col in enumerate(header):
        if "學校名稱" in col: name_idx = i
        elif ("網址" in col or "首頁" in col) and "公告" not in col: url_idx = i
        elif "備註" in col: note_idx = i
            
    if name_idx == -1 or url_idx == -1 or note_idx == -1:
        # Fallback to high.csv default
        name_idx, url_idx, note_idx = 2, 7, 8

    # 確保每一行都有備註欄
    for row in all_rows:
        while len(row) <= note_idx:
            row.append("")

    schools_to_process = []
    for row_index, row in enumerate(all_rows):
        school_name = row[name_idx].strip()
        base_url = row[url_idx].strip()
        note_content = row[note_idx].strip()
        
        # 如果備註欄已經有 http 開頭的網址，表示已經成功抓過，跳過
        if note_content.startswith('http'):
            continue
            
        if school_name:
            schools_to_process.append((row_index, school_name, base_url))
                
    print(f"共 {len(all_rows)} 所學校，本次需要 (重新) 分析 {len(schools_to_process)} 所學校。")
    print("提示：您可以隨時按下 Ctrl+C 中斷執行，系統會儲存當前進度。")
    
    if not schools_to_process:
        sys.exit(0)
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {executor.submit(process_school, school_name, base_url): row_idx for row_idx, school_name, base_url in schools_to_process}
        
        count = 0
        try:
            for future in concurrent.futures.as_completed(futures):
                if stop_flag:
                    # Cancel all remaining futures
                    for f in futures:
                        f.cancel()
                    break
                    
                row_idx = futures[future]
                name, base, news_url = future.result()
                
                if news_url:
                    all_rows[row_idx][note_idx] = news_url
                    
                count += 1
                if count % 5 == 0 or count == len(schools_to_process):
                    print(f"進度: {count}/{len(schools_to_process)} - 剛處理完: {name} -> {news_url or '未找到'}")
                        
        except KeyboardInterrupt:
            # The signal handler already catches this, but just in case
            pass

    print("\n正在寫入檔案...")
    with open(args.output, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(header)
        writer.writerows(all_rows)
        
    print(f"進度已儲存至 {args.output}")

if __name__ == "__main__":
    main()
