package org.example.spaceinvaders;

import javafx.scene.Node;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Rectangle;
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

    // Für erweiterte Bewegungsmuster
    private double waveTime = 0; // Zeit für Wellenbewegung
    private long lastEnemyMoveTime = 0; // Für stepped movement in Wave 2
    private int wave2StepCount = 0; // Zähler für Schritte in Wave 2
    private boolean wave3FormationMoving = false; // Für Formation movement in Wave 3
    private long lastFormationMoveTime = 0;

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
        updateEnemyMovement(deltaTime, now);
        checkCollisions();

        if (entityManager.getEnemies().isEmpty() && !entityManager.isBossActive()) {
            if (entityManager.bossAlreadySpawnedThisCycle() && entityManager.getBossEnemy() == null) {
                mainApp.changeGameState(GameState.CREDITS);
            } else if (!entityManager.isLoadingNextWave()) {
                entityManager.spawnNextWaveOrBoss();
            }
        }
    }

    private void updatePlayer(long now) {
        Player player = entityManager.getPlayer();
        if (player == null) return;
        double dx = 0;
        if (inputHandler.isMoveLeftPressed()) dx -= gameDimensions.getPlayerSpeed();
        if (inputHandler.isMoveRightPressed()) dx += gameDimensions.getPlayerSpeed();
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
        if (entityManager.isBossActive() || entityManager.getEnemies().isEmpty()) {
            return;
        }

        int currentWave = entityManager.getCurrentWaveNumber();

        // Wähle Bewegungsmuster basierend auf der Welle
        switch (currentWave) {
            case 1:
                updateBasicMovement(deltaTime);
                break;
            case 2:
                updateAcceleratingMovement(deltaTime, now);
                break;
            case 3:
            default:
                updateFormationMovement(deltaTime, now);
                break;
        }
    }

    // WELLE 1: Grundlegendes Bewegungsmuster (dein ursprüngliches)
    private void updateBasicMovement(double deltaTime) {
        List<Enemy> currentEnemies = entityManager.getEnemies();
        double currentDirection = entityManager.getEnemyMovementDirection();
        double speedX = entityManager.getEnemyGroupSpeedX();
        double speedY = entityManager.getEnemyGroupSpeedY();
        boolean reverseDirection = false;

        final double EFFECTIVE_LEFT_WALL = 3.5;
        final double EFFECTIVE_RIGHT_WALL = 1240.0;

        double minEnemyXThisFrame = Double.MAX_VALUE;
        double maxEnemyXThisFrame = Double.MIN_VALUE;

        if (currentEnemies.isEmpty()) return;

        // Finde die äußersten Kanten der Gegnergruppe
        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;
            minEnemyXThisFrame = Math.min(minEnemyXThisFrame, enemyNode.getLayoutX());
            maxEnemyXThisFrame = Math.max(maxEnemyXThisFrame, enemyNode.getLayoutX() + enemyNode.getBoundsInLocal().getWidth());
        }

        double potentialNextMinGroupX = minEnemyXThisFrame + (speedX * currentDirection);
        double potentialNextMaxGroupX = maxEnemyXThisFrame + (speedX * currentDirection);
        double actualDx = speedX * currentDirection;

        // Wandkollision prüfen
        if (currentDirection > 0) {
            if (potentialNextMaxGroupX > EFFECTIVE_RIGHT_WALL) {
                double overshoot = potentialNextMaxGroupX - EFFECTIVE_RIGHT_WALL;
                actualDx -= overshoot;
                reverseDirection = true;
            }
        } else {
            if (potentialNextMinGroupX < EFFECTIVE_LEFT_WALL) {
                double undershoot = EFFECTIVE_LEFT_WALL - potentialNextMinGroupX;
                actualDx += undershoot;
                reverseDirection = true;
            }
        }

        double actualDy = 0;
        if (reverseDirection) {
            actualDy = speedY;
            entityManager.setEnemyMovementDirection(currentDirection * -1);
        }

        // Alle Gegner bewegen
        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            enemyNode.setLayoutX(enemyNode.getLayoutX() + actualDx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + actualDy);

            // Game Over Prüfung
            if (enemyNode.getLayoutY() + enemyNode.getBoundsInLocal().getHeight() >=
                    gameDimensions.getHeight() - (gameDimensions.getPlayerHeight() * 0.8)) {
                mainApp.triggerGameOver();
                return;
            }
        }
    }

    // WELLE 2: Beschleunigende Space Invaders Bewegung (wird schneller, je weniger Gegner übrig sind)
    private void updateAcceleratingMovement(double deltaTime, long now) {
        List<Enemy> currentEnemies = entityManager.getEnemies();
        if (currentEnemies.isEmpty()) return;

        // Geschwindigkeit basierend auf verbleibenden Gegnern - weniger Gegner = schneller
        int totalEnemies = GameDimensions.ENEMIES_PER_ROW * GameDimensions.ENEMY_ROWS;
        int remainingEnemies = currentEnemies.size();
        double speedMultiplier = (double) totalEnemies / remainingEnemies; // Je weniger übrig, desto schneller
        speedMultiplier = Math.min(speedMultiplier, 4.0); // Maximal 4x so schnell

        // Stepped movement wie im Original Space Invaders - bewege alle 200-800ms je nach Geschwindigkeit
        long stepInterval = (long) (600_000_000 / speedMultiplier); // In Nanosekunden, wird schneller

        if (now - lastEnemyMoveTime < stepInterval) {
            return; // Noch nicht Zeit für den nächsten Schritt
        }

        lastEnemyMoveTime = now;
        wave2StepCount++;

        double currentDirection = entityManager.getEnemyMovementDirection();
        double stepSizeX = entityManager.getEnemyGroupSpeedX() * 8 * speedMultiplier; // Größere Schritte
        double stepSizeY = entityManager.getEnemyGroupSpeedY() * 1.5;
        boolean reverseDirection = false;

        final double EFFECTIVE_LEFT_WALL = 3.5;
        final double EFFECTIVE_RIGHT_WALL = gameDimensions.getWidth() - 50;

        // Finde die äußersten Kanten
        double minEnemyX = Double.MAX_VALUE;
        double maxEnemyX = Double.MIN_VALUE;

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;
            minEnemyX = Math.min(minEnemyX, enemyNode.getLayoutX());
            maxEnemyX = Math.max(maxEnemyX, enemyNode.getLayoutX() + enemyNode.getBoundsInLocal().getWidth());
        }

        // Prüfe Kollision mit Wänden
        double potentialNextMinX = minEnemyX + (stepSizeX * currentDirection);
        double potentialNextMaxX = maxEnemyX + (stepSizeX * currentDirection);

        if (currentDirection > 0 && potentialNextMaxX > EFFECTIVE_RIGHT_WALL) {
            reverseDirection = true;
        } else if (currentDirection < 0 && potentialNextMinX < EFFECTIVE_LEFT_WALL) {
            reverseDirection = true;
        }

        // Bewegung ausführen
        double actualDx = reverseDirection ? 0 : (stepSizeX * currentDirection);
        double actualDy = reverseDirection ? stepSizeY : 0;

        if (reverseDirection) {
            entityManager.setEnemyMovementDirection(currentDirection * -1);
        }

        // Alle Gegner in einem Schritt bewegen (wie im Original)
        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            enemyNode.setLayoutX(enemyNode.getLayoutX() + actualDx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + actualDy);

            // Game Over Prüfung
            if (enemyNode.getLayoutY() + enemyNode.getBoundsInLocal().getHeight() >=
                    gameDimensions.getHeight() - (gameDimensions.getPlayerHeight() * 0.8)) {
                mainApp.triggerGameOver();
                return;
            }
        }
    }

    // WELLE 3: Wellenbewegung nach unten (wie Centipede/Frogger inspiriert)
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

    private void checkCollisions() {
        Player player = entityManager.getPlayer();
        if (player == null || player.getNode() == null) {
            return;
        }

        // Spielerprojektile mit Gegnern/Boss
        Iterator<Rectangle> projIterator = entityManager.getPlayerProjectiles().iterator();
        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            boolean projectileHasHit = false;

            // Kollision mit Boss
            if (entityManager.isBossActive() && entityManager.getBossEnemy() != null) {
                Enemy boss = entityManager.getBossEnemy();
                if (boss.getNode() != null && projectile.getBoundsInParent().intersects(boss.getNode().getBoundsInParent())) {
                    boss.takeHit();
                    playSoundEffect("enemy_hit.wav");

                    if (!boss.isAlive()) {
                        uiManager.addScore(boss.getPoints());
                        playSoundEffect("boss_explosion.wav");
                        entityManager.bossDefeated();
                    }
                    projectileHasHit = true;
                }
            }

            // Kollision mit normalen Gegnern
            if (!projectileHasHit && !entityManager.isBossActive()) {
                Iterator<Enemy> enemyIterator = entityManager.getEnemies().iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (enemy.getNode() != null && projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                        enemy.takeHit();
                        playSoundEffect("enemy_hit.wav");

                        if (!enemy.isAlive()) {
                            enemyIterator.remove();
                            entityManager.removeEnemyNode(enemy.getNode());
                            uiManager.addScore(enemy.getPoints());
                            playSoundEffect("enemy_explosion.wav");
                        }
                        projectileHasHit = true;
                        break;
                    }
                }
            }

            if (projectileHasHit) {
                projIterator.remove();
                entityManager.removeProjectileNode(projectile);
            }
        }

        // Gegner/Boss mit Spieler
        if (!entityManager.isBossActive()) {
            for (Enemy enemy : new ArrayList<>(entityManager.getEnemies())) {
                if (enemy.getNode() != null && player.getNode().getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                    handlePlayerDeath();
                    return;
                }
            }
        } else if (entityManager.getBossEnemy() != null && entityManager.getBossEnemy().getNode() != null) {
            if (player.getNode().getBoundsInParent().intersects(entityManager.getBossEnemy().getNode().getBoundsInParent())) {
                handlePlayerDeath();
                return;
            }
        }
    }

    private void handlePlayerDeath() {
        Player player = entityManager.getPlayer();
        if (player == null) return;
        playSoundEffect("player_explosion.wav");
        mainApp.triggerGameOver();
    }

    private void playSoundEffect(String sfxFileName) {
        if (activeVoiceProfile != null && activeVoiceProfile.getSfxFolderPath() != null) {
            String fullSfxPath = activeVoiceProfile.getSfxFolderPath() + sfxFileName;
            try {
                AudioClip clip = new AudioClip(getClass().getResource(fullSfxPath).toExternalForm());
                clip.play();
            } catch (Exception e) {
                System.err.println("SFX Fehler: " + fullSfxPath + " - " + e.getMessage());
            }
        }
    }
}