import java.util.Random;

public class MapGenerator {
   public static Tile[][] generateRandomMap(int width, int height) {
    Tile[][] map = new Tile[height][width];
    Random random = new Random();
    
    // Düşman doğma noktaları için üst kenarda boşluk bırakılacak sütunlar
    int[] enemySpawnCols = {1, width / 2, width - 2}; // Kenardan 1 blok içeride

    // 1. Haritayı tamamen boşlukla doldur
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            map[row][col] = new Tile(col * Tile.TILE_SIZE, row * Tile.TILE_SIZE, TileType.EMPTY);
        }
    }

    // --- KALDIRILAN BÖLÜM ---
    // Haritanın etrafını çelik duvarlarla kaplayan bu döngüler kaldırıldı.
    /*
    for (int row = 0; row < height; row++) {
        map[row][0].setType(TileType.STEEL);
        map[row][width - 1].setType(TileType.STEEL);
    }
    for (int col = 0; col < width; col++) {
        map[0][col].setType(TileType.STEEL);
        map[height - 1][col].setType(TileType.STEEL);
    }
    */
    // --- KALDIRMA SONU ---

    // Not: Artık çerçeve olmadığı için düşman doğma noktalarında delik açmaya gerek yok,
    // çünkü üst taraf zaten boş olacak.

    // 2. Kartal ve kalesini yerleştir
    int eagleCol = width / 2;
    int eagleRow = height - 2;
    if (eagleRow < 1 || eagleCol < 1 || eagleCol + 1 >= width) {
         eagleRow = height - 2; eagleCol = width / 2;
    }
    map[eagleRow][eagleCol].setType(TileType.EAGLE);
    
    // Kartalın etrafını tuğla ile koru
    map[eagleRow - 1][eagleCol - 1].setType(TileType.BRICK);
    map[eagleRow - 1][eagleCol].setType(TileType.BRICK);
    map[eagleRow - 1][eagleCol + 1].setType(TileType.BRICK);
    map[eagleRow][eagleCol - 1].setType(TileType.BRICK);
    map[eagleRow][eagleCol + 1].setType(TileType.BRICK);

      // --- EKLENEN BÖLÜM: Boşlukları dolduruyoruz ---
    map[eagleRow + 1][eagleCol - 1].setType(TileType.BRICK);
    map[eagleRow + 1][eagleCol].setType(TileType.BRICK);
    map[eagleRow + 1][eagleCol + 1].setType(TileType.BRICK);
    // --- EKLEME SONU ---

    // 3. İçeriye rastgele engeller yerleştir
    int totalTiles = width * height;
    int brickCount = totalTiles / 8;
    int steelCount = totalTiles / 16;
    int waterCount = totalTiles / 20;
    int treesCount = totalTiles / 10;

    for (int i = 0; i < brickCount; i++) placeRandomTile(map, TileType.BRICK, random);
    for (int i = 0; i < steelCount; i++) placeRandomTile(map, TileType.STEEL, random);
    for (int i = 0; i < waterCount; i++) placeRandomTile(map, TileType.WATER, random);
    for (int i = 0; i < treesCount; i++) placeRandomTile(map, TileType.TREES, random);

    return map;
}
    
    // Eagle koruma brick'i oluştur (yarı kalınlık)
    private static void createEagleProtectionBrick(Tile[][] map, int row, int col) {
        if (row >= 0 && row < map.length && col >= 0 && col < map[0].length) {
            map[row][col].setType(TileType.BRICK);
            map[row][col].setAsEagleProtectionBrick(); // Yarı kalınlık yap
        }
    }

    private static void placeRandomTile(Tile[][] map, TileType type, Random random) {
        int height = map.length;
        int width = map[0].length;
        if (height <= 2 || width <= 2) return;

        int row, col;
        do {
            row = random.nextInt(height - 2) + 1;
            col = random.nextInt(width - 2) + 1;
        } while (map[row][col].getType() != TileType.EMPTY);
        map[row][col].setType(type);
    }
}