package doctor_m;

//import doctor_m.init.DOCTORMitems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOCTORM implements ModInitializer {
	public static final String MOD_ID = "doctor-m";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	@Override
	public void onInitialize() {
        LOGGER.info("开始初始化 Doctor M ...");

        //RegistryContainer.register(DOCTORMitems.class, "doctor_m");


        LOGGER.info("Doctor M 已加载完成！");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}