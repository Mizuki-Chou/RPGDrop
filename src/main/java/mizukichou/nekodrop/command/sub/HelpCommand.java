package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.RPGDropCommand;
import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.CommandSender;

public final class HelpCommand implements SubCommand {

    private final RPGDropCommand root;

    public HelpCommand(RPGDropCommand root) {
        this.root = root;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String usage() {
        return "/ndrop help";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.help";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Msg.sendRaw(sender, "command.help_header");
        root.subCommands().values().forEach(sub ->
                Msg.sendRaw(sender, "command.help_entry", root.usageOf(sender, sub), Msg.tr(sender, sub.descriptionKey())));
        Msg.sendRaw(sender, "command.help_footer");
        Msg.send(sender, "command.help_tip");
    }
}
