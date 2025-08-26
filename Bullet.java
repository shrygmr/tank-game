import java.awt.Color;
import java.awt.Graphics;
// import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
// import java.io.Serializable;

public class Bullet extends GameObject {
    private Direction direction;
    private double speed;
    private boolean isAlive = true;
    private transient GamePanel gamePanel;
    private BulletOwner ownerType;
    private Tank ownerTank;
    
    // Bullet görselleri için statik değişkenler (4 farklı boyut)
    private static BufferedImage bullet1, bullet2, bullet3, bullet4;
    private int travelDistance = 0; // Merminin kat ettiği mesafe
    
    // Bullet görsellerini yükleyen statik metot
    public static void loadBulletImage() {
        try {
            String basePath = "/Users/apple/Desktop/ödev_oyun/assets/";
            bullet1 = ImageIO.read(new File(basePath + "bullet1.jpeg"));
            bullet2 = ImageIO.read(new File(basePath + "bullet2.jpeg"));
            bullet3 = ImageIO.read(new File(basePath + "bullet3.jpeg"));
            bullet4 = ImageIO.read(new File(basePath + "bullet4.jpeg"));
            System.out.println("Bullet görselleri yüklendi (1-4).");
        } catch (IOException e) {
            System.err.println("Bullet görselleri yüklenemedi: " + e.getMessage());
        }
    }

    public Bullet(int x, int y, Direction direction, GamePanel gamePanel, Tank ownerTank) {
        super(x, y, 20, 20); // 16x16'dan 20x20'ye büyüttük
        this.direction = direction;
        this.gamePanel = gamePanel;
        this.ownerTank = ownerTank;
        this.ownerType = ownerTank.getTankType();
        this.speed = ownerTank.getBulletSpeed();
    }
    
    public void reattach(GamePanel panel, Tank owner) {
        this.gamePanel = panel;
        this.ownerTank = owner;
    }

    public void update() {
        if (gamePanel == null) return;
        
        // Mesafeyi artır
        travelDistance += (int)speed;
        
        switch (direction) {
            case UP: y -= speed; break;
            case DOWN: y += speed; break;
            case LEFT: x -= speed; break;
            case RIGHT: x += speed; break;
        }
        if (x < 0 || x > gamePanel.getPanelWidth() || y < 0 || y > gamePanel.getPanelHeight()) {
            isAlive = false;
        }
    }
    
    @Override
    public void draw(Graphics g) {
        // Mesafeye göre hangi bullet görselini kullanacağımızı belirle
        BufferedImage currentBullet = null;
        int bulletSize = 12; // Varsayılan boyut (8'den 12'ye büyüttük)
        
        if (travelDistance < 60) {
            currentBullet = bullet1;
            bulletSize = 12; // En büyük (8'den 12'ye)
        } else if (travelDistance < 120) {
            currentBullet = bullet2;
            bulletSize = 10; // 7'den 10'a
        } else if (travelDistance < 180) {
            currentBullet = bullet3;
            bulletSize = 9; // 6'dan 9'a
        } else {
            currentBullet = bullet4;
            bulletSize = 8; // En küçük (5'ten 8'e)
        }
        
        // Boyutu güncelle
        this.width = bulletSize;
        this.height = bulletSize;
        
        // Merminin merkezini koru (küçülürken merkez kaymasın)
        int offsetX = (12 - bulletSize) / 2; // 8'den 12'ye
        int offsetY = (12 - bulletSize) / 2; // 8'den 12'ye
        
        // Eğer bullet görseli yüklendiyse onu kullan
        if (currentBullet != null) {
            g.drawImage(currentBullet, x + offsetX, y + offsetY, bulletSize, bulletSize, null);
        } else {
            // Görsel yoksa renk değiştiren kare çiz (yedek)
            if (travelDistance < 60) {
                g.setColor(Color.WHITE);
            } else if (travelDistance < 120) {
                g.setColor(Color.LIGHT_GRAY);
            } else if (travelDistance < 180) {
                g.setColor(Color.GRAY);
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            g.fillRect(x + offsetX, y + offsetY, bulletSize, bulletSize);
        }
    }
    
    public boolean isAlive() {
        return isAlive;
    }

    public void destroy() {
        isAlive = false;
    }

    public BulletOwner getOwnerType() {
        return ownerType;
    }

    public Tank getOwnerTank() {
        return ownerTank;
    }
    
    public Direction getDirection() {
        return direction;
    }
}