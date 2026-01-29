package me.xeroday.world;

import me.xeroday.utils.MathUtils;
import me.xeroday.utils.MeshBuilder;
import me.xeroday.utils.PerlinNoise;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL33.*;

public class Chunk {
    public static int CHUNKS_PER_FRAME = 16;
    public static int chunksLoadedCount = 0;
    public static void nextFrame() { chunksLoadedCount = 0; }

    // CHUNK, WORLD HEIGHT, WATER LVL
    public static final int SIZE = 16;
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 320;
    private static final int SEA_LEVEL = 70;

    private final byte[][][] blocks = new byte[SIZE][MAX_Y - MIN_Y][SIZE];
    public final int cx, cz;

    public boolean dirty = true;
    public void markDirty() { this.dirty = true; }

    // NOISE SCALES
    private static final float SCALE_TEMP = 0.0012f;
    private static final float SCALE_HUMIDITY = 0.0012f;
    private static final float SCALE_CONTINENT = 0.0018f;
    private static final float SCALE_EROSION = 0.002f;
    private static final float SCALE_NOISE_3D = 0.006f;

    // OpenGL handles
    private int vaoSolid, vboSolid, countSolid;
    private int vaoWater, vboWater, countWater;
    private int vaoShortGrass, vboShortGrass, countShortGrass;
    private int vaoTallGrass, vboTallGrass, countTallGrass;
    private MeshBuilder pendingSolids, pendingWater, pendingShortGrass, pendingTallGrass;
    private volatile boolean hasMeshToUpload = false;

    public Chunk(int cx, int cz, PerlinNoise noise) {
        this.cx = cx; this.cz = cz;
        Random r = new Random(getSeed(cx, cz));

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int wx = cx * SIZE + x;
                int wz = cz * SIZE + z;

                // CONTINENT/BIOME SHAPES
                // using 3 octaves breaks the "straight line" artifacts
                float continent = noise.getFBM(wx, wz, SCALE_CONTINENT, 3);

                // MOUNTAIN HEIGHTS
                float pv = (float) noise.getNoise(wx * 0.004, 0, wz * 0.004);

                // EROSION/DETAIL
                float erosion = noise.getFBM(wx + 2000, wz + 2000, SCALE_EROSION, 2);
                float temp = noise.getFBM(wx + 5000, wz + 5000, SCALE_TEMP, 2);
                float humidity = noise.getFBM(wx + 1000, wz + 1000, SCALE_HUMIDITY, 2);

                // Calculate Biome & Height
                Biome biome = Biome.getBiome(continent, temp, humidity);
                float targetHeight = getSmoothHeight(continent, erosion, pv);

                // FILL COLUMN
                for (int y = MIN_Y; y < MAX_Y; y++) {
                    // vertical domain warp
                    double yWarp = noise.getNoise(wx * 0.02, y * 0.02, wz * 0.02) * 3.0; // reduced amplitude
                    float noise3D = fbm3D(wx, y + yWarp, wz, SCALE_NOISE_3D, 4, 0.5f, 2.0f, noise);

                    // vertical bias pushes surfaces to targetHeight smoothly
                    float distFromSurface = targetHeight - y;
                    float verticalBias = distFromSurface / 60.0f;
                    if (y < -50) verticalBias += 5.0f;
                    if (y > 200) verticalBias -= 5.0f;

                    // combined density (backbone)
                    float density = noise3D + verticalBias;

                    // Hard/soft band thresholds
                    final float HARD_SOLID = 0.25f; // definitely solid above this
                    final float HARD_AIR   = -0.25f; // definitely air below this

                    byte blockId = 0;

                    // 1. DENSITY CALCULATION
                    if (density > HARD_SOLID) {
                        blockId = Block.STONE.id;
                    } else if (density < HARD_AIR) {
                        if (y <= SEA_LEVEL) blockId = Block.WATER.id;
                        else blockId = 0;
                    } else {
                        // Interpolate surface zone
                        float t = MathUtils.inverseLerp(HARD_AIR, HARD_SOLID, density);
                        t = MathUtils.smootherStep(t);

                        float micro = (float) noise.getNoise(wx * 0.12, y * 0.35, wz * 0.12) * 0.15f;
                        t += micro;

                        if (t > 0.5f) blockId = Block.STONE.id;
                        else if (y <= SEA_LEVEL) blockId = Block.WATER.id;
                    }

                    // 2. CAVES & DEEPSLATE
                    if (blockId == Block.STONE.id) {
                        if (isCave(wx, y, wz, noise)) {
                            blockId = (y < -54) ? Block.LAVA.id : 0;
                        } else if (y < -50) {
                            float ds = MathUtils.inverseLerp(-40.0f, -8.0f, y);
                            ds += (float) noise.getNoise(wx * 0.02, y * 0.05, wz * 0.02) * 0.12f;
                            if (ds < 0.35f) blockId = Block.DEEPSLATE.id;
                        }
                    }

                    // 3. SURFACE PAINTING (FIXED LOGIC)
                    if (blockId == Block.STONE.id) {
                        // Only paint if we are somewhat near the surface density-wise
                        // Increased from 0.3 to 0.45 to ensure we catch the surface layer properly
                        if (density < 0.45f) {

                            // Steepness check:
                            // Increased threshold from 15 to 28.
                            // If the 3D noise pushes the terrain 28 blocks higher than the smoothed height, it's a cliff.
                            boolean steep = (y > targetHeight + 28);

                            // Special Biome handling
                            if (biome == Biome.MOUNTAINS || biome == Biome.SNOWY_MOUNTAINS) {
                                // Mountains are naturally stony, but maybe snow on top
                                if (biome == Biome.SNOWY_MOUNTAINS && y > targetHeight + 10 && !steep) blockId = Block.SNOW.id;
                                // else remains STONE
                            }
                            else if (biome == Biome.DESERT) {
                                // Desert is always Sand, even on steep dunes usually
                                blockId = Block.SAND.id;
                            }
                            else if (steep) {
                                // Actual cliff in a grassy biome -> Stone
                                blockId = Block.STONE.id;
                            }
                            else {
                                // Normal Terrain (Plains, Forest, etc.)
                                // Ensure we paint down a few blocks from the "air" line
                                if (y >= targetHeight - 10) {
                                    // If density is very low (close to air), it's the top block -> Grass
                                    // If density is slightly higher (deeper), it's filler -> Dirt
                                    if (density < 0.2f) blockId = biome.topBlock.id;
                                    else blockId = biome.fillerBlock.id;
                                }
                            }
                        }
                    }

                    if (blockId != 0) setB(x, y, z, blockId);
                }
            }
        }

        // VEGETATION
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int surfY = getHighestBlock(x, z);
                if (surfY <= SEA_LEVEL || surfY >= MAX_Y - 10) continue;

                int wx = cx * SIZE + x; int wz = cz * SIZE + z;

                // Recalculate biome params for vegetation
                float continent = (float) noise.getNoise(wx * SCALE_CONTINENT, wz * SCALE_CONTINENT);
                float temp = (float) noise.getNoise(wx * SCALE_TEMP + 5000, wz * SCALE_TEMP + 5000);
                float humidity = (float) noise.getNoise(wx * SCALE_HUMIDITY + 1000, wz * SCALE_HUMIDITY + 1000);

                Biome biome = Biome.getBiome(continent, temp, humidity);
                byte ground = getB(x, surfY, z);

                if (ground == biome.topBlock.id) {
                    biome.generateVegetation(x, surfY, z, r, this);
                }
            }
        }
    }

    // this helps in smoothing out the area to prevent spiky spiky generation
    private float getSmoothHeight(float c, float e, float pv) {
        // Base Height (The Floor)
        // We keep the floor deep for oceans
        float[] cPoints = { -1.0f, -0.2f, -0.1f, 0.0f, 0.4f, 1.0f };
        float[] hPoints = {  20.0f, 50.0f, 60.0f, 66.0f, 85.0f, 140.0f };

        float baseHeight = 63.0f;
        for (int i = 0; i < cPoints.length - 1; i++) {
            if (c >= cPoints[i] && c <= cPoints[i+1]) {
                float t = MathUtils.inverseLerp(cPoints[i], cPoints[i+1], c);
                t = MathUtils.smootherStep(t);
                baseHeight = MathUtils.lerp(hPoints[i], hPoints[i+1], t);
                break;
            }
        }

        // 2. Roughness (The Bumps)
        float roughness = 0;

        if (c < 0.05f) {
            // UNDERWATER / COAST
            roughness = pv * 2.0f;
            if (pv < -0.5f) roughness -= 2.0f;
        }
        else if (c > 0.6f) {
            // MOUNTAINS (High Spikes)
            float mountainFactor = MathUtils.inverseLerp(0.6f, 1.0f, c);
            mountainFactor = MathUtils.smootherStep(mountainFactor);

            // Rigid noise for sharp peaks
            float rigid = Math.abs(pv);
            roughness = (rigid * rigid) * 110.0f * mountainFactor;
        }
        else {
            // PLAINS / FOREST (Rolling Hills)
            roughness = pv * 8.0f;
            // Erosion check to flatten valleys
            float flatFactor = (e + 1) / 2.0f;
            if (flatFactor > 0.6f) roughness *= 0.2f;
        }

        return baseHeight + roughness;
    }

    public void placeModel(int x, int y, int z, byte[] model) {
        for (int i = 0; i < model.length; i += 4) {
            byte dx = model[i]; byte dy = model[i+1]; byte dz = model[i+2]; byte blockId = model[i+3];
            setB(x + dx, y + dy, z + dz, blockId);
        }
    }

    private float getWarpedNoise(double x, double z, float scale, PerlinNoise noise) {
        double warpScale = scale * 0.3;
        double warpX = noise.getNoise(x * warpScale, z * warpScale);
        double warpZ = noise.getNoise(z * warpScale + 500, x * warpScale + 500);

        double distortedX = x * scale + (warpX * 40.0);
        double distortedZ = z * scale + (warpZ * 40.0);

        return (float) noise.getNoise(distortedX, distortedZ);
    }

    private float fbm3D(double x, double y, double z, float scale, int octaves, float pers, float lac, PerlinNoise noise) {
        float total = 0, freq = scale, amp = 1, max = 0;
        for(int i=0; i<octaves; i++) { total += (float) noise.getNoise(x * freq, y * freq, z * freq) * amp; max += amp; amp *= pers; freq *= lac; }
        return total / max;
    }

    private boolean isCave(double wx, double wy, double wz, PerlinNoise noise) {
        double n1 = noise.getNoise(wx * 0.02, wy * 0.02, wz * 0.02);
        double n2 = noise.getNoise(wx * 0.02 + 1337, wy * 0.02 + 1337, wz * 0.02 + 1337);
        return (n1 * n1 + n2 * n2) < 0.003;
    }

    private float getFBM(double x, double z, float scale, int octaves, PerlinNoise noise) {
        float total = 0;
        float frequency = scale;
        float amplitude = 1f;
        float maxValue = 0;
        for(int i=0; i<octaves; i++) {
            total += (float) noise.getNoise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        return total / maxValue;
    }

    public void computeMesh(World world) {
        this.dirty = false;
        MeshBuilder solids = new MeshBuilder(8192);
        MeshBuilder water = new MeshBuilder(2048);
        MeshBuilder sGrass = new MeshBuilder(2048);
        MeshBuilder tGrass = new MeshBuilder(2048);

        // 3x3 neighbor chunk cache
        final Chunk[][] neighborChunks = new Chunk[3][3];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                neighborChunks[dx + 1][dz + 1] = world.getChunk(cx + dx, cz + dz);
            }
        }

        for (int x = 0; x < SIZE; x++) {
            for (int y = MIN_Y; y < MAX_Y; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = getB(x, y, z);
                    if (id == 0) continue;
                    Block b = Block.get(id);

                    if (id == Block.WATER.id || id == Block.LAVA.id)
                        addBlockFaces(water, world, neighborChunks, x, y, z, b);
                    else if (id == Block.PLANT_GRASS.id)
                        addCross(sGrass, x, y, z, b);
                    else if (id == Block.PLANT_TALL_BOT.id || id == Block.PLANT_TALL_TOP.id)
                        addCross(tGrass, x, y, z, b);
                    else if (id == Block.LEAVES.id)
                        addBlockFaces(sGrass, world, neighborChunks, x, y, z, b);
                    else
                        addBlockFaces(solids, world, neighborChunks, x, y, z, b);
                }
            }
        }
        synchronized(this) {
            this.pendingSolids = solids; this.pendingWater = water;
            this.pendingShortGrass = sGrass; this.pendingTallGrass = tGrass;
            this.hasMeshToUpload = true;
        }
    }

    private void checkFace(MeshBuilder verts, World world, Chunk[][] neighborChunks,
                           int x, int y, int z, int dx, int dy, int dz, Block self) {
        int nx = x + dx; int ny = y + dy; int nz = z + dz;
        byte neighborId = 0;
        final int minY = MIN_Y; final int maxY = MAX_Y;

        if ((nx & ~15) == 0 && (nz & ~15) == 0) {
            if (ny >= minY && ny < maxY) {
                neighborId = blocks[nx][ny - minY][nz];
            }
        } else {
            final int cxOff = (nx >> 4) + 1;
            final int czOff = (nz >> 4) + 1;

            if (cxOff < 0 || cxOff > 2 || czOff < 0 || czOff > 2) {
                int wx = cx * SIZE + nx; int wz = cz * SIZE + nz;
                Chunk c = world.getChunk(wx >> 4, wz >> 4);
                if (c != null) neighborId = c.getB(wx & 15, ny, wz & 15);
                else {
                    final int selfId = self.id;
                    if (selfId == Block.WATER.id) neighborId = Block.WATER.id;
                    else if (selfId == Block.LAVA.id) neighborId = Block.LAVA.id;
                    else neighborId = 0;
                }
            } else {
                Chunk c = neighborChunks[cxOff][czOff];
                if (c != null) {
                    neighborId = c.getB(nx & 15, ny, nz & 15);
                } else {
                    final int selfId = self.id;
                    if (selfId == Block.WATER.id) neighborId = Block.WATER.id;
                    else if (selfId == Block.LAVA.id) neighborId = Block.LAVA.id;
                    else neighborId = 0;
                }
            }
        }

        Block neighbor = Block.get(neighborId);
        boolean render = !neighbor.opaque;
        if ((self.id == Block.WATER.id || self.id == Block.LAVA.id) &&
                (neighbor.id == Block.WATER.id || neighbor.id == Block.LAVA.id)) render = false;
        if (self.id == Block.LEAVES.id && neighbor.id == Block.LEAVES.id) render = false;
        if (render) addFace(verts, x, y, z, self, dx, dy, dz);
    }

    private void addBlockFaces(MeshBuilder verts, World world, Chunk[][] neighborChunks,
                               int x, int y, int z, Block b) {
        checkFace(verts, world, neighborChunks, x, y, z, 0, 1, 0, b);
        checkFace(verts, world, neighborChunks, x, y, z, 0, -1, 0, b);
        checkFace(verts, world, neighborChunks, x, y, z, 1, 0, 0, b);
        checkFace(verts, world, neighborChunks, x, y, z, -1, 0, 0, b);
        checkFace(verts, world, neighborChunks, x, y, z, 0, 0, 1, b);
        checkFace(verts, world, neighborChunks, x, y, z, 0, 0, -1, b);
    }

    private void addCross(MeshBuilder verts, float x, float y, float z, Block b) {
        float wx = cx * SIZE + x; float wz = cz * SIZE + z; float wy = y;
        float tex = b.side;
        float r = 1.0f, g = 1.0f, bl = 1.0f;
        if (b.tintType != Block.TINT_NONE) { r = b.r; g = b.g; bl = b.b; }
        long seed = (long)(wx * 3129871) ^ (long)(wz * 116129781L) ^ (long)(wy);
        float offX = ((seed & 15) / 15.0f - 0.5f) * 0.3f; float offZ = (((seed >> 4) & 15) / 15.0f - 0.5f) * 0.3f;
        wx+=offX; wz+=offZ;
        addV(verts, wx, wy, wz, 0, 1, tex, r, g, bl); addV(verts, wx+1, wy, wz+1, 1, 1, tex, r, g, bl); addV(verts, wx+1, wy+1, wz+1, 1, 0, tex, r, g, bl);
        addV(verts, wx, wy, wz, 0, 1, tex, r, g, bl); addV(verts, wx+1, wy+1, wz+1, 1, 0, tex, r, g, bl); addV(verts, wx, wy+1, wz, 0, 0, tex, r, g, bl);
        addV(verts, wx, wy, wz+1, 0, 1, tex, r, g, bl); addV(verts, wx+1, wy, wz, 1, 1, tex, r, g, bl); addV(verts, wx+1, wy+1, wz, 1, 0, tex, r, g, bl);
        addV(verts, wx, wy, wz+1, 0, 1, tex, r, g, bl); addV(verts, wx+1, wy+1, wz, 1, 0, tex, r, g, bl); addV(verts, wx+1, wy+1, wz+1, 0, 0, tex, r, g, bl);
    }
    private void addV(MeshBuilder b, float x, float y, float z, float u, float v, float l, float r, float g, float bl) { b.add(x); b.add(y); b.add(z); b.add(u); b.add(v); b.add(l); b.add(r); b.add(g); b.add(bl); }

    private void uploadMesh() { MeshBuilder s,w,g,t; synchronized(this) { if (!this.hasMeshToUpload) return; s=pendingSolids; w=pendingWater; g=pendingShortGrass; t=pendingTallGrass; hasMeshToUpload=false; pendingSolids=null; pendingWater=null; pendingShortGrass=null; pendingTallGrass=null; } cleanup(); if (s!=null && !s.isEmpty()) uploadBuffer(vaoSolid=glGenVertexArrays(), vboSolid=glGenBuffers(), s, 0); if (w!=null && !w.isEmpty()) uploadBuffer(vaoWater=glGenVertexArrays(), vboWater=glGenBuffers(), w, 1); if (g!=null && !g.isEmpty()) uploadBuffer(vaoShortGrass=glGenVertexArrays(), vboShortGrass=glGenBuffers(), g, 2); if (t!=null && !t.isEmpty()) uploadBuffer(vaoTallGrass=glGenVertexArrays(), vboTallGrass=glGenBuffers(), t, 3); }
    private void uploadBuffer(int vao, int vbo, MeshBuilder builder, int type) { FloatBuffer b = builder.toBuffer(); glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER, vbo); glBufferData(GL_ARRAY_BUFFER, b, GL_STATIC_DRAW); int stride = 9 * 4; glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0); glEnableVertexAttribArray(0); glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * 4); glEnableVertexAttribArray(1); glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * 4); glEnableVertexAttribArray(2); glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 6 * 4); glEnableVertexAttribArray(3); glBindBuffer(GL_ARRAY_BUFFER, 0); glBindVertexArray(0); int count = builder.size() / 9; if(type==0) countSolid = count; else if(type==1) countWater = count; else if(type==2) countShortGrass = count; else if(type==3) countTallGrass = count; MemoryUtil.memFree(b); }

    public void renderSolids() { if (hasMeshToUpload && chunksLoadedCount < CHUNKS_PER_FRAME) { uploadMesh(); chunksLoadedCount++; } if (countSolid > 0) { glBindVertexArray(vaoSolid); glDrawArrays(GL_TRIANGLES, 0, countSolid); } }
    public void renderShortGrass() { if (countShortGrass > 0) { glBindVertexArray(vaoShortGrass); glDrawArrays(GL_TRIANGLES, 0, countShortGrass); } }
    public void renderTallGrass() { if (countTallGrass > 0) { glBindVertexArray(vaoTallGrass); glDrawArrays(GL_TRIANGLES, 0, countTallGrass); } }
    public void renderWater() { if (countWater > 0) { glBindVertexArray(vaoWater); glDrawArrays(GL_TRIANGLES, 0, countWater); } }

    public void cleanup() { glDeleteVertexArrays(vaoSolid); glDeleteBuffers(vboSolid); glDeleteVertexArrays(vaoWater); glDeleteBuffers(vboWater); glDeleteVertexArrays(vaoShortGrass); glDeleteBuffers(vboShortGrass); glDeleteVertexArrays(vaoTallGrass); glDeleteBuffers(vboTallGrass); vaoSolid=0; vboSolid=0; vaoWater=0; vboWater=0; countSolid=0; countWater=0; }
    public void setB(int x, int y, int z, byte id) { if (x >= 0 && x < SIZE && z >= 0 && z < SIZE && y >= MIN_Y && y < MAX_Y) blocks[x][y - MIN_Y][z] = id; }
    public byte getB(int x, int y, int z) { if (y < MIN_Y || y >= MAX_Y) return 0; return blocks[x][y - MIN_Y][z]; }
    private long getSeed(int cx, int cz) { return (long)cx * 341873128712L + (long)cz * 132897987541L; }
    private int getHighestBlock(int x, int z) { for(int y = MAX_Y - 1; y >= MIN_Y; y--) { if(getB(x, y, z) != 0 && getB(x, y, z) != Block.WATER.id) return y; } return MIN_Y; }

    private static final float[][][] FACE_TEMPLATES = { {{0,1,1, 0,0}, {1,1,1, 1,0}, {1,1,0, 1,1}, {0,1,0, 0,1}}, {{0,0,1, 0,1}, {0,0,0, 0,0}, {1,0,0, 1,0}, {1,0,1, 1,1}}, {{1,0,1, 1,1}, {1,1,1, 1,0}, {0,1,1, 0,0}, {0,0,1, 0,1}}, {{0,0,0, 1,1}, {0,1,0, 1,0}, {1,1,0, 0,0}, {1,0,0, 0,1}}, {{1,0,0, 1,1}, {1,1,0, 1,0}, {1,1,1, 0,0}, {1,0,1, 0,1}}, {{0,0,1, 1,1}, {0,1,1, 1,0}, {0,1,0, 0,0}, {0,0,0, 0,1}} };
    private void addFace(MeshBuilder verts, float x, float y, float z, Block b, int dx, int dy, int dz) { float wx = cx * SIZE + x; float wz = cz * SIZE + z; float wy = y; float tex = (dy == 1) ? b.top : (dy == -1 ? b.bot : b.side); float r = 1.0f, g = 1.0f, bl = 1.0f; boolean applyTint = false; if (b.tintType == Block.TINT_ALL) applyTint = true; else if (b.tintType == Block.TINT_TOP && dy == 1) applyTint = true; if (applyTint) { r = b.r; g = b.g; bl = b.b; } if (dy == -1) { r*=0.7f; g*=0.7f; bl*=0.7f; } else if (dx != 0 || dz != 0) { r*=0.85f; g*=0.85f; bl*=0.85f; } int face = (dy==1)?0:(dy==-1)?1:(dz==1)?2:(dz==-1)?3:(dx==1)?4:5; float[][] d = FACE_TEMPLATES[face]; int[] indices = {0, 1, 3, 1, 2, 3}; for (int i : indices) { verts.add(wx + d[i][0]); verts.add(wy + d[i][1]); verts.add(wz + d[i][2]); verts.add(d[i][3]); verts.add(d[i][4]); verts.add(tex); verts.add(r); verts.add(g); verts.add(bl); } }
}