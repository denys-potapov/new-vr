attribute vec4 a_Position;		// Per-vertex position information we will pass in.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.

varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.

uniform mat4 u_MVPMatrix;

void main()                         // The entry point for our vertex shader.
{
    // Pass through the texture coordinate.
    v_TexCoordinate = a_TexCoordinate;

    gl_Position = a_Position;    // gl_Position is a special variable used to store the final position.

}
