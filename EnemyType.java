public enum EnemyType {
    BASIC(100),
    FAST(200),
    POWER(300),
    ARMOR(400);

    public final int points;

    EnemyType(int points) {
        this.points = points;
    }
}