package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.command.TabUtil;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class DeleteCommand implements SubCommand {

    private final DropManager dropManager;

    public DeleteCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public String usage() {
        return "/rdrop delete <id>";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.delete";
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DropRule rule = dropManager.getRule(args[0]);
        if (rule == null) {
            Msg.send(sender, "command.rule_not_found", args[0]);
            return;
        }
        dropManager.deleteRule(rule.id());
        Msg.send(sender, "command.rule_deleted", rule.id());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(TabUtil.ruleIds(dropManager), args[0]);
        }
        return List.of();
    }
}
