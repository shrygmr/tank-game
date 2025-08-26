import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PlayerTank extends Tank {
    private int lives;
    private Color color;
    private final int startX, startY;
    private int score = 0; // Puan değişkeni eklendi

    public PlayerTank(int x, int y, int size, GamePanel gamePanel, Color color) {
        super(x, y, size, gamePanel);
        this.startX = x;
        this.startY = y;
        this.speed = 2; // 3'ten 2'ye düşürüldü
        this.lives = 2;
        this.health = 1;
        this.direction = Direction.UP;
        this.color = color;
        activateShield();
    }

    @Override
    public BulletOwner getTankType() {
        return BulletOwner.PLAYER;
    }

    @Override
    public void takeDamage(int damage) {
        if (hasShield) return;
        
        if (this.lives > 0) {
            this.lives--;
            respawn();
        } else {
            this.isAlive = false;
        }
    }

    public void respawn() {
        this.x = startX;
        this.y = startY;
        this.health = 1;
        this.isAlive = true; // Önemli: isAlive'ı true yap
        activateShield();
        
        // Thread durmuşsa yeniden başlat
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this);
            thread.start();
        }
        
        System.out.println("Respawned! Lives left: " + this.lives);
    }

    public void addLife() {
        this.lives++;
        System.out.println("Extra life! Total lives: " + this.lives);
    }
    
    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }
    
    // YENİ METOTLAR: Puan sistemi için
    public void addScore(int points) {
        this.score += points;
    }

    public int getScore() {
        return score;
    }

    @Override
    public void run() {
        while (this.isAlive) {
            updateStatusEffects();
            if (isSliding) {
                slide();
            } else {
                move();
            }
            try {
                Thread.sleep(15); // 10'dan 15'e çıkarıldı
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    @Override
    public void draw(Graphics g) {
        BufferedImage tankImage = null;
        
        // Player 1 (turuncu) veya Player 2 (yeşil) görseli seç
        boolean isPlayer1 = (this.color.equals(Color.ORANGE));
        
        switch (direction) {
            case UP:
                tankImage = isPlayer1 ? player1Up : player2Up;
                break;
            case DOWN:
                tankImage = isPlayer1 ? player1Down : player2Down;
                break;
            case LEFT:
                tankImage = isPlayer1 ? player1Left : player2Left;
                break;
            case RIGHT:
                tankImage = isPlayer1 ? player1Right : player2Right;
                break;
        }
        
        // Ağaç altında mı kontrol et (gizlenme efekti)
        boolean isUnderTree = (gamePanel != null && gamePanel.getTileTypeAt(this.x, this.y) == TileType.TREES);
        
        // Görsel varsa onu çiz, yoksa eski yöntemi kullan
        if (tankImage != null) {
            if (isUnderTree) {
                // Ağaç altındaysa %50 şeffaflık
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
                g2d.drawImage(tankImage, x, y, width, height, null);
                g2d.dispose();
            } else {
                g.drawImage(tankImage, x, y, width, height, null);
            }
        } else {
            // Yedek çizim (görsel yoksa)
            if (isUnderTree) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
                g2d.setColor(this.color);
                g2d.fillRect(x, y, width, height);
                g2d.dispose();
            } else {
                g.setColor(this.color);
                g.fillRect(x, y, width, height);
                g.setColor(Color.WHITE);
                switch (direction) {
                    case UP: g.fillRect(x + width/2 - 2, y, 4, height/2); break;
                    case DOWN: g.fillRect(x + width/2 - 2, y + height/2, 4, height/2); break;
                    case LEFT: g.fillRect(x, y + height/2 - 2, width/2, 4); break;
                    case RIGHT: g.fillRect(x + width/2, y + height/2 - 2, width/2, 4); break;
                }
            }
        }
    }
}