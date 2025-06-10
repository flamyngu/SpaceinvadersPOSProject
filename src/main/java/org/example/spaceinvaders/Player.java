package org.example.spaceinvaders;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color; // Nur für Fallback
import javafx.scene.shape.Rectangle; // Nur für Fallback

public class Player {
    private ImageView node;
    private GameDimensions gameDimensions;
    private Image playerImage;

    public Player(GameDimensions gameDimensions) {
        this.gameDimensions = gameDimensions;

        try {
            // Lade das Spielerbild - stelle sicher, dass der Pfad korrekt ist!
            // Beispiel: /images/xwing.png
            playerImage = new Image(getClass().getResourceAsStream("/images/xwing.png"));
            if (playerImage.isError()) {
                throw new IllegalArgumentException("Fehler beim Laden des Spielerbildes: " + playerImage.getException().getMessage());
            }
        } catch (Exception e) {
            System.err.println("Schwerwiegender Fehler beim Laden des Spielerbildes: /images/xwing.png - " + e.getMessage());
            // Fallback zu einem Rechteck (als Notlösung, besser wäre ein Standard-Sprite)
            Rectangle fallbackNode = new Rectangle(gameDimensions.getPlayerWidth(), gameDimensions.getPlayerHeight());
            fallbackNode.setFill(Color.CYAN);
            this.node = new ImageView(); // Leeres ImageView, um NPE zu vermeiden
            // Besser wäre es, das Spiel hier zu beenden oder einen echten Fallback-Sprite zu verwenden
            System.err.println("Spiel kann aufgrund fehlender Spieler-Grafik möglicherweise nicht korrekt angezeigt werden.");
            return;
        }

        this.node = new ImageView(playerImage);
        this.node.setFitWidth(gameDimensions.getPlayerWidth());
        this.node.setFitHeight(gameDimensions.getPlayerHeight());
        this.node.setPreserveRatio(true); // Seitenverhältnis beibehalten

        double startX = gameDimensions.getWidth() / 2 - gameDimensions.getPlayerWidth() / 2;
        double startY = gameDimensions.getHeight() - gameDimensions.getPlayerHeight() - gameDimensions.getHeight() * 0.05; // Etwas höher positioniert
        this.node.setLayoutX(startX);
        this.node.setLayoutY(startY);
    }

    public Node getNode() {
        return node;
    }

    public void move(double dx) {
        if (node == null || node.getImage() == null || node.getImage().isError()) return; // Sicherheitscheck
        double newX = node.getLayoutX() + dx;
        if (newX >= 0 && newX <= gameDimensions.getWidth() - node.getFitWidth()) {
            node.setLayoutX(newX);
        }
    }

    public double getX() {
        if (node == null) return 0;
        return node.getLayoutX();
    }
    public double getY() {
        if (node == null) return 0;
        return node.getLayoutY();
    }
    public double getWidth() {
        if (node == null || node.getImage() == null || node.getImage().isError()) return gameDimensions.getPlayerWidth(); // Fallback
        return node.getFitWidth();
    }
    public double getHeight() {
        if (node == null || node.getImage() == null || node.getImage().isError()) return gameDimensions.getPlayerHeight(); // Fallback
        return node.getFitHeight();
    }
}