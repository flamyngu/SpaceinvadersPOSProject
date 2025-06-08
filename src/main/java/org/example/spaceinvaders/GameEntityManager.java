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
    private boolean bossWasJustDefeated = false;
    private double enemyMovementDirection = 1.0;
    private double enemyGroupSpeedX;
    private double enemyGroupSpeedY;
    private boolean moveDownNextCycle = false;

    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Rectangle> playerProjectiles = new ArrayList<>();

    public GameEntityManager(Pane gamePane, GameDimensions gameDimensions, UIManager uiManager, SoundManager soundManager) {
        this.gamePane = gamePane;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;

        this.enemyGroupSpeedX = this.gameDimensions.getWidth() * 0.002;
        if(this.enemyGroupSpeedX < 1.0) this.enemyGroupSpeedX = 1.0;
        this.enemyGroupSpeedY = this.gameDimensions.getEnemyHeight() * 0.5;

        this.bossController = new BossController(this, gameDimensions, uiManager, soundManager);
        resetInitialStateFlags();
        System.out.println("GameEntityManager: Konstruktor - gamePane Children: " + this.gamePane.getChildren().size());
    }

    private void resetInitialStateFlags() {
        this.bossEnemy = null;
        this.bossActive = false;
        this.currentWaveNumber = 0;
        this.isLoadingNextWave = false;
        this.bossHasSpawnedThisGameCycle = false;
        this.bossWasJustDefeated = false;
        this.enemyMovementDirection = 1.0;
        this.moveDownNextCycle = false;
        this.player = null;
    }


    public void createPlayer() {
        this.player = new Player(gameDimensions);
        if (!gamePane.getChildren().contains(player.getNode())) {
            gamePane.getChildren().add(player.getNode());
        } else {
            System.out.println("WARNUNG: Spieler-Node war bereits in gamePane in createPlayer!");
        }
        System.out.println("GameEntityManager.createPlayer(): Spieler zu gamePane hinzugefügt. gamePane Children: " + gamePane.getChildren().size());
    }

    public void createEnemies() {
        System.out.println("GameEntityManager.createEnemies(): Start. Aktuelle Gegner in Liste: " + enemies.size() + ", Kinder in gamePane: " + gamePane.getChildren().size());

        if (bossActive) {
            System.out.println("GameEntityManager.createEnemies(): Boss ist aktiv, keine normalen Gegner erstellt.");
            return;
        }

        for (Enemy oldEnemy : new ArrayList<>(enemies)) {
            if (oldEnemy.getNode() != null && gamePane.getChildren().contains(oldEnemy.getNode())) {
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
                // System.out.println("  Gegner erstellt: Reihe=" + row + ", Spalte=" + col + ", Y-Position=" + y +
                //                  ", Höhe=" + enemyShape.getBoundsInLocal().getHeight()); // Log von vorheriger Anfrage
                Enemy newLogicalEnemy = new Enemy(enemyShape, 1, GameDimensions.POINTS_PER_ENEMY);
                enemies.add(newLogicalEnemy);
                if (!gamePane.getChildren().contains(enemyShape)) {
                    gamePane.getChildren().add(enemyShape);
                } else {
                    System.out.println("WARNUNG: Gegner-Node war bereits in gamePane in createEnemies!");
                }
            }
        }
        System.out.println("GameEntityManager.createEnemies(): " + enemies.size() + " Gegner erstellt und zu gamePane hinzugefügt für Welle " + currentWaveNumber + ". gamePane Children: " + gamePane.getChildren().size());
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
        if(bossActive){
            return;
        }
        System.out.println("createBoss: Entferne normale Gegner. enemies.size()=" + enemies.size());
        for(Enemy oldEnemy : new ArrayList<>(enemies)) {
            if(oldEnemy.getNode() != null && gamePane.getChildren().contains(oldEnemy.getNode())) {
                gamePane.getChildren().remove(oldEnemy.getNode());
            }
        }
        enemies.clear();
        System.out.println("createBoss: Normale Gegner entfernt. enemies.size()=" + enemies.size());


        double bossWidth = gameDimensions.getEnemyWidth() * GameDimensions.BOSS_WIDTH_MULTIPLIER;
        double bossHeight = gameDimensions.getEnemyHeight() * GameDimensions.BOSS_HEIGHT_MULTIPLIER;
        Rectangle bossShape = new Rectangle(bossWidth, bossHeight);

        this.bossEnemy = new Enemy(bossShape, GameDimensions.BOSS_HEALTH, GameDimensions.BOSS_POINTS);
        this.bossActive = true;
        gamePane.getChildren().add(bossShape);
        uiManager.showBossSpawnMessage();

        bossController.initializeBoss();

        isLoadingNextWave = false;
        System.out.println("createBoss: Boss erstellt. gamePane Children: " + gamePane.getChildren().size());
    }

    public void spawnEnemyWaveInitial() {
        System.out.println("spawnEnemyWaveInitial: Start. enemies.isEmpty()=" + enemies.isEmpty() + ", bossActive=" + bossActive + ", isLoadingNextWave=" + isLoadingNextWave);
        if (enemies.isEmpty() && !bossActive && !isLoadingNextWave) {
            isLoadingNextWave = true;
            currentWaveNumber = 1;
            createEnemies();
            uiManager.showWaveStartMessage(currentWaveNumber);
            isLoadingNextWave = false;
        } else {
            System.out.println("spawnEnemyWaveInitial: Bedingungen nicht erfüllt, keine Welle gespawnt.");
            if (!enemies.isEmpty()) System.out.println("  Grund: enemies nicht leer, size=" + enemies.size());
            if (bossActive) System.out.println("  Grund: bossActive ist true");
            if (isLoadingNextWave) System.out.println("  Grund: isLoadingNextWave ist true");
        }
    }

    public void spawnNextWaveOrBoss() {
        if(isLoadingNextWave) { return; }
        isLoadingNextWave = true;

        if(!bossWasJustDefeated){
            currentWaveNumber++;
        } else {
            isLoadingNextWave = false;
            return;
        }

        boolean spawnBossNow = false;
        if(currentWaveNumber >= GameDimensions.WAVE_NUMBER_TO_SPAWN_BOSS && !bossHasSpawnedThisGameCycle){
            spawnBossNow = true;
        }

        if(spawnBossNow){
            PauseTransition bossPause = new PauseTransition(Duration.seconds(2));
            bossPause.setOnFinished(event -> {
                bossHasSpawnedThisGameCycle = true;
                createBoss();
            });
            bossPause.play();
        }else{
            uiManager.showWaveClearMessage(currentWaveNumber);
            PauseTransition wavePause = new PauseTransition(Duration.seconds(2));
            wavePause.setOnFinished(event -> {
                createEnemies();
                isLoadingNextWave = false;
            });
            wavePause.play();
        }
    }

    public int getCurrentWaveNumber(){
        return currentWaveNumber;
    }

    public void removeEnemy(Enemy enemy) {
        if (enemy != null) {
            if(enemy.getNode() != null && gamePane.getChildren().contains(enemy.getNode())) {
                gamePane.getChildren().remove(enemy.getNode());
            }
            enemies.remove(enemy);
        }
    }

    public void removeProjectileNode(Node projectileNode) {
        if (projectileNode != null && gamePane.getChildren().contains(projectileNode)) {
            gamePane.getChildren().remove(projectileNode);
        }
    }

    public void bossDefeated() {
        if(bossEnemy != null && bossEnemy.getNode() != null && gamePane.getChildren().contains(bossEnemy.getNode())) {
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        this.bossActive = false;
        this.bossEnemy = null;
        setBossAlreadySpawnedThisCycle(true);
        this.bossWasJustDefeated = true;
        isLoadingNextWave = false;
    }

    public void setBossAlreadySpawnedThisCycle(boolean status){
        this.bossHasSpawnedThisGameCycle = status;
    }

    public void resetGame(){
        System.out.println("GameEntityManager.resetGame(): Start. Kinder in gamePane VOR Reset: " + gamePane.getChildren().size());
        System.out.println("  enemies.size() VOR Reset: " + enemies.size());
        System.out.println("  playerProjectiles.size() VOR Reset: " + playerProjectiles.size());

        for(Enemy enemy : new ArrayList<>(enemies)) {
            if(enemy.getNode()!=null && gamePane.getChildren().contains(enemy.getNode())) {
                gamePane.getChildren().remove(enemy.getNode());
            }
        }
        enemies.clear();

        for(Rectangle projectile : new ArrayList<>(playerProjectiles)){
            if(projectile!=null && gamePane.getChildren().contains(projectile)) {
                gamePane.getChildren().remove(projectile);
            }
        }
        playerProjectiles.clear();

        if(bossEnemy!=null && bossEnemy.getNode()!=null && gamePane.getChildren().contains(bossEnemy.getNode())){
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        if(player != null && player.getNode() != null && gamePane.getChildren().contains(player.getNode())){
            gamePane.getChildren().remove(player.getNode());
        }

        resetInitialStateFlags();

        if (bossController != null) {
            bossController.resetBoss();
        }
        System.out.println("GameEntityManager.resetGame(): Abgeschlossen. Kinder in gamePane NACH Reset: " + gamePane.getChildren().size());
        System.out.println("  enemies.size() NACH Reset: " + enemies.size());
        System.out.println("  playerProjectiles.size() NACH Reset: " + playerProjectiles.size());
    }


    public void removeProjectile(Node projectileNode) {
        if (projectileNode != null && gamePane.getChildren().contains(projectileNode)) {
            gamePane.getChildren().remove(projectileNode);
            if (projectileNode instanceof Rectangle) {
                playerProjectiles.remove((Rectangle) projectileNode);
            }
        }
    }

    public void removeEnemyNode(Node enemyNode) {
        if(enemyNode != null && gamePane.getChildren().contains(enemyNode)){
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