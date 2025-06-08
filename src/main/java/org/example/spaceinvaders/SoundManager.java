package org.example.spaceinvaders;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SoundManager {
    private final String baseSfxPath;
    private final Random random = new Random();

    private final List<AudioClip> playerShootSounds = new ArrayList<>();
    private final List<AudioClip> enemyHitSounds = new ArrayList<>();
    private final List<AudioClip> bossShootSounds = new ArrayList<>();
    private final List<AudioClip> todBossScaredSounds = new ArrayList<>();
    private final List<AudioClip> todBossFinalSounds = new ArrayList<>();
    private final List<AudioClip> playerDeathSounds = new ArrayList<>();
    private final List<AudioClip> playerEnemyCollisionSounds = new ArrayList<>();

    private final List<AudioClip> allJubelSounds = new ArrayList<>();
    private boolean isJubelLooping = false;
    private List<Timeline> jubelFadeOutTimelines = new ArrayList<>();

    public SoundManager(String baseSfxPath) {
        if (baseSfxPath == null || baseSfxPath.trim().isEmpty()) {
            // System.err.println("SoundManager (Profile): Base SFX path is null or empty. No profile-specific sounds will be loaded.");
            this.baseSfxPath = null;
            return;
        }
        this.baseSfxPath = baseSfxPath.endsWith("/") ? baseSfxPath : baseSfxPath + "/";
        loadProfileSpecificSounds();
    }

    public SoundManager() {
        this.baseSfxPath = null;
        // System.out.println("SoundManager (Global): Instance created for Jubel sounds.");
        loadAllJubelSoundsGlobally();
    }


    private void loadProfileSpecificSounds() {
        if (this.baseSfxPath == null || this.baseSfxPath.isEmpty()) return;
        // System.out.println("SoundManager: Loading profile-specific sounds from base path: " + this.baseSfxPath);
        loadSoundsForCategory(playerShootSounds, this.baseSfxPath, "SchussPlayer", s -> true);
        loadSoundsForCategory(enemyHitSounds, this.baseSfxPath, "TodEnemy", s -> true);
        loadSoundsForCategory(bossShootSounds, this.baseSfxPath, "SchussBoss", s -> true);
        loadSoundsForCategory(todBossScaredSounds, this.baseSfxPath, "TodBoss", s -> !s.toLowerCase().contains("bosstodfinal"));
        loadSoundsForCategory(todBossFinalSounds, this.baseSfxPath, "TodBoss", s -> s.toLowerCase().contains("bosstodfinal"));
        loadSoundsForCategory(playerDeathSounds, this.baseSfxPath, "TodPlayer", s -> true);
        loadSoundsForCategory(playerEnemyCollisionSounds, this.baseSfxPath, "Kollision", s -> true);
    }

    private void loadAllJubelSoundsGlobally() {
        // System.out.println("SoundManager: Attempting to load all Jubel.wav files globally.");
        allJubelSounds.clear();

        String globalSfxBasePath = "/sfx/";
        URL sfxRootUrl = getClass().getResource(globalSfxBasePath);

        if (sfxRootUrl == null) {
            System.err.println("SoundManager: Global SFX root folder '" + globalSfxBasePath + "' not found.");
            return;
        }

        try {
            if ("file".equals(sfxRootUrl.getProtocol())) {
                File sfxRootDir = new File(sfxRootUrl.toURI());
                if (sfxRootDir.isDirectory() && sfxRootDir.listFiles() != null) {
                    for (File profileDir : sfxRootDir.listFiles()) {
                        if (profileDir.isDirectory()) {
                            String profileResourcePath = globalSfxBasePath + profileDir.getName() + "/";
                            loadSoundsForCategory(allJubelSounds, profileResourcePath, "", fileName -> fileName.equalsIgnoreCase("Jubel.wav"));
                        }
                    }
                }
            } else if ("jar".equals(sfxRootUrl.getProtocol())) {
                JarURLConnection jarConnection = (JarURLConnection) sfxRootUrl.openConnection();
                try (JarFile jarFile = jarConnection.getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    String sfxRootPathInJar = globalSfxBasePath.substring(1);
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (entryName.startsWith(sfxRootPathInJar) &&
                                entryName.substring(sfxRootPathInJar.length()).contains("/") &&
                                entryName.toLowerCase().endsWith("/jubel.wav") &&
                                !entry.isDirectory()) {

                            URL clipUrl = getClass().getResource("/" + entryName);
                            if (clipUrl != null) {
                                AudioClip clip = new AudioClip(clipUrl.toExternalForm());
                                clip.setCycleCount(AudioClip.INDEFINITE);
                                allJubelSounds.add(clip);
                                // System.out.println("  Loaded Jubel (JAR): /" + entryName);
                            } else {
                                // System.err.println("  Error loading Jubel (JAR - URL null): /" + entryName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("SoundManager: Error loading global Jubel sounds: " + e.getMessage());
            e.printStackTrace();
        }

        // if (allJubelSounds.isEmpty()) {
        //     System.out.println("SoundManager: No Jubel.wav files found globally.");
        // } else {
        //     System.out.println("SoundManager: Loaded " + allJubelSounds.size() + " global Jubel.wav files.");
        // }
    }


    private void loadSoundsForCategory(List<AudioClip> soundList, String basePathForProfile, String categorySubfolder, Predicate<String> fileNameFilter) {
        if (basePathForProfile == null || basePathForProfile.isEmpty()) return;

        String fullCategoryPath;
        if (categorySubfolder == null || categorySubfolder.isEmpty()) {
            fullCategoryPath = basePathForProfile;
        } else {
            fullCategoryPath = basePathForProfile + categorySubfolder;
        }

        if (!fullCategoryPath.endsWith("/")) {
            fullCategoryPath += "/";
        }

        URL folderURL = getClass().getResource(fullCategoryPath);
        if (folderURL == null) {
            if (! (soundList == allJubelSounds && (categorySubfolder == null || categorySubfolder.isEmpty())) ) {
                // System.out.println("SoundManager: SFX category folder not found (optional): " + fullCategoryPath);
            } else {
                // System.err.println("SoundManager: Jubel SFX folder not found: " + fullCategoryPath);
            }
            return;
        }

        try {
            if ("file".equals(folderURL.getProtocol())) {
                File directory = new File(folderURL.toURI());
                collectWavFilesFromFileSystem(directory, fullCategoryPath, soundList, fileNameFilter);
            } else if ("jar".equals(folderURL.getProtocol())) {
                collectWavFilesFromJar(folderURL, fullCategoryPath, soundList, fileNameFilter);
            }
        } catch (URISyntaxException e) {
            System.err.println("SoundManager: Invalid URI syntax for SFX folder " + fullCategoryPath + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("SoundManager: IOException while accessing SFX folder " + fullCategoryPath + ": " + e.getMessage());
        }
    }


    private void collectWavFilesFromFileSystem(File directory, String currentResourcePath, List<AudioClip> soundList, Predicate<String> fileNameFilter) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
            } else if (fileName.toLowerCase().endsWith(".wav") && fileNameFilter.test(fileName)) {
                String fullResourcePathToFile = currentResourcePath + fileName;
                try {
                    URL clipURL = getClass().getResource(fullResourcePathToFile);
                    if (clipURL != null) {
                        AudioClip clip = new AudioClip(clipURL.toExternalForm());
                        if (soundList == allJubelSounds && fileName.equalsIgnoreCase("Jubel.wav")) {
                            clip.setCycleCount(AudioClip.INDEFINITE);
                        }
                        soundList.add(clip);
                    }
                } catch (Exception e) {
                    // System.err.println("  Error loading SFX (File): " + fullResourcePathToFile + " - " + e.getMessage());
                }
            }
        }
    }

    private void collectWavFilesFromJar(URL jarFolderURL, String baseResourcePathInJar, List<AudioClip> soundList, Predicate<String> fileNameFilter) throws IOException {
        String searchPrefix = baseResourcePathInJar.startsWith("/") ? baseResourcePathInJar.substring(1) : baseResourcePathInJar;

        JarURLConnection jarConnection = (JarURLConnection) jarFolderURL.openConnection();
        try (JarFile jarFile = jarConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(searchPrefix) && !entry.isDirectory() && entryName.toLowerCase().endsWith(".wav")) {
                    String fileNameInDir = entryName.substring(searchPrefix.length());
                    if (!fileNameInDir.contains("/") && fileNameFilter.test(fileNameInDir)) {
                        String resourcePathForClip = "/" + entryName;
                        try {
                            URL clipUrl = getClass().getResource(resourcePathForClip);
                            if (clipUrl != null) {
                                AudioClip clip = new AudioClip(clipUrl.toExternalForm());
                                if (soundList == allJubelSounds && fileNameInDir.equalsIgnoreCase("Jubel.wav")) {
                                    clip.setCycleCount(AudioClip.INDEFINITE);
                                }
                                soundList.add(clip);
                            }
                        } catch (Exception e) {
                            // System.err.println("  Error loading SFX (JAR): " + resourcePathForClip + " - " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void playRandom(List<AudioClip> soundList, String categoryName) {
        if (soundList == null || soundList.isEmpty()) {
            return;
        }
        AudioClip clip = soundList.get(random.nextInt(soundList.size()));
        clip.play();
    }

    public void startJubelLoop() {
        if (allJubelSounds.isEmpty()) {
            // System.out.println("SoundManager: Keine Jubel-Sounds zum Abspielen vorhanden.");
            return;
        }
        for (Timeline timeline : jubelFadeOutTimelines) {
            timeline.stop();
        }
        jubelFadeOutTimelines.clear();

        // System.out.println("SoundManager: Starte Jubel-Loop mit " + allJubelSounds.size() + " Sounds.");
        isJubelLooping = true;
        for (AudioClip jubelClip : allJubelSounds) {
            jubelClip.setVolume(1.0);
            if (!jubelClip.isPlaying()) {
                jubelClip.play();
            }
        }
    }

    public void stopJubelLoop() {
        if (!isJubelLooping && allJubelSounds.stream().noneMatch(AudioClip::isPlaying)) {
            // System.out.println("SoundManager: Nichts zu stoppen für Jubel-Loop (war nicht aktiv).");
            return;
        }
        // System.out.println("SoundManager: Stoppe Jubel-Loop mit Fade-Out.");
        isJubelLooping = false;

        for (Timeline timeline : jubelFadeOutTimelines) {
            timeline.stop();
        }
        jubelFadeOutTimelines.clear();

        for (AudioClip jubelClip : allJubelSounds) {
            if (jubelClip.isPlaying()) {
                Timeline fadeOut = new Timeline(
                        new KeyFrame(Duration.seconds(1.5),
                                new KeyValue(jubelClip.volumeProperty(), 0))
                );
                fadeOut.setOnFinished(event -> {
                    jubelClip.stop();
                    jubelClip.setVolume(1.0);
                });
                jubelFadeOutTimelines.add(fadeOut);
                fadeOut.play();
            } else {
                jubelClip.setVolume(1.0);
            }
        }
    }

    public void playPlayerShoot() { playRandom(playerShootSounds, "PlayerShoot"); }
    public void playEnemyHit() { playRandom(enemyHitSounds, "EnemyHit"); }
    public void playBossShoot() { playRandom(bossShootSounds, "BossShoot"); }
    public void playBossScared() { playRandom(todBossScaredSounds, "BossScared"); }
    public void playBossFinalDefeat() {
        if (todBossFinalSounds != null && !todBossFinalSounds.isEmpty()) {
            todBossFinalSounds.get(random.nextInt(todBossFinalSounds.size())).play();
        } else {
            // System.err.println("SoundManager: Kein BossFinalDefeat-Sound gefunden, spiele BossScared.");
            playBossScared();
        }
    }
    public void playPlayerDeath() { playRandom(playerDeathSounds, "PlayerDeath"); }
    public void playPlayerEnemyCollision() { playRandom(playerEnemyCollisionSounds, "PlayerEnemyCollision"); }
}