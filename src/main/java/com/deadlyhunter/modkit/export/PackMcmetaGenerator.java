package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.core.ProjectInfo;

public final class PackMcmetaGenerator {

    private PackMcmetaGenerator() {}

    public static String generate(ProjectInfo info) {
        String description = info.displayName + " (created with Modkit)";
        description = description.replace("\\", "\\\\").replace("\"", "\\\"");

        return """
                {
                  "pack": {
                    "pack_format": 34,
                    "description": "%s"
                  }
                }
                """.formatted(description);
    }
}
