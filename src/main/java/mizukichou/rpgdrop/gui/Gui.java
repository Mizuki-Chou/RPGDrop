package mizukichou.rpgdrop.gui;
import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

    /** 页面创建时的代次：reload（invalidateAll）换代后，本页面的排队导航任务作废。 */
    private final long generation;
    private Inventory inventory;

    protected Gui(RPGDropPlugin plugin, GuiManager manager, Player viewer) {
        this.generation = manager.generation();
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
        if (slot < 0 || slot >= size()) {
            plugin.getLogger().warning("Button slot " + slot + " out of range in " + getClass().getSimpleName());
            return;
        }
        inventory.setItem(slot, icon);
        actions.put(slot, action);
    }

    /** 放置纯展示图标（不可点击）。 */
    protected final void icon(int slot, ItemStack item) {
        if (slot < 0 || slot >= size()) {
            return;
        }
        inventory.setItem(slot, item);
    }

    /**
     * 页面跳转（延迟一拍执行，避免在事件回调中直接开新库存引发问题）。
     * 若玩家已离线则静默取消，防止 openInventory 抛异常。
     */
    protected final void navigate(Gui next) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // stale 防护（跨页回调安全版）：页面被重载/关闭后会从 route 移除。
            // 注意不能要求 holder == this——navigate 常由"发起页"的字段回调触发，
            // 而玩家此刻停留在子页面（如点选页），发起页与当前页并非同一实例。
            if (!viewer.isOnline()) {
                return;
            }
            // stale 防护（代次机制）：仅 reload（invalidateAll）换代后作废排队任务。
            // 不做"玩家当前页面"检查——聊天输入流程中页面已主动关闭、玩家在聊天界面，
            // 输入完成后的回调导航必须仍能打开新页面。
            if (generation != manager.generation()) {
                return;
            }
            manager.open(viewer, next);
        });
    }

    /** 关闭当前页面。 */
    protected final void close() {
        plugin.getServer().getScheduler().runTask(plugin, () -> viewer.closeInventory());
    }

    // ------------------------------------------------------------------
    // 事件处理（由 GuiManager 路由调用）
    // ------------------------------------------------------------------

    /** 点击处理：一律 cancel，仅执行按钮动作；动作异常时兜底，不让异常击穿事件处理。 */
    public final void click(InventoryClickEvent event) {
        event.setCancelled(true);
        // 权限在执行修改时重查：进入 GUI 后权限被撤销（如 LuckPerms）的玩家不能再操作
        if (!viewer.hasPermission("rpgdrop.gui")) {
            close();
            return;
        }
        Runnable action = actions.get(event.getRawSlot());
        if (action == null) {
            return;
        }
        // 动作一次性消费：navigate 是下一 tick 才跳转，消费掉可防止双击导致同一动作执行两次
        //（例如奖品添加页双击 = 添加两个奖品；删除确认双击 = 删掉两个奖品）
        actions.remove(event.getRawSlot());
        try {
            action.run();
        } catch (Exception | LinkageError e) {
            plugin.getLogger().log(Level.SEVERE, "GUI action failed in " + getClass().getSimpleName(), e);
            Msg.send(viewer, "command.execution_error");
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
