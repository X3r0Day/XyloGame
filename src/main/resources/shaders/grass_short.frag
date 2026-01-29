#version 330 core

in vec2 pass_uv;
in float pass_layer;
in vec3 pass_color;

out vec4 FragColor;

uniform sampler2DArray textureSampler;

void main() {
    vec4 texColor = texture(textureSampler, vec3(pass_uv, pass_layer));

    // IMPORTANT: Discard invisible pixels for grass
    if(texColor.a < 0.5) discard;

    FragColor = texColor * vec4(pass_color, 1.0);
}