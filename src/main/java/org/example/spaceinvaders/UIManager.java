package org.example.spaceinvaders;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
// import javafx.beans.property.SimpleObjectProperty; // Nicht mehr direkt verwendet für scoreLabel
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull; // Beibehalten, falls du es für andere Dinge nutzt

public class UIManager {
    private Pane uiPane; // Sollte jetzt bei jedem Spielstart neu zugewiesen werden oder UIManager neu erstellt
    private GameDimensions gameDimensions;
    private Label waveMessageLabel;
    // private MusicalInvaders mainApp; // Nicht mehr direkt für Sounds benötigt
    private Label scoreLabel; // Direkte Referenz statt Property
    private int currentScore = 0;


    public UIManager(Pane uiPane, GameDimensions gameDimensions, MusicalInvaders mainApp) {
        this.uiPane = uiPane;
        this.gameDimensions = gameDimensions;
        // this.mainApp = mainApp; // Nicht mehr zwingend hier nötig, wenn keine direkten Aufrufe an mainApp erfolgen
        createWaveMessageLabel();
        // Score Label wird jetzt in createScoreLabel erstellt, da uiPane neu sein kann
    }

    // Optional: Setter, falls UIManager nicht bei jedem Spiel neu erstellt wird, sondern nur das uiPane aktualisiert
    public void setUiPane(Pane newUiPane) {
        // Entferne alte UI-Elemente vom alten Pane, falls nötig
        if (this.uiPane != null) {
            if (waveMessageLabel != null) this.uiPane.getChildren().remove(waveMessageLabel);
            if (scoreLabel != null) this.uiPane.getChildren().remove(scoreLabel);
        }
        this.uiPane = newUiPane;
        // Erstelle UI-Elemente auf dem neuen Pane neu
        createWaveMessageLabel();
        createScoreLabel(); // Stellt sicher, dass Score auf dem neuen Pane ist und zurückgesetzt wird
        resetScore(); // Score auch logisch zurücksetzen
    }


    public void createWaveMessageLabel() {
        if (waveMessageLabel != null && uiPane.getChildren().contains(waveMessageLabel)) {
            uiPane.getChildren().remove(waveMessageLabel); // Entferne altes Label, falls vorhanden
        }
        waveMessageLabel = new Label();
        waveMessageLabel.setFont(Font.font("Verdana", gameDimensions.getHeight()*0.05));
        waveMessageLabel.setTextFill(Color.WHITE);
        waveMessageLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-padding: 10px; -fx-border-color: lightgreen; -fx-border-width: 2px;");
        waveMessageLabel.setTextAlignment(TextAlignment.CENTER);
        waveMessageLabel.setVisible(false);
        uiPane.getChildren().add(waveMessageLabel);
    }

    public void showPopupMessage(String message, double durationSeconds) {
        if (waveMessageLabel == null) createWaveMessageLabel(); // Sicherstellen, dass es existiert

        waveMessageLabel.setText(message);
        // Autosize ist gut, aber wir müssen es manuell machen, wenn das Label nicht in der Szene ist,
        // um seine Breite für die Positionierung zu bekommen. Besser: nach dem Sichtbarmachen positionieren.
        // waveMessageLabel.autosize();

        waveMessageLabel.setOpacity(0);
        waveMessageLabel.setVisible(true); // Erst sichtbar machen, dann positionieren, damit Größe bekannt ist

        // Positionierung nachdem Text gesetzt und Label potenziell Größe geändert hat
        // Um die korrekte Breite zu bekommen, muss das Label ggf. kurz in der Szene sein oder man erzwingt ein Layout-Pass
        // Für Popups ist es oft einfacher, eine feste Breite zu geben oder sie nach dem Einblenden zu zentrieren.
        // Hier vereinfacht:
        waveMessageLabel.layout(); // Erzwingt Neuberechnung der Größe
        waveMessageLabel.setLayoutX((gameDimensions.getWidth() - waveMessageLabel.prefWidth(-1)) / 2);
        waveMessageLabel.setLayoutY((gameDimensions.getHeight() - waveMessageLabel.prefHeight(-1)) / 3);


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

        return new SequentialTransition(fadeIn, displayPause, fadeOut);
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
        if (scoreLabel != null && uiPane.getChildren().contains(scoreLabel)) {
            uiPane.getChildren().remove(scoreLabel); // Entferne altes Label
        }
        scoreLabel = new Label("Score: 0"); // Startet immer bei 0
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(new Font("Consolas", gameDimensions.getHeight() * (20.0 / 600.0)));
        scoreLabel.setLayoutX(gameDimensions.getWidth() * (15.0/800.0));
        scoreLabel.setLayoutY(gameDimensions.getHeight() * (10.0/600.0));
        uiPane.getChildren().add(scoreLabel);
        // currentScore wird in resetScore() oder beim Hinzufügen von Punkten aktualisiert.
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
        if (scoreLabel == null) { // Falls Label noch nicht erstellt wurde (z.B. bei allererstem Start)
            createScoreLabel(); // Erstellt das Label und zeigt Score 0
        } else {
            updateScoreDisplay(); // Aktualisiert das existierende Label
        }
    }

    public int getCurrentScore() {
        return currentScore;
    }
}