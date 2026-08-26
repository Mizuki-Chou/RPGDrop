package mizukichou.rpgdrop;

import mizukichou.rpgdrop.command.RPGDropCommand;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.itemprovider.ItemProviderRegistry;
import mizukichou.rpgdrop.drop.itemprovider.NekoNYumeItemProvider;
import mizukichou.rpgdrop.drop.itemprovider.RPGItemsItemProvider;
import mizukichou.rpgdrop.drop.itemprovider.VanillaItemProvider;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.hook.NekoNYumeHook;
import mizukichou.rpgdrop.hook.RPGItemsHook;
import mizukichou.rpgdrop.i18n.I18n;
import mizukichou.rpgdrop.listener.EntityDeathListener;
import mizukichou.rpgdrop.listener.LotteryListener;
import mizukichou.rpgdrop.util.Log;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * RPGDrop 插件主类。
 *
 * 职责仅限"组装"：创建各组件并把它们连接起来，不包含业务逻辑。
 */
public final class RPGDropPlugin extends JavaPlugin {

    private Log log;
    private ConfigManager configManager;
    private DropManager dropManager;
    private LotteryManager lotteryManager;
    private RPGItemsHook rpgItemsHook;
    private NekoNYumeHook nekoNYumeHook;
    private GuiManager guiManager;
    private I18n i18n;
    private ItemProviderRegistry providerRegistry;

    @Override
    public void onEnable() {
        getLogger().info("Loading...");

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.log = new Log(getLogger(), () -> configManager.settings().debug());

        // ---- 国际化（i18n）：语言文件加载后消息才能发送 ----
        this.i18n = new I18n(this);
        i18n.reload();
        Msg.init(i18n);

        // ---- RPGItems 检测（softdepend：存在且版本兼容则集成，否则降级为纯原版掉落） ----
        org.bukkit.plugin.Plugin rpgItemsPlugin = configManager.settings().rpgItemsEnabled()
                ? Bukkit.getPluginManager().getPlugin("RPGItems")
                : null;
        if (rpgItemsPlugin != null && "3".equals(majorVersion(rpgItemsPlugin.getPluginMeta().getVersion()))) {
            this.rpgItemsHook = new RPGItemsHook(log);
            log.info("RPGItems detected (v" + rpgItemsPlugin.getPluginMeta().getVersion() + ").");
        } else if (rpgItemsPlugin != null) {
            this.rpgItemsHook = null;
            log.severe("RPGItems version " + rpgItemsPlugin.getPluginMeta().getVersion()
                    + " is not compatible with RPGDrop (tested against 3.x).");
            log.severe("RPGItem integration disabled - vanilla drops and lottery still work.");
        } else {
            this.rpgItemsHook = null;
            log.info("RPGItems not found.");
            log.info("RPGItem integration disabled.");
        }

        // ---- ItemProvider 注册（未来扩展 ItemsAdder / MMOItems 时在这里注册新 Provider） ----
        this.providerRegistry = new ItemProviderRegistry();
        providerRegistry.register(new VanillaItemProvider());
        if (rpgItemsHook != null) {
            providerRegistry.register(new RPGItemsItemProvider(rpgItemsHook));
        }
        // NekoNYume 是 load: POSTWORLD 插件（此刻可能尚未启用）：Hook 内部每次动态检测
        this.nekoNYumeHook = new NekoNYumeHook(log);
        providerRegistry.register(new NekoNYumeItemProvider(nekoNYumeHook));

        // ---- 掉落规则管理器 + 监听器 ----
        this.dropManager = new DropManager(this, log, configManager, providerRegistry);
        dropManager.loadAll();
        getLogger().info("Loaded " + dropManager.getRuleCount() + " drop rules.");

        Bukkit.getPluginManager().registerEvents(new EntityDeathListener(dropManager), this);
        // NekoNYume 是 load: POSTWORLD 插件，启用晚于本插件；在其真正启用时打印状态，便于诊断
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                String name = event.getPlugin().getName();
                String version = event.getPlugin().getPluginMeta().getVersion();
                if (name.equals("NekoNYume")) {
                    if (nekoNYumeHook != null) {
                        nekoNYumeHook.reset();
                    }
                    log.info("NekoNYume enabled (v" + version + ") - NekoNYume drops are now active. (API tested against 0.8.0-alpha)");
                } else if (name.equals("RPGItems")) {
                    if (rpgItemsHook != null) {
                        rpgItemsHook.reset();
                    } else if (configManager.settings().rpgItemsEnabled() && "3".equals(majorVersion(version))) {
                        // 热恢复：RPGDrop 启动时 RPGItems 不存在，运行中才安装/启用
                        rpgItemsHook = new RPGItemsHook(log);
                        providerRegistry.register(new RPGItemsItemProvider(rpgItemsHook));
                        log.info("RPGItems enabled (v" + version + ") - RPGItem drops are now active.");
                    } else {
                        log.warn("RPGItems enabled (v" + version + ") but RPGDrop integration stays disabled (incompatible major version or disabled in config).");
                    }
                }
            }

            @EventHandler
            public void onPluginDisable(PluginDisableEvent event) {
                String name = event.getPlugin().getName();
                if (name.equals("RPGItems") && rpgItemsHook != null) {
                    rpgItemsHook.reset();
                    log.warn("RPGItems disabled - RPGItem drops suspended until it is enabled again.");
                } else if (name.equals("NekoNYume") && nekoNYumeHook != null) {
                    nekoNYumeHook.reset();
                    log.warn("NekoNYume disabled - NekoNYume drops suspended until it is enabled again.");
                }
            }
        }, this);

        // ---- 抽奖规则管理器 + 监听器（Release 2 新增，与掉落完全独立） ----
        this.lotteryManager = new LotteryManager(this, log, configManager, providerRegistry, nekoNYumeHook);
        lotteryManager.loadAll();
        getLogger().info("Loaded " + lotteryManager.getRuleCount() + " lottery rules.");

        Bukkit.getPluginManager().registerEvents(new LotteryListener(this, lotteryManager), this);

        // ---- 命令 ----
        PluginCommand command = getCommand("rpgdrop");
        if (command != null) {
            RPGDropCommand executor = new RPGDropCommand(this, dropManager, lotteryManager, log);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // ---- GUI（V0.2） ----
        this.guiManager = new GuiManager(this);
        Bukkit.getPluginManager().registerEvents(guiManager, this);

        getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        // 把防抖未落盘的修改刷入磁盘，避免服务器关闭时丢失最后几次编辑
        if (dropManager != null) {
            dropManager.flush();
        }
        if (lotteryManager != null) {
            lotteryManager.flush();
        }
        getLogger().info("Disabled.");
    }

    /** 提取版本号主版本（"3.38.0-68" -> "3"）。 */
    private static String majorVersion(String version) {
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        return end == 0 ? "" : version.substring(0, end);
    }

    /** /rdrop reload 的入口：重载配置 + 掉落规则 + 语言文件 + 清空 RPGItem 缓存。 */
    public void reloadAll() {
        if (rpgItemsHook != null) {
            rpgItemsHook.clearCache();
        }
        // 先关闭所有打开的页面/输入会话，避免它们持有已被替换的旧规则对象
        guiManager.invalidateAll();
        // 先把尚未落盘的修改 flush 到磁盘，再重新读取——否则 pending 的修改会被旧配置覆盖丢失
        if (!dropManager.flush()) {
            log.severe("Reload aborted: failed to save drops.yml (see above). Pending changes are kept and will be retried on next save.");
            return;
        }
        if (!lotteryManager.flush()) {
            log.severe("Reload aborted: failed to save lotteries.yml (see above). Pending changes are kept and will be retried on next save.");
            return;
        }
        configManager.reload(); // 配置文件只读一次，避免重复 IO
        dropManager.reload();
        lotteryManager.reload();
        i18n.reload();
        log.info("Reloaded.");
    }

    /** RPGItems 集成当前是否可用（供命令层提示用）。 */
    public boolean isRpgItemsAvailable() {
        return rpgItemsHook != null;
    }

    /** RPGItems 中是否存在指定 ID 的物品（设置掉落物时的即时校验用）。 */
    public boolean isRpgItemExist(String rpgItemId) {
        return rpgItemsHook != null && rpgItemsHook.itemExists(rpgItemId);
    }

    /** 所有已加载的 RPGItem ID（tab 补全用）；RPGItems 不可用时返回空列表。 */
    public List<String> getRpgItemIds() {
        return rpgItemsHook == null ? List.of() : rpgItemsHook.getAllItemIds();
    }

    /** GUI 管理器。 */
    public GuiManager getGuiManager() {
        return guiManager;
    }

    /** 抽奖规则管理器。 */
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }

    /** 识别 ItemStack 的 RPGItem ID（抽奖触发物匹配用）；不可用时返回 empty。 */
    public Optional<String> getRpgItemId(ItemStack stack) {
        return rpgItemsHook == null ? Optional.empty() : rpgItemsHook.getItemId(stack);
    }

    /** 设置 RPGItem 时的即时校验提示：RPGItems 存在但物品不存在时警告。 */
    public void notifyRpgItemMissing(CommandSender sender, String rpgItemId) {
        if (rpgItemsHook != null && !rpgItemsHook.itemExists(rpgItemId)) {
            Msg.send(sender, "command.item_rpgitem_missing", rpgItemId);
        }
    }

    /** 国际化管理器。 */
    public I18n getI18n() {
        return i18n;
    }

    /**
     * 通过 RPGItems API 实时生成 RPGItem 预览（GUI 用）；
     * RPGItems 不可用或物品不存在时返回 empty。
     */
    public NekoNYumeHook nekoNYumeHook() {
        return nekoNYumeHook;
    }

    /** NekoNYume 是否已启用（动态检测，可晚于本插件启用）。 */
    public boolean isNekoNYumeAvailable() {
        return nekoNYumeHook != null && nekoNYumeHook.isAvailable();
    }

    public Optional<ItemStack> previewRpgItem(String rpgItemId) {
        return rpgItemsHook == null ? Optional.empty() : rpgItemsHook.createItemStack(rpgItemId);
    }
}
