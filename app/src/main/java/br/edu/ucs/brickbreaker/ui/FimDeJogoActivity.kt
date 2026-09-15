package br.edu.ucs.brickbreaker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import br.edu.ucs.brickbreaker.R
import br.edu.ucs.brickbreaker.niveis.GeradorDeParede

/** WF-14: um layout com duas variantes, fim de jogo (vidas esgotadas) e vitoria (nivel 5 concluido). */
class FimDeJogoActivity : TelaCheiaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fim_de_jogo)
        respeitarRecorte(findViewById(R.id.raiz))

        val vitoria = intent.getBooleanExtra(EXTRA_VITORIA, false)
        val formatar = { p: Int -> "%03d %03d".format(p / 1000, p % 1000) }

        findViewById<TextView>(R.id.titulo).setText(if (vitoria) R.string.voce_venceu else R.string.fim_de_jogo)
        findViewById<TextView>(R.id.subtitulo).setText(if (vitoria) R.string.mensagem_vitoria else R.string.mensagem_derrota)
        findViewById<TextView>(R.id.valorPontos).text = formatar(intent.getIntExtra(EXTRA_PONTOS, 0))
        findViewById<TextView>(R.id.valorNiveis).text =
            getString(R.string.n_de_total, intent.getIntExtra(EXTRA_NIVEIS, 0), GeradorDeParede.TOTAL_DE_NIVEIS)
        findViewById<TextView>(R.id.valorTijolos).text = intent.getIntExtra(EXTRA_TIJOLOS, 0).toString()
        findViewById<TextView>(R.id.valorMelhor).text = formatar(intent.getIntExtra(EXTRA_MELHOR, 0))

        findViewById<Button>(R.id.botaoJogarNovamente).setOnClickListener {
            startActivity(Intent(this, JogoActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.botaoMenu).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_VITORIA = "vitoria"
        const val EXTRA_PONTOS = "pontos"
        const val EXTRA_NIVEIS = "niveis"
        const val EXTRA_TIJOLOS = "tijolos"
        const val EXTRA_MELHOR = "melhor"
    }
}
