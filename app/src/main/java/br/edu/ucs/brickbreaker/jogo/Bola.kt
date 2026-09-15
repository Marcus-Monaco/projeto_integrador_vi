package br.edu.ucs.brickbreaker.jogo

import kotlin.math.hypot

/** Bola: centro, raio e vetor de velocidade em px/s. Na colisao e tratada como quadrado (doc 03, secao 7.3). */
class Bola(var raio: Float) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f

    val caixa get() = Caixa(x - raio, y - raio, x + raio, y + raio)
    val velocidade get() = hypot(vx, vy)

    fun avancar(dt: Float) {
        x += vx * dt
        y += vy * dt
    }
}

/** Plataforma controlada pelo jogador. Acompanha o alvo do toque com suavizacao. */
class Paddle(var largura: Float, var altura: Float) {
    var x = 0f          // centro
    var topo = 0f
    var alvoX = 0f

    val caixa get() = Caixa(x - largura / 2f, topo, x + largura / 2f, topo + altura)

    fun seguirAlvo(dt: Float, limiteEsquerda: Float, limiteDireita: Float) {
        val alvo = alvoX.coerceIn(limiteEsquerda + largura / 2f, limiteDireita - largura / 2f)
        val fator = (dt * SUAVIZACAO).coerceAtMost(1f)
        x += (alvo - x) * fator
    }

    private companion object {
        const val SUAVIZACAO = 22f
    }
}
