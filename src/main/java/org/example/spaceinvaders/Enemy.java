package org.example.spaceinvaders;

import javafx.scene.Node;

public class Enemy {
    private Node node;
    private int health;
    private final int points;

    public Enemy(Node node, int initialHealth, int points) {
        this.node = node;
        this.health = initialHealth;
        this.points = points;
    }

    public Node getNode() { return node; }
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
}