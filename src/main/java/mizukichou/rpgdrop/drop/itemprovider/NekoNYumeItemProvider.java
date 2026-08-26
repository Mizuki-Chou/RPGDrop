package mizukichou.rpgdrop.drop.itemprovider;

import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.ItemType;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.hook.NekoNYumeHook;
import org.bukkit.inventory.ItemStack;

/**
 * NekoNYume 物品提供者：全部逻辑委托给 {@link NekoNYumeHook}（唯一 API 触点）。
 */
public final class NekoNYumeItemProvider implements ItemProvider {

    private final NekoNYumeHook hook;

    public NekoNYumeItemProvider(NekoNYumeHook hook) {
        this.hook = hook;
    }

    @Override
    public ItemType type() {
        return ItemType.NEKONYUME;
    }

    @Override
    public ItemStack create(DropItem item) {
        if (!(item instanceof NekoNYumeDropItem nyn)) {
            throw new IllegalArgumentException("NekoNYumeItemProvider can only handle NekoNYumeDropItem");
        }
        return hook.createItemStack(nyn)
                .orElseThrow(() -> new IllegalStateException(
                        "NekoNYume item '" + nyn.kind() + ":" + nyn.value()
                                + "' unavailable (plugin missing or value invalid)"));
    }
}
