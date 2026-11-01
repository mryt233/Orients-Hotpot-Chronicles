# 玩家教程（tutorial）

作者：mryt233，2026-10-24 建。用户要求：给模组做详细的图文教程（物品、合成表、玩法），**每次版本更新都要及时更新进教程**。

## 怎么看

- **推荐：三语合一版** `Minecraft_Orients_Hotpot_Chronicles_Player_Guide.html`（2026-10-25 更新）：右上角选择简体中文 / `zh_tw` / English，记住语言，切换后停留在对应小节；图片全部内嵌。`打开教程.bat` 默认打开这一份。上传到 Wiki 静态托管目录时可改名为 `index.html`；需要允许 JavaScript 执行。
- **中文单语言版** `我的世界[之晗的火锅物语]模组-游玩教程.html`（一个文件、一直往下翻、左边目录；图片全部内嵌，可单独发给别人），仍可直接双击 HTML 打开。分页版在 `site/`。（mryt233 改：启动入口改为上面的双语版。）
- 或用 `preview_start` 的 `tutorial` 配置（端口 5195）。
- `site/` 是生成出来的静态网站，可以整个文件夹放到任何网页空间 / GitHub Pages。**不要手改 `site/` 和单文件版**，两者都由 `build_tutorial.py` 一次生成。

## 文件

```text
tutorial/
├── content/*.md      教程正文（手写，简体中文）：index、quickstart、hotpot、tables、tableware、ingredients、sauce、faq、changelog
│                     recipes、items 两页是脚本自动生成的（content/recipes.md 若存在会当前言放在最前面）
├── img/              插图（截图、webp）和 img/icons/ 物品图标（查看器导出的 PNG，文件名 命名空间__物品ID.png）
├── data/vanilla_zh.json  用到的原版物品中文名（脚本从 Loom 缓存自动补）
├── assets/           网页样式和脚本
├── site/             生成结果
└── _ids.py / _icon_ids.json  导出图标用的物品清单
tools/build_tutorial.py   生成脚本（在 mod/ 下运行：python tools/build_tutorial.py）
```

## 写文章的小指令

| 写法 | 效果 |
| --- | --- |
| `{{i:ID}}` | 行内小图标 + 中文名；模组物品可省略 `orients_hotpot_chronicles:`，原版写 `minecraft:xxx` |
| `{{recipe:ID,ID}}` | 画出产物是这些物品的全部配方（合成 / 切石 / 烹饪），**直接读游戏的配方 JSON**，所以不会和游戏不一致 |
| `{{icons:a,b,c}}` | 一排物品图标 |
| `{{img:文件名\|说明}}` | 插入 `tutorial/img/` 里的图 |
| `{{since:0.1.2-alpha}}` | 「自某版本」绿标签；`{{dev}}` 黄色「开发版预览」标签 |
| `:::tip` / `:::warn` / `:::dev` … `:::` | 提示框 / 注意框 / 开发版内容框 |
| 标题后 `{#xxx}` | 给标题起锚点，别的页面可以 `页面.html#xxx` 链过来 |

## 新增物品图标

新加了物品 / 方块，或教程里引用了新的原版物品，脚本会警告「缺图标」。导出方法：

1. `preview_start` 启动 `viewer`，打开本项目；在页面里：
   `await window.__icon.get('命名空间:物品ID')` 得到 PNG 数据，再 `POST /api/save-guide-icon?p=mod&n=命名空间__物品ID`（请求体就是数据 URL），文件会存进 `tutorial/img/icons/`。
2. 批量导出：改 `_ids.py` 里的清单跑一遍生成 `_icon_ids.json`，再在浏览器里循环导出（做法见 DEV_LOG「玩家教程」那条）。

## 每次改游戏后要做的事（维护流程）

1. 游戏里有改动（新东西、数值、外观、交互、修 bug）→ 照常写 DEV_LOG、`docs/移植同步清单.md`，**再**：
   - 改动影响玩法描述 → 改对应 `content/*.md`；
   - 往 `content/preview.md`（书里的「开发版预览」一章）或 `content/changelog.md` 的「开发版预览」里加一条，后面带 `<!--清单#N-->`（N = 移植同步清单里的编号）；
   - 新物品 / 新方块 → 在合适的页面里讲（带 `{{recipe:…}}`），并导出图标。
2. 跑 `python tools/build_tutorial.py`，**把所有「警告」处理到清零**：
   - 「教程正文没提到：XX」= 有新物品没人写；
   - 「移植同步清单未发布 #N 还没写进更新日志」= 有改动没记进更新日志；
   - 「缺图标 / 缺插图 / 没有配方」。
3. 用户**打包发布新版本**时：把 `preview.md` 的内容搬进 `changelog.md` 并改名成版本号（并把里面的 `{{dev}}` 标签、正文里的 `{{dev}}` / `:::dev` 换成 `{{since:新版本号}}` 或去掉），然后再新开一段「开发版预览」；重新生成。
4. 交付时说一声教程更新了什么。

## 注意

- 教程写的是**玩家能看到、做到的事**，别写代码类名。
- 数值（饥饿、秒数、概率）要和代码一致：不确定就去读代码或 DEV_LOG / 移植同步清单，别猜。
- 开发版内容一律标 `{{dev}}`；没进世界测过的玩法在 faq 或正文里不要说得太绝对。

## 版本基准（2026-10-24 用户定）

教程以**已发布的 0.1.4-alpha** 为准。之后做好、还没发布的 r130 内容（同步清单 #72–#82）放进 `content/preview.md`，对应英文和 zh_tw 也保留 `{{dev}}` / `:::dev` 黄色「开发版预览」提示；配方和图鉴由 `build_tutorial.py` 的 `UPCOMING` 集合加角标。**发下一版时**：把预览内容搬进 changelog 的新版本段，把正文开发标签改成 `{{since:版本号}}`，清空已发布的 `UPCOMING`，更新首页和本说明的正式版基准，再新开下一段预览。三语必须同步后重新构建。（mryt233 改，2026-10-30）

## GitHub 在线中文教程（2026-10-24）

- 玩家直接打开：https://mryt233.github.io/Orients-Hotpot-Chronicles/zh-cn/
- GitHub Pages 发布来源：`main` 分支 `/docs`；网页文件为仓库 `docs/zh-cn/index.html`，与本地生成的单文件教程保持原始字节一致。
- `docs/index.html` 跳到中文教程，`docs/.nojekyll` 使用原生静态文件；README 已添加入口。
- 本地照常维护正文、图片并生成教程；用户要求上传时，再将这份单文件同步至公开路径。不要把主线未发布代码混入教程提交，也不要为更新教程修改旧发布或旧标签。
- 历史发布提交：`932e9e1eb38834e5facec29ae0d35a57130aa2a2`。

## 英文单文件教程（2026-10-24）

- 成品：`Minecraft_Orients_Hotpot_Chronicles_Player_Guide_EN.html`，双击打开，图片全部内嵌，可单独分发。
- 英文正文：`content-en/*.md`，和简体同步到 **0.1.4-alpha 正式版 + r130 开发版预览**；物品名使用游戏 `en_us.json`。2026-10-30 已迁移补齐，未外发校对。（mryt233 改）
- 在 `mod/` 下运行 `python tools/build_tutorial_en.py` 重新生成，不覆盖中文成品。脚本核对正文指令、图片、配方、版本标记、链接和锚点，并记录中文来源哈希。
- 中文正文更新后，先同步对应英文正文，再重新生成英文版；作者均为 mryt233。
- 浏览器验证：`python tools/check_tutorial_en.py desktop` / `mobile`，截图和检查结果在 `qa-en/`。本次仅生成本地英文版，未上传 GitHub Pages。

## 三语合一版维护（2026-10-25 更新）

- 先同步三语正文，依次运行 `python tools/build_tutorial.py`、`python tools/build_tutorial_en.py`、`python tools/build_tutorial_tw.py`、`python tools/build_tutorial_bilingual.py`。最后一个脚本合并三份单语言成品；图片不重复嵌入，章节锚点必须一致。
- 交互源码为 `assets/bilingual.js`；修改后重新运行双语生成脚本即可。单语言成品仍保留。
- 默认使用已记住的语言；首次按浏览器语言选择。`?lang=zh-cn` / `?lang=zh_tw` / `?lang=en` 可指定打开语言，章节链接例如 `?lang=zh_tw#page-sauce`。切换后目录、正文、物品提示、搜索提示一并变化。
- 双击离线打开或放到支持 HTML / JavaScript 的静态网页托管即可使用；若 Wiki 过滤脚本，请单独托管本 HTML，再从 Wiki 链接或嵌入。
- 验证：`python tools/check_tutorial_bilingual.py desktop` / `mobile`，结果位于 `qa-bilingual/`。本次未更新线上网页。

## zh_tw 独立版（2026-10-25）

- 成品：`Minecraft_Orients_Hotpot_Chronicles_Player_Guide_zh_tw.html`；可单独分发或离线双击打开。旧中英合一成品另存为 `Minecraft_Orients_Hotpot_Chronicles_Player_Guide_BILINGUAL.html`。
- 正文在 `content-zh-tw/*.md`，原版物品名称在 `data/vanilla_zh_tw.json`；物品图鉴保留游戏 `zh_tw.json` 的名称，正文解释常用词与游戏名的对应关系，详见 `zh_tw-校订说明.md`。
- 公开网页、文件名和语言选项统一用 `zh_tw`，不使用地区名称作标签；浏览器识别语言所需的标准代码 `lang="zh-TW"` 保留。
- 构建：`python tools/build_tutorial_tw.py`；验证：`python tools/check_tutorial_en.py desktop zh_tw` / `mobile zh_tw`。原英文检查脚本新增可选语言参数，默认仍检查英文版。截图和结果在 `qa-zh_tw/`。

## 指南样式版（书本样式，2026-10-27 起）

`tools/build_tutorial_book.py` 把同一份 `content/*.md` 排成游戏内「火锅指南」那本书的样子，输出单文件
`tutorial/我的世界[之晗的火锅物语]模组-游玩教程（书本样式）.html`。样式 `assets/book.css`、交互 `assets/book.js`，贴图直接用游戏的 `textures/gui/guide_book.png`。
- 分类（左书签）和条目（顶书签）在脚本开头的 `CATEGORIES`、`ENTRY_ICONS` 里配；一般按二级标题拆条目。
- 「查阅」里的合成表大全由脚本直接读配方生成：同一类配方合一张卡、材料自动轮换、点材料停住。配方以 `version-ports/base/0.1.4-alpha/` 的已发布版为准，开发版新加的标「开发版预览」。发新版后再更新发布底本路径。（mryt233 改，2026-10-30）
- 更新日志拆成「开发版预览」和「更新日志」两条；维护提示框不进书。
- 改完两个脚本都要跑：`python tools/build_tutorial.py`（清警告）和 `python tools/build_tutorial_book.py`。
