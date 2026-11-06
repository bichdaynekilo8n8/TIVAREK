"""Generate Pages-only canonical and sitemap. Never point to the local Spring application."""
import argparse,re
from pathlib import Path
from urllib.parse import urlsplit
from xml.sax.saxutils import escape
p=argparse.ArgumentParser(); p.add_argument('--base-url',required=True); args=p.parse_args()
u=urlsplit(args.base_url)
if u.scheme!='https' or not u.hostname or u.username or u.password or u.query or u.fragment or any(c in args.base_url for c in '<>"\\'):
    p.error('Use the exact public HTTPS Pages URL, including /TIVAREK/ when applicable.')
base=args.base_url.rstrip('/')+'/'
docs=Path(__file__).resolve().parents[1]/'docs'
page=docs/'index.html'; text=page.read_text(encoding='utf-8')
text=re.sub(r'\s*<link rel="canonical"[^>]*>','',text)
text=text.replace('</head>','<link rel="canonical" href="'+escape(base,{'"':'&quot;'})+'">\n</head>')
page.write_text(text,encoding='utf-8')
(docs/'robots.txt').write_text('User-agent: *\nAllow: /\nSitemap: '+base+'sitemap.xml\n',encoding='utf-8')
(docs/'sitemap.xml').write_text('<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>'+escape(base)+'</loc></url></urlset>\n',encoding='utf-8')
print('Pages SEO generated for '+base)
