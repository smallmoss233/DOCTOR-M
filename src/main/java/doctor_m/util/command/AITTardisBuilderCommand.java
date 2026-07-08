package doctor_m.util.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.amble.ait.api.tardis.TardisComponent;
import dev.amble.ait.core.engine.DurableSubSystem;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.control.impl.DirectionControl;
import dev.amble.ait.core.tardis.handler.FuelHandler;
import dev.amble.ait.core.tardis.handler.LoyaltyHandler;
import dev.amble.ait.core.tardis.handler.StatsHandler;
import dev.amble.ait.core.tardis.handler.SubSystemHandler;
import dev.amble.ait.core.tardis.manager.ServerTardisManager;
import dev.amble.ait.core.tardis.manager.TardisBuilder;
import dev.amble.ait.core.tardis.util.DefaultThemes;
import dev.amble.ait.data.Loyalty;
import dev.amble.ait.data.schema.desktop.TardisDesktopSchema;
import dev.amble.ait.data.schema.exterior.ExteriorVariantSchema;
import dev.amble.ait.registry.impl.DesktopRegistry;
import dev.amble.ait.registry.impl.exterior.ExteriorVariantRegistry;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.Set;

public class AITTardisBuilderCommand {

    private static final Random RANDOM = new Random();

    // 子系统配置模式
    public enum SubSystemMode {
        FULL("full"),           // 全满
        ESSENTIAL("essential"), // 必要：引擎、生命维持、稳定器、变色龙
        ENGINE_ONLY("only_engine"), // 仅引擎
        NONE("none");           // 无

        private final String id;

        SubSystemMode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static SubSystemMode fromString(String str) {
            for (SubSystemMode mode : values()) {
                if (mode.id.equalsIgnoreCase(str)) {
                    return mode;
                }
            }
            return null;
        }

        public static String[] ids() {
            return java.util.Arrays.stream(values()).map(SubSystemMode::getId).toArray(String[]::new);
        }
    }

    // 必要子系统集合
    private static final Set<SubSystem.IdLike> ESSENTIAL_SUBSYSTEMS = Set.of(
            SubSystem.Id.ENGINE,
            SubSystem.Id.LIFE_SUPPORT,
            SubSystem.Id.STABILISERS,
            SubSystem.Id.CHAMELEON
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("doctor_m")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("build")
                        .executes(AITTardisBuilderCommand::executeDefault)
                        // /doctor_m ait_tardisbuilder <desktop>
                        .then(CommandManager.argument("desktop", StringArgumentType.string())
                                .suggests(DESKTOP_SUGGESTIONS)
                                .executes(ctx -> executeWithArgs(ctx,
                                        StringArgumentType.getString(ctx, "desktop"),
                                        null, null, null, null))
                                // /doctor_m ait_tardisbuilder <desktop> <exterior>
                                .then(CommandManager.argument("exterior", StringArgumentType.string())
                                        .suggests(EXTERIOR_SUGGESTIONS)
                                        .executes(ctx -> executeWithArgs(ctx,
                                                StringArgumentType.getString(ctx, "desktop"),
                                                StringArgumentType.getString(ctx, "exterior"),
                                                null, null, null))
                                        // /doctor_m ait_tardisbuilder <desktop> <exterior> <owner>
                                        .then(CommandManager.argument("owner", StringArgumentType.string())
                                                .suggests(OWNER_SUGGESTIONS)
                                                .executes(ctx -> executeWithArgs(ctx,
                                                        StringArgumentType.getString(ctx, "desktop"),
                                                        StringArgumentType.getString(ctx, "exterior"),
                                                        StringArgumentType.getString(ctx, "owner"),
                                                        null, null))
                                                // /doctor_m ait_tardisbuilder <desktop> <exterior> <owner> <subsystem>
                                                .then(CommandManager.argument("subsystem", StringArgumentType.string())
                                                        .suggests(SUBSYSTEM_SUGGESTIONS)
                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                StringArgumentType.getString(ctx, "desktop"),
                                                                StringArgumentType.getString(ctx, "exterior"),
                                                                StringArgumentType.getString(ctx, "owner"),
                                                                StringArgumentType.getString(ctx, "subsystem"),
                                                                null))
                                                        // /doctor_m ait_tardisbuilder <desktop> <exterior> <owner> <subsystem> <pos>
                                                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                                .executes(ctx -> executeWithArgs(ctx,
                                                                        StringArgumentType.getString(ctx, "desktop"),
                                                                        StringArgumentType.getString(ctx, "exterior"),
                                                                        StringArgumentType.getString(ctx, "owner"),
                                                                        StringArgumentType.getString(ctx, "subsystem"),
                                                                        Vec3ArgumentType.getVec3(ctx, "pos")))
                                                        )
                                                )
                                                // 兼容旧格式：owner 后直接跟 pos（默认全满）
                                                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                StringArgumentType.getString(ctx, "desktop"),
                                                                StringArgumentType.getString(ctx, "exterior"),
                                                                StringArgumentType.getString(ctx, "owner"),
                                                                null,  // 默认全满
                                                                Vec3ArgumentType.getVec3(ctx, "pos")))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int executeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return executeWithArgs(ctx, null, null, null, null, null);
    }

    private static int executeWithArgs(CommandContext<ServerCommandSource> ctx,
                                       String desktopRaw, String exteriorRaw,
                                       String ownerRaw, String subsystemRaw, Vec3d pos) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();

        // 解析子系统模式
        SubSystemMode subsystemMode = SubSystemMode.FULL; // 默认全满
        if (subsystemRaw != null && !subsystemRaw.isEmpty()) {
            subsystemMode = SubSystemMode.fromString(subsystemRaw);
            if (subsystemMode == null) {
                source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.invalid_subsystem_mode", subsystemRaw));
                return 0;
            }
        }

        // 位置
        CachedDirectedGlobalPos tardisPos;
        if (pos != null) {
            BlockPos blockPos = BlockPos.ofFloored(pos);
            tardisPos = CachedDirectedGlobalPos.create(world, blockPos, (byte) Direction.NORTH.getId());
        } else {
            BlockPos playerPos = player.getBlockPos().up(2);
            tardisPos = CachedDirectedGlobalPos.create(world, playerPos,
                    DirectionControl.getGeneralizedRotation(
                            RotationPropertyHelper.fromYaw(player.getBodyYaw())));
        }

        // 解析内饰
        TardisDesktopSchema desktop = null;
        Identifier desktopId = null;
        if (desktopRaw != null && !desktopRaw.isEmpty()) {
            desktopId = IdMappingUtil.fromMapping(desktopRaw);
            if (desktopId == null) {
                source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.invalid_desktop_id", desktopRaw));
                return 0;
            }
            desktop = DesktopRegistry.getInstance().get(desktopId);
            if (desktop == null) {
                source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.unknown_desktop", desktopRaw, desktopId));
                return 0;
            }
        }

        // 解析外观
        ExteriorVariantSchema exterior = null;
        Identifier exteriorId = null;
        if (exteriorRaw != null && !exteriorRaw.isEmpty()) {
            exteriorId = IdMappingUtil.fromMapping(exteriorRaw);
            if (exteriorId == null) {
                source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.invalid_exterior_id", exteriorRaw));
                return 0;
            }
            exterior = ExteriorVariantRegistry.getInstance().get(exteriorId);
            if (exterior == null) {
                source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.unknown_exterior", exteriorRaw, exteriorId));
                return 0;
            }
        }

        // 主人
        ServerPlayerEntity ownerPlayer = null;
        String ownerName = player.getName().getString();
        if (ownerRaw != null && !ownerRaw.isEmpty()) {
            ownerPlayer = world.getServer().getPlayerManager().getPlayer(ownerRaw);
            if (ownerPlayer != null) {
                ownerName = ownerPlayer.getName().getString();
            } else {
                ownerName = ownerRaw;
            }
        }

        // ===== 构建 TARDIS =====
        TardisBuilder builder = new TardisBuilder().at(tardisPos);
        if (ownerPlayer != null) builder.owner(ownerPlayer);

        builder.with(TardisComponent.Id.FUEL, (FuelHandler fuel) -> {
            fuel.setCurrentFuel(fuel.getMaxFuel());
            fuel.enablePower();
        });

        final SubSystemMode finalSubsystemMode = subsystemMode;
        builder.with(TardisComponent.Id.SUBSYSTEM, (SubSystemHandler sub) -> {
            applySubSystemMode(sub, finalSubsystemMode);
        });

        final ServerPlayerEntity finalOwner = ownerPlayer;
        final String finalOwnerName = ownerName;
        builder.with(TardisComponent.Id.LOYALTY, (LoyaltyHandler loyalty) -> {
            loyalty.setMessageEnabled(false);
            if (finalOwner != null) {
                loyalty.set(finalOwner, new Loyalty(Loyalty.Type.OWNER));
            }
            loyalty.setMessageEnabled(true);
        });

        builder.with(TardisComponent.Id.STATS, (StatsHandler stats) -> {
            stats.setPlayerCreatorName(finalOwnerName);
            stats.markPlayerCreatorName();
        });

        // 应用内饰和外观
        if (desktop == null || exterior == null) {
            DefaultThemes.getRandom().apply(builder);
        } else {
            builder.desktop(desktop);
            builder.exterior(exterior);
        }

        // 创建
        ServerTardis tardis = ServerTardisManager.getInstance().create(builder);
        if (tardis == null) {
            source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.creation_failed"));
            return 0;
        }

        tardis.getDesktop().getDoorPos(); // 触发加载

        Text feedback = Text.literal("")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.created")).append("\n")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.uuid", tardis.getUuid())).append("\n")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.desktop", tardis.getDesktop().getSchema().name())).append("\n")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.exterior", tardis.getExterior().getVariant().name())).append("\n")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.owner", finalOwnerName)).append("\n")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.subsystem_mode",
                        Text.translatable("tooltip.doctor_m.ait_tardisbuilder.subsystem_mode." + finalSubsystemMode.getId()))).append("\n")
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.position", tardisPos.getPos().toShortString()));

        source.sendFeedback(() -> feedback, true);

        return 1;
    }

    /**
     * 根据模式应用子系统状态
     */
    private static void applySubSystemMode(SubSystemHandler handler, SubSystemMode mode) {
        switch (mode) {
            case FULL -> {
                // 全满：所有子系统启用且耐久度满
                for (SubSystem sub : handler) {
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(DurableSubSystem.MAX_DURABILITY);
                    }
                    sub.setEnabled(true);
                }
            }
            case ESSENTIAL -> {
                // 必要：仅启用必要子系统
                for (SubSystem sub : handler) {
                    boolean isEssential = ESSENTIAL_SUBSYSTEMS.contains(sub.getId());
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(isEssential ? DurableSubSystem.MAX_DURABILITY : 0);
                    }
                    sub.setEnabled(isEssential);
                }
            }
            case ENGINE_ONLY -> {
                // 仅引擎
                for (SubSystem sub : handler) {
                    boolean isEngine = sub.getId() == SubSystem.Id.ENGINE;
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(isEngine ? DurableSubSystem.MAX_DURABILITY : 0);
                    }
                    sub.setEnabled(isEngine);
                }
            }
            case NONE -> {
                // 无：所有子系统禁用
                for (SubSystem sub : handler) {
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(0);
                    }
                    sub.setEnabled(false);
                }
            }
        }
    }

    // ===== 补全建议 =====
    private static final SuggestionProvider<ServerCommandSource> DESKTOP_SUGGESTIONS = (ctx, builder) -> {
        for (TardisDesktopSchema schema : DesktopRegistry.getInstance().toList()) {
            builder.suggest(IdMappingUtil.toMapping(schema.id()));
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> EXTERIOR_SUGGESTIONS = (ctx, builder) -> {
        for (ExteriorVariantSchema schema : ExteriorVariantRegistry.getInstance().toList()) {
            builder.suggest(IdMappingUtil.toMapping(schema.id()));
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> OWNER_SUGGESTIONS = (ctx, builder) -> {
        for (ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
            builder.suggest(player.getName().getString());
        }
        builder.suggest("Doctor");
        builder.suggest("Master");
        builder.suggest("Mary.Jin");
        builder.suggest("Evereye");
        builder.suggest("SmallMoss");
        builder.suggest("Mobius");
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> SUBSYSTEM_SUGGESTIONS = (ctx, builder) -> {
        for (String id : SubSystemMode.ids()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    };
}