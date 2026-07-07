package doctor_m.util.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.amble.ait.api.tardis.TardisComponent;
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

public class AITTardisBuilderCommand {

    private static final Random RANDOM = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("doctor_m")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("build")
                        .executes(AITTardisBuilderCommand::executeDefault)
                        .then(CommandManager.argument("desktop", StringArgumentType.string())
                                .suggests(DESKTOP_SUGGESTIONS)
                                .executes(ctx -> executeWithArgs(ctx,
                                        StringArgumentType.getString(ctx, "desktop"),
                                        null, null, null))
                                .then(CommandManager.argument("exterior", StringArgumentType.string())
                                        .suggests(EXTERIOR_SUGGESTIONS)
                                        .executes(ctx -> executeWithArgs(ctx,
                                                StringArgumentType.getString(ctx, "desktop"),
                                                StringArgumentType.getString(ctx, "exterior"),
                                                null, null))
                                        .then(CommandManager.argument("owner", StringArgumentType.string())
                                                .suggests(OWNER_SUGGESTIONS)
                                                .executes(ctx -> executeWithArgs(ctx,
                                                        StringArgumentType.getString(ctx, "desktop"),
                                                        StringArgumentType.getString(ctx, "exterior"),
                                                        StringArgumentType.getString(ctx, "owner"),
                                                        null))
                                                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                        .executes(ctx -> executeWithArgs(ctx,
                                                                StringArgumentType.getString(ctx, "desktop"),
                                                                StringArgumentType.getString(ctx, "exterior"),
                                                                StringArgumentType.getString(ctx, "owner"),
                                                                Vec3ArgumentType.getVec3(ctx, "pos")))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int executeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return executeWithArgs(ctx, null, null, null, null);
    }

    private static int executeWithArgs(CommandContext<ServerCommandSource> ctx,
                                       String desktopRaw, String exteriorRaw,
                                       String ownerRaw, Vec3d pos) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();

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
        builder.with(TardisComponent.Id.SUBSYSTEM, (SubSystemHandler sub) -> sub.repairAll());

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
                .append(Text.translatable("tooltip.doctor_m.ait_tardisbuilder.success.position", tardisPos.getPos().toShortString()));

        source.sendFeedback(() -> feedback, true);

        return 1;
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
}