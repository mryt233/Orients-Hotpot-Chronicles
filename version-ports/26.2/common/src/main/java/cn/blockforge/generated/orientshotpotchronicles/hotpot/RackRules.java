// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.network.HotPotNetwork;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRuleDef.Category;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRuleDef.Layout;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRuleDef.Patch;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 服务端事务编译标签，客户端只消费服务端映射。独立快照避免单人退出再进串档。 */
public final class RackRules {
    public static final int MAX_WIRE_BYTES = 256 * 1024;
    private static final int MAX_FILES = 512, MAX_FILE_BYTES = 8192, MAX_ITEMS = 4096;
    private static final FileToIdConverter FILES = FileToIdConverter.json("prep_rack_rules");
    private static final Map<Identifier,RackRuleDef> REGISTERED = new LinkedHashMap<>();
    private static final Comparator<RackRuleDef> ORDER = Comparator
        .comparing(RackRuleDef::target).thenComparing(Comparator.comparingInt(RackRuleDef::priority).reversed())
        .thenComparing(RackRuleDef::id);
    private static final Snapshot EMPTY = new Snapshot(0, List.of(), Map.of(), Map.of());
    private static volatile Snapshot server = EMPTY, client = EMPTY;
    private static volatile String wire = EMPTY.toJson();
    private static volatile MinecraftServer owner;
    private static boolean frozen;
    private static long revision;
    private RackRules() {}

    static synchronized void register(RackRuleDef def) {
        if (frozen) throw new IllegalStateException("prep rack rule registry frozen");
        if (REGISTERED.size() >= MAX_FILES || REGISTERED.putIfAbsent(def.id(),def) != null) {
            throw new IllegalArgumentException("duplicate or too many rack rules: " + def.id());
        }
    }

    static synchronized void freeze() { frozen = true; }
    static synchronized boolean frozen() { return frozen; }

    static void start(MinecraftServer instance) {
        owner = instance;
        server = EMPTY;
        wire = EMPTY.toJson();
        reload(instance);
    }

    static void stop(MinecraftServer instance) {
        if (owner == instance) {
            owner = null;
            server = EMPTY;
            wire = EMPTY.toJson();
        }
    }

    /** 只在原版成功提交资源和标签后执行；所有失败路径均保留上一整代。 */
    public static void reload(MinecraftServer instance) {
        if (instance != owner) return;
        try {
            var rules = new LinkedHashMap<>(REGISTERED);
            var resources = FILES.listMatchingResources(instance.getResourceManager());
            if (resources.size() > MAX_FILES) throw new IllegalArgumentException("too many rack rule files");
            int bytes = 0;
            for (var e : resources.entrySet()) {
                var id = FILES.fileToId(e.getKey());
                try (var stream = e.getValue().open()) {
                    var data = stream.readNBytes(MAX_FILE_BYTES+1);
                    bytes += data.length;
                    if (data.length > MAX_FILE_BYTES || bytes > MAX_WIRE_BYTES) throw new IllegalArgumentException("input too large");
                    rules.put(id, RackRuleDef.parse(id, JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject()));
                } catch (IOException | RuntimeException error) {
                    throw new IllegalArgumentException(id + ": " + error.getMessage(), error);
                }
            }
            Snapshot candidate = compile(revision+1, new ArrayList<>(rules.values()), BuiltInRegistries.ITEM::containsKey,
                id -> BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id))
                    .stream().flatMap(set -> set.stream()).map(Holder::value).map(BuiltInRegistries.ITEM::getKey)
                    .limit(MAX_ITEMS+1L).toList());
            String encoded = candidate.toJson();
            server = candidate;
            wire = encoded;
            revision = candidate.revision;
        } catch (RuntimeException error) {
            HotPot.LOGGER.warn("[hotpot] prep rack rules kept at {}: {}", server.revision, error.getMessage());
            return;
        }
        HotPotNetwork.broadcastRackRules(instance);
    }

    static Snapshot compile(long generation, List<RackRuleDef> input, Predicate<Identifier> registered,
                            Function<Identifier,List<Identifier>> tagItems) {
        if (input.size() > MAX_FILES) throw new IllegalArgumentException("too many rack rules");
        var profiles = new ArrayList<List<Patch>>();
        var byItem = new TreeMap<Identifier,Integer>();
        var byCategory = new EnumMap<Category,Integer>(Category.class);
        var winners = new TreeMap<Identifier,RackRuleDef>();
        for (var r : input.stream().sorted(ORDER).toList()) {
            int next = profiles.size();
            var patches = new ArrayList<Patch>();
            for (var kind : PrepRackBlock.Kind.values()) patches.add(r.base().overlay(r.overrides().get(kind)));
            profiles.add(List.copyOf(patches));
            switch (r.target()) {
                case ITEM -> {
                    var id = Identifier.parse(r.match());
                    if (registered.test(id)) index(r,next,id,byItem,winners);
                }
                case TAG -> {
                    var ids = tagItems.apply(Identifier.parse(r.match()));
                    if (ids.size() > MAX_ITEMS) throw new IllegalArgumentException("oversized rack tag: " + r.match());
                    for (var id : ids) index(r,next,id,byItem,winners);
                }
                case CATEGORY -> byCategory.putIfAbsent(Category.valueOf(r.match()),next);
            }
            if (byItem.size() > MAX_ITEMS) throw new IllegalArgumentException("too many resolved rack items");
        }
        return new Snapshot(generation, List.copyOf(profiles), Map.copyOf(byItem), Map.copyOf(byCategory));
    }

    private static void index(RackRuleDef rule, int next, Identifier id,
                              Map<Identifier,Integer> out, Map<Identifier,RackRuleDef> winners) {
        RackRuleDef first = winners.putIfAbsent(id,rule);
        if (first == null) out.put(id,next);
        else if (first.target() == rule.target() && first.priority() == rule.priority()) {
            // 仅匹配少量 ID 时逐个提示会在大标签上刷屏，记录冲突规则的首个匹配。
            if (id.equals(Identifier.tryParse(rule.match()))) {
                HotPot.LOGGER.warn("[hotpot] prep rack rules tie: {} < {}", first.id(),rule.id());
            }
        }
    }

    record Snapshot(long revision, List<List<Patch>> profiles, Map<Identifier,Integer> byItem,
                    Map<Category,Integer> byCategory) {
        Patch resolve(Identifier item, Category category, PrepRackBlock.Kind kind) {
            Integer i = byItem.get(item);
            if (i == null) i = byCategory.get(category);
            if (i == null) i = byCategory.get(Category.OTHER);
            return i == null ? Patch.EMPTY : profiles.get(i).get(kind.ordinal());
        }

        String toJson() {
            JsonObject root = new JsonObject(), items = new JsonObject(), categories = new JsonObject();
            JsonArray array = new JsonArray();
            for (List<Patch> profile : profiles) {
                JsonArray p = new JsonArray();
                for (Patch patch : profile) p.add(patch.toJson());
                array.add(p);
            }
            byItem.forEach((id,i) -> items.addProperty(id.toString(),i));
            byCategory.forEach((category,i) -> categories.addProperty(category.name(),i));
            root.addProperty("revision",revision);
            root.add("profiles",array); root.add("items",items); root.add("categories",categories);
            String json = root.toString();
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_WIRE_BYTES) throw new IllegalArgumentException("rack packet too large");
            return json;
        }

        static Snapshot fromJson(String json) {
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_WIRE_BYTES) throw new IllegalArgumentException("rack packet too large");
            var root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.keySet().equals(Set.of("revision","profiles","items","categories"))) throw new IllegalArgumentException("rack packet schema");
            long generation = root.get("revision").getAsBigDecimal().longValueExact();
            if (generation < 0) throw new IllegalArgumentException("revision");
            var entries = root.getAsJsonArray("profiles");
            var itemValues = root.getAsJsonObject("items");
            if (entries.size()>MAX_FILES || itemValues.size()>MAX_ITEMS) throw new IllegalArgumentException("rack packet entry limit");
            List<List<Patch>> profiles = new ArrayList<>();
            for (var entry : entries) {
                var array = entry.getAsJsonArray();
                if (array.size() != PrepRackBlock.Kind.values().length) throw new IllegalArgumentException("rack profile length");
                var row = new ArrayList<Patch>();
                for (var p : array) row.add(RackRuleDef.parsePatch(p.getAsJsonObject(), false));
                profiles.add(List.copyOf(row));
            }
            var items = new LinkedHashMap<Identifier,Integer>();
            for (var e : itemValues.entrySet()) {
                if (e.getKey().length()>128) throw new IllegalArgumentException("rack item id length");
                var id = Identifier.parse(e.getKey());
                if (items.put(id, checkedIndex(RackRuleDef.integer(e.getValue()), profiles.size())) != null) {
                    throw new IllegalArgumentException("duplicate rack item id");
                }
            }
            var categories = new EnumMap<Category,Integer>(Category.class);
            root.getAsJsonObject("categories").entrySet().forEach(e -> categories.put(Category.valueOf(e.getKey()),
                checkedIndex(RackRuleDef.integer(e.getValue()),profiles.size())));
            return new Snapshot(generation,List.copyOf(profiles),Map.copyOf(items),Map.copyOf(categories));
        }

        private static int checkedIndex(int index, int size) {
            if (index < 0 || index >= size) throw new IllegalArgumentException("rack profile index");
            return index;
        }
    }

    public static String wireSnapshot() { return wire; }

    public static void acceptClient(String json) {
        try {
            Snapshot next = Snapshot.fromJson(json);
            if (next.revision >= client.revision) client = next;
        } catch (RuntimeException error) {
            HotPot.LOGGER.warn("[hotpot] invalid prep rack sync: {}", error.getMessage());
        }
    }

    public static void clearClient() { client = EMPTY; }
    public static boolean clientReady() { return client.revision > 0; }

    public static Patch profile(Level level, ItemStack stack, PrepRackBlock.Kind kind) {
        var snapshot = level != null && level.isClientSide() ? client : server;
        var category = category(stack);
        return snapshot.resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()), category, kind);
    }

    static Category category(ItemStack s) {
        if (s.has(DataComponents.FOOD) || s.getItem() instanceof ConfigurableFood) return Category.FOOD;
        return s.getItem() instanceof BlockItem ? Category.BLOCK : Category.OTHER;
    }

    public static int capacity(Level level, ItemStack stack, PrepRackBlock.Kind kind) {
        if (stack.isEmpty()) return 0;
        Integer override = profile(level,stack,kind).maxStack();
        int fallback = stack.getItem() instanceof BlockItem && category(stack)==Category.BLOCK ? 2 : Config.rackDefaultMaxStack();
        return Math.min(Math.min(RackRuleDef.STACK_LIMIT, stack.getMaxStackSize()), override == null ? fallback : override);
    }

    public static Layout layout(PrepRackBlock.Kind kind, ItemStack stack, Patch patch) {
        Layout explicit = patch.layout();
        if (kind == PrepRackBlock.Kind.HANGING) return explicit == Layout.HANGING ? explicit : Layout.SINGLE;
        if (explicit != null && explicit != Layout.HANGING) return explicit;
        return category(stack)==Category.BLOCK || stack.getMaxStackSize()==1 ? Layout.SINGLE : Layout.ROW_DEPTH;
    }
}
