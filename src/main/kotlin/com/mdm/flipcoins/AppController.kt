package com.mdm.flipcoins

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.event.ActionEvent
import java.util.Random

class AppController {
    private val random = Random()

    @FXML
    private lateinit var descriptionField: TextField

    @FXML
    private lateinit var oddField: TextField

    @FXML
    private lateinit var headRadio: RadioButton

    @FXML
    private lateinit var tailRadio: RadioButton

    @FXML
    private lateinit var flipButton: Button

    @FXML
    private lateinit var restartButton: Button

    @FXML
    private lateinit var exitButton: Button

    @FXML
    private lateinit var outputArea: TextArea

    @FXML
    fun initialize() {
        headRadio.isSelected = true
    }

    @FXML
    fun onFlip(event: ActionEvent?) {
        outputArea.clear()

        val desc = descriptionField.text?.trim().takeUnless { it.isNullOrEmpty() } ?: "(no description)"
        val oddText = oddField.text?.trim() ?: ""

        val odd = try {
            oddText.toInt()
        } catch (e: NumberFormatException) {
            showAlert("Invalid input", "Please enter a numeric odd integer.")
            return
        }

        if (odd <= 0) {
            showAlert("Invalid input", "Please enter a positive odd number.")
            return
        }
        if (odd % 2 == 0) {
            showAlert("Invalid input", "Number is even. Please enter an odd number.")
            return
        }

        val playerChoice = if (headRadio.isSelected) "Head" else "Tail"

        // Disable controls while simulating
        flipButton.isDisable = true
        restartButton.isDisable = true
        exitButton.isDisable = true
        descriptionField.isDisable = true
        oddField.isDisable = true
        headRadio.isDisable = true
        tailRadio.isDisable = true

        // Background task to run flips and update UI incrementally
        val task = object : Task<Unit>() {
            override fun call() {
                Platform.runLater {
                    outputArea.appendText("Description: $desc\n")
                    outputArea.appendText("Best of: $odd\n")
                    outputArea.appendText("You chose: $playerChoice\n")
                    outputArea.appendText("Simulating...\n")
                }

                val target = (odd + 1) / 2
                var headCount = 0
                var tailCount = 0

                for (i in 1..odd) {
                    if (isCancelled) break
                    val flip = random.nextInt(2) // 0 = head, 1 = tail
                    if (flip == 0) headCount++ else tailCount++

                    val flipText = "Flip $i: ${if (flip == 0) "Head" else "Tail"}\n"
                    Platform.runLater { outputArea.appendText(flipText) }

                    if (headCount == target || tailCount == target) break

                    try {
                        Thread.sleep(120) // slight delay so user sees progress
                    } catch (ie: InterruptedException) {
                        if (isCancelled) break
                    }
                }

                val winner = if (headCount > tailCount) "Head" else "Tail"
                val resultText = StringBuilder()
                resultText.append("\nHead Count: $headCount\n")
                resultText.append("Tail Count: $tailCount\n")
                resultText.append("Winner: $winner\n")
                resultText.append("Your result: ${if (winner == playerChoice) "You won!" else "You lost."}\n")

                Platform.runLater { outputArea.appendText(resultText.toString()) }
            }
        }

        task.setOnSucceeded {
            flipButton.isDisable = false
            restartButton.isDisable = false
            exitButton.isDisable = false
            descriptionField.isDisable = false
            oddField.isDisable = false
            headRadio.isDisable = false
            tailRadio.isDisable = false
        }

        task.setOnFailed {
            flipButton.isDisable = false
            restartButton.isDisable = false
            exitButton.isDisable = false
            descriptionField.isDisable = false
            oddField.isDisable = false
            headRadio.isDisable = false
            tailRadio.isDisable = false
            showAlert("Error", "Simulation failed: ${task.exception?.message}")
        }

        Thread(task).start()
    }

    @FXML
    fun onRestart(event: ActionEvent?) {
        descriptionField.clear()
        oddField.clear()
        headRadio.isSelected = true
        outputArea.clear()
    }

    @FXML
    fun onExit(event: ActionEvent?) {
        Platform.exit()
    }

    private fun showAlert(title: String, text: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = text
        alert.showAndWait()
    }
}