package com.kingxion.quickharvest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

public class QuickHarvestCompat {

    private static final List<String> AGE_PROPERTY_NAMES = List.of(
            "age", "growth", "stage", "crop_age", "maturity"
    );

    public static Optional<IntegerProperty> getAgeProperty(BlockState state) {
        Block block = state.getBlock();
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(block).toString();

        // Explicit allowlist — always harvest
        if (!QuickHarvest.ALLOWLIST.isEmpty() && QuickHarvest.ALLOWLIST.contains(blockId)) {
            return findAgeProperty(state);
        }

        // Skip pumpkin/melon stems — they don't reset cleanly
        if (block instanceof StemBlock) return Optional.empty();

        // All crops (vanilla and modded) — scan for age/growth property by name
        // This works for CropBlock subclasses too since they all use "age"
        return findAgeProperty(state);
    }

    private static Optional<IntegerProperty> findAgeProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty intProp) {
                String name = property.getName().toLowerCase(Locale.ROOT);
                for (String knownName : AGE_PROPERTY_NAMES) {
                    if (name.equals(knownName)) {
                        return Optional.of(intProp);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void removeSeedFromDrops(Block block, List<ItemStack> drops) {
        // Map vanilla crops to their seed items directly — no protected method needed
        net.minecraft.world.item.Item seedItem = getVanillaSeed(block);

        if (seedItem == null) {
            // Modded crop fallback — remove the smallest stack as best-effort seed removal
            drops.stream()
                    .filter(s -> !s.isEmpty())
                    .min(Comparator.comparingInt(ItemStack::getCount))
                    .ifPresent(smallest -> {
                        smallest.shrink(1);
                        drops.removeIf(ItemStack::isEmpty);
                    });
            return;
        }

        for (ItemStack drop : drops) {
            if (drop.getItem() == seedItem) {
                drop.shrink(1);
                break;
            }
        }
        drops.removeIf(ItemStack::isEmpty);
    }

    private static net.minecraft.world.item.Item getVanillaSeed(Block block) {
        if (block == Blocks.WHEAT)      return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)    return Items.CARROT;
        if (block == Blocks.POTATOES)   return Items.POTATO;
        if (block == Blocks.BEETROOTS)  return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (block == Blocks.TORCHFLOWER_CROP) return Items.TORCHFLOWER_SEEDS;
        if (block == Blocks.PITCHER_CROP)     return Items.PITCHER_POD;
        return null; // modded crop — fall through to smallest-stack removal
    }
}