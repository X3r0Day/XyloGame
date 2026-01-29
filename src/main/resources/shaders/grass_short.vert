#version 330 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 uv;
layout (location = 2) in float layer;
layout (location = 3) in vec3 color;

out vec2 pass_uv;
out float pass_layer;
out vec3 pass_color;

uniform mat4 view;
uniform mat4 projection;

void main() {
    // TIP: This is where you will add wind sway later!
    vec3 finalPos = position;

    gl_Position = projection * view * vec4(finalPos, 1.0);
    pass_uv = uv;
    pass_layer = layer;
    pass_color = color;
}