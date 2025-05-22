package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
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

    // Boss Status
    private int bossPhase = 1; // 1, 2, oder 3
    private int bossMaxHealth = GameDimensions.BOSS_HEALTH;
    private boolean bossIsRetreating = false;
    private boolean bossIsOffScreen = false;
    private long lastBossMovementTime = 0;
    private long lastBossShootTime = 0;
    private long lastMinionSpawnTime = 0;

    // Boss Movement Pattern
    private double bossTargetX = 0;
    private double bossTargetY = 0;
    private BossMovementState currentMovementState = BossMovementState.ENTERING;
    private long movementStateStartTime = 0;
    private Random random = new Random();

    // Boss Projectiles
    private List<Rectangle> bossProjectiles = new ArrayList<>();

    // Minion enemies during retreat
    private List<Enemy> minionEnemies = new ArrayList<>();
    private MinionWaveType currentMinionWaveType = MinionWaveType.DIAGONAL_SWEEP;
    private int minionWaveCount = 0;
    private long minionWaveStartTime = 0;

    private enum BossMovementState {
        ENTERING,      // Boss fliegt von oben ein
        HOVERING,      // Boss schwebt und schießt
        SIDE_STRAFE,   // Boss fliegt seitlich hin und her
        DIVE_ATTACK,   // Boss stürzt nach unten
        RETREATING,    // Boss zieht sich zurück
        OFF_SCREEN     // Boss ist außerhalb des Bildschirms
    }

    private enum MinionWaveType {
        DIAGONAL_SWEEP,    // Gegner fliegen diagonal über den Bildschirm
        FORMATION_ATTACK,  // Gegner in V-Formation
        SWARM_ATTACK,      // Viele kleine Gegner chaotisch
        BOUNCING_PATTERN   // Gegner springen/hüpfen nach unten
    }

    public BossController(GameEntityManager entityManager, GameDimensions gameDimensions, UIManager uiManager) {
        this.entityManager = entityManager;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
    }

    public void initializeBoss() {
        bossPhase = 1;
        bossIsRetreating = false;
        bossIsOffScreen = false;
        currentMovementState = BossMovementState.ENTERING;
        movementStateStartTime = System.nanoTime();

        // Boss startet oberhalb des Bildschirms
        Enemy boss = entityManager.getBossEnemy();
        if (boss != null && boss.getNode() != null) {
            boss.getNode().setLayoutX(gameDimensions.getWidth() / 2 - boss.getNode().getBoundsInLocal().getWidth() / 2);
            boss.getNode().setLayoutY(-boss.getNode().getBoundsInLocal().getHeight());

            // Boss-Farbe je nach Phase
            updateBossAppearance();
        }
    }

    public void updateBossMovement(long now, double deltaTime) {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null || !entityManager.isBossActive()) {
            return;
        }

        if (bossIsRetreating) {
            updateRetreatMovement(now, deltaTime, boss);
            updateMinionWaves(now, deltaTime);
        } else {
            updateActiveBossMovement(now, deltaTime, boss);
            updateBossShooting(now, boss);
        }

        updateBossProjectiles(deltaTime);
    }

    private void updateActiveBossMovement(long now, double deltaTime, Enemy boss) {
        Node bossNode = boss.getNode();
        long timeSinceStateStart = now - movementStateStartTime;

        switch (currentMovementState) {
            case ENTERING:
                // Boss fliegt langsam von oben ein
                double entrySpeed = gameDimensions.getHeight() * 0.001;
                double targetEntryY = gameDimensions.getHeight() * 0.15;

                if (bossNode.getLayoutY() < targetEntryY) {
                    bossNode.setLayoutY(bossNode.getLayoutY() + entrySpeed * deltaTime * 60);
                } else {
                    changeMovementState(BossMovementState.HOVERING, now);
                }
                break;

            case HOVERING:
                // Boss schwebt leicht auf und ab
                double hoverAmplitude = 20;
                double hoverSpeed = 0.002;
                double baseY = gameDimensions.getHeight() * 0.15;
                double hoverOffset = Math.sin(timeSinceStateStart * hoverSpeed) * hoverAmplitude;
                bossNode.setLayoutY(baseY + hoverOffset);

                // Nach 3-5 Sekunden wechseln zu seitlicher Bewegung
                if (timeSinceStateStart > 3_000_000_000L + random.nextInt(2_000_000_000)) {
                    changeMovementState(BossMovementState.SIDE_STRAFE, now);
                }
                break;

            case SIDE_STRAFE:
                // Boss fliegt seitlich hin und her
                double strafeSpeed = gameDimensions.getWidth() * 0.002;
                double centerX = gameDimensions.getWidth() / 2;
                double strafeRange = gameDimensions.getWidth() * 0.3;

                double strafeX = centerX + Math.sin(timeSinceStateStart * 0.000000002) * strafeRange;
                bossNode.setLayoutX(strafeX - bossNode.getBoundsInLocal().getWidth() / 2);

                // Nach 4-6 Sekunden zu Sturzangriff wechseln
                if (timeSinceStateStart > 4_000_000_000L + random.nextInt(2_000_000_000)) {
                    bossTargetY = gameDimensions.getHeight() * 0.4; // Tiefer hinunter
                    changeMovementState(BossMovementState.DIVE_ATTACK, now);
                }
                break;

            case DIVE_ATTACK:
                // Boss stürzt nach unten und wieder hoch
                double diveSpeed = gameDimensions.getHeight() * 0.003;

                if (bossNode.getLayoutY() < bossTargetY) {
                    // Nach unten
                    bossNode.setLayoutY(bossNode.getLayoutY() + diveSpeed * deltaTime * 60);
                } else {
                    // Wieder nach oben
                    bossNode.setLayoutY(bossNode.getLayoutY() - diveSpeed * deltaTime * 60);
                    if (bossNode.getLayoutY() <= gameDimensions.getHeight() * 0.15) {
                        changeMovementState(BossMovementState.HOVERING, now);
                    }
                }
                break;
        }
    }

    private void updateRetreatMovement(long now, double deltaTime, Enemy boss) {
        Node bossNode = boss.getNode();

        if (!bossIsOffScreen) {
            // Boss fliegt nach oben aus dem Bildschirm
            double retreatSpeed = gameDimensions.getHeight() * 0.002;
            bossNode.setLayoutY(bossNode.getLayoutY() - retreatSpeed * deltaTime * 60);

            if (bossNode.getLayoutY() + bossNode.getBoundsInLocal().getHeight() < 0) {
                bossIsOffScreen = true;
                startMinionWave();
            }
        } else {
            // Boss ist außerhalb - prüfe ob alle Minions besiegt sind
            if (minionEnemies.isEmpty() && !entityManager.isLoadingNextWave()) {
                // Boss kehrt zurück für nächste Phase
                returnBossForNextPhase(now);
            }
        }
    }

    private void updateBossShooting(long now, Enemy boss) {
        // Boss schießt je nach Phase unterschiedlich oft
        long shootCooldown = switch (bossPhase) {
            case 1 -> 1_500_000_000L; // 1.5 Sekunden
            case 2 -> 1_000_000_000L; // 1 Sekunde
            case 3 -> 700_000_000L;   // 0.7 Sekunden
            default -> 1_500_000_000L;
        };

        if (now - lastBossShootTime > shootCooldown) {
            shootBossProjectile(boss);
            lastBossShootTime = now;
        }
    }

    private void shootBossProjectile(Enemy boss) {
        Node bossNode = boss.getNode();
        Player player = entityManager.getPlayer();

        if (player == null || player.getNode() == null) return;

        // Schuss-Pattern je nach Phase
        switch (bossPhase) {
            case 1:
                // Einzelschuss zum Spieler
                createBossProjectile(
                        bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() / 2,
                        bossNode.getLayoutY() + bossNode.getBoundsInLocal().getHeight(),
                        player.getX() + player.getWidth() / 2,
                        player.getY()
                );
                break;

            case 2:
                // Doppelschuss
                double bossLeftEdge = bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() * 0.25;
                double bossRightEdge = bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() * 0.75;
                double bossBottom = bossNode.getLayoutY() + bossNode.getBoundsInLocal().getHeight();

                createBossProjectile(bossLeftEdge, bossBottom, player.getX(), player.getY());
                createBossProjectile(bossRightEdge, bossBottom, player.getX() + player.getWidth(), player.getY());
                break;

            case 3:
                // Streuschuss (3 Projektile)
                double centerX = bossNode.getLayoutX() + bossNode.getBoundsInLocal().getWidth() / 2;
                double centerY = bossNode.getLayoutY() + bossNode.getBoundsInLocal().getHeight();

                createBossProjectile(centerX, centerY, player.getX() - 50, player.getY());
                createBossProjectile(centerX, centerY, player.getX() + player.getWidth() / 2, player.getY());
                createBossProjectile(centerX, centerY, player.getX() + player.getWidth() + 50, player.getY());
                break;
        }
    }

    private void createBossProjectile(double startX, double startY, double targetX, double targetY) {
        Rectangle projectile = new Rectangle(
                gameDimensions.getProjectileWidth() * 1.5,
                gameDimensions.getProjectileHeight() * 1.5
        );
        projectile.setFill(Color.RED);
        projectile.setLayoutX(startX - projectile.getWidth() / 2);
        projectile.setLayoutY(startY);

        // Berechne Richtung zum Ziel
        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            projectile.setUserData(new double[]{
                    dx / distance * gameDimensions.getProjectileSpeed() * 0.8, // vx
                    dy / distance * gameDimensions.getProjectileSpeed() * 0.8  // vy
            });
        } else {
            projectile.setUserData(new double[]{0, gameDimensions.getProjectileSpeed() * 0.8});
        }

        bossProjectiles.add(projectile);
        entityManager.getGamePane().getChildren().add(projectile);
    }

    private void updateBossProjectiles(double deltaTime) {
        Iterator<Rectangle> iterator = bossProjectiles.iterator();
        while (iterator.hasNext()) {
            Rectangle projectile = iterator.next();
            double[] velocity = (double[]) projectile.getUserData();

            projectile.setLayoutX(projectile.getLayoutX() + velocity[0] * deltaTime * 60);
            projectile.setLayoutY(projectile.getLayoutY() + velocity[1] * deltaTime * 60);

            // Entferne Projektile, die den Bildschirm verlassen haben
            if (projectile.getLayoutY() > gameDimensions.getHeight() + 50 ||
                    projectile.getLayoutX() < -50 ||
                    projectile.getLayoutX() > gameDimensions.getWidth() + 50) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
            }
        }
    }

    public void bossTakeHit() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || bossIsRetreating) return;

        boss.takeHit();

        if (!boss.isAlive()) {
            if (bossPhase < 3) {
                // Boss "stirbt" aber wird in nächster Phase zurückkehren
                startBossRetreat();
            } else {
                // Boss ist endgültig besiegt
                entityManager.bossDefeated();
                clearAllBossProjectiles();
                clearAllMinions();
            }
        }

        // Visual feedback
        flashBoss();
    }

    private void startBossRetreat() {
        bossIsRetreating = true;
        bossIsOffScreen = false;
        currentMovementState = BossMovementState.RETREATING;

        String message = switch (bossPhase) {
            case 1 -> "Boss retreating... Wave 1 incoming!";
            case 2 -> "Boss retreating... Final wave incoming!";
            default -> "Boss retreating...";
        };

        uiManager.showPopupMessage(message, 2.0);
    }

    private void startMinionWave() {
        minionWaveCount++;
        minionWaveStartTime = System.nanoTime();

        // Wähle Minion-Wave-Type basierend auf Boss-Phase
        currentMinionWaveType = switch (bossPhase) {
            case 1 -> (minionWaveCount % 2 == 1) ? MinionWaveType.DIAGONAL_SWEEP : MinionWaveType.FORMATION_ATTACK;
            case 2 -> (minionWaveCount % 2 == 1) ? MinionWaveType.SWARM_ATTACK : MinionWaveType.BOUNCING_PATTERN;
            default -> MinionWaveType.DIAGONAL_SWEEP;
        };

        spawnMinionWave(currentMinionWaveType);
    }

    private void spawnMinionWave(MinionWaveType waveType) {
        switch (waveType) {
            case DIAGONAL_SWEEP:
                spawnDiagonalSweepMinions();
                break;
            case FORMATION_ATTACK:
                spawnFormationMinions();
                break;
            case SWARM_ATTACK:
                spawnSwarmMinions();
                break;
            case BOUNCING_PATTERN:
                spawnBouncingMinions();
                break;
        }
    }

    private void spawnDiagonalSweepMinions() {
        int minionCount = 6;
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(
                    gameDimensions.getEnemyWidth() * 0.7,
                    gameDimensions.getEnemyHeight() * 0.7
            );
            minionShape.setFill(Color.ORANGE);

            // Minions starten links oder rechts außerhalb des Bildschirms
            boolean fromLeft = i % 2 == 0;
            double startX = fromLeft ? -minionShape.getWidth() : gameDimensions.getWidth();
            double startY = -minionShape.getHeight() - (i * 40);

            minionShape.setLayoutX(startX);
            minionShape.setLayoutY(startY);

            // Setze Bewegungsrichtung als UserData
            double targetX = fromLeft ? gameDimensions.getWidth() + 100 : -100;
            double targetY = gameDimensions.getHeight() * 0.7;
            minionShape.setUserData(new double[]{targetX, targetY, fromLeft ? 1 : -1}); // target_x, target_y, direction

            Enemy minion = new Enemy(minionShape, 1, GameDimensions.POINTS_PER_ENEMY / 2);
            minionEnemies.add(minion);
            entityManager.getGamePane().getChildren().add(minionShape);
        }
    }

    private void spawnFormationMinions() {
        int minionCount = 5;
        double formationWidth = gameDimensions.getWidth() * 0.6;
        double startX = (gameDimensions.getWidth() - formationWidth) / 2;

        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(
                    gameDimensions.getEnemyWidth() * 0.8,
                    gameDimensions.getEnemyHeight() * 0.8
            );
            minionShape.setFill(Color.YELLOW);

            double x = startX + (i * formationWidth / (minionCount - 1));
            double y = -minionShape.getHeight() - (Math.abs(i - 2) * 30); // V-Formation

            minionShape.setLayoutX(x);
            minionShape.setLayoutY(y);
            minionShape.setUserData(new double[]{0, 1}); // straight down movement

            Enemy minion = new Enemy(minionShape, 1, GameDimensions.POINTS_PER_ENEMY / 2);
            minionEnemies.add(minion);
            entityManager.getGamePane().getChildren().add(minionShape);
        }
    }

    private void spawnSwarmMinions() {
        int minionCount = 12;
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(
                    gameDimensions.getEnemyWidth() * 0.5,
                    gameDimensions.getEnemyHeight() * 0.5
            );
            minionShape.setFill(Color.LIGHTCORAL);

            double x = random.nextDouble() * gameDimensions.getWidth();
            double y = -minionShape.getHeight() - (random.nextDouble() * 200);

            minionShape.setLayoutX(x);
            minionShape.setLayoutY(y);

            // Chaotische Bewegung
            double vx = (random.nextDouble() - 0.5) * 2;
            double vy = 0.5 + random.nextDouble() * 1.5;
            minionShape.setUserData(new double[]{vx, vy, 0}); // vx, vy, time

            Enemy minion = new Enemy(minionShape, 1, GameDimensions.POINTS_PER_ENEMY / 3);
            minionEnemies.add(minion);
            entityManager.getGamePane().getChildren().add(minionShape);
        }
    }

    private void spawnBouncingMinions() {
        int minionCount = 4;
        for (int i = 0; i < minionCount; i++) {
            Rectangle minionShape = new Rectangle(
                    gameDimensions.getEnemyWidth(),
                    gameDimensions.getEnemyHeight()
            );
            minionShape.setFill(Color.LIGHTGREEN);

            double x = (i + 1) * gameDimensions.getWidth() / (minionCount + 1);
            double y = -minionShape.getHeight();

            minionShape.setLayoutX(x);
            minionShape.setLayoutY(y);
            minionShape.setUserData(new double[]{0, 0, i * 0.5}); // bounce_offset, bounce_speed, phase_offset

            Enemy minion = new Enemy(minionShape, 2, GameDimensions.POINTS_PER_ENEMY);
            minionEnemies.add(minion);
            entityManager.getGamePane().getChildren().add(minionShape);
        }
    }

    private void updateMinionWaves(long now, double deltaTime) {
        Iterator<Enemy> iterator = minionEnemies.iterator();
        double currentTime = (now - minionWaveStartTime) / 1_000_000_000.0;

        while (iterator.hasNext()) {
            Enemy minion = iterator.next();
            Node minionNode = minion.getNode();
            if (minionNode == null) {
                iterator.remove();
                continue;
            }

            double[] userData = (double[]) minionNode.getUserData();
            boolean shouldRemove = false;

            switch (currentMinionWaveType) {
                case DIAGONAL_SWEEP:
                    // Diagonal movement
                    double targetX = userData[0];
                    double targetY = userData[1];
                    double direction = userData[2];

                    double speed = gameDimensions.getWidth() * 0.002;
                    minionNode.setLayoutX(minionNode.getLayoutX() + direction * speed * deltaTime * 60);
                    minionNode.setLayoutY(minionNode.getLayoutY() + speed * 0.5 * deltaTime * 60);

                    if (minionNode.getLayoutY() > gameDimensions.getHeight() ||
                            minionNode.getLayoutX() < -100 || minionNode.getLayoutX() > gameDimensions.getWidth() + 100) {
                        shouldRemove = true;
                    }
                    break;

                case FORMATION_ATTACK:
                    // Straight down movement
                    double downSpeed = gameDimensions.getHeight() * 0.001;
                    minionNode.setLayoutY(minionNode.getLayoutY() + downSpeed * deltaTime * 60);

                    if (minionNode.getLayoutY() > gameDimensions.getHeight()) {
                        shouldRemove = true;
                    }
                    break;

                case SWARM_ATTACK:
                    // Chaotic movement
                    double vx = userData[0];
                    double vy = userData[1];
                    userData[2] += deltaTime; // time

                    // Add some randomness over time
                    vx += (random.nextDouble() - 0.5) * 0.1;
                    vy += (random.nextDouble() - 0.5) * 0.05;

                    // Clamp values
                    vx = Math.max(-3, Math.min(3, vx));
                    vy = Math.max(0.2, Math.min(3, vy));

                    userData[0] = vx;
                    userData[1] = vy;

                    minionNode.setLayoutX(minionNode.getLayoutX() + vx * gameDimensions.getWidth() * 0.001 * deltaTime * 60);
                    minionNode.setLayoutY(minionNode.getLayoutY() + vy * gameDimensions.getHeight() * 0.001 * deltaTime * 60);

                    // Bounce off walls
                    if (minionNode.getLayoutX() < 0 || minionNode.getLayoutX() > gameDimensions.getWidth() - minionNode.getBoundsInLocal().getWidth()) {
                        userData[0] = -vx;
                    }

                    if (minionNode.getLayoutY() > gameDimensions.getHeight()) {
                        shouldRemove = true;
                    }
                    break;

                case BOUNCING_PATTERN:
                    // Bouncing movement
                    double phaseOffset = userData[2];
                    double bounceAmplitude = 100;
                    double bounceFreq = 2.0;

                    double bounceX = Math.sin((currentTime + phaseOffset) * bounceFreq) * bounceAmplitude;
                    double baseSpeed = gameDimensions.getHeight() * 0.0008;

                    minionNode.setLayoutY(minionNode.getLayoutY() + baseSpeed * deltaTime * 60);

                    // Update bounce position but keep within screen bounds
                    double newX = (gameDimensions.getWidth() / 2) + bounceX - (minionNode.getBoundsInLocal().getWidth() / 2);
                    newX = Math.max(0, Math.min(newX, gameDimensions.getWidth() - minionNode.getBoundsInLocal().getWidth()));
                    minionNode.setLayoutX(newX);

                    if (minionNode.getLayoutY() > gameDimensions.getHeight()) {
                        shouldRemove = true;
                    }
                    break;
            }

            if (shouldRemove) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(minionNode);
            }
        }
    }

    private void returnBossForNextPhase(long now) {
        bossPhase++;
        bossIsRetreating = false;
        bossIsOffScreen = false;

        Enemy boss = entityManager.getBossEnemy();
        if (boss != null) {
            // Boss kehrt mit voller Gesundheit zurück
            // Setze Gesundheit zurück (hack: erstelle neuen Boss mit mehr Gesundheit)
            entityManager.resetBossHealth(bossMaxHealth + (bossPhase - 1) * 3);
        }

        // Boss startet wieder oberhalb des Bildschirms
        currentMovementState = BossMovementState.ENTERING;
        movementStateStartTime = now;

        updateBossAppearance();

        String phaseMessage = switch (bossPhase) {
            case 2 -> "Boss Phase 2 - Increased Aggression!";
            case 3 -> "Boss Phase 3 - Final Form!";
            default -> "Boss returned!";
        };

        uiManager.showPopupMessage(phaseMessage, 2.5);
    }

    private void updateBossAppearance() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null) return;

        Rectangle bossRect = (Rectangle) boss.getNode();
        Color phaseColor = switch (bossPhase) {
            case 1 -> Color.DARKRED;
            case 2 -> Color.PURPLE;
            case 3 -> Color.DARKMAGENTA;
            default -> Color.DARKRED;
        };

        bossRect.setFill(phaseColor);
    }

    private void flashBoss() {
        Enemy boss = entityManager.getBossEnemy();
        if (boss == null || boss.getNode() == null) return;

        Rectangle bossRect = (Rectangle) boss.getNode();
        Color originalColor = (Color) bossRect.getFill();

        bossRect.setFill(Color.WHITE);

        PauseTransition flash = new PauseTransition(Duration.millis(100));
        flash.setOnFinished(e -> bossRect.setFill(originalColor));
        flash.play();
    }

    private void changeMovementState(BossMovementState newState, long now) {
        currentMovementState = newState;
        movementStateStartTime = now;
    }

    private void clearAllBossProjectiles() {
        for (Rectangle projectile : bossProjectiles) {
            entityManager.getGamePane().getChildren().remove(projectile);
        }
        bossProjectiles.clear();
    }

    private void clearAllMinions() {
        for (Enemy minion : minionEnemies) {
            if (minion.getNode() != null) {
                entityManager.getGamePane().getChildren().remove(minion.getNode());
            }
        }
        minionEnemies.clear();
    }

    public boolean checkBossProjectileCollisions(Player player) {
        if (player == null || player.getNode() == null) return false;

        Iterator<Rectangle> iterator = bossProjectiles.iterator();
        while (iterator.hasNext()) {
            Rectangle projectile = iterator.next();
            if (projectile.getBoundsInParent().intersects(player.getNode().getBoundsInParent())) {
                iterator.remove();
                entityManager.getGamePane().getChildren().remove(projectile);
                return true; // Hit detected
            }
        }
        return false;
    }

    public boolean checkPlayerProjectileVsMinionCollisions(List<Rectangle> playerProjectiles, UIManager uiManager) {
        boolean hitDetected = false;

        Iterator<Rectangle> projIterator = playerProjectiles.iterator();
        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();

            Iterator<Enemy> minionIterator = minionEnemies.iterator();
            while (minionIterator.hasNext()) {
                Enemy minion = minionIterator.next();
                if (minion.getNode() != null &&
                        projectile.getBoundsInParent().intersects(minion.getNode().getBoundsInParent())) {

                    minion.takeHit();
                    if (!minion.isAlive()) {
                        minionIterator.remove();
                        entityManager.getGamePane().getChildren().remove(minion.getNode());
                        uiManager.addScore(minion.getPoints());
                    }

                    projIterator.remove();
                    entityManager.getGamePane().getChildren().remove(projectile);
                    hitDetected = true;
                    break;
                }
            }
        }
        return hitDetected;
    }

    public boolean checkPlayerVsMinionCollisions(Player player) {
        if (player == null || player.getNode() == null) return false;

        for (Enemy minion : minionEnemies) {
            if (minion.getNode() != null &&
                    player.getNode().getBoundsInParent().intersects(minion.getNode().getBoundsInParent())) {
                return true; // Collision detected
            }
        }
        return false;
    }

    public void resetBoss() {
        bossPhase = 1;
        bossIsRetreating = false;
        bossIsOffScreen = false;
        currentMovementState = BossMovementState.ENTERING;
        minionWaveCount = 0;

        clearAllBossProjectiles();
        clearAllMinions();
    }

    public List<Rectangle> getBossProjectiles() {
        return bossProjectiles;
    }

    public List<Enemy> getMinionEnemies() {
        return minionEnemies;
    }

    public int getBossPhase() {
        return bossPhase;
    }

    public Pane getGamePane() {
        return gamePane;
    }
}