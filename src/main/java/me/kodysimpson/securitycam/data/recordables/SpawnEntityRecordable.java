package me.kodysimpson.securitycam.data.recordables;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.MojangAPIUtil;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import me.kodysimpson.securitycam.data.Recordable;
import me.kodysimpson.securitycam.data.Replay;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@BsonDiscriminator(key = "type", value = "SpawnEntity")
public class SpawnEntityRecordable extends Recordable {

    private EntityType entityType;
    private Location location;
    private UUID bukkitEntityId;
    private String playerName;

    public SpawnEntityRecordable(Entity entity) {
        this.entityType = entity.getType();
        this.location = entity.getLocation();
        this.bukkitEntityId = entity.getUniqueId();
        if (this.entityType == EntityType.PLAYER){
            this.playerName = entity.getName();
        }
    }

    @Override
    public void replay(Replay replay, User user) throws Exception {
        if (entityType == EntityType.PLAYER){

            List<TextureProperty> skin = MojangAPIUtil.requestPlayerTextureProperties(MojangAPIUtil.requestPlayerUUID(playerName));

            NPC npc = new NPC(new UserProfile(UUID.randomUUID(), playerName, skin),
                    SpigotReflectionUtil.generateEntityId(),
                    GameMode.SURVIVAL,
                    null,
                    null,
                    null,
                    null);
            npc.setLocation(new com.github.retrooper.packetevents.protocol.world.Location(location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch()));

            npc.spawn(PacketEvents.getAPI().getPlayerManager().getChannel(replay.getViewer()));

            replay.getSpawnedEntities().put(bukkitEntityId, npc.getId());
        }else{

            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Field field = entityClass.getDeclaredField("d");
            field.setAccessible(true);
            AtomicInteger ENTITY_COUNTER = (AtomicInteger) field.get(null);
            int entityId = ENTITY_COUNTER.incrementAndGet();

            com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType1 = EntityTypes.getByName(entityType.getKey().toString());

            WrapperPlayServerSpawnEntity spawnEntityPacket = new WrapperPlayServerSpawnEntity(
                    entityId, UUID.randomUUID(),
                    entityType1, SpigotConversionUtil.fromBukkitLocation(location),
                    0, 0, null);

            user.sendPacket(spawnEntityPacket);
            replay.getSpawnedEntities().put(bukkitEntityId, entityId);
        }
    }
}
