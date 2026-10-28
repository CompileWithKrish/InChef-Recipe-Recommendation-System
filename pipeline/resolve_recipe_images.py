import os
import re
import sys
import json
import time
import socket
import ipaddress
import argparse
from urllib.parse import urlparse, urljoin
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
from bs4 import BeautifulSoup
import pymysql

CACHE_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "cache")
CACHE_PATH = os.path.join(CACHE_DIR, "recipe_image_cache.json")

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_USER = os.environ.get("DB_USERNAME", "root")
DB_PASS = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "inchef_db")

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 InChefRecipeBot/1.0"
MAX_BYTES = 512 * 1024  # 512 KB limit to prevent oversized downloads
REQUEST_TIMEOUT = 8     # 8 seconds

def is_safe_public_url(url):
    """Rigorous SSRF protection blocking local, private, and internal addresses."""
    if not url or not isinstance(url, str):
        return False
    try:
        parsed = urlparse(url.strip())
        if parsed.scheme not in ('http', 'https'):
            return False
        hostname = parsed.hostname
        if not hostname:
            return False
        
        hostname_low = hostname.lower()
        if hostname_low in ('localhost', '127.0.0.1', '::1') or hostname_low.endswith('.local') or hostname_low.endswith('.internal'):
            return False

        # Resolve IP to test against private address spaces
        ip_str = socket.gethostbyname(hostname)
        ip = ipaddress.ip_address(ip_str)
        if (ip.is_private or ip.is_loopback or ip.is_link_local or 
            ip.is_multicast or ip.is_reserved or ip.is_unspecified):
            return False
        return True
    except Exception:
        return False

def extract_og_image_from_html(html_text, base_url):
    """Use BeautifulSoup to extract Open Graph or Twitter image metadata."""
    if not html_text:
        return None
    try:
        soup = BeautifulSoup(html_text, 'html.parser')
        
        # 1. Open Graph image
        og_tag = soup.find('meta', property=re.compile(r'^og:image(:url)?$', re.I))
        if not og_tag:
            og_tag = soup.find('meta', attrs={"name": re.compile(r'^og:image(:url)?$', re.I)})
        if og_tag and og_tag.get('content'):
            img_candidate = og_tag['content'].strip()
            if img_candidate:
                return urljoin(base_url, img_candidate)

        # 2. Twitter Card image
        tw_tag = soup.find('meta', attrs={"name": re.compile(r'^twitter:image(:src)?$', re.I)})
        if not tw_tag:
            tw_tag = soup.find('meta', property=re.compile(r'^twitter:image(:src)?$', re.I))
        if tw_tag and tw_tag.get('content'):
            img_candidate = tw_tag['content'].strip()
            if img_candidate:
                return urljoin(base_url, img_candidate)

        # 3. Link image_src
        link_tag = soup.find('link', rel=re.compile(r'^image_src$', re.I))
        if link_tag and link_tag.get('href'):
            img_candidate = link_tag['href'].strip()
            if img_candidate:
                return urljoin(base_url, img_candidate)

        return None
    except Exception:
        return None

def fetch_page_metadata(source_url):
    """Safely fetch HTML head with size limits and SSRF verification."""
    if not is_safe_public_url(source_url):
        return None
    
    headers = {
        'User-Agent': USER_AGENT,
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9'
    }

    try:
        with requests.get(source_url, headers=headers, timeout=REQUEST_TIMEOUT, stream=True, allow_redirects=True) as response:
            if response.status_code != 200:
                return None
            
            # Verify final redirected URL for SSRF safety as well
            if not is_safe_public_url(response.url):
                return None

            content_type = response.headers.get('content-type', '').lower()
            if 'text/html' not in content_type and 'xhtml' not in content_type:
                return None

            # Read only up to MAX_BYTES (512 KB)
            chunks = []
            total_size = 0
            for chunk in response.iter_content(chunk_size=16384):
                chunks.append(chunk)
                total_size += len(chunk)
                # If we've passed </head>, we have all metadata
                partial_text = b"".join(chunks)
                if b'</head>' in partial_text.lower() or total_size >= MAX_BYTES:
                    break

            html = partial_text.decode('utf-8', errors='ignore')
            img_url = extract_og_image_from_html(html, response.url)
            
            if img_url and is_safe_public_url(img_url):
                # Ensure it's not a generic site favicon
                if not any(bad in img_url.lower() for bad in ['favicon', 'logo-placeholder', 'blank.gif']):
                    return img_url
            return None
    except Exception:
        return None

def load_cache():
    if os.path.exists(CACHE_PATH):
        try:
            with open(CACHE_PATH, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception:
            return {}
    return {}

def save_cache(cache):
    os.makedirs(CACHE_DIR, exist_ok=True)
    temp_path = CACHE_PATH + ".tmp"
    with open(temp_path, 'w', encoding='utf-8') as f:
        json.dump(cache, f, indent=2)
    os.replace(temp_path, CACHE_PATH)

def resolve_images_job(limit=None, workers=5, dry_run=False):
    print("=" * 70)
    print("PHASE 2: RECIPE IMAGE RESOLUTION & CACHING JOB")
    print(f"Workers: {workers} | Limit: {limit} | Dry Run: {dry_run}")
    print("=" * 70)

    cache = load_cache()
    print(f"[CACHE] Loaded {len(cache)} existing cached image entries from {CACHE_PATH}")

    if not DB_PASS:
        print("Error: DB_PASSWORD environment variable is not set.", file=sys.stderr)
        print("Please set DB_PASSWORD in your environment or .env file before resolving images.", file=sys.stderr)
        sys.exit(1)

    conn = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset='utf8mb4',
        autocommit=False
    )

    try:
        with conn.cursor() as cur:
            query = """
                SELECT id, title, source_recipe_id, source_url, image_url 
                FROM recipes 
                WHERE (image_url IS NULL OR image_url = '') 
                  AND source_url IS NOT NULL 
                  AND source_url != ''
                ORDER BY id ASC
            """
            if limit:
                query += f" LIMIT {limit}"
            cur.execute(query)
            candidates = cur.fetchall()

        print(f"[DATABASE] Found {len(candidates)} recipes eligible for image resolution.")

        # Filter out what we can resolve from cache first
        to_fetch = []
        already_cached_updates = []

        for r_id, title, s_id, s_url, cur_img in candidates:
            s_url_clean = s_url.strip()
            if s_url_clean in cache:
                cached_val = cache[s_url_clean]
                if cached_val:
                    already_cached_updates.append((r_id, cached_val))
            else:
                to_fetch.append((r_id, title, s_id, s_url_clean))

        print(f"[CACHE] Resolved {len(already_cached_updates)} recipes directly from cache.")
        print(f"[NETWORK] Needs remote metadata fetch: {len(to_fetch)} recipes.")

        # Apply cached updates to database first
        if already_cached_updates and not dry_run:
            with conn.cursor() as cur:
                cur.executemany("UPDATE recipes SET image_url = %s, updated_at = NOW() WHERE id = %s",
                               [(img, rid) for rid, img in already_cached_updates])
            conn.commit()
            print(f"[DATABASE] Committed {len(already_cached_updates)} cached image updates to MySQL.")

        # Remote resolution with ThreadPoolExecutor
        resolved_count = 0
        failed_count = 0
        db_updates_batch = []

        def worker_task(item):
            rid, title, sid, url = item
            img = fetch_page_metadata(url)
            return (rid, url, img)

        if to_fetch:
            print(f"[FETCH] Starting polite asynchronous metadata extraction across {workers} workers...")
            with ThreadPoolExecutor(max_workers=workers) as executor:
                futures = {executor.submit(worker_task, item): item for item in to_fetch}
                
                processed = 0
                for fut in as_completed(futures):
                    processed += 1
                    rid, url, img = fut.result()
                    cache[url] = img  # store even if None so we don't repeat failed pages
                    
                    if img:
                        resolved_count += 1
                        db_updates_batch.append((img, rid))
                    else:
                        failed_count += 1

                    if processed % 25 == 0 or processed == len(to_fetch):
                        # Commit to DB
                        if db_updates_batch and not dry_run:
                            with conn.cursor() as cur:
                                cur.executemany("UPDATE recipes SET image_url = %s, updated_at = NOW() WHERE id = %s",
                                                db_updates_batch)
                            conn.commit()
                            db_updates_batch = []
                        # Save cache to disk
                        save_cache(cache)
                        print(f"  Processed {processed}/{len(to_fetch)} (Resolved: {resolved_count}, Unresolved: {failed_count})...")

            # Final flush
            if db_updates_batch and not dry_run:
                with conn.cursor() as cur:
                    cur.executemany("UPDATE recipes SET image_url = %s, updated_at = NOW() WHERE id = %s",
                                    db_updates_batch)
                conn.commit()
            save_cache(cache)

        print("\n" + "=" * 70)
        print("PHASE 2 IMAGE RESOLUTION SUMMARY")
        print("=" * 70)
        print(f"Total Eligible Candidates Checked:     {len(candidates)}")
        print(f"Resolved from Disk Cache:              {len(already_cached_updates)}")
        print(f"Newly Resolved via Web Metadata:       {resolved_count}")
        print(f"Unresolved / Inaccessible Pages:       {failed_count}")
        print(f"Total Updated in Database:             {len(already_cached_updates) + resolved_count if not dry_run else 0}")
        print(f"Total Cached Entries on Disk:          {len(cache)}")
        print("=" * 70)

    finally:
        conn.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Recipe Image Metadata Resolver")
    parser.add_argument("--limit", type=int, default=None, help="Max recipes to resolve in this run")
    parser.add_argument("--workers", type=int, default=8, help="Number of concurrent workers")
    parser.add_argument("--dry-run", action="store_true", help="Resolve without writing to MySQL")
    args = parser.parse_args()

    resolve_images_job(limit=args.limit, workers=args.workers, dry_run=args.dry_run)
