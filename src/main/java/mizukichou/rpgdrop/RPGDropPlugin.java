package mizukichou.rpgdrop;

import mizukichou.rpgdrop.command.RPGDropCommand;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.itemprovider.ItemProviderRegistry;
import mizukichou.rpgdrop.drop.itemprovider.RPGItemsItemProvider;
import mizukichou.rpgdrop.drop.itemprovider.VanillaItemProvider;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.hook.RPGItemsHook;
import mizukichou.rpgdrop.i18n.I18n;
import mizukichou.rpgdrop.listener.EntityDeathListener;
import mizukichou.rpgdrop.util.Log;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
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
    private RPGItemsHook rpgItemsHook;
    private GuiManager guiManager;
    private I18n i18n;

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

        // ---- RPGItems 检测（softdepend：存在则集成，不存在则降级为纯原版掉落） ----
        boolean rpgItemsEnabled = configManager.settings().rpgItemsEnabled()
                && Bukkit.getPluginManager().isPluginEnabled("RPGItems");
        if (rpgItemsEnabled) {
            this.rpgItemsHook = new RPGItemsHook(log, configManager.settings().cacheRpgItems());
            log.info("RPGItems detected.");
        } else {
            this.rpgItemsHook = null;
            log.info("RPGItems not found.");
            log.info("RPGItem integration disabled.");
        }

        // ---- ItemProvider 注册（未来扩展 ItemsAdder / MMOItems 时在这里注册新 Provider） ----
        ItemProviderRegistry providerRegistry = new ItemProviderRegistry();
        providerRegistry.register(new VanillaItemProvider());
        if (rpgItemsHook != null) {
            providerRegistry.register(new RPGItemsItemProvider(rpgItemsHook));
        }

        // ---- 掉落规则管理器 + 监听器 ----
        this.dropManager = new DropManager(log, configManager, providerRegistry);
        dropManager.loadAll();
        getLogger().info("Loaded " + dropManager.getRuleCount() + " drop rules.");

        Bukkit.getPluginManager().registerEvents(new EntityDeathListener(dropManager), this);

        // ---- 命令 ----
        PluginCommand command = getCommand("rpgdrop");
        if (command != null) {
            RPGDropCommand executor = new RPGDropCommand(this, dropManager, log);
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
        getLogger().info("Disabled.");
    }

    /** /rdrop reload 的入口：重载配置 + 掉落规则 + 语言文件 + 清空 RPGItem 缓存。 */
    public void reloadAll() {
        if (rpgItemsHook != null) {
            rpgItemsHook.clearCache();
        }
        // 先关闭所有打开的页面/输入会话，避免它们持有已被替换的旧规则对象
        guiManager.invalidateAll();
        dropManager.reload();
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

    /** 国际化管理器。 */
    public I18n getI18n() {
        return i18n;
    }

    /**
     * 通过 RPGItems API 实时生成 RPGItem 预览（GUI 用）；
     * RPGItems 不可用或物品不存在时返回 empty。
     */
    public Optional<ItemStack> previewRpgItem(String rpgItemId) {
        return rpgItemsHook == null ? Optional.empty() : rpgItemsHook.createItemStack(rpgItemId);
    }
}
