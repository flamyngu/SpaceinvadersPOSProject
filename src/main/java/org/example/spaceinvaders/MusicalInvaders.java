package org.example.spaceinvaders;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MusicalInvaders extends Application {

    private GameDimensions gameDimensions;
    private Pane gamePane;
    private Pane uiPane;

    private GameEntityManager entityManager;
    private GameUpdater gameUpdater;
    private UIManager uiManager;
    private InputHandler inputHandler;

    private AnimationTimer gameLoop;

    @Override
    public void start(Stage primaryStage) {
        // Bildschirmauflösung und Fenstergrößen ermitteln
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        double windowHeight = bounds.getHeight();
        double windowWidth = windowHeight * (4.0 / 3.0);
        if (windowWidth > bounds.getWidth()) {
            windowWidth = bounds.getWidth();
            windowHeight = windowWidth * (3.0 / 4.0);
        }
        this.gameDimensions = new GameDimensions(windowWidth, windowHeight);

        // Panes initialisieren
        gamePane = new Pane();
        gamePane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        gamePane.setStyle("-fx-background-color: #1a1a1a;");

        uiPane = new Pane();
        uiPane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        uiPane.setMouseTransparent(true); // Wichtig, damit Klicks zum gamePane durchgehen

        StackPane rootPane = new StackPane(gamePane, uiPane);
        Scene scene = new Scene(rootPane, gameDimensions.getWidth(), gameDimensions.getHeight());

        // Manager-Klassen instanziieren
        inputHandler = new InputHandler(scene);
        // Die Reihenfolge der Instanziierung kann wichtig sein, je nach Abhängigkeiten
        uiManager = new UIManager(uiPane, gameDimensions);
        entityManager = new GameEntityManager(gamePane, gameDimensions, uiManager); // entityManager könnte uiManager für Score-Updates brauchen
        gameUpdater = new GameUpdater(entityManager, inputHandler, gameDimensions, uiManager);


        // Initiale Spielobjekte und UI erstellen
        entityManager.createPlayer();
        entityManager.spawnEnemyWaveInitial(); // Um die erste Welle ohne Pause zu starten
        uiManager.createScoreLabel(); // Score Label erstellen und initialisieren
        // Der Score wird jetzt im UIManager verwaltet

        // Game Loop starten
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0; // Für delta time, falls benötigt

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) { // Erster Frame
                    lastUpdate = now;
                    return;
                }
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // Delta in Sekunden
                lastUpdate = now;

                // Spielzustand aktualisieren
                gameUpdater.update(now, deltaTime); // Übergabe von 'now' für Cooldowns und deltaTime für Physik
            }
        };
        gameLoop.start();

        primaryStage.setTitle("Musical Invaders - Modular");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}