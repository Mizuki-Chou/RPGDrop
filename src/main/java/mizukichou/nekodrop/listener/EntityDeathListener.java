package mizukichou.nekodrop.listener;

import mizukichou.nekodrop.drop.DropManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 实体死亡监听器。
 *
 * 硬性原则：Listener 只负责转发事件，不承担任何业务逻辑。
 * 匹配、概率、掉落等全部交给 {@link DropManager}。
 */
public final class EntityDeathListener implements Listener {

    private final DropManager dropManager;

    public EntityDeathListener(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        // 玩家死亡不参与掉落规则
        if (entity.getType() == EntityType.PLAYER) {
            return;
        }
        dropManager.processDeath(event);
    }
}
