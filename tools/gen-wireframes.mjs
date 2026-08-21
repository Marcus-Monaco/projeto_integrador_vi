#!/usr/bin/env node
/**
 * gen-wireframes.mjs
 * Gera os wireframes do Brick Breaker em SVG (vetorial, alta definicao).
 *
 * Uso:  node tools/gen-wireframes.mjs
 * Saida: docs/wireframes/*.svg  +  docs/wireframes/paredes.json
 *
 * A malha de tijolos usada aqui e a MESMA descrita em docs/03-construcao-paredes.md.
 * Alterar um metodo de parede aqui mantem wireframe e documentacao sincronizados.
 */

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const OUT = join(ROOT, 'docs', 'wireframes');
mkdirSync(OUT, { recursive: true });

/* ------------------------------------------------------------------ */
/* tokens de estilo (escala de cinza + um unico acento p/ anotacoes)   */
/* ------------------------------------------------------------------ */
const T = {
  ink: '#1E1E1E',
  ink2: '#5A5A5A',
  ink3: '#8C8C8C',
  line: '#3A3A3A',
  hair: '#C8C8C8',
  f0: '#FFFFFF',
  f1: '#F4F4F4',
  f2: '#E6E6E6',
  f3: '#D0D0D0',
  f4: '#A9A9A9',
  f5: '#6E6E6E',
  f6: '#3F3F3F',
  accent: '#2A6FB5',
  accentSoft: '#E3EDF7',
  font: "'Segoe UI','Helvetica Neue',Arial,sans-serif",
  mono: "'Cascadia Mono','Consolas','DejaVu Sans Mono',monospace",
};

const CANVAS = { w: 1060, h: 1060 };
const DEV = { x: 60, y: 116, w: 360, h: 800, r: 26 };
const ANN = { x: 476, y: 116, w: 540 };

/* ------------------------------------------------------------------ */
/* primitivas svg                                                      */
/* ------------------------------------------------------------------ */
const r2 = (n) => Math.round(n * 100) / 100;
const esc = (s) =>
  String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const rect = (x, y, w, h, o = {}) =>
  `<rect x="${r2(x)}" y="${r2(y)}" width="${r2(w)}" height="${r2(h)}" rx="${o.rx ?? 0}" ` +
  `fill="${o.fill ?? 'none'}" stroke="${o.stroke ?? 'none'}" stroke-width="${o.sw ?? 1}"` +
  `${o.dash ? ` stroke-dasharray="${o.dash}"` : ''}${o.op ? ` opacity="${o.op}"` : ''}/>`;

const txt = (x, y, s, o = {}) =>
  `<text x="${r2(x)}" y="${r2(y)}" font-family="${o.mono ? T.mono : T.font}" ` +
  `font-size="${o.size ?? 13}" font-weight="${o.weight ?? 400}" fill="${o.fill ?? T.ink}" ` +
  `text-anchor="${o.anchor ?? 'start'}"${o.ls ? ` letter-spacing="${o.ls}"` : ''}>${esc(s)}</text>`;

const ln = (x1, y1, x2, y2, o = {}) =>
  `<line x1="${r2(x1)}" y1="${r2(y1)}" x2="${r2(x2)}" y2="${r2(y2)}" ` +
  `stroke="${o.stroke ?? T.hair}" stroke-width="${o.sw ?? 1}"` +
  `${o.dash ? ` stroke-dasharray="${o.dash}"` : ''}${o.marker ? ` marker-end="url(#seta)"` : ''}/>`;

const circ = (cx, cy, r, o = {}) =>
  `<circle cx="${r2(cx)}" cy="${r2(cy)}" r="${r2(r)}" fill="${o.fill ?? 'none'}" ` +
  `stroke="${o.stroke ?? 'none'}" stroke-width="${o.sw ?? 1}"${o.dash ? ` stroke-dasharray="${o.dash}"` : ''}/>`;

const path = (d, o = {}) =>
  `<path d="${d}" fill="${o.fill ?? 'none'}" stroke="${o.stroke ?? 'none'}" ` +
  `stroke-width="${o.sw ?? 1}"${o.dash ? ` stroke-dasharray="${o.dash}"` : ''}` +
  `${o.marker ? ' marker-end="url(#seta)"' : ''}/>`;

/** marcador numerado de anotacao */
const callout = (n, cx, cy, r = 11) =>
  circ(cx, cy, r, { fill: T.accent }) +
  txt(cx, cy + 4, n, { size: 11, weight: 700, fill: '#fff', anchor: 'middle' });

/** largura da tarja branca atras do rotulo de cota */
const caixaRotulo = (label) => Math.max(50, label.length * 6.4 + 10);

/** cota horizontal com rotulo */
function cotaH(x1, x2, y, label) {
  const m = (x1 + x2) / 2;
  const cw = caixaRotulo(label);
  return [
    ln(x1, y - 5, x1, y + 5, { stroke: T.accent }),
    ln(x2, y - 5, x2, y + 5, { stroke: T.accent }),
    ln(x1, y, x2, y, { stroke: T.accent, dash: '3 2' }),
    rect(m - cw / 2, y - 8, cw, 16, { fill: '#fff' }),
    txt(m, y + 4, label, { size: 10.5, fill: T.accent, anchor: 'middle', weight: 600 }),
  ].join('');
}

/** cota vertical com rotulo */
function cotaV(y1, y2, x, label) {
  const m = (y1 + y2) / 2;
  const cw = caixaRotulo(label);
  return [
    ln(x - 5, y1, x + 5, y1, { stroke: T.accent }),
    ln(x - 5, y2, x + 5, y2, { stroke: T.accent }),
    ln(x, y1, x, y2, { stroke: T.accent, dash: '3 2' }),
    rect(x - cw / 2, m - 8, cw, 16, { fill: '#fff' }),
    txt(x, m + 4, label, { size: 10.5, fill: T.accent, anchor: 'middle', weight: 600 }),
  ].join('');
}

/** quebra de linha aproximada por contagem de caracteres */
function wrap(s, max) {
  const out = [];
  let cur = '';
  for (const w of String(s).split(/\s+/)) {
    if (cur && (cur + ' ' + w).length > max) {
      out.push(cur);
      cur = w;
    } else cur = cur ? cur + ' ' + w : w;
  }
  if (cur) out.push(cur);
  return out;
}

const DEFS = `
<defs>
  <pattern id="hA" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
    <line x1="0" y1="0" x2="0" y2="6" stroke="#8C8C8C" stroke-width="1.2"/>
  </pattern>
  <pattern id="hB" width="5" height="5" patternUnits="userSpaceOnUse">
    <line x1="0" y1="0" x2="0" y2="5" stroke="#6E6E6E" stroke-width="1.1"/>
    <line x1="0" y1="0" x2="5" y2="0" stroke="#6E6E6E" stroke-width="1.1"/>
  </pattern>
  <pattern id="hX" width="4" height="4" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
    <line x1="0" y1="0" x2="0" y2="4" stroke="#EDEDED" stroke-width="1.6"/>
  </pattern>
  <pattern id="dots" width="8" height="8" patternUnits="userSpaceOnUse">
    <circle cx="1.5" cy="1.5" r="1" fill="#D8D8D8"/>
  </pattern>
  <marker id="seta" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
    <path d="M 0 0 L 10 5 L 0 10 z" fill="${T.accent}"/>
  </marker>
</defs>`;

/* ------------------------------------------------------------------ */
/* componentes de tela reutilizaveis                                   */
/* ------------------------------------------------------------------ */
const dx = DEV.x;
const dy = DEV.y;
const DW = DEV.w;
const DH = DEV.h;
const M = 16; // margem lateral padrao (dp)

/** faixa indicando que a status bar esta oculta (modo imersivo) */
const faixaImersiva = () =>
  [
    rect(dx, dy, DW, 20, { fill: 'url(#dots)' }),
    ln(dx, dy + 20, dx + DW, dy + 20, { stroke: T.hair, dash: '4 3' }),
    txt(dx + DW / 2, dy + 14, 'status bar oculta — immersive sticky (requisito a)', {
      size: 8.5,
      fill: T.ink3,
      anchor: 'middle',
    }),
  ].join('');

/** barra superior de navegacao com botao voltar */
const appBar = (y, titulo) =>
  [
    rect(dx, y, DW, 52, { fill: T.f1, stroke: T.hair }),
    path(`M ${dx + 28} ${y + 26} l 10 -8 M ${dx + 28} ${y + 26} l 10 8 M ${dx + 28} ${y + 26} h 16`, {
      stroke: T.ink,
      sw: 1.6,
    }),
    txt(dx + 62, y + 31, titulo, { size: 15, weight: 700 }),
  ].join('');

/** botao retangular */
function botao(x, y, w, h, label, tipo = 'sec') {
  const est =
    tipo === 'pri'
      ? { fill: T.f4, stroke: T.f6, sw: 1.6, tf: '#FFFFFF', ts: 17, tw: 800 }
      : tipo === 'ghost'
      ? { fill: 'none', stroke: T.f4, sw: 1.2, dash: '5 4', tf: T.ink2, ts: 13, tw: 600 }
      : { fill: T.f1, stroke: T.f4, sw: 1.4, tf: T.ink, ts: 15, tw: 700 };
  return [
    rect(x, y, w, h, { rx: 8, fill: est.fill, stroke: est.stroke, sw: est.sw, dash: est.dash }),
    txt(x + w / 2, y + h / 2 + est.ts / 3, label, {
      size: est.ts,
      weight: est.tw,
      fill: est.tf,
      anchor: 'middle',
      ls: 0.8,
    }),
  ].join('');
}

/** placeholder de imagem (retangulo com X) */
const imgBox = (x, y, w, h, label) =>
  [
    rect(x, y, w, h, { rx: 6, fill: T.f1, stroke: T.f4, sw: 1.3 }),
    ln(x, y, x + w, y + h, { stroke: T.f4 }),
    ln(x + w, y, x, y + h, { stroke: T.f4 }),
    label ? txt(x + w / 2, y + h + 14, label, { size: 10, fill: T.ink3, anchor: 'middle' }) : '',
  ].join('');

/** linha de texto simulada */
const textoFalso = (x, y, w, h = 8, tom = T.f3) => rect(x, y, w, h, { rx: h / 2, fill: tom });

/* ------------------------------------------------------------------ */
/* paredes de tijolos — os 5 metodos de construcao                     */
/* ------------------------------------------------------------------ */

/** PRNG deterministico (mulberry32) — mesma semente => mesma parede */
function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Metodo 1 — mapa fixo literal */
function metodoFixo(rows, cols, ch = '1') {
  return Array.from({ length: rows }, () => ch.repeat(cols));
}

/** Metodo 2 — meia matriz + espelhamento horizontal (garante simetria) */
function metodoEspelhado(meiaMatriz) {
  return meiaMatriz.map((h) => h + [...h].reverse().join(''));
}

/** Metodo 3 — regra matematica sobre (linha, coluna) */
function metodoRegra(rows, cols, regra) {
  const g = [];
  for (let i = 0; i < rows; i++) {
    let linha = '';
    for (let j = 0; j < cols; j++) linha += regra(i, j);
    g.push(linha);
  }
  return g;
}

/** Metodo 4 — procedural com semente, densidade e pesos de resistencia */
function metodoProcedural(rows, cols, seed, densidade, pesos) {
  const rnd = mulberry32(seed);
  const sorteia = () => {
    const r = rnd();
    let acc = 0;
    for (const [ch, p] of pesos) {
      acc += p;
      if (r <= acc) return ch;
    }
    return pesos[pesos.length - 1][0];
  };
  let g;
  let tentativa = 0;
  do {
    g = [];
    for (let i = 0; i < rows; i++) {
      let linha = '';
      for (let j = 0; j < cols; j++) linha += rnd() < densidade ? sorteia() : '.';
      g.push(linha);
    }
    tentativa++;
  } while (!validaParede(g) && tentativa < 50);
  return g;
}

/** Metodo 5 — hibrido: moldura fixa + miolo procedural */
function metodoHibrido(rows, cols, seed) {
  const rnd = mulberry32(seed);
  const g = [];
  for (let i = 0; i < rows; i++) {
    let linha = '';
    for (let j = 0; j < cols; j++) {
      if (i === 0) linha += '3';
      else if (i === rows - 1) linha += '2';
      else if (j === 0 || j === cols - 1) linha += 'X';
      else linha += rnd() < 0.72 ? (rnd() < 0.35 ? '2' : '1') : '.';
    }
    g.push(linha);
  }
  return g;
}

/** regras de solubilidade: nenhuma linha vazia e massa minima destrutivel */
function validaParede(g) {
  const destrutivel = g.join('').split('').filter((c) => '123'.includes(c)).length;
  const linhaVazia = g.some((l) => !/[123X]/.test(l));
  return destrutivel >= Math.floor(g.length * g[0].length * 0.45) && !linhaVazia;
}

/** reamostragem por vizinho mais proximo: malha canonica de 8 col -> 6 ou 10 col */
function reamostra(g, colsDestino) {
  const colsOrigem = g[0].length;
  if (colsOrigem === colsDestino) return g;
  return g.map((linha) => {
    let out = '';
    for (let j = 0; j < colsDestino; j++) {
      const src = Math.min(colsOrigem - 1, Math.round((j * (colsOrigem - 1)) / (colsDestino - 1)));
      out += linha[src];
    }
    return out;
  });
}

const ESTILO_TIJOLO = {
  1: { fill: T.f1, stroke: '#9A9A9A', pat: null },
  2: { fill: T.f2, stroke: '#7C7C7C', pat: 'hA' },
  3: { fill: T.f3, stroke: '#585858', pat: 'hB' },
  X: { fill: T.f5, stroke: '#242424', pat: 'hX' },
};

/** desenha a matriz de tijolos dentro de uma area */
function desenhaParede(g, { x, y, w, gap = 3, brickH = 18 }) {
  const cols = g[0].length;
  const bw = (w - gap * (cols - 1)) / cols;
  const els = [];
  const contagem = { 1: 0, 2: 0, 3: 0, X: 0 };
  for (let i = 0; i < g.length; i++) {
    for (let j = 0; j < cols; j++) {
      const ch = g[i][j];
      if (ch === '.') continue;
      const e = ESTILO_TIJOLO[ch];
      if (!e) continue;
      contagem[ch]++;
      const bx = x + j * (bw + gap);
      const by = y + i * (brickH + gap);
      els.push(rect(bx, by, bw, brickH, { rx: 2, fill: e.fill }));
      if (e.pat) els.push(rect(bx, by, bw, brickH, { rx: 2, fill: `url(#${e.pat})`, op: 0.9 }));
      els.push(rect(bx, by, bw, brickH, { rx: 2, stroke: e.stroke, sw: 1.1 }));
    }
  }
  const destrutivel = contagem[1] + contagem[2] + contagem[3];
  const golpes = contagem[1] + contagem[2] * 2 + contagem[3] * 3;
  return { els: els.join('\n'), contagem, destrutivel, golpes, bw, brickH, gap };
}

/** legenda de resistencia */
function legendaTijolos(x, y) {
  const itens = [
    ['1', 'resistência 1'],
    ['2', 'resistência 2'],
    ['3', 'resistência 3'],
    ['X', 'indestrutível'],
  ];
  const els = [txt(x, y - 6, 'LEGENDA', { size: 9, weight: 700, fill: T.ink3, ls: 1.2 })];
  itens.forEach(([ch, rot], i) => {
    const bx = x + i * 130;
    const e = ESTILO_TIJOLO[ch];
    els.push(rect(bx, y, 26, 12, { rx: 2, fill: e.fill }));
    if (e.pat) els.push(rect(bx, y, 26, 12, { rx: 2, fill: `url(#${e.pat})` }));
    els.push(rect(bx, y, 26, 12, { rx: 2, stroke: e.stroke, sw: 1.1 }));
    els.push(txt(bx + 32, y + 10, rot, { size: 10, fill: T.ink2 }));
  });
  return els.join('\n');
}

/* ------------------------------------------------------------------ */
/* montagem do artboard                                                */
/* ------------------------------------------------------------------ */
function renderAnotacoes(itens) {
  const out = [
    txt(ANN.x, ANN.y + 10, 'ANOTAÇÕES DE COMPORTAMENTO E ESPECIFICAÇÃO', {
      size: 10,
      weight: 700,
      fill: T.ink3,
      ls: 1.4,
    }),
    ln(ANN.x, ANN.y + 18, ANN.x + ANN.w, ANN.y + 18, { stroke: T.hair }),
  ];
  let y = ANN.y + 44;
  for (const it of itens) {
    if (it.tipo === 'secao') {
      y += 6;
      out.push(txt(ANN.x, y, it.texto, { size: 11, weight: 700, fill: T.accent, ls: 1.1 }));
      out.push(ln(ANN.x, y + 5, ANN.x + ANN.w, y + 5, { stroke: T.accentSoft, sw: 2 }));
      y += 22;
      continue;
    }
    if (it.tipo === 'mapa') {
      out.push(txt(ANN.x, y, it.titulo, { size: 10.5, weight: 700, fill: T.ink2 }));
      y += 6;
      const h = it.linhas.length * 13 + 14;
      out.push(rect(ANN.x, y, 210, h, { rx: 4, fill: '#FAFAFA', stroke: T.hair }));
      let my = y + 17;
      for (const l of it.linhas) {
        out.push(txt(ANN.x + 10, my, l, { size: 11.5, mono: true, fill: T.ink }));
        my += 13;
      }
      if (it.nota) {
        let ny = y + 17;
        for (const l of wrap(it.nota, 42)) {
          out.push(txt(ANN.x + 226, ny, l, { size: 10.5, fill: T.ink2 }));
          ny += 13;
        }
      }
      y += h + 16;
      continue;
    }
    out.push(callout(it.n, ANN.x + 10, y - 4));
    out.push(txt(ANN.x + 28, y, it.titulo, { size: 12, weight: 700 }));
    y += 15;
    for (const l of wrap(it.texto, 74)) {
      out.push(txt(ANN.x + 28, y, l, { size: 11, fill: T.ink2 }));
      y += 13;
    }
    y += 10;
  }
  return out.join('\n');
}

function artboard({ id, titulo, subtitulo, conteudo, anotacoes, requisitos, nota }) {
  const body = [
    rect(0, 0, CANVAS.w, CANVAS.h, { fill: '#FFFFFF' }),
    // cabecalho
    txt(60, 46, titulo, { size: 23, weight: 800, ls: -0.2 }),
    txt(60, 68, subtitulo, { size: 12, fill: T.ink2 }),
    txt(CANVAS.w - 44, 46, id, { size: 13, weight: 700, fill: T.accent, anchor: 'end', mono: true }),
    txt(CANVAS.w - 44, 68, 'Brick Breaker · Projeto Integrador VI-A · wireframe v1', {
      size: 10.5,
      fill: T.ink3,
      anchor: 'end',
    }),
    ln(60, 88, CANVAS.w - 44, 88, { stroke: T.line, sw: 1.4 }),
    // moldura do aparelho
    rect(DEV.x - 12, DEV.y - 12, DW + 24, DH + 24, {
      rx: DEV.r + 8,
      fill: '#FBFBFB',
      stroke: T.line,
      sw: 2,
    }),
    rect(DEV.x, DEV.y, DW, DH, { rx: 4, fill: '#FFFFFF', stroke: T.hair, sw: 1 }),
    conteudo,
    rect(DEV.x, DEV.y, DW, DH, { rx: 4, fill: 'none', stroke: T.line, sw: 1.2 }),
    txt(DEV.x + DW / 2, DEV.y + DH + 34, '360 × 800 dp  ·  1 px = 1 dp  ·  1080 × 2400 px @ xxhdpi', {
      size: 10,
      fill: T.ink3,
      anchor: 'middle',
    }),
    // anotacoes
    renderAnotacoes(anotacoes),
    // rodape
    ln(60, CANVAS.h - 66, CANVAS.w - 44, CANVAS.h - 66, { stroke: T.hair }),
    txt(60, CANVAS.h - 44, 'REQUISITO ATENDIDO', { size: 9.5, weight: 700, fill: T.ink3, ls: 1.2 }),
    txt(60, CANVAS.h - 26, requisitos, { size: 12, weight: 600, fill: T.ink }),
    nota ? txt(CANVAS.w - 44, CANVAS.h - 26, nota, { size: 10.5, fill: T.ink3, anchor: 'end' }) : '',
  ].join('\n');

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${CANVAS.w}" height="${CANVAS.h}" viewBox="0 0 ${CANVAS.w} ${CANVAS.h}" font-family="${T.font}">${DEFS}\n${body}\n</svg>\n`;
}

function salva(nome, svg) {
  writeFileSync(join(OUT, nome), svg, 'utf8');
  console.log('  ok  docs/wireframes/' + nome);
}

/* ================================================================== */
/* TELAS                                                              */
/* ================================================================== */

/* --- WF-01 Splash -------------------------------------------------- */
function wf01() {
  const cy = dy + 300;
  const conteudo = [
    rect(dx, dy, DW, DH, { fill: T.f1 }),
    faixaImersiva(),
    imgBox(dx + DW / 2 - 64, cy - 64, 128, 128, 'logotipo do jogo (vetorial)'),
    txt(dx + DW / 2, cy + 130, 'BRICK BREAKER', { size: 26, weight: 800, anchor: 'middle', ls: 3 }),
    txt(dx + DW / 2, cy + 154, 'Projeto Integrador VI-A', { size: 12, fill: T.ink2, anchor: 'middle' }),
    rect(dx + 100, dy + 620, 160, 6, { rx: 3, fill: T.f3 }),
    rect(dx + 100, dy + 620, 96, 6, { rx: 3, fill: T.f5 }),
    txt(dx + DW / 2, dy + 648, 'carregando recursos…', { size: 10.5, fill: T.ink3, anchor: 'middle' }),
    txt(dx + DW / 2, dy + 764, 'versão 1.0.0 · Grupo XX', { size: 10, fill: T.ink3, anchor: 'middle' }),
    callout(1, dx + DW / 2 - 92, cy),
    callout(2, dx + 88, dy + 623),
    callout(3, dx + 300, dy + 132),
  ].join('\n');

  const anotacoes = [
    { tipo: 'secao', texto: 'PROPÓSITO' },
    {
      n: 1,
      titulo: 'Identidade visual',
      texto:
        'Logotipo em vetor (VectorDrawable) para escalar sem perda em qualquer densidade. Fica centralizado vertical e horizontalmente, ocupando 128 × 128 dp.',
    },
    {
      n: 2,
      titulo: 'Pré-carregamento (duração fixa de 1,5 s)',
      texto:
        'Enquanto a barra avança, a Activity carrega o SoundPool (som de início de fase e som de rebatida no paddle), lê as preferências salvas de cor e tamanho de tijolo e infla as matrizes dos 5 níveis. Assim o primeiro nível inicia sem engasgo no game loop.',
    },
    {
      n: 3,
      titulo: 'Tela cheia desde o primeiro frame',
      texto:
        'Status bar e barra de navegação escondidas via WindowInsetsController (immersive sticky) já na SplashActivity, para não haver "pulo" de layout ao entrar no menu.',
    },
    { tipo: 'secao', texto: 'INTERAÇÃO' },
    {
      n: 4,
      titulo: 'Sem interação do usuário',
      texto:
        'A tela não responde a toques. Ao final do carregamento navega automaticamente para WF-02 (Menu principal), encerrando a si mesma (finish()) para não voltar na pilha.',
    },
  ];

  salva(
    '01-splash.svg',
    artboard({
      id: 'WF-01',
      titulo: 'Splash / Abertura',
      subtitulo: 'Primeira tela exibida ao abrir o aplicativo',
      conteudo,
      anotacoes,
      requisitos: '(a) ocupar a maior parte possível da tela · (e) pré-carga dos sons',
      nota: 'estado único',
    })
  );
}

/* --- WF-02 Menu principal ------------------------------------------ */
function wf02() {
  const bx = dx + 44;
  const bw = DW - 88;
  const conteudo = [
    faixaImersiva(),
    imgBox(dx + DW / 2 - 40, dy + 74, 80, 80, ''),
    txt(dx + DW / 2, dy + 194, 'BRICK BREAKER', { size: 24, weight: 800, anchor: 'middle', ls: 2.5 }),
    txt(dx + DW / 2, dy + 216, 'toque em uma das opções', { size: 11, fill: T.ink3, anchor: 'middle' }),

    botao(bx, dy + 268, bw, 66, 'JOGAR', 'pri'),
    botao(bx, dy + 350, bw, 58, 'CONFIGURAÇÕES'),
    botao(bx, dy + 424, bw, 58, 'INTEGRANTES'),
    botao(bx + 52, dy + 512, bw - 104, 40, 'SAIR', 'ghost'),

    txt(dx + DW / 2, dy + 764, 'v1.0.0', { size: 10, fill: T.ink3, anchor: 'middle' }),

    // cotas
    cotaH(bx, bx + bw, dy + 252, `${bw} dp`),
    cotaV(dy + 268, dy + 334, dx + 24, '66 dp'),
    cotaH(dx, bx, dy + 600, '44 dp'),

    callout(2, bx + bw + 26, dy + 301),
    callout(3, bx + bw + 26, dy + 379),
    callout(1, bx + bw + 26, dy + 453),
    callout(4, dx + DW / 2, dy + 532 + 10),
    callout(5, dx + 30, dy + 200),
  ].join('\n');

  const anotacoes = [
    { tipo: 'secao', texto: 'AS TRÊS OPÇÕES OBRIGATÓRIAS (requisito b)' },
    {
      n: 1,
      titulo: 'Opção 1 — Integrantes',
      texto:
        'Abre WF-03, que lista nome e sobrenome de todos os integrantes do grupo. Botão secundário: é informativo, não é o caminho principal.',
    },
    {
      n: 2,
      titulo: 'Opção 2 — Jogar',
      texto:
        'Botão primário (maior, alto contraste, no ponto de maior peso visual). Inicia sempre pelo nível 1 e aplica as configurações salvas. Vai para WF-12 (transição) e em seguida WF-05.',
    },
    {
      n: 3,
      titulo: 'Opção 3 — Configurações',
      texto:
        'Abre WF-04, onde o usuário escolhe o padrão de cores e o tamanho dos tijolos que serão usados na geração da parede. As escolhas ficam persistidas e valem para as partidas seguintes.',
    },
    { tipo: 'secao', texto: 'DEMAIS ELEMENTOS' },
    {
      n: 4,
      titulo: 'Sair (opcional, fora do enunciado)',
      texto:
        'Estilo terciário tracejado para deixar claro na revisão que é um extra e não uma das três opções exigidas.',
    },
    {
      n: 5,
      titulo: 'Área de marca',
      texto:
        'Logo + título ocupam o terço superior. Nenhum elemento interativo acima de 268 dp, o que mantém todos os botões dentro do alcance do polegar.',
    },
    { tipo: 'secao', texto: 'ESPECIFICAÇÃO' },
    {
      n: 6,
      titulo: 'Alvos de toque e espaçamento',
      texto:
        'Altura mínima de 48 dp em qualquer botão (recomendação de acessibilidade do Android); usamos 58–66 dp. Espaçamento vertical de 16 dp entre botões e margem lateral de 44 dp.',
    },
    {
      n: 7,
      titulo: 'Comportamento do botão físico "voltar"',
      texto:
        'Nesta tela o gesto de voltar encerra o aplicativo (com confirmação). Nas demais telas ele retorna ao menu.',
    },
  ];

  salva(
    '02-menu-principal.svg',
    artboard({
      id: 'WF-02',
      titulo: 'Menu principal',
      subtitulo: 'Tela inicial com as três opções exigidas no enunciado',
      conteudo,
      anotacoes,
      requisitos: '(b) tela inicial com 3 opções · (a) tela cheia',
      nota: 'ponto de entrada da navegação',
    })
  );
}

/* --- WF-03 Integrantes --------------------------------------------- */
function wf03() {
  // Nomes reais do grupo. Os marcados com [sobrenome] ainda precisam ser completados —
  // o enunciado exige NOME E SOBRENOME de todos os integrantes.
  const linhas = [
    ['Marcus Sena', 'Motor do jogo'],
    ['Lucas Hoffman', 'Telas e navegação'],
    ['Henrique [sobrenome]', 'Níveis e paredes'],
    ['Luan [sobrenome]', 'Áudio e assets'],
    ['Mauricio Porgeri', 'Testes e geração do APK'],
  ];
  const els = [faixaImersiva(), appBar(dy + 20, 'Integrantes do grupo')];
  els.push(txt(dx + M, dy + 108, 'GRUPO XX — TURMA XXXX', { size: 10, weight: 700, fill: T.ink3, ls: 1.2 }));
  linhas.forEach(([nome, papel], i) => {
    const y = dy + 126 + i * 78;
    els.push(rect(dx + M, y, DW - 2 * M, 66, { rx: 8, fill: T.f0, stroke: T.hair }));
    els.push(circ(dx + M + 33, y + 33, 21, { fill: T.f2, stroke: T.f4 }));
    const iniciais = nome
      .replace(/\[[^\]]*\]/g, '')
      .trim()
      .split(/\s+/)
      .map((p) => p[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
    els.push(txt(dx + M + 33, y + 38, iniciais, { size: 12, weight: 700, fill: T.ink2, anchor: 'middle' }));
    els.push(txt(dx + M + 68, y + 29, nome, { size: 14, weight: 700 }));
    els.push(txt(dx + M + 68, y + 47, papel, { size: 10.5, fill: T.ink2 }));
  });
  els.push(
    txt(dx + DW / 2, dy + 660, 'Projeto Integrador VI-A · 2026/2', {
      size: 10.5,
      fill: T.ink3,
      anchor: 'middle',
    })
  );
  els.push(botao(dx + 80, dy + 700, DW - 160, 44, 'VOLTAR AO MENU', 'ghost'));
  els.push(callout(1, dx + 24, dy + 46));
  els.push(callout(2, dx + DW - 26, dy + 159));
  els.push(callout(3, dx + DW - 26, dy + 722));

  const anotacoes = [
    { tipo: 'secao', texto: 'CONTEÚDO' },
    {
      n: 1,
      titulo: 'Barra com retorno explícito',
      texto:
        'Título da tela e seta de voltar. Evita depender apenas do gesto do sistema, que muda de aparelho para aparelho.',
    },
    {
      n: 2,
      titulo: 'Item da lista (repetido por integrante)',
      texto:
        'Cada cartão traz obrigatoriamente NOME e SOBRENOME em destaque (14 sp, peso 700). As linhas secundárias — RA e frente de trabalho — são complementares e podem ser removidas sem quebrar o layout.',
    },
    {
      n: 3,
      titulo: 'Retorno redundante',
      texto:
        'Botão de voltar ao final da lista, para quem rolou até o fim não precisar subir de novo.',
    },
    { tipo: 'secao', texto: 'IMPLEMENTAÇÃO' },
    {
      n: 4,
      titulo: 'Origem dos dados',
      texto:
        'RecyclerView alimentado por uma lista imutável declarada em Integrantes.kt (ou res/values/integrantes.xml). Nenhum acesso a rede: os dados são estáticos e versionados junto do código.',
    },
    {
      n: 5,
      titulo: 'Rolagem',
      texto:
        'A lista rola verticalmente caso o grupo tenha mais integrantes do que cabe na tela. O rodapé acompanha a rolagem (não é fixo).',
    },
    {
      n: 6,
      titulo: 'Placeholder de avatar',
      texto:
        'Círculo com as iniciais, gerado em tempo de execução. Não depende de nenhuma imagem no APK.',
    },
  ];

  salva(
    '03-integrantes.svg',
    artboard({
      id: 'WF-03',
      titulo: 'Integrantes do grupo',
      subtitulo: 'Opção 1 do menu — nome e sobrenome de todos os integrantes',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: '(b) opção 1 — lista de nome e sobrenome de todos os integrantes',
      nota: 'preencher com os nomes reais antes da entrega',
    })
  );
}

/* --- WF-04 Configuracoes ------------------------------------------- */
function wf04() {
  const els = [faixaImersiva(), appBar(dy + 20, 'Configurações')];
  const cx = dx + M;
  const cw = DW - 2 * M;

  // secao cores
  els.push(txt(cx, dy + 108, 'PADRÃO DE CORES DOS TIJOLOS', { size: 10, weight: 700, fill: T.ink3, ls: 1.1 }));
  const paletas = ['Clássico', 'Neon', 'Mono', 'Livre'];
  paletas.forEach((p, i) => {
    const w = (cw - 18) / 4;
    const x = cx + i * (w + 6);
    const sel = i === 0;
    els.push(rect(x, dy + 120, w, 34, { rx: 6, fill: sel ? T.f4 : T.f1, stroke: sel ? T.f6 : T.f4, sw: sel ? 1.6 : 1.1 }));
    els.push(
      txt(x + w / 2, dy + 142, p, {
        size: 11,
        weight: sel ? 800 : 600,
        fill: sel ? '#fff' : T.ink,
        anchor: 'middle',
      })
    );
  });
  // amostras
  els.push(rect(cx, dy + 164, cw, 58, { rx: 8, fill: T.f0, stroke: T.hair }));
  els.push(txt(cx + 12, dy + 183, 'cores por resistência do tijolo', { size: 10, fill: T.ink3 }));
  ['1', '2', '3', 'X'].forEach((ch, i) => {
    const e = ESTILO_TIJOLO[ch];
    const bx2 = cx + 12 + i * 58;
    els.push(rect(bx2, dy + 192, 48, 20, { rx: 3, fill: e.fill }));
    if (e.pat) els.push(rect(bx2, dy + 192, 48, 20, { rx: 3, fill: `url(#${e.pat})` }));
    els.push(rect(bx2, dy + 192, 48, 20, { rx: 3, stroke: e.stroke, sw: 1.1 }));
  });
  els.push(txt(cx + cw - 12, dy + 206, 'editar ▸', { size: 10.5, fill: T.accent, anchor: 'end', weight: 700 }));

  // secao tamanho
  els.push(txt(cx, dy + 256, 'TAMANHO DOS TIJOLOS', { size: 10, weight: 700, fill: T.ink3, ls: 1.1 }));
  const tam = [
    ['Pequeno', '10 col'],
    ['Médio', '8 col'],
    ['Grande', '6 col'],
  ];
  tam.forEach(([t, sub], i) => {
    const w = (cw - 12) / 3;
    const x = cx + i * (w + 6);
    const sel = i === 1;
    els.push(rect(x, dy + 268, w, 62, { rx: 8, fill: sel ? T.accentSoft : T.f0, stroke: sel ? T.accent : T.hair, sw: sel ? 1.8 : 1.1 }));
    const bh = i === 0 ? 8 : i === 1 ? 11 : 15;
    const bwid = i === 0 ? 22 : i === 1 ? 30 : 40;
    els.push(rect(x + w / 2 - bwid / 2, dy + 282, bwid, bh, { rx: 2, fill: T.f2, stroke: T.f5 }));
    els.push(txt(x + w / 2, dy + 312, t, { size: 11.5, weight: 700, anchor: 'middle' }));
    els.push(txt(x + w / 2, dy + 325, sub, { size: 9.5, fill: T.ink3, anchor: 'middle' }));
  });

  // slider altura
  els.push(txt(cx, dy + 358, 'ALTURA DO TIJOLO', { size: 10, weight: 700, fill: T.ink3, ls: 1.1 }));
  els.push(txt(cx + cw, dy + 358, '18 dp', { size: 11, weight: 700, fill: T.ink, anchor: 'end' }));
  els.push(rect(cx, dy + 374, cw, 4, { rx: 2, fill: T.f3 }));
  els.push(rect(cx, dy + 374, cw * 0.5, 4, { rx: 2, fill: T.f5 }));
  els.push(circ(cx + cw * 0.5, dy + 376, 10, { fill: T.f0, stroke: T.f6, sw: 1.6 }));
  els.push(txt(cx, dy + 398, '12 dp', { size: 9.5, fill: T.ink3 }));
  els.push(txt(cx + cw, dy + 398, '26 dp', { size: 9.5, fill: T.ink3, anchor: 'end' }));

  // preview
  els.push(txt(cx, dy + 434, 'PRÉ-VISUALIZAÇÃO DA PAREDE', { size: 10, weight: 700, fill: T.ink3, ls: 1.1 }));
  els.push(rect(cx, dy + 446, cw, 132, { rx: 8, fill: T.f1, stroke: T.hair }));
  const prev = desenhaParede(metodoFixo(4, 8, '1'), { x: cx + 12, y: dy + 462, w: cw - 24, brickH: 18, gap: 3 });
  els.push(prev.els);
  els.push(txt(cx + cw / 2, dy + 568, 'atualiza em tempo real a cada alteração', { size: 9.5, fill: T.ink3, anchor: 'middle' }));

  // acoes
  els.push(botao(cx, dy + 600, cw / 2 - 6, 48, 'RESTAURAR', 'ghost'));
  els.push(botao(cx + cw / 2 + 6, dy + 600, cw / 2 - 6, 48, 'SALVAR', 'pri'));
  els.push(txt(dx + DW / 2, dy + 676, 'As escolhas valem para todos os níveis da próxima partida.', { size: 10, fill: T.ink3, anchor: 'middle' }));

  els.push(callout(1, dx + DW - 26, dy + 137));
  els.push(callout(2, dx + DW - 26, dy + 193));
  els.push(callout(3, dx + DW - 26, dy + 299));
  els.push(callout(4, dx + DW - 26, dy + 376));
  els.push(callout(5, dx + DW - 26, dy + 512));
  els.push(callout(6, dx + DW - 26, dy + 624));

  const anotacoes = [
    { tipo: 'secao', texto: 'PADRÃO DE CORES (requisito c da opção 3)' },
    {
      n: 1,
      titulo: 'Paletas pré-definidas + modo livre',
      texto:
        'Três paletas prontas (Clássico, Neon, Mono) atendem a parte "opções pré-definidas" do enunciado; a opção "Livre" abre um seletor HSV e atende a parte "escolhidas livremente pelo usuário".',
    },
    {
      n: 2,
      titulo: 'Uma cor por nível de resistência',
      texto:
        'A paleta define 4 cores, uma para cada tipo de tijolo (resistência 1, 2, 3 e indestrutível). É esse mapa cor↔resistência que a geração da parede consome. No wireframe as cores aparecem como cinzas e hachuras, propositalmente.',
    },
    { tipo: 'secao', texto: 'TAMANHO DOS TIJOLOS' },
    {
      n: 3,
      titulo: 'Tamanho = número de colunas',
      texto:
        'Pequeno = 10 colunas, Médio = 8 (padrão), Grande = 6. A largura do tijolo é calculada, nunca fixada: larguraTijolo = (larguraÚtil − espaçamento × (colunas − 1)) / colunas. Isso mantém a parede encaixada em qualquer tela.',
    },
    {
      n: 4,
      titulo: 'Altura independente da largura',
      texto:
        'Slider de 12 a 26 dp. Alterar a altura muda quantas fileiras cabem na área da parede; o gerador recalcula as fileiras e reamostra os mapas fixos.',
    },
    { tipo: 'secao', texto: 'FEEDBACK E PERSISTÊNCIA' },
    {
      n: 5,
      titulo: 'Pré-visualização viva',
      texto:
        'Mostra um recorte real da parede com as opções atuais. Reduz o risco de o usuário salvar uma combinação ilegível (ex.: tijolo escuro sobre fundo escuro).',
    },
    {
      n: 6,
      titulo: 'Salvar / Restaurar',
      texto:
        'Gravado em DataStore (Preferences) nas chaves paleta, colunas e alturaTijolo. "Restaurar" volta a Clássico / Médio / 18 dp. Sair sem salvar descarta as alterações.',
    },
  ];

  salva(
    '04-configuracoes.svg',
    artboard({
      id: 'WF-04',
      titulo: 'Configurações',
      subtitulo: 'Opção 3 do menu — padrão de cores e tamanho dos tijolos',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: '(b) opção 3 — configuração de cores e tamanhos usados na geração da parede',
      nota: 'alimenta diretamente docs/03-construcao-paredes.md',
    })
  );
}

/* --- WF-05 Tela de jogo (anatomia) --------------------------------- */
function wf05() {
  const els = [faixaImersiva()];
  // HUD
  els.push(rect(dx, dy + 20, DW, 44, { fill: T.f1, stroke: T.hair }));
  els.push(txt(dx + 12, dy + 40, 'NÍVEL', { size: 8.5, weight: 700, fill: T.ink3, ls: 1 }));
  els.push(txt(dx + 12, dy + 56, '1 / 5', { size: 15, weight: 800 }));
  els.push(txt(dx + DW / 2, dy + 40, 'PONTOS', { size: 8.5, weight: 700, fill: T.ink3, ls: 1, anchor: 'middle' }));
  els.push(txt(dx + DW / 2, dy + 56, '001 250', { size: 15, weight: 800, anchor: 'middle' }));
  [0, 1, 2].forEach((i) => {
    els.push(circ(dx + DW - 78 + i * 18, dy + 50, 6, { fill: i < 2 ? T.f5 : 'none', stroke: T.f5, sw: 1.4 }));
  });
  els.push(txt(dx + DW - 84, dy + 40, 'VIDAS', { size: 8.5, weight: 700, fill: T.ink3, ls: 1 }));
  els.push(rect(dx + DW - 34, dy + 32, 24, 24, { rx: 4, fill: T.f2, stroke: T.f4 }));
  els.push(rect(dx + DW - 28, dy + 38, 4, 12, { fill: T.ink2 }));
  els.push(rect(dx + DW - 21, dy + 38, 4, 12, { fill: T.ink2 }));

  // area de jogo
  const gy = dy + 64;
  const gh = DH - 64;
  els.push(rect(dx, gy, DW, gh, { fill: T.f0 }));

  // parede
  const parede = desenhaParede(metodoFixo(5, 8, '1'), { x: dx + 12, y: gy + 24, w: DW - 24, brickH: 18, gap: 3 });
  els.push(parede.els);

  // bola e trajetoria
  const balls = { x: dx + 150, y: gy + 380 };
  els.push(path(`M ${dx + 96} ${gy + 560} L ${balls.x} ${balls.y} L ${dx + 250} ${gy + 200}`, { stroke: T.accent, sw: 1.2, dash: '5 4' }));
  els.push(circ(balls.x, balls.y, 7, { fill: T.f6 }));

  // paddle
  const py = gy + 600;
  els.push(rect(dx + DW / 2 - 46, py, 92, 14, { rx: 7, fill: T.f4, stroke: T.f6, sw: 1.4 }));

  // zona de controle
  els.push(rect(dx, py + 34, DW, gh - (py + 34 - gy), { fill: 'url(#dots)', op: 0.9 }));
  els.push(rect(dx, py + 34, DW, gh - (py + 34 - gy), { stroke: T.accent, sw: 1.2, dash: '6 4' }));
  els.push(txt(dx + DW / 2, py + 66, 'zona de controle — arraste para mover o paddle', { size: 10, fill: T.accent, anchor: 'middle', weight: 600 }));
  els.push(txt(dx + DW / 2, py + 82, 'o dedo não cobre a bola nem os tijolos', { size: 9.5, fill: T.ink3, anchor: 'middle' }));

  // linha de morte
  els.push(ln(dx, py + 30, dx + DW, py + 30, { stroke: T.ink, sw: 1.4, dash: '2 3' }));
  els.push(txt(dx + DW - 6, py + 26, 'linha de perda', { size: 9, fill: T.ink2, anchor: 'end' }));

  // cotas
  els.push(cotaV(dy + 20, dy + 64, dx - 32, '44 dp'));
  els.push(cotaV(gy, py + 30, dx - 32, '630 dp'));
  els.push(cotaH(dx + DW / 2 - 46, dx + DW / 2 + 46, py + 26, '92 × 14'));
  els.push(cotaH(dx + 12, dx + DW - 12, gy + 14, `largura útil ${DW - 24} dp`));

  // callouts
  els.push(callout(1, dx + DW - 26, dy + 42));
  els.push(callout(2, dx + DW - 26, gy + 60));
  els.push(callout(3, balls.x - 22, balls.y));
  els.push(callout(4, dx + DW / 2 + 70, py + 7));
  els.push(callout(5, dx + 26, py + 66));
  els.push(callout(6, dx + 26, gy + 340));

  const anotacoes = [
    { tipo: 'secao', texto: 'ESTRUTURA DA TELA' },
    {
      n: 1,
      titulo: 'HUD fixo — 44 dp',
      texto:
        'Nível atual/total, pontuação e vidas restantes. Único elemento fora da superfície de desenho: é um overlay em View comum sobre o SurfaceView, para não ser redesenhado a 60 fps.',
    },
    {
      n: 2,
      titulo: 'Parede de tijolos',
      texto:
        'Ocupa a largura útil inteira (328 dp) e começa 24 dp abaixo do HUD. Quantidade de colunas e altura do tijolo vêm das Configurações (WF-04). O layout de cada nível está em WF-06 a WF-10.',
    },
    {
      n: 3,
      titulo: 'Bola — raio 7 dp',
      texto:
        'A linha tracejada indica a trajetória, não é desenhada no jogo. O deslocamento por quadro é limitado a metade da menor dimensão de um tijolo, o que impede a bola de atravessar a parede em um único passo (requisito d).',
    },
    {
      n: 4,
      titulo: 'Paddle — 92 × 14 dp',
      texto:
        'O ângulo de saída da bola depende de onde ela bate no paddle: centro devolve na vertical, extremidades abrem o ângulo até 60°. Toda colisão aqui dispara o som de rebatida (requisito e).',
    },
    {
      n: 5,
      titulo: 'Zona de controle por arrasto — 120 dp',
      texto:
        'Faixa inferior reservada ao dedo. O paddle acompanha o eixo X do toque com suavização; a área garante que a mão não cubra a bola. Toque fora dessa faixa não move o paddle.',
    },
    {
      n: 6,
      titulo: 'Aproveitamento da tela (requisito a)',
      texto:
        'Sem status bar, sem barra de navegação e sem barra de título: a superfície de jogo usa 95% da altura da tela. O canvas é escalado proporcionalmente para qualquer resolução, mantendo a razão de aspecto da área jogável.',
    },
    { tipo: 'secao', texto: 'MOTOR' },
    {
      n: 7,
      titulo: 'Laço de jogo',
      texto:
        'Thread dedicada sobre SurfaceHolder, passo fixo de 16 ms (60 fps) com acumulador de tempo. Atualiza física, resolve colisões, desenha. Pausa automaticamente em onPause() da Activity.',
    },
    {
      n: 8,
      titulo: 'Sons (requisito e)',
      texto:
        'SoundPool com dois efeitos curtos em .ogg: início de fase (disparado na transição, WF-12) e rebatida no paddle. Volume respeita o volume de mídia do aparelho.',
    },
  ];

  salva(
    '05-tela-de-jogo.svg',
    artboard({
      id: 'WF-05',
      titulo: 'Tela de jogo — anatomia',
      subtitulo: 'Estrutura, medidas e zonas de interação comuns a todos os níveis',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: '(a) tela cheia · (d) colisão controlada · (e) sons de fase e de rebatida',
      nota: 'gabarito para WF-06 a WF-10',
    })
  );
}

/* --- WF-06..10 Niveis ---------------------------------------------- */
const NIVEIS = [
  {
    id: 'WF-06',
    arq: '06-nivel-1.svg',
    n: 1,
    nome: 'Muralha',
    metodo: 'Mapa fixo literal',
    g: metodoFixo(5, 8, '1'),
    resumo:
      'A parede mais simples possível: matriz cheia, todos os tijolos com resistência 1. Serve para o jogador aprender o controle do paddle sem punição.',
    detalhe:
      'O mapa é uma constante no código (array de strings). Nenhuma aleatoriedade: o nível é idêntico em toda partida, o que facilita testar o motor de colisão.',
    dificuldade: 1,
  },
  {
    id: 'WF-07',
    arq: '07-nivel-2.svg',
    n: 2,
    nome: 'Pirâmide',
    metodo: 'Meia matriz + espelhamento',
    g: metodoEspelhado(['...1', '..11', '.222', '2222', '3333']),
    resumo:
      'Só metade da parede é declarada; a outra metade é o espelho horizontal da primeira. A simetria sai de graça e o mapa a manter é metade do tamanho.',
    detalhe:
      'A resistência cresce com a profundidade: as fileiras de baixo (mais fáceis de alcançar) são as mais duras, forçando o jogador a abrir caminho pelas laterais.',
    dificuldade: 2,
  },
  {
    id: 'WF-08',
    arq: '08-nivel-3.svg',
    n: 3,
    nome: 'Xadrez',
    metodo: 'Regra matemática sobre (linha, coluna)',
    g: metodoRegra(6, 8, (i, j) => ((i + j) % 2 === 0 ? (i < 2 ? '1' : i < 4 ? '2' : '3') : '.')),
    resumo:
      'Nenhum mapa é armazenado: cada célula é decidida por uma função pura de (linha, coluna). Trocar a regra troca a parede inteira em uma linha de código.',
    detalhe:
      'Os vãos alternados deixam a bola entrar na parede e ricochetear lá dentro, o que produz sequências longas de destruição e exige o controle de "uma colisão por passo".',
    dificuldade: 3,
  },
  {
    id: 'WF-09',
    arq: '09-nivel-4.svg',
    n: 4,
    nome: 'Campo minado',
    metodo: 'Procedural com semente fixa',
    g: metodoProcedural(6, 8, 20260821, 0.7, [
      ['1', 0.5],
      ['2', 0.32],
      ['3', 0.18],
    ]),
    resumo:
      'Gerada por sorteio, mas com semente fixa: a parede tem cara de aleatória e mesmo assim é reproduzível em qualquer aparelho e em qualquer execução.',
    detalhe:
      'Densidade de 70% e pesos de resistência 50/32/18. Depois de gerada, a matriz passa por uma validação: se ficar rala demais ou com fileira vazia, é descartada e sorteada de novo.',
    dificuldade: 4,
  },
  {
    id: 'WF-10',
    arq: '10-nivel-5.svg',
    n: 5,
    nome: 'Fortaleza',
    metodo: 'Híbrido — moldura fixa + miolo procedural',
    g: metodoHibrido(6, 8, 5150),
    resumo:
      'Combina os dois mundos: o contorno é declarado à mão (teto duro e colunas indestrutíveis) e o interior é sorteado. Nível final, o mais difícil.',
    detalhe:
      'Os tijolos indestrutíveis não entram na contagem de conclusão do nível — o jogador vence ao zerar apenas os destrutíveis. As colunas indestrutíveis estreitam o corredor da bola.',
    dificuldade: 5,
  },
];

function wfNivel(cfg) {
  const els = [faixaImersiva()];
  els.push(rect(dx, dy + 20, DW, 44, { fill: T.f1, stroke: T.hair }));
  els.push(txt(dx + 12, dy + 40, 'NÍVEL', { size: 8.5, weight: 700, fill: T.ink3, ls: 1 }));
  els.push(txt(dx + 12, dy + 56, `${cfg.n} / 5`, { size: 15, weight: 800 }));
  els.push(txt(dx + DW / 2, dy + 50, cfg.nome.toUpperCase(), { size: 12, weight: 700, anchor: 'middle', ls: 1.6, fill: T.ink2 }));
  [0, 1, 2].forEach((i) => els.push(circ(dx + DW - 60 + i * 18, dy + 44, 6, { fill: T.f5 })));

  const gy = dy + 64;
  const parede = desenhaParede(cfg.g, { x: dx + 12, y: gy + 24, w: DW - 24, brickH: 18, gap: 3 });
  els.push(parede.els);

  const alturaParede = cfg.g.length * 21 - 3;
  els.push(cotaV(gy + 24, gy + 24 + alturaParede, dx - 30, `${alturaParede} dp`));
  els.push(cotaH(dx + 12, dx + DW - 12, gy + 14, `${cfg.g[0].length} colunas`));

  // bola + paddle em posicao inicial
  const py = gy + 600;
  els.push(rect(dx + DW / 2 - 46, py, 92, 14, { rx: 7, fill: T.f4, stroke: T.f6, sw: 1.4 }));
  els.push(circ(dx + DW / 2, py - 12, 7, { fill: T.f6 }));
  els.push(path(`M ${dx + DW / 2} ${py - 20} l -14 -26 M ${dx + DW / 2} ${py - 20} l 14 -26`, { stroke: T.accent, sw: 1.2, dash: '4 3' }));
  els.push(txt(dx + DW / 2, py + 44, 'posição inicial: bola parada sobre o paddle', { size: 9.5, fill: T.ink3, anchor: 'middle' }));
  els.push(txt(dx + DW / 2, py + 58, 'primeiro toque lança a bola', { size: 9.5, fill: T.ink3, anchor: 'middle' }));

  els.push(legendaTijolos(dx + 12, gy + 470));

  const anotacoes = [
    { tipo: 'secao', texto: `MÉTODO DE CONSTRUÇÃO — ${cfg.metodo.toUpperCase()}` },
    { n: 1, titulo: cfg.metodo, texto: cfg.resumo },
    { n: 2, titulo: 'Detalhe do desenho do nível', texto: cfg.detalhe },
    {
      tipo: 'mapa',
      titulo: 'MATRIZ DO NÍVEL (malha canônica de 8 colunas)',
      linhas: cfg.g,
      nota:
        `Legenda: 1, 2, 3 = resistência (número de acertos) · X = indestrutível · . = vazio.\n` +
        `Tijolos destrutíveis: ${parede.destrutivel}. Indestrutíveis: ${parede.contagem.X}. ` +
        `Acertos necessários para concluir o nível: ${parede.golpes}.`,
    },
    {
      n: 3,
      titulo: 'Adaptação ao tamanho escolhido pelo usuário',
      texto:
        'A matriz acima é a versão canônica de 8 colunas. Se o usuário escolher tijolo Pequeno (10 col) ou Grande (6 col), o mapa é reamostrado por vizinho mais próximo na horizontal, o que preserva a silhueta e a simetria do desenho.',
    },
    {
      n: 4,
      titulo: 'Conclusão do nível',
      texto:
        'O nível termina quando o contador de tijolos destrutíveis chega a zero. A transição para o próximo nível é automática (WF-12), sem passar pelo menu — exigência do enunciado.',
    },
    {
      n: 5,
      titulo: 'Curva de dificuldade',
      texto:
        `Nível ${cfg.n} de 5 — dificuldade ${'●'.repeat(cfg.dificuldade)}${'○'.repeat(5 - cfg.dificuldade)}. ` +
        'A velocidade base da bola sobe 8% a cada nível concluído, com teto na velocidade que ainda permite uma varredura de colisão segura.',
    },
  ];

  salva(
    cfg.arq,
    artboard({
      id: cfg.id,
      titulo: `Nível ${cfg.n} — ${cfg.nome}`,
      subtitulo: `Parede de blocos construída por: ${cfg.metodo.toLowerCase()}`,
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: '(c) 5 paredes diferentes, uma por nível, com avanço automático',
      nota: `${parede.destrutivel} tijolos destrutíveis · ${parede.golpes} acertos`,
    })
  );

  return {
    nivel: cfg.n,
    nome: cfg.nome,
    metodo: cfg.metodo,
    linhas: cfg.g.length,
    colunas: cfg.g[0].length,
    mapa: cfg.g,
    destrutiveis: parede.destrutivel,
    indestrutiveis: parede.contagem.X,
    acertos: parede.golpes,
    por_resistencia: parede.contagem,
    dificuldade: cfg.dificuldade,
  };
}

/* --- WF-11 Dialogo de bola perdida --------------------------------- */
function wf11() {
  const els = [faixaImersiva()];
  // jogo esmaecido ao fundo
  els.push(rect(dx, dy + 20, DW, 44, { fill: T.f1, stroke: T.hair, op: 0.45 }));
  els.push(txt(dx + 12, dy + 50, 'NÍVEL 3 / 5', { size: 12, weight: 700, fill: T.ink3, op: 0.5 }));
  const gy = dy + 64;
  const bg = desenhaParede(metodoRegra(6, 8, (i, j) => ((i + j) % 2 === 0 ? '1' : '.')), {
    x: dx + 12,
    y: gy + 24,
    w: DW - 24,
    brickH: 18,
    gap: 3,
  });
  els.push(`<g opacity="0.28">${bg.els}</g>`);
  els.push(rect(dx, dy, DW, DH, { fill: '#1E1E1E', op: 0.42 }));

  // cartao
  const cw = DW - 48;
  const cxx = dx + 24;
  const cy0 = dy + 236;
  els.push(rect(cxx, cy0, cw, 330, { rx: 14, fill: '#FFFFFF', stroke: T.line, sw: 1.6 }));
  els.push(circ(dx + DW / 2, cy0 + 54, 24, { fill: T.f1, stroke: T.f4, sw: 1.4 }));
  els.push(circ(dx + DW / 2, cy0 + 54, 8, { fill: T.f5 }));
  els.push(txt(dx + DW / 2, cy0 + 104, 'Você perdeu a bola', { size: 18, weight: 800, anchor: 'middle' }));
  els.push(txt(dx + DW / 2, cy0 + 126, 'O que deseja fazer agora?', { size: 12, fill: T.ink2, anchor: 'middle' }));
  els.push(botao(cxx + 20, cy0 + 148, cw - 40, 52, 'REINICIAR O NÍVEL 3', 'pri'));
  els.push(botao(cxx + 20, cy0 + 210, cw - 40, 52, 'IR PARA O NÍVEL 4'));
  els.push(botao(cxx + 20, cy0 + 274, cw - 40, 38, 'VOLTAR AO MENU', 'ghost'));

  els.push(callout(1, dx + DW / 2, cy0 + 54 - 40));
  els.push(callout(2, cxx - 12, cy0 + 174));
  els.push(callout(3, cxx - 12, cy0 + 236));
  els.push(callout(4, cxx - 12, cy0 + 293));
  els.push(callout(5, dx + DW - 24, dy + 140));

  const anotacoes = [
    { tipo: 'secao', texto: 'GATILHO' },
    {
      n: 1,
      titulo: 'Quando aparece',
      texto:
        'No instante em que a bola cruza a linha de perda sem tocar o paddle. O laço de jogo é pausado (não encerrado): a parede, a pontuação e a posição do paddle continuam em memória.',
    },
    { tipo: 'secao', texto: 'AS DUAS ESCOLHAS EXIGIDAS (requisito g)' },
    {
      n: 2,
      titulo: 'Reiniciar o nível atual',
      texto:
        'Reconstrói a parede do nível corrente pelo mesmo método (mesma semente, portanto parede idêntica), devolve a bola ao paddle e toca de novo o som de início de fase.',
    },
    {
      n: 3,
      titulo: 'Passar para o próximo nível',
      texto:
        'Avança sem exigir que o nível atual seja concluído. O texto do botão nomeia o nível de destino ("IR PARA O NÍVEL 4") para não restar dúvida. No nível 5 este botão é substituído por "FINALIZAR PARTIDA".',
    },
    {
      n: 4,
      titulo: 'Saída de emergência',
      texto: 'Terceira opção, terciária, para abandonar a partida e voltar ao menu principal.',
    },
    { tipo: 'secao', texto: 'ESPECIFICAÇÃO DO DIÁLOGO' },
    {
      n: 5,
      titulo: 'Modal bloqueante',
      texto:
        'Fundo escurecido a 42% de opacidade sobre o jogo congelado, deixando o contexto visível. O diálogo não é cancelável: nem o toque fora dele nem o gesto de voltar o fecham, porque o enunciado exige uma escolha explícita.',
    },
    {
      n: 6,
      titulo: 'Vidas',
      texto:
        'Cada bola perdida consome uma vida. Zeradas as vidas, em vez deste diálogo é exibida a tela de fim de jogo (WF-14).',
    },
  ];

  salva(
    '11-dialogo-bola-perdida.svg',
    artboard({
      id: 'WF-11',
      titulo: 'Diálogo — bola perdida',
      subtitulo: 'A bola não foi rebatida: reiniciar o nível atual ou passar para o próximo',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: '(g) perguntar ao usuário se deseja reiniciar o nível atual ou ir para o próximo',
      nota: 'sobreposição — não é uma tela nova',
    })
  );
}

/* --- WF-12 Transicao de nivel -------------------------------------- */
function wf12() {
  const els = [faixaImersiva()];
  const gy = dy + 64;
  const bg = desenhaParede(metodoFixo(5, 8, '1'), { x: dx + 12, y: gy + 24, w: DW - 24, brickH: 18, gap: 3 });
  els.push(`<g opacity="0.22">${bg.els}</g>`);
  els.push(rect(dx, dy, DW, DH, { fill: '#1E1E1E', op: 0.55 }));

  els.push(txt(dx + DW / 2, dy + 300, 'NÍVEL 2', { size: 40, weight: 800, fill: '#FFFFFF', anchor: 'middle', ls: 4 }));
  els.push(txt(dx + DW / 2, dy + 334, 'PIRÂMIDE', { size: 14, weight: 700, fill: '#E0E0E0', anchor: 'middle', ls: 3 }));
  els.push(ln(dx + 120, dy + 356, dx + 240, dy + 356, { stroke: '#9A9A9A' }));
  els.push(txt(dx + DW / 2, dy + 386, '28 tijolos · 3 vidas restantes', { size: 12, fill: '#D0D0D0', anchor: 'middle' }));
  els.push(circ(dx + DW / 2, dy + 470, 34, { stroke: '#FFFFFF', sw: 2 }));
  els.push(txt(dx + DW / 2, dy + 484, '3', { size: 30, weight: 800, fill: '#FFFFFF', anchor: 'middle' }));
  els.push(txt(dx + DW / 2, dy + 542, 'preparando…', { size: 11.5, fill: '#C0C0C0', anchor: 'middle' }));
  els.push(rect(dx + 24, dy + 640, DW - 48, 44, { rx: 8, fill: 'none', stroke: '#8A8A8A', sw: 1.2, dash: '5 4' }));
  els.push(txt(dx + DW / 2, dy + 667, 'toque para começar imediatamente', { size: 11, fill: '#D8D8D8', anchor: 'middle', weight: 600 }));

  els.push(callout(1, dx + 40, dy + 300));
  els.push(callout(2, dx + 40, dy + 470));
  els.push(callout(3, dx + 40, dy + 662));

  const anotacoes = [
    { tipo: 'secao', texto: 'AVANÇO AUTOMÁTICO (requisito c)' },
    {
      n: 1,
      titulo: 'Sem intervenção do usuário',
      texto:
        'Assim que o último tijolo destrutível do nível anterior é destruído, esta sobreposição aparece e, ao fim da contagem, o próximo nível começa sozinho. Em nenhum momento se volta ao menu.',
    },
    {
      n: 2,
      titulo: 'Contagem regressiva de 3 s',
      texto:
        'Dá tempo de o jogador reposicionar o polegar e ler o layout novo da parede, que já está desenhado ao fundo com opacidade reduzida.',
    },
    {
      n: 3,
      titulo: 'Pular a contagem',
      texto: 'Um toque em qualquer ponto encerra a contagem e lança a bola imediatamente.',
    },
    { tipo: 'secao', texto: 'SOM (requisito e)' },
    {
      n: 4,
      titulo: 'Som de início de fase',
      texto:
        'Disparado uma única vez no primeiro frame desta sobreposição, incluindo o início do nível 1 e todo reinício de nível pedido no diálogo WF-11. É o primeiro dos dois sons exigidos pelo enunciado.',
    },
    { tipo: 'secao', texto: 'ESTADO' },
    {
      n: 5,
      titulo: 'O que é reiniciado',
      texto:
        'Parede reconstruída pelo método do novo nível, bola recolocada sobre o paddle, velocidade base aumentada em 8%. Pontuação e vidas são cumulativas e não zeram entre níveis.',
    },
  ];

  salva(
    '12-transicao-de-nivel.svg',
    artboard({
      id: 'WF-12',
      titulo: 'Transição de nível',
      subtitulo: 'Sobreposição exibida entre o fim de um nível e o início automático do próximo',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: '(c) ao final do nível o próximo inicia automaticamente · (e) som de início de fase',
      nota: 'sobreposição — não é uma tela nova',
    })
  );
}

/* --- WF-13 Pausa ---------------------------------------------------- */
function wf13() {
  const els = [faixaImersiva()];
  const gy = dy + 64;
  const bg = desenhaParede(metodoFixo(5, 8, '1'), { x: dx + 12, y: gy + 24, w: DW - 24, brickH: 18, gap: 3 });
  els.push(`<g opacity="0.22">${bg.els}</g>`);
  els.push(rect(dx, dy, DW, DH, { fill: '#1E1E1E', op: 0.5 }));

  const cw = DW - 64;
  const cxx = dx + 32;
  els.push(rect(cxx, dy + 220, cw, 340, { rx: 14, fill: '#FFFFFF', stroke: T.line, sw: 1.6 }));
  els.push(txt(dx + DW / 2, dy + 262, 'PAUSA', { size: 18, weight: 800, anchor: 'middle', ls: 3 }));
  els.push(txt(dx + DW / 2, dy + 284, 'Nível 3 · 001 250 pontos', { size: 11.5, fill: T.ink2, anchor: 'middle' }));
  els.push(botao(cxx + 20, dy + 302, cw - 40, 48, 'CONTINUAR', 'pri'));
  els.push(botao(cxx + 20, dy + 360, cw - 40, 44, 'REINICIAR NÍVEL'));
  els.push(botao(cxx + 20, dy + 414, cw - 40, 44, 'CONFIGURAÇÕES'));
  els.push(botao(cxx + 20, dy + 468, cw - 40, 44, 'SAIR PARA O MENU', 'ghost'));

  els.push(callout(1, dx + DW - 30, dy + 42));
  els.push(callout(2, cxx - 14, dy + 326));
  els.push(callout(3, cxx - 14, dy + 436));

  const anotacoes = [
    { tipo: 'secao', texto: 'ACESSO' },
    {
      n: 1,
      titulo: 'Botão de pausa no HUD',
      texto:
        'Ícone de 24 dp no canto superior direito, com área de toque expandida para 48 dp. A pausa também é acionada automaticamente em onPause() — quando chega uma ligação ou o usuário troca de aplicativo.',
    },
    { tipo: 'secao', texto: 'OPÇÕES' },
    {
      n: 2,
      titulo: 'Continuar',
      texto:
        'Retoma exatamente o estado congelado: posição e vetor da bola, posição do paddle, tijolos restantes. Uma contagem curta de 1 s antecede a retomada.',
    },
    {
      n: 3,
      titulo: 'Configurações durante a partida',
      texto:
        'Abre WF-04. Mudanças de cor valem imediatamente; mudanças de tamanho de tijolo só valem a partir do próximo nível, para não recriar a parede no meio da jogada.',
    },
    { tipo: 'secao', texto: 'OBSERVAÇÃO' },
    {
      n: 4,
      titulo: 'Tela complementar',
      texto:
        'A pausa não é exigida pelo enunciado, mas é necessária na prática: sem ela, uma interrupção do sistema faria o jogador perder a bola sem ter errado.',
    },
  ];

  salva(
    '13-pausa.svg',
    artboard({
      id: 'WF-13',
      titulo: 'Pausa',
      subtitulo: 'Sobreposição de pausa durante a partida',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: 'complementar — suporte ao ciclo de vida da Activity',
      nota: 'sobreposição — não é uma tela nova',
    })
  );
}

/* --- WF-14 Fim de jogo ---------------------------------------------- */
function wf14() {
  const els = [faixaImersiva()];
  els.push(rect(dx, dy, DW, DH, { fill: T.f1 }));
  els.push(txt(dx + DW / 2, dy + 150, 'FIM DE JOGO', { size: 28, weight: 800, anchor: 'middle', ls: 3 }));
  els.push(txt(dx + DW / 2, dy + 176, 'variante A — vidas esgotadas', { size: 11, fill: T.accent, anchor: 'middle', weight: 700 }));
  els.push(ln(dx + 60, dy + 196, dx + DW - 60, dy + 196, { stroke: T.hair }));

  const linhas = [
    ['Pontuação final', '004 380'],
    ['Níveis concluídos', '4 de 5'],
    ['Tijolos destruídos', '138'],
    ['Melhor pontuação', '005 120'],
  ];
  linhas.forEach(([k, v], i) => {
    const y = dy + 232 + i * 46;
    els.push(txt(dx + M + 8, y, k, { size: 12.5, fill: T.ink2 }));
    els.push(txt(dx + DW - M - 8, y, v, { size: 14, weight: 800, anchor: 'end' }));
    els.push(ln(dx + M, y + 14, dx + DW - M, y + 14, { stroke: T.hair, dash: '2 3' }));
  });

  els.push(botao(dx + 44, dy + 448, DW - 88, 56, 'JOGAR NOVAMENTE', 'pri'));
  els.push(botao(dx + 44, dy + 516, DW - 88, 48, 'MENU PRINCIPAL'));

  els.push(rect(dx + 24, dy + 600, DW - 48, 116, { rx: 10, fill: 'none', stroke: T.accent, sw: 1.2, dash: '6 4' }));
  els.push(txt(dx + DW / 2, dy + 626, 'VARIANTE B — VITÓRIA', { size: 10.5, weight: 700, fill: T.accent, anchor: 'middle', ls: 1.2 }));
  els.push(txt(dx + DW / 2, dy + 650, 'Mesmo layout, com o título trocado por', { size: 10.5, fill: T.ink2, anchor: 'middle' }));
  els.push(txt(dx + DW / 2, dy + 668, '"VOCÊ VENCEU!" e "Níveis concluídos: 5 de 5".', { size: 10.5, fill: T.ink2, anchor: 'middle' }));
  els.push(txt(dx + DW / 2, dy + 694, 'Alcançada ao concluir o nível 5 com vidas restantes.', { size: 10.5, fill: T.ink3, anchor: 'middle' }));

  els.push(callout(1, dx + 28, dy + 148));
  els.push(callout(2, dx + 28, dy + 300));
  els.push(callout(3, dx + 28, dy + 476));
  els.push(callout(4, dx + 12, dy + 626));

  const anotacoes = [
    { tipo: 'secao', texto: 'DUAS VARIANTES DA MESMA TELA' },
    {
      n: 1,
      titulo: 'Variante A — fim de jogo',
      texto:
        'Exibida quando as vidas acabam. Substitui o diálogo WF-11, que só faz sentido enquanto ainda há vidas.',
    },
    {
      n: 4,
      titulo: 'Variante B — vitória',
      texto:
        'Exibida ao concluir o nível 5. Mesma estrutura, apenas título e mensagem diferentes — por isso é um único layout com duas variantes, e não duas telas.',
    },
    { tipo: 'secao', texto: 'CONTEÚDO' },
    {
      n: 2,
      titulo: 'Resumo da partida',
      texto:
        'Pontuação final, níveis concluídos e tijolos destruídos. A melhor pontuação fica gravada em DataStore e sobrevive ao fechamento do aplicativo.',
    },
    {
      n: 3,
      titulo: 'Ações',
      texto:
        'Jogar novamente reinicia do nível 1 zerando pontuação e vidas. Menu principal volta a WF-02. Não há caminho de volta para a partida encerrada.',
    },
  ];

  salva(
    '14-fim-de-jogo.svg',
    artboard({
      id: 'WF-14',
      titulo: 'Fim de jogo / Vitória',
      subtitulo: 'Encerramento da partida — duas variantes do mesmo layout',
      conteudo: els.join('\n'),
      anotacoes,
      requisitos: 'complementar — fechamento do ciclo de partida',
      nota: 'variantes A e B',
    })
  );
}

/* --- WF-00 Fluxo de navegacao --------------------------------------- */
function wf00() {
  const W = 1240;
  const H = 860;
  const nos = [
    { id: 'WF-01', t: 'Splash', x: 70, y: 120, w: 150, h: 62 },
    { id: 'WF-02', t: 'Menu principal', x: 290, y: 120, w: 170, h: 62, forte: true },
    { id: 'WF-03', t: 'Integrantes', x: 290, y: 250, w: 170, h: 56 },
    { id: 'WF-04', t: 'Configurações', x: 290, y: 344, w: 170, h: 56 },
    { id: 'WF-12', t: 'Transição de nível', x: 560, y: 120, w: 180, h: 62 },
    { id: 'WF-05', t: 'Tela de jogo', x: 840, y: 120, w: 180, h: 62, forte: true },
    { id: 'WF-13', t: 'Pausa', x: 840, y: 262, w: 180, h: 52 },
    { id: 'WF-11', t: 'Bola perdida', x: 840, y: 388, w: 180, h: 56 },
    { id: 'WF-14', t: 'Fim de jogo', x: 840, y: 522, w: 180, h: 56 },
    { id: 'WF-06…10', t: 'Níveis 1 a 5', x: 560, y: 262, w: 180, h: 76 },
  ];
  const els = [rect(0, 0, W, H, { fill: '#FFFFFF' })];
  els.push(txt(70, 52, 'Fluxo de navegação', { size: 23, weight: 800 }));
  els.push(txt(70, 74, 'Mapa das telas e das transições entre elas', { size: 12, fill: T.ink2 }));
  els.push(txt(W - 50, 52, 'WF-00', { size: 13, weight: 700, fill: T.accent, anchor: 'end', mono: true }));
  els.push(txt(W - 50, 74, 'Brick Breaker · Projeto Integrador VI-A', { size: 10.5, fill: T.ink3, anchor: 'end' }));
  els.push(ln(70, 92, W - 50, 92, { stroke: T.line, sw: 1.4 }));

  const byId = Object.fromEntries(nos.map((n) => [n.id, n]));
  const seta = (a, b, rot, lado = 'h') => {
    const A = byId[a];
    const B = byId[b];
    let d;
    if (lado === 'h') {
      const y = A.y + A.h / 2;
      d = `M ${A.x + A.w + 4} ${y} H ${B.x - 8}`;
      els.push(path(d, { stroke: T.accent, sw: 1.4, marker: true }));
      if (rot)
        els.push(
          txt((A.x + A.w + B.x) / 2, y - 8, rot, { size: 10, fill: T.accent, anchor: 'middle', weight: 600 })
        );
    } else {
      const x = A.x + A.w / 2;
      d = `M ${x} ${A.y + A.h + 4} V ${B.y - 8}`;
      els.push(path(d, { stroke: T.accent, sw: 1.4, marker: true }));
      if (rot) els.push(txt(x + 8, (A.y + A.h + B.y) / 2 + 4, rot, { size: 10, fill: T.accent, weight: 600 }));
    }
  };

  // arestas
  seta('WF-01', 'WF-02', 'auto');
  seta('WF-02', 'WF-12', 'JOGAR');
  seta('WF-12', 'WF-05', 'auto');
  seta('WF-02', 'WF-03', 'INTEGRANTES', 'v');
  seta('WF-03', 'WF-04', '', 'v');
  seta('WF-05', 'WF-13', 'pausa', 'v');
  seta('WF-13', 'WF-11', '', 'v');
  seta('WF-11', 'WF-14', 'sem vidas', 'v');
  // retornos manuais
  els.push(
    path(`M 375 250 V 200 H 375`, { stroke: T.ink3, sw: 1.2, dash: '4 3', marker: true }),
    path(`M 930 120 C 930 30, 560 30, 560 110`, { stroke: T.accent, sw: 1.4, dash: '5 4', marker: true }),
    txt(745, 24, 'nível concluído → próximo nível (automático, requisito c)', {
      size: 10.5,
      fill: T.accent,
      anchor: 'middle',
      weight: 700,
    }),
    path(`M 1020 416 C 1120 416, 1120 170, 1030 152`, { stroke: T.accent, sw: 1.4, marker: true }),
    txt(1130, 300, 'reiniciar nível', { size: 10, fill: T.accent, anchor: 'middle', weight: 600 }),
    txt(1130, 314, 'ou pular (req. g)', { size: 10, fill: T.accent, anchor: 'middle', weight: 600 }),
    path(`M 840 550 C 640 600, 420 560, 375 190`, { stroke: T.ink3, sw: 1.2, dash: '5 4', marker: true }),
    txt(600, 590, 'voltar ao menu', { size: 10, fill: T.ink3, weight: 600 }),
    path(`M 650 262 V 200`, { stroke: T.hair, sw: 1.2, dash: '3 3' })
  );

  for (const n of nos) {
    els.push(
      rect(n.x, n.y, n.w, n.h, {
        rx: 10,
        fill: n.forte ? T.f2 : T.f0,
        stroke: n.forte ? T.line : T.f4,
        sw: n.forte ? 1.8 : 1.2,
      })
    );
    els.push(txt(n.x + n.w / 2, n.y + 26, n.t, { size: 13, weight: 700, anchor: 'middle' }));
    els.push(txt(n.x + n.w / 2, n.y + 44, n.id, { size: 10, fill: T.ink3, anchor: 'middle', mono: true }));
  }
  // caixa de niveis: mini indicadores
  [1, 2, 3, 4, 5].forEach((i) => {
    els.push(rect(576 + (i - 1) * 30, 316, 24, 12, { rx: 2, fill: T.f2, stroke: T.f5 }));
    els.push(txt(588 + (i - 1) * 30, 325, String(i), { size: 8, anchor: 'middle', fill: T.ink2, weight: 700 }));
  });

  // legenda
  els.push(rect(70, 640, 520, 150, { rx: 10, fill: '#FAFAFA', stroke: T.hair }));
  els.push(txt(88, 668, 'LEGENDA', { size: 10, weight: 700, fill: T.ink3, ls: 1.2 }));
  els.push(ln(88, 690, 128, 690, { stroke: T.accent, sw: 1.4, marker: true }));
  els.push(txt(140, 694, 'transição automática ou ação principal', { size: 11, fill: T.ink2 }));
  els.push(ln(88, 716, 128, 716, { stroke: T.ink3, sw: 1.2, dash: '5 4', marker: true }));
  els.push(txt(140, 720, 'retorno acionado pelo usuário', { size: 11, fill: T.ink2 }));
  els.push(rect(88, 736, 40, 16, { rx: 4, fill: T.f2, stroke: T.line, sw: 1.6 }));
  els.push(txt(140, 749, 'tela central do aplicativo', { size: 11, fill: T.ink2 }));
  els.push(txt(88, 772, 'WF-11, WF-12 e WF-13 são sobreposições sobre WF-05, não Activities novas.', { size: 10.5, fill: T.ink3 }));

  els.push(rect(626, 640, W - 676, 150, { rx: 10, fill: '#FAFAFA', stroke: T.hair }));
  els.push(txt(644, 668, 'RASTREABILIDADE COM O ENUNCIADO', { size: 10, weight: 700, fill: T.ink3, ls: 1.2 }));
  [
    '(a) tela cheia — todas as telas usam modo imersivo',
    '(b) três opções na tela inicial — WF-02 → WF-03, WF-12, WF-04',
    '(c) cinco paredes + avanço automático — WF-06 a WF-10 e WF-12',
    '(d) colisão controlada — WF-05',
    '(e) dois sons — WF-12 (fase) e WF-05 (paddle)',
    '(g) reiniciar ou pular nível — WF-11',
  ].forEach((s, i) => els.push(txt(644, 692 + i * 17, s, { size: 10.5, fill: T.ink2 })));

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" font-family="${T.font}">${DEFS}\n${els.join('\n')}\n</svg>\n`;
  salva('00-fluxo-navegacao.svg', svg);
}

/* ------------------------------------------------------------------ */
/* execucao                                                            */
/* ------------------------------------------------------------------ */
console.log('Gerando wireframes…');
wf00();
wf01();
wf02();
wf03();
wf04();
wf05();
const resumoNiveis = NIVEIS.map(wfNivel);
wf11();
wf12();
wf13();
wf14();

// exemplos de reamostragem para a documentacao
const exemplo = NIVEIS[1].g;
const resumo = {
  gerado_em: new Date().toISOString().slice(0, 10),
  malha_canonica: 8,
  tamanhos: { Pequeno: 10, 'Médio': 8, Grande: 6 },
  niveis: resumoNiveis,
  exemplo_reamostragem: {
    origem_8: exemplo,
    destino_10: reamostra(exemplo, 10),
    destino_6: reamostra(exemplo, 6),
  },
};
writeFileSync(join(OUT, 'paredes.json'), JSON.stringify(resumo, null, 2), 'utf8');
console.log('  ok  docs/wireframes/paredes.json');

console.log('\nResumo das paredes:');
for (const n of resumoNiveis) {
  console.log(
    `  Nível ${n.nivel} ${n.nome.padEnd(13)} ${n.metodo.padEnd(38)} ` +
      `${n.linhas}x${n.colunas}  destrutíveis=${String(n.destrutiveis).padStart(3)}  ` +
      `indestrutíveis=${String(n.indestrutiveis).padStart(2)}  acertos=${String(n.acertos).padStart(3)}`
  );
}
