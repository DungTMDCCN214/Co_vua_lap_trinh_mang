package protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Logic cờ vua dùng chung cho Server (kiểm tra hợp lệ - authoritative) và Client (tô sáng nước đi).
 * Bàn cờ g[hàng][cột]; chữ HOA = quân Trắng, chữ thường = quân Đen, '.' = ô trống.
 * Trắng ở phía dưới (hàng 6-7), đi theo hướng hàng giảm dần.
 * Đã hỗ trợ: luật đi từng quân, nhập thành, bắt tốt qua đường, không cho để Vua bị chiếu,
 * phong Hậu tự động, chiếu hết / hòa cờ (hết nước đi).
 */
public class ChessBoard {
    private final char[][] g = new char[8][8];
    private boolean whiteTurn = true;
    private boolean whiteKingSideCastle, whiteQueenSideCastle;
    private boolean blackKingSideCastle, blackQueenSideCastle;
    private int enPassantColumn, enPassantRow;

    public ChessBoard() { reset(); }

    public void reset() {
        String back = "rnbqkbnr";
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) g[r][c] = '.';
            g[0][c] = back.charAt(c);
            g[1][c] = 'p';
            g[6][c] = 'P';
            g[7][c] = Character.toUpperCase(back.charAt(c));
        }
        whiteTurn = true;
        whiteKingSideCastle = whiteQueenSideCastle = true;
        blackKingSideCastle = blackQueenSideCastle = true;
        enPassantColumn = enPassantRow = -1;
    }

    public char get(int c, int r) { return g[r][c]; }
    public boolean isWhiteTurn() { return whiteTurn; }

    private static boolean inside(int c, int r) { return c >= 0 && c < 8 && r >= 0 && r < 8; }
    private static boolean isWhite(char p) { return Character.isUpperCase(p); }

    /** Nước đi hợp lệ hoàn toàn: đúng lượt, đúng luật quân, và không để Vua mình bị chiếu. */
    public boolean isLegalMove(int fc, int fr, int tc, int tr) {
        if (!inside(fc, fr) || !inside(tc, tr)) return false;
        char p = g[fr][fc];
        if (p == '.' || isWhite(p) != whiteTurn) return false;
        boolean castling = isCastlingMove(fc, fr, tc, tr);
        if (castling ? !canCastle(fc, fr, tc) : !pseudoLegal(fc, fr, tc, tr)) return false;
        // Thử đi rồi kiểm tra Vua có bị chiếu không, sau đó hoàn tác
        char captured = g[tr][tc];
        boolean enPassant = isEnPassantMove(fc, fr, tc, tr);
        int capturedPawnRow = tr - (isWhite(p) ? -1 : 1);
        if (enPassant) g[capturedPawnRow][tc] = '.';
        g[tr][tc] = p; g[fr][fc] = '.';
        int rookFrom = -1, rookTo = -1;
        if (castling) {
            rookFrom = tc > fc ? 7 : 0;
            rookTo = tc > fc ? tc - 1 : tc + 1;
            g[fr][rookTo] = g[fr][rookFrom];
            g[fr][rookFrom] = '.';
        }
        boolean safe = !inCheck(whiteTurn);
        if (castling) {
            g[fr][rookFrom] = g[fr][rookTo];
            g[fr][rookTo] = '.';
        }
        g[fr][fc] = p; g[tr][tc] = captured;
        if (enPassant) g[capturedPawnRow][tc] = isWhite(p) ? 'p' : 'P';
        return safe;
    }

    /** Thực hiện nước đi nếu hợp lệ, đổi lượt. Trả về false nếu không hợp lệ. */
    public boolean move(int fc, int fr, int tc, int tr) {
        if (!isLegalMove(fc, fr, tc, tr)) return false;
        char p = g[fr][fc];
        char captured = g[tr][tc];
        boolean enPassant = isEnPassantMove(fc, fr, tc, tr);
        boolean castling = isCastlingMove(fc, fr, tc, tr);
        updateCastlingRights(p, fc, fr, captured, tc, tr);
        if (enPassant) g[tr - (isWhite(p) ? -1 : 1)][tc] = '.';
        g[fr][fc] = '.';
        if (castling) {
            int rookFrom = tc > fc ? 7 : 0;
            int rookTo = tc > fc ? tc - 1 : tc + 1;
            g[fr][rookTo] = g[fr][rookFrom];
            g[fr][rookFrom] = '.';
        }
        if (p == 'P' && tr == 0) p = 'Q';      // phong Hậu tự động
        if (p == 'p' && tr == 7) p = 'q';
        g[tr][tc] = p;
        enPassantColumn = enPassantRow = -1;
        if (Character.toLowerCase(p) == 'p' && Math.abs(tr - fr) == 2) {
            enPassantColumn = fc;
            enPassantRow = (fr + tr) / 2;
        }
        whiteTurn = !whiteTurn;
        return true;
    }

    /** Các ô đích hợp lệ của quân tại (c,r) - dùng để tô sáng phía Client. */
    public List<int[]> legalTargets(int c, int r) {
        List<int[]> res = new ArrayList<>();
        for (int tc = 0; tc < 8; tc++)
            for (int tr = 0; tr < 8; tr++)
                if (isLegalMove(c, r, tc, tr)) res.add(new int[]{tc, tr});
        return res;
    }

    /** Bên đang tới lượt còn nước đi hợp lệ nào không? */
    public boolean hasAnyLegalMove() {
        for (int c = 0; c < 8; c++)
            for (int r = 0; r < 8; r++) {
                char p = g[r][c];
                if (p != '.' && isWhite(p) == whiteTurn && !legalTargets(c, r).isEmpty()) return true;
            }
        return false;
    }

    /** Vua của bên 'white' có đang bị chiếu không? */
    public boolean inCheck(boolean white) {
        char king = white ? 'K' : 'k';
        for (int c = 0; c < 8; c++)
            for (int r = 0; r < 8; r++)
                if (g[r][c] == king) return attacked(c, r, !white);
        return false;
    }

    /** Ô (c,r) có bị quân của bên byWhite tấn công không? */
    private boolean attacked(int c, int r, boolean byWhite) {
        for (int fc = 0; fc < 8; fc++)
            for (int fr = 0; fr < 8; fr++) {
                char p = g[fr][fc];
                if (p == '.' || isWhite(p) != byWhite) continue;
                if (Character.toLowerCase(p) == 'p') {   // Tốt chỉ ăn chéo
                    int dir = byWhite ? -1 : 1;
                    if (r - fr == dir && Math.abs(c - fc) == 1) return true;
                } else if (pseudoLegal(fc, fr, c, r)) return true;
            }
        return false;
    }

    /** Luật di chuyển cơ bản của từng quân (chưa xét Vua có bị chiếu hay không). */
    private boolean pseudoLegal(int fc, int fr, int tc, int tr) {
        if (fc == tc && fr == tr) return false;
        char p = g[fr][fc], t = g[tr][tc];
        if (t != '.' && isWhite(t) == isWhite(p)) return false; // không ăn quân mình
        int dc = tc - fc, dr = tr - fr, adc = Math.abs(dc), adr = Math.abs(dr);
        switch (Character.toLowerCase(p)) {
            case 'p': {
                int dir = isWhite(p) ? -1 : 1, start = isWhite(p) ? 6 : 1;
                if (dc == 0 && dr == dir) return t == '.';
                if (dc == 0 && dr == 2 * dir && fr == start) return t == '.' && g[fr + dir][fc] == '.';
                return adc == 1 && dr == dir && (t != '.' || isEnPassantMove(fc, fr, tc, tr));
            }
            case 'n': return adc * adr == 2;
            case 'b': return adc == adr && clear(fc, fr, tc, tr);
            case 'r': return (dc == 0 || dr == 0) && clear(fc, fr, tc, tr);
            case 'q': return (adc == adr || dc == 0 || dr == 0) && clear(fc, fr, tc, tr);
            case 'k': return adc <= 1 && adr <= 1;
            default: return false;
        }
    }

    private boolean isCastlingMove(int fc, int fr, int tc, int tr) {
        return g[fr][fc] != '.' && Character.toLowerCase(g[fr][fc]) == 'k'
                && fr == tr && Math.abs(tc - fc) == 2;
    }

    private boolean canCastle(int fc, int fr, int tc) {
        char king = g[fr][fc];
        boolean white = isWhite(king);
        int homeRow = white ? 7 : 0;
        boolean kingSide = tc > fc;
        int rookColumn = kingSide ? 7 : 0;
        if (fc != 4 || fr != homeRow || tc != (kingSide ? 6 : 2)) return false;
        if (white ? (kingSide ? !whiteKingSideCastle : !whiteQueenSideCastle)
                : (kingSide ? !blackKingSideCastle : !blackQueenSideCastle)) return false;
        if (g[homeRow][rookColumn] != (white ? 'R' : 'r')) return false;

        int step = kingSide ? 1 : -1;
        for (int c = fc + step; c != rookColumn; c += step)
            if (g[homeRow][c] != '.') return false;
        return !inCheck(white) && !attacked(fc + step, homeRow, !white)
                && !attacked(tc, homeRow, !white);
    }

    private boolean isEnPassantMove(int fc, int fr, int tc, int tr) {
        char pawn = g[fr][fc];
        if (pawn != 'P' && pawn != 'p') return false;
        int direction = isWhite(pawn) ? -1 : 1;
        if (tc != enPassantColumn || tr != enPassantRow || g[tr][tc] != '.'
                || tr - fr != direction || Math.abs(tc - fc) != 1) return false;
        return g[fr][tc] == (isWhite(pawn) ? 'p' : 'P');
    }

    private void updateCastlingRights(char piece, int fc, int fr, char captured, int tc, int tr) {
        if (piece == 'K') whiteKingSideCastle = whiteQueenSideCastle = false;
        if (piece == 'k') blackKingSideCastle = blackQueenSideCastle = false;
        if (piece == 'R' && fr == 7 && fc == 0) whiteQueenSideCastle = false;
        if (piece == 'R' && fr == 7 && fc == 7) whiteKingSideCastle = false;
        if (piece == 'r' && fr == 0 && fc == 0) blackQueenSideCastle = false;
        if (piece == 'r' && fr == 0 && fc == 7) blackKingSideCastle = false;
        if (captured == 'R' && tr == 7 && tc == 0) whiteQueenSideCastle = false;
        if (captured == 'R' && tr == 7 && tc == 7) whiteKingSideCastle = false;
        if (captured == 'r' && tr == 0 && tc == 0) blackQueenSideCastle = false;
        if (captured == 'r' && tr == 0 && tc == 7) blackKingSideCastle = false;
    }

    /** Đường đi từ (fc,fr) tới (tc,tr) có bị quân nào cản không (không tính ô đầu/cuối). */
    private boolean clear(int fc, int fr, int tc, int tr) {
        int sc = Integer.signum(tc - fc), sr = Integer.signum(tr - fr);
        int c = fc + sc, r = fr + sr;
        while (c != tc || r != tr) {
            if (g[r][c] != '.') return false;
            c += sc; r += sr;
        }
        return true;
    }
}