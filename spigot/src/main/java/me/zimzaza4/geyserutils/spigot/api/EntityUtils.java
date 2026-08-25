package me.zimzaza4.geyserutils.spigot.api;

import me.zimzaza4.geyserutils.common.channel.GeyserUtilsChannels;
import me.zimzaza4.geyserutils.common.packet.*;
import me.zimzaza4.geyserutils.spigot.GeyserUtils;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class EntityUtils {

    public static void sendCustomHitBox(Player player, int id, float height, float width) {
        CustomEntityDataPacket packet = new CustomEntityDataPacket();
        packet.setEntityId(id);
        packet.setWidth(width);
        packet.setHeight(height);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));

    }

    public static void sendCustomScale(Player player, int id, float scale) {
        CustomEntityDataPacket packet = new CustomEntityDataPacket();
        packet.setEntityId(id);
        packet.setScale(scale);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));

    }

    public static void sendCustomColor(Player player, int id, Color color) {
        CustomEntityDataPacket packet = new CustomEntityDataPacket();
        packet.setEntityId(id);
        packet.setColor(color.getRGB());
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));

    }

    public static void sendVariant(Player player, int id, int variant) {
        CustomEntityDataPacket packet = new CustomEntityDataPacket();
        packet.setEntityId(id);
        packet.setVariant(variant);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));

    }

    public static void setCustomEntity(Player player, int entityId, String def) {
        CustomEntityPacket packet = new CustomEntityPacket(entityId, def);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));

    }

    // (yes I'm aware it's "horrible" code), also this aint player packets at all lmao
    // right, so this part needs to be refactored xD
    // the plugin didn't have this much functionality in its earliest days (it even just have camera shakes),
    // so I didn't think too much about it

    public static void registerProperty(Player player, int id, String identifier, Class<?> type) {
        EntityPropertyRegisterPacket packet = new EntityPropertyRegisterPacket();
        packet.setEntityId(id);
        packet.setIdentifier(identifier);
        packet.setType(type);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendBoolProperty(Player player, int id, String identifier, Boolean value) {
        EntityPropertyPacket<Boolean> packet = new EntityPropertyPacket<>();
        packet.setEntityId(id);
        packet.setIdentifier(identifier);
        packet.setValue(value);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendBoolProperties(Player player, int id, Map<String, Boolean> bundle) {
        BundlePacket packet = new BundlePacket();
        bundle.forEach((identifier, value) -> {
            EntityPropertyPacket<Boolean> propertyPacket = new EntityPropertyPacket<>();
            propertyPacket.setEntityId(id);
            propertyPacket.setIdentifier(identifier);
            propertyPacket.setValue(value);
            packet.addPacket(propertyPacket);
        });

        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendFloatProperty(Player player, int id, String identifier, Float value) {
        EntityPropertyPacket<Float> packet = new EntityPropertyPacket<>();
        packet.setEntityId(id);
        packet.setIdentifier(identifier);
        packet.setValue(value);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendFloatProperties(Player player, int id, Map<String, Float> bundle) {
        BundlePacket packet = new BundlePacket();
        bundle.forEach((identifier, value) -> {
            EntityPropertyPacket<Float> propertyPacket = new EntityPropertyPacket<>();
            propertyPacket.setEntityId(id);
            propertyPacket.setIdentifier(identifier);
            propertyPacket.setValue(value);
            packet.addPacket(propertyPacket);
        });

        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendIntProperty(Player player, int id, String identifier, Integer value) {
        EntityPropertyPacket<Integer> packet = new EntityPropertyPacket<>();
        packet.setEntityId(id);
        packet.setIdentifier(identifier);
        packet.setValue(value);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

    public static void sendIntProperties(Player player, int id, Map<String, Integer> bundle) {
        BundlePacket packet = new BundlePacket();
        bundle.forEach((identifier, value) -> {
            EntityPropertyPacket<Integer> propertyPacket = new EntityPropertyPacket<>();
            propertyPacket.setEntityId(id);
            propertyPacket.setIdentifier(identifier);
            propertyPacket.setValue(value);
            packet.addPacket(propertyPacket);
        });

        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
    }

}
