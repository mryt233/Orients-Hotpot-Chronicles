// Web ports of this mod's GUI screens for the workspace mod viewer (../../viewer).
// Keep in sync with the Java screens when they change:
//   stove  ← client/StoveScreen.java + hotpot/StoveScreenHandler.java
//   hotpot ← client/HotPotScreen.java + hotpot/HotPotScreenHandler.java
// Draw code mirrors the Java (same coordinates, colors, easing and hash), so what you see here is what the game draws.
import { F, playerInventory } from "/gui.js";

const NS = "orients_hotpot_chronicles";
const key = (k) => `text.${NS}.${k}`;

// ---------------------------------------------------------------- palette

const RIM_LINE = 0xff140a04;
const BRASS_DARK = 0xff5a340e;
const BRASS = 0xffb47a1c;
const BRASS_HI = 0xfff0c459;
const COPPER_DARK = 0xff7a3a12;
const COPPER = 0xffc97a2a;
const COPPER_SHADE = 0xff6e3a12;
const WOOD = 0xff3a2310;
const WOOD_SEAM = 0xff1b0f05;
const RIVET = 0xffffe39a;
const INNER_SHADOW = 0xff120704;
const SLOT_SHADOW = 0xff140b05;
const SLOT_LIGHT = 0xff6b4524;
const SLOT_BG = 0xff2a190c;
const LABEL = 0xffe8c88a;
const TITLE = 0xfff0d9a8;

// ---------------------------------------------------------------- math (mirrors HotPotScreen helpers)

const clamp255 = (v) => Math.max(0, Math.min(255, v | 0));
const clamp01 = (v) => Math.max(0, Math.min(1, v));
const shade = (c, f) => (((c >>> 24) << 24) | (clamp255(((c >> 16) & 255) * f) << 16) | (clamp255(((c >> 8) & 255) * f) << 8) | clamp255((c & 255) * f)) >>> 0;
const alpha = (c, f) => ((clamp255(((c >>> 24) & 255) * f) << 24) | (c & 0xffffff)) >>> 0;
const mix = (a, b, t) => {
  const ch = (s) => (((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) | 0;
  return ((0xff << 24) | (ch(16) << 16) | (ch(8) << 8) | ch(0)) >>> 0;
};
const frac = (x) => x - Math.floor(x);
const easeOutCubic = (x) => 1 - Math.pow(1 - x, 3);
const easeOutQuad = (x) => 1 - (1 - x) * (1 - x);
const easeInOutSine = (x) => -(Math.cos(Math.PI * x) - 1) / 2;
const easeOutBack = (x) => { const c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2); };
// integer hash → [0,1); identical bit-for-bit in Java
function hash(a, b) {
  let h = (Math.imul(a, 374761393) + Math.imul(b, 668265263)) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}

// ---------------------------------------------------------------- shared pieces

function frame(g, w, h, seams, bottomRivets = true) {
  g.fill(0, 0, w, h, RIM_LINE);
  g.fill(1, 1, w - 1, h - 1, BRASS_DARK);
  g.fill(2, 2, w - 2, h - 2, BRASS);
  g.fill(2, 2, w - 2, 5, BRASS_HI);
  g.fill(2, h - 5, w - 2, h - 2, COPPER_SHADE);
  g.fill(3, 3, w - 3, h - 3, WOOD);
  for (let i = 1; i <= seams; i++) {
    const ly = 3 + Math.floor((i * (h - 6)) / (seams + 1));
    g.fill(3, ly, w - 3, ly + 1, WOOD_SEAM);
    g.fill(3, ly + 1, w - 3, ly + 2, 0x14ffffff);
  }
  rivet(g, 6, 6);
  rivet(g, w - 10, 6);
  if (bottomRivets) {
    rivet(g, 6, h - 10);
    rivet(g, w - 10, h - 10);
  }
}

function rivet(g, x, y) {
  g.fill(x, y, x + 4, y + 4, BRASS_DARK);
  g.fill(x, y, x + 4, y + 1, BRASS_HI);
  g.fill(x + 1, y + 1, x + 3, y + 3, RIVET);
}

// recessed 18x18 slot around an item position (x, y)
function slotFrame(g, x, y) {
  g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_LIGHT);
  g.fill(x - 1, y - 1, x + 16, y + 16, SLOT_SHADOW);
  g.fill(x, y, x + 16, y + 16, SLOT_BG);
}

function inventoryFrames(g, x, y, hotbarY) {
  for (let row = 0; row < 3; row++) for (let col = 0; col < 9; col++) slotFrame(g, x + col * 18, y + row * 18);
  for (let col = 0; col < 9; col++) slotFrame(g, x + col * 18, hotbarY);
}

// sample player inventory shared by both screens
const PLAYER_ITEMS = {
  0: { item: `${NS}:bamboo_chopsticks` },
  1: { item: "minecraft:bowl", count: 4 },
  2: { item: "minecraft:coal", count: 23 },
  3: { item: "minecraft:beef", count: 12 },
  4: { item: "minecraft:potato", count: 9 },
  9: { item: "minecraft:cod", count: 6 },
  10: { item: "minecraft:kelp", count: 16 },
  11: { item: "minecraft:oak_log", count: 32 },
};

// ===================================================================== stove

const FUELS = { coal: 1600, charcoal: 1600, oak_log: 300, lava_bucket: 20000 };
const ST = { W: 176, H: 166, HX: 44, HY: 23, HW: 52, HH: 46, FUEL_X: 112, FUEL_Y: 35, INV_X: 8, INV_Y: 84, HOTBAR_Y: 142 };
// 14x14 flame like the vanilla furnace; '#' outline body, 'o' core
const FLAME = [
  "      #       ",
  "      ##      ",
  "     ###      ",
  "     ###  #   ",
  "  #  ####  #  ",
  "  # #####  ## ",
  " ## ####o# ## ",
  " ######oo#### ",
  " #####ooo#### ",
  "######oooo####",
  "#####oooooo###",
  "####oooooooo##",
  " ###oooooooo# ",
  "  ##########  ",
];

const stove = {
  id: "stove",
  name: "灶膛界面",
  blocks: ["brick_stove"],
  open: "右键灶台：没装锅时点哪里都行；装了锅时点砖砌的灶身（或手里拿着跟锅无关的东西点锅）",
  size: [ST.W, ST.H],
  textures: ["minecraft:block/bricks", `${NS}:block/stove_front`, `${NS}:block/stove_front_lit`],
  params: [
    { key: "fuel", label: "燃料槽", options: [["minecraft:coal", "煤炭"], ["minecraft:oak_log", "原木"], ["minecraft:lava_bucket", "岩浆桶"], ["", "空"]], default: "minecraft:coal" },
    { key: "burning", label: "灶火", options: [["on", "正在烧"], ["off", "没点火"]], default: "on" },
    { key: "ratio", label: "剩余燃烧", range: [0.05, 1, 0.05], default: 0.7 },
  ],

  slots(s) {
    const fuel = { x: ST.FUEL_X, y: ST.FUEL_Y, kind: "fuel" };
    if (s.fuel) Object.assign(fuel, { item: s.fuel, count: s.fuel.endsWith("bucket") ? 1 : 8 });
    return [fuel, ...playerInventory(ST.INV_X, ST.INV_Y, ST.HOTBAR_Y, PLAYER_ITEMS)];
  },

  derive(s) {
    const total = FUELS[(s.fuel || "minecraft:coal").split(":")[1]] || 1600;
    const burnTicks = s.burning === "on" ? Math.round(total * s.ratio) : 0;
    return { burnTicks, burnRatio: burnTicks > 0 ? s.ratio : 0, hasFuelInSlot: !!s.fuel };
  },

  background(g, s, w, h, time) {
    const d = this.derive(s);
    const t = time / 1000;
    frame(g, w, h, 3, false);
    this.hearth(g, d);
    // fuel slot: brass ring around a recessed slot
    const fx = ST.FUEL_X, fy = ST.FUEL_Y;
    g.fill(fx - 4, fy - 4, fx + 20, fy + 20, RIM_LINE);
    g.fill(fx - 3, fy - 3, fx + 19, fy + 19, BRASS_DARK);
    g.fill(fx - 2, fy - 2, fx + 18, fy + 18, BRASS);
    g.fill(fx - 2, fy - 2, fx + 18, fy - 1, BRASS_HI);
    slotFrame(g, fx, fy);
    // arrow fuel → hearth
    // hearth + arrow + fuel slot form one group centred in the panel
    const ax = ST.HX + ST.HW + 3, ay = 39;
    g.fill(ax + 5, ay + 1, ax + 8, ay + 8, BRASS);
    g.fill(ax + 3, ay + 3, ax + 8, ay + 6, BRASS_HI);
    g.fill(ax + 1, ay + 4, ax + 8, ay + 5, BRASS_HI);
    this.flameGauge(g, fx + 1, fy + 22, d.burnRatio);
    inventoryFrames(g, ST.INV_X, ST.INV_Y, ST.HOTBAR_Y);
  },

  // hearth window = the stove block's own front texture (2026-10-26, = StoveScreen.drawHearth): rows 5..16 of
  // stove_front(_lit), ×3, centred; the window frames just that (no extra bricks around it). The game animates the lit texture
  // (16 frames); screen textures in the viewer are single frames, so here it shows frame 0.
  hearth(g, d) {
    const { HX, HY, HW, HH } = ST;
    const rows = 11, k = 3, w = 16 * k, h = rows * k;
    const x = HX + (HW - w) / 2, y = HY + (HH - h) / 2;
    const img = g.textures.get(d.burnTicks > 0 ? `${NS}:block/stove_front_lit` : `${NS}:block/stove_front`);
    if (img) g.g.drawImage(img, 0, 16 - rows, 16, rows, (g.ox + x) * g.s, (g.oy + y) * g.s, w * g.s, h * g.s);
    g.fill(x - 1, y - 1, x + w + 1, y, RIM_LINE);
    g.fill(x - 1, y + h, x + w + 1, y + h + 1, RIM_LINE);
    g.fill(x - 1, y, x, y + h, RIM_LINE);
    g.fill(x + w, y, x + w + 1, y + h, RIM_LINE);
  },

  // vanilla-furnace style flame that empties from the top as fuel burns down
  flameGauge(g, x, y, ratio) {
    const lit = Math.round(14 * ratio);
    FLAME.forEach((row, ry) => {
      for (let rx = 0; rx < 14; rx++) {
        const c = row[rx];
        if (c === " ") continue;
        const on = ry >= 14 - lit;
        g.fill(x + rx, y + ry, x + rx + 1, y + ry + 1, on ? (c === "o" ? 0xfffbe08a : 0xffe2582b) : 0xff24160a);
      }
    });
  },

  foreground(g, s, w) {
    g.text(g.tr(key("screen.stove")), 12, 7, TITLE, true);
    g.fill(12, 17, w - 12, 18, BRASS_DARK);
    g.text(g.tr(key("screen.inventory")), ST.INV_X, ST.INV_Y - 11, LABEL, true);
  },

  tooltip(g, slot, s) {
    if (slot.kind !== "fuel" || slot.item) return null;
    const d = this.derive(s);
    const lines = [{ text: g.tr(key("screen.stove.fuel")), color: F.GOLD }, ...g.wrap(g.tr(key("screen.stove.hint")), 140).map((l) => ({ text: l, color: F.GRAY }))];
    if (d.burnTicks <= 0 && !d.hasFuelInSlot) lines.push({ text: g.tr(key("screen.stove.waiting")), color: F.DARK_GRAY });
    return lines;
  },
};

// ==================================================================== hotpot

const HP = { W: 368, H: 212, GRID_X: 13, GRID_Y: 27, GAP: 4, PANEL_X: 196, INV_X: 198, INV_Y: 128, HOTBAR_Y: 186 };
const GW = 9 * 18 + 2 * HP.GAP; // 170
// pot metal, same as HotPotScreen.Metal (taken from the pot textures)
const METALS = {
  copper: { shade: 0xff6e3524, dark: 0xff8e4a33, mid: 0xffc26b4c, light: 0xffe3826c, rivet: 0xfff2a88c },
  iron: { shade: 0xff101113, dark: 0xff1c1e21, mid: 0xff3d3f44, light: 0xff5a5d63, rivet: 0xff7a7d84 },
};
const SOUPS = {
  clear: { base: 0xffb4bcbc, hi: 0xfff4f4ec, lo: 0xffc6ccca, bits: [0xff6faf3a, 0xff8fcf52, 0xffd04a2a] },
  rich_stock: { base: 0xffe3d7bf, hi: 0xffffffff, lo: 0xfff2e7d2, bits: [0xfffff8ea, 0xff6faf3a, 0xffd9c9a8] },
  mushroom: { base: 0xff7c5836, hi: 0xffe8c88a, lo: 0xff9a7048, bits: [0xff4a3220, 0xffc9a16a, 0xff5e4026] },
  spicy: { base: 0xffb32a1e, hi: 0xffffb070, lo: 0xffd94a2e, bits: [0xffe8862a, 0xff7a1408, 0xff3a1208] },
  tomato: { base: 0xffd8482a, hi: 0xffff9a6a, lo: 0xffe86040, bits: [0xfff2c14a, 0xffb82818, 0xff6faf3a] },
  three_fresh: { base: 0xffd8c79a, hi: 0xfffff6dc, lo: 0xffe6d8b0, bits: [0xff8a5a34, 0xfff08a30, 0xff3e7a3a] },
};
// = PotLayout：每个大格属于哪边汤（-1 = 不用），几边（鸳鸯锅 / 田字锅，2026-10-23 mryt233 加）
const LAYOUTS = {
  nine: { sides: 1, cells: [0, 0, 0, 0, 0, 0, 0, 0, 0] },
  s: { sides: 2, cells: [0, 0, 1, 0, -1, 1, 0, 1, 1] },
  o: { sides: 2, cells: [0, 0, 0, 1, -1, 1, 0, 0, 0] },
  square: { sides: 2, cells: [0, 0, 0, 1, -1, 1, 0, 0, 0] },   // 方心鸳鸯锅 2026-10-25
  split: { sides: 2, cells: [0, -1, 1, 0, -1, 1, 0, -1, 1] },
  four: { sides: 4, cells: [0, 0, 1, 3, -1, 1, 3, 2, 2] },
};
const cellOf = (i) => Math.floor(Math.floor(i / 9) / 3) * 3 + Math.floor((i % 9) / 3);
// = PotLayout.sideAt（锅腔比例 0..1）；-1 = 隔板
function yySideAt(shape, u, v) {   // = PotLayout.sideAt；-1 = 隔板
  const x = u * 13, z = v * 13, c = 6.5;
  if (shape === "split") return Math.abs(x - c) < 0.5 ? -1 : x < c ? 0 : 1;
  if (shape === "four") {
    if (Math.abs(x - c) < 0.5 || Math.abs(z - c) < 0.5) return -1;
    return z < c ? (x < c ? 0 : 1) : (x < c ? 3 : 2);
  }
  if (shape === "o") { const d = Math.hypot(x - c, z - c); return Math.abs(d - 3.4) < 0.5 ? -1 : d < 3.4 ? 1 : 0; }
  if (shape === "square") { const d = Math.max(Math.abs(x - c), Math.abs(z - c)); return Math.abs(d - 3.0) < 0.5 ? -1 : d < 3.0 ? 1 : 0; }
  const r = c / 2;
  const dUp = x >= c ? Math.abs(Math.hypot(x - c, z - r) - r) : Math.hypot(x - c, Math.min(Math.abs(z), Math.abs(z - c)));
  const dDn = x <= c ? Math.abs(Math.hypot(x - c, z - c - r) - r) : Math.hypot(x - c, Math.min(Math.abs(z - c), Math.abs(z - 2 * c)));
  if (Math.min(dUp, dDn) < 0.5) return -1;
  const left = z < c ? x < c + Math.sqrt(Math.max(0, r * r - (z - r) ** 2)) : x < c - Math.sqrt(Math.max(0, r * r - (z - c - r) ** 2));
  return left ? 0 : 1;
}
const YY_INSET = 4;   // = PotLayout.GUI_INSET：鸳鸯 / 田字锅 9×9 小格均匀排满 170 像素锅面，四周留 4
const yyCache = {};
function yyLayout(shape) {   // = PotLayout.sideOfSlot（每小格取 5×5 个点，全在同一边才用）+ regions（半像素格子一行行合并）
  if (yyCache[shape]) return yyCache[shape];
  const slotSides = [];
  for (let slot = 0; slot < 81; slot++) {
    const x0 = (YY_INSET + (slot % 9) * 18 + 1) / GW, y0 = (YY_INSET + Math.floor(slot / 9) * 18 + 1) / GW, span = 16 / GW;
    let side = yySideAt(shape, x0, y0);
    for (let i = 0; i <= 4 && side >= 0; i++) for (let j = 0; j <= 4; j++) if (yySideAt(shape, x0 + (span * i) / 4, y0 + (span * j) / 4) !== side) { side = -1; break; }
    slotSides.push(side);
  }
  const regions = [[], [], [], []], n = 26;
  for (let j = 0; j < n; j++) {
    let i = 0;
    while (i < n) {
      const sd = yySideAt(shape, (i + 0.5) / n, (j + 0.5) / n);
      let k = i;
      while (k < n && yySideAt(shape, (k + 0.5) / n, (j + 0.5) / n) === sd) k++;
      if (sd >= 0) regions[sd].push([i / n, j / n, k / n, (j + 1) / n]);
      i = k;
    }
  }
  return (yyCache[shape] = { slotSides, regions });
}
const COOKED = { "minecraft:beef": "minecraft:cooked_beef", "minecraft:potato": "minecraft:baked_potato", "minecraft:cod": "minecraft:cooked_cod", "minecraft:kelp": "minecraft:dried_kelp", "minecraft:mutton": "minecraft:cooked_mutton", "minecraft:chicken": "minecraft:cooked_chicken" };

// pot contents presets: slot index -> [item, doneness (0..1000), cooked]
const PRESETS = {
  empty: {},
  raw: { 10: ["minecraft:beef"], 11: ["minecraft:beef"], 13: ["minecraft:potato"], 40: ["minecraft:cod"], 42: ["minecraft:kelp"], 67: ["minecraft:potato"] },
  cooking: { 10: ["minecraft:beef", 820], 11: ["minecraft:beef", 640], 13: ["minecraft:potato", 300], 40: ["minecraft:cod", 1000, true], 42: ["minecraft:kelp", 120], 50: ["minecraft:mutton", 470], 67: ["minecraft:potato", 1000, true], 70: ["minecraft:chicken", 900] },
  cooked: Object.fromEntries([0, 1, 2, 9, 10, 11, 18, 19, 20, 30, 31, 32, 40, 48, 49, 50, 60, 61, 62, 70, 78, 79, 80].map((i, n) => [i, [["minecraft:beef", "minecraft:potato", "minecraft:cod", "minecraft:kelp"][n % 4], 1000, true]])),
};

const hotpot = {
  id: "hotpot",
  name: "火锅界面",
  blocks: ["copper_nine_grid_hotpot", "iron_nine_grid_hotpot", "brick_stove",
    ...["copper", "iron"].flatMap((m) => ["taiji_yuanyang_hotpot", "round_yuanyang_hotpot", "split_yuanyang_hotpot", "four_grid_hotpot"].map((k) => `${m}_${k}`))],
  open: "手拿筷子右键锅（放在地上的锅、或坐在灶台上的锅都可以）",
  size: [HP.W, HP.H],
  params: [
    { key: "pot", label: "锅", options: [["copper", "铜锅"], ["iron", "铁锅"]], default: "copper" },
    { key: "layout", label: "分格", options: [["nine", "九宫格"], ["s", "太极鸳鸯"], ["o", "圆心鸳鸯"], ["square", "方心鸳鸯"], ["split", "直隔鸳鸯"], ["four", "田字"]], default: "nine" },
    { key: "soup", label: "汤底（第 1 边）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["", "空锅"]], default: "spicy" },
    { key: "soup2", label: "第 2 边", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["", "没汤"]], default: "clear" },
    { key: "soup3", label: "第 3 边（田字）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["", "没汤"]], default: "mushroom" },
    { key: "soup4", label: "第 4 边（田字）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["", "没汤"]], default: "" },
    { key: "fire", label: "灶火", options: [["on", "在烧"], ["off", "熄了"], ["none", "没灶台"]], default: "on" },
    { key: "ratio", label: "剩余燃烧", range: [0.05, 1, 0.05], default: 0.6 },
    { key: "food", label: "锅里", options: [["cooking", "正在煮"], ["raw", "刚下锅"], ["cooked", "全熟了"], ["empty", "空的"]], default: "cooking" },
  ],

  derive(s) {
    const burnTotal = s.fire === "none" ? 0 : 1600;
    const burnTicks = s.fire === "on" ? Math.round(burnTotal * s.ratio) : 0;
    const layout = LAYOUTS[s.layout] || LAYOUTS.nine;
    const sideSoups = [s.soup, s.soup2, s.soup3, s.soup4].slice(0, layout.sides);
    const hasSoup = sideSoups.some((x) => !!x);
    const ratio = burnTotal > 0 ? Math.min(1, burnTicks / burnTotal) : 0;
    // soupOf(slot) = HotPotScreenHandler.soupOfSlot
    const multi = layout.sides > 1;
    const yy = multi ? yyLayout(s.layout) : null;
    const sideOf = (i) => (multi ? yy.slotSides[i] : layout.cells[cellOf(i)]);   // = PotLayout.sideOfSlot
    const soupOf = (i) => { const side = sideOf(i); return side < 0 ? "" : sideSoups[side] || ""; };
    return { hasSoup, soup: s.soup, layout, multi, yy, shape: s.layout, sideOf, sideSoups, soupOf, burnTotal, burnTicks, heat: hasSoup && burnTicks > 0 ? 0.45 + 0.55 * ratio : 0 };
  },

  slots(s) {
    const preset = PRESETS[s.food] || {};
    const d = this.derive(s);
    const pot = [];
    for (let i = 0; i < 81; i++) {
      const row = Math.floor(i / 9), col = i % 9;
      if (d.sideOf(i) < 0) continue;   // PotSlot.isEnabled：鸳鸯 / 田字锅压到隔板的格子空着
      const p = preset[i];
      const soup = d.soupOf(i);
      const cooked = !!(p && p[2] && soup);
      pot.push({
        // slot.x/y is the item position; the 18px cell starts one pixel up-left
        x: d.multi ? HP.GRID_X + YY_INSET + col * 18 + 1 : HP.GRID_X + col * 18 + Math.floor(col / 3) * HP.GAP + 1,
        y: d.multi ? HP.GRID_Y + YY_INSET + row * 18 + 1 : HP.GRID_Y + row * 18 + Math.floor(row / 3) * HP.GAP + 1,
        kind: "pot", index: i,
        item: p ? (cooked ? COOKED[p[0]] || p[0] : p[0]) : undefined,
        doneness: p && soup ? p[1] || 0 : 0,
        cooked,
      });
    }
    return [...pot, ...playerInventory(HP.INV_X, HP.INV_Y, HP.HOTBAR_Y, { ...PLAYER_ITEMS, 2: undefined, 11: undefined })];
  },

  background(g, s, w, h, time) {
    const d = this.derive(s);
    const t = time / 1000;
    frame(g, w, h, 4, false);
    rivet(g, 6, h - 10);
    // header plaque
    g.fill(6, 4, w - 6, 17, 0xe01a0f07);
    g.fill(6, 17, w - 6, 18, BRASS_DARK);
    g.fill(8, 5, w - 8, 6, 0x30fff0c0);

    const gx = HP.GRID_X, gy = HP.GRID_Y;
    this.pot(g, gx, gy, METALS[s.pot] || METALS.copper);
    g.fill(gx - 2, gy - 2, gx + GW + 2, gy + GW + 2, INNER_SHADOW);
    this.soups(g, d, gx, gy, t, METALS[s.pot] || METALS.copper);
    if (!d.multi) this.dividers(g, gx, gy, METALS[s.pot] || METALS.copper);
    if (d.heat > 0) this.steam(g, d, gx, gy, t);

    // status card
    const px0 = HP.PANEL_X, px1 = w - 6;
    g.fill(px0, 20, px1, 104, RIM_LINE);
    g.fill(px0 + 1, 21, px1 - 1, 103, 0xff22140a);
    g.fill(px0 + 1, 21, px1 - 1, 22, 0x20fff0c0);
    inventoryFrames(g, HP.INV_X, HP.INV_Y, HP.HOTBAR_Y);
  },

  // the pot body in its own metal (copper = vanilla copper block colours, iron = iron grey); no gold
  pot(g, gx, gy, m) {
    const x0 = gx - 7, y0 = gy - 7, x1 = gx + GW + 7, y1 = gy + GW + 7;
    g.fill(x0, y0, x1, y1, RIM_LINE);
    g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, m.dark);
    g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, m.mid);
    g.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 5, m.light);
    g.fill(x0 + 2, y1 - 5, x1 - 2, y1 - 2, m.shade);
    g.fill(x0 + 2, y0 + 2, x0 + 5, y1 - 2, m.light);
    g.fill(x1 - 5, y0 + 2, x1 - 2, y1 - 2, m.shade);
    const potRivet = (x, y) => {
      g.fill(x, y, x + 4, y + 4, m.dark);
      g.fill(x, y, x + 4, y + 1, m.light);
      g.fill(x + 1, y + 1, x + 3, y + 3, m.rivet);
    };
    for (let i = 0; i <= 2; i++) {
      const cx = x0 + 4 + Math.floor((i * (x1 - x0 - 10)) / 2);
      potRivet(cx, y0 + 2);
      potRivet(cx, y1 - 6);
      const cy = y0 + 4 + Math.floor((i * (y1 - y0 - 10)) / 2);
      potRivet(x0 + 2, cy);
      potRivet(x1 - 6, cy);
    }
  },

  // = HotPotScreen.drawSoups：九宫格整锅一种；鸳鸯 / 田字锅先铺锅底金属，每边按 regions 填汤底色（隔板露出金属），
  // 高光、配料、气泡只画在各自那边
  soups(g, d, gx, gy, t, m) {
    if (!d.multi) return this.soup(g, d, gx, gy, t, d.soup, null);
    g.fill(gx, gy, gx + GW, gy + GW, m.mid);
    d.sideSoups.forEach((id, side) => {
      const color = SOUPS[id] ? SOUPS[id].base : 0xff1b1c20;
      for (const r of d.yy.regions[side]) g.fill(gx + Math.round(r[0] * GW), gy + Math.round(r[1] * GW), gx + Math.round(r[2] * GW), gy + Math.round(r[3] * GW), color);
    });
    g.fill(gx, gy, gx + GW, gy + 4, 0x30000000);
    g.fill(gx, gy + GW - 4, gx + GW, gy + GW, 0x22000000);
    g.fill(gx, gy, gx + 4, gy + GW, 0x26000000);
    g.fill(gx + GW - 4, gy, gx + GW, gy + GW, 0x1c000000);
    d.sideSoups.forEach((id, side) => this.soup(g, d, gx, gy, t, id, (x, y) => yySideAt(d.shape, (x - gx) / GW, (y - gy) / GW) === side));
  },

  // top-down soup: convection carries garnish in slow orbits, sheen patches breathe,
  // bubbles swell in place (ease-out-back) then pop into a fading ring (ease-out-cubic)
  // keep(x, y)：鸳鸯 / 田字锅只画落在这一边的东西；null = 整锅（同时铺底色）
  soup(g, d, gx, gy, t, soupId, keep) {
    const soup = SOUPS[soupId];
    if (!keep) {
      g.fill(gx, gy, gx + GW, gy + GW, soup ? soup.base : 0xff1b1c20);
      // depth: darker toward the pot wall
      g.fill(gx, gy, gx + GW, gy + 4, 0x30000000);
      g.fill(gx, gy + GW - 4, gx + GW, gy + GW, 0x22000000);
      g.fill(gx, gy, gx + 4, gy + GW, 0x26000000);
      g.fill(gx + GW - 4, gy, gx + GW, gy + GW, 0x1c000000);
    }
    if (!soup) return;
    const cx = gx + GW / 2, cy = gy + GW / 2;

    // sheen: soft light patches fading in and out
    for (let i = 0; i < 5; i++) {
      const life = 1.8 + hash(i, 11) * 1.2;
      const cyc = Math.floor(t / life + hash(i, 5));
      const u = frac(t / life + hash(i, 5));
      const a = easeInOutSine(u < 0.5 ? u * 2 : 2 - u * 2);
      const x = gx + 8 + Math.floor(hash(i, cyc * 3) * (GW - 30));
      const y = gy + 8 + Math.floor(hash(i, cyc * 3 + 1) * (GW - 16));
      if (keep && !(keep(x, y) && keep(x + 14, y + 2))) continue;
      g.fill(x, y, x + 14, y + 2, alpha(0xfffff4d8, 0.13 * a));
      g.fill(x + 3, y + 2, x + 10, y + 3, alpha(0xfffff4d8, 0.09 * a));
    }
    // garnish riding the convection: angular speed grows with heat, with a slow wobble
    const spin = 0.1 + 0.6 * d.heat;
    for (let i = 0; i < 22; i++) {
      const r = 14 + hash(i, 1) * 66;
      const ang = hash(i, 2) * Math.PI * 2 + t * spin * (0.6 + 0.8 * hash(i, 3)) * (i % 2 ? 1 : -1) * (30 / r) + 0.35 * Math.sin(t * 0.7 + i);
      const x = Math.round(cx + Math.cos(ang) * r);
      const y = Math.round(cy + Math.sin(ang) * r * 0.96);
      const c = soup.bits[i % soup.bits.length];
      if (keep && !keep(x + 1, y + 0.5)) continue;
      g.fill(x, y, x + 2, y + (i % 3 === 0 ? 2 : 1), c);
    }
    // bubbles: froth clusters popping up all over the pot, plus a few lone big bubbles.
    // each one swells, sits and drifts with the convection, then shrinks away with a droplet or two.
    if (d.heat > 0) {
      const sizeOf = (h, big) => (big ? 3 + Math.floor(h * 2.99) : 1 + Math.floor(Math.pow(h, 2.2) * 3.99)); // mostly 1-2 px
      const clusters = Math.round(4 + 8 * d.heat);
      for (let k = 0; k < clusters; k++) {
        const period = (0.9 + 0.5 * hash(k, 21)) / (0.7 + 0.5 * d.heat);
        const tt = t / period + hash(k, 22);
        const cyc = Math.floor(tt), c = frac(tt);
        const cx = gx + 5 + hash(k, cyc * 2) * (GW - 10);
        const cy = gy + 5 + hash(k, cyc * 2 + 1) * (GW - 10);
        if (keep && !keep(cx, cy)) continue;
        const m = 3 + Math.floor(hash(k, cyc * 3) * 4.99);
        for (let j = 0; j < m; j++) {
          const s0 = 0.35 * hash(k * 7 + j, cyc + 31);
          const life = 0.45 + 0.2 * hash(k * 7 + j, cyc + 32);
          const u = (c - s0) / life;
          if (u < 0 || u > 1) continue;
          const ang = hash(k * 7 + j, cyc + 33) * Math.PI * 2;
          const dist = j === 0 ? 0 : 2 + 4 * hash(k * 7 + j, cyc + 34);
          this.boilBubble(g, gx, gy, cx + Math.cos(ang) * dist, cy + Math.sin(ang) * dist, sizeOf(hash(k * 7 + j, cyc + 35), false), u, soup, k * 31 + j);
        }
      }
      const lone = Math.round(2 + 4 * d.heat);
      for (let i = 0; i < lone; i++) {
        const period = (0.7 + 0.4 * hash(i, 61)) / (0.7 + 0.5 * d.heat);
        const tt = t / period + hash(i, 62);
        const cyc = Math.floor(tt), u = frac(tt);
        const x = gx + 7 + hash(i, cyc * 2 + 70) * (GW - 14);
        const y = gy + 7 + hash(i, cyc * 2 + 71) * (GW - 14);
        if (keep && !keep(x, y)) continue;
        this.boilBubble(g, gx, gy, x, y, sizeOf(hash(i, cyc + 72), true), u, soup, 500 + i);
      }
    }
  },

  // one bubble over its life u (0..1): swell (ease-out), sit and drift, then shrink away fast
  boilBubble(g, gx, gy, x, y, rMax, u, soup, seed) {
    let r;
    if (u < 0.35) r = rMax * easeOutCubic(u / 0.35);
    else if (u < 0.85) r = rMax * (1 + 0.08 * Math.sin((u - 0.35) * 18));
    else r = rMax * (1 - (u - 0.85) / 0.15) ** 2;
    const drift = 1.5 * easeOutCubic(u);
    const bx = x + drift * 0.6, by = y - drift;
    if (r >= 0.5) this.bubble(g, gx, gy, bx, by, r, soup);
    if (u > 0.85) {
      // a droplet or two flicked out as it pops
      const p = (u - 0.85) / 0.15;
      const c = alpha(soup.hi, 0.9 * (1 - p));
      for (let j = 0; j < 1 + (seed & 1); j++) {
        const a = hash(seed, j + 90) * Math.PI * 2;
        const dd = rMax + 1 + 2 * p;
        const px = Math.round(bx + Math.cos(a) * dd), py = Math.round(by + Math.sin(a) * dd);
        g.fill(px, py, px + 1, py + 1, c);
      }
    }
  },

  // a round bubble seen from above: lighter film inside, bright rim top-left, shadowed rim
  // bottom-right and a highlight spot; clipped to the soup area
  bubble(g, gx, gy, x, y, r, soup) {
    const R = Math.ceil(r + 0.5);
    const body = alpha(mix(mix(soup.base, soup.lo, 0.5), soup.hi, 0.18), 0.92);
    const dark = shade(soup.base, 0.68);
    for (let dy = -R; dy <= R; dy++) {
      for (let dx = -R; dx <= R; dx++) {
        const dist = Math.hypot(dx, dy);
        if (dist > r + 0.35) continue;
        const px = Math.round(x + dx), py = Math.round(y + dy);
        if (px < gx || py < gy || px >= gx + GW || py >= gy + GW) continue;
        let c = body;
        if (dist > r - 0.65) c = dx + dy < 0 ? mix(soup.lo, soup.hi, 0.55) : dark; // the rim
        g.fill(px, py, px + 1, py + 1, c);
      }
    }
    const hl = Math.max(1, Math.round(r * 0.35));
    const hx = Math.round(x - r * 0.4), hy = Math.round(y - r * 0.4);
    g.fill(hx, hy, hx + hl, hy + hl, soup.hi);
  },

  // translucent steam puffs drifting up and dissolving
  steam(g, d, gx, gy, t) {
    const m = Math.round(3 + 5 * d.heat);
    for (let i = 0; i < m; i++) {
      const life = 1.3 + hash(i, 41) * 0.8;
      const cyc = Math.floor(t / life + hash(i, 42));
      const u = frac(t / life + hash(i, 42));
      const x = Math.round(gx + 12 + hash(i, cyc * 7) * (GW - 24) + 3 * Math.sin(u * Math.PI * 2 + i));
      const y = Math.round(gy + 30 + hash(i, cyc * 7 + 1) * (GW - 40) - 22 * easeOutQuad(u));
      const sz = 2 + Math.round(4 * easeOutCubic(u));
      const c = alpha(0xffffffff, 0.2 * Math.pow(Math.sin(Math.PI * u), 1.5));
      g.fill(x - sz, y - (sz >> 1), x + sz, y + (sz >> 1), c);
      g.fill(x - (sz >> 1), y - sz, x + (sz >> 1), y + sz, c);
    }
  },

  dividers(g, gx, gy, m) {
    const cuts = [3 * 18, 6 * 18 + HP.GAP];
    for (const cut of cuts) {
      const vx = gx + cut;
      g.fill(vx, gy, vx + 1, gy + GW, m.dark);
      g.fill(vx + 1, gy, vx + 3, gy + GW, m.mid);
      g.fill(vx + 3, gy, vx + 4, gy + GW, m.light);
      const hy = gy + cut;
      g.fill(gx, hy, gx + GW, hy + 1, m.dark);
      g.fill(gx, hy + 1, gx + GW, hy + 3, m.mid);
      g.fill(gx, hy + 3, gx + GW, hy + 4, m.light);
    }
    for (const a of cuts) for (const b of cuts) {
      const cx = gx + a, cy = gy + b;
      g.fill(cx - 2, cy - 2, cx + 6, cy + 6, m.dark);
      g.fill(cx - 1, cy - 1, cx + 5, cy + 5, m.mid);
      g.fill(cx, cy, cx + 4, cy + 4, m.light);
      g.fill(cx + 1, cy + 1, cx + 3, cy + 3, m.rivet);
    }
  },

  // HotPotScreen.drawSlot overlay, drawn after the item
  slotOverlay(g, slot, s, time) {
    if (slot.kind !== "pot") return;
    const sx = slot.x - 1, sy = slot.y - 1;
    g.fill(sx, sy, sx + 18, sy + 1, 0x40000000);
    g.fill(sx, sy, sx + 1, sy + 18, 0x40000000);
    g.fill(sx + 17, sy, sx + 18, sy + 18, 0x24fff0c0);
    g.fill(sx, sy + 17, sx + 18, sy + 18, 0x24fff0c0);
    if (!slot.item) return;
    if (slot.cooked) {
      // smooth breathing glow instead of a hard blink
      const pulse = easeInOutSine(0.5 + 0.5 * Math.sin((time / 1000) * Math.PI * 1.2));
      const glow = mix(0xff4fae3a, 0xff9cff7e, pulse);
      g.fill(sx, sy, sx + 18, sy + 1, glow);
      g.fill(sx, sy + 17, sx + 18, sy + 18, glow);
      g.fill(sx, sy, sx + 1, sy + 18, glow);
      g.fill(sx + 17, sy, sx + 18, sy + 18, glow);
      // check mark
      g.fill(sx + 11, sy + 4, sx + 12, sy + 5, 0xffffffff);
      g.fill(sx + 12, sy + 5, sx + 13, sy + 6, 0xffffffff);
      g.fill(sx + 13, sy + 4, sx + 14, sy + 5, 0xffffffff);
      g.fill(sx + 14, sy + 3, sx + 15, sy + 4, 0xffffffff);
      g.fill(sx + 15, sy + 2, sx + 16, sy + 3, 0xffffffff);
      return;
    }
    const dn = slot.doneness;
    if (dn <= 0) return;
    const ratio = clamp01(dn / 1000);
    g.fill(sx + 1, sy + 1, sx + 17, sy + 17, alpha(0xffe0a23c, 0.08 + 0.16 * ratio));
    g.fill(sx + 2, sy + 15, sx + 16, sy + 17, 0xe0120b08);
    const bw = Math.round(14 * ratio);
    g.fill(sx + 2, sy + 15, sx + 2 + bw, sy + 17, dn >= 1000 ? 0xff63d14c : mix(0xffe0a23c, 0xff9ad04c, ratio));
  },

  foreground(g, s, w, slots, time) {
    const d = this.derive(s);
    // header: pot icon + title
    const ix = 9, iy = 4;
    const m = METALS[s.pot] || METALS.copper;
    g.fill(ix + 2, iy + 2, ix + 12, iy + 10, m.dark);
    g.fill(ix + 1, iy + 1, ix + 13, iy + 4, m.mid);
    g.fill(ix + 1, iy + 1, ix + 13, iy + 2, m.light);
    g.fill(ix + 3, iy + 4, ix + 11, iy + 9, d.hasSoup ? (SOUPS[d.soup].base) : 0xff1a1a1e);
    g.fill(ix + 6, iy + 4, ix + 7, iy + 9, m.mid);
    g.fill(ix + 3, iy + 6, ix + 11, iy + 7, m.mid);
    g.fill(ix, iy + 2, ix + 1, iy + 5, m.dark);
    g.fill(ix + 13, iy + 2, ix + 14, iy + 5, m.dark);
    // 鸳鸯 / 田字锅标题用锅自己的名字（= HotPotScreen 构造器）
    const potId = { s: "taiji_yuanyang_hotpot", o: "round_yuanyang_hotpot", square: "square_yuanyang_hotpot", split: "split_yuanyang_hotpot", four: "four_grid_hotpot" }[s.layout];
    g.text(potId ? g.tr(`block.orients_hotpot_chronicles.${s.pot}_${potId}`) : g.tr(key("screen.title")), 28, 6, TITLE, true);

    const x = HP.PANEL_X + 6, right = w - 12;
    // soup + temperature
    // 鸳鸯 / 田字锅每边一个色块，名字用「/」隔开，放不下就缩小（= HotPotScreen.drawForeground）
    const multi = d.layout.sides > 1;
    let swatchX = x;
    for (const id of d.sideSoups) {
      const sw = multi ? 5 : 9;
      g.fill(swatchX, 26, swatchX + sw, 35, RIM_LINE);
      g.fill(swatchX + 1, 27, swatchX + sw - 1, 34, id ? SOUPS[id].base : 0xff1b1c20);
      swatchX += sw + (multi ? 1 : 0);
    }
    const soupName = (id) => g.tr(key(id ? `soup.${id}` : "soup.none"));
    const name = { text: d.sideSoups.map(soupName).join(" / "), color: d.hasSoup ? F.GOLD : F.GRAY };
    const nameX = swatchX + 4, room = right - nameX, width = g.width(name.text);
    if (width > room && room > 0) {
      const k = Math.max(0.5, room / width);
      g.textScaled(name, nameX, 27 + (1 - k) * 4, k, F.WHITE, true);
    } else {
      g.text(name, nameX, 27, F.WHITE, true);
    }
    // fire: flickering icon + remaining-fuel bar, no numbers
    this.fireIcon(g, x + 1, 39, d.burnTicks > 0, time / 1000);
    g.text(g.tr(key("screen.fire")), x + 13, 41, LABEL, true);
    const bx = x + 40;
    g.fill(bx, 42, right, 48, RIM_LINE);
    g.fill(bx + 1, 43, right - 1, 47, 0xff120b08);
    const bw = d.burnTotal > 0 ? Math.round((right - bx - 2) * Math.min(1, d.burnTicks / Math.max(1, d.burnTotal))) : 0;
    if (bw > 0) {
      g.fill(bx + 1, 43, bx + 1 + bw, 47, 0xffe2582b);
      g.fill(bx + 1, 43, bx + 1 + bw, 45, 0xfff0a23c);
    }
    // ingredients
    const pot = slots.filter((sl) => sl.kind === "pot");
    const filled = pot.filter((sl) => sl.item).length;
    const cooked = pot.filter((sl) => sl.item && sl.cooked).length;
    const capacity = Array.from({ length: 81 }, (_, i) => d.sideOf(i)).filter((x) => x >= 0).length;   // 压到隔板的格子不算
    const end = g.text(g.tr(key("screen.filled"), `${filled}/${capacity}`), x, 55, 0xffe8c88a, true);
    if (cooked > 0) g.text({ text: g.tr(key("screen.cooked"), cooked), color: F.GREEN }, end + 8, 55, F.WHITE, true);
    g.fill(x, 67, right, 68, 0x40fff0c0);
    // context hint
    let hint;
    if (!d.hasSoup) hint = { k: "ui.need_soup", c: F.GRAY };
    else if (d.burnTicks <= 0) hint = { k: "ui.no_fire", c: 0xffff7a6a };
    else if (cooked > 0) hint = { k: "screen.hint.collect", c: 0xff9ad87e };
    else if (filled > 0) hint = { k: "screen.hint.cooking", c: F.GRAY };
    else hint = { k: "screen.hint.add", c: F.GRAY };
    g.wrap(g.tr(key(hint.k)), right - x).slice(0, 3).forEach((line, i) => g.text({ text: line, color: hint.c }, x, 73 + i * 10, F.WHITE, false));

    g.text(g.tr(key("screen.inventory")), HP.INV_X, HP.INV_Y - 11, LABEL, true);
  },

  fireIcon(g, x, y, lit, t) {
    const f = lit ? 0.5 + 0.5 * Math.sin(t * 7.3) * Math.sin(t * 3.1) : 0;
    const outer = lit ? 0xffe2582b : 0xff4a3a30;
    const inner = lit ? mix(0xfff0a23c, 0xfffbe08a, f) : 0xff5e4c3e;
    g.fill(x + 3, y, x + 5, y + 2, outer);
    g.fill(x + 2, y + 2, x + 7, y + 5, outer);
    g.fill(x + 1, y + 4, x + 8, y + 9, outer);
    g.fill(x + 3, y + 5, x + 6, y + 9, inner);
    g.fill(x + 4, y + 3, x + 5, y + 5, inner);
  },

  tooltip(g, slot, s, itemName) {
    if (slot.kind !== "pot" || !slot.item) return null;
    const d = this.derive(s);
    const cell = Math.floor(Math.floor(slot.index / 9) / 3) * 3 + Math.floor((slot.index % 9) / 3);
    let status;
    if (!d.soupOf(slot.index)) status = { text: g.tr(key("ui.no_soup_yet")), color: F.GRAY };
    else if (slot.cooked) status = { text: g.tr(key("ui.cooked")), color: F.GREEN };
    else if (d.burnTicks <= 0) status = { text: g.tr(key("ui.no_fire")), color: F.RED };
    else status = { text: g.tr(key("ui.wait"), (((1000 - slot.doneness) / 1000) * 10).toFixed(1)), color: F.YELLOW };
    return [
      { text: itemName, color: F.WHITE },
      // 鸳鸯 / 田字锅：改成这一格泡在哪种汤里
      d.multi ? { text: g.tr(key(d.soupOf(slot.index) ? `soup.${d.soupOf(slot.index)}` : "soup.none")), color: F.GRAY }
        : { text: g.tr(key("ui.cell"), Math.floor(cell / 3) + 1, (cell % 3) + 1), color: F.GRAY },
      status,
    ];
  },
};


// ===================================================================== sauce station (r103, mryt233)
// Web port of client/SauceStationScreen + hotpot/SauceStationScreenHandler. User's design (2026-10-23): the station is a self-service sauce bar — you
// take a bowl and right-click the small bowls on it to add seasonings (any mix, each kind once per bowl,
// 9 dips per bowl). This screen is ONLY for restocking: empty-hand right-click opens it. Each slot = one of the 9 small
// bowls on the station; bowls are NOT fixed any more (user, later the same day): an empty bowl takes any raw material,
// a bowl that still holds something only takes the same seasoning. 1 raw = 4 portions, up to 64; the bar under a slot shows
// what's left. The "?" button (top right) opens an "ingredients" table on the right: raw items → seasoning → effect.
// Vanilla-style grey container (user: native look, no brass).
const SAUCE_SEASONINGS = [
  // id, raw items (first = icon when one is shown), colour, raw names (zh, as Seasoning.rawNames shows them in game) — same order / colours as hotpot/Seasoning
  ["msg", ["minecraft:wheat"], 0xffececf0, "小麦"],
  ["chili_oil", ["minecraft:blaze_powder"], 0xffc42816, "烈焰粉"],
  ["sweet_sour_sauce", ["minecraft:sweet_berries"], 0xffc85a3c, "甜浆果"],
  ["honey", ["minecraft:honey_bottle"], 0xffeca826, "蜂蜜瓶"],
  ["sesame_oil", ["minecraft:sunflower", "minecraft:pumpkin_seeds"], 0xffcc8a28, "向日葵 / 南瓜种子"],
  ["vinegar", ["minecraft:apple", "minecraft:beetroot"], 0xff462418, "苹果 / 甜菜根"],
  ["sugar", ["minecraft:sugar"], 0xfff0eee8, "糖"],
  ["chopped_scallion", [`${NS}:scallion`], 0xff6cb03a, "小葱"],
  ["minced_cilantro", [`${NS}:cilantro`], 0xff4e9a2c, "香菜"],
  ["garlic_paste", [`${NS}:garlic_paste`], 0xffe8d696, "蒜泥"],
  // mryt233 改（2026-11-06，r137）：蚝油、芝麻酱、花生碎、小米辣，共 14 种（和 hotpot/Seasoning 同序同色）
  ["oyster_sauce", [`${NS}:oyster_sauce`], 0xff5a321e, "蚝油"],
  ["sesame_paste", [`${NS}:sesame_paste`], 0xffc89e64, "芝麻酱"],
  ["crushed_peanuts", [`${NS}:crushed_peanuts`], 0xffdab46e, "花生碎"],
  ["millet_chili", [`${NS}:millet_chili`], 0xffe2281c, "小米辣"],
  // mryt233 改（2026-11-06，r139）：折耳根碎，共 15 种（第 15 种；和 hotpot/Seasoning 同序同色）
  ["minced_zhe_er_gen", [`${NS}:minced_zhe_er_gen`], 0xffd8d2bc, "折耳根碎"],
];
const SAUCE_KEY = (k) => `text.${NS}.${k}`;
// 2026-10-23: the 9 raw slots form a 3×3 grid matching the bowls on the station (row 0 = top step, left → right),
// with a vertical bar right of each slot (SauceStationScreenHandler.GRID_X / GRID_Y / STEP_X / STEP_Y)
const SS = { W: 176, H: 178, GRID_X: 44, GRID_Y: 18, STEP_X: 30, STEP_Y: 22, INV_X: 8, INV_Y: 96, HOTBAR_Y: 154, MAX: 64 };
const ssX = (i) => SS.GRID_X + (i % 3) * SS.STEP_X;
const ssY = (i) => SS.GRID_Y + Math.floor(i / 3) * SS.STEP_Y;
// mryt233 改（r146f）：与 Java 相同的固定单列宽度、内容区、动态行高与分页。
const SG = { GAP: 2, ICONS_W: 34, W: 176, TOP: 22, BOTTOM: SS.H - 24, TEXT_W: 124 };

// mryt233 改（r146f）：网页字体比游戏偏窄；排版用原版 1.21.1 字形步进，避免最后一味的效果错误地挤在首行。
const SG_ASCII_ADVANCES = [4,2,4,6,6,6,6,2,4,4,4,6,2,6,2,6,6,6,6,6,6,6,6,6,6,6,2,2,5,6,5,6,7,6,6,6,6,6,6,6,6,4,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,4,6,4,6,6,3,6,6,6,6,6,5,6,6,2,6,5,3,6,6,6,6,6,6,6,4,6,6,6,6,6,6,4,2,4,7];
function sgWidth(text) {
  return [...text].reduce((width, char) => {
    const code = char.codePointAt(0);
    return width + (code >= 32 && code <= 126 ? SG_ASCII_ADVANCES[code - 32] : 9);
  }, 0);
}
function sgWrap(text) {
  const lines = [];
  let line = "";
  for (const token of String(text).match(/[⺀-￿]|\s+|[^\s⺀-￿]+/g) || []) {
    if (line && sgWidth(line + token) > SG.TEXT_W) {
      lines.push(line.trimEnd()); line = token.trimStart();
    } else line += token;
  }
  if (line) lines.push(line);
  return lines;
}

// what is in each of the 9 bowls for the preview (seasoning index, -1 = empty bowl)
const SAUCE_LAYOUTS = {
  each: [0, 1, 2, 3, 4, 5, 6, 7, 8],
  mixed: [1, 1, 5, -1, 3, 1, 0, 7, -1],
};
// 2026-10-26: both sauce screens now use the wood frame of the stove / hot pot screens (GuiKit) with light shadowed text;
// woodPanel = SauceStationScreen.panel, S_HINT = SauceStationScreen.HINT (mryt233)
const S_HINT = 0xffa88c5c;
function woodPanel(g, w, h, x = 0, y = 0) {
  g.translate(x, y);
  frame(g, w, h, 0, false);   // no wood seams: they would cross the tier labels and the table rows
  g.translate(-x, -y);
}
// vanilla ButtonWidget look (12×12 "?")
function vanillaButton(g, x, y, w, h, label) {
  g.fill(x, y, x + w, y + h, 0xff000000);
  g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xff6f6f6f);
  g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xffaaaaaa);
  g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xffaaaaaa);
  g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xff4a4a4a);
  g.text(label, x + w / 2 - g.width(label) / 2, y + (h - 8) / 2, 0xffffffff, true);
}

const sauceStation = {
  id: "sauce_station",
  name: "蘸料台补货界面",
  blocks: ["oak_sauce_station", "spruce_sauce_station", "birch_sauce_station", "jungle_sauce_station", "acacia_sauce_station", "dark_oak_sauce_station", "mangrove_sauce_station", "cherry_sauce_station", "bamboo_sauce_station", "crimson_sauce_station", "warped_sauce_station", "iron_sauce_station"],
  open: "右键蘸料台先打开「蘸料台总览」，点「补充蘸料台」进到这里（11 种木头 + 铁台都一样）；拿碗右键台上的小碗才是打蘸料。9 格排成 3×3 = 台上 9 个小碗（第一行是最上层，从左到右），空碗放什么原料都行。右上角「?」打开原料表",
  // mryt233 改（r146f）：原料表与主面板等宽，组合宽 354。
  size: (s) => [s.guide === "open" ? SS.W + SG.GAP + SG.W : SS.W, SS.H],
  textures: [],
  params: [
    { key: "layout", label: "碗里放的", options: [["each", "九种各一碗"], ["mixed", "随便放（有重复、有空碗）"]], default: "mixed" },
    { key: "stock", label: "存货", options: [["full", "都有"], ["some", "有几碗没货"], ["none", "全空"]], default: "some" },
    { key: "raw", label: "格子里的原料", options: [["yes", "还有没化开的原料"], ["no", "都化成份了"]], default: "yes" },
    { key: "guide", label: "原料表（?）", options: [["closed", "关"], ["open", "开"]], default: "open" },
  ],

  derive(s) {
    const kinds = SAUCE_LAYOUTS[s.layout].map((k, i) => (s.stock === "none" || (s.stock === "some" && i % 4 === 2) ? -1 : k));
    // portions already in the bowl on the counter (raw items in the slot turn into portions as they get used)
    const portions = kinds.map((k, i) => (k < 0 ? 0 : 1 + ((i * 5) % 4)));
    const raws = kinds.map((k, i) => (k < 0 || s.raw !== "yes" ? 0 : 1 + (i % 4)));
    return { kinds, portions, raws };
  },

  icons() {
    return SAUCE_SEASONINGS.flatMap(([, raws]) => raws);
  },

  slots(s) {
    const d = this.derive(s);
    const out = d.kinds.map((k, i) => {
      const n = d.raws[i];
      // raw used up but portions left: faded icon of what's in the bowl; empty bowl: nothing
      const item = k >= 0 ? SAUCE_SEASONINGS[k][1][0] : null;
      return { x: ssX(i), y: ssY(i), kind: "seasoning", index: i, item: item, count: n || 1, ghost: !n && k >= 0 };
    });
    return [...out, ...playerInventory(SS.INV_X, SS.INV_Y, SS.HOTBAR_Y, {
      0: { item: `${NS}:bamboo_chopsticks` }, 1: { item: "minecraft:wheat", count: 12 }, 2: { item: "minecraft:blaze_powder", count: 5 },
      3: { item: `${NS}:scallion`, count: 9 }, 4: { item: "minecraft:bowl", count: 8 }, 5: { item: "minecraft:apple", count: 3 },
    })];
  },

  background(g, s, w, h) {
    const d = this.derive(s);
    woodPanel(g, SS.W, SS.H);
    g.text("橡木蘸料台", 12, 7, TITLE, true);
    vanillaButton(g, SS.W - 26, 4, 12, 12, "?");
    ["tier_top", "tier_mid", "tier_low"].forEach((k, row) => g.text(g.tr(SAUCE_KEY(`screen.sauce.${k}`)), 8, SS.GRID_Y + row * SS.STEP_Y + 4, S_HINT, true));
    d.kinds.forEach((k, i) => {
      const x = ssX(i), y = ssY(i);
      slotFrame(g, x, y);
      const p = Math.min(SS.MAX, d.portions[i] + d.raws[i] * 4);
      const bx = x + 18;
      g.fill(bx, y - 1, bx + 5, y + 17, SLOT_SHADOW);
      g.fill(bx + 1, y, bx + 4, y + 16, SLOT_BG);
      const fh = Math.round((16 * p) / SS.MAX);
      if (fh && k >= 0) g.fill(bx + 1, y + 16 - fh, bx + 4, y + 16, SAUCE_SEASONINGS[k][2]);
    });
    inventoryFrames(g, SS.INV_X, SS.INV_Y, SS.HOTBAR_Y);
    g.text("物品栏", 8, SS.INV_Y - 11, LABEL, true);
    if (s.guide === "open") this.guide(g, s, SS.W + SG.GAP, 0);
  },

  // mryt233 改（r146f）：文字和图标顺序不变，换行不截断；页面高度按 Java 的 9 像素行距计算。
  guide(g, s, px, py) {
    const pages = [[]];
    let used = 0;
    SAUCE_SEASONINGS.forEach(([id, raws]) => {
      const name = g.tr(SAUCE_KEY(`seasoning.${id}`)), effect = g.tr(SAUCE_KEY(`seasoning.${id}.effect`));
      const inline = sgWidth(name) + 4 + sgWidth(effect) <= SG.TEXT_W;
      const names = sgWrap(name), effects = sgWrap(effect);
      const height = Math.max(18, (inline ? 1 : names.length + effects.length) * 9 + 4);
      if (pages.at(-1).length && used + height > SG.BOTTOM - SG.TOP) { pages.push([]); used = 0; }
      pages.at(-1).push({ raws, names, effects, inline, height });
      used += height;
    });
    s.guidePage = Math.max(0, Math.min(s.guidePage || 0, pages.length - 1));
    s.guidePageCount = pages.length;
    woodPanel(g, SG.W, SS.H, px, py);
    g.text(g.tr(SAUCE_KEY("screen.sauce.guide")), px + 12, py + 7, TITLE, true);
    let ry = py + SG.TOP;
    pages[s.guidePage].forEach((r) => {
      r.raws.slice(0, 2).forEach((id, k) => g.item(id, px + 7 + k * 17, ry));
      const tx = px + 7 + SG.ICONS_W + 4;
      r.names.forEach((line, i) => g.text(line, tx, ry + 2 + i * 9, LABEL, true));
      if (r.inline) g.text(r.effects[0], tx + sgWidth(r.names[0]) + 4, ry + 2, S_HINT, true);
      else r.effects.forEach((line, i) => g.text(line, tx, ry + 2 + (r.names.length + i) * 9, S_HINT, true));
      ry += r.height;
    });
    vanillaButton(g, px + 7, py + SS.H - 21, 16, 14, "<");
    vanillaButton(g, px + SG.W - 23, py + SS.H - 21, 16, 14, ">");
    // 边界页按钮禁用，同 Java Button.active。
    if (!s.guidePage) g.fill(px + 7, py + SS.H - 21, px + 23, py + SS.H - 7, 0x80000000);
    if (s.guidePage === pages.length - 1) g.fill(px + SG.W - 23, py + SS.H - 21, px + SG.W - 7, py + SS.H - 7, 0x80000000);
    const page = g.tr(SAUCE_KEY("screen.sauce.page"), s.guidePage + 1, pages.length);
    g.text(page, px + (SG.W - g.width(page)) / 2, py + SS.H - 18, LABEL, true);
  },

  click(s, mx, my) {
    if (mx >= SS.W - 26 && mx < SS.W - 14 && my >= 4 && my < 16) {
      s.guide = s.guide === "open" ? "closed" : "open";
      return true;
    }
    if (s.guide !== "open" || my < SS.H - 21 || my >= SS.H - 7) return false;
    const x = mx - SS.W - SG.GAP;
    if (x >= 7 && x < 23) s.guidePage = Math.max(0, (s.guidePage || 0) - 1);
    else if (x >= SG.W - 23 && x < SG.W - 7) s.guidePage = Math.min(s.guidePageCount - 1, (s.guidePage || 0) + 1);
    else return false;
    return true;
  },

  wheel(s, mx, my, dx, dy) {
    if (s.guide !== "open" || !dy || mx < SS.W + SG.GAP || mx >= SS.W + SG.GAP + SG.W || my < 0 || my >= SS.H) return false;
    s.guidePage = Math.max(0, Math.min(s.guidePageCount - 1, (s.guidePage || 0) + (dy > 0 ? 1 : -1)));
    return true;
  },

  slotOverlay(g, slot) {
    if (slot.ghost) g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xb32a190c);   // raw used up: faded icon of what's in the bowl
  },

  tooltip(g, slot, s) {
    const d = this.derive(s);
    if (slot.kind === "seasoning") {
      const k = d.kinds[slot.index];
      const left = Math.min(SS.MAX, d.portions[slot.index] + d.raws[slot.index] * 4);
      const grey = 0xffaaaaaa;
      const leftLine = { text: g.tr(SAUCE_KEY("screen.sauce.left"), left, SS.MAX), color: grey };
      if (k < 0) return [g.tr(SAUCE_KEY("screen.sauce.empty")), leftLine];
      const [id, , , rawNames] = SAUCE_SEASONINGS[k];
      return [g.tr(SAUCE_KEY(`seasoning.${id}`)), { text: g.tr(SAUCE_KEY("screen.sauce.slot"), rawNames), color: grey }, leftLine];
    }
    return null;
  },
};

// ===================================================================== sauce station overview (2026-10-23, mryt233)
// Web port of client/SauceOverviewScreen: right-clicking the station opens this read-only overview first — 3×3 cards matching
// the bowls (row 0 = top step), each with the raw item, name, portions left and a bar; "Restock" opens the restock screen above.
const SO = { W: 176, H: 176, CARD_X: 8, CARD_Y: 30, CARD_W: 52, CARD_H: 36, STEP_X: 54, STEP_Y: 38 };
const sauceOverview = {
  id: "sauce_overview",
  name: "蘸料台总览",
  blocks: sauceStation.blocks,
  open: "空手右键蘸料台先打开这里：九张卡片和台上的九个碗一一对应，指着卡片看调料和效果；点「补充蘸料台」进补货界面",
  size: [SO.W, SO.H],
  textures: [],
  params: sauceStation.params.filter((p) => p.key !== "guide" && p.key !== "raw"),
  icons() {
    return sauceStation.icons();
  },
  slots() {
    return [];
  },
  background(g, s) {
    const d = sauceStation.derive({ ...s, raw: "yes" });
    woodPanel(g, SO.W, SO.H);
    g.text("橡木蘸料台", 12, 7, TITLE, true);
    g.fill(12, 17, SO.W - 12, 18, BRASS_DARK);
    g.text(g.tr(SAUCE_KEY("screen.sauce.overview_hint")), 12, 20, S_HINT, true);
    d.kinds.forEach((k, i) => {
      const cx = SO.CARD_X + (i % 3) * SO.STEP_X, cy = SO.CARD_Y + Math.floor(i / 3) * SO.STEP_Y;
      const left = Math.min(SS.MAX, d.portions[i] + d.raws[i] * 4);
      g.fill(cx, cy, cx + SO.CARD_W, cy + SO.CARD_H, SLOT_LIGHT);
      g.fill(cx, cy, cx + SO.CARD_W - 1, cy + SO.CARD_H - 1, SLOT_SHADOW);
      g.fill(cx + 1, cy + 1, cx + SO.CARD_W - 1, cy + SO.CARD_H - 1, SLOT_BG);
      if (k >= 0 && left > 0) {
        const [id, raws, color] = SAUCE_SEASONINGS[k];
        g.item(raws[0], cx + 3, cy + 3);
        g.text(g.tr(SAUCE_KEY(`seasoning.${id}`)), cx + 21, cy + 4, TITLE, true);
        g.text(String(left), cx + 21, cy + 14, LABEL, true);
        const bw = SO.CARD_W - 8, fw = Math.round((bw * left) / SS.MAX);
        g.fill(cx + 4, cy + SO.CARD_H - 8, cx + 4 + bw, cy + SO.CARD_H - 4, SLOT_SHADOW);
        g.fill(cx + 4, cy + SO.CARD_H - 8, cx + 4 + fw, cy + SO.CARD_H - 4, color);
      } else {
        const t = g.tr(SAUCE_KEY("screen.sauce.empty_bowl"));
        g.text(t, cx + SO.CARD_W / 2 - g.width(t) / 2, cy + SO.CARD_H / 2 - 4, S_HINT, true);
      }
    });
    vanillaButton(g, SO.W / 2 - 50, SO.H - 26, 100, 20, g.tr(SAUCE_KEY("screen.sauce.restock")));
  },
};

// ===================================================================== hot pot guide book (2026-10-25, mryt233; redone 2026-10-26; blocks 2026-11-05)
// Web port of client/GuideBookScreen: an open red book (textures/gui/guide_book.png from tools/gen_guide_gui.py), category
// bookmarks down the left, entry bookmarks along the top (hover = stick out + name), one spread per entry.
// Content = assets/<ns>/guide/zh_cn.json (tools/gen_guide_book.py, generated from the web book tutorial's own sources).
// An entry is a list of blocks in source order (sub / p / note / th / tr / bul / num / icons / recipes / code / rule) that
// flow from the left page to the right page and on to the next spread, same as the Java. Layout numbers are shared with the
// Java (book 272×180, columns x 16 / 144, width 112, from y 12, height 144; icon row 18, recipe block 66); the bookmarks,
// the table-of-contents rows and the page arrows are clickable here too.
// Differences: no bold, recipes show the first item of a tag, no item tooltips.
const GB = { W: 272, H: 180, LEFT_X: 16, RIGHT_X: 144, COL_W: 112, COL_Y: 12, COL_H: 144, RECIPE_H: 66, BX: 28, BY: 24,
  CAT_W: 28, TAB_H: 24, ENTRY_W: 24, ENTRY_STEP: 26 };
const GB_COL = { text: 0xff2a1a0e, heading: 0xff5a2a10, muted: 0xff7a6040, tip: 0xff2e5e1e, warn: 0xff8a3a12,
  number: 0xff1e6e78, rule: 0x40603820, codeBg: 0x18603820 };
const GB_TEX = `${NS}:gui/guide_book`;
const GB_PROJECT = (new URLSearchParams(location.hash.slice(1)).get("p")) || "mod";
const GUIDE = await fetch(`/api/file?p=${encodeURIComponent(GB_PROJECT)}&f=src/main/resources/assets/${NS}/guide/zh_cn.json`)
  .then((r) => (r.ok ? r.json() : { categories: [] })).catch(() => ({ categories: [] }));
const GB_CATS = GUIDE.categories || [];
const GB_INDEX = await fetch(`/api/index?p=${encodeURIComponent(GB_PROJECT)}`).then((r) => r.json()).catch(() => ({ recipes: [], itemTags: {} }));

function gbItemName(g, id) {
  const [ns, path] = id.includes(":") ? id.split(":") : ["minecraft", id];
  for (const k of [`item.${ns}.${path}`, `block.${ns}.${path}`]) if (g.lang[k]) return g.lang[k];
  return path;
}
// result item → { cells: 9 item ids (first item of a tag), count, kind: "grid" | "stonecutter" | "furnace", input }
// (= GuideBookScreen.findRecipe / drawRecipe: crafting first, then stonecutting / smelting / smoking / campfire)
const GB_CRAFT = /crafting_(shaped|shapeless)|soup_bucket/;
function gbRecipe(id) {
  const pick = (ing) => {
    if (!ing) return null;
    if (Array.isArray(ing)) return pick(ing[0]);
    if (ing.item) return ing.item;
    if (ing.tag) return ((GB_INDEX.itemTags || {})[ing.tag] || [])[0] || null;
    return null;
  };
  const outOf = (x) => (x.json.result || {}).id || (x.json.result || {}).item;
  const r = GB_INDEX.recipes.find((x) => GB_CRAFT.test(x.json.type) && outOf(x) === id)
    || GB_INDEX.recipes.find((x) => /stonecutting|smelting|smoking|campfire_cooking/.test(x.json.type) && outOf(x) === id);
  if (!r) return { cells: Array(9).fill(null), count: 1, kind: "grid", input: null };
  const j = r.json;
  const kind = GB_CRAFT.test(j.type) ? "grid" : /stonecutting/.test(j.type) ? "stonecutter" : "furnace";
  const cells = Array(9).fill(null);
  if (kind === "grid") {
    if (j.pattern) j.pattern.forEach((row, y) => [...row].forEach((ch, x) => { if (ch !== " ") cells[y * 3 + x] = pick(j.key[ch]); }));
    else (j.ingredients || []).forEach((ing, i) => { if (i < 9) cells[i] = pick(ing); });
  }
  return { cells, count: (j.result && j.result.count) || 1, kind, input: pick(j.ingredient) };
}
// "<pot>#<soup>" (a pot holding that broth) → the pot item model with that soup surface, e.g. iron_hot_pot_tomato;
// "<id>#lit" → the "<id>_lit" item model (the lit stove) (= GuideBookScreen.stack)
function gbIcon(id) {
  if (id.endsWith("#lit")) return id.slice(0, -4) + "_lit";
  const m = /^(.*):(iron|copper)_nine_grid_hotpot#(\w+)$/.exec(id);
  return m ? `${m[1]}:${m[2]}_hot_pot_${m[3]}` : id;
}
function gbTitle(g, e) {
  return e.title || gbItemName(g, e.icon);
}
// = GuideBookScreen.layout: one header block + the entry's blocks in source order, poured into columns
const GB_ICON_STEP = 18, GB_ICON_PER_ROW = 6;
function gbWrap(g, text, w) {
  return g.wrap(text === null || text === undefined ? "" : String(text), w);
}
function gbLayout(g, e) {
  const flow = [];
  const title = gbWrap(g, gbTitle(g, e), GB.COL_W - 20);
  flow.push({ kind: "header", h: Math.max(24, title.length * 9 + 10), lines: title, icon: e.icon });
  const text = (s, color, after) => {
    if (!s) return;
    for (const t of gbWrap(g, s, GB.COL_W)) flow.push({ kind: "text", h: 9, lines: [t], color });
    flow.push({ kind: "gap", h: after });
  };
  const note = (head, body, kind) => {
    const color = kind === "warn" ? GB_COL.warn : kind === "dev" ? GB_COL.number : kind === "quote" ? GB_COL.muted : GB_COL.tip;
    const lines = [];
    if (head) lines.push(...gbWrap(g, head, GB.COL_W - 10));
    for (const t of body || []) if (t) lines.push(...gbWrap(g, t, GB.COL_W - 10));
    if (lines.length) flow.push({ kind: "note", h: lines.length * 9 + 6, lines, color });
  };
  // old guide_content.py entries still work
  if (!e.blocks) {
    if (e.get) text(e.get, GB_COL.muted, 3);
    if (e.text) text(e.text, GB_COL.text, 5);
    if (e.rows) {
      for (const [icon, s] of e.rows) {
        const lines = gbWrap(g, s, GB.COL_W - 15);
        flow.push({ kind: "row", h: lines.length === 1 ? 14 : lines.length * 9 + 5, lines, icon });
      }
      flow.push({ kind: "gap", h: 3 });
    }
    if (e.recipes) flow.push({ kind: "recipe", h: GB.RECIPE_H, recipes: e.recipes });
    if (e.steps) e.steps.forEach((s, i) => {
      const lines = gbWrap(g, s, GB.COL_W - 11);
      flow.push({ kind: "num", h: lines.length * 9 + 3, lines, n: String(i + 1), color: GB_COL.text, indent: 0 });
    });
    if (e.tip) note("", [e.tip], "tip");
  } else {
    for (const b of e.blocks) {
      if (b.k === "sub") {
        const lines = gbWrap(g, b.t, GB.COL_W - 4);
        flow.push({ kind: "sub", h: lines.length * 9 + 5, lines, color: GB_COL.heading });
      } else if (b.k === "p") text(b.t, GB_COL.text, 4);
      else if (b.k === "note") note(b.t, b.b, b.s || "tip");
      else if (b.k === "th") {
        const lines = gbWrap(g, b.t, GB.COL_W);
        flow.push({ kind: "th", h: lines.length * 9 + 4, lines, color: GB_COL.heading });
      } else if (b.k === "tr") {
        const lines = gbWrap(g, b.t, GB.COL_W - 15);
        flow.push({ kind: "row", h: lines.length <= 1 ? 14 : lines.length * 9 + 5, lines, icon: b.icon,
          color: b.go ? GB_COL.number : GB_COL.text, go: b.go });
      } else if (b.k === "bul" || b.k === "num") {
        const indent = Math.max(0, Math.min(3, b.d || 0)) * 4;
        const textX = b.icon ? (b.k === "bul" ? 15 : 22) : (b.k === "bul" ? 8 : 11);
        const lines = gbWrap(g, b.t, Math.max(40, GB.COL_W - textX - indent));
        flow.push({ kind: b.k, h: Math.max(b.icon ? 12 : 0, lines.length * 9 + 3), lines, n: b.k === "bul" ? "•" : String(b.n || 1),
          color: GB_COL.text, indent, icon: b.icon || null });
      } else if (b.k === "icons") {
        const ids = b.ids || [];
        if (ids.length) flow.push({ kind: "icons", h: Math.ceil(ids.length / GB_ICON_PER_ROW) * GB_ICON_STEP + 2, ids });
      } else if (b.k === "recipes") flow.push({ kind: "recipe", h: GB.RECIPE_H, recipes: b.ids });
      else if (b.k === "code") {
        const lines = [];
        for (const l of b.l) lines.push(...gbWrap(g, l, GB.COL_W - 6));
        flow.push({ kind: "code", h: lines.length * 9 + 6, lines, color: GB_COL.muted });
      } else if (b.k === "rule") flow.push({ kind: "rule", h: 7 });
    }
  }
  // a block taller than a page (long English paragraphs, notes) is cut by lines, like GuideBookScreen.splitBlock
  const splitBlock = (el) => {
    if (el.h <= GB.COL_H) return [el];
    if (el.kind === "icons") {
      const out = [], per = Math.max(1, Math.floor((GB.COL_H - 2) / GB_ICON_STEP) * GB_ICON_PER_ROW);
      for (let i = 0; i < el.ids.length; i += per) {
        const ids = el.ids.slice(i, i + per);
        out.push({ kind: "icons", h: Math.ceil(ids.length / GB_ICON_PER_ROW) * GB_ICON_STEP + 2, ids });
      }
      return out;
    }
    const pad = el.kind === "note" ? 6 : el.kind === "row" ? 5 : el.kind === "bul" || el.kind === "num" ? 3 : 0;
    const maxLines = Math.max(1, Math.floor((GB.COL_H - pad) / 9));
    if (el.lines.length <= maxLines) return [el];
    const out = [];
    for (let i = 0; i < el.lines.length; i += maxLines) {
      const lines = el.lines.slice(i, i + maxLines);
      const part = { ...el, lines, h: Math.max(pad + lines.length * 9, el.kind === "row" ? 9 : 0) };
      if (i > 0) { part.icon = null; part.n = ""; part.go = null; }
      out.push(part);
    }
    return out;
  };
  const cols = [];
  let cur = [], used = 0;
  for (const el of flow) {
    for (const part of splitBlock(el)) {
      if (used + part.h > GB.COL_H && cur.length) {
        cols.push(cur);
        cur = []; used = 0;
        if (part.kind === "gap") continue;
      }
      cur.push(part); used += part.h;
    }
  }
  cols.push(cur);
  return cols;
}
function gbState(s) {
  const cat = Math.max(0, Math.min(parseInt(s.cat, 10) || 0, GB_CATS.length - 1));
  const entries = GB_CATS[cat] ? GB_CATS[cat].entries : [];
  const entry = Math.max(0, Math.min(parseInt(s.entry, 10) || 0, entries.length - 1));
  return { cat, entry, entries, e: entries[entry] };
}
function gbIn(m, x, y, w, h) {
  return m && m[0] >= x && m[0] < x + w && m[1] >= y && m[1] < y + h;
}
// mryt233 改（2026-11-07）：固定步距、独立书签滚动与全文搜索，和游戏内书本保持一致。
function gbStrip(s) {
  const { cat, entry, entries } = gbState(s), searchX = GB.BX + GB.W - 6 - GB.ENTRY_W;
  const total = Math.max(0, entries.length * GB.ENTRY_STEP - 2), overflow = total > searchX - (GB.BX + 6) - 6;
  const left = GB.BX + (overflow ? 14 : 6), right = searchX - (overflow ? 10 : 6), max = Math.max(0, total - (right - left));
  const key = `${cat}:${entry}`;
  if (s._stripEntry !== key) {
    s._stripEntry = key;
    s._entryScroll = entry * GB.ENTRY_STEP + GB.ENTRY_W / 2 - (right - left) / 2;
  }
  s._entryScroll = Math.max(0, Math.min(max, s._entryScroll || 0));
  return { left, right, max, searchX, x: (i) => left + i * GB.ENTRY_STEP - Math.round(s._entryScroll) };
}
const GB_FIND = { x: GB.BX + 18, y: GB.BY + 16, w: 236, h: 148, row: 25, listH: 100 };
function gbFind(g, s) {
  const q = (s._searchQuery || "").trim().toLowerCase();
  if (s._findQuery === q) return;
  s._findQuery = q; s._hitScroll = 0; s._hits = [];
  if (!q) return;
  const parts = (v) => Array.isArray(v) ? v.flatMap(parts) : typeof v === "string" ? [v] : [];
  for (let ci = 0; ci < GB_CATS.length; ci++) for (let ei = 0; ei < GB_CATS[ci].entries.length; ei++) {
    const c = GB_CATS[ci], e = c.entries[ei], title = `${c.title} · ${gbTitle(g, e)}`;
    const text = [gbItemName(g, e.icon.split("#")[0])];
    for (const b of e.blocks || []) {
      text.push(...parts(b.t), ...parts(b.b), ...parts(b.l));
      if (b.icon) text.push(gbItemName(g, b.icon.split("#")[0]));
      for (const id of b.ids || []) text.push(gbItemName(g, id.split("#")[0]));
    }
    if (!e.blocks) {
      for (const k of ["get", "text", "steps", "tip"]) text.push(...parts(e[k]));
      for (const [id, label] of e.rows || []) text.push(gbItemName(g, id), label);
      for (const id of e.recipes || []) text.push(gbItemName(g, id));
    }
    const body = text.join(" "), at = body.toLowerCase().indexOf(q), titleHit = title.toLowerCase().includes(q);
    if (at < 0 && !titleHit) continue;
    const from = Math.max(0, at - 12);
    const cols = gbLayout(g, e), offsets = [];
    let laid = "";
    for (const col of cols) {
      offsets.push(laid.length);
      for (const el of col) {
        laid += (el.lines || []).join("");
        for (const id of [el.icon, ...(el.ids || []), ...(el.recipes || [])].filter(Boolean)) laid += " " + gbItemName(g, id.split("#")[0]);
        if (el.kind !== "text") laid += " ";
      }
    }
    const hit = laid.toLowerCase().indexOf(q);
    let spread = 0;
    offsets.forEach((offset, i) => { if (hit >= offset) spread = Math.floor(i / 2); });
    s._hits.push({ cat: ci, entry: ei, title, icon: e.icon, snippet: (from ? "…" : "") + body.slice(from), rank: titleHit ? 0 : 1, spread });
  }
  s._hits.sort((a, b) => a.rank - b.rank);
}
function gbSearchJump(s, hit) {
  s.cat = String(hit.cat); s.entry = String(hit.entry); s.spread = String(hit.spread); s._searchOpen = false;
}
function gbFit(g, text, width) {
  if (g.width(text) <= width) return text;
  const chars = [...text];
  while (chars.length && g.width(chars.join("") + "…") > width) chars.pop();
  return chars.join("") + "…";
}
function gbDrawSearch(g, s, mouse) {
  gbFind(g, s);
  const { x, y, w, h, row, listH } = GB_FIND;
  g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x602a1a0e);
  g.fill(x, y, x + w, y + h, GB_COL.text);
  g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xfff5ecd8);
  g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xffdfc9a3);
  g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xfff5ecd8);
  g.item("minecraft:compass", x + 7, y + 7);
  g.text("×", x + w - 16, y + 10, gbIn(mouse, x + w - 20, y + 5, 16, 18) ? GB_COL.warn : GB_COL.heading);
  g.fill(x + 6, y + 26, x + w - 6, y + 27, 0xffdfc9a3);
  g.enableScissor(x + 5, y + 31, x + w - 5, y + 31 + listH);
  for (let i = 0; i < s._hits.length; i++) {
    const ry = y + 31 + i * row - (s._hitScroll || 0), hit = s._hits[i];
    if (ry + row <= y + 31 || ry >= y + 31 + listH) continue;
    if (gbIn(mouse, x + 5, ry, w - 10, row)) g.fill(x + 5, ry, x + w - 5, ry + row, 0x40dfc9a3);
    g.item(gbIcon(hit.icon), x + 7, ry + 4);
    g.text(gbFit(g, hit.title, w - 38), x + 28, ry + 2, GB_COL.heading);
    g.text(gbFit(g, hit.snippet, w - 38), x + 28, ry + 13, GB_COL.muted);
  }
  if (!s._hits.length) g.text((s._searchQuery || "").trim() ? "没有找到相关条目" : "输入关键词查找物品、玩法或配方", x + 8, y + 36, GB_COL.muted);
  g.disableScissor();
  const max = Math.max(0, s._hits.length * row - listH);
  if (max) {
    const thumb = Math.max(8, Math.floor(listH * listH / (s._hits.length * row)));
    const top = y + 31 + Math.floor((s._hitScroll || 0) * (listH - thumb) / max);
    g.fill(x + w - 5, top, x + w - 4, top + thumb, GB_COL.muted);
  }
  g.text(`${s._hits.length} 条结果 · 回车打开首项`, x + 8, y + h - 13, GB_COL.muted);
}
const guideBook = {
  id: "hotpot_guide",
  name: "火锅指南（教程书）",
  blocks: [],
  open: "右键打开指南。顶部滚轮或两端箭头横向滑动条目，右上角指南针、Ctrl+F 或 / 搜索全文；正文区域滚轮翻页。设置从模组菜单配置按钮进入（Fabric 需 Mod Menu）。",
  size: [GB.BX + GB.W + 3, GB.BY + GB.H + 3],
  textures: [GB_TEX],
  params: [
    { key: "cat", label: "分类", options: GB_CATS.map((c, i) => [String(i), c.title]), default: "0" },
    { key: "entry", label: "条目", options: [...Array(Math.max(1, ...GB_CATS.map((c) => c.entries.length)))].map((_, i) => [String(i), String(i + 1)]), default: "0" },
    { key: "spread", label: "跨页", options: [["0", "1"], ["1", "2"], ["2", "3"]], default: "0" },
  ],
  icons() {
    const ids = new Set(["minecraft:compass"]);
    const rec = (r) => {
      ids.add(r);
      const d = gbRecipe(r);
      d.cells.forEach((x) => x && ids.add(x));
      if (d.input) ids.add(d.input);
    };
    for (const c of GB_CATS) {
      ids.add(gbIcon(c.icon));
      for (const e of c.entries) {
        ids.add(gbIcon(e.icon));
        (e.rows || []).forEach((r) => ids.add(r[0]));
        (e.recipes || []).forEach(rec);
        for (const b of e.blocks || []) {
          if (b.icon) ids.add(gbIcon(b.icon));
          (b.ids || []).forEach((x) => ids.add(gbIcon(x)));
          (b.recipes || []).forEach(rec);
        }
      }
    }
    return [...ids];
  },
  slots() { return []; },
  background() {},
  foreground(g, s, w, slots, time, mouse) {
    const { BX: bx, BY: by } = GB;
    const { cat, entry, entries, e } = gbState(s);
    if (!e) return;
    const cols = gbLayout(g, e);
    const spreads = Math.max(1, Math.ceil(cols.length / 2));
    const spread = Math.min(parseInt(s.spread, 10) || 0, spreads - 1);
    s._spreads = spreads;
    // category bookmarks: 9 categories still fit inside the 180-pixel book (step shrinks when there are many)
    const catStep = GB_CATS.length <= 1 ? GB.TAB_H
      : Math.min(GB.TAB_H, Math.max(9, Math.floor((GB.H - GB.TAB_H - 6) / (GB_CATS.length - 1))));
    const catH = GB.TAB_H;
    const catY = (i) => by + 6 + i * catStep;
    const strip = gbStrip(s), entX = strip.x;
    let hoverCat = -1, hoverEntry = -1;
    GB_CATS.forEach((c, i) => {
      const ty = catY(i);
      const hover = !s._searchOpen && gbIn(mouse, bx - GB.CAT_W, ty, GB.CAT_W, catH);
      if (hover) hoverCat = i;
      g.texture(GB_TEX, bx - 24 - (i === cat || hover ? 4 : 0), ty, 280, i === cat ? 24 : 0, GB.CAT_W, GB.TAB_H, 512, 256);
    });
    g.enableScissor(strip.left, by - 26, strip.right, by);
    entries.forEach((x, i) => {
      const tx = entX(i);
      const hover = !s._searchOpen && gbIn(mouse, strip.left, by - 26, strip.right - strip.left, 26)
        && gbIn(mouse, tx, by - 23, GB.ENTRY_W, 23);
      if (hover) hoverEntry = i;
      g.texture(GB_TEX, tx, by - 20 - (i === entry || hover ? 3 : 0), 312, i === entry ? 24 : 0, GB.ENTRY_W, GB.TAB_H, 512, 256);
    });
    g.disableScissor();
    const hoverSearch = gbIn(mouse, strip.searchX, by - 23, GB.ENTRY_W, 23);
    g.enableScissor(strip.searchX, by - 26, strip.searchX + GB.ENTRY_W, by);
    g.texture(GB_TEX, strip.searchX, by - 20 - (s._searchOpen || hoverSearch ? 3 : 0), 312, s._searchOpen ? 24 : 0, GB.ENTRY_W, GB.TAB_H, 512, 256);
    g.disableScissor();
    g.texture(GB_TEX, bx, by, 0, 0, GB.W + 3, GB.H + 3, 512, 256);
    GB_CATS.forEach((c, i) => g.item(gbIcon(c.icon), bx - 19 - (i === cat || i === hoverCat ? 4 : 0), catY(i) + 4));
    g.enableScissor(strip.left, by - 26, strip.right, by);
    entries.forEach((x, i) => g.item(gbIcon(x.icon), entX(i) + 4, by - 18 - (i === entry || i === hoverEntry ? 3 : 0)));
    g.disableScissor();
    g.enableScissor(strip.searchX, by - 26, strip.searchX + GB.ENTRY_W, by);
    g.item("minecraft:compass", strip.searchX + 4, by - 18 - (s._searchOpen || hoverSearch ? 3 : 0));
    g.disableScissor();
    if (strip.max > 0) {
      g.text("<", bx + 6, by - 16, GB_COL.heading);
      g.text(">", strip.right + 1, by - 16, GB_COL.heading);
    }
    const links = [];   // table-of-contents rows: [x, y, w, h, cat, entry], clicked in click()
    for (let k = 0; k < 2 && spread * 2 + k < cols.length; k++) {
      const x = bx + (k === 0 ? GB.LEFT_X : GB.RIGHT_X);
      let y = by + GB.COL_Y;
      for (const el of cols[spread * 2 + k]) {
        if (el.kind === "header") {
          g.item(gbIcon(el.icon), x, y);
          let ty = y + (el.lines.length === 1 ? 4 : 1);
          for (const t of el.lines) { g.text(t, x + 20, ty, GB_COL.heading); ty += 9; }
          g.fill(x, y + el.h - 4, x + GB.COL_W, y + el.h - 3, GB_COL.rule);
        } else if (el.kind === "text") g.text(el.lines[0], x, y, el.color);
        else if (el.kind === "sub") {
          let ty = y + 1;
          for (const t of el.lines) { g.text(t, x, ty, el.color); ty += 9; }
          g.fill(x, y + el.h - 3, x + GB.COL_W, y + el.h - 2, GB_COL.rule);
        } else if (el.kind === "note") {
          g.fill(x, y, x + 2, y + el.h - 2, el.color);
          let ty = y + 1;
          for (const t of el.lines) { g.text(t, x + 6, ty, el.color); ty += 9; }
        } else if (el.kind === "th") {
          let ty = y;
          for (const t of el.lines) { g.text(t, x, ty, el.color); ty += 9; }
          g.fill(x, y + el.h - 3, x + GB.COL_W, y + el.h - 2, GB_COL.rule);
        } else if (el.kind === "row") {
          const img = g.icons.get(gbIcon(el.icon || ""));
          if (img) g.g.drawImage(img, (g.ox + x) * g.s, (g.oy + y + 1) * g.s, 12 * g.s, 12 * g.s);
          let ty = y + (el.lines.length === 1 ? 3 : 2);
          for (const t of el.lines) { g.text(t, x + 15, ty, el.color || GB_COL.text); ty += 9; }
          if (el.go) links.push([x, y, GB.COL_W, el.h, el.go[0], el.go[1]]);
        } else if (el.kind === "bul" || el.kind === "num") {
          const off = el.indent || 0;
          g.text(el.n, x + off + 1, y, el.kind === "num" ? GB_COL.number : GB_COL.muted);
          let textX = el.kind === "num" ? 11 : 8;
          if (el.icon) {
            const img = g.icons.get(gbIcon(el.icon));
            if (img) g.g.drawImage(img, (g.ox + x + off + (el.kind === "num" ? 7 : 4)) * g.s, (g.oy + y) * g.s, 12 * g.s, 12 * g.s);
            textX = el.kind === "num" ? 22 : 15;
          }
          let ty = y;
          for (const t of el.lines) { g.text(t, x + off + textX, ty, el.color); ty += 9; }
        } else if (el.kind === "icons") {
          el.ids.forEach((id, i) => g.item(gbIcon(id), x + (i % GB_ICON_PER_ROW) * GB_ICON_STEP, y + 1 + Math.floor(i / GB_ICON_PER_ROW) * GB_ICON_STEP));
        } else if (el.kind === "code") {
          g.fill(x - 1, y, x + GB.COL_W, y + el.h - 3, GB_COL.codeBg);
          let ty = y + 2;
          for (const t of el.lines) { g.text(t, x + 2, ty, el.color); ty += 9; }
        } else if (el.kind === "rule") {
          g.fill(x, y + 3, x + GB.COL_W, y + 4, GB_COL.rule);
        } else if (el.kind === "recipe") {
          const idx = Math.floor(time / 2000) % el.recipes.length;
          const id = el.recipes[idx];
          const { cells, count, kind, input } = gbRecipe(id);
          const gx = x + 10, gy = y + 1;
          if (kind === "grid") {
            for (let i = 0; i < 9; i++) {
              g.texture(GB_TEX, gx + (i % 3) * 18, gy + Math.floor(i / 3) * 18, 340, 0, 18, 18, 512, 256);
              if (cells[i]) g.item(cells[i], gx + (i % 3) * 18 + 1, gy + Math.floor(i / 3) * 18 + 1);
            }
            g.texture(GB_TEX, gx + 58, gy + 23, 340, 18, 12, 8, 512, 256);
            g.texture(GB_TEX, gx + 74, gy + 18, 340, 0, 18, 18, 512, 256);
            g.item(id, gx + 75, gy + 19, count);
          } else {
            // stonecutter / furnace: one ingredient, arrow, one result; small label above (= GuideBookScreen.drawRecipe)
            const sx = gx + 12, sy = gy + 4;
            g.texture(GB_TEX, sx, sy, 340, 0, 18, 18, 512, 256);
            if (input) g.item(input, sx + 1, sy + 1);
            g.texture(GB_TEX, sx + 26, sy + 5, 340, 18, 12, 8, 512, 256);
            g.texture(GB_TEX, gx + 44, gy + 4, 340, 0, 18, 18, 512, 256);
            g.item(id, gx + 45, gy + 5, count);
            const how = kind === "stonecutter" ? g.tr("block.minecraft.stonecutter") : g.tr("block.minecraft.furnace");
            g.text(how, x + (GB.COL_W - g.width(how)) / 2, gy + 28, GB_COL.muted);
          }
          const name = gbItemName(g, id), cnt = el.recipes.length > 1 ? `  ${idx + 1}/${el.recipes.length}` : "";
          const lx = x + (GB.COL_W - g.width(name) - g.width(cnt)) / 2;
          g.text(name, lx, y + 57, GB_COL.number);
          if (cnt) g.text(cnt, lx + g.width(name), y + 57, GB_COL.muted);
        }
        y += el.h;
      }
    }
    s._links = links;
    const atStart = cat === 0 && entry === 0 && spread === 0;
    const atEnd = cat === GB_CATS.length - 1 && entry === entries.length - 1 && spread === spreads - 1;
    if (!atStart) g.texture(GB_TEX, bx + GB.LEFT_X, by + 159, gbIn(mouse, bx + GB.LEFT_X - 2, by + 158, 13, 11) ? 370 : 360, 0, 9, 9, 512, 256);
    if (!atEnd) g.texture(GB_TEX, bx + GB.W - GB.LEFT_X - 9, by + 159, gbIn(mouse, bx + GB.W - GB.LEFT_X - 11, by + 158, 13, 11) ? 370 : 360, 9, 9, 9, 512, 256);
    if (spreads > 1) { const p = `${spread + 1} / ${spreads}`; g.text(p, bx + GB.RIGHT_X + GB.COL_W / 2 - g.width(p) / 2, by + 160, GB_COL.muted); }
    if (s._searchOpen) gbDrawSearch(g, s, mouse);
    if (hoverSearch) g.tooltip(["搜索"], mouse[0], mouse[1], w, GB.BY + GB.H);
    else if (s._searchOpen) return;
    else if (hoverCat >= 0) g.tooltip([GB_CATS[hoverCat].title], mouse[0], mouse[1], w, GB.BY + GB.H);
    else if (hoverEntry >= 0) g.tooltip([gbTitle(g, entries[hoverEntry])], mouse[0], mouse[1], w, GB.BY + GB.H);
  },
  // clicks: bookmarks, table-of-contents rows and page arrows change the preview state (= GuideBookScreen.mouseClicked); true = redraw
  click(s, mx, my) {
    const { BX: bx, BY: by } = GB;
    const m = [mx, my];
    const strip = gbStrip(s);
    if (gbIn(m, strip.searchX, by - 23, GB.ENTRY_W, 23)) { s._searchOpen = !s._searchOpen; return true; }
    if (s._searchOpen) {
      const { x, y, w, row, listH } = GB_FIND;
      if (gbIn(m, x + w - 20, y + 5, 16, 18)) { s._searchOpen = false; return true; }
      if (gbIn(m, x + 5, y + 31, w - 10, listH)) {
        const hit = (s._hits || [])[Math.floor((my - y - 31 + (s._hitScroll || 0)) / row)];
        if (hit) { gbSearchJump(s, hit); return true; }
      }
      return false;
    }
    if (strip.max > 0) {
      if (gbIn(m, bx + 6, by - 26, 8, 26)) { s._entryScroll = Math.max(0, s._entryScroll - GB.ENTRY_STEP * 3); return true; }
      if (gbIn(m, strip.right, by - 26, 8, 26)) { s._entryScroll = Math.min(strip.max, s._entryScroll + GB.ENTRY_STEP * 3); return true; }
    }
    const catStep = GB_CATS.length <= 1 ? GB.TAB_H
      : Math.min(GB.TAB_H, Math.max(9, Math.floor((GB.H - GB.TAB_H - 6) / (GB_CATS.length - 1))));
    const catH = GB.TAB_H;
    for (let i = 0; i < GB_CATS.length; i++) if (gbIn(m, bx - GB.CAT_W, by + 6 + i * catStep, GB.CAT_W, catH)) { s.cat = String(i); s.entry = "0"; s.spread = "0"; return true; }
    const { cat, entry, entries } = gbState(s);
    for (let i = 0; i < entries.length; i++) if (gbIn(m, strip.left, by - 26, strip.right - strip.left, 26)
      && gbIn(m, strip.x(i), by - 23, GB.ENTRY_W, 23)) { s.entry = String(i); s.spread = "0"; return true; }
    for (const [x, y, w2, h, c2, e2] of s._links || []) if (gbIn(m, x, y, w2, h)) { s.cat = String(c2); s.entry = String(e2); s.spread = "0"; return true; }
    const spreads = s._spreads || 1;
    const spread = Math.min(parseInt(s.spread, 10) || 0, spreads - 1);
    const go = (c, en, sp) => { s.cat = String(c); s.entry = String(en); s.spread = String(sp); return true; };
    if (gbIn(m, bx + GB.LEFT_X - 2, by + 158, 13, 11)) {
      if (spread > 0) return go(cat, entry, spread - 1);
      if (entry > 0) return go(cat, entry - 1, 9);
      if (cat > 0) return go(cat - 1, GB_CATS[cat - 1].entries.length - 1, 9);
    }
    if (gbIn(m, bx + GB.W - GB.LEFT_X - 11, by + 158, 13, 11)) {
      if (spread < spreads - 1) return go(cat, entry, spread + 1);
      if (entry < entries.length - 1) return go(cat, entry + 1, 0);
      if (cat < GB_CATS.length - 1) return go(cat + 1, 0, 0);
    }
    return false;
  },
  wheel(s, mx, my, dx, dy) {
    const strip = gbStrip(s), { x, y, w, row, listH } = GB_FIND;
    if (s._searchOpen) {
      if (gbIn([mx, my], x + 5, y + 31, w - 10, listH)) s._hitScroll = Math.max(0,
        Math.min(Math.max(0, (s._hits || []).length * row - listH), (s._hitScroll || 0) + Math.sign(dy) * row));
      return true;
    }
    if (gbIn([mx, my], GB.BX, GB.BY - 26, GB.W, 28)) {
      s._entryScroll = Math.max(0, Math.min(strip.max, s._entryScroll + Math.sign(dy || dx) * GB.ENTRY_STEP * 2));
      return true;
    }
    if (dy) this.click(s, dy > 0 ? GB.BX + GB.W - GB.LEFT_X - 5 : GB.BX + GB.LEFT_X, GB.BY + 162);
    return true;
  },
  key(s, event) {
    if (event.isComposing) return false;
    if (((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "f") || (!s._searchOpen && event.key === "/")) {
      s._searchOpen = !s._searchOpen; return true;
    }
    if (s._searchOpen) {
      if (event.key === "Escape") { s._searchOpen = false; return true; }
      if (event.key === "Enter") { if ((s._hits || []).length) gbSearchJump(s, s._hits[0]); return true; }
      return false;
    }
    if (["ArrowLeft", "PageUp", "ArrowRight", "PageDown"].includes(event.key)) {
      this.click(s, ["ArrowRight", "PageDown"].includes(event.key) ? GB.BX + GB.W - GB.LEFT_X - 5 : GB.BX + GB.LEFT_X, GB.BY + 162);
      return true;
    }
    return false;
  },
  textInput(s) {
    if (!s._searchOpen) return null;
    return { x: GB_FIND.x + 29, y: GB_FIND.y + 11, w: GB_FIND.w - 54, value: s._searchQuery || "",
      label: "书内搜索", placeholder: "搜索：物品、玩法、配方……", change: (value) => { s._searchQuery = value; } };
  },
};


// ===================================================================== settings (r146a, mryt233)
// Web port of client/HotPotConfigScreen (r146a 重做版): two tabs — 常用 (particle slider + the few things
// players actually touch) and 高级 (everything else in collapsible groups, collapsed by default).
// 和 Java 一样的规矩：面板宽度随画布自适应、每行只放一项、名字换行不截断、超出滚轮 / 滚动条、
// [- 数值 +]（Shift 一次走 10 步）、点数值可直接键入（输错标红、不让保存）、↺ 只在当前值 ≠ 默认值时出现、
// 「恢复默认」点一次变确认态再点一次才生效。默认值 / 范围全部照 Config.rememberXxx 和 Config.rule() 抄。
// 网页预览不写任何配置文件：改过的数值只存在本界面的 preview state 里；键入沿用 canvas 的 key 事件（Java 是 EditBox）。
const CF = { W: 520, H: 390, TAB_W: 100, TAB_H: 20, TAB_GAP: 4, BTN: 16, VALUE_W: 108, RESET_W: 14,
  CTRL_GAP: 2, ROW_MIN_H: 30, HEADER_H: 20, BOTTOM_H: 20, SCROLL_W: 5, SCROLL_STEP: 24, LIST_TOP: 60 };
const CF_COL = { text: 0xffe8c88a, muted: 0xffa88c5c, hover: 0xff6a4418, stripe: 0x18ffffff,
  panel: 0xff2a190c, bad: 0xffff6a6a, confirm: 0xffff9a5a, valueBg: 0xff1a0e05, valueBgHover: 0xff3a2410,
  typedBg: 0xff3a2410 };
const CF_KEY = (s) => "text." + NS + ".config." + s;
const CF_UNIT = { tick: "config.unit.tick", multiplier: "config.unit.multiplier", point: "config.unit.point",
  level: "config.unit.level", "": "" };
// 悬停那句白话（= Config.fieldHelp 的补句；游戏里走 config.help.<键>，预览里先用这几句兜底）
const CF_HELP = {
  power: "灶火有多旺：越大，同一份菜涮熟得越快。1 = 和现在一样快，2 = 快一倍。",
  burn_ticks: "这种燃料能烧多久（20 tick = 1 秒）。原版燃料照原版自己的时长走，这里只对联动模组登记的燃料生效。",
  speed: "这种汤底把菜涮熟的速度：1 = 标准，1.3 = 快三成。",
  fallback_ticks: "没在配置里单独写时间的食材，按这个时长涮熟（20 tick = 1 秒）。",
  fallback_xp: "这类食材出锅时给的经验值。",
  xp_per_item: "有配方食材至少给的经验值。",
  tofu_cook_ticks: "整块冷豆腐放进锅里要煮多久才变热豆腐（20 tick = 1 秒）。",
  tofu_overcook_ticks: "豆腐从下锅到煮碎的总时间，必须比煮熟时间长（20 tick = 1 秒）。",
  cook_ticks: "这种食材要涮多久才熟（20 tick = 1 秒）。",
  hunger: "吃下去回多少饥饿点数；2 点 = 一个鸡腿图标。",
  saturation: "回多少饱和度系数（不是点数）；越大越顶饿，0.6 就够一顿。",
  duration: "效果持续多少 tick，0 = 不加这个效果（20 tick = 1 秒）。",
  amplifier: "效果等级：0 = I 级，1 = II 级，最多到 9（X 级）。",
  second_duration: "第二条效果（副效果）持续多少 tick，0 = 不加。",
  second_amplifier: "副效果的等级：0 = I 级。",
  fire_duration: "红汤「抗火」持续多少 tick（只有红汤用，特辣起生效）。",
  fire_amplifier: "红汤「抗火」的等级。",
  hunger_duration: "红汤负面「饥饿」持续多少 tick（只有红汤用，特辣起生效）。",
  hunger_amplifier: "红汤负面「饥饿」的等级。",
  sip_hunger: "饮料每口回多少饥饿点数。",
  sip_saturation: "饮料每口的饱和度系数。",
};
// 常用页：粒子强度（滑块）+ 这几项（= Java commonFields()）
// help = 悬停那句白话的键（= 游戏里 config.help.<key>，主会话补进语言文件与 Config.fieldHelp）
const CF_COMMON = [
  { path: ["fuel", "coal", "power"], name: "煤炭 · 灶火功率", def: 1.0, min: 0.1, max: 10, step: 0.05, unit: "multiplier", help: "power" },
  { path: ["cooking", "fallback_ticks"], name: "涮煮兜底时间", def: 200, min: 1, max: 100000, step: 10, unit: "tick", help: "fallback_ticks" },
  { path: ["soup_speed", "spicy"], name: "红汤锅底 · 加热倍率", def: 1.3, min: 0.1, max: 10, step: 0.05, unit: "multiplier", help: "speed" },
  { path: ["soup_speed", "clear"], name: "清汤锅底 · 加热倍率", def: 1.0, min: 0.1, max: 10, step: 0.05, unit: "multiplier", help: "speed" },
  { path: ["soup_speed", "rich_stock"], name: "高汤锅底 · 加热倍率", def: 0.85, min: 0.1, max: 10, step: 0.05, unit: "multiplier", help: "speed" },
];
// 生食材默认涮煮刻数（= HotPotCooking.DEFAULT_ITEM_COOK_TICKS）
const CF_ITEMS = [
  ["beef_loin_slice", 50],
  ["tripe", 50],
  ["enoki_mushroom", 50],
  ["greens", 50],
  ["fatty_beef_roll", 100],
  ["mutton_roll", 100],
  ["potato_slice", 100],
  ["cold_tofu_cube", 100],
  ["ham_sausage_slice", 100],
  ["peeled_shrimp", 100],
  ["lotus_root_slice", 100],
  ["squid_roll", 100],
  ["tofu_skin", 100],
  ["sweet_potato_skin", 100],
  ["shrimp", 150],
  ["shrimp_paste", 150],
  ["raw_ham_sausage", 150],
  ["dried_tofu", 150],
  ["wide_noodles", 150],
  ["raw_crispy_pork", 150],
  ["crispy_pork", 150],
  ["beef_loin", 200],
  ["beef_meatball", 200],
  ["pork_meatball", 200],
  ["mutton_meatball", 200],
  ["fish_ball", 200],
  ["cilantro_beef_meatball", 200],
  ["cilantro_pork_meatball", 200],
  ["cilantro_mutton_meatball", 200],
  ["cilantro_fish_ball", 200],
  ["zhe_er_gen_slice", 200],
];
// 食物营养（= HotPotContent 的 foodItem(id, 饥饿, 饱和系数)）
const CF_FOODS = [
  ["tripe", 2, 0.3],
  ["cooked_tripe", 5, 0.6],
  ["fatty_beef_roll", 1, 0.3],
  ["cooked_fatty_beef_roll", 3, 0.6],
  ["mutton_roll", 1, 0.3],
  ["cooked_mutton_roll", 3, 0.6],
  ["beef_loin", 3, 0.3],
  ["cooked_beef_loin", 8, 0.8],
  ["beef_loin_slice", 1, 0.3],
  ["cooked_beef_loin_slice", 3, 0.6],
  ["potato_slice", 1, 0.3],
  ["cooked_potato_slice", 2, 0.6],
  ["soybean", 1, 0.1],
  ["cold_tofu", 2, 0.4],
  ["hot_tofu", 2, 0.4],
  ["cold_tofu_cube", 1, 0.3],
  ["orange", 4, 0.3],
  ["garlic", 1, 0.1],
  ["garlic_paste", 1, 0.1],
  ["shrimp", 1, 0.2],
  ["cooked_shrimp", 3, 0.5],
  ["peeled_shrimp", 1, 0.2],
  ["cooked_peeled_shrimp", 3, 0.6],
  ["shrimp_paste", 1, 0.2],
  ["cooked_shrimp_paste", 4, 0.6],
  ["raw_ham_sausage", 1, 0.3],
  ["ham_sausage", 3, 0.6],
  ["ham_sausage_slice", 1, 0.3],
  ["cooked_ham_sausage_slice", 2, 0.5],
  ["oyster_sauce", 1, 0.2],
  ["sesame_paste", 2, 0.3],
  ["peanut", 1, 0.2],
  ["crushed_peanuts", 1, 0.2],
  ["millet_chili", 1, 0.1],
  ["raw_crispy_pork", 1, 0.3],
  ["crispy_pork", 4, 0.6],
  ["boiled_crispy_pork", 6, 0.8],
  ["dried_tofu", 2, 0.3],
  ["cooked_dried_tofu", 4, 0.6],
  ["tofu_skin", 2, 0.3],
  ["cooked_tofu_skin", 4, 0.6],
  ["wide_noodles", 1, 0.2],
  ["cooked_wide_noodles", 4, 0.6],
  ["sweet_potato_skin", 1, 0.2],
  ["cooked_sweet_potato_skin", 4, 0.6],
  ["enoki_mushroom", 1, 0.2],
  ["cooked_enoki_mushroom", 3, 0.5],
  ["greens", 1, 0.2],
  ["cooked_greens", 3, 0.5],
  ["lotus_root", 2, 0.3],
  ["lotus_root_slice", 1, 0.2],
  ["cooked_lotus_root_slice", 3, 0.6],
  ["raw_squid", 2, 0.2],
  ["squid_roll", 1, 0.2],
  ["cooked_squid_roll", 4, 0.6],
  ["egg_fried_rice", 7, 0.8],
  ["zhe_er_gen", 1, 0.15],
  ["zhe_er_gen_slice", 1, 0.15],
  ["cooked_zhe_er_gen_slice", 3, 0.5],
  ["minced_zhe_er_gen", 1, 0.1],
  ["tomato", 2, 0.3],
  ["chili_pepper", 1, 0.1],
  ["cooked_kelp", 2, 0.3],
  ["hot_tofu_cube", 2, 0.6],
  ["beef_meatball", 1, 0.3],
  ["pork_meatball", 1, 0.3],
  ["mutton_meatball", 1, 0.3],
  ["fish_ball", 1, 0.3],
  ["cilantro_beef_meatball", 1, 0.3],
  ["cilantro_pork_meatball", 1, 0.3],
  ["cilantro_mutton_meatball", 1, 0.3],
  ["cilantro_fish_ball", 1, 0.3],
  ["cooked_beef_meatball", 3, 0.6],
  ["cooked_pork_meatball", 3, 0.6],
  ["cooked_mutton_meatball", 3, 0.6],
  ["cooked_fish_ball", 3, 0.6],
  ["cooked_cilantro_beef_meatball", 4, 0.6],
  ["cooked_cilantro_pork_meatball", 4, 0.6],
  ["cooked_cilantro_mutton_meatball", 4, 0.6],
  ["cooked_cilantro_fish_ball", 4, 0.6],
  ["scallion", 1, 0.1],
  ["cilantro", 1, 0.1],
];
const CF_SOUPS = [["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["spicy", "红汤"],
  ["tomato", "番茄汤"], ["three_fresh", "三鲜汤"]];
const CF_SOUP_SPEED = { clear: 1.0, rich_stock: 0.85, mushroom: 1.15, spicy: 1.3, tomato: 1.1, three_fresh: 1.0 };
const CF_FUELS = [["wood", "柴火", 0.65, 300], ["coal", "煤炭", 1.0, 1600], ["lava", "岩浆", 1.6, 20000]];
const CF_SEASONINGS = [["msg", "味精", 0, 0, 2], ["chili_oil", "辣油", 200, 0, 0], ["sweet_sour_sauce", "酸甜酱", 200, 0, 0],
  ["honey", "蜂蜜", 0, 0, 0], ["sesame_oil", "香油", 100, 0, 0], ["vinegar", "醋", 0, 0, 0], ["sugar", "白糖", 200, 0, 0],
  ["chopped_scallion", "葱花", 0, 0, 0], ["minced_cilantro", "香菜碎末", 0, 0, 0], ["garlic_paste", "蒜泥", 160, 0, 0],
  ["oyster_sauce", "蚝油", 200, 0, 0], ["sesame_paste", "芝麻酱", 200, 0, 0], ["crushed_peanuts", "花生碎", 200, 0, 0],
  ["millet_chili", "小米辣", 160, 0, 0], ["minced_zhe_er_gen", "折耳根碎", 200, 0, 0]];
const CF_BOWL_KEYS = ["hunger", "saturation", "duration", "amplifier", "second_duration", "second_amplifier",
  "fire_duration", "fire_amplifier", "hunger_duration", "hunger_amplifier"];
// 汤碗默认值（= SoupBase 里 rememberSoupBowl 的参数，顺序同 CF_BOWL_KEYS）
const CF_BOWLS = [
  ["clear", [4, 0.6, 300, 0, 1, 0, 0, 0, 0, 0]],
  ["rich_stock", [4, 0.9, 300, 0, 600, 0, 0, 0, 0, 0]],
  ["mushroom", [4, 0.6, 200, 0, 0, 0, 0, 0, 0, 0]],
  ["spicy", [4, 0.6, 120, 0, 120, 0, 300, 0, 120, 0]],
  ["tomato", [4, 0.6, 300, 0, 0, 0, 0, 0, 0, 0]],
  ["three_fresh", [4, 0.9, 400, 0, 600, 0, 0, 0, 0, 0]],
];
const CF_DRINKS = [["orange_drink", "橙子饮料", 300, 0], ["plant_protein_drink", "植物蛋白饮料", 300, 0],
  ["orient_cola", "之晗可乐", 300, 0]];
// 高级页的分组（= Java sections()）
function cfGroupDefs() {
  const unitOf = (k2) => k2 === "saturation" ? "" : k2.endsWith("amplifier") ? "level" : k2 === "hunger" ? "point" : "tick";
  const rangeOf = (k2) => k2 === "saturation" ? [0, 10, 0.05] : k2.endsWith("amplifier") ? [0, 9, 1]
    : k2 === "hunger" ? [0, 100, 1] : [0, 100000, 20];
  return [
    { id: "cooking", title: "涮煮时间", fields: [
      { path: ["cooking", "fallback_ticks"], name: "涮煮兜底时间", def: 200, min: 1, max: 100000, step: 10, unit: "tick", help: "fallback_ticks" },
      { path: ["cooking", "fallback_xp"], name: "无配方出锅经验", def: 0.15, min: 0, max: 100, step: 0.05, unit: "point", help: "fallback_xp" },
      { path: ["cooking", "xp_per_item"], name: "有配方出锅经验下限", def: 0.7, min: 0, max: 100, step: 0.05, unit: "point", help: "xp_per_item" },
      { path: ["cooking", "tofu_cook_ticks"], name: "冷豆腐煮熟刻数", def: 200, min: 1, max: 100000, step: 10, unit: "tick", help: "tofu_cook_ticks" },
      { path: ["cooking", "tofu_overcook_ticks"], name: "豆腐煮过头刻数", def: 600, min: 1, max: 100000, step: 10, unit: "tick", help: "tofu_overcook_ticks" },
    ] },
    { id: "items", title: "食材涮煮时间", fields: CF_ITEMS.map(([id, ticks]) => ({
      path: ["cooking", "items", id], name: id + " · 涮煮刻数", def: ticks, min: 1, max: 100000, step: 20,
      unit: "tick", item: NS + ":" + id, help: "cook_ticks" })) },
    { id: "soup", title: "汤底加热倍率", fields: CF_SOUPS.map(([id, name]) => ({
      path: ["soup_speed", id], name: name + "锅底 · 加热倍率", def: CF_SOUP_SPEED[id], min: 0.1, max: 10,
      step: 0.05, unit: "multiplier", help: "speed" })) },
    { id: "fuel", title: "燃料档位", fields: CF_FUELS.flatMap(([id, name, power, burn]) => [
      { path: ["fuel", id, "power"], name: name + " · 灶火功率", def: power, min: 0.1, max: 10, step: 0.05, unit: "multiplier", help: "power" },
      { path: ["fuel", id, "burn_ticks"], name: name + " · 联动时长", def: burn, min: 0, max: 100000, step: 200, unit: "tick", help: "burn_ticks" },
    ]) },
    { id: "bowl", title: "汤碗", fields: CF_BOWLS.flatMap(([id, defaults]) => CF_BOWL_KEYS.map((key, i) => {
      const r = rangeOf(key);
      return { path: ["soup_bowl", id, key], name: CF_SOUPS.find((x) => x[0] === id)[1] + " · " + key,
        def: defaults[i], min: r[0], max: r[1], step: r[2], unit: unitOf(key), help: key };
    })) },
    { id: "seasoning", title: "蘸料", fields: CF_SEASONINGS.flatMap(([id, name, dur, amp, hunger]) => id === "msg"
      ? [{ path: ["seasoning", id, "hunger"], name: name + " · 额外饥饿", def: hunger, min: 0, max: 100, step: 1, unit: "point", help: "hunger" }]
      : [{ path: ["seasoning", id, "duration"], name: name + " · 持续", def: dur, min: 0, max: 100000, step: 20, unit: "tick", help: "duration" },
         { path: ["seasoning", id, "amplifier"], name: name + " · 等级", def: amp, min: 0, max: 9, step: 1, unit: "level", help: "amplifier" }]) },
    { id: "drink", title: "饮料", fields: [
      { path: ["drink", "sip", "hunger"], name: "每口饥饿点数", def: 2, min: 0, max: 100, step: 1, unit: "point", help: "sip_hunger" },
      { path: ["drink", "sip", "saturation"], name: "每口饱和系数", def: 0.3, min: 0, max: 10, step: 0.05, unit: "", help: "sip_saturation" },
    ].concat(CF_DRINKS.flatMap(([id, name, dur, amp]) => [
      { path: ["drink", id, "duration"], name: name + " · 效果时长", def: dur, min: 0, max: 100000, step: 20, unit: "tick", help: "duration" },
      { path: ["drink", id, "amplifier"], name: name + " · 效果等级", def: amp, min: 0, max: 9, step: 1, unit: "level", help: "amplifier" },
    ])) },
    { id: "food", title: "食物营养", fields: CF_FOODS.flatMap(([id, hunger, sat]) => [
      { path: ["food", id, "hunger"], name: id + " · 饥饿点数", def: hunger, min: 0, max: 100, step: 1, unit: "point", item: NS + ":" + id, help: "hunger" },
      { path: ["food", id, "saturation"], name: id + " · 饱和系数", def: sat, min: 0, max: 10, step: 0.05, unit: "", item: NS + ":" + id, help: "saturation" },
    ]) },
  ];
}
const cfFieldKey = (f) => f.path.join(".");
// Java formatValue：整数项不留小数，小数项去掉尾零
function cfFormat(f, v) {
  let s = (Math.round(v * 100) / 100).toFixed(f.step < 1 ? 2 : 0);
  if (s.indexOf(".") >= 0) s = s.replace(/0+$/, "").replace(/[.]$/, "");
  return s;
}
// Java parse()：不是数字 / 不是整数 / 超范围都不合法
function cfParse(f, text) {
  const t = String(text == null ? "" : text).trim();
  if (!t || !/^-?[0-9]+([.][0-9]+)?$/.test(t)) return null;
  const v = Number(t);
  if (!isFinite(v)) return null;
  if (f.step >= 1 && Math.abs(v - Math.round(v)) > 1e-6) return null;
  return v < f.min - 1e-6 || v > f.max + 1e-6 ? null : v;
}
function cfState(s) {
  if (!s.values) s.values = {};
  if (!s.bad) s.bad = {};
  if (!s.expanded) s.expanded = [];
  if (!s.tab) s.tab = "common";
  if (s.particles == null) s.particles = 100;
  return s;
}
function cfValue(s, f) {
  const key = cfFieldKey(f);
  return key in s.values ? s.values[key] : f.def;
}
// 单位字：语言文件里还没有这个键时不显示（网页先看得懂就行，游戏里是翻译好的）
function cfUnit(g, f) {
  const key = CF_UNIT[f.unit];
  if (!key) return "";
  const text = g.tr(CF_KEY(key));
  return text === CF_KEY(key) ? "" : text;
}
function cfValueText(g, s, f, width) {
  const v = cfValue(s, f);
  const num = cfFormat(f, v);
  const unit = cfUnit(g, f);
  const base = unit ? num + " " + unit : num;
  if (f.unit === "tick" && v >= 20) {
    const both = base + " (" + (v / 20).toFixed(1) + " 秒)";
    if (g.width(both) <= width - 6) return both;
  }
  return base;
}
// 一行的名字：和 Java 完全一致，只画名字（不追加配置键——追加会撑爆行宽、和 Java 也对不上）；
// 配置键放在悬停提示里，照样能回配置文件对位置。
function cfLabel(f) {
  return f.name;
}
// 数值框宽度：画布很窄时先压缩它（= Java valueWidth()）
function cfValueWidth(w) {
  const listW = w - 20 - CF.SCROLL_W - 4;
  const fixed = 2 * CF.BTN + 3 * CF.CTRL_GAP + CF.RESET_W;
  // = Java valueWidth()：名字区至少留 96 像素（2026-11-09 r146a 修）
  return Math.max(56, Math.min(CF.VALUE_W, listW - 10 - 96 - fixed));
}
function cfLabelWidth(w) {
  const controls = CF.BTN + CF.CTRL_GAP + cfValueWidth(w) + CF.CTRL_GAP + CF.BTN + CF.CTRL_GAP + CF.RESET_W;
  return Math.max(60, w - 20 - CF.SCROLL_W - 4 - 10 - controls);
}
function cfLayout(g, s, w) {
  const labelW = cfLabelWidth(w);
  const out = [];
  const push = (f) => {
    if (!s._fields) s._fields = {};
    s._fields[cfFieldKey(f)] = f;
    const name = cfLabel(f);
    const lines = g.width(name) <= labelW ? [name] : g.wrap(name, labelW);
    out.push({ kind: "row", f, lines, h: Math.max(CF.ROW_MIN_H, lines.length * 10 + 10) });
  };
  s._fields = {};
  if (s.tab === "common") CF_COMMON.forEach(push);
  else {
    if (!s._groups) s._groups = cfGroupDefs();
    s._groups.forEach((grp) => {
      out.push({ kind: "header", grp, h: CF.HEADER_H });
      if (s.expanded.indexOf(grp.id) >= 0) grp.fields.forEach(push);
    });
  }
  return out;
}
// 列表上边界跟着页签走（= Java applyTab() 里的 listTop）：
// 常用页上面有粒子滑块（48..68）+ 提示（70）+ 快捷档（68..80），列表从 88 起；高级页直接从 48 起。
// 2026-11-09 r146a 修：以前写死 CF.LIST_TOP=60，常用页第一行会压在快捷档上。
function cfListArea(s) {
  // 常用页：滑块 48..68 + 提示 70 + 快捷档 83..95，列表从 100 起（= Java listTop = contentTop + 52）
  const top = s && s.tab === "advanced" ? 48 : 100;
  // mryt233 改（2026-11-10）：保存失败回执持续显示，列表为回执让出位置。
  const bottom = CF.H - CF.BOTTOM_H - 16 - (s._noticeHeight || 0);
  return { top, bottom, view: Math.max(1, bottom - top) };
}
function cfIn(mouse, x, y, w, h) {
  return !!mouse && mouse[0] >= x && mouse[0] < x + w && mouse[1] >= y && mouse[1] < y + h;
}
function cfCentre(g, text, cx, y, color, shadow) {
  g.text(text, cx - g.width(text) / 2, y, color, shadow !== false);
}
// 超宽就等比缩小（= Java drawFitted）
function cfFit(g, text, cx, y, max, color) {
  if (g.width(text) <= max) { cfCentre(g, text, cx, y, color); return; }
  const k = Math.max(0.6, max / g.width(text));
  g.textScaled(text, cx - (g.width(text) * k) / 2, y + (1 - k) * 4, k, color, true);
}
function cfHeaderRow(g, s, el, lx, y, lw, mouse) {
  const open = s.expanded.indexOf(el.grp.id) >= 0;
  const hover = cfIn(mouse, lx, y, lw, el.h);
  g.fill(lx, y, lx + lw, y + el.h - 1, hover ? 0x30b47a1c : 0x204a2e12);
  g.fill(lx, y + el.h - 1, lx + lw, y + el.h, BRASS_DARK);
  g.text((open ? "\u25BE " : "\u25B8 ") + el.grp.title + "  (" + el.grp.fields.length + ")", lx + 6,
    y + (el.h - 8) / 2, TITLE, true);
  s._acts.push({ x: lx, y, w: lw, h: el.h, run: () => {
    if (open) s.expanded = s.expanded.filter((id) => id !== el.grp.id);
    else s.expanded = s.expanded.concat([el.grp.id]);
    s._tip = (open ? "收起 " : "展开 ") + el.grp.title;
  } });
}
function cfFieldRow(g, s, el, lx, y, lw, mouse) {
  const f = el.f, key = cfFieldKey(f);
  const area = cfListArea(s);
  const hover = cfIn(mouse, lx, y, lw, el.h);
  g.fill(lx, y, lx + lw, y + el.h - 1, hover ? 0x30ffffff : CF_COL.stripe);
  let labelX = lx + 6;
  const icon = f.item ? g.icons.get(f.item) : null;
  if (icon) {
    g.g.drawImage(icon, (g.ox + lx + 3) * g.s, (g.oy + y + (el.h - 16) / 2) * g.s, 16 * g.s, 16 * g.s);
    labelX = lx + 24;
  }
  let textY = y + Math.floor((el.h - el.lines.length * 10) / 2);
  for (const line of el.lines) { g.text(line, labelX, textY, CF_COL.text); textY += 10; }
  const vw = cfValueWidth(CF.W);
  const resetX = lx + lw - 6 - CF.RESET_W;
  const plusX = resetX - CF.CTRL_GAP - CF.BTN;
  const valueX = plusX - CF.CTRL_GAP - vw;
  const minusX = valueX - CF.CTRL_GAP - CF.BTN;
  const by = y + Math.floor((el.h - CF.BTN) / 2);
  const visible = (yy, hh) => yy >= area.top && yy + hh <= area.bottom;
  [[minusX, "-", -1], [plusX, "+", 1]].forEach(([bx, mark, dir]) => {
    const hot = cfIn(mouse, bx, by, CF.BTN, CF.BTN);
    g.fill(bx, by, bx + CF.BTN, by + CF.BTN, hot ? CF_COL.hover : CF_COL.panel);
    g.fill(bx, by, bx + CF.BTN, by + 1, BRASS);
    cfCentre(g, mark, bx + CF.BTN / 2, by + 4, hot ? TITLE : CF_COL.text);
    if (!visible(by, CF.BTN)) return;
    s._acts.push({ x: bx, y: by, w: CF.BTN, h: CF.BTN, run: () => {
      const step = f.step * (s.shift === "on" ? 10 : 1) * dir;
      s.values[key] = Math.round(Math.max(f.min, Math.min(f.max, cfValue(s, f) + step)) * 100) / 100;
      delete s.bad[key];
      s.editing = null;
      s._tip = f.name + "：" + cfFormat(f, s.values[key]) + (s.shift === "on" ? "（Shift 按着，一次 10 步）" : "");
    } });
  });
  const valueY = y + Math.floor((el.h - 16) / 2);
  const vHover = cfIn(mouse, valueX, valueY, vw, 16);
  const editing = s.editing === key;
  g.fill(valueX, valueY, valueX + vw, valueY + 16,
    editing ? CF_COL.typedBg : (vHover ? CF_COL.valueBgHover : CF_COL.valueBg));
  g.fill(valueX, valueY, valueX + vw, valueY + 1, vHover || editing ? BRASS_HI : BRASS_DARK);
  if (editing) {
    // 正在键入：把玩家敲的原文画在框里，不合法的标红（Java 里是 EditBox 的文字色）
    const typed = String(s.typed == null ? "" : s.typed);
    const ok = cfParse(f, typed) != null;
    cfCentre(g, typed + "|", valueX + vw / 2, valueY + 4, ok ? TITLE : CF_COL.bad);
  } else if (s.bad[key] != null) {
    cfCentre(g, s.bad[key], valueX + vw / 2, valueY + 4, CF_COL.bad);
  } else {
    cfCentre(g, cfValueText(g, s, f, vw), valueX + vw / 2, valueY + 4, RIVET);
  }
  if (visible(valueY, 16)) {
    s._acts.push({ x: valueX, y: valueY, w: vw, h: 16, run: () => {
      s.editing = key;
      s.typed = key in s.bad ? s.bad[key] : cfFormat(f, cfValue(s, f));
    } });
  }
  // ↺：只在当前值 ≠ 默认值（或输错了）时出现（= Java drawFieldControls）
  if (Math.abs(cfValue(s, f) - f.def) >= 5e-4 || s.bad[key] != null) {
    const hot = cfIn(mouse, resetX, by, CF.RESET_W, CF.BTN);
    g.fill(resetX, by, resetX + CF.RESET_W, by + CF.BTN, hot ? CF_COL.hover : CF_COL.panel);
    g.fill(resetX, by, resetX + CF.RESET_W, by + 1, BRASS);
    cfCentre(g, "\u21BA", resetX + CF.RESET_W / 2, by + 3, hot ? TITLE : CF_COL.muted);
    if (visible(by, CF.BTN)) {
      s._acts.push({ x: resetX, y: by, w: CF.RESET_W, h: CF.BTN, run: () => {
        s.values[key] = f.def;
        delete s.bad[key];
        s.editing = null;
        s._tip = "只恢复这一项：" + f.name + " = " + cfFormat(f, f.def);
      } });
    }
  }
  if (hover && !editing) {
    // 悬停说明 = 一句白话（管什么）+ 范围 / 默认值（= Java tooltipFor 的两行）
    const help = CF_HELP[f.help] || "";
    s._tips = [help, f.name + "｜范围 " + cfFormat(f, f.min) + "～" + cfFormat(f, f.max) + "，默认 " + cfFormat(f, f.def),
      "配置键：" + cfFieldKey(f)]
      .filter(Boolean);
  }
}

const configScreen = {
  id: "config",
  name: "设置界面",
  blocks: [],
  open: "主菜单 Mods → 选中本模组 → 「Config」按钮（Fabric 装 Mod Menu 后同样从这里进）。打开默认在「常用」页；"
    + "点上方「高级」切页，点分组标题展开 / 收起；每行 [- 数值 +]，把「按住 Shift」选项切到「按着」再点 ± 就是一次 10 步；"
    + "点数值框可以直接键入（输错标红、保存被拦，Esc 撤掉），行尾 ↺ 只在该项不是默认值时出现；"
    + "底部「恢复默认」点一次变确认态、再点一次才生效。滚轮可以滚列表。预览里改的值只存在这个界面里，不写配置文件。",
  size: [CF.W, CF.H],
  textures: [],
  params: [
    { key: "tab", label: "页签", options: [["common", "常用"], ["advanced", "高级"]], default: "common" },
    { key: "particles", label: "粒子强度", range: [0, 100, 5], default: 100 },
    { key: "saveResult", label: "保存回执演示", options: [["written", "已写盘"], ["memory", "仅内存"], ["failed", "失败"], ["backup", "坏文件备份"]], default: "written" },
    { key: "shift", label: "按住 Shift（± 一次 10 步）", options: [["off", "没按"], ["on", "按着"]], default: "off" },
  ],
  icons() {
    const ids = new Set();
    for (const [id] of CF_FOODS) ids.add(NS + ":" + id);
    for (const [id] of CF_ITEMS) ids.add(NS + ":" + id);
    return [...ids];
  },
  slots() { return []; },
  background(g, s, w, h) {
    cfState(s);
    s._acts = [];
    s._tips = null;
    s._saveMessages = {
      memory: g.tr(CF_KEY("msg.memory_only"), "Permission denied"),
      failed: g.tr(CF_KEY("msg.save_failed"), "Permission denied"),
      backup: g.tr(CF_KEY("msg.broken_backup"), "orients_hotpot_chronicles.json.broken-20261110-120000"),
    };
    const tab = s.tab === "advanced" ? "advanced" : "common";
    s.tab = tab;
    frame(g, w, h, 1, true);
    g.text(g.tr(CF_KEY("title")), 12, 7, TITLE, true);
    g.fill(12, 19, w - 12, 20, BRASS_DARK);
    // 页签：常用 / 高级
    const startX = Math.floor((w - (CF.TAB_W * 2 + CF.TAB_GAP)) / 2);
    [["common", "常用"], ["advanced", "高级"]].forEach(([id, label], i) => {
      const x = startX + i * (CF.TAB_W + CF.TAB_GAP), y = 24;
      const active = tab === id, hover = cfIn(s._mouse, x, y, CF.TAB_W, CF.TAB_H);
      g.fill(x, y, x + CF.TAB_W, y + CF.TAB_H, active ? CF_COL.hover : (hover ? 0xff4a2e12 : CF_COL.panel));
      g.fill(x, y, x + CF.TAB_W, y + 1, active ? BRASS_HI : BRASS);
      if (active) g.fill(x, y + CF.TAB_H - 2, x + CF.TAB_W, y + CF.TAB_H, BRASS_HI);
      const key = CF_KEY("tab." + id);
      cfFit(g, g.tr(key) === key ? label : g.tr(key), x + CF.TAB_W / 2, y + 6, CF.TAB_W - 8,
        active ? TITLE : CF_COL.text);
      s._acts.push({ x, y, w: CF.TAB_W, h: CF.TAB_H, run: () => { s.tab = id; s._scroll = 0; s.editing = null; } });
    });
    s._slider = null;
    if (tab === "common") {
      // 粒子强度滑块 + 一行提示 + 右侧三个快捷档（= Java drawParticleBlock）
      const sy = 48, hintY = sy + 22;
      g.fill(10, sy, w - 10, sy + 20, BRASS_DARK);
      g.fill(11, sy + 1, w - 11, sy + 19, CF_COL.panel);
      const knobW = 10;
      const ratio = Math.max(0, Math.min(1, Number(s.particles) / 100));
      const knobX = 11 + Math.round(ratio * (w - 22 - knobW));
      g.fill(knobX, sy + 2, knobX + knobW, sy + 18, BRASS);
      g.fill(knobX, sy + 2, knobX + knobW, sy + 3, BRASS_HI);
      const offKey = CF_KEY("particles.off");
      const offText = g.tr(offKey);
      cfCentre(g, Number(s.particles) <= 0
        ? (offText === offKey ? "火锅粒子效果：已关闭 (0%)" : offText)
        : "火锅粒子效果：" + s.particles + "%", w / 2, sy + 6, TITLE);
      s._slider = { x: 11, w: w - 22, y: sy };
      const hintKey = CF_KEY("common.hint");
      const hintText = g.tr(hintKey);
      // 提示独占一行、给满宽度；快捷档挪到下面一行右对齐（= Java drawParticleBlock，2026-11-09 r146a 修）
      g.text(hintText === hintKey ? "控制灶口火苗、火星与汤面热气浓淡（仅本机生效）" : hintText,
        12, hintY, CF_COL.muted, true);
      const qw = Math.max(36, Math.min(64, Math.floor((w - 24) / 6)));
      const percents = [0, 50, 100];
      const quickTotal = percents.length * qw + (percents.length - 1) * 4;
      const x0 = w - 10 - quickTotal;
      percents.forEach((percent, i) => {
        const x = x0 + i * (qw + 4), y = hintY + 13;
        const chosen = Number(s.particles) === percent;
        const hover = cfIn(s._mouse, x, y, qw, 12);
        g.fill(x, y, x + qw, y + 12, chosen ? CF_COL.hover : (hover ? 0xff4a2e12 : CF_COL.panel));
        g.fill(x, y, x + qw, y + 1, BRASS);
        cfCentre(g, percent + "%", x + qw / 2, y + 2, chosen ? TITLE : CF_COL.text);
        s._acts.push({ x, y, w: qw, h: 12, run: () => {
          s.particles = percent;
          s._tip = "粒子强度 " + percent + "%（仅本机生效）";
        } });
        if (hover) s._tip = "点一下直接切到 " + percent + "%（＝ Java 里的快捷档）";
      });
    }
    const notice = s._saveNotice ? g.wrap(s._saveNotice, w - 24) : [];
    s._noticeHeight = notice.length * 10;
    // 列表（= Java drawList）
    const area = cfListArea(s);
    const elements = cfLayout(g, s, w);
    const total = elements.reduce((n, e) => n + e.h, 4);
    s._scroll = Math.max(0, Math.min(s._scroll || 0, total - area.view));
    const lx = 10, lw = w - 20 - CF.SCROLL_W - 4;
    g.enableScissor(lx, area.top, lx + lw + CF.SCROLL_W + 4, area.bottom);
    let y = area.top - (s._scroll || 0);
    for (const el of elements) {
      if (y + el.h > area.top && y < area.bottom) {
        if (el.kind === "header") cfHeaderRow(g, s, el, lx, y, lw, s._mouse);
        else cfFieldRow(g, s, el, lx, y, lw, s._mouse);
      }
      y += el.h;
      if (y > area.bottom) break;
    }
    g.disableScissor();
    if (total > area.view) {
      const thumbH = Math.max(12, Math.floor((area.view * area.view) / Math.max(1, total)));
      const maxScroll = Math.max(1, total - area.view);
      const thumbY = area.top + Math.floor(((area.view - thumbH) * (s._scroll || 0)) / maxScroll);
      g.fill(lx + lw + 3, area.top, lx + lw + 3 + CF.SCROLL_W, area.top + area.view, 0x40ffffff);
      g.fill(lx + lw + 3, thumbY, lx + lw + 3 + CF.SCROLL_W, thumbY + thumbH, BRASS);
    }
    notice.forEach((line, i) => g.text(line, 12, area.bottom + 3 + i * 10, CF_COL.bad, true));
    // 底部三个按钮（= Java drawBottomBar；「恢复默认」点一次变确认态）
    const gap = 6, btnW = Math.max(70, Math.min(150, Math.floor((w - 24 - gap * 2) / 3)));
    const bx0 = Math.floor((w - (btnW * 3 + gap * 2)) / 2), by = h - CF.BOTTOM_H - 8;
    s._resetAct = null;
    [["save", "保存并生效"], ["reset", "恢复默认"], ["cancel", "取消"]].forEach(([id, label], i) => {
      const x = bx0 + i * (btnW + gap);
      const confirming = i === 1 && s._confirm === true;
      const hover = cfIn(s._mouse, x, by, btnW, CF.BOTTOM_H);
      g.fill(x, by, x + btnW, by + CF.BOTTOM_H, confirming ? 0x60b47a1c : (hover ? 0xff4a2e12 : CF_COL.panel));
      g.fill(x, by, x + btnW, by + 1, confirming ? CF_COL.confirm : BRASS);
      const key = CF_KEY(id);
      const text = confirming ? "再点一次确认" : (g.tr(key) === key ? label : g.tr(key));
      cfFit(g, text, x + btnW / 2, by + 6, btnW - 6, confirming ? CF_COL.confirm : CF_COL.text);
      const act = { x, y: by, w: btnW, h: CF.BOTTOM_H, run: () => {
        if (i === 0) {
          if (Object.keys(s.bad).length) {
            s._tip = "有 " + Object.keys(s.bad).length + " 项输错了（标红），改对之前不让保存";
            return;
          }
          s._saveNotice = s._saveMessages[s.saveResult] || null;
          if (!s._saveNotice) s._tip = "已保存并生效（预览演示）";
        } else if (i === 1) {
          if (s._confirm === true) {
            s._confirm = false;
            s.values = {};
            s.bad = {};
            s.editing = null;
            s.particles = 100;
            s._tip = "已恢复全部默认值";
          } else {
            s._confirm = true;
            s._tip = "再点一次「恢复默认」才会真的恢复";
          }
        } else {
          s.values = {};
          s.bad = {};
          s.editing = null;
          s._confirm = false;
          s._tip = "取消：丢掉这次的改动，粒子强度退回进界面前的值";
        }
      } };
      if (i === 1) s._resetAct = act;
      s._acts.push(act);
    });
  },
  foreground(g, s, w, h, slots, time, mouse) {
    if (mouse) s._mouse = [mouse[0], mouse[1]];
    const lines = s._tips || (s._tip ? [s._tip] : null);
    if (lines && mouse) g.tooltip(lines, mouse[0], mouse[1], w, h);
    s._tip = null;
    s._tips = null;
  },
  // 点：先判滑块，再走这一帧画出来的命中表（Java 是同一套自绘命中表）
  click(s, mx, my) {
    cfState(s);
    if (s._slider && my >= s._slider.y && my <= s._slider.y + 20
      && mx >= s._slider.x && mx <= s._slider.x + s._slider.w) {
      s.particles = Math.max(0, Math.min(100, Math.round(((mx - s._slider.x) / s._slider.w) * 100)));
      s._tip = "粒子强度 " + s.particles + "%（仅本机生效）";
      return true;
    }
    for (const act of s._acts || []) {
      if (!cfIn([mx, my], act.x, act.y, act.w, act.h)) continue;
      if (s.editing) {
        // 点别处：先把这一项的输入结算掉（合法写进副本；不合法标红留着）
        const f = s._fields ? s._fields[s.editing] : null;
        if (f) {
          const value = cfParse(f, s.typed);
          if (value == null) s.bad[s.editing] = String(s.typed == null ? "" : s.typed);
          else { s.values[s.editing] = value; delete s.bad[s.editing]; }
        }
        s.editing = null;
      }
      if (s._confirm && act !== s._resetAct) s._confirm = false;
      act.run();
      return true;
    }
    return false;
  },
  wheel(s, mx, my, dx, dy) {
    const area = cfListArea(s);
    if (my < area.top || my > area.bottom) return false;
    s._scroll = Math.max(0, (s._scroll || 0) + Math.sign(dy || dx) * CF.SCROLL_STEP);
    return true;
  },
  // 键入：数字、小数点、退格、回车确认、Esc 撤销（Java 走 EditBox，这里走 canvas 的 key 事件）
  key(s, event) {
    if (!s.editing) return false;
    const f = s._fields ? s._fields[s.editing] : null;
    if (!f) { s.editing = null; return true; }
    const typed = String(s.typed == null ? "" : s.typed);
    if (event.key === "Enter") {
      const value = cfParse(f, typed);
      if (value == null) s.bad[s.editing] = typed;
      else { s.values[s.editing] = value; delete s.bad[s.editing]; }
      s.editing = null;
      s._tip = value == null ? "这一项输错了（要 " + cfFormat(f, f.min) + "～" + cfFormat(f, f.max) + "），改对之前不让保存"
        : f.name + "：" + cfFormat(f, value);
      return true;
    }
    if (event.key === "Escape") {
      delete s.bad[s.editing];
      s.editing = null;
      s._tip = "撤销这一项的输入";
      return true;
    }
    if (event.key === "Backspace") {
      s.typed = typed.slice(0, -1);
      return true;
    }
    if (/^[0-9.]$/.test(event.key) && !event.ctrlKey && !event.metaKey) {
      s.typed = typed + event.key;
      return true;
    }
    return true;   // 编辑中吞掉其余按键
  },
};

export default [hotpot, stove, sauceOverview, sauceStation, guideBook, configScreen];