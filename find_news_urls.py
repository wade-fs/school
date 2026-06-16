#!/usr/bin/env python3
import csv
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse, parse_qs
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
        print("\n[強制中斷] 放棄儲存，強制退出。")
        os._exit(1)

signal.signal(signal.SIGINT, signal_handler)

# 關鍵字
TEXT_KEYWORDS = ["最新消息", "校務公告", "學校公告", "公告訊息", "公告事項", "行政公告", "NEWS", "公告", "更多", "MORE", "+", "ALL", "彙整", "全部", "更多公告", "MAIN_NEWS"]
URL_KEYWORDS = ['site_news', '/p/4', 'news', 'bulletin', 'announcement', 'board', 'main2.php', 'main_news']

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
        return -1 

    text_nospace = text.replace(" ", "")
    if "分機" in text_nospace:
        if "校長室" in text_nospace or "教務處" in text_nospace or "總務處" in text_nospace:
            rowspan_count = len(soup.find_all(['td', 'th'], attrs={'rowspan': True}))
            if rowspan_count >= 5: return -1 
            
    # 檢查表格內容是否明顯是通訊錄
    tables = soup.find_all('table')
    for table in tables:
        table_text = table.get_text(separator='', strip=True)
        if ("分機" in table_text or "電話" in table_text) and ("職稱" in table_text or "姓名" in table_text):
            if "處室" in table_text or "教務處" in table_text:
                return -1
        merged_cells = [cell for cell in (table.find_all(['td', 'th'], attrs={'colspan': True}) + table.find_all(['td', 'th'], attrs={'rowspan': True})) if int(cell.get('colspan', 1)) > 1 or int(cell.get('rowspan', 1)) > 1]
        rowspan_count = sum(1 for cell in table.find_all(['td', 'th'], attrs={'rowspan': True}) if int(cell.get('rowspan', 1)) > 1)
        if len(merged_cells) >= 10 or rowspan_count >= 3: return -1

    score = 0

    # ── 強烈特徵 ──
    if 'site_news/main' in url_lower:
        score += 15
    elif '/p/428-' in url_lower:
        score += 20
    elif re.search(r'/p/4\d{2}-\d+-\d+\.php', url_lower):
        score += 5
    
    # ── 懲罰特徵 (單篇文章/非清單頁) ──
    if re.search(r'/p/[1-3]\d{2}-', url_lower) or 'aid=' in url_lower or 'cid=' in url_lower:
        score -= 30

    # ── 評分機制 ──
    # 評分一：核心關鍵字
    score += sum(15 for kw in ["最新消息", "校務公告", "學校公告", "公告訊息", "公告事項", "行政公告", "NEWS", "公告", "彙整", "全部公告", "公告模組", "公佈欄"] if kw in text)
    
    # 評分二：是否包含常見的處室單位名稱
    units = ["教務", "學務", "總務", "輔導", "圖書", "人事", "教學", "註冊", "訓育", "資訊", "設備", "校長室", "秘書", "特教"]
    unit_count = sum(1 for u in units if u in text)
    score += unit_count * 2 # 每個單位加二分
    
    # 評分三：找尋多個日期格式
    date_pattern = re.compile(r'((?:11[0-9]|202[0-9])[/\-\.][0-1][0-9][/\-\.][0-3][0-9])')
    dates_count = len(set(date_pattern.findall(text)))
    score += dates_count * 2
        
    # 評分四：檢查條列式結構
    list_items = len(soup.find_all('tr')) + (len(soup.find_all('li')) // 2) + (len(soup.find_all('div', class_=re.compile(r'item|row|list', re.I))) // 5)
    if list_items >= 5: score += 10
            
    if soup.find(class_=re.compile(r'news|announcement|list|cg-list', re.I)):
        score += 10

    # ── 分頁特徵 ──
    pagination_keywords = ["第一頁", "最後一頁", "上一頁", "下一頁", "頁次：", "Next", "Previous", "Last Page", "First Page", "總共"]
    pagi_match = sum(1 for p in pagination_keywords if p.upper() in text.upper())
    if pagi_match >= 1 or re.search(r'\[\s*\d+\s*\]', text) or re.search(r'第\s*\d+\s*頁', text) or re.search(r'共\s*\d+\s*頁', text):
        score += 30 

    return score

def guess_cms_urls(base_url, soup):
    cms_urls = []
    for a in soup.find_all('a', href=True):
        if 'uid=' in a['href']:
            uid_match = re.search(r'uid=([a-zA-Z0-9_]+)', a['href'])
            if uid_match:
                uid = uid_match.group(1)
                cms_urls.append(urljoin(base_url, f"/ischool/widget/site_news/main2.php?uid={uid}&maximize=1&allbtn=0"))
    return cms_urls

def process_school(school_name, base_url):
    global stop_flag
    if stop_flag: return school_name, base_url, None
    if base_url.startswith('http://'): base_url = base_url.replace('http://', 'https://')
    if not base_url or not is_valid_url(base_url): return school_name, base_url, None

    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"}

    try:
        # 第一階段：請求並處理跳轉
        res = requests.get(base_url, headers=headers, timeout=10, verify=False)
        actual_url = res.url 
        res.encoding = res.apparent_encoding 
        soup = BeautifulSoup(res.text, 'html.parser')
        
        meta_refresh = soup.find('meta', attrs={'http-equiv': re.compile(r'^refresh$', re.I)})
        if meta_refresh and meta_refresh.get('content'):
            match = re.search(r'url=([^;]+)', meta_refresh['content'], re.I)
            if match:
                actual_url = urljoin(actual_url, match.group(1).strip(' \'"'))
                try:
                    res = requests.get(actual_url, headers=headers, timeout=10, verify=False)
                    res.encoding = res.apparent_encoding
                    soup = BeautifulSoup(res.text, 'html.parser')
                except: pass

        # 新增機制：如果首頁內容過於簡單 (可能是進入頁)，尋找可能為「真正首頁」的連結
        if len(soup.get_text().strip()) < 2000:
            for a in soup.find_all('a', href=True):
                href = a['href'].lower()
                text = a.get_text(strip=True).upper()
                if any(kw in href or kw in text for kw in ["APP/HOME", "HOME.PHP", "INDEX.PHP", "進入網站", "進入首頁", "回首頁"]):
                    new_actual_url = urljoin(actual_url, a['href'])
                    try:
                        res = requests.get(new_actual_url, headers=headers, timeout=10, verify=False)
                        res.encoding = res.apparent_encoding
                        soup = BeautifulSoup(res.text, 'html.parser')
                        actual_url = new_actual_url
                        break
                    except: pass
            
            # 2. 如果還是很簡單，尋找 iframe
            if len(soup.get_text().strip()) < 2000:
                iframe = soup.find('iframe', src=True)
                if iframe:
                    new_actual_url = urljoin(actual_url, iframe['src'])
                    try:
                        res = requests.get(new_actual_url, headers=headers, timeout=10, verify=False)
                        res.encoding = res.apparent_encoding
                        soup = BeautifulSoup(res.text, 'html.parser')
                        actual_url = new_actual_url
                    except: pass
        
        # 基準：首頁得分
        best_score = check_is_announcement_page(res.text, actual_url)
        best_candidate = actual_url if best_score >= 15 else None
        
        candidate_pool = []
        seen_urls = {actual_url}
        base_netloc = urlparse(actual_url).netloc
        
        # 加入預測出的 CMS 連結
        for cms_url in guess_cms_urls(actual_url, soup):
            if cms_url not in seen_urls:
                candidate_pool.append((100, cms_url))
                seen_urls.add(cms_url)
        
        for a in soup.find_all('a', href=True):
            href = a['href']
            href_lower = href.lower()
            if href.startswith(('javascript:', 'mailto:', '#', 'tel:')): continue
            if href_lower.endswith(('.jpg', '.jpeg', '.png', '.gif', '.pdf', '.doc', '.docx', '.zip', '.rar')): continue
            if 'login' in href_lower or 'signin' in href_lower: continue
            
            combined_text = f"{a.get_text(strip=True).upper()} {a.get('title', '').upper()}"
            if any(kw in combined_text for kw in TEXT_KEYWORDS) or any(kw in href_lower for kw in URL_KEYWORDS):
                full_url = urljoin(actual_url, href)
                if full_url not in seen_urls and is_valid_url(full_url):
                    candidate_netloc = urlparse(full_url).netloc
                    if base_netloc.replace('www.', '') in candidate_netloc:
                        seen_urls.add(full_url)
                        priority = 0
                        # 核心邏輯：極度優先處理「彙整」、「全部」、「更多」
                        if any(kw in combined_text for kw in ["更多", "全部", "MORE", "ALL", "彙整"]):
                            priority = 20
                        elif any(kw in combined_text for kw in ["公告", "NEWS"]):
                            priority = 15
                        elif 'site_news/main' in full_url.lower() or 'main_news' in full_url.lower():
                            priority = 12
                        elif '/p/4' in full_url.lower():
                            priority = 5
                        
                        if priority > 0:
                            candidate_pool.append((priority, full_url))
                    
        candidate_pool.sort(key=lambda x: x[0], reverse=True)
        candidates = [url for p, url in candidate_pool]
                    
        # 測試候選者
        for candidate_url in candidates[:50]:
            if stop_flag: break
            try:
                c_res = requests.get(candidate_url, headers=headers, timeout=10, verify=False)
                c_res.encoding = c_res.apparent_encoding
                c_score = check_is_announcement_page(c_res.text, candidate_url)
                
                if c_score > best_score:
                    best_score = c_score
                    best_candidate = candidate_url
            except: continue 
                
        return school_name, base_url, best_candidate
    except Exception: return school_name, base_url, None

def main():
    parser = argparse.ArgumentParser(description="從學校清單 CSV 檔爬取學校公告網址。")
    parser.add_argument("input_file")
    parser.add_argument("-o", "--output", default="school_announcements.csv")
    parser.add_argument("-w", "--workers", type=int, default=5)
    parser.add_argument("--rescan", action="store_true", help="強制重新掃描")
    args = parser.parse_args()

    load_file = args.output if os.path.exists(args.output) else args.input_file
    header, all_rows = [], []
    try:
        with open(load_file, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            header = next(reader)
            for row in reader: all_rows.append(row)
    except FileNotFoundError: sys.exit(1)
        
    name_idx, url_idx, note_idx = -1, -1, -1
    for i, col in enumerate(header):
        if "學校名稱" in col: name_idx = i
        elif ("網址" in col or "首頁" in col) and "公告" not in col: url_idx = i
        elif "備註" in col: note_idx = i
            
    if name_idx == -1 or url_idx == -1 or note_idx == -1: name_idx, url_idx, note_idx = 2, 7, 8
    for row in all_rows:
        while len(row) <= note_idx: row.append("")

    schools_to_process = []
    for row_index, row in enumerate(all_rows):
        if not args.rescan and row[note_idx].strip().startswith('http'): continue
        if row[name_idx].strip(): schools_to_process.append((row_index, row[name_idx].strip(), row[url_idx].strip()))
                
    print(f"待分析學校: {len(schools_to_process)} 所")
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {executor.submit(process_school, n, u): i for i, n, u in schools_to_process}
        
        count = 0
        try:
            for future in concurrent.futures.as_completed(futures):
                if stop_flag:
                    for f in futures: f.cancel()
                    break
                
                row_idx = futures[future]
                name, base, news_url = future.result()
                
                if news_url:
                    all_rows[row_idx][note_idx] = news_url
                    
                count += 1
                if count % 5 == 0 or count == len(schools_to_process):
                    print(f"進度: {count}/{len(schools_to_process)} - 剛處理完: {name} -> {news_url or '未找到'}")
        except KeyboardInterrupt:
            pass
                        
    with open(args.output, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(header)
        writer.writerows(all_rows)

if __name__ == "__main__":
    main()
