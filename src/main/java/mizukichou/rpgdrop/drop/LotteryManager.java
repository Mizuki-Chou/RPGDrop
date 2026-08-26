package mizukichou.rpgdrop.drop;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.itemprovider.ItemProviderRegistry;
import mizukichou.rpgdrop.hook.NekoNYumeHook;
import mizukichou.rpgdrop.util.Log;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 抽奖规则引擎（Release 2 新增，与掉落规则完全独立，不影响 R1 任何行为）。
 */
public final class LotteryManager {

    private final RPGDropPlugin plugin;
    private final Log log;
    private final ConfigManager configManager;
    private final ItemProviderRegistry providers;
    /** 奖品生成失败的规则 ID 集合：同规则只报一次严重日志（reload 时清空）。 */
    private final java.util.Set<String> reportedFailures = new java.util.HashSet<>();

    private final Map<String, LotteryRule> rulesById = new LinkedHashMap<>();
    /** 触发物索引：原版物品 / RPGItem ID -> 规则（O(1) 匹配，避免每次右键全表扫描）。 */
    private final Map<Material, LotteryRule> vanillaTriggerIndex = new HashMap<>();
    private final Map<String, LotteryRule> rpgTriggerIndex = new HashMap<>();
    /** NekoNYume 触发物规则（数量少，直接遍历调用官方识别 API）。 */
    private final Map<String, LotteryRule> nynTriggerIndex = new HashMap<>();
    private boolean dirty = false;
    private BukkitTask saveTask;

    /** 修改后的延迟保存间隔（防抖）。 */
    private static final long SAVE_DELAY_TICKS = 20L;

    private final NekoNYumeHook nekoNYumeHook;

    public LotteryManager(RPGDropPlugin plugin, Log log, ConfigManager configManager, ItemProviderRegistry providers,
                          NekoNYumeHook nekoNYumeHook) {
        this.plugin = plugin;
        this.log = log;
        this.configManager = configManager;
        this.providers = providers;
        this.nekoNYumeHook = nekoNYumeHook;
    }

    public void loadAll() {
        rulesById.clear();
        reportedFailures.clear();
        for (LotteryRule rule : configManager.loadLotteries()) {
            rulesById.put(rule.id().toLowerCase(Locale.ROOT), rule);
            if (rule.isEnabled() && !isReady(rule)) {
                log.warn("Lottery rule '" + rule.id() + "' is not ready (total weight "
                        + totalWeight(rule) + "%, needs exactly 100%) - right-click will not consume items.");
            }
        }
        long nynRefs = rulesById.values().stream().filter(r ->
                r.trigger() instanceof NekoNYumeDropItem
                        || r.prizes().stream().anyMatch(p -> p.item() instanceof NekoNYumeDropItem))
                .count();
        if (nynRefs > 0 && !plugin.isNekoNYumeAvailable()) {
            log.warn(nynRefs + " lottery rule(s) use NEKONYUME items but NekoNYume is not enabled - those triggers/prizes will not work!");
        }
        scanDuplicateTriggers();
        rebuildTriggerIndex();
    }

    public void reload() {
        loadAll();
    }

    public boolean saveAll() {
        return configManager.saveLotteries(getAllRules());
    }

    /** 延迟保存：连续修改合并为一次写盘。 */
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

    /**
     * 加载完成后扫描重复触发物：启用的规则之间触发物必须唯一，
     * 后加载的重复规则自动禁用并 SEVERE 提示（避免"第一个静默获胜、第二个永远不触发"）。
     */
    private void scanDuplicateTriggers() {
        Map<String, LotteryRule> seen = new HashMap<>();
        for (LotteryRule rule : rulesById.values()) {
            if (!rule.isEnabled() || rule.trigger() == null) {
                continue;
            }
            String key = triggerKey(rule.trigger());
            LotteryRule prev = seen.putIfAbsent(key, rule);
            if (prev != null) {
                rule.setEnabled(false);
                log.severe("Lottery rule '" + rule.id() + "' has duplicate trigger " + key
                        + " (already used by '" + prev.id() + "') - it was automatically disabled.");
            }
        }
    }

    /** 触发物身份键（用于唯一性检查）。 */
    private static String triggerKey(DropItem trigger) {
        if (trigger instanceof VanillaDropItem vanilla) {
            return "vanilla:" + vanilla.material();
        }
        if (trigger instanceof RPGItemDropItem rpgItem) {
            return "rpgitem:" + rpgItem.rpgItemId();
        }
        if (trigger instanceof NekoNYumeDropItem nyn) {
            return "nyn:" + nyn.kind().toLowerCase(Locale.ROOT) + ":" + nyn.value().toLowerCase(Locale.ROOT);
        }
        return "other";
    }

    /** 重建触发物索引（加载/增删/修改规则后调用）。 */
    private void rebuildTriggerIndex() {
        vanillaTriggerIndex.clear();
        rpgTriggerIndex.clear();
        nynTriggerIndex.clear();
        for (LotteryRule rule : rulesById.values()) {
            if (!rule.isEnabled() || rule.trigger() == null) {
                continue;
            }
            if (rule.trigger() instanceof VanillaDropItem vanilla) {
                vanillaTriggerIndex.putIfAbsent(vanilla.material(), rule);
            } else if (rule.trigger() instanceof RPGItemDropItem rpgItem) {
                rpgTriggerIndex.putIfAbsent(rpgItem.rpgItemId(), rule);
            } else if (rule.trigger() instanceof NekoNYumeDropItem nyn) {
                nynTriggerIndex.putIfAbsent(nyn.kind().toLowerCase(Locale.ROOT) + ":" + nyn.value().toLowerCase(Locale.ROOT), rule);
            }
        }
    }

    public LotteryRule getRule(String id) {
        return rulesById.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<LotteryRule> getAllRules() {
        return Collections.unmodifiableCollection(rulesById.values());
    }

    /** 是否已达规则总量上限（命令 / GUI 创建前检查用）。 */
    public boolean isLimitReached() {
        return rulesById.size() >= ConfigManager.MAX_LOTTERY_RULES;
    }

    public int getRuleCount() {
        return rulesById.size();
    }

    /** 创建新规则；ID 已存在或已达总量上限时返回 false。 */
    public boolean createRule(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (rulesById.containsKey(key) || rulesById.size() >= ConfigManager.MAX_LOTTERY_RULES) {
            return false;
        }
        LotteryRule rule = new LotteryRule(id);
        rulesById.put(key, rule);
        rebuildTriggerIndex();
        scheduleSave();
        return true;
    }

    /** 删除规则；不存在时返回 false。 */
    public boolean deleteRule(String id) {
        LotteryRule removed = rulesById.remove(id.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        rebuildTriggerIndex();
        scheduleSave();
        return true;
    }

    /** 规则内容变化后调用：重建索引并延迟持久化。 */
    public void ruleUpdated(LotteryRule rule) {
        rebuildTriggerIndex();
        scheduleSave();
    }

    /** 检查触发物是否已被其他规则占用。 */
    public Optional<LotteryRule> findTriggerConflict(LotteryRule self, DropItem trigger) {
        for (LotteryRule rule : rulesById.values()) {
            // 只与"启用中"的规则冲突：禁用规则不参与匹配，也不应占用触发物（与索引语义一致）
            if (rule == self || rule.trigger() == null || !rule.isEnabled()) {
                continue;
            }
            if (sameTrigger(rule.trigger(), trigger)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /** 按触发物匹配规则（O(1) 索引查询，不再全表扫描）。 */
    public Optional<LotteryRule> matchTrigger(ItemStack hand) {
        if (hand == null || hand.getType().isAir()) {
            return Optional.empty();
        }
        // 匹配优先级：RPGITEM > NEKONYUME > VANILLA。
        // 自定义物品可能伪装成原版外观（如"长得像钻石的 RPGItem"），必须先识别特殊物品，
        // 否则原版触发物规则会抢先匹配，导致特殊物品规则永远不触发。
        if (!rpgTriggerIndex.isEmpty() && plugin.isRpgItemsAvailable()) {
            Optional<String> id = plugin.getRpgItemId(hand);
            if (id.isPresent()) {
                LotteryRule rpg = rpgTriggerIndex.get(id.get());
                if (rpg != null) {
                    return Optional.of(rpg);
                }
            }
        }
        if (!nynTriggerIndex.isEmpty() && plugin.isNekoNYumeAvailable()) {
            Optional<String[]> identity = nekoNYumeHook.resolveIdentity(hand);
            if (identity.isPresent()) {
                LotteryRule nyn = nynTriggerIndex.get(identity.get()[0].toLowerCase(Locale.ROOT) + ":" + identity.get()[1].toLowerCase(Locale.ROOT));
                if (nyn != null) {
                    return Optional.of(nyn);
                }
            }
        }
        LotteryRule vanilla = vanillaTriggerIndex.get(hand.getType());
        if (vanilla != null) {
            return Optional.of(vanilla);
        }
        return Optional.empty();
    }

    /** 奖品权重合计（百分比）。 */
    public double totalWeight(LotteryRule rule) {
        double total = 0.0;
        for (Prize prize : rule.prizes()) {
            total += prize.weight();
        }
        return total;
    }

    /** 规则是否就绪：奖品非空且权重合计正好 100%。 */
    public boolean isReady(LotteryRule rule) {
        if (rule.prizes().isEmpty()) {
            return false;
        }
        return Math.abs(totalWeight(rule) - 100.0) < 1e-9;
    }

    /**
     * 执行一次抽奖（权重区间模型）：权重合计必须正好 100%，roll 落在哪个区间就中哪个，必中其一。
     * 规则未就绪时不消耗也不抽奖，并提示玩家，返回 false；否则掉落奖品并返回 true（表示应消耗触发物）。
     */
    public boolean tryRoll(Player player, LotteryRule rule) {
        if (!isReady(rule)) {
            Msg.send(player, "command.lottery_not_ready", rule.id(), totalWeight(rule));
            return false;
        }
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        List<Prize> prizes = rule.prizes();
        int index = pickPrizeIndex(prizes, roll);
        Prize prize = prizes.get(index);
        try {
            ItemStack stack = prize.item().createItemStack(providers);
            if (player.getWorld().dropItemNaturally(player.getLocation(), stack) == null) {
                // 掉落实体被第三方插件拦截：不消耗触发物，保证经济一致性
                //（注：Bukkit API 本身不保证通过 null 表达"被拦截"，此分支为防御性兜底）
                log.warn("Prize spawn was blocked for lottery rule '" + rule.id()
                        + "', trigger item was NOT consumed");
                return false;
            }
            Msg.send(player, "lottery.result_win", describe(prize.item()));
            return true;
        } catch (Exception | LinkageError e) {
            // 奖品生成失败：不消耗玩家物品（防止白亏）
            if (reportedFailures.add(rule.id() + "#prize")) {
                log.severe("Failed to generate lottery prize for rule '" + rule.id()
                        + "' (further failures suppressed until reload)", e);
            }
            return false;
        }
    }

    /**
     * 纯函数：按权重区间选出中奖奖品下标。
     * roll 区间 [0, 100)；权重合计 = 100 时必中一个；浮点边界兜底返回最后一个奖品。
     * 抽成纯函数是为了让核心概率边界可以脱离服务器环境做单元测试。
     */
    public static int pickPrizeIndex(List<Prize> prizes, double roll) {
        if (prizes.isEmpty()) {
            return -1; // 防御：空列表（正常调用路径已由 isReady 保证非空）
        }
        double acc = 0.0;
        for (int i = 0; i < prizes.size(); i++) {
            acc += prizes.get(i).weight();
            if (roll < acc) {
                return i;
            }
        }
        return Math.max(0, prizes.size() - 1);
    }

    /** 判断两个触发物是否"同一物品"（NekoNYume 比较 kind+value）。 */
    private static boolean sameTrigger(DropItem a, DropItem b) {
        if (a instanceof VanillaDropItem va && b instanceof VanillaDropItem vb) {
            return va.material() == vb.material();
        }
        if (a instanceof RPGItemDropItem ra && b instanceof RPGItemDropItem rb) {
            return ra.rpgItemId().equalsIgnoreCase(rb.rpgItemId());
        }
        if (a instanceof NekoNYumeDropItem na && b instanceof NekoNYumeDropItem nb) {
            return na.kind().equalsIgnoreCase(nb.kind()) && na.value().equalsIgnoreCase(nb.value());
        }
        return false;
    }

    /** 物品描述（消息显示用）。 */
    public String describe(DropItem item) {
        if (item instanceof NekoNYumeDropItem nyn) {
            return "NYN(" + nyn.kind() + ":" + nyn.value() + ")";
        }
        if (item instanceof RPGItemDropItem rpgItem) {
            return "RPGITEM(" + rpgItem.rpgItemId() + ")";
        }
        if (item instanceof VanillaDropItem vanilla) {
            return vanilla.material().getKey().toString();
        }
        return "?";
    }

    /** 奖品列表快照（按加入顺序）。 */
    public List<Prize> prizes(LotteryRule rule) {
        return new ArrayList<>(rule.prizes());
    }
}
