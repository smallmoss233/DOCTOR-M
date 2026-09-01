package doctor_m.config;

public class ModConfig {

    //涡旋背景
    public boolean enableVortexTitleBackground = true; // 默认开启

    //TT护盾半径
    public double shieldHalfSize = 4.0;  // 护盾立方体半边长（总边长 = 2 * 此值），默认 4 格

    //力场盾牌
    public int forceFieldMaxEnergy = 1500;            // 能量上限
    public int forceFieldDrainPerTick = 2;              // 开启时每 tick 消耗
    public int forceFieldRechargePerTick = 1;           // 未使用时每 tick 恢复
    public double forceFieldPushStrength = 0.25;        // 持续力场推开力度
    public double forceFieldReleaseRadius = 5.0;        // 关闭时弹开半径
    public double forceFieldReleaseStrength = 1.2;      // 关闭时水平弹开力度
    public double forceFieldReleaseUpward = 0.4;        // 关闭时向上击飞力度
    public int forceFieldCooldownTicks = 40;            // 主动关闭后冷却时间（tick，40=2秒）
    public double forceFieldEnvironmentalDamageMultiplier = 0.1; // 环境伤害减免比例（保留10%）
    public boolean forceFieldBlockAllNonEnvironmental = true;     // 是否完全格挡非环境伤害

    //护盾生成器
    public int shieldMaxEnergy = 1000;              // 护盾能量总容量
    public int shieldRechargePerTick = 1;           // 每 tick 恢复的能量
    public int shieldCostPerDamage = 5;             // 每 1 点伤害消耗的能量

    //时间钥匙
    public static final int KeytoTimeDamage = 1;//冷却
    public static final double KeytoTimeMultiplier = 2;//扣血倍率
    public static final double KeytoTimeExtra = 15.0;//最低扣血量（百分比）

    //永恒水晶
    public static final int eternalCrystalDamage = 100;//冷却
    public static final double eternalCrystalMultiplier = 0.5;//扣血倍率
    public static final double eternalCrystalExtra = 2.5;//最低扣血量（百分比）

    //特莉波卡的镰刀
    public static final int tlipocaScytheDamage = 30;//冷却
    public static final double tlipocaScytheMultiplier = 1.0;//扣血倍率
    public static final double tlipocaScytheExtra = 25.0;//最低扣血量（百分比）
    public float slashDamage = 500.0f;               // 右键伤害值
    public double tlipocaScytheAoeRadius = 5.0;                  // 伤害共享/AoE 半径
    public int tlipocaScytheFoodBase = 1;                        // 饥饿回复基础值
    public double tlipocaScytheSaturationMultiplier = 0.5;       // 饱和度系数
    public double tlipocaScytheExecuteHealRatio = 0.6;           // 处决吸血比例
    public double tlipocaScytheNormalHealRatio = 0.25;           // 普通吸血比例
    public boolean tlipocaScytheExecuteAoEDamageIgnoresArmor = true; // 处决 AoE 是否无视护甲/减伤

    //STCS
    public int stcsMinEnergyCost = 1;                            // 格挡最低能量消耗
    public double stcsAoeRadius = 3.0;                           // STCS 范围伤害半径

    //塔迪斯自毁
    public boolean enableSelfDestructEnhancement = true;  // 总开关
    public int selfDestructMaxRadius = 80;                // 最大扩散半径
    public int selfDestructExplosionSteps = 20;           // 扩散步数
    public int selfDestructDelayPerStep = 40;              // 每步间隔（tick）
    public int selfDestructFinalClearRadius = 100;        // 最终清除半径
    public int selfDestructKnockbackRadius = 2;          // 击退影响半径
    public double selfDestructKnockbackForce = 0.5;       // 击退力度

    //氧气瓶
    public double oxygenTankMaxOxygen = 1200.0;          // 氧气瓶最大氧气容量
    public double oxygenTankTransferRate = 100.0;        // 每次转移的氧气量
    public int oxygenTankFoodThreshold = 6;              // 饱食度阈值（<= 此值视为极低）
    public int oxygenTankHoldTicksForAchievement = 100;  // 长按多少 tick 触发成就（5秒 = 100 tick）

    //氧气补充机
    public int oxygenChargerCooldownSeconds = 32;      // 充能冷却时间（秒）

    //宇航服
    public double spacesuitMaxOxygen = 1200.0;           // 宇航服最大氧气容量
    public double spacesuitOxygenConsumeUnderwater = 0.5;   // 水下每2秒消耗的氧气量
    public double spacesuitOxygenConsumeSpace = 1.0;        // 太空无氧环境每3秒消耗的氧气量

    //氧气机
    public int oxygenatorMaxRadius = 48;              // 最大供氧半径（格）
    public int oxygenatorOpenSpaceRadius = 3;         // 开放空间有效半径
    public int oxygenatorCacheExpireTicks = 100;      // 缓存过期（5秒）
    public int oxygenatorMinAirBlocks = 10;           // 最小有效房间大小

    //真空进食
    public double vacuumEatingOxygenCost = 100.0;         // 真空环境吃一次食物扣多少氧
    public int vacuumEatingPendingTimeoutSeconds = 10;    // 进食 pending 超时（秒）

    //玩具匠的锤子
    public int toymakerHammerCopyChunkRadius = 20;
    public int toymakerHammerSpawnOffsetBlocks = 2;
    public double toymakerHammerReachDistance = 5.0;
    public int toymakerHammerBlockUpdateFlags = 2 | 16;
    public boolean toymakerHammerCopyEntities = true;
    public boolean toymakerHammerCopyBlockEntities = true;

    //追踪器
    public double tracerScanRange = 45.0;      // 手持自动扫描半径（格）
    public int tracerContainerScanRange = 45; // 右键扫描容器半径（格）

    //涡旋操纵器
    public int vortexManipulatorMaxFuel = 1500;                // 最大燃料
    public int vortexManipulatorMaxOverheat = 100;             // 最大过热
    public int vortexManipulatorCooldownTicks = 1200;          // 普通冷却（tick，1200=60秒）
    public long vortexManipulatorBrokenCooldownTicks = 72000L; // 损坏恢复时间（3游戏日）
    public int vortexManipulatorCoolingIntervalTicks = 80;     // 散热间隔（tick）
    public int vortexManipulatorCoolingPerInterval = 1;        // 每次散热减少的过热量
}