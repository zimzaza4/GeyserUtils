/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package me.zimzaza4.geyserutils.geyser.replace;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.GeyserEntityType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.FallingBlockEntity;
import org.geysermc.geyser.entity.type.FishingHookEntity;
import org.geysermc.geyser.entity.type.HangingEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.util.EnvironmentUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.FallingBlockData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ProjectileData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.WardenData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

import static me.zimzaza4.geyserutils.geyser.GeyserUtils.CUSTOM_ENTITIES;
import static me.zimzaza4.geyserutils.geyser.GeyserUtils.LOADED_ENTITY_DEFINITIONS;

public final class JavaAddEntityTranslatorReplace extends PacketTranslator<ClientboundAddEntityPacket> {
    private static final boolean SHOW_PLAYER_LIST_LOGS =
            Boolean.parseBoolean(System.getProperty("Geyser.ShowPlayerListLogs", "true"));

    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        GeyserEntityType entityType = GeyserEntityType.of(packet.getType());
        if (entityType.isUnregistered()) {
            session.getGeyser().getLogger().warning("Received unregistered entity type " + entityType + " in add entity packet");
            return;
        }

        EntityTypeDefinition<?> definition = Registries.JAVA_ENTITY_TYPES.get(entityType);
        if (definition == null) {
            session.getGeyser().getLogger().debug("Could not find an entity definition for add entity packet " + packet);
            return;
        }

        String customIdentifier = null;
        var customEntityCache = CUSTOM_ENTITIES.get(session);
        if (customEntityCache != null) {
            customIdentifier = customEntityCache.getIfPresent(packet.getEntityId());
        }

        if (packet.getType() == EntityType.AREA_EFFECT_CLOUD && customIdentifier != null) {
            GeyserEntityType interactionType = GeyserEntityType.of(EntityType.INTERACTION);
            EntityTypeDefinition<?> interactionDefinition = Registries.JAVA_ENTITY_TYPES.get(interactionType);
            if (interactionDefinition != null && interactionDefinition.factory() != null) {
                definition = interactionDefinition;
            } else {
                session.getGeyser().getLogger().warning(
                        "Could not resolve the INTERACTION entity definition for custom area-effect-cloud entity "
                                + packet.getEntityId());
            }
        }

        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = packet.getMovement().toFloat();
        float yaw = packet.getYaw();
        float pitch = packet.getPitch();
        float headYaw = packet.getHeadYaw();

        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, definition, packet);

        if (customIdentifier != null) {
            BedrockEntityDefinition customDefinition = LOADED_ENTITY_DEFINITIONS.get(customIdentifier);
            if (customDefinition != null) {
                context.bedrockEntityDefinition(customDefinition);
            }
        }

        if (packet.getType() == EntityType.PLAYER) {
            PlayerEntity entity;
            if (packet.getUuid().equals(session.getPlayerEntity().uuid())) {
                // Server is sending a fake version of the current player
                entity = new PlayerEntity(
                        context,
                        session.getPlayerEntity().getUsername(),
                        session.getPlayerEntity().getTextures()
                );
            } else {
                entity = session.getEntityCache().getPlayerEntity(packet.getUuid());
                if (entity == null) {
                    if (SHOW_PLAYER_LIST_LOGS) {
                        GeyserImpl.getInstance().getLogger().error(
                                GeyserLocale.getLocaleStringLog("geyser.entity.player.failed_list", packet.getUuid())
                        );
                    }
                    return;
                }

                entity.setEntityId(packet.getEntityId());
                entity.setPosition(position);
                entity.setYaw(yaw);
                entity.setPitch(pitch);
                entity.setHeadYaw(headYaw);
                entity.setMotion(motion);
            }

            entity.sendPlayer();
            if (!EnvironmentUtils.IS_UNIT_TESTING) {
                SkinManager.requestAndHandleSkinAndCape(entity, session, null);
            }
            return;
        }

        if (!context.callServerSpawnEvent()) {
            GeyserImpl.getInstance().getLogger().debug(
                    session,
                    "Cancelled entity spawn (%s) at (%s)",
                    entityType.identifier(),
                    context.position()
            );
            return;
        }

        Entity entity;
        if (packet.getType() == EntityType.FALLING_BLOCK) {
            entity = new FallingBlockEntity(context, ((FallingBlockData) packet.getData()).getId());
        } else if (packet.getType() == EntityType.FISHING_BOBBER) {
            int ownerEntityId = ((ProjectileData) packet.getData()).getOwnerId();
            Entity owner = session.getEntityCache().getEntityByJavaId(ownerEntityId);
            if (owner instanceof PlayerEntity playerOwner) {
                entity = new FishingHookEntity(context, playerOwner);
            } else {
                return;
            }
        } else {
            if (definition.factory() == null) {
                session.getGeyser().getLogger().warning("Entity definition has no factory for add entity packet " + packet);
                return;
            }

            entity = definition.factory().create(context);

            if (entity instanceof HangingEntity hanging) {
                hanging.setDirection((Direction) packet.getData());
            }
        }

        if (packet.getType() == EntityType.WARDEN) {
            WardenData wardenData = (WardenData) packet.getData();
            if (wardenData.isEmerging()) {
                entity.setPose(Pose.EMERGING);
            }
        }

        if (context.consumers() != null) {
            context.consumers().forEach(consumer -> consumer.accept(entity));
        }

        session.getEntityCache().spawnEntity(entity);
    }
}
