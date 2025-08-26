public enum TileType {
    EMPTY,
    BRICK,
    STEEL,
    TREES,
    WATER,
    ICE,
    EAGLE;

    public static TileType fromInteger(int x) {
        switch (x) {
            case 0: return EMPTY;
            case 1: return STEEL;
            case 2: return BRICK;
            case 3: return TREES;
            case 4: return WATER;
            case 5: return ICE;
            case 9: return EAGLE;
        }
        return EMPTY; // Varsayılan değer
    }
}