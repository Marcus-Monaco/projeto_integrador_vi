package br.edu.ucs.brickbreaker.niveis

/**
 * PRNG mulberry32. O `Random` do Kotlin aceita semente, mas o algoritmo pode mudar entre versoes;
 * com um gerador proprio a mesma semente produz a mesma parede em qualquer aparelho (doc 03, secao 4.4).
 * E o mesmo algoritmo usado em tools/gen-wireframes.mjs, entao jogo e wireframe mostram a mesma parede.
 */
class Aleatorio(semente: Int) {
    private var estado = semente

    /** Proximo valor no intervalo [0, 1). */
    fun proximo(): Double {
        estado += 0x6D2B79F5
        var t = estado
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }
}
