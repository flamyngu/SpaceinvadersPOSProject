package org.example.spaceinvaders;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
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
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

public class Musicalinvaders extends Application {

    private static final double ENEMY_HEIGHT_RATIO = 40.0/800.0;
    // --- Konstanten ---
    private static double WINDOW_WIDTH;
    private static double WINDOW_HEIGHT;
    private static final double PLAYER_WIDTH_RATIO = 60.0 / 800.0;
    private static final double PLAYER_HEIGHT_RATIO = 30.0 / 600.0;
    private static final double ENEMY_WIDTH_RATIO = 40.0 / 800.0;
    private static double PLAYER_WIDTH = 60; // Platzhalter-Breite
    private static double PLAYER_HEIGHT = 30; // Platzhalter-Höhe
    private static double PLAYER_SPEED = 5.0;
    private static double ENEMY_WIDTH = 40; // Platzhalter-Breite
    private static double ENEMY_HEIGHT = 40; // Platzhalter-Höhe
    private static final int ENEMIES_PER_ROW = 10;
    private static final int ENEMY_ROWS = 4;
    private double ENEMY_SPACING_X_RATIO = 15.0 / 800.0;
    private double ENEMY_SPACING_Y_RATIO = 10.0 / 600.0;
    private static double ENEMY_SPACING_X = 15;
    private static double ENEMY_SPACING_Y = 10;
    private static final double PROJECTILE_WIDTH_RATIO = 5.0 / 800.0;
    private static final double PROJECTILE_HEIGHT_RATIO = 15.0 / 600.0;
    private static double PROJECTILE_WIDTH = 5;
    private static double PROJECTILE_HEIGHT = 15;
    private static double PROJECTILE_SPEED = 8.0;
    private static final long SHOOT_COOLDOWN_MS = 300; // Millisekunden zwischen Schüssen
    private static final int POINTS_PER_ENEMY = 10;
    // --- Spielzustand ---
    private Pane root;
    private Rectangle player; // Platzhalter für dein Spieler-Design (ImageView)
    private List<Enemy> enemies = new ArrayList<Enemy>(); // Platzhalter für Gegner-Designs (ImageViews)
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
        // --- Bildschirmauflösung ermitteln ---
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds(); // Verwende VisualBounds, um Taskleiste etc. zu berücksichtigen

        // Setze die Fenstergröße auf die volle verfügbare Höhe und eine proportionale Breite,
        // oder eine feste Breite und volle Höhe, oder volle Breite und volle Höhe.
        // Beispiel 1: Volle Höhe, proportionale Breite (angenommenes Seitenverhältnis 4:3 für das Spiel)
         WINDOW_HEIGHT = bounds.getHeight();
         WINDOW_WIDTH = WINDOW_HEIGHT * (4.0 / 3.0);
         // Stelle sicher, dass die Breite nicht die Bildschirmbreite überschreitet
         if (WINDOW_WIDTH > bounds.getWidth()) {
            WINDOW_WIDTH = bounds.getWidth();
            WINDOW_HEIGHT = WINDOW_WIDTH * (3.0 / 4.0); // Höhe neu anpassen
         }

        // Beispiel 2: Volle verfügbare Breite und Höhe (Vollbild-ähnlich, aber nicht exklusiver Vollbildmodus)
//        WINDOW_WIDTH = bounds.getWidth();
//        WINDOW_HEIGHT = bounds.getHeight();

        // Beispiel 3: Feste Breite, volle Höhe (wenn die Breite wichtiger ist)
        // WINDOW_WIDTH = 800; // Oder eine andere feste Breite
        // WINDOW_HEIGHT = bounds.getHeight();


        // --- Dynamische Größen für Spielelemente berechnen ---
        // Dies ist wichtig, damit dein Spiel auf verschiedenen Auflösungen gut aussieht.
        PLAYER_WIDTH = WINDOW_WIDTH * PLAYER_WIDTH_RATIO;
        PLAYER_HEIGHT = WINDOW_HEIGHT * PLAYER_HEIGHT_RATIO;
        ENEMY_WIDTH = WINDOW_WIDTH * ENEMY_WIDTH_RATIO;
        ENEMY_HEIGHT = WINDOW_HEIGHT * ENEMY_HEIGHT_RATIO; // Angenommen, du hast auch ENEMY_HEIGHT_RATIO
        ENEMY_SPACING_X = WINDOW_WIDTH * ENEMY_SPACING_X_RATIO;
        ENEMY_SPACING_Y = WINDOW_HEIGHT * ENEMY_SPACING_Y_RATIO; // Angenommen, du hast auch ENEMY_SPACING_Y_RATIO
        PROJECTILE_WIDTH = WINDOW_WIDTH * PROJECTILE_WIDTH_RATIO;
        PROJECTILE_HEIGHT = WINDOW_HEIGHT * PROJECTILE_HEIGHT_RATIO;

        // Geschwindigkeiten könnten auch angepasst werden, aber das ist optional
        PLAYER_SPEED = WINDOW_WIDTH * (5.0 / 800.0); // Basisgeschwindigkeit relativ zur ursprünglichen Breite
        PROJECTILE_SPEED = WINDOW_HEIGHT * (8.0 / 600.0); // Basisgeschwindigkeit relativ zur ursprünglichen Höhe


        // --- Restlicher Aufbau ---
        root = new Pane();
        // Wichtig: Setze die bevorzugte Größe des Panes auf die dynamische Fenstergröße
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Die Scene wird jetzt mit der dynamischen Größe erstellt
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        // Alternativ, wenn root.setPrefSize() schon gesetzt wurde:
        // Scene scene = new Scene(root);

        setupInput(scene);
        createPlayer();
        createEnemies(); // Diese Methode muss jetzt die dynamischen Größen verwenden
        createScoreLabel();
        startGameLoop();

        primaryStage.setTitle("Musical Invaders - Dynamische Auflösung");
        primaryStage.setScene(scene);
        // Optional: Vollbild setzen (echter Vollbildmodus)
        // primaryStage.setFullScreen(true);
        // primaryStage.setFullScreenExitHint(""); // Optional: Vollbild-Exit-Hinweis ausblenden
        primaryStage.setResizable(false); // Verhindert, dass der Benutzer die Größe manuell ändert, wenn du nicht auf Größenänderungen reagierst
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
        // 1. Vorhandene visuelle Gegner-Nodes aus der Szene entfernen,
        //    bevor die Logik-Liste geleert wird.
        for (Enemy oldEnemy : enemies) { // Iteriere über die bestehenden Enemy-Objekte
            if (oldEnemy.getNode() != null) { // Sicherheitscheck
                root.getChildren().remove(oldEnemy.getNode());
            }
        }
        enemies.clear(); // Leere die Liste der Enemy-Logik-Objekte

        // Berechnung der Startposition (bleibt gleich)
        double startX = (WINDOW_WIDTH - (ENEMIES_PER_ROW * (ENEMY_WIDTH + ENEMY_SPACING_X) - ENEMY_SPACING_X)) / 2;
        double startY = 50;

        for (int row = 0; row < ENEMY_ROWS; row++) {
            for (int col = 0; col < ENEMIES_PER_ROW; col++) {
                // --- HIER DEIN EIGENTLICHES GEGNER-DESIGN ERSTELLEN (VISUELLE NODE) ---
                // Ersetze Rectangle durch ImageView mit deinem Gegner-Bild, wenn du soweit bist.
                // Für jetzt bleiben wir bei Rectangle als Platzhalter für die Node.

                Rectangle enemyShape = new Rectangle(ENEMY_WIDTH, ENEMY_HEIGHT);
                // TODO: Setze hier dein gewünschtes Design für die enemyShape
                // z.B. enemyShape.setFill(Color.BLUE); oder lade ein Bild für eine ImageView

                // Gib der enemyShape eine spezifische Farbe für diese Demonstration,
                // damit du siehst, dass neue Gegner erstellt werden.
                // Du kannst später komplexere Designs/Bilder verwenden.
                enemyShape.setFill(Color.rgb( (row * 50) % 255, (col * 30) % 255, 150)); // Beispiel-Farbe


                // Positioniere die visuelle Node (enemyShape)
                double x = startX + col * (ENEMY_WIDTH + ENEMY_SPACING_X);
                double y = startY + row * (ENEMY_HEIGHT + ENEMY_SPACING_Y);
                enemyShape.setX(x);
                enemyShape.setY(y);

                // --- Erstelle das Enemy-LOGIK-Objekt ---
                // Jeder normale Gegner hat hier z.B. 1 Leben und gibt POINTS_PER_ENEMY Punkte.
                Enemy newLogicalEnemy = new Enemy(enemyShape, 1, POINTS_PER_ENEMY);

                // --- Füge das Logik-Objekt zur enemies-Liste hinzu ---
                enemies.add(newLogicalEnemy);

                // --- Füge die visuelle Node (enemyShape) zur Szene (root Pane) hinzu ---
                root.getChildren().add(enemyShape);
            }
        }
        // Optional: Debug-Ausgabe

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
//                if(enemies.isEmpty()){ //Spawns invincible Enemy's
//                    //creates a little pause between the current and next wave of enemies
//                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
//                    pause.setOnFinished(event -> {
//                        createEnemies();
//                    });
//                    pause.play();
//                }
                spawnEnemyWave();
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
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();

                // Einfache Bounding-Box Kollision
                if (projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                    // Kollision!
                    projIterator.remove(); // Projektil entfernen
                     // Gegner entfernen
                    root.getChildren().remove(projectile); // Projektil aus Anzeige entfernen
                    enemy.takeHit();
                    if(!enemy.isAlive()){
                        enemyIterator.remove();
                        root.getChildren().remove(enemy.getNode());
                        score+= enemy.getPoints();
                        updateScoreLabel();
                    }
                     // Gegner aus Anzeige entfernen


                    // TODO: Treffer-Sound/Effekt abspielen (z.B. anderer Ton, Partikeleffekt)
                    // TODO: Score erhöhen

                    // Da das Projektil nur einen Gegner treffen kann, können wir die innere Schleife verlassen
                    break;
                }
            }
        }
    }
    public class Enemy {
        private Node node;
        private int health;
        private int points;

        public Enemy(Node node, int initialHealth, int points) {
            this.node = node;
            this.health = initialHealth;
            this.points = points;
        }

        public Node getNode() { return node; }
        public int getHealth() { return health; }
        public int getPoints() { return points; }
        public boolean isAlive() { return health > 0; }

        public void takeHit() {
            if (health > 0) {
                health--;
                // Optional: Visuelles Feedback für Treffer hier implementieren
                // z.B. node.setOpacity(0.5); new PauseTransition(Duration.millis(50)).setOnFinished(e -> node.setOpacity(1.0)).play();
            }
        }
    }
    public void spawnEnemyWave(){
        int waveCount = 0;
        if(enemies.isEmpty()){
                   //creates a little pause between the current and next wave of enemies
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(event -> {

                    createEnemies();
                });
            pause.play();
        }
    }
    private void updateScoreLabel(){
        scoreLabel.get().setText("Score: " + score);
    }

    // --- Main Methode ---
    public static void main(String[] args) {
        launch(args);
    }
}