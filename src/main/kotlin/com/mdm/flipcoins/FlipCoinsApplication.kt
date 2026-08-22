package com.mdm.flipcoins

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class FlipCoinsApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(FlipCoinsApplication::class.java.getResource("flip-view.fxml"))
        val root: Parent = fxmlLoader.load()
        val scene = Scene(root, 640.0, 480.0)

        // Try to load stylesheet from same package
        val cssUrl = FlipCoinsApplication::class.java.getResource("style.css")
        if (cssUrl == null) {
            System.err.println("WARNING: style.css not found at com/mdm/flipcoins/style.css")
        } else {
            scene.stylesheets.add(cssUrl.toExternalForm())
        }

        stage.title = "FlipCoins — Test your luck"
        stage.scene = scene
        stage.show()
    }
}