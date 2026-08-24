package mizukichou.rpgdrop.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Locale;
import java.util.Optional;

/**
 * 原版物品材料解析工具。
 *
 * 支持两种写法：DIAMOND 或 minecraft:diamond。
 * 使用 Registry API 解析（新版 Paper 推荐，避免使用已弃用的 Material.matchMaterial）。
 * 注意：Registry.MATERIAL 需要 Paper 1.21.2+（api-version 1.21.2 起），本插件目标为 26.2，无此顾虑。
 */
public final class Materials {

    private Materials() {
    }

    public static Optional<Material> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            String input = raw.trim();
            NamespacedKey key = input.indexOf(':') >= 0
                    ? NamespacedKey.fromString(input)
                    : NamespacedKey.minecraft(input.toLowerCase(Locale.ROOT));
            if (key == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(Registry.MATERIAL.get(key));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
