package mizukichou.rpgdrop.util;

/**
 * 字符串安全工具：防止玩家可控文本污染日志（log forging）。
 */
public final class Strings {

    /** 世界名最大长度（玩家输入路径统一校验）。 */
    public static final int MAX_WORLD_NAME = 64;

    private Strings() {
    }

    /**
     * 把控制字符替换为可视形式，防止玩家输入伪造日志行。
     * 换行/回车/制表符转义显示，其余控制字符替换为 '?'。
     */
    public static String sanitizeLog(String raw) {
        if (raw == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20 || ch == 0x7f) {
                        sb.append('?');
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
