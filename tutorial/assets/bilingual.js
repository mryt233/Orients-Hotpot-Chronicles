// 作者：。简中、zh_tw、英文三语选择；图片共享，支持离线与章节定位。
(function () {
  'use strict';
  var pages = JSON.parse(document.getElementById('translations').textContent);
  var images = JSON.parse(document.getElementById('images').textContent);
  var nav = document.getElementById('nav'), main = document.querySelector('main');
  var menu = document.getElementById('menu'), language = document.getElementById('language');
  var q = document.getElementById('q'), top = document.getElementById('top');
  var links = [], targets = [], current, generation = 0;
  var key = 'orients-hotpot-guide-language';
  var labels = {
    'zh-CN': {contents: '目录', search: '搜索教程', hint: '搜索（回车跳到下一处）', top: '↑ 回到顶部', language: '语言选择'},
    'zh-TW': {contents: '目錄', search: '搜尋指南', hint: '搜尋（按 Enter 跳至下一筆）', top: '↑ 回到頂端', language: '語言選擇'},
    en: {contents: 'Contents', search: 'Search the guide', hint: 'Search (Enter for next match)', top: '↑ Back to top', language: 'Select language'}
  };

  function storedLanguage() {
    try { return localStorage.getItem(key); } catch (_) { return null; }
  }
  function spy() {
    var active = 0;
    targets.forEach(function (target, i) {
      if (target && target.getBoundingClientRect().top <= headerHeight() + 12) active = i;
    });
    links.forEach(function (a, i) { a.classList.toggle('on', i === active); });
    var a = links[active];
    if (a && getComputedStyle(nav).display !== 'none') {
      var r = a.getBoundingClientRect(), n = nav.getBoundingClientRect();
      if (r.top < n.top || r.bottom > n.bottom) nav.scrollTop += r.top - n.top - 60;
    }
    top.style.display = window.scrollY > 600 ? 'block' : 'none';
  }
  function headerHeight() {
    var height = document.querySelector('header').getBoundingClientRect().height;
    document.documentElement.style.setProperty('--guide-header-height', height + 'px');
    return height;
  }
  function jump(id) {
    var target = document.getElementById(id);
    if (target) window.scrollTo({top: Math.max(0, window.scrollY + target.getBoundingClientRect().top - headerHeight() - 12), behavior: 'instant'});
    spy();
  }
  function visibleSection() {
    var id = 'page-index';
    main.querySelectorAll('article[id],h2[id]').forEach(function (target) {
      if (target.getBoundingClientRect().top <= headerHeight() + 15) id = target.id;
    });
    return id;
  }
  async function render(locale, anchor) {
    if (!pages[locale]) return;
    var turn = ++generation, page = pages[locale], text = labels[locale];
    current = locale;
    document.documentElement.lang = locale;
    document.title = page.title;
    document.querySelector('.brand').innerHTML = page.brand;
    nav.innerHTML = page.nav;
    main.innerHTML = page.main;
    nav.classList.remove('open');
    menu.setAttribute('aria-expanded', 'false');
    menu.setAttribute('aria-label', text.contents);
    nav.setAttribute('aria-label', text.contents);
    q.value = '';
    q.placeholder = text.hint;
    q.setAttribute('aria-label', text.search);
    top.textContent = text.top;
    language.value = locale;
    language.setAttribute('aria-label', text.language);
    links = Array.from(nav.querySelectorAll('a[data-t]'));
    targets = links.map(function (a) { return document.getElementById(a.dataset.t); });
    var pending = Array.from(main.querySelectorAll('img[data-k]')).map(function (img) {
      img.src = images[img.dataset.k];
      return img.decode ? img.decode().catch(function () {}) : Promise.resolve();
    });
    try { localStorage.setItem(key, locale); } catch (_) { /* 文件模式或隐私设置可能禁用存储。 */ }
    try {
      var url = new URL(location.href);
      url.searchParams.set('lang', locale === 'zh-TW' ? 'zh_tw' : locale.toLowerCase());
      url.hash = anchor ? '#' + anchor : '';
      history.replaceState(null, '', url.href);
    } catch (_) { /* 离线嵌入场景可能不允许改地址。 */ }
    await Promise.all(pending);
    if (turn !== generation) return;
    jump(anchor || 'page-index');
    // 提供明确完成事件，供嵌入页面与验证工具使用。
    document.dispatchEvent(new CustomEvent('guide-language-ready', {detail: {language: locale}}));
  }
  language.onchange = function () { render(language.value, visibleSection()); };
  menu.onclick = function () {
    nav.classList.toggle('open');
    menu.setAttribute('aria-expanded', String(nav.classList.contains('open')));
  };
  nav.addEventListener('click', function (e) {
    var a = e.target.closest('a[href^="#"]');
    if (!a) return;
    nav.classList.remove('open');
    menu.setAttribute('aria-expanded', 'false');
  });
  window.addEventListener('scroll', spy, {passive: true});
  window.addEventListener('resize', spy);
  window.addEventListener('hashchange', function () {
    try { jump(decodeURIComponent(location.hash.slice(1))); } catch (_) {}
  });
  top.onclick = function () { window.scrollTo({top: 0, behavior: 'smooth'}); };
  q.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && q.value.trim() && window.find) window.find(q.value.trim(), false, false, true);
  });
  function normalizeLanguage(value) {
    value = String(value || '').toLowerCase().replace(/_/g, '-');
    if (/^zh-(tw|hk|mo|hant)(-|$)/.test(value)) return 'zh-TW';
    if (/^zh($|-)/.test(value)) return 'zh-CN';
    if (/^en($|-)/.test(value)) return 'en';
    return null;
  }
  var requested = new URLSearchParams(location.search).get('lang');
  var preferred = normalizeLanguage(requested) || normalizeLanguage(storedLanguage()) || normalizeLanguage(navigator.language) || 'en';
  var anchor = '';
  try { anchor = decodeURIComponent(location.hash.slice(1)); } catch (_) {}
  render(preferred, anchor);
})();
