package com.deadlyhunter.modkit.content;

import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.item.ModkitItem;
import com.deadlyhunter.modkit.content.ore.OreDefinition;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
import com.deadlyhunter.modkit.content.armor.ModkitArmorItem;
import com.deadlyhunter.modkit.content.recipe.RecipeDefinition;
import com.deadlyhunter.modkit.content.recipe.RecipeOverrideDefinition;
import com.deadlyhunter.modkit.content.tool.ToolDefinition;
import com.deadlyhunter.modkit.content.weapon.ModkitSword;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModkitProject {

    public final String modId;
    public final String displayName;
    public final String author;

    public final List<ItemDefinition> itemDefinitions = new ArrayList<>();
    public final List<DeferredHolder<Item, ModkitItem>> registeredItems = new ArrayList<>();

    public final List<BlockDefinition> blockDefinitions = new ArrayList<>();
    public final List<DeferredHolder<Block, Block>> registeredBlocks = new ArrayList<>();
    public final List<DeferredHolder<Item, BlockItem>> registeredBlockItems = new ArrayList<>();

    public final List<OreDefinition> oreDefinitions = new ArrayList<>();
    public final List<RecipeDefinition> recipeDefinitions = new ArrayList<>();
    public final List<RecipeOverrideDefinition> recipeOverrideDefinitions = new ArrayList<>();

    public final List<WeaponDefinition> weaponDefinitions = new ArrayList<>();
    public final List<DeferredHolder<Item, ModkitSword>> registeredSwords = new ArrayList<>();

    public final List<ToolDefinition> toolDefinitions = new ArrayList<>();
    public final List<DeferredHolder<Item, ? extends Item>> registeredTools = new ArrayList<>();

    public final List<ArmorSetDefinition> armorSetDefinitions = new ArrayList<>();
    public final List<DeferredHolder<Item, ModkitArmorItem>> registeredArmor = new ArrayList<>();

    public ModkitProject(String modId, String displayName, String author) {
        this.modId = modId;
        this.displayName = displayName;
        this.author = author;
    }

    public List<ItemDefinition> getItemDefinitions() { return Collections.unmodifiableList(itemDefinitions); }
    public List<BlockDefinition> getBlockDefinitions() { return Collections.unmodifiableList(blockDefinitions); }
    public List<OreDefinition> getOreDefinitions() { return Collections.unmodifiableList(oreDefinitions); }
    public List<RecipeDefinition> getRecipeDefinitions() { return Collections.unmodifiableList(recipeDefinitions); }
    public List<RecipeOverrideDefinition> getRecipeOverrideDefinitions() { return Collections.unmodifiableList(recipeOverrideDefinitions); }
    public List<WeaponDefinition> getWeaponDefinitions() { return Collections.unmodifiableList(weaponDefinitions); }
    public List<ToolDefinition> getToolDefinitions() { return Collections.unmodifiableList(toolDefinitions); }
    public List<ArmorSetDefinition> getArmorSetDefinitions() { return Collections.unmodifiableList(armorSetDefinitions); }

    public boolean isEmpty() {
        return itemDefinitions.isEmpty() && blockDefinitions.isEmpty()
                && weaponDefinitions.isEmpty() && toolDefinitions.isEmpty()
                && armorSetDefinitions.isEmpty();
    }
}
