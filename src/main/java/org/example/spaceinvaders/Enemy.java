package org.example.spaceinvaders;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Enemy {
    private ImageView node;
    private int health;
    private final int points;
    // private Image enemyImage; // Nicht unbedingt nötig, da das Bild im ImageView gespeichert ist

    public Enemy(Image image, double width, double height, int initialHealth, int points) {
        // this.enemyImage = image; // Kann entfernt werden, wenn nicht anderweitig benötigt
        if (image == null || image.isError()) {
            System.err.println("Fehlerhaftes oder fehlendes Bild für Enemy übergeben. Erstelle Dummy-Node.");
            this.node = new ImageView(); // Leerer Node, um NPE zu vermeiden
            // Hier könnte man ein Fallback-Rechteck erstellen und dieses als Node setzen,
            // aber dann müsste der Typ von 'node' zu 'Node' geändert werden.
        } else {
            this.node = new ImageView(image);
            this.node.setFitWidth(width);
            this.node.setFitHeight(height);
            this.node.setPreserveRatio(true);
        }
        this.health = initialHealth;
        this.points = points;
    }

    public Node getNode() {
        return node;
    }
    public int getHealth() { return health; }
    public int getPoints() { return points; }
    public boolean isAlive() { return health > 0; }

    public void takeHit() {
        if (health > 0) {
            health--;
        }
    }

    public void setHealth(int newHealth) {
        this.health = Math.max(0, newHealth);
    }

    public void setImage(Image newImage) {
        // this.enemyImage = newImage; // Falls benötigt
        if (this.node != null && newImage != null && !newImage.isError()) {
            this.node.setImage(newImage);
        } else if (newImage == null || newImage.isError()) {
            System.err.println("Versuch, fehlerhaftes oder fehlendes Bild für Enemy zu setzen.");
        }
    }
}