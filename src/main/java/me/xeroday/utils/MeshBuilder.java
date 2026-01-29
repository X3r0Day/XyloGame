package me.xeroday.utils;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class MeshBuilder {
    private float[] data;
    private int size;

    public MeshBuilder(int initialCapacity) {
        this.data = new float[initialCapacity];
        this.size = 0;
    }

    public void add(float value) {
        if (size == data.length) {
            int newCapacity = data.length * 2;
            data = Arrays.copyOf(data, newCapacity);
        }
        data[size++] = value;
    }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public FloatBuffer toBuffer() {
        if (size == 0) return null;
        FloatBuffer buffer = MemoryUtil.memAllocFloat(size);
        buffer.put(data, 0, size);
        buffer.flip();
        return buffer;
    }
}