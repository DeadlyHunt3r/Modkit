package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.item.ItemDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TagJsonGenerator {

    private TagJsonGenerator() {}

    public record TagFile(String path, String json) {}

    public static List<TagFile> generateItemTags(String modId, List<ItemDefinition> items) {
        Map<String, List<String>> tagToEntries = new LinkedHashMap<>();
        for (ItemDefinition item : items) {
            if (item.tags == null) continue;
            String entryId = modId + ":" + item.id;
            for (String tag : item.tags) {
                if (tag == null || tag.isBlank()) continue;
                tagToEntries.computeIfAbsent(tag.trim(), k -> new ArrayList<>()).add(entryId);
            }
        }
        return buildFiles("item", tagToEntries);
    }

    public static List<TagFile> generateBlockTags(String modId, List<BlockDefinition> blocks) {
        Map<String, List<String>> tagToEntries = new LinkedHashMap<>();
        for (BlockDefinition block : blocks) {
            if (block.tags == null) continue;
            String entryId = modId + ":" + block.id;
            for (String tag : block.tags) {
                if (tag == null || tag.isBlank()) continue;
                tagToEntries.computeIfAbsent(tag.trim(), k -> new ArrayList<>()).add(entryId);
            }
        }
        return buildFiles("block", tagToEntries);
    }

    private static List<TagFile> buildFiles(String kind, Map<String, List<String>> tagToEntries) {
        List<TagFile> files = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : tagToEntries.entrySet()) {
            String tagId = e.getKey();
            int colon = tagId.indexOf(':');
            if (colon < 0) continue;
            String tagNamespace = tagId.substring(0, colon);
            String tagPath = tagId.substring(colon + 1);

            String path = "data/" + tagNamespace + "/tags/" + kind + "/" + tagPath + ".json";
            String json = buildJson(e.getValue());
            files.add(new TagFile(path, json));
        }
        return files;
    }

    private static String buildJson(List<String> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"replace\": false,\n");
        sb.append("  \"values\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            sb.append("    \"").append(entries.get(i)).append("\"");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
