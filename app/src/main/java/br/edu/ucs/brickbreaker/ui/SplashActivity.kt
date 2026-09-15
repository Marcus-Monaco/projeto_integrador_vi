package br.edu.ucs.brickbreaker.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import br.edu.ucs.brickbreaker.R
import br.edu.ucs.brickbreaker.audio.Sons
import br.edu.ucs.brickbreaker.dados.Preferencias
import br.edu.ucs.brickbreaker.niveis.GeradorDeParede
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** WF-01: abertura de 1,5 s que pre-carrega sons, preferencias e as matrizes dos 5 niveis. */
@SuppressLint("CustomSplashScreen")
class SplashActivity : TelaCheiaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val barra = findViewById<ProgressBar>(R.id.barraCarregamento)
        ObjectAnimator.ofInt(barra, "progress", 0, 100).setDuration(DURACAO_MS).start()

        lifecycleScope.launch {
            Sons.carregar(this@SplashActivity)
            Preferencias(this@SplashActivity).ler()
            (1..GeradorDeParede.TOTAL_DE_NIVEIS).forEach { GeradorDeParede.matrizDoNivel(it) }
            delay(DURACAO_MS)
            startActivity(Intent(this@SplashActivity, MenuActivity::class.java).apply {
                intent.extras?.let { putExtras(it) }
            })
            finish()
        }
    }

    private companion object {
        const val DURACAO_MS = 1500L
    }
}
