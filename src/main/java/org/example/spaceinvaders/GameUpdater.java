package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameUpdater {
    private GameEntityManager entityManager;
    private InputHandler inputHandler;
    private GameDimensions gameDimensions;
    private UIManager uiManager;
    private MusicalInvaders mainApp;
    private VoiceProfile activeVoiceProfile;
    private long lastShotTime = 0;

    private double waveTime = 0; // For wave-specific time-based patterns
    private long lastEnemyMoveTimeWave2 = 0;
    private int lastProcessedWaveForTimeReset = 0; // To reset waveTime only once per wave

    // Store initial positions for Wave 3 (Formation Movement)
    private List<Double> wave3InitialX = new ArrayList<>();
    private List<Double> wave3InitialY = new ArrayList<>();
    private boolean wave3Initialized = false;


    public GameUpdater(GameEntityManager entityManager, InputHandler inputHandler,
                       GameDimensions gameDimensions, UIManager uiManager, MusicalInvaders mainApp) {
        this.entityManager = entityManager;
        this.inputHandler = inputHandler;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
        this.mainApp = mainApp;
        if (this.mainApp != null) {
            this.activeVoiceProfile = this.mainApp.getSelectedVoiceProfile();
        }
    }

    public void update(long now, double deltaTime) {
        if (mainApp.getCurrentGameState() != GameState.PLAYING) return;

        updatePlayer(now);
        handlePlayerShooting(now);
        updateProjectiles();

        BossController bossController = entityManager.getBossController();

        if (entityManager.isBossActive() && bossController != null) {
            wave3Initialized = false; // Reset wave 3 init if boss becomes active
            bossController.updateBossMovement(now, deltaTime);
        } else if (!entityManager.getEnemies().isEmpty()) {
            updateEnemyMovement(deltaTime, now);
        } else if (entityManager.getEnemies().isEmpty() && !entityManager.isBossActive() && !entityManager.isLoadingNextWave() && !entityManager.wasBossJustDefeated()) {
            wave3Initialized = false; // Reset before spawning next wave
            entityManager.spawnNextWaveOrBoss();
        }

        checkCollisions(now);

        if (entityManager.wasBossJustDefeated() && entityManager.getBossEnemy() == null) {
            if (mainApp.getCurrentGameState() == GameState.PLAYING) {
                uiManager.showBossDefeatedMessage();
                PauseTransition delay = new PauseTransition(Duration.seconds(3));
                delay.setOnFinished(event -> mainApp.changeGameState(GameState.CREDITS));
                delay.play();
            }
        }
    }

    private void updatePlayer(long now) {
        Player player = entityManager.getPlayer();
        if (player == null) return;
        double dx = 0;
        double effectiveSpeed = gameDimensions.getPlayerSpeed();
        if (inputHandler.isMoveLeftPressed()) dx -= effectiveSpeed;
        if (inputHandler.isMoveRightPressed()) dx += effectiveSpeed;
        player.move(dx);
    }
    private void handlePlayerShooting(long now) {
        if (entityManager.getPlayer() == null) return;
        if (inputHandler.isShootingPressed() && (now - lastShotTime) / 1_000_000 >= GameDimensions.SHOOT_COOLDOWN_MS) {
            entityManager.createProjectile();
            lastShotTime = now;
            playSoundEffect("player_shoot.wav");
        }
    }
    private void updateProjectiles() {
        Iterator<Rectangle> iterator = entityManager.getPlayerProjectiles().iterator();
        while (iterator.hasNext()) {
            Rectangle p = iterator.next();
            p.setY(p.getY() - gameDimensions.getProjectileSpeed());
            if (p.getY() + p.getHeight() < 0) {
                iterator.remove();
                entityManager.removeProjectileNode(p);
            }
        }
    }

    private void updateEnemyMovement(double deltaTime, long now) {
        if (entityManager.isBossActive() || entityManager.getEnemies().isEmpty()) return;

        int currentWave = entityManager.getCurrentWaveNumber();

        if (currentWave != lastProcessedWaveForTimeReset) {
            waveTime = 0;
            lastProcessedWaveForTimeReset = currentWave;
            wave3Initialized = false; // Reset for new wave
            System.out.println("GameUpdater: Resetting waveTime and wave3Initialized for new wave: " + currentWave);
        }

        switch (currentWave % 3) {
            case 1:
                updateBasicMovement(deltaTime);
                break;
            case 2:
                updateAcceleratingMovement(deltaTime, now);
                break;
            case 0:
            default:
                updateFormationMovement(deltaTime, now);
                break;
        }
    }

    private void updateBasicMovement(double deltaTime) {
        List<Enemy> currentEnemies = entityManager.getEnemies(); if (currentEnemies.isEmpty()) return;
        double currentDirection = entityManager.getEnemyMovementDirection();
        double effectiveSpeedX = entityManager.getEnemyGroupSpeedX(); double effectiveSpeedY = entityManager.getEnemyGroupSpeedY();
        boolean reverseDirectionAndMoveDown = false; double groupLeftMost = Double.MAX_VALUE; double groupRightMost = Double.MIN_VALUE;
        for (Enemy enemy : currentEnemies) { Node enemyNode = enemy.getNode(); if (enemyNode == null) continue;
            groupLeftMost = Math.min(groupLeftMost, enemyNode.getLayoutX());
            groupRightMost = Math.max(groupRightMost, enemyNode.getLayoutX() + enemyNode.getBoundsInLocal().getWidth());
        }
        if (currentDirection > 0 && groupRightMost + effectiveSpeedX * deltaTime * 60 > gameDimensions.getWidth()) { // Apply deltaTime
            reverseDirectionAndMoveDown = true;
        } else if (currentDirection < 0 && groupLeftMost + effectiveSpeedX * currentDirection * deltaTime * 60 < 0) { // Apply deltaTime and direction
            reverseDirectionAndMoveDown = true;
        }

        double dx = effectiveSpeedX * currentDirection * deltaTime * 60; // Apply deltaTime
        double dy = 0;

        if (reverseDirectionAndMoveDown) {
            entityManager.setEnemyMovementDirection(currentDirection * -1);
            dx = 0; // No horizontal movement on the turn frame
            dy = effectiveSpeedY * deltaTime * 60; // Apply deltaTime
        }

        for (Enemy enemy : currentEnemies) { Node enemyNode = enemy.getNode(); if (enemyNode == null) continue;
            enemyNode.setLayoutX(enemyNode.getLayoutX() + dx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + dy);
            if (enemyNode.getLayoutY() + enemyNode.getBoundsInLocal().getHeight() >= gameDimensions.getHeight() - (gameDimensions.getPlayerHeight() * 0.8)) {
                if (mainApp.getCurrentGameState() == GameState.PLAYING) mainApp.triggerGameOver();
                return;
            }
        }
    }
    private void updateAcceleratingMovement(double deltaTime, long now) {
        List<Enemy> currentEnemies = entityManager.getEnemies(); if (currentEnemies.isEmpty()) return;
        int totalEnemiesAtStart = GameDimensions.ENEMIES_PER_ROW * GameDimensions.ENEMY_ROWS;
        int remainingEnemies = currentEnemies.size();
        double speedMultiplier =(double) totalEnemiesAtStart /remainingEnemies;
        speedMultiplier = Math.min(speedMultiplier, 4.0); // Cap speed multiplier

        // The step interval should be dependent on deltaTime to ensure smooth movement across different frame rates
        // However, this pattern is inherently step-based. We keep the stepIntervalNanos for the "tick"
        // but apply movement more smoothly if possible, or just use the tick.
        // For simplicity, let's keep it tick-based for now as it was.
        long stepIntervalNanos = (long) (600_000_000 / speedMultiplier);

        if (now - lastEnemyMoveTimeWave2 < stepIntervalNanos) return;
        lastEnemyMoveTimeWave2 = now;

        double currentDirection = entityManager.getEnemyMovementDirection();
        // Step sizes are fixed per "tick"
        double stepSizeX = entityManager.getEnemyGroupSpeedX() * 8 * speedMultiplier; // Größere Schritte
        double stepSizeY = entityManager.getEnemyGroupSpeedY() * 1.5;

        boolean reverseDirectionAndMoveDown = false;
        double groupLeftMost = Double.MAX_VALUE;
        double groupRightMost = Double.MIN_VALUE;

        for (Enemy enemy : currentEnemies) { Node enemyNode = enemy.getNode(); if (enemyNode == null) continue;
            groupLeftMost = Math.min(groupLeftMost, enemyNode.getLayoutX());
            groupRightMost = Math.max(groupRightMost, enemyNode.getLayoutX() + enemyNode.getBoundsInLocal().getWidth());
        }

        if (currentDirection > 0 && groupRightMost + stepSizeX > gameDimensions.getWidth()) {
            reverseDirectionAndMoveDown = true;
        } else if (currentDirection < 0 && groupLeftMost - stepSizeX < 0) {
            reverseDirectionAndMoveDown = true;
        }

        double dx = 0;
        double dy = 0;

        if (reverseDirectionAndMoveDown) {
            entityManager.setEnemyMovementDirection(currentDirection * -1);
            dy = stepSizeY; // Move down on the turn
        } else {
            dx = stepSizeX * currentDirection; // Move sideways
        }

        for (Enemy enemy : currentEnemies) { Node enemyNode = enemy.getNode(); if (enemyNode == null) continue;
            enemyNode.setLayoutX(enemyNode.getLayoutX() + dx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + dy);
            if (enemyNode.getLayoutY() + enemyNode.getBoundsInLocal().getHeight() >= gameDimensions.getHeight() - (gameDimensions.getPlayerHeight() * 0.8)) {
                if (mainApp.getCurrentGameState() == GameState.PLAYING) mainApp.triggerGameOver();
                return;
            }
        }
    }

    private void updateFormationMovement(double deltaTime, long now) {
        waveTime += deltaTime * 2.5; // Wellenbewegungszeit

        List<Enemy> currentEnemies = entityManager.getEnemies();
        if (currentEnemies.isEmpty()) return;

        // Konstante Abwärtsbewegung für alle Gegner
        double constantDownwardSpeed = entityManager.getEnemyGroupSpeedY() * 0.02;

        // Wellenbewegung für horizontale Position
        final double WAVE_AMPLITUDE = 200.0; // Wie stark die Welle ausschlägt
        final double WAVE_FREQUENCY = 3;   // Wie schnell die Welle oszilliert

        int enemyIndex = 0;
        int totalEnemiesPerRow = GameDimensions.ENEMIES_PER_ROW;

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            // Berechne Reihe und Spalte des Gegners
            int row = enemyIndex / totalEnemiesPerRow;
            int col = enemyIndex % totalEnemiesPerRow;

            // Jede Reihe hat eine etwas andere Wellenphase
            double rowPhaseOffset = row * 0.8;
            double colPhaseOffset = col * 0.3;

            // Berechne die Wellenbewegung
            double waveOffset = Math.sin((waveTime + rowPhaseOffset + colPhaseOffset) * WAVE_FREQUENCY) * WAVE_AMPLITUDE;

            // Grundposition: Mittig im Bildschirm + Wellenversatz
            double centerX = gameDimensions.getWidth() / 2;
            double baseX = centerX - (totalEnemiesPerRow * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX())) / 2;
            double targetX = baseX + col * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX()) + waveOffset;

            // Begrenze die horizontale Bewegung auf den Bildschirm
            targetX = Math.max(10, Math.min(targetX, gameDimensions.getWidth() - gameDimensions.getEnemyWidth() - 10));

            // Setze neue Position
            enemyNode.setLayoutX(targetX);

            // Konstante Abwärtsbewegung
            double newY = enemyNode.getLayoutY() + (constantDownwardSpeed * deltaTime * 60);
            enemyNode.setLayoutY(newY);

            // Game Over Prüfung
            if (newY + enemyNode.getBoundsInLocal().getHeight() >=
                    gameDimensions.getHeight() - (gameDimensions.getPlayerHeight() * 0.8)) {
                mainApp.triggerGameOver();
                return;
            }

            enemyIndex++;
        }
    }



    private void checkCollisions(long now) {
        Player player = entityManager.getPlayer();
        if (player == null || player.getNode() == null) return;

        BossController bossController = entityManager.getBossController();
        Iterator<Rectangle> projIterator = entityManager.getPlayerProjectiles().iterator();

        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            boolean projectileRemovedInThisIteration = false;

            if (entityManager.isBossActive() && bossController != null && entityManager.getBossEnemy() != null) {
                Enemy currentBossEntity = entityManager.getBossEnemy();
                if (currentBossEntity.getNode() != null) {
                    boolean isIntersecting = projectile.getBoundsInParent().intersects(currentBossEntity.getNode().getBoundsInParent());
                    boolean isBossRetreating = bossController.isBossRetreating();

                    if (isIntersecting && !isBossRetreating) {
                        bossController.bossTakeHit();
                        playSoundEffect("enemy_hit.wav");

                        Enemy bossAfterHit = entityManager.getBossEnemy();
                        if (bossAfterHit != null && !bossAfterHit.isAlive() && bossController.getBossPhase() >= 3) {
                            uiManager.addScore(currentBossEntity.getPoints());
                            playSoundEffect("boss_explosion.wav");
                            entityManager.bossDefeated();
                        }
                        projIterator.remove();
                        entityManager.removeProjectileNode(projectile);
                        projectileRemovedInThisIteration = true;
                    }
                }

                if (!projectileRemovedInThisIteration && bossController.isBossRetreating() && !bossController.getMinionEnemies().isEmpty()) {
                    List<Rectangle> singleProjectileList = new ArrayList<>();
                    singleProjectileList.add(projectile);
                    if (bossController.checkPlayerProjectileVsMinionCollisions(singleProjectileList, uiManager)) {
                        playSoundEffect("enemy_hit.wav");
                        projIterator.remove(); // Projectile node removal handled by bossController
                        // projectileRemovedInThisIteration = true; // Already handled by the fact that projIterator.remove() is called
                    }
                }

            } else if (!projectileRemovedInThisIteration && !entityManager.getEnemies().isEmpty()) {
                Iterator<Enemy> enemyIterator = entityManager.getEnemies().iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (enemy.getNode() != null && projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                        enemy.takeHit(); playSoundEffect("enemy_hit.wav");
                        if (!enemy.isAlive()) {
                            enemyIterator.remove(); entityManager.removeEnemyNode(enemy.getNode());
                            uiManager.addScore(enemy.getPoints()); playSoundEffect("enemy_explosion.wav");
                        }
                        projIterator.remove(); entityManager.removeProjectileNode(projectile);
                        // projectileRemovedInThisIteration = true; // No need, break immediately
                        break; // Projectile is used, exit inner loop
                    }
                }
            }
        }

        if (entityManager.isBossActive() && bossController != null) {
            if (bossController.checkBossProjectileCollisions(player)) {
                handlePlayerDeath(); return;
            }
        }

        if (entityManager.isBossActive() && bossController != null && entityManager.getBossEnemy() != null) {
            Enemy currentBossEntity = entityManager.getBossEnemy();
            if (currentBossEntity.getNode() != null && !bossController.isBossRetreating() &&
                    player.getNode().getBoundsInParent().intersects(currentBossEntity.getNode().getBoundsInParent())) {
                handlePlayerDeath(); return;
            }
            if (bossController.isBossRetreating() && bossController.checkPlayerVsMinionCollisions(player)) {
                handlePlayerDeath(); return;
            }
        } else if (!entityManager.getEnemies().isEmpty()) {
            for (Enemy enemy : new ArrayList<>(entityManager.getEnemies())) { // Iterate copy for safety if player dies
                if (enemy.getNode() != null && player.getNode().getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                    handlePlayerDeath(); return;
                }
            }
        }
    }

    private void handlePlayerDeath() {
        Player player = entityManager.getPlayer(); if (player == null) return;
        if (mainApp.getCurrentGameState() == GameState.PLAYING) {
            playSoundEffect("player_explosion.wav");
            mainApp.triggerGameOver();
        }
    }
    private void playSoundEffect(String sfxFileName) {
        if (activeVoiceProfile != null && activeVoiceProfile.getSfxFolderPath() != null) {
            String sfxPath = activeVoiceProfile.getSfxFolderPath();
            if (!sfxPath.endsWith("/")) sfxPath += "/";
            String fullSfxPath = sfxPath.startsWith("/") ? sfxPath : "/" + sfxPath;
            fullSfxPath += sfxFileName;
            try {
                String resourceUrl = getClass().getResource(fullSfxPath).toExternalForm();
                if (resourceUrl == null) {
                    System.err.println("SFX Resource URL is null for: " + fullSfxPath);
                    return;
                }
                AudioClip clip = new AudioClip(resourceUrl);
                clip.play();
            } catch (NullPointerException npe) {
                System.err.println("SFX Resource not found (NullPointerException): " + fullSfxPath + " - " + npe.getMessage());
            } catch (Exception e) {
                System.err.println("SFX Error loading: " + fullSfxPath + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}