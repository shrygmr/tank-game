import java.awt.Rectangle;
import java.awt.Graphics;
import java.io.Serializable;

public abstract class GameObject implements Serializable {
    protected int x, y;
    protected int width, height;

    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(Graphics g);

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
}