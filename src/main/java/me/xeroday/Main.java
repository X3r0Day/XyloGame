package me.xeroday;

import me.xeroday.engine.*;
import me.xeroday.utils.DiscordIntegration;
import me.xeroday.world.Biome;
import me.xeroday.world.World;
import me.xeroday.world.Chunk;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Main {
    private long window;
    private final Camera camera = new Camera();
    private double lastX, lastY;
    private double mouseX, mouseY; // Track raw mouse pos

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    // Map State
    private boolean isMapOpen = false;
    private boolean mKeyPressed = false; // Debounce

    public void run() {
        if (!glfwInit()) return;

        // Set to null if no app id
        String raw = System.getenv("DISCORD_APP_ID");
        Long appId = raw != null ? Long.parseLong(raw) : null;
        DiscordIntegration.init(appId);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(1920, 1080, "XyloGame - InfDev", 0, 0);
        if (window == 0) throw new RuntimeException("Failed to create window");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // GL States
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthFunc(GL_LEQUAL);

        Shader solidShader = new Shader("/shaders/world.vert", "/shaders/solid.frag");
        Shader shortGrassShader = new Shader("/shaders/grass_short.vert", "/shaders/grass_short.frag");
        Shader tallGrassShader = new Shader("/shaders/grass_tall.vert", "/shaders/grass_tall.frag");

        TextureManager textures = new TextureManager(
                "grass_block_top.png",  // 0
                "grass_block_side.png", // 1
                "dirt.png",             // 2
                "stone.png",            // 3
                "oak_log.png",          // 4
                "oak_log_top.png",      // 5
                "oak_leaves.png",       // 6
                "sand.png",             // 7
                "water_still.png",      // 8
                "snow.png",             // 9
                "short_grass.png",      // 10
                "tall_grass_bottom.png",// 11
                "tall_grass_top.png"    // 12
        );

        World world = new World();
        TextRenderer textRenderer = new TextRenderer();
        MapRenderer mapRenderer = new MapRenderer();

        // Mouse Move Callback
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            mouseX = x;
            mouseY = y;
            // Only rotate camera if map is CLOSED
            if (!isMapOpen) {
                camera.rotate((float)(x - lastX), (float)(y - lastY));
            }
            lastX = x; lastY = y;
        });


        // Bug to fix: Well on wayland when I take SS I still have problem but dw I'll fix soon
        glfwSetWindowFocusCallback(window, (w, focused) -> {
            if (focused) {
                // If we tab back in and map is not open, lock cursor again
                if (!isMapOpen) {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }

                // to prevent the camera from snapping to the previous position
                double[] xpos = new double[1], ypos = new double[1];
                glfwGetCursorPos(window, xpos, ypos);
                lastX = xpos[0];
                lastY = ypos[0];
            } else {
                // If we tab out, unlock the cursor so user can use other apps
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }
        });

        // Key Callback
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            // Toggle Map on M press
            if (key == GLFW_KEY_M && action == GLFW_PRESS) {
                isMapOpen = !isMapOpen;
                if (isMapOpen) {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL); // Unlock mouse
                } else {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED); // Lock mouse

                    // Reset pos when closing map
                    double[] xpos = new double[1], ypos = new double[1];
                    glfwGetCursorPos(window, xpos, ypos);
                    lastX = xpos[0];
                    lastY = ypos[0];
                }
            }
            // Close map on ESC or Exit
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                if (isMapOpen) {
                    isMapOpen = false;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

                    // Reset pos when closing map via ESC
                    double[] xpos = new double[1], ypos = new double[1];
                    glfwGetCursorPos(window, xpos, ypos);
                    lastX = xpos[0];
                    lastY = ypos[0];
                } else {
                    glfwSetWindowShouldClose(window, true);
                }
            }
        });

        int frames = 0;
        int lastFPS = 0;
        long lastTime = System.currentTimeMillis();
        double prevPosX = camera.x, prevPosY = camera.y, prevPosZ = camera.z;
        double currentSpeed = 0;

        while (!glfwWindowShouldClose(window)) {
            Chunk.nextFrame();
            int[] width = new int[1], height = new int[1];
            glfwGetFramebufferSize(window, width, height);
            glViewport(0, 0, width[0], height[0]);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearColor(0.53f, 0.81f, 0.92f, 1.0f);

            // Update Logic
            if (!isMapOpen) {
                camera.update(window); // Only move when map closed
            }
            world.update(camera);

            // FPS Calculation
            frames++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime >= 1000) {
                lastFPS = frames;
                frames = 0;
                lastTime = currentTime;
                double dx = camera.x - prevPosX; double dy = camera.y - prevPosY; double dz = camera.z - prevPosZ;
                currentSpeed = Math.sqrt(dx*dx + dy*dy + dz*dz);
                prevPosX = camera.x; prevPosY = camera.y; prevPosZ = camera.z;
            }

            // 3D Render Setup
            float aspectRat = (float) width[0] / height[0];
            projectionMatrix.identity().perspective((float) Math.toRadians(65.0f), aspectRat, 0.1f, 1000.0f);
            viewMatrix.set(camera.getViewMatrix());
            Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(viewMatrix);

            // We set the camera matrix on all of them so they are ready when World.render binds them.
            Vector3f camPos = new Vector3f((float)camera.x, (float)camera.y, (float)camera.z);

            solidShader.bind();
            solidShader.setUniform("projection", projectionMatrix);
            solidShader.setUniform("view", viewMatrix);
            solidShader.setUniform("textureSampler", 0);

            shortGrassShader.bind();
            shortGrassShader.setUniform("projection", projectionMatrix);
            shortGrassShader.setUniform("view", viewMatrix);
            shortGrassShader.setUniform("textureSampler", 0);

            tallGrassShader.bind();
            tallGrassShader.setUniform("projection", projectionMatrix);
            tallGrassShader.setUniform("view", viewMatrix);
            tallGrassShader.setUniform("textureSampler", 0);
            tallGrassShader.unbind(); // Done setting up

            textures.bind();

            // Pass all shaders
            world.render(solidShader, shortGrassShader, tallGrassShader, viewProj, camPos);

            // 2D UI Render
            if (isMapOpen) {
                // Draw Map
                mapRenderer.draw(world, (int)camera.x, (int)camera.z, width[0], height[0], textRenderer, mouseX, mouseY);
            } else {
                // Draw HUD
                Biome currentBiome = world.getBiomeAt((int)camera.x, (int)camera.z);
                String stats = String.format(
                        "FPS: %d\nXYZ: %.1f / %.1f / %.1f\nBiome: %s\nDir: %s\nSpeed: %.1f m/s\n[M] Map",
                        lastFPS, camera.x, camera.y, camera.z,
                        currentBiome.name(),
                        getDirection(camera.yaw, camera.pitch),
                        currentSpeed
                );
                textRenderer.drawString(stats, 10, 10, 1.0f, width[0], height[0]);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        // Cleanup
        mapRenderer.cleanup();
        textRenderer.cleanup();
        solidShader.cleanup();
        shortGrassShader.cleanup();
        tallGrassShader.cleanup();
        textures.cleanup();
        world.cleanup();
        glfwTerminate();
    }

    private String getDirection(float yaw, float pitch) {
        if (pitch < -45) return "+Y";
        if (pitch > 45)  return "-Y";
        float angle = yaw % 360;
        if (angle < 0) angle += 360;
        if (angle >= 315 || angle < 45) return "-Z";
        if (angle >= 45 && angle < 135) return "+X";
        if (angle >= 135 && angle < 225) return "+Z";
        if (angle >= 225 && angle < 315) return "-X";
        return "?";
    }

    public static void main(String[] args) { new Main().run(); }
}