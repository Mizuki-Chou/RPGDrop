package mizukichou.nekodrop.gui.page;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.gui.Gui;
import mizukichou.nekodrop.gui.GuiManager;
import mizukichou.nekodrop.util.Items;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界选择页：切换已加载世界；规则中未加载的世界以红色显示（可移除）；支持手动添加。
 */
public final class WorldPickerGui extends Gui {

    private final DropManager dropManager;
    private final DropRule rule;

    public WorldPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        int slot = 0;

        // 已加载世界（最多占 45 格，防止覆盖底部按钮）
        for (World world : Bukkit.getWorlds()) {
            if (slot >= 45) {
                break;
            }
            String name = world.getName();
            boolean selected = rule.worlds().contains(name);
            ItemStack icon = Items.icon(selected ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                    (selected ? "&a✔ " : "&7") + name,
                    selected ? t("gui.world.selected") : t("gui.world.unselected"));
            if (selected) {
                icon = Items.glow(icon);
            }
            button(slot++, icon, () -> toggle(name));
        }

        // 规则中存在但未加载的世界（如服务器重启后未加载，或名字写错）
        List<String> unloaded = new ArrayList<>(rule.worlds());
        Bukkit.getWorlds().forEach(w -> unloaded.remove(w.getName()));
        for (String name : unloaded) {
            button(slot++, Items.icon(Material.RED_WOOL, "&c⚠ " + name,
                    t("gui.world.unloaded"),
                    t("gui.world.remove_hint")), () -> {
                rule.removeWorld(name);
                dropManager.ruleUpdated(rule);
                render();
            });
        }

        icon(49, Items.icon(Material.GRASS_BLOCK, t("gui.world.count"),
                t("gui.world.count_value", rule.worlds().size()),
                rule.worlds().isEmpty() ? t("gui.world.count_empty")
                        : "&f" + String.join(", ", rule.worlds().stream().sorted().toList())));

        button(46, Items.icon(Material.REDSTONE_BLOCK, t("gui.world.clear")), () -> {
            for (String name : List.copyOf(rule.worlds())) {
                rule.removeWorld(name);
            }
            dropManager.ruleUpdated(rule);
            render();
        });
        button(47, Items.icon(Material.OAK_SIGN, t("gui.world.add"), t("gui.world.add_lore")), () ->
                manager.requestTextInput(viewer, "gui.world.add_prompt",
                        this::onAdd,
                        () -> navigate(new WorldPickerGui(plugin, manager, viewer, dropManager, rule))));
        button(48, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void toggle(String name) {
        if (rule.worlds().contains(name)) {
            rule.removeWorld(name);
        } else {
            rule.addWorld(name);
        }
        dropManager.ruleUpdated(rule);
        render();
    }

    private void onAdd(String raw) {
        String input = raw.trim();
        if (input.isEmpty()) {
            Msg.send(viewer, "gui.world.name_empty");
            navigate(new WorldPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        // 已加载的世界按真实名（大小写）归一
        World loaded = Bukkit.getWorld(input);
        String name = loaded != null ? loaded.getName() : input;
        rule.addWorld(name);
        dropManager.ruleUpdated(rule);
        if (Bukkit.getWorld(name) == null) {
            Msg.send(viewer, "gui.world.not_loaded", name);
        } else {
            Msg.send(viewer, "gui.world.added", name);
        }
        navigate(new WorldPickerGui(plugin, manager, viewer, dropManager, rule));
    }

    @Override
    protected String titleKey() {
        return "gui.world.title";
    }

    @Override
    protected int size() {
        return 54;
    }
}
