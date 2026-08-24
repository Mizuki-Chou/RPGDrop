package mizukichou.nekodrop.gui;

import mizukichou.nekodrop.RPGDropPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI 页面基类（每个实例对应一个玩家正在观看的页面）。
 *
 * 安全设计（硬性原则）：
 *   - 所有点击事件一律 cancel，只有注册了按钮的格子才执行动作；
 *   - Shift Click / Number Key / Double Click / Drag / Drop / Swap 全部无效；
 *   - 玩家无法移动、取出或放入任何物品。
 *
 * 文本全部来自语言文件，按玩家客户端语言（Adventure Identity.LOCALE）自动选择。
 */
public abstract class Gui implements InventoryHolder {

    protected final RPGDropPlugin plugin;
    protected final GuiManager manager;
    protected final Player viewer;

    private final Map<Integer, Runnable> actions = new HashMap<>();
    private Inventory inventory;

    protected Gui(RPGDropPlugin plugin, GuiManager manager, Player viewer) {
        this.plugin = plugin;
        this.manager = manager;
        this.viewer = viewer;
    }

    /** 渲染并打开。 */
    public final void open() {
        render();
        viewer.openInventory(inventory);
    }

    /** 重新渲染（保留当前库存实例，刷新内容）。 */
    protected final void render() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size(), Component.text(title()));
        }
        inventory.clear();
        actions.clear();
        fill();
    }

    // ------------------------------------------------------------------
    // i18n 快捷方法（按当前 viewer 的客户端语言）
    // ------------------------------------------------------------------

    /** 单行翻译。 */
    protected final String t(String key, Object... args) {
        return plugin.getI18n().get(viewer, key, args);
    }

    /** 多行翻译（用于物品 lore）。 */
    protected final List<String> tl(String key, Object... args) {
        return plugin.getI18n().getList(viewer, key, args);
    }

    // ------------------------------------------------------------------
    // 供子类使用的渲染助手
    // ------------------------------------------------------------------

    /** 放置可点击按钮。 */
    protected final void button(int slot, ItemStack icon, Runnable action) {
        inventory.setItem(slot, icon);
        actions.put(slot, action);
    }

    /** 放置纯展示图标（不可点击）。 */
    protected final void icon(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    /**
     * 页面跳转（延迟一拍执行，避免在事件回调中直接开新库存引发问题）。
     * 若玩家已离线则静默取消，防止 openInventory 抛异常。
     */
    protected final void navigate(Gui next) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (viewer.isOnline()) {
                manager.open(viewer, next);
            }
        });
    }

    /** 关闭当前页面。 */
    protected final void close() {
        plugin.getServer().getScheduler().runTask(plugin, () -> viewer.closeInventory());
    }

    // ------------------------------------------------------------------
    // 事件处理（由 GuiManager 路由调用）
    // ------------------------------------------------------------------

    /** 点击处理：一律 cancel，仅执行按钮动作。 */
    public final void click(InventoryClickEvent event) {
        event.setCancelled(true);
        Runnable action = actions.get(event.getRawSlot());
        if (action != null) {
            action.run();
        }
    }

    /** 拖拽处理：一律 cancel，防止任何物品被放入/取出。 */
    public void drag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    /** 页面关闭回调（默认空）。 */
    public void onClose() {
    }

    // ------------------------------------------------------------------
    // 子类必须实现
    // ------------------------------------------------------------------

    /** 填充页面内容（inventory 已被清空，actions 已重置）。 */
    protected abstract void fill();

    /** 标题文本（默认按 titleKey 翻译；需要带参数时可覆盖）。 */
    protected String title() {
        return t(titleKey());
    }

    /** 标题的语言文件键。 */
    protected abstract String titleKey();

    protected abstract int size();

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
