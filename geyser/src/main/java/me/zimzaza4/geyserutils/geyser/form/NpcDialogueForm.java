package me.zimzaza4.geyserutils.geyser.form;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.zimzaza4.geyserutils.geyser.form.element.Button;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.NpcDialoguePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.entity.GeyserDirtyMetadata;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Accessors(fluent = true, chain = true)
public class NpcDialogueForm {

    public static final Gson GSON = new Gson();

    @Getter
    private final String sceneName = UUID.randomUUID().toString();
    @Getter
    private final ObjectArrayList<Button> dialogueButtons = new ObjectArrayList<>();
    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String dialogue;
    @Getter
    @Setter
    private Entity bindEntity;
    private String actionJson = "";
    @Getter
    @Setter
    private String skinData = "{\"picker_offsets\":{\"scale\":[1.70,1.70,1.70],\"translate\":[0,20,0]},\"portrait_offsets\":{\"scale\":[1.750,1.750,1.750],\"translate\":[-7,50,0]},\"skin_list\":[{\"variant\":0},{\"variant\":1},{\"variant\":2},{\"variant\":3},{\"variant\":4},{\"variant\":5},{\"variant\":6},{\"variant\":7},{\"variant\":8},{\"variant\":9},{\"variant\":10},{\"variant\":11},{\"variant\":12},{\"variant\":13},{\"variant\":14},{\"variant\":15},{\"variant\":16},{\"variant\":17},{\"variant\":18},{\"variant\":19},{\"variant\":20},{\"variant\":21},{\"variant\":22},{\"variant\":23},{\"variant\":24},{\"variant\":25},{\"variant\":26},{\"variant\":27},{\"variant\":28},{\"variant\":29},{\"variant\":30},{\"variant\":31},{\"variant\":32},{\"variant\":33},{\"variant\":34}]}";
    @Getter
    @Setter
    private Runnable closeHandler = () -> {
    };

    @Getter
    @Setter
    private boolean hasNextForm;

    public NpcDialogueForm buttons(Button... buttons) {

        List<JsonObject> objects = new ArrayList<>();

        for (Button button : buttons) {
            objects.add(button.toJsonObject());
        }
        this.actionJson = GSON.toJson(objects);

        this.dialogueButtons.addAll(Arrays.asList(buttons));

        return this;
    }

    public NpcDialogueForm buttons(List<Button> buttons) {

        List<JsonObject> objects = new ArrayList<>();

        for (Button button : buttons) {
            objects.add(button.toJsonObject());
        }
        this.actionJson = GSON.toJson(objects);

        this.dialogueButtons.addAll(buttons);

        return this;
    }

    public void createAndSend(GeyserSession player) {
        SetEntityDataPacket setEntityDataPacket = new SetEntityDataPacket();
        setEntityDataPacket.setRuntimeEntityId(this.bindEntity.geyserId());

        GeyserDirtyMetadata data = this.bindEntity.getDirtyMetadata();

        GeyserDirtyMetadata cloneData = GSON.fromJson(GSON.toJson(data), GeyserDirtyMetadata.class);


        cloneData.put(EntityDataTypes.NAME, this.title);
        cloneData.put(EntityDataTypes.NPC_DATA, skinData);
        cloneData.put(EntityDataTypes.ACTIONS, actionJson);
        cloneData.put(EntityDataTypes.HAS_NPC, true);
        cloneData.put(EntityDataTypes.INTERACT_TEXT, this.dialogue);

        cloneData.apply(setEntityDataPacket.getMetadata());

        player.sendUpstreamPacket(setEntityDataPacket);

        NpcDialoguePacket npcDialoguePacket = new NpcDialoguePacket();
        npcDialoguePacket.setUniqueEntityId(this.bindEntity.geyserId());
        npcDialoguePacket.setAction(NpcDialoguePacket.Action.OPEN);
        npcDialoguePacket.setDialogue(this.dialogue);
        npcDialoguePacket.setNpcName(this.title);
        npcDialoguePacket.setSceneName(this.sceneName);
        player.sendUpstreamPacket(setEntityDataPacket);
        npcDialoguePacket.setActionJson(this.actionJson);

        player.sendUpstreamPacket(npcDialoguePacket);
        NpcDialogueForms.addNpcDialogueForm(player, this);


    }

    public void close(GeyserSession player) {
        NpcDialoguePacket npcDialoguePacket = new NpcDialoguePacket();
        npcDialoguePacket.setUniqueEntityId(this.bindEntity.geyserId());
        npcDialoguePacket.setAction(NpcDialoguePacket.Action.CLOSE);
        npcDialoguePacket.setDialogue(this.dialogue);
        npcDialoguePacket.setNpcName(this.title);
        npcDialoguePacket.setSceneName(this.sceneName);
        npcDialoguePacket.setActionJson(this.actionJson);

        player.sendUpstreamPacket(npcDialoguePacket);
        SetEntityDataPacket setEntityDataPacket = new SetEntityDataPacket();
        setEntityDataPacket.setRuntimeEntityId(this.bindEntity.geyserId());

        GeyserDirtyMetadata data = this.bindEntity.getDirtyMetadata();

        GeyserDirtyMetadata cloneData = GSON.fromJson(GSON.toJson(data), GeyserDirtyMetadata.class);

        cloneData.apply(setEntityDataPacket.getMetadata());


        NpcDialogueForms.removeNpcDialogueForm(player, this);
    }
}