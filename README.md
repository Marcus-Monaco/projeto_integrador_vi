# Brick Breaker — Projeto Integrador VI-A

Aplicativo móvel de **Brick Breaker (Breakout)** desenvolvido para a disciplina de Projeto Integrador VI-A.
O jogador controla uma plataforma (*paddle*) para rebater a bola e destruir a parede de tijolos, avançando
por 5 níveis com paredes construídas por métodos diferentes.

> **Status:** entrega 2 — aplicativo implementado em Kotlin nativo, com código-fonte e APK assinado.

---

## Onde está o APK

| Arquivo | Onde |
| --- | --- |
| **APK de entrega (v1.0.0, assinado)** | [`apk/brick-breaker-v1.0.0.apk`](apk/brick-breaker-v1.0.0.apk) |

Para instalar, copie o arquivo para um celular Android 7.0 ou superior e abra-o (é preciso permitir a
instalação de fontes desconhecidas). Pelo computador, com o aparelho conectado:

```bash
adb install apk/brick-breaker-v1.0.0.apk
```

O APK fica versionado na pasta `apk/` para a entrega. O passo a passo para gerá-lo de novo está em
[doc 01, seção 8](docs/01-ambiente-tecnologias.md#8-geração-do-arquivo-apk).

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
| Marcus Sena | Motor do jogo (game loop, física, colisão) |
| Lucas Hoffman | Telas e navegação |
| Henrique Bin Estramar | Níveis e geração de paredes |
| Luan Bossardi | Áudio e assets |
| Mauricio Porgeri | Testes e geração do APK |

---

## O jogo em uma olhada

![Fluxo de navegação](docs/wireframes/00-fluxo-navegacao.svg)

---

## Requisitos do enunciado e onde estão atendidos

| # | Requisito | Documentação | Implementação |
| --- | --- | --- | --- |
| a | Ocupar a maior parte possível da tela | [WF-05](docs/wireframes/05-tela-de-jogo.svg) · [doc 02](docs/02-wireframes.md#a--aproveitamento-da-tela) | [`TelaCheiaActivity.kt`](app/src/main/java/br/edu/ucs/brickbreaker/ui/TelaCheiaActivity.kt) |
| b | Tela inicial com 3 opções (integrantes, jogar, configurações) | [WF-02](docs/wireframes/02-menu-principal.svg), [WF-03](docs/wireframes/03-integrantes.svg), [WF-04](docs/wireframes/04-configuracoes.svg) | [`MenuActivity.kt`](app/src/main/java/br/edu/ucs/brickbreaker/ui/MenuActivity.kt), [`IntegrantesActivity.kt`](app/src/main/java/br/edu/ucs/brickbreaker/ui/IntegrantesActivity.kt), [`ConfiguracoesActivity.kt`](app/src/main/java/br/edu/ucs/brickbreaker/ui/ConfiguracoesActivity.kt) |
| c | 5 paredes diferentes, uma por nível, com avanço automático | [WF-06 a WF-10](docs/02-wireframes.md#níveis-do-jogo) · [doc 03](docs/03-construcao-paredes.md#4-os-cinco-métodos-de-construção) | [`GeradorDeParede.kt`](app/src/main/java/br/edu/ucs/brickbreaker/niveis/GeradorDeParede.kt), [`JogoActivity.kt`](app/src/main/java/br/edu/ucs/brickbreaker/ui/JogoActivity.kt) |
| d | Controle de colisão — a bola não atravessa a parede de uma só vez | [doc 03](docs/03-construcao-paredes.md#7-controle-de-colisão-requisito-d) | [`Motor.kt`](app/src/main/java/br/edu/ucs/brickbreaker/jogo/Motor.kt), [`Colisao.kt`](app/src/main/java/br/edu/ucs/brickbreaker/jogo/Colisao.kt) |
| e | Som ao iniciar a fase e som ao bater no paddle | [WF-12](docs/wireframes/12-transicao-de-nivel.svg) e [WF-05](docs/wireframes/05-tela-de-jogo.svg) · [doc 01](docs/01-ambiente-tecnologias.md#5-recursos-de-hardware-utilizados) | [`Sons.kt`](app/src/main/java/br/edu/ucs/brickbreaker/audio/Sons.kt) |
| g | Ao perder a bola, perguntar se reinicia o nível ou vai para o próximo | [WF-11](docs/wireframes/11-dialogo-bola-perdida.svg) | [`JogoActivity.kt`](app/src/main/java/br/edu/ucs/brickbreaker/ui/JogoActivity.kt), [`overlay_bola_perdida.xml`](app/src/main/res/layout/overlay_bola_perdida.xml) |

---

## Como compilar e rodar

Requisitos: JDK 17 e Android SDK (API 35) — ambos já vêm com o Android Studio.

**Pelo Android Studio:** *File ▸ Open* ▸ pasta do projeto ▸ aguardar o Gradle Sync ▸ *Run* no emulador ou
no aparelho.

**Pela linha de comando:**

```bash
./gradlew testDebugUnitTest   # testes do gerador de paredes, da colisão e do motor
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk (assinado se houver keystore.properties)
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

---

## Estrutura do repositório

```
.
├── README.md                          índice da entrega
├── apk/
│   └── brick-breaker-v1.0.0.apk       APK de entrega assinado
├── app/
│   ├── build.gradle.kts               módulo Android (minSdk 24, targetSdk 35)
│   └── src/
│       ├── main/java/br/edu/ucs/brickbreaker/
│       │   ├── ui/                    Activities: splash, menu, integrantes, configurações, jogo, fim
│       │   ├── jogo/                  motor, colisão, laço de jogo, SurfaceView e renderização
│       │   ├── niveis/                parede, tijolo e os 5 métodos de construção
│       │   ├── dados/                 paletas e preferências (DataStore)
│       │   └── audio/                 SoundPool
│       ├── main/res/                  layouts, textos, cores, ícones e sons (raw/*.ogg)
│       └── test/                      testes unitários (JUnit)
├── docs/
│   ├── 01-ambiente-tecnologias.md     ambiente, stack e geração do APK
│   ├── 02-wireframes.md               catálogo dos wireframes
│   ├── 03-construcao-paredes.md       métodos de construção da parede
│   └── wireframes/                    15 pranchas SVG e paredes.json
├── gradle/wrapper/                    Gradle Wrapper (versão fixa para o grupo)
└── tools/
    ├── gen-wireframes.mjs             gerador dos wireframes e das paredes
    └── gerar-sons.py                  gerador dos dois efeitos sonoros
```

## Regerando os wireframes

Os wireframes são gerados por script, o que mantém o desenho das paredes idêntico ao que está descrito
na documentação. Requer apenas Node.js 18 ou superior:

```bash
node tools/gen-wireframes.mjs
```

Saída: os 15 arquivos `.svg` em `docs/wireframes/` e o resumo `paredes.json`.

As matrizes dos 5 níveis geradas pelo app são conferidas contra essas mesmas matrizes no teste
[`GeradorDeParedeTest.kt`](app/src/test/java/br/edu/ucs/brickbreaker/niveis/GeradorDeParedeTest.kt).

## Regerando os sons

Os dois efeitos sonoros são sintetizados por script (sem arquivos de terceiros). Requer Python 3 e ffmpeg:

```bash
python tools/gerar-sons.py
```

---

## Como contribuir (fluxo do grupo)

O fluxo completo e as convenções estão em [docs/01-ambiente-tecnologias.md](docs/01-ambiente-tecnologias.md#7-organização-do-trabalho-em-grupo).
