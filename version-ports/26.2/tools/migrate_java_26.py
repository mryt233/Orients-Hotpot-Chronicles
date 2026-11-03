"""作者：mryt233。一次性机械迁移入口，具体行为保留原实现。"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
HOT = ROOT / 'common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot'

def write(path, before, after):
    if before != after:
        if 'mryt233 改：26.1' not in after:
            after = '// mryt233 改：26.1 原版 API。\n' + after
        path.write_text(after, encoding='utf-8')

for module in ['framework', 'common', 'fabric', 'neoforge']:
    for path in (ROOT / module).rglob('*.java'):
        if 'build' in path.relative_to(ROOT).parts:
            continue
        before = path.read_text(encoding='utf-8')
        after = before.replace('GuiGraphics', 'GuiGraphicsExtractor').replace('.location()', '.identifier()')
        after = re.sub(r'(BuiltInRegistries\.(?:BLOCK|ITEM|ENTITY_TYPE))\.get\(', r'\1.getValue(', after)
        write(path, before, after)

# mryt233 改：工厂收到的 Properties 已包含注册键，构造器只追加原有属性。
for name in ['StoveBlock', 'StoveTopBlock', 'BenchBlock', 'HotPotTableBlock', 'TallHotPotTableBlock',
             'PlateBlock', 'BowlBlock', 'DrinkBlock', 'CupBlock', 'DrinkCauldronBlock',
             'PlateHolderBlock', 'SauceStationTopBlock', 'HotPotBlock']:
    path = HOT / (name + '.java')
    before = path.read_text(encoding='utf-8')
    after = re.sub(r'public ' + name + r'\(([^)]*)\)',
                   lambda m: 'public ' + name + '(' + m[1] + (', ' if m[1] else '')
                   + 'net.minecraft.world.level.block.state.BlockBehaviour.Properties properties)', before)
    after = after.replace('super(BlockBehaviour.Properties.of()', 'super(properties')
    if name == 'StoveBlock':
        after = after.replace('this(0);', 'this(0, properties);')
    elif name == 'BenchBlock':
        after = after.replace('this(planks, false);', 'this(planks, false, properties);')
    elif name == 'TallHotPotTableBlock':
        after = after.replace('super(planks);', 'super(planks, properties);')
    elif name in ['BowlBlock', 'DrinkBlock', 'CupBlock']:
        after = re.sub(r'(super\([^;]+)(\);)', r'\1, properties\2', after, count=1)
    elif name == 'DrinkCauldronBlock':
        after = after.replace('simpleCodec(s -> new DrinkCauldronBlock())', 'simpleCodec(DrinkCauldronBlock::new)')
        after = after.replace('super(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON), BEHAVIOR)', 'super(properties, BEHAVIOR)')
    elif name == 'HotPotBlock':
        after = after.replace('super(settingsFor(material));', 'super(settingsFor(material, properties));')
        after = after.replace('settingsFor(HotPotMaterial material)', 'settingsFor(HotPotMaterial material, BlockBehaviour.Properties properties)')
        after = after.replace('return BlockBehaviour.Properties.of()', 'return properties')
    write(path, before, after)

for name in ['SoupBowlItem', 'CupItem', 'EmptyBottleItem', 'DrinkItem', 'GuideBookItem', 'DippingSauceItem', 'ChopsticksItem']:
    path = HOT / (name + '.java')
    before = path.read_text(encoding='utf-8')
    after = re.sub(r'public ' + name + r'\(([^)]*)\)',
                   lambda m: 'public ' + name + '(' + m[1] + (', ' if m[1] else '') + 'Item.Properties properties)', before)
    after = after.replace('super(new Item.Properties()', 'super(properties')
    write(path, before, after)

path = HOT / 'HotPotContent.java'
before = path.read_text(encoding='utf-8')
pattern = re.compile(r'reg\(\)\.(items|blocks)\(\)\.register\(')
replacements = []
for match in pattern.finditer(before):
    start = match.end()
    depth = 0
    comma = None
    quote = False
    escape = False
    for i in range(start, len(before)):
        c = before[i]
        if quote:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == '"':
                quote = False
            continue
        if c == '"':
            quote = True
        elif c in '([{':
            depth += 1
        elif c == ')' and depth == 0:
            end = i
            break
        elif c in ')]}':
            depth -= 1
        elif c == ',' and depth == 0 and comma is None:
            comma = i
    expr = before[comma + 1:end].strip()
    if not expr.startswith('new '):
        continue
    if match[1] == 'items':
        if 'new Item.Properties()' in expr:
            expr = expr.replace('new Item.Properties()', 'properties')
        else:
            j = expr.rfind(')')
            expr = expr[:j] + (', ' if not expr[:j].endswith('(') else '') + 'properties' + expr[j:]
    else:
        if expr.startswith('new SauceStationBlock('):
            props = expr[len('new SauceStationBlock('):-1]
            replacements.append((comma + 1, end, ' ' + props + ', SauceStationBlock::new'))
            continue
        if expr.startswith('new DrinkCauldronBlock('):
            replacements.append((comma + 1, end, ' net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAULDRON), DrinkCauldronBlock::new'))
            continue
        if 'net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()' in expr:
            expr = expr.replace('net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()', 'properties')
        elif 'BlockBehaviour.Properties.of()' in expr:
            expr = expr.replace('BlockBehaviour.Properties.of()', 'properties')
        else:
            j = expr.rfind(')')
            expr = expr[:j] + (', ' if not expr[:j].endswith('(') else '') + 'properties' + expr[j:]
    replacements.append((comma + 1, end, ' properties -> ' + expr))
after = before
for start, end, replacement in reversed(replacements):
    after = after[:start] + replacement + after[end:]
write(path, before, after)
