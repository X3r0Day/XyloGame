package me.xeroday.models;

import me.xeroday.world.Block;
import java.io.*;
import java.nio.file.*;

/**
 * This class pre generates the model files so they dont have to be generated everytime
 */


public class Models {
    private static final byte[] SRC_BOULDER_SMALL = {
            0,0,0, Block.STONE.id,  0,0,1, Block.STONE.id
    };

    private static final byte[] SRC_BOULDER_MED = {
            0,0,0, Block.STONE.id,  1,0,0, Block.STONE.id,
            0,0,1, Block.STONE.id,  1,0,1, Block.STONE.id,
            0,1,0, Block.STONE.id
    };

    private static final byte[] SRC_BUSH_SHORT = {
            0,0,0, Block.LOG.id,
            1,0,0, Block.LEAVES.id, -1,0,0, Block.LEAVES.id,
            0,0,1, Block.LEAVES.id,  0,0,-1, Block.LEAVES.id,
            0,1,0, Block.LEAVES.id
    };

    private static final byte[] SRC_BUSH_TALL = {
            0,0,0, Block.LOG.id,
            1,0,0, Block.LEAVES.id, -1,0,0, Block.LEAVES.id,
            0,0,1, Block.LEAVES.id,  0,0,-1, Block.LEAVES.id,
            0,1,0, Block.LEAVES.id,
            0,2,0, Block.LEAVES.id
    };

    private static final byte[] SRC_TALL_GRASS = {
            0, 0, 0, Block.PLANT_TALL_BOT.id,
            0, 1, 0, Block.PLANT_TALL_TOP.id
    };

    private static final byte[] SRC_OAK = {
            0,0,0, Block.LOG.id, 0,1,0, Block.LOG.id, 0,2,0, Block.LOG.id,
            0,3,0, Block.LOG.id, 0,4,0, Block.LOG.id, 0,5,0, Block.LOG.id,
            -2,3,-1, Block.LEAVES.id, -2,3,0, Block.LEAVES.id, -2,3,1, Block.LEAVES.id,
            -1,3,-2, Block.LEAVES.id, -1,3,-1, Block.LEAVES.id, -1,3,0, Block.LEAVES.id,
            -1,3,1, Block.LEAVES.id, -1,3,2, Block.LEAVES.id,
            0,3,-2, Block.LEAVES.id, 0,3,-1, Block.LEAVES.id,
            0,3, 1, Block.LEAVES.id, 0,3, 2, Block.LEAVES.id,
            1,3,-2, Block.LEAVES.id, 1,3,-1, Block.LEAVES.id,
            1,3, 0, Block.LEAVES.id, 1,3, 1, Block.LEAVES.id,
            1,3, 2, Block.LEAVES.id,
            2,3,-1, Block.LEAVES.id, 2,3,0, Block.LEAVES.id, 2,3,1, Block.LEAVES.id,
            -2,4,0, Block.LEAVES.id,
            -1,4,-1, Block.LEAVES.id, -1,4,0, Block.LEAVES.id, -1,4,1, Block.LEAVES.id,
            0,4,-2, Block.LEAVES.id,  0,4,-1, Block.LEAVES.id,
            0,4, 1, Block.LEAVES.id,  0,4, 2, Block.LEAVES.id,
            1,4,-1, Block.LEAVES.id,  1,4,0, Block.LEAVES.id,  1,4,1, Block.LEAVES.id,
            2,4,0, Block.LEAVES.id,
            -1,5,0, Block.LEAVES.id,
            0,5,-1, Block.LEAVES.id, 0,5,1, Block.LEAVES.id,
            1,5,0, Block.LEAVES.id,
            0,6,0, Block.LEAVES.id
    };

    public static byte[] OAK_TREE;
    public static byte[] BOULDER_SMALL;
    public static byte[] BOULDER_MED;
    public static byte[] BUSH_SHORT;
    public static byte[] BUSH_TALL;
    public static byte[] TALL_GRASS;

    static {
        try {
            OAK_TREE      = loadOrGenerate("oak.model", SRC_OAK);
            BOULDER_SMALL = loadOrGenerate("boulder_small.model", SRC_BOULDER_SMALL);
            BOULDER_MED   = loadOrGenerate("boulder_med.model", SRC_BOULDER_MED);
            BUSH_SHORT    = loadOrGenerate("bush_short.model", SRC_BUSH_SHORT);
            BUSH_TALL     = loadOrGenerate("bush_tall.model", SRC_BUSH_TALL);
            TALL_GRASS    = loadOrGenerate("tall_grass.model", SRC_TALL_GRASS);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load models", e);
        }
    }

    private static byte[] loadOrGenerate(String name, byte[] src) throws IOException {
        Path path = Paths.get("models", name);
        if (Files.exists(path)) return Files.readAllBytes(path);
        Files.createDirectories(path.getParent());
        Files.write(path, src);
        return src;
    }
}
