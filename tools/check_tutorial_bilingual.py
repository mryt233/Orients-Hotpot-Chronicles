# -*- coding: utf-8 -*-
"""作者：。隔离 Edge 验证三语切换、台湾用语、刷新记忆、导航和图片。"""
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import hashlib
import json
import subprocess
import sys
import threading

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tutorial/Minecraft_Orients_Hotpot_Chronicles_Player_Guide.html"
QA = ROOT / "tutorial/qa-bilingual"
QA.mkdir(exist_ok=True)
mode = sys.argv[1] if len(sys.argv) > 1 else "desktop"
original = SOURCE.read_bytes()
prelude = """<script>window.qaErrors=[];window.addEventListener('error',e=>qaErrors.push(e.message||'resource error'),true);window.addEventListener('unhandledrejection',e=>qaErrors.push(String(e.reason)));</script>"""
checks = """<script>
window.addEventListener('load',()=>setTimeout(async()=>{
 const result={width:innerWidth},lang=document.getElementById('language');
 const ready=()=>new Promise(resolve=>document.addEventListener('guide-language-ready',resolve,{once:true}));
 const switchLanguage=async locale=>{let done=ready();lang.value=locale;lang.dispatchEvent(new Event('change'));await done};
 const snapshot=()=>{
  const imgs=Array.from(document.images),anchors=Array.from(document.querySelectorAll('a[href^="#"]'));
  return {language:document.documentElement.lang,images:imgs.length,
   brokenImages:imgs.filter(i=>!i.complete||!i.naturalWidth).map(i=>i.dataset.k),
   missingAnchors:anchors.filter(a=>!document.getElementById(a.hash.slice(1))).map(a=>a.hash),
   overflow:document.documentElement.scrollWidth>innerWidth};
 };
 try {
  document.documentElement.style.scrollBehavior='auto';
  if(!sessionStorage.getItem('bilingual-qa-phase')) {
   result.chinese=snapshot();
   document.getElementById('sauce-station').scrollIntoView();
   result.sectionPreserved=true;result.transitions=[];
   for(const locale of ['zh-TW','en','zh-CN','en','zh-TW','zh-CN','zh-TW']) {
    const from=document.documentElement.lang;
    await switchLanguage(locale);
    result.transitions.push(from+'>'+document.documentElement.lang);
    result.sectionPreserved=result.sectionPreserved&&location.hash==='#sauce-station';
    if(locale==='en')result.english=snapshot();
    if(locale==='zh-TW')result.taiwan=snapshot();
   }
   const text=document.querySelector('main').textContent;
   result.taiwanTerms=['噁心','回復','挖掘加速','掠奪','滑鼠','游標','資料夾','相思木','沾醬'].every(s=>text.includes(s));
   result.vanillaNames=['地獄石英','蘑菇湯','熔岩桶','橡木材','鵝卵石'].every(s=>document.querySelector('img[alt="'+s+'"]'));
   result.modNames=['橡木蘸料台','擀麵棍','小蔥'].every(s=>document.querySelector('img[alt="'+s+'"]'));
   sessionStorage.setItem('bilingual-qa-phase',JSON.stringify(result));
   let url=new URL(location.href);url.searchParams.delete('lang');history.replaceState(null,'',url.href);
   location.reload();return;
  }
  Object.assign(result,JSON.parse(sessionStorage.getItem('bilingual-qa-phase')));
  result.rememberedAfterReload=document.documentElement.lang==='zh-TW';
  await switchLanguage('zh-CN');result.returnedToChinese=document.documentElement.lang==='zh-CN';
  result.roundTripSection=location.hash==='#sauce-station';
  await switchLanguage('zh-TW');
  const menu=document.getElementById('menu'),nav=document.getElementById('nav');
  menu.click();result.menuOpens=nav.classList.contains('open');
  document.querySelector('nav a[href="#page-recipes"]').click();
  result.navigation=location.hash==='#page-recipes'&&!nav.classList.contains('open');
  let found='',find=window.find;window.find=s=>{found=s;return true};
  let q=document.getElementById('q');q.value='噁心';q.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter'}));
  result.search=found==='噁心';window.find=find;q.value='';
  result.labels=document.title.includes('遊玩指南')&&q.placeholder.startsWith('搜尋')&&lang.value==='zh-TW'&&lang.options.length===3&&lang.selectedOptions[0].textContent==='zh_tw';
  result.errors=qaErrors;
  history.replaceState(null,'',location.pathname+'?lang=zh_tw');window.scrollTo({top:0,behavior:'instant'});
  sessionStorage.removeItem('bilingual-qa-phase');
 } catch(e) {result.failure=String(e)}
 navigator.sendBeacon('/qa-result',JSON.stringify(result));
},500));
</script>"""
page = original.decode("utf-8").replace("<script>", prelude + "<script>", 1).replace("</body>", checks + "</body>").encode("utf-8")
reports = []
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass
    def do_GET(self):
        if self.path.startswith("/preview.html"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(page)
        else:
            self.send_response(204)
            self.end_headers()
    def do_POST(self):
        reports.append(json.loads(self.rfile.read(int(self.headers["Content-Length"]))))
        self.send_response(204)
        self.end_headers()

server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
edge = "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
size = "1440,1000" if mode == "desktop" else "520,900"
try:
    run = subprocess.run([edge, "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
                          "--user-data-dir=" + str(QA / ("edge-" + mode)), "--window-size=" + size,
                          "--virtual-time-budget=10000", "--screenshot=" + str(QA / (mode + ".png")),
                          "--dump-dom", f"http://127.0.0.1:{server.server_port}/preview.html?lang=zh-cn"],
                         stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=45,
                         creationflags=subprocess.CREATE_NO_WINDOW)
finally:
    server.shutdown()
    server.server_close()
assert run.returncode == 0, run.stderr.decode(errors="replace")
assert reports, "Browser checks did not complete"
result = reports[-1]
(QA / (mode + "-result.json")).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
assert "failure" not in result, result
for lang in ("chinese", "taiwan", "english"):
    state = result[lang]
    assert state["images"] == 710 and not state["brokenImages"] and not state["missingAnchors"] and not state["overflow"], result
for key in ("sectionPreserved", "rememberedAfterReload", "returnedToChinese", "roundTripSection", "menuOpens", "navigation", "search", "labels", "taiwanTerms", "vanillaNames", "modNames"):
    assert result[key], result
assert not result["errors"], result
assert hashlib.sha256(SOURCE.read_bytes()).digest() == hashlib.sha256(original).digest()
print(json.dumps(result, ensure_ascii=True))
