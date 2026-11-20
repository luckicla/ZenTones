/* ==========================================================================
   Zentones · estilo HyperOS · motion.js
   - Aparición con IntersectionObserver (escala desde 0.96 con muelle)
   - Rebote/escala al pulsar
   - Pestañas del móvil con deslizamiento lateral y profundidad
   - Interruptores con deslizamiento elástico
   - Reproducir tonos (uno a la vez, Esc para parar)
   - Onda del editor, cristal que aparece y móvil del hero sonando
   Todo es opcional: si un elemento no existe, simplemente no se engancha.
   ========================================================================== */
(function () {
  "use strict";

  var doc = document;
  var root = doc.documentElement;
  var mq = window.matchMedia ? window.matchMedia("(prefers-reduced-motion: reduce)") : null;
  var reduced = !!(mq && mq.matches);
  if (mq) {
    var onMq = function (e) { reduced = e.matches; };
    if (mq.addEventListener) mq.addEventListener("change", onMq);
    else if (mq.addListener) mq.addListener(onMq);
  }

  var canAnimate = typeof Element !== "undefined" && "animate" in Element.prototype;
  var hasLinear = !!(window.CSS && CSS.supports && CSS.supports("transition-timing-function", "linear(0, 1)"));

  function cssVar(name, fallback) {
    var v = getComputedStyle(root).getPropertyValue(name);
    return v && v.trim() ? v.trim() : fallback;
  }
  // Muelles (definidos en motion.css como linear(); con respaldo cubic-bezier)
  var SPRING_BOUNCY = hasLinear ? cssVar("--hm-spring-bouncy", "cubic-bezier(.34,1.56,.64,1)") : "cubic-bezier(.34,1.56,.64,1)";
  var SPRING = hasLinear ? cssVar("--hm-spring", "cubic-bezier(.25,1.25,.5,1)") : "cubic-bezier(.25,1.25,.5,1)";
  var SPRING_SOFT = hasLinear ? cssVar("--hm-spring-soft", "cubic-bezier(.22,1,.36,1)") : "cubic-bezier(.22,1,.36,1)";

  function $all(sel, ctx) { return Array.prototype.slice.call((ctx || doc).querySelectorAll(sel)); }
  function anim(el, frames, opts) {
    if (!el || reduced || !canAnimate) return null;
    try { return el.animate(frames, opts); } catch (e) { return null; }
  }

  root.classList.add("hm-js");

  /* ------------------------------------------------------------------ */
  /* 1. Aparecer al entrar en pantalla                                   */
  /* ------------------------------------------------------------------ */
  var GLASS_SEL = ".glass, [class*='glass'], [data-glass]";

  function revealGlass(el) {
    if (reduced) return;
    var list = [el].concat($all(GLASS_SEL, el));
    list.forEach(function (g) {
      var cs = getComputedStyle(g);
      var bf = cs.backdropFilter || cs.webkitBackdropFilter;
      if (bf && bf !== "none") {
        g.classList.add("hm-glass-in");
        setTimeout(function () { g.classList.remove("hm-glass-in"); }, 1000);
      }
    });
  }

  function markIn(el) {
    if (el.classList.contains("hm-in")) return;
    el.classList.add("hm-in");
    revealGlass(el);
    var delay = parseFloat(el.style.getPropertyValue("--hm-delay")) || 0;
    setTimeout(function () { el.classList.add("hm-done"); }, delay + 900);
  }

  function setupEnter() {
    var els = $all('[data-anim="enter"]');
    if (!els.length) return;

    // Escalonado: hermanos animados del mismo padre entran uno tras otro
    var groups = typeof Map === "function" ? new Map() : null;
    els.forEach(function (el) {
      if (!groups) return;
      var p = el.parentNode;
      var n = groups.get(p) || 0;
      groups.set(p, n + 1);
      if (n) el.style.setProperty("--hm-delay", Math.min(n * 70, 420) + "ms");
    });

    if (reduced || !("IntersectionObserver" in window)) {
      els.forEach(markIn);
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) {
          markIn(en.target);
          io.unobserve(en.target);
        }
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.12 });
    els.forEach(function (el) { io.observe(el); });

    // Red de seguridad: nada se queda oculto si el observador falla
    setTimeout(function () {
      els.forEach(function (el) {
        var r = el.getBoundingClientRect();
        if (r.top < window.innerHeight && r.bottom > 0) markIn(el);
      });
    }, 1600);
  }

  /* ------------------------------------------------------------------ */
  /* 2. Pulsar: encoger y volver con muelle                              */
  /* ------------------------------------------------------------------ */
  var PRESS_SEL = '[data-anim="press"], .phone [data-tab], [data-play], [role="switch"]';

  function setupPress() {
    var current = null;
    function release() {
      if (current) { current.classList.remove("hm-pressed"); current = null; }
    }
    doc.addEventListener("pointerdown", function (e) {
      if (reduced || (e.button && e.button !== 0)) return;
      var el = e.target.closest && e.target.closest(PRESS_SEL);
      if (!el || el.disabled) return;
      release();
      current = el;
      // Las tarjetas grandes se encogen menos
      var r = el.getBoundingClientRect();
      if (r.width * r.height > 60000) el.classList.add("hm-large");
      el.classList.add("hm-pressed");
    }, { passive: true });
    ["pointerup", "pointercancel", "dragstart"].forEach(function (t) {
      doc.addEventListener(t, release, { passive: true });
    });
    doc.addEventListener("pointerleave", release, { passive: true });
    window.addEventListener("blur", release);
    doc.addEventListener("pointermove", function (e) {
      if (current && !current.contains(e.target)) release();
    }, { passive: true });

    // Teclado: Enter/Espacio da el mismo rebote
    doc.addEventListener("keydown", function (e) {
      if (reduced || (e.key !== "Enter" && e.key !== " ") || e.repeat) return;
      var el = e.target.closest && e.target.closest(PRESS_SEL);
      if (!el) return;
      el.classList.add("hm-pressed");
      setTimeout(function () { el.classList.remove("hm-pressed"); }, 110);
    });
  }

  /* ------------------------------------------------------------------ */
  /* 3. Pestañas del móvil                                               */
  /* ------------------------------------------------------------------ */
  function setupTabs() {
    var phones = $all(".phone");
    if (!phones.length && doc.querySelector("[data-tab]")) phones = [doc.body];

    phones.forEach(function (phone) {
      var tabs = $all("[data-tab]", phone);
      var screens = $all("[data-screen]", phone);
      if (!tabs.length || !screens.length) return;

      var order = tabs.map(function (t) { return t.getAttribute("data-tab"); });
      function screenFor(name) {
        for (var i = 0; i < screens.length; i++) if (screens[i].getAttribute("data-screen") === name) return screens[i];
        return null;
      }
      function currentName() {
        for (var i = 0; i < screens.length; i++) if (!screens[i].hidden) return screens[i].getAttribute("data-screen");
        return order[0];
      }

      var list = tabs[0].parentNode;
      if (list && !list.getAttribute("role") && tabs.every(function (t) { return t.parentNode === list; })) {
        list.setAttribute("role", "tablist");
      }

      function syncTabs(name) {
        tabs.forEach(function (t) {
          var on = t.getAttribute("data-tab") === name;
          if (!t.getAttribute("role")) t.setAttribute("role", "tab");
          t.setAttribute("aria-selected", on ? "true" : "false");
          t.setAttribute("tabindex", on ? "0" : "-1");
          t.classList.toggle("is-active", on);
          var s = screenFor(t.getAttribute("data-tab"));
          if (s) {
            if (!s.id) s.id = "hm-screen-" + t.getAttribute("data-tab") + "-" + Math.random().toString(36).slice(2, 7);
            if (!t.getAttribute("aria-controls")) t.setAttribute("aria-controls", s.id);
            if (!s.getAttribute("role")) s.setAttribute("role", "tabpanel");
          }
        });
      }
      syncTabs(currentName());

      var token = 0;
      var leaving = null;

      function show(name, focusTab) {
        var from = currentName();
        var next = screenFor(name);
        if (!next) return;
        syncTabs(name);
        var tab = tabs[order.indexOf(name)];
        if (focusTab && tab) tab.focus();

        // Salto del icono de la pestaña
        if (tab) {
          var ico = tab.querySelector("svg, img, .icon, [class*='icon'], span, i") || tab.firstElementChild;
          anim(ico, [{ scale: "1" }, { scale: ".82" }, { scale: "1" }],
            { duration: 520, easing: SPRING_BOUNCY, composite: "replace" });
        }
        if (from === name) return;

        var prev = screenFor(from);
        var dir = order.indexOf(name) > order.indexOf(from) ? 1 : -1;
        var my = ++token;

        if (leaving) { leaving.hidden = true; leaving.classList.remove("hm-leaving"); leaving = null; }
        screens.forEach(function (s) { s.style.zIndex = ""; s.style.background = ""; });
        screens.forEach(function (s) {
          if (s.getAnimations) s.getAnimations().forEach(function (a) { a.cancel(); });
        });

        if (reduced || !canAnimate || !prev) {
          screens.forEach(function (s) { s.hidden = s !== next; });
          return;
        }

        // Pantallas superpuestas (position:absolute): transición simultánea,
        // la nueva entra deslizándose y la anterior se hunde hacia atrás.
        if (getComputedStyle(prev).position === "absolute") {
          leaving = prev;
          prev.classList.add("hm-leaving");
          next.hidden = false;
          var back = prev.animate([
            { opacity: 1, translate: "0 0", scale: "1", filter: "brightness(1)" },
            { opacity: 0, translate: (-dir * 30) + "% 0", scale: ".92", filter: "brightness(.9)" }
          ], { duration: 380, easing: "cubic-bezier(.3,0,.2,1)", fill: "forwards" });
          next.style.zIndex = "1";
          next.style.background = "var(--bg, #F7F7F7)";
          next.animate([
            { translate: (dir * 100) + "% 0", scale: "1", boxShadow: "0 0 40px rgba(0,0,0,.18)" },
            { translate: "0 0", scale: "1", boxShadow: "0 0 0 rgba(0,0,0,0)" }
          ], { duration: 620, easing: SPRING, fill: "none" });
          Array.prototype.slice.call(next.children, 0, 10).forEach(function (k, i) {
            k.animate([{ translate: (dir * 26) + "px 0" }, { translate: "0 0" }],
              { duration: 620, delay: 40 + i * 26, easing: SPRING, fill: "backwards" });
          });
          back.onfinish = function () {
            if (my !== token) return;
            prev.hidden = true;
            prev.classList.remove("hm-leaving");
            back.cancel();
            next.style.zIndex = "";
            next.style.background = "";
            leaving = null;
          };
          return;
        }

        // Salida: se aleja un poco hacia el lado contrario, con profundidad
        leaving = prev;
        prev.classList.add("hm-leaving");
        var out = prev.animate([
          { opacity: 1, translate: "0 0", scale: "1", filter: "blur(0px)" },
          { opacity: 0, translate: (-dir * 14) + "% 0", scale: ".94", filter: "blur(3px)" }
        ], { duration: 170, easing: "cubic-bezier(.4,0,1,1)", fill: "forwards" });

        out.onfinish = function () {
          if (my !== token) return;
          prev.hidden = true;
          prev.classList.remove("hm-leaving");
          out.cancel();
          leaving = null;
          next.hidden = false;
          // Entrada: llega deslizándose desde el lado con muelle
          next.animate([
            { opacity: 0, translate: (dir * 28) + "% 0", scale: ".97" },
            { opacity: 1, translate: "0 0", scale: "1" }
          ], { duration: 560, easing: SPRING, fill: "none" });
          // Contenido escalonado
          var kids = Array.prototype.slice.call(next.children, 0, 10);
          kids.forEach(function (k, i) {
            k.animate([
              { opacity: 0, translate: (dir * 22) + "px 0" },
              { opacity: 1, translate: "0 0" }
            ], { duration: 520, delay: 30 + i * 28, easing: SPRING, fill: "backwards" });
          });
        };
      }

      tabs.forEach(function (t, i) {
        t.addEventListener("click", function (e) {
          if (t.tagName === "A") e.preventDefault();
          show(t.getAttribute("data-tab"), false);
        });
        t.addEventListener("keydown", function (e) {
          var k = e.key, n = null;
          if (k === "ArrowRight" || k === "ArrowDown") n = (i + 1) % tabs.length;
          else if (k === "ArrowLeft" || k === "ArrowUp") n = (i - 1 + tabs.length) % tabs.length;
          else if (k === "Home") n = 0;
          else if (k === "End") n = tabs.length - 1;
          if (n === null) return;
          e.preventDefault();
          show(order[n], true);
        });
      });

      // Enlaces internos dentro del móvil (p. ej. una fila de Ajustes) que apunten a una pestaña
      $all("[data-go]", phone).forEach(function (a) {
        a.addEventListener("click", function (e) {
          var name = a.getAttribute("data-go");
          if (screenFor(name)) { e.preventDefault(); show(name, false); }
        });
      });
    });
  }

  /* ------------------------------------------------------------------ */
  /* 4. Interruptores elásticos                                          */
  /* ------------------------------------------------------------------ */
  function setupSwitches() {
    var sws = $all('[role="switch"]');
    sws.forEach(function (sw) {
      if (!sw.hasAttribute("aria-checked")) sw.setAttribute("aria-checked", "false");
      var native = sw.tagName === "BUTTON" || sw.tagName === "INPUT";
      if (!native && !sw.hasAttribute("tabindex")) sw.setAttribute("tabindex", "0");
    });
    if (!sws.length) return;

    function toggle(sw) {
      if (sw.disabled || sw.getAttribute("aria-disabled") === "true") return;
      var on = sw.getAttribute("aria-checked") !== "true";
      sw.setAttribute("aria-checked", on ? "true" : "false");
      if (!reduced) {
        sw.classList.remove("hm-flip");
        void sw.offsetWidth; // reinicia la animación
        sw.classList.add("hm-flip");
        setTimeout(function () { sw.classList.remove("hm-flip"); }, 460);
      }
      try {
        sw.dispatchEvent(new CustomEvent("zt:switch", { bubbles: true, detail: { checked: on } }));
      } catch (e) { /* navegadores antiguos */ }
    }

    doc.addEventListener("click", function (e) {
      var sw = e.target.closest && e.target.closest('[role="switch"]');
      if (!sw) return;
      // Si es una etiqueta o un enlace, que no navegue
      if (sw.tagName === "A" || sw.tagName === "LABEL") e.preventDefault();
      toggle(sw);
    });
    doc.addEventListener("keydown", function (e) {
      var sw = e.target.closest && e.target.closest('[role="switch"]');
      if (!sw || sw.tagName === "BUTTON" || sw.tagName === "INPUT") return;
      if (e.key === " " || e.key === "Enter") { e.preventDefault(); toggle(sw); }
    });
  }

  /* ------------------------------------------------------------------ */
  /* 5. Reproducir tonos                                                 */
  /* ------------------------------------------------------------------ */
  var player = { audio: null, btn: null, src: null, raf: 0 };

  function waveFor(btn) {
    if (!btn) return null;
    var scope = btn.closest(".editor, [data-editor], section, article, .phone") || null;
    var w = scope ? scope.querySelector(".wave") : null;
    return w;
  }

  function setPlaying(btn, on) {
    $all("[data-play]").forEach(function (b) {
      if (b.getAttribute("data-play") === (btn && btn.getAttribute("data-play"))) {
        b.setAttribute("aria-pressed", on ? "true" : "false");
        b.classList.toggle("is-playing", on);
      }
    });
    var w = waveFor(btn);
    if (w) {
      w.classList.toggle("hm-playing", on && !reduced);
      if (!on) w.style.removeProperty("--hm-x");
    }
    var ring = btn && btn.closest("[data-ring]");
    if (ring) ring.classList.toggle("is-playing", on);
  }

  function stop() {
    if (player.audio) {
      try { player.audio.pause(); } catch (e) { /* nada */ }
      player.audio.removeAttribute("src");
      try { player.audio.load(); } catch (e) { /* nada */ }
    }
    if (player.btn) setPlaying(player.btn, false);
    cancelAnimationFrame(player.raf);
    player.audio = null; player.btn = null; player.src = null;
  }

  function trackPlayhead(btn, audio) {
    var w = waveFor(btn);
    if (!w || reduced) return;
    var head = w.querySelector(".hm-playhead");
    if (!head) {
      if (getComputedStyle(w).position === "static") w.style.position = "relative";
      head = doc.createElement("span");
      head.className = "hm-playhead";
      head.setAttribute("aria-hidden", "true");
      w.appendChild(head);
    }
    (function tick() {
      if (player.audio !== audio) return;
      var d = audio.duration;
      var p = d && isFinite(d) ? Math.min(1, audio.currentTime / d) : 0;
      w.style.setProperty("--hm-x", (p * w.clientWidth).toFixed(1) + "px");
      player.raf = requestAnimationFrame(tick);
    })();
  }

  function play(btn) {
    var src = btn.getAttribute("data-play");
    if (!src) return;
    if (player.src === src && player.audio && !player.audio.paused) { stop(); return; }
    stop();
    var audio = new Audio();
    audio.preload = "auto";
    audio.src = src;
    player.audio = audio; player.btn = btn; player.src = src;
    setPlaying(btn, true);
    audio.addEventListener("ended", function () { if (player.audio === audio) stop(); });
    audio.addEventListener("error", function () { fail(btn, audio); });
    var ed = editorFor(btn);
    if (ed) {
      // "Escuchar recorte": suena solo entre los dos bordes
      var seek = function () {
        var d = audio.duration;
        if (d && isFinite(d)) { ed.dur = d; try { audio.currentTime = ed.a * d; } catch (x) { /* nada */ } }
      };
      if (audio.readyState >= 1) seek(); else audio.addEventListener("loadedmetadata", seek, { once: true });
      audio.addEventListener("timeupdate", function () {
        var d = audio.duration;
        if (player.audio === audio && d && audio.currentTime >= ed.b * d) stop();
      });
    }
    var p = audio.play();
    if (p && p.catch) p.catch(function () { fail(btn, audio); });
    trackPlayhead(btn, audio);
  }

  function fail(btn, audio) {
    if (player.audio !== audio) return;
    stop();
    btn.classList.remove("hm-error");
    void btn.offsetWidth;
    btn.classList.add("hm-error");
    setTimeout(function () { btn.classList.remove("hm-error"); }, 450);
  }

  function setupPlay() {
    $all("[data-play]").forEach(function (b) {
      if (!b.hasAttribute("aria-pressed")) b.setAttribute("aria-pressed", "false");
      if (b.tagName !== "BUTTON" && !b.hasAttribute("tabindex")) {
        b.setAttribute("tabindex", "0");
        if (!b.getAttribute("role")) b.setAttribute("role", "button");
      }
    });
    doc.addEventListener("click", function (e) {
      var b = e.target.closest && e.target.closest("[data-play]");
      if (!b) return;
      e.preventDefault();
      play(b);
    });
    doc.addEventListener("keydown", function (e) {
      if (e.key === "Escape" || e.key === "Esc") { if (player.audio) stop(); return; }
      var b = e.target.closest && e.target.closest("[data-play]");
      if (b && b.tagName !== "BUTTON" && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); play(b); }
    });
    // Si la pestaña se oculta, se para el sonido
    doc.addEventListener("visibilitychange", function () { if (doc.hidden) stop(); });
  }

  /* ------------------------------------------------------------------ */
  /* 6. Móvil del hero sonando                                           */
  /* ------------------------------------------------------------------ */
  function setupRing() {
    var rings = $all("[data-ring]");
    if (!rings.length || reduced) return;

    rings.forEach(function (ph) {
      if (!ph.querySelector(".hm-ring-pulse")) {
        var cs = getComputedStyle(ph);
        if (cs.position === "static") ph.style.position = "relative";
        if (cs.zIndex === "auto" && cs.isolation !== "isolate") ph.style.isolation = "isolate";
        var p = doc.createElement("span");
        p.className = "hm-ring-pulse";
        p.setAttribute("aria-hidden", "true");
        p.appendChild(doc.createElement("i"));
        p.appendChild(doc.createElement("i"));
        ph.insertBefore(p, ph.firstChild);
      }

      var visible = true, busy = false, timer = 0;
      function ring() {
        if (reduced || !visible || busy || doc.hidden) return;
        ph.classList.remove("hm-ringing");
        void ph.offsetWidth;
        ph.classList.add("hm-ringing");
        setTimeout(function () { ph.classList.remove("hm-ringing"); }, 1300);
      }
      // Mientras el usuario lo toca o navega con teclado dentro, no vibra
      ph.addEventListener("pointerenter", function () { busy = true; });
      ph.addEventListener("pointerleave", function () { busy = false; });
      ph.addEventListener("focusin", function () { busy = true; });
      ph.addEventListener("focusout", function () { busy = false; });

      if ("IntersectionObserver" in window) {
        new IntersectionObserver(function (en) { visible = en[0].isIntersecting; }, { threshold: 0.3 }).observe(ph);
      }
      // Primer "timbre" tras la entrada y luego cada pocos segundos
      setTimeout(ring, 1400);
      timer = setInterval(ring, 6500);
    });
  }

  /* ------------------------------------------------------------------ */
  /* 7. Entrada del móvil del hero                                       */
  /* ------------------------------------------------------------------ */
  function heroIntro() {
    var ph = doc.querySelector("[data-ring]") || doc.querySelector(".phone");
    if (!ph || reduced || !canAnimate) return;
    if (ph.matches('[data-anim="enter"]') || ph.closest('[data-anim="enter"]')) return;
    var r = ph.getBoundingClientRect();
    if (r.top > window.innerHeight) return;
    ph.animate([
      { opacity: 0, translate: "0 48px", scale: ".92" },
      { opacity: 1, translate: "0 0", scale: "1" }
    ], { duration: 900, easing: SPRING, fill: "backwards", delay: 80 });
    var bar = ph.querySelector("[role='tablist'], nav, .tabbar, .nav");
    if (bar) anim(bar, [{ opacity: 0, translate: "0 24px" }, { opacity: 1, translate: "0 0" }],
      { duration: 700, delay: 360, easing: SPRING, fill: "backwards" });
    revealGlass(ph);
  }

  /* ------------------------------------------------------------------ */
  /* 8. Editor de recortes: mover bordes y escuchar solo el recorte      */
  /* ------------------------------------------------------------------ */
  var editors = [];

  function fmt(t) {
    var m = Math.floor(t / 60), s = t - m * 60;
    return m + ":" + (s < 10 ? "0" : "") + s.toFixed(1).replace(".", ",");
  }

  function setupEditor() {
    $all(".wave").forEach(function (w) {
      var hl = w.querySelector(".wave__handle--l"), hr = w.querySelector(".wave__handle--r");
      if (!hl || !hr) return;
      var ml = w.querySelector(".wave__mask--l"), mr = w.querySelector(".wave__mask--r");
      var bars = $all(".wave__bars > *", w);
      var scope = w.closest(".sheet, .editor, [data-editor], section") || w.parentNode;
      var label = scope.querySelector(".sheet__time, [data-time]");
      var btn = scope.querySelector("[data-play]");
      function pct(el, fb) { var v = parseFloat(el.style.left || getComputedStyle(el).left); return isNaN(v) ? fb : v; }
      var ed = { wave: w, btn: btn, a: 0.08, b: 0.78, dur: 0 };
      // Lee la posición inicial que puso style.css (en px o %)
      var W = w.clientWidth || 1;
      var la = pct(hl, NaN), lb = pct(hr, NaN);
      if (!isNaN(la) && !isNaN(lb)) { ed.a = la / W; ed.b = lb / W; }

      function render() {
        hl.style.left = (ed.a * 100) + "%";
        hr.style.left = (ed.b * 100) + "%";
        if (ml) ml.style.width = (ed.a * 100) + "%";
        if (mr) mr.style.left = (ed.b * 100) + "%";
        bars.forEach(function (bar, i) {
          var c = (i + 0.5) / bars.length;
          bar.classList.toggle("in", c >= ed.a && c <= ed.b);
        });
        if (label && ed.dur) {
          var s = ed.a * ed.dur, e = ed.b * ed.dur;
          label.textContent = fmt(s) + " → " + fmt(e) + " · " + (e - s).toFixed(1).replace(".", ",") + " s";
        }
      }
      ed.render = render;

      if (btn) {
        var meta = new Audio();
        meta.preload = "metadata";
        meta.addEventListener("loadedmetadata", function () { ed.dur = meta.duration; render(); });
        meta.src = btn.getAttribute("data-play");
      }

      var drag = null;
      function frac(e) {
        var r = w.getBoundingClientRect();
        return Math.max(0, Math.min(1, (e.clientX - r.left) / r.width));
      }
      function place(which, f) {
        var gap = 0.04;
        if (which === "a") ed.a = Math.min(f, ed.b - gap);
        else ed.b = Math.max(f, ed.a + gap);
        render();
      }
      w.style.touchAction = "pan-y";
      w.style.cursor = "ew-resize";
      w.addEventListener("pointerdown", function (e) {
        var f = frac(e);
        drag = Math.abs(f - ed.a) <= Math.abs(f - ed.b) ? "a" : "b";
        place(drag, f);
        var h = drag === "a" ? hl : hr;
        anim(h, [{ scale: "1 1" }, { scale: "1.8 1" }, { scale: "1 1" }], { duration: 420, easing: SPRING_BOUNCY });
        try { w.setPointerCapture(e.pointerId); } catch (x) { /* nada */ }
      });
      w.addEventListener("pointermove", function (e) { if (drag) place(drag, frac(e)); });
      ["pointerup", "pointercancel", "lostpointercapture"].forEach(function (t) {
        w.addEventListener(t, function () { drag = null; });
      });

      // Atajos "Usar entero", "Primeros 30 s", "Primeros 3 s"
      $all(".chip", scope).forEach(function (c) {
        var t = (c.textContent || "").toLowerCase();
        var secs = /entero/.test(t) ? Infinity : (t.match(/(\d+)\s*s/) || [])[1];
        if (!secs) return;
        c.style.cursor = "pointer";
        c.addEventListener("click", function () {
          $all(".chip", scope).forEach(function (o) { o.classList.toggle("is-on", o === c); });
          ed.a = 0;
          ed.b = secs === Infinity || !ed.dur ? 1 : Math.min(1, Number(secs) / ed.dur);
          if (ed.b < 0.04) ed.b = 0.04;
          if (reduced) { render(); return; }
          w.classList.add("hm-snap");
          render();
          setTimeout(function () { w.classList.remove("hm-snap"); }, 600);
        });
      });
      editors.push(ed);
    });
  }

  function editorFor(btn) {
    for (var i = 0; i < editors.length; i++) if (editors[i].btn === btn) return editors[i];
    return null;
  }

  function init() {
    setupEditor();
    setupEnter();
    setupPress();
    setupTabs();
    setupSwitches();
    setupPlay();
    heroIntro();
    setupRing();
  }

  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", init);
  else init();
})();
