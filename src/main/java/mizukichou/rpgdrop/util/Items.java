package mizukichou.rpgdrop.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 图标构建工具（GUI 用）。
 *
 * 颜色代码使用 & 符号；全部通过 Component API 设置（26.2 已弃用 String 版）。
 */
public final class Items {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Items() {
    }

    /** 构建带名字与 lore 的图标。 */
    public static ItemStack icon(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LEGACY.deserialize(name));
        if (lore != null && lore.length > 0) {
            meta.lore(Arrays.stream(lore).map(LEGACY::deserialize).toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** 给图标添加附魔光效（用于"已选中"状态），不改变物品属性。 */
    public static ItemStack glow(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        Enchantment mending = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft("mending"));
        if (mending != null) {
            meta.addEnchant(mending, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    /** 生物对应的刷怪蛋图标；找不到时返回兜底图标。 */
    public static ItemStack mobIcon(String entityTypeName) {
        Material egg = Materials.parse(entityTypeName + "_SPAWN_EGG").orElse(Material.BAT_SPAWN_EGG);
        return new ItemStack(egg);
    }
}
