package com.deadlyhunter.modkit.content;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.content.block.BlockDefinition;
import com.deadlyhunter.modkit.content.block.ModkitBlock;
import com.deadlyhunter.modkit.content.item.ItemDefinition;
import com.deadlyhunter.modkit.content.item.ModkitItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

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
        DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, project.modId);
        DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, project.modId);
        DeferredRegister<CreativeModeTab> tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, project.modId);

        for (ItemDefinition def : project.itemDefinitions) {
            RegistryObject<ModkitItem> ro = items.register(def.id, () -> new ModkitItem(def));
            project.registeredItems.add(ro);
        }

        for (BlockDefinition def : project.blockDefinitions) {
            RegistryObject<ModkitBlock> blockRo = blocks.register(def.id, () -> new ModkitBlock(def));
            project.registeredBlocks.add(blockRo);

            RegistryObject<BlockItem> blockItemRo = items.register(def.id,
                    () -> new BlockItem(blockRo.get(), new Item.Properties()));
            project.registeredBlockItems.add(blockItemRo);
        }

        for (com.deadlyhunter.modkit.content.weapon.WeaponDefinition def : project.weaponDefinitions) {
            RegistryObject<com.deadlyhunter.modkit.content.weapon.ModkitSword> swordRo = items.register(def.id,
                    () -> new com.deadlyhunter.modkit.content.weapon.ModkitSword(def, project.modId));
            project.registeredSwords.add(swordRo);
        }

        for (com.deadlyhunter.modkit.content.tool.ToolDefinition def : project.toolDefinitions) {
            RegistryObject<Item> toolRo = items.register(def.id, () -> switch (def.toolType) {
                case "axe"     -> new com.deadlyhunter.modkit.content.tool.ModkitAxe(def, project.modId);
                case "shovel"  -> new com.deadlyhunter.modkit.content.tool.ModkitShovel(def, project.modId);
                case "hoe"     -> new com.deadlyhunter.modkit.content.tool.ModkitHoe(def, project.modId);
                default        -> new com.deadlyhunter.modkit.content.tool.ModkitPickaxe(def, project.modId);
            });
            project.registeredTools.add(toolRo);
        }

        for (com.deadlyhunter.modkit.content.armor.ArmorSetDefinition def : project.armorSetDefinitions) {
            final com.deadlyhunter.modkit.content.armor.ModkitArmorMaterial material =
                    new com.deadlyhunter.modkit.content.armor.ModkitArmorMaterial(project.modId, def);
            for (String pieceType : com.deadlyhunter.modkit.content.armor.ArmorSetDefinition.PIECE_TYPES) {
                if (!def.hasPiece(pieceType)) continue;
                final String pt = pieceType;
                RegistryObject<com.deadlyhunter.modkit.content.armor.ModkitArmorItem> armorRo =
                        items.register(def.pieceItemId(pieceType),
                                () -> new com.deadlyhunter.modkit.content.armor.ModkitArmorItem(def, material, pt));
                project.registeredArmor.add(armorRo);
            }
        }
        if (!project.isEmpty()) {
            tabs.register("main", () ->
                    CreativeModeTab.builder()
                            .title(Component.literal(project.displayName))
                            .icon(() -> pickTabIcon(project))
                            .displayItems((params, out) -> {

                                for (RegistryObject<ModkitItem> ro : project.registeredItems) {
                                    out.accept(ro.get());
                                }

                                for (RegistryObject<BlockItem> ro : project.registeredBlockItems) {
                                    out.accept(ro.get());
                                }

                                for (RegistryObject<com.deadlyhunter.modkit.content.weapon.ModkitSword> ro
                                        : project.registeredSwords) {
                                    out.accept(ro.get());
                                }

                                for (RegistryObject<Item> ro : project.registeredTools) {
                                    out.accept(ro.get());
                                }

                                for (RegistryObject<com.deadlyhunter.modkit.content.armor.ModkitArmorItem> ro
                                        : project.registeredArmor) {
                                    out.accept(ro.get());
                                }
                            })
                            .build()
            );
        }

        items.register(modEventBus);
        blocks.register(modEventBus);
        tabs.register(modEventBus);

        ITEM_REGISTERS.put(project.modId, items);
        BLOCK_REGISTERS.put(project.modId, blocks);
        TAB_REGISTERS.put(project.modId, tabs);

        Modkit.LOGGER.info("[Modkit] Prepared registry for '{}' — {} item(s), {} block(s), {} weapon(s), {} tool(s), {} armor set(s)",
                project.modId,
                project.itemDefinitions.size(),
                project.blockDefinitions.size(),
                project.weaponDefinitions.size(),
                project.toolDefinitions.size(),
                project.armorSetDefinitions.size());
    }

    private static ItemStack pickTabIcon(ModkitProject p) {
        if (!p.registeredItems.isEmpty()) {
            return new ItemStack(p.registeredItems.get(0).get());
        }
        if (!p.registeredBlockItems.isEmpty()) {
            return new ItemStack(p.registeredBlockItems.get(0).get());
        }
        return new ItemStack(net.minecraft.world.item.Items.PAPER);
    }

    public static List<ModkitProject> getProjects() {
        return Collections.unmodifiableList(PROJECTS);
    }

    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        Item item = event.getItemStack().getItem();
        if (item instanceof ModkitItem mi) {
            int t = mi.getDefinition().fuelBurnTime;
            if (t > 0) event.setBurnTime(t);
        }
    }
}
