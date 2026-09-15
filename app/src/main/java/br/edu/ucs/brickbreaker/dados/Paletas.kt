package br.edu.ucs.brickbreaker.dados

/**
 * Padrao de cores dos tijolos (WF-04). Cada paleta define 4 cores, na ordem:
 * resistencia 1, resistencia 2, resistencia 3 e indestrutivel.
 */
data class Paleta(val id: String, val nome: String, val cores: List<Int>) {
    fun corDe(resistencia: Int): Int = when (resistencia) {
        1 -> cores[0]
        2 -> cores[1]
        3 -> cores[2]
        else -> cores[3]
    }
}

object Paletas {
    const val LIVRE = "livre"

    val CLASSICO = Paleta("classico", "Clássico", listOf(0xFF4FC3F7.toInt(), 0xFFFFB74D.toInt(), 0xFFEF5350.toInt(), 0xFF78909C.toInt()))
    val NEON = Paleta("neon", "Neon", listOf(0xFF39FF14.toInt(), 0xFF00E5FF.toInt(), 0xFFFF2BD6.toInt(), 0xFFB0B0B0.toInt()))
    val MONO = Paleta("mono", "Mono", listOf(0xFFEDEDED.toInt(), 0xFFB3B3B3.toInt(), 0xFF7A7A7A.toInt(), 0xFF474747.toInt()))

    val PRONTAS = listOf(CLASSICO, NEON, MONO)

    fun porId(id: String, coresLivres: List<Int>): Paleta =
        PRONTAS.firstOrNull { it.id == id } ?: Paleta(LIVRE, "Livre", coresLivres)
}
