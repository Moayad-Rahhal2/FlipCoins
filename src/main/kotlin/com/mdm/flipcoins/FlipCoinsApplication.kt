package com.mdm.flipcoins

import javafx.animation.PauseTransition
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Duration

class FlipCoinsApplication : Application() {

    override fun start(stage: Stage) {
        val fxmlUrl = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/flip-view.fxml")
        requireNotNull(fxmlUrl) { "flip-view.fxml not found on classpath at /com/mdm/flipcoins/flip-view.fxml" }
        val loader = FXMLLoader(fxmlUrl)
        val root: Parent = loader.load()

        val scene = Scene(root, 800.0, 560.0)

        val cssUrl = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/style.css")
        if (cssUrl == null) {
            System.err.println("WARNING: style.css not found at /com/mdm/flipcoins/style.css")
        } else {
            val cssExternal = cssUrl.toExternalForm()
            if (!scene.stylesheets.contains(cssExternal)) {
                scene.stylesheets.add(cssExternal)
            }
        }

        // Debug: print stylesheets
        println("Loaded stylesheets:")
        scene.stylesheets.forEach { println(" - $it") }

        stage.title = "FlipCoins — Test your luck"
        stage.scene = scene
        stage.show()

        // Ensure CSS is applied to the live scene graph
        Platform.runLater {
            try {
                root.applyCss()
                root.layout()
            } catch (t: Throwable) {
                // ignore; applyCss/layout can occasionally throw on some platforms
            }
        }

        // Robustly hide spinner arrows (retries a few times in case children are created lazily)
        hideSpinnerArrowsWithRetries(root, retries = 4, delayMs = 60)
    }

    private fun hideSpinnerArrowsWithRetries(root: Parent, retries: Int, delayMs: Int) {
        var attempts = 0

        fun attemptHide() {
            val inc = root.lookupAll(".increment-arrow-button")
            val dec = root.lookupAll(".decrement-arrow-button")
            val found = inc.size + dec.size
            if (found > 0) {
                var hidden = 0
                (inc + dec).forEach { n ->
                    (n as? Node)?.let {
                        if (it.isVisible || it.isManaged) {
                            it.isVisible = false
                            it.isManaged = false
                            hidden++
                        }
                    }
                }
                println("hideSpinnerArrowsWithRetries: found $found arrow node(s), hid $hidden")
            } else {
                println("hideSpinnerArrowsWithRetries: no arrow nodes found on attempt ${attempts + 1}")
            }

            attempts++
            if (attempts < retries && (inc.isEmpty() && dec.isEmpty())) {
                val pause = PauseTransition(Duration.millis(delayMs.toDouble()))
                pause.setOnFinished { attemptHide() }
                pause.play()
            }
        }

        // start attempts on the FX thread
        Platform.runLater { attemptHide() }
    }
}