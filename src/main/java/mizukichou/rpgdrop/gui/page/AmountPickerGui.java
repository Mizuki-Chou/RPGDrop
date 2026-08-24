package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Amounts;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 数量设置页：min/max 步进调节 + 预设 + 自定义。
 */
public final class AmountPickerGui extends Gui {

    private final DropManager dropManager;
    private final DropRule rule;

    public AmountPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        icon(4, Items.icon(Material.PAPER, t("gui.amount.current"),
                "&f" + rule.minAmount() + " ~ " + rule.maxAmount(),
                t("gui.amount.note")));

        // min 步进行
        icon(9, Items.icon(Material.NAME_TAG, "&7min"));
        button(10, Items.icon(Material.RED_DYE, "&c-10"), () -> adjustMin(-10));
        button(11, Items.icon(Material.RED_DYE, "&c-1"), () -> adjustMin(-1));
        icon(12, Items.icon(Material.PAPER, "&f" + rule.minAmount()));
        button(13, Items.icon(Material.LIME_DYE, "&a+1"), () -> adjustMin(1));
        button(14, Items.icon(Material.LIME_DYE, "&a+10"), () -> adjustMin(10));

        // max 步进行
        icon(18, Items.icon(Material.NAME_TAG, "&7max"));
        button(19, Items.icon(Material.RED_DYE, "&c-10"), () -> adjustMax(-10));
        button(20, Items.icon(Material.RED_DYE, "&c-1"), () -> adjustMax(-1));
        icon(21, Items.icon(Material.PAPER, "&f" + rule.maxAmount()));
        button(22, Items.icon(Material.LIME_DYE, "&a+1"), () -> adjustMax(1));
        button(23, Items.icon(Material.LIME_DYE, "&a+10"), () -> adjustMax(10));

        // 预设
        int slot = 28;
        for (int[] preset : new int[][]{{1, 1}, {1, 2}, {1, 3}, {1, 5}, {2, 3}, {3, 5}}) {
            boolean current = rule.minAmount() == preset[0] && rule.maxAmount() == preset[1];
            var icon = Items.icon(Material.LIME_DYE, "&a" + preset[0] + " ~ " + preset[1],
                    current ? t("gui.amount.marker") : t("gui.amount.click_set"));
            if (current) {
                icon = Items.glow(icon);
            }
            button(slot++, icon, () -> onSet(preset[0], preset[1]));
        }

        button(34, Items.icon(Material.OAK_SIGN, t("gui.amount.custom"), t("gui.amount.custom_lore")), () ->
                manager.requestTextInput(viewer, "gui.amount.prompt_min",
                        this::onCustomMin,
                        () -> navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule))));

        button(35, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void adjustMin(int delta) {
        int min = Math.max(1, rule.minAmount() + delta);
        onSet(min, Math.max(min, rule.maxAmount()));
    }

    private void adjustMax(int delta) {
        int max = Math.max(rule.minAmount(), rule.maxAmount() + delta);
        onSet(rule.minAmount(), max);
    }

    private void onSet(int min, int max) {
        rule.setAmount(min, max);
        dropManager.ruleUpdated(rule);
        render();
    }

    private void onCustomMin(String raw) {
        int min;
        try {
            min = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            Msg.send(viewer, "gui.amount.not_int", raw);
            navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        if (min < 1) {
            Msg.send(viewer, "gui.amount.min_invalid");
            navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        // min 合法（>=1），后续 max 校验交给 Amounts.isValid
        manager.requestTextInput(viewer, "gui.amount.prompt_max", min,
                maxRaw -> onCustomMax(min, maxRaw),
                () -> navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void onCustomMax(int min, String raw) {
        int max;
        try {
            max = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            Msg.send(viewer, "gui.amount.not_int", raw);
            navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        if (!Amounts.isValid(min, max)) {
            Msg.send(viewer, "gui.amount.max_invalid", min);
            navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        onSet(min, max);
        Msg.send(viewer, "gui.amount.set", min, max);
        navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule));
    }

    @Override
    protected String titleKey() {
        return "gui.amount.title";
    }

    @Override
    protected String title() {
        return t(titleKey(), rule.id());
    }

    @Override
    protected int size() {
        return 45;
    }
}
