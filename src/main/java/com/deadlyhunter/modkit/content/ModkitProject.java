package com.deadlyhunter.modkit.content;

import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.block.ModkitBlock;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.item.ModkitItem;
import com.deadlyhunter.modkit.content.ore.OreDefinition;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModkitProject {

    public final String modId;
    public final String displayName;
    public final String author;

    public final List<ItemDefinition> itemDefinitions = new ArrayList<>();
    public final List<RegistryObject<ModkitItem>> registeredItems = new ArrayList<>();

    public final List<BlockDefinition> blockDefinitions = new ArrayList<>();
    public final List<RegistryObject<ModkitBlock>> registeredBlocks = new ArrayList<>();
    public final List<RegistryObject<BlockItem>> registeredBlockItems = new ArrayList<>();

    public final List<OreDefinition> oreDefinitions = new ArrayList<>();

    public final List<com.deadlyhunter.modkit.content.recipe.RecipeDefinition> recipeDefinitions = new ArrayList<>();

    public final List<com.deadlyhunter.modkit.content.weapon.WeaponDefinition> weaponDefinitions = new ArrayList<>();
    public final List<RegistryObject<com.deadlyhunter.modkit.content.weapon.ModkitSword>> registeredSwords = new ArrayList<>();

    public final List<com.deadlyhunter.modkit.content.tool.ToolDefinition> toolDefinitions = new ArrayList<>();

    public final List<RegistryObject<net.minecraft.world.item.Item>> registeredTools = new ArrayList<>();

    public final List<com.deadlyhunter.modkit.content.armor.ArmorSetDefinition> armorSetDefinitions = new ArrayList<>();

    public final List<RegistryObject<com.deadlyhunter.modkit.content.armor.ModkitArmorItem>> registeredArmor = new ArrayList<>();

    public ModkitProject(String modId, String displayName, String author) {
        this.modId = modId;
        this.displayName = displayName;
        this.author = author;
    }

    public List<ItemDefinition> getItemDefinitions() { return Collections.unmodifiableList(itemDefinitions); }
    public List<BlockDefinition> getBlockDefinitions() { return Collections.unmodifiableList(blockDefinitions); }
    public List<OreDefinition> getOreDefinitions() { return Collections.unmodifiableList(oreDefinitions); }
    public List<com.deadlyhunter.modkit.content.recipe.RecipeDefinition> getRecipeDefinitions() {
        return Collections.unmodifiableList(recipeDefinitions);
    }
    public List<com.deadlyhunter.modkit.content.weapon.WeaponDefinition> getWeaponDefinitions() {
        return Collections.unmodifiableList(weaponDefinitions);
    }
    public List<com.deadlyhunter.modkit.content.tool.ToolDefinition> getToolDefinitions() {
        return Collections.unmodifiableList(toolDefinitions);
    }
    public List<com.deadlyhunter.modkit.content.armor.ArmorSetDefinition> getArmorSetDefinitions() {
        return Collections.unmodifiableList(armorSetDefinitions);
    }

    public boolean isEmpty() {
        return itemDefinitions.isEmpty() && blockDefinitions.isEmpty()
                && weaponDefinitions.isEmpty() && toolDefinitions.isEmpty()
                && armorSetDefinitions.isEmpty();
    }
}
