package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
// import javafx.scene.paint.Color; // Kaum noch direkt verwendet
// import javafx.scene.shape.Rectangle; // Kaum noch direkt verwendet
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEntityManager {
    private Pane gamePane;
    private GameDimensions gameDimensions;
    private UIManager uiManager;
    private BossController bossController;
    private MusicalInvaders mainApp;

    private Image enemyGreenEyeImage;
    private Image enemyPurpleEyeImage;
    private Image bossEyesImage;
    private Image playerCometShotImage;
    private Image playerBeerShotImage;
    private Image bossCometShotImage;

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
    private List<ImageView> playerProjectiles = new ArrayList<>();
    private Random random = new Random(); // Wird hier nicht direkt verwendet, aber oft nützlich


    public GameEntityManager(Pane gamePane, GameDimensions gameDimensions, UIManager uiManager, SoundManager soundManager, MusicalInvaders mainApp) {
        this.gamePane = gamePane;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
        this.mainApp = mainApp;

        try {
            enemyGreenEyeImage = loadImage("/images/enemy_eye_green.png");
            enemyPurpleEyeImage = loadImage("/images/enemy_eye_purple.png");
            bossEyesImage = loadImage("/images/boss_eyes.png");
            playerCometShotImage = loadImage("/images/comet_shot.png");
            playerBeerShotImage = loadImage("/images/beer_shot.png");
            bossCometShotImage = loadImage("/images/pistol_bullet1.png");

        } catch (RuntimeException e) {
            System.err.println("Ein oder mehrere kritische Spielgrafiken konnten nicht geladen werden. Spiel wird beendet.");
            e.printStackTrace();
            Platform.exit();
            return;
        }

        this.enemyGroupSpeedX = this.gameDimensions.getWidth() * 0.002;
        if(this.enemyGroupSpeedX < 1.0) this.enemyGroupSpeedX = 1.0;
        this.enemyGroupSpeedY = this.gameDimensions.getEnemyHeight() * 0.5;

        this.bossController = new BossController(this, gameDimensions, uiManager, soundManager);
        resetInitialStateFlags();
    }

    private Image loadImage(String path) {
        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            if (img.isError()) {
                System.err.println("Fehler beim Laden des Bildes: " + path);
                System.err.println("Exception: " + (img.getException() != null ? img.getException().getMessage() : "Unbekannter Bildladefehler"));
                throw new RuntimeException("Konnte Bild nicht laden: " + path, img.getException());
            }
            System.out.println("Bild geladen: " + path);
            return img;
        } catch (NullPointerException e) {
            System.err.println("Bildressource nicht gefunden (NullPointerException): " + path);
            throw new RuntimeException("Bildressource nicht gefunden: " + path, e);
        } catch (Exception e) {
            System.err.println("Allgemeiner Fehler beim Laden des Bildes: " + path);
            throw new RuntimeException("Allgemeiner Fehler beim Laden des Bildes: " + path, e);
        }
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
        if (player.getNode() != null && player.getNode().getScene() == null) {
            if (!gamePane.getChildren().contains(player.getNode())) {
                gamePane.getChildren().add(player.getNode());
            }
        } else if (player.getNode() == null) {
            System.err.println("Spieler-Node ist null nach Erstellung!");
        }
    }

    public void createEnemies() {
        if (bossActive) return;

        for (Enemy oldEnemy : new ArrayList<>(enemies)) {
            if (oldEnemy.getNode() != null) gamePane.getChildren().remove(oldEnemy.getNode());
        }
        enemies.clear();

        double enemyWidth = gameDimensions.getEnemyWidth() * 0.75;
        double enemyHeight = gameDimensions.getEnemyHeight() * 0.75;

        double startX = (gameDimensions.getWidth() - (GameDimensions.ENEMIES_PER_ROW * (enemyWidth + gameDimensions.getEnemySpacingX()) - gameDimensions.getEnemySpacingX())) / 2;
        double startY = gameDimensions.getHeight() * 0.10;

        for (int row = 0; row < GameDimensions.ENEMY_ROWS; row++) {
            for (int col = 0; col < GameDimensions.ENEMIES_PER_ROW; col++) {
                Image currentEnemyImg = (row < GameDimensions.ENEMY_ROWS / 2) ? enemyPurpleEyeImage : enemyGreenEyeImage;
                if (currentEnemyImg == null || currentEnemyImg.isError()){ // Zusätzlicher Check
                    System.err.println("Fehlerhaftes Bild für normalen Gegner, überspringe Erstellung.");
                    continue;
                }

                Enemy newLogicalEnemy = new Enemy(currentEnemyImg, enemyWidth, enemyHeight, 1, GameDimensions.POINTS_PER_ENEMY);

                Node enemyNode = newLogicalEnemy.getNode();
                double x = startX + col * (enemyWidth + gameDimensions.getEnemySpacingX());
                double y = startY + row * (enemyHeight + gameDimensions.getEnemySpacingY());
                enemyNode.setLayoutX(x);
                enemyNode.setLayoutY(y);

                enemies.add(newLogicalEnemy);
                if (!gamePane.getChildren().contains(enemyNode)) {
                    gamePane.getChildren().add(enemyNode);
                }
            }
        }
    }

    public void createProjectile() {
        if (player == null) return;

        Image projectileImageToUse = playerCometShotImage;
        VoiceProfile selectedVoice = mainApp.getSelectedVoiceProfile();

        if (selectedVoice != null) {
            String voiceName = selectedVoice.getDisplayName();
            if ("Mhh lecka Bierchen".equals(voiceName) || "General TK25".equals(voiceName)) {
                projectileImageToUse = playerBeerShotImage;
            }
        }

        if (projectileImageToUse == null || projectileImageToUse.isError()) {
            System.err.println("Projektilbild nicht geladen oder fehlerhaft, Schuss wird nicht erstellt.");
            return;
        }

        ImageView projectileNode = new ImageView(projectileImageToUse);
        double projWidth, projHeight;

        if (projectileImageToUse == playerBeerShotImage) {
            projWidth = gameDimensions.getProjectileWidth() * 3.5;
            projHeight = gameDimensions.getProjectileHeight() * 2.5;
        } else {
            projWidth = gameDimensions.getProjectileWidth() * 1.2;
            projHeight = gameDimensions.getProjectileHeight() * 2.0;
        }

        projectileNode.setFitWidth(projWidth);
        projectileNode.setFitHeight(projHeight);
        projectileNode.setPreserveRatio(true);

        projectileNode.setLayoutX(player.getX() + player.getWidth() / 2 - projectileNode.getFitWidth() / 2);
        projectileNode.setLayoutY(player.getY() - projectileNode.getFitHeight());

        playerProjectiles.add(projectileNode);
        gamePane.getChildren().add(projectileNode);
    }

    public void createBoss() {
        if(bossActive || bossEyesImage == null || bossEyesImage.isError()){
            System.err.println("Boss kann nicht erstellt werden. Aktiv: " + bossActive + ", Bild geladen: " + (bossEyesImage != null && !bossEyesImage.isError()));
            return;
        }
        for(Enemy oldEnemy : new ArrayList<>(enemies)) {
            if(oldEnemy.getNode() != null) gamePane.getChildren().remove(oldEnemy.getNode());
        }
        enemies.clear();

        double bossSpriteAspectRatio = bossEyesImage.getWidth() / bossEyesImage.getHeight();
        double bossDisplayWidth = gameDimensions.getWidth() * 0.4;
        double bossDisplayHeight = bossDisplayWidth / bossSpriteAspectRatio;

        this.bossEnemy = new Enemy(bossEyesImage, bossDisplayWidth, bossDisplayHeight, GameDimensions.BOSS_HEALTH, GameDimensions.BOSS_POINTS);

        this.bossActive = true;
        if (bossEnemy.getNode() != null && bossEnemy.getNode().getScene() == null) {
            if(!gamePane.getChildren().contains(bossEnemy.getNode())){
                gamePane.getChildren().add(bossEnemy.getNode());
            }
        }
        uiManager.showBossSpawnMessage();
        bossController.initializeBoss();
        isLoadingNextWave = false;
    }

    public void removeProjectile(Node projectileNode) {
        if (projectileNode != null && gamePane.getChildren().contains(projectileNode)) {
            gamePane.getChildren().remove(projectileNode);
            if (projectileNode instanceof ImageView) {
                playerProjectiles.remove((ImageView) projectileNode);
            }
        }
    }

    public List<ImageView> getPlayerProjectiles() { return playerProjectiles; }

    // Getter für Bilder, die der BossController benötigt
    public Image getBossProjectileImage() {
        return (bossCometShotImage != null && !bossCometShotImage.isError()) ? bossCometShotImage : null;
    }
    public Image getEnemyGreenEyeImage() {
        return (enemyGreenEyeImage != null && !enemyGreenEyeImage.isError()) ? enemyGreenEyeImage : null;
    }
    public Image getEnemyPurpleEyeImage() {
        return (enemyPurpleEyeImage != null && !enemyPurpleEyeImage.isError()) ? enemyPurpleEyeImage : null;
    }

    public void spawnEnemyWaveInitial() {
        if (enemies.isEmpty() && !bossActive && !isLoadingNextWave) {
            isLoadingNextWave = true;
            currentWaveNumber = 1;
            createEnemies();
            uiManager.showWaveStartMessage(currentWaveNumber);
            isLoadingNextWave = false;
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

        boolean spawnBossNow = (currentWaveNumber >= GameDimensions.WAVE_NUMBER_TO_SPAWN_BOSS && !bossHasSpawnedThisGameCycle);

        if(spawnBossNow){
            uiManager.showBossSpawnMessage();
            PauseTransition bossPause = new PauseTransition(Duration.seconds(2.5));
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

    public int getCurrentWaveNumber(){ return currentWaveNumber; }

    public void removeEnemy(Enemy enemy) {
        if (enemy != null) {
            if(enemy.getNode() != null) gamePane.getChildren().remove(enemy.getNode());
            enemies.remove(enemy);
        }
    }

    public void removeProjectileNode(Node projectileNode) {
        if (projectileNode != null) gamePane.getChildren().remove(projectileNode);
    }

    public void bossDefeated() {
        if(bossEnemy != null && bossEnemy.getNode() != null) {
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
        System.out.println("GameEntityManager.resetGame(): Start.");
        for(Enemy enemy : new ArrayList<>(enemies)) {
            if(enemy.getNode()!=null) gamePane.getChildren().remove(enemy.getNode());
        }
        enemies.clear();

        for(ImageView projectile : new ArrayList<>(playerProjectiles)){
            if(projectile!=null) gamePane.getChildren().remove(projectile);
        }
        playerProjectiles.clear();

        if(bossEnemy!=null && bossEnemy.getNode()!=null) {
            gamePane.getChildren().remove(bossEnemy.getNode());
        }
        if(player != null && player.getNode() != null) {
            gamePane.getChildren().remove(player.getNode());
        }
        resetInitialStateFlags();
        if (bossController != null) bossController.resetBoss();
        System.out.println("GameEntityManager.resetGame(): Abgeschlossen.");
    }

    public void removeEnemyNode(Node enemyNode) {
        if(enemyNode != null) gamePane.getChildren().remove(enemyNode);
    }

    public void resetBossHealth(int newHealth) {
        if (bossEnemy != null) bossEnemy.setHealth(newHealth);
    }

    public Player getPlayer() { return player; }
    public boolean bossAlreadySpawnedThisCycle(){return this.bossHasSpawnedThisGameCycle;}
    public List<Enemy> getEnemies() { return enemies; }
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