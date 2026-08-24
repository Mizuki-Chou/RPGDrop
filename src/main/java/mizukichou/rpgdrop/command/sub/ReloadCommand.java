package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.command.CommandSender;

public final class ReloadCommand implements SubCommand {

    private final RPGDropPlugin plugin;

    public ReloadCommand(RPGDropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String usage() {
        return "/rdrop reload";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.reload";
    }

    @Override
    public String permission() {
        return "rpgdrop.reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadAll();
        Msg.send(sender, "command.reload_done");
    }
}
