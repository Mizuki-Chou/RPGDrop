package mizukichou.rpgdrop.config;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.util.RuleIds;
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
 * 配置管理器：config.yml（插件设置）+ drops.yml（掉落规则）+ lotteries.yml（抽奖规则）的读取与保存。
 *
 * 目录结构：
 *   plugins/RPGDrop/
 *   ├── config.yml
 *   ├── drops.yml
 *   └── data/          （预留）
 */
public final class ConfigManager {

    /** 掉落规则总数上限（超出部分拒绝加载并告警）。 */
    public static final int MAX_DROP_RULES = 500;

    /** 抽奖规则总数上限。 */
    public static final int MAX_LOTTERY_RULES = 200;

    private final RPGDropPlugin plugin;
    private final File dropsFile;
    private final File lotteriesFile;

    private Settings settings;
    private YamlConfiguration dropsConfig;
    private YamlConfiguration lotteriesConfig;

    public ConfigManager(RPGDropPlugin plugin) {
        this.plugin = plugin;
        this.dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        this.lotteriesFile = new File(plugin.getDataFolder(), "lotteries.yml");
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
                cfg.getBoolean("settings.drops.keep-vanilla-drops", true)
        );

        if (!dropsFile.exists()) {
            plugin.saveResource("drops.yml", false);
        }
        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);

        if (!lotteriesFile.exists()) {
            plugin.saveResource("lotteries.yml", false);
        }
        lotteriesConfig = YamlConfiguration.loadConfiguration(lotteriesFile);
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
            if (!RuleIds.isValid(id)) {
                severe("Failed to load drop rule '" + id + "': invalid rule ID (must match [A-Za-z0-9_-]{1,32})");
                continue;
            }
            ConfigurationSection ruleSection = section.getConfigurationSection(id);
            if (ruleSection == null) {
                severe("Failed to load drop rule '" + id + "': not a valid configuration section");
                continue;
            }
            if (!seenIds.add(id.toLowerCase(Locale.ROOT))) {
                severe("Failed to load drop rule '" + id + "': duplicate rule ID (case-insensitive)");
                continue;
            }
            if (rules.size() >= MAX_DROP_RULES) {
                severe("Too many drop rules (limit " + MAX_DROP_RULES + "), rule '" + id + "' was skipped");
                continue;
            }
            try {
                rules.add(DropRuleSerializer.parse(id, ruleSection));
            } catch (ConfigException | IllegalArgumentException e) {
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
    public boolean saveRules(Collection<DropRule> rules) {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection section = out.createSection("drops");
        for (DropRule rule : rules) {
            DropRuleSerializer.write(rule, section);
        }

        return saveAtomic(out, dropsFile, "drops");
    }

    /**
     * 解析 lotteries.yml 中全部抽奖规则（Release 2 新增）。
     * 与掉落规则同样的容错策略：非法规则记录 SEVERE 并跳过。
     */
    public List<LotteryRule> loadLotteries() {
        ConfigurationSection section = lotteriesConfig.getConfigurationSection("lotteries");
        if (section == null) {
            if (lotteriesFile.exists() && lotteriesFile.length() > 0) {
                severe("lotteries.yml is missing the 'lotteries' section - the file may be corrupted! "
                        + "No lottery rules were loaded. Restore from lotteries.yml.bak or fix the file, then run /rdrop reload.");
            }
            return List.of();
        }

        List<LotteryRule> rules = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String id : section.getKeys(false)) {
            if (!RuleIds.isValid(id)) {
                severe("Failed to load lottery rule '" + id + "': invalid rule ID (must match [A-Za-z0-9_-]{1,32})");
                continue;
            }
            ConfigurationSection ruleSection = section.getConfigurationSection(id);
            if (ruleSection == null) {
                severe("Failed to load lottery rule '" + id + "': not a valid configuration section");
                continue;
            }
            if (!seenIds.add(id.toLowerCase(Locale.ROOT))) {
                severe("Failed to load lottery rule '" + id + "': duplicate rule ID (case-insensitive)");
                continue;
            }
            if (rules.size() >= MAX_LOTTERY_RULES) {
                severe("Too many lottery rules (limit " + MAX_LOTTERY_RULES + "), rule '" + id + "' was skipped");
                continue;
            }
            try {
                rules.add(LotteryRuleSerializer.parse(id, ruleSection));
            } catch (ConfigException | IllegalArgumentException e) {
                severe("Failed to load lottery rule '" + id + "': " + e.getMessage());
            }
        }
        return rules;
    }

    /** 把内存抽奖规则写回 lotteries.yml（备份 + 原子写入，与 drops.yml 同机制）。 */
    public boolean saveLotteries(Collection<LotteryRule> rules) {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection section = out.createSection("lotteries");
        for (LotteryRule rule : rules) {
            LotteryRuleSerializer.write(rule, section);
        }
        return saveAtomic(out, lotteriesFile, "lotteries");
    }

    /**
     * 备份 + 原子写入。
     * @return true=写入成功；false=备份或写入失败（调用方必须保留 dirty 状态，防止修改静默丢失）
     * 注意：这里保证的是"写入/替换过程不会产生半写文件"（原子替换），
     * 不包含 fsync(directory) 级别的断电强持久化。
     */
    private boolean saveAtomic(YamlConfiguration out, File target, String name) {
        File backup = new File(plugin.getDataFolder(), name + ".yml.bak");
        try {
            if (target.exists()) {
                Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to back up " + name + ".yml - save ABORTED to keep the previous file intact", e);
            return false;
        }

        File tmp = new File(plugin.getDataFolder(), name + ".yml.tmp");
        try {
            out.save(tmp);
            try {
                Files.move(tmp.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + name + ".yml", e);
            return false;
        }
        return true;
    }

    private void severe(String message) {
        plugin.getLogger().log(Level.SEVERE, message);
    }
}
