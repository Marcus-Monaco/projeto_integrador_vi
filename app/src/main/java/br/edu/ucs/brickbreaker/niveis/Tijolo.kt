package br.edu.ucs.brickbreaker.niveis

import br.edu.ucs.brickbreaker.jogo.Caixa

/**
 * Um tijolo da parede.
 *
 * @property resistencia acertos restantes: 1..3; [INDESTRUTIVEL] nunca e destruido; 0 = destruido.
 */
class Tijolo(
    val linha: Int,
    val coluna: Int,
    val resistenciaInicial: Int,
    val caixa: Caixa,
) {
    var resistencia = resistenciaInicial
        private set

    val destrutivel get() = resistenciaInicial > 0
    val vivo get() = resistencia != 0

    /** Aplica um acerto. Devolve true se o tijolo foi destruido por este acerto. */
    fun atingir(): Boolean {
        if (!destrutivel || !vivo) return false
        resistencia--
        return resistencia == 0
    }

    companion object {
        const val INDESTRUTIVEL = -1

        fun resistenciaDe(caractere: Char): Int? = when (caractere) {
            '1' -> 1
            '2' -> 2
            '3' -> 3
            'X' -> INDESTRUTIVEL
            else -> null
        }
    }
}
