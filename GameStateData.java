import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GameStateData implements Serializable {
    private static final long serialVersionUID = 1L;

    public int level;
    public int enemiesSpawned;
    public GameMode gameMode;
    public Tile[][] map;

    public PlayerTank player1;
    public PlayerTank player2;

    public List<EnemyTank> enemies;
    public List<Bullet> bullets;
    public List<PowerUp> powerUps;
    public List<Explosion> explosions; // Patlama efektleri için
    
    public Map<EnemyType, Integer> destroyedEnemyCounts;
}