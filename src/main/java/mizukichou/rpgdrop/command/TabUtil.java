package mizukichou.rpgdrop.command;

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
