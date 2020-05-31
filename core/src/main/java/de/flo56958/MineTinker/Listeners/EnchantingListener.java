package de.flo56958.MineTinker.Listeners;

import de.flo56958.MineTinker.Data.Lists;
import de.flo56958.MineTinker.Data.ToolType;
import de.flo56958.MineTinker.Main;
import de.flo56958.MineTinker.Modifiers.ModManager;
import de.flo56958.MineTinker.Modifiers.Modifier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EnchantingListener implements Listener {

	private final ModManager modManager = ModManager.instance();

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onTableEnchant(EnchantItemEvent event) {
		if (!Main.getPlugin().getConfig().getBoolean("ConvertEnchantmentsOnEnchant", true)) return;
		if (!ToolType.ALL.contains(event.getItem().getType())) { //Something different (like a book)
			return;
		}
		if (!(modManager.isToolViable(event.getItem()) || modManager.isWandViable(event.getItem())
				|| modManager.isArmorViable(event.getItem()))) { //not a MineTinker Tool
			return;
		}

		boolean free = !Main.getPlugin().getConfig().getBoolean("EnchantingCostsSlots", true);

		Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();

		for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
			Modifier modifier = modManager.getModifierFromEnchantment(entry.getKey());

			// The modifier may be disabled
			if (modifier != null && modifier.isAllowed()) {
				for (int i = 0; i < entry.getValue(); i++) {
					//Adding necessary slots
					if (free)
						modManager.setFreeSlots(event.getItem(), modManager.getFreeSlots(event.getItem()) + modifier.getSlotCost());
					if (!modManager.addMod(event.getEnchanter(), event.getItem(), modifier,
							false,false, true)) {
						//Remove slots as they were not needed
						if (free)
							modManager.setFreeSlots(event.getItem(), modManager.getFreeSlots(event.getItem()) - modifier.getSlotCost());
						if (Main.getPlugin().getConfig().getBoolean("RefundLostEnchantmentsAsItems", true)) {
							for (; i < entry.getValue(); i++) { //Drop lost enchantments due to some error in addMod
								if (event.getEnchanter().getInventory().addItem(modifier.getModItem()).size() != 0) { //adds items to (full) inventory
									event.getEnchanter().getWorld().dropItem(event.getEnchanter().getLocation(), modifier.getModItem());
								} // no else as it gets added in if-clause
							}
						}
						break;
					}
				}
			}
		}

		// The enchants should be added when calling addMod
		event.getEnchantsToAdd().clear();
	}

	@EventHandler
	public void onAnvilPrepare(InventoryClickEvent event) {
		if (!Main.getPlugin().getConfig().getBoolean("ConvertEnchantmentsOnEnchant", true)) return;
		HumanEntity entity = event.getWhoClicked();

		if (!(entity instanceof Player && event.getClickedInventory() instanceof AnvilInventory)) {
			return;
		}

		AnvilInventory inv = (AnvilInventory) event.getClickedInventory();
		Player player = (Player) entity;

		ItemStack tool = inv.getItem(0);
		ItemStack book = inv.getItem(1);
		ItemStack newTool = inv.getItem(2);

		if (tool == null || book == null || newTool == null) {
			return;
		}

		if (book.getType() != Material.ENCHANTED_BOOK) {
			return;
		}

		boolean free = !Main.getPlugin().getConfig().getBoolean("EnchantingCostsSlots", true);

		for (Map.Entry<Enchantment, Integer> entry : newTool.getEnchantments().entrySet()) {
			int oldEnchantLevel = tool.getEnchantmentLevel(entry.getKey());

			if (oldEnchantLevel < entry.getValue()) {
				int difference = entry.getValue() - oldEnchantLevel;
				Modifier modifier = ModManager.instance().getModifierFromEnchantment(entry.getKey());

				if (modifier != null) {
					for (int i = 0; i < difference; i++) {
						//Adding necessary slots
						if (free)
							modManager.setFreeSlots(newTool, modManager.getFreeSlots(newTool) + modifier.getSlotCost());
						if (!modManager.addMod(player, newTool, modifier,
								false,false, true)) {
							//Remove slots as they were not needed
							if (free)
								modManager.setFreeSlots(newTool, modManager.getFreeSlots(newTool) - modifier.getSlotCost());
							if (Main.getPlugin().getConfig().getBoolean("RefundLostEnchantmentsAsItems", true)) {
								for (; i < difference; i++) { //Drop lost enchantments due to some error in addMod
									if (player.getInventory().addItem(modifier.getModItem()).size() != 0) { //adds items to (full) inventory
										player.getWorld().dropItem(player.getLocation(), modifier.getModItem());
									} // no else as it gets added in if-clause
								}
							}
							break;
						}
					}
				}
			}
		}

		// TODO: Refund enchantment levels lost due to removeEnchantment and addMod
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onEnchant(PrepareItemEnchantEvent event) {
		if (Lists.WORLDS.contains(event.getEnchanter().getWorld().getName())) {
			return;
		}

		if (Main.getPlugin().getConfig().getBoolean("AllowEnchanting")) {
			return;
		}

		ItemStack tool = event.getItem();

		if (modManager.isToolViable(tool) || modManager.isWandViable(tool) || modManager.isArmorViable(tool)) {
			event.setCancelled(true);
		}
	}
}
