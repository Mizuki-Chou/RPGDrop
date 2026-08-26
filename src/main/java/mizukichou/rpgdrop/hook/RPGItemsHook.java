package mizukichou.rpgdrop.hook;

import mizukichou.rpgdrop.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPGDrop 与 RPGItems Reloaded 之间的【唯一】桥梁（软依赖：编译期零依赖）。
 *
 * 本类完全通过反射调用 RPGItems，因此构建 RPGDrop 不需要 RPGItems 的 jar 或 Maven 依赖；
 * 服务器上装了 RPGItems 就生效，没装时所有调用自动跳过。
 *
 * 已对照 RPGItems Reloaded 3.38.0 源码验证以下 API：
 *   - ItemManager.getItemByName(String)  -> RPGItem（可空，注意不是 Optional）
 *   - ItemManager.toRPGItem(ItemStack)   -> Optional<RPGItem>
 *   - ItemManager.itemNames()            -> Set<String>
 *   - RPGItem#toItemStack()              -> ItemStack（每次调用生成全新实例）
 *   - RPGItem#getName()                  -> String（物品 ID）
 * 若 RPGItems 未来版本改动了上述签名，只需维护本类。
 *
 * 设计说明：
 *  - 不做任何对象缓存（getItemByName 本身就是 O(1) 查表，缓存反而会在 reload 后持有过期对象）。
 *  - Method 缓存为实例字段（不 static）：RPGDrop 自身被重载（/reload 或插件管理器）时缓存随实例
 *    重建，不会跨 ClassLoader 持有旧类，避免 stale method / classloader 泄漏。
 *  - "API 不可用"采用可恢复熔断：失败后 60 秒内静默，之后自动重试（兼容依赖插件重载恢复的场景），
 *    持续故障时约每分钟记录一条 SEVERE，不会刷屏。
 */
public final class RPGItemsHook {

    private static final String PLUGIN_NAME = "RPGItems";
    private static final String ITEM_MANAGER_CLASS = "think.rpgitems.item.ItemManager";
    /** API 熔断重试间隔（毫秒）。 */
    private static final long RETRY_INTERVAL_MS = 60_000L;

    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    /** 已提示过"不存在"的物品 ID（每 ID 只 warn 一次，避免刷屏）。 */
    private final Set<String> warnedMissing = new HashSet<>();

    private final Log log;
    private long brokenSince = 0L;

    public RPGItemsHook(Log log) {
        this.log = log;
    }

    /** RPGItems 是否已启用（每次动态检测，兼容其异步加载流程）。 */
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    /** 根据 RPGItem ID 生成真实 ItemStack；物品不存在或生成失败时返回 empty。 */
    public Optional<ItemStack> createItemStack(String rpgItemId) {
        if (rpgItemId == null || rpgItemId.isBlank()) {
            return Optional.empty();
        }
        Object item = resolveRpgItem(rpgItemId);
        if (item == null) {
            if (warnedMissing.size() > 512) {
                warnedMissing.clear(); // 有界化：防止长期运行下无限增长
            }
            if (warnedMissing.add(rpgItemId)) {
                log.warn("RPGItem '" + rpgItemId + "' does not exist, skipped this drop");
            }
            return Optional.empty();
        }
        try {
            Method toItemStack = method(item.getClass(), "toItemStack");
            Object result = toItemStack.invoke(item);
            return result instanceof ItemStack stack ? Optional.of(stack) : Optional.empty();
        } catch (Exception | LinkageError e) {
            markApiBroken("Failed to generate RPGItem '" + rpgItemId + "'", e);
            return Optional.empty();
        }
    }

    /** 识别 ItemStack 对应的 RPGItem ID；不是 RPGItem 时返回 empty（抽奖触发物识别用）。 */
    public Optional<String> getItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return Optional.empty();
        }
        try {
            Method toRpgItem = itemManagerMethod("toRPGItem", ItemStack.class);
            Object result = toRpgItem.invoke(null, itemStack);
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }
            Object rpgItem = optional.get();
            Method getName = method(rpgItem.getClass(), "getName");
            Object name = getName.invoke(rpgItem);
            return name instanceof String s ? Optional.of(s) : Optional.empty();
        } catch (Exception | LinkageError e) {
            markApiBroken("Error while checking if ItemStack is an RPGItem", e);
            return Optional.empty();
        }
    }

    /** 是否为 RPGItem 物品。 */
    public boolean isRPGItem(ItemStack itemStack) {
        return getItemId(itemStack).isPresent();
    }

    /** 所有已加载的 RPGItem ID（tab 补全用）。 */
    public List<String> getAllItemIds() {
        try {
            Method itemNames = itemManagerMethod("itemNames");
            Object result = itemNames.invoke(null);
            if (result instanceof Set<?> names) {
                return names.stream().map(String::valueOf).toList();
            }
            return List.of();
        } catch (Exception | LinkageError e) {
            markApiBroken("Error while reading RPGItem list", e);
            return List.of();
        }
    }

    /** RPGItems 中是否存在指定 ID 的物品（设置时的即时校验）。 */
    public boolean itemExists(String rpgItemId) {
        return rpgItemId != null && resolveRpgItem(rpgItemId) != null;
    }

    /** 兼容保留：不再缓存对象，此方法无操作。 */
    /**
     * 重置全部缓存状态（依赖插件 RPGItems 被禁用/重载时调用）：
     * 清掉指向旧 ClassLoader 的 Method 缓存与熔断状态，下次调用重新解析。
     */
    public void clearCache() {
        reset();
    }

    /** 同 {@link #clearCache()}。 */
    public void reset() {
        methodCache.clear();
        warnedMissing.clear();
        brokenSince = 0L;
    }

    private Object resolveRpgItem(String rpgItemId) {
        if (isApiBroken()) {
            return null; // 熔断期短路（与 NekoNYumeHook 对称）：避免每次掉落都走一次注定失败的反射
        }
        try {
            Method getItemByName = itemManagerMethod("getItemByName", String.class);
            return getItemByName.invoke(null, rpgItemId);
        } catch (Exception | LinkageError e) {
            markApiBroken("Error while looking up RPGItem '" + rpgItemId + "'", e);
            return null;
        }
    }

    /** ItemManager 的静态方法（带缓存）。 */
    private Method itemManagerMethod(String name, Class<?>... parameterTypes)
            throws ClassNotFoundException, NoSuchMethodException {
        return method(Class.forName(ITEM_MANAGER_CLASS), name, parameterTypes);
    }

    private Method method(Class<?> owner, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        StringBuilder key = new StringBuilder(owner.getName()).append('#').append(name);
        for (Class<?> type : parameterTypes) {
            key.append('/').append(type.getName());
        }
        String cacheKey = key.toString();
        Method cached = methodCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Method resolved = owner.getMethod(name, parameterTypes);
        methodCache.put(cacheKey, resolved);
        return resolved;
    }

    /** API 熔断是否生效（60 秒内不再重试）。 */
    private boolean isApiBroken() {
        return brokenSince > 0 && System.currentTimeMillis() - brokenSince < RETRY_INTERVAL_MS;
    }

    /** 记录 API 失效：窗口期内只打一条 SEVERE，之后自动恢复重试。 */
    private void markApiBroken(String reason, Throwable cause) {
        if (isApiBroken()) {
            return;
        }
        brokenSince = System.currentTimeMillis();
        log.severe(reason + " (RPGItems API broken, will retry in "
                + (RETRY_INTERVAL_MS / 1000) + "s)", cause);
    }
}
