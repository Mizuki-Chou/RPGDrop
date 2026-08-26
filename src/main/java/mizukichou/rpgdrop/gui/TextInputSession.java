package mizukichou.rpgdrop.gui;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.i18n.I18n;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/**
 * 文本输入会话（聊天输入）。
 *
 * 为什么不用铁砧：Paper 26.2 起 Bukkit.createInventory(ANVIL) 返回普通容器
 * （CraftInventoryCustom），没有改名行为，PrepareAnvilEvent 不再触发——
 * 经典"虚拟铁砧输入"方案在 26.2 已失效（ClassCastException / 无法输入）。
 * 因此改用聊天输入：跨版本稳定、无物品偷窃风险、实现简单。
 *
 * 行为：
 *   - 请求输入后关闭 GUI，玩家在聊天栏直接输入文本；
 *   - 输入 "cancel" 或 60 秒无输入视为取消（回调 onCancel）；
 *   - 玩家的输入消息会被拦截（cancel），不会广播到聊天栏；
 *   - 输入事件为异步，处理时统一切回主线程。
 */
public final class TextInputSession {

    private static final long TIMEOUT_TICKS = 20L * 60;

    private final GuiManager manager;
    private final Player player;
    private final I18n i18n;
    private final String promptKey;
    private final Object[] promptArgs;
    private final Consumer<String> onConfirm;
    private final Runnable onCancel;

    private final BukkitTask timeoutTask;
    private boolean done;
    private boolean cancelled;

    public TextInputSession(RPGDropPlugin plugin, GuiManager manager, Player player, I18n i18n,
                            String promptKey, Object[] promptArgs, Consumer<String> onConfirm, Runnable onCancel) {
        this.manager = manager;
        this.player = player;
        this.i18n = i18n;
        this.promptKey = promptKey;
        this.promptArgs = promptArgs;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        // 超时：静默取消（不触发 onCancel，避免 60 秒后突然弹 GUI 打断玩家）
        this.timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin,
                this::cancelQuietly, TIMEOUT_TICKS);
    }

    /** 发出输入提示（需在主线程调用）。 */
    void open() {
        if (cancelled) {
            return; // 会话已被 reload/退出取消，不再打扰玩家（stale queued task 防护）
        }
        Msg.send(player, "gui.input.prompt");
        Msg.send(player, promptKey, promptArgs);
        Msg.send(player, "gui.input.cancel_hint");
    }

    /** 收到玩家聊天输入（已在主线程）。 */
    void onChat(String message) {
        String text = message.trim();
        if (text.equalsIgnoreCase("cancel")) {
            complete(null);
            return;
        }
        complete(text);
    }

    /** 完成或取消：null 视为取消。 */
    private void complete(String result) {
        if (done) {
            return;
        }
        done = true;
        timeoutTask.cancel();
        manager.finishChatInput(this);
        if (result == null) {
            onCancel.run();
        } else {
            onConfirm.accept(result);
        }
    }

    /** 玩家退出服务器、打开新 GUI 或输入超时：静默清理，不触发任何回调。 */
    public void cancelQuietly() {
        if (!done) {
            done = true;
            cancelled = true;
            timeoutTask.cancel();
            manager.finishChatInput(this);
        }
    }

    public Player getPlayer() {
        return player;
    }

}
