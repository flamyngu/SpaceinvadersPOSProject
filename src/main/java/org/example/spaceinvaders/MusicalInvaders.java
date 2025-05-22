package org.example.spaceinvaders;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

// Importiere VoiceProfile korrekt, z.B. wenn es in einem Unterordner 'menu' liegt:
// import org.example.spaceinvaders.menu.VoiceProfile;

import java.util.Objects; // Für Objects.requireNonNull

public class MusicalInvaders extends Application {

    private Stage primaryStage;
    private Scene mainMenuScene;
    private Scene gameScene;

    private GameState currentGameState;
    private StackPane pauseMenuPane;
    private StackPane gameOverMenuPane;
    private StackPane creditsMenuPane;
    private StackPane gameRootPane;
    private ScrollPane creditsScrollPane; // Member-Variable für Zugriff in Animation
    private VBox creditsContentBox;       // Inhalt der Credits
    private Timeline creditRollTimeline;  // Für die Animation// Root Pane der Spielszene, wichtig für Overlays

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

    // Konsistenter CSS-Pfad (angenommen, es liegt direkt in resources oder resources/css)
    private static final String MAIN_MENU_CSS_PATH = "/mainmenu.css"; // Wenn direkt in resources
    // private static final String MAIN_MENU_CSS_PATH = "/css/mainmenu.css"; // Wenn in resources/css

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Musical Invaders");

        // Programmatisches Laden der Schriftart (empfohlen)
        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PressStart2P-Regular.ttf"), 10);
            System.out.println("Schriftart 'Press Start 2P' programmatisch geladen.");
        } catch (Exception e) {
            System.err.println("Fehler beim programmatischen Laden der Schriftart '/fonts/PressStart2P-Regular.ttf': " + e.getMessage());
        }

        loadVoiceProfiles();
        initializeGlobalInput(primaryStage); // Globale Tasten (ESC für Pause)
        createMenuOverlays();                // Erstellt die unsichtbaren Overlay-Panes

        changeGameState(GameState.MAIN_MENU); // Startet im Hauptmenü
        if (mainMenuScene != null) { // Sicherstellen, dass die Menü-Szene erstellt wurde
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX((screenBounds.getWidth() - mainMenuScene.getWidth()) / 2);
            primaryStage.setY((screenBounds.getHeight() - mainMenuScene.getHeight()) / 2);
            System.out.println("Hauptmenü-Fenster zentriert.");
        } else {
            // Fallback, falls die Szene noch nicht da ist (sollte nicht passieren bei diesem Ablauf)
            primaryStage.centerOnScreen(); // Einfachere Methode, falls Größe noch nicht bekannt
            System.out.println("Hauptmenü-Fenster mit centerOnScreen() zentriert (Fallback).");
        }
        this.primaryStage.setResizable(false);
        this.primaryStage.show();
    }

    private void loadVoiceProfiles() {
        // Deine VoiceProfile-Liste...
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
                new VoiceProfile("Stimme Beta", "/sfx/StimmeBeta/intro_StimmeBeta.wav", "/sfx/StimmeBeta/", "Teststimme Beta.")

                // Füge hier alle deine Profile hinzu
        );
        if (!voiceProfiles.isEmpty()) {
            currentlyPreviewedVoice = voiceProfiles.get(0); // Standard-Preview
        }
    }

    private void initializeGlobalInput(Stage stage) {
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (currentGameState == GameState.PLAYING) {
                    changeGameState(GameState.PAUSED);
                } else if (currentGameState == GameState.PAUSED) {
                    changeGameState(GameState.PLAYING); // Zurück zum Spiel
                }
                event.consume();
            }
        });
    }

    private void createMenuOverlays() {
        // PAUSE MENÜ
        pauseMenuPane = new StackPane();
        pauseMenuPane.setId("pause-menu-pane");
        Label pauseLabel = new Label("PAUSED");
        pauseLabel.getStyleClass().add("menu-title"); // CSS-Klasse für Menütitel
        Button resumeButton = new Button("Resume");
        resumeButton.getStyleClass().add("menu-button");
        resumeButton.setOnAction(e -> changeGameState(GameState.PLAYING));
        Button backToMainMenuFromPauseButton = new Button("Back to Main Menu");
        backToMainMenuFromPauseButton.getStyleClass().add("menu-button");
        backToMainMenuFromPauseButton.setOnAction(e -> changeGameState(GameState.MAIN_MENU));
        VBox pauseContent = new VBox(20, pauseLabel, resumeButton, backToMainMenuFromPauseButton);
        pauseContent.setAlignment(Pos.CENTER);
        pauseContent.getStyleClass().add("menu-content-box");
        pauseMenuPane.getChildren().add(pauseContent);
        pauseMenuPane.setVisible(false);

        // GAME OVER MENÜ
        gameOverMenuPane = new StackPane();
        gameOverMenuPane.setId("game-over-menu-pane");
        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.getStyleClass().add("menu-title");
        Button restartButton = new Button("Try Again (Main Menu)"); // Geht erstmal zum Hauptmenü
        restartButton.getStyleClass().add("menu-button");
        restartButton.setOnAction(e -> {
            // Für einen echten Neustart mit derselben Stimme:
            // resetGameLogic(); // Methode, die Spielvariablen zurücksetzt
            // changeGameState(GameState.PLAYING);
            changeGameState(GameState.MAIN_MENU); // Vorerst zurück zum Hauptmenü
        });
        Button backToMainMenuFromGameOverButton = new Button("Back to Main Menu");
        backToMainMenuFromGameOverButton.getStyleClass().add("menu-button");
        backToMainMenuFromGameOverButton.setOnAction(e -> changeGameState(GameState.MAIN_MENU));
        VBox gameOverContent = new VBox(20, gameOverLabel, restartButton, backToMainMenuFromGameOverButton);
        gameOverContent.setAlignment(Pos.CENTER);
        gameOverContent.getStyleClass().add("menu-content-box");
        gameOverMenuPane.getChildren().add(gameOverContent);
        gameOverMenuPane.setVisible(false);

        // CREDITS MENÜ
        creditsMenuPane = new StackPane();
        creditsMenuPane.setId("credits-menu-pane");
        creditsMenuPane.setVisible(false);
        creditsContentBox = new VBox(10);
        creditsContentBox.setAlignment(Pos.CENTER);
        creditsContentBox.setId("credits-text-container");

        creditsScrollPane = new ScrollPane(creditsContentBox);
        creditsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        creditsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        creditsScrollPane.setFitToWidth(true);
        creditsScrollPane.getStyleClass().add("credits-scroll-pane");

        creditsMenuPane.getChildren().add(creditsScrollPane);
    }

    public void changeGameState(GameState newState) {
        // Verhindere unnötige Zustandswechsel, außer wenn wir das Hauptmenü "neu laden" wollen
        if (this.currentGameState == newState && newState != GameState.MAIN_MENU) {
            return;
        }
        System.out.println("Changing state from " + this.currentGameState + " to " + newState);
        GameState previousState = this.currentGameState;
        this.currentGameState = newState;

        // Overlays standardmäßig ausblenden
        pauseMenuPane.setVisible(false);
        gameOverMenuPane.setVisible(false);
        creditsMenuPane.setVisible(false);

        // GameLoop stoppen, wenn nicht mehr gespielt wird
        if (gameLoop != null && newState != GameState.PLAYING) {
            gameLoop.stop();
            System.out.println("GameLoop stopped.");
        }
        // Intro-Sound stoppen, wenn Menü verlassen wird
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying() && newState != GameState.MAIN_MENU) {
            currentPlayingIntro.stop();
        }

        switch (newState) {
            case MAIN_MENU:
                selectedVoiceProfile = null; // Stimme für nächstes Spiel zurücksetzen
                if (voiceProfiles != null && !voiceProfiles.isEmpty()) { // Sicherstellen, dass voiceProfiles initialisiert ist
                    currentlyPreviewedVoice = voiceProfiles.get(0);
                }
                mainMenuScene = createMainMenuScene(); // Erstellt oder aktualisiert die Menü-Szene
                primaryStage.setScene(mainMenuScene);
                break;

            case PLAYING:
                if (selectedVoiceProfile == null) {
                    System.err.println("Keine Stimme ausgewählt. Zurück zum Hauptmenü.");
                    changeGameState(GameState.MAIN_MENU); // Erzwinge Rückkehr zum Menü

                    return;
                }
                // Wenn wir aus der Pause kommen oder das Spiel zum ersten Mal starten (nach Menüauswahl)
                if (previousState == GameState.PAUSED) {
                    // Spielszene existiert bereits, Loop wieder starten
                    if (primaryStage.getScene() != gameScene) primaryStage.setScene(gameScene); // Nur wenn Szene gewechselt wurde
                } else {
                    // Neues Spiel oder erstes Spiel nach Menü
                    initializeGame(); // Bereitet Spielvariablen und Szene vor
                    primaryStage.setScene(gameScene);
                }
                if (gameLoop != null) {
                    gameLoop.start();
                    System.out.println("GameLoop started/resumed.");
                }
                break;

            case PAUSED:
                if (gameRootPane != null) { // Nur wenn Spielszene existiert
                    if (!gameRootPane.getChildren().contains(pauseMenuPane)) {
                        gameRootPane.getChildren().add(pauseMenuPane);
                    }
                    pauseMenuPane.setVisible(true);
                    pauseMenuPane.toFront();
                }
                break;

            case GAME_OVER:
                if (gameRootPane != null) {
                    if (!gameRootPane.getChildren().contains(gameOverMenuPane)) {
                        gameRootPane.getChildren().add(gameOverMenuPane);
                    }
                    gameOverMenuPane.setVisible(true);
                    gameOverMenuPane.toFront();
                }
                break;

            case CREDITS:
                // Stoppe den GameLoop explizit, falls er noch lief (z.B. wenn Credits direkt nach dem Spiel kommen)
                if (gameLoop != null) {
                    gameLoop.stop();
                    System.out.println("GameLoop stopped for Credits.");
                }

                // Bevorzugte Methode: Credits als Overlay über der aktuellen Szene (Spiel oder Menü)
                // Wir brauchen den Root-Node der aktuellen Szene, um das Overlay hinzuzufügen.
                Node currentSceneRootNode = primaryStage.getScene() != null ? primaryStage.getScene().getRoot() : null;

                if (mainMenuScene != null && mainMenuScene.getRoot() instanceof StackPane) {
                    StackPane menuRoot = (StackPane) mainMenuScene.getRoot();
                    if (!menuRoot.getChildren().contains(creditsMenuPane)) {
                        menuRoot.getChildren().add(creditsMenuPane);
                    }
                    creditsMenuPane.setVisible(true);
                    creditsMenuPane.toFront();
                    primaryStage.setScene(mainMenuScene); // Bleibe auf der Hauptmenü-Szene
                    System.out.println("Credits als Overlay zum Hauptmenü hinzugefügt.");
                } else {
                    // Fallback: Wenn die aktuelle Szene keinen StackPane als Root hat
                    // oder keine Szene gesetzt ist (unwahrscheinlich), erstelle eine neue Szene nur für Credits.
                    // Dies sollte idealerweise vermieden werden, um den Kontext nicht zu verlieren.
                    System.out.println("Fallback: Erstelle neue Szene für Credits."); // Debug
                    double w = (gameDimensions != null) ? gameDimensions.getWidth() : 900;
                    double h = (gameDimensions != null) ? gameDimensions.getHeight() : 650;
                    Scene creditsOnlyScene = new Scene(creditsMenuPane, w, h); // creditsMenuPane ist hier der Root
                    try {
                        // HIER müsste das CSS explizit für die neue Szene geladen werden
                        String cssPath = Objects.requireNonNull(getClass().getResource(MAIN_MENU_CSS_PATH)).toExternalForm();
                        creditsOnlyScene.getStylesheets().add(cssPath); // <<--- WICHTIG
                        System.out.println("CSS für separate Credits-Szene geladen: " + cssPath);
                    } catch (Exception e) {
                        System.err.println("CSS für Credits-Szene nicht gefunden (" + MAIN_MENU_CSS_PATH + "): " + e.getMessage());
                    }
                    creditsMenuPane.setVisible(true); // Stelle sicher, dass das Root-Pane der Szene sichtbar ist
                    primaryStage.setScene(creditsOnlyScene);
                }
                startCreditRoll(); // Starte die Animation
                break;

            case LEVEL_TRANSITION:
                // Hier Logik für Wellenübergang einfügen
                System.out.println("Level Transition (Placeholder)");
                // Nach kurzer Pause: changeGameState(GameState.PLAYING);
                break;
        }
    }

    private void startCreditRoll() {
        creditsContentBox.getChildren().clear();
        addCreditEntry("", "");
        for(VoiceProfile voiceProfile : voiceProfiles) {
            addCreditEntry("- " + voiceProfile.getDisplayName(), "");
        }
        addCreditEntry("", "");

        creditsScrollPane.setVvalue(0.0);

        double contentHeight = voiceProfiles.size() * 30+20*30;
        double scrollPaneHeight = creditsContentBox.getHeight(); //könnte ein problem sein, falls fehler => gameDimensions.getHeight()

        scrollPaneHeight = gameDimensions.getHeight()*0.8;

        if(creditRollTimeline != null){
            creditRollTimeline.stop();
        }
        PauseTransition pt = new PauseTransition(Duration.millis(100));
        pt.setOnFinished(event -> {
            double actualHeight = creditsContentBox.getBoundsInLocal().getHeight();
            double visibleHeight = creditsScrollPane.getViewportBounds().getHeight();
            if(visibleHeight <= 0) visibleHeight = gameDimensions.getHeight()*0.8;

            creditsContentBox.setTranslateY(visibleHeight);

            creditRollTimeline = new Timeline();
            double scrollSpeed = 0.05;
            double durrationMillis = (actualHeight + visibleHeight)/scrollSpeed;

            KeyValue kv = new KeyValue(creditsContentBox.translateYProperty(), -actualHeight);
            KeyFrame kf = new KeyFrame(Duration.millis(durrationMillis), kv);
            creditRollTimeline.getKeyFrames().add(kf);
            creditRollTimeline.setOnFinished(e ->{
                changeGameState(GameState.MAIN_MENU);
            });
            creditRollTimeline.play();
        });
        pt.play();
    }

    private void addCreditEntry(String titel, String names) {
        Label titleLabel = new Label(titel);
        titleLabel.getStyleClass().add("credit-title-label");
        creditsContentBox.getChildren().add(titleLabel);

        if(names != null && !names.isEmpty()) {
            Label namesLabel = new Label(names);
            namesLabel.getStyleClass().add("credit-names-label");
            namesLabel.setWrapText(true);
            creditsContentBox.getChildren().add(namesLabel);
            VBox.setMargin(namesLabel, new Insets(0, 0,15,0));
        }
    }

    private Scene createMainMenuScene() {
        // Dein bestehender Code für createMainMenuScene(), stelle sicher, dass IDs und StyleClasses gesetzt sind
        // und der Confirm-Button changeGameState(GameState.PLAYING) aufruft.
        // Der Credits-Button ruft changeGameState(GameState.CREDITS) auf.
        // (Code von dir hier einfügen, angepasst für Klarheit)

        BorderPane rootLayout = new BorderPane();
        rootLayout.setId("main-menu-layout");

        StackPane backgroundPane = new StackPane();
        backgroundPane.setId("main-menu-background-container");
        Image backgroundImage = null;
        try { backgroundImage = new Image(getClass().getResourceAsStream("/images/outer-space-background.jpg")); }
        catch (Exception e) { System.err.println("Hintergrundbild nicht gefunden: " + e.getMessage()); }
        if (backgroundImage != null) {
            ImageView backgroundImageView = new ImageView(backgroundImage);
            backgroundImageView.fitWidthProperty().bind(backgroundPane.widthProperty());
            backgroundImageView.fitHeightProperty().bind(backgroundPane.heightProperty());
            backgroundImageView.setPreserveRatio(false);
            Rectangle tintOverlay = new Rectangle();
            tintOverlay.widthProperty().bind(backgroundPane.widthProperty());
            tintOverlay.heightProperty().bind(backgroundPane.heightProperty());
            tintOverlay.setFill(Color.rgb(0, 0, 20, 0.5));
            tintOverlay.setId("tint-overlay");
            backgroundPane.getChildren().addAll(backgroundImageView, tintOverlay);
        }
        StackPane sceneRoot = new StackPane(backgroundPane, rootLayout);

        try {
            String cssPath = getClass().getResource(MAIN_MENU_CSS_PATH).toExternalForm();
            sceneRoot.getStylesheets().add(cssPath);
        } catch (Exception e) { System.err.println("CSS-Datei für Hauptmenü nicht gefunden: " + e.getMessage()); }

        Label titleLabel = new Label("Welcome");
        titleLabel.setId("title-label");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        rootLayout.setTop(titleLabel);

        VBox voiceSelectionBox = new VBox();
        voiceSelectionBox.setId("voice-selection-box");
        voiceSelectionBox.setPrefWidth(380);
        Label chooseVoiceLabel = new Label("Choose your Voice");
        chooseVoiceLabel.getStyleClass().add("section-title");
        voiceSelectionBox.getChildren().add(chooseVoiceLabel);
        ListView<VoiceProfile> voiceListView = new ListView<>(voiceProfiles);
        voiceListView.setId("voice-list-view");
        voiceListView.setCellFactory(param -> new ListCell<VoiceProfile>() { /* ... Deine CellFactory ... */
            private final HBox cellContent = new HBox();
            private final Label nameLabel = new Label();
            private final Button playButton = new Button("▶");
            private final Region spacer = new Region();
            private static final javafx.css.PseudoClass FIRST_CELL_PSEUDO_CLASS = javafx.css.PseudoClass.getPseudoClass("first-cell");
            private static final javafx.css.PseudoClass LAST_CELL_PSEUDO_CLASS = javafx.css.PseudoClass.getPseudoClass("last-cell");
            { /* ... Initialisierung der Zellenelemente ... */
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
                if (empty || profile == null) { setGraphic(null); setText(null); }
                else {
                    nameLabel.setText(profile.getDisplayName());
                    playButton.setOnAction(event -> { playIntro(profile); event.consume(); });
                    setGraphic(cellContent);
                    ListView<VoiceProfile> listView = getListView();
                    if (listView != null && listView.getItems() != null && !listView.getItems().isEmpty()){
                        int currentIndex = getIndex(); int totalItems = listView.getItems().size();
                        if (currentIndex == 0) pseudoClassStateChanged(FIRST_CELL_PSEUDO_CLASS, true);
                        if (currentIndex == totalItems - 1) pseudoClassStateChanged(LAST_CELL_PSEUDO_CLASS, true);
                    }
                }
            }
        });
        ScrollPane scrollPane = new ScrollPane(voiceListView);
        scrollPane.setFitToWidth(true); scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        voiceSelectionBox.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        rootLayout.setLeft(voiceSelectionBox);

        VBox infoArea = new VBox();
        infoArea.setId("info-area");
        Label infoTitleLabel = new Label("Everyone has a story to tell");
        infoTitleLabel.getStyleClass().add("section-title");
        TextArea infoTextArea = new TextArea();
        infoTextArea.setEditable(false); infoTextArea.setWrapText(true); infoTextArea.setId("info-text-area");
        if (currentlyPreviewedVoice != null) infoTextArea.setText(currentlyPreviewedVoice.getInfoText());
        voiceListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> { if (n != null) { currentlyPreviewedVoice = n; infoTextArea.setText(n.getInfoText()); } });
        if (voiceProfiles != null && !voiceProfiles.isEmpty()) voiceListView.getSelectionModel().selectFirst();
        Button confirmButton = new Button("Confirm selected Voice for SFX");
        confirmButton.setId("confirm-voice-button");
        confirmButton.setOnAction(event -> {
            if (voiceListView.getSelectionModel().getSelectedItem() != null) {
                selectedVoiceProfile = voiceListView.getSelectionModel().getSelectedItem();
                changeGameState(GameState.PLAYING);
            } else { new Alert(Alert.AlertType.WARNING, "Bitte wähle zuerst eine Stimme.").showAndWait(); }
        });
        infoArea.getChildren().addAll(infoTitleLabel, infoTextArea, confirmButton);
        VBox.setVgrow(infoTextArea, Priority.ALWAYS);
        rootLayout.setCenter(infoArea);

        Button creditsButton = new Button("Credits");
        creditsButton.getStyleClass().add("menu-button-bottom");
        creditsButton.setOnAction(e -> changeGameState(GameState.CREDITS));
        BorderPane.setAlignment(creditsButton, Pos.BOTTOM_RIGHT);
        BorderPane.setMargin(creditsButton, new Insets(10));
        rootLayout.setBottom(creditsButton);

        return new Scene(sceneRoot, 900, 650);
    }

    // Umbenannt und zentralisiert für die Spielinitialisierung
    private void initializeGame() {
        // Bildschirmauflösung (kann global sein, wenn Menü und Spiel dieselbe Auflösung haben)
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        double windowHeight = bounds.getHeight();
        double windowWidth = windowHeight * (4.0 / 3.0); // Behalte dein Seitenverhältnis
        if (windowWidth > bounds.getWidth()) { windowWidth = bounds.getWidth(); windowHeight = windowWidth * (3.0 / 4.0); }
        this.gameDimensions = new GameDimensions(windowWidth, windowHeight);

        // Spiel-Panes erstellen/zurücksetzen
        gamePane = new Pane();
        gamePane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        gamePane.setId("game-pane"); // ID für potenzielles CSS
        gamePane.setStyle("-fx-background-color: #1a1a1a;"); // Dein Spielhintergrund

        uiPane = new Pane();
        uiPane.setId("ui-pane");
        uiPane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        uiPane.setMouseTransparent(true);

        // Root-Pane für die Spielszene (wichtig für Overlays)
        gameRootPane = new StackPane(gamePane, uiPane);
        // In MusicalInvaders.initializeGame(), nachdem gamePane dem gameRootPane hinzugefügt wurde:
        System.out.println("GamePane in Scene - LayoutX: " + gamePane.getLayoutX() + ", LayoutY: " + gamePane.getLayoutY());
        System.out.println("GamePane in Scene - TranslateX: " + gamePane.getTranslateX() + ", TranslateY: " + gamePane.getTranslateY());
        System.out.println("GamePane Bounds in Parent (gameRootPane): " + gamePane.getBoundsInParent());
        // Füge Overlay-Panes hinzu (werden durch changeGameState sichtbar/unsichtbar)
        if (!gameRootPane.getChildren().contains(pauseMenuPane)) gameRootPane.getChildren().add(pauseMenuPane);
        if (!gameRootPane.getChildren().contains(gameOverMenuPane)) gameRootPane.getChildren().add(gameOverMenuPane);
        pauseMenuPane.setVisible(false); // Stelle sicher, dass sie anfangs unsichtbar sind
        gameOverMenuPane.setVisible(false);

        gameScene = new Scene(gameRootPane, gameDimensions.getWidth(), gameDimensions.getHeight());

        // InputHandler an die NEUE Spielszene binden
        inputHandler = new InputHandler(gameScene);

        // Spiel-Manager instanziieren oder zurücksetzen
        // WICHTIG: Passe die Konstruktoren deiner Manager-Klassen an, um 'this' (MusicalInvaders-Instanz) zu akzeptieren!
        gameUIManager = new UIManager(uiPane, gameDimensions, this);
        entityManager = new GameEntityManager(gamePane, gameDimensions, gameUIManager /*, this // Falls benötigt */);
        gameUpdater = new GameUpdater(entityManager, inputHandler, gameDimensions, gameUIManager, this);

        // Spiel initialisieren (Gegner, Spieler, Score etc.)
        gameUIManager.resetScore(); // Wichtig: Score bei Neustart zurücksetzen
        entityManager.resetGame(); // NEUE METHODE im EntityManager zum Zurücksetzen von Wellen, Boss etc.
        entityManager.createPlayer();
        gameUIManager.createScoreLabel();
        entityManager.spawnEnemyWaveInitial(); // Startet Welle 1
         // Stellt sicher, dass das Score-Label da ist

        // Game Loop erstellen, falls nicht vorhanden (wird in changeGameState gestartet)
        if (gameLoop == null) {
            gameLoop = new AnimationTimer() {
                private long lastUpdate = 0;
                @Override
                public void handle(long now) {
                    if (currentGameState != GameState.PLAYING) {
                        lastUpdate = 0; return;
                    }
                    if (lastUpdate == 0) { lastUpdate = now; return; }
                    lastUpdate = now; // Für deltaTime Berechnung
                    // double deltaTime = (now - lastUpdateNanos) / 1_000_000_000.0; // Korrekte deltaTime
                    // lastUpdateNanos = now;
                    gameUpdater.update(now, 0.016); // Feste deltaTime für jetzt
                }
            };
        }
        // In MusicalInvaders.initializeGame(), nachdem gamePane erstellt wurde:
        System.out.println("GamePane Bounds in Parent: " + gamePane.getBoundsInParent());
        System.out.println("GamePane Layout Bounds: " + gamePane.getLayoutBounds());
        System.out.println("GamePane Breite/Höhe: " + gamePane.getWidth() + "/" + gamePane.getHeight()); // Ist oft 0 bis zum ersten Layout-Pass
        System.out.println("GamePane Pref Breite/Höhe: " + gamePane.getPrefWidth() + "/" + gamePane.getPrefHeight());
    }

    // Wird vom GameUpdater aufgerufen
    public void triggerGameOver() {
        changeGameState(GameState.GAME_OVER);
    }

    public VoiceProfile getSelectedVoiceProfile() {
        return selectedVoiceProfile;
    }

    public GameState getCurrentGameState(){
        return this.currentGameState;
    }

    private void playIntro(VoiceProfile profile) {
        // Deine playIntro-Logik...
        if (profile == null || profile.getIntroAudioClip() == null) { System.err.println("Kein Intro-Clip für: " + (profile != null ? profile.getDisplayName() : "Unbekannt")); return; }
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying()) { currentPlayingIntro.stop(); }
        currentPlayingIntro = profile.getIntroAudioClip();
        currentPlayingIntro.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}