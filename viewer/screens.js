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
  0: { item: `${NS}:chopsticks` },
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
  blocks: ["stove"],
  open: "右键灶台：没装锅时点哪里都行；装了锅时点砖砌的灶身（或手里拿着跟锅无关的东西点锅）",
  size: [ST.W, ST.H],
  textures: ["minecraft:block/bricks"],
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
    this.hearth(g, d, t);
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

  // brick hearth with an arched mouth; flames flicker with layered sines, sparks rise and slow down
  hearth(g, d, t) {
    const { HX, HY, HW, HH } = ST;
    for (let ty = 0; ty < HH; ty += 16) for (let tx = 0; tx < HW; tx += 16) {
      g.texture("minecraft:block/bricks", HX + tx, HY + ty, 0, 0, Math.min(16, HW - tx), Math.min(16, HH - ty), 16, 16);
    }
    g.fill(HX - 1, HY - 1, HX + HW + 1, HY, RIM_LINE);
    g.fill(HX - 1, HY + HH, HX + HW + 1, HY + HH + 1, RIM_LINE);
    g.fill(HX - 1, HY, HX, HY + HH, RIM_LINE);
    g.fill(HX + HW, HY, HX + HW + 1, HY + HH, RIM_LINE);
    const ax = HX + 8, ay = HY + 9, aw = HW - 16, ah = HH - 12;
    const bottom = ay + ah;
    const archTop = (x) => (x >= ax + 6 && x < ax + aw - 6 ? ay : x >= ax + 3 && x < ax + aw - 3 ? ay + 3 : ay + 8);
    const MOUTH = 0xff0a0503;
    g.fill(ax + 6, ay, ax + aw - 6, bottom, MOUTH);
    g.fill(ax + 3, ay + 3, ax + aw - 3, bottom, MOUTH);
    g.fill(ax, ay + 8, ax + aw, bottom, MOUTH);

    const burning = d.burnTicks > 0;
    if (!burning) {
      // cold: grey ash bed; a faint ember breathes when fuel is waiting
      g.fill(ax + 3, bottom - 3, ax + aw - 3, bottom, 0xff2e2a26);
      for (let k = 0; k < 6; k++) g.fill(ax + 5 + k * 5, bottom - 3 + (k % 2), ax + 7 + k * 5, bottom - 2 + (k % 2), 0xff4a443d);
      if (d.hasFuelInSlot) {
        const a = 0.25 + 0.35 * easeInOutSine(frac(t / 2.4) < 0.5 ? frac(t / 2.4) * 2 : 2 - frac(t / 2.4) * 2);
        g.fill(ax + 14, bottom - 3, ax + 22, bottom - 1, alpha(0xffb8361c, a));
      }
      return;
    }
    const heat = 0.3 + 0.7 * easeOutQuad(d.burnRatio);
    // warm light spilling onto the bricks
    g.fill(HX, HY, HX + HW, HY + HH, alpha(0xffff8a2a, 0.07 + 0.05 * Math.sin(t * 2.3)));
    g.fill(ax, ay + 8, ax + aw, bottom, alpha(0xff5a1606, 0.9));
    g.fill(ax + 3, ay + 3, ax + aw - 3, bottom, alpha(0xff5a1606, 0.5));
    // ember bed
    const base = bottom - 3;
    for (let k = 0; k < 10; k++) {
      const x = ax + 2 + Math.floor((k * (aw - 4)) / 10);
      const glow = 0.5 + 0.5 * Math.sin(t * 2.1 + k * 1.3);
      g.fill(x, base, x + 3, bottom, mix(0xffb8361c, 0xfff0a23c, glow));
      if (k % 3 === 1) g.fill(x + 1, base + 1, x + 2, base + 2, mix(0xfff0a23c, 0xfffbe08a, glow));
    }
    // flame tongues: three nested teardrops per tongue, each row narrower toward a swaying tip
    const maxH = (base - ay - 2) * heat;
    const layers = [[1, 3.2, 0xffd8401f], [0.7, 2.1, 0xfff08a2c], [0.42, 1.1, 0xfffbe08a]];
    for (const [hMul, wMax, color] of layers) {
      for (let j = 0; j < 5; j++) {
        const cx = ax + 6 + j * 6;
        const n = 0.5 + 0.5 * (0.55 * Math.sin(t * 6.3 + j * 1.9) + 0.3 * Math.sin(t * 11.7 + j * 0.7) + 0.15 * Math.sin(t * 23.1 + j * 2.3));
        const edge = j === 0 || j === 4 ? 0.62 : j === 2 ? 1 : 0.85;
        const hj = Math.round(maxH * (0.55 + 0.45 * n) * edge * hMul);
        const sway = Math.sin(t * 4.7 + j * 2.1);
        for (let yy = 0; yy < hj; yy++) {
          const f = yy / hj;
          const half = wMax * (1 - Math.pow(f, 1.7));
          const xc = cx + sway * 1.6 * f * f;
          const y = base - yy - 1;
          const x0 = Math.round(xc - half), x1 = Math.round(xc + half) + 1;
          if (y <= archTop(x0) || x1 <= x0) continue;
          g.fill(Math.max(ax, x0), y, Math.min(ax + aw, x1), y + 1, color);
        }
      }
    }
    // sparks: ease-out rise, fading
    for (let i = 0; i < 6; i++) {
      const life = (0.9 + hash(i, 7) * 0.7) / (0.6 + 0.6 * d.burnRatio);
      const cyc = Math.floor(t / life + hash(i, 3));
      const u = frac(t / life + hash(i, 3));
      const x0 = ax + 6 + Math.floor(hash(i, cyc) * (aw - 12));
      const y = Math.round(base - 2 - (base - ay - 4) * easeOutCubic(u));
      const x = x0 + Math.round(Math.sin(u * 5 + i) * 2);
      if (y < archTop(x) + 1) continue;
      g.fill(x, y, x + 1, y + 1, alpha(0xfffbe08a, Math.pow(1 - u, 1.5)));
    }
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
  clear: { base: 0xffb9a878, hi: 0xfffff6de, lo: 0xffd6c79a, bits: [0xff6faf3a, 0xff8fcf52, 0xffd04a2a] },
  milky: { base: 0xffe3d7bf, hi: 0xffffffff, lo: 0xfff2e7d2, bits: [0xfffff8ea, 0xff6faf3a, 0xffd9c9a8] },
  mushroom: { base: 0xff7c5836, hi: 0xffe8c88a, lo: 0xff9a7048, bits: [0xff4a3220, 0xffc9a16a, 0xff5e4026] },
  spicy: { base: 0xffb32a1e, hi: 0xffffb070, lo: 0xffd94a2e, bits: [0xffe8862a, 0xff7a1408, 0xff3a1208] },
};
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
  name: "九宫格火锅界面",
  blocks: ["copper_hot_pot", "iron_hot_pot", "stove"],
  open: "手拿筷子右键锅（放在地上的锅、或坐在灶台上的锅都可以）",
  size: [HP.W, HP.H],
  params: [
    { key: "pot", label: "锅", options: [["copper", "铜锅"], ["iron", "铁锅"]], default: "copper" },
    { key: "soup", label: "汤底", options: [["spicy", "红汤"], ["clear", "清汤"], ["milky", "浓汤"], ["mushroom", "菌汤"], ["", "空锅"]], default: "spicy" },
    { key: "fire", label: "灶火", options: [["on", "在烧"], ["off", "熄了"], ["none", "没灶台"]], default: "on" },
    { key: "ratio", label: "剩余燃烧", range: [0.05, 1, 0.05], default: 0.6 },
    { key: "food", label: "锅里", options: [["cooking", "正在煮"], ["raw", "刚下锅"], ["cooked", "全熟了"], ["empty", "空的"]], default: "cooking" },
  ],

  derive(s) {
    const burnTotal = s.fire === "none" ? 0 : 1600;
    const burnTicks = s.fire === "on" ? Math.round(burnTotal * s.ratio) : 0;
    const hasSoup = !!s.soup;
    const ratio = burnTotal > 0 ? Math.min(1, burnTicks / burnTotal) : 0;
    return { hasSoup, soup: s.soup, burnTotal, burnTicks, heat: hasSoup && burnTicks > 0 ? 0.45 + 0.55 * ratio : 0 };
  },

  slots(s) {
    const preset = PRESETS[s.food] || {};
    const pot = [];
    for (let i = 0; i < 81; i++) {
      const row = Math.floor(i / 9), col = i % 9;
      const p = preset[i];
      const cooked = !!(p && p[2] && s.soup);
      pot.push({
        // slot.x/y is the item position; the 18px cell starts one pixel up-left
        x: HP.GRID_X + col * 18 + Math.floor(col / 3) * HP.GAP + 1,
        y: HP.GRID_Y + row * 18 + Math.floor(row / 3) * HP.GAP + 1,
        kind: "pot", index: i,
        item: p ? (cooked ? COOKED[p[0]] || p[0] : p[0]) : undefined,
        doneness: p && s.soup ? p[1] || 0 : 0,
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
    this.soup(g, d, gx, gy, t);
    this.dividers(g, gx, gy, METALS[s.pot] || METALS.copper);
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

  // top-down soup: convection carries garnish in slow orbits, sheen patches breathe,
  // bubbles swell in place (ease-out-back) then pop into a fading ring (ease-out-cubic)
  soup(g, d, gx, gy, t) {
    const soup = SOUPS[d.soup];
    const base = soup ? soup.base : 0xff1b1c20;
    g.fill(gx, gy, gx + GW, gy + GW, base);
    // depth: darker toward the pot wall
    g.fill(gx, gy, gx + GW, gy + 4, 0x30000000);
    g.fill(gx, gy + GW - 4, gx + GW, gy + GW, 0x22000000);
    g.fill(gx, gy, gx + 4, gy + GW, 0x26000000);
    g.fill(gx + GW - 4, gy, gx + GW, gy + GW, 0x1c000000);
    if (!d.hasSoup) return;
    const cx = gx + GW / 2, cy = gy + GW / 2;

    // sheen: soft light patches fading in and out
    for (let i = 0; i < 5; i++) {
      const life = 1.8 + hash(i, 11) * 1.2;
      const cyc = Math.floor(t / life + hash(i, 5));
      const u = frac(t / life + hash(i, 5));
      const a = easeInOutSine(u < 0.5 ? u * 2 : 2 - u * 2);
      const x = gx + 8 + Math.floor(hash(i, cyc * 3) * (GW - 30));
      const y = gy + 8 + Math.floor(hash(i, cyc * 3 + 1) * (GW - 16));
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
    g.text(g.tr(key("screen.title")), 28, 6, TITLE, true);

    const x = HP.PANEL_X + 6, right = w - 12;
    // soup + temperature
    const soupColor = d.hasSoup ? SOUPS[d.soup].base : 0xff1b1c20;
    g.fill(x, 26, x + 9, 35, RIM_LINE);
    g.fill(x + 1, 27, x + 8, 34, soupColor);
    const name = d.hasSoup ? { text: g.tr(key(`soup.${d.soup}`)), color: F.GOLD } : { text: g.tr(key("soup.none")), color: F.GRAY };
    g.text(name, x + 13, 27, F.WHITE, true);
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
    const end = g.text(g.tr(key("screen.filled"), `${filled}/81`), x, 55, 0xffe8c88a, true);
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
    if (!d.hasSoup) status = { text: g.tr(key("ui.no_soup_yet")), color: F.GRAY };
    else if (slot.cooked) status = { text: g.tr(key("ui.cooked")), color: F.GREEN };
    else if (d.burnTicks <= 0) status = { text: g.tr(key("ui.no_fire")), color: F.RED };
    else status = { text: g.tr(key("ui.wait"), (((1000 - slot.doneness) / 1000) * 10).toFixed(1)), color: F.YELLOW };
    return [
      { text: itemName, color: F.WHITE },
      { text: g.tr(key("ui.cell"), Math.floor(cell / 3) + 1, (cell % 3) + 1), color: F.GRAY },
      status,
    ];
  },
};

export default [hotpot, stove];
