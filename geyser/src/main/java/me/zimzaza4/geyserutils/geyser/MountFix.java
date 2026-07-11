package me.zimzaza4.geyserutils.geyser;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MountFix {

    public static void start() {
        // just keep send SetEntityLinkPacket to fix the mount bug
        // https://github.com/GeyserMC/Geyser/issues/3302
        // if the vehicle is too fast, the problem appear
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    try {
                        for (GeyserSession session : GeyserImpl.getInstance().onlineConnections()) {
                            Entity v = session.getPlayerEntity().getVehicle();
                            if (v != null && v.getDefinition() == EntityDefinitions.ARMOR_STAND) {
                                session.setShouldSendSneak(true);
                                long vehicleBedrockId = v.geyserId();
                                if (session.getPlayerEntity().getVehicle().geyserId() == vehicleBedrockId) {
                                    // The Bedrock client, as of 1.19.51, dismounts on its end. The server may not agree with this.
                                    // If the server doesn't agree with our dismount (sends a packet saying we dismounted),
                                    // then remount the player.
                                    SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                                    linkPacket.setEntityLink(new EntityLinkData(vehicleBedrockId, session.getPlayerEntity().geyserId(), EntityLinkData.Type.PASSENGER, true, false, 0f));
                                    session.sendUpstreamPacket(linkPacket);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }, 200, 50, TimeUnit.MILLISECONDS);


    }

}
