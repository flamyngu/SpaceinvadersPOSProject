package org.example.spaceinvaders;

import javafx.beans.property.SimpleObjectProperty; // Beibehalten, falls du es spezifisch brauchst
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class UIManager {
    private Pane uiPane;
    private GameDimensions gameDimensions;

    private SimpleObjectProperty<Label> scoreLabelProperty = new SimpleObjectProperty<>(); // Beibehalten
    private int currentScore = 0;


    public UIManager(Pane uiPane, GameDimensions gameDimensions) {
        this.uiPane = uiPane;
        this.gameDimensions = gameDimensions;
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

    public int getCurrentScore() {
        return currentScore;
    }
}