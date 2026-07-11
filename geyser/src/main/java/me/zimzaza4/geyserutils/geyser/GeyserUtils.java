package me.zimzaza4.geyserutils.geyser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.zimzaza4.geyserutils.common.camera.data.CameraPreset;
import me.zimzaza4.geyserutils.common.camera.instruction.ClearInstruction;
import me.zimzaza4.geyserutils.common.camera.instruction.FadeInstruction;
import me.zimzaza4.geyserutils.common.camera.instruction.SetInstruction;
import me.zimzaza4.geyserutils.common.channel.GeyserUtilsChannels;
import me.zimzaza4.geyserutils.common.form.element.NpcDialogueButton;
import me.zimzaza4.geyserutils.common.manager.PacketManager;
import me.zimzaza4.geyserutils.common.packet.*;
import me.zimzaza4.geyserutils.geyser.form.NpcDialogueForm;
import me.zimzaza4.geyserutils.geyser.form.NpcDialogueForms;
import me.zimzaza4.geyserutils.geyser.form.element.Button;
import me.zimzaza4.geyserutils.geyser.replace.JavaAddEntityTranslatorReplace;
import me.zimzaza4.geyserutils.geyser.translator.NPCFormResponseTranslator;
import me.zimzaza4.geyserutils.geyser.util.Converter;
import me.zimzaza4.geyserutils.geyser.util.DeltaUtils;
import me.zimzaza4.geyserutils.geyser.util.ReflectionUtils;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.bedrock.camera.CameraShake;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.skin.Cape;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.skin.SkinGeometry;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GeyserUtils implements Extension {


    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final Map<String, List<Map.Entry<String, Class<?>>>> properties = new HashMap<>();
    @Getter
    public static PacketManager packetManager = new PacketManager();
    public static List<String> REGISTERED_ENTITIES = new ArrayList<>();
    public static boolean GEYSER_LOADED = false;
    @Getter
    public static Map<String, SkinData> LOADED_SKIN_DATA = new HashMap<>();
    @Getter
    public static Map<String, EntityDefinition> LOADED_ENTITY_DEFINITIONS = new HashMap<>();
    @Getter
    public static Map<GeyserConnection, Cache<Integer, String>> CUSTOM_ENTITIES = new ConcurrentHashMap<>();
    static Cape EMPTY_CAPE = new Cape("", "no-cape", new byte[0], true);
    private static List<String> ENTITIES_WAIT_FOR_LOAD = new ArrayList<>();
    @Getter
    private static GeyserUtils instance;

    public static final Integer MAX_VALUE = 1000000;

    public static final Integer MIN_VALUE = -1000000;

    public GeyserUtils() {
        instance = this;
    }

    // the static here is crazy ;(
    private static GeyserEntityProperties.Builder getProperties(String id) {
        if (!properties.containsKey(id)) return null;

        GeyserEntityProperties.Builder builder = new GeyserEntityProperties.Builder(id);
        List<Map.Entry<String, Class<?>>> pairs = properties.get(id);
        pairs.forEach(p -> {
            // only bool, float and int support for now
            if (p.getValue() == Boolean.class) builder.add(new BooleanProperty(Identifier.of(p.getKey()), false));
            else if (p.getValue() == Float.class) builder.add(new FloatProperty(Identifier.of(p.getKey()), MAX_VALUE, MIN_VALUE,0f));
            else if (p.getValue() == Integer.class) builder.add(new IntProperty(Identifier.of(p.getKey()), MAX_VALUE, MIN_VALUE, 0));
            else instance.logger().info("Found unknown property: " + p.getKey());
        });

        return builder;
    }

    private static boolean containsProperty(String entityId, String identifier) {
        if (!properties.containsKey(entityId)) return false;

        return properties.get(entityId).stream().anyMatch(p -> p.getKey().equalsIgnoreCase(identifier));
    }

    public static void addProperty(String entityId, String identifier, Class<?> type) {
        if (containsProperty(entityId, identifier)) return;

        List<Map.Entry<String, Class<?>>> pairs = properties.getOrDefault(entityId, new ArrayList<>());
        pairs.add(new AbstractMap.SimpleEntry<>(identifier, type));

        if (properties.containsKey(entityId)) properties.replace(entityId, pairs);
        else properties.put(entityId, pairs);
    }

    public static void registerProperties(String entityId) {
        if (GEYSER_LOADED) {
            registerProperties(entityId);
        }
        ENTITIES_WAIT_FOR_LOAD.add(entityId);
    }

    public static NbtMap registerPropertiesForGeyser(String entityId) {
        GeyserEntityProperties.Builder builder = getProperties(entityId);
        if (builder == null) return null;
        GeyserEntityProperties entityProperties = builder.build();
        properties.values().stream()
                .flatMap(List::stream)
                .map(Map.Entry::getKey)
                .forEach(id -> {
                    Registries.BEDROCK_ENTITY_PROPERTIES.get().removeIf(i -> i.containsKey(id));
                });

        EntityDefinition old = LOADED_ENTITY_DEFINITIONS.get(entityId);
        LOADED_ENTITY_DEFINITIONS.replace(entityId, new EntityDefinition(old.factory(), old.entityType(), old.identifier(),
                old.width(), old.height(), old.offset(), entityProperties, old.translators()));

        instance.logger().info("Defined entity: " + entityId + " in registry.");
        return entityProperties.toNbtMap(entityId);
    }

    public static void addCustomEntity(String id) {
        /*
        LOADED_ENTITY_DEFINITIONS.put(id,
                EntityDefinition.builder()
                        .identifier(EntityIdentifier.builder().identifier(id)
                                .summonable(true)
                                .spawnEgg(false).build())
                        .height(0.6f)
                        .width(0.6f)
                        .build());

         */
        if (GEYSER_LOADED) {
            registerEntityToGeyser(id);
        }
        REGISTERED_ENTITIES.add(id);
    }

    public static void registerEntityToGeyser(String id) {
        NbtMap registry = Registries.BEDROCK_ENTITY_IDENTIFIERS.get();
        List<NbtMap> idList = new ArrayList<>(registry.getList("idlist", NbtType.COMPOUND));
        idList.add(NbtMap.builder()
                .putString("id", id)
                .putString("bid", "")
                .putBoolean("hasspawnegg", false)
                .putInt("rid", idList.size() + 1)
                .putBoolean("summonable", false).build()
        );

        Registries.BEDROCK_ENTITY_IDENTIFIERS.set(NbtMap.builder()
                .putList("idlist", NbtType.COMPOUND, idList).build()
        );

        EntityDefinition<Entity> def = EntityDefinition.builder(null)
                .height(0.1f).width(0.1f).identifier(id).propertiesBuilder(getProperties(id)).build();

        LOADED_ENTITY_DEFINITIONS.put(id, def);
    }

    @NotNull
    private static AnimateEntityPacket getAnimateEntityPacket(AnimateEntityCustomPayloadPacket animateEntityCustomPayloadPacket) {
        AnimateEntityPacket animateEntityPacket = new AnimateEntityPacket();
        animateEntityPacket.setAnimation(animateEntityCustomPayloadPacket.getAnimation());
        animateEntityPacket.setController(animateEntityCustomPayloadPacket.getController());
        animateEntityPacket.setBlendOutTime(animateEntityCustomPayloadPacket.getBlendOutTime());
        animateEntityPacket.setNextState(animateEntityCustomPayloadPacket.getNextState());
        animateEntityPacket.setStopExpressionVersion(animateEntityCustomPayloadPacket.getStopExpressionVersion());
        animateEntityPacket.setStopExpression(animateEntityCustomPayloadPacket.getStopExpression());
        return animateEntityPacket;
    }

    public static void sendSkinPacket(GeyserSession session, PlayerEntity entity, SkinData skinData) {
        Skin skin = skinData.skin();
        Cape cape = skinData.cape();
        SkinGeometry geometry = skinData.geometry();

        if (entity.uuid().equals(session.getPlayerEntity().uuid())) {
            PlayerListPacket.Entry updatedEntry = buildEntryManually(
                    session,
                    entity.uuid(),
                    entity.getUsername(),
                    entity.geyserId(),
                    skin,
                    cape,
                    geometry
            );

            PlayerListPacket playerAddPacket = new PlayerListPacket();
            playerAddPacket.setAction(PlayerListPacket.Action.ADD);
            playerAddPacket.getEntries().add(updatedEntry);
            session.sendUpstreamPacket(playerAddPacket);
        } else {
            PlayerSkinPacket packet = new PlayerSkinPacket();
            packet.setUuid(entity.uuid());
            packet.setOldSkinName("");
            packet.setNewSkinName(skin.textureUrl());
            packet.setSkin(getSkin(skin.textureUrl(), skin, cape, geometry));
            packet.setTrustedSkin(true);
            session.sendUpstreamPacket(packet);
        }
    }

    public static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, String username, long geyserId,
                                                            Skin skin,
                                                            Cape cape,
                                                            SkinGeometry geometry) {
        SerializedSkin serializedSkin = getSkin(skin.textureUrl(), skin, cape, geometry);

        String xuid = "";
        GeyserSession playerSession = GeyserImpl.getInstance().connectionByUuid(uuid);

        if (playerSession != null) {
            xuid = playerSession.getAuthData().xuid();
        }

        PlayerListPacket.Entry entry;
        if (session.getPlayerEntity().uuid().equals(uuid)) {
            entry = new PlayerListPacket.Entry(session.getAuthData().uuid());
        } else {
            entry = new PlayerListPacket.Entry(uuid);
        }

        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(serializedSkin);
        entry.setXuid(xuid);
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        return entry;
    }

    private static SerializedSkin getSkin(String skinId, Skin skin, Cape cape, SkinGeometry geometry) {

        try {
            ImageData image = ImageData.from(ImageIO.read(new ByteArrayInputStream(skin.skinData())));
            return SerializedSkin.of(skinId, "", geometry.geometryName(), image, Collections.emptyList(), ImageData.of(cape.capeData()), geometry.geometryData(), "", true, false, false, cape.capeId(), skinId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cape getEmptyCapeData() {
        return EMPTY_CAPE;
    }

    private static int getColor(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        double[] colorLab = DeltaUtils.rgbToLab(r, g, b);

        List<int[]> colors = Arrays.asList(
                new int[]{249, 255, 254},    // 0: White
                new int[]{249, 128, 29},     // 1: Orange
                new int[]{199, 78, 189},     // 2: Magenta
                new int[]{58, 179, 218},     // 3: Light Blue
                new int[]{254, 216, 61},     // 4: Yellow
                new int[]{128, 199, 31},     // 5: Lime
                new int[]{243, 139, 170},    // 6: Pink
                new int[]{71, 79, 82},       // 7: Gray
                new int[]{159, 157, 151},    // 8: Light Gray
                new int[]{22, 156, 156},     // 9: Cyan
                new int[]{137, 50, 184},     // 10: Purple
                new int[]{60, 68, 170},      // 11: Blue
                new int[]{131, 84, 50},      // 12: Brown
                new int[]{94, 124, 22},      // 13: Green
                new int[]{176, 46, 38},      // 14: Red
                new int[]{29, 29, 33}        // 15: Black
        );

        int closestColorIndex = -1;
        double minDeltaE = Double.MAX_VALUE;

        for (int i = 0; i < colors.size(); i++) {
            int[] rgb = colors.get(i);
            double[] lab = DeltaUtils.rgbToLab(rgb[0], rgb[1], rgb[2]);
            double deltaE = DeltaUtils.calculateDeltaE(colorLab, lab);
            if (deltaE < minDeltaE) {
                minDeltaE = deltaE;
                closestColorIndex = i;
            }
        }

        return closestColorIndex;
    }

    @Subscribe
    public void onEnable(GeyserPostInitializeEvent event) {
        // This is temporary and we should remove this at some point lmao
        try {
            Class.forName("org.geysermc.geyser.entity.spawn.EntitySpawnContext");
        } catch (ClassNotFoundException exception) {
            logger().warning("Seems like you're on an outdated version of Geyser which doesn't support the refactored entity API, please update <3");
        }

        Registries.BEDROCK_PACKET_TRANSLATORS.register(NpcRequestPacket.class, new NPCFormResponseTranslator());
        loadSkins();
        ReflectionUtils.init();
        CameraPreset.load();

        replaceTranslator();
        GEYSER_LOADED = true;
        logger().info("Defined " + LOADED_ENTITY_DEFINITIONS.size() + " entities");
        // MountFix.start();
    }


    @Subscribe
    public void onDefineProperties(GeyserDefineEntityPropertiesEvent event) {
        for (String registeredEntity : REGISTERED_ENTITIES) {
            registerEntityToGeyser(registeredEntity);
        }
        Set<NbtMap> entityProperties = new HashSet<>();
        for (String id : ENTITIES_WAIT_FOR_LOAD) {
            NbtMap map = registerPropertiesForGeyser(id);
            if (map == null) continue;
            entityProperties.add(map);
        }
        // Prevent Error: "Cannot add properties outside the GeyserDefineEntityProperties event!"
        scheduler.schedule(() -> Registries.BEDROCK_ENTITY_PROPERTIES.get().addAll(entityProperties), 1, TimeUnit.SECONDS);
    }

    public void replaceTranslator() {
        Registries.JAVA_PACKET_TRANSLATORS
                .register(ClientboundAddEntityPacket.class, new JavaAddEntityTranslatorReplace());
    }

    @Subscribe
    public void onLoadCommand(GeyserDefineCommandsEvent event) {

    }

    public void loadSkins() {
        LOADED_SKIN_DATA.clear();
        File folder = this.dataFolder().resolve("skins").toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                File textureFile = null;
                File geometryFile = null;

                for (File folderFile : file.listFiles()) {
                    if (folderFile.getName().endsWith(".png")) {
                        textureFile = folderFile;
                    }
                    if (folderFile.getName().endsWith(".json")) {
                        geometryFile = folderFile;
                    }

                }

                loadSkin(file.getName(), geometryFile, textureFile);


            }
        }
    }

    public void loadSkin(String skinId, File geometryFile, File textureFile) {
        try {
            Skin skin = new Skin(skinId, Files.readAllBytes(textureFile.toPath()), false);

            String geoId = "";
            JsonElement json = new JsonParser().parse(new FileReader(geometryFile));
            for (JsonElement element : json.getAsJsonObject().get("minecraft:geometry").getAsJsonArray()) {
                if (element.isJsonObject() && element.getAsJsonObject().has("description")) {
                    geoId = element.getAsJsonObject().get("description").getAsJsonObject().get("identifier").getAsString();
                    break;
                }
            }
            String geoName = "{\"geometry\" :{\"default\" :\"" + geoId + "\"}}";
            SkinGeometry geometry = new SkinGeometry(geoName, Files.readString(geometryFile.toPath()));
            LOADED_SKIN_DATA.put(skinId, new SkinData(skin, getEmptyCapeData(), geometry));
            this.logger().info("Loaded skin: " + skinId + "| geo:" + geoName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        CUSTOM_ENTITIES.put(event.connection(), CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build());
        if (event.connection() instanceof GeyserSession session) {
            registerPacketListener(session);
        }
    }

    @Subscribe
    public void onSessionQuit(SessionDisconnectEvent event) {
        CUSTOM_ENTITIES.remove(event.connection());
    }

    public void registerPacketListener(GeyserSession session) {

        scheduler.schedule(() -> {
            if (session.getDownstream() == null) {
                registerPacketListener(session);
                return;
            }

            session.getDownstream().getSession().addListener(new SessionAdapter() {

                @Override
                public void packetSending(PacketSendingEvent event) {
                    Packet packet = event.getPacket();
                    if (packet instanceof ServerboundCustomPayloadPacket payloadPacket) {
                        if (ReflectionUtils.getChannel(payloadPacket).toString().equals("minecraft:register")) {
                            String channels = new String(payloadPacket.getData(), StandardCharsets.UTF_8);
                            channels = channels + "\0" + GeyserUtilsChannels.MAIN;
                            event.setPacket(ReflectionUtils.buildServerboundPayloadPacket("minecraft:register", channels.getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                }

                @Override
                public void packetReceived(Session tcpSession, Packet packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket payloadPacket) {
                        if (ReflectionUtils.getChannel(payloadPacket).toString().equals(GeyserUtilsChannels.MAIN)) {
                            CustomPayloadPacket customPacket = packetManager.decodePacket(payloadPacket.getData());
                            handleCustomPacket(session, customPacket);
                        }
                    }
                }
            });
        }, 80, TimeUnit.MILLISECONDS);
    }

    private void handleCustomPacket(GeyserSession session, CustomPayloadPacket customPacket) {
        if (customPacket instanceof BundlePacket bundlePacket) {
            bundlePacket.getPackets().forEach(p -> handleCustomPacket(session, p));
        } else if (customPacket instanceof CameraShakeCustomPayloadPacket cameraShakePacket) {
            session.camera().shakeCamera(cameraShakePacket.getIntensity(), cameraShakePacket.getDuration(), CameraShake.values()[cameraShakePacket.getType()]);
        } else if (customPacket instanceof NpcDialogueFormDataCustomPayloadPacket formData) {

            if (formData.action().equals("CLOSE")) {
                NpcDialogueForm openForm = NpcDialogueForms.getOpenNpcDialogueForms(session);
                if (openForm != null) {
                    openForm.close(session);
                }
                return;
            }

            NpcDialogueForm form = new NpcDialogueForm();
            form.title(formData.title())
                    .dialogue(formData.dialogue())
                    .bindEntity(session.getEntityCache().getEntityByJavaId(formData.bindEntity()))
                    .hasNextForm(formData.hasNextForm());

            if (formData.skinData() != null) {
                form.skinData(formData.skinData());
            }


            List<Button> buttons = new ArrayList<>();

            if (formData.buttons() != null) {

                int i = 0;
                for (NpcDialogueButton button : formData.buttons()) {


                    int finalI = i;
                    buttons.add(new Button(button.text(), button.commands(),
                            button.mode(), () -> {
                        if (button.mode() == NpcDialogueButton.ButtonMode.BUTTON_MODE) {
                            session.sendDownstreamPacket(ReflectionUtils.buildServerboundPayloadPacket(GeyserUtilsChannels.MAIN, packetManager.encodePacket(new NpcFormResponseCustomPayloadPacket(formData.formId(), finalI))));
                        }
                    }, button.hasNextForm()));
                    i++;
                }
            }

            form.closeHandler(() -> session.sendDownstreamPacket(ReflectionUtils.buildServerboundPayloadPacket(GeyserUtilsChannels.MAIN, packetManager.encodePacket(new NpcFormResponseCustomPayloadPacket(formData.formId(), -1)))));
            form.buttons(buttons);

            form.createAndSend(session);

        } else if (customPacket instanceof AnimateEntityCustomPayloadPacket animateEntityCustomPayloadPacket) {
            AnimateEntityPacket animateEntityPacket = getAnimateEntityPacket(animateEntityCustomPayloadPacket);
            for (int id : animateEntityCustomPayloadPacket.getEntityJavaIds()) {
                Entity entity = session.getEntityCache().getEntityByJavaId(id);
                if (entity != null) {
                    try {
                        // because of shaded jar
                        Object object = AnimateEntityPacket.class.getMethod("getRuntimeEntityIds").invoke(animateEntityPacket);
                        object.getClass().getMethod("add", Long.class).invoke(object, entity.geyserId());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            session.sendUpstreamPacket(animateEntityPacket);
        } else if (customPacket instanceof CustomEntityPacket customEntityPacket) {
            if (!LOADED_ENTITY_DEFINITIONS.containsKey(customEntityPacket.getIdentifier())) {
                // System.out.println("Not a vaild entity:" + customEntityPacket.getEntityId());
                return;
            }
            // System.out.println("custom entity:" + customEntityPacket.getEntityId());

            Cache<Integer, String> cache = CUSTOM_ENTITIES.get(session);
            cache.put(customEntityPacket.getEntityId(), customEntityPacket.getIdentifier());
        } else if (customPacket instanceof CameraInstructionCustomPayloadPacket cameraInstructionPacket) {
            if (cameraInstructionPacket.getInstruction() instanceof SetInstruction instruction) {
                session.camera().sendCameraPosition(Converter.serializeSetInstruction(instruction));
                session.getCameraData().forceCameraPerspective(Converter.serializeCameraPerspective(instruction.getPreset()));

            } else if (cameraInstructionPacket.getInstruction() instanceof FadeInstruction instruction) {
                session.camera().sendCameraFade(Converter.serializeFadeInstruction(instruction));
            } else if (cameraInstructionPacket.getInstruction() instanceof ClearInstruction) {
                session.camera().clearCameraInstructions();
            }

        } else if (customPacket instanceof CustomParticleEffectPayloadPacket customParticleEffectPacket) {
            SpawnParticleEffectPacket spawnParticleEffectPacket = new SpawnParticleEffectPacket();
            spawnParticleEffectPacket.setDimensionId(DimensionUtils.javaToBedrock(session));
            spawnParticleEffectPacket.setPosition(Converter.serializePos(customParticleEffectPacket.getPos()));
            spawnParticleEffectPacket.setIdentifier(customParticleEffectPacket.getParticle().identifier());
            spawnParticleEffectPacket.setMolangVariablesJson(Optional.ofNullable(customParticleEffectPacket.getParticle().molangVariablesJson()));
            session.sendUpstreamPacket(spawnParticleEffectPacket);
        } else if (customPacket instanceof CustomSkinPayloadPacket customSkinPayloadPacket) {
            if (session.getEntityCache().getEntityByJavaId(customSkinPayloadPacket.getEntityId()) instanceof PlayerEntity player) {
                SkinData data = LOADED_SKIN_DATA.get(customSkinPayloadPacket.getSkinId());
                if (data != null) {
                    sendSkinPacket(session, player, data);
                }
            }

        } else if (customPacket instanceof CustomEntityDataPacket customEntityDataPacket) {
            Entity entity = session.getEntityCache().getEntityByJavaId(customEntityDataPacket.getEntityId());
            if (entity != null) {
                if (customEntityDataPacket.getHeight() != null)
                    entity.setBoundingBoxHeight(customEntityDataPacket.getHeight());
                if (customEntityDataPacket.getWidth() != null)
                    entity.setBoundingBoxWidth(customEntityDataPacket.getWidth());
                if (customEntityDataPacket.getScale() != null)
                    entity.getDirtyMetadata().put(EntityDataTypes.SCALE, customEntityDataPacket.getScale());
                if (customEntityDataPacket.getColor() != null)
                    entity.getDirtyMetadata().put(EntityDataTypes.COLOR, Byte.parseByte(String.valueOf(getColor(customEntityDataPacket.getColor()))));
                if (customEntityDataPacket.getVariant() != null)
                    entity.getDirtyMetadata().put(EntityDataTypes.VARIANT, customEntityDataPacket.getVariant());
                entity.updateBedrockMetadata();
            }
        } else if (customPacket instanceof EntityPropertyPacket entityPropertyPacket) {
            Entity entity = session.getEntityCache().getEntityByJavaId(entityPropertyPacket.getEntityId());
            if (entity != null) {
                if (entityPropertyPacket.getIdentifier() == null
                        || entityPropertyPacket.getValue() == null) return;

                if (entity.getPropertyManager() == null) return;
                if (entityPropertyPacket.getValue() instanceof Boolean value) {
                    entity.getPropertyManager().addProperty(new BooleanProperty(Identifier.of(entityPropertyPacket.getIdentifier()), false), value);
                } else if (entityPropertyPacket.getValue() instanceof Integer value) {
                    entity.getPropertyManager().addProperty(new IntProperty(Identifier.of(entityPropertyPacket.getIdentifier()), MAX_VALUE, MIN_VALUE, 0), value);
                }
                entity.updateBedrockEntityProperties();
            }
        } else if (customPacket instanceof EntityPropertyRegisterPacket entityPropertyRegisterPacket) {
            if (entityPropertyRegisterPacket.getIdentifier() == null
                    || entityPropertyRegisterPacket.getType() == null) return;

            Entity entity = (session.getEntityCache().getEntityByJavaId(entityPropertyRegisterPacket.getEntityId()));
            if (entity != null) {
                String def = CUSTOM_ENTITIES.get(session).getIfPresent(entity.getEntityId());
                if (def == null) return;

                if (!containsProperty(def, entityPropertyRegisterPacket.getIdentifier())) {
                    addProperty(def, entityPropertyRegisterPacket.getIdentifier(), entityPropertyRegisterPacket.getType());

                    registerProperties(def);
                    logger().info("DEF PROPERTIES: " + entityPropertyRegisterPacket.getIdentifier());
                }
            }


        }
    }

}

