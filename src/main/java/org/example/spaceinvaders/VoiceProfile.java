package org.example.spaceinvaders;
import javafx.scene.media.AudioClip;

import java.util.Objects;

public class VoiceProfile {
    private final String displayName;
    private final String introClipPath;
    private final String sfxFolderPath;
    private final String infoText;
    private AudioClip introAudioClip;
    private GameDimensions gameDimensions;

    public VoiceProfile(String displayName, String introClipPath, String sfxFolderPath, String infoText) {
        this.displayName = displayName;
        this.introClipPath = introClipPath;
        this.sfxFolderPath = sfxFolderPath;
        this.infoText = infoText;
        try{
            String fullIntroPath = Objects.requireNonNull(getClass().getResource(introClipPath)).toExternalForm();
            this.introAudioClip = new AudioClip(fullIntroPath);
            System.out.println(gameDimensions.getWinHeight() + "<- height: " + gameDimensions.getWinWidth() + "<- width: ");
        }catch (NullPointerException e){
            System.err.println("Fehler: Intro-Audiodatei nicht gefunden für: " + displayName + " unter Pfad: " + introClipPath);
        }catch(Exception e){
            System.err.println("Allgemeiner Fehler beim Laden des Intro-Audios für: " + displayName + ": " + e.getMessage());
            this.introAudioClip = null;
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public AudioClip getIntroAudioClip() {
        return introAudioClip;
    }

    public String getSfxFolderPath() {
        return sfxFolderPath;
    }

    public String getInfoText() {
        return infoText;
    }

    @Override
    public String toString() {
        return displayName;
    }
}