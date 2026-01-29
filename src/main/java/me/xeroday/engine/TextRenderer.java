package me.xeroday.engine;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class TextRenderer {
    private final int textureId;
    private final int vaoId, vboId;
    private final Shader shader;
    private final Matrix4f projection = new Matrix4f();

    // Glyph info
    private static class Glyph {
        float u, v, u2, v2, width;
    }
    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private int fontHeight;

    public TextRenderer() {
        shader = new Shader("/shaders/ui.vert", "/shaders/ui.frag");
        textureId = generateFontTexture();

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        // Dynamic draw because text changes every frame
        glBufferData(GL_ARRAY_BUFFER, 65536, GL_DYNAMIC_DRAW);

        // Pos (2 floats) + UV (2 floats) = 4 floats stride
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 2 * 4);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void drawString(String text, float x, float y, float scale, int screenWidth, int screenHeight) {
        projection.identity().ortho(0, screenWidth, screenHeight, 0, -1, 1);

        shader.bind();
        shader.setUniform("projection", projection);
        shader.setUniform("textTexture", 0);
        shader.setUniform("color", new org.joml.Vector3f(1, 1, 1));

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glBindVertexArray(vaoId);

        FloatBuffer vertices = MemoryUtil.memAllocFloat(text.length() * 6 * 4);

        float startX = x;
        int verticesToDraw = 0;

        for (char c : text.toCharArray()) {
            if (c == '\n') {
                y += fontHeight * scale;
                x = startX;
                continue; // Skips adding data, but loop continues
            }

            Glyph g = glyphs.get(c);
            if (g == null) continue;

            float w = g.width * scale;
            float h = fontHeight * scale;

            vertices.put(x).put(y + h).put(g.u).put(g.v2);
            vertices.put(x + w).put(y).put(g.u2).put(g.v);
            vertices.put(x).put(y).put(g.u).put(g.v);

            vertices.put(x).put(y + h).put(g.u).put(g.v2);
            vertices.put(x + w).put(y + h).put(g.u2).put(g.v2);
            vertices.put(x + w).put(y).put(g.u2).put(g.v);

            x += w;
            verticesToDraw += 6;
        }
        vertices.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glDrawArrays(GL_TRIANGLES, 0, verticesToDraw);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);

        MemoryUtil.memFree(vertices);
        glBindVertexArray(0);
        shader.unbind();
    }

    private int generateFontTexture() {
        int imgSize = 512;
        int fontSize = 24;

        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setFont(new Font("Monospaced", Font.BOLD, fontSize));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);

        FontMetrics fm = g2d.getFontMetrics();
        this.fontHeight = fm.getHeight();

        int x = 0;
        int y = 0;

        // Generate ASCII 32-126
        for (int i = 32; i < 127; i++) {
            char c = (char) i;
            int w = fm.charWidth(c);
            int h = fm.getHeight();

            if (x + w >= imgSize) {
                x = 0;
                y += h;
            }

            g2d.drawString(String.valueOf(c), x, y + fm.getAscent());

            Glyph g = new Glyph();
            g.u = (float) x / imgSize;
            g.v = (float) y / imgSize;
            g.u2 = (float) (x + w) / imgSize;
            g.v2 = (float) (y + h) / imgSize;
            g.width = w;
            glyphs.put(c, g);

            x += w;
        }
        g2d.dispose();

        // Convert to ByteBuffer
        int[] pixels = new int[imgSize * imgSize];
        img.getRGB(0, 0, imgSize, imgSize, pixels, 0, imgSize);

        ByteBuffer buffer = MemoryUtil.memAlloc(imgSize * imgSize * 4);
        for (int pixel : pixels) {
            // ARGB -> RGBA
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) ((pixel) & 0xFF));       // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, imgSize, imgSize, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        MemoryUtil.memFree(buffer);
        return id;
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteTextures(textureId);
        shader.cleanup();
    }
}