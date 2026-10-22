// Animation scenes for the workspace mod viewer's 「动画」 tab.
// Web ports of everything the game animates at runtime, kept in sync with the Java:
//   floating food        ← client/HotPotBlockEntityRenderer.java
//   square bubbles       ← client/BoilingEffects.java
//   steam / stove flames ← hotpot/HotPotBlockEntity.clientTick
//   heat ramp            ← clientHeat += (target - clientHeat) * 0.08 per tick
// Animated block textures (boiling soup etc.) come from the models themselves.
import * as THREE from "three";
import { BoxBatch, Particles, TexBoxBatch, hash, frac, easeOutCubic, easeInOutSine, mc, rand } from "/anim.js";

const NS = "orients_hotpot_chronicles";
const PX = 1 / 16;

// ---------------------------------------------------------------- shared constants (HotPotBlockEntityRenderer)

const STOVE = { innerMin: 1.837 / 16, innerSize: 12.326 / 16, surface: 14.32 / 16 };
const STANDALONE = { innerMin: 1.5 / 16, innerSize: 13 / 16, surface: 5.6 / 16 };
const OMEGA = 0.22, AMPLITUDE = 0.022, FOOD_SCALE = 0.24;
const FOODS = ["minecraft:beef", "minecraft:potato", "minecraft:cod", "minecraft:kelp", "minecraft:mutton",
  "minecraft:chicken", "minecraft:carrot", "minecraft:salmon", "minecraft:beef"];

const PALETTES = {
  clear: { body: 0xd6c79a, hi: 0xfff6de, shadow: 0x9c8c5c },
  milky: { body: 0xf2e7d2, hi: 0xffffff, shadow: 0xc4b699 },
  mushroom: { body: 0x9a7048, hi: 0xe8c88a, shadow: 0x5a3e24 },
  spicy: { body: 0xbe3325, hi: 0xd8553a, shadow: 0x7e1c16 },
};

// ---------------------------------------------------------------- BoilingEffects port

// Square bubbles: a cube swells out of the soup, floats up, splits from the middle into four
// smaller cubes that fly apart, then they drop back and sink, leaving a faint ripple.
// All pure functions of time (same hash / easing as the Java side).
function boilingEffects(batch, soup, pot, heat, t, salt) {
  if (heat < 0.05) return;
  const pal = PALETTES[soup];
  const { innerMin, innerSize } = pot;
  const y = pot.surface + 0.002;
  const box = (x0, y0, z0, x1, y1, z1, top, side, alpha = 1) => batch.box(mc(x0), mc(y0), mc(z0), mc(x1), mc(y1), mc(z1), top, side, alpha);
  const cube = (x, yb, z, s, alpha = 1) => box(x - s / 2, yb, z - s / 2, x + s / 2, yb + s, z + s / 2, pal.hi, pal.body, alpha);

  // Timing follows real boiling, slowed to be readable: a site sends up a bubble every fraction
  // of a second, the cap ruptures almost instantly, and the bits drop back fast.
  const n = Math.round(3 + 5 * heat);
  for (let i = 0; i < n; i++) {
    const period = (0.7 + hash(i, 21) * 0.45) / (0.7 + 0.5 * heat); // ~0.6-1.0 s at full heat
    const off = hash(i, 22 + salt);
    const cyc = Math.floor(t / period + off);
    const u = frac(t / period + off);
    // a new spot each round, anywhere in the pot
    const x0 = innerMin + innerSize * hash(i, cyc * 2 + salt);
    const z0 = innerMin + innerSize * hash(i, cyc * 2 + 1 + salt);
    // smaller bubbles toward the pot wall: full size 3 px in from the edge, ~1/3 at the edge
    const edge = Math.min(x0 - innerMin, innerMin + innerSize - x0, z0 - innerMin, innerMin + innerSize - z0) / PX;
    const near = Math.max(0.35, Math.min(1, 0.35 + (0.65 * edge) / 3));
    const sMax = PX * (0.7 + 0.6 * hash(i, cyc * 5 + salt)) * (0.85 + 0.15 * heat) * near; // ~0.25..1.3 px
    // keep the bubble and its split pieces clear of the wall
    const keep = sMax * 1.3 + 0.3 * PX;
    const x = Math.min(innerMin + innerSize - keep, Math.max(innerMin + keep, x0));
    const z = Math.min(innerMin + innerSize - keep, Math.max(innerMin + keep, z0));
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
  if (kind === "stove") state = { lit: String(lit), facing: "north", pot: `${s.pot}_soup`, soup: s.soup, pot_axis: "x" };
  // an empty stove in game is pot=none, soup=none (StoveBlock default state), so no soup is drawn
  else if (kind === "stoveOnly") state = { lit: String(lit), facing: "north", pot: "none", soup: "none", pot_axis: "x" };
  else state = { soup: s.soup, boiling: String(lit), axis: "x" };
  const blockId = kind === "pot" ? `${s.pot}_hot_pot` : "stove";
  root.add(await ctx.buildBlock(blockId, state));

  const pot = kind === "stove" ? STOVE : STANDALONE;
  const hasSoup = kind !== "stoveOnly";
  const batch = new BoxBatch();
  const particles = new Particles();
  root.add(batch, particles);

  // floating food: each big cell holds 1-3 foods (sample) and shows them in turn, like visibleSlots()
  const cells = [];
  if (hasSoup && s.food === "on") {
    for (let cell = 0; cell < 9; cell++) {
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
  const target = () => (s.fire === "on" && hasSoup ? 1 : 0);
  const salt = 0x1d3; // a fixed "block position" seed

  return {
    object: root,
    // once per game tick (20/s)
    tick() {
      heat += (target() - heat) * 0.08;
      s._heat = heat;
      if (kind !== "pot" && s.fire === "on" && rand() < 0.5) {
        // HotPotBlockEntity: SMALL_FLAME at the stove mouth (facing north)
        const side = (rand() - 0.5) * 0.36;
        const life = Math.floor(8 / (rand() * 0.8 + 0.2)) + 4;
        particles.spawn({ x: mc(0.5 - side), y: mc(0.27), z: mc(0.07), vx: 0, vy: 0.035, vz: 0, life, size0: 0.06, size1: 0.01, color: 0xffd060, color1: 0xff5a20 });
      }
      // sparks from the gap between pot and stove collar on all four sides (or the open socket)
      if (kind !== "pot" && s.fire === "on" && rand() < 0.35) {
        let x, z, y;
        if (kind === "stove") {
          const gap = 1.125 / 16, along = (1 + rand() * 14) / 16;
          const side = Math.floor(rand() * 4);
          [x, z] = side === 0 ? [along, gap] : side === 1 ? [along, 1 - gap] : side === 2 ? [gap, along] : [1 - gap, along];
          y = 11 / 16 + 0.01;
        } else {
          x = (2 + rand() * 12) / 16; z = (2 + rand() * 12) / 16; y = 8 / 16;
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
      if (hasSoup) boilingEffects(batch, s.soup, pot, heat, ticks / 20, salt);
      batch.end();
      for (const { cell, items } of cells) {
        const n = items.length;
        const col = cell % 3, row = Math.floor(cell / 3);
        const phase = cell * 1.37; // per big cell only, so swapping foods never jumps the rhythm
        const angle = OMEGA * ticks + phase;
        // a new food each bob, swapped at the low point (sin = -1)
        const cycle = (angle - 1.5 * Math.PI) / (2 * Math.PI);
        const idx = ((Math.floor(cycle) % n) + n) % n;
        let dip = 0;
        if (n > 1) {
          const f = cycle - Math.floor(cycle);
          const edge = Math.min(f, 1 - f);
          if (edge < 0.15) dip = Math.pow(1 - edge / 0.15, 2);
        }
        const yOffset = AMPLITUDE * heat * Math.sin(angle);
        const drift = 0.006 * heat * Math.cos(OMEGA * 0.7 * ticks + phase);
        const tilt = 3.5 * heat * Math.sin(OMEGA * 0.55 * ticks + phase);
        const x = pot.innerMin + (pot.innerSize * (col + 0.5)) / 3 + drift;
        const z = pot.innerMin + (pot.innerSize * (row + 0.5)) / 3 - drift;
        items.forEach((it, j) => {
          it.holder.visible = j === idx;
          if (j !== idx) return;
          it.holder.position.set(mc(x), mc(pot.surface - 0.007 - 0.03 * dip + yOffset), mc(z));
          it.holder.rotation.set(((90 + tilt) * Math.PI) / 180, (((it.slot * 47) % 360) * Math.PI) / 180, ((tilt * 0.55) * Math.PI) / 180);
          it.holder.scale.setScalar(FOOD_SCALE * (1 - 0.65 * dip));
        });
      }
      particles.draw(partial);
    },
  };
}

const POT = { key: "pot", label: "锅", options: [["copper", "铜锅"], ["iron", "铁锅"]], default: "copper" };
const SOUP = { key: "soup", label: "汤底", options: [["spicy", "红汤"], ["clear", "清汤"], ["milky", "浓汤"], ["mushroom", "菌汤"]], default: "spicy" };
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
        const bench = await ctx.buildBlock(`${s.wood}_bench`, state);
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
      const leg = new TexBoxBatch(tex.texture || tex, 1);
      const p = (v) => v / 16;
      // leg hangs down from its pivot at the top centre, then tilts outwards
      leg.box(p(0), p(-len), p(-LEG_T / 2), p(LEG_W), p(0), p(LEG_T / 2),
        [0, 1, LEG_W / 16, 1 - LEG_T / 16], [0, 1, LEG_W / 16, 1 - len / 16]);
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

// Preview only (not in the game yet): 1.5-block table, 1.5-block stove (firebox still at the bottom,
// everything above lifted 8 px) and the one-block-high bench. Static models; no BER food / bubbles.
async function buildTallRing(ctx, s) {
  const root = new THREE.Group();
  const lit = s.fire === "on";
  root.add(await ctx.buildBlock("tall_stove", { lit: String(lit), facing: "north", pot: `${s.pot}_soup`, soup: s.soup, pot_axis: "x" }));
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
    const id = s.seat === "tall" ? `${s.wood}_tall_bench` : `${s.wood}_bench`;
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
  return { object: root, tick() {}, frame() {} };
}

// Plates (PlateHolderRenderer): several plates share one block, each drawn with its own model shifted
// to its centre; one food per cell lying flat, scaled to the cell's short side (PlateSize.cellCenter).
// Sizes in pixels, long side along x. On a hotpot table the whole layer drops 5 px onto the table top.
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
      const plate = await ctx.buildBlock(`${s.material}_plate_${size}`, { axis: "x" });
      plate.position.set(bx + (px - 8) / 16, -drop / 16, bz + (pz - 8) / 16);
      root.add(plate);
      const innerL = len - 1, innerW = wid - 1;
      // r89: every plate shows food the same size (4 px) and each cell holds up to 3
      // r90 preview: other plates shingle (each next one half-overlaps the previous, sloping DOWN to the plate
      // floor at its free end, kept inside the rim); the 1x1 dish piles straight, each one turned 15° clockwise
      const cells = cols * rows, layers = 3;
      const count = s.fill === "full" ? cells * layers : cells;
      // RISE: how much higher each next item sits. Item models are 1/16 thick → 0.25 px at this size; a smaller
      // rise lets neighbours sink into each other a little (never all the way through), which reads right from
      // every angle and keeps the slope gentle (r90 user feedback: "翘得太高")
      const FOOD = 4, RISE = 0.15;
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
        holder.scale.setScalar(FOOD / 16);
        let y = 1 - drop + 0.002;
        let tiltZ = 0;
        if (size === "1x1" || s.stack !== "shingle") {
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
  return { object: root, tick() {}, frame() {} };
}

export default [
  {
    id: "stove_pot",
    name: "灶台上煮火锅",
    desc: "锅坐在灶台上、灶膛点火：汤面大滚（沸腾贴图 + 立体气泡和滚包）、食材在汤面漂浮、热气上升、炉口冒火苗",
    params: [POT, SOUP, FIRE, FOOD],
    build: (ctx, s) => buildScene(ctx, s, "stove"),
  },
  {
    id: "pot",
    name: "单独的锅",
    desc: "单独摆放的锅；下方有点着的灶台时同样会沸腾（这里用「灶火」开关模拟）",
    params: [POT, SOUP, FIRE, FOOD],
    build: (ctx, s) => buildScene(ctx, s, "pot"),
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
    name: "预览：1.5 格高的桌子和灶台",
    desc: "还没进游戏，先看比例：桌面 1.5 格高，灶台砖面比桌面低 2 像素、锅沿露出桌面 3 像素，灶膛还在最下面；条凳一格高，也可以换成现在的板凳对比",
    params: [WOOD, { key: "seat", label: "凳子", options: [["tall", "一格高条凳"], ["low", "现在的板凳"], ["none", "不放"]], default: "tall" }, POT, SOUP, FIRE],
    build: buildTallRing,
  },
  {
    id: "plates",
    name: "盘子",
    desc: "一格里能摆几个盘子：一个四格方盘 / 两个五格长盘 / 五个小碟 / 两个八格大盘 / 两个三格长盘 / 混着摆；每格放一样食物，摆在火锅桌上时贴着桌面",
    params: [
      { key: "material", label: "材质", options: [["quartz", "石英"], ["bamboo", "竹编"], ["terracotta", "红陶"], ["copper", "铜"], ["iron", "铁"]], default: "quartz" },
      { key: "place", label: "摆在", options: [["table", "火锅桌上"], ["ground", "地上"]], default: "table" },
      { key: "fill", label: "食物", options: [["full", "装满（叠 3 层）"], ["half", "每格一样"]], default: "full" },
      { key: "stack", label: "叠法", options: [["shingle", "压半面（预览）"], ["pile", "正叠（游戏里现在的）"]], default: "shingle" },
      WOOD,
    ],
    build: buildPlates,
  },
  {
    id: "stove_fire",
    name: "空灶台烧火",
    desc: "没装锅的灶台：点火后炉口火光贴图和火苗粒子",
    params: [FIRE],
    build: (ctx, s) => buildScene(ctx, s, "stoveOnly"),
  },
];
