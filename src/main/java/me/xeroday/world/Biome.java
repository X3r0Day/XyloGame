package me.xeroday.world;

import me.xeroday.models.Models;
import java.util.Random;

public enum Biome {
    OCEAN(0.5f, 0.5f, Block.SAND, Block.SAND, 0.0f),
    BEACH(0.5f, 0.4f, Block.SAND, Block.SAND, 0.0f),

    // MID
    PLAINS(0.5f, 0.4f, Block.GRASS, Block.DIRT, 0.05f),
    FOREST(0.5f, 0.8f, Block.GRASS, Block.DIRT, 0.9f),

    // MOUNTAINS
    MOUNTAINS(0.2f, 0.4f, Block.STONE, Block.STONE, 0.1f),
    SNOWY_MOUNTAINS(-1.0f, 0.5f, Block.SNOW, Block.STONE, 0.0f),

    // EXTREMES
    DESERT(2.0f, 0.0f, Block.SAND, Block.SAND, 0.0f),
    SNOWY_PLAINS(-1.0f, 0.5f, Block.SNOW, Block.DIRT, 0.05f);

    public final float temperature;
    public final float humidity;
    public final Block topBlock;
    public final Block fillerBlock;
    public final float treeDensity;

    Biome(float temp, float humidity, Block top, Block filler, float treeDensity) {
        this.temperature = temp;
        this.humidity = humidity;
        this.topBlock = top;
        this.fillerBlock = filler;
        this.treeDensity = treeDensity;
    }

    public static Biome getBiome(float continent, float temp, float humidity) {
        // OCEAN & COAST
        if (continent < -0.10f) return OCEAN;
        if (continent < -0.05f) return BEACH;

        // MOUNTAIN check (High Continent Value)
        if (continent > 0.6f) {
            if (temp < -0.2f) return SNOWY_MOUNTAINS;
            return MOUNTAINS;
        }

        // INLAND BIOMES (Based on Temp & Humidity)
        if (temp < -0.3f) {
            // Cold Zone
            return SNOWY_PLAINS;
        }
        else if (temp > 0.4f) {
            // Hot Zone
            if (humidity < 0.0f) return DESERT;
            return PLAINS; // Savanna could go here
        }
        else {
            // Mid zone
            if (humidity > 0.3f) return FOREST;
            return PLAINS;
        }
    }

    public void generateVegetation(int x, int y, int z, Random r, Chunk chunk) {
        if (this == MOUNTAINS || this == SNOWY_MOUNTAINS || this == OCEAN || this == BEACH) return;

        // Tree Generation
        if (r.nextFloat() < (this.treeDensity * 0.1f)) { // Adjusted density multiplier
            if (this == FOREST) chunk.placeModel(x, y + 1, z, Models.OAK_TREE);
            else if (this == PLAINS && r.nextInt(5) == 0) chunk.placeModel(x, y + 1, z, Models.OAK_TREE); // Rare trees in plains
            else if (this == SNOWY_PLAINS && r.nextInt(10) == 0) chunk.placeModel(x, y + 1, z, Models.OAK_TREE); // Rare pine trees
            return;
        }

        // Grass/Bushes
        if (this.topBlock == Block.GRASS) {
            if (r.nextInt(10) == 0) chunk.placeModel(x, y + 1, z, Models.TALL_GRASS);
            else if (r.nextInt(15) == 0) chunk.setB(x, y + 1, z, Block.PLANT_GRASS.id);
        }
        else if (this == DESERT) {
            if (r.nextInt(150) == 0) chunk.placeModel(x, y + 1, z, Models.BUSH_SHORT);
        }
    }
}