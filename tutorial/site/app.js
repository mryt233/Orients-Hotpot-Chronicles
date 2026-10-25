// 作者： 教程站脚本：当前页高亮、手机菜单、全文搜索
(function () {
  var slug = location.pathname.split("/").pop().replace(".html", "") || "index";
  document.querySelectorAll("#nav a").forEach(function (a) { if (a.dataset.slug === slug) a.classList.add("on"); });
  var nav = document.getElementById("nav");
  document.getElementById("menu").onclick = function () { nav.classList.toggle("open"); };
  nav.addEventListener("click", function () { nav.classList.remove("open"); });
  var q = document.getElementById("q"), box = document.getElementById("results"), doc = document.getElementById("doc");
  q.addEventListener("input", function () {
    var s = q.value.trim().toLowerCase();
    if (!s) { box.hidden = true; doc.hidden = false; return; }
    var out = [];
    (window.SEARCH || []).forEach(function (p) {
      var i = p.text.toLowerCase().indexOf(s);
      if (i >= 0 || p.title.toLowerCase().indexOf(s) >= 0) {
        var from = Math.max(0, i - 24), snip = i >= 0 ? p.text.slice(from, i + 70) : "";
        out.push('<a href="' + p.slug + '.html"><b>' + p.title + "</b><small>" + (from ? "…" : "") + snip.replace(/</g, "&lt;") + "…</small></a>");
      }
    });
    box.innerHTML = out.length ? "<b>搜索结果</b>" + out.join("") : "没有找到「" + s.replace(/</g, "&lt;") + "」";
    box.hidden = false; doc.hidden = true;
  });
})();
