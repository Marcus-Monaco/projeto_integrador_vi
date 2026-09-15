package br.edu.ucs.brickbreaker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import br.edu.ucs.brickbreaker.BuildConfig
import br.edu.ucs.brickbreaker.R

/** WF-02: tela inicial com as tres opcoes do requisito (b). */
class MenuActivity : TelaCheiaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        respeitarRecorte(findViewById(R.id.raiz))

        findViewById<TextView>(R.id.versao).text = "v${BuildConfig.VERSION_NAME}"

        findViewById<Button>(R.id.botaoJogar).setOnClickListener {
            startActivity(Intent(this, JogoActivity::class.java).apply {
                putExtra(JogoActivity.EXTRA_DEMO, intent.getBooleanExtra(JogoActivity.EXTRA_DEMO, false))
            })
        }
        findViewById<Button>(R.id.botaoConfiguracoes).setOnClickListener {
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }
        findViewById<Button>(R.id.botaoIntegrantes).setOnClickListener {
            startActivity(Intent(this, IntegrantesActivity::class.java))
        }
        findViewById<Button>(R.id.botaoSair).setOnClickListener { confirmarSaida() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmarSaida()
        })
    }

    private fun confirmarSaida() {
        AlertDialog.Builder(this)
            .setTitle(R.string.sair_titulo)
            .setMessage(R.string.sair_mensagem)
            .setPositiveButton(R.string.sair) { _, _ -> finishAffinity() }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}
