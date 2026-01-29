package me.xeroday.world;

import me.xeroday.engine.Camera;
import me.xeroday.engine.Shader;
import me.xeroday.utils.PerlinNoise;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.*;

import static org.lwjgl.opengl.GL33.*;

public class World {
    private final Map<Long, Chunk> chunkMap = new ConcurrentHashMap<>();
    private final Set<Long> loadingChunks = Collections.synchronizedSet(new HashSet<>());
    private final Queue<Chunk> chunksToRegister = new ConcurrentLinkedQueue<>();
    private final List<int[]> sortedOffsets = new ArrayList<>();

    private final PerlinNoise noise;
    private final int renderDist = 16;

    // SCALES
    private static final float SCALE_TEMP = 0.0012f;
    private static final float SCALE_HUMIDITY = 0.0012f;
    private static final float SCALE_CONTINENT = 0.0018f;

    private final ExecutorService genExecutor = Executors.newFixedThreadPool(4);
    private final ExecutorService meshExecutor = Executors.newFixedThreadPool(2);
    private final FrustumIntersection frustum = new FrustumIntersection();

    private volatile int playerChunkX = 0;
    private volatile int playerChunkZ = 0;

    public World() {
        this.noise = new PerlinNoise(new Random().nextInt());

        for (int x = -renderDist; x <= renderDist; x++) {
            for (int z = -renderDist; z <= renderDist; z++) {
                sortedOffsets.add(new int[]{x, z});
            }
        }
        sortedOffsets.sort((a, b) -> {
            int distA = a[0] * a[0] + a[1] * a[1];
            int distB = b[0] * b[0] + b[1] * b[1];
            return Integer.compare(distA, distB);
        });
    }

    // This function is used by MapRenderer to predict biomes
    public Biome getBiomeAt(int x, int z) {
        float continent = noise.getFBM(x, z, SCALE_CONTINENT, 3);
        float temp = noise.getFBM(x + 5000, z + 5000, SCALE_TEMP, 2);
        float humidity = noise.getFBM(x + 1000, z + 1000, SCALE_HUMIDITY, 2);
        return Biome.getBiome(continent, temp, humidity);
    }

    public void update(Camera cam) {
        int cx = (int) Math.floor(cam.x / 16.0);
        int cz = (int) Math.floor(cam.z / 16.0);

        this.playerChunkX = cx;
        this.playerChunkZ = cz;

        if (loadingChunks.size() < 16) {
            for (int[] offset : sortedOffsets) {
                if (loadingChunks.size() >= 16) break;

                int x = cx + offset[0];
                int z = cz + offset[1];
                long key = getChunkKey(x, z);

                if (!chunkMap.containsKey(key) && !loadingChunks.contains(key)) {
                    loadingChunks.add(key);
                    int finalX = x; int finalZ = z;

                    genExecutor.submit(() -> {
                        try {
                            int dx = Math.abs(finalX - playerChunkX);
                            int dz = Math.abs(finalZ - playerChunkZ);
                            if (dx > renderDist + 2 || dz > renderDist + 2) {
                                loadingChunks.remove(key);
                                return;
                            }

                            Chunk c = new Chunk(finalX, finalZ, noise);
                            meshExecutor.submit(() -> {
                                c.computeMesh(this);
                                chunksToRegister.add(c);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            loadingChunks.remove(key);
                        }
                    });
                }
            }
        }

        Chunk c;
        while ((c = chunksToRegister.poll()) != null) {
            long key = getChunkKey(c.cx, c.cz);
            chunkMap.put(key, c);
            loadingChunks.remove(key);
            refreshChunk(c.cx + 1, c.cz); refreshChunk(c.cx - 1, c.cz);
            refreshChunk(c.cx, c.cz + 1); refreshChunk(c.cx, c.cz - 1);
        }

        chunkMap.entrySet().removeIf(e -> {
            Chunk chunk = e.getValue();
            int dx = Math.abs(chunk.cx - cx);
            int dz = Math.abs(chunk.cz - cz);
            if (dx > renderDist + 2 || dz > renderDist + 2) {
                chunk.cleanup();
                return true;
            }
            return false;
        });
    }

    private void refreshChunk(int cx, int cz) { Chunk c = chunkMap.get(getChunkKey(cx, cz)); if (c != null) meshExecutor.submit(() -> c.computeMesh(this)); }
    public Chunk getChunk(int cx, int cz) { return chunkMap.get(getChunkKey(cx, cz)); }
    private long getChunkKey(int x, int z) { return ((long)x << 32) | (z & 0xFFFFFFFFL); }

    public void render(Shader solid, Shader sGrass, Shader tGrass, Matrix4f viewProjMatrix, Vector3f camPos) {
        frustum.set(viewProjMatrix);
        List<Chunk> visibleChunks = new ArrayList<>();
        for (Chunk ch : chunkMap.values()) {
            if (frustum.testAab(ch.cx * 16, -64, ch.cz * 16, ch.cx * 16 + 16, 320, ch.cz * 16 + 16)) visibleChunks.add(ch);
        }
        visibleChunks.sort((c1, c2) -> {
            float d1 = (c1.cx * 16 - camPos.x) * (c1.cx * 16 - camPos.x) + (c1.cz * 16 - camPos.z) * (c1.cz * 16 - camPos.z);
            float d2 = (c2.cx * 16 - camPos.x) * (c2.cx * 16 - camPos.x) + (c2.cz * 16 - camPos.z) * (c2.cz * 16 - camPos.z);
            return Float.compare(d1, d2);
        });

        solid.bind();
        glDisable(GL_BLEND); glEnable(GL_CULL_FACE);
        for (Chunk chunk : visibleChunks) chunk.renderSolids();

        sGrass.bind();
        glDisable(GL_CULL_FACE);
        for (Chunk chunk : visibleChunks) chunk.renderShortGrass();

        tGrass.bind();
        for (Chunk chunk : visibleChunks) chunk.renderTallGrass();

        solid.bind();
        glEnable(GL_BLEND); glDisable(GL_CULL_FACE);
        for (Chunk chunk : visibleChunks) chunk.renderWater();

        glDisable(GL_BLEND); glEnable(GL_CULL_FACE);
    }

    public void cleanup() { genExecutor.shutdownNow(); meshExecutor.shutdownNow(); }
}