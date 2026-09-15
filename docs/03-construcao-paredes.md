# 03 — Construção da parede de blocos

[◀ voltar ao índice](../README.md)

Este documento descreve **como** a parede de tijolos é modelada, **quais métodos** são usados para
construir cada um dos 5 níveis e **como a colisão é controlada** para que a bola não atravesse a parede.

---

## 1. Modelo de dados

A parede é representada por uma **matriz de caracteres** — uma malha lógica independente de pixels.
Cada caractere descreve o conteúdo de uma célula:

| Caractere | Significado | Acertos para destruir |
| --- | --- | --- |
| `.` | célula vazia | — |
| `1` | tijolo comum | 1 |
| `2` | tijolo reforçado | 2 |
| `3` | tijolo blindado | 3 |
| `X` | tijolo indestrutível | nunca é destruído |

Trabalhar com texto em vez de objetos tem três vantagens práticas: o mapa de um nível cabe em cinco linhas
legíveis no código, pode ser comparado em um diff do Git, e o mesmo mapa alimenta o wireframe e o jogo.

```kotlin
data class Tijolo(
    val linha: Int,
    val coluna: Int,
    var resistencia: Int,        // 1..3; -1 = indestrutível
    val retangulo: RectF         // posição em pixels, calculada na montagem
) {
    val destrutivel get() = resistencia > 0
    val vivo get() = resistencia != 0
}

class Parede(val matriz: List<String>) {
    lateinit var tijolos: List<Tijolo>
    val restantes get() = tijolos.count { it.destrutivel && it.vivo }
    val concluida get() = restantes == 0        // indestrutíveis não contam
}
```

---

## 2. Da matriz para a tela

A matriz não guarda posição em pixels. A conversão acontece uma única vez, quando o nível é montado, e é
recalculada se a tela mudar de tamanho. É isso que faz a parede caber em qualquer aparelho.

```
larguraÚtil    = larguraTela − 2 × margemLateral
larguraTijolo  = (larguraÚtil − espaçamento × (colunas − 1)) / colunas
x(coluna)      = margemLateral + coluna × (larguraTijolo + espaçamento)
y(linha)       = topoDaParede  + linha  × (alturaTijolo  + espaçamento)
```

Com os valores de referência dos wireframes — tela de 360 dp, margem de 16 dp, espaçamento de 3 dp:

| Tamanho escolhido | Colunas | Largura do tijolo |
| --- | --- | --- |
| Pequeno | 10 | 30,1 dp |
| **Médio** (padrão) | 8 | 38,4 dp |
| Grande | 6 | 52,2 dp |

**A largura do tijolo é sempre calculada, nunca fixada.** Fixar a largura seria o caminho mais curto para
a parede desalinhar em telas de proporção diferente.

O número de linhas segue o mesmo raciocínio: a parede pode ocupar até 40% da altura jogável, e

```
linhasMáximas = ⌊(alturaDaÁreaDaParede + espaçamento) / (alturaTijolo + espaçamento)⌋
```

Se a matriz de um nível tiver mais linhas do que cabe, as linhas excedentes (as de baixo, mais fáceis) são
descartadas.

---

## 3. Escolha dos métodos

Os 5 níveis não usam o mesmo método de propósito. Cada um demonstra uma abordagem diferente e tem um
custo/benefício próprio:

| Método | Controle do desenho | Esforço de manutenção | Variedade |
| --- | --- | --- | --- |
| Mapa fixo literal | total | alto (um mapa por nível) | nenhuma |
| Meia matriz + espelhamento | total, com simetria garantida | metade do mapa | nenhuma |
| Regra matemática | indireto | mínimo (uma função) | nenhuma |
| Procedural com semente | nenhum | mínimo (parâmetros) | alta |
| Híbrido | parcial | médio | média |

Os três primeiros são **determinísticos por construção**; o quarto é aleatório mas **reprodutível**; o
quinto combina os dois.

---

## 4. Os cinco métodos de construção

### 4.1 Nível 1 — "Muralha": mapa fixo literal

O mapa é uma constante no código. Nenhuma aleatoriedade: o nível é idêntico em toda partida, o que
facilita depurar o motor de colisão.

```
11111111
11111111
11111111
11111111
11111111
```

```kotlin
val NIVEL_1 = listOf(
    "11111111",
    "11111111",
    "11111111",
    "11111111",
    "11111111",
)
```

**40 tijolos · 40 acertos.** Parede cheia, todos com resistência 1 — o jogador aprende o controle do
paddle sem punição.

---

### 4.2 Nível 2 — "Pirâmide": meia matriz + espelhamento

Apenas a metade esquerda é declarada; a direita é o espelho horizontal. A simetria sai de graça e o mapa
a manter tem metade do tamanho.

```kotlin
fun espelhar(metade: List<String>): List<String> =
    metade.map { it + it.reversed() }

val NIVEL_2 = espelhar(listOf(
    "...1",
    "..11",
    ".222",
    "2222",
    "3333",
))
```

Resultado:

```
...11...
..1111..
.222222.
22222222
33333333
```

**28 tijolos · 58 acertos.** A resistência cresce com a profundidade: as fileiras de baixo, mais fáceis de
alcançar, são as mais duras — o que empurra o jogador a abrir caminho pelas laterais e atacar o topo por
dentro.

---

### 4.3 Nível 3 — "Xadrez": regra matemática sobre (linha, coluna)

Nenhum mapa é armazenado. Cada célula é decidida por uma **função pura** de linha e coluna:

```kotlin
fun porRegra(linhas: Int, colunas: Int, regra: (Int, Int) -> Char): List<String> =
    (0 until linhas).map { i ->
        (0 until colunas).map { j -> regra(i, j) }.joinToString("")
    }

val NIVEL_3 = porRegra(6, 8) { i, j ->
    if ((i + j) % 2 == 0) when {
        i < 2 -> '1'
        i < 4 -> '2'
        else  -> '3'
    } else '.'
}
```

Resultado:

```
1.1.1.1.
.1.1.1.1
2.2.2.2.
.2.2.2.2
3.3.3.3.
.3.3.3.3
```

**24 tijolos · 48 acertos.** Trocar a regra troca a parede inteira em uma linha de código. Os vãos
alternados deixam a bola entrar na parede e ricochetear lá dentro — é justamente o nível que mais exige o
controle de "uma colisão por passo" descrito na seção 7.

---

### 4.4 Nível 4 — "Campo minado": procedural com semente fixa

A parede é sorteada, mas com **semente fixa**: parece aleatória e mesmo assim é idêntica em qualquer
aparelho e em qualquer execução. Isso importa para poder reproduzir um bug e para o nível ser justo entre
os jogadores.

O gerador `Random` do Kotlin aceita uma semente, mas para garantir o mesmo resultado independentemente da
versão da JVM usamos um PRNG próprio e trivial (*mulberry32*):

```kotlin
class Aleatorio(semente: Int) {
    private var estado = semente
    fun proximo(): Double {
        estado += 0x6D2B79F5
        var t = estado
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)).toUInt().toDouble()) / 4294967296.0
    }
}

fun procedural(
    linhas: Int, colunas: Int, semente: Int,
    densidade: Double, pesos: List<Pair<Char, Double>>
): List<String> {
    val rnd = Aleatorio(semente)
    var matriz: List<String>
    var tentativas = 0
    do {
        matriz = (0 until linhas).map {
            (0 until colunas).map {
                if (rnd.proximo() < densidade) sorteiaResistencia(rnd, pesos) else '.'
            }.joinToString("")
        }
    } while (!valida(matriz) && ++tentativas < 50)
    return matriz
}

val NIVEL_4 = procedural(
    linhas = 6, colunas = 8, semente = 20260821,
    densidade = 0.70,
    pesos = listOf('1' to 0.50, '2' to 0.32, '3' to 0.18)
)
```

Resultado com a semente `20260821`:

```
..221.1.
.11.1212
121..131
112.2.12
1.1223.2
.1.1.23.
```

**32 tijolos · 50 acertos** (17 de resistência 1, 12 de resistência 2, 3 de resistência 3).

#### Validação de solubilidade

Uma parede sorteada pode sair ruim. Depois de gerada, ela passa por uma verificação; se reprovar, é
descartada e sorteada de novo (até 50 tentativas, com queda para o mapa fixo do nível 1 em caso extremo):

```kotlin
fun valida(matriz: List<String>): Boolean {
    val destrutiveis = matriz.sumOf { linha -> linha.count { it in "123" } }
    val temLinhaVazia = matriz.any { linha -> linha.none { it in "123X" } }
    val minimo = (matriz.size * matriz[0].length * 0.45).toInt()
    return destrutiveis >= minimo && !temLinhaVazia
}
```

Duas regras, com motivos distintos: **massa mínima** (uma parede rala demais acaba em segundos e o nível
perde a graça) e **nenhuma fileira vazia** (uma fileira totalmente aberta cria um corredor livre que
descaracteriza o nível).

---

### 4.5 Nível 5 — "Fortaleza": híbrido

Combina as duas famílias: o contorno é declarado à mão e o interior é sorteado.

```kotlin
fun hibrido(linhas: Int, colunas: Int, semente: Int): List<String> {
    val rnd = Aleatorio(semente)
    return (0 until linhas).map { i ->
        (0 until colunas).map { j ->
            when {
                i == 0            -> '3'                       // teto blindado
                i == linhas - 1   -> '2'                       // base reforçada
                j == 0 || j == colunas - 1 -> 'X'               // colunas indestrutíveis
                rnd.proximo() < 0.72 ->
                    if (rnd.proximo() < 0.35) '2' else '1'      // miolo sorteado
                else -> '.'
            }
        }.joinToString("")
    }
}

val NIVEL_5 = hibrido(linhas = 6, colunas = 8, semente = 5150)
```

Resultado com a semente `5150`:

```
33333333
X111.2.X
X1.1.12X
X1.1..1X
X11112.X
22222222
```

**32 tijolos destrutíveis + 8 indestrutíveis · 59 acertos.**

Os tijolos `X` **não entram na contagem de conclusão**: o nível termina quando os destrutíveis acabam. Sem
essa regra, o nível seria impossível de vencer. As colunas indestrutíveis estreitam o corredor por onde a
bola circula, e o teto blindado obriga o jogador a insistir na mesma região.

---

## 5. Adaptação ao tamanho escolhido pelo usuário

Os mapas acima são declarados em uma **malha canônica de 8 colunas**. Se o usuário escolher tijolo Pequeno
(10 colunas) ou Grande (6), o mapa é reamostrado horizontalmente por **vizinho mais próximo**:

```kotlin
fun reamostrar(matriz: List<String>, colunasDestino: Int): List<String> {
    val colunasOrigem = matriz[0].length
    if (colunasOrigem == colunasDestino) return matriz
    return matriz.map { linha ->
        (0 until colunasDestino).map { j ->
            val origem = ((j * (colunasOrigem - 1).toDouble()) / (colunasDestino - 1))
                .roundToInt().coerceAtMost(colunasOrigem - 1)
            linha[origem]
        }.joinToString("")
    }
}
```

A reamostragem preserva a silhueta e a simetria do desenho. O nível 2 em três tamanhos:

| 6 colunas (Grande) | 8 colunas (Médio) | 10 colunas (Pequeno) |
| --- | --- | --- |
| <pre>..11..<br>..11..<br>.2222.<br>222222<br>333333</pre> | <pre>...11...<br>..1111..<br>.222222.<br>22222222<br>33333333</pre> | <pre>....11....<br>..111111..<br>.22222222.<br>2222222222<br>3333333333</pre> |

Os níveis 3, 4 e 5 são gerados por função, então dispensam reamostragem: recebem o número de colunas
diretamente como parâmetro e nascem no tamanho certo.

---

## 6. Integração com as configurações do usuário

O que a tela de Configurações ([WF-04](wireframes/04-configuracoes.svg)) grava e o que a parede faz com isso:

| Preferência | Chave no DataStore | Efeito na parede |
| --- | --- | --- |
| Padrão de cores | `paleta` | Define a cor de cada resistência (1, 2, 3, X) no momento do desenho. Não altera a geometria. |
| Tamanho do tijolo | `colunas` (10 / 8 / 6) | Entra no cálculo da largura e dispara a reamostragem dos mapas fixos. |
| Altura do tijolo | `alturaTijolo` (12–26 dp) | Define quantas linhas cabem na área da parede. |

```kotlin
fun montarNivel(numero: Int, prefs: Preferencias, area: RectF): Parede {
    val matriz = reamostrar(matrizDoNivel(numero, prefs.colunas), prefs.colunas)
        .take(linhasQueCabem(area, prefs.alturaTijolo))
    return Parede(matriz).apply { posicionar(area, prefs) }
}
```

A separação é intencional: **cor é decisão de desenho, tamanho é decisão de geometria.** Trocar a paleta
não reconstrói a parede — por isso, na pausa, mudar a cor vale na hora e mudar o tamanho só vale no
próximo nível.

---

## 7. Controle de colisão (requisito d)

> *"Controle de colisão da bola com os blocos. Impedir que a bola sendo movimentada dentro do jogo
> atravesse todos os blocos de uma só vez."*

São **dois problemas diferentes** escondidos na mesma frase, e cada um tem sua solução.

### 7.1 Problema 1 — atravessar sem detectar (*tunneling*)

Se a bola anda 40 px em um quadro e o tijolo tem 18 px de altura, um teste de sobreposição feito só na
posição final simplesmente não vê o tijolo: a bola estava antes dele e passou a estar depois.

**Solução: limitar o deslocamento por passo e subdividir o quadro.**

```kotlin
val passoMaximo = min(larguraTijolo, alturaTijolo) / 2f
val subpassos = ceil(velocidade.comprimento() * dt / passoMaximo).toInt().coerceAtLeast(1)
val dtSub = dt / subpassos
repeat(subpassos) { avancarUmSubpasso(dtSub) }
```

Nenhum subpasso move a bola mais do que meio tijolo. Impossível atravessar sem tocar.

### 7.2 Problema 2 — destruir vários blocos de uma vez

Mesmo detectando as colisões, se o código percorrer todos os tijolos e destruir **todos** os que estiverem
sobrepostos à bola, a bola varre uma fileira inteira em um quadro só. Era exatamente isso que o enunciado
proíbe.

**Solução: em cada subpasso, resolver apenas a primeira colisão no tempo.**

```kotlin
fun avancarUmSubpasso(dt: Float) {
    var restante = dt
    var iteracoes = 0

    while (restante > 0f && iteracoes++ < MAX_COLISOES_POR_SUBPASSO) {
        val impacto = primeiroImpacto(bola, velocidade, restante)   // menor tempo
            ?: break

        bola.avancar(velocidade, impacto.tempo)                     // até tocar
        when (impacto.eixo) {                                       // reflete
            Eixo.X -> velocidade.x = -velocidade.x
            Eixo.Y -> velocidade.y = -velocidade.y
        }
        impacto.tijolo?.let { atingir(it) }                         // 1 tijolo, só
        restante -= impacto.tempo
    }
    bola.avancar(velocidade, restante)
}
```

O laço resolve **um único tijolo por iteração**, com `MAX_COLISOES_POR_SUBPASSO = 2`. A bola pode
ricochetear em um canto e atingir um segundo tijolo no mesmo quadro — o que é fisicamente correto — mas
nunca varre a parede.

### 7.3 Detecção: *swept AABB*

A bola é tratada como um quadrado (aproximação aceitável para um raio de 7 dp) e se calcula o **tempo de
impacto** contra cada tijolo candidato pelo método das faixas (*slab method*):

```kotlin
fun tempoDeImpacto(bola: RectF, v: PointF, alvo: RectF, dt: Float): Impacto? {
    // distâncias até entrar e até sair, em cada eixo
    val entradaX = if (v.x > 0) alvo.left - bola.right else alvo.right - bola.left
    val saidaX   = if (v.x > 0) alvo.right - bola.left else alvo.left - bola.right
    val entradaY = if (v.y > 0) alvo.top - bola.bottom else alvo.bottom - bola.top
    val saidaY   = if (v.y > 0) alvo.bottom - bola.top else alvo.top - bola.bottom

    val tEntradaX = if (v.x == 0f) -Float.MAX_VALUE else entradaX / v.x
    val tSaidaX   = if (v.x == 0f)  Float.MAX_VALUE else saidaX / v.x
    val tEntradaY = if (v.y == 0f) -Float.MAX_VALUE else entradaY / v.y
    val tSaidaY   = if (v.y == 0f)  Float.MAX_VALUE else saidaY / v.y

    val tEntrada = max(tEntradaX, tEntradaY)
    val tSaida   = min(tSaidaX,   tSaidaY)

    if (tEntrada > tSaida || tEntrada < 0f || tEntrada > dt) return null

    // o eixo que entrou por último é a face atingida
    val eixo = if (tEntradaX > tEntradaY) Eixo.X else Eixo.Y
    return Impacto(tempo = tEntrada, eixo = eixo, tijolo = alvo.dono)
}
```

Saber **qual face** foi atingida é o que permite refletir no eixo certo: bater na lateral inverte a
velocidade horizontal, bater embaixo inverte a vertical. Um teste ingênuo de sobreposição não distingue
os dois casos e produz aquele ricochete "grudento" clássico de implementações apressadas.

> **Na implementação** ([`Colisao.kt`](../app/src/main/java/br/edu/ucs/brickbreaker/jogo/Colisao.kt)) há um
> cuidado a mais: com velocidade zero em um eixo, só existe impacto se a bola já estiver sobreposta ao tijolo
> nesse eixo. Sem essa guarda, uma bola subindo na vertical "acertaria" tijolos de outras colunas. O
> retângulo é a classe `Caixa`, e não `RectF`, para que o motor rode em teste unitário sem Android.

### 7.4 Restringir os candidatos pela malha

Testar a bola contra os 40 tijolos a cada subpasso é desperdício. Como a parede **é** uma malha regular,
dá para calcular diretamente quais células o trajeto da bola pode cruzar:

```kotlin
val colInicial = ((min(bola.left, destino.left)   - margem) / (larguraTijolo + espaço)).toInt()
val colFinal   = ((max(bola.right, destino.right) - margem) / (larguraTijolo + espaço)).toInt()
val linInicial = ((min(bola.top, destino.top)     - topo)   / (alturaTijolo  + espaço)).toInt()
val linFinal   = ((max(bola.bottom, destino.bottom) - topo) / (alturaTijolo  + espaço)).toInt()
```

Sobram tipicamente **2 a 6 candidatos** por subpasso em vez de 40. Essa é a vantagem prática de manter a
parede como matriz e não como uma lista solta de tijolos.

### 7.5 Colisão com o paddle e com as bordas

- **Paredes laterais e teto:** reflexão simples no eixo correspondente.
- **Paddle:** além de refletir, o ângulo de saída depende do ponto de contato — o centro devolve na
  vertical e as extremidades abrem o ângulo até 60°. É o que dá controle ao jogador. Toda colisão com o
  paddle dispara o **som de rebatida** (requisito e).
- **Fundo da tela:** a bola cruzou a linha de perda. O laço pausa e o diálogo
  [WF-11](wireframes/11-dialogo-bola-perdida.svg) é exibido (requisito g).

---

## 8. Conclusão do nível e avanço automático

```kotlin
fun aoDestruirTijolo() {
    pontuacao += pontosPor(tijolo.resistenciaInicial)
    if (parede.concluida) {                 // só conta tijolos destrutíveis
        lacoDeJogo.pausar()
        if (nivelAtual < 5) {
            nivelAtual++
            velocidadeBase *= 1.08f         // +8% por nível
            mostrarTransicao(nivelAtual)    // WF-12: som de fase + contagem de 3 s
            iniciarNivel(nivelAtual)        // automático, sem passar pelo menu
        } else {
            mostrarVitoria()                // WF-14
        }
    }
}
```

O aumento de 8% na velocidade por nível é limitado por um teto: a velocidade nunca pode ultrapassar o
valor em que a subdivisão da seção 7.1 exigiria mais subpassos do que cabe no orçamento de 16 ms por
quadro. Dificuldade nunca vale mais do que a corretude da colisão.

---

## 9. Resumo dos cinco níveis

| Nível | Nome | Método | Malha | Destrutíveis | Indestrutíveis | Acertos | Dificuldade |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Muralha | Mapa fixo literal | 5 × 8 | 40 | 0 | 40 | ●○○○○ |
| 2 | Pirâmide | Meia matriz + espelhamento | 5 × 8 | 28 | 0 | 58 | ●●○○○ |
| 3 | Xadrez | Regra sobre (linha, coluna) | 6 × 8 | 24 | 0 | 48 | ●●●○○ |
| 4 | Campo minado | Procedural com semente fixa | 6 × 8 | 32 | 0 | 50 | ●●●●○ |
| 5 | Fortaleza | Híbrido | 6 × 8 | 32 | 8 | 59 | ●●●●● |

Os números vêm de `wireframes/paredes.json`, gerado pelo mesmo código que desenha as pranchas.

---

## 10. Testes previstos

| Teste | O que verifica |
| --- | --- |
| `paredeDoNivelTemAsDimensoesEsperadas` | Cada nível gera a matriz com as linhas e colunas corretas |
| `geradorProceduralEReprodutivel` | Mesma semente produz matriz idêntica em 100 execuções |
| `paredeGeradaPassaNaValidacao` | Nenhum nível nasce ralo demais ou com fileira vazia |
| `reamostragemPreservaSimetria` | O nível 2 continua simétrico em 6, 8 e 10 colunas |
| `nivelConcluiIgnorandoIndestrutiveis` | O nível 5 termina com os 8 tijolos `X` ainda de pé |
| `bolaRapidaNaoAtravessaAParede` | Bola a 10× a velocidade normal continua colidindo — requisito (d) |
| `umSubpassoDestroiNoMaximoDoisTijolos` | O limite de colisões por subpasso é respeitado |
| `faceAtingidaDefineOEixoDaReflexao` | Impacto lateral inverte X, impacto inferior inverte Y |

Os testes de geração de parede são testes unitários puros (`src/test`), sem dependência do Android — rodam
em segundos e são o melhor lugar para travar o comportamento antes de mexer no motor.

Todos estão implementados e passam (`./gradlew testDebugUnitTest`):
[`GeradorDeParedeTest.kt`](../app/src/test/java/br/edu/ucs/brickbreaker/niveis/GeradorDeParedeTest.kt),
[`ColisaoTest.kt`](../app/src/test/java/br/edu/ucs/brickbreaker/jogo/ColisaoTest.kt) e
[`MotorTest.kt`](../app/src/test/java/br/edu/ucs/brickbreaker/jogo/MotorTest.kt). Além da lista acima,
`matrizesIguaisAsDaDocumentacao` confere, caractere por caractere, que as 5 paredes do app são as mesmas
desta página e dos wireframes.
