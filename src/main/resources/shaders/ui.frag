#version 330 core
in vec2 TexCoords;
out vec4 color;

uniform sampler2D textTexture;
uniform vec3 colorUniform; // Matches your "color" uniform

void main() {
    vec4 sampled = texture(textTexture, TexCoords);
    // The font texture uses alpha for the shape
    color = vec4(colorUniform, sampled.a);
}