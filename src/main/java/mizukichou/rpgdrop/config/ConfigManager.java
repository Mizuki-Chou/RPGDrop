package mizukichou.rpgdrop.config;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
            // 文件存在但读不到 drops 节 = 文件损坏/结构错误，必须让管理员看到，否则掉落会静默全部失效
            if (dropsFile.exists() && dropsFile.length() > 0) {
                severe("drops.yml is missing the 'drops' section - the file may be corrupted! "
                        + "No rules were loaded. Restore from drops.yml.bak or fix the file, then run /rdrop reload.");
            }
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

    /**
     * 把内存规则写回 drops.yml（注意：重写会丢失原有注释，属预期行为）。
     *
     * 生产安全：
     *   - 写入前先备份当前文件为 drops.yml.bak（误操作或损坏时可恢复）；
     *   - 先写临时文件再原子替换，避免中途断电/崩溃导致 drops.yml 半写损坏
     *     （损坏的 drops.yml 会导致下次启动全部规则加载失败）。
     */
    public void saveRules(Collection<DropRule> rules) {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection section = out.createSection("drops");
        for (DropRule rule : rules) {
            DropRuleSerializer.write(rule, section);
        }

        // 备份当前文件
        File backup = new File(plugin.getDataFolder(), "drops.yml.bak");
        try {
            if (dropsFile.exists()) {
                Files.copy(dropsFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to back up drops.yml", e);
        }

        // 原子写入：临时文件 -> 替换
        File tmp = new File(plugin.getDataFolder(), "drops.yml.tmp");
        try {
            out.save(tmp);
            try {
                Files.move(tmp.toPath(), dropsFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), dropsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save drops.yml", e);
        }
    }

    private void severe(String message) {
        plugin.getLogger().log(Level.SEVERE, message);
    }
}
