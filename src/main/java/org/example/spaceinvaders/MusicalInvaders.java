package org.example.spaceinvaders;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;


// import org.example.spaceinvaders.VoiceProfile; // Stelle sicher, dass VoiceProfile im richtigen Package ist oder importiere es korrekt
// Annahme: VoiceProfile ist im selben Package oder du hast den Import angepasst
// z.B. wenn VoiceProfile in org.example.spaceinvaders.menu liegt:
// import org.example.spaceinvaders.menu.VoiceProfile;


public class MusicalInvaders extends Application {

    private Stage primaryStage; // Mache primaryStage zu einer Instanzvariable
    private Scene mainMenuScene;
    private Scene gameScene;

    // Für das Menü
    private ObservableList<VoiceProfile> voiceProfiles;
    private VoiceProfile currentlyPreviewedVoice = null;
    private VoiceProfile selectedVoiceProfile = null; // Die für das Spiel ausgewählte Stimme
    private AudioClip currentPlayingIntro = null;

    // Spielspezifische Manager und Variablen
    private GameDimensions gameDimensions;
    private Pane gamePane; // Wird für die Spielszene verwendet
    private Pane uiPane;   // Wird für die Spiel-UI verwendet
    private GameEntityManager entityManager;
    private GameUpdater gameUpdater;
    private UIManager gameUIManager; // Umbenannt von uiManager zu gameUIManager, um Verwechslung zu vermeiden
    private InputHandler inputHandler;
    private AnimationTimer gameLoop;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage; // Speichere die primaryStage
        this.primaryStage.setTitle("Musical Invaders");

        loadVoiceProfiles(); // Lade die Stimmprofile

        // Erstelle die Hauptmenü-Szene und zeige sie zuerst an
        mainMenuScene = createMainMenuScene();
        this.primaryStage.setScene(mainMenuScene);
        this.primaryStage.setResizable(false); // Kann hier oder später gesetzt werden
        this.primaryStage.show();
    }

    private void loadVoiceProfiles() {
        // Hier lädst du deine VoiceProfile-Daten.
        // Beispiel (Pfade anpassen!):
        // Pfade müssen relativ zum 'resources' Ordner sein und mit "/" beginnen.
        // z.B. "/audio/voices/max_mustermann/intro.wav"
        voiceProfiles = FXCollections.observableArrayList(
                new VoiceProfile("Stimme von Max", "/sfx/MaxMustermann/intro_MaxMustermann.wav", "/sfx/MaxMustermann/", "Max isst gerne Pizza."),
                new VoiceProfile("Stimme von Erika", "/sfx/ErikaMusterfrau/intro_ErikaMusterfrau.wav", "/sfx/ErikaMusterfrau/", "Erika singt gerne."),
                new VoiceProfile("Stimme Alpha", "/sfx/StimmeAlpha/intro_StimmeAlpha.wav", "/sfx/StimmeAlpha/", "Teststimme Alpha."),
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta."),
                new VoiceProfile("Stimme von Max", "/sfx/MaxMustermann/intro_MaxMustermann.wav", "/sfx/MaxMustermann/", "Max isst gerne Pizza."),
                new VoiceProfile("Stimme von Erika", "/sfx/ErikaMusterfrau/intro_ErikaMusterfrau.wav", "/sfx/ErikaMusterfrau/", "Erika singt gerne."),
                new VoiceProfile("Stimme Alpha", "/sfx/StimmeAlpha/intro_StimmeAlpha.wav", "/sfx/StimmeAlpha/", "Teststimme Alpha."),
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta."),
                new VoiceProfile("Stimme von Max", "/sfx/MaxMustermann/intro_MaxMustermann.wav", "/sfx/MaxMustermann/", "Max isst gerne Pizza."),
                new VoiceProfile("Stimme von Erika", "/sfx/ErikaMusterfrau/intro_ErikaMusterfrau.wav", "/sfx/ErikaMusterfrau/", "Erika singt gerne."),
                new VoiceProfile("Stimme Alpha", "/sfx/StimmeAlpha/intro_StimmeAlpha.wav", "/sfx/StimmeAlpha/", "Teststimme Alpha."),
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta."),
                new VoiceProfile("Stimme von Max", "/sfx/MaxMustermann/intro_MaxMustermann.wav", "/sfx/MaxMustermann/", "Max isst gerne Pizza."),
                new VoiceProfile("Stimme von Erika", "/sfx/ErikaMusterfrau/intro_ErikaMusterfrau.wav", "/sfx/ErikaMusterfrau/", "Erika singt gerne."),
                new VoiceProfile("Stimme Alpha", "/sfx/StimmeAlpha/intro_StimmeAlpha.wav", "/sfx/StimmeAlpha/", "Teststimme Alpha."),
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta."),
                new VoiceProfile("Stimme von Max", "/sfx/MaxMustermann/intro_MaxMustermann.wav", "/sfx/MaxMustermann/", "Max isst gerne Pizza."),
                new VoiceProfile("Stimme von Erika", "/sfx/ErikaMusterfrau/intro_ErikaMusterfrau.wav", "/sfx/ErikaMusterfrau/", "Erika singt gerne."),
                new VoiceProfile("Stimme Alpha", "/sfx/StimmeAlpha/intro_StimmeAlpha.wav", "/sfx/StimmeAlpha/", "Teststimme Alpha."),
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta."),
                new VoiceProfile("Stimme von Max", "/sfx/MaxMustermann/intro_MaxMustermann.wav", "/sfx/MaxMustermann/", "Max isst gerne Pizza."),
                new VoiceProfile("Stimme von Erika", "/sfx/ErikaMusterfrau/intro_ErikaMusterfrau.wav", "/sfx/ErikaMusterfrau/", "Erika singt gerne."),
                new VoiceProfile("Stimme Alpha", "/sfx/StimmeAlpha/intro_StimmeAlpha.wav", "/sfx/StimmeAlpha/", "Teststimme Alpha."),
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta.")


        // Füge hier alle deine VoiceProfile-Objekte hinzu
        );
        if (!voiceProfiles.isEmpty()) {
            currentlyPreviewedVoice = voiceProfiles.get(0);
        }
    }

    private Scene createMainMenuScene() {
        BorderPane rootLayout = new BorderPane();
        rootLayout.setPadding(new Insets(20));
        //rootLayout.setStyle("-fx-background-color: #FFFFFF;"); // Weißer Hintergrund für das Menü
        Image backgroundImage = null;
        try{
            backgroundImage = new Image(this.getClass().getResourceAsStream("/images/outer-space-background.jpg"));
        }catch(Exception e){
            System.err.println("Hintergrundbild nicht gefunden: " + e.getMessage());
            rootLayout.setStyle("-fx-background-color: #6c4675;");
        }
        StackPane backgroundPane = new StackPane();
        if (backgroundImage != null) {
            ImageView backgroundImageView = new ImageView(backgroundImage);
            backgroundImageView.setFitHeight(900);
            backgroundImageView.setFitWidth(650);
            backgroundImageView.setPreserveRatio(true);
            Rectangle tintOverlay = new Rectangle(900,650);
            tintOverlay.setFill(Color.rgb(0,0,50,0.3));
            backgroundPane.getChildren().addAll(backgroundImageView, tintOverlay);
        }
        StackPane sceneRoot = new StackPane(backgroundPane, rootLayout);

        Label titleLabel = new Label("Welcome to Musical Invaders");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web("#333333"));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        rootLayout.setTop(titleLabel);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 30, 0));

        VBox voiceSelectionBox = new VBox(10);
        voiceSelectionBox.setPadding(new Insets(15));
        voiceSelectionBox.setStyle("-fx-background-color: rgba(249, 249, 249, 0.85); " + // Halbtransparentes Weiß
                "-fx-border-color: #CCCCCC; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");
        voiceSelectionBox.setPrefWidth(350);
        Label chooseVoiceLabel = new Label("Choose your Voice");
        chooseVoiceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        voiceSelectionBox.getChildren().add(chooseVoiceLabel);

        ListView<VoiceProfile> voiceListView = getVoiceProfileListView();
        ScrollPane scrollPane = new ScrollPane(voiceListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        voiceSelectionBox.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        rootLayout.setLeft(voiceSelectionBox);
        BorderPane.setMargin(voiceSelectionBox, new Insets(0, 20, 0, 0));

        voiceListView.setCellFactory(param -> new ListCell<VoiceProfile>(){
            private final HBox cellContent = new HBox(10);
            private final Label nameLabel = new Label();
            private final Button playButton = new Button("▶");
            private final Region spacer = new Region();
            {
                playButton.setStyle("-fx-font-size: 12px; -fx-min-width: 35px; -fx-pref-width: 35px; " +
                        "-fx-alignment: CENTER;"); // Zentriert das Symbol im Button
                playButton.setFocusTraversable(false); // Nimmt keinen Fokus beim Tabben

                // Label soll wachsen
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                nameLabel.setMaxWidth(Double.MAX_VALUE); // Erlaube dem Label, den verfügbaren Platz zu nutzen
                nameLabel.setFont(Font.font("Arial", 15)); // Größere Schrift für die Stimmen

                // Spacer, um den Button nach rechts zu drücken
                HBox.setHgrow(spacer, Priority.ALWAYS);

                cellContent.setAlignment(Pos.CENTER_LEFT); // Vertikale Zentrierung in der HBox
                cellContent.getChildren().addAll(nameLabel, spacer, playButton); // Spacer hinzugefügt
                // Padding für jede Zelle, damit die Elemente nicht am Rand kleben
                cellContent.setPadding(new Insets(5, 5, 5, 10)); // Oben, Rechts, Unten, Links
            }
            @Override
            protected void updateItem(VoiceProfile voiceProfile, boolean empty) {
                super.updateItem(voiceProfile, empty);
                if (empty || voiceProfile == null) {
                    setText(null);
                    setGraphic(null);
                }else{
                    nameLabel.setText(voiceProfile.getDisplayName());
                    playButton.setOnAction(event -> {
                        playIntro(voiceProfile);
                        event.consume();
                    });
                    setGraphic(cellContent);
                }
            }
        });

        VBox infoArea = new VBox(15);
        infoArea.setPadding(new Insets(15));
        infoArea.setStyle("-fx-background-color: rgba(249, 249, 249, 0.85); " +
                "-fx-border-color: #CCCCCC; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");
        Label infoTitleLabel = new Label("'Info über die Stimme");
        infoTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        TextArea infoTextArea = new TextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setWrapText(true);
        infoTextArea.setPrefHeight(300); // Angepasste Höhe
        if (currentlyPreviewedVoice != null) {
            infoTextArea.setText(currentlyPreviewedVoice.getInfoText());
        }
        voiceListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentlyPreviewedVoice = newSelection; // Aktualisiere die für Preview/Bestätigung gewählte Stimme
                infoTextArea.setText(newSelection.getInfoText());
            }
        });
        if (!voiceProfiles.isEmpty()) {
            voiceListView.getSelectionModel().selectFirst(); // Wähle das erste Element standardmäßig aus
        }

        Button confirmButton = new Button("Mit dieser Stimme fortfahren?");
        confirmButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px 20px;");
        confirmButton.setOnAction(event -> {
            if (voiceListView.getSelectionModel().getSelectedItem() != null) { // Prüfe die Auswahl aus der ListView
                selectedVoiceProfile = voiceListView.getSelectionModel().getSelectedItem();
                System.out.println("Stimme ausgewählt für Spiel: " + selectedVoiceProfile.getDisplayName());
                System.out.println("SFX Ordner: " + selectedVoiceProfile.getSfxFolderPath());
                startGameScene(); // Wechsle zur Spielszene
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Bitte wähle zuerst eine Stimme aus der Liste.");
                alert.showAndWait();
            }
        });
        infoArea.getChildren().addAll(infoTitleLabel, infoTextArea, confirmButton);
        VBox.setVgrow(infoTextArea, Priority.ALWAYS);
        rootLayout.setCenter(infoArea);

        return new Scene(sceneRoot, 900, 650); // Etwas größere Szene für das Menü
    }

    @NotNull
    private ListView<VoiceProfile> getVoiceProfileListView() {
        ListView<VoiceProfile> voiceListView = new ListView<>(voiceProfiles);
        voiceListView.setPrefHeight(350); // Angepasste Höhe
        voiceListView.setCellFactory(param -> new ListCell<VoiceProfile>() {
            private final HBox content = new HBox(10);
            private final Label nameLabel = new Label();
            private final Button playButton = new Button("▶");
            {
                playButton.setStyle("-fx-font-size: 10px; -fx-min-width: 30px; -fx-pref-width: 30px;");
                content.setAlignment(Pos.CENTER_LEFT);
                content.getChildren().addAll(nameLabel, playButton);
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
            }
            @Override
            protected void updateItem(VoiceProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(profile.getDisplayName());
                    playButton.setOnAction(event -> playIntro(profile));
                    setGraphic(content);
                }
            }
        });
        return voiceListView;
    }

    private void playIntro(VoiceProfile profile) {
        if (profile == null || profile.getIntroAudioClip() == null) {
            System.err.println("Kein Intro-Clip verfügbar für: " + (profile != null ? profile.getDisplayName() : "Unbekannt"));
            return;
        }
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying()) {
            currentPlayingIntro.stop();
        }
        currentPlayingIntro = profile.getIntroAudioClip();
        currentPlayingIntro.play();
    }

    // Methode zum Erstellen und Starten der Spielszene
    private void startGameScene() {
        if (selectedVoiceProfile == null) {
            System.err.println("Keine Stimme für das Spiel ausgewählt!");
            // Zeige evtl. eine Fehlermeldung und bleibe im Menü
            Alert alert = new Alert(Alert.AlertType.ERROR, "Fehler: Keine Stimme ausgewählt. Bitte wähle eine Stimme im Menü.");
            alert.showAndWait();
            return;
        }

        // Bildschirmauflösung und Fenstergrößen für das Spiel ermitteln
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        double windowHeight = bounds.getHeight();
        double windowWidth = windowHeight * (4.0 / 3.0);
        if (windowWidth > bounds.getWidth()) {
            windowWidth = bounds.getWidth();
            windowHeight = windowWidth * (3.0 / 4.0);
        }
        this.gameDimensions = new GameDimensions(windowWidth, windowHeight);

        // Panes für das Spiel initialisieren
        gamePane = new Pane();
        gamePane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        gamePane.setStyle("-fx-background-color: #1a1a1a;"); // Dein Spielhintergrund

        uiPane = new Pane();
        uiPane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        uiPane.setMouseTransparent(true);

        StackPane gameRootPane = new StackPane(gamePane, uiPane);
        gameScene = new Scene(gameRootPane, gameDimensions.getWidth(), gameDimensions.getHeight());

        // Spiel-Manager instanziieren (jetzt, da wir die gameDimensions und das selectedVoiceProfile haben)
        inputHandler = new InputHandler(gameScene); // Input an die Spiel-Szene binden!
        gameUIManager = new UIManager(uiPane, gameDimensions); // UIManager für die Spiel-UI
        // Hier könntest du dem entityManager oder einem SoundManager das selectedVoiceProfile übergeben
        entityManager = new GameEntityManager(gamePane, gameDimensions, gameUIManager /*, selectedVoiceProfile.getSfxFolderPath() */);
        gameUpdater = new GameUpdater(entityManager, inputHandler, gameDimensions, gameUIManager /*, selectedVoiceProfile.getSfxFolderPath() */);

        // Initiale Spielobjekte und Spiel-UI erstellen
        entityManager.createPlayer(); // Player-SFX könnten hier geladen werden
        entityManager.spawnEnemyWaveInitial();
        gameUIManager.createScoreLabel();

        // Game Loop starten
        if (gameLoop != null) { // Stoppe alten Loop, falls vorhanden (z.B. nach Neustart)
            gameLoop.stop();
        }
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                // double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                gameUpdater.update(now, 0.016); // Feste deltaTime für jetzt oder berechne sie
            }
        };
        gameLoop.start();

        primaryStage.setScene(gameScene);
        System.out.println("Spielszene gestartet mit Stimme: " + selectedVoiceProfile.getDisplayName());
    }

    // Methode, um zum Hauptmenü zurückzukehren (z.B. bei Game Over)
    public void showMainMenu() {
        if (gameLoop != null) {
            gameLoop.stop(); // Wichtig: Game Loop anhalten!
        }
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying()) {
            currentPlayingIntro.stop();
        }
        // Setze Spielzustände zurück, falls nötig
        selectedVoiceProfile = null;
        // currentlyPreviewedVoice zurücksetzen, wenn das Menü neu aufgebaut wird oder die ListView aktualisiert
        if (!voiceProfiles.isEmpty()) {
            currentlyPreviewedVoice = voiceProfiles.get(0);
        }

        // Stelle sicher, dass die mainMenuScene neu erstellt oder zumindest aktualisiert wird,
        // um die Auswahl in der ListView etc. zurückzusetzen.
        // Sicherer ist oft, die Szene neu zu erstellen, wenn der Zustand komplex ist.
        // Für dieses Beispiel erstellen wir sie neu, um einen sauberen Zustand zu haben.
        mainMenuScene = createMainMenuScene();
        primaryStage.setScene(mainMenuScene);
        System.out.println("Zurück zum Hauptmenü.");
    }


    public static void main(String[] args) {
        launch(args);
    }
}