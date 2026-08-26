package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 主入口（Release 2）：二选一 —— 掉落规则 / 抽奖规则。
 */
public final class HubGui extends Gui {

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;

    public HubGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                  DropManager dropManager, LotteryManager lotteryManager) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
    }

    @Override
    protected void fill() {
        button(11, Items.icon(Material.CHEST, t("gui.hub.drop"),
                        t("gui.hub.drop_lore_1"), t("gui.hub.drop_lore_2")),
                () -> navigate(new DropListGui(plugin, manager, viewer, dropManager, lotteryManager)));

        button(15, Items.glow(Items.icon(Material.END_CRYSTAL, t("gui.hub.lottery"),
                        t("gui.hub.lottery_lore_1"), t("gui.hub.lottery_lore_2"))),
                () -> navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager)));

        button(13, Items.icon(Material.BARRIER, t("close")), this::close);
    }

    @Override
    protected String titleKey() {
        return "gui.hub.title";
    }

    @Override
    protected int size() {
        return 27;
    }
}
