public enum GameMode {
    ONE_PLAYER,
    TWO_PLAYER;
    
    private static GameMode currentMode = ONE_PLAYER;
    
    public static GameMode getCurrentMode() {
        return currentMode;
    }
    
    public static void setCurrentMode(GameMode mode) {
        currentMode = mode;
    }
}