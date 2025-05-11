package org.example.spaceinvaders; // Stelle sicher, dass das Package stimmt

import javafx.scene.Node;
import javafx.scene.media.AudioClip; // Für SFX
import javafx.scene.shape.Rectangle;
import java.util.Iterator;

public class GameUpdater {
    private GameEntityManager entityManager;
    private InputHandler inputHandler;
    private GameDimensions gameDimensions;
    private UIManager uiManager;
    private MusicalInvaders mainApp; // Referenz auf die Hauptanwendungsklasse
    private VoiceProfile activeVoiceProfile; // Das aktuell für SFX verwendete Profil

    private long lastShotTime = 0; // Für den Schuss-Cooldown des Spielers

    // ANGEPASSTER Konstruktor
    public GameUpdater(GameEntityManager entityManager, InputHandler inputHandler,
                       GameDimensions gameDimensions, UIManager uiManager, MusicalInvaders mainApp) {
        this.entityManager = entityManager;
        this.inputHandler = inputHandler;
        this.gameDimensions = gameDimensions;
        this.uiManager = uiManager;
        this.mainApp = mainApp; // Speichere die Referenz

        if (this.mainApp != null) {
            this.activeVoiceProfile = this.mainApp.getSelectedVoiceProfile(); // Hole das ausgewählte Profil
        } else {
            System.err.println("GameUpdater: MusicalInvaders mainApp Instanz ist null im Konstruktor!");
            // Fallback oder Fehlerbehandlung, falls kein Profil verfügbar ist
            this.activeVoiceProfile = null;
        }
    }

    public void update(long now, double deltaTime) {
        // Update-Logik ( deltaTime wird noch nicht überall genutzt, aber ist da für später)
        updatePlayer(now /*, deltaTime*/); // deltaTime für Spielerbewegung optional
        handlePlayerShooting(now);
        updateProjectiles(/*deltaTime*/);   // deltaTime für Projektilbewegung optional
        // updateEnemyMovement(deltaTime); // Hier wäre Platz für Gegnerbewegung
        checkCollisions();

        // Spawnen neuer Wellen / Boss (wird vom EntityManager gehandhabt)
        if(entityManager.getEnemies().isEmpty() && !entityManager.isBossActive()) {
            if(entityManager.bossAlreadySpawnedThisCycle() && entityManager.getBossEnemy() == null){
                mainApp.changeGameState(GameState.CREDITS);
            } else if (!entityManager.isLoadingNextWave()) {
                entityManager.spawnNextWaveOrBoss();
            }
        }

        // Beispiel für eine Game-Over-Bedingung (muss angepasst werden)
        // if (entityManager.getPlayer() != null && entityManager.getPlayer().getLives() <= 0) { // Annahme: Player hat getLives()
        //     mainApp.triggerGameOver();
        // }
    }

    private void updatePlayer(long now /*, double deltaTime*/) {
        Player player = entityManager.getPlayer();
        if (player == null) return;

        double dx = 0;
        if (inputHandler.isMoveLeftPressed()) {
            dx -= gameDimensions.getPlayerSpeed();
        }
        if (inputHandler.isMoveRightPressed()) {
            dx += gameDimensions.getPlayerSpeed();
        }
        // Wenn du deltaTime nutzen willst: player.move(dx * deltaTime * 60); // * 60 als Beispiel-Skalierung
        player.move(dx); // Aktuelle Implementierung
    }

    private void handlePlayerShooting(long now) {
        if (entityManager.getPlayer() == null) return; // Stelle sicher, dass der Spieler existiert

        if (inputHandler.isShootingPressed() && (now - lastShotTime) / 1_000_000 >= GameDimensions.SHOOT_COOLDOWN_MS) {
            entityManager.createProjectile();
            lastShotTime = now;
            playSoundEffect("player_shoot.wav"); // Beispiel für Schuss-Sound
        }
    }

    private void updateProjectiles(/*double deltaTime*/) {
        Iterator<Rectangle> iterator = entityManager.getPlayerProjectiles().iterator();
        while (iterator.hasNext()) {
            Rectangle p = iterator.next();
            // p.setY(p.getY() - gameDimensions.getProjectileSpeed() * deltaTime * 60); // Mit deltaTime
            p.setY(p.getY() - gameDimensions.getProjectileSpeed());

            if (p.getY() + p.getHeight() < 0) {
                iterator.remove(); // Entfernt aus der Liste im EntityManager
                // Das Entfernen aus dem gamePane sollte jetzt der EntityManager machen,
                // wenn seine removeProjectile-Methode aufgerufen wird.
                // Hier rufen wir nur iterator.remove() auf, um ConcurrentModification zu vermeiden.
                // Der EntityManager braucht dann eine Methode, um Nodes basierend auf der Liste zu synchronisieren,
                // oder wir übergeben das Node-Objekt zum Entfernen.
                // Besser: EntityManager hat eine Methode removeProjectile(Rectangle p)
                entityManager.removeProjectileNode(p); // Sagt dem EntityManager, es auch visuell zu entfernen
            }
        }
    }

    private void checkCollisions() {
        Iterator<Rectangle> projIterator = entityManager.getPlayerProjectiles().iterator();
        while (projIterator.hasNext()) {
            Rectangle projectile = projIterator.next();
            boolean projectileHitSomething = false;

            // Kollision mit Boss
            if (entityManager.isBossActive() && entityManager.getBossEnemy() != null) {
                Enemy boss = entityManager.getBossEnemy();
                if (projectile.getBoundsInParent().intersects(boss.getNode().getBoundsInParent())) {
                    // projIterator.remove(); // Wird unten gemeinsam gemacht
                    // entityManager.removeProjectileNode(projectile);
                    projectileHitSomething = true;

                    boss.takeHit();
                    playSoundEffect("enemy_hit.wav"); // Beispiel
                    // Optional: Visuelles Feedback für Boss-Treffer

                    if (!boss.isAlive()) {
                        uiManager.addScore(boss.getPoints());
                        // uiManager.showBossDefeatedMessage(); // Wird jetzt in entityManager.bossDefeated() gemacht
                        playSoundEffect("boss_explosion.wav"); // Beispiel
                        entityManager.bossDefeated();
                    }
                    // Da das Projektil den Boss getroffen hat, sollte es hier verbraucht sein.
                }
            }

            // Kollision mit normalen Gegnern (nur wenn kein Boss aktiv oder Boss nicht getroffen wurde)
            if (!projectileHitSomething && !entityManager.isBossActive()) {
                Iterator<Enemy> enemyIterator = entityManager.getEnemies().iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (projectile.getBoundsInParent().intersects(enemy.getNode().getBoundsInParent())) {
                        // projIterator.remove(); // Wird unten gemeinsam gemacht
                        // entityManager.removeProjectileNode(projectile);
                        projectileHitSomething = true;

                        enemy.takeHit();
                        playSoundEffect("enemy_hit.wav"); // Beispiel

                        if (!enemy.isAlive()) {
                            enemyIterator.remove(); // Aus der Logik-Liste entfernen
                            entityManager.removeEnemyNode(enemy.getNode()); // Visuell entfernen
                            uiManager.addScore(enemy.getPoints());
                            playSoundEffect("enemy_explosion.wav"); // Beispiel
                        }
                        break; // Projektil hat einen normalen Gegner getroffen, innere Schleife verlassen
                    }
                }
            }

            if (projectileHitSomething) {
                projIterator.remove(); // Projektil aus der Liste entfernen
                entityManager.removeProjectileNode(projectile); // Projektil visuell entfernen
            }
        }
        // TODO: Kollision Spieler mit Gegner-Projektilen oder Gegnern/Boss selbst
        // Wenn Spieler getroffen wird:
        // Player player = entityManager.getPlayer();
        // player.loseLife();
        // playSoundEffect("player_hit.wav");
        // if (player.getLives() <= 0) {
        //    playSoundEffect("player_explosion.wav");
        //    mainApp.triggerGameOver();
        // }
    }

    // Hilfsmethode zum Abspielen von Sounds
    private void playSoundEffect(String sfxFileName) {
        if (activeVoiceProfile != null && activeVoiceProfile.getSfxFolderPath() != null) {
            String fullSfxPath = activeVoiceProfile.getSfxFolderPath() + sfxFileName;
            try {
                // Du solltest eine robustere SoundManager-Klasse in Betracht ziehen,
                // die Audioclips vorlädt und wiederverwendet, anstatt sie jedes Mal neu zu erstellen.
                AudioClip clip = new AudioClip(getClass().getResource(fullSfxPath).toExternalForm());
                clip.play();
                System.out.println("Played SFX: " + fullSfxPath); // Debug-Ausgabe
            } catch (NullPointerException e) {
                System.err.println("Fehler: SFX-Datei nicht gefunden unter Pfad: " + fullSfxPath + " (Originalname: " + sfxFileName + ")");
            }
            catch (Exception e) {
                System.err.println("Fehler beim Laden/Abspielen von SFX: " + fullSfxPath + " - " + e.getMessage());
            }
        } else {
            // System.out.println("Kein aktives VoiceProfile oder SFX-Pfad für Sound: " + sfxFileName);
        }
    }
}