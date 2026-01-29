package me.xeroday.utils;

import java.util.Random;

public class Noise {
    private final SimplexNoise simplex;
    private final long seed;

    public Noise(long seed) {
        this.seed = seed;
        this.simplex = new SimplexNoise(new Random(seed));
    }

    public float fbm(float x, float z, int octaves, float persistence, float scale) {
        float total = 0;
        float frequency = scale;
        float amplitude = 1;
        float maxValue = 0;

        for(int i = 0; i < octaves; i++) {
            total += simplex.getNoise(x * frequency, z * frequency) * amplitude;

            maxValue += amplitude;

            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    public float noise3D(float x, float y, float z, float scale) {
        return (float) simplex.getNoise(x * scale, y * scale, z * scale);
    }

    public static class SimplexNoise {

        private PerlinNoise internal;

        public SimplexNoise(Random r) {
            internal = new PerlinNoise(r.nextInt());
        }

        public double getNoise(double x, double z) {
            return internal.getNoise(x, z);
        }

        public double getNoise(double x, double y, double z) {
            return internal.getNoise(x + (y * 100), z + (y * 100));
        }
    }
}