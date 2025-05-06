package org.example.spaceinvaders;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

public class InputHandler {
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean shooting = false;

    public InputHandler(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) moveLeft = true;
            else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) moveRight = true;
            else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.UP) shooting = true;
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) moveLeft = false;
            else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) moveRight = false;
            else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.UP) shooting = false;
        });
    }

    public boolean isMoveLeftPressed() { return moveLeft; }
    public boolean isMoveRightPressed() { return moveRight; }
    public boolean isShootingPressed() { return shooting; }
}