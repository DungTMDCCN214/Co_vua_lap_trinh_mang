package server;

/**
 * Công thức Elo.
 *   Điểm kỳ vọng của A:  E = 1 / (1 + 10^((Rb - Ra) / 400))
 *   Điểm mới:            R' = R + K * (S - E)      (S = 1 thắng, 0.5 hòa, 0 thua)
 * Thắng người mạnh hơn được cộng nhiều, thắng người yếu hơn được cộng ít.
 */
public final class EloCalculator {
    private EloCalculator() {}

    public static final int DEFAULT_ELO = 1200;
    public static final int K = 32;

    public static double expected(int ra, int rb) {
        return 1.0 / (1.0 + Math.pow(10.0, (rb - ra) / 400.0));
    }

    /** Độ thay đổi điểm của Trắng. Đen thay đổi đúng bằng giá trị ngược dấu (tổng điểm được bảo toàn). */
    public static int deltaForWhite(int whiteElo, int blackElo, double whiteScore) {
        return (int) Math.round(K * (whiteScore - expected(whiteElo, blackElo)));
    }
}
