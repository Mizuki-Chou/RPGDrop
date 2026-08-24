package mizukichou.rpgdrop.drop;

import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.itemprovider.ItemProviderRegistry;
import mizukichou.rpgdrop.util.Log;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
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

    private final Log log;
    private final ConfigManager configManager;
    private final ItemProviderRegistry providerRegistry;

    /** 全部规则（按加载顺序）。键为小写规则 ID。 */
    private final Map<String, DropRule> rulesById = new LinkedHashMap<>();

    /** 实体索引：EntityType -> 匹配的规则列表。 */
    private Map<EntityType, List<DropRule>> indexByEntity = new EnumMap<>(EntityType.class);

    public DropManager(Log log, ConfigManager configManager, ItemProviderRegistry providerRegistry) {
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
    }

    /** 重载配置与规则（/rdrop reload）。 */
    public void reload() {
        configManager.reload();
        loadAll();
    }

    /** 把当前内存中的全部规则写回 drops.yml（命令修改后立即持久化）。 */
    public void saveAll() {
        configManager.saveRules(getAllRules());
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

    public int getRuleCount() {
        return rulesById.size();
    }

    /** 创建规则；ID 已存在时返回 false。 */
    public boolean createRule(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (rulesById.containsKey(key)) {
            return false;
        }
        rulesById.put(key, new DropRule(id));
        rebuildIndex();
        saveAll();
        return true;
    }

    /** 删除规则；不存在时返回 false。 */
    public boolean deleteRule(String id) {
        DropRule removed = rulesById.remove(id.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        rebuildIndex();
        saveAll();
        return true;
    }

    /** 规则内容被修改后调用：重建索引并持久化。 */
    public void ruleUpdated(DropRule rule) {
        rebuildIndex();
        saveAll();
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

        if (!configManager.settings().keepVanillaDrops() && !event.getDrops().isEmpty()) {
            event.getDrops().clear();
        }

        for (DropRule rule : candidates) {
            if (!rule.isEnabled()) {
                continue;
            }
            if (!rule.matchesWorld(world)) {
                continue;
            }
            executeDrop(rule, entity, world);
        }
    }

    private void executeDrop(DropRule rule, Entity entity, World world) {
        log.debug("Matched rule: " + rule.id());

        // 概率判定：0.01 = 0.01%（nextDouble(100.0) < 0.01 即 0.01% 概率）
        double chance = rule.chance();
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        log.debug("Chance: " + chance + "%");
        log.debug("Roll: " + roll + "%");

        if (!(roll < chance)) {
            log.debug("FAILED");
            return;
        }
        log.debug("SUCCESS");

        DropItem item = rule.item();
        if (item == null) {
            log.warn("Drop rule '" + rule.id() + "' has no drop item configured, skipped. Use /rdrop item " + rule.id() + " ... to set one.");
            return;
        }

        ItemStack stack;
        try {
            stack = item.createItemStack(providerRegistry);
        } catch (Exception e) {
            log.severe("Failed to generate item for drop rule '" + rule.id() + "'", e);
            return;
        }

        if (item instanceof RPGItemDropItem rpgItem) {
            log.debug("Generated RPGItem: " + rpgItem.rpgItemId());
        } else if (item instanceof VanillaDropItem vanilla) {
            log.debug("Generated VANILLA: " + vanilla.material());
        }

        int amount = rollAmount(rule);
        Location location = entity.getLocation();
        for (int i = 0; i < amount; i++) {
            // 每个掉落实体使用独立的 ItemStack 实例（clone），互不影响
            world.dropItemNaturally(location, stack.clone());
        }
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
