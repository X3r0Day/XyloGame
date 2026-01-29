#version 330 core
layout (location = 0) in vec2 position; // Must be vec2, not vec3
layout (location = 1) in vec2 texCoords;

uniform mat4 projection;

out vec2 TexCoords;

void main() {
    gl_Position = projection * vec4(position, 0.0, 1.0);
    TexCoords = texCoords;
}