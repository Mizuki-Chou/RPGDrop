package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
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
 * 抽奖规则列表（分页）。
 */
public final class LotteryListGui extends Gui {

    private static final int ITEMS_PER_PAGE = 45;

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    private int page;

    public LotteryListGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                          DropManager dropManager, LotteryManager lotteryManager) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
    }

    @Override
    protected void fill() {
        List<LotteryRule> rules = List.copyOf(lotteryManager.getAllRules());

        int totalPages = Math.max(1, (int) Math.ceil(rules.size() / (double) ITEMS_PER_PAGE));
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int from = page * ITEMS_PER_PAGE;
        int to = Math.min(rules.size(), from + ITEMS_PER_PAGE);

        for (int i = from; i < to; i++) {
            LotteryRule rule = rules.get(i);
            int slot = i - from;
            String triggerDesc = rule.trigger() == null ? t("not_set") : "&f" + lotteryManager.describe(rule.trigger());
            String[] lore = {
                    t("gui.lottery_list.rule_lore_trigger", triggerDesc),
                    t("gui.lottery_list.rule_lore_prizes", rule.prizes().size()),
                    t("gui.lottery_list.rule_lore_enabled", rule.isEnabled() ? t("yes") : t("no")),
                    "",
                    t("gui.lottery_list.click_edit")
            };
            ItemStack item = rule.isEnabled()
                    ? Items.icon(Material.END_CRYSTAL, "&d" + rule.id(), lore)
                    : Items.icon(Material.ECHO_SHARD, "&7" + rule.id(), lore);
            button(slot, item,
                    () -> navigate(new LotteryEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule)));
        }

        if (rules.isEmpty()) {
            icon(22, Items.icon(Material.OAK_SIGN, t("gui.lottery_list.empty"), t("gui.lottery_list.empty_lore")));
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
        button(49, Items.glow(Items.icon(Material.EMERALD, t("gui.lottery_list.create"))),
                () -> manager.requestTextInput(viewer, "gui.main.create_prompt",
                        this::onCreateRule,
                        () -> navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager))));
        button(48, Items.icon(Material.BARRIER, t("close")), this::close);
        icon(50, Items.icon(Material.NETHER_STAR, "&dRPGDrop", t("gui.main.rule_count", rules.size())));
    }

    private void onCreateRule(String id) {
        if (!RuleIds.isValid(id)) {
            Msg.send(viewer, "command.invalid_id");
            navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager));
            return;
        }
        if (lotteryManager.isLimitReached()) {
            Msg.send(viewer, "command.lottery_limit", ConfigManager.MAX_LOTTERY_RULES);
            navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager));
            return;
        }
        if (!lotteryManager.createRule(id)) {
            Msg.send(viewer, "command.rule_already_exists", id);
            navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager));
            return;
        }
        Msg.send(viewer, "command.lottery_created", id);
        LotteryRule rule = lotteryManager.getRule(id);
        navigate(new LotteryEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule));
    }

    @Override
    protected String titleKey() {
        return "gui.lottery_list.title";
    }

    @Override
    protected int size() {
        return 54;
    }
}
