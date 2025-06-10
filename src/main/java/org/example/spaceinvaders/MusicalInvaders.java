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

import java.util.Objects;

public class MusicalInvaders extends Application {

    private Stage primaryStage;
    private Scene mainMenuScene;
    private Scene gameScene;

    private GameState currentGameState;
    private StackPane pauseMenuPane;
    private StackPane gameOverMenuPane;
    private StackPane creditsMenuPane;
    private ScrollPane creditsScrollPane;
    private VBox creditsContentBox;
    private Timeline creditRollTimeline;

    private StackPane gameRootPane;
    private Pane gamePane;
    private Pane uiPane;


    private ObservableList<VoiceProfile> voiceProfiles;
    private VoiceProfile currentlyPreviewedVoice = null;
    private VoiceProfile selectedVoiceProfile = null;
    private AudioClip currentPlayingIntro = null;

    private GameDimensions gameDimensions;
    private GameEntityManager entityManager;
    private GameUpdater gameUpdater;
    private UIManager gameUIManager;
    private InputHandler inputHandler;
    private AnimationTimer gameLoop;

    // SoundManager Instanzen
    private SoundManager globalSoundManager; // Für globale Sounds wie Jubel
    private SoundManager profileSoundManager; // Für profil-spezifische SFX im Spiel


    private static final String MAIN_MENU_CSS_PATH = "/mainmenu.css";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Musical Invaders");

        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PressStart2P-Regular.ttf"), 10);
            System.out.println("Schriftart 'Press Start 2P' programmatisch geladen.");
        } catch (Exception e) {
            System.err.println("Fehler beim programmatischen Laden der Schriftart '/fonts/PressStart2P-Regular.ttf': " + e.getMessage());
        }

        // Globale Instanz für Jubel erstellen, bevor VoiceProfiles geladen werden,
        // da der VoiceProfile Konstruktor u.U. schon Sounds laden könnte (obwohl Jubel global ist)
        this.globalSoundManager = new SoundManager();

        loadVoiceProfiles();
        initializeGlobalInput(primaryStage);
        createMenuOverlays();

        changeGameState(GameState.MAIN_MENU);
        if (mainMenuScene != null) {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX((screenBounds.getWidth() - mainMenuScene.getWidth()) / 2);
            primaryStage.setY((screenBounds.getHeight() - mainMenuScene.getHeight()) / 2);
        } else {
            primaryStage.centerOnScreen();
        }
        this.primaryStage.setResizable(false);
        this.primaryStage.show();
    }

    private void loadVoiceProfiles() {
        voiceProfiles = FXCollections.observableArrayList(
                new VoiceProfile("Der Franzose", "/sfx/Der Franzose/Vorstellung.wav", "/sfx/Der Franzose/", "Ein Baguette, frisch aus dem Ofen heiß,\n" +
                        "\n" +
                        "geformt von Bäckerhand mit Fleiß.\n" +
                        "\n" +
                        "Im Laden lag es, goldbraun, ein wahrer Augenschmaus,\n" +
                        "\n" +
                        "ich trug es stolz dann schnell nach Haus.\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "Die Kruste knackt, so knusprig fein,\n" +
                        "\n" +
                        "das Inn're weich, ein lichter Schein.\n" +
                        "\n" +
                        "Mit Butter, Käse oder pur – ein Genuss,\n" +
                        "\n" +
                        "so endet es mit einem Kuss."),
                new VoiceProfile("Prof. Slawitscheck", "/sfx/Prof. Slawitscheck/Introduction.wav", "/sfx/Prof. Slawitscheck/", "Slawitscheck, ein Lehrer feinster Art,\n" +
                        "Mathe für ihn ist wie Musik für Mozart,\n" +
                        "\n" +
                        "SOPK mit ihm, ist wie eine Sitzung im Senat, \n" +
                        "\n" +
                        "Bei ihm sammelt man Wissen wie Benzin bei einem Tankautomat. \n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "In seiner Freizeit tüftelt er mit Herz und Sinn,\n" +
                        "\n" +
                        "Mit seinen Spielen macht er ordentlich Gewinn.\n" +
                        "\n" +
                        "Die Spielstunden sind seiner Schüler ein Stein in der Brandung,\n" +
                        "\n" +
                        "Unsere Schularbeiten sind ihm wichtig wie eine Dokumentensammlung."
                        ),
                new VoiceProfile("Prof. Csaszar", "/sfx/Prof. Csaszar/Introduction.wav", "/sfx/Prof. Csaszar/", "So nehme man einen Dwayn 'the Rock'\n\nJohnson und wartet 10 Jahre so\n\nerscheint ein Professor Robert Csaszar.\n\nSein Physikunterricht ist wie eine Oase\n\ndes Wissens, es gibt keine Dummen\n\nFragen und keine dürftigen Antworten.\n\nAn Spaß mangelte es auch nicht, du\n\nmöchtest mit einem Auto in eine Wand\n\nfahren und es 'Experiment' nennen? Dann\n\nist er dein Mann! Was muss ein Lehrer\n\nhaben damit er als 'cool' durchgeht?\n\nEin Motorrad? Eine glänzende Glatze?\n\nEinen eleganter Bart? Er hat sie alle.\n\nUnd wie kann man bloß auf seine\n\nCatchphrase vergessen? ein 'Abgelehnt'\n\ngesprochen mit der Stimme eines Engels."),
                new VoiceProfile("Mary Fly", "/sfx/Mary Fly/Introduction.wav", "/sfx/Mary Fly/", "Mary Fly, offiziell Marie Kissler\n\ngenannt, hat eine mysteriöse\n\nObsession mit Mailand,\n\nich weiß auch nicht wieso, naja.\n\nHugo ist ihr Erzfeind,\n\nnur ein Schluck und sie fällt Tot um\n\n(stimmt wirklich, hab ich schon mal miterlebt).\n\nDoch solltest du so unvorsichtig\n\nsein und das Wort 'Vodkabull' in ihrer\n\nNähe erwähnen, so kannst du dir\n\nsicher sein, dass sie bereits den Weg\n\nzu dir eingeschlagen hat und du bald\n\nihrer funkelnden Augen siehst\n\n(naja, angenommen du schaust nicht über sie drüber).\n\nSie wohnt bei 47°57'40.6\"N 16°24'...\n\nNein, Spaß Marie ich doxs dich nicht, keine Angst."),
                new VoiceProfile("Omar Rosbal", "/sfx/Omar Rosypal/Introduction.wav", "/sfx/Omar Rosypal/", "Omar Rosbal? Wieso Omar Rosbal?\n\nEigentlicht heißt er Omar Rosypal,\n\ndoch niemand weiß wie man seinen Namen ausspricht,\n\ndeswegen kassiert er jede Stunde einen neuen Spitznamen vom Lehrer.\n\nLeider haben wir den echten Omar Rosypal schon vor langer Zeit verloren.\n\nSein Mörder?\n\nEin Spieleentwickler Studio namens 'Riot Games'.\n\n'Wie ist das nur möglich?' hör ich dich fragen.\n\nDie Antwort ist simpel: LEAGUE OF LEGENDS und Valorant,\n\ndas eine hat ihn vergifted das ander ihm ein Messer in den Rücken gestoßen.\n\nNun eine Schweigeminute für unseren gefallenen Bruder."), // Korrigierter Pfad
                new VoiceProfile("Mhh lecka Bierchen", "/sfx/Simon Leber/Introduction.wav", "/sfx/Simon Leber/", "'Mhh lecka Bierchen' wurde von Simon Leber gevoiced.\n\nBierchen sind seine Leibspeise, ich mein im Endeffekt ist Bier eh nur flüssiges Brot, also...\n\nFalls du dich jemals in einer\n\nkniffligen Lage befindest, in welcher\n\nein Muskelpaket, welches Hulk den Platz weißt, deine einzige Rettung ist,\n\ndann sollte Simon Leber deine erste Wahl sein.\n\nDoch er ist nicht nur eine absolute Maschine,\n\nseine Stimme ist wie das Knistern eines heißen Feuers in einem Stein Kamin an einem kalten Wintertag,\n\nwie die Glocke nach der 10ten Stunde am Mittwoch,\n\nwie dieser eine Gehfehler,\n\nwie der Sternenhimmel an einer mondlosen Nacht."),
                new VoiceProfile("Momoko X Selin", "/sfx/Momoko X Selin/Vorstellung.wav", "/sfx/Momoko X Selin/", "Max isst gerne Pizza."),
                new VoiceProfile("Der Japaner", "/sfx/Leo Fukahori/Introduction.wav", "/sfx/Leo Fukahori/", "'Der Japaner', gevoiced von Leo Fukahori,\n\nist eine energetische und unterstützende Wahl.\n\n'Fukahori' ist der Lieblingsname vom Herrn Dr. Karl Wodnar,\n\nbei jeder seiner Amtshandlungen durchtrennt er beim vorlesen des Namens die Luft mit seiner Hand,\n\nwie Butter mit einem heißen Messer.\n\nDoch verstehst du alles was unser Reis verschlingende Freund zu sagen hat?\n\nUm es präziser zu formulieren, wie hoch ist deine Doulingo-Streak?\n\nIch habe gehört er hat mal 2 Platten Sushi mit nur einem Stift zubereitet,\n\nist diese Geschichte wirklich so passiert oder haben wir sie frei erfunden?\n\nHier noch ein lustiges Yojijukugo (Japanese: 四字熟語): 今朝毎朝 (Kesamaiasa),\n\nder Witz liegt im Bilingualismus der Aussprache der vier Kanji :))."),
                new VoiceProfile("Fabian Meduna", "/sfx/Fabio Meduna/Introduction.wav", "/sfx/Fabio Meduna/", "Fabio Meduna, was soll ich sagen?\n\nEine besondere Figur, man könnte fast schon Komiker sagen.\n\nEigentlich würde ich ihn hier jetzt haten (eng), jedoch finde ich,\n\ndass man auch ab und zu etwas positives über dem Herrn Meduna sagen kann.\n\nZum Beispiel weiß ich, dass der Kollege,\n\nwenn ihn ein Thema interessiert,\n\näußerst lernfreudig sein kann und voller Neugier und Enthusiasmus über ein Thema reden kann.\n\nAuch weiß ich, dass er eine interessanten Kink für Straßenschilder hat,\n\ndoch wieso, weiß ich nicht,\n\nähnlich wie bei Mary Fly's Obsession mit Mailand.\n\nNaja, jedem das Seine ig."),
                new VoiceProfile("Lebron James", "/sfx/Lebron James/Introduction.wav", "/sfx/Lebron James/", "Die Stimme von Lebron James interpretiert vom professionellen Basketballspieler Polat Sahin Keles. .")
        );
        if (!voiceProfiles.isEmpty()) {
            currentlyPreviewedVoice = voiceProfiles.get(0);
        }
    }

    private void initializeGlobalInput(Stage stage) {
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (currentGameState == GameState.PLAYING) {
                    changeGameState(GameState.PAUSED);
                } else if (currentGameState == GameState.PAUSED) {
                    changeGameState(GameState.PLAYING);
                }
                event.consume();
            }
        });
    }

    private void createMenuOverlays() {
        pauseMenuPane = new StackPane();
        pauseMenuPane.setId("pause-menu-pane");
        Label pauseLabel = new Label("PAUSED");
        pauseLabel.getStyleClass().add("menu-title");
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

        gameOverMenuPane = new StackPane();
        gameOverMenuPane.setId("game-over-menu-pane");
        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.getStyleClass().add("menu-title");
        Button restartButton = new Button("Try Again (Main Menu)");
        restartButton.getStyleClass().add("menu-button");
        restartButton.setOnAction(e -> changeGameState(GameState.MAIN_MENU));
        Button backToMainMenuFromGameOverButton = new Button("Back to Main Menu");
        backToMainMenuFromGameOverButton.getStyleClass().add("menu-button");
        backToMainMenuFromGameOverButton.setOnAction(e -> changeGameState(GameState.MAIN_MENU));
        VBox gameOverContent = new VBox(20, gameOverLabel, restartButton, backToMainMenuFromGameOverButton);
        gameOverContent.setAlignment(Pos.CENTER);
        gameOverContent.getStyleClass().add("menu-content-box");
        gameOverMenuPane.getChildren().add(gameOverContent);
        gameOverMenuPane.setVisible(false);

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
        if (this.currentGameState == newState && newState != GameState.MAIN_MENU) {
            return;
        }
        System.out.println("Changing state from " + this.currentGameState + " to " + newState);
        GameState previousState = this.currentGameState;
        this.currentGameState = newState;

        pauseMenuPane.setVisible(false);
        gameOverMenuPane.setVisible(false);
        creditsMenuPane.setVisible(false);

        if (gameLoop != null && newState != GameState.PLAYING) {
            gameLoop.stop();
            System.out.println("GameLoop stopped.");
        }
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying() && newState != GameState.MAIN_MENU) {
            currentPlayingIntro.stop();
        }

        // Stoppe Jubel-Loop, wenn Credits verlassen werden (egal wohin)
        // oder wenn ein neues Spiel gestartet wird (um sicherzugehen)
        if (globalSoundManager != null && previousState == GameState.CREDITS) {
            if (newState == GameState.MAIN_MENU || newState == GameState.PLAYING) {
                globalSoundManager.stopJubelLoop();
            }
        }
        // Auch stoppen, wenn vom Hauptmenü ins Spiel gewechselt wird (falls es lief und nicht gestoppt wurde)
        if (globalSoundManager != null && newState == GameState.PLAYING && previousState == GameState.MAIN_MENU){
            globalSoundManager.stopJubelLoop(); // Sicherstellen, dass es aus ist
        }


        switch (newState) {
            case MAIN_MENU:
                selectedVoiceProfile = null;
                if (voiceProfiles != null && !voiceProfiles.isEmpty()) {
                    currentlyPreviewedVoice = voiceProfiles.get(0);
                }
                mainMenuScene = createMainMenuScene();
                primaryStage.setScene(mainMenuScene);
                break;

            case PLAYING:
                if (selectedVoiceProfile == null) {
                    System.err.println("Keine Stimme ausgewählt. Zurück zum Hauptmenü.");
                    changeGameState(GameState.MAIN_MENU);
                    return;
                }
                if (previousState == GameState.PAUSED) {
                    if (primaryStage.getScene() != gameScene) primaryStage.setScene(gameScene);
                } else {
                    initializeGame();
                    primaryStage.setScene(gameScene);
                }
                if (gameLoop != null) {
                    gameLoop.start();
                    System.out.println("GameLoop started/resumed.");
                }
                break;

            case PAUSED:
                if (gameRootPane != null) {
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
                if (gameLoop != null) {
                    gameLoop.stop();
                }
                Node currentSceneRootNode = primaryStage.getScene() != null ? primaryStage.getScene().getRoot() : null;
                if (mainMenuScene != null && mainMenuScene.getRoot() instanceof StackPane) {
                    StackPane menuRoot = (StackPane) mainMenuScene.getRoot();
                    if (!menuRoot.getChildren().contains(creditsMenuPane)) {
                        menuRoot.getChildren().add(creditsMenuPane);
                    }
                    creditsMenuPane.setVisible(true);
                    creditsMenuPane.toFront();
                    primaryStage.setScene(mainMenuScene);
                } else {
                    double w = (gameDimensions != null) ? gameDimensions.getWidth() : 900;
                    double h = (gameDimensions != null) ? gameDimensions.getHeight() : 650;
                    Scene creditsOnlyScene = new Scene(creditsMenuPane, w, h);
                    try {
                        String cssPath = Objects.requireNonNull(getClass().getResource(MAIN_MENU_CSS_PATH)).toExternalForm();
                        creditsOnlyScene.getStylesheets().add(cssPath);
                    } catch (Exception e) {
                        System.err.println("CSS für Credits-Szene nicht gefunden (" + MAIN_MENU_CSS_PATH + "): " + e.getMessage());
                    }
                    creditsMenuPane.setVisible(true);
                    primaryStage.setScene(creditsOnlyScene);
                }
                startCreditRoll();
                if (globalSoundManager != null) {
                    globalSoundManager.startJubelLoop();
                }
                break;

            case LEVEL_TRANSITION:
                System.out.println("Level Transition (Placeholder)");
                break;
        }
    }

    private void startCreditRoll() {
        creditsContentBox.getChildren().clear();
        addCreditEntry("Herzlichen Glückwunsch!", "");
        addCreditEntry("Du hast Musical Invaders besiegt!", "");
        addCreditEntry("== Stimmen von ==", "");
        for(VoiceProfile voiceProfile : voiceProfiles) {
            addCreditEntry("- " + voiceProfile.getDisplayName(), "");
        }
        addCreditEntry("", "");
        addCreditEntry("== Entwicklung ==", "");
        addCreditEntry("My, Myself and I", ""); // Platzhalter für deinen Namen
        addCreditEntry("", "");
        addCreditEntry("Danke fürs Spielen!", "");


        creditsScrollPane.setVvalue(0.0);

        if(creditRollTimeline != null){
            creditRollTimeline.stop();
        }
        PauseTransition pt = new PauseTransition(Duration.millis(100));
        pt.setOnFinished(event -> {
            double actualHeight = creditsContentBox.getBoundsInLocal().getHeight();
            double visibleHeight = creditsScrollPane.getViewportBounds().getHeight();
            if(visibleHeight <= 0 && gameDimensions != null) visibleHeight = gameDimensions.getHeight()*0.8;
            else if (visibleHeight <= 0) visibleHeight = 650 * 0.8;

            creditsContentBox.setTranslateY(visibleHeight);

            creditRollTimeline = new Timeline();
            double scrollSpeedFactor = 0.03;
            if (actualHeight <=0 ) actualHeight = 500;
            double durationMillis = (actualHeight + visibleHeight) / scrollSpeedFactor;


            KeyValue kv = new KeyValue(creditsContentBox.translateYProperty(), -actualHeight);
            KeyFrame kf = new KeyFrame(Duration.millis(durationMillis), kv);
            creditRollTimeline.getKeyFrames().add(kf);
            creditRollTimeline.setOnFinished(e ->{
                if (globalSoundManager != null) {
                    globalSoundManager.stopJubelLoop();
                }
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
        } else {
            VBox.setMargin(titleLabel, new Insets(0, 0,5,0));
        }
    }
    private Scene createMainMenuScene() {
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

        Label titleLabel = new Label("Musical Invaders");
        titleLabel.setId("title-label");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(20,0,0,0));
        rootLayout.setTop(titleLabel);

        VBox voiceSelectionBox = new VBox();
        voiceSelectionBox.setId("voice-selection-box");
        voiceSelectionBox.setPrefWidth(380);
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
            private static final javafx.css.PseudoClass FIRST_CELL_PSEUDO_CLASS = javafx.css.PseudoClass.getPseudoClass("first-cell");
            private static final javafx.css.PseudoClass LAST_CELL_PSEUDO_CLASS = javafx.css.PseudoClass.getPseudoClass("last-cell");
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
        Label infoTitleLabel = new Label("Beschreibung");
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


    private void initializeGame() {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        double windowHeight = bounds.getHeight();
        double windowWidth = windowHeight * (4.0 / 3.0);
        if (windowWidth > bounds.getWidth()) { windowWidth = bounds.getWidth(); windowHeight = windowWidth * (3.0 / 4.0); }

        this.gameDimensions = new GameDimensions(windowWidth, windowHeight);

        if (selectedVoiceProfile != null && selectedVoiceProfile.getSfxFolderPath() != null) {
            this.profileSoundManager = new SoundManager(selectedVoiceProfile.getSfxFolderPath());
        } else {
            System.err.println("MusicalInvaders: selectedVoiceProfile or its SFX path is null for profileSoundManager.");
            this.profileSoundManager = new SoundManager((String) null);
        }

        gamePane = new Pane();
        gamePane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        gamePane.setId("game-pane");
        gamePane.setStyle("-fx-background-color: #1a1a1a;");

        uiPane = new Pane();
        uiPane.setId("ui-pane");
        uiPane.setPrefSize(gameDimensions.getWidth(), gameDimensions.getHeight());
        uiPane.setMouseTransparent(true);

        gameRootPane = new StackPane(gamePane, uiPane);
        if (!gameRootPane.getChildren().contains(pauseMenuPane)) gameRootPane.getChildren().add(pauseMenuPane);
        if (!gameRootPane.getChildren().contains(gameOverMenuPane)) gameRootPane.getChildren().add(gameOverMenuPane);
        pauseMenuPane.setVisible(false);
        gameOverMenuPane.setVisible(false);


        gameScene = new Scene(gameRootPane, gameDimensions.getWidth(), gameDimensions.getHeight());
        inputHandler = new InputHandler(gameScene);

        gameUIManager = new UIManager(uiPane, gameDimensions, this);
        entityManager = new GameEntityManager(gamePane, gameDimensions, gameUIManager, this.profileSoundManager);
        gameUpdater = new GameUpdater(entityManager, inputHandler, gameDimensions, gameUIManager, this, this.profileSoundManager);

        gameUIManager.resetScore();
        entityManager.createPlayer();
        gameUIManager.createScoreLabel();
        entityManager.spawnEnemyWaveInitial();

        if (gameLoop == null) {
            gameLoop = new AnimationTimer() {
                private long lastUpdate = 0;
                private boolean firstFrameAfterResume = true;
                @Override
                public void handle(long now) {
                    if (currentGameState != GameState.PLAYING) {
                        firstFrameAfterResume = true;
                        return;
                    }
                    if (firstFrameAfterResume || lastUpdate == 0) {
                        lastUpdate = now;
                        firstFrameAfterResume = false;
                        return;
                    }
                    double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                    lastUpdate = now;
                    if (deltaTime > 0.1) { deltaTime = 0.1; }
                    if (deltaTime <= 0) { deltaTime = 1.0/60.0; }
                    gameUpdater.update(now, deltaTime);
                }
            };
        }
        System.out.println("Spiel initialisiert/neu gestartet.");
    }


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
        if (profile == null || profile.getIntroAudioClip() == null) { System.err.println("Kein Intro-Clip für: " + (profile != null ? profile.getDisplayName() : "Unbekannt")); return; }
        if (currentPlayingIntro != null && currentPlayingIntro.isPlaying()) { currentPlayingIntro.stop(); }
        currentPlayingIntro = profile.getIntroAudioClip();
        currentPlayingIntro.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}