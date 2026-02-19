package me.son14ka.mineChess;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class ViewSpace {
    private final Player player;
    private final List<Integer> entities = new ArrayList<>();
    private static final AtomicInteger ENTITY_ID_SEQ = new AtomicInteger(2_000_000);
    private static final Quaternion4f IDENTITY_QUATERNION = new Quaternion4f(0f, 0f, 0f, 1f);

    public ViewSpace(JavaPlugin plugin, Player player) {
        this.player = player;
    }

    public int spawnBlockDisplay(Location location, Material material, Vector3f scale) {
        int entityId = nextEntityId();
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.BLOCK_DISPLAY,
                SpigotConversionUtil.fromBukkitLocation(location),
                location.getYaw(),
                0,
                null
        );
        send(spawn);

        WrappedBlockState blockState = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
        List<EntityData<?>> metadata = Arrays.asList(
                new EntityData<>(12, EntityDataTypes.VECTOR3F, scale),
                new EntityData<>(13, EntityDataTypes.QUATERNION, IDENTITY_QUATERNION),
                new EntityData<>(14, EntityDataTypes.QUATERNION, IDENTITY_QUATERNION),
                new EntityData<>(23, EntityDataTypes.BLOCK_STATE, blockState.getGlobalId())
        );
        send(new WrapperPlayServerEntityMetadata(entityId, metadata));
        entities.add(entityId);
        return entityId;
    }

    public int spawnItemDisplay(Location location, ItemStack itemStack, Vector3f scale) {
        int entityId = nextEntityId();
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.ITEM_DISPLAY,
                SpigotConversionUtil.fromBukkitLocation(location),
                location.getYaw(),
                0,
                null
        );
        send(spawn);

        List<EntityData<?>> metadata = Arrays.asList(
                new EntityData<>(12, EntityDataTypes.VECTOR3F, scale),
                new EntityData<>(13, EntityDataTypes.QUATERNION, IDENTITY_QUATERNION),
                new EntityData<>(14, EntityDataTypes.QUATERNION, IDENTITY_QUATERNION),
                new EntityData<>(23, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(itemStack)),
                new EntityData<>(24, EntityDataTypes.BYTE, (byte) 0)
        );
        send(new WrapperPlayServerEntityMetadata(entityId, metadata));
        entities.add(entityId);
        return entityId;
    }

    public void destroy(int entityId) {
        if (entities.remove((Integer) entityId)) {
            send(new WrapperPlayServerDestroyEntities(entityId));
        }
    }

    public void announce() {
        // PacketEvents wrappers are sent immediately.
    }

    public void close() {
        if (entities.isEmpty()) {
            return;
        }
        int[] ids = entities.stream().mapToInt(Integer::intValue).toArray();
        send(new WrapperPlayServerDestroyEntities(ids));
        entities.clear();
    }

    private void send(PacketWrapper<?> wrapper) {
        if (!player.isOnline() || PacketEvents.getAPI() == null) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }

    private static int nextEntityId() {
        return ENTITY_ID_SEQ.incrementAndGet();
    }
}
