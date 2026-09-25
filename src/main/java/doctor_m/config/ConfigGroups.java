package doctor_m.config;

/**
 * 配置分组的标识符常量。
 * 对应的显示名从语言文件 gui.doctor_m.config.group.<id> 读取。
 * <p>新增分组时，在这里加一个常量，并在语言文件里补上对应的翻译。
 */
public final class ConfigGroups {

    private ConfigGroups() {}

    public static final String VORTEX             = "vortex";              // 涡旋背景
    public static final String STP                = "stp";                 // STP 无缝传送技术
    public static final String TT_SHIELD          = "tt_shield";           // TT 护盾半径
    public static final String FORCE_FIELD        = "force_field";         // 力场盾牌
    public static final String SHIELD_GENERATOR   = "shield_generator";    // 护盾生成器
    public static final String TIME_KEY           = "time_key";            // 时间钥匙
    public static final String ETERNAL_CRYSTAL    = "eternal_crystal";     // 永恒水晶
    public static final String SCYTHE             = "scythe";              // 特莉波卡的镰刀
    public static final String SAR               = "sar";                // STCS
    public static final String SELF_DESTRUCT      = "self_destruct";       // 塔迪斯自毁
    public static final String OXYGEN_TANK        = "oxygen_tank";         // 氧气瓶
    public static final String OXYGEN_CHARGER     = "oxygen_charger";      // 氧气补充机
    public static final String SPACESUIT          = "spacesuit";           // 宇航服
    public static final String OXYGENATOR         = "oxygenator";          // 氧气机
    public static final String VACUUM_EATING      = "vacuum_eating";       // 真空进食
    public static final String TOYMAKER_HAMMER    = "toymaker_hammer";     // 玩具匠的锤子
    public static final String TRACER             = "tracer";              // 追踪器
    public static final String TELEPATHIC         = "telepathic";          // 心灵感应电路
    public static final String EYE_OF_HARMONY     = "eye_of_harmony";      // 和谐之眼方尖碑
    public static final String VORTEX_MANIPULATOR = "vortex_manipulator";  // 涡旋操纵器
    public static final String MISC               = "misc";                // 其他

    /** 语言文件 key 前缀。 */
    public static final String LANG_PREFIX = "gui.doctor_m.config.group.";
}