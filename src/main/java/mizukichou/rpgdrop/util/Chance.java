package mizukichou.rpgdrop.util;

/**
 * 概率校验（单一来源）。
 *
 * 合法范围：有限数值且 0 <= value <= 100。
 * 概率单位：0.01 = 0.01%，100 = 100%。
 * NaN / Infinity / 负数 / 超 100 一律非法（防止把规则概率写坏）。
 */
public final class Chance {

    private Chance() {
    }

    /** 概率是否合法。 */
    public static boolean isValid(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 100.0;
    }
}
