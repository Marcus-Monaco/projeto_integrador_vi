package br.edu.ucs.brickbreaker.jogo

import kotlin.math.max
import kotlin.math.min

/**
 * Retangulo alinhado aos eixos, em pixels.
 * O motor usa esta classe em vez de `RectF` para que a fisica rode em teste unitario puro, sem Android.
 */
data class Caixa(val esquerda: Float, val topo: Float, val direita: Float, val base: Float) {
    val largura get() = direita - esquerda
    val altura get() = base - topo
    val centroX get() = (esquerda + direita) / 2f
    val centroY get() = (topo + base) / 2f

    fun deslocada(dx: Float, dy: Float) = Caixa(esquerda + dx, topo + dy, direita + dx, base + dy)

    fun uniao(outra: Caixa) = Caixa(
        min(esquerda, outra.esquerda), min(topo, outra.topo),
        max(direita, outra.direita), max(base, outra.base),
    )
}
