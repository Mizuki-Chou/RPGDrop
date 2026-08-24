package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Chance;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 概率选择页：常用预设 + 自定义输入（0.01 = 0.01%）。
 */
public final class ChancePickerGui extends Gui {

    private static final List<Double> PRESETS = List.of(100.0, 50.0, 10.0, 5.0, 1.0, 0.5, 0.1, 0.05, 0.01, 0.0);

    private final DropManager dropManager;
    private final DropRule rule;

    public ChancePickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        icon(4, Items.icon(Material.GOLD_NUGGET, t("gui.chance.current"),
                "&f" + rule.chance() + "%",
                t("gui.chance.note")));

        int slot = 10;
        for (Double preset : PRESETS) {
            boolean current = rule.chance() == preset;
            var icon = Items.icon(Material.LIME_DYE, "&a" + preset + "%",
                    current ? t("gui.chance.marker") : t("gui.chance.click_set"));
            if (current) {
                icon = Items.glow(icon);
            }
            button(slot++, icon, () -> onSet(preset));
        }

        button(16, Items.icon(Material.OAK_SIGN, t("gui.chance.custom"), t("gui.chance.custom_lore")), () ->
                manager.requestTextInput(viewer, "gui.chance.custom_prompt",
                        this::onCustom,
                        () -> navigate(new ChancePickerGui(plugin, manager, viewer, dropManager, rule))));
        button(17, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void onSet(double value) {
        rule.setChance(value);
        dropManager.ruleUpdated(rule);
        Msg.send(viewer, "gui.chance.set", value);
        navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule));
    }

    private void onCustom(String raw) {
        double value;
        try {
            value = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            Msg.send(viewer, "gui.chance.not_number", raw);
            navigate(new ChancePickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        if (!Chance.isValid(value)) {
            Msg.send(viewer, "gui.chance.range");
            navigate(new ChancePickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        onSet(value);
    }

    @Override
    protected String titleKey() {
        return "gui.chance.title";
    }

    @Override
    protected String title() {
        return t(titleKey(), rule.id());
    }

    @Override
    protected int size() {
        return 27;
    }
}
