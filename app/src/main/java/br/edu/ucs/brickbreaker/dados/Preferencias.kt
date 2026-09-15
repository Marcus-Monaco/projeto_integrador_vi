package br.edu.ucs.brickbreaker.dados

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

/** O que a tela de Configuracoes grava (doc 03, secao 6). */
data class Configuracao(
    val paleta: String = Paletas.CLASSICO.id,
    val coresLivres: List<Int> = Paletas.CLASSICO.cores,
    val colunas: Int = 8,
    val alturaTijolo: Int = 18,
) {
    val paletaAtual get() = Paletas.porId(paleta, coresLivres)
}

/** Persistencia com Jetpack DataStore (Preferences). */
class Preferencias(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    suspend fun ler(): Configuracao {
        val p = dataStore.data.first()
        val padrao = Configuracao()
        return Configuracao(
            paleta = p[PALETA] ?: padrao.paleta,
            coresLivres = p[CORES_LIVRES]?.let(::decodificarCores) ?: padrao.coresLivres,
            colunas = p[COLUNAS] ?: padrao.colunas,
            alturaTijolo = p[ALTURA_TIJOLO] ?: padrao.alturaTijolo,
        )
    }

    suspend fun salvar(config: Configuracao) {
        dataStore.edit { p ->
            p[PALETA] = config.paleta
            p[CORES_LIVRES] = config.coresLivres.joinToString(",")
            p[COLUNAS] = config.colunas
            p[ALTURA_TIJOLO] = config.alturaTijolo
        }
    }

    suspend fun melhorPontuacao(): Int = dataStore.data.first()[MELHOR_PONTUACAO] ?: 0

    /** Grava a pontuacao se for recorde e devolve a melhor pontuacao resultante. */
    suspend fun registrarPontuacao(pontos: Int): Int {
        var melhor = 0
        dataStore.edit { p ->
            melhor = maxOf(p[MELHOR_PONTUACAO] ?: 0, pontos)
            p[MELHOR_PONTUACAO] = melhor
        }
        return melhor
    }

    private fun decodificarCores(texto: String): List<Int>? =
        texto.split(",").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 4 }

    private companion object {
        val PALETA = stringPreferencesKey("paleta")
        val CORES_LIVRES = stringPreferencesKey("coresLivres")
        val COLUNAS = intPreferencesKey("colunas")
        val ALTURA_TIJOLO = intPreferencesKey("alturaTijolo")
        val MELHOR_PONTUACAO = intPreferencesKey("melhorPontuacao")
    }
}
