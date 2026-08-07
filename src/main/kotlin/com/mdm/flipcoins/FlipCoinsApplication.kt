package com.mdm.flipcoins

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class FlipCoinsApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(FlipCoinsApplication::class.java.getResource("flip-view.fxml"))
        val scene = Scene(fxmlLoader.load(), 320.0, 240.0)
        stage.title = "Test your luck"
        stage.scene = scene
        stage.show()
    }
}
  
