import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class PowerUp extends GameObject implements Serializable {
    private PowerUpType type;
    private long spawnTime;
    private static final long DURATION = 10000;

    // Her bir PowerUpType için görsel tutacak statik bir Map
    private static Map<PowerUpType, BufferedImage> powerUpImages = new EnumMap<>(PowerUpType.class);

    public PowerUp(int x, int y, PowerUpType type) {
        super(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
        this.type = type;
        this.spawnTime = System.currentTimeMillis();
    }

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Güçlendirici görseli yüklenemedi: " + path);
            return null;
        }
    }

    // Tüm güçlendirici görsellerini tek seferde yükleyen metot
    public static void loadImages() {
        String basePath = "/Users/apple/Desktop/ödev_oyun/assets/";
        powerUpImages.put(PowerUpType.GRENADE, loadImage(basePath + "grenade.png"));
        powerUpImages.put(PowerUpType.HELMET, loadImage(basePath + "helmet.png"));
        powerUpImages.put(PowerUpType.SHOVEL, loadImage(basePath + "shovel.png"));
        powerUpImages.put(PowerUpType.STAR, loadImage(basePath + "star.png"));
        powerUpImages.put(PowerUpType.TANK_LIFE, loadImage(basePath + "tank_life.png"));
        powerUpImages.put(PowerUpType.TIMER, loadImage(basePath + "timer.png"));
        powerUpImages.put(PowerUpType.WEAPON, loadImage(basePath + "weapon.png"));
        System.out.println("Tüm güçlendirici görselleri yüklendi.");
    }

    public PowerUpType getType() {
        return type;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > DURATION;
    }

    @Override
    public void draw(Graphics g) {
        BufferedImage image = powerUpImages.get(this.type);
        
        // Eğer bu tip için bir görsel yüklendiyse, onu çiz
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            // Görsel yoksa, eski usul kare ve yazı çiz (yedek)
            g.setColor(Color.WHITE);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            String text = "";
            switch(type) {
                case GRENADE: text = "BOMB"; break;
                case HELMET: text = "HELM"; break;
                case SHOVEL: text = "SHOV"; break;
                case STAR: text = "STAR"; break;
                case TANK_LIFE: text = "LIFE"; break;
                case TIMER: text = "TIME"; break;
                case WEAPON: text = "GUN"; break;
            }
            g.drawString(text, x + 4, y + height / 2 + 5);
        }
    }
}