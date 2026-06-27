package com.deadlyhunter.modkit.content;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.block.ModkitBlock;
import com.deadlyhunter.modkit.content.block.ModkitFacingBlock;
import com.deadlyhunter.modkit.content.block.ModkitFence;
import com.deadlyhunter.modkit.content.block.ModkitSlab;
import com.deadlyhunter.modkit.content.block.ModkitStairs;
import com.deadlyhunter.modkit.content.block.ModkitWall;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.item.ModkitItem;
import com.deadlyhunter.modkit.content.armor.ArmorSetDefinition;
import com.deadlyhunter.modkit.content.armor.ModkitArmorItem;
import com.deadlyhunter.modkit.content.armor.ModkitArmorMaterial;
import com.deadlyhunter.modkit.content.tool.ModkitAxe;
import com.deadlyhunter.modkit.content.tool.ModkitHoe;
import com.deadlyhunter.modkit.content.tool.ModkitPickaxe;
import com.deadlyhunter.modkit.content.tool.ModkitShovel;
import com.deadlyhunter.modkit.content.tool.ToolDefinition;
import com.deadlyhunter.modkit.content.weapon.ModkitSword;
import com.deadlyhunter.modkit.content.weapon.WeaponDefinition;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProjectRegistry {

    private static final List<ModkitProject> PROJECTS = new ArrayList<>();
    private static final Map<String, DeferredRegister<Item>> ITEM_REGISTERS = new HashMap<>();
    private static final Map<String, DeferredRegister<Block>> BLOCK_REGISTERS = new HashMap<>();
    private static final Map<String, DeferredRegister<CreativeModeTab>> TAB_REGISTERS = new HashMap<>();
    private static boolean prepared = false;

    private ProjectRegistry() {}

    public static void scanAndPrepareRegistries(IEventBus modEventBus) {
        if (prepared) {
            Modkit.LOGGER.warn("[Modkit] scanAndPrepareRegistries called twice, ignoring");
            return;
        }
        prepared = true;

        List<ModkitProject> found = ProjectScanner.scanAll();
        for (ModkitProject project : found) {
            registerProject(project, modEventBus);
        }
        PROJECTS.addAll(found);
    }

    private static void registerProject(ModkitProject project, IEventBus modEventBus) {
        DeferredRegister<Item> items = DeferredRegister.createItems(project.modId);
        DeferredRegister<Block> blocks = DeferredRegister.createBlocks(project.modId);
        DeferredRegister<CreativeModeTab> tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, project.modId);

        for (ItemDefinition def : project.itemDefinitions) {
            var ro = items.register(def.id, () -> new ModkitItem(def));
            project.registeredItems.add(ro);
        }

        for (BlockDefinition def : project.blockDefinitions) {
            var blockRo = blocks.register(def.id, () -> makeBlock(def));
            project.registeredBlocks.add(blockRo);

            var blockItemRo = items.register(def.id, () -> new BlockItem(blockRo.get(), new Item.Properties()));
            project.registeredBlockItems.add(blockItemRo);
        }

        for (WeaponDefinition def : project.weaponDefinitions) {
            var swordRo = items.register(def.id, () -> new ModkitSword(def, project.modId));
            project.registeredSwords.add(swordRo);
        }

        for (ToolDefinition def : project.toolDefinitions) {
            var toolRo = items.register(def.id, () -> switch (def.toolType) {
                case "axe"    -> new ModkitAxe(def, project.modId);
                case "shovel" -> new ModkitShovel(def, project.modId);
                case "hoe"    -> new ModkitHoe(def, project.modId);
                default       -> new ModkitPickaxe(def, project.modId);
            });
            project.registeredTools.add(toolRo);
        }

        for (ArmorSetDefinition def : project.armorSetDefinitions) {
            Holder<ArmorMaterial> material = ModkitArmorMaterial.create(project.modId, def);
            int durabilityMultiplier = ModkitArmorMaterial.durabilityMultiplier(def);

            for (String pieceType : ArmorSetDefinition.PIECE_TYPES) {
                if (!def.hasPiece(pieceType)) continue;
                final String pt = pieceType;
                var armorRo = items.register(def.pieceItemId(pieceType),
                        () -> new ModkitArmorItem(def, material, durabilityMultiplier, pt));
                project.registeredArmor.add(armorRo);
            }
        }

        if (!project.isEmpty()) {
            tabs.register("main", () -> CreativeModeTab.builder()
                    .title(Component.literal(project.displayName))
                    .icon(() -> pickTabIcon(project))
                    .displayItems((params, out) -> {
                        project.registeredItems.forEach(ro -> out.accept(ro.get()));
                        project.registeredBlockItems.forEach(ro -> out.accept(ro.get()));
                        project.registeredSwords.forEach(ro -> out.accept(ro.get()));
                        project.registeredTools.forEach(ro -> out.accept(ro.get()));
                        project.registeredArmor.forEach(ro -> out.accept(ro.get()));
                    })
                    .build());
        }

        items.register(modEventBus);
        blocks.register(modEventBus);
        tabs.register(modEventBus);

        ITEM_REGISTERS.put(project.modId, items);
        BLOCK_REGISTERS.put(project.modId, blocks);
        TAB_REGISTERS.put(project.modId, tabs);

        Modkit.LOGGER.info("[Modkit] Prepared registry for '{}' - {} item(s), {} block(s), {} weapon(s), {} tool(s), {} armor set(s)",
                project.modId,
                project.itemDefinitions.size(),
                project.blockDefinitions.size(),
                project.weaponDefinitions.size(),
                project.toolDefinitions.size(),
                project.armorSetDefinitions.size());
    }

    private static Block makeBlock(BlockDefinition def) {
        if (def.isVariant()) {
            return switch (def.variantType) {
                case "slab"   -> new ModkitSlab(def);
                case "stairs" -> new ModkitStairs(def);
                case "wall"   -> new ModkitWall(def);
                case "fence"  -> new ModkitFence(def);
                default       -> new ModkitBlock(def);
            };
        }
        if (def.usesFacing()) {
            return new ModkitFacingBlock(def);
        }
        return new ModkitBlock(def);
    }

    private static ItemStack pickTabIcon(ModkitProject p) {
        if (!p.registeredItems.isEmpty()) return new ItemStack(p.registeredItems.get(0).get());
        if (!p.registeredBlockItems.isEmpty()) return new ItemStack(p.registeredBlockItems.get(0).get());
        return new ItemStack(Items.PAPER);
    }

    public static List<ModkitProject> getProjects() {
        return Collections.unmodifiableList(PROJECTS);
    }
}
