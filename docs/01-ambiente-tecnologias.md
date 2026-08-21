# 01 — Ambiente de desenvolvimento, tecnologias e geração do APK

[◀ voltar ao índice](../README.md)

Este documento registra **onde** o aplicativo é desenvolvido, **com o quê** é escrito e **como** o arquivo
`.apk` de entrega é produzido.

---

## 1. Resumo da decisão

| Item | Escolha |
| --- | --- |
| Plataforma alvo | Android (telefone, orientação retrato travada) |
| Linguagem principal | **Kotlin** |
| IDE | **Android Studio** (Ladybug 2024.2 ou superior) |
| Kit de desenvolvimento | Android SDK · JDK 17 (embutido no Android Studio) |
| Sistema de build | Gradle (Kotlin DSL, `build.gradle.kts`) + Android Gradle Plugin 8.x |
| Renderização do jogo | `SurfaceView` + `Canvas`, com laço de jogo em thread própria |
| Telas de menu | Activities com layouts XML (`ConstraintLayout`) |
| Áudio | `SoundPool` (efeitos curtos, baixa latência) |
| Persistência | Jetpack DataStore (Preferences) |
| `minSdk` / `targetSdk` | 24 (Android 7.0) / 35 (Android 15) |
| Artefato de entrega | `app-release.apk` assinado, gerado via Gradle |

> **Nota para o grupo:** ao instalar o ambiente, anote aqui as versões exatas que ficaram na sua máquina
> (Android Studio, AGP, Gradle, JDK). Isso evita o clássico "na minha máquina funciona".

---

## 2. Ambiente de desenvolvimento

### 2.1 Ferramentas instaladas

| Ferramenta | Versão mínima | Para que serve |
| --- | --- | --- |
| Android Studio | Ladybug (2024.2) | IDE, editor de layout, emulador, profiler e assinatura do APK |
| Android SDK Platform | API 35 | Bibliotecas da plataforma para compilar |
| Android SDK Build-Tools | 35.x | `aapt2`, `d8`, `apksigner` — empacotamento e assinatura |
| Android Emulator + AVD | — | Testes sem aparelho físico |
| JDK | 17 | Compilação do Kotlin/Java (vem embutido no Android Studio) |
| Git | 2.40 | Controle de versão |
| Node.js | 18 | Somente para regerar os wireframes (`tools/gen-wireframes.mjs`) |

### 2.2 Requisitos de máquina

8 GB de RAM (16 GB recomendado se for usar o emulador), 10 GB livres em disco e virtualização habilitada
na BIOS (Intel VT-x / AMD-V) para o emulador rodar em velocidade aceitável.

### 2.3 Dispositivos de teste

| Perfil | Configuração | Objetivo |
| --- | --- | --- |
| AVD "Pixel 6 · API 34" | 1080 × 2400, densidade 420 dpi | Referência dos wireframes (360 × 800 dp) |
| AVD "Pixel 4a · API 24" | 1080 × 2340 | Verificar o `minSdk` e telas menores |
| Aparelho físico de um integrante | qualquer | Validar toque real, som e desempenho do laço de jogo |

O emulador é suficiente para layout e lógica, mas **o teste de toque e de som precisa acontecer em aparelho
físico** — a latência do áudio e o comportamento do arrasto no emulador não representam o real.

### 2.4 Como abrir o projeto

```bash
git clone https://github.com/<organizacao>/<repositorio>.git
cd <repositorio>
```

Em seguida, no Android Studio: *File ▸ Open* ▸ selecionar a pasta do projeto ▸ aguardar o **Gradle Sync**.
Nenhuma configuração manual é necessária: o `gradle/wrapper` fixa a versão do Gradle para todo o grupo.

---

## 3. Linguagem e arquitetura do aplicativo

### 3.1 Por que Kotlin nativo

**Kotlin** é a linguagem oficial do Android desde 2019 e é o que a ementa da disciplina cobra:
componentes de tela e acesso aos recursos de hardware do dispositivo, sem camada de abstração no meio.

Um jogo de Breakout é essencialmente um **laço de simulação** (atualizar posição → detectar colisão →
desenhar), e não uma tela de formulário. Por isso o jogo não usa a árvore de Views: ele desenha em um
`SurfaceView`, que dá acesso direto a um `Canvas` fora da thread de interface. Os menus, esses sim, usam
componentes de tela convencionais.

### 3.2 Divisão de responsabilidades

```
app/src/main/java/br/edu/<grupo>/brickbreaker/
├── ui/
│   ├── SplashActivity.kt            WF-01
│   ├── MenuActivity.kt              WF-02  (as 3 opções)
│   ├── IntegrantesActivity.kt       WF-03  (RecyclerView)
│   ├── ConfiguracoesActivity.kt     WF-04  (cores e tamanhos)
│   └── JogoActivity.kt              WF-05  (hospeda o SurfaceView e o HUD)
├── jogo/
│   ├── JogoView.kt                  SurfaceView + SurfaceHolder.Callback
│   ├── LacoDeJogo.kt                thread do laço, passo fixo de 16 ms
│   ├── Bola.kt · Paddle.kt          entidades e física
│   ├── Colisao.kt                   varredura de colisão (requisito d)
│   └── Renderizador.kt              desenho no Canvas
├── niveis/
│   ├── Parede.kt                    matriz de tijolos
│   ├── Tijolo.kt                    posição, resistência, cor
│   └── GeradorDeParede.kt           os 5 métodos (ver documento 03)
├── dados/
│   └── Preferencias.kt              DataStore: paleta, colunas, altura
└── audio/
    └── Sons.kt                      SoundPool: início de fase e rebatida
```

### 3.3 Justificativa comparada

| Critério | **Kotlin + Android Studio** | Flutter + Flame | Unity | React Native |
| --- | --- | --- | --- | --- |
| Geração do APK | Direta, integrada à IDE | Direta, mas com toolchain extra | Direta, exige módulos Android | Mais etapas, depende do Metro/Node |
| Acesso a hardware (som, toque, sensores) | Nativo, sem ponte | Via plugins | Abstraído pela engine | Via ponte JS ↔ nativo |
| Aderência à ementa da disciplina | Total | Parcial | Baixa (engine faz o trabalho) | Parcial |
| Tamanho do APK | ~3 MB | ~15 MB | ~25 MB | ~20 MB |
| Curva de aprendizado para o grupo | Média | Média/alta (Dart) | Alta | Média |
| Dependências externas | Praticamente nenhuma | Várias | Engine inteira | Muitas |

A escolha por **Kotlin nativo** se sustenta em dois pontos: (1) é a alternativa com menos camadas entre o
código do grupo e o hardware, que é justamente o que a disciplina quer exercitar; e (2) o caminho até o
APK assinado é o mais curto e o menos sujeito a quebrar às vésperas da entrega.

Uma engine como Unity resolveria a física de graça — e é exatamente por isso que foi descartada: o
controle de colisão pedido no requisito (d) deixaria de ser um trabalho do grupo.

---

## 4. Bibliotecas e dependências

O projeto é deliberadamente enxuto. Nenhuma engine de jogo, nenhuma biblioteca gráfica de terceiros.

| Dependência | Uso |
| --- | --- |
| `androidx.core:core-ktx` | Extensões Kotlin da plataforma |
| `androidx.appcompat:appcompat` | Compatibilidade das Activities |
| `androidx.constraintlayout:constraintlayout` | Layout dos menus |
| `androidx.recyclerview:recyclerview` | Lista de integrantes (WF-03) |
| `androidx.datastore:datastore-preferences` | Persistir cores e tamanho dos tijolos |
| `junit` / `androidx.test.ext:junit` | Testes do gerador de paredes e da colisão |

`SoundPool`, `Canvas`, `SurfaceView`, `MotionEvent` e `WindowInsetsController` fazem parte do próprio
Android SDK — não são dependências adicionais.

---

## 5. Recursos de hardware utilizados

A disciplina exige acesso aos recursos de hardware do dispositivo. Estes são os pontos de contato:

| Recurso | API utilizada | Onde aparece |
| --- | --- | --- |
| **Tela — modo imersivo** | `WindowInsetsControllerCompat.hide(systemBars())` | Todas as telas — atende o requisito (a) |
| **Tela — densidade e resolução** | `DisplayMetrics`, `resources.displayMetrics.density` | Conversão dp → px do motor e da parede |
| **Toque (multitoque)** | `View.onTouchEvent(MotionEvent)`, `ACTION_MOVE` | Arrasto do paddle (WF-05) |
| **Alto-falante** | `SoundPool` + `AudioAttributes(USAGE_GAME)` | Som de início de fase e som de rebatida — requisito (e) |
| **Vibração (opcional)** | `VibratorManager`, `VibrationEffect` | Retorno tátil ao perder a bola |
| **Ciclo de vida do aparelho** | `onPause()` / `onResume()` da Activity | Pausa automática ao receber uma ligação (WF-13) |

Os dois sons exigidos ficam em `res/raw/` no formato **OGG Vorbis**, mono, 44,1 kHz, com menos de 1 segundo
cada — `SoundPool` mantém os efeitos descomprimidos em memória, o que garante disparo imediato sem o atraso
que um `MediaPlayer` introduziria.

---

## 6. Permissões declaradas

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

Apenas isso. O jogo **não** acessa rede, câmera, localização, contatos ou armazenamento externo. Áudio de
saída e toque não exigem permissão. Um `AndroidManifest.xml` curto é também um argumento de privacidade
na apresentação.

---

## 7. Organização do trabalho em grupo

### 7.1 Repositório

Repositório **público** no GitHub, com todos os integrantes adicionados como *collaborators*
(*Settings ▸ Collaborators ▸ Add people*), o que dá permissão de escrita a todo o grupo.

### 7.2 Ramificações

| Ramo | Papel |
| --- | --- |
| `main` | Sempre entregável. Recebe alterações apenas por Pull Request. |
| `feat/<assunto>` | Uma funcionalidade em desenvolvimento. Ex.: `feat/gerador-paredes`. |
| `fix/<assunto>` | Correção pontual. |
| `docs/<assunto>` | Alterações apenas em documentação. |

### 7.3 Mensagens de commit

Formato `<tipo>: <o que mudou>` — `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
Exemplo: `feat: gerador procedural da parede do nível 4`.

### 7.4 Revisão

Todo Pull Request precisa da aprovação de pelo menos um outro integrante antes de entrar em `main`.
Além de melhorar o código, isso garante que mais de uma pessoa entenda cada parte do projeto — o que
importa na apresentação.

---

## 8. Geração do arquivo APK

Esta é a parte que **precisa funcionar no dia da entrega**. Faça um ensaio completo com antecedência.

### 8.1 APK de depuração (para testes internos)

```bash
./gradlew assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`.

Serve para o grupo testar entre si. **Não é o arquivo de entrega**: é assinado com uma chave de
depuração gerada automaticamente e vem com a depuração habilitada.

### 8.2 Chave de assinatura (uma única vez, para o grupo todo)

Todo APK instalável precisa estar assinado. Gere o *keystore* uma vez e compartilhe entre os integrantes
por um canal privado:

```bash
keytool -genkey -v -keystore brickbreaker.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias brickbreaker
```

> ⚠️ O arquivo `.jks` e as senhas **não vão para o repositório** — já estão barrados pelo `.gitignore`.
> As senhas ficam em `keystore.properties`, também fora do controle de versão.

`keystore.properties` (na raiz do projeto, não versionado):

```properties
storeFile=../brickbreaker.jks
storePassword=<senha>
keyAlias=brickbreaker
keyPassword=<senha>
```

E em `app/build.gradle.kts`:

```kotlin
val props = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isDebuggable = false
        }
    }
}
```

### 8.3 APK de entrega (assinado)

**Pela linha de comando:**

```bash
./gradlew clean assembleRelease
```

**Ou pela interface do Android Studio:**
*Build ▸ Generate Signed App Bundle / APK… ▸ **APK** ▸ selecionar o keystore ▸ variante `release` ▸ Finish*

> Atenção: escolha **APK**, e não *Android App Bundle* (`.aab`). O `.aab` é o formato da Play Store e
> **não instala diretamente em um aparelho** — entregar um `.aab` por engano é o erro mais comum aqui.

Saída: **`app/build/outputs/apk/release/app-release.apk`**

### 8.4 Conferência antes de entregar

```bash
# a assinatura está válida?
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app-release.apk

# versão, permissões e SDK mínimo declarados
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging app-release.apk | head -20

# instalação em aparelho conectado
adb install -r app-release.apk
```

### 8.5 Lista de conferência da entrega

- [ ] `versionName` e `versionCode` atualizados em `app/build.gradle.kts`
- [ ] Ícone do aplicativo e `android:label` definidos (nada de "My Application")
- [ ] `./gradlew clean assembleRelease` conclui sem erros a partir de um clone limpo
- [ ] `apksigner verify` confirma a assinatura
- [ ] APK instalado e jogado do início ao fim em pelo menos um **aparelho físico**
- [ ] Os 5 níveis são alcançáveis e o avanço entre eles é automático
- [ ] Os dois sons tocam (início de fase e rebatida no paddle)
- [ ] O APK está anexado à entrega **e** publicado em *Releases* no GitHub
- [ ] Nomes reais dos integrantes no README e na tela de Integrantes

### 8.6 Publicação do APK no repositório

O APK **não é versionado** dentro do repositório (arquivos binários grandes poluem o histórico do Git).
Ele é publicado como *release*:

*GitHub ▸ Releases ▸ Draft a new release ▸ tag `v1.0.0` ▸ anexar `app-release.apk` ▸ Publish*

Assim o professor baixa o arquivo por um link estável e o repositório continua leve.
