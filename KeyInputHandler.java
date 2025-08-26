import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyInputHandler extends KeyAdapter {
    private GamePanel gamePanel;

    public KeyInputHandler(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        GameState currentState = gamePanel.getGameState();

        if (currentState == GameState.START_SCREEN) {
            if (key == KeyEvent.VK_ENTER) {
                gamePanel.setGameState(GameState.MENU);
            }
        } else if (currentState == GameState.MENU) {
            handleMenuInput(key);
        } else if (currentState == GameState.STAGE_SELECT) {
            handleStageSelectInput(key);
        } else if (currentState == GameState.PLAYING) {
            handlePlayingInput(key);
        } else if (currentState == GameState.PAUSED) {
            if (key == KeyEvent.VK_P) {
                gamePanel.togglePause();
                gamePanel.setGameState(GameState.PLAYING);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gamePanel.getGameState() == GameState.PLAYING) {
            int key = e.getKeyCode();
            PlayerTank player1 = gamePanel.getPlayer1();
            PlayerTank player2 = gamePanel.getPlayer2();

            if (player1 != null) {
                if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN || 
                    key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    player1.setMoving(false);
                }
            }
            
            if (player2 != null) {
                 if (key == KeyEvent.VK_W || key == KeyEvent.VK_S || 
                    key == KeyEvent.VK_A || key == KeyEvent.VK_D) {
                    player2.setMoving(false);
                }
            }
        }
    }

    private void handleMenuInput(int key) {
        if (key == KeyEvent.VK_UP) {
            gamePanel.moveMenuSelection(-1);
        } else if (key == KeyEvent.VK_DOWN) {
            gamePanel.moveMenuSelection(1);
        } else if (key == KeyEvent.VK_ENTER) {
            gamePanel.selectMenuOption();
        }
    }

    private void handleStageSelectInput(int key) {
        if (key == KeyEvent.VK_UP) {
            gamePanel.moveStageSelection(-1);
        } else if (key == KeyEvent.VK_DOWN) {
            gamePanel.moveStageSelection(1);
        } else if (key == KeyEvent.VK_ENTER) {
            gamePanel.confirmStageSelection();
        }
    }

    private void handlePlayingInput(int key) {
        PlayerTank player1 = gamePanel.getPlayer1();
        PlayerTank player2 = gamePanel.getPlayer2();

        if (player1 != null) {
            switch (key) {
                case KeyEvent.VK_UP: player1.setDirection(Direction.UP); player1.setMoving(true); break;
                case KeyEvent.VK_DOWN: player1.setDirection(Direction.DOWN); player1.setMoving(true); break;
                case KeyEvent.VK_LEFT: player1.setDirection(Direction.LEFT); player1.setMoving(true); break;
                case KeyEvent.VK_RIGHT: player1.setDirection(Direction.RIGHT); player1.setMoving(true); break;
                case KeyEvent.VK_Z: player1.fire(); break;
            }
        }

        if (player2 != null) {
            switch (key) {
                case KeyEvent.VK_W: player2.setDirection(Direction.UP); player2.setMoving(true); break;
                case KeyEvent.VK_S: player2.setDirection(Direction.DOWN); player2.setMoving(true); break;
                case KeyEvent.VK_A: player2.setDirection(Direction.LEFT); player2.setMoving(true); break;
                case KeyEvent.VK_D: player2.setDirection(Direction.RIGHT); player2.setMoving(true); break;
                case KeyEvent.VK_SPACE: player2.fire(); break;
            }
        }
        
        if (key == KeyEvent.VK_P) {
            gamePanel.togglePause();
            if (gamePanel.isPaused()) {
                gamePanel.setGameState(GameState.PAUSED);
            } else {
                gamePanel.setGameState(GameState.PLAYING);
            }
        }
    }
}