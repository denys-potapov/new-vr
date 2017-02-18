precision mediump float;        // Set the default precision to medium. We don't need as high of a
                                // precision in the fragment shader.
uniform sampler2D u_Depth;
uniform sampler2D u_Texture;  // The input texture.
uniform mat4 u_MVPMatrix;

varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.

void main()                     // The entry point for our fragment shader.
{
    vec4 p = texture2D(u_Depth, v_TexCoordinate);

    if (p.b < 0.1) {
        gl_FragColor = vec4(0.0);
        return ;
    }

    vec3 pos;
    pos.xy = v_TexCoordinate;
    pos.z = p.r + p.g / 256.0;

    vec4 all = u_MVPMatrix * vec4(pos, 1.0);
    gl_FragColor = vec4(all.xy, 0.0, 1 .0);
}
