import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Tile extends GameObject {
    private TileType type;
    public static final int TILE_SIZE = 40;
    
    // Görsel kaynakları
    private static BufferedImage brickImage;
    private static BufferedImage steelImage;
    private static BufferedImage treeImage;
    private static BufferedImage waterImage;
    private static BufferedImage iceImage;
    private static BufferedImage eagleImage;
    
    // 4x4 Brick parça sistemi - dinamik parçalanma
    private boolean[][] brickParts; // 4x4 grid: [row][col]
    private boolean isEagleProtectionBrick = false; // Eagle çevresindeki yarı kalınlık brick'ler
    
    public Tile(int x, int y, TileType type) {
        super(x, y, TILE_SIZE, TILE_SIZE);
        this.type = type;
        
        // Sadece BRICK türü için 4x4 parça sistemi başlat
        if (type == TileType.BRICK) {
            brickParts = new boolean[4][4];
            // Tüm parçaları başlangıçta true yap (var)
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    brickParts[i][j] = true;
                }
            }
        }
    }
    
    // Eagle koruma brick'i olarak işaretle
    public void setAsEagleProtectionBrick() {
        this.isEagleProtectionBrick = true;
        
        // Brick parts sistemini initialize et
        if (type == TileType.BRICK) {
            if (brickParts == null) {
                brickParts = new boolean[4][4];
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        brickParts[i][j] = true;
                    }
                }
            }
            
            // Sadece alt yarısını aktif bırak (yarı kalınlık efekti)
            for (int i = 0; i < 2; i++) { // Üst 2 sıra
                for (int j = 0; j < 4; j++) {
                    brickParts[i][j] = false;
                }
            }
        }
    }

    // Brick parçası vurulduğunda çağrılır - dinamik parçalanma sistemi
    public void hitBrick(int localX, int localY, Direction bulletDirection) {
        if (type != TileType.BRICK || brickParts == null) return;
        
        System.out.println("Brick vuruldu: Local(" + localX + "," + localY + "), Direction: " + bulletDirection);
        
        // Local koordinatları 4x4 grid koordinatlarına çevir
        int col = localX / (TILE_SIZE / 4); // Her parça 10x10 pixel
        int row = localY / (TILE_SIZE / 4);
        
        // Sınırları kontrol et
        if (row < 0) row = 0;
        if (row >= 4) row = 3;
        if (col < 0) col = 0;
        if (col >= 4) col = 3;
        
        System.out.println("Grid pozisyonu: [" + row + "][" + col + "]");
        
        // Bullet yönüne göre KADEMELI parça yıkımı
        switch (bulletDirection) {
            case LEFT:
                // Soldan gelen bullet - sağdan başlayarak 1 sütun yok et
                for (int c = 3; c >= 0; c--) {
                    boolean hasAnyPart = false;
                    for (int r = 0; r < 4; r++) {
                        if (brickParts[r][c]) {
                            hasAnyPart = true;
                            break;
                        }
                    }
                    if (hasAnyPart) {
                        for (int r = 0; r < 4; r++) {
                            brickParts[r][c] = false;
                        }
                        System.out.println("Sağ sütun " + c + " yok edildi (LEFT bullet)!");
                        break; // Sadece 1 sütun yok et
                    }
                }
                break;
            case RIGHT:
                // Sağdan gelen bullet - soldan başlayarak 1 sütun yok et  
                for (int c = 0; c < 4; c++) {
                    boolean hasAnyPart = false;
                    for (int r = 0; r < 4; r++) {
                        if (brickParts[r][c]) {
                            hasAnyPart = true;
                            break;
                        }
                    }
                    if (hasAnyPart) {
                        for (int r = 0; r < 4; r++) {
                            brickParts[r][c] = false;
                        }
                        System.out.println("Sol sütun " + c + " yok edildi (RIGHT bullet)!");
                        break; // Sadece 1 sütun yok et
                    }
                }
                break;
            case UP:
                // Yukarıdan gelen bullet - alttan başlayarak 1 satır yok et
                for (int r = 3; r >= 0; r--) {
                    boolean hasAnyPart = false;
                    for (int c = 0; c < 4; c++) {
                        if (brickParts[r][c]) {
                            hasAnyPart = true;
                            break;
                        }
                    }
                    if (hasAnyPart) {
                        for (int c = 0; c < 4; c++) {
                            brickParts[r][c] = false;
                        }
                        System.out.println("Alt satır " + r + " yok edildi (UP bullet)!");
                        break; // Sadece 1 satır yok et
                    }
                }
                break;
            case DOWN:
                // Aşağıdan gelen bullet - üstten başlayarak 1 satır yok et
                for (int r = 0; r < 4; r++) {
                    boolean hasAnyPart = false;
                    for (int c = 0; c < 4; c++) {
                        if (brickParts[r][c]) {
                            hasAnyPart = true;
                            break;
                        }
                    }
                    if (hasAnyPart) {
                        for (int c = 0; c < 4; c++) {
                            brickParts[r][c] = false;
                        }
                        System.out.println("Üst satır " + r + " yok edildi (DOWN bullet)!");
                        break; // Sadece 1 satır yok et
                    }
                }
                break;
        }
        
        // Eğer tüm parçalar yok olduysa tile'ı EMPTY yap
        if (isBrickDestroyed()) {
            type = TileType.EMPTY;
            System.out.println("Brick tamamen yok oldu!");
        }
    }

    // Belirli bir konumda brick parçası var mı? (4x4 sistem)
    public boolean hasBrickAt(int checkX, int checkY) {
        if (type != TileType.BRICK || brickParts == null) {
            return type == TileType.BRICK; // Normal brick durumu
        }
        
        // Koordinatları 4x4 grid'e çevir
        int relativeX = checkX - this.x;
        int relativeY = checkY - this.y;
        
        int col = relativeX / (TILE_SIZE / 4);
        int row = relativeY / (TILE_SIZE / 4);
        
        if (row >= 0 && row < 4 && col >= 0 && col < 4) {
            return brickParts[row][col];
        }
        return false;
    }

    // Brick tamamen yok oldu mu? (4x4 sistem)
    public boolean isBrickDestroyed() {
        if (brickParts == null) return (type != TileType.BRICK);
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (brickParts[i][j]) {
                    return false; // En az bir parça var
                }
            }
        }
        return true; // Tüm parçalar yok
    }

    // Tank bounds içinde brick parçası var mı kontrol et
    public boolean hasBrickPartsInBounds(Rectangle bounds) {
        if (type != TileType.BRICK || brickParts == null) return false;
        
        // Tank'ın bounds'u içindeki tüm grid hücrelerini kontrol et
        int startX = Math.max(0, (bounds.x - this.x) / (TILE_SIZE / 4));
        int endX = Math.min(3, (bounds.x + bounds.width - this.x - 1) / (TILE_SIZE / 4));
        int startY = Math.max(0, (bounds.y - this.y) / (TILE_SIZE / 4));
        int endY = Math.min(3, (bounds.y + bounds.height - this.y - 1) / (TILE_SIZE / 4));
        
        for (int row = startY; row <= endY; row++) {
            for (int col = startX; col <= endX; col++) {
                if (brickParts[row][col]) {
                    return true; // Bu bölgede brick parçası var
                }
            }
        }
        return false; // Bu bölgede brick parçası yok
    }

    // Görsel yükleme
    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Görsel yüklenemedi: " + path);
            return null;
        }
    }

    public static void loadImages() {
        String basePath = "/Users/apple/Desktop/ödev_oyun/assets/";
        brickImage = loadImage(basePath + "brick.png");
        steelImage = loadImage(basePath + "steel.png");
        treeImage = loadImage(basePath + "tree.png");
        waterImage = loadImage(basePath + "water.png");
        iceImage = loadImage(basePath + "ice.png");
        eagleImage = loadImage(basePath + "eagle.png");
        System.out.println("Tüm tile görselleri yüklendi.");
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
        
        // BRICK type'a geçişte parça sistemini başlat
        if (type == TileType.BRICK && brickParts == null) {
            brickParts = new boolean[4][4];
            // Tüm parçaları başlangıçta true yap (var)
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    brickParts[i][j] = true;
                }
            }
        }
    }

    // Geriye uyumluluk için eski metodlar (şimdilik boş)
    public void setHalfSize(boolean isHalfSize, String direction) {
        // 2x2 sistemde kullanılmıyor, geriye uyumluluk için boş
    }

    public boolean isHalfSize() {
        return false; // 2x2 sistemde kullanılmıyor
    }

    public String getHalfSizeDirection() {
        return "NONE"; // 2x2 sistemde kullanılmıyor
    }

    @Override
    public void draw(Graphics g) {
        switch (type) {
            case BRICK:
                if (brickParts != null) {
                    // 2x2 parça sistemi ile çiz
                    drawBrickParts(g);
                } else {
                    // Normal brick çiz
                    if (brickImage != null) {
                        g.drawImage(brickImage, x, y, width, height, null);
                    } else {
                        g.setColor(new Color(180, 80, 30));
                        g.fillRect(x, y, width, height);
                    }
                }
                break;
            case STEEL:
                if (steelImage != null) {
                    g.drawImage(steelImage, x, y, width, height, null);
                } else {
                    g.setColor(Color.GRAY.brighter());
                    g.fillRect(x, y, width, height);
                }
                break;
            case WATER:
                if (waterImage != null) {
                    g.drawImage(waterImage, x, y, width, height, null);
                } else {
                    g.setColor(Color.BLUE);
                    g.fillRect(x, y, width, height);
                }
                break;
            case TREES:
                if (treeImage != null) {
                    g.drawImage(treeImage, x, y, width, height, null);
                } else {
                    g.setColor(new Color(34, 139, 34));
                    g.fillRect(x, y, width, height);
                }
                break;
            case ICE:
                if (iceImage != null) {
                    g.drawImage(iceImage, x, y, width, height, null);
                } else {
                    g.setColor(Color.CYAN);
                    g.fillRect(x, y, width, height);
                }
                break;
            case EAGLE:
                if (eagleImage != null) {
                    g.drawImage(eagleImage, x, y, width, height, null);
                } else {
                    g.setColor(Color.YELLOW);
                    g.fillRect(x, y, width, height);
                }
                break;
            default:
                // EMPTY - hiçbir şey çizme
                break;
        }
    }

    // 4x4 brick parçalarını çiz
    private void drawBrickParts(Graphics g) {
        int partWidth = width / 4;   // Her parça 10x10 pixel
        int partHeight = height / 4;
        
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (brickParts[row][col]) { // Bu parça varsa çiz
                    int drawX = x + col * partWidth;
                    int drawY = y + row * partHeight;
                    
                    if (brickImage != null) {
                        // Resmin ilgili parçasını çiz
                        int srcX = col * (brickImage.getWidth() / 4);
                        int srcY = row * (brickImage.getHeight() / 4);
                        int srcWidth = brickImage.getWidth() / 4;
                        int srcHeight = brickImage.getHeight() / 4;
                        
                        g.drawImage(brickImage, drawX, drawY, drawX + partWidth, drawY + partHeight,
                                    srcX, srcY, srcX + srcWidth, srcY + srcHeight, null);
                    } else {
                        // Resim yoksa renk ile çiz
                        g.setColor(new Color(180, 80, 30));
                        g.fillRect(drawX, drawY, partWidth, partHeight);
                    }
                }
            }
        }
    }
}
