package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.gui.page.MainMenuGui;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ndrop gui —— 打开 RPGDrop 图形化管理器。
 */
public final class GuiCommand implements SubCommand {

    private final RPGDropPlugin plugin;
    private final DropManager dropManager;

    public GuiCommand(RPGDropPlugin plugin, DropManager dropManager) {
        this.plugin = plugin;
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String usage() {
        return "/ndrop gui";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.gui";
    }

    @Override
    public String permission() {
        return "nekodrop.gui";
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
        plugin.getGuiManager().open(player, new MainMenuGui(plugin, plugin.getGuiManager(), player, dropManager));
    }
}
