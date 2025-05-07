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
    private UIManager uiManager;// Für Score-Updates bei Bedarf

    private Enemy bossEnemy = null;
    private boolean bossActive = false;
    private int currentWaveNumber = 0;
    private boolean isLoadingNextWave = false;
    private boolean bossHasSpawnedThisGameCycle = false;
    private boolean bossWasJustDefeated = false;

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
        if(bossActive){isLoadingNextWave = false;return;};
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
        isLoadingNextWave = false;
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

    public void createBoss() {
        if(bossActive){isLoadingNextWave = false; return;}; //Boss ist bereits vorhanden
        //Alte Gegner aus dem Array werfen
        for(Enemy oldEnemy : new ArrayList<>(enemies)) {
            gamePane.getChildren().remove(oldEnemy.getNode());
            enemies.remove(oldEnemy);
        }
        enemies.clear();

        double bossWidth = gameDimensions.getEnemyWidth()*GameDimensions.BOSS_WIDTH_MULTIPLIER;
        double bossHeight = gameDimensions.getEnemyHeight()*GameDimensions.BOSS_HEIGHT_MULTIPLIER;

        Rectangle bossShape = new Rectangle(bossWidth, bossHeight);
        bossShape.setFill(Color.DARKRED);
        bossShape.setX(gameDimensions.getWidth()/2 - bossWidth/2);
        bossShape.setY(gameDimensions.getHeight()*0.1);

        this.bossEnemy = new Enemy(bossShape,GameDimensions.BOSS_HEALTH, GameDimensions.BOSS_POINTS);
        this.bossActive = true;
        gamePane.getChildren().add(bossShape);
        uiManager.showBossSpawnMessage();
        isLoadingNextWave = false;
    }

    public void spawnEnemyWaveInitial() {
        if (enemies.isEmpty() && !bossActive && !isLoadingNextWave) {
            isLoadingNextWave = true;
            currentWaveNumber = 1;
            createEnemies(); // Erste Welle sofort
            uiManager.showWaveStartMessage(currentWaveNumber);
        }
    }

    public void spawnNextWaveOrBoss() {
        if(isLoadingNextWave){return;}
        isLoadingNextWave = true;

        if(!bossWasJustDefeated){currentWaveNumber++;}
        bossWasJustDefeated = false;
        boolean spawnBossNow = false;

        if(currentWaveNumber == GameDimensions.WAVE_NUMBER_TO_SPAWN_BOSS && !bossHasSpawnedThisGameCycle){
            spawnBossNow = true;
        }
        if(spawnBossNow){
            PauseTransition bossPause = new PauseTransition(Duration.seconds(2));
            bossPause.setOnFinished(event -> {
                bossHasSpawnedThisGameCycle = true; //could cause problems
                createBoss();
            });
            bossPause.play();
        }else{
            uiManager.showWaveClearMessage(currentWaveNumber);
            PauseTransition wavePause = new PauseTransition(Duration.seconds(2));
            wavePause.setOnFinished(event -> {
                createEnemies();
            });
            wavePause.play();
        }
    }

    public int getCurrentWaveNumber(){
        return currentWaveNumber;
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

    public void bossDefeated() {
        if(bossEnemy != null) {
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        this.bossActive = false;
        this.bossEnemy = null;
        setBossAlreadySpawneThisCycle(true);
        bossWasJustDefeated = true;
        isLoadingNextWave = false;
    }
    public void setBossAlreadySpawneThisCycle(boolean status){
        this.bossHasSpawnedThisGameCycle = status;
    }


    // Getter
    public Player getPlayer() { return player; }
    public boolean bossAlreadySpawnedThisCycle(){return this.bossHasSpawnedThisGameCycle;}
    public List<Enemy> getEnemies() { return enemies; }
    public List<Rectangle> getPlayerProjectiles() { return playerProjectiles; }
    public boolean isBossActive() { return bossActive; }
    public Enemy getBossEnemy() { return bossEnemy;}
}