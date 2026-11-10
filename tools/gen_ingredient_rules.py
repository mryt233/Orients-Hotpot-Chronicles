"""作者：mryt233；mryt233 改（2026-11-11）：锅专用煮法、捣制及禁止下锅标签。"""
import json
from pixkit.paths import DATA, MODID


def own(name):
    return f"{MODID}:{name}"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = {"_说明": "作者：mryt233", **data}
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def generate():
    boiling = {
        "kelp": ("minecraft:kelp", "cooked_kelp", {}),
        "cold_tofu": (own("cold_tofu"), "hot_tofu", {
            "cook_ticks": 200, "overcook_result": {"id": own("hot_tofu_cube")}, "overcook_ticks": 600}),
        "crispy_pork": (own("crispy_pork"), "boiled_crispy_pork", {}),
        "zhe_er_gen_slice": (own("zhe_er_gen_slice"), "cooked_zhe_er_gen_slice", {}),
    }
    for name, (raw, result, extra) in boiling.items():
        write(DATA / f"recipe/pot_boiling/{name}.json", {
            "type": own("pot_boiling"), "ingredient": {"item": raw}, "result": {"id": own(result)}, **extra})
    pounding = [
        ("beef", "minecraft:beef", "beef_meatball", 3, 1),
        ("porkchop", "minecraft:porkchop", "pork_meatball", 3, 3),
        ("mutton", "minecraft:mutton", "mutton_meatball", 3, 5),
        ("cod", "minecraft:cod", "fish_ball", 3, 7),
        ("salmon", "minecraft:salmon", "fish_ball", 3, 7),
        ("garlic", own("garlic"), "garlic_paste", 2, None),
        ("peeled_shrimp", own("peeled_shrimp"), "shrimp_paste", 1, None),
        ("peanut", own("peanut"), "crushed_peanuts", 2, None),
        ("zhe_er_gen", own("zhe_er_gen"), "minced_zhe_er_gen", 2, None),
    ]
    for name, raw, result, count, model in pounding:
        if model is None:
            stages = {"stage2": {"id": own(result)}}
        else:
            stages = {"stage1": {"id": own("pounded_meat"), "components": {"minecraft:custom_model_data": model}},
                      "stage2": {"id": own("pounded_meat"), "components": {"minecraft:custom_model_data": model + 1}}}
        write(DATA / f"recipe/pounding/{name}.json", {
            "type": own("pounding"), "ingredient": {"item": raw},
            "result": {"id": own(result), "count": count}, **stages})
    write(DATA / "tags/item/not_pot_ingredient.json", {"replace": False, "values": [
        own(name) for name in ("bamboo_chopsticks", "chili_pepper", "millet_chili", "oyster_sauce",
            "sesame_paste", "peanut", "crushed_peanuts", "lotus_root", "raw_squid", "egg_fried_rice",
            "zhe_er_gen", "minced_zhe_er_gen")
    ] + ["#" + own("cloth_aprons")]})


if __name__ == "__main__":
    generate()
