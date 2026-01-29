package me.xeroday.utils;

import java.util.Random;

public class PerlinNoise {
    private final int[] p = new int[512];
    private final int[] permutation;

    public PerlinNoise(int seed) {
        Random r = new Random(seed);
        this.permutation = new int[256];

        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int swapIdx = r.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[swapIdx];
            permutation[swapIdx] = temp;
        }

        for (int i = 0; i < 256; i++) {
            p[256 + i] = p[i] = permutation[i];
        }
    }

    public double getNoise(double x, double y) {
        return getNoise(x, y, 0);
    }

    public double getNoise(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z;
        int B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;

        return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),
                                grad(p[BA], x - 1, y, z)),
                        lerp(u, grad(p[AB], x, y - 1, z),
                                grad(p[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),
                                grad(p[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(p[AB + 1], x, y - 1, z - 1),
                                grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }


    public float getFBM(double x, double z, float scale, int octaves) {
        float total = 0;
        float frequency = scale;
        float amplitude = 1f;
        float maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += (float) getNoise(x * frequency, 0, z * frequency) * amplitude;
            maxValue += amplitude;

            amplitude *= 0.5f;   // Reduce strength for next layer
            frequency *= 2.0f;   // Increase detail for next layer
        }

        return total / maxValue; // Normalize to [-1, 1]
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}