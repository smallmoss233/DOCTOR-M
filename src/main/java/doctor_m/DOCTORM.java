package doctor_m;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.Registry;

public class DOCTORM implements ModInitializer {
	public static final String MOD_ID = "doctor-m";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Block one_BLOCK = new Block(FabricBlockSettings.create().strength(4.0f));

	@Override
	public void onInitialize() {
        Registry.register(Registries.BLOCK,new Identifier("doctor_m", "one_block"),one_BLOCK);

		LOGGER.info("Doctor M Mod 已加载完成！");
	}
}