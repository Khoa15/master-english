#!/usr/bin/env python3
"""Generates the offline media assets bundled with the Android app.

Outputs (all reproducible, no network needed):
  app/src/main/res/raw/listen_XXX.mp3   Listening audio, spoken with espeak-ng
                                        (one male / one female voice per speaker)
  app/src/main/res/raw/{ding,buzz_error,pop,level_up,victory}.mp3
                                        Synthesised sound effects for SoundPool
  app/src/main/res/raw/mascot_{celebrate,thinking,wave}.json
                                        Lottie mascot animations (owl)
  app/src/main/assets/data/*.json       Master content copied from data/

Requirements: python3, espeak-ng, ffmpeg (libmp3lame).
Run from the repository root:  python3 tools/generate_assets.py
"""
import json
import math
import os
import shutil
import struct
import subprocess
import tempfile
import wave

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets", "data")
DATA = os.path.join(ROOT, "data")
SAMPLE_RATE = 22050


def run(cmd):
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def write_wav(path, samples, rate=SAMPLE_RATE):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        frames = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples)
        w.writeframes(frames)


def to_mp3(wav_path, mp3_path, bitrate="64k"):
    run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path,
         "-codec:a", "libmp3lame", "-b:a", bitrate, mp3_path])


def read_wav_samples(path):
    with wave.open(path, "rb") as w:
        assert w.getnchannels() == 1 and w.getsampwidth() == 2, "expected mono 16-bit wav"
        rate = w.getframerate()
        raw = w.readframes(w.getnframes())
    count = len(raw) // 2
    return list(struct.unpack("<%dh" % count, raw)), rate


# ---------------------------------------------------------------- listening audio
VOICE_BY_SPEAKER = {"Nam": "en-us+m3", "Mai": "en-us+f3"}
FALLBACK_VOICES = ["en-us+m3", "en-us+f3", "en-us+m5", "en-us+f2"]


def speak(text, voice, out_wav):
    run(["espeak-ng", "-v", voice, "-s", "150", "-p", "45", "-w", out_wav, text])


def build_listening_audio(tmp):
    with open(os.path.join(DATA, "listening.json"), encoding="utf-8") as f:
        items = json.load(f)
    for item in items:
        lesson_id = item["id"]  # e.g. list_001 -> listen_001
        res_name = "listen_" + lesson_id.split("_")[1]
        speaker_voice = {}
        pieces = []
        silence = [0.0] * int(SAMPLE_RATE * 0.45)
        for index, line in enumerate(item["transcript"]):
            speaker = line["speaker"]
            if speaker not in speaker_voice:
                speaker_voice[speaker] = VOICE_BY_SPEAKER.get(
                    speaker, FALLBACK_VOICES[len(speaker_voice) % len(FALLBACK_VOICES)])
            wav = os.path.join(tmp, "%s_%02d.wav" % (res_name, index))
            speak(line["text"], speaker_voice[speaker], wav)
            samples, rate = read_wav_samples(wav)
            assert rate == SAMPLE_RATE, "espeak sample rate changed: %d" % rate
            pieces.append(samples)
            pieces.append(silence)
        merged = [s for piece in pieces for s in piece]
        combined = os.path.join(tmp, res_name + ".wav")
        write_wav(combined, [v / 32767.0 for v in merged])
        to_mp3(combined, os.path.join(RAW, res_name + ".mp3"), bitrate="48k")
        print("listening audio:", res_name + ".mp3")


# ---------------------------------------------------------------- sound effects
def tone_sequence(notes, tmp_name, tmp):
    """notes: list of (frequency_hz, seconds). Adds a soft attack and decay per note."""
    samples = []
    for freq, seconds in notes:
        n = int(SAMPLE_RATE * seconds)
        for i in range(n):
            t = i / SAMPLE_RATE
            attack = min(1.0, t / 0.01)
            decay = math.exp(-3.0 * t / seconds)
            samples.append(0.55 * attack * decay * math.sin(2 * math.pi * freq * t))
    path = os.path.join(tmp, tmp_name + ".wav")
    write_wav(path, samples)
    return path


def buzz_sequence(tmp):
    n = int(SAMPLE_RATE * 0.35)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        square = 1.0 if math.sin(2 * math.pi * 140 * t) >= 0 else -1.0
        samples.append(0.35 * square * (1.0 - t / 0.35))
    path = os.path.join(tmp, "buzz_error.wav")
    write_wav(path, samples)
    return path


def build_sound_effects(tmp):
    effects = {
        "ding": tone_sequence([(1318.5, 0.45)], "ding", tmp),
        "pop": tone_sequence([(880, 0.06), (1320, 0.08)], "pop", tmp),
        "level_up": tone_sequence([(523.25, 0.12), (659.25, 0.12), (783.99, 0.12), (1046.5, 0.35)], "level_up", tmp),
        "victory": tone_sequence([(523.25, 0.14), (659.25, 0.14), (783.99, 0.14), (659.25, 0.1),
                                  (783.99, 0.14), (1046.5, 0.6)], "victory", tmp),
        "buzz_error": buzz_sequence(tmp),
    }
    for name, wav in effects.items():
        to_mp3(wav, os.path.join(RAW, name + ".mp3"), bitrate="96k")
        print("sound effect:", name + ".mp3")


# ---------------------------------------------------------------- Lottie mascot
FPS = 30
FRAMES = 60  # 2 seconds, loops seamlessly because the first and last keys match


def keyframes(track):
    """track: list of (frame, value). Produces a static or animated Lottie property.

    Lottie wants animated keyframe values as arrays, even for scalars such as
    rotation or opacity, so scalars are wrapped on the animated path only.
    """
    if len(track) == 1:
        return {"a": 0, "k": track[0][1]}
    keys = []
    for idx, (frame, value) in enumerate(track):
        key = {"t": frame, "s": value if isinstance(value, list) else [value]}
        if idx < len(track) - 1:
            key["i"] = {"x": [0.42], "y": [1]}
            key["o"] = {"x": [0.58], "y": [0]}
        keys.append(key)
    return {"a": 1, "k": keys}


def ellipse_layer(index, name, size, pos, color, rot=None, scale=None, offset=None, opacity=None):
    rot_track = rot or [(0, 0)]
    scale_track = scale or [(0, [100, 100])]
    pos_track = offset or [(0, pos)]
    opacity_track = opacity or [(0, 100)]
    return {
        "ddd": 0, "ind": index, "ty": 4, "nm": name, "sr": 1,
        "ks": {
            "o": keyframes(opacity_track),
            "r": keyframes(rot_track),
            "p": keyframes(pos_track),
            "a": {"a": 0, "k": [0, 0, 0]},
            "s": keyframes(scale_track),
        },
        "ao": 0, "ip": 0, "op": FRAMES, "st": 0, "bm": 0,
        "shapes": [{
            "ty": "gr", "nm": name,
            "it": [
                {"d": 1, "ty": "el", "nm": "ellipse", "p": {"a": 0, "k": [0, 0]},
                 "s": {"a": 0, "k": size}},
                {"ty": "fl", "nm": "fill", "c": {"a": 0, "k": color}, "o": {"a": 0, "k": 100}, "r": 1},
                {"ty": "tr", "nm": "transform", "p": {"a": 0, "k": [0, 0]},
                 "a": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [100, 100]},
                 "r": {"a": 0, "k": 0}, "o": {"a": 0, "k": 100}},
            ],
        }],
    }


BODY = [0.16, 0.55, 0.85, 1]
BELLY = [0.82, 0.93, 0.98, 1]
WING = [0.10, 0.36, 0.66, 1]
BEAK = [1.0, 0.72, 0.18, 1]
EYE = [1, 1, 1, 1]
PUPIL = [0.1, 0.1, 0.18, 1]
SPARK = [1.0, 0.85, 0.2, 1]


def mascot_animation(name, motion):
    """motion(part) returns (rot_track, scale_track, offset_track) for each body part."""
    parts = [
        ("body", [240, 260], [200, 215], BODY),
        ("belly", [150, 170], [200, 245], BELLY),
        ("eye_left", [70, 70], [160, 170], EYE),
        ("eye_right", [70, 70], [240, 170], EYE),
        ("pupil_left", [30, 30], [160, 176], PUPIL),
        ("pupil_right", [30, 30], [240, 176], PUPIL),
        ("beak", [30, 22], [200, 205], BEAK),
        ("wing_left", [50, 130], [118, 240], WING),
        ("wing_right", [50, 130], [282, 240], WING),
        ("foot_left", [44, 20], [172, 338], BEAK),
        ("foot_right", [44, 20], [228, 338], BEAK),
    ]
    layers = []
    for index, (part, size, pos, color) in enumerate(parts, start=1):
        rot, scale, offset = motion(part, pos)
        layers.append(ellipse_layer(index, part, size, pos, color, rot=rot, scale=scale, offset=offset))
    # Sparkles: appear and fade during the celebrate animation only.
    if name == "celebrate":
        for index, (x, y) in enumerate([(60, 90), (340, 80), (50, 280), (350, 270)], start=len(parts) + 1):
            fade = [(0, 0), (15, 100), (30, 0), (45, 100), (59, 0)]
            layers.append(ellipse_layer(index, "spark_%d" % index, [18, 18], [x, y], SPARK,
                                        scale=[(0, [40, 40]), (30, [120, 120]), (59, [40, 40])],
                                        opacity=fade))
    return {
        "v": "5.7.4", "fr": FPS, "ip": 0, "op": FRAMES, "w": 400, "h": 400, "nm": name, "ddd": 0,
        "assets": [], "layers": list(reversed(layers)),
    }


def celebrate_motion(part, pos):
    bounce = [(0, [100, 100]), (15, [100, 84]), (30, [104, 110]), (45, [100, 100]), (59, [100, 100])]
    lift = [(0, pos), (15, [pos[0], pos[1] + 0]), (30, [pos[0], pos[1] - 26]), (45, pos), (59, pos)]
    if part.startswith("wing"):
        side = -1 if part == "wing_left" else 1
        rot = [(0, 0), (15, side * 35), (30, side * -15), (45, side * 35), (59, 0)]
        return rot, [(0, [100, 100])], lift
    return [(0, 0)], bounce, lift


def thinking_motion(part, pos):
    """Head tilts side to side (puzzled), eyes glance sideways, one wing scratches the head."""
    head_tilt = [(0, 0), (15, -9), (30, 0), (45, 9), (59, 0)]
    still = [(0, [100, 100])]
    if part == "wing_right":
        return [(0, 0), (20, -60), (40, -40), (59, 0)], still, [(0, pos)]
    if part.startswith("eye") or part.startswith("pupil"):
        glance = [(0, pos), (30, [pos[0] - 6, pos[1]]), (59, pos)]
        return [(0, 0)], still, glance
    return head_tilt, still, [(0, pos)]


def wave_motion(part, pos):
    if part == "wing_right":
        return [(0, 0), (10, -55), (20, -15), (30, -55), (40, -15), (50, -55), (59, 0)], \
               [(0, [100, 100])], [(0, pos)]
    if part == "wing_left":
        return [(0, 0), (59, 0)], [(0, [100, 100])], [(0, pos)]
    if part.startswith("eye") or part.startswith("pupil") or part == "beak":
        return [(0, 0)], [(0, [100, 100])], [(0, pos)]
    return [(0, 0), (30, 3), (59, 0)], [(0, [100, 100])], [(0, pos)]


def build_lottie():
    animations = {
        "mascot_celebrate": mascot_animation("celebrate", celebrate_motion),
        "mascot_thinking": mascot_animation("thinking", thinking_motion),
        "mascot_wave": mascot_animation("wave", wave_motion),
    }
    for name, document in animations.items():
        with open(os.path.join(RAW, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(document, f, ensure_ascii=False, separators=(",", ":"))
        print("lottie:", name + ".json")


def copy_master_data():
    os.makedirs(ASSETS, exist_ok=True)
    for name in ["vocabulary.json", "grammar.json", "reading.json", "listening.json"]:
        shutil.copyfile(os.path.join(DATA, name), os.path.join(ASSETS, name))
        print("asset:", name)


def main():
    os.makedirs(RAW, exist_ok=True)
    copy_master_data()
    with tempfile.TemporaryDirectory() as tmp:
        build_sound_effects(tmp)
        build_listening_audio(tmp)
    build_lottie()


if __name__ == "__main__":
    main()
