import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapLoader {
    public static Tile[][] loadMap(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (lines.isEmpty()) {
            return new Tile[0][0];
        }

        int rows = lines.size();
        int cols = lines.get(0).split(" ").length;
        Tile[][] map = new Tile[rows][cols];

        for (int row = 0; row < rows; row++) {
            String[] numbers = lines.get(row).split(" ");
            for (int col = 0; col < cols; col++) {
                int type = Integer.parseInt(numbers[col]);
                map[row][col] = new Tile(col * Tile.TILE_SIZE, row * Tile.TILE_SIZE, TileType.fromInteger(type));
            }
        }
        
        // Eagle etrafındaki brick'leri otomatik yarı kalınlık yap
        makeEagleProtectionBricksHalfThickness(map);
        
        return map;
    }
    
    /**
     * Eagle etrafındaki brick'leri otomatik olarak yarı kalınlık yapar
     */
    private static void makeEagleProtectionBricksHalfThickness(Tile[][] map) {
        // Eagle pozisyonunu bul
        int eagleRow = -1, eagleCol = -1;
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col].getType() == TileType.EAGLE) {
                    eagleRow = row;
                    eagleCol = col;
                    break;
                }
            }
            if (eagleRow != -1) break;
        }
        
        if (eagleRow == -1 || eagleCol == -1) return; // Eagle bulunamadı
        
        // Eagle etrafındaki 5x5 alandaki tüm brick'leri yarı kalınlık yap
        for (int row = eagleRow - 2; row <= eagleRow + 2; row++) {
            for (int col = eagleCol - 2; col <= eagleCol + 2; col++) {
                if (row >= 0 && row < map.length && col >= 0 && col < map[row].length) {
                    if (map[row][col].getType() == TileType.BRICK) {
                        map[row][col].setAsEagleProtectionBrick();
                    }
                }
            }
        }
    }
}