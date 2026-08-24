package mizukichou.rpgdrop.util;

/**
 * 掉落数量校验（单一来源）。
 *
 * 合法范围：min >= 1 且 max >= min 且 max <= {@link #MAX_AMOUNT}。
 * 上限保护：单次掉落数量过大会在主线程循环生成物品实体，直接卡死服务器。
 */
public final class Amounts {

    /** 单次掉落的最大数量（防呆：过大会卡死主线程）。 */
    public static final int MAX_AMOUNT = 1024;

    private Amounts() {
    }

    /** 数量范围是否合法。 */
    public static boolean isValid(int min, int max) {
        return min >= 1 && max >= min && max <= MAX_AMOUNT;
    }
}
