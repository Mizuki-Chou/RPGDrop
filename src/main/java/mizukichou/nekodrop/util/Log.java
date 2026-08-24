package mizukichou.nekodrop.util;

import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 统一日志入口。
 *
 * 硬性原则：全项目禁止使用 printStackTrace()。
 * 错误一律通过 {@link #severe(String, Throwable)} 交给服务端日志系统，
 * 附带完整堆栈信息，方便排查。
 */
public final class Log {

    private final Logger logger;
    private final BooleanSupplier debugEnabled;

    public Log(Logger logger, BooleanSupplier debugEnabled) {
        this.logger = logger;
        this.debugEnabled = debugEnabled;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warning(message);
    }

    /** 记录严重错误（含完整堆栈），禁止 printStackTrace()。 */
    public void severe(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    /** 仅当 settings.debug 开启时输出。 */
    public void debug(String message) {
        if (debugEnabled.getAsBoolean()) {
            logger.info("[Debug] " + message);
        }
    }
}
