package org.example.spaceinvaders;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleObjectProperty; // Beibehalten, falls du es spezifisch brauchst
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class UIManager {
    private Pane uiPane;
    private GameDimensions gameDimensions;
    private Label waveMessageLabel;
    private MusicalInvaders mainApp;
    private SimpleObjectProperty<Label> scoreLabelProperty = new SimpleObjectProperty<>(); // Beibehalten
    private int currentScore = 0;


    public UIManager(Pane uiPane, GameDimensions gameDimensions, MusicalInvaders mainApp) {
        this.uiPane = uiPane;
        this.gameDimensions = gameDimensions;
        this.mainApp = mainApp;
        createWaveMessageLabel();
    }

    public void createWaveMessageLabel() {
        waveMessageLabel = new Label();
        waveMessageLabel.setFont(Font.font("Verdana", gameDimensions.getHeight()*0.05));
        waveMessageLabel.setTextFill(Color.WHITE);
        waveMessageLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-padding: 10px; -fx-border-color: lightgreen; -fx-border-width: 2px;");
        waveMessageLabel.setTextAlignment(TextAlignment.CENTER);
        waveMessageLabel.setVisible(false);
        uiPane.getChildren().add(waveMessageLabel);
    }

    public void showPopupMessage(String message, double durationSeconds) {
        waveMessageLabel.setText(message);
        waveMessageLabel.autosize();
        waveMessageLabel.setLayoutX((gameDimensions.getWidth()-waveMessageLabel.getWidth())/2);
        waveMessageLabel.setLayoutY((gameDimensions.getHeight()-waveMessageLabel.getHeight())/3);

        waveMessageLabel.setOpacity(0);
        waveMessageLabel.setVisible(true);

        SequentialTransition sequence = getSequentialTransition(durationSeconds);
        sequence.play();
    }

    @NotNull
    private SequentialTransition getSequentialTransition(double durationSeconds) {
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), waveMessageLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition displayPause = new PauseTransition(Duration.seconds(durationSeconds));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), waveMessageLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> waveMessageLabel.setVisible(false));

        SequentialTransition sequence = new SequentialTransition(fadeIn, displayPause, fadeOut);
        return sequence;
    }

    public void showWaveStartMessage(int waveNumber) {
        showPopupMessage("Wave " + waveNumber + " started", 1.5);
    }

    public void showWaveClearMessage(int nextWaveNumber) {
        showPopupMessage("Wave Cleared!\nGet Ready for Wave " + nextWaveNumber, 2);
    }

    public void showBossSpawnMessage() {
        showPopupMessage("!!! BOSS INCOMING !!!", 2);
    }

    public void showBossDefeatedMessage() {
        showPopupMessage("BOSS DEFEATED!", 2);
    }

    public void createScoreLabel() {
        Label label = new Label("Score: 0");
        label.setTextFill(Color.WHITE);
        // Schriftgröße relativ zur Höhe
        label.setFont(new Font("Consolas", gameDimensions.getHeight() * (20.0 / 600.0)));
        // Position relativ
        label.setLayoutX(gameDimensions.getWidth() * (15.0/800.0));
        label.setLayoutY(gameDimensions.getHeight() * (10.0/600.0));
        scoreLabelProperty.set(label); // Property setzen
        uiPane.getChildren().add(label);
    }

    public void updateScoreDisplay() {
        if (scoreLabelProperty.get() != null) {
            scoreLabelProperty.get().setText("Score: " + currentScore);
        }
    }

    public void addScore(int points) {
        this.currentScore += points;
        updateScoreDisplay();
    }
    public void resetScore() {
        this.currentScore = 0;
        updateScoreDisplay();
    }

    public int getCurrentScore() {
        return currentScore;
    }
}