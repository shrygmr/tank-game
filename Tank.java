import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public abstract class Tank extends GameObject implements Runnable {
    protected Direction direction;
    protected double speed;
    protected boolean isMoving;
    protected transient Thread thread;
    
    protected transient GamePanel gamePanel;
    private long lastFireTime = 0;
    private long fireCooldown = 500;
    
    protected int health;
    protected boolean isAlive = true;

    // Tank görselleri için statik değişkenler
    protected static BufferedImage player1Up, player1Down, player1Left, player1Right;
    protected static BufferedImage player2Up, player2Down, player2Left, player2Right;
    protected static BufferedImage enemyBasicUp, enemyBasicDown, enemyBasicLeft, enemyBasicRight;
    protected static BufferedImage enemyFastUp, enemyFastDown, enemyFastLeft, enemyFastRight;
    protected static BufferedImage enemyPowerUp, enemyPowerDown, enemyPowerLeft, enemyPowerRight;
    protected static BufferedImage enemyArmorUp, enemyArmorDown, enemyArmorLeft, enemyArmorRight;

    protected boolean isSliding = false;
    private int slideCounter = 0;
    private Direction slideDirection;
    private final int SLIDE_DURATION = 15;

    protected boolean hasShield = false;
    private long shieldActivateTime;
    private static final long SHIELD_DURATION = 10000;

    protected boolean isFrozen = false;
    private long frozenActivateTime;
    private static final long FROZEN_DURATION = 8000;

    protected int starLevel = 0;
    protected int bulletLimit = 1;
    protected boolean canMoveOnWater = false;

    // Tank görsellerini yükleyen statik metot
    public static void loadTankImages() {
        String basePath = "/Users/apple/Desktop/ödev_oyun/assets/";
        
        try {
            // Player 1 görselleri
            player1Up = ImageIO.read(new File(basePath + "player1_up.png"));
            player1Down = ImageIO.read(new File(basePath + "player1_down.png"));
            player1Left = ImageIO.read(new File(basePath + "player1_left.png"));
            player1Right = ImageIO.read(new File(basePath + "player1_right.png"));
            
            // Player 2 görselleri
            player2Up = ImageIO.read(new File(basePath + "player2_up.png"));
            player2Down = ImageIO.read(new File(basePath + "player2_down.png"));
            player2Left = ImageIO.read(new File(basePath + "player2_left.png"));
            player2Right = ImageIO.read(new File(basePath + "player2_right.png"));
            
            // Enemy Basic görselleri
            enemyBasicUp = ImageIO.read(new File(basePath + "enemy_basic_up.png"));
            enemyBasicDown = ImageIO.read(new File(basePath + "enemy_basic_down.png"));
            enemyBasicLeft = ImageIO.read(new File(basePath + "enemy_basic_left.png"));
            enemyBasicRight = ImageIO.read(new File(basePath + "enemy_basic_right.png"));
            
            // Enemy Fast görselleri
            enemyFastUp = ImageIO.read(new File(basePath + "enemy_fast_up.png"));
            enemyFastDown = ImageIO.read(new File(basePath + "enemy_fast_down.png"));
            enemyFastLeft = ImageIO.read(new File(basePath + "enemy_fast_left.png"));
            enemyFastRight = ImageIO.read(new File(basePath + "enemy_fast_right.png"));
            
            // Enemy Power görselleri
            enemyPowerUp = ImageIO.read(new File(basePath + "enemy_power_up.png"));
            enemyPowerDown = ImageIO.read(new File(basePath + "enemy_power_down.png"));
            enemyPowerLeft = ImageIO.read(new File(basePath + "enemy_power_left.png"));
            enemyPowerRight = ImageIO.read(new File(basePath + "enemy_power_right.png"));
            
            // Enemy Armor görselleri
            enemyArmorUp = ImageIO.read(new File(basePath + "enemy_armor_up_1.png"));
            enemyArmorDown = ImageIO.read(new File(basePath + "enemy_armor_down_1.png"));
            enemyArmorLeft = ImageIO.read(new File(basePath + "enemy_armor_left_1.png"));
            enemyArmorRight = ImageIO.read(new File(basePath + "enemy_armor_right_1.png"));
            
            System.out.println("Tüm tank görselleri başarıyla yüklendi.");
        } catch (IOException e) {
            System.err.println("Tank görselleri yüklenirken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Tank(int x, int y, int size, GamePanel gamePanel) {
        super(x, y, size, size);
        revive(gamePanel);
    }
    
    public void revive(GamePanel panel) {
        this.gamePanel = panel;
        this.thread = new Thread(this);
        this.thread.start(); // Thread'i başlat!
    }

    public abstract BulletOwner getTankType();
    
    public int getHealth() {
        return health;
    }

    public void updateStatusEffects() {
        if (hasShield && System.currentTimeMillis() - shieldActivateTime > SHIELD_DURATION) {
            hasShield = false;
        }
        if (isFrozen && System.currentTimeMillis() - frozenActivateTime > FROZEN_DURATION) {
            isFrozen = false;
        }
    }

    public void takeDamage(int damage) {
        if (hasShield) {
            return;
        }
        this.health -= damage;
        if (this.health <= 0) {
            this.isAlive = false;
        }
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    // Kayma efekti başlatma metodu
    public void startSliding(Direction slideDirection, int steps) {
        this.slideDirection = slideDirection;
        this.slideCounter = steps;
        this.isSliding = true;
        this.isMoving = false; // Normal hareketi durdur
    }

    public void start() {
        if (thread != null && thread.isAlive()) {
            return; // Zaten çalışıyorsa hiçbir şey yapma
        }
        this.isAlive = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        this.isAlive = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public void fire() {
        // Mermi limiti kontrolü kaldırıldı

        if (System.currentTimeMillis() - lastFireTime > fireCooldown) {
            int bulletX = x + width / 2 - 10;  // 20/2 = 10
            int bulletY = y + height / 2 - 10; // 20/2 = 10

            switch (direction) {
                case UP: bulletY = y - 20; break;
                case DOWN: bulletY = y + height; break;
                case LEFT: bulletX = x - 20; break;
                case RIGHT: bulletX = x + width; break;
            }
            
            gamePanel.addBullet(new Bullet(bulletX, bulletY, this.direction, this.gamePanel, this));
            lastFireTime = System.currentTimeMillis();
        }
    }

    protected void slide() {
        if (slideCounter > 0) {
            int nextX = x;
            int nextY = y;
            double slideSpeed = speed * 1.5; // Buzda HIZLI hareket (1.5x hız)

            switch (slideDirection) {
                case UP: nextY = (int)(nextY - slideSpeed); break;
                case DOWN: nextY = (int)(nextY + slideSpeed); break;
                case LEFT: nextX = (int)(nextX - slideSpeed); break;
                case RIGHT: nextX = (int)(nextX + slideSpeed); break;
            }
            
            if (gamePanel.getCollisionChecker().canTankMove(this, nextX, nextY)) {
                x = nextX;
                y = nextY;
            }
            slideCounter--;
        } else {
            isSliding = false;
            isMoving = false;
        }
    }

    public void move() {
        if (!isMoving || isSliding || isFrozen) return;

        int nextX = x;
        int nextY = y;
        
        // Buz üstünde hız artışı
        double currentSpeed = speed;
        if (gamePanel.getTileTypeAt(x, y) == TileType.ICE) {
            currentSpeed = speed * 1.5; // Buzda 1.5x hızlı hareket
        }
        
        switch (direction) {
            case UP: nextY = (int)(nextY - currentSpeed); break;
            case DOWN: nextY = (int)(nextY + currentSpeed); break;
            case LEFT: nextX = (int)(nextX - currentSpeed); break;
            case RIGHT: nextX = (int)(nextX + currentSpeed); break;
        }

        if (gamePanel.getCollisionChecker().canTankMove(this, nextX, nextY)) {
            //System.out.println("Tank pozisyon değişiyor: (" + x + "," + y + ") -> (" + nextX + "," + nextY + ")");
            x = nextX;
            y = nextY;
            
            // Buz kontrolü - eğer tank buz üstündeyse kayma başlat
            if (gamePanel.getTileTypeAt(x, y) == TileType.ICE && !isSliding) {
                startSliding(direction, 5); // 5 adım kayma (daha uzun)
            }
        }
    }
    
    @Override
    public void run() {
        while (isAlive && !Thread.currentThread().isInterrupted()) {
            try {
                updateStatusEffects();
                if (isSliding) {
                    slide();
                } else {
                    move();
                }
                Thread.sleep(15); // 10'dan 15'e çıkarıldı (daha yumuşak hareket)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Tank thread hatası: " + e.getMessage());
                // Hata durumunda thread'i durdurmayıp devam ettir
            }
        }
    }

    public void setDirection(Direction newDir) {
        this.direction = newDir;
    }
    
    public void setMoving(boolean moving) {
        if (!moving && gamePanel.getTileTypeAt(this.x, this.y) == TileType.ICE) {
            this.isSliding = true;
            this.slideCounter = SLIDE_DURATION;
        } else {
            this.isMoving = moving;
            this.isSliding = false;
        }
    }
    
    public Direction getDirection() {
        return direction;
    }

    public void activateShield() {
        this.hasShield = true;
        this.shieldActivateTime = System.currentTimeMillis();
    }
    
    public void setFrozen(boolean frozen) {
        this.isFrozen = frozen;
        if(frozen) {
            this.frozenActivateTime = System.currentTimeMillis();
        }
    }

    public void addStar() {
        if (starLevel < 3) {
            starLevel++;
        }
        if (starLevel >= 2) {
            bulletLimit = 2;
        }
    }
    
    public int getStarLevel() {
        return starLevel;
    }

    public void setStarLevel(int level) {
        this.starLevel = Math.min(level, 3);
        if (this.starLevel >= 2) {
            this.bulletLimit = 2;
        }
    }

    public double getBulletSpeed() {
        if (starLevel >= 1) {
            return 8.0; // 12'den 8'e düşürüldü (animasyon için)
        }
        return 6.0; // 8'den 6'ya düşürüldü
    }
    
    public boolean canMoveOnWater() {
        return canMoveOnWater;
    }
    
    public void setCanMoveOnWater(boolean canMove) {
        this.canMoveOnWater = canMove;
    }
}