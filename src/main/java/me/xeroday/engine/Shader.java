package me.xeroday.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.lwjgl.opengl.GL33.*;

public class Shader {
    private final int programId;
    private int vsId, fsId;

    public Shader(String vertexPath, String fragmentPath) {
        String vsSrc = loadResource(vertexPath);
        String fsSrc = loadResource(fragmentPath);

        vsId = compile(GL_VERTEX_SHADER, vsSrc);
        fsId = compile(GL_FRAGMENT_SHADER, fsSrc);

        programId = glCreateProgram();
        glAttachShader(programId, vsId);
        glAttachShader(programId, fsId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0)
            throw new RuntimeException("Link Error: " + glGetProgramInfoLog(programId));
    }

    private String loadResource(String path) {
        try (InputStream is = Shader.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader file: " + path, e);
        }
    }

    private int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Compile Error: " + glGetShaderInfoLog(id));
        return id;
    }

    public void bind() { glUseProgram(programId); }
    public void unbind() { glUseProgram(0); }

    public void setUniform(String name, Matrix4f mat) {
        int loc = glGetUniformLocation(programId, name);
        if (loc == -1) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            mat.get(fb);
            glUniformMatrix4fv(loc, false, fb);
        }
    }

    public void setUniform(String name, Vector3f vec) {
        int loc = glGetUniformLocation(programId, name);
        if (loc != -1) glUniform3f(loc, vec.x, vec.y, vec.z);
    }

    public void setUniform(String name, int val) {
        int loc = glGetUniformLocation(programId, name);
        if (loc != -1) glUniform1i(loc, val);
    }

    public void cleanup() {
        glDetachShader(programId, vsId);
        glDetachShader(programId, fsId);
        glDeleteShader(vsId);
        glDeleteShader(fsId);
        glDeleteProgram(programId);
    }
}