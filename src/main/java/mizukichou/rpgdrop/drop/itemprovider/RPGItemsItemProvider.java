package mizukichou.rpgdrop.drop.itemprovider;

import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.ItemType;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.hook.RPGItemsHook;
import org.bukkit.inventory.ItemStack;

/**
 * RPGItems Provider：把 RPGItem ID 交给 {@link RPGItemsHook}，
 * 通过 RPGItems 官方 API 生成真实 ItemStack（不伪造 NBT/PDC/Lore）。
 */
public final class RPGItemsItemProvider implements ItemProvider {

    private final RPGItemsHook hook;

    public RPGItemsItemProvider(RPGItemsHook hook) {
        this.hook = hook;
    }

    @Override
    public ItemType type() {
        return ItemType.RPGITEM;
    }

    @Override
    public ItemStack create(DropItem item) {
        if (!(item instanceof RPGItemDropItem rpgItem)) {
            throw new IllegalArgumentException("RPGItemsItemProvider can only handle RPGItemDropItem");
        }
        return hook.createItemStack(rpgItem.rpgItemId())
                .orElseThrow(() -> new IllegalStateException("RPGItem '" + rpgItem.rpgItemId() + "' does not exist or failed to generate"));
    }
}
