package org.example.spaceinvaders;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.image.ImageView; // Import
// import javafx.scene.shape.Rectangle; // Nicht mehr für Spieler-Projektile verwendet
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
    // private boolean wave3Initialized = false; // wave3Initialized wurde im vorherigen Code nicht verwendet, kann entfernt werden


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
            // wave3Initialized = false; // Nicht verwendet
            bossController.updateBossMovement(now, deltaTime);
        } else if (!entityManager.getEnemies().isEmpty()) {
            updateEnemyMovement(deltaTime, now);
        } else if (entityManager.getEnemies().isEmpty() && !entityManager.isBossActive() && !entityManager.isLoadingNextWave() && !entityManager.wasBossJustDefeated()) {
            // wave3Initialized = false; // Nicht verwendet
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
        if (player == null || player.getNode() == null) { // Zusätzlicher Null-Check für Node
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
        Iterator<ImageView> iterator = entityManager.getPlayerProjectiles().iterator(); // Typ geändert
        while (iterator.hasNext()) {
            ImageView p = iterator.next(); // Typ geändert
            p.setLayoutY(p.getLayoutY() - gameDimensions.getProjectileSpeed()); // Verwende setLayoutY
            if (p.getLayoutY() + p.getFitHeight() < 0) { // Verwende getFitHeight
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
            // wave3Initialized = false; // Nicht verwendet
        }

        if (currentWave == 1 || (currentWave > 0 && currentWave % 3 == 1) ) {
            updateBasicMovementOriginal(deltaTime);
        } else if (currentWave > 0 && currentWave % 3 == 2) {
            updateAcceleratingMovementOriginal(now);
        } else if (currentWave > 0 && currentWave % 3 == 0) {
            updateFormationMovementOriginal(deltaTime, now);
        } else { // Fallback für unerwartete Wellennummern
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
            double nodeWidth = (enemyNode instanceof ImageView) ? ((ImageView)enemyNode).getFitWidth() : enemyNode.getBoundsInLocal().getWidth();
            groupLeftMost = Math.min(groupLeftMost, enemyNode.getLayoutX());
            groupRightMost = Math.max(groupRightMost, enemyNode.getLayoutX() + nodeWidth);
        }

        double moveXThisFrameForCheck = effectiveSpeedX * deltaTime * 60.0;
        if (moveXThisFrameForCheck == 0 && effectiveSpeedX != 0) moveXThisFrameForCheck = effectiveSpeedX * currentDirection;


        if (currentDirection > 0 && groupRightMost + moveXThisFrameForCheck > gameDimensions.getWidth()) {
            reverseDirectionAndMoveDown = true;
        } else if (currentDirection < 0 && groupLeftMost + moveXThisFrameForCheck < 0) { // Hier war ein Fehler: -moveXThisFrameForCheck muss + sein oder der Vergleich umgekehrt
            reverseDirectionAndMoveDown = true;
        }


        double dx = 0;
        double dy = 0;

        if (reverseDirectionAndMoveDown) {
            entityManager.setEnemyMovementDirection(currentDirection * -1);
            dy = effectiveSpeedY * deltaTime * 60.0; // Bewegung pro Frame
            if (dy == 0 && effectiveSpeedY != 0) dy = effectiveSpeedY; // Fallback falls deltaTime 0 ist
        } else {
            dx = effectiveSpeedX * currentDirection * deltaTime * 60.0; // Bewegung pro Frame
            if (dx == 0 && effectiveSpeedX != 0) dx = effectiveSpeedX * currentDirection; // Fallback
        }

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            enemyNode.setLayoutX(enemyNode.getLayoutX() + dx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + dy);

            double nodeHeight = (enemyNode instanceof ImageView) ? ((ImageView)enemyNode).getFitHeight() : enemyNode.getBoundsInLocal().getHeight();
            double enemyBottomY = enemyNode.getLayoutY() + nodeHeight;
            double playerHeightForGameOver = gameDimensions.getPlayerHeight();
            if (playerHeightForGameOver <=0 && entityManager.getPlayer() != null) playerHeightForGameOver = entityManager.getPlayer().getHeight();
            if (playerHeightForGameOver <=0) playerHeightForGameOver = 30;

            double gameOverLine = gameDimensions.getHeight() - (playerHeightForGameOver * 0.8);

            if (enemyBottomY >= gameOverLine) {
                if (mainApp.getCurrentGameState() == GameState.PLAYING) {
                    mainApp.triggerGameOver();
                }
                return; // Spiel vorbei, keine weiteren Updates für diese Methode
            }
        }
    }

    private void updateAcceleratingMovementOriginal(long now) {
        List<Enemy> currentEnemies = entityManager.getEnemies(); if (currentEnemies.isEmpty()) return;
        int totalEnemiesAtStart = GameDimensions.ENEMIES_PER_ROW * GameDimensions.ENEMY_ROWS;
        int remainingEnemies = currentEnemies.size();
        double speedMultiplier = Math.max(1.0, (double) totalEnemiesAtStart / Math.max(1, remainingEnemies));
        speedMultiplier = Math.min(speedMultiplier, 4.0); // Max Multiplikator

        long stepIntervalNanos = (long) (600_000_000 / speedMultiplier); // Basisintervall / Multiplikator

        if (now - lastEnemyMoveTimeWave2 < stepIntervalNanos) return;
        lastEnemyMoveTimeWave2 = now;

        double currentDirection = entityManager.getEnemyMovementDirection();
        double stepSizeX = entityManager.getEnemyGroupSpeedX() * 8 * speedMultiplier; // Basis-Schrittweite * Multiplikator
        double stepSizeY = entityManager.getEnemyGroupSpeedY() * 1.5; // Vertikale Schrittweite

        boolean reverseDirectionAndMoveDown = false;
        double groupLeftMost = Double.MAX_VALUE;
        double groupRightMost = Double.MIN_VALUE;

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;
            double nodeWidth = (enemyNode instanceof ImageView) ? ((ImageView)enemyNode).getFitWidth() : enemyNode.getBoundsInLocal().getWidth();
            groupLeftMost = Math.min(groupLeftMost, enemyNode.getLayoutX());
            groupRightMost = Math.max(groupRightMost, enemyNode.getLayoutX() + nodeWidth);
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

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;
            enemyNode.setLayoutX(enemyNode.getLayoutX() + dx);
            enemyNode.setLayoutY(enemyNode.getLayoutY() + dy);

            double nodeHeight = (enemyNode instanceof ImageView) ? ((ImageView)enemyNode).getFitHeight() : enemyNode.getBoundsInLocal().getHeight();
            double enemyBottomY = enemyNode.getLayoutY() + nodeHeight;
            double playerHeightForGameOver = gameDimensions.getPlayerHeight();
            if (playerHeightForGameOver <=0 && entityManager.getPlayer() != null) playerHeightForGameOver = entityManager.getPlayer().getHeight();
            if (playerHeightForGameOver <=0) playerHeightForGameOver = 30;

            double gameOverLine = gameDimensions.getHeight() - (playerHeightForGameOver * 0.8);
            if (enemyBottomY >= gameOverLine) {
                if (mainApp.getCurrentGameState() == GameState.PLAYING) mainApp.triggerGameOver();
                return;
            }
        }
    }

    private void updateFormationMovementOriginal(double deltaTime, long now) {
        waveTime += deltaTime * 2.5; // Geschwindigkeit der Wellenbewegung

        List<Enemy> currentEnemies = entityManager.getEnemies();
        if (currentEnemies.isEmpty()) return;

        double constantDownwardSpeed = entityManager.getEnemyGroupSpeedY() * 0.02; // Sehr langsame konstante Abwärtsbewegung

        final double WAVE_AMPLITUDE = gameDimensions.getWidth() * 0.25; // Amplitude relativ zur Bildschirmbreite
        final double WAVE_FREQUENCY = 2.5; // Frequenz der Sinuswelle

        int enemyIndex = 0;
        int totalEnemiesPerRow = GameDimensions.ENEMIES_PER_ROW;

        for (Enemy enemy : currentEnemies) {
            Node enemyNode = enemy.getNode();
            if (enemyNode == null) continue;

            double nodeWidth = (enemyNode instanceof ImageView) ? ((ImageView)enemyNode).getFitWidth() : enemyNode.getBoundsInLocal().getWidth();
            double nodeHeight = (enemyNode instanceof ImageView) ? ((ImageView)enemyNode).getFitHeight() : enemyNode.getBoundsInLocal().getHeight();

            int row = enemyIndex / totalEnemiesPerRow;
            int col = enemyIndex % totalEnemiesPerRow;
            double rowPhaseOffset = row * 0.8;
            double colPhaseOffset = col * 0.3;
            double waveOffset = Math.sin((waveTime + rowPhaseOffset + colPhaseOffset) * WAVE_FREQUENCY) * WAVE_AMPLITUDE;

            double centerX = gameDimensions.getWidth() / 2;
            // Basis-X-Position, um die Formation zu zentrieren
            double formationTotalWidth = totalEnemiesPerRow * (nodeWidth + gameDimensions.getEnemySpacingX()) - gameDimensions.getEnemySpacingX();
            double baseX = centerX - formationTotalWidth / 2;

            double targetX = baseX + col * (nodeWidth + gameDimensions.getEnemySpacingX()) + waveOffset;
            // Sicherstellen, dass Gegner im Bildschirm bleiben
            targetX = Math.max(10, Math.min(targetX, gameDimensions.getWidth() - nodeWidth - 10));
            enemyNode.setLayoutX(targetX);

            // Konstante Abwärtsbewegung
            double downwardMovementThisFrame = constantDownwardSpeed * deltaTime * 60.0;
            if (downwardMovementThisFrame == 0 && constantDownwardSpeed != 0) downwardMovementThisFrame = constantDownwardSpeed;

            double newY = enemyNode.getLayoutY() + downwardMovementThisFrame;
            enemyNode.setLayoutY(newY);

            double enemyBottomY = newY + nodeHeight;
            double playerHeightForGameOver = gameDimensions.getPlayerHeight();
            if (playerHeightForGameOver <=0 && entityManager.getPlayer() != null) playerHeightForGameOver = entityManager.getPlayer().getHeight();
            if (playerHeightForGameOver <=0) playerHeightForGameOver = 30;

            double gameOverLine = gameDimensions.getHeight() - (playerHeightForGameOver * 0.8);
            if (enemyBottomY >= gameOverLine) {
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
        Iterator<ImageView> projIterator = entityManager.getPlayerProjectiles().iterator(); // Typ geändert

        while (projIterator.hasNext()) {
            ImageView projectile = projIterator.next(); // Typ geändert
            boolean projectileUsedThisHit = false;

            if (entityManager.isBossActive() && bossController != null && entityManager.getBossEnemy() != null) {
                Enemy currentBossEntity = entityManager.getBossEnemy();
                if (currentBossEntity.getNode() != null) {
                    boolean isIntersecting = projectile.getBoundsInParent().intersects(currentBossEntity.getNode().getBoundsInParent());

                    if (isIntersecting && !bossController.isBossRetreating()) {
                        bossController.bossTakeHit();
                        Enemy bossAfterHit = entityManager.getBossEnemy(); // Erneut holen, da Zustand sich ändern kann
                        if (bossAfterHit != null && !bossAfterHit.isAlive()) { // Boss besiegt in dieser Phase
                            if (bossController.getBossPhase() >= 3) { // Endgültig besiegt
                                uiManager.addScore(currentBossEntity.getPoints());
                                if (soundManager != null) soundManager.playBossFinalDefeat();
                                entityManager.bossDefeated(); // Markiert Boss als besiegt
                            }
                            // Wenn nicht endgültig besiegt, startet der Rückzug in bossTakeHit() -> startBossRetreat()
                        } else if (bossAfterHit != null) { // Boss nur getroffen
                            if (soundManager != null) soundManager.playEnemyHit();
                        }
                        projectileUsedThisHit = true;
                    }
                }
                // Kollision mit Minions, während Boss sich zurückzieht
                if (!projectileUsedThisHit && bossController.isBossRetreating() && !bossController.getMinionEnemies().isEmpty()) {
                    // checkPlayerProjectileVsMinionCollisions erwartet List<ImageView>
                    List<ImageView> singleProjectileList = new ArrayList<>();
                    singleProjectileList.add(projectile);
                    if (bossController.checkPlayerProjectileVsMinionCollisions(singleProjectileList, uiManager)) {
                        if (soundManager != null) soundManager.playEnemyHit();
                        projectileUsedThisHit = true;
                        // Das Projektil wird in checkPlayerProjectileVsMinionCollisions entfernt, wenn es trifft.
                        // Wir müssen es hier nicht erneut aus dem Iterator entfernen, wenn es dort schon passiert.
                        // Um sicherzugehen, können wir den Rückgabewert prüfen oder die Logik anpassen.
                        // Fürs Erste: Wenn check... true zurückgibt, wurde es dort behandelt.
                    }
                }
            }
            else if (!entityManager.getEnemies().isEmpty()) { // Normale Gegner
                Iterator<Enemy> enemyIterator = entityManager.getEnemies().iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (enemy.getNode() != null && projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                        enemy.takeHit();
                        if (soundManager != null) soundManager.playEnemyHit();

                        if (!enemy.isAlive()) {
                            enemyIterator.remove(); // Entferne logischen Gegner
                            entityManager.removeEnemyNode(enemy.getNode()); // Entferne grafischen Node
                            uiManager.addScore(enemy.getPoints());
                        }
                        projectileUsedThisHit = true;
                        break;
                    }
                }
            }

            if (projectileUsedThisHit && playerProjectilesContains(projectile)) { // Nur entfernen, wenn noch in der Liste
                projIterator.remove();
                entityManager.removeProjectileNode(projectile);
            }
        }

        // Kollision Spieler mit Gegnern/Boss
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
            for (Enemy enemy : new ArrayList<>(entityManager.getEnemies())) { // Kopie für sichere Iteration
                if (enemy.getNode() != null && player.getNode().getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                    hitReason = "Spieler vs Normaler Gegner";
                    playerHitSomething = true;
                    break;
                }
            }
        }

        if (playerHitSomething) {
            if (soundManager != null) {
                soundManager.playPlayerEnemyCollision();
            }
            handlePlayerDeath();
        }
    }

    // Hilfsmethode, um zu prüfen, ob ein Projektil noch in der Liste ist
    // (nötig, da checkPlayerProjectileVsMinionCollisions es evtl. schon entfernt hat)
    private boolean playerProjectilesContains(ImageView projectile) {
        for (ImageView p : entityManager.getPlayerProjectiles()) {
            if (p == projectile) return true;
        }
        return false;
    }


    private void handlePlayerDeath() {
        Player player = entityManager.getPlayer(); if (player == null) return;
        if (mainApp.getCurrentGameState() == GameState.PLAYING) {
            if (soundManager != null) {
                soundManager.playPlayerDeath();
            }
            mainApp.triggerGameOver();
        }
    }
}