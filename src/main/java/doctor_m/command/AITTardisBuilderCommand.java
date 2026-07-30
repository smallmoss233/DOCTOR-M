package doctor_m.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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

import java.util.Set;

public class AITTardisBuilderCommand {

    public enum SubSystemMode {
        FULL("full"),
        ESSENTIAL("essential"),
        ENGINE_ONLY("only_engine"),
        NONE("none");

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

    private static final Set<SubSystem.IdLike> ESSENTIAL_SUBSYSTEMS = Set.of(
            SubSystem.Id.ENGINE,
            SubSystem.Id.LIFE_SUPPORT,
            SubSystem.Id.STABILISERS,
            SubSystem.Id.DEMAT,
            SubSystem.Id.CHAMELEON
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("doctor_m")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("build")
                        .executes(AITTardisBuilderCommand::executeDefault)

                        // /doctor_m build <desktop>
                        .then(CommandManager.argument("desktop", StringArgumentType.string())
                                .suggests(DESKTOP_SUGGESTIONS)
                                .executes(ctx -> executeWithArgs(ctx,
                                        getString(ctx, "desktop"), null, null, null, SubSystemMode.FULL, null, null))

                                // /doctor_m build <desktop> <exterior>
                                .then(CommandManager.argument("exterior", StringArgumentType.string())
                                        .suggests(EXTERIOR_SUGGESTIONS)
                                        .executes(ctx -> executeWithArgs(ctx,
                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                null, null, SubSystemMode.FULL, null, null))

                                        // /doctor_m build <desktop> <exterior> <owner>
                                        .then(CommandManager.argument("owner", StringArgumentType.string())
                                                .suggests(OWNER_SUGGESTIONS)
                                                .executes(ctx -> executeWithArgs(ctx,
                                                        getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                        getString(ctx, "owner"), null, SubSystemMode.FULL, null, null))

                                                // ===== 分支 A: 直接跟子系统模式 =====
                                                .then(CommandManager.argument("subsystem", StringArgumentType.string())
                                                        .suggests(SUBSYSTEM_SUGGESTIONS)
                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                getString(ctx, "owner"), null,
                                                                parseSubsystem(ctx), null, null))
                                                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                                .executes(ctx -> executeWithArgs(ctx,
                                                                        getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                        getString(ctx, "owner"), null,
                                                                        parseSubsystem(ctx),
                                                                        Vec3ArgumentType.getVec3(ctx, "pos"), null))
                                                                .then(CommandManager.argument("executor", StringArgumentType.string())
                                                                        .suggests(EXECUTOR_SUGGESTIONS)
                                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                getString(ctx, "owner"), null,
                                                                                parseSubsystem(ctx),
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                getString(ctx, "executor"))))))

                                                // ===== 分支 B: name <名称> =====
                                                .then(CommandManager.literal("name")
                                                        .then(CommandManager.argument("name", StringArgumentType.string())
                                                                .suggests(NAME_SUGGESTIONS)
                                                                .executes(ctx -> executeWithArgs(ctx,
                                                                        getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                        getString(ctx, "owner"), getString(ctx, "name"),
                                                                        SubSystemMode.FULL, null, null))

                                                                // /... name <名称> <subsystem>
                                                                .then(CommandManager.argument("subsystem", StringArgumentType.string())
                                                                        .suggests(SUBSYSTEM_SUGGESTIONS)
                                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                getString(ctx, "owner"), getString(ctx, "name"),
                                                                                parseSubsystem(ctx), null, null))
                                                                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                                                .executes(ctx -> executeWithArgs(ctx,
                                                                                        getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                        getString(ctx, "owner"), getString(ctx, "name"),
                                                                                        parseSubsystem(ctx),
                                                                                        Vec3ArgumentType.getVec3(ctx, "pos"), null))
                                                                                .then(CommandManager.argument("executor", StringArgumentType.string())
                                                                                        .suggests(EXECUTOR_SUGGESTIONS)
                                                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                                getString(ctx, "owner"), getString(ctx, "name"),
                                                                                                parseSubsystem(ctx),
                                                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                                getString(ctx, "executor"))))))

                                                                // /... name <名称> <pos> (无 subsystem，默认 full)
                                                                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                getString(ctx, "owner"), getString(ctx, "name"),
                                                                                SubSystemMode.FULL,
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"), null))
                                                                        .then(CommandManager.argument("executor", StringArgumentType.string())
                                                                                .suggests(EXECUTOR_SUGGESTIONS)
                                                                                .executes(ctx -> executeWithArgs(ctx,
                                                                                        getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                        getString(ctx, "owner"), getString(ctx, "name"),
                                                                                        SubSystemMode.FULL,
                                                                                        Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                        getString(ctx, "executor"))))))

                                                        // ===== 分支 C: 直接跟坐标（默认名称 + 默认子系统）=====
                                                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                                .executes(ctx -> executeWithArgs(ctx,
                                                                        getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                        getString(ctx, "owner"), null, SubSystemMode.FULL,
                                                                        Vec3ArgumentType.getVec3(ctx, "pos"), null))
                                                                .then(CommandManager.argument("executor", StringArgumentType.string())
                                                                        .suggests(EXECUTOR_SUGGESTIONS)
                                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                                getString(ctx, "desktop"), getString(ctx, "exterior"),
                                                                                getString(ctx, "owner"), null, SubSystemMode.FULL,
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                                getString(ctx, "executor")))))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static SubSystemMode parseSubsystem(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String sub = StringArgumentType.getString(ctx, "subsystem");
        SubSystemMode mode = SubSystemMode.fromString(sub);
        if (mode == null) {
            throw new SimpleCommandExceptionType(
                    Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.invalid_subsystem_mode", sub)
            ).create();
        }
        return mode;
    }

    private static String getString(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int executeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return executeWithArgs(ctx, null, null, null, null, SubSystemMode.FULL, null, null);
    }

    private static int executeWithArgs(CommandContext<ServerCommandSource> ctx,
                                       String desktopRaw, String exteriorRaw,
                                       String ownerRaw, String nameRaw,
                                       SubSystemMode subsystemMode, Vec3d pos,
                                       String executorRaw) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();

        ServerPlayerEntity commandSender = null;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            commandSender = player;
        }

        // ===== 解析执行玩家 =====
        ServerPlayerEntity executor;
        if (executorRaw != null && !executorRaw.isEmpty()) {
            executor = world.getServer().getPlayerManager().getPlayer(executorRaw);
            if (executor == null) {
                source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.unknown_executor", executorRaw));
                return 0;
            }
        } else {
            // 没指定 executor，必须是玩家亲自执行的
            if (commandSender == null) {
                source.sendError(Text.literal("Console execution requires <executor> argument."));
                return 0;
            }
            executor = commandSender;
        }

        // 位置：若未指定坐标，则从执行玩家位置生成
        CachedDirectedGlobalPos tardisPos;
        if (pos != null) {
            BlockPos blockPos = BlockPos.ofFloored(pos);
            tardisPos = CachedDirectedGlobalPos.create(world, blockPos, (byte) Direction.NORTH.getId());
        } else {
            BlockPos playerPos = executor.getBlockPos().up(2);
            tardisPos = CachedDirectedGlobalPos.create(world, playerPos,
                    DirectionControl.getGeneralizedRotation(
                            RotationPropertyHelper.fromYaw(executor.getBodyYaw())));
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

        // ===== 主人：支持 me，和执行玩家串联 =====
        ServerPlayerEntity ownerPlayer = null;
        String ownerName = executor.getName().getString();
        if (ownerRaw != null && !ownerRaw.isEmpty()) {
            if (ownerRaw.equalsIgnoreCase("me")) {
                ownerPlayer = executor;
                ownerName = executor.getName().getString();
            } else {
                ownerPlayer = world.getServer().getPlayerManager().getPlayer(ownerRaw);
                if (ownerPlayer != null) {
                    ownerName = ownerPlayer.getName().getString();
                } else {
                    ownerName = ownerRaw;
                }
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

        if (desktop == null && exterior == null) {
            DefaultThemes.getRandom().apply(builder);
        } else {
            if (desktop != null) builder.desktop(desktop);
            if (exterior != null) builder.exterior(exterior);
        }

        ServerTardis tardis = ServerTardisManager.getInstance().create(builder);
        if (tardis == null) {
            source.sendError(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.error.creation_failed"));
            return 0;
        }

        if (nameRaw != null && !nameRaw.isEmpty()) {
            tardis.stats().setName(nameRaw);
        } else {
            tardis.stats().setName(finalOwnerName + "'s TARDIS");
        }

        tardis.getDesktop().getDoorPos();

        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.created"), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.uuid", tardis.getUuid()), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.name", tardis.stats().getName()), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.desktop", tardis.getDesktop().getSchema().name()), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.exterior", tardis.getExterior().getVariant().name()), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.owner", finalOwnerName), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.subsystem_mode",
                Text.translatable("tooltip.doctor_m.ait_tardisbuilder.subsystem_mode." + finalSubsystemMode.getId())), true);
        source.sendFeedback(() -> Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.position", tardisPos.getPos().toShortString()), true);

        return 1;
    }

    private static void applySubSystemMode(SubSystemHandler handler, SubSystemMode mode) {
        switch (mode) {
            case FULL -> {
                for (SubSystem sub : handler) {
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(DurableSubSystem.MAX_DURABILITY);
                    }
                    sub.setEnabled(true);
                }
            }
            case ESSENTIAL -> {
                for (SubSystem sub : handler) {
                    boolean isEssential = ESSENTIAL_SUBSYSTEMS.contains(sub.getId());
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(isEssential ? DurableSubSystem.MAX_DURABILITY : 0);
                    }
                    sub.setEnabled(isEssential);
                }
            }
            case ENGINE_ONLY -> {
                for (SubSystem sub : handler) {
                    boolean isEngine = sub.getId() == SubSystem.Id.ENGINE;
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(isEngine ? DurableSubSystem.MAX_DURABILITY : 0);
                    }
                    sub.setEnabled(isEngine);
                }
            }
            case NONE -> {
                for (SubSystem sub : handler) {
                    if (sub instanceof DurableSubSystem durable) {
                        durable.setDurability(0);
                    }
                    sub.setEnabled(false);
                }
            }
        }
    }

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
        for (ServerPlayerEntity p : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
            builder.suggest(p.getName().getString());
        }
        builder.suggest("me");
        builder.suggest("Doctor");
        builder.suggest("Master");
        builder.suggest("Mary.Jin");
        builder.suggest("Marian.jin");
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

    private static final SuggestionProvider<ServerCommandSource> NAME_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("Lolita");
        builder.suggest("Marian");
        builder.suggest("Marian.jin");
        builder.suggest("Evereye");
        builder.suggest("Mobius");
        builder.suggest("Sexy");
        builder.suggest("Idris");
        builder.suggest("Watcher");
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> EXECUTOR_SUGGESTIONS = (ctx, builder) -> {
        for (ServerPlayerEntity p : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
            builder.suggest(p.getName().getString());
        }
        builder.suggest("me");
        return builder.buildFuture();
    };
}