package org.example.spaceinvaders;

public class GameDimensions {
    private final double width;
    private final double height;

    public static final double PLAYER_WIDTH_RATIO = 60.0 / 800.0;
    public static final double PLAYER_HEIGHT_RATIO = 30.0 / 600.0;
    public static final double ENEMY_WIDTH_RATIO = 40.0 / 800.0;
    public static final double ENEMY_HEIGHT_RATIO = 40.0 / 600.0;
    public static final double ENEMY_SPACING_X_RATIO = 15.0 / 800.0;
    public static final double ENEMY_SPACING_Y_RATIO = 10.0 / 600.0;
    public static final double PROJECTILE_WIDTH_RATIO = 5.0 / 800.0;
    public static final double PROJECTILE_HEIGHT_RATIO = 15.0 / 600.0;
    public static final int BOSS_HEALTH = 10; // War GameDimensions.BOSS_HEALTH
    public static final int BOSS_POINTS = 250;
    public static final double BOSS_WIDTH_MULTIPLIER = 4.5;
    public static final double BOSS_HEIGHT_MULTIPLIER = 2.0;
    public static final int WAVE_NUMBER_TO_SPAWN_BOSS = 4;
    public static final int SCORE_TO_SPAWN_BOSS_ALTERNATIVE = 1000; // Nicht verwendet, aber vorhanden

    private final double playerWidth;
    private final double playerHeight;
    private final double enemyWidth;
    private final double enemyHeight;
    private final double enemySpacingX;
    private final double enemySpacingY;
    private final double projectileWidth;
    private final double projectileHeight;
    private final double playerSpeedBase;
    private final double projectileSpeedBase;


    public GameDimensions(double windowWidth, double windowHeight) {
        // Verwendung der vollen Fenstergröße ohne -10
        this.width = windowWidth;
        this.height = windowHeight;

        this.playerWidth = windowWidth * PLAYER_WIDTH_RATIO;
        this.playerHeight = windowHeight * PLAYER_HEIGHT_RATIO;
        this.enemyWidth = windowWidth * ENEMY_WIDTH_RATIO;
        this.enemyHeight = windowHeight * ENEMY_HEIGHT_RATIO;
        this.enemySpacingX = windowWidth * ENEMY_SPACING_X_RATIO;
        this.enemySpacingY = windowHeight * ENEMY_SPACING_Y_RATIO;
        this.projectileWidth = windowWidth * PROJECTILE_WIDTH_RATIO;
        this.projectileHeight = windowHeight * PROJECTILE_HEIGHT_RATIO;

        this.playerSpeedBase = windowWidth * (5.0 / 800.0); // War windowWidth * (5.0 / 800.0)
        this.projectileSpeedBase = windowHeight * (8.0 / 600.0); // War windowHeight * (8.0 / 600.0)
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getPlayerWidth() { return playerWidth; }
    public double getPlayerHeight() { return playerHeight; }
    public double getEnemyWidth() { return enemyWidth; }
    public double getEnemyHeight() { return enemyHeight; }
    public double getEnemySpacingX() { return enemySpacingX; }
    public double getEnemySpacingY() { return enemySpacingY; }
    public double getProjectileWidth() { return projectileWidth; }
    public double getProjectileHeight() { return projectileHeight; }
    public double getPlayerSpeed() { return playerSpeedBase; }
    public double getProjectileSpeed() { return projectileSpeedBase; }

    public static final int ENEMIES_PER_ROW = 10;
    public static final int ENEMY_ROWS = 4;
    public static final int POINTS_PER_ENEMY = 10;
    public static final long SHOOT_COOLDOWN_MS = 300;
}