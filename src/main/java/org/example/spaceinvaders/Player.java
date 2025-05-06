package org.example.spaceinvaders;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player {
    private Rectangle node; // Die visuelle Darstellung
    private GameDimensions gameDimensions;

    public Player(GameDimensions gameDimensions) {
        this.gameDimensions = gameDimensions;
        this.node = new Rectangle(gameDimensions.getPlayerWidth(), gameDimensions.getPlayerHeight());
        this.node.setFill(Color.CYAN);
        // Startposition
        this.node.setX(gameDimensions.getWidth() / 2 - gameDimensions.getPlayerWidth() / 2);
        this.node.setY(gameDimensions.getHeight() - gameDimensions.getPlayerHeight() - 20); // 20px Abstand von unten
    }

    public Node getNode() {
        return node;
    }

    public void move(double dx) {
        double newX = node.getX() + dx;
        // Grenzenprüfung
        if (newX >= 0 && newX <= gameDimensions.getWidth() - node.getWidth()) {
            node.setX(newX);
        }
    }

    public double getX() { return node.getX(); }
    public double getY() { return node.getY(); }
    public double getWidth() { return node.getWidth(); }
}