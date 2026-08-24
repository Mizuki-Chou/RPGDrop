package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.command.TabUtil;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.util.Msg;
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
        return "/ndrop delete <id>";
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
