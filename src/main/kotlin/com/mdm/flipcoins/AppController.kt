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
    lateinit var descriptionField: TextField

    @FXML
    lateinit var descCount: Label

    @FXML
    lateinit var oddSpinner: Spinner<Int>

    @FXML
    lateinit var headRadio: RadioButton

    @FXML
    lateinit var tailRadio: RadioButton

    @FXML
    lateinit var flipButton: Button

    @FXML
    lateinit var outputArea: TextArea

    @FXML
    lateinit var headCountLabel: Label

    @FXML
    lateinit var tailCountLabel: Label

    @FXML
    lateinit var summaryLabel: Label

    @FXML
    lateinit var winnerBanner: Label

    private var runningTask: Task<Unit>? = null

    @FXML
    fun initialize() {
        // wire description counter
        descriptionField.textProperty().addListener { _, _, new ->
            descCount.text = (new?.length ?: 0).toString()
        }

        // ensure radio buttons are in a ToggleGroup
        if (headRadio.toggleGroup == null && tailRadio.toggleGroup == null) {
            val group = ToggleGroup()
            headRadio.toggleGroup = group
            tailRadio.toggleGroup = group
        }
        headRadio.isSelected = true

        // banner init
        winnerBanner.isVisible = false
        winnerBanner.isManaged = false

        headCountLabel.text = "0"
        tailCountLabel.text = "0"
        summaryLabel.text = ""

        // Try to use the injected spinner; if it's not injected, attempt to find it after scene is ready
        if (!this::oddSpinner.isInitialized) {
            Platform.runLater {
                val lookup = descriptionField.scene?.lookup("#oddSpinner") as? Spinner<*>
                if (lookup != null) {
                    @Suppress("UNCHECKED_CAST")
                    oddSpinner = lookup as Spinner<Int>
                    // initialize spinner factory if still needed
                    oddSpinner.isEditable = true
                    val factory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, Int.MAX_VALUE, 1, 2)
                    oddSpinner.valueFactory = factory
                } else {
                    // give a clear message so you can see what's wrong
                    showAlert("UI error", "Spinner control not found in FXML. Check fx:id=\"oddSpinner\" and resource packaging.")
                }
            }
        } else {
            // normal spinner setup if injected
            oddSpinner.isEditable = true
            val factory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, Int.MAX_VALUE, 1, 2)
            oddSpinner.valueFactory = factory
        }
    }

    // Preset handlers (wired from FXML)
    @FXML fun onPreset1(e: ActionEvent?) = setSpinnerValue(1)
    @FXML fun onPreset3(e: ActionEvent?) = setSpinnerValue(3)
    @FXML fun onPreset5(e: ActionEvent?) = setSpinnerValue(5)
    @FXML fun onPreset7(e: ActionEvent?) = setSpinnerValue(7)

    private fun setSpinnerValue(v: Int) {
        oddSpinner.valueFactory.value = v
    }

    @FXML
    fun onFlip(event: ActionEvent?) {
        // Clear previous results
        outputArea.clear()
        winnerBanner.isVisible = false
        winnerBanner.isManaged = false
        summaryLabel.text = ""

        val desc = descriptionField.text?.trim().takeUnless { it.isNullOrEmpty() } ?: "(no description)"

        // Read spinner value safely (editable spinner might have typed text)
        val odd = try {
            val text = oddSpinner.editor.text.trim()
            val parsed = text.toInt()
            if (parsed <= 0) {
                showAlert("Invalid input", "Please enter a positive odd number.")
                return
            }
            if (parsed % 2 == 0) parsed + 1 else parsed
        } catch (ex: Exception) {
            showAlert("Invalid input", "Please enter a numeric odd integer.")
            return
        }

        // update spinner display with validated odd
        oddSpinner.valueFactory.value = odd

        val playerChoice = if (headRadio.isSelected) "Head" else "Tail"

        if (runningTask != null) {
            // Already running; ignore or optionally cancel & restart
            return
        }

        setControlsDisabled(true)
        headCountLabel.text = "0"
        tailCountLabel.text = "0"

        val task = object : Task<Unit>() {
            override fun call() {
                Platform.runLater {
                    outputArea.appendText("Description: $desc\n")
                    outputArea.appendText("Best of: $odd\n")
                    outputArea.appendText("You chose: $playerChoice\n")
                    outputArea.appendText("Simulating...\n\n")
                }

                val target = (odd + 1) / 2
                var headCount = 0
                var tailCount = 0

                for (i in 1..odd) {
                    if (isCancelled) break
                    val flip = random.nextInt(2) // 0=head, 1=tail
                    if (flip == 0) headCount++ else tailCount++

                    val flipText = "Flip $i: ${if (flip == 0) "Head" else "Tail"}\n"
                    Platform.runLater {
                        outputArea.appendText(flipText)
                        headCountLabel.text = headCount.toString()
                        tailCountLabel.text = tailCount.toString()
                    }

                    if (headCount == target || tailCount == target) break

                    try {
                        Thread.sleep(120)
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

                Platform.runLater {
                    outputArea.appendText(resultText.toString())
                    showWinnerBanner(winner, playerChoice)
                    summaryLabel.text = "Total flips: ${headCount + tailCount}"
                }
            }
        }

        task.setOnSucceeded {
            runningTask = null
            setControlsDisabled(false)
        }

        task.setOnCancelled {
            runningTask = null
            Platform.runLater {
                outputArea.appendText("\nSimulation cancelled.\n")
                setControlsDisabled(false)
            }
        }

        task.setOnFailed {
            runningTask = null
            setControlsDisabled(false)
            showAlert("Error", "Simulation failed: ${task.exception?.message}")
        }

        runningTask = task
        Thread(task).start()
    }

    private fun showWinnerBanner(winner: String, playerChoice: String) {
        winnerBanner.text = if (winner == playerChoice) "$winner WINS! You won!" else "$winner WINS!"
        winnerBanner.styleClass.removeAll("winner-head", "winner-tail")
        winnerBanner.styleClass.add(if (winner == "Head") "winner-head" else "winner-tail")
        winnerBanner.isManaged = true
        winnerBanner.isVisible = true
    }

    // Helper to enable/disable inputs
    private fun setControlsDisabled(disabled: Boolean) {
        flipButton.isDisable = disabled
        descriptionField.isDisable = disabled
        oddSpinner.isDisable = disabled
        headRadio.isDisable = disabled
        tailRadio.isDisable = disabled
    }

    private fun showAlert(title: String, text: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = text
        alert.showAndWait()
    }
}