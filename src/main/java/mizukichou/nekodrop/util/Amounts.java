package mizukichou.nekodrop.util;

/**
 * 掉落数量校验（单一来源）。
 *
 * 合法范围：min >= 1 且 max >= min。
 */
public final class Amounts {

    private Amounts() {
    }

    /** 数量范围是否合法。 */
    public static boolean isValid(int min, int max) {
        return min >= 1 && max >= min;
    }
}
