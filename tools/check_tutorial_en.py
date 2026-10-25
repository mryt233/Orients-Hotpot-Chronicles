# 作者：。内置浏览器桥接不可用，使用隔离 Edge 无头配置验证静态教程，不接入用户浏览器会话。
from pathlib import Path
from html.parser import HTMLParser
import hashlib
import json
import subprocess
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

r=Path(__file__).resolve().parents[1]
locale=sys.argv[2] if len(sys.argv)>2 else 'en'
assert locale in ('en','zh_tw'),locale
source=r/('tutorial/Minecraft_Orients_Hotpot_Chronicles_Player_Guide_zh_tw.html' if locale=='zh_tw' else 'tutorial/Minecraft_Orients_Hotpot_Chronicles_Player_Guide_EN.html')
source_hash=hashlib.sha256(source.read_bytes()).hexdigest()
mode=sys.argv[1] if len(sys.argv)>1 else 'desktop'
qa=r/('tutorial/qa-zh_tw' if locale=='zh_tw' else 'tutorial/qa-en');qa.mkdir(exist_ok=True)
prelude='''<script>window.__qaErrors=[];window.addEventListener('error',function(e){window.__qaErrors.push(e.message||'resource error')},true);window.addEventListener('unhandledrejection',function(e){window.__qaErrors.push(String(e.reason))});</script>'''
checks='''<script>
window.addEventListener('load',function(){setTimeout(function(){
 var images=Array.from(document.images),nav=document.getElementById('nav'),menu=document.getElementById('menu');
 var broken=images.filter(function(i){return !i.complete||!i.naturalWidth}).map(function(i){return i.dataset.k||i.getAttribute('src')||''});
 var anchors=Array.from(document.querySelectorAll('a[href^="#"]')).filter(function(a){return a.getAttribute('href').length>1});
 var missing=anchors.filter(function(a){return !document.getElementById(decodeURIComponent(a.getAttribute('href').slice(1)))}).map(function(a){return a.getAttribute('href')});
 var openBefore=nav.classList.contains('open');menu.click();var toggled=nav.classList.contains('open')!==openBefore;menu.click();
 var link=document.querySelector('a[href="#page-sauce"]');link.click();var navigates=location.hash==='#page-sauce';
 history.replaceState(null,'',location.pathname);document.documentElement.style.scrollBehavior='auto';window.scrollTo({top:0,behavior:'instant'});
 var result={title:document.title,width:innerWidth,imageCount:images.length,brokenImages:broken,anchorCount:anchors.length,missingAnchors:missing,menuToggles:toggled,sauceNavigationWorks:navigates,errors:window.__qaErrors,horizontalOverflow:document.documentElement.scrollWidth>innerWidth,language:document.documentElement.lang,hasReleasedVersion:document.body.innerText.includes('0.1.2-alpha'),hasUpcomingLabels:(document.body.innerText.includes('Coming in 0.1.3')||document.body.innerText.includes('0.1.3 更新預告'))};
 var out=document.createElement('pre');out.id='qa-result';out.hidden=true;out.textContent=JSON.stringify(result);document.body.appendChild(out);navigator.sendBeacon('/qa-result',JSON.stringify(result));
},1000)});
</script>'''
text=source.read_text(encoding='utf-8');text=text.replace('<script>',prelude+'<script>',1).replace('</body>',checks+'</body>')
preview=qa/'preview.html';preview.write_text(text,encoding='utf-8')
reports=[];report_ready=threading.Event();requests_seen=[]
class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def do_GET(self):
        requests_seen.append(self.path)
        if self.path.startswith('/preview.html'):
            raw=preview.read_bytes();self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
        else:self.send_response(204);self.end_headers()
    def do_POST(self):
        if self.path!='/qa-result':self.send_error(404);return
        reports.append(json.loads(self.rfile.read(int(self.headers['Content-Length']))));report_ready.set();self.send_response(204);self.end_headers()
server=ThreadingHTTPServer(('127.0.0.1',0),Handler)
threading.Thread(target=server.serve_forever,daemon=True).start()
edge=Path('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe')
size='1440,1000' if mode=='desktop' else '520,900'
args=[str(edge),'--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check',
    '--user-data-dir='+str(qa/f'edge-{mode}'),'--window-size='+size,'--virtual-time-budget=6000',
    '--screenshot='+str(qa/f'{mode}.png'),'--dump-dom',f'http://127.0.0.1:{server.server_port}/preview.html']
run=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=60,creationflags=subprocess.CREATE_NO_WINDOW)
report_ready.wait(20)
server.shutdown();server.server_close()
(qa/f'{mode}-edge.log').write_bytes(run.stderr)
assert run.returncode==0,'Headless Edge failed'
class ResultParser(HTMLParser):
    def __init__(self):super().__init__();self.active=False;self.data=[]
    def handle_starttag(self,tag,attrs):
        if tag=='pre' and dict(attrs).get('id')=='qa-result':self.active=True
    def handle_endtag(self,tag):
        if tag=='pre':self.active=False
    def handle_data(self,data):
        if self.active:self.data.append(data)
assert reports,('Browser checks did not complete',requests_seen)
result=reports[-1];(qa/f'{mode}-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
assert result['imageCount']>100 and not result['brokenImages'] and not result['missingAnchors'] and not result['errors'],result
assert not result['horizontalOverflow'] and result['language']==('zh-TW' if locale=='zh_tw' else 'en'),result
assert result['menuToggles'] and result['sauceNavigationWorks'] and result['hasReleasedVersion'] and result['hasUpcomingLabels']
assert hashlib.sha256(source.read_bytes()).hexdigest()==source_hash
print(json.dumps(result,ensure_ascii=False))
