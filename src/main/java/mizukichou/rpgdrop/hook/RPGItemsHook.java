package mizukichou.rpgdrop.hook;

import mizukichou.rpgdrop.util.Log;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * RPGDrop 与 RPGItems Reloaded 之间的【唯一】桥梁。
 *
 * 硬性原则：项目中任何与 RPGItems 相关的操作都必须经过本类，
 * 其它类（DropManager / Listener / GUI / 命令）不得直接接触 RPGItems API。
 * 未来 RPGItems API 变化时，理论上只需修改本类。
 *
 * 本类已对照 RPGItems Reloaded 3.38.0 源码验证以下 API：
 *   - ItemManager.getItemByName(String)  -> RPGItem（可空，注意不是 Optional）
 *   - ItemManager.toRPGItem(ItemStack)   -> Optional<RPGItem>
 *   - ItemManager.itemNames()            -> Set<String>
 *   - RPGItem#toItemStack()              -> ItemStack（每次调用生成全新实例）
 *   - RPGItem#getName()                  -> String（物品 ID）
 */
public final class RPGItemsHook {

    private final Log log;
    private final boolean cacheEnabled;

    /**
     * 缓存 RPGItem ID -> ItemStack 工厂。
     * 硬性原则：只缓存"工厂"（Supplier），不缓存 ItemStack 实例本身，
     * 保证每次掉落都是全新的、独立的 ItemStack。
     */
    private final Map<String, Supplier<ItemStack>> cache = new ConcurrentHashMap<>();

    public RPGItemsHook(Log log, boolean cacheEnabled) {
        this.log = log;
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * 根据 RPGItem ID 生成真实 ItemStack。
     * 物品不存在或生成failed时返回 empty（并记录日志）。
     */
    public Optional<ItemStack> createItemStack(String rpgItemId) {
        if (rpgItemId == null || rpgItemId.isBlank()) {
            return Optional.empty();
        }
        Supplier<ItemStack> supplier = resolveSupplier(rpgItemId);
        if (supplier == null) {
            log.warn("RPGItem '" + rpgItemId + "' does not exist, skipped this drop");
            return Optional.empty();
        }
        try {
            return Optional.of(supplier.get());
        } catch (Exception e) {
            log.severe("Failed to generate RPGItem '" + rpgItemId + "'", e);
            return Optional.empty();
        }
    }

    /** 识别物品是否为 RPGItem，并返回其 ID（GUI 拖入识别用，V0.3 起使用）。 */
    public Optional<String> getItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return Optional.empty();
        }
        try {
            return ItemManager.toRPGItem(itemStack).map(RPGItem::getName);
        } catch (Exception e) {
            log.severe("Error while checking if ItemStack is an RPGItem", e);
            return Optional.empty();
        }
    }

    /** 判断物品是否为 RPGItem。 */
    public boolean isRPGItem(ItemStack itemStack) {
        return getItemId(itemStack).isPresent();
    }

    /** 所有已加载的 RPGItem ID（tab 补全用）。 */
    public List<String> getAllItemIds() {
        try {
            return List.copyOf(ItemManager.itemNames());
        } catch (Exception e) {
            log.severe("Error while reading RPGItem list", e);
            return List.of();
        }
    }

    /** 检查 RPGItems 中是否存在指定 ID（设置掉落物时的即时校验用）。 */
    public boolean itemExists(String rpgItemId) {
        if (rpgItemId == null || rpgItemId.isBlank()) {
            return false;
        }
        return resolveRpgItem(rpgItemId) != null;
    }

    /** 清空缓存（/rdrop reload 或 RPGItems 自身重载后调用）。 */
    public void clearCache() {
        cache.clear();
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private Supplier<ItemStack> resolveSupplier(String rpgItemId) {
        if (cacheEnabled) {
            Supplier<ItemStack> cached = cache.get(rpgItemId);
            if (cached != null) {
                return cached;
            }
            // 只缓存"存在"的物品；不存在的物品每次都重新查询，
            // 这样管理员之后补建同名 RPGItem 时无需重载即可生效。
            Supplier<ItemStack> fresh = lookupSupplier(rpgItemId);
            if (fresh != null) {
                cache.put(rpgItemId, fresh);
            }
            return fresh;
        }
        return lookupSupplier(rpgItemId);
    }

    private Supplier<ItemStack> lookupSupplier(String rpgItemId) {
        RPGItem item = resolveRpgItem(rpgItemId);
        return item == null ? null : item::toItemStack;
    }

    private RPGItem resolveRpgItem(String rpgItemId) {
        try {
            return ItemManager.getItemByName(rpgItemId);
        } catch (Exception e) {
            log.severe("Error while looking up RPGItem '" + rpgItemId + "'", e);
            return null;
        }
    }
}
