package org.example.spaceinvaders;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
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
    private Label scoreLabel;
    private int currentScore = 0;

    private static final String GAME_FONT_NAME = "Press Start 2P";

    public UIManager(Pane uiPane, GameDimensions gameDimensions, MusicalInvaders mainApp) {
        this.uiPane = uiPane;
        this.gameDimensions = gameDimensions;
        createWaveMessageLabel();
        // Score Label wird durch setUiPane oder resetScore initial erstellt
    }

    public void setUiPane(Pane newUiPane) {
        if (this.uiPane != null) {
            if (waveMessageLabel != null) this.uiPane.getChildren().remove(waveMessageLabel);
            if (scoreLabel != null) this.uiPane.getChildren().remove(scoreLabel);
        }
        this.uiPane = newUiPane;
        createWaveMessageLabel();
        createScoreLabel();
        resetScore();
    }


    public void createWaveMessageLabel() {
        if (waveMessageLabel != null && uiPane.getChildren().contains(waveMessageLabel)) {
            uiPane.getChildren().remove(waveMessageLabel);
        }
        waveMessageLabel = new Label();
        waveMessageLabel.setFont(Font.font(GAME_FONT_NAME, gameDimensions.getHeight() * 0.032)); // Größe leicht anpassbar
        waveMessageLabel.setTextFill(Color.WHITE);

        // Verbesserter Stil für Pop-up-Nachrichten
        waveMessageLabel.setStyle(
                "-fx-background-color: rgba(10, 20, 50, 0.85); " +  // Dunkelblau, leicht transparent
                        "-fx-padding: 15px 20px; " +                         // Etwas mehr horizontales Padding
                        "-fx-border-color: #00FFFF; " +                      // Helles Cyan (Aqua) für den Rand
                        "-fx-border-width: 2.5px; " +                        // Etwas dickerer Rand
                        "-fx-border-radius: 3px; " +                         // Leicht abgerundete Ecken für den Rand
                        "-fx-background-radius: 4px; " +                       // Leicht abgerundete Ecken für den Hintergrund
                        "-fx-effect: dropshadow(gaussian, rgba(0,220,220,0.4), 12, 0.1, 0, 0);" // Subtiler Cyan-Glow
        );

        waveMessageLabel.setTextAlignment(TextAlignment.CENTER);
        waveMessageLabel.setWrapText(true);
        waveMessageLabel.setVisible(false);
        uiPane.getChildren().add(waveMessageLabel);
    }

    public void showPopupMessage(String message, double durationSeconds) {
        if (waveMessageLabel == null) createWaveMessageLabel();

        waveMessageLabel.setText(message);
        waveMessageLabel.setMaxWidth(gameDimensions.getWidth() * 0.75); // Max 75% der Bildschirmbreite

        waveMessageLabel.applyCss();
        waveMessageLabel.layout();

        double preferredWidth = waveMessageLabel.prefWidth(-1);
        double preferredHeight = waveMessageLabel.prefHeight(preferredWidth);

        // Sicherstellen, dass das Label nicht breiter als seine MaxWidth wird, auch für die Positionierung
        preferredWidth = Math.min(preferredWidth, waveMessageLabel.getMaxWidth());

        waveMessageLabel.setOpacity(0);
        waveMessageLabel.setVisible(true);

        waveMessageLabel.setLayoutX((gameDimensions.getWidth() - preferredWidth) / 2);
        waveMessageLabel.setLayoutY((gameDimensions.getHeight() - preferredHeight) / 3);


        SequentialTransition sequence = getSequentialTransition(durationSeconds);
        sequence.play();
    }

    @NotNull
    private SequentialTransition getSequentialTransition(double durationSeconds) {
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.4), waveMessageLabel); // Etwas schnelleres Einblenden
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition displayPause = new PauseTransition(Duration.seconds(durationSeconds));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.4), waveMessageLabel); // Etwas schnelleres Ausblenden
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> waveMessageLabel.setVisible(false));

        return new SequentialTransition(fadeIn, displayPause, fadeOut);
    }

    public void showWaveStartMessage(int waveNumber) {
        showPopupMessage("Wave " + waveNumber + "\nstarted", 1.5);
    }

    public void showWaveClearMessage(int nextWaveNumber) {
        showPopupMessage("Wave Cleared!\nGet Ready for Wave\n" + nextWaveNumber, 2.5);
    }

    public void showBossSpawnMessage() {
        showPopupMessage("!!!\nBOSS\nINCOMING\n!!!", 2.5);
    }

    public void showBossDefeatedMessage() {
        showPopupMessage("BOSS\nDEFEATED\nYOU WIN!", 3);
    }

    public void createScoreLabel() {
        if (scoreLabel != null && uiPane.getChildren().contains(scoreLabel)) {
            uiPane.getChildren().remove(scoreLabel);
        }
        scoreLabel = new Label("Score: 0");
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font(GAME_FONT_NAME, gameDimensions.getHeight() * (20.0 / 600.0)));
        // Optional: Dem Score-Label einen leichten Schatten für bessere Lesbarkeit geben
        scoreLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 1, 0, 0, 1);");
        scoreLabel.setLayoutX(gameDimensions.getWidth() * (15.0/800.0));
        scoreLabel.setLayoutY(gameDimensions.getHeight() * (10.0/600.0));
        uiPane.getChildren().add(scoreLabel);
    }

    public void updateScoreDisplay() {
        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + currentScore);
        }
    }

    public void addScore(int points) {
        this.currentScore += points;
        updateScoreDisplay();
    }
    public void resetScore() {
        this.currentScore = 0;
        if (scoreLabel == null) {
            createScoreLabel();
        } else {
            updateScoreDisplay();
        }
    }

    public int getCurrentScore() {
        return currentScore;
    }
}