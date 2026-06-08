package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.core.ProjectInfo;

public final class ManifestGenerator {

    private ManifestGenerator() {}

    public static String generate(ProjectInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest-Version: 1.0\r\n");
        sb.append("Created-By: Modkit\r\n");
        sb.append("Specification-Title: ").append(safe(info.displayName)).append("\r\n");
        sb.append("Specification-Vendor: ").append(safe(info.author)).append("\r\n");
        sb.append("Specification-Version: 1\r\n");
        sb.append("Implementation-Title: ").append(safe(info.modId)).append("\r\n");
        sb.append("Implementation-Version: ").append(safe(info.version)).append("\r\n");
        sb.append("Implementation-Vendor: ").append(safe(info.author)).append("\r\n");
        sb.append("\r\n");
        return sb.toString();
    }

    private static String safe(String input) {
        if (input == null) return "";
        return input.replace("\r", "").replace("\n", " ");
    }
}
