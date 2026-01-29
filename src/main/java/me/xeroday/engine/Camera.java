package me.xeroday.engine;

import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Camera {
    public float x = 0, y = 120, z = 0;
    public float yaw = 0, pitch = 0;
    private final float speed = 0.45f, sens = 0.15f;

    private final Matrix4f viewMatrix = new Matrix4f();

    public void update(long win) {
        float s = (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) ? speed * 3 : speed;

        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_W) == GLFW_PRESS) {
            x += Math.sin(Math.toRadians(yaw)) * s;
            z -= Math.cos(Math.toRadians(yaw)) * s;
        }
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_S) == GLFW_PRESS) {
            x -= Math.sin(Math.toRadians(yaw)) * s;
            z += Math.cos(Math.toRadians(yaw)) * s;
        }
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_A) == GLFW_PRESS) {
            x += Math.sin(Math.toRadians(yaw - 90)) * s;
            z -= Math.cos(Math.toRadians(yaw - 90)) * s;
        }
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_D) == GLFW_PRESS) {
            x += Math.sin(Math.toRadians(yaw + 90)) * s;
            z -= Math.cos(Math.toRadians(yaw + 90)) * s;
        }
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_SPACE) == GLFW_PRESS) y += s;
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) y -= s;
    }

    public void rotate(float dx, float dy) {
        yaw += dx * sens;
        pitch += dy * sens;

        // Clamp pitch to prevent camera flipping
        if (pitch > 89.9f) pitch = 89.9f;
        if (pitch < -89.9f) pitch = -89.9f;
    }

    public Matrix4f getViewMatrix() {
        viewMatrix.identity();

        viewMatrix.rotate((float) Math.toRadians(pitch), 1.0f, 0.0f, 0.0f);
        viewMatrix.rotate((float) Math.toRadians(yaw), 0.0f, 1.0f, 0.0f);

        viewMatrix.translate(-x, -y, -z);

        return viewMatrix;
    }

    public Vector3f getPosition() {
        return new Vector3f(x, y, z);
    }
}