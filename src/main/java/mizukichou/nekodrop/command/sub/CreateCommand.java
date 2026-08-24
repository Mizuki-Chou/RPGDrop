package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.CommandSender;

public final class CreateCommand implements SubCommand {

    private static final String ID_PATTERN = "[A-Za-z0-9_-]+";

    private final DropManager dropManager;

    public CreateCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public String usage() {
        return "/ndrop create <id>";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.create";
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String id = args[0];
        if (!id.matches(ID_PATTERN)) {
            Msg.send(sender, "command.invalid_id");
            return;
        }
        if (dropManager.getRule(id) != null) {
            Msg.send(sender, "command.rule_already_exists", id);
            return;
        }
        dropManager.createRule(id);
        Msg.send(sender, "command.rule_created", id);
        Msg.sendList(sender, "command.create_hints", id);
    }
}
