package doctor_m.util.config;

public class ModConfig {

    //涡旋背景
    public boolean enableVortexTitleBackground = true; // 默认开启

    //护盾半径
    public double shieldHalfSize = 4.0;  // 护盾立方体半边长（总边长 = 2 * 此值），默认 4 格

    //时间钥匙
    public static final int DEFAULT_TIME_KEY_DAMAGE = 20;//冷却
    public static final double DEFAULT_TIME_KEY_MULTIPLIER = 1.0;//扣血倍率
    public static final double DEFAULT_TIME_KEY_EXTRA = 10.0;//最低扣血量（百分比）

    //永恒水晶
    public static final int DEFAULT_ETERNAL_CRYSTAL_DAMAGE = 100;//冷却
    public static final double DEFAULT_ETERNAL_CRYSTAL_MULTIPLIER = 0.5;//扣血倍率
    public static final double DEFAULT_ETERNAL_CRYSTAL_EXTRA = 2.5;//最低扣血量（百分比）

    //特莉波卡的镰刀
    public static final int DEFAULT_TLIPOCA_SCYTHE_DAMAGE = 30;//冷却
    public static final double DEFAULT_TLIPOCA_SCYTHE_MULTIPLIER = 1.0;//扣血倍率
    public static final double DEFAULT_TLIPOCA_SCYTHE_EXTRA = 25.0;//最低扣血量（百分比）
    public boolean enableSlashSystem = true;         // 总开关
    public long slashCooldownTicks = 60;             // 冷却时间（tick）
    public float slashDamage = 400.0f;               // 伤害值
    public double slashReach = 8.0;                  // 攻击距离
    public double slashWidth = 5.0;                  // 攻击范围宽度
    public double slashHeight = 3.0;                 // 攻击范围高度

    //塔迪斯自毁
    public boolean enableSelfDestructEnhancement = true;  // 总开关
    public int selfDestructMaxRadius = 80;                // 最大扩散半径
    public int selfDestructExplosionSteps = 20;           // 扩散步数
    public int selfDestructDelayPerStep = 20;              // 每步间隔（tick）
    public int selfDestructFinalClearRadius = 100;        // 最终清除半径
    public int selfDestructKnockbackRadius = 30;          // 击退影响半径
    public double selfDestructKnockbackForce = 2.5;       // 击退力度

    //氧气瓶
    public double oxygenTankMaxOxygen = 1200.0;          // 最大氧气容量
    public double oxygenTankTransferRate = 100.0;        // 每次转移的氧气量
    public int oxygenTankFoodThreshold = 6;              // 饱食度阈值（<= 此值视为极低）
    public int oxygenTankHoldTicksForAchievement = 100;  // 长按多少 tick 触发成就（5秒 = 100 tick）

    //氧气补充机
    public int oxygenChargerCooldownSeconds = 32;      // 充能冷却时间（秒）
    public double spacesuitMaxOxygen = 1200.0;           // 宇航服最大氧气容量

    //氧气消耗
    public double spacesuitOxygenConsumeUnderwater = 0.5;   // 水下每2秒消耗的氧气量
    public double spacesuitOxygenConsumeSpace = 1.0;        // 太空无氧环境每3秒消耗的氧气量

    //氧气机
    public int oxygenatorBiologicalDetectionRadius = 16;       // 检测生物的半径（格）
    public int oxygenatorMaxSearchSize = 5000;                 // 最大搜索空气方块数
    public int oxygenatorMinAirBlocks = 10;                    // 最小空气方块数（判定为封闭空间）
    public int oxygenatorCacheExpireTicks = 40;                // 房间缓存过期时间（tick）

    // --- 实际配置值 ---
    public int timeKeyDamage = DEFAULT_TIME_KEY_DAMAGE;
    public double timeKeyMultiplier = DEFAULT_TIME_KEY_MULTIPLIER;
    public double timeKeyExtra = DEFAULT_TIME_KEY_EXTRA;

    public int eternalCrystalDamage = DEFAULT_ETERNAL_CRYSTAL_DAMAGE;
    public double eternalCrystalMultiplier = DEFAULT_ETERNAL_CRYSTAL_MULTIPLIER;
    public double eternalCrystalExtra = DEFAULT_ETERNAL_CRYSTAL_EXTRA;

    public int tlipocaScytheDamage = DEFAULT_TLIPOCA_SCYTHE_DAMAGE;
    public double tlipocaScytheMultiplier = DEFAULT_TLIPOCA_SCYTHE_MULTIPLIER;
    public double tlipocaScytheExtra = DEFAULT_TLIPOCA_SCYTHE_EXTRA;
}