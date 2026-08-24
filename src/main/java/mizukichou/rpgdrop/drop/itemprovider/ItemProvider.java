package mizukichou.rpgdrop.drop.itemprovider;

import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.ItemType;
import org.bukkit.inventory.ItemStack;

/**
 * 物品提供者：负责把 {@link DropItem} 描述转换为真实 ItemStack。
 *
 * 架构预留：未来新增 ItemsAdder / MMOItems 等物品插件时，
 * 只需新增一个 ItemProvider 实现并注册，无需改动掉落主流程。
 */
public interface ItemProvider {

    /** 本 Provider 负责的掉落物类型。 */
    ItemType type();

    /** 生成真实、独立的 ItemStack；失败时抛出异常。 */
    ItemStack create(DropItem item);
}
