package com.mdm.flipcoins

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class FlipCoinsApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlUrl = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/flip-view.fxml")
        requireNotNull(fxmlUrl) { "flip-view.fxml not found on classpath at /com/mdm/flipcoins/flip-view.fxml" }
        val loader = FXMLLoader(fxmlUrl)
        val root: Parent = loader.load()

        val scene = Scene(root, 900.0, 620.0)

        val cssUrl = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/style.css")
        if (cssUrl == null) {
            System.err.println("WARNING: style.css not found at /com/mdm/flipcoins/style.css")
        } else {
            val cssExternal = cssUrl.toExternalForm()
            // ensure stylesheet is present
            if (!scene.stylesheets.contains(cssExternal)) scene.stylesheets.add(cssExternal)
        }

        println("Loaded stylesheets:")
        scene.stylesheets.forEach { println(" - $it") }

        stage.title = "FlipCoins — Test your luck"
        stage.scene = scene
        stage.show()

        // Debug: print key node styleClass lists (via controller)
        try {
            val controller = loader.getController() as? AppController
            if (controller != null) {
                Platform.runLater {
                    println("flipButton style classes: ${controller.flipButton.styleClass}")
                    println("winnerBanner style classes: ${controller.winnerBanner.styleClass}")
                    println("root style classes: ${root.styleClass}")
                }
            } else {
                println("Controller not available for debug printing.")
            }
        } catch (t: Throwable) {
            println("Could not inspect controller: ${t.message}")
        }

        // Re-apply stylesheet after show to ensure it is last (overrides others added late)
        Platform.runLater {
            try {
                val cssExternal = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/style.css")?.toExternalForm()
                if (cssExternal != null) {
                    // remove if present and re-add to end so it is applied after any others
                    scene.stylesheets.remove(cssExternal)
                    scene.stylesheets.add(cssExternal)
                    println("Re-applied stylesheet to scene (ensured last in order).")
                }
            } catch (t: Throwable) {
                println("Error re-applying stylesheet: ${t.message}")
            }
        }
    }
}