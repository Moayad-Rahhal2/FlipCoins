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
        // Setup description counter (safe if injected)
        if (this::descriptionField.isInitialized && this::descCount.isInitialized) {
            descriptionField.textProperty().addListener { _, _, new ->
                descCount.text = (new?.length ?: 0).toString()
            }
        }

        // Setup spinner (guarded)
        if (this::oddSpinner.isInitialized) {
            setupSpinner(oddSpinner)
        } else {
            // defensive: try to lookup later if injection missed for some reason
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
                } catch (t: Throwable) {
                    // ignore; we'll show friendly message elsewhere if needed
                }
            }
        }

        // Ensure radio buttons are mutually exclusive
        if (this::headRadio.isInitialized && this::tailRadio.isInitialized) {
            if (headRadio.toggleGroup == null && tailRadio.toggleGroup == null) {
                val group = ToggleGroup()
                headRadio.toggleGroup = group
                tailRadio.toggleGroup = group
            }
            headRadio.isSelected = true
        }

        // Banner defaults
        if (this::winnerBanner.isInitialized) {
            winnerBanner.isVisible = false
            winnerBanner.isManaged = false
        }

        // counters defaults
        if (this::headCountLabel.isInitialized) headCountLabel.text = "0"
        if (this::tailCountLabel.isInitialized) tailCountLabel.text = "0"
        if (this::summaryLabel.isInitialized) summaryLabel.text = ""
    }

    private fun setupSpinner(spinner: Spinner<Int>) {
        spinner.isEditable = true
        val factory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, Int.MAX_VALUE, 1, 2)
        spinner.valueFactory = factory
    }

    // Presets
    @FXML fun onPreset1(e: ActionEvent?) = setSpinnerValue(1)
    @FXML fun onPreset3(e: ActionEvent?) = setSpinnerValue(3)
    @FXML fun onPreset5(e: ActionEvent?) = setSpinnerValue(5)
    @FXML fun onPreset7(e: ActionEvent?) = setSpinnerValue(7)

    private fun setSpinnerValue(v: Int) {
        if (this::oddSpinner.isInitialized) {
            oddSpinner.valueFactory.value = v
        }
    }

    @FXML
    fun onFlip(event: ActionEvent?) {
        // Defensive checks for required injected nodes
        if (!this::oddSpinner.isInitialized || !this::flipButton.isInitialized || !this::outputArea.isInitialized) {
            showAlert("UI error", "Required UI controls are not initialized. Rebuild and ensure resources are packaged.")
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

        // Read spinner editor value, coerce to odd positive
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
        // Update spinner display
        oddSpinner.valueFactory.value = odd

        val playerChoice = if (this::headRadio.isInitialized && headRadio.isSelected) "Head" else "Tail"

        if (runningTask != null) return // ignore while running

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
                    val flip = random.nextInt(2) // 0=head, 1=tail
                    if (flip == 0) headCount++ else tailCount++

                    val flipText = "Flip $i: ${if (flip == 0) "Head" else "Tail"}\n"
                    Platform.runLater {
                        outputArea.appendText(flipText)
                        if (this@AppController::headCountLabel.isInitialized) headCountLabel.text = headCount.toString()
                        if (this@AppController::tailCountLabel.isInitialized) tailCountLabel.text = tailCount.toString()
                    }

                    if (headCount == target || tailCount == target) break

                    try {
                        Thread.sleep(120) // brief pause so user can see progress
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
        winnerBanner.text = if (winner == playerChoice) "$winner WINS! You won!" else "$winner WINS!"
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