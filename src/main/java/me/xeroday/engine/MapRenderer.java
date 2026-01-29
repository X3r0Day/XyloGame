package me.xeroday.engine;

import me.xeroday.world.Biome;
import me.xeroday.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class MapRenderer {
    private final int vaoId, vboId;
    private final Shader shader;
    private int mapTextureId;
    private final int MAP_SIZE = 400;
    private final Matrix4f projection = new Matrix4f();

    private int lastCenterX = Integer.MAX_VALUE;
    private int lastCenterZ = Integer.MAX_VALUE;

    public MapRenderer() {
        // Map have its own shaders cuz duh whatelse?
        shader = new Shader("/shaders/map.vert", "/shaders/map.frag");

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        // Quad vertices (Pos x,y + UV u,v)
        float[] vertices = {
                0, 0, 0, 0,
                0, 1, 0, 1,
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 0, 1, 0,
                0, 0, 0, 0
        };

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        // Attribs
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 2 * 4);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        mapTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, mapTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    public void draw(World world, int playerX, int playerZ, int screenW, int screenH, TextRenderer textRenderer, double mouseX, double mouseY) {
        if (Math.abs(playerX - lastCenterX) > 0 || Math.abs(playerZ - lastCenterZ) > 0) {
            generateMapTexture(world, playerX, playerZ);
            lastCenterX = playerX;
            lastCenterZ = playerZ;
        }

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();

        float mapSizeDisplay = 500f;
        float x = (screenW - mapSizeDisplay) / 2;
        float y = (screenH - mapSizeDisplay) / 2;

        projection.identity().ortho(0, screenW, screenH, 0, -1, 1);
        Matrix4f model = new Matrix4f().translate(x, y, 0).scale(mapSizeDisplay, mapSizeDisplay, 1);

        shader.setUniform("projection", projection);
        shader.setUniform("model", model);
        shader.setUniform("mapTexture", 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mapTextureId);
        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        // Hover Info (it tells biome and cords)
        // Bug to fix: only shows X when cursor is on map screen.
        if (mouseX >= x && mouseX <= x + mapSizeDisplay && mouseY >= y && mouseY <= y + mapSizeDisplay) {
            float localX = (float)(mouseX - x) / mapSizeDisplay;
            float localY = (float)(mouseY - y) / mapSizeDisplay;

            int worldX = (int) (playerX + (localX - 0.5f) * MAP_SIZE);
            int worldZ = (int) (playerZ + (localY - 0.5f) * MAP_SIZE);

            Biome b = world.getBiomeAt(worldX, worldZ);

            String info = String.format("%s (%d, %d)", b.name(), worldX, worldZ);
            textRenderer.drawString(info, (float)mouseX + 15, (float)mouseY, 0.8f, screenW, screenH);
            textRenderer.drawString("+", screenW/2f - 4, screenH/2f - 4, 1.0f, screenW, screenH);
        }

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
    }

    private void generateMapTexture(World world, int cx, int cz) {
        ByteBuffer buffer = MemoryUtil.memAlloc(MAP_SIZE * MAP_SIZE * 4);
        int startX = cx - (MAP_SIZE / 2);
        int startZ = cz - (MAP_SIZE / 2);

        for (int z = 0; z < MAP_SIZE; z++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                Biome b = world.getBiomeAt(startX + x, startZ + z);
                int r=0, g=0, bl=0;
                // Simple colors
                switch (b) {
                    case OCEAN: r=0; g=0; bl=180; break;
                    case BEACH: r=240; g=220; bl=130; break;
                    case PLAINS: r=100; g=200; bl=100; break;
                    case FOREST: r=34; g=139; bl=34; break;
                    case MOUNTAINS: r=120; g=120; bl=120; break;
                    case SNOWY_MOUNTAINS: r=240; g=240; bl=255; break;
                    case DESERT: r=210; g=180; bl=100; break;
                    case SNOWY_PLAINS: r=200; g=240; bl=255; break;
                }
                buffer.put((byte) r).put((byte) g).put((byte) bl).put((byte) 255);
            }
        }
        buffer.flip();
        glBindTexture(GL_TEXTURE_2D, mapTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, MAP_SIZE, MAP_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteTextures(mapTextureId);
        shader.cleanup();
    }
}