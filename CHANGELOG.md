# 更新日志 / Changelog

---

## [ v2.1 - 过去的历史...与尚未发生的.../past... and what hasn't happened yet...]

### 修改 / Edit

- 土卫六-泰坦 / Saturn VI-Titan

-  修改：时隙 / Edit : rift
-  修改：永恒之眼/永恒之眼 45型 / Edit : Evereye/Evereye Type45

-  新增：立方（作者：Asnit_PnQing） / New addition: Cube (Author: Asnit_PnQing)
-  新增：时隙[战争往事]（作者：TC020） / New addition: The rift [War Stories](Author: TC020)

-  护盾生成器，能提供99.99%的伤害减免并消耗能量，默认1000点能量，每次受伤消耗5点（可配置） / Shield Generator: Provides 99.99% damage reduction while consuming energy. Default energy capacity is 1000, and each hit consumes 5 energy (configurable)
-  去物质枪！现在终于完成了！去物质枪会抹除击中的目标的物品栏/末影箱/成就，同时强制抹杀玩家，复活后玩家约等于重开新号（时间线级别抹除是这样的） / It's done—the De‑Mat Gun is finally finished! It wipes the target's inventory, ender chest, and achievements, and kills the player outright. When they respawn, it's like starting from scratch (that's timeline‑level erasure for you)
-  103型塔迪斯和玛丽安NPC / Type-103-TARDIS and Marian NPC

---

-  AIT的哈赞卓水晶不再是消耗物品本身，而是消耗物品耐久（16） / AIT's Hazandra Crystal no longer consumes the item itself, but instead consumes item durability (16).
-  增强神秘宝石（遗物宝石）[@TC-020 iss12](https://github.com/smallmoss233/DOCTOR-M/issues/11) / Enhance Mysterious Gem (Relic Gem) [@TC-020 iss12](https://github.com/smallmoss233/DOCTOR-M/issues/11)
-  指令创建塔迪斯功能 / Command to create TARDIS functionality
-  拉斯隆之钥可以当塔迪斯的钥匙 / Rassilon key can be used as TARDIS key
-  塔迪斯自毁的爆炸威力更大，更壮观 / The explosion from TARDIS self-destruction is now more powerful and more spectacular.
-  现在有非常的可配置项在config/doctor_m里！/ There are now many configurable options available in the config/doctor_m directory!
-  时间钥匙被动超级加强！不再有复活冷却，且复活优先级被拉到最大！ / Time Key passive massively buffed! Resurrection cooldown removed, and resurrection priority is now set to maximum!
-  增强音速起子 / Enhanced Sonic
   扫描模式：能探测墙后空间/生物类型，敌对状况/时间，维度，坐标 / Scan Mode: Can detect spaces behind walls, entity types, hostility status, time, dimension, and coordinates.
   塔迪斯模式：召唤塔迪斯失败时汇报塔迪斯的位置/维度于玩家的相对距离和方向 / TARDIS Mode: When summoning TARDIS fails, reports the TARDIS's location and dimension, along with its relative distance and direction from the player.
   过载模式：可以对坚守者造成伤害和硬控10s / Overload Mode: Can deal damage to the Warden and apply a hard stun for 10 seconds.
   交互模式：可以修复损坏的子系统耐久 / Interaction Mode: Can repair damaged subsystem durability.
-  太空大改2.0 / Space Overhaul 2.0
   在真空进食会扣除大量氧气（孩子，谁告诉你太空可以打开面罩吃东西的？！） / Eating in a vacuum will drain a significant amount of oxygen (Kid, who told you that you can open your helmet and eat in space?!)
   修复航天服UI的关于有氧环境的误判 / Fixed the spacesuit UI's false detection regarding oxygenated environments.
   航天服新增氧气阀值警告 / Added oxygen threshold warnings for the spacesuit.
   氧气机的算法优化，一个区域内放置多台氧气机有氧范围扩大，且修复了一些潜在BUG / Optimized oxygen generator algorithm; placing multiple generators in an area expands the oxygen coverage, and fixed some potential bugs.
-  塔迪斯型号不再局限于50型！可在doctor_m/tardis_type.json当中填写内饰ID+型号来自定义你的塔迪斯型号！ / TARDIS types are no longer limited to Type 50! You can now customise your TARDIS type by adding interior IDs and type names in doctor_m/tardis_type.json!
-  音速起子晶体系统 / Sonic Screwdriver Crystal System
   来自AIT的iss，让音速起子可以替换端部晶体获得新功能 / Derived from AIT's ISS, allowing the Sonic Screwdriver to swap end crystals for new abilities.
   紫水晶：引力 / Amethyst: Gravity
   引力牵引：抓取一个实体，范围10格以内 / Gravitational Pull: Pulls an entity within 10 blocks.
   引力护盾：推开周围所有实体 / Gravitational Shield: Pushes all nearby entities away.
   引力拖拽：需预热，将玩家朝着视角方向拉过去 / Gravitational Swap: Requires warming up, pulls the player in the direction they are looking.
   充能泽顿水晶：激光 / Charged Zeiton Crystal: Laser
   脉冲：间歇性的激光 / Pulse: Intermittent laser bursts.
   激光：持续性的激光 / Laser: Continuous laser beam.
   冲击波：推开周围生物，冷却5s / Shockwave: Pushes nearby mobs away, 5s cooldown.

---

### 修复 / Repair

-  fabric.mod.json中贡献者:今悄修改为：贡献者:Siletonight / In fabric.mod.json, change the contributor from "今悄" to "Siletonight".
-  贡献者名字修改：Asnit_PnQing改为名游茶 / Change the contributor name from "Asnit_PnQing" to "名游茶".
-  所有的塔迪斯废墟结构现在都能正常生成 / All TARDIS ruin structures now generate correctly
-  所有有光影的内饰预览图都经过拍摄了 / All interior preview images with shaders have been captured