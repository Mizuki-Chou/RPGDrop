package mizukichou.nekodrop.config;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.drop.DropRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/**
 * 配置管理器：config.yml（插件设置）+ drops.yml（掉落规则）的读取与保存。
 *
 * 目录结构：
 *   plugins/RPGDrop/
 *   ├── config.yml
 *   ├── drops.yml
 *   └── data/          （预留）
 */
public final class ConfigManager {

    private final RPGDropPlugin plugin;
    private final File dropsFile;

    private Settings settings;
    private YamlConfiguration dropsConfig;

    public ConfigManager(RPGDropPlugin plugin) {
        this.plugin = plugin;
        this.dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        reload();
    }

    public Settings settings() {
        return settings;
    }

    /** 重载 config.yml 与 drops.yml。 */
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        settings = new Settings(
                cfg.getBoolean("settings.debug", false),
                cfg.getBoolean("settings.rpgitems.enabled", true),
                cfg.getBoolean("settings.drops.keep-vanilla-drops", true),
                cfg.getBoolean("settings.performance.cache-rpgitems", true)
        );

        if (!dropsFile.exists()) {
            plugin.saveResource("drops.yml", false);
        }
        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);
    }

    /**
     * 解析 drops.yml 中全部规则。
     * 非法规则：记录 SEVERE 日志并跳过（绝不静默失败、绝不吞错）。
     */
    public List<DropRule> loadRules() {
        ConfigurationSection section = dropsConfig.getConfigurationSection("drops");
        if (section == null) {
            return List.of();
        }

        List<DropRule> rules = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection ruleSection = section.getConfigurationSection(id);
            if (ruleSection == null) {
                severe("Failed to load drop rule '" + id + "': not a valid configuration section");
                continue;
            }
            if (!seenIds.add(id.toLowerCase(Locale.ROOT))) {
                severe("Failed to load drop rule '" + id + "': duplicate rule ID (case-insensitive)");
                continue;
            }
            try {
                rules.add(DropRuleSerializer.parse(id, ruleSection));
            } catch (ConfigException e) {
                severe("Failed to load drop rule '" + id + "': " + e.getMessage());
            }
        }
        return rules;
    }

    /** 把内存规则写回 drops.yml（注意：重写会丢失原有注释，属预期行为）。 */
    public void saveRules(Collection<DropRule> rules) {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection section = out.createSection("drops");
        for (DropRule rule : rules) {
            DropRuleSerializer.write(rule, section);
        }
        try {
            out.save(dropsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save drops.yml", e);
        }
    }

    private void severe(String message) {
        plugin.getLogger().log(Level.SEVERE, message);
    }
}
