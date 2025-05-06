package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class GameEntityManager {
    private Pane gamePane;
    private GameDimensions gameDimensions;
    private UIManager uiManager; // Für Score-Updates bei Bedarf

    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Rectangle> playerProjectiles = new ArrayList<>(); // Vorerst Rectangles

    public GameEntityManager(Pane gamePane, GameDimensions gameDimensions, UIManager uiManager) {
        this.gamePane = gamePane;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
    }

    public void createPlayer() {
        this.player = new Player(gameDimensions);
        gamePane.getChildren().add(player.getNode());
    }

    public void createEnemies() {
        // Alte Gegner-Nodes entfernen
        for (Enemy oldEnemy : enemies) {
            if (oldEnemy.getNode() != null) {
                gamePane.getChildren().remove(oldEnemy.getNode());
            }
        }
        enemies.clear();

        double startX = (gameDimensions.getWidth() - (GameDimensions.ENEMIES_PER_ROW * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX()) - gameDimensions.getEnemySpacingX())) / 2;
        double startY = gameDimensions.getHeight() * (50.0 / 600.0); // Relativer StartY

        for (int row = 0; row < GameDimensions.ENEMY_ROWS; row++) {
            for (int col = 0; col < GameDimensions.ENEMIES_PER_ROW; col++) {
                Rectangle enemyShape = new Rectangle(gameDimensions.getEnemyWidth(), gameDimensions.getEnemyHeight());
                enemyShape.setFill(Color.rgb((row * 60) % 255, (col * 40) % 255, 180)); // Etwas andere Farben

                double x = startX + col * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX());
                double y = startY + row * (gameDimensions.getEnemyHeight() + gameDimensions.getEnemySpacingY());
                enemyShape.setX(x);
                enemyShape.setY(y);

                Enemy newLogicalEnemy = new Enemy(enemyShape, 1, GameDimensions.POINTS_PER_ENEMY);
                enemies.add(newLogicalEnemy);
                gamePane.getChildren().add(enemyShape);
            }
        }
    }

    public void createProjectile() {
        if (player == null) return;

        Rectangle projectile = new Rectangle(gameDimensions.getProjectileWidth(), gameDimensions.getProjectileHeight());
        projectile.setFill(Color.YELLOW);
        projectile.setX(player.getX() + player.getWidth() / 2 - gameDimensions.getProjectileWidth() / 2);
        projectile.setY(player.getY() - gameDimensions.getProjectileHeight()); // Direkt über dem Spieler

        playerProjectiles.add(projectile);
        gamePane.getChildren().add(projectile);
    }

    public void spawnEnemyWaveInitial() {
        if (enemies.isEmpty()) {
            createEnemies(); // Erste Welle sofort
        }
    }

    public void spawnEnemyWaveWithDelay() {
        if (enemies.isEmpty()) {
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> createEnemies());
            pause.play();
        }
    }


    // Methoden zum Entfernen (werden vom GameUpdater aufgerufen nach Kollision)
    public void removeEnemy(Enemy enemy) {
        if (enemy != null) {
            gamePane.getChildren().remove(enemy.getNode());
            // enemies.remove(enemy); // Das Entfernen aus der Liste passiert im Iterator im GameUpdater
        }
    }

    public void removeProjectileNode(Node projectileNode) {
        if (projectileNode != null) {
            gamePane.getChildren().remove(projectileNode);
            // playerProjectiles.remove(projectileNode); // Das Entfernen aus der Liste passiert im Iterator im GameUpdater
        }
    }


    // Getter
    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Rectangle> getPlayerProjectiles() { return playerProjectiles; }
}