// Animation scenes for the workspace mod viewer's 「动画」 tab.
// Web ports of everything the game animates at runtime, kept in sync with the Java:
//   floating food        ← client/HotPotBlockEntityRenderer.java
//   square bubbles       ← client/BoilingEffects.java
//   steam / stove flames ← hotpot/HotPotBlockEntity.clientTick
//   heat ramp            ← clientHeat += (target - clientHeat) * 0.08 per tick
// Animated block textures (boiling soup etc.) come from the models themselves.
// mryt233 改（2026-11-11）：Java 方块图集查询移至 client/RenderSprites；此处绘制参数不变。
// mryt233 改（2026-11-13）：支持 other 通用自定义汤底预览与沸腾配色兜底。
// mryt233 改（2026-11-16）：新增 Phase 4 三种备菜架（prep_racks）与统一火锅桌换材质（table_materials）场景。
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
  let prevHeat = heat;
  let bubbleT = s._bubbleT ?? 0;
  let prevBubbleT = bubbleT;
  let cycleAngle = s._cycleAngle ?? 0;
  let prevCycleAngle = cycleAngle;
  const cycleRate = () => (hasSoup ? COLD_OMEGA + (OMEGA - COLD_OMEGA) * Math.max(0, Math.min(1, heat)) : OMEGA);
  const target = () => (s.fire === "on" && hasSoup ? 1 : 0);
  const salt = 0x1d3; // a fixed "block position" seed

  return {
    object: root,
    // once per game tick (20/s)
    tick() {
      prevHeat = heat;
      prevBubbleT = bubbleT;
      prevCycleAngle = cycleAngle;
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
      const h = prevHeat + partial * (heat - prevHeat);
      const bt = prevBubbleT + partial * (bubbleT - prevBubbleT);
      const baseAngle = prevCycleAngle + partial * (cycleAngle - prevCycleAngle);
      if (hasSoup && !shape) boilingEffects(batch, s.soup, pot, h, bt, salt);
      if (hasSoup && shape) {
        for (let side = 0; side < shape[2]; side++) {
          boilingEffects(batch, sideSoups[side], pot, h, bt, salt + side * 97,   // = HotPotBlockEntityRenderer（2026-11-01 对齐，原来是 101）
            (x, z) => yySide(shape[0], (x - pot.innerMin) / pot.innerSize, (z - pot.innerMin) / pot.innerSize) === side);
        }
      }
      batch.end();
      for (const { cell, items } of cells) {
        const n = items.length;
        const col = cell % 3, row = Math.floor(cell / 3);
        const phase = cell * 1.37; // per big cell only, so swapping foods never jumps the rhythm
        const angle = baseAngle + phase;
        const cold = hasSoup ? 1 - h : 0;
        // a new food each bob, swapped at the low point (sin = -1) along the unified continuous angle
        const cycle = (angle - 1.5 * Math.PI) / (2 * Math.PI);
        const idx = ((Math.floor(cycle) % n) + n) % n;
        let dip = 0;
        if (n > 1) {
          const f = cycle - Math.floor(cycle);
          const edge = Math.min(f, 1 - f);
          if (edge < 0.15) dip = Math.pow(1 - edge / 0.15, 2);
        }
        const amp = hasSoup ? COLD_AMPLITUDE * cold + AMPLITUDE * h : 0;
        const yOffset = amp * Math.sin(angle);
        const drift = 0.006 * h * Math.cos(angle * 0.7);
        const tilt = (hasSoup ? 1.0 * cold + 3.5 * h : 0) * Math.sin(angle * 0.55);
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
      // r152（mryt233 改）：零穿插轻错层，与 Java PlateFoodLayout 完全一致
      const FOOD = 4, FLOOR_GAP = 0.02, PILE_STEP_SMALL = 0.22, PILE_STEP_STANDARD = 0.28;
      const SHINGLE_RISE = 0.50, CELL_STAGGER = 0.04, MAX_SLOPE_TAN = 0.025;
      const SMALL_DISH_FOOD = 3.0, SMALL_DISH_SPIN = -12;
      const smallDish = size === "1x1";
      const foodSize = smallDish ? SMALL_DISH_FOOD : FOOD;
      const thickness = foodSize / 16;
      const inner = { x0: 8 - innerL / 2 + (px - 8), x1: 8 + innerL / 2 + (px - 8) };
      const clampShingleX = (base, k, nInCell) => {
        const raw = base + (k - (nInCell - 1) / 2) * (FOOD / 2);
        return Math.min(inner.x1 - FOOD / 2, Math.max(inner.x0 + FOOD / 2, raw));
      };
      for (let i = 0; i < count; i++) {
        const c = i % cells, col = c % cols, row = Math.floor(c / cols), layer = Math.floor(i / cells);
        const baseCx = 8 - innerL / 2 + innerL / cols * (col + 0.5) + (px - 8);
        let cx = baseCx;
        const cz = 8 - innerW / 2 + innerW / rows * (row + 0.5) + (pz - 8);
        const item = await ctx.buildItem(PLATE_FOODS[food++ % PLATE_FOODS.length]);
        const fixed = new THREE.Group();
        fixed.rotation.y = Math.PI; // item/generated "fixed" display: rotation [0, 180, 0]
        fixed.add(item);
        const holder = new THREE.Group();
        holder.add(fixed);
        holder.rotation.order = "YXZ";
        holder.scale.setScalar(foodSize / 16);
        let y = 1 - drop + thickness / 2 + FLOOR_GAP;
        let tiltZ = 0;
        if (smallDish) {
          const spin = SMALL_DISH_SPIN * layer;
          holder.rotation.x = Math.PI / 2;
          holder.rotation.y = (spin * Math.PI) / 180;
          y += layer * PILE_STEP_SMALL;
        } else if (s.stack !== "shingle") {
          holder.rotation.x = Math.PI / 2;
          holder.rotation.y = (-15 * layer * Math.PI) / 180;
          y += layer * PILE_STEP_STANDARD;
        } else {
          const n = Math.min(layers, Math.ceil((count - c) / cells));
          cx = clampShingleX(baseCx, layer, n);
          const cellLevel = rows === 1 ? (col % 3) : ((col & 1) + 2 * (row & 1));
          y += cellLevel * CELL_STAGGER + layer * SHINGLE_RISE;
          if (layer > 0) {
            const prevCx = clampShingleX(baseCx, layer - 1, n);
            const actualShift = Math.max(0, cx - prevCx);
            tiltZ = -Math.atan(MAX_SLOPE_TAN * Math.min(1, actualShift / (FOOD / 2)));
          }
          holder.rotation.x = Math.PI / 2;
        }
        // tilt around the world z axis after the item is laid flat (holder lays it flat / spins it)
        const tilt = new THREE.Group();
        tilt.add(holder);
        tilt.rotation.z = tiltZ;
        tilt.position.set(bx + mc(cx / 16), mc(y / 16), bz + mc(cz / 16));
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
        holder.position.set(bx + ox / 16, mc((lift + 1 + 0.25 / 2 + 0.02 + layer * 0.28) / 16), bz);
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

// ---------------------------------------------------------------- Phase 4: 三种备菜架 & 统一火锅桌换材质 (mryt233)
const TOP_MAT_PRESETS = {
  default: { cap: `${NS}:block/mat_slot_top_cap`, side: `${NS}:block/mat_slot_top_side` },
  quartz: { cap: "minecraft:block/quartz_block_top", side: "minecraft:block/quartz_block_side" },
  deepslate: { cap: "minecraft:block/polished_deepslate", side: "minecraft:block/polished_deepslate" },
  copper: { cap: "minecraft:block/cut_copper", side: "minecraft:block/cut_copper" },
  cherry: { cap: "minecraft:block/cherry_planks", side: "minecraft:block/cherry_planks" },
};
const BASE_MAT_PRESETS = {
  default: { cap: `${NS}:block/mat_slot_base_cap`, side: `${NS}:block/mat_slot_base_side`, ring: `${NS}:block/mat_slot_tall_ring` },
  oak: { cap: "minecraft:block/oak_planks", side: "minecraft:block/oak_planks", ring: "minecraft:block/oak_planks" },
  blackstone: { cap: "minecraft:block/polished_blackstone", side: "minecraft:block/polished_blackstone", ring: "minecraft:block/polished_blackstone" },
  smooth_stone: { cap: "minecraft:block/smooth_stone", side: "minecraft:block/smooth_stone", ring: "minecraft:block/smooth_stone" },
  quartz: { cap: "minecraft:block/quartz_block_top", side: "minecraft:block/quartz_block_side", ring: "minecraft:block/quartz_block_side" },
};

async function buildSwappedModel(ctx, modelId, overrides) {
  const base = await ctx.resolveModel(modelId);
  return ctx.buildElements({ ...base, textures: { ...base.textures, ...overrides } });
}

async function addRackDish(ctx, parent, xPx, shelfTopPx, zPx, scale = 0.85) {
  const dish = await ctx.buildBlock("quartz_small_dish", { axis: "x" });
  dish.scale.setScalar(scale);
  dish.position.set(xPx * PX, shelfTopPx * PX - 0.5 * (1 - scale), zPx * PX);
  parent.add(dish);
}

async function addRackItem(ctx, parent, itemId, xPx, yPx, zPx, scale, hanging = false) {
  const item = await ctx.buildItem(itemId);
  item.scale.setScalar(scale);
  if (hanging) item.rotation.z = Math.PI;
  item.position.set(xPx * PX, mc(yPx * PX), zPx * PX);
  parent.add(item);
}

async function populateDesktopRack(ctx, parent) {
  const ns = ctx.modid;
  await addRackDish(ctx, parent, -3.0, 1.5, 2.15, 0.85);
  await addRackDish(ctx, parent, 3.0, 1.5, 2.15, 0.85);
  await addRackItem(ctx, parent, `${ns}:tripe`, -3.0, 8.5, -2.1, 0.28);
  await addRackItem(ctx, parent, `${ns}:lotus_root_slice`, 3.0, 8.5, -2.1, 0.28);
}

async function buildPrepRacks(ctx, s) {
  const ns = ctx.modid;
  const root = new THREE.Group();

  // 1. 多层落地菜架（6 格：底层放小碟，中上两层放备菜食材）
  const multi = new THREE.Group();
  multi.position.set(-1.25, 0, 0);
  multi.add(await ctx.buildBlock("multi_tier_prep_rack", { facing: "south", mount: "wall" }));
  await addRackDish(ctx, multi, -3.2, 1.5, 0, 0.88);
  await addRackDish(ctx, multi, 3.2, 1.5, 0, 0.88);
  await addRackItem(ctx, multi, `${ns}:fatty_beef_roll`, -3.2, 9.85, 1.2, 0.30);
  await addRackItem(ctx, multi, `${ns}:beef_loin_slice`, 3.2, 9.85, 1.2, 0.30);
  await addRackItem(ctx, multi, `${ns}:cold_tofu`, -3.2, 15.35, 1.2, 0.30);
  await addRackItem(ctx, multi, `${ns}:enoki_mushroom`, 3.2, 15.35, 1.2, 0.30);
  root.add(multi);

  // 2. 桌面备菜架（4 格：支持贴面抬高/下沉贴合矮桌与高桌）
  const desktopLift = s.table === "low" ? -5 * PX : s.table === "tall" ? 7 * PX : 0;
  if (s.table === "low") {
    const t = await ctx.buildBlock("hotpot_table", { north: "false", east: "false", south: "false", west: "false" });
    t.position.set(0, -1, 0);
    root.add(t);
  } else if (s.table === "tall") {
    const t = await ctx.buildBlock("tall_hotpot_table", { north: "false", east: "false", south: "false", west: "false", open: "false" });
    t.position.set(0, -1, 0);
    root.add(t);
  }
  const desktop = new THREE.Group();
  desktop.position.set(0, desktopLift, 0);
  desktop.add(await ctx.buildBlock("desktop_prep_rack", { facing: "south", mount: "wall" }));
  await populateDesktopRack(ctx, desktop);
  root.add(desktop);

  // 3. 挂架（4 格双态：wall 靠墙 / ceiling 吸顶，挂布围裙与厨具）
  const ceiling = s.mount === "ceiling";
  const hanging = new THREE.Group();
  hanging.position.set(1.25, 0, 0);
  hanging.add(await ctx.buildBlock("hanging_prep_rack", { facing: "south", mount: s.mount }));
  const hangItems = [`${ns}:cloth_apron`, `${ns}:blue_print_cloth_apron`, `${ns}:iron_kitchen_knife`, `${ns}:rolling_pin`];
  for (let i = 0; i < hangItems.length; i++) {
    await addRackItem(ctx, hanging, hangItems[i], -4.5 + i * 3.0, ceiling ? 6.4 : 7.8, ceiling ? 0.0 : -5.2, 0.30, true);
  }
  root.add(hanging);

  return { object: root, tick() {}, frame() {} };
}

async function buildTableMaterials(ctx, s) {
  const ns = ctx.modid;
  const root = new THREE.Group();
  const top = TOP_MAT_PRESETS[s.top] || TOP_MAT_PRESETS.default;
  const base = BASE_MAT_PRESETS[s.base] || BASE_MAT_PRESETS.default;
  const overrides = {
    particle: top.cap,
    top_cap: top.cap,
    top_side: top.side,
    base_cap: base.cap,
    base_side: base.side,
    tall_ring: base.ring,
  };

  const low = await buildSwappedModel(ctx, `${ns}:block/hotpot_table_e`, overrides);
  low.position.set(-0.5, 0, 0);
  root.add(low);

  const tallName = s.open === "true" ? "tall_hotpot_table_open_w" : "tall_hotpot_table_w";
  const tall = await buildSwappedModel(ctx, `${ns}:block/${tallName}`, overrides);
  tall.position.set(0.5, 0, 0);
  root.add(tall);

  const desktop = new THREE.Group();
  desktop.position.set(-0.5, 11 * PX, 0);
  desktop.add(await ctx.buildBlock("desktop_prep_rack", { facing: "south", mount: "wall" }));
  await populateDesktopRack(ctx, desktop);
  root.add(desktop);

  return { object: root, tick() {}, frame() {} };
}

// mryt233 改（2026-11-20）：采样位置由真实 Java 导出；这里定格首帧，不伪造后续世界碰撞。
async function buildPotBreakFrame(ctx, s) {
  if (!ctx.projectId) throw new Error("当前查看器缺少项目标识，请刷新查看器");
  const data = await fetch(`/api/file?p=${encodeURIComponent(ctx.projectId)}&f=viewer/pot_break_samples.json`, { cache: "no-store" }).then(r => r.json());
  const lift = s.height === "tall" ? data.tall_lift : 0;
  const block = lift ? "tall_brick_stove" : "brick_stove";
  const state = { lit: "false", facing: "north", pot: s.pot, soup: "none", pot_axis: s.axis };
  const root = new THREE.Group();
  root.add(await ctx.buildBlock(block, { ...state, pot: s.phase === "whole" ? s.pot : "none" }));
  if (s.phase === "whole") return { object: root, tick() {}, frame() {} };
  if (s.phase === "drop") {
    const item = await ctx.buildItem(`${NS}:${s.pot}_nine_grid_hotpot`);
    item.scale.setScalar(.25);
    item.position.set(0, mc(data.drop_y + lift), 0);
    root.add(item);
    return { object: root, tick() {}, frame() {} };
  }
  const samples = data[s.axis];
  const particles = new Particles(samples.length);
  const texture = await ctx.texture(`${NS}:block/hot_pot_${s.pot}_side`);
  for (const sprite of particles.pool) {
    const fragment = texture.clone();
    fragment.repeat.set(.25, .25);
    fragment.offset.set(rand() * .75, rand() * .75);
    fragment.needsUpdate = true;
    sprite.material.map = fragment;
  }
  for (const [x, y, z] of samples) particles.spawn({ x: mc(x), y: mc(y + lift), z: mc(z),
    vx: 0, vy: 0, vz: 0, life: 1, size0: .07, size1: .07, color: 0x999999 });
  root.add(particles);
  particles.draw(0);
  return { object: root, tick() {}, frame() { particles.draw(0); } };
}

// mryt233 改（2026-11-21）：新版小石磨仍限查看器；整像素模型和静态预览共用同一份尺寸。
async function buildStoneMillPreview(ctx, s) {
  const faces = (tex) => ({ north: { texture: tex }, south: { texture: tex }, west: { texture: tex }, east: { texture: tex }, up: { texture: tex }, down: { texture: tex } });
  const el = (from, to, tex) => ({ from, to, faces: faces(tex) });
  let textures = { stone: "minecraft:block/cobblestone", top: "minecraft:block/cobblestone", wood: `${NS}:block/stone_mill_wood` };
  let slot = { ring: [2.4, 3.4, 12.6, 13.6], floor: 5.8, seam: 9, mouth: [6, 10, 16] };

  let baseElements = [
    el([2.5, 0, 2.5], [4.5, 4, 4.5], "#wood"),
    el([11.5, 0, 2.5], [13.5, 4, 4.5], "#wood"),
    el([2.5, 0, 11.5], [4.5, 4, 13.5], "#wood"),
    el([11.5, 0, 11.5], [13.5, 4, 13.5], "#wood"),
    el([3.5, 2.2, 3.5], [12.5, 4, 12.5], "#wood"),
    el([1.4, 4.0, 1.4], [14.6, 5.8, 14.6], "#stone"),
    el([1.4, 5.8, 1.4], [14.6, 8.0, 2.4], "#stone"),
    el([1.4, 5.8, 2.4], [2.4, 8.0, 13.6], "#stone"),
    el([13.6, 5.8, 2.4], [14.6, 8.0, 13.6], "#stone"),
    el([1.4, 5.8, 13.6], [6.0, 8.0, 14.6], "#stone"),
    el([10.0, 5.8, 13.6], [14.6, 8.0, 14.6], "#stone"),
    el([3.4, 5.8, 3.4], [12.6, 9.0, 12.6], "#stone"),
    el([5.0, 5.0, 14.6], [11.0, 5.8, 16.0], "#stone"),
    el([5.0, 5.8, 14.6], [6.0, 7.6, 16.0], "#stone"),
    el([10.0, 5.8, 14.6], [11.0, 7.6, 16.0], "#stone"),
  ];
  const rim = [2.8, 3.5, 4.2, 11.8, 12.5, 13.2];
  const inset = [1.4, 0.7, 0.0, 0.7, 1.4];
  let topElements = [
    el([3.6, 9.0, 3.6], [12.4, 9.25, 12.4], "#top"),
    ...inset.map((d, i) => el([2.8 + d, 9.25, rim[i]], [13.2 - d, 12.8, rim[i + 1]], "#top")),
    el([5.2, 12.8, 5.2], [10.8, 13.6, 6.4], "#top"),
    el([5.2, 12.8, 9.6], [10.8, 13.6, 10.8], "#top"),
    el([5.2, 12.8, 6.4], [6.4, 13.6, 9.6], "#top"),
    el([9.6, 12.8, 6.4], [10.8, 13.6, 9.6], "#top"),
    el([10.6, 12.8, 7.1], [12.6, 13.5, 8.9], "#wood"),
    el([11.1, 13.5, 7.3], [12.4, 15.8, 8.7], "#wood"),
  ];

  const aligned = s.shape !== "fractional";
  if (aligned) {
    const data = await fetch(`/api/file?p=${encodeURIComponent(ctx.projectId)}&f=viewer/stone_mill_pixel_preview.json`, { cache: "no-store" }).then(r => {
      if (!r.ok) throw new Error(`无法读取小石磨整像素预览：${r.status}`);
      return r.json();
    });
    const fromRow = row => el(row.slice(0, 3), row.slice(3, 6), `#${row[6]}`);
    textures = data.textures;
    slot = data.slot;
    baseElements = data.base.map(fromRow);
    topElements = data.top.map(fromRow);
  }
  const { floor, seam } = slot;
  const [outerL, innerL, innerR, outerR] = slot.ring;
  const [mouthL, mouthR, mouthZ] = slot.mouth;
  const a = outerL + .03, b = innerL - .03, c = innerR + .03, d = outerR - .03;
  const lipFloor = floor + .02;
  const grainY = aligned ? seam + 4.4 : 13.15;

  const root = new THREE.Group();
  const mill = new THREE.Group();
  // 默认把 +Z 导流嘴转朝查看器前方，打开即可直视导流槽与磨盘动作
  mill.rotation.y = Math.PI;
  root.add(mill);

  const [base, top] = await Promise.all([
    ctx.buildElements({ textures, elements: baseElements }),
    ctx.buildElements({ textures, elements: topElements }),
  ]);
  mill.add(base, top);

  let grain = null;
  if (s.kind !== "water") {
    grain = await ctx.buildItem(s.kind === "dry" ? `${NS}:peanut` : `${NS}:soybean`);
    grain.scale.setScalar(0.24);
    grain.rotation.x = Math.PI / 2;
    grain.position.set(0, mc(grainY / 16), 0);
    top.add(grain);
  }

  const isLiquid = s.kind !== "dry";
  const isWater = s.kind === "water";
  const flowBands = [];
  const liquidGroup = new THREE.Group();
  mill.add(liquidGroup);

  let spoutLip = null;
  let fallColumn = null;
  let fallBridge = null;
  let puddle = null;
  let fallTex = null;
  let bridgeTex = null;
  let dryLayer = null;

  const cloneTex = (baseTex, repX, repY) => {
    const tex = baseTex.clone();
    tex.image = baseTex.image;
    tex.wrapS = THREE.RepeatWrapping;
    tex.wrapT = THREE.RepeatWrapping;
    tex.magFilter = THREE.NearestFilter;
    tex.minFilter = THREE.NearestFilter;
    tex.colorSpace = THREE.SRGBColorSpace;
    tex.repeat.set(repX, repY);
    tex.needsUpdate = true;
    return tex;
  };

  if (isLiquid) {
    const srcTex = await ctx.texture(isWater ? "minecraft:block/water_still" : `${NS}:block/soymilk_still`);
    const tint = isWater ? 0x3f76e4 : 0xffffff;
    const opacity = isWater ? 0.84 : 0.96;

    // 7 段零重叠液面，精确贴合四周环形集浆槽与前侧导流嘴通道，按真实汇流方向滚动 UV
    const segs = [
      { x0: a, x1: 8, z0: a, z1: b, vx: -0.24, vy: 0, tag: "ring" },
      { x0: 8, x1: d, z0: a, z1: b, vx: 0.24, vy: 0, tag: "ring" },
      { x0: a, x1: b, z0: b, z1: d, vx: 0, vy: 0.34, tag: "ring" },
      { x0: c, x1: d, z0: b, z1: d, vx: 0, vy: 0.34, tag: "ring" },
      { x0: b, x1: mouthL + .02, z0: c, z1: d, vx: 0.38, vy: 0, tag: "ring" },
      { x0: mouthR - .02, x1: c, z0: c, z1: d, vx: -0.38, vy: 0, tag: "ring" },
      { x0: mouthL + .02, x1: mouthR - .02, z0: c, z1: mouthZ - .04, vx: 0, vy: 0.56, tag: "spout" },
    ];
    for (const seg of segs) {
      const wPx = seg.x1 - seg.x0, dPx = seg.z1 - seg.z0;
      const tex = cloneTex(srcTex, wPx / 16, dPx / 16);
      const geo = new THREE.PlaneGeometry(wPx * PX, dPx * PX);
      geo.rotateX(-Math.PI / 2);
      const mat = new THREE.MeshBasicMaterial({ map: tex, color: tint, transparent: true, opacity, side: THREE.DoubleSide });
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(mc((seg.x0 + seg.x1) / 32), mc((floor + 0.8) / 16), mc((seg.z0 + seg.z1) / 32));
      liquidGroup.add(mesh);
      flowBands.push({ mesh, tex, vx: seg.vx, vy: seg.vy, tag: seg.tag, z0: seg.z0, z1: seg.z1 });
    }

    // 导流嘴出口液流断面（z = 15.96）
    const lipTex = cloneTex(srcTex, 3.96 / 16, 1 / 16);
    const lipGeo = new THREE.PlaneGeometry(3.96 * PX, 1.0 * PX);
    spoutLip = new THREE.Mesh(lipGeo, new THREE.MeshBasicMaterial({ map: lipTex, color: tint, transparent: true, opacity, side: THREE.DoubleSide }));
    spoutLip.position.set(0, mc((lipFloor + .38) / 16), mc((mouthZ - .04) / 16));
    liquidGroup.add(spoutLip);
    flowBands.push({ mesh: spoutLip, tex: lipTex, vx: 0, vy: 0.6, tag: "lip" });

    // 嘴口弯折过渡与向下垂落的像素液柱（在 z = 15.95..16.75 悬空垂落至 y = 0.02，不穿任何木腿）
    bridgeTex = cloneTex(srcTex, 2.2 / 16, 1.1 / 16);
    fallBridge = new THREE.Mesh(
      new THREE.BoxGeometry(2.2 * PX, 0.75 * PX, 1.05 * PX),
      new THREE.MeshBasicMaterial({ map: bridgeTex, color: tint, transparent: true, opacity }),
    );
    fallBridge.position.set(mc(8 / 16), mc((floor + 0.45) / 16), mc(16.12 / 16));
    liquidGroup.add(fallBridge);

    const fallH = floor + 0.4;
    fallTex = cloneTex(srcTex, 1.85 / 16, fallH / 16);
    fallColumn = new THREE.Mesh(
      new THREE.BoxGeometry(1.85 * PX, fallH * PX, 1.05 * PX),
      new THREE.MeshBasicMaterial({ map: fallTex, color: tint, transparent: true, opacity }),
    );
    fallColumn.position.set(mc(8 / 16), mc((fallH / 2) / 16), mc(16.38 / 16));
    liquidGroup.add(fallColumn);

    const puddleTex = cloneTex(srcTex, 4.2 / 16, 4.2 / 16);
    const puddleGeo = new THREE.PlaneGeometry(4.2 * PX, 4.2 * PX);
    puddleGeo.rotateX(-Math.PI / 2);
    puddle = new THREE.Mesh(puddleGeo, new THREE.MeshBasicMaterial({ map: puddleTex, color: tint, transparent: true, opacity: opacity * 0.78 }));
    puddle.position.set(mc(8 / 16), mc(0.05 / 16), mc(16.45 / 16));
    liquidGroup.add(puddle);
    flowBands.push({ mesh: puddle, tex: puddleTex, vx: 0, vy: 0.2, tag: "puddle" });
  } else {
    // 干磨槽底细碎花生粉层
    dryLayer = new THREE.Group();
    const powderMat = new THREE.MeshBasicMaterial({ color: 0xd9b36c, transparent: true, opacity: 0.94 });
    const parts = [
      [a, d, a, b], [a, b, b, d], [c, d, b, d],
      [b, c, c, d], [mouthL + .05, mouthR - .05, d, mouthZ - .15],
    ];
    for (const [x0, x1, z0, z1] of parts) {
      const m = new THREE.Mesh(new THREE.BoxGeometry((x1 - x0) * PX, 0.35 * PX, (z1 - z0) * PX), powderMat);
      m.position.set(mc((x0 + x1) / 32), mc((floor + .18) / 16), mc((z0 + z1) / 32));
      dryLayer.add(m);
    }
    mill.add(dryLayer);
  }

  const batch = new BoxBatch(400);
  const particles = new Particles(120);
  mill.add(batch, particles);

  const pxBox = (x0, y0, z0, x1, y1, z1, topCol, sideCol, alpha = 1) =>
    batch.box(mc(x0 / 16), mc(y0 / 16), mc(z0 / 16), mc(x1 / 16), mc(y1 / 16), mc(z1 / 16), topCol, sideCol, alpha);

  // 槽内波纹依次走侧槽、前槽和导流嘴，跟随所选模型的槽心。
  const sideTrack = (outerL + innerL) / 2;
  const frontTrack = (outerR + innerR) / 2;
  const channelPoint = (side, u) => {
    const x = side < 0 ? sideTrack : 16 - sideTrack;
    const endX = 8 + side * .6;
    if (u < .48) return [x, sideTrack + (frontTrack - sideTrack) * u / .48];
    if (u < .72) return [x + (endX - x) * (u - .48) / .24, frontTrack];
    return [endX - side * .25 * (u - .72) / .28, frontTrack + (mouthZ - .1 - frontTrack) * (u - .72) / .28];
  };
  const nearSide = innerL - .26, farSide = innerR + .26;
  const seamSites = [
    [5, farSide], [8, farSide], [11, farSide],
    [nearSide, 6.2], [nearSide, 10.2], [farSide, 6.2], [farSide, 10.2], [8, nearSide],
  ];
  let lastFlowT = 0;
  let flowPhase = 0;
  let lastFill = 1;
  let lastEffort = 0;

  return {
    object: root,
    tick() {
      // 20 TPS 粒子：磨缝细沫 + 导流嘴底部落点微型水花（干磨则只落颗粒）
      if (isLiquid) {
        if (lastEffort > 0.25 && rand() < 0.45) {
          const ang = rand() * Math.PI * 2;
          const r = 4.9 / 16;
          particles.spawn({
            x: mc(0.5 + Math.cos(ang) * r), y: mc((seam + .05) / 16), z: mc(0.5 + Math.sin(ang) * r),
            vx: Math.cos(ang) * 0.008, vy: 0.006, vz: Math.sin(ang) * 0.008,
            gravity: 0.0045, life: 10, size0: 0.032, size1: 0.014,
            color: isWater ? 0x99ccff : 0xfffaf0, opacity: 0.85,
          });
        }
        if (lastFill > 0.62 && rand() < 0.75) {
          const ang = rand() * Math.PI * 2;
          const spd = 0.012 + rand() * 0.014;
          particles.spawn({
            x: mc((8 + (rand() - 0.5) * 1.3) / 16), y: mc(0.25 / 16), z: mc((16.38 + (rand() - 0.5) * 0.7) / 16),
            vx: Math.cos(ang) * spd, vy: 0.022 + rand() * 0.016, vz: Math.sin(ang) * spd,
            gravity: 0.0042, life: 12, size0: 0.042, size1: 0.015,
            color: isWater ? 0xbce0ff : 0xfffdf6, color1: isWater ? 0x4b87f0 : 0xe5dac2, opacity: 0.9,
          });
        }
      } else if (lastEffort > 0.2 && rand() < 0.65) {
        const ang = rand() * Math.PI * 2;
        const r = 4.9 / 16;
        particles.spawn({
          x: mc(0.5 + Math.cos(ang) * r), y: mc((seam + .05) / 16), z: mc(0.5 + Math.sin(ang) * r),
          vx: Math.cos(ang) * 0.009, vy: 0.004, vz: Math.sin(ang) * 0.009,
          gravity: 0.005, life: 13, size0: 0.038, size1: 0.018, color: 0xe3be78, color1: 0xb88f46,
        });
      }
      particles.tick();
    },
    frame(t, partial) {
      const dt = Math.max(0, Math.min(0.1, t - lastFlowT));
      lastFlowT = t;

      // 上磨盘推磨节奏：每 1.05 秒发力顺时针推 1/4 圈（带加速发力与惯性微停）
      const turnRate = 0.95;
      const turnPos = t * turnRate;
      const step = Math.floor(turnPos);
      const u = frac(turnPos);
      const pushU = Math.min(1, u / 0.72);
      const eased = easeOutCubic(pushU);
      const effort = u < 0.72 ? Math.pow(1 - pushU, 1.5) : 0;
      lastEffort = effort;
      top.rotation.y = -(step + eased) * (Math.PI / 2);

      // 完整 4 圈蓄满循环或满槽持续流浆
      const cycle = frac(turnPos / 4.5);
      const fill = s.stage === "filling"
        ? (cycle < 0.84 ? Math.min(1, cycle / 0.72) : Math.max(0, 1 - (cycle - 0.84) / 0.16))
        : 1.0;
      lastFill = fill;

      if (grain) {
        const sink = s.stage === "filling" ? fill * 0.32 : frac(turnPos / 4) * 0.22;
        grain.position.y = mc((grainY - sink + Math.sin(t * 18) * 0.04 * effort) / 16);
      }

      const flowSpeed = (0.65 + 0.75 * effort) * (0.35 + 0.65 * fill);
      flowPhase += dt * flowSpeed;

      batch.begin();

      if (isLiquid) {
        const pulse = Math.sin(t * 7.5) * 0.05 + effort * 0.1;
        const liquidY = floor + .06 + fill * .96 + pulse * Math.min(1, fill * 2);
        const spoutOverflow = Math.max(0, Math.min(1, (fill - 0.58) / 0.24));

        for (const b of flowBands) {
          if (b.tag === "ring" || b.tag === "spout") {
            const visible = b.tag === "spout" ? fill > 0.22 : fill > 0.05;
            b.mesh.visible = visible;
            b.mesh.position.y = mc(liquidY / 16);
            if (b.tag === "spout") {
              const reach = Math.min(1, Math.max(0.15, (fill - 0.18) / 0.45));
              b.mesh.scale.z = reach;
              const len = (b.z1 - b.z0) * reach;
              b.mesh.position.z = mc((b.z0 + len / 2) / 16);
            }
          } else if (b.tag === "lip") {
            const lipH = Math.max(0.05, liquidY - lipFloor);
            b.mesh.visible = spoutOverflow > 0.05;
            b.mesh.scale.y = lipH;
            b.mesh.position.y = mc((lipFloor + lipH / 2) / 16);
          } else if (b.tag === "puddle") {
            b.mesh.visible = spoutOverflow > 0.1;
            const sP = 0.65 + 0.35 * spoutOverflow + Math.sin(t * 8) * 0.06;
            b.mesh.scale.set(sP, 1, sP);
          }
          // UV 滚动：平面绕 X 转 -90° 后，+Y 偏移对应向 +Z（导流嘴）流，-X 偏移对应向 +X 流
          b.tex.offset.x = frac(-b.vx * flowPhase);
          b.tex.offset.y = frac(b.vy * flowPhase);
          if (isWater) b.tex.needsUpdate = true;
        }

        // 垂落液柱与弯折嘴口
        const streamW = (0.45 + 0.55 * spoutOverflow) * (0.9 + 0.18 * effort + 0.08 * Math.sin(t * 14));
        fallBridge.visible = spoutOverflow > 0.08;
        fallBridge.scale.set(streamW, Math.max(0.25, (liquidY - lipFloor) / 0.9), 1);
        fallBridge.position.y = mc((liquidY - 0.38) / 16);
        bridgeTex.offset.y = frac(flowPhase * 0.9);
        if (isWater) bridgeTex.needsUpdate = true;

        const colTop = Math.max(0.4, liquidY - 0.25);
        fallColumn.visible = spoutOverflow > 0.1;
        fallColumn.scale.set(streamW, colTop / fallH, 0.72 + 0.28 * spoutOverflow);
        fallColumn.position.y = mc((colTop / 2 + 0.02) / 16);
        fallTex.offset.y = frac(flowPhase * 1.45);
        if (isWater) fallTex.needsUpdate = true;

        const topCol = isWater ? 0xb5e2ff : 0xfffef8;
        const sideCol = isWater ? 0x5c9cf5 : 0xe8ddc6;

        // 磨缝液滴沿承托石心外侧落入集浆槽。
        for (let i = 0; i < seamSites.length; i++) {
          const [sx, sz] = seamSites[i];
          const p = frac(t * (0.95 + 0.25 * hash(i, 7)) + i * 0.17);
          const yTop = seam + .02 - p * (seam + .02 - liquidY);
          const dropH = 0.55 + 0.35 * effort;
          const yBot = Math.max(liquidY - 0.08, yTop - dropH);
          const r = 0.24 + 0.08 * effort;
          pxBox(sx - r, yBot, sz - r, sx + r, yTop, sz + r, topCol, sideCol, (1 - 0.35 * p) * Math.min(1, fill * 3 + 0.35));
        }

        // 2. 环形集浆槽与导流嘴表面前进的像素浮沫/波纹小块，直观标示流向
        if (fill > 0.12) {
          for (const side of [-1, 1]) {
            for (let i = 0; i < 4; i++) {
              const p = frac(flowPhase * 0.26 + i * 0.25 + (side < 0 ? 0 : 0.12));
              if (p > 0.72 && fill < 0.35) continue;
              const [cx, cz] = channelPoint(side, p);
              const alongZ = p < 0.48 || p >= 0.72;
              const wx = alongZ ? 0.28 : 0.55;
              const wz = alongZ ? 0.58 : 0.28;
              pxBox(cx - wx, liquidY + 0.01, cz - wz, cx + wx, liquidY + 0.18, cz + wz, topCol, sideCol, 0.88);
            }
          }
        }

        // 3. 垂落液柱内加速下坠的亮色液滴珠
        if (spoutOverflow > 0.12) {
          for (let i = 0; i < 4; i++) {
            const p = frac(t * 1.45 + i * 0.25);
            const dy = colTop * (1 - p * p);
            const dx = 8.0 + (hash(i, 19) - 0.5) * 0.9;
            const dz = 16.38 + (hash(i, 23) - 0.5) * 0.35;
            pxBox(dx - 0.24, Math.max(0.05, dy - 0.55), dz - 0.24, dx + 0.24, dy + 0.15, dz + 0.24, topCol, sideCol, spoutOverflow);
          }
        }
      } else {
        // 干磨：花生碎粉层高度随进度累积，颗粒从磨缝簌簌落入槽中并少量从嘴口滑出
        const hScale = Math.max(0.15, fill);
        dryLayer.visible = fill > 0.06;
        dryLayer.scale.y = hScale;
        dryLayer.position.y = mc((floor + .18 * hScale) / 16) - mc((floor + .18) / 16) * hScale;

        for (let i = 0; i < 10; i++) {
          const p = frac(t * (1.2 + 0.4 * hash(i, 3)) + i * 0.13);
          const ang = top.rotation.y + i * 0.63;
          const r = 4.65 + p * 0.75;
          const gx = 8 + Math.cos(ang) * r;
          const gz = 8 + Math.sin(ang) * r;
          const gy = (seam + 0.05) - p * p * (seam - floor + 0.05);
          pxBox(gx - 0.2, gy, gz - 0.2, gx + 0.2, gy + 0.36, gz + 0.2, 0xe8c47a, 0xc2984f, 1 - 0.4 * p);
        }
        if (fill > 0.45) {
          for (let i = 0; i < 3; i++) {
            const p = frac(t * 0.85 + i * 0.33);
            const gx = 7.2 + i * 0.8;
            const gz = 14.0 + p * 2.2;
            const gy = gz < 15.95 ? (floor + 0.18) : (floor + 0.18) - Math.pow((gz - 15.95) * 2.8, 2) * 3.2;
            if (gy > 0.1) pxBox(gx - 0.22, gy, gz - 0.22, gx + 0.22, gy + 0.32, gz + 0.22, 0xe8c47a, 0xc2984f, 0.92);
          }
        }
      }

      batch.end();
      particles.draw(partial);
    },
  };
}

// mryt233 改：按 StoneMillRenderer / StoneMillMotion 重放已同步的游戏状态，支持 1.5 格支架紧邻大木桶自动流浆。
async function buildStoneMillGame(ctx, s) {
  const ns = ctx.modid;
  const root = new THREE.Group();
  root.rotation.y = Math.PI;
  const intoVat = s.kind === "vat";
  const [base, top, geometry, vatModel] = await Promise.all([
    ctx.buildModel(`${ns}:block/stone_mill_base`),
    ctx.buildModel(`${ns}:block/stone_mill_top`),
    fetch(`/api/file?p=${encodeURIComponent(ctx.projectId)}&f=viewer/stone_mill_pixel_preview.json`, { cache: "no-store" }).then(r => r.json()),
    intoVat ? ctx.buildModel(`${ns}:block/wooden_vat`) : Promise.resolve(null),
  ]);
  root.add(base, top);
  if (vatModel) {
    vatModel.position.set(0, 0, 1);
    root.add(vatModel);
    root.position.set(0, 0, 0.45);
  }
  const { ring: [outerL, innerL, innerR, outerR], floor, mouth: [mouthL, mouthR, mouthZ], seam } = geometry.slot;
  const wetRecipe = s.kind === "wet" || intoVat;
  const idle = s.kind === "idle";
  const a = outerL + .03, b = innerL - .03, c = innerR + .03, d = outerR - .03;
  const left = mouthL + .02, right = mouthR - .02;
  const sideTrack = (outerL + innerL) / 2, frontTrack = (outerR + innerR) / 2;
  const sites = [[5, innerR + .26], [10.5, innerR + .26], [innerL - .26, 7], [innerR + .26, 9], [8, innerL - .26]];
  const [grain, water, milk] = await Promise.all([
    idle ? null : ctx.buildItem(`${ns}:${wetRecipe ? "soybean" : "peanut"}`),
    idle || wetRecipe ? ctx.texture("minecraft:block/water_still") : null,
    wetRecipe ? ctx.texture(`${ns}:block/soymilk_still`) : null,
  ]);
  if (grain) {
    grain.scale.setScalar(.24);
    grain.rotation.x = Math.PI / 2;
    grain.position.set(0, mc((seam + 4.4) / 16), 0);
    top.add(grain);
  }
  const liquids = new THREE.Group();
  root.add(liquids);
  let vatLiq = null;
  if (intoVat && milk) {
    const vatGeo = new THREE.PlaneGeometry(11 * PX, 11 * PX);
    vatGeo.rotateX(-Math.PI / 2);
    vatLiq = new THREE.Mesh(vatGeo, new THREE.MeshBasicMaterial({
      map: milk.texture || milk, color: 0xf3ecd8, side: THREE.DoubleSide,
    }));
    vatLiq.position.set(0, mc(11.63 / 16), 1);
    vatLiq.visible = false;
    root.add(vatLiq);
  }
  const makeSurface = () => {
    const geo = new THREE.PlaneGeometry(1, 1);
    geo.rotateX(-Math.PI / 2);
    const mesh = new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ side: THREE.DoubleSide }));
    liquids.add(mesh);
    return mesh;
  };
  const surfaces = water ? Array.from({ length: 6 }, makeSurface) : [];
  const batch = new BoxBatch(42);
  root.add(batch);
  const px = n => mc(n / 16);
  const box = (x0, y0, z0, x1, y1, z1, color, alpha=1) => batch.box(px(x0), px(y0), px(z0), px(x1), px(y1), px(z1), color, color, alpha);
  const drawSurface = (m, x0, z0, x1, z1, y, isMilk) => {
    const uv = m.geometry.attributes.uv;
    uv.setXY(0, x0 / 16, 1 - z0 / 16); uv.setXY(1, x1 / 16, 1 - z0 / 16);
    uv.setXY(2, x0 / 16, 1 - z1 / 16); uv.setXY(3, x1 / 16, 1 - z1 / 16);
    uv.needsUpdate = true;
    const map = isMilk ? milk : water;
    if (m.material.map !== map) {
      m.material.map = map;
      m.material.transparent = !isMilk;
      m.material.opacity = isMilk ? 1 : .8;
      m.material.color.setHex(isMilk ? 0xf3ecd8 : 0x3f76e4);
      m.material.needsUpdate = true;
    }
    m.position.set(px((x0 + x1) / 2), px(y), px((z0 + z1) / 2));
    m.scale.set((x1 - x0) / 16, 1, (z1 - z0) / 16);
  };
  const unit = v => Math.max(0, Math.min(1, v));
  const events = [16, 34, 52, 70]; // 四次推磨，最后一次出浆，紧邻大木桶时直接越过桶沿倾泻入桶。
  return {
    object: root,
    tick() {},
    frame(t) {
      const k = (t * 20) % 128;
      const turns = idle ? 0 : events.filter(at => k >= at).length;
      const age = turns ? k - events[turns - 1] : Infinity;
      const finished = wetRecipe && turns === events.length;
      const pending = finished && (intoVat ? age < 20 : k < 106);
      const wet = wetRecipe && (!finished || pending) ? unit((age + 1) / 3) * unit((13 - age) / 5) : 0;
      const spill = pending ? unit((age - 3) / 3) * unit((20 - age) / 7) : 0;
      const angle = turns ? turns - 1 + easeOutCubic(unit(age / 9)) : 0;
      top.rotation.y = -angle * Math.PI / 2;
      if (grain) grain.visible = turns < events.length;
      if (vatLiq) vatLiq.visible = intoVat && k >= 74 && k < 114;
      const color = 0xf3ecd8;
      const y = floor + (pending ? .8 : .18) + wet * .18;
      if (water) {
        const isMilk = wet > 0 || pending;
        const reach = spill > .02 ? mouthZ : d + (pending ? .65 : wet * .5);
        [[a,a,d,b],[a,b,b,d],[c,b,d,d],[b,c,left,d],[right,c,c,d],[left,c,right,reach]]
          .forEach(([x0,z0,x1,z1], i) => drawSurface(surfaces[i], x0,z0,x1,z1,y,isMilk));
      }
      batch.begin();
      if (wet > .02) {
        sites.forEach(([x, z], i) => {
          const f = frac(age * .16 + i * .21), py = seam - f * (seam - y), r = .11 + .13 * wet;
          box(x-r, Math.max(y,py-.55), z-r, x+r, py, z+r, color);
        });
        for (const side of [-1, 1]) for (let i = 0; i < 3; i++) {
          const p = frac(age * .06 + i / 3 + side * .07), fromSide = p < .55;
          const x0 = side < 0 ? sideTrack : 16 - sideTrack, x1 = 8 + side * .55;
          const x = fromSide ? x0 : x0 + (x1 - x0) * (p - .55) / .45;
          const z = fromSide ? sideTrack + (frontTrack - sideTrack) * p / .55 : frontTrack;
          const w = fromSide ? .15 : .35, dz = fromSide ? .35 : .15;
          box(x-w, y+.025, z-dz, x+w, y+.05, z+dz, 0xffffff);
        }
      }
      if (spill > .02) {
        const w = .18 + .61 * spill;
        if (intoVat) {
          const jetBottom = Math.max(14.18, y - 0.35);
          box(8 - w, jetBottom, mouthZ - 0.15, 8 + w, y + 0.02, mouthZ + 3.15, color);
          box(8 - w * 0.82, 14.2 - 2.65 * spill, mouthZ + 2.65, 8 + w * 0.82, Math.max(14.22, y - 0.15), mouthZ + 3.15, color);
        } else {
          box(8-w, y-.23, mouthZ-.22, 8+w, y+.02, mouthZ+.36, color);
          box(8-w*.82, y-1.1-3.6*spill, mouthZ+.04, 8+w*.82, y-.16, mouthZ+.36, color);
        }
      }
      if (!idle && !wetRecipe && age < 9) {
        for (let i=0; i<6; i++) {
          const u = frac(age / 9 + i * .2), theta = i * Math.PI / 3;
          const x = 8 + Math.cos(theta) * (3.9 + u * 1.3), z = 8 + Math.sin(theta) * (3.9 + u * 1.3);
          const py = seam - u * 2.2;
          box(x-.22,py,z-.22,x+.22,py+.4,z+.22,0xd8b26e,1-u);
        }
      }
      batch.end();
    },
  };
}

export const blockExtras = [
  { match: ["*_bench"], build: (ctx, id, state) => benchLegs(ctx, id.replace(/_bench$/, ""), state) },
];

export default [
  {
    id: "stone_mill_preview",
    name: "小石磨磨制与液流（新版预览）",
    desc: "圆石不调色；可切换整像素与旧小数坐标，近看磨盘转角、进料口和导流槽；持续流展示不是游戏行为。",
    params: [
      { key: "shape", label: "模型轮廓", options: [["aligned", "整像素版"], ["fractional", "上一版对照"]], default: "aligned" },
      { key: "kind", label: "研磨状态", options: [["soymilk", "黄豆湿磨豆浆"], ["water", "清水测试导流"], ["dry", "花生干磨出粉"]], default: "soymilk" },
      { key: "stage", label: "演示阶段", options: [["flowing", "满槽连续流浆"], ["filling", "从空槽逐步蓄满"]], default: "flowing" },
    ],
    build: buildStoneMillPreview,
  },
  {
    id: "pot_break_frame", name: "拆锅粒子与掉落位置（首帧定格）",
    desc: "位置直接来自 Java 真实锅形状的破坏采样；可核对矮/高、X/Z 锅耳及铜铁材质。定格首帧和掉落生成点，不模拟之后的世界碰撞，也不代替游戏验收。",
    params: [POT,
      { key: "height", label: "灶台高度", options: [["low", "矮灶台"], ["tall", "高灶台"]], default: "tall" },
      { key: "axis", label: "锅耳方向", options: [["x", "东西"], ["z", "南北"]], default: "x" },
      { key: "phase", label: "核对内容", options: [["whole", "完整锅"], ["particles", "碎屑首帧"], ["drop", "掉落生成点"]], default: "particles" }],
    build: buildPotBreakFrame,
  },
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
  {
    id: "stone_mill",
    name: "小石磨研磨（游戏效果）",
    desc: "1.5 格高木架抬高出料口以齐平开口大木桶沿；每次成功推磨转四分之一圈，最后一次湿磨从导流嘴短暂出浆：紧挨大木桶时直接越过桶沿流进桶内，单摆则留槽等待空桶。干磨不出液。",
    params: [
      { key: "kind", label: "配方", options: [["vat", "紧挨大木桶自动接浆"], ["wet", "黄豆湿磨并接取"], ["dry", "花生干磨"], ["idle", "加水待机"]], default: "vat" },
    ],
    build: buildStoneMillGame,
  },
  {
    id: "wooden_vat",
    name: "开口大木桶 + 3D 木压板（Phase 3.2）",
    desc: "敞口箍铁木桶：支持「豆浆 + 卤水 + 3D 木压板下压成豆腐（冷）」；右侧并排展示独立 3D 木压板（八角倒角拼板 + 双反翘横档 + 拱形木提手，对应 WoodenVatRenderer）",
    params: [
      { key: "mode", label: "压制状态", options: [["pressing", "木压板压制动画中"], ["done", "压制完成"]], default: "pressing" },
      { key: "extra", label: "旁摆对照", options: [["both", "桶 + 独立 3D 木压板"], ["vat", "仅看木桶"], ["lid", "特写 3D 木压板"]], default: "both" },
    ],
    build: async (ctx, s) => {
      const ns = ctx.modid;
      const root = new THREE.Group();
      let lid = null;
      let liq = null;

      if (s.extra !== "lid") {
        const vat = await ctx.buildModel(`${ns}:block/wooden_vat`);
        root.add(vat);

        const tex = await ctx.texture(`${ns}:block/soymilk_still`);
        const liqGeo = new THREE.PlaneGeometry(11 * PX, 11 * PX);
        liqGeo.rotateX(-Math.PI / 2);
        const liqMat = new THREE.MeshBasicMaterial({
          map: tex.texture || tex,
          color: 0xfaf3df,
          transparent: false,
          opacity: 1,
          side: THREE.DoubleSide,
        });
        liq = new THREE.Mesh(liqGeo, liqMat);
        liq.position.set(0, mc((11.2 - 0.15) * PX), 0);
        root.add(liq);

        lid = await ctx.buildModel(`${ns}:block/wooden_vat_lid`);
        root.add(lid);
      }

      if (s.extra === "both" || s.extra === "lid") {
        const soloLid = await ctx.buildModel(`${ns}:block/wooden_vat_lid`);
        if (s.extra === "both") {
          soloLid.position.set(1.1, 0, 0);
        } else {
          soloLid.scale.setScalar(1.35);
        }
        root.add(soloLid);
      }
      if (s.extra === "both") {
        root.position.x = -0.45;
      }

      return {
        object: root,
        tick() {},
        frame(t) {
          const cycle = s.mode === "done" ? 1 : (Math.sin(t * 1.4 - Math.PI / 2) + 1) / 2;
          const baseLidY = 11.2 - cycle * 1.4;
          if (lid) {
            lid.position.y = baseLidY * PX;
          }
          if (liq) {
            liq.position.y = mc((baseLidY - 0.15) * PX);
          }
        },
      };
    },
  },
  {
    id: "orange_tree",
    name: "独立橙子树（1 格露底 + 3×3×2 树冠 + 顶叶）",
    desc: "对应 worldgen/configured_feature/orange_tree.json（2 格树干：底部露 1 格 + 3×3×2 紧凑树冠，顶上允许长出 1 格顶叶）；橙子树叶随原版流畅/高品质画质开关切换镂空与不透明。",
    params: [
      { key: "topLeaf", label: "顶部顶叶", options: [["on", "顶上带 1 格树叶（4 格高）"], ["off", "平顶 3×3×2（3 格高）"]], default: "on" },
      { key: "stage", label: "树叶阶段", options: [["mix", "自然花果混生（5:2:2:3）"], ["3", "满树熟橙 (stage 3)"], ["2", "青幼果期 (stage 2)"], ["1", "满树橘花 (stage 1)"], ["0", "纯绿叶 (stage 0)"]], default: "mix" },
    ],
    build: async (ctx, s) => {
      const ns = ctx.modid;
      const root = new THREE.Group();

      const grassProto = await ctx.buildModel("minecraft:block/moss_block");
      for (let gx = -2; gx <= 2; gx++) {
        for (let gz = -2; gz <= 2; gz++) {
          const g = grassProto.clone();
          g.position.set(gx, -1, gz);
          root.add(g);
        }
      }

      const logProto = await ctx.buildModel(`${ns}:block/orange_log`);
      for (let y = 0; y < 2; y++) {
        const log = logProto.clone();
        log.position.set(0, y, 0);
        root.add(log);
      }

      const leafProtos = await Promise.all([0, 1, 2, 3].map((st) => ctx.buildModel(`${ns}:block/orange_leaves_stage${st}`)));
      const pickStage = (x, y, z) => {
        if (s.stage !== "mix") return Number(s.stage);
        const r = Math.floor(hash(x * 13 + y * 29 + z * 43 + 7) * 12);
        return r < 5 ? 0 : (r < 7 ? 1 : (r < 9 ? 2 : 3));
      };

      // 对应 OrangeFoliagePlacer（radius=1, height=2, top_leaf_chance=0.65, upper_corner_skip_chance=0.45）
      for (let dx = -1; dx <= 1; dx++) {
        for (let dz = -1; dz <= 1; dz++) {
          if (dx === 0 && dz === 0) continue; // Y=1 中心保留树干保证不凋零
          const low = leafProtos[pickStage(dx, 1, dz)].clone();
          low.position.set(dx, 1, dz);
          root.add(low);
        }
      }
      for (let dx = -1; dx <= 1; dx++) {
        for (let dz = -1; dz <= 1; dz++) {
          if (Math.abs(dx) === 1 && Math.abs(dz) === 1 && hash(dx * 17 + dz * 31 + 3) < 0.45) continue;
          const high = leafProtos[pickStage(dx, 2, dz)].clone();
          high.position.set(dx, 2, dz);
          root.add(high);
        }
      }
      if (s.topLeaf !== "off") {
        const cap = leafProtos[pickStage(0, 3, 0)].clone();
        cap.position.set(0, 3, 0);
        root.add(cap);
      }

      const saplingPad = grassProto.clone();
      saplingPad.position.set(-3.2, -1, 1.2);
      root.add(saplingPad);
      const sapling = await ctx.buildModel(`${ns}:block/orange_sapling`);
      sapling.position.set(-3.2, 0, 1.2);
      root.add(sapling);

      const strippedPad = grassProto.clone();
      strippedPad.position.set(3.2, -1, 1.2);
      root.add(strippedPad);
      const stripped = await ctx.buildModel(`${ns}:block/stripped_orange_log`);
      stripped.position.set(3.2, 0, 1.2);
      root.add(stripped);

      root.position.y = -0.8;
      return {
        object: root,
        tick() {},
        frame() {},
      };
    },
  },
  {
    id: "prep_racks",
    name: "三种备菜架（Phase 4）",
    desc: "并排展示 6 格多层落地菜架（小碟 + 肥牛卷 / 吊龙 / 冷豆腐 / 金针菇）、4 格桌面备菜架（加入 table_decorations 自动贴面抬高/下沉贴桌）与 4 格双态挂架（wall 靠墙 / ceiling 吸顶，挂布围裙与铁菜刀 / 擀面杖；对应 PrepRackRenderer）。",
    params: [
      { key: "mount", label: "挂架形态", options: [["wall", "靠墙 wall"], ["ceiling", "吸顶 ceiling"]], default: "wall" },
      { key: "table", label: "桌面架摆在", options: [["low", "矮火锅桌上"], ["tall", "高火锅桌上"], ["ground", "地上"]], default: "low" },
    ],
    build: buildPrepRacks,
  },
  {
    id: "table_materials",
    name: "统一火锅桌分部位换材质（Phase 4）",
    desc: "展示 40→2 收束后的通用矮火锅桌与高火锅桌互相连接，支持桌面（上半）与底座（下半）分部位换材质预览，桌面备菜架自动贴合桌面。",
    params: [
      { key: "top", label: "桌面材质", options: [["default", "默认木板"], ["quartz", "石英块"], ["deepslate", "磨制深板岩"], ["copper", "切制铜块"], ["cherry", "樱花木板"]], default: "quartz" },
      { key: "base", label: "底座材质", options: [["default", "默认红砖"], ["oak", "橡木木板"], ["blackstone", "磨制黑石"], ["smooth_stone", "平滑石头"], ["quartz", "石英块"]], default: "blackstone" },
      { key: "open", label: "高桌朝灶口", options: [["false", "封闭底座"], ["true", "敞口（仅留上圈）"]], default: "false" },
    ],
    build: buildTableMaterials,
  },
];
