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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
// Stelle sicher, dass VoiceProfile korrekt importiert wird, z.B.:
// import org.example.spaceinvaders.menu.VoiceProfile;

public class MusicalInvaders extends Application {

    private Stage primaryStage;
    private Scene mainMenuScene;
    private Scene gameScene;

    private ObservableList<VoiceProfile> voiceProfiles;
    private VoiceProfile currentlyPreviewedVoice = null;
    private VoiceProfile selectedVoiceProfile = null;
    private AudioClip currentPlayingIntro = null;

    private GameDimensions gameDimensions;
    private Pane gamePane;
    private Pane uiPane;
    private GameEntityManager entityManager;
    private GameUpdater gameUpdater;
    private UIManager gameUIManager;
    private InputHandler inputHandler;
    private AnimationTimer gameLoop;

    private static final String MAIN_MENU_CSS_PATH = "/mainmenu.css"; // Pfad zur CSS-Datei

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Musical Invaders");

        loadVoiceProfiles();

        mainMenuScene = createMainMenuScene();
        this.primaryStage.setScene(mainMenuScene);
        this.primaryStage.setResizable(false);
        this.primaryStage.show();
    }

    private void loadVoiceProfiles() {
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
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta.")
        );
        if (!voiceProfiles.isEmpty()) {
            currentlyPreviewedVoice = voiceProfiles.get(0);
        }
    }

    private Scene createMainMenuScene() {
        BorderPane rootLayout = new BorderPane();
        rootLayout.setId("main-menu-layout"); // ID für CSS

        // --- HINTERGRUND (bleibt im Java-Code für dynamisches Laden und Tint) ---
        Image backgroundImage = null;
        try {
            backgroundImage = new Image(getClass().getResourceAsStream("/images/outer-space-background.jpg"));
        } catch (Exception e) {
            System.err.println("Hintergrundbild nicht gefunden: " + e.getMessage());
            // Fallback-Farbe wird über CSS für #main-menu-background gesetzt, falls Bild fehlt
        }
        StackPane backgroundPane = new StackPane(); // Dieser StackPane wird #main-menu-background
        backgroundPane.setId("main-menu-background-container"); // Eigene ID für den Container des Bildes und Tints

        if (backgroundImage != null) {
            ImageView backgroundImageView = new ImageView(backgroundImage);
            // Wichtig: Binde die Größe der ImageView an die Größe des backgroundPane,
            // damit das Bild den gesamten Bereich füllt.
            backgroundImageView.fitWidthProperty().bind(backgroundPane.widthProperty());
            backgroundImageView.fitHeightProperty().bind(backgroundPane.heightProperty());
            backgroundImageView.setPreserveRatio(false); // Erlaube Strecken/Stauchen

            Rectangle tintOverlay = new Rectangle();
            tintOverlay.widthProperty().bind(backgroundPane.widthProperty());
            tintOverlay.heightProperty().bind(backgroundPane.heightProperty());
            // Passe die Tint-Farbe und Opazität an: R, G, B, Alpha (0.0 bis 1.0)
            tintOverlay.setFill(Color.rgb(0, 0, 20, 0.5)); // Beispiel: Sehr dunkles Blau mit 50% Deckkraft
            tintOverlay.setId("tint-overlay"); // Für optionales CSS-Styling, falls benötigt

            backgroundPane.getChildren().addAll(backgroundImageView, tintOverlay);
        } else {
            // Fallback wird durch CSS auf #main-menu-background-container gehandhabt
        }
        // Das rootLayout (BorderPane) wird über das backgroundPane gelegt
        StackPane sceneRoot = new StackPane(backgroundPane, rootLayout);


        // --- CSS LADEN für die gesamte Szene ---
        try {
            String cssPath = getClass().getResource(MAIN_MENU_CSS_PATH).toExternalForm();
            sceneRoot.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("CSS-Datei nicht gefunden: " + MAIN_MENU_CSS_PATH + " - " + e.getMessage());
        }

        // --- TITEL ---
        Label titleLabel = new Label("Welcome");
        titleLabel.setId("title-label");
        BorderPane.setAlignment(titleLabel, Pos.CENTER); // Positionierung bleibt im Java-Code
        rootLayout.setTop(titleLabel);

        // --- LINKER BEREICH: Stimmenauswahl ---
        VBox voiceSelectionBox = new VBox();
        voiceSelectionBox.setId("voice-selection-box");
        voiceSelectionBox.setPrefWidth(380); // Etwas breiter

        Label chooseVoiceLabel = new Label("Choose your Voice");
        chooseVoiceLabel.getStyleClass().add("section-title");
        voiceSelectionBox.getChildren().add(chooseVoiceLabel);

        ListView<VoiceProfile> voiceListView = new ListView<>(voiceProfiles);
        voiceListView.setId("voice-list-view");

        voiceListView.setCellFactory(param -> new ListCell<VoiceProfile>() {
            private final HBox cellContent = new HBox();
            private final Label nameLabel = new Label();
            private final Button playButton = new Button("▶");
            private final Region spacer = new Region();
            private static final javafx.css.PseudoClass FIRST_CELL_PSEUDO_CLASS =
                    javafx.css.PseudoClass.getPseudoClass("first-cell");
            private static final javafx.css.PseudoClass LAST_CELL_PSEUDO_CLASS =
                    javafx.css.PseudoClass.getPseudoClass("last-cell");
            {
                cellContent.getStyleClass().add("voice-list-cell-content");
                nameLabel.getStyleClass().add("voice-name-label");
                playButton.getStyleClass().add("play-intro-button");

                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                cellContent.getChildren().addAll(nameLabel, spacer, playButton);
            }
            @Override
            protected void updateItem(VoiceProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                pseudoClassStateChanged(FIRST_CELL_PSEUDO_CLASS, false);
                pseudoClassStateChanged(LAST_CELL_PSEUDO_CLASS, false);
                if (empty || profile == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    nameLabel.setText(profile.getDisplayName());
                    playButton.setOnAction(event -> {
                        playIntro(profile);
                        event.consume();
                    });
                    setGraphic(cellContent);
                    ListView<VoiceProfile> listView = getListView();
                    if (listView != null && listView.getItems() != null && !listView.getItems().isEmpty()){
                        int currentIndex = getIndex();
                        int totalItems = listView.getItems().size();
                        if (currentIndex == 0) {
                            pseudoClassStateChanged(FIRST_CELL_PSEUDO_CLASS, true);
                        }
                        if (currentIndex == totalItems - 1) {
                            pseudoClassStateChanged(LAST_CELL_PSEUDO_CLASS, true);
                        }
                    }
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(voiceListView);
        scrollPane.setFitToWidth(true); // Wichtig
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        voiceSelectionBox.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        rootLayout.setLeft(voiceSelectionBox);

        // --- RECHTER BEREICH: Info und Bestätigung ---
        VBox infoArea = new VBox();
        infoArea.setId("info-area");

        Label infoTitleLabel = new Label("Voice Profile");
        infoTitleLabel.getStyleClass().add("section-title");

        TextArea infoTextArea = new TextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setWrapText(true);
        infoTextArea.setId("info-text-area");
        infoTextArea.setPrefHeight(250); // Höhe kann auch über CSS (min/pref/max) gesteuert werden

        if (currentlyPreviewedVoice != null) {
            infoTextArea.setText(currentlyPreviewedVoice.getInfoText());
        }
        voiceListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentlyPreviewedVoice = newSelection;
                infoTextArea.setText(newSelection.getInfoText());
            }
        });
        if (!voiceProfiles.isEmpty()) {
            voiceListView.getSelectionModel().selectFirst();
        }

        Button confirmButton = new Button("Confirm selected Voice for SFX");
        confirmButton.setId("confirm-voice-button");
        confirmButton.setOnAction(event -> {
            if (voiceListView.getSelectionModel().getSelectedItem() != null) {
                selectedVoiceProfile = voiceListView.getSelectionModel().getSelectedItem();
                System.out.println("Stimme ausgewählt für Spiel: " + selectedVoiceProfile.getDisplayName());
                startGameScene();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Bitte wähle zuerst eine Stimme aus der Liste.");
                alert.showAndWait();
            }
        });
        infoArea.getChildren().addAll(infoTitleLabel, infoTextArea, confirmButton);
        VBox.setVgrow(infoTextArea, Priority.ALWAYS);
        rootLayout.setCenter(infoArea);

        return new Scene(sceneRoot, 900, 650);
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

    private void startGameScene() {
        if (selectedVoiceProfile == null) {
            System.err.println("Keine Stimme für das Spiel ausgewählt!");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Fehler: Keine Stimme ausgewählt.");
            alert.showAndWait();
            return;
        }

        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        double windowHeight = bounds.getHeight();
        double windowWidth = windowHeight * (4.0 / 3.0);
        if (windowWidth > bounds.getWidth()) {
            windowWidth = bounds.getWidth();
            windowHeight = windowWidth * (3.0 / 4.0);
        }
        this.gameDimensions = new GameDimensions(windowWidth, windowHeight);

        gamePane = new Pane();
        gamePane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        gamePane.setStyle("-fx-background-color: #1a1a1a;"); // Spielhintergrund bleibt hier per Style

        uiPane = new Pane();
        uiPane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        uiPane.setMouseTransparent(true);

        StackPane gameRootPane = new StackPane(gamePane, uiPane);
        gameScene = new Scene(gameRootPane, gameDimensions.getWidth(), gameDimensions.getHeight());

        // Hier könntest du auch eine separate CSS-Datei für die Spielszene laden
        // gameScene.getStylesheets().add(getClass().getResource("/css/game.css").toExternalForm());


        inputHandler = new InputHandler(gameScene);
        gameUIManager = new UIManager(uiPane, gameDimensions);
        entityManager = new GameEntityManager(gamePane, gameDimensions, gameUIManager /*, selectedVoiceProfile*/);
        gameUpdater = new GameUpdater(entityManager, inputHandler, gameDimensions, gameUIManager /*, selectedVoiceProfile*/);

        entityManager.createPlayer();
        entityManager.spawnEnemyWaveInitial();
        gameUIManager.createScoreLabel();

        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now; return;
                }
                lastUpdate = now;
                gameUpdater.update(now, 0.016);
            }
        };
        gameLoop.start();

        primaryStage.setScene(gameScene);
    }

    public void showMainMenu() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying()) {
            currentPlayingIntro.stop();
        }
        selectedVoiceProfile = null;
        if (!voiceProfiles.isEmpty()) {
            currentlyPreviewedVoice = voiceProfiles.get(0);
        }
        mainMenuScene = createMainMenuScene(); // Erstellt Menü neu mit CSS
        primaryStage.setScene(mainMenuScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}