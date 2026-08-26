package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Chance;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * 奖品概率选择页：预设 + 自定义输入。选中后通过回调返回。
 */
public final class LotteryChanceGui extends Gui {

    private static final double[] PRESETS = {100, 50, 10, 5, 1, 0.5, 0.1, 0.05, 0.01};

    private final Consumer<Double> onPick;
    private final Runnable onBack;

    public LotteryChanceGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                            Consumer<Double> onPick, Runnable onBack) {
        super(plugin, manager, viewer);
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void fill() {
        for (int i = 0; i < PRESETS.length; i++) {
            final double preset = PRESETS[i];
            button(i, Items.icon(Material.GOLD_NUGGET, "&e" + preset + "%",
                            t("gui.chance.click_set")),
                    () -> onPick.accept(preset));
        }

        icon(13, Items.icon(Material.CLOCK, t("gui.chance.note")));

        button(15, Items.glow(Items.icon(Material.EMERALD, t("gui.chance.custom"),
                        t("gui.chance.custom_lore"))),
                () -> manager.requestTextInput(viewer, "gui.chance.custom_prompt",
                        text -> {
                            try {
                                double value = Double.parseDouble(text.trim());
                                if (!Chance.isValid(value)) {
                                    Msg.send(viewer, "gui.chance.range");
                                    navigate(new LotteryChanceGui(plugin, manager, viewer, onPick, onBack));
                                    return;
                                }
                                onPick.accept(value);
                            } catch (NumberFormatException e) {
                                Msg.send(viewer, "gui.chance.not_number", text);
                                navigate(new LotteryChanceGui(plugin, manager, viewer, onPick, onBack));
                            }
                        },
                        () -> navigate(new LotteryChanceGui(plugin, manager, viewer, onPick, onBack))));

        button(16, Items.icon(Material.RED_WOOL, t("gui.lottery_add_page.cancel")), onBack::run);
    }

    @Override
    protected String titleKey() {
        return "gui.lottery_weight_page.title";
    }

    @Override
    protected int size() {
        return 27;
    }
}
