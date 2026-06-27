package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.core.ProjectInfo;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ModsTomlGenerator {

    private ModsTomlGenerator() {}

    public static String generate(ProjectInfo info) {
        return generate(info, null);
    }

    public static String generate(ProjectInfo info, Set<String> loadAfterMods) {
        String safeDisplay = escape(info.displayName);
        String safeDesc = escape(info.description);
        String safeAuthor = escape(info.author);
        String safeLicense = escape(info.license != null && !info.license.isBlank()
                ? info.license : "All Rights Reserved");

        StringBuilder sb = new StringBuilder();
        sb.append("""
                modLoader="lowcodefml"
                loaderVersion="[4,)"
                license="%s"

                [[mods]]
                modId="%s"
                version="%s"
                displayName="%s"
                authors="%s"
                description='''
                %s
                '''
                credits="Created with Modkit"

                [[dependencies.%s]]
                    modId="neoforge"
                    type="required"
                    versionRange="[21.1,)"
                    ordering="NONE"
                    side="BOTH"

                [[dependencies.%s]]
                    modId="minecraft"
                    type="required"
                    versionRange="[1.21.1,1.22)"
                    ordering="NONE"
                    side="BOTH"

                [[dependencies.%s]]
                    modId="modkit"
                    type="required"
                    versionRange="[0.1,)"
                    ordering="AFTER"
                    side="BOTH"
                """.formatted(
                safeLicense,
                info.modId,
                info.version,
                safeDisplay,
                safeAuthor,
                safeDesc,
                info.modId,
                info.modId,
                info.modId
        ));

        if (loadAfterMods != null) {
            Set<String> clean = new LinkedHashSet<>();
            for (String m : loadAfterMods) {
                if (m == null) continue;
                String id = m.trim().toLowerCase();
                if (id.isEmpty()) continue;
                if (id.equals("minecraft") || id.equals("neoforge") || id.equals("forge") || id.equals("modkit")) continue;
                if (id.equals(info.modId)) continue;
                if (!id.matches("[a-z0-9_.-]+")) continue;
                clean.add(id);
            }
            for (String dep : clean) {
                sb.append("""

                        [[dependencies.%s]]
                            modId="%s"
                            type="optional"
                            versionRange="*"
                            ordering="AFTER"
                            side="BOTH"
                        """.formatted(info.modId, dep));
            }
        }

        return sb.toString();
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "").replace("\"", "");
    }
}
