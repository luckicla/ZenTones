/* Zentones · estilo Pixel · motion.js
 * Movimiento Material 3 Expressive: muelles con ligero rebote, ripple, fade-through
 * entre pestañas, indicador que se estira, interruptor con pulgar que crece,
 * aparición escalonada, móvil que suena y reproducción de tonos.
 * Todo es opcional: si un elemento no existe, simplemente no se engancha.
 */
(function () {
  "use strict";

  var doc = document;
  var root = doc.documentElement;
  var mq = window.matchMedia ? window.matchMedia("(prefers-reduced-motion: reduce)") : null;
  function reduced() { return !!(mq && mq.matches); }

  root.classList.add("m3-js");

  /* Curvas de muelle (amortiguamiento 0.55 = rápido con rebote; 0.7 = espacial por defecto) */
  var SPRING_FAST = "linear(0, 0.036, 0.128, 0.255, 0.398, 0.545, 0.683, 0.806, 0.909, 0.991, 1.052, 1.093, 1.116, 1.126, 1.124, 1.114, 1.1, 1.082, 1.063, 1.045, 1.029, 1.015, 1.004, 0.996, 0.99, 0.986, 0.984, 0.984, 0.985, 0.987, 0.989, 0.991, 0.994, 0.996, 0.998, 0.999, 1)";
  var SPRING = "linear(0, 0.022, 0.08, 0.161, 0.255, 0.356, 0.457, 0.554, 0.644, 0.725, 0.796, 0.856, 0.907, 0.948, 0.98, 1.004, 1.022, 1.034, 1.041, 1.045, 1.046, 1.045, 1.042, 1.038, 1.033, 1.029, 1.024, 1.02, 1.016, 1.012, 1.009, 1.006, 1.004, 1.002, 1.001, 1)";
  var EMPH_DECEL = "cubic-bezier(0.05, 0.7, 0.1, 1)";
  var EMPH_ACCEL = "cubic-bezier(0.3, 0, 0.8, 0.15)";
  try {
    if (!(window.CSS && CSS.supports && CSS.supports("transition-timing-function", SPRING))) {
      SPRING_FAST = "cubic-bezier(0.34, 1.56, 0.64, 1)";
      SPRING = "cubic-bezier(0.34, 1.3, 0.64, 1)";
    }
  } catch (e) { /* sin CSS.supports */ }

  function $all(sel, ctx) { return Array.prototype.slice.call((ctx || doc).querySelectorAll(sel)); }
  function canAnimate(el) { return !reduced() && el && typeof el.animate === "function"; }
  function anim(el, frames, opts) {
    if (!canAnimate(el)) return null;
    try { return el.animate(frames, opts); } catch (e) { return null; }
  }

  /* ------------------------------------------------------------------ aparición escalonada */
  function initEnter() {
    var els = $all('[data-anim="enter"]');
    if (!els.length) return;
    // índice de escalonado entre hermanos que también entran
    els.forEach(function (el) {
      var parent = el.parentElement;
      if (!parent) return;
      var sibs = Array.prototype.filter.call(parent.children, function (c) {
        return c.getAttribute("data-anim") === "enter";
      });
      var i = sibs.indexOf(el);
      el.style.setProperty("--m3-i", String(Math.min(Math.max(i, 0), 8)));
    });
    if (!("IntersectionObserver" in window) || reduced()) {
      els.forEach(function (el) { el.classList.add("is-in"); });
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) {
          en.target.classList.add("is-in");
          io.unobserve(en.target);
        }
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.12 });
    els.forEach(function (el) {
      // lo que ya está en pantalla al cargar aparece enseguida (sin esperar al observador)
      var r = el.getBoundingClientRect();
      if (r.top < window.innerHeight * 0.92 && r.bottom > 0) {
        requestAnimationFrame(function () { el.classList.add("is-in"); });
      } else {
        io.observe(el);
      }
    });
    // red de seguridad: nada se queda invisible
    window.addEventListener("load", function () {
      setTimeout(function () {
        els.forEach(function (el) {
          var r = el.getBoundingClientRect();
          if (r.bottom < 0) el.classList.add("is-in");
        });
      }, 1200);
    });
  }

  /* ------------------------------------------------------------------ ripple */
  function ensureHost(el) {
    var host = null;
    for (var c = el.firstElementChild; c; c = c.nextElementSibling) {
      if (c.classList && c.classList.contains("m3-ripple-host")) { host = c; break; }
    }
    if (host) return host;
    var tag = el.tagName;
    if (tag === "INPUT" || tag === "IMG" || tag === "SELECT" || tag === "TEXTAREA") return null;
    if (getComputedStyle(el).position === "static") el.style.position = "relative";
    host = doc.createElement("span");
    host.className = "m3-ripple-host";
    host.setAttribute("aria-hidden", "true");
    el.appendChild(host);
    return host;
  }

  function ripple(el, x, y) {
    if (reduced()) return;
    var host = ensureHost(el);
    if (!host) return;
    var rect = el.getBoundingClientRect();
    if (x == null) { x = rect.width / 2; y = rect.height / 2; }
    var dx = Math.max(x, rect.width - x), dy = Math.max(y, rect.height - y);
    var size = Math.sqrt(dx * dx + dy * dy) * 2;
    var wave = doc.createElement("span");
    wave.className = "m3-ripple";
    wave.style.width = wave.style.height = size + "px";
    wave.style.left = x - size / 2 + "px";
    wave.style.top = y - size / 2 + "px";
    host.appendChild(wave);
    var grow = anim(wave, [{ transform: "scale(0.1)", opacity: 0.32 }, { transform: "scale(1)", opacity: 0.22 }],
      { duration: 450, easing: EMPH_DECEL, fill: "forwards" });
    var done = false;
    function release() {
      if (done) return; done = true;
      var fade = anim(wave, [{ opacity: 0.22 }, { opacity: 0 }], { duration: 300, delay: 90, easing: "linear", fill: "forwards" });
      var rm = function () { if (wave.parentNode) wave.parentNode.removeChild(wave); };
      if (fade) fade.onfinish = rm; else rm();
    }
    if (!grow) { release(); return; }
    var end = function () { setTimeout(release, 60); cleanup(); };
    function cleanup() {
      window.removeEventListener("pointerup", end);
      window.removeEventListener("pointercancel", end);
    }
    window.addEventListener("pointerup", end);
    window.addEventListener("pointercancel", end);
    setTimeout(release, 900); // por si nunca llega el pointerup
  }

  function initPress() {
    $all('[data-anim="press"]').forEach(ensureHost);
    doc.addEventListener("pointerdown", function (ev) {
      if (ev.button && ev.button !== 0) return;
      var el = ev.target.closest && ev.target.closest('[data-anim="press"], .phone [data-tab]');
      if (!el || el.disabled || el.getAttribute("aria-disabled") === "true") return;
      var target = el;
      // en la barra de navegación, el ripple va dentro del indicador si existe
      if (el.hasAttribute("data-tab") && !el.hasAttribute("data-anim")) {
        target = indicatorOf(el) || el;
      }
      var r = target.getBoundingClientRect();
      ripple(target, ev.clientX - r.left, ev.clientY - r.top);
      el.classList.add("is-pressed");
    });
    function unpress() { $all(".is-pressed").forEach(function (e) { e.classList.remove("is-pressed"); }); }
    window.addEventListener("pointerup", unpress);
    window.addEventListener("pointercancel", unpress);
    doc.addEventListener("keydown", function (ev) {
      if (ev.key !== "Enter" && ev.key !== " ") return;
      var el = ev.target.closest && ev.target.closest('[data-anim="press"]');
      if (el && !ev.repeat) ripple(el, null, null);
    });
  }

  /* ------------------------------------------------------------------ pestañas del móvil */
  var TAB_ORDER = ["tonos", "tienda", "apps", "ajustes"];

  function indicatorOf(tab) {
    return tab.querySelector(".z-tab__pill, [data-indicator], .indicator, .nav-indicator, .nav-pill, .pill, .tab-pill, .icon-wrap, .nav-icon, .tab-icon");
  }

  function scopeOf(tab) {
    return tab.closest(".phone") || doc.querySelector(".phone") || doc;
  }

  function selectTab(tab, focus) {
    var scope = scopeOf(tab);
    var name = tab.getAttribute("data-tab");
    var tabs = $all("[data-tab]", scope);
    var screens = $all("[data-screen]", scope);
    var next = screens.filter(function (s) { return s.getAttribute("data-screen") === name; })[0];
    var prev = screens.filter(function (s) { return !s.hidden && s !== next; })[0];

    tabs.forEach(function (t) {
      var on = t.getAttribute("data-tab") === name;
      if (t.getAttribute("role") === "tab" || t.hasAttribute("aria-selected")) {
        t.setAttribute("aria-selected", on ? "true" : "false");
        if (t.getAttribute("role") === "tab") t.setAttribute("tabindex", on ? "0" : "-1");
      }
      t.classList.toggle("is-active", on);
    });
    // también fuera del móvil (p. ej. botones de sección que manejan el móvil)
    $all("[data-tab]").forEach(function (t) { if (tabs.indexOf(t) < 0) t.classList.remove("is-active"); });
    $all('[data-tab="' + name + '"]').forEach(function (t) { if (tabs.indexOf(t) < 0) t.classList.add("is-active"); });

    if (focus) tab.focus();
    if (!next || next === prev) return;

    // el indicador se estira por CSS (transición de width con muelle en motion.css)
    var oldIdx = prev ? TAB_ORDER.indexOf(prev.getAttribute("data-screen")) : -1;
    var newIdx = TAB_ORDER.indexOf(name);
    var dir = oldIdx < 0 || newIdx < 0 ? 0 : (newIdx > oldIdx ? 1 : -1);

    var token = (selectTab._t = (selectTab._t || 0) + 1);
    next.__m3token = token;

    function showNext() {
      if (next.__m3token !== token) return;
      screens.forEach(function (s) { if (s !== next) s.hidden = true; });
      next.hidden = false;
      // fade-through con un toque de eje compartido horizontal
      anim(next, [
        { opacity: 0, transform: "translateX(" + 18 * dir + "px) scale(0.94)" },
        { opacity: 1, transform: "none" }
      ], { duration: 380, easing: EMPH_DECEL });
      var kids = Array.prototype.slice.call(next.children, 0, 8);
      kids.forEach(function (k, i) {
        anim(k, [{ opacity: 0, transform: "translateY(14px)" }, { opacity: 1, transform: "none" }],
          { duration: 460, delay: 40 + i * 35, easing: SPRING, fill: "backwards" });
      });
    }

    if (prev && canAnimate(prev)) {
      var out = anim(prev, [
        { opacity: 1, transform: "none" },
        { opacity: 0, transform: "translateX(" + -12 * dir + "px) scale(0.98)" }
      ], { duration: 110, easing: EMPH_ACCEL, fill: "forwards" });
      if (out) {
        out.onfinish = function () {
          if (next.__m3token === token) { prev.hidden = true; }
          try { out.cancel(); } catch (e) {}
          showNext();
        };
        return;
      }
    }
    showNext();
  }

  function initTabs() {
    var tabs = $all("[data-tab]");
    if (!tabs.length) return;
    tabs.forEach(function (t) {
      t.addEventListener("click", function (ev) {
        if (t.tagName === "A") ev.preventDefault();
        selectTab(t, false);
      });
    });
    // teclado para role="tab": flechas, Inicio, Fin
    doc.addEventListener("keydown", function (ev) {
      var t = ev.target.closest && ev.target.closest('[role="tab"][data-tab]');
      if (!t) return;
      var list = $all('[role="tab"][data-tab]', t.closest('[role="tablist"]') || scopeOf(t));
      var i = list.indexOf(t), j = -1;
      if (ev.key === "ArrowRight" || ev.key === "ArrowDown") j = (i + 1) % list.length;
      else if (ev.key === "ArrowLeft" || ev.key === "ArrowUp") j = (i - 1 + list.length) % list.length;
      else if (ev.key === "Home") j = 0;
      else if (ev.key === "End") j = list.length - 1;
      if (j < 0) return;
      ev.preventDefault();
      selectTab(list[j], true);
    });
  }

  /* ------------------------------------------------------------------ interruptores */
  function initSwitches() {
    function toggle(sw) {
      if (sw.disabled || sw.getAttribute("aria-disabled") === "true") return;
      var on = sw.getAttribute("aria-checked") !== "true";
      sw.setAttribute("aria-checked", on ? "true" : "false");
      sw.classList.remove("m3-sw-bump");
      void sw.offsetWidth; // reinicia la animación
      if (!reduced()) sw.classList.add("m3-sw-bump");
      try { sw.dispatchEvent(new CustomEvent("m3:switch", { bubbles: true, detail: { checked: on } })); } catch (e) {}
    }
    doc.addEventListener("click", function (ev) {
      var sw = ev.target.closest && ev.target.closest('[role="switch"][aria-checked]');
      if (!sw) return;
      // si es un <input type=checkbox role=switch>, el navegador ya cambia el estado
      if (sw.tagName === "INPUT") { sw.setAttribute("aria-checked", sw.checked ? "true" : "false"); return; }
      if (sw.tagName === "A") ev.preventDefault();
      toggle(sw);
    });
    doc.addEventListener("keydown", function (ev) {
      var sw = ev.target.closest && ev.target.closest('[role="switch"][aria-checked]');
      if (!sw || sw.tagName === "BUTTON" || sw.tagName === "INPUT") return;
      if (ev.key === " " || ev.key === "Enter") { ev.preventDefault(); toggle(sw); }
    });
    $all('[role="switch"][aria-checked]').forEach(function (sw) {
      if (sw.tagName !== "BUTTON" && sw.tagName !== "INPUT" && !sw.hasAttribute("tabindex")) sw.setAttribute("tabindex", "0");
      sw.addEventListener("animationend", function () { sw.classList.remove("m3-sw-bump"); });
    });
  }

  /* ------------------------------------------------------------------ reproducir tonos */
  var current = null; // { audio, btn }

  function setPlaying(btn, on) {
    $all('[data-play="' + (btn.getAttribute("data-play") || "").replace(/"/g, '\\"') + '"]').forEach(function (b) {
      b.setAttribute("aria-pressed", on ? "true" : "false");
      b.classList.toggle("is-playing", on);
      var row = b.closest("li, .row, .item, .card, .tone");
      if (row) row.classList.toggle("is-playing", on);
    });
    root.classList.toggle("m3-playing", on);
  }

  function stop() {
    if (!current) return;
    try { current.audio.pause(); current.audio.currentTime = 0; } catch (e) {}
    setPlaying(current.btn, false);
    current = null;
  }

  function initPlay() {
    var btns = $all("[data-play]");
    btns.forEach(function (b) { if (!b.hasAttribute("aria-pressed")) b.setAttribute("aria-pressed", "false"); });
    doc.addEventListener("click", function (ev) {
      var b = ev.target.closest && ev.target.closest("[data-play]");
      if (!b) return;
      ev.preventDefault();
      var src = b.getAttribute("data-play");
      if (current && current.src === src) { stop(); return; }
      stop();
      if (!src || typeof Audio === "undefined") return;
      var audio = new Audio(src);
      audio.preload = "auto";
      current = { audio: audio, btn: b, src: src };
      setPlaying(b, true);
      var me = current;
      audio.addEventListener("ended", function () { if (current === me) stop(); });
      audio.addEventListener("error", function () { if (current === me) stop(); });
      var p = audio.play();
      if (p && p.catch) p.catch(function () { if (current === me) stop(); });
    });
    doc.addEventListener("keydown", function (ev) {
      if (ev.key === "Escape" || ev.key === "Esc") stop();
    });
    doc.addEventListener("visibilitychange", function () { if (doc.hidden) stop(); });
  }

  /* ------------------------------------------------------------------ onda del editor y móvil sonando */
  function initLive() {
    var waves = $all(".wave");
    waves.forEach(function (w) {
      var holder = w.querySelector(".wave__bars, .bars") || w;
      var bars = w.tagName.toLowerCase() === "svg" ? $all("rect, line", w)
        : Array.prototype.filter.call(holder.children, function (c) {
          return !/wave__(dim|handle)|m3-/.test(c.className || "");
        });
      if (bars.length > 3) {
        w.classList.add("m3-wave-bars");
        bars.forEach(function (b, i) {
          b.classList.add("m3-bar");
          b.style.setProperty("--m3-b", String(i));
          b.style.setProperty("--m3-d", String(((i * 37) % 11) / 10));
        });
      }
      if (!w.querySelector(".m3-playhead") && w.tagName.toLowerCase() !== "svg") {
        var ph = doc.createElement("span");
        ph.className = "m3-playhead";
        ph.setAttribute("aria-hidden", "true");
        w.appendChild(ph);
      }
    });
    $all("[data-ring]").forEach(function (p) {
      if (p.querySelector(":scope > .m3-rings")) return;
      var r = doc.createElement("span");
      r.className = "m3-rings";
      r.setAttribute("aria-hidden", "true");
      p.insertBefore(r, p.firstChild);
    });
    var lives = waves.concat($all("[data-ring]"));
    if (!lives.length) return;
    if (!("IntersectionObserver" in window)) {
      lives.forEach(function (el) { el.classList.add("is-live"); });
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) { en.target.classList.toggle("is-live", en.isIntersecting); });
    }, { threshold: 0.15 });
    lives.forEach(function (el) { io.observe(el); });
  }

  /* ------------------------------------------------------------------ arranque */
  function start() {
    initEnter();
    initPress();
    initTabs();
    initSwitches();
    initPlay();
    initLive();
  }
  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", start);
  else start();

  if (mq) {
    var onChange = function () {
      if (reduced()) $all('[data-anim="enter"]').forEach(function (el) { el.classList.add("is-in"); });
    };
    if (mq.addEventListener) mq.addEventListener("change", onChange);
    else if (mq.addListener) mq.addListener(onChange);
  }
})();
