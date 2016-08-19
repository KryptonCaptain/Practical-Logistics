package sonar.logistics.common.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.core.inventory.ContainerSync;
import sonar.core.inventory.slots.SlotList;

public class ContainerItemRouter extends ContainerSync {

	private static final int INV_START = 9, INV_END = INV_START + 26, HOTBAR_START = INV_END + 1, HOTBAR_END = HOTBAR_START + 8;

	public int state = 0;

	public ContainerItemRouter(TileEntityItemRouter entity, InventoryPlayer inventoryPlayer) {
		super(entity);
		addSlots(inventoryPlayer, entity);
	}

	public void addSlots(InventoryPlayer player, TileEntityItemRouter entity) {
		if (state == 0) {
			for (int i = 0; i < 3; ++i) {
				for (int j = 0; j < 9; ++j) {
					this.addSlotToContainer(new Slot(player, j + i * 9 + 9, 8 + j * 18, 151 + i * 18));
				}
			}

			for (int i = 0; i < 9; ++i) {
				this.addSlotToContainer(new Slot(player, i, 8 + i * 18, 209));
			}
			for (int i = 0; i < 9; ++i) {
				addSlotToContainer(new Slot(entity, i, 8 + i * 18, 129));
			}
		} else {
			for (int i = 0; i < 3; ++i) {
				for (int j = 0; j < 9; ++j) {
					this.addSlotToContainer(new Slot(player, j + i * 9 + 9, 8 + j * 18, 56 + i * 18));
				}
			}

			for (int i = 0; i < 9; ++i) {
				this.addSlotToContainer(new Slot(player, i, 8 + i * 18, 114));
			}
			if (state == 1)
				addSlotToContainer(new SlotList(entity.handler.clientStackFilter, 0, 23, 23));

		}
	}

	public void switchState(InventoryPlayer player, TileEntityItemRouter entity, int state) {
		entity.handler.resetClientStackFilter();
		entity.handler.resetClientOreFilter();
		this.state = state;
		this.inventoryItemStacks.clear();
		this.inventorySlots.clear();
		this.addSlots(player, entity);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	public ItemStack transferStackInSlot(EntityPlayer player, int id) {
		ItemStack itemstack = null;
		Slot slot = (Slot) this.inventorySlots.get(id);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (state == 0) {

				if (id < 36) {
					if (!this.mergeItemStack(itemstack1, 36, 45, false)) {
						return null;
					}

				} else if (id < 27) {
					if (!this.mergeItemStack(itemstack1, 27, 36, false)) {
						return null;
					}
				} else if (id >= 27 && id < 36) {
					if (!this.mergeItemStack(itemstack1, 0, 27, false)) {
						return null;
					}
				} else if (!this.mergeItemStack(itemstack1, 0, 36, false)) {
					return null;
				}

				if (itemstack1.stackSize == 0) {
					slot.putStack((ItemStack) null);
				} else {
					slot.onSlotChanged();

				}
			} else {
				if (state == 1 && id < 36) {
					((Slot) this.inventorySlots.get(36)).putStack(itemstack1.copy());

				} else if (id < 27) {
					if (!this.mergeItemStack(itemstack1, 27, 36, false)) {
						return null;
					}
				} else if (id >= 27 && id < 36) {
					if (!this.mergeItemStack(itemstack1, 0, 27, false)) {
						return null;
					}
				} else if (!this.mergeItemStack(itemstack1, 0, 36, false)) {
					return null;
				}

				if (itemstack1.stackSize == 0) {
					slot.putStack((ItemStack) null);
				} else {
					slot.onSlotChanged();
				}

				if (itemstack1.stackSize == itemstack.stackSize) {
					return null;
				}

				slot.onPickupFromSlot(player, itemstack1);

			}
		}
		return itemstack;
	}

    public ItemStack slotClick(int slotID, int drag, ClickType click, EntityPlayer player){
		if (state == 1) {
			Slot targetSlot = slotID < 0 ? null : (Slot) this.inventorySlots.get(slotID);
			if ((targetSlot instanceof SlotList)) {
				if (drag == 1) {
					targetSlot.putStack(null);
				} else {
					targetSlot.putStack(player.inventory.getItemStack() == null ? null : player.inventory.getItemStack().copy());
				}
				return player.inventory.getItemStack();
			}
		}
		return super.slotClick(slotID, drag, click, player);
	}

	public SyncType[] getSyncTypes() {
		return new SyncType[] { SyncType.SPECIAL };
	}
}
