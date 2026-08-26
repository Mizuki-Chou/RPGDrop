package mizukichou.rpgdrop.drop;

import java.util.Locale;
import java.util.Optional;

/**
 * 掉落物来源类型。
 * 现有：VANILLA（原版）、RPGITEM（RPGItems）、NEKONYUME（NekoNYume，可选依赖）。
 */
public enum ItemType {
    VANILLA,
    RPGITEM,
    NEKONYUME;

    public static Optional<ItemType> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ItemType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
