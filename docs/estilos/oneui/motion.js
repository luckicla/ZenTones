/* Zentones · estilo One UI · movimiento
   Pestañas del móvil, interruptores, reproducción de tonos (Esc para parar),
   apariciones con IntersectionObserver, pulsación atenuada, cabecera alta que
   se encoge al hacer scroll, barra inferior flotante y móvil "sonando".
   Todo es opcional: si un elemento no existe, simplemente no se engancha. */
(function () {
  "use strict";

  var doc = document;
  var root = doc.documentElement;
  var mqReduce = window.matchMedia ? window.matchMedia("(prefers-reduced-motion: reduce)") : null;
  function reduced() { return !!(mqReduce && mqReduce.matches); }
  function $all(sel, ctx) { return Array.prototype.slice.call((ctx || doc).querySelectorAll(sel)); }
  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
  function once(el, ev, fn, ms) {
    var done = false;
    function run() { if (done) return; done = true; el.removeEventListener(ev, run); fn(); }
    el.addEventListener(ev, run);
    setTimeout(run, ms);
  }

  root.classList.add("oui-js");

  /* ---------- 1. Aparición escalonada (data-anim="enter") ---------- */
  function setupEnter() {
    var items = $all('[data-anim="enter"]');
    if (!items.length) return;
    // Retardo escalonado entre hermanos que entran juntos (tarjetas en rejilla, listas…)
    var groups = new Map();
    items.forEach(function (el) {
      var p = el.parentElement;
      var n = groups.get(p) || 0;
      el.style.setProperty("--oui-delay", Math.min(n, 7) * 70 + "ms");
      groups.set(p, n + 1);
    });
    function show(el) { el.classList.add("is-in"); }
    if (reduced() || !("IntersectionObserver" in window)) { items.forEach(show); return; }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) { show(e.target); io.unobserve(e.target); }
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.08 });
    items.forEach(function (el) { io.observe(el); });
    // Imprimir o anclas directas: no dejar nada oculto
    window.addEventListener("beforeprint", function () { items.forEach(show); });
  }

  /* ---------- 2. Pulsación One UI: atenuado y escala ligera ---------- */
  function setupPress() {
    var sel = '[data-anim="press"], .phone [data-tab], [data-play], [role="switch"]';
    var timers = new WeakMap();
    function down(el) {
      clearTimeout(timers.get(el));
      el.classList.add("is-pressed");
      el.__ouiDown = Date.now();
    }
    function up(el) {
      // La atenuación se ve al menos 140 ms aunque el toque sea muy corto
      var wait = Math.max(0, 140 - (Date.now() - (el.__ouiDown || 0)));
      timers.set(el, setTimeout(function () { el.classList.remove("is-pressed"); }, wait));
    }
    doc.addEventListener("pointerdown", function (e) {
      if (e.button !== undefined && e.button !== 0) return;
      var el = e.target.closest && e.target.closest(sel);
      if (!el) return;
      down(el);
      function end() {
        up(el);
        doc.removeEventListener("pointerup", end);
        doc.removeEventListener("pointercancel", end);
      }
      doc.addEventListener("pointerup", end);
      doc.addEventListener("pointercancel", end);
    }, { passive: true });
    doc.addEventListener("keydown", function (e) {
      if (e.key !== "Enter" && e.key !== " ") return;
      var el = e.target.closest && e.target.closest(sel);
      if (el && !e.repeat) down(el);
    });
    doc.addEventListener("keyup", function (e) {
      if (e.key !== "Enter" && e.key !== " ") return;
      var el = e.target.closest && e.target.closest(sel);
      if (el) up(el);
    });
  }

  /* ---------- 3. Pestañas del móvil ---------- */
  var ORDER = ["tonos", "tienda", "apps", "ajustes"];
  function setupTabs(phone) {
    var tabs = $all("[data-tab]", phone);
    var screens = $all("[data-screen]", phone);
    if (!tabs.length || !screens.length) return;

    var bar = tabs[0].closest('[role="tablist"]') || tabs[0].parentElement;
    if (bar && bar !== phone) bar.classList.add("oui-navbar");

    function current() {
      var s = screens.filter(function (x) { return !x.hidden; })[0];
      return s ? s.getAttribute("data-screen") : null;
    }

    function syncTabs(name) {
      tabs.forEach(function (t) {
        var on = t.getAttribute("data-tab") === name;
        t.setAttribute("aria-selected", on ? "true" : "false");
        if (t.getAttribute("role") === "tab") t.setAttribute("tabindex", on ? "0" : "-1");
        t.classList.toggle("is-active", on);
      });
    }

    function go(name, focusTab) {
      var from = current();
      var target = screens.filter(function (s) { return s.getAttribute("data-screen") === name; })[0];
      if (!target) return;
      syncTabs(name);
      if (focusTab) {
        var ft = tabs.filter(function (t) { return t.getAttribute("data-tab") === name; })[0];
        if (ft) ft.focus();
      }
      if (from === name) return;
      var dir = ORDER.indexOf(name) >= ORDER.indexOf(from) ? 1 : -1;
      screens.forEach(function (s) {
        if (s === target) return;
        s.hidden = true;
        s.classList.remove("oui-screen-in");
      });
      target.hidden = false;
      // Vuelve arriba con la cabecera grande desplegada
      var sc = scrollerOf(target, phone);
      if (sc) sc.scrollTop = 0;
      updateCollapse(target, phone);
      if (!reduced()) {
        target.style.setProperty("--oui-dir", dir);
        target.classList.remove("oui-screen-in");
        void target.offsetWidth; // reinicia la animación
        target.classList.add("oui-screen-in");
        once(target, "animationend", function () { target.classList.remove("oui-screen-in"); }, 600);
      }
    }

    tabs.forEach(function (t) {
      t.addEventListener("click", function (e) {
        if (t.tagName === "A") e.preventDefault();
        go(t.getAttribute("data-tab"), false);
      });
      t.addEventListener("keydown", function (e) {
        var i = tabs.indexOf(t), n = tabs.length, j = -1;
        if (e.key === "ArrowRight") j = (i + 1) % n;
        else if (e.key === "ArrowLeft") j = (i - 1 + n) % n;
        else if (e.key === "Home") j = 0;
        else if (e.key === "End") j = n - 1;
        if (j < 0) return;
        e.preventDefault();
        go(tabs[j].getAttribute("data-tab"), true);
      });
    });

    // Estado inicial coherente con la pantalla visible sin JS
    var start = current() || screens[0].getAttribute("data-screen");
    screens.forEach(function (s) { s.hidden = s.getAttribute("data-screen") !== start; });
    syncTabs(start);

    setupCollapse(phone, screens);
    setupNavReveal(phone, bar);
  }

  /* ---------- 4. Cabecera alta que se encoge al hacer scroll ---------- */
  var HEADER_SEL = "[data-header], .app-head, .oui-header, .screen-header, .app-header, .big-header, .hdr, header";
  function isScrollable(el) {
    if (!el || el.nodeType !== 1) return false;
    var oy = getComputedStyle(el).overflowY;
    return (oy === "auto" || oy === "scroll") && el.scrollHeight > el.clientHeight + 4;
  }
  function scrollerOf(screen, phone) {
    var el = screen;
    while (el && el !== phone.parentElement) {
      if (isScrollable(el)) return el;
      el = el.parentElement;
    }
    // ¿algún hijo del screen hace scroll?
    var inner = $all("*", screen).filter(isScrollable)[0];
    return inner || null;
  }
  function headerOf(screen) {
    var h = screen.querySelector(HEADER_SEL);
    if (!h) {
      var t = screen.querySelector("h1, h2, h3");
      h = t ? (t.parentElement !== screen ? t.parentElement : t) : null;
    }
    return h;
  }
  function titleOf(screen) {
    var h = headerOf(screen);
    var t = h && (h.matches("h1,h2,h3") ? h : h.querySelector("h1, h2, h3, strong, b, p"));
    return (t || h) ? (t || h).textContent.trim().split("\n")[0] : "";
  }
  function updateCollapse(screen, phone) {
    var sc = scrollerOf(screen, phone);
    var h = headerOf(screen);
    var p = 0;
    if (sc && h) {
      var max = sc.scrollHeight - sc.clientHeight;
      var range = Math.max(48, Math.min(h.offsetHeight * 0.75, max));
      p = clamp(sc.scrollTop / range, 0, 1);
    }
    var v = p.toFixed(3);
    screen.style.setProperty("--oui-collapse", v);
    phone.style.setProperty("--oui-collapse", v);
    if (h) {
      h.classList.add("oui-bighead");
      h.style.setProperty("--oui-collapse", v);
    }
    var mini = phone.querySelector(".oui-minibar");
    if (mini) {
      var title = titleOf(screen);
      var span = mini.firstChild;
      if (span && span.textContent !== title) span.textContent = title;
    }
    phone.classList.toggle("is-collapsed", p > 0.92);
  }
  function setupCollapse(phone, screens) {
    var any = false;
    screens.forEach(function (screen) {
      var wasHidden = screen.hidden;
      screen.hidden = false; // medir
      var sc = scrollerOf(screen, phone);
      screen.hidden = wasHidden;
      if (!sc || sc.__oui) return;
      sc.__oui = true;
      any = true;
      var raf = 0;
      sc.addEventListener("scroll", function () {
        if (raf) return;
        raf = requestAnimationFrame(function () {
          raf = 0;
          var vis = screens.filter(function (s) { return !s.hidden; })[0];
          if (vis) updateCollapse(vis, phone);
        });
      }, { passive: true });
    });
    // Barra compacta con el título (como el Frame de One UI al plegarse)
    if (any && !phone.querySelector(".oui-minibar")) {
      var host = screens[0].parentElement;
      if (host) {
        var mini = doc.createElement("div");
        mini.className = "oui-minibar";
        mini.setAttribute("aria-hidden", "true");
        mini.appendChild(doc.createElement("span"));
        var pos = getComputedStyle(host).position;
        if (pos === "static") host.style.position = "relative";
        host.insertBefore(mini, host.firstChild);
      }
    }
    var vis = screens.filter(function (s) { return !s.hidden; })[0];
    if (vis) updateCollapse(vis, phone);
  }

  /* ---------- 5. Barra inferior flotante que aparece ---------- */
  function setupNavReveal(phone, bar) {
    if (!bar || bar === phone) return;
    if (reduced() || !("IntersectionObserver" in window)) { bar.classList.add("is-shown"); return; }
    bar.classList.add("oui-nav-wait");
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        setTimeout(function () { bar.classList.add("is-shown"); }, 380);
        io.disconnect();
      });
    }, { threshold: 0.25 });
    io.observe(phone);
    setTimeout(function () { bar.classList.add("is-shown"); }, 4000); // red de seguridad
  }

  /* ---------- 6. Interruptores ---------- */
  function setupSwitches() {
    function toggle(sw) {
      if (sw.getAttribute("aria-disabled") === "true" || sw.disabled) return;
      var on = sw.getAttribute("aria-checked") !== "true";
      sw.setAttribute("aria-checked", on ? "true" : "false");
      sw.classList.remove("oui-sw-on", "oui-sw-off");
      void sw.offsetWidth;
      sw.classList.add(on ? "oui-sw-on" : "oui-sw-off");
      var input = sw.querySelector('input[type="checkbox"]');
      if (input) input.checked = on;
      sw.dispatchEvent(new CustomEvent("oui:toggle", { bubbles: true, detail: { checked: on } }));
    }
    doc.addEventListener("click", function (e) {
      var sw = e.target.closest && e.target.closest('[role="switch"][aria-checked]');
      if (!sw) return;
      if (sw.tagName === "A") e.preventDefault();
      toggle(sw);
    });
    doc.addEventListener("keydown", function (e) {
      var sw = e.target.closest && e.target.closest('[role="switch"][aria-checked]');
      if (!sw || sw.tagName === "BUTTON" || sw.tagName === "INPUT") return; // los botones ya generan click
      if (e.key === " " || e.key === "Enter") { e.preventDefault(); toggle(sw); }
    });
    $all('[role="switch"][aria-checked]').forEach(function (sw) {
      if (!sw.hasAttribute("tabindex") && !/^(BUTTON|INPUT|A)$/.test(sw.tagName)) sw.setAttribute("tabindex", "0");
    });
  }

  /* ---------- 7. Reproducir tonos ---------- */
  function setupPlay() {
    var btns = $all("[data-play]");
    if (!btns.length) return;
    var audio = null, owner = null;
    btns.forEach(function (b) { if (!b.hasAttribute("aria-pressed")) b.setAttribute("aria-pressed", "false"); });

    function setState(btn, on) {
      if (!btn) return;
      btn.setAttribute("aria-pressed", on ? "true" : "false");
      btn.classList.toggle("is-playing", on);
      var row = btn.closest("li, [data-anim], .card, .item, .row");
      if (row && row !== btn) row.classList.toggle("oui-row-playing", on);
      root.classList.toggle("oui-playing", on);
      var scope = btn.closest(".editor, section") || doc;
      $all(".wave", scope).forEach(function (w) { w.classList.toggle("is-playing", on); });
      if (!on) $all(".wave.is-playing").forEach(function (w) { w.classList.remove("is-playing"); });
    }
    function stop() {
      if (audio) { try { audio.pause(); audio.currentTime = 0; } catch (e) {} }
      setState(owner, false);
      audio = null; owner = null;
    }
    function play(btn) {
      var src = btn.getAttribute("data-play");
      if (!src) return;
      if (owner === btn) { stop(); return; }
      stop();
      var a = new Audio(src);
      a.preload = "auto";
      audio = a; owner = btn;
      setState(btn, true);
      a.addEventListener("ended", function () { if (audio === a) stop(); });
      a.addEventListener("error", function () { if (audio === a) stop(); });
      var p = a.play();
      if (p && p.catch) p.catch(function () { if (audio === a) stop(); });
    }
    doc.addEventListener("click", function (e) {
      var b = e.target.closest && e.target.closest("[data-play]");
      if (!b) return;
      e.preventDefault();
      play(b);
    });
    doc.addEventListener("keydown", function (e) {
      if (e.key === "Escape" || e.key === "Esc") { if (owner) stop(); }
    });
    doc.addEventListener("visibilitychange", function () { if (doc.hidden && owner) stop(); });
  }

  /* ---------- 8. Onda del editor: entra creciendo, baila al sonar y se puede recortar ---------- */
  function setupWave() {
    $all(".wave").forEach(function (w) {
      var host = w.querySelector(".bars") || w;
      var kids = Array.prototype.slice.call(host.children);
      kids.forEach(function (k, i) { k.style.setProperty("--i", i); });
      host.classList.add("oui-wave-bars");
      w.classList.add("oui-wave");
      setupTrim(w);
      if (reduced() || !("IntersectionObserver" in window)) { w.classList.add("is-in"); return; }
      var io = new IntersectionObserver(function (es) {
        es.forEach(function (e) { if (e.isIntersecting) { w.classList.add("is-in"); io.disconnect(); } });
      }, { threshold: 0.3 });
      io.observe(w);
    });
  }
  // Asas de recorte: --a y --b en porcentaje (si el diseño las usa)
  function setupTrim(w) {
    var cs = getComputedStyle(w);
    var a0 = parseFloat(w.style.getPropertyValue("--a") || cs.getPropertyValue("--a"));
    var b0 = parseFloat(w.style.getPropertyValue("--b") || cs.getPropertyValue("--b"));
    if (isNaN(a0) || isNaN(b0)) return;
    var st = { a: a0, b: b0 }, drag = null;
    function pct(e) {
      var r = w.getBoundingClientRect();
      return clamp((e.clientX - r.left) / r.width * 100, 0, 100);
    }
    function apply() {
      w.style.setProperty("--a", st.a.toFixed(1) + "%");
      w.style.setProperty("--b", st.b.toFixed(1) + "%");
    }
    function set(which, v) {
      if (which === "a") st.a = clamp(v, 0, st.b - 6);
      else st.b = clamp(v, st.a + 6, 100);
      apply();
    }
    w.addEventListener("pointerdown", function (e) {
      var x = pct(e);
      drag = Math.abs(x - st.a) <= Math.abs(x - st.b) ? "a" : "b";
      w.classList.add("is-dragging");
      if (w.setPointerCapture) try { w.setPointerCapture(e.pointerId); } catch (er) {}
      set(drag, x);
    });
    w.addEventListener("pointermove", function (e) { if (drag) set(drag, pct(e)); });
    function end() { drag = null; w.classList.remove("is-dragging"); }
    w.addEventListener("pointerup", end);
    w.addEventListener("pointercancel", end);
    w.style.touchAction = "pan-y";
  }

  /* ---------- 9. Móvil sonando: solo se anima cuando se ve ---------- */
  function setupRing() {
    var rings = $all("[data-ring]");
    if (!rings.length) return;
    rings.forEach(function (r) { r.classList.add("oui-ring"); });
    if (!("IntersectionObserver" in window)) { rings.forEach(function (r) { r.classList.add("is-visible"); }); return; }
    var io = new IntersectionObserver(function (es) {
      es.forEach(function (e) { e.target.classList.toggle("is-visible", e.isIntersecting); });
    }, { threshold: 0.15 });
    rings.forEach(function (r) { io.observe(r); });
  }

  /* ---------- 10. Barra superior: estado "con scroll" ---------- */
  function setupScrolled() {
    var raf = 0;
    function upd() { raf = 0; root.classList.toggle("oui-scrolled", window.scrollY > 8); }
    window.addEventListener("scroll", function () { if (!raf) raf = requestAnimationFrame(upd); }, { passive: true });
    upd();
  }

  function init() {
    setupEnter();
    setupPress();
    $all(".phone").forEach(setupTabs);
    setupSwitches();
    setupPlay();
    setupWave();
    setupRing();
    setupScrolled();
    if (mqReduce && mqReduce.addEventListener) {
      mqReduce.addEventListener("change", function () {
        if (reduced()) $all('[data-anim="enter"], .oui-navbar, .wave').forEach(function (el) { el.classList.add("is-in", "is-shown"); });
      });
    }
  }

  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", init);
  else init();
})();
