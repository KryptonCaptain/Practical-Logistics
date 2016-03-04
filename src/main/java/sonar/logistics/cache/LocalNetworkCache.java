package sonar.logistics.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import sonar.core.utils.BlockCoords;
import sonar.logistics.api.cache.CacheTypes;
import sonar.logistics.api.cache.INetworkCache;
import sonar.logistics.api.connecting.IConnectionNode;
import sonar.logistics.api.connecting.ILogicTile;

public class LocalNetworkCache implements INetworkCache {
	public final ILogicTile tile;

	public LocalNetworkCache(ILogicTile tile) {
		this.tile = tile;
	}

	@Override
	public Entry<BlockCoords, ForgeDirection> getExternalBlock() {
		try {
			if (tile instanceof IConnectionNode) {
				for (Entry<BlockCoords, ForgeDirection> entry : ((IConnectionNode) tile).getConnections().entrySet()) {
					if (entry.getKey().getBlock(entry.getKey().getWorld()) != null) {
						return entry;
					}
				}
			}
		} catch (Exception exception) {
		}
		return null;
	}

	@Override
	public LinkedHashMap<BlockCoords, ForgeDirection> getExternalBlocks() {
		LinkedHashMap map = new LinkedHashMap();
		try {
			map.putAll((((IConnectionNode) tile).getConnections()));
		} catch (Exception exception) {
			return new LinkedHashMap();
		}
		return map;
	}

	@Override
	public ArrayList<BlockCoords> getConnections(CacheTypes type) {
		ArrayList array = new ArrayList();
		try {
			array.add(tile.getCoords());
		} catch (Exception exception) {
		}
		return array;
	}

	@Override
	public BlockCoords getFirstConnection(CacheTypes type) {
		return tile.getCoords();
	}

	@Override
	public Block getFirstBlock(CacheTypes type) {
		try {
			return tile.getCoords().getBlock();
		} catch (Exception exception) {
		}
		return null;
	}

	@Override
	public TileEntity getFirstTileEntity(CacheTypes type) {
		try {
			return tile.getCoords().getTileEntity();
		} catch (Exception exception) {
		}
		return null;
	}
}
