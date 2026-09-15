package br.edu.ucs.brickbreaker.jogo

import br.edu.ucs.brickbreaker.niveis.GeometriaParede
import br.edu.ucs.brickbreaker.niveis.GeradorDeParede
import br.edu.ucs.brickbreaker.niveis.Parede
import br.edu.ucs.brickbreaker.niveis.Tijolo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Estado e fisica da partida. Nao depende de Android: recebe medidas em pixels e a densidade da tela,
 * e avisa a Activity por [Ouvinte]. Chamado pela thread do laco de jogo e pela thread de interface,
 * sempre com `synchronized(motor)`.
 */
class Motor(private val densidade: Float) {

    enum class Estado { AGUARDANDO, JOGANDO, PAUSADO, BOLA_PERDIDA, NIVEL_CONCLUIDO }

    interface Ouvinte {
        fun aoRebaterNoPaddle()
        fun aoAtingirTijolo(destruido: Boolean)
        fun aoPerderBola()
        fun aoConcluirNivel()
    }

    /** Parametros vindos das Configuracoes (WF-04) que afetam a geometria. */
    data class Ajustes(val colunas: Int = 8, val alturaTijoloDp: Int = 18)

    var ouvinte: Ouvinte? = null

    var estado = Estado.AGUARDANDO
        private set
    var nivel = 1
        private set
    var pontos = 0
        private set
    var vidas = VIDAS_INICIAIS
        private set
    var tijolosDestruidos = 0
        private set

    /** Modo de demonstracao (so em build de depuracao): o paddle segue a bola. */
    var pilotoAutomatico = false

    val bola = Bola(dp(7f))
    val paddle = Paddle(dp(92f), dp(14f))
    lateinit var parede: Parede
        private set

    // medidas da tela, em px
    var largura = 0f
        private set
    var altura = 0f
        private set
    var tetoDoJogo = 0f
        private set
    var zonaControleTopo = 0f
        private set
    var linhaDePerda = 0f
        private set

    val telaConfigurada get() = largura > 0f
    val paredeMontada get() = ::parede.isInitialized

    fun dp(valor: Float) = valor * densidade

    /** Recalcula as areas da tela (WF-05): HUD de 44 dp, parede 24 dp abaixo dele, zona de controle de 120 dp. */
    fun configurarTela(largura: Float, altura: Float, topoSeguro: Float) {
        this.largura = largura
        this.altura = altura
        tetoDoJogo = topoSeguro + dp(ALTURA_HUD_DP)
        zonaControleTopo = altura - dp(ZONA_CONTROLE_DP)
        paddle.topo = zonaControleTopo - dp(22f)
        linhaDePerda = paddle.topo + paddle.altura + dp(24f)
        if (paddle.x == 0f) {
            paddle.x = largura / 2f
            paddle.alvoX = paddle.x
        }
    }

    /** Monta a parede do nivel e recoloca a bola sobre o paddle. A mesma entrada gera sempre a mesma parede. */
    fun iniciarNivel(numero: Int, ajustes: Ajustes) {
        nivel = numero
        val topoParede = tetoDoJogo + dp(24f)
        val alturaAreaJogavel = zonaControleTopo - tetoDoJogo
        val alturaTijolo = dp(ajustes.alturaTijoloDp.toFloat())
        val espaco = dp(ESPACO_DP)
        val geometria = GeometriaParede.calcular(
            larguraTela = largura,
            margemLateral = dp(MARGEM_LATERAL_DP),
            topo = topoParede,
            colunas = ajustes.colunas,
            alturaTijolo = alturaTijolo,
            espaco = espaco,
        )
        val cabem = GeradorDeParede.linhasQueCabem(alturaAreaJogavel * FRACAO_AREA_PAREDE, alturaTijolo, espaco)
        val matriz = GeradorDeParede.matrizDoNivel(numero, ajustes.colunas).take(cabem)
        parede = Parede(matriz, geometria)
        colocarBolaNoPaddle()
        estado = Estado.AGUARDANDO
    }

    fun reiniciarPartida() {
        pontos = 0
        vidas = VIDAS_INICIAIS
        tijolosDestruidos = 0
    }

    fun lancar() {
        if (estado != Estado.AGUARDANDO) return
        val v = velocidadeDoNivel()
        val angulo = Math.toRadians(ANGULO_LANCAMENTO_GRAUS).toFloat()
        bola.vx = v * sin(angulo)
        bola.vy = -v * cos(angulo)
        estado = Estado.JOGANDO
    }

    fun pausar() {
        if (estado == Estado.JOGANDO) estado = Estado.PAUSADO
    }

    fun retomar() {
        if (estado == Estado.PAUSADO) estado = Estado.JOGANDO
    }

    fun moverPaddlePara(x: Float) {
        paddle.alvoX = x
    }

    /**
     * Velocidade base +8% por nivel, com teto: nunca acima do valor em que a subdivisao exigiria
     * mais subpassos do que cabe em um quadro (doc 03, secao 8).
     */
    fun velocidadeDoNivel(numero: Int = nivel): Float {
        val desejada = dp(VELOCIDADE_BASE_DP) * 1.08f.pow(numero - 1)
        return min(desejada, dp(VELOCIDADE_MAXIMA_DP))
    }

    // --- laco ----------------------------------------------------------------------------------

    fun atualizar(dt: Float) {
        if (!telaConfigurada || !paredeMontada) return
        if (estado == Estado.PAUSADO || estado == Estado.BOLA_PERDIDA || estado == Estado.NIVEL_CONCLUIDO) return

        if (pilotoAutomatico && estado == Estado.JOGANDO) paddle.alvoX = alvoDoPiloto()
        paddle.seguirAlvo(dt, 0f, largura)

        when (estado) {
            Estado.AGUARDANDO -> colocarBolaNoPaddle()
            Estado.JOGANDO -> simular(dt)
            else -> Unit
        }
    }

    /**
     * Modo demonstracao: posiciona o paddle para que a rebatida saia na direcao do tijolo restante
     * mais baixo. Usa a mesma regra de angulo de [rebaterNoPaddle], so que ao contrario.
     */
    private fun alvoDoPiloto(): Float {
        val alvo = parede.tijolos
            .filter { it.destrutivel && it.vivo }
            .maxByOrNull { it.linha * largura - abs(it.caixa.centroX - bola.x) }
            ?: return bola.x
        val dx = alvo.caixa.centroX - bola.x
        val dy = paddle.topo - alvo.caixa.base
        val angulo = Math.toDegrees(atan2(dx, dy).toDouble()).toFloat().coerceIn(-55f, 55f)
        return bola.x - (angulo / ANGULO_MAXIMO_GRAUS) * paddle.largura / 2f
    }

    /** Limita o deslocamento por passo a meio tijolo, subdividindo o quadro (doc 03, secao 7.1). */
    private fun simular(dt: Float) {
        val g = parede.geometria
        val passoMaximo = min(g.larguraTijolo, g.alturaTijolo) / 2f
        val subpassos = ceil(bola.velocidade * dt / passoMaximo).toInt().coerceAtLeast(1)
        val dtSub = dt / subpassos
        repeat(subpassos) {
            if (estado != Estado.JOGANDO) return
            avancarUmSubpasso(dtSub)
        }
    }

    /** Em cada subpasso resolve apenas a primeira colisao no tempo, no maximo duas (doc 03, secao 7.2). */
    internal fun avancarUmSubpasso(dt: Float): Int {
        var restante = dt
        var colisoes = 0

        while (restante > 0f && colisoes < MAX_COLISOES_POR_SUBPASSO) {
            val impacto = primeiroImpacto(restante) ?: break
            bola.x += bola.vx * impacto.tempo
            bola.y += bola.vy * impacto.tempo
            restante -= impacto.tempo
            colisoes++

            when {
                impacto.tijolo != null -> {
                    refletir(impacto.eixo)
                    atingir(impacto.tijolo)
                }
                impacto.noPaddle -> rebaterNoPaddle(impacto.eixo)
                else -> refletir(impacto.eixo)
            }
            if (estado != Estado.JOGANDO) return colisoes
        }
        // so avanca o tempo que sobrou se isso nao fizer a bola entrar em algo
        if (restante > 0f && primeiroImpacto(restante) == null) bola.avancar(restante)

        if (bola.caixa.topo > linhaDePerda) {
            estado = Estado.BOLA_PERDIDA
            vidas--
            ouvinte?.aoPerderBola()
        }
        return colisoes
    }

    private class Impacto(val tempo: Float, val eixo: Eixo, val tijolo: Tijolo? = null, val noPaddle: Boolean = false)

    private fun primeiroImpacto(dt: Float): Impacto? {
        val caixa = bola.caixa
        var melhor: Impacto? = null
        fun considerar(impacto: Impacto) {
            if (melhor == null || impacto.tempo < melhor!!.tempo) melhor = impacto
        }

        // bordas laterais e teto
        if (bola.vx < 0f) tempoAteLimite(0f - caixa.esquerda, bola.vx, dt)?.let { considerar(Impacto(it, Eixo.X)) }
        if (bola.vx > 0f) tempoAteLimite(largura - caixa.direita, bola.vx, dt)?.let { considerar(Impacto(it, Eixo.X)) }
        if (bola.vy < 0f) tempoAteLimite(tetoDoJogo - caixa.topo, bola.vy, dt)?.let { considerar(Impacto(it, Eixo.Y)) }

        // tijolos: so os candidatos da malha ao longo do trajeto
        val trajeto = caixa.uniao(caixa.deslocada(bola.vx * dt, bola.vy * dt))
        for (tijolo in parede.candidatos(trajeto)) {
            Colisao.tempoDeImpacto(caixa, bola.vx, bola.vy, tijolo.caixa, dt)
                ?.let { considerar(Impacto(it.tempo, it.eixo, tijolo = tijolo)) }
        }

        // paddle, so com a bola descendo
        if (bola.vy > 0f) {
            Colisao.tempoDeImpacto(caixa, bola.vx, bola.vy, paddle.caixa, dt)
                ?.let { considerar(Impacto(it.tempo, it.eixo, noPaddle = true)) }
        }
        return melhor
    }

    private fun tempoAteLimite(distancia: Float, v: Float, dt: Float): Float? {
        val t = (distancia / v).coerceAtLeast(0f)
        return if (t <= dt) t else null
    }

    private fun refletir(eixo: Eixo) {
        when (eixo) {
            Eixo.X -> bola.vx = -bola.vx
            Eixo.Y -> bola.vy = -bola.vy
        }
    }

    /** Centro devolve na vertical; as extremidades abrem o angulo ate 60 graus (doc 03, secao 7.5). */
    private fun rebaterNoPaddle(eixo: Eixo) {
        if (eixo == Eixo.X) {
            refletir(Eixo.X)
            return
        }
        val deslocamento = ((bola.x - paddle.x) / (paddle.largura / 2f)).coerceIn(-1f, 1f)
        val angulo = Math.toRadians((deslocamento * ANGULO_MAXIMO_GRAUS).toDouble()).toFloat()
        val v = velocidadeDoNivel()
        bola.vx = v * sin(angulo)
        bola.vy = -v * cos(angulo)
        ouvinte?.aoRebaterNoPaddle()
    }

    private fun atingir(tijolo: Tijolo) {
        if (!tijolo.destrutivel) return
        val destruido = tijolo.atingir()
        if (destruido) {
            pontos += PONTOS_POR_RESISTENCIA * tijolo.resistenciaInicial
            tijolosDestruidos++
        } else {
            pontos += PONTOS_POR_ACERTO
        }
        ouvinte?.aoAtingirTijolo(destruido)
        if (parede.concluida) {
            estado = Estado.NIVEL_CONCLUIDO
            ouvinte?.aoConcluirNivel()
        }
    }

    private fun colocarBolaNoPaddle() {
        bola.x = paddle.x
        bola.y = paddle.topo - bola.raio - dp(1f)
        bola.vx = 0f
        bola.vy = 0f
    }

    companion object {
        const val VIDAS_INICIAIS = 3
        const val MAX_COLISOES_POR_SUBPASSO = 2

        const val ALTURA_HUD_DP = 44f
        const val ZONA_CONTROLE_DP = 120f
        const val MARGEM_LATERAL_DP = 16f
        const val ESPACO_DP = 3f
        const val FRACAO_AREA_PAREDE = 0.40f

        const val VELOCIDADE_BASE_DP = 400f
        const val VELOCIDADE_MAXIMA_DP = 720f
        const val ANGULO_LANCAMENTO_GRAUS = 22.0
        const val ANGULO_MAXIMO_GRAUS = 60f

        const val PONTOS_POR_ACERTO = 10
        const val PONTOS_POR_RESISTENCIA = 50
    }
}
