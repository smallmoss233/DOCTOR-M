package doctor_m.config;

public class ModConfig {

    //涡旋背景
    @ConfigGroup(ConfigGroups.VORTEX)
    public boolean enableVortexTitleBackground = true;               // 默认开启

    //STP 无缝传送技术
    @ConfigGroup(ConfigGroups.STP)
    public boolean seamlessTeleportEnabled = true;

    //TT 护盾半径
    @ConfigGroup(ConfigGroups.TT_SHIELD)
    public double shieldHalfSize = 4.0;                              // 护盾立方体半边长（总边长 = 2 * 此值），默认 4 格

    //力场盾牌
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public int forceFieldMaxEnergy = 1500;                           // 能量上限
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public int forceFieldDrainPerTick = 2;                           // 开启时每 tick 消耗
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public int forceFieldRechargePerTick = 1;                        // 未使用时每 tick 恢复
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public double forceFieldPushStrength = 0.25;                     // 持续力场推开力度
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public double forceFieldReleaseRadius = 5.0;                     // 关闭时弹开半径
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public double forceFieldReleaseStrength = 1.2;                   // 关闭时水平弹开力度
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public double forceFieldReleaseUpward = 0.4;                     // 关闭时向上击飞力度
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public int forceFieldCooldownTicks = 40;                         // 主动关闭后冷却时间（tick，40=2 秒）
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public double forceFieldEnvironmentalDamageMultiplier = 0.1;     // 环境伤害减免比例（保留 10%）
    @ConfigGroup(ConfigGroups.FORCE_FIELD)
    public boolean forceFieldBlockAllNonEnvironmental = true;        // 是否完全格挡非环境伤害

    //护盾生成器
    @ConfigGroup(ConfigGroups.SHIELD_GENERATOR)
    public int shieldMaxEnergy = 1000;                               // 护盾能量总容量
    @ConfigGroup(ConfigGroups.SHIELD_GENERATOR)
    public int shieldRechargePerTick = 1;                            // 每 tick 恢复的能量
    @ConfigGroup(ConfigGroups.SHIELD_GENERATOR)
    public int shieldCostPerDamage = 5;                              // 每 1 点伤害消耗的能量

    //时间钥匙
    @ConfigGroup(ConfigGroups.TIME_KEY)
    public int keytoTimeDamage = 1;                                  // 冷却（tick）
    @ConfigGroup(ConfigGroups.TIME_KEY)
    public double keytoTimeMultiplier = 2.0;                         // 扣血倍率
    @ConfigGroup(ConfigGroups.TIME_KEY)
    public double keytoTimeExtra = 15.0;                             // 最低扣血量（百分比）

    //永恒水晶
    @ConfigGroup(ConfigGroups.ETERNAL_CRYSTAL)
    public int eternalCrystalDamage = 100;                           // 冷却（tick）
    @ConfigGroup(ConfigGroups.ETERNAL_CRYSTAL)
    public double eternalCrystalMultiplier = 0.5;                    // 扣血倍率
    @ConfigGroup(ConfigGroups.ETERNAL_CRYSTAL)
    public double eternalCrystalExtra = 2.5;                         // 最低扣血量（百分比）

    //[档案编号:46] 镰刀
    @ConfigGroup(ConfigGroups.SCYTHE)
    public int tlipocaScytheDamage = 30;                             // 冷却（tick）
    @ConfigGroup(ConfigGroups.SCYTHE)
    public double tlipocaScytheMultiplier = 1.0;                     // 扣血倍率
    @ConfigGroup(ConfigGroups.SCYTHE)
    public double tlipocaScytheExtra = 25.0;                         // 最低扣血量（百分比）
    @ConfigGroup(ConfigGroups.SCYTHE)
    public float slashDamage = 500.0f;                               // 右键伤害值
    @ConfigGroup(ConfigGroups.SCYTHE)
    public double tlipocaScytheAoeRadius = 5.0;                      // 伤害共享 / AoE 半径
    @ConfigGroup(ConfigGroups.SCYTHE)
    public int tlipocaScytheFoodBase = 1;                            // 饥饿回复基础值
    @ConfigGroup(ConfigGroups.SCYTHE)
    public double tlipocaScytheSaturationMultiplier = 0.5;           // 饱和度系数
    @ConfigGroup(ConfigGroups.SCYTHE)
    public double tlipocaScytheExecuteHealRatio = 0.6;               // 处决吸血比例
    @ConfigGroup(ConfigGroups.SCYTHE)
    public double tlipocaScytheNormalHealRatio = 0.25;               // 普通吸血比例
    @ConfigGroup(ConfigGroups.SCYTHE)
    public boolean tlipocaScytheExecuteAoEDamageIgnoresArmor = true; // 处决 AoE 是否无视护甲 / 减伤

    //SAR
    @ConfigGroup(ConfigGroups.SAR)
    public int sarMinEnergyCost = 1;                                // 格挡最低能量消耗
    @ConfigGroup(ConfigGroups.SAR)
    public double sarAoeRadius = 3.0;                               // SAR 范围伤害半径

    //塔迪斯自毁
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public boolean enableSelfDestructEnhancement = true;             // 总开关
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public int selfDestructMaxRadius = 80;                           // 最大扩散半径
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public int selfDestructExplosionSteps = 20;                      // 扩散步数
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public int selfDestructDelayPerStep = 40;                        // 每步间隔（tick）
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public int selfDestructFinalClearRadius = 100;                   // 最终清除半径
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public int selfDestructKnockbackRadius = 2;                      // 击退影响半径
    @ConfigGroup(ConfigGroups.SELF_DESTRUCT)
    public double selfDestructKnockbackForce = 0.5;                  // 击退力度

    //氧气瓶
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double oxygenTankMaxOxygen = 1200.0;                      // 氧气瓶最大氧气容量
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double oxygenTankTransferRate = 100.0;                    // 每次转移的氧气量
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public int oxygenTankFoodThreshold = 6;                          // 饱食度阈值（<= 此值视为极低）
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public int oxygenTankHoldTicksForAchievement = 100;              // 长按多少 tick 触发成就（5 秒 = 100 tick）
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double advancedOxygenTankCapacityMultiplier = 3.0;        // 高级氧气瓶容量倍率
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double superOxygenTankCapacityMultiplier = 5.0;           // 超级氧气瓶基于高级氧气瓶容量倍率
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double jetOxygenTankThrustStrength = 0.5;                 // 推力强度
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double jetOxygenTankInertia = 0.72;                       // 惯性保留比例
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double jetOxygenTankGravityCompensation = 0.12;           // 重力补偿
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double jetOxygenTankMaxSpeed = 8.0;                       // 最大水平速度
    @ConfigGroup(ConfigGroups.OXYGEN_TANK)
    public double jetOxygenTankMaxVerticalSpeed = 6.0;               // 最大垂直速度

    //氧气补充机
    @ConfigGroup(ConfigGroups.OXYGEN_CHARGER)
    public int oxygenChargerCooldownSeconds = 32;                    // 充能冷却时间（秒）

    //宇航服
    @ConfigGroup(ConfigGroups.SPACESUIT)
    public double spacesuitMaxOxygen = 1200.0;                       // 宇航服最大氧气容量
    @ConfigGroup(ConfigGroups.SPACESUIT)
    public double spacesuitOxygenConsumeUnderwater = 0.5;            // 水下每 2 秒消耗的氧气量
    @ConfigGroup(ConfigGroups.SPACESUIT)
    public double spacesuitOxygenConsumeSpace = 1.0;                 // 太空无氧环境每 3 秒消耗的氧气量

    //氧气机
    @ConfigGroup(ConfigGroups.OXYGENATOR)
    public int oxygenatorMaxRadius = 48;                             // 最大供氧半径（格）
    @ConfigGroup(ConfigGroups.OXYGENATOR)
    public int oxygenatorOpenSpaceRadius = 3;                        // 开放空间有效半径
    @ConfigGroup(ConfigGroups.OXYGENATOR)
    public int oxygenatorCacheExpireTicks = 100;                     // 缓存过期（5 秒）
    @ConfigGroup(ConfigGroups.OXYGENATOR)
    public int oxygenatorMinAirBlocks = 10;                          // 最小有效房间大小

    //真空进食
    @ConfigGroup(ConfigGroups.VACUUM_EATING)
    public double vacuumEatingOxygenCost = 100.0;                    // 真空环境吃一次食物扣多少氧
    @ConfigGroup(ConfigGroups.VACUUM_EATING)
    public int vacuumEatingPendingTimeoutSeconds = 10;               // 进食 pending 超时（秒）

    //玩具匠的锤子
    @ConfigGroup(ConfigGroups.TOYMAKER_HAMMER)
    public int toymakerHammerCopyChunkRadius = 20;                   // 复制半径（区块数）
    @ConfigGroup(ConfigGroups.TOYMAKER_HAMMER)
    public int toymakerHammerSpawnOffsetBlocks = 2;                  // 新 TARDIS 生成位置偏移（格）
    @ConfigGroup(ConfigGroups.TOYMAKER_HAMMER)
    public double toymakerHammerReachDistance = 5.0;                 // 使用时的最大作用距离（格）
    @ConfigGroup(ConfigGroups.TOYMAKER_HAMMER)
    public int toymakerHammerBlockUpdateFlags = 2 | 16;              // setBlockState 更新标志位

    //追踪器
    @ConfigGroup(ConfigGroups.TRACER)
    public double tracerScanRange = 45.0;                            // 手持自动扫描半径（格）
    @ConfigGroup(ConfigGroups.TRACER)
    public int tracerContainerScanRange = 45;                        // 右键扫描容器半径（格）

    //心灵感应电路
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public double tracerTelepathicScanRange = 5120;                  // 心灵感应电路远程搜索半径（格）
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerStructureSearchRadius = 51200;                  // 结构搜索半径（格）
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerBlacklistTolerance = 128;                       // 黑名单容差半径（格）—— 同一结构在此距离内视为已标记
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerMaxChainAttempts = 5;                           // 结构搜索最大重试次数
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerFragmentFuelCost = 300;                         // 锁定碎片消耗的塔迪斯燃料
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerStructureFuelCost = 600;                        // 锁定结构消耗的塔迪斯燃料
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerLandingOffset = 40;                             // 塔迪斯着陆偏移（±N 格）—— 避免直接卡进结构内部
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public double tracerHealAmount = 8.0;                            // 心灵感应电路空手潜行右键的回血量（HP）
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public int tracerHealFood = 4;                                   // 心灵感应电路空手潜行右键的饱食度回复
    @ConfigGroup(ConfigGroups.TELEPATHIC)
    public double tracerHealSaturation = 0.5;                        // 心灵感应电路空手潜行右键的饱和度回复

    //和谐之眼方尖碑
    @ConfigGroup(ConfigGroups.EYE_OF_HARMONY)
    public int eyeOfHarmonyGenerationRate = 20;                      // 每 tick 产生量（Artron）
    @ConfigGroup(ConfigGroups.EYE_OF_HARMONY)
    public double eyeOfHarmonyMaxStorage = 100000.0;                 // 最大存储
    @ConfigGroup(ConfigGroups.EYE_OF_HARMONY)
    public double eyeOfHarmonyTransferRate = 400.0;                  // 每次传输上限

    //涡旋操纵器
    @ConfigGroup(ConfigGroups.VORTEX_MANIPULATOR)
    public int vortexManipulatorMaxFuel = 1500;                      // 最大燃料
    @ConfigGroup(ConfigGroups.VORTEX_MANIPULATOR)
    public int vortexManipulatorMaxOverheat = 100;                   // 最大过热
    @ConfigGroup(ConfigGroups.VORTEX_MANIPULATOR)
    public int vortexManipulatorCooldownTicks = 200;                 // 普通冷却（tick，200=10 秒）
    @ConfigGroup(ConfigGroups.VORTEX_MANIPULATOR)
    public long vortexManipulatorBrokenCooldownTicks = 72000L;       // 损坏恢复时间（3 游戏日）
    @ConfigGroup(ConfigGroups.VORTEX_MANIPULATOR)
    public int vortexManipulatorCoolingIntervalTicks = 80;           // 散热间隔（tick）
    @ConfigGroup(ConfigGroups.VORTEX_MANIPULATOR)
    public int vortexManipulatorCoolingPerInterval = 1;              // 每次散热减少的过热量
}