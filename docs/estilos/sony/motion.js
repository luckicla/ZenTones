/* Zentones · estilo Sony — movimiento.
   Mínimo, preciso, rápido: sin rebotes, sin escalas, solo opacidad y desplazamientos lineales.
   Todo es opcional: si un elemento no existe, se omite sin errores. */
(function () {
  'use strict';

  var doc = document;
  var root = doc.documentElement;
  var mqReduce = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : { matches: false };
  function reduced() { return !!mqReduce.matches; }
  function $all(sel, ctx) { try { return Array.prototype.slice.call((ctx || doc).querySelectorAll(sel)); } catch (e) { return []; } }
  function on(el, ev, fn, opt) { if (el) el.addEventListener(ev, fn, opt || false); }

  root.classList.add('m-js');

  /* ------------------------------------------------------------ 1. Aparición al entrar */
  function initEnter() {
    var items = $all('[data-anim="enter"]');
    if (!items.length) return;
    items.forEach(function (el) {
      el.classList.add('m-enter');
      // líneas que se dibujan: trazos SVG en línea marcados con .draw o [data-draw]
      $all('svg .draw, svg [data-draw]', el).forEach(function (p) {
        if (p.setAttribute) p.setAttribute('pathLength', '1');
        p.classList.add('m-draw');
      });
    });
    if (reduced() || !('IntersectionObserver' in window)) {
      items.forEach(function (el) { el.classList.add('is-in'); });
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) {
          en.target.classList.add('is-in');
          io.unobserve(en.target);
        }
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.12 });
    items.forEach(function (el) {
      // si ya está a la vista al cargar, se muestra sin esperar
      var r = el.getBoundingClientRect();
      if (r.top < window.innerHeight * 0.92 && r.bottom > 0) {
        requestAnimationFrame(function () { el.classList.add('is-in'); });
      } else {
        io.observe(el);
      }
    });
    // red de seguridad: nunca dejar contenido invisible
    setTimeout(function () { items.forEach(function (el) { if (!el.classList.contains('is-in')) { var r = el.getBoundingClientRect(); if (r.top < window.innerHeight) el.classList.add('is-in'); } }); }, 2500);
  }

  /* ------------------------------------------------------------ 2. Pulsación sin adornos */
  function initPress() {
    function down(e) {
      var t = e.target.closest && e.target.closest('[data-anim="press"]');
      if (t) t.classList.add('is-pressed');
    }
    function up() { $all('.is-pressed').forEach(function (el) { el.classList.remove('is-pressed'); }); }
    on(doc, 'pointerdown', down, { passive: true });
    on(doc, 'pointerup', up, { passive: true });
    on(doc, 'pointercancel', up, { passive: true });
    on(doc, 'dragstart', up);
    on(window, 'blur', up);
  }

  /* ------------------------------------------------------------ 3. Pestañas del móvil */
  function initTabs() {
    $all('.phone').forEach(function (phone) {
      var tabs = $all('[data-tab]', phone);
      var screens = $all('[data-screen]', phone);
      if (!tabs.length || !screens.length) return;

      // indicador deslizante (2 px, color de acento, márgenes como en la app)
      var bar = tabs[0].parentElement;
      var sameParent = tabs.every(function (t) { return t.parentElement === bar; });
      var ind = null;
      if (sameParent && bar) {
        bar.classList.add('m-tabbar');
        ind = doc.createElement('span');
        ind.className = 'm-tab-ind';
        ind.setAttribute('aria-hidden', 'true');
        bar.appendChild(ind);
      }
      function place(tab, instant) {
        if (!ind || !tab) return;
        var inset = Math.min(14, tab.offsetWidth * 0.18);
        if (instant) ind.classList.add('m-noanim');
        ind.style.width = Math.max(0, tab.offsetWidth - inset * 2) + 'px';
        ind.style.transform = 'translateX(' + (tab.offsetLeft + inset) + 'px)';
        if (instant) { void ind.offsetWidth; ind.classList.remove('m-noanim'); }
      }

      var current = null;
      function select(name, focus, instant) {
        var tab = null;
        tabs.forEach(function (t) {
          var sel = t.getAttribute('data-tab') === name;
          t.setAttribute('aria-selected', sel ? 'true' : 'false');
          if (t.getAttribute('role') === 'tab') t.setAttribute('tabindex', sel ? '0' : '-1');
          if (sel) tab = t;
        });
        if (!tab) return;
        place(tab, instant);
        if (focus) tab.focus();
        if (name === current) return;
        var prev = current;
        current = name;
        screens.forEach(function (s) {
          var n = s.getAttribute('data-screen');
          if (n === name) {
            s.hidden = false;
            if (!instant && !reduced()) {
              s.classList.remove('m-screen-in');
              void s.offsetWidth;
              s.classList.add('m-screen-in');
            }
          } else if (n === prev || !s.hidden) {
            s.hidden = true;
            s.classList.remove('m-screen-in');
          }
        });
        var scroller = phone.querySelector('.m-scroll, [data-scroll]');
        if (scroller) scroller.scrollTop = 0;
      }

      var start = tabs.filter(function (t) { return t.getAttribute('aria-selected') === 'true'; })[0];
      if (!start) {
        var visible = screens.filter(function (s) { return !s.hidden; })[0];
        start = visible ? tabs.filter(function (t) { return t.getAttribute('data-tab') === visible.getAttribute('data-screen'); })[0] : tabs[0];
      }
      select((start || tabs[0]).getAttribute('data-tab'), false, true);

      tabs.forEach(function (t, i) {
        on(t, 'click', function (e) { e.preventDefault(); select(t.getAttribute('data-tab'), false, false); });
        on(t, 'keydown', function (e) {
          var k = e.key, j = -1;
          if (k === 'ArrowRight') j = (i + 1) % tabs.length;
          else if (k === 'ArrowLeft') j = (i - 1 + tabs.length) % tabs.length;
          else if (k === 'Home') j = 0;
          else if (k === 'End') j = tabs.length - 1;
          if (j >= 0) { e.preventDefault(); select(tabs[j].getAttribute('data-tab'), true, false); }
        });
      });

      var sel = function () { return tabs.filter(function (t) { return t.getAttribute('aria-selected') === 'true'; })[0]; };
      on(window, 'resize', function () { place(sel(), true); });
      if (doc.fonts && doc.fonts.ready) doc.fonts.ready.then(function () { place(sel(), true); });
      if ('ResizeObserver' in window && bar) new ResizeObserver(function () { place(sel(), true); }).observe(bar);
    });
  }

  /* ------------------------------------------------------------ 4. Interruptores */
  function initSwitches() {
    function toggle(sw) {
      if (sw.getAttribute('aria-disabled') === 'true' || sw.disabled) return;
      var v = sw.getAttribute('aria-checked') === 'true';
      sw.setAttribute('aria-checked', v ? 'false' : 'true');
      try { sw.dispatchEvent(new CustomEvent('zt:switch', { bubbles: true, detail: { checked: !v } })); } catch (e) { /* noop */ }
    }
    $all('[role="switch"][aria-checked]').forEach(function (sw) {
      sw.classList.add('m-switch');
      var native = sw.tagName === 'BUTTON' || sw.tagName === 'INPUT';
      if (!native && !sw.hasAttribute('tabindex')) sw.setAttribute('tabindex', '0');
      on(sw, 'click', function (e) {
        if (sw.tagName === 'A') e.preventDefault();
        toggle(sw);
      });
      if (!native) on(sw, 'keydown', function (e) {
        if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); toggle(sw); }
      });
    });
  }

  /* ------------------------------------------------------------ 5. Reproducir tonos */
  var audio = null, playingBtn = null, ctx = null, analyser = null, levels = null, srcNode = null;

  function setPlaying(btn, state) {
    if (!btn) return;
    btn.setAttribute('aria-pressed', state ? 'true' : 'false');
    btn.classList.toggle('is-playing', state);
    var row = btn.closest && btn.closest('li, [role="listitem"], .row, .item');
    if (row) row.classList.toggle('m-row-playing', state);
    root.classList.toggle('m-audio-on', !!state);
  }

  function stop() {
    if (audio) { try { audio.pause(); audio.currentTime = 0; } catch (e) { /* noop */ } }
    if (playingBtn) setPlaying(playingBtn, false);
    playingBtn = null;
  }

  function ensureAnalyser() {
    if (analyser || !audio) return;
    var AC = window.AudioContext || window.webkitAudioContext;
    // con file:// el navegador silenciaría la salida (origen opaco): solo medir en http(s)
    if (!AC || !/^https?:$/.test(location.protocol)) return;
    try {
      ctx = new AC();
      srcNode = ctx.createMediaElementSource(audio);
      analyser = ctx.createAnalyser();
      analyser.fftSize = 64;
      analyser.smoothingTimeConstant = 0.6;
      srcNode.connect(analyser);
      analyser.connect(ctx.destination);
      levels = new Uint8Array(analyser.frequencyBinCount);
    } catch (e) { analyser = null; }
  }

  function play(btn) {
    var src = btn.getAttribute('data-play');
    if (!src) return;
    if (playingBtn === btn) { stop(); return; }
    stop();
    if (!audio) {
      audio = new Audio();
      audio.preload = 'auto';
      on(audio, 'ended', function () { if (playingBtn) setPlaying(playingBtn, false); playingBtn = null; });
      on(audio, 'error', function () { if (playingBtn) setPlaying(playingBtn, false); playingBtn = null; });
    }
    audio.src = src;
    ensureAnalyser();
    if (ctx && ctx.state === 'suspended') { try { ctx.resume(); } catch (e) { /* noop */ } }
    playingBtn = btn;
    setPlaying(btn, true);
    var p = audio.play();
    if (p && p.catch) p.catch(function () { if (playingBtn === btn) { setPlaying(btn, false); playingBtn = null; } });
  }

  function initPlay() {
    $all('[data-play]').forEach(function (b) { if (!b.hasAttribute('aria-pressed')) b.setAttribute('aria-pressed', 'false'); });
    on(doc, 'click', function (e) {
      var b = e.target.closest && e.target.closest('[data-play]');
      if (!b) return;
      e.preventDefault();
      play(b);
    });
    on(doc, 'keydown', function (e) {
      if (e.key === 'Escape' || e.key === 'Esc') stop();
    });
    on(doc, 'visibilitychange', function () { if (doc.hidden) stop(); });
  }

  /* ------------------------------------------------------------ 6. Medidor de nivel en el móvil "sonando" */
  var meters = [];
  function initRing() {
    $all('[data-ring]').forEach(function (el) {
      el.classList.add('m-ring');
      if (el.querySelector('.m-meter')) return;
      // el medidor va en la cabecera de la app (junto a la pestaña actual); si no existe, flota sobre el móvil
      var head = el.querySelector('.ap__head, [data-meter]');
      var m = doc.createElement('span');
      m.className = 'm-meter' + (head ? ' m-meter--inline' : '');
      m.setAttribute('aria-hidden', 'true');
      for (var i = 0; i < 10; i++) {
        var b = doc.createElement('i');
        m.appendChild(b);
      }
      if (head) {
        var where = head.querySelector('.ap__where');
        head.insertBefore(m, where || null);
      } else {
        el.appendChild(m);
      }
      meters.push({ el: m, bars: $all('i', m), host: el });
    });
  }

  /* ------------------------------------------------------------ 7. Onda del editor */
  var waves = [];
  function initWave() {
    $all('.wave').forEach(function (w) {
      w.classList.add('m-wave');
      $all('i', w).forEach(function (b, k) { b.style.setProperty('--k', k); });
      if (w.querySelector('.m-playhead')) return;
      var ph = doc.createElement('span');
      ph.className = 'm-playhead';
      ph.setAttribute('aria-hidden', 'true');
      w.appendChild(ph);
      waves.push({ el: w, ph: ph });
    });
  }

  /* ------------------------------------------------------------ 8. Líneas que se dibujan en los encabezados */
  function initRules() {
    $all('[data-anim="enter"] .eyebrow, .eyebrow[data-anim="enter"]').forEach(function (e) {
      if (e.querySelector('.m-rule')) return;
      var r = doc.createElement('span');
      r.className = 'm-rule';
      r.setAttribute('aria-hidden', 'true');
      e.appendChild(r);
    });
  }

  /* ------------------------------------------------------------ bucle de animación (solo si hace falta y está a la vista) */
  var visible = new WeakMap();
  function watchVisibility(list) {
    if (!('IntersectionObserver' in window)) { list.forEach(function (o) { visible.set(o.el, true); }); return; }
    var io = new IntersectionObserver(function (es) { es.forEach(function (en) { visible.set(en.target, en.isIntersecting); }); });
    list.forEach(function (o) { io.observe(o.el); });
  }

  function loop() {
    if (!meters.length && !waves.length) return;
    watchVisibility(meters.concat(waves));
    var t0 = performance.now();
    var smooth = [];
    function frame(now) {
      var t = (now - t0) / 1000;
      if (!reduced()) {
        var live = analyser && playingBtn && audio && !audio.paused;
        if (live) analyser.getByteFrequencyData(levels);
        meters.forEach(function (m) {
          if (!visible.get(m.el)) return;
          m.bars.forEach(function (b, i) {
            var v;
            if (live) {
              var bin = Math.min(levels.length - 1, 1 + Math.floor(i * (levels.length * 0.7) / m.bars.length));
              v = levels[bin] / 255;
            } else {
              // reposo: nivel bajo y tranquilo, como un vúmetro con señal suave
              v = 0.22 + 0.42 * Math.abs(Math.sin(t * 1.3 + i * 0.8)) * (0.55 + 0.45 * Math.sin(t * 0.5 + i * 1.7));
            }
            smooth[i] = smooth[i] == null ? v : smooth[i] + (v - smooth[i]) * 0.35;
            b.style.transform = 'scaleY(' + Math.max(0.06, Math.min(1, smooth[i])).toFixed(3) + ')';
          });
          m.el.classList.toggle('is-live', !!live);
        });
        waves.forEach(function (w) {
          // el cabezal solo aparece mientras suena el recorte del propio editor, entre los tiradores
          var box = w.el.closest('[role="group"], .dialog, .editor, section') || w.el.parentElement;
          var mine = playingBtn && audio && !audio.paused && box && box.contains(playingBtn);
          w.el.classList.toggle('m-wave--play', !!mine);
          if (!mine) return;
          var cs = getComputedStyle(w.el);
          var a = parseFloat(cs.getPropertyValue('--a')) || 0;
          var b = parseFloat(cs.getPropertyValue('--b')) || 1;
          var len = parseFloat(w.el.getAttribute('data-clip')) || (audio.duration ? audio.duration * (b - a) : 0);
          var p = len ? Math.min(1, audio.currentTime / len) : 0;
          if (p >= 1 && audio.duration && audio.currentTime < audio.duration) { /* fin del recorte: para como en la app */ stop(); return; }
          var x = (a + (b - a) * p) * w.el.clientWidth;
          w.ph.style.transform = 'translateX(' + x.toFixed(1) + 'px)';
        });
      }
      requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);
  }

  function init() {
    initEnter();
    initPress();
    initTabs();
    initSwitches();
    initPlay();
    initRing();
    initWave();
    initRules();
    loop();
    if (mqReduce.addEventListener) mqReduce.addEventListener('change', function () { if (reduced()) $all('[data-anim="enter"]').forEach(function (el) { el.classList.add('is-in'); }); });
  }

  if (doc.readyState === 'loading') on(doc, 'DOMContentLoaded', init); else init();
})();
