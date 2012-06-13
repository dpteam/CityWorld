package me.daddychurchill.CityWorld.Plats;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import me.daddychurchill.CityWorld.WorldGenerator;
import me.daddychurchill.CityWorld.PlatMap;
import me.daddychurchill.CityWorld.Context.ContextData;
import me.daddychurchill.CityWorld.Support.ByteChunk;
import me.daddychurchill.CityWorld.Support.Direction.Ladder;
import me.daddychurchill.CityWorld.Support.Direction.Stair;
import me.daddychurchill.CityWorld.Support.HeightInfo;
import me.daddychurchill.CityWorld.Support.RealChunk;

public abstract class PlatLot {
	
	// extremes
	private boolean extremeComputed = false;
	protected int averageHeight;
	protected int minHeight = Integer.MAX_VALUE;
	protected int minHeightX = 0;
	protected int minHeightZ = 0;
	protected int maxHeight = Integer.MIN_VALUE;
	protected int maxHeightX = 0;
	protected int maxHeightZ = 0;
	
	public enum LotStyle {NATURE, STRUCTURE, ROAD, ROUNDABOUT};
	public LotStyle style;
	
	public PlatLot(Random random) {
		super();
		style = LotStyle.NATURE;
	}
	
	protected final static byte airId = (byte) Material.AIR.getId();
	protected final static byte snowId = (byte) Material.SNOW_BLOCK.getId();
	protected final static byte grassId = (byte) Material.GRASS.getId();
	protected final static byte dirtId = (byte) Material.DIRT.getId();
	protected final static byte stoneId = (byte) Material.STONE.getId();
	protected final static byte waterId = (byte) Material.STATIONARY_WATER.getId();
	protected final static byte sandId = (byte) Material.SAND.getId();
	protected final static byte sandstoneId = (byte) Material.SANDSTONE.getId();
	protected final static byte bedrockId = (byte) Material.BEDROCK.getId();
	protected final static byte lavaId = (byte) Material.LAVA.getId();
	protected final static byte fenceId = (byte) Material.FENCE.getId();

	public abstract long getConnectedKey();
	public abstract boolean makeConnected(Random random, PlatLot relative);
	public abstract boolean isConnectable(PlatLot relative);
	public abstract boolean isIsolatedLot(Random random, int oddsOfIsolation);
	public abstract boolean isConnected(PlatLot relative);
	
	public void generateChunk(WorldGenerator generator, PlatMap platmap, ByteChunk chunk, BiomeGrid biomes, ContextData context, int platX, int platZ) {
		
		// total height
		int sumHeight = 0;
		
		// compute offset to start of chunk
		int originX = chunk.getOriginX();
		int originZ = chunk.getOriginZ();
		
		// surface caves?
		boolean surfaceCaves = generator.isSurfaceCaveAt(chunk.chunkX, chunk.chunkZ);
		
		// shape the world
		for (int x = 0; x < chunk.width; x++) {
			for (int z = 0; z < chunk.width; z++) {

				// how high are we?
				int y = generator.findBlockY(originX + x, originZ + z);
				
				// keep the tally going
				sumHeight += y;
				if (y < minHeight) {
					minHeight = y;
					minHeightX = x;
					minHeightZ = z;
				}
				if (y > maxHeight) {
					maxHeight = y;
					maxHeightX = x;
					maxHeightZ = z;
				}

				// make the base
				chunk.setBlock(x, 0, z, bedrockId);

				// buildable?
				if (style == LotStyle.STRUCTURE || style == LotStyle.ROUNDABOUT) {
					generateCrust(generator, chunk, x, z, stoneId, generator.sidewalkLevel - 2, dirtId, generator.sidewalkLevel, dirtId, false);
					biomes.setBiome(x, z, Biome.JUNGLE);
					
				// possibly buildable?
				} else if (y == generator.sidewalkLevel) {
					generateCrust(generator, chunk, x, z, stoneId, y - 3, dirtId, y, grassId, false);
					biomes.setBiome(x, z, Biome.PLAINS);
				
				// won't likely have a building
				} else {

					// on the beach
					if (y == generator.seaLevel) {
						generateCrust(generator, chunk, x, z, stoneId, y - 2, sandId, y, sandId, false);
						biomes.setBiome(x, z, Biome.BEACH);

						// we are in the water!
					} else if (y < generator.seaLevel) {
						generateCrust(generator, chunk, x, z, stoneId, y - 2, sandstoneId, y, sandId, generator.seaLevel, waterId, surfaceCaves);
						biomes.setBiome(x, z, Biome.OCEAN);

						// we are in the mountains
					} else {

						// regular trees only
						if (y < generator.treeLevel) {
							generateCrust(generator, chunk, x, z, stoneId, y - 3, dirtId, y, grassId, false);
							biomes.setBiome(x, z, Biome.FOREST_HILLS);

						// regular trees and some evergreen trees
						} else if (y < generator.evergreenLevel) {
							generateCrust(generator, chunk, x, z, stoneId, y - 2, dirtId, y, grassId, surfaceCaves);
							biomes.setBiome(x, z, Biome.EXTREME_HILLS);

						// evergreen and some of fallen snow
						} else if (y < generator.snowLevel) {
							generateCrust(generator, chunk, x, z, stoneId, y - 1, dirtId, y, grassId, surfaceCaves);
							biomes.setBiome(x, z, Biome.ICE_MOUNTAINS);
							
						// only snow up here!
						} else {
							generateCrust(generator, chunk, x, z, stoneId, y - 1, stoneId, y, snowId, surfaceCaves);
							biomes.setBiome(x, z, Biome.ICE_PLAINS);
						}
					}
				}
			}
		}
		
		// what was the average height
		averageHeight = sumHeight / (chunk.width * chunk.width);
		extremeComputed = true;
		
		// get shafted!
		for (int y = 0; y + 16 < minHeight; y += 16) {
			generateHorizontalMineShafts(generator, chunk, context, y);
		}
	}

	private void generateHorizontalMineShafts(WorldGenerator generator, ByteChunk chunk, ContextData context, int y) {
		int y1 = y + 6;
		int y2 = y1 + 1;
		
		// draw the shafts/walkways
		boolean pathFound = false;
		if (generator.getHorizontalNSShaft(chunk.chunkX, y, chunk.chunkZ)) {
			generateShaftSpace(chunk, 6, 10, y1, y1 + 4, 0, 6);
			generateNSSupport(chunk, 6, y2, 1);
			generateNSSupport(chunk, 6, y2, 4);
			generateShaftSpace(chunk, 6, 10, y1, y1 + 4, 10, 16);
			generateNSSupport(chunk, 6, y2, 11);
			generateNSSupport(chunk, 6, y2, 14);
			pathFound = true;
		}
		if (generator.getHorizontalWEShaft(chunk.chunkX, y, chunk.chunkZ)) {
			generateShaftSpace(chunk, 0, 6, y1, y1 + 4, 6, 10);
			generateWESupport(chunk, 1, y2, 6);
			generateWESupport(chunk, 4, y2, 6);
			generateShaftSpace(chunk, 10, 16, y1, y1 + 4, 6, 10);
			generateWESupport(chunk, 11, y2, 6);
			generateWESupport(chunk, 14, y2, 6);
			pathFound = true;
		}
		
		// draw the center bit
		if (pathFound)
			generateShaftSpace(chunk, 6, 10, y1, y1 + 4, 6, 10);
	}
	
	private final static byte shaftBridgeId = (byte) Material.WOOD.getId(); 
	private final static byte shaftSupportId = (byte) Material.FENCE.getId();
	private final static byte shaftBeamId = (byte) Material.STEP.getId(); //TODO need to switch this over to wooden slabs

	private void generateShaftSpace(ByteChunk chunk, int x1, int x2, int y1, int y2, int z1, int z2) {
		chunk.setEmptyBlocks(x1, x2, y1, z1, z2, shaftBridgeId);
		chunk.setBlocks(x1, x2, y1 + 1, y2, z1, z2, airId);
	}
	
	private void generateNSSupport(ByteChunk chunk, int x, int y, int z) {
		chunk.setBlock(x, y, z, shaftSupportId);
		chunk.setBlock(x, y + 1, z, shaftSupportId);
		chunk.setBlock(x + 3, y, z, shaftSupportId);
		chunk.setBlock(x + 3, y + 1, z, shaftSupportId);
		chunk.setBlocks(x, x + 4, y + 2, z, z + 1, shaftBeamId);
	}
	
	private void generateWESupport(ByteChunk chunk, int x, int y, int z) {
		chunk.setBlock(x, y, z, shaftSupportId);
		chunk.setBlock(x, y + 1, z, shaftSupportId);
		chunk.setBlock(x, y, z + 3, shaftSupportId);
		chunk.setBlock(x, y + 1, z + 3, shaftSupportId);
		chunk.setBlocks(x, x + 1, y + 2, z, z + 4, shaftBeamId);
	}
	
	public void generateBlocks(WorldGenerator generator, PlatMap platmap, RealChunk chunk, ContextData context, int platX, int platZ) {
		
		// have we done this yet?
		if (!extremeComputed) {
			HeightInfo heights = HeightInfo.getHeights(generator, chunk.worldX, chunk.worldZ);
			averageHeight = heights.averageHeight;
			minHeight = heights.minHeight;
			minHeightX = heights.minHeightX;
			minHeightZ = heights.minHeightZ;
			maxHeight = heights.maxHeight;
			maxHeightX = heights.maxHeightX;
			maxHeightZ = heights.maxHeightZ;
			extremeComputed = true;
		}
		
		// get shafted!
		for (int y = 0; y + 16 < minHeight; y += 16) {
			generateVerticalMineShafts(generator, chunk, context, y);
		}
		
		//TODO additional natural sub-terrain structures, if any
		
	}
	
	private void generateVerticalMineShafts(WorldGenerator generator, RealChunk chunk, ContextData context, int y) {
		int y1 = y + 6;
		int y2 = y1 + 1;
		boolean stairsFound = false;
		
		// going down?
		if (y - 16 > 0) {
			if (generator.getHorizontalNSShaft(chunk.chunkX, y, chunk.chunkZ) &&
				generator.getHorizontalNSShaft(chunk.chunkX, y - 16, chunk.chunkZ)) {
				
				// draw the going down bit
				placeStairBase(chunk, 10, y1	, 15);
				placeStairStep(chunk, 10, y1    , 14, Stair.SOUTH);
				placeStairStep(chunk, 10, y1 - 1, 13, Stair.SOUTH);
				placeStairStep(chunk, 10, y1 - 2, 12, Stair.SOUTH);
				placeStairStep(chunk, 10, y1 - 3, 11, Stair.SOUTH);
				placeStairStep(chunk, 10, y1 - 4, 10, Stair.SOUTH);
				placeStairStep(chunk, 10, y1 - 5,  9, Stair.SOUTH);
				placeStairStep(chunk, 10, y1 - 6,  8, Stair.SOUTH);
				
				// only one type of stairs are allowed
				stairsFound = true;
			}
			
			if (!stairsFound &&
				generator.getHorizontalWEShaft(chunk.chunkX, y, chunk.chunkZ) &&
				generator.getHorizontalWEShaft(chunk.chunkX, y - 16, chunk.chunkZ)) {
				
				// draw the going down bit
				placeStairBase(chunk, 15, y1	, 10);
				placeStairStep(chunk, 14, y1    , 10, Stair.EAST);
				placeStairStep(chunk, 13, y1 - 1, 10, Stair.EAST);
				placeStairStep(chunk, 12, y1 - 2, 10, Stair.EAST);
				placeStairStep(chunk, 11, y1 - 3, 10, Stair.EAST);
				placeStairStep(chunk, 10, y1 - 4, 10, Stair.EAST);
				placeStairStep(chunk,  9, y1 - 5, 10, Stair.EAST);
				placeStairStep(chunk,  8, y1 - 6, 10, Stair.EAST);
			}
		}
		
		// reset the flag
		stairsFound = false;
		
		// going up?
		if (y + 16 < minHeight) {
			if (generator.getHorizontalNSShaft(chunk.chunkX, y, chunk.chunkZ) &&
				generator.getHorizontalNSShaft(chunk.chunkX, y + 16, chunk.chunkZ)) {
					
				// draw the going up bit
				placeStairBase(chunk,  5, y1	, 15);
				placeStairStep(chunk,  5, y1 + 1, 14, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 2, 13, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 3, 12, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 4, 11, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 5, 10, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 6,  9, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 7,  8, Stair.NORTH);
				placeStairStep(chunk,  5, y1 + 8,  7, Stair.NORTH);
				placeStairBase(chunk,  5, y1 + 8,  6);
				placeStairBase(chunk,  6, y1 + 8,  6);
				placeStairBase(chunk,  7, y1 + 8,  6);
				placeStairBase(chunk,  8, y1 + 8,  6);
				placeStairBase(chunk,  9, y1 + 8,  6);
				placeStairBase(chunk, 10, y1 + 8,  6);
				placeStairStep(chunk, 10, y1 + 9,  7, Stair.SOUTH);
				
				// only one type of stairs are allowed
				stairsFound = true;
			}
			
			if (!stairsFound &&
				generator.getHorizontalWEShaft(chunk.chunkX, y, chunk.chunkZ) &&
				generator.getHorizontalWEShaft(chunk.chunkX, y + 16, chunk.chunkZ)) {
				
				// draw the going up bit
				placeStairBase(chunk, 15, y1	,  5);
				placeStairStep(chunk, 14, y1 + 1,  5, Stair.WEST);
				placeStairStep(chunk, 13, y1 + 2,  5, Stair.WEST);
				placeStairStep(chunk, 12, y1 + 3,  5, Stair.WEST);
				placeStairStep(chunk, 11, y1 + 4,  5, Stair.WEST);
				placeStairStep(chunk, 10, y1 + 5,  5, Stair.WEST);
				placeStairStep(chunk,  9, y1 + 6,  5, Stair.WEST);
				placeStairStep(chunk,  8, y1 + 7,  5, Stair.WEST);
				placeStairStep(chunk,  7, y1 + 8,  5, Stair.WEST);
				placeStairBase(chunk,  6, y1 + 8,  5);
				placeStairBase(chunk,  6, y1 + 8,  6);
				placeStairBase(chunk,  6, y1 + 8,  7);
				placeStairBase(chunk,  6, y1 + 8,  8);
				placeStairBase(chunk,  6, y1 + 8,  9);
				placeStairBase(chunk,  6, y1 + 8, 10);
				placeStairStep(chunk,  7, y1 + 9, 10, Stair.EAST);
			}
		}
	}
	
	private void placeStairBase(RealChunk chunk, int x, int y, int z) {
		chunk.setBlocks(x, y + 1, y + 3, z, Material.AIR);
		chunk.setEmptyBlock(x, y, z, Material.WOOD);
	}
	
	private void placeStairStep(RealChunk chunk, int x, int y, int z, Stair direction) {
		chunk.setBlocks(x, y + 1, y + 3, z, Material.AIR);
		chunk.setStair(x, y, z, Material.WOOD_STAIRS, direction);
	}
	
	private void generateCrust(WorldGenerator generator, ByteChunk byteChunk, int x, int z, byte baseId,
			int baseY, byte substrateId, int substrateY, byte surfaceId,
			boolean surfaceCaves) {

		byteChunk.setBlock(x, baseY, z, surfaceId);
		
//		// compute the world block coordinates
//		int blockX = byteChunk.chunkX * byteChunk.width + x;
//		int blockZ = byteChunk.chunkZ * byteChunk.width + z;
//
//		// stony bits
//		for (int y = 1; y < baseY; y++)
//			if (generator.notACave(blockX, y, blockZ))
////				byteChunk.setBlock(x, y, z, baseId);
//				byteChunk.setBlock(x, y, z, generator.getOre(byteChunk, blockX, y, blockZ, baseId));
//
//		// aggregate bits
//		for (int y = baseY; y < substrateY; y++)
//			if (!surfaceCaves || generator.notACave(blockX, y, blockZ))
//				byteChunk.setBlock(x, y, z, substrateId);
//
//		// icing for the cake
//		if (!surfaceCaves || generator.notACave(blockX, substrateY, blockZ))
//			byteChunk.setBlock(x, substrateY, z, surfaceId);

	}

	private void generateCrust(WorldGenerator generator, ByteChunk byteChunk, int x, int z, byte baseId,
			int baseY, byte substrateId, int substrateY, byte surfaceId,
			int coverY, byte coverId, boolean surfaceCaves) {

		// a little crust please?
		generateCrust(generator, byteChunk, x, z, baseId, baseY, substrateId, substrateY, surfaceId, surfaceCaves);

//		// cover it up
//		for (int y = substrateY + 1; y <= coverY; y++)
//			byteChunk.setBlock(x, y, z, coverId);
	}

	//TODO move this logic to SurroundingLots, add to it the ability to produce SurroundingHeights and SurroundingDepths
	public PlatLot[][] getNeighborPlatLots(PlatMap platmap, int platX, int platZ, boolean onlyConnectedNeighbors) {
		PlatLot[][] miniPlatMap = new PlatLot[3][3];
		
		// populate the results
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				
				// which platchunk are we looking at?
				int atX = platX + x - 1;
				int atZ = platZ + z - 1;

				// is it in bounds?
				if (!(atX < 0 || atX > PlatMap.Width - 1 || atZ < 0 || atZ > PlatMap.Width - 1)) {
					PlatLot relative = platmap.getLot(atX, atZ);
					
					if (!onlyConnectedNeighbors || isConnected(relative)) {
						miniPlatMap[x][z] = relative;
					}
				}
			}
		}
		
		return miniPlatMap;
	}
}
