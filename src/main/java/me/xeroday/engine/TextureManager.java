package me.xeroday.engine;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL42.glTexStorage3D;

public class TextureManager {
    private int id;

    public TextureManager(String... files) {
        int count = files.length;
            int size = 16; // 16x16 textures

        // Generate Texture Array
        id = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT); // Repeat on X axis
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT); // Repeat on Y axis

        // Allocate storage for the 3D texture (Width, Height, Layers)
        // 5 mipmap levels should be enough?
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, 5, GL_RGBA8, size, size, count);

        // Upload images one by one into the layers
        for (int i = 0; i < count; i++) {
            String path = "/textures/blocks/" + files[i];
            ByteBuffer data = null;

            try {
                ByteBuffer raw = ioToBuffer(path);
                IntBuffer w = MemoryUtil.memAllocInt(1);
                IntBuffer h = MemoryUtil.memAllocInt(1);
                IntBuffer c = MemoryUtil.memAllocInt(1);

                // Load image
                data = STBImage.stbi_load_from_memory(raw, w, h, c, 4);

                if (data != null) {
                    // Upload to layer 'i'
                    glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, size, size, 1, GL_RGBA, GL_UNSIGNED_BYTE, data);
                    STBImage.stbi_image_free(data);
                }

                MemoryUtil.memFree(w);
                MemoryUtil.memFree(h);
                MemoryUtil.memFree(c);
                MemoryUtil.memFree(raw);

            } catch (Exception e) {
                System.err.println("Failed to load texture: " + path);
                e.printStackTrace();
            }
        }

        // Parameters
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Anisotropic filtering (4x is best more than that is waste)
        float maxAnisotropy = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        glTexParameterf(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(4.0f, maxAnisotropy));

        // Generate Mipmaps
        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
    }

    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }

    private ByteBuffer ioToBuffer(String res) throws IOException {
        InputStream is = getClass().getResourceAsStream(res);
        if (is == null) throw new IOException("Resource not found: " + res);

        ReadableByteChannel rbc = Channels.newChannel(is);
        ByteBuffer buf = MemoryUtil.memAlloc(32768);
        while (true) {
            int bytes = rbc.read(buf);
            if (bytes == -1) break;
            if (buf.remaining() == 0) buf = MemoryUtil.memRealloc(buf, buf.capacity() * 2);
        }
        buf.flip();
        return buf;
    }
}