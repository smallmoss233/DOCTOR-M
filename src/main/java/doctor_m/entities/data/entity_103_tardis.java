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

    private String selectedSkin = "";      // 纹理文件名（安全，如 "omega.png"）
    private String displayName = "";       // 显示名字（如 "Омега"）

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
    }

    private void chooseRandomSkin() {
        List<SkinEntry> entries = loadSkinList();
        if (entries.isEmpty()) return;
        Random rand = new Random();
        SkinEntry chosen = entries.get(rand.nextInt(entries.size()));
        this.selectedSkin = chosen.texture;
        this.displayName = chosen.display;
        // 设置实体头顶的名字
        this.setCustomName(Text.literal(displayName));
        this.setCustomNameVisible(true);
        // 同步纹理文件名到客户端（用于渲染）
        this.dataTracker.set(SELECTED_SKIN, selectedSkin);
    }

    private List<SkinEntry> loadSkinList() {
        List<SkinEntry> list = new ArrayList<>();
        try (InputStream stream = entity_103_tardis.class.getResourceAsStream("/assets/doctor_m/textures/entity/tardis_skins/skins.txt")) {
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
                if (parts.length == 2) {
                    textureName = parts[0].trim();
                    displayName = parts[1].trim();
                } else {
                    // 兼容旧格式：整行作为文件名，显示名称为去掉扩展名的文件名（不安全，不推荐）
                    textureName = line;
                    String nameWithoutExt = line.contains(".") ? line.substring(0, line.lastIndexOf('.')) : line;
                    displayName = nameWithoutExt;
                }
                // 确保纹理文件名合法（小写字母、数字、下划线、点）
                textureName = textureName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
                if (!textureName.endsWith(".png")) textureName += ".png";
                list.add(new SkinEntry(textureName, displayName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class SkinEntry {
        String texture;
        String display;
        SkinEntry(String t, String d) { texture = t; display = d; }
    }

    // 客户端通过此方法获取纹理文件名
    public String getSelectedSkin() {
        if (this.getWorld().isClient) {
            return this.dataTracker.get(SELECTED_SKIN);
        }
        return selectedSkin;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SelectedSkin", selectedSkin);
        nbt.putString("DisplayName", displayName);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SelectedSkin")) {
            selectedSkin = nbt.getString("SelectedSkin");
            displayName = nbt.getString("DisplayName");
            this.dataTracker.set(SELECTED_SKIN, selectedSkin);
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