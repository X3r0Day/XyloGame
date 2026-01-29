#version 330 core

in vec2 pass_uv;
in float pass_layer;
in vec3 pass_color;

out vec4 FragColor;

uniform sampler2DArray textureSampler;

void main() {
    vec4 texColor = texture(textureSampler, vec3(pass_uv, pass_layer));
    vec3 finalColor = texColor.rgb * pass_color;

    FragColor = vec4(finalColor, 1.0);
}