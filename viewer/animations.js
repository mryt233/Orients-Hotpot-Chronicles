// Animation scenes for the workspace mod viewer's 「动画」 tab.
// Web ports of everything the game animates at runtime, kept in sync with the Java:
//   floating food        ← client/HotPotBlockEntityRenderer.java
//   square bubbles       ← client/BoilingEffects.java
//   steam / stove flames ← hotpot/HotPotBlockEntity.clientTick
//   heat ramp            ← clientHeat += (target - clientHeat) * 0.08 per tick
// Animated block textures (boiling soup etc.) come from the models themselves.
// mryt233 改（2026-11-11）：Java 方块图集查询移至 client/RenderSprites；此处绘制参数不变。
// mryt233 改（2026-11-13）：支持 other 通用自定义汤底预览与沸腾配色兜底。
import * as THREE from "three";
import { BoxBatch, Particles, TexBoxBatch, hash, frac, easeOutCubic, easeInOutSine, mc, rand } from "/anim.js";

const NS = "orients_hotpot_chronicles";
const PX = 1 / 16;

// ---------------------------------------------------------------- shared constants (HotPotBlockEntityRenderer)

const STOVE = { innerMin: 1.837 / 16, innerSize: 12.326 / 16, surface: 14.32 / 16 };
const STANDALONE = { innerMin: 1.5 / 16, innerSize: 13 / 16, surface: 5.6 / 16 };
// tall stove (StoveBlock.TALL_LIFT = 10 px): pot, soup, food, steam and the collar sparks are all lifted;
// the firebox mouth flame stays at the bottom (HotPotBlockEntity.clientTick, liftOf). mryt233 加，2026-10-25
const TALL_LIFT = 10 / 16;
const TALL_STOVE = { ...STOVE, surface: STOVE.surface + TALL_LIFT };
const OMEGA = 0.22, AMPLITUDE = 0.022, FOOD_SCALE = 0.24;
// cold pot: float just above the soup, slow bob (~6 s) — HotPotBlockEntityRenderer COLD_*
const COLD_OMEGA = 0.05, COLD_AMPLITUDE = 0.004, COLD_FLOAT = 0.009;
// the mod's own hotpot ingredients (pot only takes food since r108) — mryt233 改 2026-10-23
const FOODS = ["orients_hotpot_chronicles:fatty_beef_roll", "orients_hotpot_chronicles:potato_slice", "orients_hotpot_chronicles:cooked_beef_meatball",
  "orients_hotpot_chronicles:tofu_cube", "orients_hotpot_chronicles:mutton_roll", "orients_hotpot_chronicles:beef_loin_slice",
  "orients_hotpot_chronicles:fish_ball", "orients_hotpot_chronicles:tripe", "orients_hotpot_chronicles:cooked_cilantro_pork_meatball"];

const PALETTES = {
  clear: { body: 0xc6ccca, hi: 0xf4f4ec, shadow: 0x96a0a2 },   // clear broth recoloured 2026-10-23
  rich_stock: { body: 0xf2e7d2, hi: 0xffffff, shadow: 0xc4b699 },
  mushroom: { body: 0x9a7048, hi: 0xe8c88a, shadow: 0x5a3e24 },
  spicy: { body: 0xbe3325, hi: 0xd8553a, shadow: 0x7e1c16 },
  tomato: { body: 0xe2563a, hi: 0xff9a6a, shadow: 0x962a18 },
  three_fresh: { body: 0xe6d8b0, hi: 0xfff6dc, shadow: 0xb2a072 },
  other: { body: 0xd9c7a3, hi: 0xf1e5c9, shadow: 0xb8a37d },
};
const soupPalette = (soup) => PALETTES[soup] || PALETTES.other;
const isBuiltinSoup = (soup) => Object.hasOwn(PALETTES, soup) && soup !== "other";

async function buildSoupMesh(ctx, modid, soup, lit, y, innerMin, innerSize, regions) {
  const builtin = isBuiltinSoup(soup);
  const texName = builtin ? soup : "clear";
  const tex = await ctx.texture(`${modid}:block/hot_pot_top_${texName}${lit ? "_boiling" : ""}`);
  const pos = [], uv = [];
  for (const [u0, v0, u1, v1] of regions) {
    const X = (u) => innerMin + innerSize * u - 0.5, Z = (v) => innerMin + innerSize * v - 0.5;
    const quad = [[u0, v1], [u1, v1], [u1, v0], [u0, v0]];
    for (const t of [0, 1, 2, 0, 2, 3]) {
      const [u, v] = quad[t];
      pos.push(X(u), y - 0.5, Z(v));
      uv.push(u, 1 - v);
    }
  }
  const geo = new THREE.BufferGeometry();
  geo.setAttribute("position", new THREE.Float32BufferAttribute(pos, 3));
  geo.setAttribute("uv", new THREE.Float32BufferAttribute(uv, 2));
  const matOpts = { map: tex.texture || tex, side: THREE.DoubleSide };
  if (!builtin) matOpts.color = soupPalette(soup).body;
  return new THREE.Mesh(geo, new THREE.MeshBasicMaterial(matOpts));
}

// ---------------------------------------------------------------- BoilingEffects port

// Square bubbles: a cube swells out of the soup, floats up, splits from the middle into four
// smaller cubes that fly apart, then they drop back and sink, leaving a faint ripple.
// All pure functions of time (same hash / easing as the Java side).
function boilingEffects(batch, soup, pot, heat, t, salt, keepAt = null) {
  if (!soup || soup === "none" || heat < 0.05) return;
  const pal = soupPalette(soup);
  const { innerMin, innerSize } = pot;
  const y = pot.surface + 0.002;
  const box = (x0, y0, z0, x1, y1, z1, top, side, alpha = 1) => batch.box(mc(x0), mc(y0), mc(z0), mc(x1), mc(y1), mc(z1), top, side, alpha);
  const cube = (x, yb, z, s, alpha = 1) => box(x - s / 2, yb, z - s / 2, x + s / 2, yb + s, z + s / 2, pal.hi, pal.body, alpha);

  // Timing follows real boiling, slowed to be readable: a site sends up a bubble every fraction
  // of a second, the cap ruptures almost instantly, and the bits drop back fast.
  // = BoilingEffects（2026-11-01 同步）：t 是锅自己累计的气泡时间（热度已经积进去），周期不再除热度；
  // 第 i 个气泡随火候从小长出来（grow），不再取整后突然冒出半个
  for (let i = 0; i < 8; i++) {
    const grow = Math.max(0, Math.min(1, 3 + 5 * heat - i));
    if (grow <= 0) continue;
    const period = 0.7 + hash(i, 21) * 0.45;
    const off = hash(i, 22 + salt);
    const cyc = Math.floor(t / period + off);
    const u = frac(t / period + off);
    // a new spot each round, anywhere in the pot
    const x0 = innerMin + innerSize * hash(i, cyc * 2 + salt);
    const z0 = innerMin + innerSize * hash(i, cyc * 2 + 1 + salt);
    // smaller bubbles toward the pot wall: full size 3 px in from the edge, ~1/3 at the edge
    const edge = Math.min(x0 - innerMin, innerMin + innerSize - x0, z0 - innerMin, innerMin + innerSize - z0) / PX;
    const near = Math.max(0.35, Math.min(1, 0.35 + (0.65 * edge) / 3));
    const sMax = PX * (0.7 + 0.6 * hash(i, cyc * 5 + salt)) * (0.85 + 0.15 * heat) * near * grow; // ~0.25..1.3 px
    // keep the bubble and its split pieces clear of the wall
    const keep = sMax * 1.3 + 0.3 * PX;
    const x = Math.min(innerMin + innerSize - keep, Math.max(innerMin + keep, x0));
    const z = Math.min(innerMin + innerSize - keep, Math.max(innerMin + keep, z0));
    if (keepAt && !keepAt(x, z)) continue;   // 鸳鸯 / 田字锅：每边只冒自己那边的泡（= BoilingEffects keepAt）
    const rise = PX * (0.3 + 0.4 * hash(i, cyc * 5 + 1)); // just clears the surface
    const pop = PX * 0.25;

    if (u < 0.45) {
      // 1. swell out of the soup, lower part still submerged
      const s = sMax * easeOutCubic(u / 0.45);
      if (s > 0.15 * PX) cube(x, y - s * 0.35, z, s);
    } else if (u < 0.6) {
      // 2. lift just off the surface
      const f = easeInOutSine((u - 0.45) / 0.15);
      cube(x, y - sMax * 0.35 * (1 - f) + rise * f, z, sMax);
    } else if (u < 0.68) {
      // 3. burst from the middle - nearly instant, like a real film rupture
      const p = easeOutCubic((u - 0.6) / 0.08);
      const q = sMax / 2;
      const spread = q / 2 + sMax * 0.55 * p;
      const yb = y + rise + pop * p;
      for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) cube(x + dx * spread, yb, z + dz * spread, q);
    } else {
      // 4. the pieces drop back under gravity, shrink and sink
      const p = (u - 0.68) / 0.32;
      const q = (sMax / 2) * (1 - 0.7 * p);
      const spread = sMax / 4 + sMax * 0.55 + sMax * 0.25 * p;
      const yb = y + (rise + pop) * (1 - p * p) - q * 0.5 * p;
      for (const [dx, dz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) cube(x + dx * spread, yb, z + dz * spread, q);
      // faint ripple where they land
      if (p > 0.6) {
        const r = sMax * (0.8 + (1.2 * (p - 0.6)) / 0.4);
        const a = 1 - (p - 0.6) / 0.4;
        const d = 0.18 * PX;
        for (let j = 0; j < 8; j++) {
          const ang = (j / 8) * Math.PI * 2;
          const rx = x + Math.cos(ang) * r, rz = z + Math.sin(ang) * r;
          box(rx - d, y, rz - d, rx + d, y + 0.1 * PX, rz + d, pal.hi, pal.hi, a);
        }
      }
    }
  }
}

// ---------------------------------------------------------------- scene builder

// kind: "stove" (pot sitting in a stove) | "pot" (pot block heated from below) | "stoveOnly"
async function buildScene(ctx, s, kind) {
  const root = new THREE.Group();
  const lit = s.fire === "on";
  let state;
  const stoveLike = kind === "stove" || kind === "tallStove";
  const lift = kind === "tallStove" ? TALL_LIFT : 0;
  // 鸳鸯 / 田字锅（2026-10-26）：锅身模型换成那种锅的，汤面不走方块状态（soup=none），照 HotPotBlockEntityRenderer.renderSideSoups 按边画
  const shape = s.shape && s.shape !== "nine" ? YY_SHAPES.find((x) => x[0] === s.shape) : null;
  const customSingleSoup = !shape && kind !== "stoveOnly" && s.soup && s.soup !== "none" && !isBuiltinSoup(s.soup);
  const blockSoup = shape || customSingleSoup ? "none" : s.soup;
  if (stoveLike) state = { lit: String(lit), facing: "north", pot: shape ? `${s.pot}_${shape[1]}_soup` : `${s.pot}_soup`, soup: blockSoup, pot_axis: "x" };
  // an empty stove in game is pot=none, soup=none (StoveBlock default state), so no soup is drawn
  else if (kind === "stoveOnly") state = { lit: String(lit), facing: "north", pot: "none", soup: "none", pot_axis: "x" };
  else state = { soup: blockSoup, boiling: String(lit), axis: "x" };
  const potId = shape ? { s: "taiji_yuanyang_hotpot", o: "round_yuanyang_hotpot", square: "square_yuanyang_hotpot", split: "split_yuanyang_hotpot", four: "four_grid_hotpot" }[shape[0]] : "nine_grid_hotpot";
  if (kind === "pot" && shape) state = { axis: "x" };
  const blockId = kind === "pot" ? `${s.pot}_${potId}` : kind === "tallStove" ? "tall_brick_stove" : "brick_stove";
  if (kind === "pot" && lit) state.on_campfire = "true";   // legs down to the campfire (gen_pot_on_campfire.py)
  root.add(await ctx.buildBlock(blockId, state));

  // 坐在篝火上的锅整体下沉 4 像素（= HotPotBlock.CAMPFIRE_DROP，2026-10-26 用户：调矮）
  const pot = kind === "tallStove" ? TALL_STOVE : kind === "stove" ? STOVE
    : kind === "pot" && lit ? { ...STANDALONE, surface: STANDALONE.surface - 4 / 16 } : STANDALONE;
  const hasSoup = kind !== "stoveOnly";
  // 每边的汤（第 1 边 = 汤底，第 2 边 = 第 2 边汤底，田字锅 3 / 4 边轮流用这两种）
  const sideSoups = shape ? [...Array(shape[2])].map((_, i) => (i % 2 === 0 ? s.soup : s.soup2 || "clear")) : null;
  if (customSingleSoup) {
    root.add(await buildSoupMesh(ctx, ctx.modid, s.soup, lit, pot.surface + 0.001, pot.innerMin, pot.innerSize, [[0, 0, 1, 1]]));
  }
  if (shape && hasSoup) {
    for (let side = 0; side < shape[2]; side++) {
      if (!sideSoups[side] || sideSoups[side] === "none") continue;
      root.add(await buildSoupMesh(ctx, ctx.modid, sideSoups[side], lit, pot.surface + 0.001, pot.innerMin, pot.innerSize, yyRegions(shape[0], side)));
    }
  }
  const batch = new BoxBatch();
  const particles = new Particles();
  root.add(batch, particles);

  // floating food: each big cell holds 1-3 foods (sample) and shows them in turn, like visibleSlots()
  const cells = [];
  if (hasSoup && s.food === "on") {
    for (let cell = 0; cell < 9; cell++) {
      if (shape && YY_LAYOUT[shape[0]].cells[cell] < 0) continue;   // 这种锅不用的格（正中 / 隔板上）
      const count = 1 + ((cell * 2) % 3); // 1, 3, 2, 1, 3, 2 ...
      const items = [];
      for (let j = 0; j < count; j++) {
        const item = await ctx.buildItem(FOODS[(cell + j * 4) % FOODS.length]);
        const holder = new THREE.Group();
        const fixed = new THREE.Group();
        fixed.rotation.y = Math.PI; // item/generated "fixed" display: rotation [0, 180, 0]
        fixed.add(item);
        holder.add(fixed);
        holder.rotation.order = "YXZ";
        holder.visible = false;
        root.add(holder);
        // small cells of this big cell, row-major
        const slot = Math.floor(cell / 3) * 27 + (cell % 3) * 3 + Math.floor(j / 3) * 9 + (j % 3);
        items.push({ holder, slot });
      }
      cells.push({ cell, items });
    }
  }

  // clientHeat survives rebuilds (toggling the fire swaps the block model), so the ramp is visible
  let heat = s._heat ?? (lit && hasSoup ? 1 : 0);
  // = HotPotBlockEntity.bubbleSeconds / cycleAngle：每刻按当前热度累加，换档不跳（2026-11-01 同步 B4/B5）
  let bubbleT = s._bubbleT ?? 0;
  let cycleAngle = s._cycleAngle ?? 0;
  const cycleRate = () => (hasSoup && heat < 0.5 ? COLD_OMEGA : OMEGA);
  const target = () => (s.fire === "on" && hasSoup ? 1 : 0);
  const salt = 0x1d3; // a fixed "block position" seed

  return {
    object: root,
    // once per game tick (20/s)
    tick() {
      heat += (target() - heat) * 0.08;
      s._heat = heat;
      bubbleT += (0.7 + 0.5 * heat) / 20;
      cycleAngle += cycleRate();
      s._bubbleT = bubbleT;
      s._cycleAngle = cycleAngle;
      if (kind !== "pot" && s.fire === "on" && rand() < 0.5) {
        // HotPotBlockEntity: SMALL_FLAME at the stove mouth (facing north)
        const side = (rand() - 0.5) * 0.36;
        const life = Math.floor(8 / (rand() * 0.8 + 0.2)) + 4;
        particles.spawn({ x: mc(0.5 - side), y: mc(0.27), z: mc(0.07), vx: 0, vy: 0.035, vz: 0, life, size0: 0.06, size1: 0.01, color: 0xffd060, color1: 0xff5a20 });
      }
      // sparks from the gap between pot and stove collar on all four sides (or the open socket)
      if (kind !== "pot" && s.fire === "on" && rand() < 0.35) {
        let x, z, y;
        if (stoveLike) {
          const gap = 1.125 / 16, along = (1 + rand() * 14) / 16;
          const side = Math.floor(rand() * 4);
          [x, z] = side === 0 ? [along, gap] : side === 1 ? [along, 1 - gap] : side === 2 ? [gap, along] : [1 - gap, along];
          y = 11 / 16 + lift + 0.01;
        } else {
          x = (2 + rand() * 12) / 16; z = (2 + rand() * 12) / 16; y = 8 / 16 + lift;
        }
        const life = Math.floor(8 / (rand() * 0.8 + 0.2)) + 4;
        particles.spawn({ x: mc(x), y: mc(y), z: mc(z), vx: (x - 0.5) * 0.01, vy: 0.02 + rand() * 0.02, vz: (z - 0.5) * 0.01, life, size0: 0.05, size1: 0.01, color: 0xffd060, color1: 0xff5a20 });
      }
      if (hasSoup && heat > 0.08) {
        const puffs = rand() < heat * 0.6 ? 1 + (rand() < heat * 0.5 ? 1 : 0) : 0;
        for (let i = 0; i < puffs; i++) {
          const life = Math.floor(8 / (rand() * 0.8 + 0.2));
          particles.spawn({
            x: mc(0.5 + (rand() - 0.5) * 0.55), y: mc(pot.surface + 0.03), z: mc(0.5 + (rand() - 0.5) * 0.55),
            vx: (rand() - 0.5) * 0.01, vy: 0.03 + heat * 0.045, vz: (rand() - 0.5) * 0.01,
            life: life + 6, size0: 0.05, size1: 0.12, color: 0xeeeeee, color1: 0xbdbdbd, gravity: -0.002, opacity: 0.5,
          });
        }
      }
      particles.tick();
    },
    // every render frame; ticks = game time in ticks (fractional), partial = progress into the current tick
    frame(ticks, partial) {
      batch.begin();
      const bt = bubbleT + (partial * (0.7 + 0.5 * heat)) / 20;   // 带这一刻内的插值（B5）
      if (hasSoup && !shape) boilingEffects(batch, s.soup, pot, heat, bt, salt);
      if (hasSoup && shape) {
        for (let side = 0; side < shape[2]; side++) {
          boilingEffects(batch, sideSoups[side], pot, heat, bt, salt + side * 97,   // = HotPotBlockEntityRenderer（2026-11-01 对齐，原来是 101）
            (x, z) => yySide(shape[0], (x - pot.innerMin) / pot.innerSize, (z - pot.innerMin) / pot.innerSize) === side);
        }
      }
      batch.end();
      for (const { cell, items } of cells) {
        const n = items.length;
        const col = cell % 3, row = Math.floor(cell / 3);
        const phase = cell * 1.37; // per big cell only, so swapping foods never jumps the rhythm
        const angle = OMEGA * ticks + phase;
        const coldAngle = COLD_OMEGA * ticks + phase;
        const cold = hasSoup ? 1 - heat : 0;
        // a new food each bob, swapped at the low point (sin = -1); slow rhythm in a cold pot.
        // 轮换相位用累计的 cycleAngle（= HotPotBlockEntity.cycleAngle），点火 / 熄火不跳（B4）
        const cycle = (cycleAngle + partial * cycleRate() + phase - 1.5 * Math.PI) / (2 * Math.PI);
        const idx = ((Math.floor(cycle) % n) + n) % n;
        let dip = 0;
        if (n > 1) {
          const f = cycle - Math.floor(cycle);
          const edge = Math.min(f, 1 - f);
          if (edge < 0.15) dip = Math.pow(1 - edge / 0.15, 2);
        }
        const yOffset = AMPLITUDE * heat * Math.sin(angle) + COLD_AMPLITUDE * cold * Math.sin(coldAngle);
        const drift = 0.006 * heat * Math.cos(OMEGA * 0.7 * ticks + phase);
        const tilt = 3.5 * heat * Math.sin(OMEGA * 0.55 * ticks + phase) + 1.0 * cold * Math.sin(COLD_OMEGA * 0.6 * ticks + phase);
        const c = shape ? YY_LAYOUT[shape[0]].centers[cell] : [(col + 0.5) / 3 * 13, (row + 0.5) / 3 * 13];   // = PotLayout.cellCenter（像素 / 13）
        const x = pot.innerMin + (pot.innerSize * c[0]) / 13 + drift;
        const z = pot.innerMin + (pot.innerSize * c[1]) / 13 - drift;
        items.forEach((it, j) => {
          it.holder.visible = j === idx;
          if (j !== idx) return;
          it.holder.position.set(mc(x), mc(pot.surface - (0.007 * (1 - cold) - COLD_FLOAT * cold) - 0.03 * dip + yOffset), mc(z));
          it.holder.rotation.set(((90 + tilt) * Math.PI) / 180, (((it.slot * 47) % 360) * Math.PI) / 180, ((tilt * 0.55) * Math.PI) / 180);
          it.holder.scale.setScalar(FOOD_SCALE);   // = HotPotBlockEntityRenderer：大小不变，换样只靠下沉（2026-11-01 用户）
        });
      }
      particles.draw(partial);
    },
  };
}

const POT = { key: "pot", label: "锅", options: [["copper", "铜锅"], ["iron", "铁锅"]], default: "copper" };
// 锅形（2026-10-26 用户：灶台上煮火锅里把各个锅形加上）：nine = 九宫格，其余同 YY_SHAPES
const SHAPE = { key: "shape", label: "锅形", options: [["nine", "九宫格"], ["s", "太极鸳鸯"], ["o", "圆心鸳鸯"], ["square", "方心鸳鸯"], ["split", "直隔鸳鸯"], ["four", "田字"]], default: "nine" };
const SOUP2 = { key: "soup2", label: "第 2 边汤底（鸳鸯 / 田字锅）", options: [["clear", "清汤"], ["spicy", "红汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["other", "自定义汤底"]], default: "clear" };
const SOUP = { key: "soup", label: "汤底", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["other", "自定义汤底"]], default: "spicy" };
const FIRE = { key: "fire", label: "灶火", options: [["on", "点着"], ["off", "熄灭"]], default: "on", live: true };
const FOOD = { key: "food", label: "食材", options: [["on", "放几样"], ["off", "不放"]], default: "on" };

const WOOD = { key: "wood", label: "桌子木头", options: [["oak", "橡木"], ["spruce", "云杉"], ["birch", "白桦"], ["jungle", "丛林"],
  ["acacia", "金合欢"], ["dark_oak", "深色橡木"], ["mangrove", "红树"], ["cherry", "樱花"], ["bamboo", "竹"], ["crimson", "绯红"], ["warped", "诡异"]], default: "oak" };

// 3x3 table around the stove: the 8 tables get the same north/east/south/west links the game
// computes (HotPotTableBlock.refreshConnections: a neighbour table or the stove counts as linked)
async function buildTableRing(ctx, s) {
  const scene = await buildScene(ctx, s, "stove");
  for (let dx = -1; dx <= 1; dx++) {
    for (let dz = -1; dz <= 1; dz++) {
      if (!dx && !dz) continue;
      const inRing = (x, z) => Math.abs(x) <= 1 && Math.abs(z) <= 1;
      const state = { north: String(inRing(dx, dz - 1)), east: String(inRing(dx + 1, dz)), south: String(inRing(dx, dz + 1)), west: String(inRing(dx - 1, dz)) };
      const table = await ctx.buildBlock(`${s.wood}_hotpot_table`, state);
      table.position.set(dx, 0, dz);
      scene.object.add(table);
    }
  }
  // benches: three per side, one block out from the table, all facing the stove
  // (BenchBlock: facing = where the sitter looks; left = facing rotated counter-clockwise)
  if (s.bench === "on") {
    const DIRS = { north: [0, -1], east: [1, 0], south: [0, 1], west: [-1, 0] };
    const ccw = { north: "west", west: "south", south: "east", east: "north" };
    const sides = [["south", 0, -2], ["north", 0, 2], ["east", -2, 0], ["west", 2, 0]]; // facing, centre x, z
    for (const [facing, cx, cz] of sides) {
      const [lx, lz] = DIRS[ccw[facing]];
      for (let k = -1; k <= 1; k++) {
        const x = cx + (cx === 0 ? k : 0), z = cz + (cz === 0 ? k : 0);
        const has = (px, pz) => Math.abs(cx === 0 ? px : pz) <= 1 && (cx === 0 ? pz === cz : px === cx);
        const state = { facing, left: String(has(x + lx, z + lz)), right: String(has(x - lx, z - lz)) };
        const bench = await ctx.buildBlock(`${s.wood}_stool`, state);
        bench.position.set(x, 0, z);
        scene.object.add(bench);
      }
    }
  }
  return scene;
}

// Tall bench legs: two per end, splayed like an "A" (top at z 6.5 / 9.5 under the seat, 2 px further out at
// the floor, ~8°). Block models can only rotate in 22.5° steps, so the legs are drawn here at the exact angle
// (in game this becomes a block-entity renderer). Coordinates are for a north-facing bench model.
const LEG_SPREAD = 2, LEG_TOP = 14, LEG_W = 2, LEG_T = 1.5;
async function benchLegs(ctx, wood, state) {
  const tex = await ctx.texture(`minecraft:block/${wood}_planks`);
  const group = new THREE.Group();
  const left = state.left === "true", right = state.right === "true";
  const single = !left && !right;
  const ends = [];
  if (!left) ends.push(single ? 1.5 : 4.5);
  if (!right) ends.push(single ? 12.5 : 9.5);
  const angle = Math.atan(LEG_SPREAD / LEG_TOP), len = Math.hypot(LEG_SPREAD, LEG_TOP);
  for (const x0 of ends) {
    for (const [zc, sign] of [[6.5, 1], [9.5, -1]]) {
      const leg = new TexBoxBatch(tex.texture || tex, 2);
      const p = (v) => v / 16;
      // leg hangs down from its pivot at the top centre, then tilts outwards
      // mryt233 改（2026-11-10）：照 BenchLegsRenderer.box()：每个面取木板贴图左上角「面宽 × 面高」像素
      // （东西两面宽 LEG_T 而不是 LEG_W），并补上底面；以前四个侧面都按 LEG_W 取图、也不画底面，和游戏差一点。
      const a = p(0), b = p(LEG_W), y0 = p(-len), y1 = p(0), z0 = p(-LEG_T / 2), z1 = p(LEG_T / 2);
      const uv = (w, h) => [0, 1, w / 16, 1 - h / 16];
      leg._quad([[a, y1, z0], [b, y1, z0], [b, y1, z1], [a, y1, z1]], uv(LEG_W, LEG_T), 1.0); // up
      leg._quad([[a, y0, z1], [b, y0, z1], [b, y0, z0], [a, y0, z0]], uv(LEG_W, LEG_T), 0.5); // down
      leg._quad([[b, y1, z0], [a, y1, z0], [a, y0, z0], [b, y0, z0]], uv(LEG_W, len), 0.8); // north
      leg._quad([[a, y1, z1], [b, y1, z1], [b, y0, z1], [a, y0, z1]], uv(LEG_W, len), 0.8); // south
      leg._quad([[a, y1, z0], [a, y1, z1], [a, y0, z1], [a, y0, z0]], uv(LEG_T, len), 0.6); // west
      leg._quad([[b, y1, z1], [b, y1, z0], [b, y0, z0], [b, y0, z1]], uv(LEG_T, len), 0.6); // east
      leg.end();
      const pivot = new THREE.Group();
      pivot.position.set(mc(p(x0)), mc(p(LEG_TOP)), mc(p(zc)));
      pivot.rotation.x = sign * angle;
      pivot.add(leg);
      group.add(pivot);
    }
  }
  const y = { north: 0, east: 90, south: 180, west: 270 }[state.facing];
  const wrap = new THREE.Group();
  wrap.rotation.y = (-y * Math.PI) / 180;
  wrap.add(group);
  return wrap;
}

// 1.5-block table, tall stove (firebox still at the bottom, pot / soup lifted 10 px) and the one-block-high bench.
// In the game since r83; the stove part is the same dynamic scene as stove_pot (BER food, boiling, steam, sparks),
// lifted like the game does (mryt233 改，2026-10-25: was static models only).
async function buildTallRing(ctx, s) {
  const scene = await buildScene(ctx, s, "tallStove");
  const root = scene.object;
  const inRing = (x, z) => Math.abs(x) <= 1 && Math.abs(z) <= 1;
  for (let dx = -1; dx <= 1; dx++) {
    for (let dz = -1; dz <= 1; dz++) {
      if (!dx && !dz) continue;
      const state = { north: String(inRing(dx, dz - 1)), east: String(inRing(dx + 1, dz)), south: String(inRing(dx, dz + 1)), west: String(inRing(dx - 1, dz)) };
      // the stove faces north here, so the table right in front of the firebox is left open
      state.open = String(dx === 0 && dz === -1);
      const t = await ctx.buildBlock(`${s.wood}_tall_hotpot_table`, state);
      t.position.set(dx, 0, dz);
      root.add(t);
    }
  }
  if (s.seat !== "none") {
    const id = s.seat === "tall" ? `${s.wood}_bench` : `${s.wood}_stool`;
    const DIRS = { north: [0, -1], east: [1, 0], south: [0, 1], west: [-1, 0] };
    const ccw = { north: "west", west: "south", south: "east", east: "north" };
    for (const [facing, cx, cz] of [["south", 0, -2], ["north", 0, 2], ["east", -2, 0], ["west", 2, 0]]) {
      const [lx, lz] = DIRS[ccw[facing]];
      for (let k = -1; k <= 1; k++) {
        const x = cx + (cx === 0 ? k : 0), z = cz + (cz === 0 ? k : 0);
        const has = (px, pz) => Math.abs(cx === 0 ? px : pz) <= 1 && (cx === 0 ? pz === cz : px === cx);
        const st = { facing, left: String(has(x + lx, z + lz)), right: String(has(x - lx, z - lz)) };
        const b = await ctx.buildBlock(id, st);
        b.position.set(x, 0, z);
        root.add(b);
        if (s.seat === "tall") {
          const legs = await benchLegs(ctx, s.wood, st);
          legs.position.set(x, 0, z);
          root.add(legs);
        }
      }
    }
  }
  return scene;
}

// Plates (PlateHolderRenderer): several plates share one block, each drawn with its own model shifted
// to its centre; one food per cell lying flat, scaled to the cell's short side (PlateSize.cellCenter).
// Sizes in pixels, long side along x. On a hotpot table the whole layer drops 5 px onto the table top.
// item / block ids of the plate sizes (r102: ids match the display names)
const PLATE_IDS = { "1x1": "small_dish", "1x2": "two_slot_plate", "1x3": "three_slot_long_plate", "2x2": "four_slot_square_plate", "1x5": "five_slot_long_plate", "2x4": "eight_slot_platter" };
const PLATE_SIZES = { "1x1": [1, 1, 5, 5], "1x2": [2, 1, 11, 6], "1x3": [3, 1, 14, 6], "2x2": [2, 2, 11, 11], "1x5": [5, 1, 15, 6], "2x4": [4, 2, 15, 8] };
const PLATE_FOODS = ["minecraft:beef", "minecraft:carrot", "minecraft:cod", "minecraft:potato", "minecraft:mutton", "minecraft:kelp", "minecraft:chicken", "minecraft:salmon"];
// what fits in one block (centres in px): the user's examples plus a mixed one
const PLATE_LAYOUTS = [
  [["2x2", 8, 8]],
  [["1x5", 8, 4.5], ["1x5", 8, 11.5]],
  [["1x1", 2.5, 2.5], ["1x1", 13.5, 2.5], ["1x1", 8, 8], ["1x1", 2.5, 13.5], ["1x1", 13.5, 13.5]],
  [["2x4", 8, 4], ["2x4", 8, 12]],
  [["1x3", 8, 4], ["1x3", 8, 12]],
  [["1x3", 8, 3.5], ["1x2", 5.5, 12.5], ["1x1", 13.5, 12.5]],
];
// 摆在桌上的芝麻酱 / 蚝油瓶（r146a，mryt233 改 2026-11-08）：和碗、盘子一样借「盘子格」摆，模型是
// models/block/sauce_bottle_<调料>.json（tools/gen_sauce_bottle.py），瓶身 4×4、高 6 / 8 像素
// （= Java 的 SauceBottleKind#bottleHeight，选中框与碰撞也按它算）。摆在桌上时和盘子一起按 drop 下移贴桌面。
const SAUCE_BOTTLES = ["sesame_paste", "oyster_sauce"];
// 两瓶的中心位置（格内像素）：瓶身 4×4，中心 4.5 / 11.5 正好并排不重叠
const SAUCE_BOTTLE_X = [4.5, 11.5];
async function addSauceBottles(ctx, root, bx, bz, lift) {
  const ns = ctx.modid;
  for (let i = 0; i < SAUCE_BOTTLES.length; i++) {
    const model = await ctx.buildModel(`${ns}:block/sauce_bottle_${SAUCE_BOTTLES[i]}`);
    model.position.set(bx + (SAUCE_BOTTLE_X[i] - 8) / 16, lift / 16, bz);
    root.add(model);
  }
}

async function buildPlates(ctx, s) {
  const root = new THREE.Group();
  const onTable = s.place === "table";
  const drop = onTable ? 5 : 0;
  let food = 0;
  for (let n = 0; n < PLATE_LAYOUTS.length; n++) {
    const bx = (n % 3) - 1, bz = Math.floor(n / 3) * 1.2 - 0.6;
    if (onTable) {
      const t = await ctx.buildBlock(`${s.wood}_hotpot_table`, { north: "true", east: "true", south: "true", west: "true" });
      t.position.set(bx, -1, bz);
      root.add(t);
    }
    for (const [size, px, pz] of PLATE_LAYOUTS[n]) {
      const [cols, rows, len, wid] = PLATE_SIZES[size];
      const plate = await ctx.buildBlock(`${s.material}_${PLATE_IDS[size]}`, { axis: "x" });
      plate.position.set(bx + (px - 8) / 16, -drop / 16, bz + (pz - 8) / 16);
      root.add(plate);
      if (s.chop !== "none") await addRestingChopsticks(ctx, root, bx+(px-8)/16, -drop+1.75, bz+(pz-8)/16, s.chop === "food", Number(s.yaw));
      const innerL = len - 1, innerW = wid - 1;
      // r89: every plate shows food the same size (4 px) and each cell holds up to 3 — except the 1x1 dish (r135b)
      // r90 preview: other plates shingle (each next one half-overlaps the previous, sloping DOWN to the plate
      // floor at its free end, kept inside the rim); the bowls pile straight, each one turned 15° clockwise
      const cells = cols * rows, layers = 3;
      const count = s.fill === "full" ? cells * layers : cells;
      // RISE: how much higher each next item sits. Item models are 1/16 thick → 0.25 px at this size; a smaller
      // rise lets neighbours sink into each other a little (never all the way through), which reads right from
      // every angle and keeps the slope gentle (r90 user feedback: "翘得太高")
      const FOOD = 4, RISE = 0.15;
      // r135b (mryt233): the 1x1 dish's well is only 4×4 px, so a 4 px item already pokes past the 5×5 rim once it is
      // turned (layer 3's diagonal reaches 5.66 px). Its food shrinks to 3.0 px and turns 12° per layer: even at 24°
      // the turned item's half-width is 1.98 px, so all three layers stay inside the well (±2 px).
      const SMALL_DISH_FOOD = 3.0, SMALL_DISH_SPIN = -12;
      const smallDish = size === "1x1";
      const foodSize = smallDish ? SMALL_DISH_FOOD : FOOD;
      const inner = { x0: 8 - innerL / 2 + (px - 8), x1: 8 + innerL / 2 + (px - 8) };
      for (let i = 0; i < count; i++) {
        const c = i % cells, layer = Math.floor(i / cells);
        let cx = 8 - innerL / 2 + innerL / cols * (c % cols + 0.5) + (px - 8);
        const cz = 8 - innerW / 2 + innerW / rows * (Math.floor(c / cols) + 0.5) + (pz - 8);
        const item = await ctx.buildItem(PLATE_FOODS[food++ % PLATE_FOODS.length]);
        const fixed = new THREE.Group();
        fixed.rotation.y = Math.PI; // item/generated "fixed" display: rotation [0, 180, 0]
        fixed.add(item);
        const holder = new THREE.Group();
        holder.add(fixed);
        holder.rotation.order = "YXZ";
        holder.scale.setScalar(foodSize / 16);
        let y = 1 - drop + 0.002;
        let tiltZ = 0;
        if (smallDish) {
          // straight pile, centred, turned 12° clockwise per layer (seen from above) — PlateFoodRenderer r135b
          const spin = SMALL_DISH_SPIN * layer;
          holder.rotation.x = Math.PI / 2;
          holder.rotation.y = (spin * Math.PI) / 180;
          y += layer * RISE;
        } else if (s.stack !== "shingle") {
          // straight pile: each one a little higher, turned 15° clockwise (seen from above) from the one below
          holder.rotation.x = Math.PI / 2;
          holder.rotation.y = (-15 * layer * Math.PI) / 180;
          y += layer * RISE;
        } else {
          const n = Math.min(layers, Math.ceil((count - c) / cells));
          cx += (layer - (n - 1) / 2) * (FOOD / 2);
          // keep the whole item inside the rim
          cx = Math.min(inner.x1 - FOOD / 2, Math.max(inner.x0 + FOOD / 2, cx));
          if (layer > 0) {
            // back half rests on the previous item, front half drops to the floor: slope down along +x
            tiltZ = -Math.atan2(RISE, FOOD / 2);
            y += layer * RISE - RISE / 2;
          }
          holder.rotation.x = Math.PI / 2;
        }
        // tilt around the world z axis after the item is laid flat (holder lays it flat / spins it)
        const tilt = new THREE.Group();
        tilt.add(holder);
        tilt.rotation.z = tiltZ;
        tilt.position.set(bx + mc(cx / 16), mc(y / 16 + layer * 0.001), bz + mc(cz / 16));
        root.add(tilt);
      }
    }
  }
  // r146a（mryt233 改 2026-11-08）：最后一排空出来的那一格摆两个蘸料瓶（芝麻酱 / 蚝油），旁边放一只蘸料碗比大小，
  // 说明它们和碗、盘子一样借「盘子格」贴着桌面摆，摆着就能给菜加料。
  const bx = 0, bz = 2 * 1.2 - 0.6;
  if (onTable) {
    const t = await ctx.buildBlock(`${s.wood}_hotpot_table`, { north: "true", east: "true", south: "true", west: "true" });
    t.position.set(bx, -1, bz);
    root.add(t);
  }
  await addSauceBottles(ctx, root, bx, bz, -drop);
  // 旁边再摆一只蘸料碗（碗在格子正中的那块），比一比大小：瓶子和碗、盘子是同一个用法
  const scaleBlockX = bx + 1;
  if (onTable) {
    const t2 = await ctx.buildBlock(`${s.wood}_hotpot_table`, { north: "true", east: "true", south: "true", west: "true" });
    t2.position.set(scaleBlockX, -1, bz);
    root.add(t2);
  }
  const scaleBowl = await ctx.buildModel(`${ctx.modid}:block/bowl_b`);
  scaleBowl.position.set(scaleBlockX, -drop / 16, bz);
  root.add(scaleBowl);
  return { object: root, tick() {}, frame() {} };
}

// Bowls (r99 preview, mryt233): the vanilla bowl placed like a plate (same plate-holder layer, same drop / lift onto
// the table top), holding up to 3 foods piled like the 1x1 dish (each turned 15° clockwise), or a bowl of soup.
// Two shapes to pick from: a = straight walls 6×6, b = flared 6×6 → 8×8. Models from tools/gen_bowls.py.
const BOWL_FOODS = ["minecraft:beef", "minecraft:carrot", "minecraft:cod"];
async function buildBowls(ctx, s) {
  const root = new THREE.Group();
  const lift = s.place === "table" ? -5 : s.place === "tall" ? 7 : 0;
  const ns = ctx.modid;
  // two bowls per block: [left, right]; contents: number of foods, or a soup id
  const rows = [[0, 1], [2, 3], ["clear", "rich_stock"], ["mushroom", "spicy"]];
  for (let n = 0; n < rows.length; n++) {
    const bx = (n % 2) - 0.5, bz = Math.floor(n / 2) - 0.5;
    if (s.place === "table") {
      const t = await ctx.buildBlock(`${s.wood}_hotpot_table`, { north: "true", east: "true", south: "true", west: "true" });
      t.position.set(bx, -1, bz);
      root.add(t);
    } else if (s.place === "tall") {
      const t = await ctx.buildBlock(`${s.wood}_tall_hotpot_table`, { north: "true", east: "true", south: "true", west: "true", open: "false" });
      t.position.set(bx, -1, bz);
      root.add(t);
    }
    for (let k = 0; k < 2; k++) {
      const what = rows[n][k];
      const ox = (k === 0 ? 4 : 12) - 8;
      const soup = typeof what === "string";
      const bowl = await ctx.buildModel(`${ns}:block/bowl_${s.shape}${soup ? "_soup_" + what : ""}`);
      bowl.position.set(bx + ox / 16, lift / 16, bz);
      root.add(bowl);
      if (s.chop !== "none") await addRestingChopsticks(ctx, root, bx + ox / 16, lift + 3, bz, s.chop === "food", Number(s.yaw));
      if (soup) continue;
      for (let layer = 0; layer < what; layer++) {
        const item = await ctx.buildItem(BOWL_FOODS[layer]);
        const fixed = new THREE.Group();
        fixed.rotation.y = Math.PI;
        fixed.add(item);
        const holder = new THREE.Group();
        holder.add(fixed);
        holder.rotation.order = "YXZ";
        holder.scale.setScalar(4 / 16);
        holder.rotation.x = Math.PI / 2;
        holder.rotation.y = (-15 * layer * Math.PI) / 180;
        holder.position.set(bx + ox / 16, mc((lift + 1 + 0.002 + layer * 0.15) / 16 + layer * 0.001), bz);
        root.add(holder);
      }
    }
  }
  return { object: root, tick() {}, frame() {} };
}

// mryt233 改（r146e）：逐项对应 PlateHolderRenderer 的尺寸、接触点、颜色和变换。
const CHOP = { length: 8.5, thick: 0.55, tip: 0.38, gap: 1, handle: 8.5 * 0.45,
  restLen: 3.8, restDepth: 1.2, restH: 1.2, lip: 1.6 };
const CHOP_TILT = Math.atan(CHOP.restH / (CHOP.length * 0.88));
const CHOP_TAIL = 8 - CHOP.length * Math.cos(CHOP_TILT) / 2;
const CHOP_REST = CHOP_TAIL + CHOP.restH / Math.tan(CHOP_TILT) + 0.55;
function chopBox(root, x0, x1, y0, y1, z0, z1, material) {
  const box = new THREE.Mesh(new THREE.BoxGeometry((x1-x0)/16, (y1-y0)/16, (z1-z0)/16), material);
  box.position.set((x0+x1)/32, (y0+y1)/32, (z0+z1)/32);
  root.add(box);
}
async function bambooPair(ctx, root, withFood) {
  for (const side of [-1, 1]) {
    const z = side * (CHOP.gap + CHOP.thick) / 2;
    // 纹理的两个色区：与 Java 的 u=0.25 / 0.75，v=0.5 相同。
    const texture = await ctx.texture(`${ctx.modid}:block/resting_bamboo`);
    for (const [x0, x1, thick, u] of [[0, CHOP.handle, CHOP.thick, 0.25], [CHOP.handle, CHOP.length, CHOP.tip, 0.75]]) {
      const geometry = new THREE.BoxGeometry((x1-x0)/16, thick/16, thick/16);
      const uv = geometry.getAttribute("uv");
      for (let i=0; i<uv.count; i++) uv.setXY(i, u, 0.5);
      const mesh = new THREE.Mesh(geometry, new THREE.MeshBasicMaterial({map:texture}));
      mesh.position.set((x0+x1)/32, thick/32, z/16);
      root.add(mesh);
    }
  }
  if (withFood) {
    const food = new THREE.Group();
    const fixed = new THREE.Group();
    fixed.rotation.y = Math.PI;
    fixed.add(await ctx.buildItem("minecraft:cooked_beef"));
    food.add(fixed);
    food.scale.setScalar(0.175);
    food.position.set(CHOP.length/16, (CHOP.tip/2+1.4)/16, 0);
    root.add(food);
  }
}
async function addRestingChopsticks(ctx, root, x, rimY, z, withFood, yaw=0) {
  const lay = new THREE.Group();
  lay.rotation.y = (-90-yaw)*Math.PI/180;
  lay.position.set(x, mc(rimY/16), z);
  const tail = new THREE.Group();
  tail.position.x = -CHOP.length/16;
  lay.add(tail);
  await bambooPair(ctx, tail, withFood);
  root.add(lay);
}
async function buildTableChopsticks(ctx, s) {
  const root = new THREE.Group();
  const table = await ctx.buildBlock(`${s.wood}_hotpot_table`, {north:"true",east:"true",south:"true",west:"true"});
  table.position.y = -1;
  root.add(table);
  const lay = new THREE.Group();
  lay.position.y = mc(-5/16);
  const yaw = {south:0, west:90, north:180, east:270}[s.facing];
  lay.rotation.y = (-90-yaw)*Math.PI/180;
  const model = new THREE.Group();
  model.position.set(-0.5,0,-0.5);
  lay.add(model);
  const porcelain = new THREE.MeshBasicMaterial({map:await ctx.texture("minecraft:block/quartz_block_bottom")});
  const wood = new THREE.MeshBasicMaterial({map:await ctx.texture("minecraft:block/dark_oak_planks")});
  const cx=CHOP_REST, hl=CHOP.restLen/2, hd=CHOP.restDepth/2;
  chopBox(model,cx-hd-0.1,cx+hd+0.1,0,0.35,8-hl-0.1,8+hl+0.1,wood);
  chopBox(model,cx-hd+0.05,cx+hd-0.05,0.35,CHOP.restH,8-hl+0.15,8+hl-0.15,porcelain);
  chopBox(model,cx-hd+0.05,cx+hd-0.05,0.35,CHOP.lip,8-hl,8-hl+0.4,porcelain);
  chopBox(model,cx-hd+0.05,cx+hd-0.05,0.35,CHOP.lip,8+hl-0.4,8+hl,porcelain);
  const tail = new THREE.Group();
  tail.position.set(CHOP_TAIL/16,0,0.5);
  tail.rotation.z = CHOP_TILT;
  model.add(tail);
  await bambooPair(ctx,tail,s.food==="on");
  root.add(lay);
  return {object:root,tick(){},frame(){}};
}

// 果橙多 (r99 preview, mryt233): the drink bottle on a table next to a 1x1 dish of food and a bowl for scale.
// a = the user's real size 33×30×18 cm → about 5×3 px, 5.3 px tall; b = the photo's slim bottle, 3×2, 6 px tall.
async function buildDrink(ctx, s) {
  const root = new THREE.Group();
  const lift = s.place === "table" ? -5 : s.place === "tall" ? 7 : 0;
  const ns = ctx.modid;
  for (let n = 0; n < 2; n++) {
    const bx = n - 0.5;
    if (s.place === "table") {
      const t = await ctx.buildBlock(`${s.wood}_hotpot_table`, { north: "true", east: "true", south: "true", west: "true" });
      t.position.set(bx, -1, 0);
      root.add(t);
    } else if (s.place === "tall") {
      const t = await ctx.buildBlock(`${s.wood}_tall_hotpot_table`, { north: "true", east: "true", south: "true", west: "true", open: "false" });
      t.position.set(bx, -1, 0);
      root.add(t);
    }
  }
  const put = async (model, px, pz, x) => {
    const o = await ctx.buildModel(`${ns}:block/${model}`);
    o.position.set(x + (px - 8) / 16, lift / 16, (pz - 8) / 16);
    root.add(o);
  };
  // left block: the two bottle shapes; right block: a bowl and a small dish of food for scale
  await put("orange_drink", 4.5, 8, -0.5);
  await put("plant_protein_drink", 11.5, 8, -0.5);
  await put("bowl_b", 4.5, 8, 0.5);
  const dish = await ctx.buildBlock("quartz_small_dish", { axis: "x" });
  dish.position.set(0.5 + 3.5 / 16, lift / 16, 0);
  root.add(dish);
  const item = await ctx.buildItem("minecraft:cooked_beef");
  const fixed = new THREE.Group();
  fixed.rotation.y = Math.PI;
  fixed.add(item);
  const holder = new THREE.Group();
  holder.add(fixed);
  holder.scale.setScalar(3 / 16); // r135b: the 1x1 dish shows its food at 3 px, matching PlateFoodRenderer
  holder.rotation.x = Math.PI / 2;
  holder.position.set(0.5 + 3.5 / 16, mc((lift + 1.002) / 16), 0);
  root.add(holder);
  return { object: root, tick() {}, frame() {} };
}

// Sauce station (r103, mryt233; tiered 1.5-block version 2026-10-23): three steps, 3 small wooden bowls per step.
// Bowls are not fixed (user, 2026-10-23): what each bowl holds lives in the block entity, so the block model is the
// stand + empty bowls and the sauce surfaces are drawn by client/SauceStationRenderer — ported here 1:1:
// liquids = one face 0.5 px under the rim, chopped scallion / minced cilantro = four offset layers.
// One per vanilla wood (planks + quartz top) plus iron, 4 × 3. Models from tools/gen_sauce_station.py.
// 15 种内置蘸料（与 Seasoning.java 顺序一致）及缺贴图兜底颜色；mryt233 改
const SAUCE_IDS = [
  "msg", "chili_oil", "sweet_sour_sauce", "honey", "sesame_oil",
  "vinegar", "sugar", "chopped_scallion", "minced_cilantro", "garlic_paste",
  "oyster_sauce", "sesame_paste", "crushed_peanuts", "millet_chili", "minced_zhe_er_gen",
];
const SAUCE_COLORS = {
  msg: 0xececf0, chili_oil: 0xc42816, sweet_sour_sauce: 0xc85a3c, honey: 0xeca826, sesame_oil: 0xcc8a28,
  vinegar: 0x462418, sugar: 0xf0eee8, chopped_scallion: 0x6cb03a, minced_cilantro: 0x4e9a2c, garlic_paste: 0xe8d696,
  oyster_sauce: 0x5a321e, sesame_paste: 0xc89e64, crushed_peanuts: 0xdab46e, millet_chili: 0xe2281c, minced_zhe_er_gen: 0xd8d2bc,
};
const SAUCE_TIER_TOPS = [11, 15, 19], SAUCE_BOWL = 5, SAUCE_BOWL_H = 2.5, SAUCE_WALL = 0.75;
// garnish layers: depth under the rim, texture rect (0..16), quarter turns — same as SauceStationRenderer.GARNISH_LAYERS
const SAUCE_GARNISH = [[1.3, 2, 2, 14, 14, 0], [0.95, 0, 4, 12, 16, 1], [0.6, 4, 0, 16, 12, 2], [0.25, 3, 3, 13, 13, 3]];
const SAUCE_FILLS = {
  each: [0, 1, 2, 3, 4, 5, 6, 7, 8],
  extra: [9, 10, 11, 12, 13, 14, 7, 8, -1],
  mixed: [1, 1, 5, -1, 3, 1, 0, 7, -1],
  empty: [-1, -1, -1, -1, -1, -1, -1, -1, -1],
};

// 检测 viewer missingCanvas() 紫黑棋盘占位贴图，与 SauceStationRenderer 的 missing sprite 兜底对齐
function isMissingTex(tex) {
  const img = tex && tex.image;
  if (!img || typeof img.getContext !== "function") return false;
  const ctx2d = img.getContext("2d");
  if (!ctx2d) return false;
  const p0 = ctx2d.getImageData(0, 0, 1, 1).data;
  const p1 = ctx2d.getImageData(8, 0, 1, 1).data;
  const p2 = ctx2d.getImageData(8, 8, 1, 1).data;
  return p0[0] === 0xf8 && p0[1] === 0 && p0[2] === 0xf8
    && p1[0] === 0 && p1[1] === 0 && p1[2] === 0
    && p2[0] === 0xf8 && p2[1] === 0 && p2[2] === 0xf8;
}

// one up-facing quad in model pixels, texture rect [u0,v0,u1,v1] (0..16) turned `rot` quarter turns
function sauceQuad(tex, x0, y, z0, x1, z1, u0, v0, u1, v1, rot, color = 0xffffff) {
  const p = (v) => mc(v / 16);
  const pos = [[x0, z0], [x1, z0], [x1, z1], [x0, z1]];
  const uv = [[u0, v0], [u1, v0], [u1, v1], [u0, v1]];
  const verts = [], uvs = [];
  for (let k = 0; k < 4; k++) {
    const t = uv[(k + rot) % 4];
    verts.push(p(pos[k][0]), p(y), p(pos[k][1]));
    uvs.push(t[0] / 16, 1 - t[1] / 16);
  }
  const geo = new THREE.BufferGeometry();
  geo.setAttribute("position", new THREE.Float32BufferAttribute(verts, 3));
  geo.setAttribute("uv", new THREE.Float32BufferAttribute(uvs, 2));
  geo.setIndex([0, 3, 2, 0, 2, 1]);
  return new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ map: tex, color, alphaTest: 0.5, side: THREE.DoubleSide }));
}

async function sauceSurfaces(ctx, fill) {
  const group = new THREE.Group();
  for (const [i, k] of fill.entries()) {
    if (k < 0 || !SAUCE_IDS[k]) continue;
    const id = SAUCE_IDS[k];
    const garnish = id === "chopped_scallion" || id === "minced_cilantro";
    let tex = await ctx.texture(`${ctx.modid}:block/${garnish ? id : "sauce_" + id}`);
    let color = 0xffffff;
    if (isMissingTex(tex)) {
      tex = await ctx.texture(`${ctx.modid}:misc/white`);
      color = SAUCE_COLORS[id] ?? 0xd4a054;
    }
    const tier = 2 - Math.floor(i / 3), col = i % 3;
    const cx = 16 - (16 / 3) * (col + 0.5), cz = (16 / 3) * (tier + 0.5);
    const rim = SAUCE_TIER_TOPS[tier] + SAUCE_BOWL_H, half = SAUCE_BOWL / 2 - SAUCE_WALL;
    const layers = garnish ? SAUCE_GARNISH : [[0.5, 2, 2, 14, 14, 0]];
    for (const [depth, u0, v0, u1, v1, rot] of layers) {
      group.add(sauceQuad(tex, cx - half, rim - depth, cz - half, cx + half, cz + half, u0, v0, u1, v1, rot, color));
    }
  }
  return group;
}

async function buildSauceStation(ctx, s) {
  const root = new THREE.Group();
  const ns = ctx.modid;
  // 11 woods + iron, 4 × 3 like a shop counter row
  const bodies = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped", "iron"];
  const fill = SAUCE_FILLS[s.fill] || SAUCE_FILLS.each;
  for (const [i, body] of bodies.entries()) {
    const o = new THREE.Group();
    o.add(await ctx.buildModel(`${ns}:block/${body}_sauce_station_empty`));
    o.add(await sauceSurfaces(ctx, fill));
    o.position.set((i % 4) * 1.15 - 1.725, 0, Math.floor(i / 4) * 1.4 - 1.4);
    root.add(o);
  }
  return { object: root, tick() {}, frame() {} };
}

// Cutting board (2026-10-23, mryt233): port of client/CuttingBoardRenderer — the item on the board lies flat in the middle,
// half a block big, on top of the 3 px board (FIXED transform = turned 180° about y like an item frame).
async function buildCuttingBoard(ctx, s) {
  const root = new THREE.Group();
  // pounding meatballs (Pounding.display): hit 1 = minced, hit 2 = paste — shown as the hidden item pounded_meat
  // (custom model data → pounded_meat_<meat>_<stage>), so it is 3D like any item on the board; hit 3 drops the balls
  const items = { beef: "minecraft:beef", mutton: "minecraft:mutton", potato: "minecraft:potato", loin: `${ctx.modid}:beef_loin`, tofu: `${ctx.modid}:tofu`, empty: null,
    minced: `${ctx.modid}:pounded_meat_beef_minced`, paste: `${ctx.modid}:pounded_meat_beef_paste`, fish_paste: `${ctx.modid}:pounded_meat_fish_paste` };
  const lineup = s.show === "all" ? ["beef", "loin", "potato", "tofu", "empty"] : s.show === "pound" ? ["beef", "minced", "paste", "fish_paste"] : [s.show];
  for (const [i, key] of lineup.entries()) {
    const x = (i - (lineup.length - 1) / 2) * 1.1;
    const board = await ctx.buildBlock("cutting_board", {});
    board.position.set(x, 0, 0);
    root.add(board);
    if (!items[key]) continue;
    const item = await ctx.buildItem(items[key]);
    const fixed = new THREE.Group();
    fixed.rotation.y = Math.PI;
    fixed.add(item);
    const holder = new THREE.Group();
    holder.add(fixed);
    holder.scale.setScalar(0.5);
    holder.rotation.x = Math.PI / 2;
    holder.position.set(x, mc(3 / 16 + 0.02), 0);
    root.add(holder);
  }
  return { object: root, tick() {}, frame() {} };
}

// Parts the game draws in code rather than in the block model; the viewer adds them when a block is shown on its
// own (方块+物品 page, list icons). Bench legs: BenchLegsRenderer in game (splayed ~8°, models can't rotate that).
// 鸳鸯锅 / 田字锅（2026-10-23，mryt233）：锅身是 tools/gen_yuanyang.py 生成的模型；各边汤面照
// client/HotPotBlockEntityRenderer.renderSideSoups 一比一画：按 PotLayout.sideAt 的半像素格子分块、一行行合并，贴图按汤底名取
const YY_INNER = 13, YY_HALF = 0.5;
function yySide(shape, u, v) {   // = PotLayout.sideAt（模型坐标比例 0..1）；-1 = 隔板
  const x = u * YY_INNER, z = v * YY_INNER, c = YY_INNER / 2;
  if (shape === "split") return Math.abs(x - c) < YY_HALF ? -1 : x < c ? 0 : 1;
  if (shape === "four") {
    if (Math.abs(x - c) < YY_HALF || Math.abs(z - c) < YY_HALF) return -1;
    return z < c ? (x < c ? 0 : 1) : (x < c ? 3 : 2);
  }
  if (shape === "o") {
    const d = Math.hypot(x - c, z - c);
    return Math.abs(d - 3.4) < YY_HALF ? -1 : d < 3.4 ? 1 : 0;
  }
  if (shape === "square") {   // 方心鸳鸯锅 2026-10-25（= PotLayout.YUANYANG_SQUARE）
    const d = Math.max(Math.abs(x - c), Math.abs(z - c));
    return Math.abs(d - 3.0) < YY_HALF ? -1 : d < 3.0 ? 1 : 0;
  }
  const r = c / 2;
  const dUp = x >= c ? Math.abs(Math.hypot(x - c, z - r) - r) : Math.hypot(x - c, Math.min(Math.abs(z), Math.abs(z - c)));
  const dDn = x <= c ? Math.abs(Math.hypot(x - c, z - c - r) - r) : Math.hypot(x - c, Math.min(Math.abs(z - c), Math.abs(z - 2 * c)));
  if (Math.min(dUp, dDn) < YY_HALF) return -1;
  const left = z < c ? x < c + Math.sqrt(Math.max(0, r * r - (z - r) ** 2)) : x < c - Math.sqrt(Math.max(0, r * r - (z - c - r) ** 2));
  return left ? 0 : 1;
}
function yyRegions(shape, side) {   // = PotLayout.regions
  const n = YY_INNER * 2, out = [];
  for (let j = 0; j < n; j++) {
    let i = 0;
    while (i < n) {
      const s = yySide(shape, (i + 0.5) / n, (j + 0.5) / n);
      let k = i;
      while (k < n && yySide(shape, (k + 0.5) / n, (j + 0.5) / n) === s) k++;
      if (s === side) out.push([i / n, j / n, k / n, (j + 1) / n]);
      i = k;
    }
  }
  return out;
}
// = PotLayout 每个大格属于哪边（-1 不用）、在锅里画在哪（像素，锅腔 13）
const YY_LAYOUT = {
  s: { cells: [0, 0, 1, 0, -1, 1, 0, 1, 1], centers: [[2.6, 2.4], [7.8, 3.0], [11.1, 2.4], [2.4, 6.6], [6.5, 6.5], [10.6, 6.4], [1.9, 10.6], [5.2, 10.0], [10.4, 10.6]] },
  o: { cells: [0, 0, 0, 1, -1, 1, 0, 0, 0], centers: [[2.2, 2.2], [6.5, 1.6], [10.8, 2.2], [5.0, 6.5], [6.5, 6.5], [8.0, 6.5], [2.2, 10.8], [6.5, 11.4], [10.8, 10.8]] },
  square: { cells: [0, 0, 0, 1, -1, 1, 0, 0, 0], centers: [[2.2, 2.2], [6.5, 1.6], [10.8, 2.2], [5.0, 6.5], [6.5, 6.5], [8.0, 6.5], [2.2, 10.8], [6.5, 11.4], [10.8, 10.8]] },
  split: { cells: [0, -1, 1, 0, -1, 1, 0, -1, 1], centers: [[3.25, 2.2], [6.5, 2.2], [9.75, 2.2], [3.25, 6.5], [6.5, 6.5], [9.75, 6.5], [3.25, 10.8], [6.5, 10.8], [9.75, 10.8]] },
  four: { cells: [0, 0, 1, 3, -1, 1, 3, 2, 2], centers: [[1.9, 3.25], [4.6, 3.25], [9.75, 1.9], [3.25, 8.4], [6.5, 6.5], [9.75, 4.6], [3.25, 11.1], [8.4, 9.75], [11.1, 9.75]] },
};
const YY_SHAPES = [["s", "yuanyang_s", 2], ["o", "yuanyang_o", 2], ["square", "yuanyang_square", 2], ["split", "yuanyang_split", 2], ["four", "four_grid", 4]];
async function buildYuanyang(ctx, s) {
  const ns = ctx.modid;
  const root = new THREE.Group();
  const shapes = s.shape === "all" ? YY_SHAPES : YY_SHAPES.filter((x) => x[0] === s.shape);
  for (const [i, [shape, key, sides]] of shapes.entries()) {
    const x = (i - (shapes.length - 1) / 2) * 1.1;
    const pot = await ctx.buildModel(`${ns}:block/${s.pot}_${key}_hot_pot_shell`);
    pot.position.set(x, 0, 0);
    root.add(pot);
    const soups = [s.soupA, s.soupB, s.soupC, s.soupD];
    for (let side = 0; side < sides; side++) {
      if (!soups[side] || soups[side] === "none") continue;
      const mesh = await buildSoupMesh(ctx, ns, soups[side], s.fire === "on", 5.6 / 16, 1.5 / 16, 13 / 16, yyRegions(shape, side));
      const g = new THREE.Group();
      g.position.set(x, 0, 0);
      g.add(mesh);
      root.add(g);
    }
  }
  return { object: root, tick() {}, frame() {} };
}

export const blockExtras = [
  { match: ["*_bench"], build: (ctx, id, state) => benchLegs(ctx, id.replace(/_bench$/, ""), state) },
];

export default [
  {
    id:"table_chopsticks",name:"桌上竹筷 + 筷架",
    desc:"两根 8.5 像素的平行竹条，尾端贴桌，前端由横放的白瓷鞍形筷架托住；四向筷尖朝外，占地 9.5×4.5 像素（r146e）。",
    params:[WOOD,{key:"facing",label:"筷尖朝向",options:[["south","南"],["west","西"],["north","北"],["east","东"]],default:"south"},
      {key:"food",label:"夹菜",options:[["off","空筷子"],["on","熟牛肉"]],default:"off"}],
    build:buildTableChopsticks,
  },
  {
    id: "stove_pot",
    name: "灶台上煮火锅",
    desc: "锅坐在灶台上、灶膛点火：汤面大滚（沸腾贴图 + 立体气泡和滚包）、食材在汤面上下翻滚、热气上升、炉口冒火苗。「灶火」切到熄灭：冷锅里的食材浮在汤面上很慢地上下漂（约 6 秒一个来回），随火力平滑过渡",
    params: [POT, SHAPE, SOUP, SOUP2, FIRE, FOOD],
    build: (ctx, s) => buildScene(ctx, s, "stove"),
  },
  {
    id: "pot",
    name: "单独的锅",
    desc: "单独摆放的锅；下方有点着的灶台，或者点着的篝火 / 灵魂篝火（r102）时同样会沸腾、能煮。「灶火」开着时下面放一个点着的篝火，锅底四角有铁支架踩在篝火上（0.1.2-alpha 后修：以前看着浮在半空）。空手 Shift+右键整锅连汤带食材拿进背包",
    params: [POT, SHAPE, SOUP, SOUP2, FIRE, FOOD],
    // fire on = the pot sits on a lit campfire: HotPotBlock on_campfire → the *_on_campfire models add four iron legs down to
    // the campfire logs; the pot stays at its normal height so its bottom covers the 17 px campfire flames (2026-10-23)
    build: async (ctx, s) => {
      const built = await buildScene(ctx, s, "pot");
      if (s.fire !== "on") return built;
      const group = new THREE.Group();
      const fire = await ctx.buildBlock("minecraft:campfire", { lit: "true", facing: "north", signal_fire: "false", waterlogged: "false" });
      fire.position.y = -1;
      group.add(fire, built.object);
      return { ...built, object: group };
    },
  },
  {
    id: "table_ring",
    name: "3×3 火锅桌 + 条凳",
    desc: "灶台居中、八块火锅桌围一圈：桌面拼成一整张，桌身朝外的一侧收进去半格（像倒放的楼梯），留出放腿的空间",
    params: [WOOD, { key: "bench", label: "条凳", options: [["on", "围一圈"], ["off", "不放"]], default: "on" }, POT, SOUP, FIRE, FOOD],
    build: buildTableRing,
  },
  {
    id: "tall_ring",
    name: "高火锅桌 + 高灶台",
    desc: "1.5 格高的一桌：灶膛在最下面，锅和汤整体抬高 10 像素；汤面沸腾、食材翻滚、热气和锅边火星都和矮灶台一样（跟着抬高），炉口火苗还在下面。凳子默认一格高的条凳，也可以换成板凳对比",
    params: [WOOD, { key: "seat", label: "凳子", options: [["tall", "一格高条凳"], ["low", "板凳"], ["none", "不放"]], default: "tall" }, POT, SOUP, FIRE, FOOD],
    build: buildTallRing,
  },
  {
    id: "plates",
    name: "盘子",
    desc: "一格里能摆几个盘子：一个四格方盘 / 两个五格长盘 / 五个小碟 / 两个八格大盘 / 两个三格长盘 / 混着摆；每格放一样食物，摆在火锅桌上时贴着桌面。最后一排另摆了两个蘸料瓶（芝麻酱是带白标签的矮玻璃罐、蚝油是细高的深色瓶）和一只碗，它们和碗、盘子一样借这个「盘子格」摆，摆着就能给菜加料（r146a）",
    params: [
      {key:"chop",label:"筷子",options:[["none","不架"],["empty","架空筷子"],["food","架夹着菜的筷子"]],default:"none"},
      {key:"yaw",label:"筷尖角度",options:[["0","南 0°"],["22.5","南偏西 22.5°"],["90","西 90°"],["180","北 180°"],["270","东 270°"]],default:"0"},
      { key: "material", label: "材质", options: [["quartz", "石英"], ["bamboo", "竹编"], ["terracotta", "红陶"], ["copper", "铜"], ["iron", "铁"]], default: "quartz" },
      { key: "place", label: "摆在", options: [["table", "火锅桌上"], ["ground", "地上"]], default: "table" },
      { key: "fill", label: "食物", options: [["full", "装满（叠 3 层）"], ["half", "每格一样"]], default: "full" },
      { key: "stack", label: "叠法", options: [["shingle", "压半面（预览）"], ["pile", "正叠（游戏里现在的）"]], default: "shingle" },
      WOOD,
    ],
    build: buildPlates,
  },
  {
    id: "bowls",
    name: "碗",
    desc: "原版的碗像盘子一样摆在桌上（2×2 四张桌子，每张两只碗）：空碗、装 1 样、2 样、3 样菜（像小碟一样正叠、每层转 15°），然后是清汤、高汤、菌汤、红汤四碗汤。两种碗形对比着看",
    params: [
      { key: "shape", label: "碗形", options: [["b", "b 敞口碗（游戏里用的）"], ["a", "a 直壁碗（留档）"]], default: "b" },
      { key: "place", label: "摆在", options: [["table", "矮火锅桌上"], ["tall", "高火锅桌上"], ["ground", "地上"]], default: "table" },
      {key:"chop",label:"筷子",options:[["none","不架"],["empty","架空筷子"],["food","架夹着菜的筷子"]],default:"none"},
      {key:"yaw",label:"筷尖角度",options:[["0","南 0°"],["22.5","南偏西 22.5°"],["90","西 90°"],["180","北 180°"],["270","东 270°"]],default:"0"},
      WOOD,
    ],
    build: buildBowls,
  },
  {
    id: "drink",
    name: "饮料：橙子饮料 + 植物蛋白饮料",
    desc: "左边那张桌上：左边果橙多（橙子饮料）、右边植物蛋白饮料（圆瓶身、紫色标签、腰收进去一圈、宽瓶盖，大小同果橙多）。果橙多：2 升装：瓶身 6×5、总高 15 像素（现实 12×10×30 厘米的比例），腰部收进去一圈，红色名称牌在上半截、下面橙子图案、下半截防滑方格；右边那张桌子放一只碗和一个装了熟牛肉的小碟，用来比大小",
    params: [
      { key: "place", label: "摆在", options: [["table", "矮火锅桌上"], ["tall", "高火锅桌上"], ["ground", "地上"]], default: "table" },
      WOOD,
    ],
    build: buildDrink,
  },
  {
    id: "sauce_station",
    name: "蘸料台",
    desc: "阶梯式（2026-10-23 改）：1.5 格高，三层台阶前低后高，每层一横排 3 个小木碗；碗不固定，空碗放什么原料就装什么（「随便放」看重复和空碗）。11 种原版木头各一个（台身该木头的木板 + 白色石英台面）+ 铁台（原版铁块），4 × 3 排开，正面朝镜头。「九种各一碗」：味精 辣油 酸甜酱 / 蜂蜜 香油 醋 / 白糖 葱花 香菜碎末；「后六种 + 葱花香菜」：蒜泥 蚝油 芝麻酱 / 花生碎 小米辣 折耳根碎 / 葱花 香菜碎末 空",
    params: [{ key: "fill", label: "碗里", options: [["each", "九种各一碗"], ["extra", "后六种 + 葱花香菜"], ["mixed", "随便放（有重复、有空碗）"], ["empty", "空碗"]], default: "each" }],
    build: buildSauceStation,
  },
  {
    id: "cutting_board",
    name: "砧板",
    desc: "砧板（2026-10-23）：东西放上去平躺在板面中间。铁菜刀：生牛肉 / 生羊肉一刀切 3 卷，牛脊肉一刀切 3 片吊龙，土豆切三下每下一份土豆片，豆腐一刀切 3 块（切石机也能切）。擀面杖：生牛肉 / 猪排 / 羊肉 / 鳕鱼 / 鲑鱼捣三下——第 1 下捶碎、第 2 下肉浆、第 3 下掉 3 个生丸子。空手右键拿回来",
    params: [{ key: "show", label: "板上", options: [["all", "生牛肉 / 牛脊肉 / 土豆 / 豆腐 / 空"], ["pound", "捣丸子：生肉 → 捶碎 → 肉浆（牛）/ 鱼浆"], ["beef", "生牛肉"], ["mutton", "生羊肉"], ["loin", "牛脊肉"], ["tofu", "豆腐"], ["empty", "空"]], default: "all" }],
    build: buildCuttingBoard,
  },
  {
    id: "yuanyang",
    name: "鸳鸯锅 / 田字锅",
    desc: "2026-10-23：锅身同九宫格锅（同大小、同贴图），隔板换成太极（S 形）、圆心（中间一圈）、中间一道直隔板、田字十字。每边一种汤，任意汤底都能放任意一边（汤面由渲染器按贴图画）。S 形左右各 4 格、O 形圈外 6 圈里 2、直隔板左右各 3、田字每块 2 格。合成：同金属九宫格锅 + 同金属锭（锭在左 = S、锭在下 = O、锭在上 = 直隔板、锭在左右 = 田字）",
    params: [{ key: "shape", label: "形状", options: [["all", "五种并排"], ["s", "太极"], ["o", "圆心"], ["square", "方心"], ["split", "直隔板"], ["four", "田字"]], default: "all" },
      POT,
      { key: "soupA", label: "第 1 边（S 左 / O 圈外 / 直隔左 / 田字左上）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["other", "自定义汤底"], ["none", "没汤"]], default: "spicy" },
      { key: "soupB", label: "第 2 边（S 右 / O 圈里 / 直隔右 / 田字右上）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["other", "自定义汤底"], ["none", "没汤"]], default: "clear" },
      { key: "soupC", label: "第 3 边（田字右下）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["other", "自定义汤底"], ["none", "没汤"]], default: "mushroom" },
      { key: "soupD", label: "第 4 边（田字左下）", options: [["spicy", "红汤"], ["clear", "清汤"], ["rich_stock", "高汤"], ["mushroom", "菌汤"], ["tomato", "番茄"], ["three_fresh", "三鲜"], ["other", "自定义汤底"], ["none", "没汤"]], default: "rich_stock" },
      { key: "fire", label: "火", options: [["off", "冷锅"], ["on", "沸腾贴图"]], default: "off" }],
    build: buildYuanyang,
  },
  {
    id: "stove_fire",
    name: "空灶台烧火",
    desc: "没装锅的灶台：点火后炉口火光贴图和火苗粒子",
    params: [FIRE],
    build: (ctx, s) => buildScene(ctx, s, "stoveOnly"),
  },
];
