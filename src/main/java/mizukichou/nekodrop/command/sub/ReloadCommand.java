package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.util.Msg;
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
        return "/ndrop reload";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.reload";
    }

    @Override
    public String permission() {
        return "nekodrop.reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadAll();
        Msg.send(sender, "command.reload_done");
    }
}
