package mizukichou.rpgdrop.util;

import mizukichou.rpgdrop.i18n.I18n;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * 消息发送工具（i18n key 驱动）。
 *
 * 所有消息文本都来自语言文件（plugins/RPGDrop/lang/*.yml），
 * 按发送者的客户端语言自动选择；控制台使用英语。
 * 语言文件中的 '&' 为颜色代码，{0} {1} 为占位符。
 */
public final class Msg {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static volatile I18n i18n;

    private Msg() {
    }

    /** 由插件在启用时注入（所有消息发送前必须完成）。 */
    public static void init(I18n instance) {
        i18n = instance;
    }

    /** 发送带前缀的消息。 */
    public static void send(CommandSender sender, String key, Object... args) {
        if (i18n == null) {
            sender.sendMessage("(RPGDrop not initialized) " + key);
            return;
        }
        String prefix = i18n.get(sender, "prefix");
        String body = i18n.get(sender, key, args);
        sender.sendMessage(LEGACY.deserialize(prefix + body));
    }

    /** 发送不带前缀的消息（如帮助页边框、详情行）。 */
    public static void sendRaw(CommandSender sender, String key, Object... args) {
        if (i18n == null) {
            sender.sendMessage(key);
            return;
        }
        sender.sendMessage(LEGACY.deserialize(i18n.get(sender, key, args)));
    }

    /** 逐行发送多行消息（不含前缀）。 */
    public static void sendList(CommandSender sender, String key, Object... args) {
        if (i18n == null) {
            return;
        }
        for (String line : i18n.getList(sender, key, args)) {
            sender.sendMessage(LEGACY.deserialize(line));
        }
    }

    /** 仅取翻译文本（供嵌套占位符等场景使用）。 */
    public static String tr(CommandSender sender, String key, Object... args) {
        return i18n == null ? key : i18n.get(sender, key, args);
    }
}
