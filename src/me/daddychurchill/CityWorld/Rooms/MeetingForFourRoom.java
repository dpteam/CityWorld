package me.daddychurchill.CityWorld.Rooms;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import me.daddychurchill.CityWorld.CityWorldGenerator;
import me.daddychurchill.CityWorld.Support.Odds;
import me.daddychurchill.CityWorld.Support.RealBlocks;

public class MeetingForFourRoom extends MeetingRoom {

	public MeetingForFourRoom() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void drawFixture(CityWorldGenerator generator, RealBlocks chunk, Odds odds, int floor, int x, int y, int z,
			int width, int height, int depth, BlockFace sideWithWall, Material materialWall, Material materialGlass) {

		Material tableLeg = getTableLeg(odds);
		Material tableTop = getTableTop(odds);

		switch (sideWithWall) {
		default:
		case NORTH:
		case SOUTH:
			chunk.setBlock(x, y, z, Material.BIRCH_STAIRS, BlockFace.WEST);
			chunk.setBlock(x, y, z + 2, Material.BIRCH_STAIRS, BlockFace.WEST);

			chunk.setTable(x + 1, x + 2, y, z, z + 3, tableLeg, tableTop);

			chunk.setBlock(x + 2, y, z, Material.BIRCH_STAIRS, BlockFace.EAST);
			chunk.setBlock(x + 2, y, z + 2, Material.BIRCH_STAIRS, BlockFace.EAST);
			break;
		case WEST:
		case EAST:
			chunk.setBlock(x, y, z, Material.BIRCH_STAIRS, BlockFace.NORTH);
			chunk.setBlock(x + 2, y, z, Material.BIRCH_STAIRS, BlockFace.NORTH);

			chunk.setTable(x, x + 3, y, z + 1, z + 2, tableLeg, tableTop);

			chunk.setBlock(x, y, z + 2, Material.BIRCH_STAIRS, BlockFace.SOUTH);
			chunk.setBlock(x + 2, y, z + 2, Material.BIRCH_STAIRS, BlockFace.SOUTH);
			break;
		}
	}

}
