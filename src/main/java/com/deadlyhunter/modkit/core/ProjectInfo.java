package com.deadlyhunter.modkit.core;

import com.google.gson.annotations.SerializedName;

public class ProjectInfo {

    @SerializedName("modkit_format_version")
    public int modkitFormatVersion = 1;

    @SerializedName("mod_id")
    public String modId;

    @SerializedName("mod_name")
    public String modName;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("author")
    public String author;

    @SerializedName("description")
    public String description = "A mod created with Modkit.";

    @SerializedName("version")
    public String version = "0.1.0";

    @SerializedName("license")
    public String license = "All Rights Reserved";

    @SerializedName("created_at")
    public long createdAt;

    public ProjectInfo() {}

    public ProjectInfo(String author, String modName) {
        this.author = author;
        this.modName = modName;
        this.modId = buildModId(author, modName);
        this.displayName = modName;
        this.createdAt = System.currentTimeMillis();
    }

    public static String buildModId(String author, String modName) {
        return "modkit_" + author + "_" + modName;
    }

    public static final String[] LICENSE_OPTIONS = {
            "All Rights Reserved",
            "MIT",
            "Apache-2.0",
            "LGPL-3.0",
            "GPL-3.0",
            "CC0-1.0"
    };
}
