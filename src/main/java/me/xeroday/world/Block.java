package me.xeroday.world;

public class Block {
    public static Block[] blocks = new Block[256];

    // Colors
    private static final float[] C_DEFAULT = {1.0f, 1.0f, 1.0f};
    private static final float[] C_GRASS   = {0.57f, 0.74f, 0.35f}; // Biome Green
    private static final float[] C_FLUID   = {0.8f, 0.8f, 0.9f};    // Water Blue

    // Tint Types
    public static final int TINT_NONE = 0; // Never tint
    public static final int TINT_ALL  = 1; // Tint all 6 sides (Leaves, Water)
    public static final int TINT_TOP  = 2; // Tint ONLY the top face (Grass Block)

    // BLOCKS from here
    public static Block AIR    = new Block(0, 0, 0, 0, C_DEFAULT, TINT_NONE, false, false);

    // Grass: Uses TINT_TOP so side/bottom stay as is as top is grayscaled
    public static Block GRASS  = new Block(1, 0, 2, 1, C_GRASS, TINT_TOP, true, false);

    // Leaves/Water: TINT_ALL
    public static Block LEAVES = new Block(5, 6, 6, 6, C_GRASS, TINT_ALL, false, false);
    public static Block WATER  = new Block(7, 8, 8, 8, C_FLUID, TINT_ALL, false, false);

    // Plants: TINT_ALL
    public static Block PLANT_GRASS    = new Block(10, 10, 10, 10, C_GRASS, TINT_ALL, false, true);
    public static Block PLANT_TALL_BOT = new Block(11, 11, 11, 11, C_GRASS, TINT_ALL, false, true);
    public static Block PLANT_TALL_TOP = new Block(12, 12, 12, 12, C_GRASS, TINT_ALL, false, true);
    public static Block LAVA   = new Block(14, 14, 14, 14, C_FLUID, TINT_ALL, false, false);

    // Normal: TINT_NONE
    public static Block DIRT   = new Block(2, 2, 2, 2, C_DEFAULT, TINT_NONE, true, false);
    public static Block STONE  = new Block(3, 3, 3, 3, C_DEFAULT, TINT_NONE, true, false);
    public static Block LOG    = new Block(4, 5, 5, 4, C_DEFAULT, TINT_NONE, true, false);
    public static Block SAND   = new Block(6, 7, 7, 7, C_DEFAULT, TINT_NONE, true, false);
    public static Block SNOW   = new Block(8, 9, 2, 9, C_DEFAULT, TINT_NONE, true, false);
    public static Block DEEPSLATE = new Block(13, 13, 13, 13, C_DEFAULT, TINT_NONE, true, false);


    public final byte id;
    public final int top, bot, side;
    public final float r, g, b;
    public final int tintType;
    public final boolean opaque;
    public final boolean isPlant;

    public Block(int id, int top, int bot, int side, float[] color, int tintType, boolean opaque, boolean isPlant) {
        this.id = (byte) id;
        this.top = top; this.bot = bot; this.side = side;
        this.r = color[0]; this.g = color[1]; this.b = color[2];
        this.tintType = tintType;
        this.opaque = opaque;
        this.isPlant = isPlant;
        blocks[id] = this;
    }

    public static Block get(byte id) { return blocks[id] != null ? blocks[id] : AIR; }
}