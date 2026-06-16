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

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# 關鍵字：用來在首頁尋找可能連向公告的連結
KEYWORDS = ["最新消息", "校務公告", "學校公告", "公告訊息", "公告事項", "行政公告", "News", "公告"]

def is_valid_url(url):
    try:
        result = urlparse(url)
        return all([result.scheme, result.netloc])
    except ValueError:
        return False

def check_is_announcement_page(html):
    """
    深度分析：判斷該網頁是否具有「公告列表」的特徵。
    特徵包含：
    1. 包含多個日期格式 (例如 2024-05, 113/05)
    2. 有表格 (table) 或是大量的列表 (ul/li)
    """
    soup = BeautifulSoup(html, 'html.parser')
    text = soup.get_text()
    
    # 找尋日期格式的特徵，例如 2024-05-12, 113.05.12, 2024/05/12
    date_pattern = re.compile(r'((?:11[0-9]|202[0-9])[/\-\.][0-1][0-9][/\-\.][0-3][0-9])')
    dates_found = date_pattern.findall(text)
    
    # 如果同一個頁面出現 3 個以上的日期，高度可能是公告列表
    if len(set(dates_found)) >= 3:
        return True
        
    # 或是檢查表格中的 <tr> 數量
    tables = soup.find_all('table')
    for table in tables:
        rows = table.find_all('tr')
        if len(rows) >= 5: # 一個表格有5列以上，可能是列表
            return True
            
    # 針對特定系統 (例如 ischool, Rpage) 的特徵 class 或 id
    if soup.find(class_=re.compile(r'news|announcement|list', re.I)):
        return True

    return False

def process_school(row):
    school_name = row[2]
    base_url = row[7].strip()
    
    if not base_url or not is_valid_url(base_url):
        return school_name, base_url, "無效網址"

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    try:
        # 1. 請求首頁
        res = requests.get(base_url, headers=headers, timeout=10, verify=False)
        res.encoding = res.apparent_encoding # 處理中文亂碼
        soup = BeautifulSoup(res.text, 'html.parser')
        
        candidates = []
        for a in soup.find_all('a', href=True):
            text = a.get_text(strip=True)
            href = a['href']
            
            # 排除明顯無關的連結
            if href.startswith('javascript:') or href.startswith('mailto:') or href == '#':
                continue
                
            # 檢查連結文字是否包含關鍵字
            if any(kw in text for kw in KEYWORDS):
                full_url = urljoin(base_url, href)
                if full_url not in candidates:
                    candidates.append(full_url)
                    
        # 2. 深度驗證候選網址
        for candidate_url in candidates:
            try:
                c_res = requests.get(candidate_url, headers=headers, timeout=5, verify=False)
                c_res.encoding = c_res.apparent_encoding
                if check_is_announcement_page(c_res.text):
                    return school_name, base_url, candidate_url
            except:
                continue # 驗證失敗就換下一個候選
                
        if candidates:
            return school_name, base_url, f"未確認的可能網址: {candidates[0]}"
        return school_name, base_url, "找不到公告連結"

    except Exception as e:
        return school_name, base_url, f"連線失敗"

def main():
    parser = argparse.ArgumentParser(description="從學校清單 CSV 檔爬取學校公告網址。")
    parser.add_argument("input_file", help="輸入的 CSV 檔案路徑 (例如: docs/high.csv)")
    parser.add_argument("-o", "--output", default="school_announcements.csv", help="輸出的 CSV 檔案路徑 (預設: school_announcements.csv)")
    parser.add_argument("-w", "--workers", type=int, default=5, help="同時執行的執行緒數量，調低可避免系統卡頓 (預設: 5)")
    args = parser.parse_args()

    input_file = args.input_file
    output_file = args.output
    
    schools = []
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            header = next(reader)
            for row in reader:
                if len(row) >= 8:
                    schools.append(row)
    except FileNotFoundError:
        print(f"錯誤: 找不到檔案 {input_file}")
        sys.exit(1)
                
    print(f"開始分析 {len(schools)} 所學校的網站...")
    print(f"使用 {args.workers} 個並行執行緒 (Threads)...")
    
    results = []
    # 使用 ThreadPoolExecutor 並行處理加速爬蟲
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {executor.submit(process_school, school): school for school in schools}
        
        count = 0
        for future in concurrent.futures.as_completed(futures):
            count += 1
            name, base, news_url = future.result()
            results.append([name, base, news_url])
            if count % 10 == 0 or count == len(schools):
                print(f"進度: {count}/{len(schools)} - 剛處理完: {name}")

    # 寫出結果
    with open(output_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["學校名稱", "學校首頁", "公告網址"])
        writer.writerows(results)
        
    print(f"分析完成！結果已儲存至 {output_file}")

if __name__ == "__main__":
    main()
