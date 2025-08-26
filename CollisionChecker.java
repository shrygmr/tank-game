import java.awt.Rectangle;
import java.util.List;

public class CollisionChecker {
    private GamePanel gamePanel;
    
    public CollisionChecker(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public boolean canTankMove(Tank tank, int nextX, int nextY) {
        Rectangle futureBounds;
        Direction direction = tank.getDirection();
        int margin = 2; // Margin'i biraz azalttım

        if (direction == Direction.UP || direction == Direction.DOWN) {
            futureBounds = new Rectangle(nextX + margin, nextY, tank.width - 2 * margin, tank.height);
        } else {
            futureBounds = new Rectangle(nextX, nextY + margin, tank.width, tank.height - 2 * margin);
        }

        // Panel sınırlarını kontrol et
        if (nextX < 0 || nextX + tank.width > gamePanel.getPanelWidth() || 
            nextY < 0 || nextY + tank.height > gamePanel.getPanelHeight()) {
            return false;
        }

        // Harita tile'larını kontrol et
        Tile[][] map = gamePanel.getMap();
        if (map == null) return true; // Güvenlik kontrolü
        
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                Tile tile = map[row][col];
                
                if (tile.getType() == TileType.BRICK || tile.getType() == TileType.STEEL || 
                    tile.getType() == TileType.EAGLE || 
                    (tile.getType() == TileType.WATER && !tank.canMoveOnWater())) {
                    
                    // BRICK türü için detaylı parça kontrolü
                    if (tile.getType() == TileType.BRICK) {
                        // Brick tile'ı tamamen yok olmamışsa ve çarpışma varsa
                        if (!tile.isBrickDestroyed() && futureBounds.intersects(tile.getBounds())) {
                            // Detaylı brick parça kontrolü yap
                            if (tile.hasBrickPartsInBounds(futureBounds)) {
                                return false;
                            }
                        }
                    } else {
                        // Diğer tile türleri için normal collision
                        if (futureBounds.intersects(tile.getBounds())) {
                            return false;
                        }
                    }
                }
            }
        }
        
        // Tank-tank çarpışması kontrolü ekle
        return !checkTankToTankCollision(tank, futureBounds);
    }
    
    // Tank-tank çarpışması kontrolü için yeni metod
    private boolean checkTankToTankCollision(Tank movingTank, Rectangle futureBounds) {
        // Player 1 ile çarpışma kontrolü
        PlayerTank player1 = gamePanel.getPlayer1();
        if (player1 != null && player1 != movingTank && player1.isAlive()) {
            if (futureBounds.intersects(player1.getBounds())) {
                return true; // Çarpışma var
            }
        }
        
        // Player 2 ile çarpışma kontrolü
        PlayerTank player2 = gamePanel.getPlayer2();
        if (player2 != null && player2 != movingTank && player2.isAlive()) {
            if (futureBounds.intersects(player2.getBounds())) {
                return true; // Çarpışma var
            }
        }
        
        // Enemy tanklar ile çarpışma kontrolü
        synchronized(gamePanel.enemies) {
            for (EnemyTank enemy : gamePanel.enemies) {
                if (enemy != movingTank && enemy.isAlive()) {
                    if (futureBounds.intersects(enemy.getBounds())) {
                        return true; // Çarpışma var
                    }
                }
            }
        }
        
        return false; // Çarpışma yok
    }

    public void checkBulletToTile(Bullet bullet) {
        Tile[][] map = gamePanel.getMap();
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                Tile tile = map[row][col];
                if (bullet.getBounds().intersects(tile.getBounds())) {
                    switch (tile.getType()) {
                        case BRICK:
                            // Bullet'ın merkez koordinatlarını hesapla
                            int hitX = bullet.getX() + bullet.width / 2;
                            int hitY = bullet.getY() + bullet.height / 2;
                            
                            // Tile içindeki local koordinata çevir
                            int localX = hitX - tile.getX();
                            int localY = hitY - tile.getY();
                            
                            System.out.println("BRICK HIT: Bullet center=(" + hitX + "," + hitY + 
                                             "), Tile bounds=(" + tile.getX() + "," + tile.getY() + 
                                             "), Local=(" + localX + "," + localY + ")");
                            
                            bullet.destroy();
                            tile.hitBrick(localX, localY, bullet.getDirection()); // Yönü de gönder
                            gamePanel.addExplosion(bullet.getX(), bullet.getY()); // Patlama efekti ekle
                            break;
                        case STEEL:
                            if (bullet.getOwnerTank().getStarLevel() >= 3) {
                                tile.setType(TileType.EMPTY);
                            }
                            bullet.destroy();
                            gamePanel.addExplosion(bullet.getX(), bullet.getY()); // Patlama efekti ekle
                            break;
                        case WATER:
                        case ICE:
                        case TREES:
                        case EMPTY:
                            break;
                        case EAGLE:
                            bullet.destroy();
                            gamePanel.addExplosion(bullet.getX(), bullet.getY()); // Patlama efekti ekle
                            gamePanel.setGameState(GameState.GAME_OVER);
                            break;
                    }
                    if (!bullet.isAlive()) return;
                }
            }
        }
    }

    public void checkBulletToTank(Bullet bullet, Tank tank) {
        if (bullet.getBounds().intersects(tank.getBounds())) {
            if (bullet.getOwnerTank() == tank) {
                return;
            }
            
            // Tank öldürülürse puan ver
            if (tank instanceof EnemyTank) {
                EnemyTank enemy = (EnemyTank) tank;
                // Oyuncu tankının mermisi düşman tankı öldürdüyse puan ver
                if (bullet.getOwnerTank() instanceof PlayerTank) {
                    PlayerTank player = (PlayerTank) bullet.getOwnerTank();
                    int playerId = (player == gamePanel.getPlayer1()) ? 1 : 2;
                    
                    // Tank ölecekse puan ver (son hasarsa)
                    if (enemy.getHealth() <= 1) {
                        gamePanel.addTankKillScore(playerId, enemy.getEnemyType());
                        System.out.println("Oyuncu " + playerId + " bir " + enemy.getEnemyType() + " tank öldürdü!");
                    }
                }
            }
            
            tank.takeDamage(1);
            bullet.destroy();
            gamePanel.addExplosion(bullet.getX(), bullet.getY()); // Tank çarpışmasında patlama efekti
        }
    }

    public void checkBulletToBulletCollision(List<Bullet> bullets) {
        for (int i = 0; i < bullets.size(); i++) {
            for (int j = i + 1; j < bullets.size(); j++) {
                Bullet b1 = bullets.get(i);
                Bullet b2 = bullets.get(j);

                if (b1.getOwnerType() != b2.getOwnerType()) {
                    if (b1.getBounds().intersects(b2.getBounds())) {
                        // İki mermi arasındaki orta noktada patlama efekti
                        int explosionX = (b1.getX() + b2.getX()) / 2;
                        int explosionY = (b1.getY() + b2.getY()) / 2;
                        gamePanel.addExplosion(explosionX, explosionY);
                        
                        b1.destroy();
                        b2.destroy();
                    }
                }
            }
        }
    }
}