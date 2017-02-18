precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
uniform sampler2D u_Depth;
uniform sampler2D u_Texture;  // The input texture.
uniform mat4 u_MVPMatrix;

varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.

void main()                     // The entry point for our fragment shader.
{
    vec3 pos;
    pos.xy = v_TexCoordinate;
    pos.z = texture2D(u_Depth, v_TexCoordinate).x;

    gl_FragColor = u_MVPMatrix * vec4(pos, 1.0);
}
