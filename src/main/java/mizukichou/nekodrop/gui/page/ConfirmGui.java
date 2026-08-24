package mizukichou.nekodrop.gui.page;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.gui.Gui;
import mizukichou.nekodrop.gui.GuiManager;
import mizukichou.nekodrop.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 通用确认页：确认 / 取消。
 */
public final class ConfirmGui extends Gui {

    private final String messageKey;
    private final Object[] messageArgs;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                      String messageKey, Object[] messageArgs, Runnable onConfirm, Runnable onCancel) {
        super(plugin, manager, viewer);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public ConfirmGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                      String messageKey, Runnable onConfirm, Runnable onCancel) {
        this(plugin, manager, viewer, messageKey, new Object[0], onConfirm, onCancel);
    }

    public ConfirmGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                      String messageKey, Object arg0, Runnable onConfirm, Runnable onCancel) {
        this(plugin, manager, viewer, messageKey, new Object[]{arg0}, onConfirm, onCancel);
    }

    @Override
    protected void fill() {
        icon(4, Items.icon(Material.OAK_SIGN, "&e" + t(messageKey, messageArgs), t("gui.confirm.lore")));

        button(11, Items.glow(Items.icon(Material.LIME_WOOL, t("gui.confirm.yes"))), onConfirm);
        button(15, Items.icon(Material.RED_WOOL, t("gui.confirm.no")), onCancel);
    }

    @Override
    protected String titleKey() {
        return "gui.confirm.title";
    }

    @Override
    protected int size() {
        return 27;
    }
}
