#version 330 core

in vec2 TexCoord;
in float TexLayer;
in vec3 Color;

out vec4 FragColor;

uniform sampler2DArray textureSampler;

void main() {
    // texture() on a sampler2DArray takes a vec3(u, v, layer)
    vec4 texColor = texture(textureSampler, vec3(TexCoord, TexLayer));

    if (texColor.a < 0.5) discard;

    FragColor = vec4(texColor.rgb * Color, texColor.a);
}