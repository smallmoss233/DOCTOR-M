package doctor_m.mixin.client.stp;

import doctor_m.client.module.stp.STPWorldRenderer;
import doctor_m.module.STP;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements STPWorldRenderer {

    @Shadow private @Nullable ChunkBuilder chunkBuilder;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow private @Nullable ClientWorld world;
    @Shadow private @Nullable BuiltChunkStorage chunks;
    @Shadow @Final private MinecraftClient client;
    @Shadow private int cameraChunkX;
    @Shadow private int cameraChunkY;
    @Shadow private int cameraChunkZ;
    @Shadow private double lastCameraChunkUpdateX;
    @Shadow private double lastCameraChunkUpdateY;
    @Shadow private double lastCameraChunkUpdateZ;
    @Shadow @Final private Set<BlockEntity> noCullingBlockEntities;
    @Shadow @Final private ObjectArrayList<WorldRenderer.ChunkInfo> chunkInfos;
    @Shadow @Final private AtomicReference<WorldRenderer.RenderableChunks> renderableChunks;
    @Shadow @Final private AtomicBoolean updateFinished;

    /** ★ 用 WorldRenderer 实际持有的 BufferBuilderStorage */
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Override
    public void stp$setWorld(ClientWorld world) {
        // ==================== 世界卸载 ====================
        if (world == null) {
            if (this.chunks != null) {
                this.chunks.clear();
                this.chunks = null;
            }
            if (this.chunkBuilder != null) {
                ChunkBuilder old = this.chunkBuilder;
                this.chunkBuilder = null;
                CompletableFuture.runAsync(() -> {
                    try { old.stop(); } catch (Throwable t) {
                        STP.LOGGER.warn("Background chunkBuilder.stop() failed", t);
                    }
                });
            }
            this.renderableChunks.set(null);
            this.chunkInfos.clear();
            this.noCullingBlockEntities.clear();
            this.entityRenderDispatcher.setWorld(null);
            this.world = null;
            return;
        }

        // ==================== 世界切换 ====================

        // 1. 立即清空可见列表
        this.renderableChunks.set(null);
        this.chunkInfos.clear();
        this.noCullingBlockEntities.clear();

        // 2. 保存旧引用
        BuiltChunkStorage oldChunks = this.chunks;
        ChunkBuilder oldBuilder = this.chunkBuilder;

        // 3. 更新世界引用
        this.entityRenderDispatcher.setWorld(world);
        this.world = world;

        // 4. 主线程快速创建新的 ChunkBuilder 和 BuiltChunkStorage
        //    ★ 从 bufferBuilders 里取 BlockBufferBuilderStorage
        ChunkBuilder newBuilder = new ChunkBuilder(
                world,
                (WorldRenderer) (Object) this,
                Util.getMainWorkerExecutor(),
                this.client.is64Bit(),
                this.bufferBuilders.getBlockBufferBuilders());

        BuiltChunkStorage newChunks = new BuiltChunkStorage(
                newBuilder,
                world,
                this.client.options.getViewDistance().getValue(),
                (WorldRenderer) (Object) this);

        this.chunkBuilder = newBuilder;
        this.chunks = newChunks;

        // 5. 强制所有 BuiltChunk 重建
        if (newChunks.chunks != null) {
            int scheduled = 0;
            for (ChunkBuilder.BuiltChunk chunk : newChunks.chunks) {
                if (chunk == null) continue;
                try {
                    chunk.scheduleRebuild(false);
                    scheduled++;
                } catch (Throwable t) {
                    STP.LOGGER.warn("Failed to schedule rebuild for BuiltChunk", t);
                }
            }
            STP.LOGGER.debug("stp$setWorld: scheduled rebuild for {} BuiltChunks", scheduled);
        }

        // 6. 重置相机缓存
        this.cameraChunkX = Integer.MIN_VALUE;
        this.cameraChunkY = Integer.MIN_VALUE;
        this.cameraChunkZ = Integer.MIN_VALUE;
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;

        this.updateFinished.set(true);

        // 7. 主线程释放旧 VBO
        if (oldChunks != null) {
            try {
                oldChunks.clear();
            } catch (Throwable t) {
                STP.LOGGER.warn("Failed to clear old chunks on main thread", t);
            }
        }

        // 8. 后台异步停止旧 worker 线程
        if (oldBuilder != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    oldBuilder.stop();
                } catch (Throwable t) {
                    STP.LOGGER.warn("Background chunkBuilder.stop() failed", t);
                }
            });
        }

        STP.LOGGER.debug("stp$setWorld: fast-swapped to {}",
                world.getRegistryKey().getValue());
    }
}