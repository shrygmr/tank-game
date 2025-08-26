import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Explosion extends GameObject implements Serializable {
    private static BufferedImage patlama1, patlama2, patlama3, patlama4;
    private long startTime;
    private static final long EXPLOSION_DURATION = 400; // 400ms patlama süresi (4 frame için)
    private boolean isActive = true;
    
    // Patlama görsellerini yükleyen statik metot
    public static void loadExplosionImage() {
        try {
            patlama1 = ImageIO.read(new File("/Users/apple/Desktop/ödev_oyun/assets/patlama1.png"));
            patlama2 = ImageIO.read(new File("/Users/apple/Desktop/ödev_oyun/assets/patlama2.png"));
            patlama3 = ImageIO.read(new File("/Users/apple/Desktop/ödev_oyun/assets/patlama3.png"));
            patlama4 = ImageIO.read(new File("/Users/apple/Desktop/ödev_oyun/assets/patlama4.png"));
            System.out.println("Patlama görselleri yüklendi (1-4).");
        } catch (IOException e) {
            System.err.println("Patlama görselleri yüklenemedi: " + e.getMessage());
        }
    }
    
    public Explosion(int x, int y) {
        super(x - 15, y - 15, 30, 30); // Mermi konumunun etrafında 30x30 patlama
        this.startTime = System.currentTimeMillis();
    }
    
    public void update() {
        if (System.currentTimeMillis() - startTime > EXPLOSION_DURATION) {
            isActive = false;
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    @Override
    public void draw(Graphics g) {
        if (!isActive) return;
        
        // Zamana göre hangi frame'i göstereceğimizi belirle
        long elapsed = System.currentTimeMillis() - startTime;
        double progress = (double)elapsed / EXPLOSION_DURATION;
        
        BufferedImage currentFrame = null;
        int explosionSize = 32; // Patlama boyutu
        
        // 4 frame'i eşit zamanda göster
        if (progress < 0.25) {
            currentFrame = patlama1;
            explosionSize = 20; // Küçük başla
        } else if (progress < 0.5) {
            currentFrame = patlama2;
            explosionSize = 28; // Büyü
        } else if (progress < 0.75) {
            currentFrame = patlama3;
            explosionSize = 35; // En büyük
        } else {
            currentFrame = patlama4;
            explosionSize = 30; // Küçül
        }
        
        // Merkezi koru - patlama merkezde kalacak şekilde
        int drawX = x + (width - explosionSize) / 2;
        int drawY = y + (height - explosionSize) / 2;
        
        if (currentFrame != null) {
            g.drawImage(currentFrame, drawX, drawY, explosionSize, explosionSize, null);
        } else {
            // Görsel yoksa renkli animasyon (yedek)
            java.awt.Color explosionColor;
            if (progress < 0.25) {
                explosionColor = java.awt.Color.YELLOW;
                explosionSize = 16;
            } else if (progress < 0.5) {
                explosionColor = java.awt.Color.ORANGE;
                explosionSize = 24;
            } else if (progress < 0.75) {
                explosionColor = java.awt.Color.RED;
                explosionSize = 30;
            } else {
                explosionColor = java.awt.Color.DARK_GRAY;
                explosionSize = 20;
            }
            
            drawX = x + (width - explosionSize) / 2;
            drawY = y + (height - explosionSize) / 2;
            
            g.setColor(explosionColor);
            g.fillOval(drawX, drawY, explosionSize, explosionSize);
            
            // İç daire efekti
            g.setColor(java.awt.Color.WHITE);
            int innerSize = explosionSize / 3;
            int innerX = drawX + (explosionSize - innerSize) / 2;
            int innerY = drawY + (explosionSize - innerSize) / 2;
            g.fillOval(innerX, innerY, innerSize, innerSize);
        }
    }
}
