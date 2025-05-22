package org.example.spaceinvaders;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player {
    private Rectangle node;
    private GameDimensions gameDimensions;

    public Player(GameDimensions gameDimensions) {
        this.gameDimensions = gameDimensions;
        this.node = new Rectangle(gameDimensions.getPlayerWidth(), gameDimensions.getPlayerHeight());
        this.node.setFill(Color.CYAN);

        // Startposition - verwende setLayoutX/setLayoutY für Konsistenz
        this.node.setLayoutX(gameDimensions.getWidth() / 2 - gameDimensions.getPlayerWidth() / 2);
        this.node.setLayoutY(gameDimensions.getHeight() - gameDimensions.getPlayerHeight() - 20);
    }

    public Node getNode() {
        return node;
    }

    public void move(double dx) {
        double newX = node.getLayoutX() + dx; // Verwende getLayoutX() statt getX()
        // Grenzenprüfung
        if (newX >= 0 && newX <= gameDimensions.getWidth() - node.getWidth()) {
            node.setLayoutX(newX); // Verwende setLayoutX() statt setX()
        }
    }

    // Getter - verwende getLayoutX/getLayoutY für Konsistenz
    public double getX() { return node.getLayoutX(); }
    public double getY() { return node.getLayoutY(); }
    public double getWidth() { return node.getWidth(); }
}