# Brick Breaker — Projeto Integrador VI-A

Aplicativo móvel de **Brick Breaker (Breakout)** desenvolvido para a disciplina de Projeto Integrador VI-A.
O jogador controla uma plataforma (*paddle*) para rebater a bola e destruir a parede de tijolos, avançando
por 5 níveis com paredes construídas por métodos diferentes.

> **Status:** entrega 1 — projeto e prototipação (documentação + wireframes).
> A implementação do aplicativo é a etapa seguinte.

---

## Documentação

| Documento | Conteúdo |
| --- | --- |
| **[01 — Ambiente, tecnologias e geração do APK](docs/01-ambiente-tecnologias.md)** | Ambiente de desenvolvimento, linguagens, bibliotecas, justificativa das escolhas, recursos de hardware acessados e o passo a passo para gerar o arquivo `.apk` de entrega. |
| **[02 — Wireframes](docs/02-wireframes.md)** | Wireframes em alta definição de todas as telas do aplicativo, incluindo os 5 níveis, o fluxo de navegação e as anotações de comportamento. |
| **[03 — Construção da parede de blocos](docs/03-construcao-paredes.md)** | Como a parede é modelada e gerada: os 5 métodos de construção, a matriz de cada nível, a integração com as configurações do usuário e o controle de colisão. |

---

## Integrantes do grupo

| Nome e sobrenome | Frente de trabalho |
| --- | --- |
| Marcus *[sobrenome]* | Motor do jogo (game loop, física, colisão) |
| Lucas Hoffman | Telas e navegação |
| Henrique *[sobrenome]* | Níveis e geração de paredes |
| Luan *[sobrenome]* | Áudio e assets |
| Mauricio Porgeri | Testes e geração do APK |

> ⚠️ **Faltam três sobrenomes.** O enunciado exige nome **e sobrenome** de todos os integrantes.
> Ao completar, atualizar em dois lugares: esta tabela e o array `linhas` em
> [`tools/gen-wireframes.mjs`](tools/gen-wireframes.mjs) (função `wf03`), rodando em seguida
> `node tools/gen-wireframes.mjs` para atualizar o wireframe
> [WF-03](docs/wireframes/03-integrantes.svg).
>
> A divisão de frentes de trabalho acima é uma proposta — ajustem conforme o combinado do grupo.

---

## O jogo em uma olhada

![Fluxo de navegação](docs/wireframes/00-fluxo-navegacao.svg)

---

## Requisitos do enunciado e onde estão atendidos

| # | Requisito | Onde está documentado |
| --- | --- | --- |
| a | Ocupar a maior parte possível da tela | [WF-05](docs/wireframes/05-tela-de-jogo.svg) · [doc 02](docs/02-wireframes.md#a--aproveitamento-da-tela) |
| b | Tela inicial com 3 opções (integrantes, jogar, configurações) | [WF-02](docs/wireframes/02-menu-principal.svg), [WF-03](docs/wireframes/03-integrantes.svg), [WF-04](docs/wireframes/04-configuracoes.svg) |
| c | 5 paredes diferentes, uma por nível, com avanço automático | [WF-06 a WF-10](docs/02-wireframes.md#níveis-do-jogo) · [doc 03](docs/03-construcao-paredes.md#4-os-cinco-métodos-de-construção) |
| d | Controle de colisão — a bola não atravessa a parede de uma só vez | [doc 03](docs/03-construcao-paredes.md#7-controle-de-colisão-requisito-d) |
| e | Som ao iniciar a fase e som ao bater no paddle | [WF-12](docs/wireframes/12-transicao-de-nivel.svg) e [WF-05](docs/wireframes/05-tela-de-jogo.svg) · [doc 01](docs/01-ambiente-tecnologias.md#5-recursos-de-hardware-utilizados) |
| g | Ao perder a bola, perguntar se reinicia o nível ou vai para o próximo | [WF-11](docs/wireframes/11-dialogo-bola-perdida.svg) |

---

## Estrutura do repositório

```
.
├── README.md                        índice desta entrega
├── docs/
│   ├── 01-ambiente-tecnologias.md   ambiente, stack e geração do APK
│   ├── 02-wireframes.md             catálogo dos wireframes
│   ├── 03-construcao-paredes.md     métodos de construção da parede
│   └── wireframes/
│       ├── 00-fluxo-navegacao.svg   mapa das telas
│       ├── 01-splash.svg … 14-fim-de-jogo.svg
│       └── paredes.json             dados das 5 paredes (gerados)
└── tools/
    └── gen-wireframes.mjs           gerador dos wireframes e das paredes
```

## Regerando os wireframes

Os wireframes são gerados por script, o que mantém o desenho das paredes idêntico ao que está descrito
na documentação. Requer apenas Node.js 18 ou superior:

```bash
node tools/gen-wireframes.mjs
```

Saída: os 15 arquivos `.svg` em `docs/wireframes/` e o resumo `paredes.json`.

---

## Como contribuir (fluxo do grupo)

1. `git switch -c feat/<assunto>` a partir de `main`
2. Commits pequenos e descritivos em português
3. Abrir Pull Request para `main` e pedir revisão de outro integrante
4. `main` sempre em estado entregável

O fluxo completo e as convenções estão em [docs/01-ambiente-tecnologias.md](docs/01-ambiente-tecnologias.md#7-organização-do-trabalho-em-grupo).
