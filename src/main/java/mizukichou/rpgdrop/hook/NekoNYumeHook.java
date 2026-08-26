package mizukichou.rpgdrop.hook;

import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NekoNYume 集成（软依赖：编译期零依赖）。
 *
 * NekoNYume 是 load: POSTWORLD 插件，且没有公开 Maven 仓库，因此本类完全通过反射调用：
 *  - 构建 RPGDrop 不需要 NekoNYume 的任何 jar / 依赖
 *  - 没装 NekoNYume：本类所有调用安全降级为 empty / false
 *  - 装了：运行时动态获取插件实例并生成物品
 *
 * 依赖的 NekoNYume API（若其未来版本变化，只需维护本类）：
 *   - NekoNYume#getCatFoodManager()                -> CatFoodManager
 *   - CatFoodManager#createMeowDan(MeowDanQuality, int, Player)   -> ItemStack
 *   - CatFoodManager#createXpPill(XpPillTier, int, Player)        -> ItemStack
 *   - CatFoodManager#createEquipment(CatEquipItem, int, Player)   -> ItemStack
 *   - CatFoodManager#createEquipBag(int, Player)                  -> ItemStack
 *   - CatFoodManager#isMeowDan/isXpPill/isEquipment/isEquipBag(ItemStack)
 *   - CatFoodManager#getMeowDanQuality/getXpPillTier/getEquipment(ItemStack)
 *   - 上述 create 方法的 Player 参数可传 null（其 Lang.forPlayer(null) 会回退默认语言）
 *   - 枚举完整类名：mizukichou.nekonyume.cat.{MeowDanQuality, XpPillTier, CatEquipItem}
 *
 * 设计说明：
 *  - 只缓存 Method 句柄与枚举类，不缓存任何对象；缓存为实例字段（不 static），
 *    插件自身重载时随实例重建，避免跨 ClassLoader 持有旧类。
 *  - "API 不可用"采用可恢复熔断：失败后 60 秒内静默，之后自动重试——
 *    兼容 NekoNYume POSTWORLD 晚启用 / 初始化未完成的场景。
 */
public final class NekoNYumeHook {

    private static final String PLUGIN_NAME = "NekoNYume";
    private static final String NYN_ROOT = "mizukichou.nekonyume."; // NekoNYume 插件自身的包名前缀（反射类名）
    /** API 熔断重试间隔（毫秒）。 */
    private static final long RETRY_INTERVAL_MS = 60_000L;

    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private final java.util.Set<String> warnedEnums = new java.util.HashSet<>();

    private final Log log;
    private long brokenSince = 0L;

    public NekoNYumeHook(Log log) {
        this.log = log;
    }

    /** 重置缓存与熔断状态（依赖插件 NekoNYume 被禁用/重载时调用），下次调用重新解析。 */
    public void reset() {
        methodCache.clear();
        classCache.clear();
        brokenSince = 0L;
        warnedEnums.clear();
    }

    /** NekoNYume 在服务器启动后启用（POSTWORLD），所以每次都动态检测。 */
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    /** 生成 NekoNYume 物品；插件未安装 / 参数非法 / 生成失败时返回 empty。 */
    public Optional<ItemStack> createItemStack(NekoNYumeDropItem item) {
        try {
            Object foods = foodManager();
            if (foods == null) {
                return Optional.empty();
            }
            Class<?> foodClass = foods.getClass();
            ItemStack stack = switch (item.kind()) {
                case "meowdan" -> createWithEnum(foodClass, foods, "createMeowDan", "MeowDanQuality", item.value(), "meowdan");
                case "xppill" -> createWithEnum(foodClass, foods, "createXpPill", "XpPillTier", item.value(), "xppill");
                case "equipment" -> createWithEnum(foodClass, foods, "createEquipment", "CatEquipItem", item.value(), "equipment");
                case "equipbag" -> (ItemStack) method(foodClass, "createEquipBag", int.class, Player.class)
                        .invoke(foods, 1, null);
                default -> null;
            };
            return Optional.ofNullable(stack);
        } catch (Exception | LinkageError e) {
            markApiBroken("Failed to generate NekoNYume item '" + item.kind() + ":" + item.value() + "'", e);
            return Optional.empty();
        }
    }

    /**
     * 一次识别手中物品的 NekoNYume 身份（kind + value），供触发物索引 O(1) 匹配。
     * 相比逐规则调用 matches，每次右键只做一次反射识别。
     * @return [kind, value]；不是 NekoNYume 物品时返回 empty
     */
    public Optional<String[]> resolveIdentity(ItemStack stack) {
        try {
            Object foods = foodManager();
            if (foods == null || stack == null || stack.getType().isAir()) {
                return Optional.empty();
            }
            Class<?> foodClass = foods.getClass();
            if ((boolean) method(foodClass, "isEquipBag", ItemStack.class).invoke(foods, stack)) {
                return Optional.of(new String[]{"equipbag", ""});
            }
            if ((boolean) method(foodClass, "isMeowDan", ItemStack.class).invoke(foods, stack)) {
                Object q = method(foodClass, "getMeowDanQuality", ItemStack.class).invoke(foods, stack);
                return q instanceof Enum<?> e ? Optional.of(new String[]{"meowdan", e.name()}) : Optional.empty();
            }
            if ((boolean) method(foodClass, "isXpPill", ItemStack.class).invoke(foods, stack)) {
                Object t = method(foodClass, "getXpPillTier", ItemStack.class).invoke(foods, stack);
                return t instanceof Enum<?> e ? Optional.of(new String[]{"xppill", e.name()}) : Optional.empty();
            }
            if ((boolean) method(foodClass, "isEquipment", ItemStack.class).invoke(foods, stack)) {
                Object e2 = method(foodClass, "getEquipment", ItemStack.class).invoke(foods, stack);
                return e2 instanceof Enum<?> e ? Optional.of(new String[]{"equipment", e.name()}) : Optional.empty();
            }
            return Optional.empty();
        } catch (Exception | LinkageError e) {
            markApiBroken("Failed to resolve NekoNYume item identity", e);
            return Optional.empty();
        }
    }

    /** 获取 CatFoodManager；插件未安装时返回 null（正常降级），已启用但返回 null 时记一次告警。 */
    private Object foodManager() throws Exception {
        if (isApiBroken()) {
            return null;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        Method getFoods = method(plugin.getClass(), "getCatFoodManager");
        Object foods = getFoods.invoke(plugin);
        if (foods == null) {
            markApiBroken("NekoNYume is enabled but getCatFoodManager() returned null (initialization incomplete?)", null);
        }
        return foods;
    }

    private ItemStack createWithEnum(Class<?> foodClass, Object foods, String createMethod,
                                     String enumSimpleName, String value, String kind) throws Exception {
        Object[] parsed = parseEnum(enumSimpleName, value);
        if (parsed == null) {
            if (warnedEnums.size() > 512) {
                warnedEnums.clear(); // 有界化
            }
            if (warnedEnums.add(kind + ":" + value)) {
                log.warn("Unknown NekoNYume enum value '" + value + "' for kind '" + kind
                        + "' (check the ID against the NekoNYume version in use)");
            }
            return null;
        }
        // 注意：parsed[0] 本身就是枚举的 Class 对象，不能再调 getClass()（那会得到 java.lang.Class）
        Class<?> enumClass = (Class<?>) parsed[0];
        Method method = method(foodClass, createMethod, enumClass, int.class, Player.class);
        return (ItemStack) method.invoke(foods, parsed[1], 1, null);
    }

    /** 解析枚举：返回 [枚举类, 枚举常量]；失败返回 null。 */
    private Object[] parseEnum(String enumSimpleName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Class<?> enumClass = classCache.computeIfAbsent(enumSimpleName, name -> {
                try {
                    return Class.forName(NYN_ROOT + "cat." + name);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            });
            if (enumClass == null) {
                return null;
            }
            for (Object constant : enumClass.getEnumConstants()) {
                if (constant instanceof Enum<?> e && e.name().equalsIgnoreCase(value.trim())) {
                    return new Object[]{enumClass, constant};
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 反射方法查找（带缓存）。 */
    private Method method(Class<?> owner, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
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
        log.severe(reason + " (NekoNYume API broken, will retry in "
                + (RETRY_INTERVAL_MS / 1000) + "s)", cause);
    }
}
