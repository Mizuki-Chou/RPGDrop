package mizukichou.rpgdrop.gui;

import mizukichou.rpgdrop.RPGDropPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI 管理器：负责把库存事件路由到对应的页面，
 * 并集中实现防物品偷窃（所有事件 cancel + 只执行注册按钮）。
 *
 * 文本输入走聊天通道（Paper 26.2 无插件层铁砧 API，详见 {@link TextInputSession}）。
 */
public final class GuiManager implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final RPGDropPlugin plugin;

    /** 打开中的页面（按 Holder 身份路由）。 */
    private final Map<InventoryHolder, Gui> route = new IdentityHashMap<>();

    /** 代次：每次 invalidateAll（重载）自增；页面持有创建时的代次，用于作废重载前的排队导航任务。 */
    private long generation = 0;

    /** 等待聊天输入的会话（每玩家至多一个）。 */
    private final Map<Player, TextInputSession> chatInputs = new ConcurrentHashMap<>();

    public GuiManager(RPGDropPlugin plugin) {
        this.plugin = plugin;
    }

    /** 打开一个 GUI 页面（会取消该玩家未完成的聊天输入）。 */
    /** 当前代次。 */
    public long generation() {
        return generation;
    }

    public void open(Player player, Gui gui) {
        cancelPendingChatInput(player);
        route.put(gui, gui);
        gui.open();
    }

    /** 请求聊天文本输入（promptKey 为语言文件键）。确认时回调 onConfirm；取消/超时回调 onCancel。 */
    public void requestTextInput(Player player, String promptKey, Consumer<String> onConfirm, Runnable onCancel) {
        requestTextInput(player, promptKey, new Object[0], onConfirm, onCancel);
    }

    /** 请求聊天文本输入，prompt 支持占位符参数。 */
    public void requestTextInput(Player player, String promptKey, Object promptArg, Consumer<String> onConfirm, Runnable onCancel) {
        requestTextInput(player, promptKey, new Object[]{promptArg}, onConfirm, onCancel);
    }

    /** 请求聊天文本输入，prompt 支持多个占位符参数。 */
    public void requestTextInput(Player player, String promptKey, Object[] promptArgs,
                                 Consumer<String> onConfirm, Runnable onCancel) {
        cancelPendingChatInput(player);
        TextInputSession session = new TextInputSession(plugin, this, player, plugin.getI18n(),
                promptKey, promptArgs, onConfirm, onCancel);
        chatInputs.put(player, session);
        // 延迟一拍：先关掉当前 GUI，再发聊天提示
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            session.open();
        });
    }

    /** 输入会话结束（确认或取消时调用）。 */
    void finishChatInput(TextInputSession session) {
        chatInputs.remove(session.getPlayer(), session);
    }

    private void cancelPendingChatInput(Player player) {
        TextInputSession session = chatInputs.remove(player);
        if (session != null) {
            session.cancelQuietly();
        }
    }

    /**
     * 插件重载（/rdrop reload）时调用：
     * 关闭所有打开的页面、取消所有聊天输入。
     * 原因：reload 后内存中的规则对象已全部替换为新的实例，
     * 旧页面持有的旧引用再点击保存会导致修改丢失。
     */
    public void invalidateAll() {
        for (TextInputSession session : new ArrayList<>(chatInputs.values())) {
            session.cancelQuietly();
        }
        chatInputs.clear();
        for (Gui gui : new ArrayList<>(route.values())) {
            if (gui.viewer.isOnline()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // 只关闭"仍停留在该页面"的玩家，避免误关之后新打开的页面（stale task）
                    InventoryView view = gui.viewer.getOpenInventory();
                    if (view != null && view.getTopInventory() != null && view.getTopInventory().getHolder() == gui) {
                        gui.viewer.closeInventory();
                    }
                });
            }
        }
        generation++; // 重载即换代：所有旧页面/旧导航任务作废
        route.clear();
    }

    // ------------------------------------------------------------------
    // 事件路由
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getView() == null || event.getView().getTopInventory() == null) {
            return;
        }
        Gui gui = route.get(event.getView().getTopInventory().getHolder());
        if (gui != null) {
            gui.click(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView() == null || event.getView().getTopInventory() == null) {
            return;
        }
        Gui gui = route.get(event.getView().getTopInventory().getHolder());
        if (gui != null) {
            gui.drag(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getView() == null || event.getView().getTopInventory() == null) {
            return;
        }
        Gui removed = route.remove(event.getView().getTopInventory().getHolder());
        if (removed != null) {
            removed.onClose();
        }
    }

    /** 聊天输入拦截：命中等待输入的玩家时 cancel 消息并切回主线程处理。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        TextInputSession session = chatInputs.get(event.getPlayer());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        // 纯文本序列化：玩家输入的样式/颜色码一律丢弃，只取字符（& 等符号不会被解析）
        String message = PLAIN.serialize(event.message());
        plugin.getServer().getScheduler().runTask(plugin, () -> session.onChat(message));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        cancelPendingChatInput(event.getPlayer());
        Iterator<Map.Entry<InventoryHolder, Gui>> it = route.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().viewer.equals(event.getPlayer())) {
                it.remove();
            }
        }
    }
}
