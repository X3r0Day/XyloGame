package me.xeroday.utils;

public class MathUtils {

    /**
     * Clamps a value between a minimum and maximum.
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Linear Interpolation.
     * Returns a value between 'a' and 'b' based on 't' (0.0 to 1.0).
     */
    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Inverse Linear Interpolation.
     * Returns a value between 0.0 and 1.0 representing where 'value' lies between 'a' and 'b'.
     * Useful for mapping a noise value (e.g., -1 to 1) to a percentage (0% to 100%).
     */
    public static float inverseLerp(float a, float b, float value) {
        if (a == b) return 0.0f;
        return clamp((value - a) / (b - a), 0.0f, 1.0f);
    }

    public static double inverseLerp(double a, double b, double value) {
        if (a == b) return 0.0;
        return clamp((value - a) / (b - a), 0.0, 1.0);
    }

    /**
     * SmoothStep (Hermite Interpolation).
     * Standard smoothing (3t^2 - 2t^3).
     */
    public static float smoothStep(float t) {
        t = clamp(t, 0.0f, 1.0f);
        return t * t * (3 - 2 * t);
    }

    /**
     * SmootherStep (Ken Perlin's Version).
     * Formula: 6t^5 - 15t^4 + 10t^3
     * * This is much better for terrain generation than SmoothStep because the
     * derivative is 0 at both endpoints, preventing "crease" lines where chunks meet.
     */
    public static float smootherStep(float t) {
        t = clamp(t, 0.0f, 1.0f);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static double smootherStep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
}