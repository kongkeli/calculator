package com.example.calculator;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class HelloController {

    @FXML private TextField display;

    @FXML private VBox helpPopup;
    @FXML private VBox secretPopup;
    @FXML private VBox historyPopup;
    @FXML private Label historyLabel;

    private PauseTransition helpTimer;
    private PauseTransition secretTimer;

    private String currentExpression = "";
    private boolean cursorVisible = true;

    private List<String> historyList = new ArrayList<>();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            display.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyboard);
            display.setFocusTraversable(false);
        });

        Timeline cursorTimeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            cursorVisible = !cursorVisible;
            updateDisplay();
        }));
        cursorTimeline.setCycleCount(Animation.INDEFINITE);
        cursorTimeline.play();
    }

    private void updateDisplay() {
        if (cursorVisible) {
            display.setText(currentExpression + "_");
        } else {
            display.setText(currentExpression);
        }
    }

    @FXML
    protected void handleInput(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        processInput(clickedButton.getText());
    }

    private void processInput(String value) {
        if (currentExpression.isEmpty() || currentExpression.equals("Error")) {
            // Δεν αφήνουμε σύμβολα να μπουν πρώτα, ΕΚΤΟΣ από - και √
            if (value.equals("+") || value.equals("*") || value.equals("/") || value.equals("%") || value.equals("=") || value.equals("^")) {
                return;
            }
            if (currentExpression.equals("Error")) {
                currentExpression = "";
            }
        }

        currentExpression += value;
        cursorVisible = true;
        updateDisplay();
    }

    @FXML
    protected void handleKeyboard(KeyEvent event) {
        if (helpPopup.isVisible() || secretPopup.isVisible() || historyPopup.isVisible()) return;

        String key = event.getText();

        if (event.getCode().toString().equals("ENTER")) {
            handleEqual();
            event.consume();
            return;
        }
        if (event.getCode().toString().equals("BACK_SPACE")) {
            handleDelete();
            event.consume();
            return;
        }

        if (key.matches("[0-9\\+\\-\\*\\/\\.\\(\\)\\[\\]\\%\\^]")) {
            processInput(key);
            event.consume();
        }
        // Αν πατήσεις 'v' στο πληκτρολόγιο, βγάζει Ρίζα (√)
        else if (key.equalsIgnoreCase("v")) {
            processInput("√");
            event.consume();
        }
    }

    @FXML
    protected void handleDelete() {
        if (!currentExpression.isEmpty() && !currentExpression.equals("Error")) {
            currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
            cursorVisible = true;
            updateDisplay();
        }
    }

    @FXML
    protected void handleClear() {
        currentExpression = "";
        cursorVisible = true;
        updateDisplay();
    }

    @FXML
    protected void handleEqual() {
        if (currentExpression.isEmpty()) return;

        if (currentExpression.equals("23069") || currentExpression.equals("21054")) {
            openSecret();
            return;
        }

        try {
            double result = eval(currentExpression);
            String resultStr;

            if (result == (long) result) {
                resultStr = String.format("%d", (long) result);
            } else {
                resultStr = String.valueOf(result);
            }

            String historyEntry = currentExpression + " = " + resultStr;
            historyList.add(historyEntry);

            if (historyList.size() > 8) {
                historyList.remove(0);
            }
            updateHistoryLabel();

            currentExpression = resultStr;

        } catch (Exception e) {
            currentExpression = "Error";
        }

        cursorVisible = true;
        updateDisplay();
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() { ch = (++pos < str.length()) ? str.charAt(pos) : -1; }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else if (eat('%')) x %= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                // Υπολογισμός Ρίζας (Math.sqrt)
                if (eat('√')) return Math.sqrt(parseFactor());

                double x;
                int startPos = this.pos;
                if (eat('(') || eat('[')) {
                    x = parseExpression();
                    eat(')'); eat(']');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                // Υπολογισμός Δύναμης (Math.pow)
                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }

    // --- POP-UP ΛΟΓΙΚΗ ---
    @FXML
    protected void openHelp(MouseEvent event) {
        if (helpTimer != null) helpTimer.stop();
        if (secretTimer != null) secretTimer.stop();
        secretPopup.setVisible(false);
        historyPopup.setVisible(false);

        helpPopup.setVisible(true);
        helpTimer = new PauseTransition(Duration.seconds(20));
        helpTimer.setOnFinished(e -> helpPopup.setVisible(false));
        helpTimer.play();
    }

    @FXML
    protected void closeHelp(ActionEvent event) {
        helpPopup.setVisible(false);
        if (helpTimer != null) helpTimer.stop();
    }

    private void openSecret() {
        if (secretTimer != null) secretTimer.stop();
        if (helpTimer != null) helpTimer.stop();
        helpPopup.setVisible(false);
        historyPopup.setVisible(false);

        secretPopup.setVisible(true);

        secretTimer = new PauseTransition(Duration.seconds(10));
        secretTimer.setOnFinished(e -> {
            secretPopup.setVisible(false);
            currentExpression = "";
            updateDisplay();
        });
        secretTimer.play();
    }

    @FXML
    protected void closeSecret(ActionEvent event) {
        secretPopup.setVisible(false);
        if (secretTimer != null) secretTimer.stop();
        currentExpression = "";
        updateDisplay();
    }

    @FXML
    protected void openHistory(MouseEvent event) {
        if (helpTimer != null) helpTimer.stop();
        if (secretTimer != null) secretTimer.stop();
        helpPopup.setVisible(false);
        secretPopup.setVisible(false);
        historyPopup.setVisible(true);
    }

    @FXML
    protected void closeHistory(ActionEvent event) {
        historyPopup.setVisible(false);
    }

    @FXML
    protected void clearHistory(ActionEvent event) {
        historyList.clear();
        updateHistoryLabel();
    }

    private void updateHistoryLabel() {
        if (historyList.isEmpty()) {
            historyLabel.setText("No history yet_");
        } else {
            historyLabel.setText(String.join("\n", historyList));
        }
    }
}