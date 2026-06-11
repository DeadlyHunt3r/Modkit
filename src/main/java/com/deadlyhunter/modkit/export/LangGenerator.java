package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.tool.ToolDefinition;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;
import com.deadlyhunter.modkit.core.ProjectInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LangGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LangGenerator() {}

    public static String generate(ProjectInfo info,
                                   List<ItemDefinition> items,
                                   List<BlockDefinition> blocks,
                                   List<WeaponDefinition> weapons,
                                   List<ToolDefinition> tools,
                                   List<ArmorSetDefinition> armorSets) {
        Map<String, String> lang = new LinkedHashMap<>();
        lang.put("itemGroup." + info.modId + ".main", info.displayName);
        for (ItemDefinition def : items) {
            lang.put("item." + info.modId + "." + def.id, def.displayName);
        }
        for (BlockDefinition def : blocks) {
            lang.put("block." + info.modId + "." + def.id, def.displayName);
        }
        for (WeaponDefinition def : weapons) {
            lang.put("item." + info.modId + "." + def.id, def.displayName);
        }
        for (ToolDefinition def : tools) {
            lang.put("item." + info.modId + "." + def.id, def.displayName);
        }
        for (ArmorSetDefinition def : armorSets) {
            for (String pieceType : ArmorSetDefinition.PIECE_TYPES) {
                if (!def.hasPiece(pieceType)) continue;
                lang.put("item." + info.modId + "." + def.pieceItemId(pieceType),
                        def.pieceDisplayName(pieceType));
            }
        }
        return GSON.toJson(lang);
    }
}
