package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Materials;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 原版物品选择页：分类页 + 分类物品网格（分页）+ 搜索。
 * 分类名通过语言文件键 gui.material.cat.&lt;id&gt; 翻译，材料名保持英文（与命令参数一致）。
 */
public final class MaterialPickerGui extends Gui {

    private static final int ITEMS_PER_PAGE = 45;

    /** 只读分类表（不可变视图，防止意外修改）。 */
    private static final Map<String, List<Material>> CATEGORIES;

    static {
        Map<String, List<Material>> categories = new LinkedHashMap<>();

        categories.put("common", List.of(
                Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
                Material.NETHERITE_INGOT, Material.NETHER_STAR, Material.DIAMOND_SWORD, Material.BOW,
                Material.ARROW, Material.SHIELD, Material.GOLDEN_APPLE, Material.ENDER_PEARL,
                Material.BLAZE_ROD, Material.GUNPOWDER, Material.ROTTEN_FLESH, Material.BONE,
                Material.STRING, Material.SPIDER_EYE, Material.SLIME_BALL, Material.TOTEM_OF_UNDYING));
        categories.put("ores", List.of(
                Material.DIAMOND, Material.EMERALD, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.COPPER_INGOT, Material.LAPIS_LAZULI, Material.REDSTONE, Material.COAL,
                Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP, Material.QUARTZ, Material.AMETHYST_SHARD,
                Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER, Material.ANCIENT_DEBRIS));
        categories.put("mob_drops", List.of(
                Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.SPIDER_EYE,
                Material.GUNPOWDER, Material.SLIME_BALL, Material.ENDER_PEARL, Material.BLAZE_ROD,
                Material.GHAST_TEAR, Material.MAGMA_CREAM, Material.PHANTOM_MEMBRANE, Material.SHULKER_SHELL,
                Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS, Material.NAUTILUS_SHELL,
                Material.WITHER_SKELETON_SKULL, Material.DRAGON_BREATH, Material.ECHO_SHARD, Material.TURTLE_SCUTE));
        categories.put("food", List.of(
                Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.COOKED_BEEF,
                Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN, Material.BREAD, Material.CARROT,
                Material.GOLDEN_CARROT, Material.BAKED_POTATO, Material.CAKE, Material.COOKIE,
                Material.PUMPKIN_PIE, Material.HONEY_BOTTLE, Material.SWEET_BERRIES));
        categories.put("tools", List.of(
                Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_PICKAXE,
                Material.NETHERITE_PICKAXE, Material.DIAMOND_AXE, Material.BOW, Material.CROSSBOW,
                Material.TRIDENT, Material.SHIELD, Material.FISHING_ROD, Material.ELYTRA,
                Material.TOTEM_OF_UNDYING, Material.SADDLE, Material.NAME_TAG, Material.LEAD));
        categories.put("rare", List.of(
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.ELYTRA, Material.TRIDENT,
                Material.TOTEM_OF_UNDYING, Material.NETHERITE_INGOT, Material.WITHER_SKELETON_SKULL,
                Material.HEART_OF_THE_SEA, Material.BEACON, Material.CONDUIT, Material.DRAGON_HEAD,
                Material.MUSIC_DISC_PIGSTEP, Material.ENCHANTED_GOLDEN_APPLE, Material.EXPERIENCE_BOTTLE));
        CATEGORIES = Collections.unmodifiableMap(categories);
    }

    private final DropManager dropManager;
    private final DropRule rule;
    private String category; // null = category selection page
    private int page;

    public MaterialPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    private MaterialPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                              DropManager dropManager, DropRule rule, String category, int page) {
        this(plugin, manager, viewer, dropManager, rule);
        this.category = category;
        this.page = page;
    }

    @Override
    protected void fill() {
        if (category == null) {
            fillCategories();
        } else {
            fillCategory();
        }
    }

    private void fillCategories() {
        int slot = 10;
        for (Map.Entry<String, List<Material>> entry : CATEGORIES.entrySet()) {
            Material representative = entry.getValue().get(0);
            button(slot++, Items.icon(representative, "&a" + t("gui.material.cat." + entry.getKey()),
                            t("gui.material.count", entry.getValue().size()),
                            t("gui.material.click_enter")),
                    () -> navigate(new MaterialPickerGui(plugin, manager, viewer, dropManager, rule, entry.getKey(), 0)));
        }
        button(22, Items.icon(Material.OAK_SIGN, t("gui.material.search"), t("gui.material.search_lore")), () ->
                manager.requestTextInput(viewer, "gui.material.search_prompt",
                        this::onSearch,
                        () -> navigate(new MaterialPickerGui(plugin, manager, viewer, dropManager, rule))));
        button(26, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void fillCategory() {
        List<Material> materials = CATEGORIES.get(category);
        int totalPages = Math.max(1, (int) Math.ceil(materials.size() / (double) ITEMS_PER_PAGE));
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int from = page * ITEMS_PER_PAGE;
        int to = Math.min(materials.size(), from + ITEMS_PER_PAGE);

        for (int i = from; i < to; i++) {
            Material material = materials.get(i);
            int slot = i - from;
            button(slot, new ItemStack(material), () -> onPick(material));
        }

        if (page > 0) {
            button(45, Items.icon(Material.ARROW, t("gui.material.prev")), () -> {
                page--;
                render();
            });
        }
        if (page + 1 < totalPages) {
            button(53, Items.icon(Material.ARROW, t("gui.material.next")), () -> {
                page++;
                render();
            });
        }
        button(48, Items.icon(Material.ARROW, t("gui.material.back_categories")),
                () -> navigate(new MaterialPickerGui(plugin, manager, viewer, dropManager, rule)));
        icon(49, Items.icon(Material.BOOK, t("gui.material.category", t("gui.material.cat." + category)),
                t("gui.material.category_total", materials.size())));
    }

    private void onPick(Material material) {
        rule.setItem(new VanillaDropItem(material));
        dropManager.ruleUpdated(rule);
        Msg.send(viewer, "gui.material.set", rule.id(), material);
        navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule));
    }

    private void onSearch(String raw) {
        var material = Materials.parse(raw);
        if (material.isEmpty()) {
            Msg.send(viewer, "gui.material.unknown", raw);
            navigate(new MaterialPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        onPick(material.get());
    }

    @Override
    protected String titleKey() {
        return category == null ? "gui.material.title" : "gui.material.title_category";
    }

    @Override
    protected String title() {
        return category == null
                ? t(titleKey(), rule.id())
                : t(titleKey(), t("gui.material.cat." + category), rule.id());
    }

    @Override
    protected int size() {
        return 54;
    }
}
