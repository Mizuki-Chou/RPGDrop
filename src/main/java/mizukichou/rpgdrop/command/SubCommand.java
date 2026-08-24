package mizukichou.rpgdrop.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 子命令接口。每个子命令一个独立类，禁止把所有逻辑塞进主命令类。
 *
 * 文本国际化：
 *   - {@link #descriptionKey()} 为描述文案的语言键；
 *   - {@link #usageKey()} 存在时用法文本从语言文件读取，否则使用 {@link #usage()} 的纯英文语法。
 */
public interface SubCommand {

    String name();

    /** 用法文本语言键；无翻译需求（纯命令语法）的子命令返回 null。 */
    default String usageKey() {
        return null;
    }

    /** 纯英文用法文本（兜底，用于 help 输出）。 */
    default String usage() {
        return "/rdrop " + name();
    }

    /** 描述文案语言键。 */
    String descriptionKey();

    /** 额外权限节点；空字符串表示仅要求基础权限 rpgdrop.command。 */
    default String permission() {
        return "";
    }

    default int minArgs() {
        return 0;
    }

    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
