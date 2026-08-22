package com.mdm.flipcoins

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.scene.control.*
import java.util.Random

class AppController {
    private val random = Random()

    @FXML lateinit var versionLabel: Label
    @FXML lateinit var descriptionField: TextField
    @FXML lateinit var oddSpinner: Spinner<Int>
    @FXML lateinit var headRadio: RadioButton
    @FXML lateinit var tailRadio: RadioButton
    @FXML lateinit var flipButton: Button
    @FXML lateinit var outputArea: TextArea
    @FXML lateinit var headCountLabel: Label
    @FXML lateinit var tailCountLabel: Label
    @FXML lateinit var summaryLabel: Label
    @FXML lateinit var winnerBanner: Label

    private var runningTask: Task<Unit>? = null

    @FXML
    fun initialize() {

        // spinner setup (defensive)
        if (this::oddSpinner.isInitialized) {
            setupSpinner(oddSpinner)
        } else {
            Platform.runLater {
                try {
                    if (!this::oddSpinner.isInitialized) {
                        val lookup = descriptionField.scene?.lookup("#oddSpinner") as? Spinner<*>
                        if (lookup != null) {
                            @Suppress("UNCHECKED_CAST")
                            oddSpinner = lookup as Spinner<Int>
                            setupSpinner(oddSpinner)
                        }
                    }
                } catch (_: Throwable) {
                    // ignore
                }
            }
        }

        // radio buttons
        if (this::headRadio.isInitialized && this::tailRadio.isInitialized) {
            if (headRadio.toggleGroup == null && tailRadio.toggleGroup == null) {
                val group = ToggleGroup()
                headRadio.toggleGroup = group
                tailRadio.toggleGroup = group
            }
            tailRadio.isSelected = true
        }

        // banner defaults
        if (this::winnerBanner.isInitialized) {
            winnerBanner.isVisible = false
            winnerBanner.isManaged = false
        }

        // counters defaults
        if (this::headCountLabel.isInitialized) headCountLabel.text = "0"
        if (this::tailCountLabel.isInitialized) tailCountLabel.text = "0"
        if (this::summaryLabel.isInitialized) summaryLabel.text = ""

        // Flip button initially disabled until odd spinner has a valid odd positive integer
        if (this::flipButton.isInitialized) flipButton.isDisable = true

        // enable Flip only when spinner editor contains a positive odd integer
        if (this::oddSpinner.isInitialized && this::flipButton.isInitialized) {
            // value changes from the spinner control
            oddSpinner.valueFactory.valueProperty().addListener { _, _, _ -> updateFlipButtonState() }
            // editor text changes when user types
            oddSpinner.editor.textProperty().addListener { _, _, _ -> updateFlipButtonState() }
            Platform.runLater { updateFlipButtonState() }
        }
    }

    private fun updateFlipButtonState() {
        if (!this::oddSpinner.isInitialized || !this::flipButton.isInitialized) return
        val text = oddSpinner.editor.text.trim()
        val ok = try {
            val v = text.toInt()
            v > 0 && (v % 2 == 1)
        } catch (_: Exception) {
            false
        }
        flipButton.isDisable = !ok
        if (ok) {
            try {
                oddSpinner.valueFactory.value = text.toInt()
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private fun setupSpinner(spinner: Spinner<Int>) {
        spinner.isEditable = true
        val factory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, Int.MAX_VALUE, 1, 2)
        spinner.valueFactory = factory
        spinner.editor.text = spinner.value.toString()
    }

    // onFlip: no-arg handler (FXML accepts this)
    @FXML
    fun onFlip() {
        // Defensive checks
        if (!this::oddSpinner.isInitialized || !this::flipButton.isInitialized || !this::outputArea.isInitialized) {
            showAlert("UI error", "Required UI controls are not initialized. Rebuild and ensure resources are packaged.")
            return
        }

        // Validate odd value again as guard
        val odd = try {
            val text = oddSpinner.editor.text.trim()
            val parsed = text.toInt()
            if (parsed <= 0) {
                showAlert("Invalid input", "Please enter a positive odd number.")
                return
            }
            if (parsed % 2 == 0) {
                showAlert("Invalid input", "Please enter an odd number.")
                return
            }
            parsed
        } catch (_: Exception) {
            showAlert("Invalid input", "Please enter a numeric odd integer.")
            return
        }

        outputArea.clear()
        if (this::winnerBanner.isInitialized) {
            winnerBanner.isVisible = false
            winnerBanner.isManaged = false
        }
        if (this::summaryLabel.isInitialized) summaryLabel.text = ""

        val desc = if (this::descriptionField.isInitialized) {
            descriptionField.text?.trim().takeUnless { it.isNullOrEmpty() } ?: "(no description)"
        } else "(no description)"

        val playerChoice = if (this::headRadio.isInitialized && headRadio.isSelected) "Head" else "Tail"

        if (runningTask != null) return

        setControlsDisabled(true)
        if (this::headCountLabel.isInitialized) headCountLabel.text = "0"
        if (this::tailCountLabel.isInitialized) tailCountLabel.text = "0"

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
                    val flip = random.nextInt(2)
                    if (flip == 0) headCount++ else tailCount++

                    val flipText = "Flip $i: ${if (flip == 0) "Head" else "Tail"}\n"
                    Platform.runLater {
                        outputArea.appendText(flipText)
                        if (this@AppController::headCountLabel.isInitialized) headCountLabel.text = headCount.toString()
                        if (this@AppController::tailCountLabel.isInitialized) tailCountLabel.text = tailCount.toString()
                    }

                    if (headCount == target || tailCount == target) break

                    try {
                        Thread.sleep(120)
                    } catch (_: InterruptedException) {
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
                    if (this@AppController::winnerBanner.isInitialized) showWinnerBanner(winner, playerChoice)
                    if (this@AppController::summaryLabel.isInitialized) summaryLabel.text = "Total flips: ${headCount + tailCount}"
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
        winnerBanner.text = if (winner == playerChoice) "$winner WINS!" else "$winner WINS!"
        winnerBanner.styleClass.removeAll("winner-head", "winner-tail")
        winnerBanner.styleClass.add(if (winner == "Head") "winner-head" else "winner-tail")
        winnerBanner.isManaged = true
        winnerBanner.isVisible = true
    }

    private fun setControlsDisabled(disabled: Boolean) {
        if (this::flipButton.isInitialized) flipButton.isDisable = disabled
        if (this::descriptionField.isInitialized) descriptionField.isDisable = disabled
        if (this::oddSpinner.isInitialized) oddSpinner.isDisable = disabled
        if (this::headRadio.isInitialized) headRadio.isDisable = disabled
        if (this::tailRadio.isInitialized) tailRadio.isDisable = disabled
    }

    private fun showAlert(title: String, text: String) {
        Platform.runLater {
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = title
            alert.headerText = null
            alert.contentText = text
            alert.showAndWait()
        }
    }
}