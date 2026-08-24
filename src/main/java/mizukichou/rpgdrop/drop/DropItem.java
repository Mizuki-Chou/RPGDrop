package mizukichou.rpgdrop.drop;

import mizukichou.rpgdrop.drop.itemprovider.ItemProviderRegistry;
import org.bukkit.inventory.ItemStack;

/**
 * 掉落物描述（接口）。
 *
 * 硬性原则：只保存"描述"（原版材料名 / RPGItem ID），
 * 绝不保存 ItemStack 本体，也不伪造 NBT/PDC/Lore。
 * 真正生成 ItemStack 由 {@link ItemProviderRegistry} 中注册的 ItemProvider 完成。
 */
public sealed interface DropItem permits VanillaDropItem, RPGItemDropItem {

    /** 掉落物来源类型。 */
    ItemType type();

    /** 通过 ItemProvider 生成一个真实、独立、可用的 ItemStack。 */
    default ItemStack createItemStack(ItemProviderRegistry registry) {
        return registry.create(this);
    }
}
