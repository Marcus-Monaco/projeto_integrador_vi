package br.edu.ucs.brickbreaker.jogo

import kotlin.math.max
import kotlin.math.min

enum class Eixo { X, Y }

/** Resultado da deteccao: quando a bola toca e em qual face (que define o eixo da reflexao). */
data class Contato(val tempo: Float, val eixo: Eixo)

object Colisao {

    /**
     * Swept AABB pelo metodo das faixas (doc 03, secao 7.3).
     *
     * Devolve o instante, dentro de `[0, dt]`, em que a caixa da bola se movendo a (vx, vy) encosta no alvo,
     * ou `null` se nao encosta neste intervalo.
     */
    fun tempoDeImpacto(bola: Caixa, vx: Float, vy: Float, alvo: Caixa, dt: Float): Contato? {
        // parado em um eixo: so ha impacto se ja houver sobreposicao nesse eixo
        if (vx == 0f && (bola.direita <= alvo.esquerda || bola.esquerda >= alvo.direita)) return null
        if (vy == 0f && (bola.base <= alvo.topo || bola.topo >= alvo.base)) return null

        // distancias ate entrar e ate sair, em cada eixo
        val entradaX = if (vx > 0) alvo.esquerda - bola.direita else alvo.direita - bola.esquerda
        val saidaX = if (vx > 0) alvo.direita - bola.esquerda else alvo.esquerda - bola.direita
        val entradaY = if (vy > 0) alvo.topo - bola.base else alvo.base - bola.topo
        val saidaY = if (vy > 0) alvo.base - bola.topo else alvo.topo - bola.base

        val tEntradaX = if (vx == 0f) Float.NEGATIVE_INFINITY else entradaX / vx
        val tSaidaX = if (vx == 0f) Float.POSITIVE_INFINITY else saidaX / vx
        val tEntradaY = if (vy == 0f) Float.NEGATIVE_INFINITY else entradaY / vy
        val tSaidaY = if (vy == 0f) Float.POSITIVE_INFINITY else saidaY / vy

        val tEntrada = max(tEntradaX, tEntradaY)
        val tSaida = min(tSaidaX, tSaidaY)

        if (tEntrada > tSaida || tEntrada < 0f || tEntrada > dt) return null

        // o eixo que entrou por ultimo e a face atingida
        val eixo = if (tEntradaX > tEntradaY) Eixo.X else Eixo.Y
        return Contato(tEntrada, eixo)
    }
}
