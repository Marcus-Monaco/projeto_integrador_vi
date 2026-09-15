package br.edu.ucs.brickbreaker.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Base de todas as telas: modo imersivo (status bar e barra de navegacao ocultas), requisito (a).
 * As barras voltam temporariamente com um gesto de deslizar a partir da borda.
 */
abstract class TelaCheiaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onResume() {
        super.onResume()
        esconderBarras()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) esconderBarras()
    }

    private fun esconderBarras() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** Afasta o conteudo do recorte da camera, sem reservar espaco para as barras (que estao ocultas). */
    protected fun respeitarRecorte(view: View, aoAplicar: ((topo: Int) -> Unit)? = null) {
        val base = intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val recorte = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(base[0] + recorte.left, base[1] + recorte.top, base[2] + recorte.right, base[3] + recorte.bottom)
            aoAplicar?.invoke(recorte.top)
            insets
        }
    }
}
