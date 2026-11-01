# 桌子与凳子

火锅得围着桌子吃。模组里有**两套桌凳**：矮的一套（贴近地面，像家里火锅桌）和高的一套（1.5 格高，像店里的高脚桌），每套都有 **11 种木头**：

{{icons:oak_hotpot_table,spruce_hotpot_table,birch_hotpot_table,jungle_hotpot_table,acacia_hotpot_table,dark_oak_hotpot_table,mangrove_hotpot_table,cherry_hotpot_table,bamboo_hotpot_table,crimson_hotpot_table,warped_hotpot_table}}

橡木、云杉、白桦、丛林、金合欢、深色橡木、红树、樱花、竹、绯红、诡异——桌子**贴图用的就是原版木板**，配方也只是换成对应的木板。

## 矮火锅桌

{{img:hotpot-low-table.webp|矮火锅桌 + 灶台 + 板凳}}

{{recipe:oak_hotpot_table}}

- 桌面和**矮灶台的砖面齐平**，灶台能直接嵌在桌子中间，像一张整桌。
- 桌面下面像倒放的楼梯，朝外一侧往里收 4 像素，给膝盖留位置。
- **会自己连**：哪边挨着另一张桌子（矮桌、高桌都行）或灶台就自己铺满，连成长桌。
- 木头桌子会着火，能用斧头快速挖。

## 高火锅桌

{{img:hotpot-tall-table.webp|高火锅桌 + 高灶台 + 条凳}}

{{icons:oak_tall_hotpot_table}}

{{recipe:oak_tall_hotpot_table}}

- **1.5 格高**，桌面悬出一圈，下面是桌腿，朝外也收进 4 像素留腿部空间。
- 下面一格是**砖块**，上面是木板（配方里有 3 个砖块）。
- 和**高灶台**搭配：灶台火口正对桌子时，桌子那侧会**去掉砖身只留木头**，方便看火。
- 同样会自己连，和矮桌、灶台都能连。
- 方块比一格高，**准星对着桌面上的东西也能选得中**（盘子、碗放在高桌上不会点不到）。

### 桌上摆东西 {#decor}

- **盘子、碗、饮料、蘸料碗**都放桌面上，见[盘子、碗与饮料](tableware.html)。
- **小摆件**（灯笼、花盆、蜡烛、蛋糕等）放火锅桌上会**贴住桌面**，不再浮空。
- 其他普通方块：矮桌上浮空 5 像素，高桌上嵌进 7 像素，当装饰也不错。

## 石料和金属火锅桌 {#stone}{{since:0.1.4-alpha}}

{{icons:smooth_stone_hotpot_table,polished_granite_hotpot_table,polished_deepslate_hotpot_table,quartz_hotpot_table,cut_copper_hotpot_table,iron_hotpot_table}}

木头的之外还有 **9 种桌面**：平滑石头、磨制安山岩 / 闪长岩 / 花岗岩 / 深板岩 / 黑石、石英、切制铜、铁；矮桌、高桌各一套，一共 **18 张**。桌身还是红砖，桌板换成这些材料。

{{recipe:smooth_stone_hotpot_table}}

## 板凳和条凳

| 凳子 | 高度 | 配方一次出 |
| --- | --- | --- |
| {{i:oak_stool}} | 矮，坐高约 7/16 格 | 3 个 |
| {{i:oak_bench}} | 一格高，凳面和桌面齐平；坐高 1.25 格 | 3 个 |

{{recipe:oak_stool,oak_bench}}

两种凳子都有 11 种木头，配方只是换对应木板。

### 摆放

- **朝向**：放的时候面朝哪边，凳子（和坐上去的人）就面朝哪边。
- **连成长凳**：同一直线上挨着的两个凳子（高矮一样）会**自己连成长凳**，中间腿去掉、两头腿往里收，垂直放的不连。**一条最长连 3 格**，再往旁边放就不连了。0.1.2-alpha 里放的时候朝向一样才连；0.1.3-alpha-hotfix1 起不管站哪边放都会连。
- 条凳腿是斜着的「A」字腿，由代码画出来。

### 怎么坐 {#sit}

1. **空手**（或手里拿不是方块的东西）**右键凳子**就坐上去了。
2. 点凳子**中间**坐中间；点**两格交界处**坐两格中间。
3. 两个人之间**至少隔一整格**，不会挤在一起。
4. **Shift（潜行）**起身，站到凳子后面。

:::tip
搭配推荐：「**条凳 + 高桌 + 高灶台**」是一套，「**板凳 + 矮桌 + 矮灶台**」是一套，别混着搭，高度才对得上。
:::
