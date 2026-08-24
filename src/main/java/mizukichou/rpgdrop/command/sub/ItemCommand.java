package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.command.TabUtil;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.util.Materials;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /rdrop item <id> rpgitem <RPGItem ID>
 * /rdrop item <id> vanilla <material>
 */
public final class ItemCommand implements SubCommand {

    private static final List<String> KINDS = List.of("rpgitem", "vanilla");
    private static final List<String> MATERIALS =
            Arrays.stream(Material.values()).map(Enum::name).sorted().toList();

    private final RPGDropPlugin plugin;
    private final DropManager dropManager;

    public ItemCommand(RPGDropPlugin plugin, DropManager dropManager) {
        this.plugin = plugin;
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "item";
    }

    @Override
    public String usageKey() {
        return "command.usage.item";
    }

    @Override
    public String usage() {
        return "/rdrop item <id> rpgitem <RPGItem ID> | vanilla <material>";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.item";
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DropRule rule = dropManager.getRule(args[0]);
        if (rule == null) {
            Msg.send(sender, "command.rule_not_found", args[0]);
            return;
        }

        String kind = args[1].toLowerCase(Locale.ROOT);
        switch (kind) {
            case "rpgitem" -> {
                if (args.length < 3) {
                    Msg.send(sender, "command.item_usage_rpgitem", rule.id());
                    return;
                }
                String rpgId = args[2];
                if (rpgId.length() > RPGItemDropItem.MAX_ID_LENGTH) {
                    Msg.send(sender, "command.item_rpgitem_too_long", RPGItemDropItem.MAX_ID_LENGTH);
                    return;
                }
                rule.setItem(new RPGItemDropItem(rpgId));
                dropManager.ruleUpdated(rule);
                Msg.send(sender, "command.item_set_rpgitem", rule.id(), rpgId);
                if (!plugin.isRpgItemsAvailable()) {
                    Msg.send(sender, "command.item_rpgitems_warning");
                } else if (!plugin.isRpgItemExist(rpgId)) {
                    Msg.send(sender, "command.item_rpgitem_missing", rpgId);
                }
            }
            case "vanilla" -> {
                if (args.length < 3) {
                    Msg.send(sender, "command.item_usage_vanilla", rule.id());
                    return;
                }
                Optional<Material> material = Materials.parse(args[2]);
                if (material.isEmpty()) {
                    Msg.send(sender, "command.item_unknown_material", args[2]);
                    return;
                }
                rule.setItem(new VanillaDropItem(material.get()));
                dropManager.ruleUpdated(rule);
                Msg.send(sender, "command.item_set_vanilla", rule.id(), material.get());
            }
            default -> Msg.send(sender, "command.item_unknown_kind", kind);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(TabUtil.ruleIds(dropManager), args[0]);
        }
        if (args.length == 2) {
            return TabUtil.filter(KINDS, args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("rpgitem")) {
            return TabUtil.filter(plugin.getRpgItemIds(), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("vanilla")) {
            return TabUtil.filter(MATERIALS, args[2]);
        }
        return List.of();
    }
}
