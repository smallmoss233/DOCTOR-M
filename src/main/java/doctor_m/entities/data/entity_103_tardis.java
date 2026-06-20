package doctor_m.entities.data;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class entity_103_tardis extends PathAwareEntity {

    // 皮肤相关
    private static final TrackedData<String> SELECTED_SKIN =
    DataTracker.registerData(entity_103_tardis.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> MODEL_TYPE =
    DataTracker.registerData(entity_103_tardis.class, TrackedDataHandlerRegistry.STRING);

    private String selectedSkin = "";
    private String displayName = "";
    private String modelType = "slim";

    // 个性系统（初始化默认值，避免 null）
    public enum Personality {
            AGGRESSIVE, DEFENSIVE, TIMID, BRAVE, TRADER
    }
    private Personality personality = Personality.DEFENSIVE;
    private long lastCounterAttackTime = 0;

    public entity_103_tardis(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        if (!world.isClient) {
            // 随机个性
            Personality[] values = Personality.values();
            this.personality = values[this.random.nextInt(values.length)];
            chooseRandomSkin();
        }
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        if (personality == null) {
            personality = Personality.DEFENSIVE;
        }
        // 基础 Goals
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));

        // 根据个性添加行为
        switch (personality) {
            case AGGRESSIVE:
            this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
            this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, true));
            this.goalSelector.add(5, new WanderAroundGoal(this, 0.7));
            break;
            case DEFENSIVE:
            this.goalSelector.add(5, new WanderAroundGoal(this, 0.6));
            break;
            case TIMID:
            this.goalSelector.add(4, new FleeEntityGoal<>(this, PlayerEntity.class, 10.0f, 1.2, 1.5));
            this.goalSelector.add(5, new WanderAroundGoal(this, 0.5));
            break;
            case BRAVE:
            this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, true));
            this.goalSelector.add(4, new MeleeAttackGoal(this, 1.2, true));
            this.goalSelector.add(5, new WanderAroundGoal(this, 0.8));
            break;
            case TRADER:
            this.goalSelector.add(5, new WanderAroundGoal(this, 0.6));
            break;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (!damaged) return false;

        if (!this.getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker) {
            long now = this.getWorld().getTime();
            if (now - lastCounterAttackTime < 20) return true;
            this.lastCounterAttackTime = now;

            if (personality == Personality.AGGRESSIVE || personality == Personality.DEFENSIVE || personality == Personality.BRAVE) {
                int type = this.random.nextInt(2); // 0=近战, 1=传送
                if (type == 0) {
                    counterAttackMelee(attacker);
                } else {
                    counterAttackTeleport(attacker);
                }
            }
        }
        return true;
    }

    private void counterAttackMelee(LivingEntity attacker) {
        this.tryAttack(attacker);
        this.getWorld().sendEntityStatus(this, (byte) 4);
    }

    private void counterAttackTeleport(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            RegistryKey<World> targetDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier("doctor_m", "trenzalore"));
            ServerWorld targetWorld = player.getServer().getWorld(targetDim);
            if (targetWorld != null) {
                player.teleport(targetWorld, 0.5, 65, 0.5, player.getYaw(), player.getPitch());
                player.sendMessage(Text.literal("你被传送到了特兰泽洛！"), true);
            }
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && personality == Personality.TRADER) {
            player.sendMessage(Text.literal("§a[交易] 你想要什么？"), true);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    // ---------- 皮肤系统 ----------
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
                String modelType = "slim";
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
        nbt.putString("Personality", personality.name());
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
        if (nbt.contains("Personality")) {
            try {
                this.personality = Personality.valueOf(nbt.getString("Personality"));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }
}