package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    private List<Rectangle> bossProjectiles = new ArrayList<>();
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
    }

    public void initializeBoss() {
        System.out.println("BossController: Initializing Boss. Phase 1.");
        bossPhase = 1;
        bossMaxHealth = GameDimensions.BOSS_HEALTH;
        bossIsRetreating = false;
        bossIsOffScreen = false;
        isBossDivingDown = false;
        currentMovementState = BossMovementState.ENTERING;
        movementStateStartTime = System.nanoTime();
        minionWaveCount = 0;

        Enemy boss = entityManager.getBossEnemy();
        if (boss != null && boss.getNode() != null) {
            boss.getNode().setLayoutX(gameDimensions.getWidth() / 2 - boss.getNode().getBoundsInLocal().getWidth() / 2);
            boss.getNode().setLayoutY(-boss.getNode().getBoundsInLocal().getHeight() - 20);
            boss.setHealth(bossMaxHealth);
            System.out.println("BossController: Boss health set to " + boss.getHealth() + " for phase 1.");
            updateBossAppearance();
        } else {
            System.err.println("BossController.initializeBoss(): Boss entity or node is null!");
        }
        clearAllBossProjectiles();
        clearAllMinions();
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
                double strafeX = centerXMovement + Math.sin((double)timeSinceStateStart / 1_000_000_000.0 * strafeSpeedFactor * Math.PI * 2) * strafeRange;
                bossNode.setLayoutX(strafeX - bossNode.getBoundsInLocal().getWidth() / 2);
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
        if (!bossIsOffScreen) {
            double retreatSpeed = gameDimensions.getHeight() * 0.002;
            bossNode.setLayoutY(bossNode.getLayoutY() - retreatSpeed * dtScaled);
            if (bossNode.getLayoutY() + bossNode.getBoundsInLocal().getHeight() < 0) {
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
        if (player == null || player.getNode() == null) return;

        double projectileSpawnY = bossNode.getLayoutY() + bossNode.getBoundsInLocal().getHeight();
        double bossCenterX = bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() / 2;
        double bossLeftQuarter = bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() * 0.25;
        double bossRightQuarter = bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() * 0.75;

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


    private void createBossProjectile(double startX, double startY, double targetX, double targetY) {
        Rectangle projectile = new Rectangle(gameDimensions.getProjectileWidth() * 1.5, gameDimensions.getProjectileHeight() * 1.5);
        projectile.setFill(Color.ORANGERED);

        projectile.setLayoutX(startX - projectile.getWidth() / 2);
        projectile.setLayoutY(startY);

        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        double projectileSpeedPerTick = gameDimensions.getProjectileSpeed() * 0.8;
        if (bossPhase == 3) {
            projectileSpeedPerTick *= 1.15;
        }

        if (distance > 0) {
            projectile.setUserData(new double[]{dx / distance * projectileSpeedPerTick, dy / distance * projectileSpeedPerTick});
        } else {
            projectile.setUserData(new double[]{0, projectileSpeedPerTick});
        }
        bossProjectiles.add(projectile);
        entityManager.getGamePane().getChildren().add(projectile);
    }

    private void updateBossProjectiles(double deltaTime) {
        Iterator<Rectangle> iterator = bossProjectiles.iterator();
        while (iterator.hasNext()) {
            Rectangle projectile = iterator.next();
            double[] velocityPerTick = (double[]) projectile.getUserData();

            projectile.setLayoutX(projectile.getLayoutX() + velocityPerTick[0]);
            projectile.setLayoutY(projectile.getLayoutY() + velocityPerTick[1]);

            if (projectile.getLayoutY() > gameDimensions.getHeight() + 50 ||
                    projectile.getLayoutY() < -projectile.getHeight() - 50 ||
                    projectile.getLayoutX() > gameDimensions.getWidth() + 50 ||
                    projectile.getLayoutX() < -projectile.getWidth() - 50) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
            }
        }
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
        switch (this.bossPhase) {
            case 1: minionCount = 6; break;
            case 2: minionCount = 8; break;
            default: minionCount = 6; break;
        }
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(gameDimensions.getEnemyWidth() * 0.7, gameDimensions.getEnemyHeight() * 0.7);
            minionShape.setFill(Color.DARKORANGE);
            boolean fromLeft = i % 2 == 0;
            double startX = fromLeft ? -minionShape.getWidth() -10 : gameDimensions.getWidth() + 10;
            double startY = gameDimensions.getHeight() * 0.1 + (i * (gameDimensions.getEnemyHeight() * 0.7 + 10));
            minionShape.setLayoutX(startX); minionShape.setLayoutY(startY);
            minionShape.setUserData(new double[]{fromLeft ? 1.0 : -1.0});
            Enemy minion = new Enemy(minionShape, 1, GameDimensions.POINTS_PER_ENEMY / 2);
            minionEnemies.add(minion); entityManager.getGamePane().getChildren().add(minionShape);
        }
    }
    private void spawnFormationMinions() {
        int minionCount;
        switch (this.bossPhase) {
            case 1: minionCount = 5; break;
            case 2: minionCount = 7; break;
            default: minionCount = 5; break;
        }
        double formationWidth = gameDimensions.getWidth() * 0.5;
        double startXOffset = (gameDimensions.getWidth() - formationWidth) / 2;
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(gameDimensions.getEnemyWidth() * 0.8, gameDimensions.getEnemyHeight() * 0.8);
            minionShape.setFill(Color.GOLD);
            double x = startXOffset + (i * (formationWidth / (minionCount > 1 ? minionCount - 1 : 1)));
            double y = -minionShape.getHeight() - (Math.abs(i - (minionCount-1)/2.0) * (gameDimensions.getEnemyHeight()*0.8 + 5));
            minionShape.setLayoutX(x); minionShape.setLayoutY(y);
            minionShape.setUserData(new double[]{0.0});
            Enemy minion = new Enemy(minionShape, 1, GameDimensions.POINTS_PER_ENEMY / 2);
            minionEnemies.add(minion); entityManager.getGamePane().getChildren().add(minionShape);
        }
    }
    private void spawnSwarmMinions() {
        int minionCount;
        switch (this.bossPhase) {
            case 1: minionCount = 8; break;
            case 2: minionCount = 12; break;
            default: minionCount = 10; break;
        }
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(gameDimensions.getEnemyWidth() * 0.5, gameDimensions.getEnemyHeight() * 0.5);
            minionShape.setFill(Color.LIGHTCORAL);
            double x = random.nextDouble() * (gameDimensions.getWidth() - minionShape.getWidth());
            double y = -minionShape.getHeight() - (random.nextDouble() * 150);
            minionShape.setLayoutX(x); minionShape.setLayoutY(y);
            double initialVX = (random.nextDouble() - 0.5) * 2.0;
            double initialVY = 0.5 + random.nextDouble() * 1.5;
            minionShape.setUserData(new double[]{initialVX, initialVY});
            Enemy minion = new Enemy(minionShape, 1, GameDimensions.POINTS_PER_ENEMY / 3);
            minionEnemies.add(minion); entityManager.getGamePane().getChildren().add(minionShape);
        }
    }
    private void spawnBouncingMinions() {
        int minionCount;
        switch (this.bossPhase) {
            case 1: minionCount = 3; break;
            case 2: minionCount = 4; break;
            default: minionCount = 4; break;
        }
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(gameDimensions.getEnemyWidth(), gameDimensions.getEnemyHeight());
            minionShape.setFill(Color.PALEGREEN);
            double x = (i + 1) * (gameDimensions.getWidth() / (minionCount + 1.0)) - minionShape.getWidth()/2;
            double y = -minionShape.getHeight() - random.nextInt(50);
            minionShape.setLayoutX(x); minionShape.setLayoutY(y);
            minionShape.setUserData(new double[]{x, System.nanoTime() + (long)(i * 500_000_000L)});
            Enemy minion = new Enemy(minionShape, 2, GameDimensions.POINTS_PER_ENEMY);
            minionEnemies.add(minion); entityManager.getGamePane().getChildren().add(minionShape);
        }
    }

    private void updateMinionWaves(long now, double deltaTime) {
        Iterator<Enemy> iterator = minionEnemies.iterator();

        double dtGameTickScaling = deltaTime * 60.0;
        if (dtGameTickScaling <= 0) {
            dtGameTickScaling = 1.0;
        }

        while (iterator.hasNext()) {
            Enemy minion = iterator.next();
            Node minionNode = minion.getNode();
            if (minionNode == null || !minion.isAlive()) {
                if(minionNode != null) entityManager.getGamePane().getChildren().remove(minionNode);
                iterator.remove(); continue;
            }
            double[] userData = (double[]) minionNode.getUserData();
            boolean shouldRemove = false;

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
                            (dirXFactor < 0 && minionNode.getLayoutX() < -minionNode.getBoundsInLocal().getWidth() - 20)) {
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
                    } else if (minionNode.getLayoutX() > gameDimensions.getWidth() - minionNode.getBoundsInLocal().getWidth()) {
                        minionNode.setLayoutX(gameDimensions.getWidth() - minionNode.getBoundsInLocal().getWidth());
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
                    newBounceX = Math.max(0, Math.min(newBounceX, gameDimensions.getWidth() - minionNode.getBoundsInLocal().getWidth()));
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
        if (boss != null) {
            int newHealth = bossMaxHealth + (bossPhase - 1) * (GameDimensions.BOSS_HEALTH / 2);
            entityManager.resetBossHealth(newHealth);
            System.out.println("BossController: Boss health reset to " + newHealth + " for phase " + bossPhase);
            if (boss.getNode() != null) {
                boss.getNode().setLayoutX(gameDimensions.getWidth() / 2 - boss.getNode().getBoundsInLocal().getWidth() / 2);
                boss.getNode().setLayoutY(-boss.getNode().getBoundsInLocal().getHeight() - 20);
                boss.getNode().setVisible(true);
            }
        } else {
            System.err.println("BossController.returnBossForNextPhase(): Boss entity is NULL!");
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

    private void updateBossAppearance() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null) return;
        Rectangle bossRect = (Rectangle) boss.getNode();
        Color phaseColor = switch (bossPhase) {
            case 1 -> Color.rgb(100, 0, 0);
            case 2 -> Color.rgb(100, 0, 100);
            case 3 -> Color.rgb(80, 0, 80, 0.9);
            default -> Color.DARKRED;
        };
        bossRect.setFill(phaseColor);
    }
    private void flashBoss() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null) return;
        Rectangle bossRect = (Rectangle) boss.getNode();
        bossRect.setFill(Color.WHITE);
        PauseTransition flash = new PauseTransition(Duration.millis(80));
        flash.setOnFinished(e -> {
            if (boss.isAlive()) {
                updateBossAppearance();
            } else {
                updateBossAppearance();
            }
        });
        flash.play();
    }
    private void changeMovementState(BossMovementState newState, long now) {
        currentMovementState = newState;
        movementStateStartTime = now;
        if (newState == BossMovementState.DIVE_ATTACK) {
            this.isBossDivingDown = true;
        }
    }

    public void clearAllBossProjectiles() {
        Iterator<Rectangle> it = bossProjectiles.iterator();
        while(it.hasNext()){
            Rectangle p = it.next();
            entityManager.getGamePane().getChildren().remove(p);
            it.remove();
        }
    }
    public void clearAllMinions() {
        Iterator<Enemy> it = minionEnemies.iterator();
        while(it.hasNext()){
            Enemy m = it.next();
            if(m.getNode() != null) entityManager.getGamePane().getChildren().remove(m.getNode());
            it.remove();
        }
    }
    public boolean checkBossProjectileCollisions(Player player) {
        if (player == null || player.getNode() == null) return false;
        Iterator<Rectangle> iterator = bossProjectiles.iterator();
        while (iterator.hasNext()) {
            Rectangle projectile = iterator.next();
            if (projectile.getBoundsInParent().intersects(player.getNode().getBoundsInParent())) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
                return true;
            }
        }
        return false;
    }
    public boolean checkPlayerProjectileVsMinionCollisions(List<Rectangle> playerProjectiles, UIManager uiManager) {
        boolean hitDetectedOverall = false;
        Iterator<Rectangle> projIterator = playerProjectiles.iterator();
        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            Iterator<Enemy> minionIterator = minionEnemies.iterator();
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
        if(boss != null && boss.getNode() != null) {
            boss.getNode().setLayoutY(-boss.getNode().getBoundsInLocal().getHeight() - 20);
        }
    }

    public List<Rectangle> getBossProjectiles() { return bossProjectiles; }
    public List<Enemy> getMinionEnemies() { return minionEnemies; }
    public int getBossPhase() { return bossPhase; }
    public boolean isBossRetreating() { return bossIsRetreating; }
}