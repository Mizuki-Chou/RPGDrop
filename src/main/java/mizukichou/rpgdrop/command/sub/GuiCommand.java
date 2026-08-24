package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.gui.page.MainMenuGui;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rdrop gui —— 打开 RPGDrop 图形化管理器。
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
        plugin.getGuiManager().open(player, new MainMenuGui(plugin, plugin.getGuiManager(), player, dropManager));
    }
}
