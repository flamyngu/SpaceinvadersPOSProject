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

        double startX = gameDimensions.getWidth() / 2 - gameDimensions.getPlayerWidth() / 2;
        double startY = gameDimensions.getHeight() - gameDimensions.getPlayerHeight() - 20;
        this.node.setLayoutX(startX);
        this.node.setLayoutY(startY);
        // System.out.println("Player.java: Spieler erstellt an Position X=" + startX + ", Y=" + startY); // Log von vorheriger Anfrage
        // System.out.println("Player.java: Spieler Node Bounds in Local: " + this.node.getBoundsInLocal());
        // System.out.println("Player.java: Spieler Node Bounds in Parent (beim Erstellen, Parent ist noch null): " + this.node.getBoundsInParent());
    }

    public Node getNode() {
        return node;
    }

    public void move(double dx) {
        double newX = node.getLayoutX() + dx;
        if (newX >= 0 && newX <= gameDimensions.getWidth() - node.getWidth()) {
            node.setLayoutX(newX);
        }
    }

    public double getX() { return node.getLayoutX(); }
    public double getY() { return node.getLayoutY(); }
    public double getWidth() { return node.getBoundsInLocal().getWidth(); }
    public double getHeight() { return node.getBoundsInLocal().getHeight();}
}