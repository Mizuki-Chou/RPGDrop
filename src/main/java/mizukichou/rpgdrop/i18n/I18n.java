package mizukichou.rpgdrop.i18n;

import mizukichou.rpgdrop.RPGDropPlugin;
import net.kyori.adventure.identity.Identity;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Locale;
import java.util.Map;

/**
 * 国际化（i18n）管理器。
 *
 * 设计：
 *   - 语言文件位于 plugins/RPGDrop/lang/ 下，首次启动从 jar 内自动解压；
 *   - 玩家消息按 Adventure 的 {@link Identity#LOCALE} 自动选择语言，未知语言回退英语（en_us）；
 *   - 控制台命令使用英语；
 *   - 键缺失时逐级回退：所选语言 → en_us → 返回键名本身；
 *   - 支持 {0} {1} ... 占位符与多行文本（List）。
 */
public final class I18n {

    public static final String DEFAULT_LOCALE = "en_us";

    /** 内置语言列表。 */
    private static final List<String> BUNDLED_LOCALES = List.of("zh_cn", "zh_tw", "ja_jp", "en_us");

    private final RPGDropPlugin plugin;
    private final Map<String, Map<String, Object>> localeData = new HashMap<>();

    public I18n(RPGDropPlugin plugin) {
        this.plugin = plugin;
    }

    /** 加载 / 重载语言文件（内置四种 + lang 目录下管理员自建的任何 *.yml）。 */
    public void reload() {
        localeData.clear();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create language directory " + langDir);
        }
        for (String code : BUNDLED_LOCALES) {
            File file = new File(langDir, code + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + code + ".yml", false);
            }
        }

        File[] files = langDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            files = new File[0];
        }
        Arrays.sort(files);
        for (File file : files) {
            String code = file.getName().substring(0, file.getName().length() - 4);
            // 合并加载：内置键打底，磁盘文件覆盖（管理员的自定义修改优先）。
            // 这样旧语言文件会自动补上插件更新后新增的键，不会再出现"显示键名"。
            Map<String, Object> flat = new HashMap<>();
            YamlConfiguration builtin = loadBuiltin("lang/" + code + ".yml");
            if (builtin != null) {
                flat.putAll(flatten(builtin));
            }
            flat.putAll(flatten(YamlConfiguration.loadConfiguration(file)));
            localeData.put(code, flat);
        }
        plugin.getLogger().info("Loaded " + localeData.size() + " language files.");
    }

    /** 读取 jar 内置语言文件；不存在（自定义语言）时返回 null。 */
    private YamlConfiguration loadBuiltin(String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read bundled language resource: " + resourcePath, e);
            return null;
        }
    }

    /** 扁平化：仅取叶子（String / List），忽略根键。 */
    private static Map<String, Object> flatten(YamlConfiguration config) {
        Map<String, Object> flat = new HashMap<>();
        config.getValues(true).forEach((key, value) -> {
            if (!key.isEmpty() && value instanceof String) {
                flat.put(key, value);
            } else if (!key.isEmpty() && value instanceof List<?> list) {
                flat.put(key, list.stream().map(String::valueOf).toList());
            }
        });
        return flat;
    }

    // ------------------------------------------------------------------
    // 查询入口
    // ------------------------------------------------------------------

    /** 按玩家客户端语言取单行消息。 */
    public String get(Player player, String key, Object... args) {
        return get(localeOf(player), key, args);
    }

    /** 按玩家客户端语言取多行消息。 */
    public List<String> getList(Player player, String key, Object... args) {
        return getList(localeOf(player), key, args);
    }

    /** 按发送者取单行消息（控制台使用英语）。 */
    public String get(CommandSender sender, String key, Object... args) {
        return get(sender instanceof Player player ? localeOf(player) : DEFAULT_LOCALE, key, args);
    }

    /** 按发送者取多行消息（控制台使用英语）。 */
    public List<String> getList(CommandSender sender, String key, Object... args) {
        return getList(sender instanceof Player player ? localeOf(player) : DEFAULT_LOCALE, key, args);
    }

    /**
     * 读取玩家客户端语言（Adventure Identity.LOCALE，Paper 1.19+ 均可用；
     * 不用 26.2 才有的 Player#locale()，也不用已弃用的 Player#getLocale()，
     * 保证在旧版 Paper 上不抛 NoSuchMethodError）。
     */
    private static String localeOf(Player player) {
        return player.get(Identity.LOCALE).orElse(Locale.ENGLISH).toLanguageTag();
    }

    /** 按语言代码取单行消息。 */
    public String get(String locale, String key, Object... args) {
        Object raw = lookup(locale, key);
        String text;
        if (raw instanceof String s) {
            text = s;
        } else if (raw instanceof List<?> list && !list.isEmpty()) {
            text = String.valueOf(list.get(0));
        } else {
            text = key;
        }
        return format(text, args);
    }

    /** 按语言代码取多行消息。 */
    public List<String> getList(String locale, String key, Object... args) {
        Object raw = lookup(locale, key);
        List<String> lines;
        if (raw instanceof List<?> list) {
            lines = list.stream().map(String::valueOf).toList();
        } else if (raw instanceof String s) {
            lines = List.of(s);
        } else {
            lines = List.of(key);
        }
        return lines.stream().map(line -> format(line, args)).toList();
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private Object lookup(String locale, String key) {
        String norm = normalize(locale);
        Map<String, Object> map = localeData.get(norm);
        Object value = map != null ? map.get(key) : null;
        if (value == null && !DEFAULT_LOCALE.equals(norm)) {
            Map<String, Object> fallback = localeData.get(DEFAULT_LOCALE);
            value = fallback != null ? fallback.get(key) : null;
        }
        return value;
    }

    /**
     * 将 Minecraft 客户端语言代码归一化为内置语言之一。
     * 例如 zh_hk / zh_mo → zh_tw，en_gb → en_us，未知语言 → en_us。
     */
    static String normalize(String raw) {
        if (raw == null) {
            return DEFAULT_LOCALE;
        }
        String lower = raw.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (lower) {
            case "zh_cn", "zh" -> "zh_cn";
            case "zh_tw", "zh_hk", "zh_mo", "zh_sg" -> "zh_tw";
            case "ja_jp", "ja" -> "ja_jp";
            case "en_us", "en_gb", "en_au", "en_nz", "en_ca", "en" -> "en_us";
            default -> DEFAULT_LOCALE;
        };
    }

    /** 占位符替换：{0} {1} ...（不使用 MessageFormat，避免单引号转义问题）。 */
    private static String format(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }
}
