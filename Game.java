import javax.swing.JFrame;

public class Game {
    public static void main(String[] args) {
        JFrame window = new JFrame("Tank 1990");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}