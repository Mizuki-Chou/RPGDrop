package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import mizukichou.rpgdrop.util.RuleIds;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 掉落规则列表（原 R1 主菜单，R2 起作为 hub 的子页面）。
 */
public final class DropListGui extends Gui {

    private static final int ITEMS_PER_PAGE = 45;

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    private int page;

    public DropListGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                       DropManager dropManager, LotteryManager lotteryManager) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
    }

    @Override
    protected void fill() {
        List<DropRule> rules = List.copyOf(dropManager.getAllRules());

        int totalPages = Math.max(1, (int) Math.ceil(rules.size() / (double) ITEMS_PER_PAGE));
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int from = page * ITEMS_PER_PAGE;
        int to = Math.min(rules.size(), from + ITEMS_PER_PAGE);

        for (int i = from; i < to; i++) {
            DropRule rule = rules.get(i);
            int slot = i - from;
            String[] lore = {
                    t("gui.main.rule_enabled", rule.isEnabled() ? t("yes") : t("no")),
                    t("gui.main.rule_entities", rule.entities().size()),
                    t("gui.main.rule_worlds", rule.worlds().isEmpty() ? "-"
                            : String.join(", ", rule.worlds().stream().sorted().toList())),
                    t("gui.main.rule_chance", rule.chance()),
                    t("gui.main.rule_amount", rule.minAmount(), rule.maxAmount()),
                    t("gui.main.rule_item", describeItem(rule)),
                    "",
                    t("gui.main.click_edit")
            };
            ItemStack item = rule.isEnabled()
                    ? Items.icon(Material.ENCHANTED_BOOK, "&d" + rule.id(), lore)
                    : Items.icon(Material.BOOK, "&7" + rule.id(), lore);
            button(slot, item,
                    () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule)));
        }

        if (rules.isEmpty()) {
            icon(22, Items.icon(Material.OAK_SIGN, t("gui.main.empty"), t("gui.main.empty_lore")));
        }

        button(45, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new HubGui(plugin, manager, viewer, dropManager, lotteryManager)));
        if (page > 0) {
            button(46, Items.icon(Material.ARROW, t("gui.main.prev")), () -> {
                page--;
                render();
            });
        }
        if (page + 1 < totalPages) {
            button(53, Items.icon(Material.ARROW, t("gui.main.next")), () -> {
                page++;
                render();
            });
        }
        button(49, Items.glow(Items.icon(Material.EMERALD, t("gui.main.create"))),
                () -> manager.requestTextInput(viewer, "gui.main.create_prompt",
                        this::onCreateRule,
                        () -> navigate(new DropListGui(plugin, manager, viewer, dropManager, lotteryManager))));
        button(48, Items.icon(Material.BARRIER, t("close")), this::close);
        icon(50, Items.icon(Material.NETHER_STAR, "&dRPGDrop", t("gui.main.rule_count", rules.size())));
    }

    private void onCreateRule(String id) {
        if (!RuleIds.isValid(id)) {
            Msg.send(viewer, "command.invalid_id");
            navigate(new DropListGui(plugin, manager, viewer, dropManager, lotteryManager));
            return;
        }
        if (dropManager.isLimitReached()) {
            Msg.send(viewer, "command.rule_limit", ConfigManager.MAX_DROP_RULES);
            navigate(new DropListGui(plugin, manager, viewer, dropManager, lotteryManager));
            return;
        }
        if (!dropManager.createRule(id)) {
            Msg.send(viewer, "command.rule_already_exists", id);
            navigate(new DropListGui(plugin, manager, viewer, dropManager, lotteryManager));
            return;
        }
        Msg.send(viewer, "command.rule_created", id);
        DropRule rule = dropManager.getRule(id);
        navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule));
    }

    private String describeItem(DropRule rule) {
        if (rule.item() instanceof RPGItemDropItem rpgItem) {
            return "&fRPGITEM(" + rpgItem.rpgItemId() + ")";
        }
        if (rule.item() instanceof VanillaDropItem vanilla) {
            return "&fVANILLA(" + vanilla.material() + ")";
        }
        if (rule.item() instanceof NekoNYumeDropItem nyn) {
            return "&fNYN(" + nyn.kind() + ":" + nyn.value() + ")";
        }
        return "&c" + t("not_set");
    }

    @Override
    protected String titleKey() {
        return "gui.main.title";
    }

    @Override
    protected int size() {
        return 54;
    }
}
