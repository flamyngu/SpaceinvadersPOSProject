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
    private BossController bossController; // Hinzugefügt

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

    public GameEntityManager(Pane gamePane, GameDimensions gameDimensions, UIManager uiManager) {
        this.gamePane = gamePane;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;

        this.enemyGroupSpeedX = this.gameDimensions.getWidth() * 0.002;
        if(this.enemyGroupSpeedX < 1.0) this.enemyGroupSpeedX = 1.0;
        this.enemyGroupSpeedY = this.gameDimensions.getEnemyHeight() * 0.5;

        // BossController initialisieren
        this.bossController = new BossController(this, gameDimensions, uiManager);
    }

    public void createPlayer() {
        this.player = new Player(gameDimensions);
        gamePane.getChildren().add(player.getNode());
    }

    public void createEnemies() {
        if(bossActive){isLoadingNextWave = false;return;}
        // Alte Gegner-Nodes entfernen
        for (Enemy oldEnemy : enemies) {
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
        isLoadingNextWave = false;
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
        if(bossActive){isLoadingNextWave = false; return;};
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
        bossShape.setLayoutX(gameDimensions.getWidth()/2 - bossWidth/2);
        bossShape.setLayoutY(gameDimensions.getHeight()*0.1);

        this.bossEnemy = new Enemy(bossShape,GameDimensions.BOSS_HEALTH, GameDimensions.BOSS_POINTS);
        this.bossActive = true;
        gamePane.getChildren().add(bossShape);
        uiManager.showBossSpawnMessage();

        // Boss-Controller initialisieren
        bossController.initializeBoss();

        isLoadingNextWave = false;
    }

    public void spawnEnemyWaveInitial() {
        if (enemies.isEmpty() && !bossActive && !isLoadingNextWave) {
            isLoadingNextWave = true;
            currentWaveNumber = 1;
            createEnemies();
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
                bossHasSpawnedThisGameCycle = true;
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

    public void removeEnemy(Enemy enemy) {
        if (enemy != null) {
            gamePane.getChildren().remove(enemy.getNode());
        }
    }

    public void removeProjectileNode(Node projectileNode) {
        if (projectileNode != null) {
            gamePane.getChildren().remove(projectileNode);
        }
    }

    public void bossDefeated() {
        if(bossEnemy != null) {
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        this.bossActive = false;
        this.bossEnemy = null;
        setBossAlreadySpawnedThisCycle(true);
        System.out.println("Boss defeated roll credits");
        bossWasJustDefeated = true;
        isLoadingNextWave = false;

        // Boss-Controller zurücksetzen
        bossController.resetBoss();
    }

    public void setBossAlreadySpawnedThisCycle(boolean status){
        this.bossHasSpawnedThisGameCycle = status;
    }

    public void resetGame(){
        for(Enemy enemy : new ArrayList<>(enemies)) {
            if(enemy.getNode()!=null)gamePane.getChildren().remove(enemy.getNode());
        }
        enemies.clear();
        for(Rectangle projectile : new ArrayList<>(playerProjectiles)){
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

        currentWaveNumber = 0;
        isLoadingNextWave = false;
        if(player != null && player.getNode() != null)gamePane.getChildren().remove(player.getNode());
        player = null;
        this.enemyMovementDirection = 1.0;
        this.moveDownNextCycle = false;

        // Boss-Controller zurücksetzen
        bossController.resetBoss();
    }

    public void removeProjectile(Node projectileNode) {
        if (projectileNode != null) {
            gamePane.getChildren().remove(projectileNode);
        }
    }

    public void removeEnemyNode(Node enemyNode) {
        if(enemyNode != null){
            gamePane.getChildren().remove(enemyNode);
        }
    }

    // Neue Methode für Boss-Health-Reset
    public void resetBossHealth(int newHealth) {
        if (bossEnemy != null) {
            // Da Enemy-Health private ist, erstellen wir einen neuen Boss mit derselben Node
            Rectangle bossShape = (Rectangle) bossEnemy.getNode();
            Enemy newBoss = new Enemy(bossShape, newHealth, bossEnemy.getPoints());
            this.bossEnemy = newBoss;
        }
    }

    // Getter
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
    public BossController getBossController() { return bossController; } // Hinzugefügt
    public Pane getGamePane() { return gamePane; } // Hinzugefügt
}