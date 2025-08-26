import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class EnemyTank extends Tank {
    public boolean isRed() {
        return isRed;
    }
    private Random random = new Random();
    private EnemyType type;
    private boolean isRed;
    private boolean hasDroppedPowerUp = false;

    public EnemyTank(int x, int y, int size, GamePanel gamePanel, EnemyType type, boolean isRed) {
        super(x, y, size, gamePanel);
        this.type = type;
        this.isRed = isRed;
        this.direction = Direction.DOWN;
        this.isMoving = true;
        
        switch (type) {
            case BASIC: 
                this.speed = 1.0; // Yavaş hareket
                this.health = 1; 
                this.bulletLimit = 1;
                break;  
            case FAST: 
                this.speed = 2.0; // Hızlı hareket
                this.health = 1; 
                this.bulletLimit = 1;
                break;   
            case POWER: 
                this.speed = 1.5; // Orta hız hareket
                this.health = 1; 
                this.bulletLimit = 2; // Hızlı ateş için daha fazla mermi
                break;  
            case ARMOR: 
                this.speed = 1.5; // Orta hız hareket
                this.health = 4;   // 4 vuruşta yok olur
                this.bulletLimit = 1;
                break;
        }
    }
    
    @Override
    public BulletOwner getTankType() {
        return BulletOwner.ENEMY;
    }

    @Override
    public void takeDamage(int damage) {
        if (!hasShield && this.isRed && !this.hasDroppedPowerUp && isAlive) {
            if (gamePanel != null) {
                gamePanel.spawnRandomPowerUp();
            }
            this.hasDroppedPowerUp = true;
        }
        super.takeDamage(damage);
    }
    
    public EnemyType getType() {
        return type;
    }
    
    public EnemyType getEnemyType() {
        return type;
    }
    
    public int getPoints() {
        return this.type.points;
    }
    
    @Override
    public void run() {
        long lastDirectionChange = System.currentTimeMillis();
        long lastFireTime = System.currentTimeMillis();

        while (this.isAlive && !Thread.currentThread().isInterrupted()) {
            try {
                updateStatusEffects();
                if (isSliding) {
                    slide();
                } else {
                    move();
                }

                if (!isFrozen) {
                    if (System.currentTimeMillis() - lastDirectionChange > 2000 + random.nextInt(3000)) {
                        direction = Direction.values()[random.nextInt(4)];
                        lastDirectionChange = System.currentTimeMillis();
                        System.out.println("EnemyTank yön değiştirdi: " + direction);
                    }

                    // Tank tipine göre ateş hızı ayarla
                    int fireDelay;
                    switch(type) {
                        case BASIC: fireDelay = 2000 + random.nextInt(2000); break; // Yavaş ateş
                        case FAST: fireDelay = 1500 + random.nextInt(1500); break;  // Orta hız ateş
                        case POWER: fireDelay = 800 + random.nextInt(800); break;   // Hızlı ateş
                        case ARMOR: fireDelay = 1500 + random.nextInt(1500); break; // Orta hız ateş
                        default: fireDelay = 2000 + random.nextInt(2000); break;
                    }
                    
                    if (System.currentTimeMillis() - lastFireTime > fireDelay) {
                        fire();
                        lastFireTime = System.currentTimeMillis();
                    }
                }
                
                Thread.sleep(15); // 10'dan 15'e çıkarıldı (daha yumuşak hareket)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("EnemyTank thread hatası: " + e.getMessage());
                // Devam et
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        BufferedImage tankImage = null;
        
        // Ağaç altında mı kontrol et (gizlenme efekti)
        boolean isUnderTree = (gamePanel != null && gamePanel.getTileTypeAt(this.x, this.y) == TileType.TREES);
        
        // Tank tipine göre görsel seç
        switch(type) {
            case BASIC:
                switch(direction) {
                    case UP: tankImage = enemyBasicUp; break;
                    case DOWN: tankImage = enemyBasicDown; break;
                    case LEFT: tankImage = enemyBasicLeft; break;
                    case RIGHT: tankImage = enemyBasicRight; break;
                }
                break;
            case FAST:
                switch(direction) {
                    case UP: tankImage = enemyFastUp; break;
                    case DOWN: tankImage = enemyFastDown; break;
                    case LEFT: tankImage = enemyFastLeft; break;
                    case RIGHT: tankImage = enemyFastRight; break;
                }
                break;
            case POWER:
                switch(direction) {
                    case UP: tankImage = enemyPowerUp; break;
                    case DOWN: tankImage = enemyPowerDown; break;
                    case LEFT: tankImage = enemyPowerLeft; break;
                    case RIGHT: tankImage = enemyPowerRight; break;
                }
                break;
            case ARMOR:
                switch(direction) {
                    case UP: tankImage = enemyArmorUp; break;
                    case DOWN: tankImage = enemyArmorDown; break;
                    case LEFT: tankImage = enemyArmorLeft; break;
                    case RIGHT: tankImage = enemyArmorRight; break;
                }
                break;
        }
        
        // Görsel varsa onu çiz, yoksa eski yöntemi kullan
        if (tankImage != null) {
            if (isUnderTree) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
                g2d.drawImage(tankImage, x, y, width, height, null);
                g2d.dispose();
            } else {
                g.drawImage(tankImage, x, y, width, height, null);
            }
        } else {
            // Yedek çizim (görsel yoksa)
            Color tankColor;
            switch(type) {
                case FAST: tankColor = Color.CYAN; break;
                case POWER: tankColor = Color.BLUE; break;
                case ARMOR: 
                    if (health > 2) tankColor = new Color(0, 100, 0);
                    else tankColor = Color.PINK;
                    break;
                default:
                    tankColor = Color.LIGHT_GRAY; break;
            }
            
            g.setColor(tankColor);
            
            if (isUnderTree) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
                g2d.fillRect(x, y, width, height);
                g2d.dispose();
            } else {
                g.fillRect(x, y, width, height);
            }
        }
    }
}