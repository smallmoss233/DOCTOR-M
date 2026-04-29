package doctor_m.entities.data;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class entity_103_tardis extends PathAwareEntity {

    private static final TrackedData<String> SELECTED_SKIN =
            DataTracker.registerData(entity_103_tardis.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> MODEL_TYPE =
            DataTracker.registerData(entity_103_tardis.class, TrackedDataHandlerRegistry.STRING);

    private String selectedSkin = "";
    private String displayName = "";
    private String modelType = "slim";

    public entity_103_tardis(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        if (!world.isClient) {
            chooseRandomSkin();
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SELECTED_SKIN, "");
        this.dataTracker.startTracking(MODEL_TYPE, "slim");
    }

    private void chooseRandomSkin() {
        List<SkinEntry> entries = loadSkinList();
        if (entries.isEmpty()) return;
        Random rand = new Random();
        SkinEntry chosen = entries.get(rand.nextInt(entries.size()));
        this.selectedSkin = chosen.texture;
        this.displayName = chosen.display;
        this.modelType = chosen.modelType;
        this.setCustomName(Text.literal(displayName));
        this.setCustomNameVisible(true);
        this.dataTracker.set(SELECTED_SKIN, selectedSkin);
        this.dataTracker.set(MODEL_TYPE, modelType);
    }

    private List<SkinEntry> loadSkinList() {
        List<SkinEntry> list = new ArrayList<>();
        try (InputStream stream = entity_103_tardis.class.getResourceAsStream("/assets/doctor_m/textures/entity/tardis/skins.txt")) {
            if (stream == null) {
                System.err.println("Could not find skins.txt");
                return list;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|");
                String textureName;
                String displayName;
                String modelType = "slim"; // 默认
                if (parts.length >= 2) {
                    textureName = parts[0].trim();
                    displayName = parts[1].trim();
                    if (parts.length >= 3) {
                        String type = parts[2].trim().toLowerCase();
                        if (type.equals("default") || type.equals("steve")) {
                            modelType = "default";
                        } else {
                            modelType = "slim";
                        }
                    }
                } else {
                    textureName = line;
                    String nameWithoutExt = line.contains(".") ? line.substring(0, line.lastIndexOf('.')) : line;
                    displayName = nameWithoutExt;
                }
                textureName = textureName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
                if (!textureName.endsWith(".png")) textureName += ".png";
                list.add(new SkinEntry(textureName, displayName, modelType));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class SkinEntry {
        String texture;
        String display;
        String modelType;
        SkinEntry(String t, String d, String m) { texture = t; display = d; modelType = m; }
    }

    public String getSelectedSkin() {
        if (this.getWorld().isClient) {
            return this.dataTracker.get(SELECTED_SKIN);
        }
        return selectedSkin;
    }

    public String getModelType() {
        if (this.getWorld().isClient) {
            return this.dataTracker.get(MODEL_TYPE);
        }
        return modelType;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SelectedSkin", selectedSkin);
        nbt.putString("DisplayName", displayName);
        nbt.putString("ModelType", modelType);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SelectedSkin")) {
            selectedSkin = nbt.getString("SelectedSkin");
            displayName = nbt.getString("DisplayName");
            modelType = nbt.getString("ModelType");
            this.dataTracker.set(SELECTED_SKIN, selectedSkin);
            this.dataTracker.set(MODEL_TYPE, modelType);
            this.setCustomName(Text.literal(displayName));
            this.setCustomNameVisible(true);
        }
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }
}