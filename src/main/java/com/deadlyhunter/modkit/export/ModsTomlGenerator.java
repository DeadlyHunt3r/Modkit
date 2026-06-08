package com.deadlyhunter.modkit.export;

import com.deadlyhunter.modkit.core.ProjectInfo;

public final class ModsTomlGenerator {

    private ModsTomlGenerator() {}

    public static String generate(ProjectInfo info) {
        String safeDisplay = escape(info.displayName);
        String safeDesc = escape(info.description);
        String safeAuthor = escape(info.author);
        String safeLicense = escape(info.license != null && !info.license.isBlank()
                ? info.license : "All Rights Reserved");

        return """
                modLoader="lowcodefml"
                loaderVersion="[47,)"
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
                    modId="forge"
                    mandatory=true
                    versionRange="[47,)"
                    ordering="NONE"
                    side="BOTH"

                [[dependencies.%s]]
                    modId="minecraft"
                    mandatory=true
                    versionRange="[1.20.1,1.21)"
                    ordering="NONE"
                    side="BOTH"

                [[dependencies.%s]]
                    modId="modkit"
                    mandatory=true
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
        );
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "").replace("\"", "");
    }
}
