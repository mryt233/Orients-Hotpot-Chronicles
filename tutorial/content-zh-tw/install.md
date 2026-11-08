<!-- 作者：mryt233；zh_tw 在地化，原文：mryt233。發布前由 mryt233 同步並審核。 -->
<!-- mryt233 改，2026-11-08：依簡體原文同步 0.2.2-alpha 多版本發布說明；保留原譯者署名。 -->
# 下載、安裝與回報

## 支援的版本 {#versions}

最新版 **0.2.2-alpha** 同步支援 **Minecraft 1.21.1、1.20.1、26.1.x 和 26.2** 的主流載入器，所有 jar 都**內嵌了前置框架 `orient_framework`**，下載一個直接放進 `mods` 就行：

| Minecraft | 載入器 | Java | 還需要裝 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（教程用它演示，**0.2.2-alpha**） | 21 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.116.14+1.21.1） |
| **1.21.1** | **NeoForge**（**0.2.2-alpha**） | 21 或更高 | NeoForge 21.1.186 或更高 |
| **1.20.1** | **Fabric**（**0.2.2-alpha**） | 17 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.92.12+1.20.1） |
| **1.20.1** | **Forge**（**0.2.2-alpha**） | 17 或更高 | Forge 47.1 以上（測試用 47.4.28） |
| **26.1.x** | **Fabric / NeoForge**（**0.2.2-alpha**） | 25 或更高 | 對應 26.1.x 的 Fabric Loader + Fabric API，或 NeoForge |
| **26.2** | **Fabric / NeoForge**（**0.2.2-alpha**） | 25 或更高 | 對應 26.2 的 Fabric Loader + Fabric API，或 NeoForge |

:::tip
**本教程用 1.21.1 Fabric 演示。** 0.2.2-alpha 起上述所有版本的玩法、食材、小料、家具與設定功能完全同步；細微底層差異見下面「[各版本有什麼不一樣](#ports)」。
:::

- **舊版存檔與設定相容**：0.2.2-alpha 將舊版 `tofu` 拆分為 `cold_tofu`（冷豆腐）和 `hot_tofu`（熱豆腐），舊存檔裡的 `config/orients_hotpot_chronicles.json` 中的 `tofu` 設定鍵會自動無損遷移，升級前仍建議備份存檔。
- 1.20.4 Fabric 仍停在舊版 0.1.1-alpha，不再維護。
- 多人連線時，用戶端和伺服器端都要裝對應載入器的同版本 jar。

## 去哪裡下載 {#download}

| 來源 | 地址 | 說明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不開就是在審核**，先用別的管道 |
| **MC 百科** | <https://www.mcmod.cn/class/32185.html> | 中文模組百科：看介紹、留言討論 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每個版本一頁，裡面放各載入器的 jar |

**下載認準兩點：Minecraft 版本、載入器。** 檔名裡寫著，比如 `orients_hotpot_chronicles-1.21.1-fabric-0.2.2-alpha.jar`、`orients_hotpot_chronicles-1.21.1-neoforge-0.2.2-alpha.jar`，選錯版本進不去遊戲。

## 怎麼裝 {#install}

### Fabric（1.21.1 / 1.20.1 / 26.1.x / 26.2）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 裝出對應版本的遊戲。
2. 下載**同版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模組 jar 放進 `.minecraft/mods`，啟動遊戲。（如想在模組列表裡點「設定」調數值，可順手裝上 **Mod Menu**。）

### NeoForge（1.21.1 / 26.1.x / 26.2）

1. 裝好對應 Minecraft 版本的 [NeoForge](https://neoforged.net/)（1.21.1 需 21.1.186 或更高）。
2. 把模組 NeoForge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

### Forge（1.20.1）

1. 裝好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.1 以上，測試用 47.4.28）。
2. 把模組 Forge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

裝好進創造模式物品欄，有「**之晗的火鍋物語**」一頁，左邊有「全部 / 廚具 / 餐具 / 桌凳 / 食物」小標籤幫你分類。

:::warn
模組還在 **alpha** 階段，只做過初步測試。0.1.0-alpha.1 舊存檔升級後東西可能遺失或變樣，**先備份存檔，最好在新存檔玩**。
:::

## 各版本有什麼不一樣 {#ports}

0.2.2-alpha 的 **1.21.1、1.20.1、26.1.x、26.2** 全部對齊了同一套玩法與新內容，只有極少數受原版底層限制的細微差別：

| 項目 | 1.21.1 / 26.1.x / 26.2 | 1.20.1（Fabric / Forge） |
| --- | --- | --- |
| 鍋裡冒的熱氣 | 白煙粒子 | 雲粒子（白色往上飄，略大一點） |
| 吃完還碗 | 讀食物自帶屬性 | 照原版與模組規則返還空碗 |

## 回報、交流 {#feedback}

遇到 bug、有疑問或建議，歡迎告訴作者：

| 渠道 | 聯絡方式 |
| --- | --- |
| **GitHub Issues** | [提交回報](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（報 bug、提建議、提問） |
| **QQ 交流群** | **248314791**（作者沒回就是在睡覺） |
| **B 站** | 蘧白川 |
| **電子信箱** | orientcheung233@gmail.com |
| 專案主頁 | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**報 bug 請寫清：**
1. 遊戲版本、載入器（Fabric / Forge / NeoForge）、模組版本；
2. 做了什麼操作、哪一步出問題；
3. 最好帶截圖，崩潰帶上 `latest.log` 或崩潰報告。

模組作者：**蘧白川 / Orient**（之晗和 Orient 是同一人的中英文名）。程式碼是 MIT 許可證，原創貼圖、模型是 CC BY-NC-SA 4.0。
