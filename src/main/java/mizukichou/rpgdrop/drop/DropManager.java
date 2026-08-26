package mizukichou.rpgdrop.drop;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.itemprovider.ItemProviderRegistry;
import mizukichou.rpgdrop.util.Log;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 掉落规则核心管理器。
 *
 * 职责：规则加载/保存/增删改查、实体与世界匹配、概率判定、执行掉落。
 * 性能设计：
 *   - 启动时把 drops.yml 全部载入内存，运行时零 YAML IO；
 *   - 按 EntityType 建立索引 Map<EntityType, List<DropRule>>，避免每次死亡遍历全部规则；
 *   - 判定顺序：实体 -> 世界 -> 概率（概率放最后，减少无效计算）。
 */
public final class DropManager {

    /** 单次死亡事件最多生成的物品实体数（防实体刷屏）。 */
    private static final int MAX_ITEMS_PER_DEATH = 256;

    /** 修改后的延迟保存间隔（防抖：连续编辑合并为一次写盘）。 */
    private static final long SAVE_DELAY_TICKS = 20L;

    private final Log log;
    private final RPGDropPlugin plugin;
    private final ConfigManager configManager;
    private final ItemProviderRegistry providerRegistry;
    /** 生成失败的规则 ID 集合：同规则只报一次严重日志，避免刷屏（reload 时清空）。 */
    private final java.util.Set<String> reportedFailures = new java.util.HashSet<>();

    /** 全部规则（按加载顺序）。键为小写规则 ID。 */
    private final Map<String, DropRule> rulesById = new LinkedHashMap<>();
    private boolean dirty = false;
    private BukkitTask saveTask;

    /** 实体索引：EntityType -> 匹配的规则列表。 */
    private Map<EntityType, List<DropRule>> indexByEntity = new EnumMap<>(EntityType.class);

    public DropManager(RPGDropPlugin plugin, Log log, ConfigManager configManager, ItemProviderRegistry providerRegistry) {
        this.plugin = plugin;
        this.log = log;
        this.configManager = configManager;
        this.providerRegistry = providerRegistry;
    }

    // ------------------------------------------------------------------
    // 规则生命周期
    // ------------------------------------------------------------------

    /** 从 drops.yml 加载全部规则并重建索引。 */
    public void loadAll() {
        rulesById.clear();
        reportedFailures.clear();
        for (DropRule rule : configManager.loadRules()) {
            rulesById.put(rule.id().toLowerCase(Locale.ROOT), rule);
        }
        rebuildIndex();

        // 启动自检：规则使用了 RPGITEM 但对应 Provider 不可用（未装 RPGItems）时警告
        long rpgRules = rulesById.values().stream()
                .filter(r -> r.item() instanceof RPGItemDropItem)
                .count();
        if (rpgRules > 0 && !providerRegistry.hasProvider(ItemType.RPGITEM)) {
            log.warn(rpgRules + " drop rule(s) use RPGITEM but RPGItems is not installed - they will not drop anything!");
        }

        // 启动自检：规则使用了 NEKONYUME 物品但 NekoNYume 未启用时警告
        long nynRules = rulesById.values().stream()
                .filter(r -> r.item() instanceof NekoNYumeDropItem)
                .count();
        if (nynRules > 0 && !plugin.isNekoNYumeAvailable()) {
            log.warn(nynRules + " drop rule(s) use NEKONYUME items but NekoNYume is not enabled - they will not drop anything!");
        }
        // 启动自检：启用的规则但永远无法触发时警告（实体为空 / 世界为空 / 未配置掉落物）
        for (DropRule rule : rulesById.values()) {
            if (!rule.isEnabled()) {
                continue;
            }
            if (rule.entities().isEmpty()) {
                log.warn("Drop rule '" + rule.id() + "' is enabled but has no entities - it will never trigger.");
            }
            if (rule.worlds().isEmpty()) {
                log.warn("Drop rule '" + rule.id() + "' is enabled but has no worlds (WHITELIST) - it will never trigger.");
            }
            if (rule.item() == null) {
                log.warn("Drop rule '" + rule.id() + "' is enabled but has no drop item - it will drop nothing.");
            }
        }

    }

    /** 重载配置与规则（/rdrop reload）。 */
    public void reload() {
        loadAll();
    }

    /** 把当前内存中的全部规则写回 drops.yml（命令修改后立即持久化）。 */
    public boolean saveAll() {
        return configManager.saveRules(getAllRules());
    }

    /** 延迟保存：连续修改合并为一次写盘（避免每次 GUI 点击都同步序列化 YAML）。 */
    private void scheduleSave() {
        dirty = true;
        if (saveTask != null) {
            return;
        }
        saveTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::flush, SAVE_DELAY_TICKS);
    }

    /**
     * 立即落盘。只有在真正写入成功后才清除 dirty；失败时保留 dirty，
     * 由下次保存/重载/关停时重试，保证修改不会静默丢失。
     * @return true=已成功落盘（或本来就没有待保存修改）
     */
    public boolean flush() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        if (!dirty) {
            return true;
        }
        if (saveAll()) {
            dirty = false;
            return true;
        }
        return false;
    }

    /** 重建实体索引（规则变更后调用）。 */
    public void rebuildIndex() {
        Map<EntityType, List<DropRule>> map = new EnumMap<>(EntityType.class);
        for (DropRule rule : rulesById.values()) {
            for (EntityType type : rule.entities()) {
                map.computeIfAbsent(type, k -> new ArrayList<>()).add(rule);
            }
        }
        indexByEntity = map;
    }

    // ------------------------------------------------------------------
    // 规则查询与修改（供命令层调用）
    // ------------------------------------------------------------------

    public DropRule getRule(String id) {
        return rulesById.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<DropRule> getAllRules() {
        return Collections.unmodifiableCollection(rulesById.values());
    }

    /** 是否已达规则总量上限（命令 / GUI 创建前检查用）。 */
    public boolean isLimitReached() {
        return rulesById.size() >= ConfigManager.MAX_DROP_RULES;
    }

    public int getRuleCount() {
        return rulesById.size();
    }

    /** 创建规则；ID 已存在时返回 false。 */
    public boolean createRule(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (rulesById.containsKey(key) || rulesById.size() >= ConfigManager.MAX_DROP_RULES) {
            return false;
        }
        rulesById.put(key, new DropRule(id));
        rebuildIndex();
        scheduleSave();
        return true;
    }

    /** 删除规则；不存在时返回 false。 */
    public boolean deleteRule(String id) {
        DropRule removed = rulesById.remove(id.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        rebuildIndex();
        scheduleSave();
        return true;
    }

    /** 规则内容被修改后调用：重建索引并持久化。 */
    public void ruleUpdated(DropRule rule) {
        rebuildIndex();
        scheduleSave();
    }

    // ------------------------------------------------------------------
    // 掉落执行
    // ------------------------------------------------------------------

    /**
     * EntityDeathEvent 的统一入口。
     * 默认行为：保留原版掉落，只追加自定义掉落；
     * keep-vanilla-drops: false 时清空原版掉落。
     */
    public void processDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        EntityType type = entity.getType();

        List<DropRule> candidates = indexByEntity.get(type);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        World world = entity.getWorld();
        log.debug("EntityDeathEvent: " + type);
        log.debug("World: " + world.getName());

        // 只有当"实际成功生成了自定义掉落"（spawned > 0）时，才考虑接管（清空）原版掉落。
        // 概率没中、物品生成失败、掉落被第三方拦截时 spawned 为 0，绝不动原版掉落，
        // 防止"一条永不成功的规则吞掉玩家原版收益"。
        int spawned = 0;
        for (DropRule rule : candidates) {
            if (!rule.isEnabled()) {
                continue;
            }
            if (!rule.matchesWorld(world)) {
                continue;
            }
            spawned += executeDrop(rule, entity, world, MAX_ITEMS_PER_DEATH - spawned);
            if (spawned >= MAX_ITEMS_PER_DEATH) {
                log.warn("Entity death reached the per-death item limit (" + MAX_ITEMS_PER_DEATH
                        + "); remaining drops for this death were skipped.");
                break;
            }
        }

        if (spawned > 0 && !configManager.settings().keepVanillaDrops() && !event.getDrops().isEmpty()) {
            event.getDrops().clear();
        }
    }

    /** 执行单条规则的掉落，返回实际生成的物品实体数（受剩余额度限制）。 */
    private int executeDrop(DropRule rule, Entity entity, World world, int remaining) {
        log.debug("Matched rule: " + rule.id());

        // 概率判定：0.01 = 0.01%（nextDouble(100.0) < 0.01 即 0.01% 概率）
        double chance = rule.chance();
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        log.debug("Chance: " + chance + "%");
        log.debug("Roll: " + roll + "%");

        if (!(roll < chance)) {
            log.debug("FAILED");
            return 0;
        }
        log.debug("SUCCESS");

        DropItem item = rule.item();
        if (item == null) {
            log.warn("Drop rule '" + rule.id() + "' has no drop item configured, skipped. Use /rdrop item " + rule.id() + " ... to set one.");
            return 0;
        }

        ItemStack stack;
        try {
            stack = item.createItemStack(providerRegistry);
        } catch (Exception | LinkageError e) {
            if (reportedFailures.add(rule.id())) {
                log.severe("Failed to generate item for drop rule '" + rule.id()
                        + "' (further failures for this rule suppressed until reload)", e);
            }
            return 0;
        }

        if (item instanceof RPGItemDropItem rpgItem) {
            log.debug("Generated RPGItem: " + rpgItem.rpgItemId());
        } else if (item instanceof VanillaDropItem vanilla) {
            log.debug("Generated VANILLA: " + vanilla.material());
        }

        int amount = Math.min(rollAmount(rule), remaining);
        Location location = entity.getLocation();
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            // 每个掉落实体使用独立的 ItemStack 实例（clone），互不影响
            // 被第三方插件拦截（EntitySpawnEvent cancel）时 dropItemNaturally 返回 null，不计入已生成数
            if (world.dropItemNaturally(location, stack.clone()) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    private int rollAmount(DropRule rule) {
        int min = rule.minAmount();
        int max = rule.maxAmount();
        if (min == max) {
            return min;
        }
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }
}
