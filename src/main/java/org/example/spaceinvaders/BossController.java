package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
// import javafx.scene.paint.Color; // Nicht mehr direkt für Farbsetzung der Projektile benötigt
// import javafx.scene.shape.Rectangle; // Minions nutzen jetzt Enemy-Klasse mit ImageView
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BossController {
    private GameEntityManager entityManager;
    private GameDimensions gameDimensions;
    private UIManager uiManager;
    private SoundManager soundManager;

    private Image bossProjectileImage; // Wird jetzt pistol_bullet1 sein

    private int bossPhase = 1;
    private int bossMaxHealth = GameDimensions.BOSS_HEALTH;
    private boolean bossIsRetreating = false;
    private boolean bossIsOffScreen = false;
    private long lastBossShootTime = 0;

    private double bossTargetY = 0;
    private BossMovementState currentMovementState = BossMovementState.ENTERING;
    private long movementStateStartTime = 0;
    private boolean isBossDivingDown = false;
    private Random random = new Random();

    private List<ImageView> bossProjectiles = new ArrayList<>();
    private List<Enemy> minionEnemies = new ArrayList<>();
    private MinionWaveType currentMinionWaveType = MinionWaveType.DIAGONAL_SWEEP;
    private int minionWaveCount = 0;
    private long minionWaveStartTime = 0;

    private enum BossMovementState {
        ENTERING, HOVERING, SIDE_STRAFE, DIVE_ATTACK, RETREATING, OFF_SCREEN
    }

    private enum MinionWaveType {
        DIAGONAL_SWEEP, FORMATION_ATTACK, SWARM_ATTACK, BOUNCING_PATTERN
    }

    public BossController(GameEntityManager entityManager, GameDimensions gameDimensions, UIManager uiManager, SoundManager soundManager) {
        this.entityManager = entityManager;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
        this.soundManager = soundManager;

        // Hole das pistol_bullet1.png Bild vom EntityManager
        this.bossProjectileImage = entityManager.getBossProjectileImage();
        if (this.bossProjectileImage == null || this.bossProjectileImage.isError()) {
            System.err.println("BossController konnte Boss-Projektilbild (pistol_bullet1.png) nicht vom EntityManager erhalten oder Bild ist fehlerhaft.");
        }
    }

    public void initializeBoss() {
        bossPhase = 1;
        bossMaxHealth = GameDimensions.BOSS_HEALTH;
        bossIsRetreating = false;
        bossIsOffScreen = false;
        isBossDivingDown = false;
        currentMovementState = BossMovementState.ENTERING;
        movementStateStartTime = System.nanoTime();
        minionWaveCount = 0;

        Enemy boss = entityManager.getBossEnemy();
        if (boss != null && boss.getNode() instanceof ImageView) {
            ImageView bossView = (ImageView) boss.getNode();
            bossView.setLayoutX(gameDimensions.getWidth() / 2 - bossView.getFitWidth() / 2);
            bossView.setLayoutY(-bossView.getFitHeight() - 30);
            boss.setHealth(bossMaxHealth);
            updateBossAppearance();
        } else {
            System.err.println("BossController.initializeBoss(): Boss entity oder dessen Node ist kein ImageView oder ist null!");
        }
        clearAllBossProjectiles();
        clearAllMinions();
    }

    public void bossTakeHit() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null || bossIsRetreating) {
            return;
        }
        boss.takeHit();
        flashBoss();
        if (!boss.isAlive()) {
            if (bossPhase < 3) {
                startBossRetreat();
            }
        }
    }

    private void startBossRetreat() {
        bossIsRetreating = true;
        bossIsOffScreen = false;
        isBossDivingDown = false;
        currentMovementState = BossMovementState.RETREATING;
        movementStateStartTime = System.nanoTime();
        minionWaveCount = 0;
        String message = switch (bossPhase) {
            case 1 -> "Boss retreating... Reinforcements incoming!";
            case 2 -> "Boss retreating... Prepare for the worst!";
            default -> "Boss retreating...";
        };
        uiManager.showPopupMessage(message, 2.0);

        if (soundManager != null) {
            soundManager.playBossScared();
        }
    }

    private void createBossProjectile(double startX, double startY, double targetX, double targetY) {
        if (bossProjectileImage == null || bossProjectileImage.isError()) {
            System.err.println("Boss-Projektilbild (pistol_bullet1.png) nicht geladen, Schuss wird nicht erstellt.");
            return;
        }

        ImageView projectileNode = new ImageView(bossProjectileImage);
        // Größe der Boss-Projektile anpassen (pistol_bullet1 könnte andere Proportionen haben als Komet)
        // Hier musst du experimentieren, um eine gute Größe zu finden.
        projectileNode.setFitWidth(gameDimensions.getProjectileWidth() * 2.0); // Beispielgröße
        projectileNode.setFitHeight(gameDimensions.getProjectileHeight() * 1.5); // Beispielgröße
        projectileNode.setPreserveRatio(true);

        // Farbänderung für Boss-Projektile, passend zur Boss-Phase
        ColorAdjust projectileColorAdjust = new ColorAdjust();
        projectileColorAdjust.setBrightness(0.0);
        projectileColorAdjust.setContrast(0.0);
        projectileColorAdjust.setHue(0.0);
        projectileColorAdjust.setSaturation(0.0);

        switch (bossPhase) {
            case 1:
                // Phase 1: vielleicht leicht rötlich oder Standardfarbe des Bullets
                projectileColorAdjust.setHue(-0.1);
                projectileColorAdjust.setSaturation(0.2);
                break;
            case 2:
                projectileColorAdjust.setHue(-0.25); // Stärkerer Rot/Violett-Stich
                projectileColorAdjust.setSaturation(0.3);
                projectileColorAdjust.setBrightness(-0.05);
                break;
            case 3:
                projectileColorAdjust.setHue(-0.5);  // Intensives Rot/Magenta
                projectileColorAdjust.setSaturation(0.5);
                projectileColorAdjust.setBrightness(-0.1);
                break;
        }
        projectileNode.setEffect(projectileColorAdjust);


        projectileNode.setLayoutX(startX - projectileNode.getFitWidth() / 2);
        projectileNode.setLayoutY(startY);

        setProjectileUserData(projectileNode, startX, startY, targetX, targetY);

        bossProjectiles.add(projectileNode);
        entityManager.getGamePane().getChildren().add(projectileNode);
    }

    private void setProjectileUserData(Node projectile, double startX, double startY, double targetX, double targetY) {
        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double projectileSpeedPerTick = gameDimensions.getProjectileSpeed() * 0.75;
        if (bossPhase == 3) projectileSpeedPerTick *= 1.3;

        if (distance > 0) {
            projectile.setUserData(new double[]{dx / distance * projectileSpeedPerTick, dy / distance * projectileSpeedPerTick});
        } else {
            projectile.setUserData(new double[]{0, projectileSpeedPerTick});
        }
    }

    private void updateBossProjectiles(double deltaTime) {
        Iterator<ImageView> iterator = bossProjectiles.iterator();
        while (iterator.hasNext()) {
            ImageView projectile = iterator.next();
            if (projectile.getUserData() == null || !(projectile.getUserData() instanceof double[])) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
                continue;
            }
            double[] velocityPerTick = (double[]) projectile.getUserData();

            projectile.setLayoutX(projectile.getLayoutX() + velocityPerTick[0]);
            projectile.setLayoutY(projectile.getLayoutY() + velocityPerTick[1]);

            if (projectile.getLayoutY() > gameDimensions.getHeight() + 50 ||
                    projectile.getLayoutY() < -projectile.getFitHeight() - 50 ||
                    projectile.getLayoutX() > gameDimensions.getWidth() + 50 ||
                    projectile.getLayoutX() < -projectile.getFitWidth() - 50) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
            }
        }
    }

    private void updateBossAppearance() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || !(boss.getNode() instanceof ImageView)) return;
        ImageView bossView = (ImageView) boss.getNode();

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(0.0);
        colorAdjust.setContrast(0.0);
        colorAdjust.setHue(0.0);
        colorAdjust.setSaturation(0.0);

        switch (bossPhase) {
            case 1:
                break;
            case 2:
                colorAdjust.setHue(-0.15);
                colorAdjust.setSaturation(0.15);
                colorAdjust.setBrightness(-0.05);
                break;
            case 3:
                colorAdjust.setHue(-0.4);
                colorAdjust.setSaturation(0.3);
                colorAdjust.setBrightness(-0.15);
                break;
        }
        bossView.setEffect(colorAdjust);
    }

    private void flashBoss() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || !(boss.getNode() instanceof ImageView)) return;
        ImageView bossView = (ImageView) boss.getNode();

        ColorAdjust flashEffect = new ColorAdjust();
        flashEffect.setBrightness(0.8);
        bossView.setEffect(flashEffect);

        PauseTransition flash = new PauseTransition(Duration.millis(70));
        flash.setOnFinished(e -> {
            updateBossAppearance();
        });
        flash.play();
    }

    public void clearAllBossProjectiles() {
        Iterator<ImageView> it = bossProjectiles.iterator();
        while(it.hasNext()){
            ImageView p = it.next();
            entityManager.getGamePane().getChildren().remove(p);
            it.remove();
        }
    }

    public void clearAllMinions() {
        Iterator<Enemy> it = minionEnemies.iterator();
        while (it.hasNext()) {
            Enemy m = it.next();
            if (m.getNode() != null) {
                entityManager.getGamePane().getChildren().remove(m.getNode());
            }
            it.remove();
        }
    }

    public boolean checkBossProjectileCollisions(Player player) {
        if (player == null || player.getNode() == null) return false;
        Iterator<ImageView> iterator = bossProjectiles.iterator();
        while (iterator.hasNext()) {
            ImageView projectile = iterator.next();
            if (projectile.getBoundsInParent().intersects(player.getNode().getBoundsInParent())) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
                return true;
            }
        }
        return false;
    }

    private void spawnMinion(Image minionImage, double x, double y, double width, double height, int health, int points, Object userData) {
        if (minionImage == null || minionImage.isError()) {
            System.err.println("Minion-Bild nicht geladen oder fehlerhaft, Minion ("+x+","+y+") wird nicht erstellt.");
            return;
        }
        Enemy minion = new Enemy(minionImage, width, height, health, points);
        Node minionNode = minion.getNode();
        minionNode.setLayoutX(x);
        minionNode.setLayoutY(y);
        if (userData != null ) {
            minionNode.setUserData(userData);
        }
        minionEnemies.add(minion);
        if (!entityManager.getGamePane().getChildren().contains(minionNode)) {
            entityManager.getGamePane().getChildren().add(minionNode);
        }
    }

    private void spawnMinionWave(MinionWaveType waveType) {
        clearAllMinions();
        switch (waveType) {
            case DIAGONAL_SWEEP: spawnDiagonalSweepMinions(); break;
            case FORMATION_ATTACK: spawnFormationMinions(); break;
            case SWARM_ATTACK: spawnSwarmMinions(); break;
            case BOUNCING_PATTERN: spawnBouncingMinions(); break;
        }
    }

    private void spawnDiagonalSweepMinions() {
        int minionCount;
        Image minionImg = entityManager.getEnemyGreenEyeImage();
        if (minionImg == null) { System.err.println("Grünes Augenbild für Minions nicht verfügbar."); return; }

        double minionWidth = gameDimensions.getEnemyWidth() * 0.65;
        double minionHeight = gameDimensions.getEnemyHeight() * 0.65;

        switch (this.bossPhase) {
            case 1: minionCount = 5; break;
            case 2: minionCount = 7; break;
            default: minionCount = 5; break;
        }
        for (int i = 0; i < minionCount; i++) {
            boolean fromLeft = i % 2 == 0;
            double startX = fromLeft ? -minionWidth -10 : gameDimensions.getWidth() + 10;
            double startY = gameDimensions.getHeight() * 0.15 + (i * (minionHeight + 15));
            spawnMinion(minionImg, startX, startY, minionWidth, minionHeight, 1, GameDimensions.POINTS_PER_ENEMY / 2, new double[]{fromLeft ? 1.0 : -1.0});
        }
    }

    private void spawnFormationMinions() {
        int minionCount;
        Image minionImg = entityManager.getEnemyPurpleEyeImage();
        if (minionImg == null) { System.err.println("Lila Augenbild für Minions nicht verfügbar."); return; }

        double minionWidth = gameDimensions.getEnemyWidth() * 0.7;
        double minionHeight = gameDimensions.getEnemyHeight() * 0.7;

        switch (this.bossPhase) {
            case 1: minionCount = 4; break;
            case 2: minionCount = 6; break;
            default: minionCount = 4; break;
        }
        double formationWidth = gameDimensions.getWidth() * 0.6;
        double startXOffset = (gameDimensions.getWidth() - formationWidth) / 2;
        for (int i = 0; i < minionCount; i++) {
            double x = startXOffset + (i * (formationWidth / (minionCount > 1 ? minionCount -1 : 1)));
            double y = -minionHeight - (Math.abs(i - (minionCount-1)/2.0) * (minionHeight + 10));
            spawnMinion(minionImg, x, y, minionWidth, minionHeight, 1, GameDimensions.POINTS_PER_ENEMY / 2, new double[]{0.0});
        }
    }

    private void spawnSwarmMinions() {
        int minionCount;
        Image minionImg = entityManager.getEnemyGreenEyeImage();
        if (minionImg == null) { System.err.println("Grünes Augenbild für Schwarm-Minions nicht verfügbar."); return; }

        double minionWidth = gameDimensions.getEnemyWidth() * 0.55;
        double minionHeight = gameDimensions.getEnemyHeight() * 0.55;

        switch (this.bossPhase) {
            case 1: minionCount = 7; break;
            case 2: minionCount = 10; break;
            default: minionCount = 8; break;
        }
        for (int i = 0; i < minionCount; i++) {
            double x = random.nextDouble() * (gameDimensions.getWidth() - minionWidth);
            double y = -minionHeight - (random.nextDouble() * 180);
            double initialVX = (random.nextDouble() - 0.5) * 2.5;
            double initialVY = 0.6 + random.nextDouble() * 1.8;
            spawnMinion(minionImg, x, y, minionWidth, minionHeight, 1, GameDimensions.POINTS_PER_ENEMY / 3, new double[]{initialVX, initialVY});
        }
    }

    private void spawnBouncingMinions() {
        int minionCount;
        Image minionImg = entityManager.getEnemyPurpleEyeImage();
        if (minionImg == null) { System.err.println("Lila Augenbild für Bouncing-Minions nicht verfügbar."); return; }

        double minionWidth = gameDimensions.getEnemyWidth() * 0.8;
        double minionHeight = gameDimensions.getEnemyHeight() * 0.8;

        switch (this.bossPhase) {
            case 1: minionCount = 2; break;
            case 2: minionCount = 3; break;
            default: minionCount = 3; break;
        }
        for (int i = 0; i < minionCount; i++) {
            double x = (i + 1) * (gameDimensions.getWidth() / (minionCount + 1.0)) - minionWidth/2;
            double y = -minionHeight - random.nextInt(60);
            spawnMinion(minionImg, x, y, minionWidth, minionHeight, 2, GameDimensions.POINTS_PER_ENEMY, new double[]{x, System.nanoTime() + (long)(i * 600_000_000L)});
        }
    }

    public void updateBossMovement(long now, double deltaTime) {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null || !entityManager.isBossActive()) {
            return;
        }
        double dtScaledForBossBody = deltaTime * 60.0;
        if (dtScaledForBossBody <= 0) dtScaledForBossBody = 1.0;

        if (bossIsRetreating) {
            updateRetreatMovement(now, dtScaledForBossBody, boss);
            updateMinionWaves(now, deltaTime);
        } else {
            updateActiveBossMovement(now, dtScaledForBossBody, boss);
            updateBossShooting(now, boss);
        }
        updateBossProjectiles(deltaTime);
    }

    private void updateActiveBossMovement(long now, double dtScaled, Enemy boss) {
        Node bossNode = boss.getNode();
        long timeSinceStateStart = now - movementStateStartTime;

        switch (currentMovementState) {
            case ENTERING:
                double entrySpeed = gameDimensions.getHeight() * 0.001;
                double targetEntryY = gameDimensions.getHeight() * 0.15;
                if (bossNode.getLayoutY() < targetEntryY) {
                    bossNode.setLayoutY(bossNode.getLayoutY() + entrySpeed * dtScaled);
                } else {
                    bossNode.setLayoutY(targetEntryY);
                    changeMovementState(BossMovementState.HOVERING, now);
                }
                break;
            case HOVERING:
                double hoverAmplitude = 20;
                double hoverSpeedFactor = 0.002;
                double baseY = gameDimensions.getHeight() * 0.15;
                double hoverOffset = Math.sin((double)timeSinceStateStart / 1_000_000_000.0 * hoverSpeedFactor * Math.PI * 2) * hoverAmplitude;
                bossNode.setLayoutY(baseY + hoverOffset);
                if (timeSinceStateStart > (3_000_000_000L + random.nextInt(2_000_000_000))) {
                    changeMovementState(BossMovementState.SIDE_STRAFE, now);
                }
                break;
            case SIDE_STRAFE:
                double strafeSpeedFactor = 0.0005;
                double centerXMovement = gameDimensions.getWidth() / 2;
                double strafeRange = gameDimensions.getWidth() * 0.3;
                double bossNodeWidth = (bossNode instanceof ImageView) ? ((ImageView)bossNode).getFitWidth() : bossNode.getBoundsInLocal().getWidth();
                double strafeX = centerXMovement + Math.sin((double)timeSinceStateStart / 1_000_000_000.0 * strafeSpeedFactor * Math.PI * 2) * strafeRange;
                bossNode.setLayoutX(strafeX - bossNodeWidth / 2);
                if (timeSinceStateStart > (4_000_000_000L + random.nextInt(2_000_000_000))) {
                    bossTargetY = gameDimensions.getHeight() * 0.4;
                    changeMovementState(BossMovementState.DIVE_ATTACK, now);
                }
                break;
            case DIVE_ATTACK:
                double diveSpeed = gameDimensions.getHeight() * 0.003;
                double hoverYReturn = gameDimensions.getHeight() * 0.15;

                if (isBossDivingDown) {
                    bossNode.setLayoutY(bossNode.getLayoutY() + diveSpeed * dtScaled);
                    if (bossNode.getLayoutY() >= bossTargetY) {
                        bossNode.setLayoutY(bossTargetY);
                        isBossDivingDown = false;
                    }
                } else {
                    bossNode.setLayoutY(bossNode.getLayoutY() - diveSpeed * dtScaled);
                    if (bossNode.getLayoutY() <= hoverYReturn) {
                        bossNode.setLayoutY(hoverYReturn);
                        changeMovementState(BossMovementState.HOVERING, now);
                    }
                }
                break;
            case RETREATING:
            case OFF_SCREEN:
                break;
        }
    }

    private void updateRetreatMovement(long now, double dtScaled, Enemy boss) {
        Node bossNode = boss.getNode();
        double bossNodeHeight = (bossNode instanceof ImageView) ? ((ImageView)bossNode).getFitHeight() : bossNode.getBoundsInLocal().getHeight();
        if (!bossIsOffScreen) {
            double retreatSpeed = gameDimensions.getHeight() * 0.002;
            bossNode.setLayoutY(bossNode.getLayoutY() - retreatSpeed * dtScaled);
            if (bossNode.getLayoutY() + bossNodeHeight < 0) {
                bossIsOffScreen = true;
                startMinionWave();
            }
        } else {
            if (minionEnemies.isEmpty() && !entityManager.isLoadingNextWave()) {
                returnBossForNextPhase(now);
            }
        }
    }

    private void updateBossShooting(long now, Enemy boss) {
        long shootCooldown = switch (bossPhase) {
            case 1 -> 1_500_000_000L;
            case 2 -> 1_000_000_000L;
            case 3 -> 700_000_000L;
            default -> 1_500_000_000L;
        };
        if (now - lastBossShootTime > shootCooldown) {
            shootBossProjectile(boss);
            lastBossShootTime = now;
            if (soundManager != null) {
                soundManager.playBossShoot();
            }
        }
    }

    private void shootBossProjectile(Enemy boss) {
        Node bossNode = boss.getNode();
        Player player = entityManager.getPlayer();
        if (player == null || player.getNode() == null || bossNode == null) return;

        double bossNodeHeight = (bossNode instanceof ImageView) ? ((ImageView)bossNode).getFitHeight() : bossNode.getBoundsInLocal().getHeight();
        double bossNodeWidth = (bossNode instanceof ImageView) ? ((ImageView)bossNode).getFitWidth() : bossNode.getBoundsInLocal().getWidth();

        double projectileSpawnY = bossNode.getLayoutY() + bossNodeHeight;
        double bossCenterX = bossNode.getLayoutX() + bossNodeWidth / 2;
        double bossLeftQuarter = bossNode.getLayoutX() + bossNodeWidth * 0.25;
        double bossRightQuarter = bossNode.getLayoutX() + bossNodeWidth * 0.75;

        double playerCenterX = player.getX() + player.getWidth() / 2;
        double playerY = player.getY();

        switch (bossPhase) {
            case 1:
                createBossProjectile(bossCenterX, projectileSpawnY, playerCenterX, playerY);
                break;
            case 2:
                createBossProjectile(bossLeftQuarter, projectileSpawnY, player.getX(), playerY);
                createBossProjectile(bossRightQuarter, projectileSpawnY, player.getX() + player.getWidth(), playerY);
                break;
            case 3:
                int patternChoice = random.nextInt(3);
                if (patternChoice == 0) {
                    createBossProjectile(bossCenterX, projectileSpawnY, playerCenterX - 50, playerY);
                    createBossProjectile(bossCenterX, projectileSpawnY, playerCenterX, playerY);
                    createBossProjectile(bossCenterX, projectileSpawnY, playerCenterX + 50, playerY);
                } else if (patternChoice == 1) {
                    double[] angles = {-Math.PI / 6, -Math.PI / 12, 0, Math.PI / 12, Math.PI / 6};
                    for (double angle : angles) {
                        double projectileDirX = Math.sin(angle);
                        double projectileDirY = Math.cos(angle);
                        double farDistance = gameDimensions.getHeight();
                        double targetX = bossCenterX + projectileDirX * farDistance;
                        double targetY = projectileSpawnY + projectileDirY * farDistance;
                        createBossProjectile(bossCenterX, projectileSpawnY, targetX, targetY);
                    }
                } else {
                    double offset = gameDimensions.getProjectileWidth() * 2.5;
                    createBossProjectile(bossCenterX - offset, projectileSpawnY, bossCenterX - offset, gameDimensions.getHeight() + 50);
                    createBossProjectile(bossCenterX, projectileSpawnY, bossCenterX, gameDimensions.getHeight() + 50);
                    createBossProjectile(bossCenterX + offset, projectileSpawnY, bossCenterX + offset, gameDimensions.getHeight() + 50);
                }
                break;
        }
    }

    private void changeMovementState(BossMovementState newState, long now) {
        currentMovementState = newState;
        movementStateStartTime = now;
        if (newState == BossMovementState.DIVE_ATTACK) {
            this.isBossDivingDown = true;
        }
    }

    private void startMinionWave() {
        minionWaveCount++;
        minionWaveStartTime = System.nanoTime();

        List<MinionWaveType> possibleTypesForThisPhase = new ArrayList<>();
        switch (bossPhase) {
            case 1:
                possibleTypesForThisPhase.add(MinionWaveType.DIAGONAL_SWEEP);
                possibleTypesForThisPhase.add(MinionWaveType.FORMATION_ATTACK);
                break;
            case 2:
                possibleTypesForThisPhase.add(MinionWaveType.SWARM_ATTACK);
                possibleTypesForThisPhase.add(MinionWaveType.BOUNCING_PATTERN);
                break;
            default:
                possibleTypesForThisPhase.add(MinionWaveType.DIAGONAL_SWEEP);
                break;
        }

        if (!possibleTypesForThisPhase.isEmpty()) {
            currentMinionWaveType = possibleTypesForThisPhase.get(random.nextInt(possibleTypesForThisPhase.size()));
        } else {
            currentMinionWaveType = MinionWaveType.DIAGONAL_SWEEP;
            System.err.println("BossController.startMinionWave(): No possible minion types found for bossPhase " +
                    bossPhase + ", defaulting to DIAGONAL_SWEEP.");
        }

        System.out.println("BossController: Starting RANDOMIZED minion wave " + minionWaveCount +
                " of type " + currentMinionWaveType + " after boss phase " + bossPhase);
        spawnMinionWave(currentMinionWaveType);
    }

    private void updateMinionWaves(long now, double deltaTime) {
        Iterator<Enemy> iterator = minionEnemies.iterator();
        double dtGameTickScaling = deltaTime * 60.0;
        if (dtGameTickScaling <= 0) dtGameTickScaling = 1.0;

        while (iterator.hasNext()) {
            Enemy minion = iterator.next();
            Node minionNode = minion.getNode();
            if (minionNode == null || !minion.isAlive()) {
                if(minionNode != null) entityManager.getGamePane().getChildren().remove(minionNode);
                iterator.remove(); continue;
            }
            if (!(minionNode.getUserData() instanceof double[])) {
                System.err.println("Minion hat ungültige UserData: " + minionNode.getUserData());
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(minionNode);
                continue;
            }
            double[] userData = (double[]) minionNode.getUserData();
            boolean shouldRemove = false;

            double nodeWidth = (minionNode instanceof ImageView) ? ((ImageView)minionNode).getFitWidth() : minionNode.getBoundsInLocal().getWidth();

            switch (currentMinionWaveType) {
                case DIAGONAL_SWEEP:
                    double dirXFactor = userData[0];
                    double baseSpeedXDiagonal = gameDimensions.getWidth() * 0.002;
                    double moveX = dirXFactor * baseSpeedXDiagonal * dtGameTickScaling;
                    double moveY = baseSpeedXDiagonal * 0.5 * dtGameTickScaling;
                    minionNode.setLayoutX(minionNode.getLayoutX() + moveX);
                    minionNode.setLayoutY(minionNode.getLayoutY() + moveY);
                    if (minionNode.getLayoutY() > gameDimensions.getHeight() + 20 ||
                            (dirXFactor > 0 && minionNode.getLayoutX() > gameDimensions.getWidth() + 20) ||
                            (dirXFactor < 0 && minionNode.getLayoutX() < -nodeWidth - 20)) {
                        shouldRemove = true;
                    }
                    break;
                case FORMATION_ATTACK:
                    double baseFormationSpeedY = gameDimensions.getHeight() * 0.0015;
                    minionNode.setLayoutY(minionNode.getLayoutY() + baseFormationSpeedY * dtGameTickScaling);
                    if (minionNode.getLayoutY() > gameDimensions.getHeight() + 20) shouldRemove = true;
                    break;
                case SWARM_ATTACK:
                    double vx = userData[0]; double vy = userData[1];
                    vx += (random.nextDouble() - 0.5) * 0.1;
                    vy += (random.nextDouble() - 0.5) * 0.05;
                    vx = Math.max(-2.5, Math.min(2.5, vx)); vy = Math.max(0.4, Math.min(3.0, vy));
                    userData[0] = vx; userData[1] = vy;

                    double baseSwarmSpeedX = gameDimensions.getEnemyWidth() * 0.04 * vx;
                    double baseSwarmSpeedY = gameDimensions.getEnemyHeight() * 0.04 * vy;
                    minionNode.setLayoutX(minionNode.getLayoutX() + baseSwarmSpeedX * dtGameTickScaling);
                    minionNode.setLayoutY(minionNode.getLayoutY() + baseSwarmSpeedY * dtGameTickScaling);

                    if (minionNode.getLayoutX() < 0) {
                        minionNode.setLayoutX(0); userData[0] = Math.abs(vx * 0.8);
                    } else if (minionNode.getLayoutX() > gameDimensions.getWidth() - nodeWidth) {
                        minionNode.setLayoutX(gameDimensions.getWidth() - nodeWidth);
                        userData[0] = -Math.abs(vx * 0.8);
                    }
                    if (minionNode.getLayoutY() > gameDimensions.getHeight() + 20) shouldRemove = true;
                    break;
                case BOUNCING_PATTERN:
                    double initialXForBounce = userData[0]; long phaseTimeOffsetNanos = (long)userData[1];
                    double bounceAmplitude = gameDimensions.getWidth() * 0.15;
                    double timeInSeconds = (now - phaseTimeOffsetNanos) / 1_000_000_000.0;
                    double bounceFrequencyFactor = 1.5;

                    double baseBouncingSpeedY = gameDimensions.getHeight() * 0.0015 * 0.8;
                    minionNode.setLayoutY(minionNode.getLayoutY() + baseBouncingSpeedY * dtGameTickScaling);
                    double newBounceX = initialXForBounce + Math.sin(timeInSeconds * bounceFrequencyFactor * Math.PI * 2) * bounceAmplitude;
                    newBounceX = Math.max(0, Math.min(newBounceX, gameDimensions.getWidth() - nodeWidth));
                    minionNode.setLayoutX(newBounceX);
                    if (minionNode.getLayoutY() > gameDimensions.getHeight() + 20) shouldRemove = true;
                    break;
            }
            if (shouldRemove) {
                iterator.remove(); entityManager.getGamePane().getChildren().remove(minionNode);
            }
        }
    }

    private void returnBossForNextPhase(long now) {
        System.out.println("BossController: Returning for Phase " + (bossPhase + 1));
        bossPhase++;
        bossIsRetreating = false;
        bossIsOffScreen = false;
        isBossDivingDown = false;

        Enemy boss = entityManager.getBossEnemy();
        if (boss != null && boss.getNode() instanceof ImageView) {
            ImageView bossView = (ImageView) boss.getNode();
            int newHealth = bossMaxHealth + (bossPhase - 1) * (GameDimensions.BOSS_HEALTH / 2);
            entityManager.resetBossHealth(newHealth);
            System.out.println("BossController: Boss health reset to " + newHealth + " for phase " + bossPhase);

            bossView.setLayoutX(gameDimensions.getWidth() / 2 - bossView.getFitWidth() / 2);
            bossView.setLayoutY(-bossView.getFitHeight() - 20);
            bossView.setVisible(true);
        } else {
            System.err.println("BossController.returnBossForNextPhase(): Boss entity or node is not ImageView or is NULL!");
        }

        currentMovementState = BossMovementState.ENTERING;
        movementStateStartTime = now;
        minionWaveCount = 0;
        updateBossAppearance();
        String phaseMessage = switch (bossPhase) {
            case 2 -> "Boss Phase 2 - More Power!";
            case 3 -> "Boss Phase 3 - Final Stand!";
            default -> "Boss has returned!";
        };
        uiManager.showPopupMessage(phaseMessage, 2.5);
    }

    public void resetBoss() {
        System.out.println("BossController: resetBoss() CALLED.");
        bossPhase = 1;
        bossIsRetreating = false;
        bossIsOffScreen = false;
        isBossDivingDown = false;
        currentMovementState = BossMovementState.ENTERING;
        movementStateStartTime = System.nanoTime();
        minionWaveCount = 0;
        clearAllBossProjectiles();
        clearAllMinions();
        Enemy boss = entityManager.getBossEnemy();
        if(boss != null && boss.getNode() instanceof ImageView) {
            ImageView bossView = (ImageView) boss.getNode();
            bossView.setLayoutY(-bossView.getFitHeight() - 20);
            updateBossAppearance();
        }
    }

    public boolean checkPlayerProjectileVsMinionCollisions(List<ImageView> playerProjectiles, UIManager uiManager) {
        boolean hitDetectedOverall = false;
        Iterator<ImageView> projIterator = playerProjectiles.iterator();
        while (projIterator.hasNext()) {
            ImageView projectile = projIterator.next();
            Iterator<Enemy> minionIterator = minionEnemies.iterator();
            // boolean projectileHitThisPass = false; // Nicht mehr benötigt, da Projektil direkt entfernt wird
            while (minionIterator.hasNext()) {
                Enemy minion = minionIterator.next();
                if (minion.getNode() != null && minion.isAlive() && projectile.getBoundsInParent().intersects(minion.getNode().getBoundsInParent())) {
                    minion.takeHit();
                    if (!minion.isAlive()) {
                        minionIterator.remove();
                        entityManager.getGamePane().getChildren().remove(minion.getNode());
                        uiManager.addScore(minion.getPoints());
                    }
                    hitDetectedOverall = true;
                    // projectileHitThisPass = true;
                    projIterator.remove(); // Projektil entfernen, nachdem es getroffen hat
                    entityManager.removeProjectileNode(projectile); // Auch aus der gamePane entfernen
                    break;
                }
            }
        }
        return hitDetectedOverall;
    }
    public boolean checkPlayerVsMinionCollisions(Player player) {
        if (player == null || player.getNode() == null) return false;
        for (Enemy minion : minionEnemies) {
            if (minion.getNode() != null && minion.isAlive() &&
                    player.getNode().getBoundsInParent().intersects(minion.getNode().getBoundsInParent())) {
                return true;
            }
        }
        return false;
    }

    public List<ImageView> getBossProjectiles() { return bossProjectiles; }
    public List<Enemy> getMinionEnemies() { return minionEnemies; }
    public int getBossPhase() { return bossPhase; }
    public boolean isBossRetreating() { return bossIsRetreating; }
}