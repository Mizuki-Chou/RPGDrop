package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.util.Msg;
import mizukichou.rpgdrop.util.RuleIds;
import org.bukkit.command.CommandSender;

public final class CreateCommand implements SubCommand {

    
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
        return "/rdrop create <id>";
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
        if (!RuleIds.isValid(id)) {
            Msg.send(sender, "command.invalid_id");
            return;
        }
        if (dropManager.isLimitReached()) {
            Msg.send(sender, "command.rule_limit", ConfigManager.MAX_DROP_RULES);
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
