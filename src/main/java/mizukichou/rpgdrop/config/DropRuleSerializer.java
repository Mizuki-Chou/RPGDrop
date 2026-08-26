package mizukichou.rpgdrop.config;

import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.ItemType;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.drop.WorldMode;
import mizukichou.rpgdrop.util.Amounts;
import mizukichou.rpgdrop.util.Chance;
import mizukichou.rpgdrop.util.Materials;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Locale;

/**
 * DropRule <-> YAML 序列化/反序列化。
 *
 * 硬性原则：配置加载必须做合法性校验，任何字段非法都抛出 ConfigException，
 * 由调用方记录 SEVERE 日志并跳过该规则（不静默、不猜测、不吞错）。
 */
public final class DropRuleSerializer {

    private DropRuleSerializer() {
    }

    public static DropRule parse(String id, ConfigurationSection s) throws ConfigException {
        DropRule rule = new DropRule(id);

        if (s.isSet("enabled") && !s.isBoolean("enabled")) {
            throw new ConfigException("'enabled' must be a boolean (true/false)");
        }
        rule.setEnabled(s.getBoolean("enabled", true));

        // ---- entities ----
        if (s.isSet("entities") && !s.isList("entities")) {
            throw new ConfigException("'entities' must be a list (e.g. entities: [ZOMBIE])");
        }
        for (String raw : s.getStringList("entities")) {
            String name = raw.trim().toUpperCase(Locale.ROOT);
            try {
                rule.addEntity(EntityType.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new ConfigException("Unknown entity type '" + raw + "'");
            }
        }

        // ---- worlds ----
        ConfigurationSection worldSection = s.getConfigurationSection("worlds");
        String modeRaw = worldSection == null ? null : worldSection.getString("mode", "WHITELIST");
        if (modeRaw == null || modeRaw.isBlank()) {
            throw new ConfigException("Missing worlds.mode (only WHITELIST is supported)");
        }
        try {
            rule.setWorldMode(WorldMode.valueOf(modeRaw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Unknown world mode '" + modeRaw + "' (only WHITELIST is supported)");
        }
        if (worldSection != null) {
            if (worldSection.isSet("list") && !worldSection.isList("list")) {
                throw new ConfigException("worlds.list must be a list (e.g. list: [resource])");
            }
            int worldCount = 0;
            for (String world : worldSection.getStringList("list")) {
                if (world.isBlank()) {
                    continue;
                }
                if (++worldCount > DropRule.MAX_WORLDS_PER_RULE) {
                    throw new ConfigException("Too many worlds (limit " + DropRule.MAX_WORLDS_PER_RULE + " per rule)");
                }
                rule.addWorld(world);
            }
        }

        // ---- item ----
        ConfigurationSection itemSection = s.getConfigurationSection("item");
        if (itemSection == null) {
            throw new ConfigException("Missing item section");
        }
        rule.setItem(parseItem(itemSection));

        // ---- chance（百分比单位：0.01 = 0.01%） ----
        double chance = s.getDouble("chance", Double.NaN);
        if (!Chance.isValid(chance)) {
            throw new ConfigException("chance must be a finite number between 0 and 100 (0.01 = 0.01%)");
        }
        rule.setChance(chance);

        // ---- amount ----
        int min;
        int max;
        if (s.isInt("amount")) {
            min = max = s.getInt("amount");
        } else {
            ConfigurationSection amountSection = s.getConfigurationSection("amount");
            if (amountSection == null) {
                throw new ConfigException("Missing amount section (min/max)");
            }
            min = amountSection.getInt("min", 0);
            max = amountSection.getInt("max", 0);
        }
        if (!Amounts.isValid(min, max)) {
            throw new ConfigException("amount must satisfy min >= 1 and max >= min");
        }
        rule.setAmount(min, max);

        return rule;
    }

    private static DropItem parseItem(ConfigurationSection itemSection) throws ConfigException {
        String typeRaw = itemSection.getString("type");
        ItemType type = ItemType.parse(typeRaw)
                .orElseThrow(() -> new ConfigException("Unknown item type '" + typeRaw + "' (supported: VANILLA / RPGITEM / NEKONYUME)"));

        return switch (type) {
            case VANILLA -> {
                String materialRaw = itemSection.getString("material");
                Material material = Materials.parse(materialRaw)
                        .orElseThrow(() -> new ConfigException("Unknown material '" + materialRaw + "'"));
                yield new VanillaDropItem(material);
            }
            case RPGITEM -> {
                String rpgId = itemSection.getString("id");
                if (rpgId == null || rpgId.isBlank()) {
                    throw new ConfigException("RPGITEM type requires an id");
                }
                yield new RPGItemDropItem(rpgId);
            }
            case NEKONYUME -> {
                String kind = itemSection.getString("nyn_kind");
                String value = itemSection.getString("nyn_id", "");
                if (kind == null || !NekoNYumeDropItem.KINDS.contains(kind.toLowerCase(Locale.ROOT))) {
                    throw new ConfigException("Unknown NekoNYume kind '" + kind + "' (meowdan/xppill/equipment/equipbag)");
                }
                yield new NekoNYumeDropItem(kind, value);
            }
        };
    }

    public static void write(DropRule rule, ConfigurationSection parent) {
        ConfigurationSection s = parent.createSection(rule.id());
        s.set("enabled", rule.isEnabled());
        s.set("entities", new ArrayList<>(rule.entities().stream().map(EntityType::name).sorted().toList()));
        s.set("worlds.mode", rule.worldMode().name());
        s.set("worlds.list", new ArrayList<>(rule.worlds().stream().sorted().toList()));

        if (rule.item() instanceof VanillaDropItem vanilla) {
            s.set("item.type", ItemType.VANILLA.name());
            s.set("item.material", vanilla.material().getKey().toString());
        } else if (rule.item() instanceof RPGItemDropItem rpgItem) {
            s.set("item.type", ItemType.RPGITEM.name());
            s.set("item.id", rpgItem.rpgItemId());
        } else if (rule.item() instanceof NekoNYumeDropItem nyn) {
            s.set("item.type", ItemType.NEKONYUME.name());
            s.set("item.nyn_kind", nyn.kind());
            s.set("item.nyn_id", nyn.value());
        }

        s.set("chance", rule.chance());
        s.set("amount.min", rule.minAmount());
        s.set("amount.max", rule.maxAmount());
    }
}
