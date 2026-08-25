package me.zimzaza4.geyserutils.spigot.api;

import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.common.camera.instruction.Instruction;
import me.zimzaza4.geyserutils.common.channel.GeyserUtilsChannels;
import me.zimzaza4.geyserutils.common.packet.*;
import me.zimzaza4.geyserutils.common.particle.CustomParticle;
import me.zimzaza4.geyserutils.common.util.Pos;
import me.zimzaza4.geyserutils.spigot.GeyserUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerUtils {


    public static void shakeCamera(Player player, float intensity, float duration, int type) {
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(new CameraShakeCustomPayloadPacket(intensity, duration, type)));
    }

    public static void playEntityAnimation(Player player, Animation animation, Integer entityId) {
        playEntityAnimation(player, animation, new ArrayList<>(entityId));
    }

    public static void playEntityAnimation(Player player, Animation animation, Entity... entityList) {
        List<Integer> idList = new ArrayList<>();
        for (Entity entity : entityList) {
            idList.add(entity.getEntityId());
        }

        playEntityAnimation(player, animation, idList);
    }

    public static void playEntityAnimation(Player player, Animation animation, List<Integer> entityList) {
        AnimateEntityCustomPayloadPacket packet = new AnimateEntityCustomPayloadPacket();
        packet.parseFromAnimation(animation);
        packet.setEntityJavaIds(entityList);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendCameraInstruction(Player player, Instruction instruction) {
        CameraInstructionCustomPayloadPacket packet = new CameraInstructionCustomPayloadPacket();
        packet.setInstruction(instruction);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));

    }

    public static void sendCustomParticle(Player player, Location location, CustomParticle particle) {
        CustomParticleEffectPayloadPacket packet = new CustomParticleEffectPayloadPacket();
        packet.setParticle(particle);
        packet.setPos(new Pos((float) location.getX(), (float) location.getY(), (float) location.getZ()));
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendCustomSkin(Player player, Entity entity, String skin) {
        CustomSkinPayloadPacket skinPayloadPacket = new CustomSkinPayloadPacket();
        skinPayloadPacket.setSkinId(skin);
        skinPayloadPacket.setEntityId(entity.getEntityId());
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(skinPayloadPacket));

    }

    public static void sendCustomHitBox(Player player, Entity entity, float height, float width) {
        EntityUtils.sendCustomHitBox(player, entity.getEntityId(), height, width);
    }

    public static void sendCustomScale(Player player, Entity entity, float scale) {
        EntityUtils.sendCustomScale(player, entity.getEntityId(), scale);
    }

    public static void sendCustomColor(Player player, Entity entity, Color color) {
        EntityUtils.sendCustomColor(player, entity.getEntityId(), color);
    }

    public static void setCustomEntity(Player player, int id, String def) {
        EntityUtils.setCustomEntity(player, id, def);
    }

    // (yes I'm aware it's "horrible" code), also this aint player packets at all lmao
    // right, so this part needs to be refactored xD
    // the plugin didn't have this much functionality in its earliest days (it even just have camera shakes),
    // so I didn't think too much about it

    public static void registerProperty(Player player, Entity entity, String identifier, Class<?> type) {
        EntityUtils.registerProperty(player, entity.getEntityId(), identifier, type);
    }

    public static void sendBoolProperty(Player player, Entity entity, String identifier, Boolean value) {
        EntityUtils.sendBoolProperty(player, entity.getEntityId(), identifier, value);
    }

    public static void sendBoolProperties(Player player, Entity entity, Map<String, Boolean> bundle) {
        EntityUtils.sendBoolProperties(player, entity.getEntityId(), bundle);
    }

    public static void sendFloatProperty(Player player, Entity entity, String identifier, Float value) {
        EntityUtils.sendFloatProperty(player, entity.getEntityId(), identifier, value);
    }

    public static void sendFloatProperties(Player player, Entity entity, Map<String, Float> bundle) {
        EntityUtils.sendFloatProperties(player, entity.getEntityId(), bundle);
    }

    public static void sendIntProperty(Player player, Entity entity, String identifier, Integer value) {
        EntityUtils.sendIntProperty(player, entity.getEntityId(), identifier, value);
    }

    public static void sendIntProperties(Player player, Entity entity, Map<String, Integer> bundle) {
        EntityUtils.sendIntProperties(player, entity.getEntityId(), bundle);
    }
}
