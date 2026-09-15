"""Gera os dois efeitos sonoros do jogo (requisito e) por sintese, sem arquivos de terceiros.

    python tools/gerar-sons.py [caminho-do-ffmpeg]

Saida: app/src/main/res/raw/inicio_fase.ogg e rebatida.ogg (OGG Vorbis, mono, 44,1 kHz, < 1 s).
Requer apenas Python 3 e o ffmpeg (para a conversao WAV -> OGG).
"""
import math
import struct
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

TAXA = 44100
RAIZ = Path(__file__).resolve().parent.parent
DESTINO = RAIZ / "app" / "src" / "main" / "res" / "raw"


def tom(freq, duracao, volume=0.5, ataque=0.005, decaimento=6.0, onda="quadrada"):
    amostras = []
    n = int(TAXA * duracao)
    for i in range(n):
        t = i / TAXA
        envelope = min(1.0, t / ataque) * math.exp(-decaimento * t)
        fase = 2 * math.pi * freq * t
        if onda == "quadrada":
            # quadrada suavizada: os tres primeiros harmonicos impares
            v = math.sin(fase) + math.sin(3 * fase) / 3 + math.sin(5 * fase) / 5
            v *= 0.75
        else:
            v = math.sin(fase)
        amostras.append(v * envelope * volume)
    return amostras


def inicio_de_fase():
    # arpejo ascendente do-mi-sol-do: sinaliza o comeco do nivel
    notas = [523.25, 659.25, 783.99, 1046.50]
    saida = []
    for i, f in enumerate(notas):
        dur = 0.11 if i < 3 else 0.30
        saida += tom(f, dur, volume=0.45, decaimento=5.0 if i < 3 else 4.0)
    return saida


def rebatida():
    # "toc" curto e grave, com leve queda de frequencia
    n = int(TAXA * 0.09)
    saida = []
    fase = 0.0
    for i in range(n):
        t = i / TAXA
        freq = 420 - 1800 * t
        fase += 2 * math.pi * freq / TAXA
        envelope = min(1.0, t / 0.002) * math.exp(-38 * t)
        saida.append(math.sin(fase) * envelope * 0.8)
    return saida


def gravar_wav(caminho, amostras):
    with wave.open(str(caminho), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(TAXA)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, a)) * 32767)) for a in amostras))


def main():
    ffmpeg = sys.argv[1] if len(sys.argv) > 1 else "ffmpeg"
    DESTINO.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for nome, gerador in (("inicio_fase", inicio_de_fase), ("rebatida", rebatida)):
            wav = Path(tmp) / f"{nome}.wav"
            gravar_wav(wav, gerador())
            ogg = DESTINO / f"{nome}.ogg"
            subprocess.run(
                [ffmpeg, "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "5", "-ac", "1", "-ar", str(TAXA), str(ogg)],
                check=True,
            )
            print(f"ok  {ogg.relative_to(RAIZ)}")


if __name__ == "__main__":
    main()
