#!/usr/bin/env python3
"""
Genera el catálogo de la tienda de Zentones: tonos originales por categorías, sintetizados
desde cero (sin samples de terceros), en OGG + catalog.json.

Uso:  python3 tools/generate_catalog.py [--per 10] [--seed 7]
Para añadir más tonos, sube --per o añade categorías en CATS. También puedes meter tus propios
archivos en catalog/tones/ y añadirlos a mano a catalog.json (asegúrate de tener derechos).
"""
import argparse, json, os, subprocess, tempfile
import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve
from scipy.io import wavfile

SR = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "catalog")

SCALES = {
    "major_pent": [0, 2, 4, 7, 9],
    "minor_pent": [0, 3, 5, 7, 10],
    "major": [0, 2, 4, 5, 7, 9, 11],
    "minor": [0, 2, 3, 5, 7, 8, 10],
    "harm_minor": [0, 2, 3, 5, 7, 8, 11],
    "dorian": [0, 2, 3, 5, 7, 9, 10],
    "whole": [0, 2, 4, 6, 8, 10],
    "lydian": [0, 2, 4, 6, 7, 9, 11],
}

def hz(m): return 440.0 * 2 ** ((m - 69) / 12)
def t_axis(d): return np.arange(int(d * SR)) / SR

# ---------- instrumentos ----------
def partials(f, d, parts, attack=0.003):
    t = t_axis(d); s = np.zeros_like(t)
    for r, a, dec in parts:
        if f * r < SR / 2.3:
            s += a * np.sin(2 * np.pi * f * r * t) * np.exp(-dec * t)
    return s * np.minimum(1, t / attack)

def marimba(f, d): return partials(f, d + 1.0, [(1, 1, 7), (3.93, .32, 22), (9.2, .07, 45)])
def kalimba(f, d): return partials(f, d + 1.4, [(1, 1, 4.2), (2, .08, 7), (5.4, .18, 20)])
def bell(f, d): return partials(f, d + 2.5, [(.5, .2, 1.1), (1, 1, 1.6), (2, .45, 2.4), (2.76, .3, 3.2), (5.4, .14, 5.5)])
def glass(f, d): return partials(f, d + 1.2, [(1, 1, 4), (2.32, .38, 8), (4.25, .12, 14)])
def musicbox(f, d, detune=0.0):
    f = f * 2 ** (detune / 1200)
    return partials(f, d + 1.6, [(1, 1, 3.2), (3.0, .25, 9), (6.1, .08, 18)], 0.002)

def env_adsr(n, a=.01, dcy=.08, s=.7, r=.1, dur=None):
    t = np.arange(n) / SR; dur = dur if dur is not None else n / SR - r
    e = np.where(t < a, t / a, np.where(t < a + dcy, 1 - (1 - s) * (t - a) / dcy, s))
    return e * np.clip(1 - (t - dur) / r, 0, 1)

def square(f, d, duty=.5, vib=0.0):
    t = t_axis(d + .05)
    ph = (f * t + vib * np.sin(2 * np.pi * 6 * t) * 0.002 * f) % 1
    return np.where(ph < duty, 1.0, -1.0) * env_adsr(len(t), .002, .05, .6, .04, d) * .35

def triangle(f, d):
    t = t_axis(d + .05)
    return (2 * np.abs(2 * ((f * t) % 1) - 1) - 1) * env_adsr(len(t), .002, .05, .8, .04, d) * .5

def saw(f, d, n=12):
    t = t_axis(d + .2); s = np.zeros_like(t)
    for k in range(1, n + 1):
        if f * k < SR / 2.3: s += np.sin(2 * np.pi * f * k * t) / k
    return s * env_adsr(len(t), .01, .1, .7, .2, d) * .4

def fm(f, d, ratio=2.0, index=3.0, dec=3.0):
    t = t_axis(d + .8)
    mod = index * np.exp(-dec * t) * np.sin(2 * np.pi * f * ratio * t)
    return np.sin(2 * np.pi * f * t + mod) * np.exp(-2.2 * t) * np.minimum(1, t / .003)

def rhodes(f, d):
    t = t_axis(d + 1.0)
    s = np.sin(2 * np.pi * f * t) + .35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-6 * t)
    return s * np.exp(-1.8 * t) * (1 + .15 * np.sin(2 * np.pi * 5 * t)) * np.minimum(1, t / .005) * .6

def pad(f, d):
    t = t_axis(d + .6)
    s = np.sin(2 * np.pi * f * t) + .6 * np.sin(2 * np.pi * f * 1.004 * t) + .3 * np.sin(2 * np.pi * f * 2.002 * t)
    return s * env_adsr(len(t), .25, .2, .9, .5, d) * .25

def pluck(f, d, rng):
    n = int((d + 1.0) * SR); p = max(2, int(SR / f))
    line = rng.uniform(-1, 1, p); out = np.zeros(n)
    for i in range(n):
        j = i % p; out[i] = line[j]
        line[j] = 0.996 * 0.5 * (line[j] + line[(j + 1) % p])
    return out * .7

def chirp(f0, f1, d):
    t = t_axis(d); f = np.geomspace(f0, f1, len(t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.sin(np.pi * t / d) ** 2

def noise(d, rng): return rng.uniform(-1, 1, int(d * SR))

# ---------- efectos ----------
def lp(x, fc): return sosfilt(butter(2, fc / (SR / 2), "low", output="sos"), x)
def hp(x, fc): return sosfilt(butter(2, fc / (SR / 2), "high", output="sos"), x)
def bp(x, lo, hi): return sosfilt(butter(2, [lo / (SR / 2), hi / (SR / 2)], "band", output="sos"), x)

def reverb(x, rng, size=1.6, mix=.25):
    n = int(size * SR); t = np.arange(n) / SR
    ir = rng.uniform(-1, 1, n) * np.exp(-4.5 * t / size)
    ir = lp(ir, 6000); ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    w = fftconvolve(x, ir)[: len(x)]
    return x * (1 - mix * .4) + w * mix

def crackle(n, rng, amount=.004):
    c = np.zeros(n); idx = rng.random(n) < amount * .02
    c[idx] = rng.uniform(-1, 1, idx.sum()); return lp(c, 4000) * .5 + rng.normal(0, .004, n)

def place(buf, sig, t, gain=1.0):
    i = int(t * SR)
    if i >= len(buf): return
    j = min(len(buf), i + len(sig)); buf[i:j] += sig[: j - i] * gain

def finish(x, fade=.05):
    n = int(fade * SR); x[-n:] *= np.linspace(1, 0, n)
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.1)
    return x / np.max(np.abs(x)) * .89

# ---------- composición ----------
def motif(rng, scale, length, span=7, start=None):
    deg = start if start is not None else int(rng.integers(0, len(scale)))
    out = []
    for _ in range(length):
        out.append(deg)
        deg = int(np.clip(deg + rng.choice([-2, -1, 1, 1, 2, 3, -3, 0], p=[.1, .2, .2, .15, .15, .07, .05, .08]), -2, span))
    return out

def deg2midi(root, scale, d):
    o, i = divmod(d, len(scale)); return root + 12 * o + scale[i]

def rhythm(rng, steps, density=.7):
    r = rng.random(steps) < density; r[0] = True; return r

def compose(rng, length, step, inst, root, scale, density=.7, motif_len=8, gain=1.0, buf=None):
    total = int(length / step)
    buf = buf if buf is not None else np.zeros(int((length + 3) * SR))
    m = motif(rng, scale, motif_len); r = rhythm(rng, motif_len, density)
    var = list(m); var[-2:] = [x + rng.choice([-1, 1, 2]) for x in var[-2:]]
    for k in range(total):
        phrase = m if (k // motif_len) % 2 == 0 else var
        i = k % motif_len
        if r[i]:
            place(buf, inst(hz(deg2midi(root, scale, phrase[i])), step), k * step, gain)
    return buf

# ---------- categorías ----------
NAMES = {
    "retro": ["Píxel", "Cartucho", "Nivel 1", "Arcade", "Joystick", "Insert Coin", "Bonus", "Sprite", "Turbo", "8 bits", "Chip", "Combo", "Highscore", "Jefe final"],
    "terror": ["Medianoche", "Pasillo", "Cámara 3", "Sótano", "Caja de música", "Turno de noche", "Animatrónico", "Pizzería vacía", "Luz parpadeante", "Ventilación", "Las 6 AM", "Susurro", "Muñeco", "Telón"],
    "radio": ["Onda media", "Dial", "Transistor", "Emisora fantasma", "Frecuencia", "Sintonía", "Radio de cocina", "Laboratorio", "Señal lejana", "Válvulas", "Estática", "Altavoz viejo", "Antena", "Interferencia"],
    "scifi": ["Órbita", "Holograma", "Nave", "Teletransporte", "Androide", "Nebulosa", "Hiperespacio", "Láser", "Satélite", "Cápsula", "Plasma", "Galaxia", "Escáner", "Puente de mando"],
    "lofi": ["Café", "Lluvia", "Domingo", "Ventana", "Vinilo", "Atardecer", "Libreta", "Sofá", "Té verde", "Persiana", "Gato dormido", "Neblina", "Postal", "Tranvía"],
    "marimba": ["Caña", "Isla", "Coco", "Brisa marina", "Laguna", "Palmera", "Arrecife", "Madera", "Bongó", "Manglar", "Tortuga", "Coral", "Marea", "Hamaca"],
    "electro": ["Neón", "Pulso", "Club", "Strobe", "Bassline", "Sintetizador", "Rave", "Voltaje", "Drop", "Circuito", "Láser verde", "Ácido", "Kick", "Glitch"],
    "zen": ["Loto", "Bambú zen", "Templo", "Respira", "Cuenco", "Jardín", "Niebla", "Piedra", "Calma", "Incienso", "Montaña", "Río", "Musgo", "Amanecer"],
    "gaming": ["Logro", "Victoria", "Nivel arriba", "Cofre", "Misión", "Power-up", "Checkpoint", "Experiencia", "Gema", "Escudo", "Loot", "Respawn", "Mapa", "Gremio"],
    "alerta": ["Urgente", "Atención", "Sirena suave", "Alarma", "Prioridad", "Faro", "Radar", "Aviso", "Señal", "Código rojo", "Despertar", "Ping", "Rebote", "Doble ping"],
}

CATS = [
    ("retro", "Retro 8-bit"), ("terror", "Terror nocturno"), ("radio", "Radio antigua"),
    ("scifi", "Ciencia ficción"), ("lofi", "Lo-fi"), ("marimba", "Marimba y madera"),
    ("electro", "Electrónica"), ("zen", "Zen"), ("gaming", "Gaming"), ("alerta", "Alertas"),
]

def make(cat, kind, rng):
    call = kind == "call"
    L = rng.uniform(6.0, 8.0) if call else rng.uniform(.8, 2.2)
    buf = np.zeros(int((L + 3) * SR))
    root = int(rng.integers(57, 69))
    if cat == "retro":
        sc = SCALES[rng.choice(["major_pent", "major", "minor_pent"])]
        duty = rng.choice([.125, .25, .5])
        lead = lambda f, d: square(f, d * .9, duty)
        if call:
            step = rng.choice([.1, .125, .15])
            compose(rng, L, step, lead, root + 12, sc, .75, 8, 1.0, buf)
            compose(rng, L, step * 2, lambda f, d: triangle(f / 2, d * .9), root - 12, sc, .9, 4, .8, buf)
        else:
            m = motif(rng, sc, int(rng.integers(3, 6)), start=0)
            for i, d in enumerate(m): place(buf, lead(hz(deg2midi(root + 12, sc, d + i)), .07), i * .07)
        return finish(buf[: int((L + .3) * SR)]), L
    if cat == "terror":
        sc = SCALES[rng.choice(["harm_minor", "minor", "whole"])]
        det = rng.uniform(15, 40)
        box = lambda f, d: musicbox(f, d, det * rng.choice([-1, 1]))
        if call:
            step = rng.choice([.28, .32, .36])
            compose(rng, L, step, box, root + 12, sc, .8, 8, 1.0, buf)
            t = t_axis(L + 1); dr = saw(hz(root - 24), L, 8); dr = lp(dr, 400) * (0.6 + .4 * np.sin(2 * np.pi * .3 * t[: len(dr)]))
            place(buf, dr, 0, .5)
            place(buf, noise(L, rng) * .02 * np.sin(np.linspace(0, np.pi, int(L * SR))), 0)
        else:
            for i, d in enumerate(motif(rng, sc, 3)): place(buf, box(hz(deg2midi(root + 12, sc, d)), .3), i * .22)
        buf = reverb(buf, rng, 2.5, .45)
        return finish(buf[: int((L + .6) * SR)]), L
    if cat == "radio":
        sc = SCALES[rng.choice(["major", "dorian", "lydian"])]
        if call:
            step = rng.choice([.2, .24])
            compose(rng, L, step, rhodes, root + 12, sc, .7, 8, 1.0, buf)
            for k in range(int(L / (step * 4))):
                ch = [deg2midi(root - 12, sc, d) for d in (0, 2, 4)] if k % 2 == 0 else [deg2midi(root - 12, sc, d) for d in (3, 5, 7)]
                for m in ch: place(buf, rhodes(hz(m), step * 3.5), k * step * 4, .35)
        else:
            for i, d in enumerate(motif(rng, sc, 3, start=4)): place(buf, rhodes(hz(deg2midi(root + 12, sc, d)), .2), i * .14)
        buf = bp(buf, 350, 3200); buf = np.tanh(buf * 2.5)
        buf += crackle(len(buf), rng) * .6
        return finish(buf[: int((L + .3) * SR)]), L
    if cat == "scifi":
        sc = SCALES[rng.choice(["whole", "lydian", "minor_pent"])]
        ratio = rng.choice([1.41, 2.0, 3.5, 1.5])
        inst = lambda f, d: fm(f, d, ratio, rng.uniform(2, 6))
        if call:
            compose(rng, L, rng.choice([.15, .2]), inst, root + 12, sc, .7, 8, 1.0, buf)
            place(buf, chirp(200, 1800, .6) * .3, 0); place(buf, pad(hz(root - 12), L - 1) , 0, .6)
        else:
            if rng.random() < .5: place(buf, chirp(rng.uniform(300, 600), rng.uniform(1500, 3000), .35) * .6, 0)
            for i, d in enumerate(motif(rng, sc, 3)): place(buf, inst(hz(deg2midi(root + 12, sc, d)), .15), .1 + i * .09)
        buf = reverb(buf, rng, 1.8, .35)
        return finish(buf[: int((L + .4) * SR)]), L
    if cat == "lofi":
        sc = SCALES[rng.choice(["dorian", "major", "minor"])]
        if call:
            step = rng.choice([.3, .35])
            for k in range(int(L / (step * 4)) + 1):
                base = [0, 2, 4, 6] if k % 2 == 0 else [3, 5, 7, 9]
                for i, d in enumerate(base): place(buf, rhodes(hz(deg2midi(root - 12, sc, d)), step * 3.6), k * step * 4 + i * .015, .45)
            compose(rng, L, step / 2, kalimba, root + 12, sc, .45, 8, .7, buf)
        else:
            for i, d in enumerate([0, 2, 4, 6]): place(buf, rhodes(hz(deg2midi(root, sc, d)), .5), i * .03, .6)
        buf = lp(buf, 3500) + crackle(len(buf), rng, .002) * .5
        return finish(buf[: int((L + .4) * SR)]), L
    if cat == "marimba":
        sc = SCALES[rng.choice(["major_pent", "major", "lydian"])]
        if call:
            compose(rng, L, rng.choice([.14, .16, .18]), marimba, root + 12, sc, .75, 8, 1.0, buf)
            compose(rng, L, .64, marimba, root - 12, sc, .9, 4, .6, buf)
        else:
            m = motif(rng, sc, int(rng.integers(2, 4)), start=2)
            for i, d in enumerate(m): place(buf, marimba(hz(deg2midi(root + 12, sc, d)), .1), i * .1)
        buf = reverb(buf, rng, 1.2, .2)
        return finish(buf[: int((L + .4) * SR)]), L
    if cat == "electro":
        sc = SCALES[rng.choice(["minor", "minor_pent", "dorian"])]
        if call:
            step = rng.choice([.125, .14])
            compose(rng, L, step, lambda f, d: lp(saw(f, d * .8, 16), 3000), root + 12, sc, .8, 8, .8, buf)
            for k in range(int(L / (step * 2))):
                place(buf, lp(saw(hz(root - 24 + (0 if (k // 8) % 2 == 0 else sc[3])), step * 1.6, 10), 500), k * step * 2, .8)
                if k % 2 == 0:
                    kk = t_axis(.15); place(buf, np.sin(2 * np.pi * np.cumsum(np.geomspace(150, 45, len(kk))) / SR) * np.exp(-25 * kk), k * step * 2, .9)
        else:
            for i, d in enumerate(motif(rng, sc, 4)): place(buf, lp(saw(hz(deg2midi(root + 12, sc, d)), .06, 16), 4000), i * .07)
        buf = reverb(buf, rng, 1.0, .15)
        return finish(buf[: int((L + .3) * SR)]), L
    if cat == "zen":
        sc = SCALES["major_pent"]
        if call:
            for k in range(int(L / 1.6) + 1):
                place(buf, bell(hz(deg2midi(root, sc, int(rng.integers(0, 8)))), 1.0), k * 1.6 + rng.uniform(0, .2), .6)
            place(buf, pad(hz(root - 12), L - .5), 0, .7); place(buf, pad(hz(root - 5), L - .5), 0, .4)
            compose(rng, L, .4, kalimba, root + 12, sc, .4, 8, .4, buf)
        else:
            place(buf, bell(hz(root + 12), .8), 0)
        buf = reverb(buf, rng, 3.0, .45)
        return finish(buf[: int((L + .8) * SR)]), L
    if cat == "gaming":
        sc = SCALES[rng.choice(["major", "lydian", "major_pent"])]
        inst = rng.choice([0, 1])
        lead = (lambda f, d: square(f, d * .85, .25)) if inst == 0 else (lambda f, d: glass(f, d))
        if call:
            step = rng.choice([.12, .14])
            compose(rng, L, step, lead, root + 12, sc, .8, 8, 1.0, buf)
            compose(rng, L, step * 4, lambda f, d: triangle(f / 2, d), root - 12, sc, 1.0, 4, .7, buf)
        else:
            n = int(rng.integers(3, 6))
            for i in range(n): place(buf, lead(hz(deg2midi(root + 12, sc, i * 2)), .06), i * .06)
        buf = reverb(buf, rng, 1.0, .18)
        return finish(buf[: int((L + .3) * SR)]), L
    # alerta
    f0 = hz(root + 24)
    if call:
        pats = [[.0, .15, .3], [.0, .12], [.0, .1, .2, .3]]; pat = pats[int(rng.integers(0, 3))]
        per = rng.uniform(.8, 1.2)
        for k in range(int(L / per)):
            for p in pat: place(buf, glass(f0 * (1.5 if p > 0 and rng.random() < .3 else 1), .05) if rng.random() < .5 else square(f0, .07, .5), k * per + p, .8)
    else:
        for i in range(int(rng.integers(1, 3))): place(buf, glass(f0 * (1 + .5 * i), .05), i * .12)
    buf = reverb(buf, rng, .6, .1)
    return finish(buf[: int((L + .3) * SR)]), L


def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--per", type=int, default=10); ap.add_argument("--seed", type=int, default=7)
    a = ap.parse_args()
    out_dir = os.path.join(ROOT, "tones"); os.makedirs(out_dir, exist_ok=True)
    tones = []
    for ci, (cid, cname) in enumerate(CATS):
        names = list(NAMES[cid])
        for i in range(a.per):
            rng = np.random.default_rng(a.seed * 1000 + ci * 100 + i)
            kind = "call" if i % 2 == 0 else "message"
            x, L = make(cid, kind, rng)
            name = names[i % len(names)] + ("" if i < len(names) else f" {i // len(names) + 1}")
            fid = f"{cid}-{i + 1:02d}"
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf:
                wavfile.write(tf.name, SR, (x * 32767).astype(np.int16))
                subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tf.name, "-c:a", "libvorbis", "-q:a", "4",
                                os.path.join(out_dir, fid + ".ogg")], check=True)
                os.unlink(tf.name)
            tones.append({"id": fid, "name": name, "category": cid, "kind": kind,
                          "file": f"tones/{fid}.ogg", "duration": round(len(x) / SR, 2),
                          "author": "Zentones", "license": "CC0"})
            print(fid, name, kind)
    cat = {"version": 1, "categories": [{"id": c, "name": n} for c, n in CATS], "tones": tones}
    with open(os.path.join(ROOT, "catalog.json"), "w", encoding="utf-8") as f:
        json.dump(cat, f, ensure_ascii=False, indent=1)

if __name__ == "__main__":
    main()
