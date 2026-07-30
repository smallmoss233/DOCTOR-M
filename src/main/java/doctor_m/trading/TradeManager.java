package doctor_m.trading;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<TradeOffer> loadPoolFromDatapack(MinecraftServer server, String filename) {
        Identifier id = new Identifier("doctor_m", "trades/" + filename);
        ResourceManager resourceManager = server.getResourceManager();

        try {
            var resourceOpt = resourceManager.getResource(id);
            if (resourceOpt.isPresent()) {
                try (Reader reader = new InputStreamReader(resourceOpt.get().getInputStream())) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    return parsePool(json);
                }
            } else {
                System.err.println("[TradeManager] 找不到数据包资源: " + id);
            }
        } catch (Exception e) {
            System.err.println("[TradeManager] 读取交易配置失败: " + id);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static List<TradeOffer> parsePool(JsonObject json) {
        List<TradeOffer> offers = new ArrayList<>();
        if (!json.has("pool")) {
            System.err.println("[TradeManager] 交易配置缺少 'pool' 数组");
            return offers;
        }

        JsonArray pool = json.getAsJsonArray("pool");
        for (var elem : pool) {
            JsonObject obj = elem.getAsJsonObject();
            try {
                Item input = Registries.ITEM.get(new Identifier(obj.get("input").getAsString()));
                Item output = Registries.ITEM.get(new Identifier(obj.get("output").getAsString()));
                int inCount = obj.has("inputCount") ? obj.get("inputCount").getAsInt() : 1;
                int outCount = obj.has("outputCount") ? obj.get("outputCount").getAsInt() : 1;
                int maxUses = obj.has("maxUses") ? obj.get("maxUses").getAsInt() : 64;

                // 新增：解析输出 NBT
                NbtCompound outputNbt = null;
                if (obj.has("outputNbt")) {
                    String nbtString = obj.get("outputNbt").getAsString();
                    try {
                        outputNbt = StringNbtReader.parse(nbtString);
                    } catch (CommandSyntaxException e) {
                        System.err.println("[TradeManager] NBT 解析失败: " + nbtString);
                        e.printStackTrace();
                    }
                }

                if (input != null && output != null) {
                    offers.add(new TradeOffer(input, inCount, output, outCount, maxUses, outputNbt));
                }
            } catch (Exception e) {
                System.err.println("[TradeManager] 解析交易条目失败: " + obj);
            }
        }
        return offers;
    }

    public static List<TradeOffer> generateDailyTrades(List<TradeOffer> pool, Random random) {
        if (pool.isEmpty()) return new ArrayList<>();
        int max = Math.min(6, pool.size());
        int count = random.nextInt(max + 1);
        List<TradeOffer> shuffled = new ArrayList<>(pool);

        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(shuffled, i, j);
        }

        List<TradeOffer> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TradeOffer t = shuffled.get(i);
            selected.add(new TradeOffer(t.getInputItem(), t.getInputCount(),
                    t.getOutputItem(), t.getOutputCount(), t.getMaxUses(),
                    t.getOutputNbt()));
        }
        return selected;
    }

    public static net.minecraft.nbt.NbtList writeOffersToNbt(List<TradeOffer> offers) {
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (TradeOffer offer : offers) {
            list.add(offer.toNbt());
        }
        return list;
    }

    public static List<TradeOffer> readOffersFromNbt(net.minecraft.nbt.NbtList list) {
        List<TradeOffer> offers = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            offers.add(TradeOffer.fromNbt(list.getCompound(i)));
        }
        return offers;
    }
}