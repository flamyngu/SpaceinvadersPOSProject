package org.example.spaceinvaders;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
//import javafx.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Musicalinvaders extends Application {

    // --- Konstanten ---
    private static final double WINDOW_WIDTH = 800;
    private static final double WINDOW_HEIGHT = 1070;
    private static final double PLAYER_WIDTH = 60; // Platzhalter-Breite
    private static final double PLAYER_HEIGHT = 30; // Platzhalter-Höhe
    private static final double PLAYER_SPEED = 5.0;
    private static final double ENEMY_WIDTH = 40; // Platzhalter-Breite
    private static final double ENEMY_HEIGHT = 40; // Platzhalter-Höhe
    private static final int ENEMIES_PER_ROW = 10;
    private static final int ENEMY_ROWS = 4;
    private static final double ENEMY_SPACING_X = 15;
    private static final double ENEMY_SPACING_Y = 10;
    private static final double PROJECTILE_WIDTH = 5;
    private static final double PROJECTILE_HEIGHT = 15;
    private static final double PROJECTILE_SPEED = 8.0;
    private static final long SHOOT_COOLDOWN_MS = 300; // Millisekunden zwischen Schüssen
    private static final int POINTS_PER_ENEMY = 10;
    // --- Spielzustand ---
    private Pane root;
    private Rectangle player; // Platzhalter für dein Spieler-Design (ImageView)
    private List<Rectangle> enemies = new ArrayList<>(); // Platzhalter für Gegner-Designs (ImageViews)
    private List<Rectangle> playerProjectiles = new ArrayList<>();
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean shooting = false;
    private long lastShotTime = 0;
    private int score = 0;
    private SimpleObjectProperty<Label> scoreLabel = new SimpleObjectProperty<Label>(this, "scoreLabel");

    private AnimationTimer gameLoop;

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        // Optional: Hintergrund setzen (später vielleicht ein Notenblatt?)
        root.setStyle("-fx-background-color: #1a1a1a;"); // Dunkelgrau

        Scene scene = new Scene(root);

        setupInput(scene);
        createPlayer();
        createEnemies();
        createScoreLabel();
        startGameLoop();

        primaryStage.setTitle("Musical Invaders");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // --- Initialisierung ---

    private void setupInput(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
                moveLeft = true;
            } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
                moveRight = true;
            } else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.UP) {
                shooting = true;
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
                moveLeft = false;
            } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
                moveRight = false;
            } else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.UP) {
                shooting = false;
            }
        });
    }

    private void createPlayer() {
        // --- HIER DEIN SPIELER-DESIGN EINFÜGEN ---
        // Ersetze Rectangle durch ImageView mit deinem Bild
        // Beispiel: Image playerImage = new Image("path/to/your/player_ship.png");
        //           ImageView playerView = new ImageView(playerImage);
        //           player = playerView; // Wenn player vom Typ Node/ImageView ist

        player = new Rectangle(PLAYER_WIDTH, PLAYER_HEIGHT);
        player.setFill(Color.CYAN); // Platzhalter-Farbe (z.B. ein heller Ton)
        // Positioniere den Spieler unten mittig
        player.setX(WINDOW_WIDTH / 2 - PLAYER_WIDTH / 2);
        player.setY(WINDOW_HEIGHT - PLAYER_HEIGHT - 20);
        root.getChildren().add(player);
    }

    private void createEnemies() {
        enemies.clear(); // Sicherstellen, dass die Liste leer ist
        double startX = (WINDOW_WIDTH - (ENEMIES_PER_ROW * (ENEMY_WIDTH + ENEMY_SPACING_X) - ENEMY_SPACING_X)) / 2;
        double startY = 50;

        for (int row = 0; row < ENEMY_ROWS; row++) {
            for (int col = 0; col < ENEMIES_PER_ROW; col++) {
                // --- HIER DEINE GEGNER-DESIGNS EINFÜGEN ---
                // Ersetze Rectangle durch ImageView mit deinem Gegner-Bild
                // Beispiel: Image enemyImage = new Image("path/to/your/enemy_note.png");
                //           ImageView enemyView = new ImageView(enemyImage);
                //           // Setze Position für enemyView
                //           enemies.add(enemyView); // Wenn Liste Nodes/ImageViews hält
                //           root.getChildren().add(enemyView);

                Rectangle enemy = new Rectangle(ENEMY_WIDTH, ENEMY_HEIGHT);
                // Vielleicht verschiedene Farben für Reihen? Oder alle schwarz wie Noten?
                enemy.setFill(Color.WHITE); // Platzhalter-Farbe (z.B. wie Notenköpfe)

                double x = startX + col * (ENEMY_WIDTH + ENEMY_SPACING_X);
                double y = startY + row * (ENEMY_HEIGHT + ENEMY_SPACING_Y);
                enemy.setX(x);
                enemy.setY(y);

                enemies.add(enemy);
                root.getChildren().add(enemy);
            }
        }
    }
    private void createBossEnemy(){

    }
    private void createScoreLabel(){
        scoreLabel.set(new Label("Score: 0"));
        scoreLabel.get().setTextFill(Color.WHITE);
        scoreLabel.get().setFont(new Font("Consolas", 20));
        scoreLabel.get().setLayoutX(15);
        scoreLabel.get().setLayoutY(10);
        root.getChildren().add(scoreLabel.get());
    }

    // --- Game Loop ---

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Update Logik
                updatePlayer(now);
                handleShooting(now);
                updateProjectiles();
                // updateEnemies(); // TODO: Gegnerbewegung hinzufügen (z.B. im Takt?)
                checkCollisions();
                if(enemies.isEmpty()){
                    //creates a little pause between the current and next wave of enemies
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(event -> {
                        createEnemies();
                    });
                    pause.play();
                }
                // TODO: Spielende prüfen (alle Gegner besiegt / Gegner erreichen Boden / Spieler getroffen)
            }
        };
        gameLoop.start();
    }

    // --- Update Methoden ---

    private void updatePlayer(long now) {
        double dx = 0;
        if (moveLeft && player.getX() > 0) {
            dx -= PLAYER_SPEED;
        }
        if (moveRight && player.getX() < WINDOW_WIDTH - PLAYER_WIDTH) {
            dx += PLAYER_SPEED;
        }
        player.setX(player.getX() + dx);
    }

    private void handleShooting(long now) {
        // Cooldown prüfen: Ist genug Zeit seit dem letzten Schuss vergangen?
        if (shooting && (now - lastShotTime) / 1_000_000 >= SHOOT_COOLDOWN_MS) {
            createProjectile();
            lastShotTime = now;
            // TODO: Schuss-Sound abspielen (z.B. ein kurzer Ton/Instrument)
        }
    }

    private void createProjectile() {
        // --- HIER DEIN PROJEKTIL-DESIGN EINFÜGEN ---
        // Ersetze Rectangle durch ImageView mit deinem Projektil-Bild (z.B. eine kleine Note, Schallwelle)

        Rectangle projectile = new Rectangle(PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        projectile.setFill(Color.YELLOW); // Platzhalter-Farbe
        projectile.setX(player.getX() + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2);
        projectile.setY(player.getY() - PROJECTILE_HEIGHT);

        playerProjectiles.add(projectile);
        root.getChildren().add(projectile); // Wichtig: Zum Pane hinzufügen
    }

    private void updateProjectiles() {
        // Iterator verwenden, um sicher Elemente während der Iteration zu entfernen
        Iterator<Rectangle> iterator = playerProjectiles.iterator();
        while (iterator.hasNext()) {
            Rectangle p = iterator.next();
            p.setY(p.getY() - PROJECTILE_SPEED);

            // Wenn Projektil aus dem Bild fliegt, entfernen
            if (p.getY() + PROJECTILE_HEIGHT < 0) {
                iterator.remove(); // Aus der Liste entfernen
                root.getChildren().remove(p); // Aus der Anzeige entfernen
            }
        }
    }

    // --- Kollisionserkennung ---

    private void checkCollisions() {
        Iterator<Rectangle> projIterator = playerProjectiles.iterator();
        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            Iterator<Rectangle> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Rectangle enemy = enemyIterator.next();

                // Einfache Bounding-Box Kollision
                if (projectile.getBoundsInParent().intersects(enemy.getBoundsInParent())) {
                    // Kollision!
                    projIterator.remove(); // Projektil entfernen
                    enemyIterator.remove(); // Gegner entfernen
                    root.getChildren().remove(projectile); // Projektil aus Anzeige entfernen
                    root.getChildren().remove(enemy); // Gegner aus Anzeige entfernen
                    score += POINTS_PER_ENEMY;
                    updateScoreLabel();

                    // TODO: Treffer-Sound/Effekt abspielen (z.B. anderer Ton, Partikeleffekt)
                    // TODO: Score erhöhen

                    // Da das Projektil nur einen Gegner treffen kann, können wir die innere Schleife verlassen
                    break;
                }
            }
        }
    }
    private void updateScoreLabel(){
        scoreLabel.get().setText(STR."Score: \{score}");
    }

    // --- Main Methode ---
    public static void main(String[] args) {
        launch(args);
    }
}