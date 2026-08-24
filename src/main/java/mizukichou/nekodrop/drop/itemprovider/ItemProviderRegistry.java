package mizukichou.nekodrop.drop.itemprovider;

import mizukichou.nekodrop.drop.DropItem;
import mizukichou.nekodrop.drop.ItemType;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * ItemProvider 注册中心。
 *
 * 扩展点：未来接入 ItemsAdder / MMOItems / Oraxen 等物品插件时，
 * 实现新的 ItemProvider 并在这里注册即可，无需改动规则引擎。
 */
public final class ItemProviderRegistry {

    private final Map<ItemType, ItemProvider> providers = new EnumMap<>(ItemType.class);

    public void register(ItemProvider provider) {
        providers.put(provider.type(), provider);
    }

    /** 指定物品类型是否有可用的 Provider（如未装 RPGItems 时 RPGITEM 即为 false）。 */
    public boolean hasProvider(ItemType type) {
        return providers.containsKey(type);
    }

    public ItemStack create(DropItem item) {
        ItemProvider provider = providers.get(item.type());
        if (provider == null) {
            throw new IllegalStateException("No ItemProvider registered for item type " + item.type()
                    + " (e.g. RPGItems is not installed while the rule uses RPGITEM)");
        }
        return provider.create(item);
    }
}
