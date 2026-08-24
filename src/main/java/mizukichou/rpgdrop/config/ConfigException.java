package mizukichou.rpgdrop.config;

/**
 * 配置解析异常：携带具体原因，由上层统一记录 SEVERE 日志并跳过该规则。
 */
public final class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
    }
}
