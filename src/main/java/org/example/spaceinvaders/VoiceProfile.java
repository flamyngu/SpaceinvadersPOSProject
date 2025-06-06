package org.example.spaceinvaders;
import javafx.scene.media.AudioClip;
public class VoiceProfile {
    private final String displayName;
    private final String introClipPath;
    private final String sfxFolderPath;
    private final String infoText;
    private AudioClip introAudioClip;

    public VoiceProfile(String displayName, String introClipPath, String sfxFolderPath, String infoText) {
        this.displayName = displayName;
        this.introClipPath = introClipPath;
        this.sfxFolderPath = sfxFolderPath;
        this.infoText = infoText;
        try{
            String fullIntroPath = getClass().getResource(introClipPath).toExternalForm();
            this.introAudioClip = new AudioClip(fullIntroPath);
        }catch (NullPointerException e){
            System.err.println("Fehler: Intro-Audiodatei nicht gefunden für: " + displayName + " unter Pfad: " + introClipPath);
            this.introAudioClip = null;
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

    // Optional: Override toString() für Debugging
    @Override
    public String toString() {
        return displayName;
    }
}
