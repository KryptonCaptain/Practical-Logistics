package sonar.logistics.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import sonar.core.SonarCore;
import sonar.core.api.ActionType;
import sonar.core.api.BlockCoords;
import sonar.core.api.FluidHandler;
import sonar.core.api.StoredFluidStack;
import sonar.core.api.StoredItemStack;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.logistics.api.LogisticsAPI;
import sonar.logistics.api.cache.INetworkCache;
import sonar.logistics.api.cache.IStorageCache;
import sonar.logistics.api.wrappers.FluidWrapper;
import sonar.logistics.api.wrappers.ItemWrapper.SortingDirection;
import sonar.logistics.api.wrappers.ItemWrapper.SortingType;
import sonar.logistics.api.wrappers.ItemWrapper.StorageItems;

public class FluidHelper extends FluidWrapper {

	public StorageFluids getFluids(INetworkCache network) {
		if (network instanceof IStorageCache) {
			StorageFluids stored = ((IStorageCache) network).getStoredFluids();
			return stored;
		}
		return StorageFluids.EMPTY;

	}

	public StoredFluidStack addFluids(StoredFluidStack add, INetworkCache network, ActionType action) {
		if (add.stored == 0) {
			return add;
		}
		Map<BlockCoords, ForgeDirection> connections = network.getExternalBlocks(true);
		for (Map.Entry<BlockCoords, ForgeDirection> entry : connections.entrySet()) {
			TileEntity tile = entry.getKey().getTileEntity();
			for (FluidHandler provider : SonarCore.fluidProviders.getObjects()) {
				if (provider.canHandleFluids(tile, entry.getValue())) {
					add = provider.addStack(add, tile, entry.getValue(), action);
					if (add == null) {
						return null;
					}
				}
			}
		}
		return add;
	}

	public StoredFluidStack removeFluids(StoredFluidStack remove, INetworkCache network, ActionType action) {
		if (remove.stored == 0) {
			return remove;
		}
		Map<BlockCoords, ForgeDirection> connections = network.getExternalBlocks(true);
		for (Map.Entry<BlockCoords, ForgeDirection> entry : connections.entrySet()) {
			TileEntity tile = entry.getKey().getTileEntity();
			for (FluidHandler provider : SonarCore.fluidProviders.getObjects()) {
				if (provider.canHandleFluids(tile, entry.getValue())) {
					remove = provider.removeStack(remove, tile, entry.getValue(), action);
					if (remove == null) {
						return null;
					}
				}
			}
		}
		return remove;
	}

	/** if simulating your expected to pass copies of both the container and stack to fill with */
	public ItemStack fillFluidItemStack(ItemStack container, StoredFluidStack fill, INetworkCache network, ActionType action) {
		if (FluidContainerRegistry.isContainer(container)) {
			return fillFluidContainer(container, fill, network, action);
		} else if ((container.getItem() instanceof IFluidContainerItem)) {
			return fillFluidHandler(container, fill, network, action);
		}
		return container;
	}

	/** if simulating your expected to pass copies of both the container and stack to fill with */
	public ItemStack drainFluidItemStack(ItemStack container, INetworkCache network, ActionType action) {
		if (FluidContainerRegistry.isContainer(container)) {
			return drainFluidContainer(container, network, action);
		} else if ((container.getItem() instanceof IFluidContainerItem)) {
			return drainFluidHandler(container, network, action);
		}
		return container;
	}

	public ItemStack fillFluidContainer(ItemStack container, StoredFluidStack fill, INetworkCache network, ActionType action) {
		FluidStack stack = FluidContainerRegistry.getFluidForFilledItem(container);
		int extractSize = 0;
		if (stack != null && stack.isFluidEqual(fill.fluid)) {
			extractSize = FluidContainerRegistry.getContainerCapacity(container) - stack.amount;
		} else if (stack == null) {
			if (container.getItem() == Items.bucket) {
				extractSize = FluidContainerRegistry.BUCKET_VOLUME;
			} else {
				extractSize = FluidContainerRegistry.getContainerCapacity(container);
			}
		}
		if (extractSize == 0) {
			return container;
		}
		StoredFluidStack remainder = removeFluids(fill.setStackSize(extractSize), network, action);
		FluidStack fillStack = fill.fluid.copy();
		if (remainder == null || remainder.stored == 0) {
			fillStack.amount = extractSize;
		} else {
			fillStack.amount = (int) (extractSize - remainder.stored);
		}

		ItemStack filledStack = FluidContainerRegistry.fillFluidContainer(fillStack, container);

		if (filledStack != null) {
			container = filledStack;
		}
		return container;
	}

	public ItemStack drainFluidContainer(ItemStack container, INetworkCache network, ActionType action) {
		FluidStack stack = FluidContainerRegistry.getFluidForFilledItem(container);
		if (stack != null) {
			StoredFluidStack remainder = addFluids(new StoredFluidStack(stack), network, action);
			if (remainder == null || remainder.stored == 0) {
				container = FluidContainerRegistry.drainFluidContainer(container);
			}
		}
		return container;
	}

	public ItemStack fillFluidHandler(ItemStack handler, StoredFluidStack fill, INetworkCache network, ActionType action) {
		IFluidContainerItem container = (IFluidContainerItem) handler.getItem();
		FluidStack stack = container.getFluid(handler);
		int extractSize = 0;
		if (stack != null && stack.isFluidEqual(fill.fluid)) {
			extractSize = (int) Math.min(fill.stored, container.getCapacity(handler) - stack.amount);
		} else if (stack == null) {
			extractSize = container.fill(handler, fill.getFullStack(), false);
		}
		if (extractSize == 0) {
			return handler;
		}
		StoredFluidStack remainder = LogisticsAPI.getFluidHelper().removeFluids(fill.setStackSize(extractSize), network, action);
		FluidStack fillStack = fill.fluid.copy();
		if (remainder == null || remainder.stored == 0) {
			fillStack.amount = extractSize;
		} else {
			fillStack.amount = (int) (extractSize - remainder.stored);
		}
		container.fill(handler, fillStack, true);
		return handler;

	}

	public ItemStack drainFluidHandler(ItemStack handler, INetworkCache network, ActionType action) {
		IFluidContainerItem container = (IFluidContainerItem) handler.getItem();
		FluidStack stack = container.getFluid(handler);
		if (stack != null) {
			FluidStack insertSize = container.drain(handler, Integer.MAX_VALUE, false);
			StoredFluidStack remainder = addFluids(new StoredFluidStack(insertSize), network, action);
			int drainSize = 0;
			if (remainder == null || remainder.stored == 0) {
				drainSize = insertSize.amount;
			} else {
				drainSize = (int) (insertSize.amount - remainder.stored);
			}
			container.drain(handler, drainSize, true);
		}
		return handler;
	}

	public void fillHeldItem(EntityPlayer player, INetworkCache cache, StoredFluidStack toFill) {
		ItemStack heldItem = player.getHeldItem();
		if (heldItem == null || toFill == null) {
			return;
		}
		if (heldItem.stackSize == 1) {
			ItemStack simulate = LogisticsAPI.getFluidHelper().fillFluidItemStack(heldItem.copy(), toFill.copy(), cache, ActionType.SIMULATE);
			if (!ItemStack.areItemStacksEqual(simulate, heldItem) || !ItemStack.areItemStackTagsEqual(simulate, heldItem)) {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, LogisticsAPI.getFluidHelper().fillFluidItemStack(heldItem, toFill, cache, ActionType.PERFORM));
			}
		} else {
			ItemStack insert = heldItem.copy();
			insert.stackSize = 1;

			ItemStack simulate = LogisticsAPI.getFluidHelper().fillFluidItemStack(insert.copy(), toFill.copy(), cache, ActionType.SIMULATE);
			if (!ItemStack.areItemStacksEqual(simulate, insert) || !ItemStack.areItemStackTagsEqual(simulate, insert)) {
				ItemStack toAdd = LogisticsAPI.getFluidHelper().fillFluidItemStack(insert, toFill, cache, ActionType.PERFORM);
				player.inventory.decrStackSize(player.inventory.currentItem, 1);
				StoredItemStack add = LogisticsAPI.getItemHelper().addStackToPlayer(new StoredItemStack(toAdd), player, false, ActionType.PERFORM);
			}
		}
	}

	public void drainHeldItem(EntityPlayer player, INetworkCache cache) {
		ItemStack heldItem = player.getHeldItem();
		if (heldItem == null) {
			return;
		}
		ItemStack insert = heldItem.copy();
		insert.stackSize = 1;
		ItemStack empty = LogisticsAPI.getFluidHelper().drainFluidItemStack(insert.copy(), cache, ActionType.PERFORM);
		if (!player.capabilities.isCreativeMode) {
			if (insert.stackSize == heldItem.stackSize) {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, empty);
			} else {
				player.inventory.decrStackSize(player.inventory.currentItem, 1);
				if (empty != null) {
					LogisticsAPI.getItemHelper().addStackToPlayer(new StoredItemStack(empty), player, false, ActionType.PERFORM);
				}
			}
		}
	}

	public static boolean writeStorageToNBT(NBTTagCompound tag, boolean lastWasNull, StorageFluids stacks, SyncType type) {
		if (type == SyncType.SYNC) {
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < stacks.fluids.size(); i++) {
				if (stacks.fluids.get(i) != null) {
					NBTTagCompound compound = new NBTTagCompound();
					StoredFluidStack.writeToNBT(compound, stacks.fluids.get(i));
					list.appendTag(compound);
				}
			}
			if (list.tagCount() != 0) {
				tag.setTag("c", list);
				return false;
			} else {
				tag.setBoolean("n", true);
				return true;
			}
		} else if (type == SyncType.SPECIAL) {
			if ((stacks.fluids == null || stacks.fluids.isEmpty())) {
				if (!lastWasNull)
					tag.setBoolean("n", true);
				return true;
			}
			NBTTagList list = new NBTTagList();
			for (int l = 0; l < 2; l++) {
				ArrayList<StoredFluidStack> stackList = null;
				switch (l) {
				case 0:
					stackList = stacks.changed;
					break;
				case 1:
					stackList = stacks.removed;
					break;
				}
				for (int i = 0; i < stackList.size(); i++) {
					if (stackList.get(i) != null) {
						NBTTagCompound compound = new NBTTagCompound();
						if (l == 1) {
							compound.setBoolean("r", true);
						}
						StoredFluidStack.writeToNBT(compound, stackList.get(i));
						list.appendTag(compound);
					}
				}
			}
			if (list.tagCount() != 0) {
				tag.setTag("s", list);
			}
		}
		return false;
	}

	public static void readStorageToNBT(NBTTagCompound tag, ArrayList<StoredFluidStack> current, SyncType type) {
		if (tag.hasKey("n")) {
			current.clear();
			return;
		}
		if (type == SyncType.SYNC) {
			if (!tag.hasKey("c")) {
				return;
			}
			NBTTagList list = tag.getTagList("c", 10);
			current.clear();
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound compound = list.getCompoundTagAt(i);
				current.add(StoredFluidStack.readFromNBT(compound));
			}
		} else if (type == SyncType.SPECIAL) {
			if (!tag.hasKey("s")) {
				return;
			}
			NBTTagList list = tag.getTagList("s", 10);
			tags: for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound compound = list.getCompoundTagAt(i);
				StoredFluidStack stack = StoredFluidStack.readFromNBT(compound);
				for (StoredFluidStack stored : (ArrayList<StoredFluidStack>) current.clone()) {
					if (stored.equalStack(stack.getFullStack())) {
						if (compound.getBoolean("r")) {
							current.remove(stored);
						} else {
							stored.setStackSize(stack.stored);
						}
						continue tags;
					}
				}
				current.add(stack);
			}
		}
	}

	public static void sortFluidList(ArrayList<StoredFluidStack> current, final SortingDirection dir, SortingType type) {
		current.sort(new Comparator<StoredFluidStack>() {
			public int compare(StoredFluidStack str1, StoredFluidStack str2) {
				
				int res = String.CASE_INSENSITIVE_ORDER.compare(str1.getFullStack().getLocalizedName(), str2.getFullStack().getLocalizedName());
				if (res == 0) {
					res = str1.getFullStack().getLocalizedName().compareTo(str2.getFullStack().getLocalizedName());
				}
				return dir == SortingDirection.DOWN ? res : -res;
			}
		});

		switch (type) {
		case STORED:
			current.sort(new Comparator<StoredFluidStack>() {
				public int compare(StoredFluidStack str1, StoredFluidStack str2) {
					if (str1.stored < str2.stored)
						return dir == SortingDirection.DOWN ? 1 : -1;
					if (str1.stored == str2.stored)
						return 0;
					return dir == SortingDirection.DOWN ? -1 : 1;
				}
			});
			break;
		case MODID:
			current.sort(new Comparator<StoredFluidStack>() {
				public int compare(StoredFluidStack str1, StoredFluidStack str2) {
					if (str1.getFullStack().getFluid().getTemperature() < str2.getFullStack().getFluid().getTemperature())
						return dir == SortingDirection.DOWN ? 1 : -1;
					if (str1.getFullStack().getFluid().getTemperature() == str2.getFullStack().getFluid().getTemperature())
						return 0;
					return dir == SortingDirection.DOWN ? -1 : 1;
				}
			});
		default:
			break;
		}
	}
}
