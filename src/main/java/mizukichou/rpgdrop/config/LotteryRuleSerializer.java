package mizukichou.rpgdrop.config;

import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.ItemType;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.drop.Prize;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.util.Chance;
import mizukichou.rpgdrop.util.Materials;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * lotteries.yml 的读取与写入。任何字段非法都会抛 {@link ConfigException}，由调用方跳过该规则并记录日志。
 */
public final class LotteryRuleSerializer {

    /** 单条抽奖规则的最大奖品数。 */
    

    private LotteryRuleSerializer() {
    }

    public static LotteryRule parse(String id, ConfigurationSection s) throws ConfigException {
        LotteryRule rule = new LotteryRule(id);
        if (s.isSet("enabled") && !s.isBoolean("enabled")) {
            throw new ConfigException("'enabled' must be a boolean (true/false)");
        }
        rule.setEnabled(s.getBoolean("enabled", true));

        ConfigurationSection triggerSection = s.getConfigurationSection("trigger");
        if (triggerSection != null) {
            rule.setTrigger(parseItem(triggerSection));
        }

        if (s.isSet("prizes") && !s.isList("prizes")) {
            throw new ConfigException("'prizes' must be a list");
        }
        List<?> prizeList = s.getList("prizes");
        if (prizeList != null && prizeList.size() > LotteryRule.MAX_PRIZES_PER_RULE) {
            throw new ConfigException("Too many prizes (limit " + LotteryRule.MAX_PRIZES_PER_RULE + " per rule)");
        }
        double totalWeight = 0.0;
        if (prizeList != null) {
            for (Object entry : prizeList) {
                ConfigurationSection prizeSection;
                if (entry instanceof ConfigurationSection cs) {
                    prizeSection = cs;
                } else if (entry instanceof Map<?, ?> map) {
                    // 容错：内存 set() 场景返回 Map（文件加载场景为 ConfigurationSection）
                    YamlConfiguration tmp = new YamlConfiguration();
                    for (Map.Entry<?, ?> e : map.entrySet()) {
                        tmp.set(String.valueOf(e.getKey()), e.getValue());
                    }
                    prizeSection = tmp;
                } else {
                    throw new ConfigException("Invalid prize entry in lottery '" + id + "'");
                }
                DropItem item = parseItem(prizeSection);
                double weight = prizeSection.getDouble("weight", Double.NaN);
                if (!Chance.isValid(weight)) {
                    throw new ConfigException("Prize weight must be a finite number between 0 and 100");
                }
                totalWeight += weight;
                if (totalWeight > 100.0 + 1e-9) {
                    throw new ConfigException("Total prize weight exceeds 100% (must add up to exactly 100%)");
                }
                rule.addPrize(new Prize(item, weight));
            }
        }
        return rule;
    }

    public static void write(LotteryRule rule, ConfigurationSection parent) {
        ConfigurationSection s = parent.createSection(rule.id());
        s.set("enabled", rule.isEnabled());
        if (rule.trigger() != null) {
            ConfigurationSection trigger = s.createSection("trigger");
            writeItem(rule.trigger(), trigger);
        }
        List<Map<String, Object>> prizeList = new ArrayList<>();
        for (Prize prize : rule.prizes()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", prize.item().type().name());
            if (prize.item() instanceof VanillaDropItem vanilla) {
                entry.put("material", vanilla.material().getKey().toString());
            } else if (prize.item() instanceof RPGItemDropItem rpgItem) {
                entry.put("id", rpgItem.rpgItemId());
            } else if (prize.item() instanceof NekoNYumeDropItem nyn) {
                entry.put("nyn_kind", nyn.kind());
                entry.put("nyn_id", nyn.value());
            }
            entry.put("weight", prize.weight());
            prizeList.add(entry);
        }
        s.set("prizes", prizeList);
    }

    private static DropItem parseItem(ConfigurationSection s) throws ConfigException {
        String typeRaw = s.getString("type");
        ItemType type = ItemType.parse(typeRaw)
                .orElseThrow(() -> new ConfigException("Unknown item type '" + typeRaw + "' (supported: VANILLA / RPGITEM / NEKONYUME)"));
        return switch (type) {
            case VANILLA -> {
                Material material = Materials.parse(s.getString("material"))
                        .orElseThrow(() -> new ConfigException("Unknown material '" + s.getString("material") + "'"));
                yield new VanillaDropItem(material);
            }
            case RPGITEM -> {
                String rpgId = s.getString("id");
                if (rpgId == null || rpgId.isBlank()) {
                    throw new ConfigException("RPGITEM type requires an id");
                }
                yield new RPGItemDropItem(rpgId);
            }
            case NEKONYUME -> {
                String kind = s.getString("nyn_kind");
                String value = s.getString("nyn_id", "");
                if (kind == null || !NekoNYumeDropItem.KINDS.contains(kind.toLowerCase(Locale.ROOT))) {
                    throw new ConfigException("Unknown NekoNYume kind '" + kind + "' (meowdan/xppill/equipment/equipbag)");
                }
                yield new NekoNYumeDropItem(kind, value);
            }
        };
    }

    private static void writeItem(DropItem item, ConfigurationSection s) {
        s.set("type", item.type().name());
        if (item instanceof VanillaDropItem vanilla) {
            s.set("material", vanilla.material().getKey().toString());
        } else if (item instanceof RPGItemDropItem rpgItem) {
            s.set("id", rpgItem.rpgItemId());
        } else if (item instanceof NekoNYumeDropItem nyn) {
            s.set("nyn_kind", nyn.kind());
            s.set("nyn_id", nyn.value());
        }
    }
}
