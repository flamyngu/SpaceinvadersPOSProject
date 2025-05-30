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
    private UIManager uiManager;
    private BossController bossController;

    private Enemy bossEnemy = null;
    private boolean bossActive = false;
    private int currentWaveNumber = 0;
    private boolean isLoadingNextWave = false;
    private boolean bossHasSpawnedThisGameCycle = false;
    private boolean bossWasJustDefeated = false; // True when final boss phase is cleared
    private double enemyMovementDirection = 1.0;
    private double enemyGroupSpeedX;
    private double enemyGroupSpeedY;
    private boolean moveDownNextCycle = false;

    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Rectangle> playerProjectiles = new ArrayList<>();

    public GameEntityManager(Pane gamePane, GameDimensions gameDimensions, UIManager uiManager) {
        this.gamePane = gamePane;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;

        this.enemyGroupSpeedX = this.gameDimensions.getWidth() * 0.002;
        if(this.enemyGroupSpeedX < 1.0) this.enemyGroupSpeedX = 1.0;
        this.enemyGroupSpeedY = this.gameDimensions.getEnemyHeight() * 0.5;

        this.bossController = new BossController(this, gameDimensions, uiManager);
    }

    public void createPlayer() {
        this.player = new Player(gameDimensions);
        gamePane.getChildren().add(player.getNode());
    }

    public void createEnemies() {
        if (bossActive) {
            System.out.println("GameEntityManager.createEnemies(): Boss is active, not creating normal enemies.");
            // If createEnemies is called and boss is active, it implies a logic flow issue elsewhere,
            // as the calling method should ideally prevent this.
            // isLoadingNextWave will be handled by the caller in such a scenario.
            return;
        }

        // Clear existing enemies - iterate over a copy to avoid ConcurrentModificationException
        for (Enemy oldEnemy : new ArrayList<>(enemies)) {
            if (oldEnemy.getNode() != null) {
                gamePane.getChildren().remove(oldEnemy.getNode());
            }
        }
        enemies.clear();

        double startX = (gameDimensions.getWidth() - (GameDimensions.ENEMIES_PER_ROW * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX()) - gameDimensions.getEnemySpacingX())) / 2;
        double startY = gameDimensions.getHeight() * (50.0 / 600.0);

        for (int row = 0; row < GameDimensions.ENEMY_ROWS; row++) {
            for (int col = 0; col < GameDimensions.ENEMIES_PER_ROW; col++) {
                Rectangle enemyShape = new Rectangle(gameDimensions.getEnemyWidth(), gameDimensions.getEnemyHeight());
                enemyShape.setFill(Color.rgb((row * 60) % 255, (col * 40) % 255, 180));
                double x = startX + col * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX());
                double y = startY + row * (gameDimensions.getEnemyHeight() + gameDimensions.getEnemySpacingY());
                enemyShape.setLayoutX(x);
                enemyShape.setLayoutY(y);
                Enemy newLogicalEnemy = new Enemy(enemyShape, 1, GameDimensions.POINTS_PER_ENEMY);
                enemies.add(newLogicalEnemy);
                gamePane.getChildren().add(enemyShape);
            }
        }
        System.out.println("GameEntityManager.createEnemies(): Created " + enemies.size() + " enemies for wave " + currentWaveNumber);
        // isLoadingNextWave = false; // DO NOT set here. The calling PauseTransition or method will handle it.
    }

    public void createProjectile() {
        if (player == null) return;
        Rectangle projectile = new Rectangle(gameDimensions.getProjectileWidth(), gameDimensions.getProjectileHeight());
        projectile.setFill(Color.YELLOW);
        projectile.setX(player.getX() + player.getWidth() / 2 - gameDimensions.getProjectileWidth() / 2);
        projectile.setY(player.getY() - gameDimensions.getProjectileHeight());
        playerProjectiles.add(projectile);
        gamePane.getChildren().add(projectile);
    }

    public void createBoss() {
        if(bossActive){ // Already active, do nothing
            return;
        }
        for(Enemy oldEnemy : new ArrayList<>(enemies)) { // Clear normal enemies
            if(oldEnemy.getNode() != null) gamePane.getChildren().remove(oldEnemy.getNode());
        }
        enemies.clear();

        double bossWidth = gameDimensions.getEnemyWidth() * GameDimensions.BOSS_WIDTH_MULTIPLIER;
        double bossHeight = gameDimensions.getEnemyHeight() * GameDimensions.BOSS_HEIGHT_MULTIPLIER;
        Rectangle bossShape = new Rectangle(bossWidth, bossHeight);

        this.bossEnemy = new Enemy(bossShape, GameDimensions.BOSS_HEALTH, GameDimensions.BOSS_POINTS);
        this.bossActive = true;
        gamePane.getChildren().add(bossShape);
        uiManager.showBossSpawnMessage(); // This message has its own duration

        bossController.initializeBoss();

        isLoadingNextWave = false; // Boss creation sequence is complete
    }

    public void spawnEnemyWaveInitial() {
        if (enemies.isEmpty() && !bossActive && !isLoadingNextWave) {
            isLoadingNextWave = true; // Mark as loading
            currentWaveNumber = 1;
            createEnemies(); // Synchronous call
            uiManager.showWaveStartMessage(currentWaveNumber);
            isLoadingNextWave = false; // Done loading initial wave
        }
    }

    public void spawnNextWaveOrBoss() {
        if(isLoadingNextWave) { return; } // Guard: already processing a wave/boss spawn
        isLoadingNextWave = true;         // Mark as loading for this new spawn sequence

        if(!bossWasJustDefeated){ // Don't increment wave if boss was just beaten (game ends)
            currentWaveNumber++;
        } else {
            // If boss was just defeated, we shouldn't be spawning a new wave or boss
            isLoadingNextWave = false; // Reset flag and exit
            return;
        }

        boolean spawnBossNow = false;
        if(currentWaveNumber >= GameDimensions.WAVE_NUMBER_TO_SPAWN_BOSS && !bossHasSpawnedThisGameCycle){
            spawnBossNow = true;
        }

        if(spawnBossNow){
            // The UIManager.showBossSpawnMessage() is called within createBoss()
            // and has its own 2-second duration. The PauseTransition here is for the delay *before* boss creation.
            PauseTransition bossPause = new PauseTransition(Duration.seconds(2));
            bossPause.setOnFinished(event -> {
                bossHasSpawnedThisGameCycle = true;
                createBoss(); // createBoss() will set isLoadingNextWave = false upon completion
            });
            bossPause.play();
        }else{
            uiManager.showWaveClearMessage(currentWaveNumber); // Shows "Wave Cleared! Get Ready for Wave X" (2s duration)
            PauseTransition wavePause = new PauseTransition(Duration.seconds(2)); // Match UI message duration
            wavePause.setOnFinished(event -> {
                createEnemies();           // Create the enemies for the next wave
                isLoadingNextWave = false; // NOW it's safe to say we are no longer loading this wave.
            });
            wavePause.play();
        }
    }

    public int getCurrentWaveNumber(){
        return currentWaveNumber;
    }

    public void removeEnemy(Enemy enemy) {
        if (enemy != null) {
            if(enemy.getNode() != null) gamePane.getChildren().remove(enemy.getNode());
            enemies.remove(enemy);
        }
    }

    public void removeProjectileNode(Node projectileNode) {
        if (projectileNode != null) {
            gamePane.getChildren().remove(projectileNode);
        }
    }

    public void bossDefeated() {
        if(bossEnemy != null && bossEnemy.getNode() != null) {
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        this.bossActive = false;
        this.bossEnemy = null;
        setBossAlreadySpawnedThisCycle(true);
        this.bossWasJustDefeated = true;
        isLoadingNextWave = false; // No longer loading anything, game might end or transition
    }

    public void setBossAlreadySpawnedThisCycle(boolean status){
        this.bossHasSpawnedThisGameCycle = status;
    }

    public void resetGame(){
        for(Enemy enemy : new ArrayList<>(enemies)) { // Iterate over a copy
            if(enemy.getNode()!=null)gamePane.getChildren().remove(enemy.getNode());
        }
        enemies.clear();
        for(Rectangle projectile : new ArrayList<>(playerProjectiles)){ // Iterate over a copy
            if(projectile!=null)gamePane.getChildren().remove(projectile);
        }
        playerProjectiles.clear();

        if(bossEnemy!=null &&bossEnemy.getNode()!=null){
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        bossEnemy  = null;
        bossActive = false;
        bossWasJustDefeated = false;
        bossHasSpawnedThisGameCycle = false;

        currentWaveNumber = 0; // Reset wave number
        isLoadingNextWave = false; // Reset loading flag
        if(player != null && player.getNode() != null)gamePane.getChildren().remove(player.getNode());
        player = null;
        this.enemyMovementDirection = 1.0;
        this.moveDownNextCycle = false;

        if (bossController != null) {
            bossController.resetBoss();
        }
    }

    public void removeProjectile(Node projectileNode) {
        if (projectileNode != null) {
            gamePane.getChildren().remove(projectileNode);
            if (projectileNode instanceof Rectangle) {
                playerProjectiles.remove((Rectangle) projectileNode);
            }
        }
    }

    public void removeEnemyNode(Node enemyNode) {
        if(enemyNode != null){
            gamePane.getChildren().remove(enemyNode);
        }
    }

    public void resetBossHealth(int newHealth) {
        if (bossEnemy != null) {
            bossEnemy.setHealth(newHealth);
        }
    }

    public Player getPlayer() { return player; }
    public boolean bossAlreadySpawnedThisCycle(){return this.bossHasSpawnedThisGameCycle;}
    public List<Enemy> getEnemies() { return enemies; }
    public List<Rectangle> getPlayerProjectiles() { return playerProjectiles; }
    public boolean isBossActive() { return bossActive; }
    public boolean isLoadingNextWave() {return isLoadingNextWave;}
    public Enemy getBossEnemy() { return bossEnemy;}
    public double getEnemyMovementDirection(){return enemyMovementDirection;}
    public void setEnemyMovementDirection(double direction){this.enemyMovementDirection = direction;}
    public double getEnemyGroupSpeedX(){return enemyGroupSpeedX;}
    public double getEnemyGroupSpeedY() {return enemyGroupSpeedY;}
    public boolean shouldMoveDownNextCycle(){return moveDownNextCycle;}
    public void setMoveDownNextCycle(boolean moveDown){moveDownNextCycle = moveDown;}
    public BossController getBossController() { return bossController; }
    public Pane getGamePane() { return gamePane; }
    public boolean wasBossJustDefeated() { return bossWasJustDefeated; }
}