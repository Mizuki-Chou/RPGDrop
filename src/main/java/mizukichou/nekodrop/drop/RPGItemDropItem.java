package mizukichou.nekodrop.drop;

/**
 * RPGItem 掉落描述：只保存 RPGItem ID。
 *
 * 每次掉落时通过 RPGItems API 实时生成最新版 ItemStack：
 * RPGItem 在游戏里被修改（伤害/Lore/Power）后，掉落系统无需重新录入。
 */
public record RPGItemDropItem(String rpgItemId) implements DropItem {

    public RPGItemDropItem {
        if (rpgItemId == null || rpgItemId.isBlank()) {
            throw new IllegalArgumentException("rpgItemId must not be empty");
        }
    }

    @Override
    public ItemType type() {
        return ItemType.RPGITEM;
    }
}
