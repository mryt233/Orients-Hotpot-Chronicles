// 作者：mryt233（2026-10-27）指南样式教程：书签切换、分栏翻页（同 GuideBookScreen：一页翻完到下一条目，再到下一分类）、原版样式鼠标提示
(function () {
  var B = window.BOOK, IMG = window.IMG;
  var $ = function (id) { return document.getElementById(id); };
  var flow = $("flow"), view = $("view"), cats = $("cats"), tabs = $("tabs"), tip = $("tip");
  var st = { c: 0, e: 0, p: 0, pages: 1 };

  function fillImgs(root) {
    root.querySelectorAll("img[data-k]").forEach(function (i) { if (!i.src) i.src = IMG[i.dataset.k]; });
  }
  function iconHTML(id) { var n = $(id); return n ? n.innerHTML : ""; }

  // 一屏几页：宽屏两页（左右），窄屏一页
  function mobile() { return window.matchMedia("(max-width:820px)").matches; }
  function perScreen() { return mobile() ? 1 : 2; }
  // 手机：点「目录」书签滑出菜单（分类书签 + 条目书签），点遮罩或选了条目就收回
  function menu(open) {
    $("drawer").classList.toggle("open", open); $("backdrop").classList.toggle("open", open);
  }
  function metrics() {
    var cs = getComputedStyle(flow);
    var colW = parseFloat(cs.columnWidth) || flow.clientWidth, gap = parseFloat(cs.columnGap) || 0;
    var n = perScreen();
    var cols = Math.max(1, Math.round((flow.scrollWidth + gap) / (colW + gap)));
    return { step: n * (colW + gap), pages: Math.max(1, Math.ceil(cols / n)) };
  }

  function drawTabs() {
    cats.innerHTML = "";
    B.forEach(function (c, i) {
      var b = document.createElement("button");
      b.className = (i === st.c ? "on " : "") + (c.todo ? "todo" : "");
      b.innerHTML = iconHTML("ci-" + i);
      b.dataset.tip = c.t + (c.todo ? "\n样稿里还没排" : "");
      b.onclick = function () { go(i, 0, 0); };   // 手机菜单里点分类：书翻过去，菜单留着让你再挑条目
      cats.appendChild(b);
    });
    tabs.innerHTML = mobile() ? '<div class="ctitle">' + B[st.c].t + "</div>" : "";
    B[st.c].e.forEach(function (t, i) {
      var b = document.createElement("button");
      b.className = i === st.e ? "on" : "";
      b.innerHTML = iconHTML("ei-" + st.c + "-" + i) + '<span class="lbl">' + t + "</span>";
      if (!mobile()) b.dataset.tip = t;
      b.onclick = function () { go(st.c, i, 0); menu(false); };
      tabs.appendChild(b);
    });
    $("menu-ic").innerHTML = iconHTML("ci-" + st.c);   // 三横线旁边放当前分类的图标
    $("menu").setAttribute("aria-label", "目录：" + B[st.c].t);
    // 手机：书顶「三横线」书签右边一排条目小书签，左右滑动挑
    var mt = $("mtabs");
    mt.innerHTML = "";
    B[st.c].e.forEach(function (t, i) {
      var b = document.createElement("button");
      b.className = i === st.e ? "on" : "";
      b.innerHTML = iconHTML("ei-" + st.c + "-" + i);
      b.setAttribute("aria-label", t);
      b.onclick = function () { go(st.c, i, 0); };
      mt.appendChild(b);
    });
    fillImgs(cats); fillImgs(tabs); fillImgs(mt); fillImgs($("menu-ic"));
    var on = mt.querySelector(".on");
    if (on && mobile()) mt.scrollLeft = Math.max(0, on.offsetLeft - mt.clientWidth / 2 + on.offsetWidth / 2);
  }


  // ---------- 合成表大全：同一类的配方一张卡，材料自动轮换；点材料停在那一种，再点恢复 ----------
  function esc(t) { return String(t == null ? "" : t).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;"); }
  function slotHTML(c, n) {
    if (!c) return '<div class="slot"></div>';
    var img = c[0] ? '<img class="ic" src="' + IMG[c[0]] + '" alt="">' : '<span class="ic missing">?</span>';
    return '<div class="slot" data-tip="' + esc(c[1]) + '">' + img + (n > 1 ? '<b class="cnt">' + n + "</b>" : "") + "</div>";
  }
  function drawGroup(el) {
    var g = el._g, v = g.v[el._i], one = g.kind === "cook" || g.kind === "cut";
    var grid = one ? '<div class="grid one">' + slotHTML(v.cells[0]) + "</div>"
      : '<div class="grid g3">' + v.cells.map(function (c) { return slotHTML(c); }).join("") + "</div>";
    el.querySelector(".rg-b").innerHTML = grid + '<i class="arr"></i>' + slotHTML(v.out.slice(0, 2), v.out[2]) +
      '<div class="rg-pv">' + (v.out[0] ? '<img src="' + IMG[v.out[0]] + '" alt="">' : "") + "<b>" + esc(v.out[1]) + "</b>" +
      (v.note ? "<small>" + esc(v.note) + "</small>" : "") + (v.dev ? '<span class="tag dev">开发版预览</span>' : "") + "</div>";
    el.querySelectorAll(".rg-chips button").forEach(function (b, i) { b.classList.toggle("on", i === el._i); b.classList.toggle("pin", i === el._i && el._pin); });
  }
  function renderGroups(root) {
    root.querySelectorAll(".rg[data-g]").forEach(function (el) {
      var g = JSON.parse(el.dataset.g);
      el._g = g; el._i = 0; el._pin = false;
      var allDev = g.v.every(function (v) { return v.dev; });
      var chips = g.v.length > 1 ? '<div class="rg-chips">' + g.v.map(function (v, i) {
        var m = v.mat || v.out;
        return '<button data-i="' + i + '" data-tip="' + esc(m[1] + (v.mat && v.out[1] !== m[1] ? " → " + v.out[1] : "")) + '"' + (v.dev ? ' class="dev"' : "") + ">" +
          (m[0] ? '<img src="' + IMG[m[0]] + '" alt="">' : "?") + "</button>";
      }).join("") + "</div>" : "";
      el.innerHTML = '<div class="rg-t">' + esc(g.title) + (allDev ? '<span class="tag dev">开发版预览</span>' : "") + "</div>" +
        '<div class="rg-b rbody"></div>' + chips;
      el.querySelectorAll(".rg-chips button").forEach(function (b) {
        b.onclick = function () {
          var i = +b.dataset.i;
          if (el._pin && el._i === i) el._pin = false; else { el._pin = true; el._i = i; }
          drawGroup(el);
        };
      });
      drawGroup(el);
    });
  }
  // ---------- 锅形展示：先选铜 / 铁，再点锅形；大图斜看、小图俯看，点大图两张互换 ----------
  function renderPotViewers(root) {
    root.querySelectorAll(".potviewer[data-p]").forEach(function (el) {
      var pots = JSON.parse(el.dataset.p), mat = "copper", k = pots[0].k, swap = false;
      var src = {};   // 书里图片都是内嵌的：从静态图里拿到地址
      el.querySelectorAll("img").forEach(function (i) { src[i.getAttribute("src") || i.dataset.k] = i.src; });
      function img(m, kk, v) { var key = "img/pot_" + m + "_" + kk + "_" + v + ".png"; return IMG[key] || src[key] || ""; }
      function draw() {
        var p = pots.filter(function (x) { return x.k === k; })[0], a = swap ? "top" : "iso", b = swap ? "iso" : "top";
        el.innerHTML =
          '<div class="pv-stage"><img class="pv-main" src="' + img(mat, k, a) + '" alt="" title="点一下换个角度">' +
          '<img class="pv-alt" src="' + img(mat, k, b) + '" alt="" title="点一下换个角度"></div>' +
          '<div class="pv-info"><b>' + (mat === "copper" ? "铜制" : "铁制") + esc(p.n) + "</b>" + (p.dev ? '<span class="tag dev">开发版预览</span>' : "") +
          "<p>" + esc(p.d) + '</p><p class="pv-r">做法：' + esc(p.r) + "</p></div>" +
          '<div class="pv-row pv-mats">' + ["copper", "iron"].map(function (m) {
            return '<button data-m="' + m + '" class="' + (m === mat ? "on" : "") + '"><img src="' + img(m, "nine_grid_hotpot", "iso") + '" alt="">' + (m === "copper" ? "铜锅" : "铁锅") + "</button>";
          }).join("") + "</div>" +
          '<div class="pv-row pv-shapes">' + pots.map(function (x) {
            return '<button data-k="' + x.k + '" class="' + (x.k === k ? "on" : "") + (x.dev ? " dev" : "") + '" data-tip="' + esc(x.n) + (x.dev ? "\n开发版预览" : "") + '"><img src="' + img(mat, x.k, "top") + '" alt=""></button>';
          }).join("") + "</div>";
        el.querySelectorAll(".pv-mats button").forEach(function (b) { b.onclick = function () { mat = b.dataset.m; draw(); }; });
        el.querySelectorAll(".pv-shapes button").forEach(function (b) { b.onclick = function () { k = b.dataset.k; draw(); }; });
        el.querySelectorAll(".pv-stage img").forEach(function (i) { i.onclick = function () { swap = !swap; draw(); }; });
      }
      el.removeAttribute("data-p");
      draw();
    });
  }
  setInterval(function () {
    flow.querySelectorAll(".rg").forEach(function (el) {
      if (!el._g || el._pin || el._g.v.length < 2 || el.matches(":hover")) return;
      el._i = (el._i + 1) % el._g.v.length; drawGroup(el);
    });
  }, 1600);

  function layout(anim) {
    var m = metrics();
    st.pages = m.pages;
    if (st.p === "last") st.p = m.pages - 1;
    st.p = Math.max(0, Math.min(st.p, m.pages - 1));
    flow.classList.toggle("noanim", !anim);
    flow.style.transform = "translateX(" + (-st.p * m.step) + "px)";
    var first = st.c === 0 && st.e === 0 && st.p === 0;
    var last = st.c === B.length - 1 && st.e === B[st.c].e.length - 1 && st.p === m.pages - 1;
    $("prev").hidden = first; $("next").hidden = last;
    $("pno").textContent = m.pages > 1 ? (st.p + 1) + " / " + m.pages : "";
  }

  // 翻页音效：同游戏里的指南，翻页、换条目、换分类都响一声原版翻书声（3 段随机）；右上角喇叭能关，记在本机
  var sndOn = true;
  try { sndOn = localStorage.getItem("hpt-book-sound") !== "off"; } catch (x) {}
  var sndBtn = $("snd");
  function drawSnd() { sndBtn.className = "snd" + (sndOn ? "" : " off"); sndBtn.dataset.tip = sndOn ? "翻页音效：开（点一下关掉）" : "翻页音效：关（点一下打开）"; }
  sndBtn.onclick = function () {
    sndOn = !sndOn; drawSnd();
    try { localStorage.setItem("hpt-book-sound", sndOn ? "on" : "off"); } catch (x) {}
    if (sndOn) flip();
  };
  drawSnd();
  var booted = false;
  function flip() {
    if (!sndOn || !booted || !window.SND || !SND.length) return;
    try {
      var a = new Audio(SND[Math.floor(Math.random() * SND.length)]);
      a.volume = 0.28;   // 用户 2026-10-27：小 50% a.playbackRate = 0.9 + Math.random() * 0.2;
      a.play().catch(function () {});
    } catch (x) {}
  }

  // 「往前翻」还是「往回翻」：决定纸往哪边翻、正文从哪边浮进来
  function forward(c, e, p) {
    if (c !== st.c) return c > st.c;
    if (e !== st.e) return e > st.e;
    return p === "last" ? false : p > st.p;
  }
  // 翻页（电脑上）：盖一张「真纸」绕书脊翻过去——正面是翻走的那页（旧内容的复制品），背面是翻过来的新页；
  // 纸翻到一半前，被它盖住那一侧照旧显示旧的一页；翻动时正反面随角度变暗变亮，下面的页上投一道影子
  var TURN_MS = 620;
  function reduced() { return window.matchMedia("(prefers-reduced-motion:reduce)").matches; }
  function cloneView(left, top) {
    var v = flow.parentNode, c = v.cloneNode(true);
    c.removeAttribute("id");
    c.querySelectorAll("[id]").forEach(function (n) { n.removeAttribute("id"); });
    var f = c.firstElementChild;
    if (f) { f.classList.remove("enter", "back"); f.style.transition = "none"; }
    c.style.cssText += ";position:absolute;margin:0;left:" + left + "px;top:" + top + "px";
    return c;
  }
  // 竖屏（单页）翻页：书脊在左边。往后翻 = 当前这页从左边掀起、翻过去消失，露出下一页；
  // 往前翻 = 上一页从左边翻回来盖住当前页（用户 2026-10-27：竖屏只有左右平移，没有翻页的样子）
  function turnPageSingle(fwd, apply) {
    var paper = flow.parentNode.parentNode, view = flow.parentNode;
    var W = paper.clientWidth, H = paper.clientHeight, pad = view.offsetLeft, top = view.offsetTop;
    var old = cloneView(pad, top);
    apply();
    var neu = cloneView(pad, top);
    function box(cls) {
      var d = document.createElement("div");
      d.className = cls;
      d.style.cssText = "top:0;left:0;height:" + H + "px;width:" + W + "px";
      return d;
    }
    var sheet = box("sheet1 " + (fwd ? "out" : "in")), face = document.createElement("div");
    face.className = "face1"; face.appendChild(fwd ? old : neu); sheet.appendChild(face);
    var shade = box("pg-shade1 " + (fwd ? "out" : "in"));
    var under = null;
    if (!fwd) { under = box("pg-under"); under.appendChild(old); paper.appendChild(under); }   // 往回翻：新页翻回来盖住之前，下面还是旧页
    paper.appendChild(shade); paper.appendChild(sheet);
    sheet.addEventListener("animationend", function (ev) {
      if (ev.target === sheet) { sheet.remove(); shade.remove(); if (under) under.remove(); }
    });
  }

  function turnPage(fwd, apply) {
    var paper = flow.parentNode.parentNode, view = flow.parentNode;
    var W = paper.clientWidth / 2, H = paper.clientHeight, pad = view.offsetLeft, top = view.offsetTop;
    var oldFront = cloneView(fwd ? pad - W : pad, top);     // 翻走的那页
    var oldUnder = cloneView(fwd ? pad : pad - W, top);     // 另一侧暂时还显示的旧页
    apply();
    var newBack = cloneView(fwd ? pad : pad - W, top);      // 翻过来的新页
    function box(cls, side) {
      var d = document.createElement("div");
      d.className = cls;
      d.style.cssText = "top:0;height:" + H + "px;width:" + W + "px;" + side;
      return d;
    }
    var under = box("pg-under", fwd ? "left:0" : "left:" + W + "px");
    under.appendChild(oldUnder);
    var sheet = box("sheet3d " + (fwd ? "fwd" : "bwd"), fwd ? "left:" + W + "px" : "left:0");
    var front = document.createElement("div"), back = document.createElement("div");
    front.className = "face front"; back.className = "face back";
    front.appendChild(oldFront); back.appendChild(newBack);
    sheet.appendChild(front); sheet.appendChild(back);
    var shade = box("pg-shade " + (fwd ? "fwd" : "bwd"), fwd ? "left:" + W + "px" : "left:0");
    paper.appendChild(under); paper.appendChild(shade); paper.appendChild(sheet);
    setTimeout(function () { under.remove(); }, TURN_MS * 0.5);
    sheet.addEventListener("animationend", function (ev) {
      if (ev.target === sheet) { sheet.remove(); shade.remove(); }
    });
  }

  function go(c, e, p, anchor) {
    var moved = c !== st.c || e !== st.e || p !== st.p;
    var fwd = moved && booted ? forward(c, e, p) : true;
    if (moved) flip();
    var anim = moved && booted && !reduced();
    if (anim && !mobile()) turnPage(fwd, function () { apply(c, e, p, anchor, fwd, true); });
    else if (anim) turnPageSingle(fwd, function () { apply(c, e, p, anchor, fwd, true); });
    else apply(c, e, p, anchor, fwd, false);
  }
  function apply(c, e, p, anchor, fwd, turning) {
    var changed = c !== st.c || e !== st.e || !flow.childElementCount;
    st.c = c; st.e = e; st.p = p;
    if (changed) {
      flow.innerHTML = $("t-" + c + "-" + e).innerHTML;
      if (booted && !turning) {   // 手机上没有翻纸，正文从翻页方向浮入
        flow.classList.remove("enter", "back"); void flow.offsetWidth;
        flow.classList.add("enter"); if (!fwd) flow.classList.add("back");
      }
      fillImgs(flow);
      renderGroups(flow);
      renderPotViewers(flow);
      // 图片解码完分栏会变长，重新数一次页
      flow.querySelectorAll("img").forEach(function (i) { if (!i.complete) i.addEventListener("load", function () { layout(false); }, { once: true }); });
      drawTabs();
    }
    layout(!changed && !turning);
    if (anchor && typeof anchor === "object") {
      var hit = markText(flow, anchor.text);
      if (hit) { st.p = Math.floor(hit.offsetLeft / metrics().step); layout(false); }
    } else if (anchor) {
      var t = document.getElementById(anchor);
      if (t && flow.contains(t)) { st.p = Math.floor(t.offsetLeft / metrics().step); layout(false); }
    }
    try { history.replaceState(null, "", "#" + c + "-" + e + "-" + st.p); } catch (x) {}
  }

  // ---------- 搜索（右上角放大镜书签；手机在书顶那排最右边） ----------
  function markText(root, q) {
    if (!q) return null;
    var low = q.toLowerCase(), w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT), n;
    while ((n = w.nextNode())) {
      var i = n.nodeValue.toLowerCase().indexOf(low);
      if (i < 0 || n.parentNode.closest("script,style")) continue;
      var r = document.createRange(); r.setStart(n, i); r.setEnd(n, i + q.length);
      var m = document.createElement("mark"); m.className = "hit";
      r.surroundContents(m);
      return m;
    }
    return null;
  }
  var findbox = $("findbox"), fq = $("fq"), flist = $("flist");
  // 指南针：同原版 CompassAngleState——每刻 速度 += (目标 - 角度) × 0.1，速度 × 0.8，角度 += 速度（20 TPS），
  // 帧 = 16 + 角度 × 32（原版角度 0 用 compass_16，指针朝上）。点搜索书签时给指针一下冲劲，转一圈多再晃着停回朝上
  var cmpImgs = [].slice.call(document.querySelectorAll("img.cmp")), cmpR = 0, cmpV = 0, cmpTimer = null;
  function cmpDraw() {
    if (!window.COMPASS || !COMPASS.length) return;
    var f = ((16 + Math.round(cmpR * 32)) % 32 + 32) % 32;
    cmpImgs.forEach(function (i) { i.src = COMPASS[f]; });
  }
  function cmpSpin() {
    cmpV += 0.42 + Math.random() * 0.12;
    if (cmpTimer) return;
    cmpTimer = setInterval(function () {
      var d = -cmpR; d = d - Math.round(d);          // 最短方向回到朝上
      cmpV += d * 0.1; cmpV *= 0.8; cmpR += cmpV;
      cmpDraw();
      if (Math.abs(cmpV) < 0.001 && Math.abs(d) < 0.005) { clearInterval(cmpTimer); cmpTimer = null; cmpR = 0; cmpV = 0; cmpDraw(); }
    }, 50);
  }
  cmpDraw();
  function openFind(on) {
    cmpSpin();
    findbox.hidden = !on;
    $("find").classList.toggle("on", on); $("mfind").classList.toggle("on", on);
    if (on) { fq.focus(); fq.select(); runFind(); }
  }
  function esc2(t) { return String(t).replace(/&/g, "&amp;").replace(/</g, "&lt;"); }
  function runFind() {
    var q = fq.value.trim(), low = q.toLowerCase();
    if (!q) { flist.innerHTML = '<p class="fb-hint">输入想找的东西，比如「毛肚」「鸳鸯锅」「蘸料」「配方」。</p>'; return; }
    var out = [];
    (window.SIDX || []).forEach(function (d) {
      var title = B[d.c].e[d.e], tl = title.toLowerCase(), x = d.x, i = x.toLowerCase().indexOf(low);
      var inTitle = tl.indexOf(low) >= 0 || B[d.c].t.toLowerCase().indexOf(low) >= 0;
      if (i < 0 && !inTitle) return;
      var snip = "";
      if (i >= 0) {
        var a = Math.max(0, i - 18), b = Math.min(x.length, i + q.length + 30);
        snip = (a ? "…" : "") + esc2(x.slice(a, i)) + "<mark>" + esc2(x.slice(i, i + q.length)) + "</mark>" + esc2(x.slice(i + q.length, b)) + (b < x.length ? "…" : "");
      }
      out.push({ d: d, score: inTitle ? 0 : 1, html: '<button class="fb-item" data-c="' + d.c + '" data-e="' + d.e + '" data-hit="' + (i >= 0 ? 1 : 0) + '">' +
        '<span class="fb-ic">' + iconHTML("ei-" + d.c + "-" + d.e) + '</span><span class="fb-t"><b>' + esc2(B[d.c].t) + " · " + esc2(title) + "</b>" +
        (snip ? "<small>" + snip + "</small>" : "") + "</span></button>" });
    });
    out.sort(function (a, b) { return a.score - b.score; });
    flist.innerHTML = out.length ? out.slice(0, 40).map(function (o) { return o.html; }).join("") : '<p class="fb-hint">没有找到「' + esc2(q) + "」。</p>";
    fillImgs(flist);
  }
  fq.addEventListener("input", runFind);
  fq.addEventListener("keydown", function (ev) {
    if (ev.key === "Enter") { var f = flist.querySelector(".fb-item"); if (f) f.click(); }
  });
  flist.addEventListener("click", function (ev) {
    var b = ev.target.closest(".fb-item"); if (!b) return;
    var q = fq.value.trim();
    openFind(false);
    go(+b.dataset.c, +b.dataset.e, 0, b.dataset.hit === "1" ? { text: q } : null);
  });
  $("find").onclick = function () { openFind(findbox.hidden); };
  $("mfind").onclick = function () { menu(false); openFind(findbox.hidden); };
  $("fclose").onclick = function () { openFind(false); };
  document.addEventListener("keydown", function (ev) {
    if (ev.key === "Escape" && !findbox.hidden) openFind(false);
    else if ((ev.key === "/" || (ev.key === "f" && (ev.ctrlKey || ev.metaKey))) && findbox.hidden && document.activeElement !== fq) { ev.preventDefault(); openFind(true); }
  });

  function next() {
    st.pages = metrics().pages;
    if (st.p < st.pages - 1) return go(st.c, st.e, st.p + 1);
    if (st.e < B[st.c].e.length - 1) return go(st.c, st.e + 1, 0);
    if (st.c < B.length - 1) return go(st.c + 1, 0, 0);
  }
  function prev() {
    if (st.p > 0) return go(st.c, st.e, st.p - 1);
    if (st.e > 0) return go(st.c, st.e - 1, "last");
    if (st.c > 0) return go(st.c - 1, B[st.c - 1].e.length - 1, "last");
  }
  $("next").onclick = next; $("prev").onclick = prev;
  // 左滑右滑翻页（手指、触控笔都认，屏幕宽窄都行）：往左滑 = 下一页，往右滑 = 上一页。
  // 拖动时书页跟着轻轻挪一点，松手够远才翻，不够弹回。文字、表格、图片、链接上滑都算（2026-10-27 用户：有内容的地方常常翻不动），
  // 只有真正的按钮（配方卡材料、锅形选择、书签等）上不算；滑过之后紧跟着的那次「点击」吞掉，免得顺手点开链接。
  (function () {
    var area = flow.parentNode.parentNode, sx = 0, sy = 0, lx = 0, ly = 0, st0 = 0, on = false, dir = 0, pid = null, swallow = false;
    function skip(t) { return t.closest("button,input,.findbox"); }
    function reset() { on = false; pid = null; flow.style.transition = ""; flow.style.translate = ""; }
    function decide() {
      var dx = lx - sx, dy = ly - sy, fast = Date.now() - st0 < 600;
      var ok = dir > 0 && Math.abs(dx) > Math.abs(dy) * 1.2 && (Math.abs(dx) > 55 || (fast && Math.abs(dx) > 30));
      reset();
      if (ok) { swallow = true; setTimeout(function () { swallow = false; }, 400); if (dx < 0) next(); else prev(); }
    }
    area.addEventListener("pointerdown", function (ev) {
      if (ev.pointerType === "mouse" || !ev.isPrimary || skip(ev.target)) return;
      on = true; dir = 0; pid = ev.pointerId; sx = lx = ev.clientX; sy = ly = ev.clientY; st0 = Date.now();
      flow.style.transition = "none";
    });
    area.addEventListener("pointermove", function (ev) {
      if (!on || ev.pointerId !== pid) return;
      lx = ev.clientX; ly = ev.clientY;
      var dx = lx - sx, dy = ly - sy;
      if (!dir) {
        if (Math.abs(dx) < 8 && Math.abs(dy) < 8) return;
        dir = Math.abs(dx) > Math.abs(dy) ? 1 : -1;
        if (dir > 0) { try { area.setPointerCapture(pid); } catch (x) {} }   // 认定是横滑后抓住这根手指，滑出书页也不丢
      }
      if (dir < 0) { reset(); return; }   // 竖着划是在滚页面，不管
      ev.preventDefault();
      flow.style.translate = (dx * 0.35) + "px 0";
    });
    area.addEventListener("pointerup", function (ev) { if (on && ev.pointerId === pid) { lx = ev.clientX; ly = ev.clientY; decide(); } });
    // 浏览器有时会把手势「收走」（开始选字、拖图片）：按最后的位置照样判断
    area.addEventListener("pointercancel", function (ev) { if (on && ev.pointerId === pid) decide(); });
    area.addEventListener("click", function (ev) { if (swallow) { ev.preventDefault(); ev.stopPropagation(); swallow = false; } }, true);
    area.addEventListener("dragstart", function (ev) { if (on) ev.preventDefault(); });
  })();
  $("menu").onclick = function () { menu(!$("drawer").classList.contains("open")); };
  $("backdrop").onclick = function () { menu(false); };
  document.addEventListener("keydown", function (ev) {
    if (document.activeElement === fq) return;
    if (ev.key === "ArrowRight" || ev.key === "PageDown") next();
    else if (ev.key === "ArrowLeft" || ev.key === "PageUp") prev();
  });

  // 书里的链接：跳到对应分类 / 条目 / 小节
  flow.addEventListener("click", function (ev) {
    var a = ev.target.closest("a");
    if (!a) return;
    if (a.dataset.go) { ev.preventDefault(); var g = a.dataset.go.split(","); go(+g[0], +g[1], 0, g[2] || null); }
    else if (a.dataset.todo) { ev.preventDefault(); showTip(a, "这一章样稿里还没排", ev); setTimeout(hideTip, 1200); }
    else if (/^https?:/.test(a.getAttribute("href") || "")) a.target = "_blank";
  });

  // 鼠标提示：书签名、物品名（原版深紫色提示框）
  function showTip(el, text, ev) {
    var lines = String(text).split("\n");
    tip.innerHTML = lines[0].replace(/</g, "&lt;") + (lines[1] ? "<small>" + lines.slice(1).join(" ").replace(/</g, "&lt;") + "</small>" : "");
    tip.hidden = false; moveTip(ev);
  }
  function moveTip(ev) {
    var x = ev.clientX + 14, y = ev.clientY - 30, w = tip.offsetWidth;
    if (x + w > innerWidth - 4) x = ev.clientX - w - 14;
    tip.style.left = x + "px"; tip.style.top = Math.max(4, y) + "px";
  }
  function hideTip() { tip.hidden = true; }
  document.addEventListener("mouseover", function (ev) {
    var el = ev.target.closest("[data-tip],[title]");
    if (!el) return hideTip();
    if (el.title) { el.dataset.tip = el.title; el.removeAttribute("title"); }
    showTip(el, el.dataset.tip, ev);
  });
  document.addEventListener("mousemove", function (ev) { if (!tip.hidden) moveTip(ev); });
  document.addEventListener("mouseout", function (ev) { if (!ev.relatedTarget || !ev.relatedTarget.closest || !ev.relatedTarget.closest("[data-tip]")) hideTip(); });
  // 正文里物品小图标（.it）也给提示：取旁边的名字
  // （.it 自带 title=物品 ID，换成「名字 + ID」）
  document.addEventListener("mouseover", function (ev) {
    var it = ev.target.closest(".it");
    if (it && it.dataset.tip && it.dataset.tip.indexOf(":") > 0 && !it.dataset.done) {
      it.dataset.tip = (it.textContent.trim() || it.dataset.tip) + "\n" + it.dataset.tip; it.dataset.done = 1;
      showTip(it, it.dataset.tip, ev);
    }
  });

  var wasMobile = mobile();
  window.addEventListener("resize", function () {
    if (mobile() !== wasMobile) { wasMobile = mobile(); menu(false); drawTabs(); }
    layout(false);
  });
  var h = (location.hash.slice(1) || "0-0-0").split("-").map(Number);
  var c0 = B[h[0]] ? h[0] : 0, e0 = B[c0].e[h[1]] !== undefined ? h[1] : 0;
  go(c0, e0, h[2] || 0);
  booted = true;   // 打开页面那一下不响，之后翻页才响
  // 图片和字体加载完，分栏会变，重新排一次
  window.addEventListener("load", function () { layout(false); });
})();
