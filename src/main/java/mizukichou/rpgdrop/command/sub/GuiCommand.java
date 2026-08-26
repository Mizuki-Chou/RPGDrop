package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.gui.page.HubGui;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rdrop gui —— 打开 RPGDrop 主入口（掉落规则 / 抽奖规则二选一）。
 */
public final class GuiCommand implements SubCommand {

    private final RPGDropPlugin plugin;
    private final DropManager dropManager;
    private final LotteryManager lotteryManager;

    public GuiCommand(RPGDropPlugin plugin, DropManager dropManager, LotteryManager lotteryManager) {
        this.plugin = plugin;
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String usage() {
        return "/rdrop gui";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.gui";
    }

    @Override
    public String permission() {
        return "rpgdrop.gui";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "command.player_only");
            return;
        }
        if (!plugin.isRpgItemsAvailable()) {
            Msg.send(player, "command.gui_rpgitems_missing");
        }
        plugin.getGuiManager().open(player,
                new HubGui(plugin, plugin.getGuiManager(), player, dropManager, lotteryManager));
    }
}
