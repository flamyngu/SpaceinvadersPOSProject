package org.example.spaceinvaders;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle; // Da Projektile noch Rectangles sind

import java.util.Iterator;

public class GameUpdater {
    private GameEntityManager entityManager;
    private InputHandler inputHandler;
    private GameDimensions gameDimensions;
    private UIManager uiManager;

    private long lastShotTime = 0;

    public GameUpdater(GameEntityManager entityManager, InputHandler inputHandler, GameDimensions gameDimensions, UIManager uiManager) {
        this.entityManager = entityManager;
        this.inputHandler = inputHandler;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
    }

    public void update(long now, double deltaTime) { // deltaTime für zukünftige physikbasierte Bewegung
        updatePlayer(now, deltaTime);
        handlePlayerShooting(now);
        updateProjectiles(deltaTime);
        checkCollisions();
        entityManager.spawnEnemyWaveWithDelay(); // Wellen-Spawnen hier oder in der Hauptschleife
    }

    private void updatePlayer(long now, double deltaTime) {
        Player player = entityManager.getPlayer();
        if (player == null) return;

        double dx = 0;
        if (inputHandler.isMoveLeftPressed()) {
            dx -= gameDimensions.getPlayerSpeed(); // Geschwindigkeit aus GameDimensions
        }
        if (inputHandler.isMoveRightPressed()) {
            dx += gameDimensions.getPlayerSpeed();
        }
        // Multipliziere mit deltaTime für frame-unabhängige Bewegung, falls gewünscht
        // player.move(dx * deltaTime * 60); // Beispiel: *60 für Angleichung an 60 FPS Basis
        player.move(dx); // Für jetzt einfache Bewegung
    }

    private void handlePlayerShooting(long now) {
        if (inputHandler.isShootingPressed() && (now - lastShotTime) / 1_000_000 >= GameDimensions.SHOOT_COOLDOWN_MS) {
            entityManager.createProjectile();
            lastShotTime = now;
        }
    }

    private void updateProjectiles(double deltaTime) {
        Iterator<Rectangle> iterator = entityManager.getPlayerProjectiles().iterator();
        while (iterator.hasNext()) {
            Rectangle p = iterator.next();
            // p.setY(p.getY() - gameDimensions.getProjectileSpeed() * deltaTime * 60); // Mit deltaTime
            p.setY(p.getY() - gameDimensions.getProjectileSpeed()); // Einfache Bewegung

            if (p.getY() + p.getHeight() < 0) { // p.getHeight() statt gameDimensions.getProjectileHeight() für das spezifische Objekt
                iterator.remove(); // Aus der Liste im EntityManager entfernen
                entityManager.removeProjectileNode(p); // Aus dem Pane entfernen
            }
        }
    }

    private void checkCollisions() {
        Iterator<Rectangle> projIterator = entityManager.getPlayerProjectiles().iterator();
        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            Iterator<Enemy> enemyIterator = entityManager.getEnemies().iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();

                if (projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                    // Kollision!
                    projIterator.remove(); // Aus der Projektil-Liste des EntityManagers entfernen
                    entityManager.removeProjectileNode(projectile); // Aus dem Pane entfernen

                    enemy.takeHit();
                    if (!enemy.isAlive()) {
                        enemyIterator.remove(); // Aus der Gegner-Liste des EntityManagers entfernen
                        entityManager.removeEnemy(enemy); // Aus dem Pane entfernen
                        uiManager.addScore(enemy.getPoints());
                    }
                    break; // Projektil kann nur einen Gegner pro Frame treffen/zerstören
                }
            }
        }
        // TODO: Kollision Spieler mit Gegner-Projektilen oder Gegnern selbst
    }
}