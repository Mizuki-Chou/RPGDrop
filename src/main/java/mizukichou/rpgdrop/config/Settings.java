package mizukichou.rpgdrop.config;

/**
 * config.yml 的强类型快照（每次 reload 重建）。
 */
public record Settings(
        boolean debug,
        boolean rpgItemsEnabled,
        boolean keepVanillaDrops,
        boolean cacheRpgItems
) {
}
