package mizukichou.rpgdrop.util;

/**
 * 规则 ID 的【唯一】校验入口：命令、GUI、配置文件加载全部走这里，
 * 保证三处约束永远一致。
 */
public final class RuleIds {

    /** 合法 ID：字母/数字/下划线/连字符，1~32 字符。 */
    public static final int MAX_LENGTH = 32;

    private RuleIds() {
    }

    public static boolean isValid(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{1," + MAX_LENGTH + "}");
    }
}
