<!-- 作者：mryt233；zh_tw 在地化，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改：依简体原文补齐正式版与开发预览；保留原译者署名。 -->
# 下載、安裝與反饋

## 支援的版本 {#versions}

模組一共支援 **2 個 Minecraft 版本、4 種載入器組合**。最新版 **0.1.4-alpha**：

| Minecraft | 載入器 | Java | 還需要裝 |
| --- | --- | --- | --- |
| **1.21.1** | **Fabric**（教程用它演示，**0.1.4-alpha**） | 21 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.116.14+1.21.1） |
| 1.21.1 | NeoForge（0.1.3-alpha-hotfix1） | 21 或更高 | NeoForge 21.1.264 或更高 |
| 1.20.1 | Fabric（0.1.3-alpha-hotfix1） | 17 或更高 | Fabric Loader 0.15.11+、**Fabric API**（0.92.12+1.20.1） |
| 1.20.1 | Forge（0.1.3-alpha-hotfix1） | 17 或更高 | Forge 47.1 以上（測試用 47.4.28） |

:::tip
**本教程用 1.21.1 Fabric 演示。** 其他三個版本玩法、配方、數值都一樣，照著玩就行；少數細小差別見下面「[移植版有什麼不一樣](#ports)」。
:::

- **0.1.4-alpha 目前只有 1.21.1 Fabric**；1.21.1 NeoForge、1.20.1 Fabric / Forge 還停在 0.1.3-alpha-hotfix1，晚一步跟上。
- 1.20.1 的 Forge 版同一個 jar 也在 NeoForge 1.20.1（47.1.106）伺服器端做過初步檢查，用戶端和連線還沒驗證。
- **1.20.4 Fabric** 有個舊的 0.1.1-alpha 版，已不再更新，教程不包含。
- 用戶端和伺服器端都要裝同一個 jar。

## 去哪裡下載 {#download}

| 來源 | 地址 | 說明 |
| --- | --- | --- |
| **CurseForge** | <https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles> | 正常用 |
| **Modrinth** | <https://modrinth.com/mod/orients-hotpot-chronicles> | **如果打不開就是在審核**，先用別的管道 |
| **MC 百科** | <https://www.mcmod.cn/class/32185.html> | 中文模組百科：看介紹、留言討論 |
| **GitHub Releases** | <https://github.com/mryt233/Orients-Hotpot-Chronicles/releases> | 每個版本一頁，裡面放各載入器的 jar |

**下載認準兩點：Minecraft 版本、載入器。** 檔名裡寫著，比如 `orients_hotpot_chronicles-1.21.1-fabric-0.1.4-alpha.jar`，選錯版本進不去遊戲。

## 怎麼裝 {#install}

### Fabric（1.21.1 / 1.20.1）

1. 用 [Fabric Loader](https://fabricmc.net/use/) 裝出對應版本的遊戲。
2. 下載**同版本**的 [Fabric API](https://modrinth.com/mod/fabric-api)。
3. 把 Fabric API 和本模組 jar 放進 `.minecraft/mods`，啟動遊戲。

### NeoForge（1.21.1）

1. 裝好 [NeoForge](https://neoforged.net/) 21.1.264 或更高。
2. 把模組 NeoForge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

### Forge（1.20.1）

1. 裝好 [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) 1.20.1（47.1 以上，測試用 47.4.28）。
2. 把模組 Forge jar 放進 `mods` 資料夾，啟動遊戲。**不需要 Fabric API。**

裝好進創造模式物品欄，有「**之晗的火鍋物語**」一頁，左邊有「全部 / 廚具 / 餐具 / 桌凳 / 食物」小標籤幫你分類。

:::warn
模組還在 **alpha** 階段，只做過初步測試。0.1.0-alpha.1 舊存檔升級後東西可能遺失或變樣，**先備份存檔，最好在新存檔玩**。
:::

## 移植版有什麼不一樣 {#ports}

1.20.1（Fabric / Forge）和 1.21.1 NeoForge 是從 1.21.1 Fabric 版移植的，**玩法、配方、數值、貼圖一樣**，差別很小：

| 項目 | 1.21.1 Fabric | 1.20.1 |
| --- | --- | --- |
| 鍋裡冒的熱氣 | 白煙粒子 | 雲粒子（白色往上飄，略大一點） |
| 吃完還碗 | 讀食物自帶屬性 | 照原版規則還碗 |

**新內容通常 Fabric 1.21.1 先有，移植版晚一步跟上**。發布頁和教程版本標籤會寫清。

## 反饋、交流 {#feedback}

遇到 bug、有疑問或建議，歡迎告訴作者：

| 渠道 | 聯絡方式 |
| --- | --- |
| **GitHub Issues** | [提交反饋](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（報 bug、提建議、提問） |
| **QQ 交流群** | **248314791**（作者沒回就是在睡覺） |
| **B 站** | 蘧白川 |
| **電子信箱** | orientcheung233@gmail.com |
| 專案主頁 | <https://github.com/mryt233/Orients-Hotpot-Chronicles> |

**報 bug 請寫清：**
1. 遊戲版本、載入器（Fabric / Forge / NeoForge）、模組版本；
2. 做了什麼操作、哪一步出問題；
3. 最好帶截圖，崩潰帶上 `latest.log` 或崩潰報告。

模組作者：**蘧白川 / Orient**（之晗和 Orient 是同一人的中英文名）。程式碼是 MIT 許可證，原創貼圖、模型是 CC BY-NC-SA 4.0。
