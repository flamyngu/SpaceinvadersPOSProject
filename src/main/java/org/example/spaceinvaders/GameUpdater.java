package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
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
    private SoundManager soundManager;
    private long lastShotTime = 0;

    private double waveTime = 0;
    private long lastEnemyMoveTimeWave2 = 0;
    private int lastProcessedWaveForTimeReset = 0;
    private boolean wave3Initialized = false;


    public GameUpdater(GameEntityManager entityManager, InputHandler inputHandler,
                       GameDimensions gameDimensions, UIManager uiManager, MusicalInvaders mainApp, SoundManager soundManager) {
        this.entityManager = entityManager;
        this.inputHandler = inputHandler;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
        this.mainApp = mainApp;
        this.soundManager = soundManager;
    }

    public void update(long now, double deltaTime) {
        if (mainApp.getCurrentGameState() != GameState.PLAYING) return;

        updatePlayer(now);
        handlePlayerShooting(now);
        updateProjectiles();

        BossController bossController = entityManager.getBossController();

        if (entityManager.isBossActive() && bossController != null) {
            wave3Initialized = false;
            bossController.updateBossMovement(now, deltaTime);
        } else if (!entityManager.getEnemies().isEmpty()) {
            updateEnemyMovement(deltaTime, now);
        } else if (entityManager.getEnemies().isEmpty() && !entityManager.isBossActive() && !entityManager.isLoadingNextWave() && !entityManager.wasBossJustDefeated()) {
            wave3Initialized = false;
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
        if (player == null) {
            return;
        }
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
            if (soundManager != null) {
                soundManager.playPlayerShoot();
            }
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
            wave3Initialized = false;
        }

        if (currentWave == 1 || (currentWave > 0 && currentWave % 3 == 1) ) {
            updateBasicMovementOriginal(deltaTime);
        } else if (currentWave > 0 && currentWave % 3 == 2) {
            updateAcceleratingMovementOriginal(now);
        } else if (currentWave > 0 && currentWave % 3 == 0) {
            updateFormationMovementOriginal(deltaTime, now);
        } else {
            updateBasicMovementOriginal(deltaTime);
        }
    }

    private void updateBasicMovementOriginal(double deltaTime) {
        List<Enemy> currentEnemies = entityManager.getEnemies();
        if (currentEnemies.isEmpty()) return;

        double currentDirection = entityManager.getEnemyMovementDirection();
        double effectiveSpeedX = entityManager.getEnemyGroupSpeedX();
        double effectiveSpeedY = entityManager.getEnemyGroupSpeedY();
        boolean reverseDirectionAndMoveDown = false;
        double groupLeftMost = Double.MAX_VALUE;
        double groupRightMost = Double.MIN_VALUE;

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;
            groupLeftMost = Math.min(groupLeftMost, enemyNode.getLayoutX());
            groupRightMost = Math.max(groupRightMost, enemyNode.getLayoutX() + enemyNode.getBoundsInLocal().getWidth());
        }

        double moveXThisFrameForCheck = effectiveSpeedX * deltaTime * 60.0;
        if (moveXThisFrameForCheck == 0 && effectiveSpeedX != 0) moveXThisFrameForCheck = effectiveSpeedX * currentDirection;


        if (currentDirection > 0 && groupRightMost + moveXThisFrameForCheck > gameDimensions.getWidth()) {
            reverseDirectionAndMoveDown = true;
        } else if (currentDirection < 0 && groupLeftMost - moveXThisFrameForCheck < 0) {
            reverseDirectionAndMoveDown = true;
        }

        double dx = 0;
        double dy = 0;

        if (reverseDirectionAndMoveDown) {
            entityManager.setEnemyMovementDirection(currentDirection * -1);
            dy = effectiveSpeedY * deltaTime * 60.0;
            if (dy == 0 && effectiveSpeedY != 0) dy = effectiveSpeedY;
        } else {
            dx = effectiveSpeedX * currentDirection * deltaTime * 60.0;
            if (dx == 0 && effectiveSpeedX != 0) dx = effectiveSpeedX * currentDirection;
        }

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            enemyNode.setLayoutX(enemyNode.getLayoutX() + dx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + dy);

            double enemyBottomY = enemyNode.getLayoutY() + enemyNode.getBoundsInLocal().getHeight();
            double playerHeightForGameOver = gameDimensions.getPlayerHeight();
            if (playerHeightForGameOver <=0 && entityManager.getPlayer() != null) playerHeightForGameOver = entityManager.getPlayer().getHeight();
            if (playerHeightForGameOver <=0) playerHeightForGameOver = 30;

            double gameOverLine = gameDimensions.getHeight() - (playerHeightForGameOver * 0.8);

            if (enemyBottomY >= gameOverLine) {
                // System.out.println("!!! GAME OVER TRIGGER durch Gegner am unteren Rand (updateBasicMovementOriginal) !!!"); // Log von vorheriger Anfrage
                // ... (andere detaillierte Logs)
                if (mainApp.getCurrentGameState() == GameState.PLAYING) {
                    mainApp.triggerGameOver();
                }
                return;
            }
        }
    }

    private void updateAcceleratingMovementOriginal(long now) {
        List<Enemy> currentEnemies = entityManager.getEnemies(); if (currentEnemies.isEmpty()) return;
        int totalEnemiesAtStart = GameDimensions.ENEMIES_PER_ROW * GameDimensions.ENEMY_ROWS;
        int remainingEnemies = currentEnemies.size();
        double speedMultiplier = Math.max(1.0, (double) totalEnemiesAtStart / Math.max(1, remainingEnemies));
        speedMultiplier = Math.min(speedMultiplier, 4.0);

        long stepIntervalNanos = (long) (600_000_000 / speedMultiplier);

        if (now - lastEnemyMoveTimeWave2 < stepIntervalNanos) return;
        lastEnemyMoveTimeWave2 = now;

        double currentDirection = entityManager.getEnemyMovementDirection();
        double stepSizeX = entityManager.getEnemyGroupSpeedX() * 8 * speedMultiplier;
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
            dy = stepSizeY;
        } else {
            dx = stepSizeX * currentDirection;
        }

        for (Enemy enemy : currentEnemies) { Node enemyNode = enemy.getNode(); if (enemyNode == null) continue;
            enemyNode.setLayoutX(enemyNode.getLayoutX() + dx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + dy);

            double enemyBottomY = enemyNode.getLayoutY() + enemyNode.getBoundsInLocal().getHeight();
            double playerHeightForGameOver = gameDimensions.getPlayerHeight();
            if (playerHeightForGameOver <=0 && entityManager.getPlayer() != null) playerHeightForGameOver = entityManager.getPlayer().getHeight();
            if (playerHeightForGameOver <=0) playerHeightForGameOver = 30;

            double gameOverLine = gameDimensions.getHeight() - (playerHeightForGameOver * 0.8);
            if (enemyBottomY >= gameOverLine) {
                // System.out.println("!!! GAME OVER TRIGGER durch Gegner am unteren Rand (updateAcceleratingMovementOriginal) !!!"); // Log von vorheriger Anfrage
                if (mainApp.getCurrentGameState() == GameState.PLAYING) mainApp.triggerGameOver();
                return;
            }
        }
    }

    private void updateFormationMovementOriginal(double deltaTime, long now) {
        waveTime += deltaTime * 2.5;

        List<Enemy> currentEnemies = entityManager.getEnemies();
        if (currentEnemies.isEmpty()) return;

        double constantDownwardSpeed = entityManager.getEnemyGroupSpeedY() * 0.02;

        final double WAVE_AMPLITUDE = 200.0;
        final double WAVE_FREQUENCY = 3;

        int enemyIndex = 0;
        int totalEnemiesPerRow = GameDimensions.ENEMIES_PER_ROW;

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            int row = enemyIndex / totalEnemiesPerRow;
            int col = enemyIndex % totalEnemiesPerRow;
            double rowPhaseOffset = row * 0.8;
            double colPhaseOffset = col * 0.3;
            double waveOffset = Math.sin((waveTime + rowPhaseOffset + colPhaseOffset) * WAVE_FREQUENCY) * WAVE_AMPLITUDE;

            double centerX = gameDimensions.getWidth() / 2;
            double baseX = centerX - (totalEnemiesPerRow * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX())) / 2;
            double targetX = baseX + col * (gameDimensions.getEnemyWidth() + gameDimensions.getEnemySpacingX()) + waveOffset;
            targetX = Math.max(10, Math.min(targetX, gameDimensions.getWidth() - gameDimensions.getEnemyWidth() - 10));
            enemyNode.setLayoutX(targetX);

            double downwardMovementThisFrame = constantDownwardSpeed * deltaTime * 60.0;
            if (downwardMovementThisFrame == 0 && constantDownwardSpeed != 0) downwardMovementThisFrame = constantDownwardSpeed;

            double newY = enemyNode.getLayoutY() + downwardMovementThisFrame;
            enemyNode.setLayoutY(newY);

            double enemyBottomY = newY + enemyNode.getBoundsInLocal().getHeight();
            double playerHeightForGameOver = gameDimensions.getPlayerHeight();
            if (playerHeightForGameOver <=0 && entityManager.getPlayer() != null) playerHeightForGameOver = entityManager.getPlayer().getHeight();
            if (playerHeightForGameOver <=0) playerHeightForGameOver = 30;

            double gameOverLine = gameDimensions.getHeight() - (playerHeightForGameOver * 0.8);
            if (enemyBottomY >= gameOverLine) {
                // System.out.println("!!! GAME OVER TRIGGER durch Gegner am unteren Rand (updateFormationMovementOriginal) !!!"); // Log von vorheriger Anfrage
                if (mainApp.getCurrentGameState() == GameState.PLAYING) mainApp.triggerGameOver();
                return;
            }
            enemyIndex++;
        }
    }


    private void checkCollisions(long now) {
        Player player = entityManager.getPlayer();
        if (player == null || player.getNode() == null) {
            return;
        }

        BossController bossController = entityManager.getBossController();
        Iterator<Rectangle> projIterator = entityManager.getPlayerProjectiles().iterator();

        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            boolean projectileUsedThisHit = false;

            if (entityManager.isBossActive() && bossController != null && entityManager.getBossEnemy() != null) {
                Enemy currentBossEntity = entityManager.getBossEnemy();
                if (currentBossEntity.getNode() != null) {
                    boolean isIntersecting = projectile.getBoundsInParent().intersects(currentBossEntity.getNode().getBoundsInParent());

                    if (isIntersecting && !bossController.isBossRetreating()) {
                        // System.out.println("KOLLISION: Spieler-Projektil vs Boss-Körper"); // Log von vorheriger Anfrage
                        bossController.bossTakeHit();
                        Enemy bossAfterHit = entityManager.getBossEnemy();
                        if (bossAfterHit != null && !bossAfterHit.isAlive()) {
                            if (bossController.getBossPhase() >= 3) {
                                uiManager.addScore(currentBossEntity.getPoints());
                                if (soundManager != null) soundManager.playBossFinalDefeat();
                                entityManager.bossDefeated();
                            }
                        } else if (bossAfterHit != null) {
                            if (soundManager != null) soundManager.playEnemyHit();
                        }
                        projectileUsedThisHit = true;
                    }
                }
                if (!projectileUsedThisHit && bossController.isBossRetreating() && !bossController.getMinionEnemies().isEmpty()) {
                    List<Rectangle> singleProjectileList = new ArrayList<>();
                    singleProjectileList.add(projectile);
                    if (bossController.checkPlayerProjectileVsMinionCollisions(singleProjectileList, uiManager)) {
                        // System.out.println("KOLLISION: Spieler-Projektil vs Minion (Boss retreating)"); // Log von vorheriger Anfrage
                        if (soundManager != null) soundManager.playEnemyHit();
                        projectileUsedThisHit = true;
                    }
                }
            }
            else if (!entityManager.getEnemies().isEmpty()) {
                Iterator<Enemy> enemyIterator = entityManager.getEnemies().iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (enemy.getNode() != null && projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                        // System.out.println("KOLLISION: Spieler-Projektil vs Normaler Gegner (" + enemy + ")"); // Log von vorheriger Anfrage
                        enemy.takeHit();
                        if (soundManager != null) soundManager.playEnemyHit();

                        if (!enemy.isAlive()) {
                            enemyIterator.remove();
                            entityManager.removeEnemyNode(enemy.getNode());
                            uiManager.addScore(enemy.getPoints());
                        }
                        projectileUsedThisHit = true;
                        break;
                    }
                }
            }

            if (projectileUsedThisHit) {
                projIterator.remove();
                entityManager.removeProjectileNode(projectile);
            }
        }

        boolean playerHitSomething = false;
        String hitReason = "Unbekannt";

        if (entityManager.isBossActive() && bossController != null) {
            if (bossController.checkBossProjectileCollisions(player)) {
                hitReason = "Spieler vs Boss-Projektil";
                playerHitSomething = true;
            }
            if (!playerHitSomething && entityManager.getBossEnemy() != null &&
                    entityManager.getBossEnemy().getNode() != null &&
                    !bossController.isBossRetreating() &&
                    player.getNode().getBoundsInParent().intersects(entityManager.getBossEnemy().getNode().getBoundsInParent())) {
                hitReason = "Spieler vs Boss-Körper";
                playerHitSomething = true;
            }
            if (!playerHitSomething && bossController.isBossRetreating() && bossController.checkPlayerVsMinionCollisions(player)) {
                hitReason = "Spieler vs Minion (Boss retreating)";
                playerHitSomething = true;
            }
        } else if (!entityManager.getEnemies().isEmpty()) {
            for (Enemy enemy : new ArrayList<>(entityManager.getEnemies())) {
                if (enemy.getNode() != null && player.getNode().getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                    hitReason = "Spieler vs Normaler Gegner (" + enemy + ") an Pos: " + enemy.getNode().getLayoutX() + "," + enemy.getNode().getLayoutY();
                    playerHitSomething = true;
                    break;
                }
            }
        }

        if (playerHitSomething) {
            // System.out.println("!!! GameUpdater.checkCollisions: playerHitSomething ist true !!! Grund: " + hitReason); // Log von vorheriger Anfrage
            // ... (andere detaillierte Logs sind schon da) ...
            if (soundManager != null) {
                soundManager.playPlayerEnemyCollision();
            }
            handlePlayerDeath();
        }
    }


    private void handlePlayerDeath() {
        Player player = entityManager.getPlayer(); if (player == null) return;
        // System.out.println("GameUpdater.handlePlayerDeath(): Spieler gestorben. GameState: " + mainApp.getCurrentGameState()); // Log von vorheriger Anfrage
        if (mainApp.getCurrentGameState() == GameState.PLAYING) {
            if (soundManager != null) {
                soundManager.playPlayerDeath();
            }
            mainApp.triggerGameOver();
        }
    }
}