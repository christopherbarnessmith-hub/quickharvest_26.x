package com.kingxion.quickharvest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class QuickHarvest implements ModInitializer {

	public static final String MOD_ID = "quickharvest";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Config — loaded from config/quickharvest.properties
	public static boolean REQUIRE_HOE       = false; // if true, empty hand does nothing
	public static boolean HOE_DURABILITY    = true;  // hoe loses 1 durability per harvest
	public static boolean AUTO_DETECT       = true;  // detect modded crops by age/growth properties
	public static boolean XP_ENABLED        = false; // award XP on harvest
	public static int     XP_AMOUNT         = 1;     // XP per harvest when enabled
	public static float   XP_CHANCE         = 0.5f;  // probability of XP drop (0.0–1.0)

	// Denylist: mod IDs or block registry names that should NOT be harvested by this mod
	// (populated from config, checked in QuickHarvestCompat)
	public static java.util.Set<String> DENYLIST = new java.util.HashSet<>();
	public static java.util.Set<String> ALLOWLIST = new java.util.HashSet<>();

	private static final Random RANDOM = new Random();

	@Override
	public void onInitialize() {
		QuickHarvestConfig.load();
		LOGGER.info("Ion Quick Harvest initialized!");

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			if (!(hitResult instanceof BlockHitResult blockHit)) return InteractionResult.PASS;

			BlockPos pos = blockHit.getBlockPos();
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();

			ItemStack heldItem = player.getItemInHand(hand);

			// Require hoe if configured, otherwise allow empty hand too
			boolean holdingHoe = heldItem.is(ItemTags.HOES);
			if (REQUIRE_HOE && !holdingHoe) return InteractionResult.PASS;

			// Skip if the block is denylisted
			String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getKey(block).toString();
			if (DENYLIST.contains(blockId)) return InteractionResult.PASS;

			// Determine if this block is a harvestable crop at full growth
			Optional<IntegerProperty> ageProperty = QuickHarvestCompat.getAgeProperty(state);
			if (ageProperty.isEmpty()) return InteractionResult.PASS;

			IntegerProperty ageProp = ageProperty.get();
			int currentAge = state.getValue(ageProp);
			int maxAge = ageProp.getPossibleValues().stream().mapToInt(i -> i).max().orElse(0);

			// Only harvest if fully grown
			if (currentAge < maxAge) return InteractionResult.PASS;

			// --- Harvest ---
			ServerLevel serverWorld = (ServerLevel) world;

			// Get drops with the held tool (respects Fortune on hoe)
			List<ItemStack> drops = Block.getDrops(state, serverWorld, pos, null, player, heldItem);

			// Remove one seed from drops so the plant stays in the ground
			QuickHarvestCompat.removeSeedFromDrops(block, drops);

			// Spawn drops
			for (ItemStack drop : drops) {
				ItemEntity entity = new ItemEntity(world,
						pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, drop);
				entity.setDefaultPickUpDelay();
				world.addFreshEntity(entity);
			}

			// Reset the crop to age 0 (stays planted)
			world.setBlock(pos, state.setValue(ageProp, 0), Block.UPDATE_ALL);

			// Durability cost if holding a hoe
			if (HOE_DURABILITY && holdingHoe) {
				heldItem.hurtAndBreak(1, player,
						net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			}

			// Optional XP drop
			if (XP_ENABLED && RANDOM.nextFloat() < XP_CHANCE) {
				world.addFreshEntity(new ExperienceOrb(serverWorld,
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						XP_AMOUNT));
			}

			return InteractionResult.SUCCESS;
		});
	}
}