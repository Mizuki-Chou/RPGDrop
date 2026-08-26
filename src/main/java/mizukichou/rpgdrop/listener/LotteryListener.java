package mizukichou.rpgdrop.listener;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.util.Log;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * 抽奖触发监听（Release 2 新增）：手持触发物右键 -> 消耗1个 -> 抽奖。
 * 只处理抽奖，与 R1 的掉落监听完全独立。
 */
public final class LotteryListener implements Listener {

    private final RPGDropPlugin plugin;
    private final LotteryManager lotteryManager;

    public LotteryListener(RPGDropPlugin plugin, LotteryManager lotteryManager) {
        this.plugin = plugin;
        this.lotteryManager = lotteryManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // PlayerInteractEvent 对 RIGHT_CLICK_AIR/RIGHT_CLICK_BLOCK 会按手分别触发：
        // 只处理主手事件，避免一次右键触发两次抽奖（双手都拿触发物时）。
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();

        ItemStack main = player.getInventory().getItemInMainHand();
        Optional<LotteryRule> matched = lotteryManager.matchTrigger(main);
        if (matched.isEmpty()) {
            return;
        }

        // 纯消耗交互：取消原版行为（防止方块被放置、食物被吃等）
        event.setCancelled(true);

        // 先抽奖（未就绪/奖品被拦截时不消耗并提示玩家），确认落地后才消耗 1 个触发物
        if (!lotteryManager.tryRoll(player, matched.get())) {
            return;
        }
        if (main.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            main.setAmount(main.getAmount() - 1);
            player.getInventory().setItemInMainHand(main);
        }
    }
}
