<!-- 作者：；zh_tw 在地化。 -->
# 下載、安裝與問題回報

## 支援的版本 {#versions}

模組一共支援 **2 個 Minecraft 版本、4 種模組載入器組合**，目前都是 **0.1.2-alpha**：

| Minecraft | 模組載入器 | Java | 還需要裝 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（本指南用它示範） | 21 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.116.14+1.21.1） |
| 1.21.1 | NeoForge | 21 或更高 | NeoForge 21.1.264 或更高（**不需要** Fabric API） |
| 1.20.1 | Fabric | 17 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.92.12+1.20.1） |
| 1.20.1 | Forge | 17 或更高 | Forge 47.1 以上（測試用的是 47.4.28，**不需要** Fabric API） |

:::tip
**本指南用 1.21.1 Fabric 示範。** 其他三個版本的玩法、配方、數值都一樣，介面和操作照著看就行；少數很小的差別見下面「[移植版有什麼不一樣](#ports)」。
:::

- 1.20.1 的 Forge 版同一個 jar，也在 NeoForge 1.20.1（47.1.106）的伺服器端上做過初步檢查，用戶端和連線還沒驗證。
- **1.20.4 Fabric** 有一個舊的 0.1.1-alpha 版本，已經不再更新，指南不涵蓋該版本。
- 模組每個版本的用戶端和伺服器端都要裝同一個 jar。

## 去哪裡下載 {#download}

| 來源 | 網址 | 說明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常使用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不開，就是還在審核**，請先用別的管道 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每個 Minecraft 版本一頁，裡面同時放 Fabric 和 Forge / NeoForge 的 jar |

GitHub 上的兩個發布頁：

- [0.1.2-alpha · Minecraft 1.21.1 · Fabric + NeoForge](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha-mc1.21.1)
- [0.1.2-alpha · Minecraft 1.20.1 · Fabric + Forge](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases/tag/v0.1.2-alpha-mc1.20.1)

**下載前先確認 Minecraft 版本與模組載入器。** 兩者都會寫在檔名中，例如 `orients_hotpot_chronicles-1.21.1-fabric-0.1.2-alpha.jar`。版本或載入器選錯，遊戲就可能無法啟動。

## 怎麼裝 {#install}

### Fabric（1.21.1 / 1.20.1）

1. 使用 [Fabric Loader](https://fabricmc.net/use/) 安裝對應 Minecraft 版本的啟動設定檔。
2. 下載**同一個 Minecraft 版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模組的 jar 都放進 `.minecraft/mods` 資料夾，啟動遊戲。

### NeoForge（1.21.1）

1. 裝好 [NeoForge](https://neoforged.net/) 21.1.264 或更高版本。
2. 把本模組的 NeoForge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

### Forge（1.20.1）

1. 裝好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.1 以上，測試用 47.4.28）。
2. 把本模組的 Forge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

裝好後，進遊戲的創造模式物品欄，裡面有「**之晗的火鍋物語**」一頁，左邊還有「廚具 / 餐具 / 桌子 / 凳子 / 食物」的小標籤幫你分類。

:::warn
模組還在 **alpha** 階段，目前**只做過初步測試**，和其他模組的相容、多人連線、少見的操作可能有 bug。
0.1.0-alpha.1 的舊存檔升級後，本模組的東西可能遺失或變樣。**先備份存檔，最好在新存檔裡玩。**
:::

## 移植版有什麼不一樣 {#ports}

1.20.1（Fabric / Forge）和 1.21.1 NeoForge 都是從 1.21.1 Fabric 版移植的，**玩法、配方、數值、貼圖一樣**。玩家能感覺到的差別很小：

| 專案 | 1.21.1 Fabric | 1.20.1 |
| --- | --- | --- |
| 鍋裡冒的熱氣 | 白煙粒子 | 雲粒子（也是白色往上飄，略大一點） |
| 蘑菇湯之類吃完還碗 | 讀食物自帶屬性 | 照原版規則還碗 |

另外要注意：**新內容通常 Fabric 1.21.1 先有，移植版晚一步跟上**。發布頁和本指南的版本標籤會寫清楚。

## 問題回報與交流 {#feedback}

遇到 bug、有疑問或建議，歡迎告訴作者：

| 管道 | 聯絡方式 |
| --- | --- |
| **GitHub Issues** | [回報問題或提供建議](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（有報 bug、提建議、提問三種表單） |
| **QQ 交流群** | **248314791**（作者沒回就是在睡覺） |
| **B 站** | 蘧白川 |
| **電子郵件** | orientcheung233@gmail.com |
| 專案首頁 | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**報 bug 時請寫清楚：**
1. Minecraft 版本、模組載入器（Fabric / Forge / NeoForge）、模組版本；
2. 做了什麼操作、哪一步出的問題；
3. 最好帶螢幕截圖，當機的話帶上 `latest.log` 或當機報告。

模組作者：**蘧白川 / Orient**（之晗和 Orient 是同一個人的中文名和英文名）。程式碼是 MIT 授權條款，原創的貼圖、模型等資源是 CC BY-NC-SA 4.0。
