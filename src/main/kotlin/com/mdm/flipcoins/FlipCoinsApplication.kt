package com.mdm.flipcoins

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class FlipCoinsApplication : Application() {
    override fun start(stage: Stage) {
        // load FXML using absolute resource path
        val fxmlUrl = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/flip-view.fxml")
        requireNotNull(fxmlUrl) { "flip-view.fxml not found on classpath at /com/mdm/flipcoins/flip-view.fxml" }
        val fxmlLoader = FXMLLoader(fxmlUrl)
        val root: Parent = fxmlLoader.load()
        val scene = Scene(root, 640.0, 480.0)

        // load stylesheet using absolute resource path and fail fast if missing
        val cssUrl = FlipCoinsApplication::class.java.getResource("/com/mdm/flipcoins/style.css")
        requireNotNull(cssUrl) { "style.css not found on classpath at /com/mdm/flipcoins/style.css" }
        scene.stylesheets.add(cssUrl.toExternalForm())

        // helpful debug print (remove if not needed)
        println("Loaded stylesheets: ${scene.stylesheets}")

        stage.title = "FlipCoins — Test your luck"
        stage.scene = scene
        stage.show()
    }
}