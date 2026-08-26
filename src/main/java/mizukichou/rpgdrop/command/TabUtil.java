package mizukichou.rpgdrop.command;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * tab 补全小工具。
 */
public final class TabUtil {

    private TabUtil() {
    }

    /**
     * 物品类型补全项：只包含对应插件已安装的类型
     * （没装 RPGItems / NekoNYume 时，补全里不会出现 rpgitem / nyn）。
     */
    public static List<String> itemKinds(RPGDropPlugin plugin) {
        java.util.ArrayList<String> kinds = new java.util.ArrayList<>();
        kinds.add("vanilla");
        if (plugin.isRpgItemsAvailable()) {
            kinds.add("rpgitem");
        }
        if (plugin.isNekoNYumeAvailable()) {
            kinds.add("nyn");
        }
        return kinds;
    }

    /** 按前缀过滤候选词。 */
    public static List<String> filter(Collection<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .limit(60)
                .toList();
    }

    /** 全部规则 ID（tab 补全用）。 */
    public static List<String> ruleIds(DropManager dropManager) {
        return dropManager.getAllRules().stream().map(DropRule::id).toList();
    }
}
