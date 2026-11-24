<!-- 作者：mryt233；繁體中文在地化，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：全量依 0.3.1-alpha 五版本十包同步發布口徑更新，含 26.3。 -->
# 下載、安裝與回報

## 支援的版本 {#versions}

最新版 **0.3.1-alpha** 同步支援 **Minecraft 1.21.1、1.20.1、26.1.x、26.2 與 26.3** 共五大版本線（10 個安裝包），所有 jar 都**內嵌了前置框架 `orient_framework 0.2.2`**，下載對應版本和載入器的單個 jar 直接放進 `mods` 就行：

| Minecraft | 載入器 | Java | 還需要裝 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（教程用它示範，**0.3.1-alpha**） | 21 或更高 | Fabric Loader 0.15.11+、同版 **Fabric API**（實測 0.101.2+1.21.1 / 0.116.14+1.21.1） |
| **1.21.1** | **NeoForge**（**0.3.1-alpha**） | 21 或更高 | NeoForge 21.1.186 或更高 |
| **1.20.1** | **Fabric**（**0.3.1-alpha**） | 17 或更高 | Fabric Loader 0.14.21+、**Fabric API**（0.89.0+1.20.1 起，實測 0.89.0 與 0.92.12） |
| **1.20.1** | **Forge**（**0.3.1-alpha**） | 17 或更高 | Forge 47.0.0 以上（實測 47.0.0 獨立伺服器與 47.4.28 雙端） |
| **26.1 / 26.1.1 / 26.1.2** | **Fabric**（**0.3.1-alpha**） | 25 或更高 | Fabric Loader 0.19.5+ 與對應 26.1.x 小版本的 **Fabric API**（0.145.0+） |
| **26.1.2** | **NeoForge**（**0.3.1-alpha**） | 25 或更高 | NeoForge 26.1.2.71 或更高 |
| **26.2** | **Fabric / NeoForge**（**0.3.1-alpha**） | 25 或更高 | Fabric Loader 0.19.5+ 與對應 26.2 的 **Fabric API**（0.152.0+），或 NeoForge 26.2.0.57+ |
| **26.3** | **Fabric / NeoForge**（**0.3.1-alpha**） | 25 或更高 | Fabric Loader 0.19.5+ 與對應 26.3 的 **Fabric API**（0.160.3+），或 NeoForge 26.3.0.76+ |

:::tip
**本教程用 1.21.1 Fabric 示範。** 0.3.1-alpha 起上述全部五個版本線的玩法、食材、工藝、小遊戲、換材質桌架、設定與開放擴充功能保持同步；極少數受原版底層限制的粒子或介面差異見下面「[各版本有什麼不一樣](#ports)」。
:::

- **舊版存檔與設定自動相容**：
  - 從 `0.2.2-alpha` / `0.3.0-alpha` 升級時，已放置和背包裡的 **40 種舊預設火鍋桌**會在區塊載入或進服時無損遷移為統一通用桌並保留原外觀，箱內舊桌取出放置同樣無損升級，不會出現紫黑方塊或丟檔；
  - 湯底、沾醬與舊乾貨泡發廢棄設定鍵（`soaked_*` / `dry_goods_soaking`）會在讀取設定時自動靜默遷移與清理，重載零警告；升級前仍建議先備份存檔。
- **1.20.4 Fabric** 仍停在舊版 0.1.1-alpha，不再維護。
- **多人連線要求**：`0.3.1-alpha` 因包含模式開關同步、音遊曲譜資料包下發與湯底 / 沾醬字串 ID 同步，**用戶端與伺服器端必須使用同版本 `0.3.1-alpha` jar**，不可與舊版混連。

## 去哪裡下載 {#download}

| 來源 | 位址 | 說明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不開就是在審核**，先用別的管道 |
| **MC 百科** | <https://www.mcmod.cn/class/32185.html> | 中文模組百科：看介紹、留言討論 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每個版本一頁，裡面放各載入器的 jar |

**下載認準兩點：Minecraft 版本、載入器。** 檔名裡寫著，比如 `orients_hotpot_chronicles-1.21.1-fabric-0.3.1-alpha.jar`、`orients_hotpot_chronicles-1.21.1-neoforge-0.3.1-alpha.jar`、`orients_hotpot_chronicles-26.3-fabric-0.3.1-alpha.jar`，選錯版本進不去遊戲。

## 怎麼裝 {#install}

### Fabric（1.21.1 / 1.20.1 / 26.1.x / 26.2 / 26.3）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 裝出對應版本的遊戲。
2. 下載**同版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模組 jar 放進 `.minecraft/mods`，啟動遊戲。（如想在模組列表裡點「設定」調數值，可順手裝上 **Mod Menu**。）

### NeoForge（1.21.1 / 26.1.x / 26.2 / 26.3）

1. 裝好對應 Minecraft 版本的 [NeoForge](https://neoforged.net/)（1.21.1 需 21.1.186 或更高，26.x 選用對應 MC 版本的 NeoForge）。
2. 把模組 NeoForge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

### Forge（1.20.1）

1. 裝好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.0.0 及以上，實測 47.0.0 與 47.4.28）。
2. 把模組 Forge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

裝好進創造模式物品欄，有「**之晗的火鍋物語**」一頁，左邊有「全部 / 廚具 / 餐具 / 桌凳 / 食物」小標籤幫你分類；開啟模組設定即可依喜好切換**完整模式**或**精簡模式**。

:::warn
模組還在 **alpha** 階段。從 `0.1.0-alpha.1` 等很早期舊存檔升級後東西可能遺失或變樣，**先備份存檔，最好在新存檔玩**。
:::

## 各版本有什麼不一樣 {#ports}

0.3.1-alpha 的 **1.21.1、1.20.1、26.1.x、26.2、26.3** 全部對齊了同一套玩法與內容，只有極少數受原版底層限制的細微差別：

| 項目 | 1.21.1 / 26.1.x / 26.2 / 26.3 | 1.20.1（Fabric / Forge） |
| --- | --- | --- |
| 鍋裡冒的熱氣 | 白煙粒子 | 雲粒子（白色往上飄，略大一點） |
| 吃完還碗 | 讀食物自帶屬性 | 依原版與模組規則返還空碗 |

## 回報、交流 {#feedback}

遇到 bug、有疑問或建議，歡迎告訴作者：

| 管道 | 聯絡方式 |
| --- | --- |
| **GitHub Issues** | [提交回報](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（報 bug、提建議、提問） |
| **QQ 交流群** | **248314791**（作者沒回就是在睡覺） |
| **B 站** | 蘧白川 |
| **電子郵件** | orientcheung233@gmail.com |
| 專案主頁 | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**報 bug 請寫清：**
1. 遊戲版本、載入器（Fabric / Forge / NeoForge）、模組版本；
2. 做了什麼操作、哪一步出問題；
3. 最好帶截圖，崩潰帶上 `latest.log` 或崩潰報告。

模組作者：**蘧白川 / Orient**（之晗和 Orient 是同一人的中英文名）。程式碼是 MIT 授權，原創貼圖、模型是 CC BY-NC-SA 4.0。
