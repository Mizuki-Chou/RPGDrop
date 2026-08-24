package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 主菜单：规则列表 + 创建按钮（分页）。
 */
public final class MainMenuGui extends Gui {

    private static final int ITEMS_PER_PAGE = 45;

    private final DropManager dropManager;
    private int page;

    public MainMenuGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
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
            //启用 =附魔书（亮色），禁用 =普通书（灰色）
            ItemStack item = rule.isEnabled()
                    ? Items.icon(Material.ENCHANTED_BOOK, "&d" + rule.id(), lore)
                    : Items.icon(Material.BOOK, "&7" + rule.id(), lore);
            button(slot, item,
                    () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)));
        }

        if (rules.isEmpty()) {
            icon(22, Items.icon(Material.OAK_SIGN, t("gui.main.empty"), t("gui.main.empty_lore")));
        }

        // 底部导航
        if (page > 0) {
            button(45, Items.icon(Material.ARROW, t("gui.main.prev")), () -> {
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
                        () -> navigate(new MainMenuGui(plugin, manager, viewer, dropManager))));
        button(48, Items.icon(Material.BARRIER, t("close")), this::close);
        icon(50, Items.icon(Material.NETHER_STAR, "&dRPGDrop", t("gui.main.rule_count", rules.size())));
    }

    private void onCreateRule(String id) {
        if (!id.matches("[A-Za-z0-9_-]{1,32}")) {
            Msg.send(viewer, "command.invalid_id");
            navigate(new MainMenuGui(plugin, manager, viewer, dropManager));
            return;
        }
        if (!dropManager.createRule(id)) {
            Msg.send(viewer, "command.rule_already_exists", id);
            navigate(new MainMenuGui(plugin, manager, viewer, dropManager));
            return;
        }
        Msg.send(viewer, "command.rule_created", id);
        DropRule rule = dropManager.getRule(id);
        navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule));
    }

    private String describeItem(DropRule rule) {
        if (rule.item() instanceof RPGItemDropItem rpgItem) {
            return "&fRPGITEM(" + rpgItem.rpgItemId() + ")";
        }
        if (rule.item() instanceof VanillaDropItem vanilla) {
            return "&fVANILLA(" + vanilla.material() + ")";
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
