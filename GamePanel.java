import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class GamePanel extends JPanel implements Runnable {
    
    private int panelWidth = 800;
    private int panelHeight = 600;

    public static final int MAX_ENEMIES_ON_SCREEN = 4;
    public static final int TOTAL_ENEMIES_PER_ROUND = 20;

    private GameState gameState;
    
    private int selectedMenuOption = 0;
    private String[] menuOptions = {"1 Player", "2 Players", "Continue"};
    private int selectedStageOption = 13;
    
    private transient Thread gameThread;
    private Tile[][] map;
    private PlayerTank player1;
    private PlayerTank player2;
List<EnemyTank> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>(); // Patlama efektleri
    private ScoreSystem scoreSystem = new ScoreSystem(); // Puan sistemi
    
    private Tile eagleBase;
    private List<Tile> fortressWalls = new ArrayList<>();
    private List<TileType> originalFortressWallTypes = new ArrayList<>();
    private boolean isShovelActive = false;
    private long shovelActivateTime;
    private static final long SHOVEL_DURATION = 15000;
    
    private transient CollisionChecker collisionChecker;
    private int enemiesSpawned = 0;
    private Random random = new Random();
    
    // Pause/Resume sistemi
    private boolean isPaused = false;
    private boolean pauseKeyPressed = false;
    private static int debugCounter = 0; // Debug mesajları için
    private int level = 1;
    private GameMode gameMode;
    private boolean saveFileExists = false;
    private Map<EnemyType, Integer> destroyedEnemyCounts;
    private long stageTransitionStartTime;

    private static transient BufferedImage cursorImage;

    public GamePanel() {
        Tile.loadImages();
        PowerUp.loadImages();
        Tank.loadTankImages();
        Bullet.loadBulletImage(); // Bullet görselini yükle
        Explosion.loadExplosionImage(); // Patlama görselini yükle
        loadCursorImage();

        this.gameState = GameState.START_SCREEN;
        this.gameMode = GameMode.ONE_PLAYER; // Varsayılan olarak tek oyuncu
        this.setPreferredSize(new Dimension(this.panelWidth, this.panelHeight));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(new KeyInputHandler(this));
        checkSaveFile();
        startGameThread();
    }
    
    public static void loadCursorImage() {
        try {
            cursorImage = ImageIO.read(new File("/Users/apple/Desktop/ödev_oyun/assets/cursor.png"));
            System.out.println("Cursor görseli başarıyla yüklendi.");
        } catch (IOException e) {
            System.err.println("Cursor görseli yüklenemedi: assets/cursor.png bulunamadı.");
        }
    }

    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setupGame() {
        if (level == 1) {
            map = MapLoader.loadMap("level1.txt");
        } else {
            map = MapGenerator.generateRandomMap(13, 13);
        }
        
        if (map == null || map.length == 0) {
            System.out.println("Harita yüklenemedi!");
            this.panelWidth = 800; this.panelHeight = 600;
        } else {
            this.panelWidth = map[0].length * Tile.TILE_SIZE;
            this.panelHeight = map.length * Tile.TILE_SIZE;
        }
        this.setPreferredSize(new Dimension(this.panelWidth, this.panelHeight));
        
        if (getTopLevelAncestor() != null) {
            ((JFrame) getTopLevelAncestor()).pack();
        }

        collisionChecker = new CollisionChecker(this);
        findEagle();
        
        destroyedEnemyCounts = new HashMap<>();
        for (EnemyType type : EnemyType.values()) {
            destroyedEnemyCounts.put(type, 0);
        }
        
        int p1_lives = 2;
        int p1_stars = 0;
        int p2_lives = 2;
        int p2_stars = 0;

        if (player1 != null) {
            p1_lives = player1.getLives();
            p1_stars = player1.getStarLevel();
            player1.stop();
        }
        if (player2 != null) {
            p2_lives = player2.getLives();
            p2_stars = player2.getStarLevel();
            player2.stop();
        }
        synchronized(enemies) {
            for (EnemyTank enemy : enemies) {
                enemy.stop();
            }
        }

        enemies.clear();
        bullets.clear();
        powerUps.clear();
        explosions.clear(); // Patlama efektlerini temizle
        enemiesSpawned = 0;
        isShovelActive = false; // Kürek etkisini yeni seviyede sıfırla
        
        player1 = new PlayerTank(10 * Tile.TILE_SIZE, (map.length - 2) * Tile.TILE_SIZE, Tile.TILE_SIZE, this, Color.ORANGE);
        player1.setLives(p1_lives);
        player1.setStarLevel(p1_stars);
        player1.start();
        System.out.println("Player 1 pozisyonu: x=" + (10 * Tile.TILE_SIZE) + ", y=" + ((map.length - 2) * Tile.TILE_SIZE));
        
        System.out.println("GameMode: " + gameMode + " (TWO_PLAYER kontrolü: " + (gameMode == GameMode.TWO_PLAYER) + ")");
        
        if (gameMode == GameMode.TWO_PLAYER) {
            player2 = new PlayerTank(8 * Tile.TILE_SIZE, (map.length - 2) * Tile.TILE_SIZE, Tile.TILE_SIZE, this, Color.GREEN);
            player2.setLives(p2_lives);
            player2.setStarLevel(p2_stars);
            player2.start();
            System.out.println("Player 2 oluşturuldu!");
            System.out.println("Player 2 pozisyonu: x=" + (8 * Tile.TILE_SIZE) + ", y=" + ((map.length - 2) * Tile.TILE_SIZE));
            System.out.println("Map boyutu: " + map[0].length + "x" + map.length + " (genişlik x yükseklik)");
            System.out.println("Panel boyutu: " + panelWidth + "x" + panelHeight);
        } else {
            player2 = null;
            System.out.println("Tek oyuncu modu - Player 2 oluşturulmadı.");
        }
        
        revalidate();
        requestFocusInWindow();
    }

    
    private void findEagle() {
        if (map == null) return;
        fortressWalls.clear();
        originalFortressWallTypes.clear();
        
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col].getType() == TileType.EAGLE) {
                    this.eagleBase = map[row][col];
                    int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
                    int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
                    String[] directions = {"CORNER_TL", "DOWN", "CORNER_TR", "RIGHT", "LEFT", "CORNER_BL", "UP", "CORNER_BR"};
                    
                    for (int i = 0; i < 8; i++) {
                        int r = row + dr[i];
                        int c = col + dc[i];
                        if (r >= 0 && r < map.length && c >= 0 && c < map[0].length) {
                            Tile wall = map[r][c];
                            fortressWalls.add(wall);
                            originalFortressWallTypes.add(wall.getType());
                            
                            // Kenar duvarları yarım boyut, köşe duvarları da yarım boyut ama farklı konumlarda
                            if (wall.getType() == TileType.BRICK) {
                                wall.setHalfSize(true, directions[i]);
                            }
                        }
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60;
        double delta = 0;
        long lastTime = System.nanoTime();
        
        while (gameThread != null && !gameThread.isInterrupted()) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                if (gameState == GameState.PLAYING && !isPaused) {
                    update();
                } else if (gameState == GameState.STAGE_TRANSITION) {
                    if (System.currentTimeMillis() - stageTransitionStartTime > 4000) {
                        advanceToNextLevel();
                    }
                }
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        boolean p1_alive = (player1 != null && player1.isAlive());
        boolean p2_alive = (player2 != null && player2.isAlive());

        if ((gameMode == GameMode.ONE_PLAYER && !p1_alive) || 
            (gameMode == GameMode.TWO_PLAYER && !p1_alive && !p2_alive)) {
            setGameState(GameState.GAME_OVER);
        }

        if (enemiesSpawned >= TOTAL_ENEMIES_PER_ROUND && enemies.isEmpty()) {
            if (gameState != GameState.STAGE_TRANSITION) {
                gameState = GameState.STAGE_TRANSITION;
                stageTransitionStartTime = System.currentTimeMillis();
            }
            return;
        }

        if (enemies.size() < MAX_ENEMIES_ON_SCREEN && enemiesSpawned < TOTAL_ENEMIES_PER_ROUND) {
            spawnNewEnemy();
        }
        
        if(player1 != null) player1.updateStatusEffects();
        if(player2 != null) player2.updateStatusEffects();
        for(EnemyTank enemy : enemies) {
            enemy.updateStatusEffects();
        }
        
        if (isShovelActive && System.currentTimeMillis() - shovelActivateTime > SHOVEL_DURATION) {
            revertFortressWalls();
        }

        synchronized(bullets) {
            Iterator<Bullet> bulletIterator = bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet bullet = bulletIterator.next();
                bullet.update();
                collisionChecker.checkBulletToTile(bullet);

                if (bullet.getOwnerType() == BulletOwner.PLAYER) {
                    synchronized(enemies) {
                        for (EnemyTank enemy : enemies) {
                            if (bullet.isAlive()) {
                                collisionChecker.checkBulletToTank(bullet, enemy);
                            }
                        }
                    }
                } else {
                    if (player1 != null && player1.isAlive() && bullet.isAlive()) {
                        collisionChecker.checkBulletToTank(bullet, player1);
                    }
                    if (player2 != null && player2.isAlive() && bullet.isAlive()) {
                        collisionChecker.checkBulletToTank(bullet, player2);
                    }
                }
                
                if (!bullet.isAlive()) {
                    bulletIterator.remove();
                }
            }
        }
        
        synchronized(enemies) {
            Iterator<EnemyTank> enemyIterator = enemies.iterator();
            while(enemyIterator.hasNext()) {
                EnemyTank enemy = enemyIterator.next();
                if (!enemy.isAlive()) {
                    // Power-up spawn burada değil, sadece EnemyTank.takeDamage'da
                    if (player1 != null) {
                        player1.addScore(enemy.getPoints());
                    }
                    if(destroyedEnemyCounts != null) {
                        int currentCount = destroyedEnemyCounts.get(enemy.getType());
                        destroyedEnemyCounts.put(enemy.getType(), currentCount + 1);
                    }
                    enemy.stop();
                    enemyIterator.remove();
                }
            }
        }
    
        collisionChecker.checkBulletToBulletCollision(bullets);
        checkPowerUpCollection();
        powerUps.removeIf(PowerUp::isExpired);
        
        // Patlama efektlerini güncelle ve bitenleri kaldır - synchronized
        synchronized(explosions) {
            Iterator<Explosion> explosionIterator = explosions.iterator();
            while(explosionIterator.hasNext()) {
                Explosion explosion = explosionIterator.next();
                explosion.update();
                if (!explosion.isActive()) {
                    explosionIterator.remove();
                }
            }
        }
    }
    
    private void advanceToNextLevel() {
        level++;
        System.out.println("Tebrikler! Seviye " + level + "'e geçtiniz.");
        setGameState(GameState.PLAYING);
        setupGame();
    }
    
    
    

    

    

    public synchronized void spawnNewEnemy() {
        enemiesSpawned++;
        
        boolean isRed = false;
        if (enemiesSpawned == 4 || enemiesSpawned == 11 || enemiesSpawned == 18) {
            isRed = true;
        } else if (random.nextInt(100) < 30) {
            isRed = true;
        }

        EnemyType type = EnemyType.values()[random.nextInt(EnemyType.values().length)];
        
        // Ödev: "sol üst, orta üst veya sağ üst'te doğmalılar"
        int[] spawnXPositions = {0, 6 * Tile.TILE_SIZE, 12 * Tile.TILE_SIZE}; // Sol, Orta, Sağ
        int spawnY = 0; // Üst row
        
        // Tüm pozisyonları test et, boş olanları bul
        java.util.List<Integer> availablePositions = new java.util.ArrayList<>();
        
        for (int i = 0; i < spawnXPositions.length; i++) {
            int testX = spawnXPositions[i];
            boolean positionAvailable = true;
            
            // Mevcut enemy tank'larla çarpışma kontrolü - synchronized içinde
            for (EnemyTank enemy : enemies) {
                // Tam pozisyon eşleşmesi kontrolü (daha kesin)
                if (enemy.getX() == testX && enemy.getY() == spawnY) {
                    positionAvailable = false;
                    break;
                }
                // Yakın mesafe kontrolü de ekle
                if (Math.abs(enemy.getX() - testX) < Tile.TILE_SIZE/2 && 
                    Math.abs(enemy.getY() - spawnY) < Tile.TILE_SIZE/2) {
                    positionAvailable = false;
                    break;
                }
            }
            
            // Tile ve player kontrolleri
            if (positionAvailable && canSpawnAt(testX, spawnY)) {
                availablePositions.add(testX);
            }
        }
        
        // Eğer hiç boş pozisyon yoksa, spawn'ı geciktir
        if (availablePositions.isEmpty()) {
            enemiesSpawned--; // Geri al, tekrar dene
            System.out.println("Spawn geciktirildi - hiç boş pozisyon yok");
            return;
        }
        
        // Rastgele boş pozisyon seç
        int spawnX = availablePositions.get(random.nextInt(availablePositions.size()));
        
        System.out.println("Enemy spawn başarılı: (" + spawnX + "," + spawnY + ") Type:" + type + " Red:" + isRed + " Total:" + enemies.size());

        EnemyTank newEnemy = new EnemyTank(spawnX, spawnY, Tile.TILE_SIZE, this, type, isRed);
        enemies.add(newEnemy);
        newEnemy.start();
    }
    
    // Spawn pozisyonunun uygun olup olmadığını kontrol et
    private boolean canSpawnAt(int x, int y) {
        // Basit tile kontrolü
        TileType tileType = getTileTypeAt(x, y);
        if (tileType != TileType.EMPTY && tileType != TileType.TREES) {
            return false;
        }
        
        // Player tank'larla çarpışma kontrolü
        if (player1 != null && player1.isAlive()) {
            Rectangle spawnBounds = new Rectangle(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
            if (spawnBounds.intersects(player1.getBounds())) {
                return false;
            }
        }
        
        if (player2 != null && player2.isAlive()) {
            Rectangle spawnBounds = new Rectangle(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
            if (spawnBounds.intersects(player2.getBounds())) {
                return false;
            }
        }
        
        return true;
    }

    public void checkPowerUpCollection() {
        Iterator<PowerUp> it = powerUps.iterator();
        while(it.hasNext()) {
            PowerUp powerUp = it.next();
            boolean collected = false;
            
            if (player1 != null && player1.isAlive() && player1.getBounds().intersects(powerUp.getBounds())) {
                applyPowerUp(player1, powerUp.getType());
                collected = true;
            }
            
            if (!collected && player2 != null && player2.isAlive() && player2.getBounds().intersects(powerUp.getBounds())) {
                applyPowerUp(player2, powerUp.getType());
                collected = true;
            }

            if (!collected) {
                synchronized(enemies) {
                    for (EnemyTank enemy : enemies) {
                        if (enemy.isAlive() && enemy.getBounds().intersects(powerUp.getBounds())) {
                            applyPowerUp(enemy, powerUp.getType());
                            collected = true;
                            break;
                        }
                    }
                }
            }

            if (collected) {
                it.remove();
            }
        }
    }

    private void applyPowerUp(Tank collector, PowerUpType type) {
        boolean isPlayerCollector = (collector.getTankType() == BulletOwner.PLAYER);
        
        switch(type) {
            case GRENADE:
                if (isPlayerCollector) {
                    Iterator<EnemyTank> it = enemies.iterator();
                    while(it.hasNext()){
                        EnemyTank enemy = it.next();
                        if (player1 != null) player1.addScore(enemy.getPoints());
                         int currentCount = destroyedEnemyCounts.get(enemy.getType());
                        destroyedEnemyCounts.put(enemy.getType(), currentCount + 1);
                        it.remove();
                    }
                } else {
                    if(player1 != null) player1.takeDamage(999);
                    if(player2 != null) player2.takeDamage(999);
                }
                break;
            case HELMET:
                collector.activateShield();
                break;
            case SHOVEL:
                if (isPlayerCollector) {
                    upgradeFortressWalls();
                } else {
                    destroyFortressWalls();
                }
                break;
            case STAR:
                collector.addStar();
                break;
            case WEAPON:
                collector.setStarLevel(3);
                break;
            case TANK_LIFE:
                if (isPlayerCollector) {
                    ((PlayerTank) collector).addLife();
                }
                break;
            case TIMER:
                if (isPlayerCollector) {
                    for(EnemyTank enemy : enemies) enemy.setFrozen(true);
                } else {
                    if(player1 != null) player1.setFrozen(true);
                    if(player2 != null) player2.setFrozen(true);
                }
                break;
        }
    }

    private void upgradeFortressWalls() {
        for (Tile wall : fortressWalls) {
            if (wall.getType() == TileType.BRICK || wall.getType() == TileType.EMPTY) {
                boolean wasHalfSize = wall.isHalfSize();
                String direction = wall.getHalfSizeDirection();
                wall.setType(TileType.STEEL);
                if (wasHalfSize) {
                    wall.setHalfSize(true, direction);
                }
            }
        }
        isShovelActive = true;
        shovelActivateTime = System.currentTimeMillis();
    }

    private void destroyFortressWalls() {
        for (Tile wall : fortressWalls) {
            if (wall.getType() == TileType.BRICK || wall.getType() == TileType.STEEL) {
                wall.setType(TileType.EMPTY);
            }
        }
        isShovelActive = false;
    }

    private void revertFortressWalls() {
        for (int i = 0; i < fortressWalls.size(); i++) {
            Tile wall = fortressWalls.get(i);
            TileType originalType = originalFortressWallTypes.get(i);
            boolean wasHalfSize = wall.isHalfSize();
            String direction = wall.getHalfSizeDirection();
            
            wall.setType(originalType);
            // Yarım boyut özelliğini koru
            if (wasHalfSize && originalType == TileType.BRICK) {
                wall.setHalfSize(true, direction);
            }
        }
        isShovelActive = false;
        System.out.println("Kürek etkisi sona erdi!");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (gameState) {
            case START_SCREEN: drawStartScreen(g2d); break;
            case MENU: drawMenu(g2d); break;
            case STAGE_SELECT: drawStageSelectScreen(g2d); break;
            case STAGE_TRANSITION: drawStageTransitionScreen(g2d); break;
            case PLAYING: drawGame(g2d); break;
            case PAUSED: drawGame(g2d); drawPauseScreen(g2d); break;
            case GAME_OVER: drawGame(g2d); drawGameOverScreen(g2d); break;
        }
    }
    
    private void drawStartScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Courier New", Font.BOLD, 50));
        String title = "TANK 1990";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (getWidth() / 2) - (titleWidth / 2), getHeight() / 2 - 50);

        g.setFont(new Font("Courier New", Font.PLAIN, 30));
        String prompt = "Press ENTER to Start";
        int promptWidth = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, (getWidth() / 2) - (promptWidth / 2), getHeight() / 2 + 50);
    }

    public void drawStageSelectScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Courier New", Font.BOLD, 30));
        
        String topText = "© Y.S 1990.2   15+28";
        g.drawString(topText, (getWidth() - g.getFontMetrics().stringWidth(topText)) / 2, 100);

        for (int i = 0; i < 14; i++) {
            char stageChar = (char) ('A' + i);
            String stageText = "TANK         " + stageChar;
            
            if (i == selectedStageOption) {
                g.setColor(Color.YELLOW);
                g.drawString("→", (getWidth() / 2) - 180, 200 + i * 30);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString(stageText, (getWidth() / 2) - 140, 200 + i * 30);
        }
    }

    private void drawStageTransitionScreen(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Courier New", Font.BOLD, 30));

        String stageText = "STAGE " + level;
        g.drawString(stageText, (getWidth() - g.getFontMetrics().stringWidth(stageText)) / 2, 100);

        if (player1 != null) {
            String scoreText = "I-PLAYER   " + player1.getScore();
            g.drawString(scoreText, (getWidth() / 2) - 150, 150);
        }
        
        int yPos = 220;
        int totalDestroyed = 0;
        for (Map.Entry<EnemyType, Integer> entry : destroyedEnemyCounts.entrySet()) {
            if (entry.getValue() > 0) {
                String line = String.format("%4d PTS %2d ←", entry.getKey().points * entry.getValue(), entry.getValue());
                g.drawString(line, (getWidth() / 2) - 150, yPos);
                yPos += 40;
                totalDestroyed += entry.getValue();
            }
        }
        
        g.drawLine((getWidth()/2) - 150, yPos, (getWidth()/2) + 100, yPos);
        yPos += 40;
        String totalText = String.format("TOTAL %4d", totalDestroyed);
        g.drawString(totalText, (getWidth() / 2) - 50, yPos);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        String title = "TANK 1990";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (getWidth() / 2) - (titleWidth / 2), 150);

        g.setFont(new Font("Arial", Font.PLAIN, 30));
        for (int i = 0; i < menuOptions.length; i++) {
            if (i == 2 && !saveFileExists) {
                g.setColor(Color.GRAY);
            } else if (i == selectedMenuOption) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            String option = menuOptions[i];
            int optionWidth = g.getFontMetrics().stringWidth(option);
            g.drawString(option, (getWidth() / 2) - (optionWidth / 2), 250 + i * 50);
        }
    }

    private void drawGame(Graphics2D g) {
        if (map == null) return;

        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                TileType type = map[row][col].getType();
                if (type != TileType.TREES && type != TileType.ICE) {
                    map[row][col].draw(g);
                }
            }
        }
        
        // Buzu tankların altında çiz (zemin katmanı)
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                TileType type = map[row][col].getType();
                if (type == TileType.ICE) {
                    map[row][col].draw(g);
                }
            }
        }
        
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g);
        }
        
        if(player1 != null) player1.draw(g);
        if(player2 != null) {
            if (debugCounter < 3) { // İlk 3 frame'de debug yap
                System.out.println("Player 2 çiziliyor... Pozisyon: (" + player2.x + ", " + player2.y + "), Canlı: " + player2.isAlive());
                debugCounter++;
            }
            player2.draw(g);
        } else {
            if (debugCounter < 3) {
                System.out.println("Player 2 null - çizilmiyor!");
                debugCounter++;
            }
        }
        
        synchronized(enemies) {
            for (EnemyTank enemy : enemies) {
                enemy.draw(g);
            }
        }
        
        // Bullets listesini synchronized şekilde çiz
        synchronized(bullets) {
            for (Bullet bullet : bullets) {
                bullet.draw(g);
            }
        }
        
        // Patlama efektlerini çiz - synchronized
        synchronized(explosions) {
            for (Explosion explosion : explosions) {
                explosion.draw(g);
            }
        }

        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                TileType type = map[row][col].getType();
                if (type == TileType.TREES) { // Sadece ağaçlar tankların üstünde
                    map[row][col].draw(g);
                }
            }
        }

        if (player1 != null) {
            g.setFont(new Font("Courier New", Font.BOLD, 20));
            g.setColor(Color.YELLOW);
            
            // Puan ve tur bilgisi
            g.drawString("P1", 20, 25);
            g.drawString(String.format("%08d", scoreSystem.getPlayer1Score()), 20, 50);
            g.drawString("TUR-" + scoreSystem.getCurrentStage(), 20, 75);
            
            // Yüksek skor
            g.drawString("HI-SCORE", panelWidth - 200, 25);
            g.drawString(String.format("%08d", scoreSystem.loadHighScore()), panelWidth - 200, 50);
            
            // İki oyuncu modunda 2. oyuncu puanı
            if (player2 != null && GameMode.getCurrentMode() == GameMode.TWO_PLAYER) {
                g.drawString("P2", panelWidth / 2 - 50, 25);
                g.drawString(String.format("%08d", scoreSystem.getPlayer2Score()), panelWidth / 2 - 50, 50);
            }
        }
    }

    private void drawPauseScreen(Graphics2D g) {
        // Yarı şeffaf siyah overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // PAUSED yazısı
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Courier New", Font.BOLD, 60));
        String msg = "PAUSED";
        int msgWidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (getWidth() / 2) - (msgWidth / 2), getHeight() / 2 - 50);
        
        // Devam etme talimatı
        g.setColor(Color.WHITE);
        g.setFont(new Font("Courier New", Font.PLAIN, 24));
        String instruction = "Devam etmek için P tuşuna basın";
        int instrWidth = g.getFontMetrics().stringWidth(instruction);
        g.drawString(instruction, (getWidth() / 2) - (instrWidth / 2), getHeight() / 2 + 30);
    }
    
    private void drawGameOverScreen(Graphics2D g) {
        g.setColor(new Color(150, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        String msg = "GAME OVER";
        int msgWidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (getWidth() / 2) - (msgWidth / 2), getHeight() / 2);
    }
    
    public void moveMenuSelection(int direction) {
        selectedMenuOption += direction;
        if (selectedMenuOption < 0) {
            selectedMenuOption = menuOptions.length - 1;
        } else if (selectedMenuOption >= menuOptions.length) {
            selectedMenuOption = 0;
        }
    }

    public void selectMenuOption() {
        System.out.println("Menü seçimi: " + selectedMenuOption + " (" + menuOptions[selectedMenuOption] + ")");
        switch (selectedMenuOption) {
            case 0:
                this.gameMode = GameMode.ONE_PLAYER;
                GameMode.setCurrentMode(GameMode.ONE_PLAYER);
                System.out.println("Tek oyuncu modu seçildi!");
                setGameState(GameState.STAGE_SELECT);
                break;
            case 1:
                this.gameMode = GameMode.TWO_PLAYER;
                GameMode.setCurrentMode(GameMode.TWO_PLAYER);
                System.out.println("İki oyuncu modu seçildi!");
                setGameState(GameState.STAGE_SELECT);
                break;
            case 2:
                if (saveFileExists) {
                    loadGame();
                }
                break;
        }
    }
    
    public void moveStageSelection(int direction) {
        selectedStageOption += direction;
        if (selectedStageOption < 0) {
            selectedStageOption = 13;
        } else if (selectedStageOption > 13) {
            selectedStageOption = 0;
        }
    }

    public void confirmStageSelection() {
        System.out.println("Bölüm " + (char)('A' + selectedStageOption) + " seçildi. Oyun başlıyor...");
        level = 1;
        setupGame();
        setGameState(GameState.PLAYING);
    }
    
    public GameState getGameState() { return gameState; }
    
    public void setGameState(GameState gameState) {
        if (this.gameState == GameState.PLAYING && gameState == GameState.PAUSED) {
            saveGame();
        }
        this.gameState = gameState;
    }

    private void checkSaveFile() {
        File saveFile = new File("savegame.dat");
        this.saveFileExists = saveFile.exists();
    }

    private void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("savegame.dat"))) {
            GameStateData data = new GameStateData();
            data.level = this.level;
            data.enemiesSpawned = this.enemiesSpawned;
            data.gameMode = this.gameMode;
            data.map = this.map;
            data.player1 = this.player1;
            data.player2 = this.player2;
            data.enemies = this.enemies;
            data.bullets = this.bullets;
            data.powerUps = this.powerUps;
            data.explosions = this.explosions;
            data.destroyedEnemyCounts = this.destroyedEnemyCounts;
            
            oos.writeObject(data);
            System.out.println("Oyun kaydedildi!");
            checkSaveFile();
        } catch (IOException e) {
            System.err.println("Oyun kaydedilirken hata oluştu!");
            e.printStackTrace();
        }
    }

    private void loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("savegame.dat"))) {
            GameStateData data = (GameStateData) ois.readObject();

            this.level = data.level;
            this.enemiesSpawned = data.enemiesSpawned;
            this.gameMode = data.gameMode;
            this.map = data.map;
            this.player1 = data.player1;
            this.player2 = data.player2;
            this.enemies = data.enemies;
            this.bullets = data.bullets;
            this.powerUps = data.powerUps;
            this.explosions = data.explosions != null ? data.explosions : new ArrayList<>();
            this.destroyedEnemyCounts = data.destroyedEnemyCounts;
            
            this.collisionChecker = new CollisionChecker(this);
            
            if (player1 != null) {
                player1.revive(this);
                player1.start();
            }
            if (player2 != null) {
                player2.revive(this);
                player2.start();
            }
            for(EnemyTank enemy : enemies) {
                enemy.revive(this);
                enemy.start();
            }
            for(Bullet bullet : bullets) {
                Tank owner = null;
                if (bullet.getOwnerType() == BulletOwner.PLAYER) {
                    if (player1 != null && bullet.getOwnerTank().getX() == player1.getX() && bullet.getOwnerTank().getY() == player1.getY()) owner = player1;
                    else if (player2 != null && bullet.getOwnerTank().getX() == player2.getX() && bullet.getOwnerTank().getY() == player2.getY()) owner = player2;
                } else {
                    for(EnemyTank newEnemy : this.enemies) {
                        if (newEnemy.getX() == bullet.getOwnerTank().getX() && newEnemy.getY() == bullet.getOwnerTank().getY()) {
                           owner = newEnemy;
                           break;
                        }
                    }
                }
                bullet.reattach(this, owner);
            }
            
            this.panelWidth = this.map[0].length * Tile.TILE_SIZE;
            this.panelHeight = this.map.length * Tile.TILE_SIZE;
            this.setPreferredSize(new Dimension(this.panelWidth, this.panelHeight));
        
            if (getTopLevelAncestor() != null) {
                ((JFrame) getTopLevelAncestor()).pack();
            }
            
            setGameState(GameState.PLAYING);
            System.out.println("Oyun yüklendi!");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Oyun yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
    
    public PlayerTank getPlayer1() { return player1; }
    public PlayerTank getPlayer2() { return player2; }
    public int getPanelWidth() { return panelWidth; }
    public int getPanelHeight() { return panelHeight; }
    public Tile[][] getMap() { return map; }
    public CollisionChecker getCollisionChecker() { return collisionChecker; }
    public synchronized void addBullet(Bullet b) { 
        synchronized(bullets) {
            this.bullets.add(b); 
        }
    }
    public synchronized void addExplosion(int x, int y) { 
        synchronized(explosions) {
            this.explosions.add(new Explosion(x, y)); 
        }
    }
    
    // ScoreSystem erişim metotları
    public ScoreSystem getScoreSystem() { return scoreSystem; }
    public void addTankKillScore(int playerId, EnemyType enemyType) {
        scoreSystem.addTankKillScore(playerId, enemyType);
    }
    public void addPowerUpScore(int playerId) {
        scoreSystem.addPowerUpScore(playerId);
    }
    
    // Pause/Resume metotları
    public boolean isPaused() { return isPaused; }
    public void togglePause() { 
        isPaused = !isPaused; 
        System.out.println("Oyun " + (isPaused ? "duraklatıldı" : "devam ediyor"));
    }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    
    public TileType getTileTypeAt(int x, int y) {
        if (map == null) { return TileType.EMPTY; }
        int col = (x + Tile.TILE_SIZE / 2) / Tile.TILE_SIZE;
        int row = (y + Tile.TILE_SIZE / 2) / Tile.TILE_SIZE;
        if (row >= 0 && row < map.length && col >= 0 && col < map[0].length) {
            return map[row][col].getType();
        }
        return TileType.EMPTY;
    }

    // Eski sistemdeki gibi: Kırmızı tank vurulunca rastgele bir yerde power-up çıkar
    public void spawnRandomPowerUp() {
        if (map == null) return;
        int maxAttempts = 100;
        int attempt = 0;
        int row = 0, col = 0;
        do {
            row = random.nextInt(map.length);
            col = random.nextInt(map[0].length);
            attempt++;
        } while (attempt < maxAttempts && (map[row][col].getType() != TileType.EMPTY || isTankOrPlayerAt(col * Tile.TILE_SIZE, row * Tile.TILE_SIZE)));

        if (map[row][col].getType() == TileType.EMPTY) {
            PowerUpType[] types = PowerUpType.values();
            PowerUpType type = types[random.nextInt(types.length)];
            PowerUp powerUp = new PowerUp(col * Tile.TILE_SIZE, row * Tile.TILE_SIZE, type);
            powerUps.add(powerUp);
            System.out.println("Power-up spawn: (" + col + "," + row + ") Type: " + type);
        }
    }

    // O tile'da tank veya oyuncu var mı?
    private boolean isTankOrPlayerAt(int x, int y) {
        Rectangle rect = new Rectangle(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
        if (player1 != null && player1.isAlive() && rect.intersects(player1.getBounds())) return true;
        if (player2 != null && player2.isAlive() && rect.intersects(player2.getBounds())) return true;
        synchronized(enemies) {
            for (EnemyTank enemy : enemies) {
                if (enemy.isAlive() && rect.intersects(enemy.getBounds())) return true;
            }
        }
        return false;
    }
}