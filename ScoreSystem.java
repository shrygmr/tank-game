import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ScoreSystem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Tank türlerine göre puanlar (TANK 1990 original)
    public static final int BASIC_TANK_POINTS = 100;
    public static final int FAST_TANK_POINTS = 200;
    public static final int POWER_TANK_POINTS = 300;
    public static final int ARMOR_TANK_POINTS = 400;
    
    // Power-up puanları
    public static final int POWERUP_POINTS = 500;
    
    // Bonus puanları
    public static final int STAGE_CLEAR_BONUS = 1000;
    public static final int EAGLE_PROTECTION_BONUS = 5000;
    
    private int player1Score = 0;
    private int player2Score = 0;
    private int currentStage = 1;
    
    // İstatistikler
    private Map<EnemyType, Integer> player1Kills = new HashMap<>();
    private Map<EnemyType, Integer> player2Kills = new HashMap<>();
    
    public ScoreSystem() {
        // Kill istatistiklerini sıfırla
        for (EnemyType type : EnemyType.values()) {
            player1Kills.put(type, 0);
            player2Kills.put(type, 0);
        }
    }
    
    // Tank öldürme puanı ekle
    public void addTankKillScore(int playerId, EnemyType enemyType) {
        int points = getTankPoints(enemyType);
        
        if (playerId == 1) {
            player1Score += points;
            player1Kills.put(enemyType, player1Kills.get(enemyType) + 1);
        } else if (playerId == 2) {
            player2Score += points;
            player2Kills.put(enemyType, player2Kills.get(enemyType) + 1);
        }
    }
    
    // Power-up alma puanı
    public void addPowerUpScore(int playerId) {
        if (playerId == 1) {
            player1Score += POWERUP_POINTS;
        } else if (playerId == 2) {
            player2Score += POWERUP_POINTS;
        }
    }
    
    // Tur bitirme bonusu
    public void addStageClearBonus(int playerId) {
        if (playerId == 1) {
            player1Score += STAGE_CLEAR_BONUS;
        } else if (playerId == 2) {
            player2Score += STAGE_CLEAR_BONUS;
        }
    }
    
    // Kartal koruma bonusu
    public void addEagleProtectionBonus() {
        player1Score += EAGLE_PROTECTION_BONUS;
        if (GameMode.getCurrentMode() == GameMode.TWO_PLAYER) {
            player2Score += EAGLE_PROTECTION_BONUS;
        }
    }
    
    // Tank türüne göre puan hesapla
    private int getTankPoints(EnemyType enemyType) {
        switch (enemyType) {
            case BASIC: return BASIC_TANK_POINTS;
            case FAST: return FAST_TANK_POINTS;
            case POWER: return POWER_TANK_POINTS;
            case ARMOR: return ARMOR_TANK_POINTS;
            default: return BASIC_TANK_POINTS;
        }
    }
    
    // Getters
    public int getPlayer1Score() { return player1Score; }
    public int getPlayer2Score() { return player2Score; }
    public int getCurrentStage() { return currentStage; }
    
    public Map<EnemyType, Integer> getPlayer1Kills() { return new HashMap<>(player1Kills); }
    public Map<EnemyType, Integer> getPlayer2Kills() { return new HashMap<>(player2Kills); }
    
    // Stage artır
    public void nextStage() {
        currentStage++;
    }
    
    // Oyunu resetle
    public void resetGame() {
        player1Score = 0;
        player2Score = 0;
        currentStage = 1;
        
        for (EnemyType type : EnemyType.values()) {
            player1Kills.put(type, 0);
            player2Kills.put(type, 0);
        }
    }
    
    // Yüksek skor kaydet
    public void saveHighScore() {
        try {
            File file = new File("highscore.dat");
            int currentHighScore = loadHighScore();
            int maxScore = Math.max(player1Score, player2Score);
            
            if (maxScore > currentHighScore) {
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeInt(maxScore);
                oos.close();
                fos.close();
                System.out.println("Yeni yüksek skor kaydedildi: " + maxScore);
            }
        } catch (IOException e) {
            System.err.println("Yüksek skor kaydedilemedi: " + e.getMessage());
        }
    }
    
    // Yüksek skor yükle
    public int loadHighScore() {
        try {
            File file = new File("highscore.dat");
            if (!file.exists()) return 0;
            
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            int highScore = ois.readInt();
            ois.close();
            fis.close();
            return highScore;
        } catch (IOException e) {
            System.err.println("Yüksek skor yüklenemedi: " + e.getMessage());
            return 0;
        }
    }
    
    // Tur sonu istatistik raporu
    public String getStageReport(int playerId) {
        Map<EnemyType, Integer> kills = (playerId == 1) ? player1Kills : player2Kills;
        int totalScore = (playerId == 1) ? player1Score : player2Score;
        
        StringBuilder report = new StringBuilder();
        report.append("=== OYUNCU ").append(playerId).append(" İSTATİSTİKLER ===\n");
        report.append("TUR: ").append(currentStage).append("\n");
        report.append("TOPLAM PUAN: ").append(totalScore).append("\n\n");
        
        report.append("TANK ÖLDÜRMELERİ:\n");
        for (EnemyType type : EnemyType.values()) {
            int killCount = kills.get(type);
            if (killCount > 0) {
                report.append(type.name()).append(" TANK: ").append(killCount)
                      .append(" x ").append(getTankPoints(type))
                      .append(" = ").append(killCount * getTankPoints(type)).append(" puan\n");
            }
        }
        
        return report.toString();
    }
    
    // Toplam puan
    public int getTotalScore() {
        return player1Score + player2Score;
    }
}
