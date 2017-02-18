package com.example.denys.newvr;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class NewVrRenderer implements GLSurfaceView.Renderer
{
    private final Context mActivityContext;

    /** Store our model data in a float buffer. */
    private final FloatBuffer mSquarePositions;
    private final FloatBuffer mSquareTextureCoordinates;

    private float[] mModelMatrix = new float[16];

    private float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model color information. */
    private int mColorHandle;

    /**
     * This will be used to pass in model texture coordinate information.
     */
    private int mTextureCoordinateHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /**
     * Size of the texture coordinate data in elements.
     */
    private final int mTextureCoordinateDataSize = 2;

    /**
     * This is a handle to our per-vertex cube shading program.
     */
    private int mProgramHandle;

    /**
     * This is a handle to our texture data.
     */
    private int mTextureDataHandle;

    /**
     * This will be used to pass in the texture.
     */
    private int mTextureUniformHandle;

    /**
     * This is a handle to our texture data.
     */
    private int mDepthDataHandle;

    /**
     * This will be used to pass in the texture.
     */
    private int mDepthUniformHandle;

    /**
     * Initialize the model data.
     */
    public NewVrRenderer(final Context activityContext)
    {
        mActivityContext = activityContext;

        // X, Y, Z
        final float[] squarePositionsData =
                {
                        -1.0f, 1.0f, 0.0f,
                        -1.0f, -1.0f, 0.0f,
                        1.0f, 1.0f, 0.0f,
                        -1.0f, -1.0f, 0.0f,
                        1.0f, -1.0f, 0.0f,
                        1.0f, 1.0f, 0.0f
                };

        final float[] squareTextureCoordinateData =
                {
                        // Front face
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        1.0f, 0.0f
                };


        // Initialize the buffers.
        mSquarePositions = ByteBuffer.allocateDirect(squarePositionsData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mSquarePositions.put(squarePositionsData).position(0);

        mSquareTextureCoordinates = ByteBuffer.allocateDirect(squareTextureCoordinateData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mSquareTextureCoordinates.put(squareTextureCoordinateData).position(0);
    }

    protected int loadShaderFromResource(final int resourceId, int shaderType)
    {
        final InputStream inputStream = mActivityContext.getResources().openRawResource(
                resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inputStream);
        final BufferedReader bufferedReader = new BufferedReader(
                inputStreamReader);

        final StringBuilder body = new StringBuilder();
        String nextLine;
        try
        {
            while ((nextLine = bufferedReader.readLine()) != null)
            {
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading shader source.");
        }

        String source = body.toString();

        // Load in the shader.
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Pass in the shader source.
        GLES20.glShaderSource(shaderHandle, source);

        // Compile the shader.
        GLES20.glCompileShader(shaderHandle);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0)
        {
            String error = GLES20.glGetShaderInfoLog(shaderHandle);
            GLES20.glDeleteShader(shaderHandle);
            throw new RuntimeException("Error 1 compiling shader" + error);
        }

        return shaderHandle;
    }

    public int loadTextureFromResource(final int resourceId) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;    // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(mActivityContext.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 1.0f, 0.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

// Load in the vertex shader.
        int vertexShaderHandle = loadShaderFromResource(R.raw.vertex_shader, GLES20.GL_VERTEX_SHADER);

        // Load in the fragment shader shader.
        int fragmentShaderHandle = loadShaderFromResource(R.raw.fragment_shader, GLES20.GL_FRAGMENT_SHADER);

        // Create a program object and store the handle to it.
        mProgramHandle = GLES20.glCreateProgram();

        if (mProgramHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(mProgramHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(mProgramHandle, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(mProgramHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(mProgramHandle, 1, "a_TexCoordinate");

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(mProgramHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(mProgramHandle);
                mProgramHandle = 0;
            }
        }

        if (mProgramHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        // Load the texture
        mTextureDataHandle = loadTextureFromResource(R.drawable.mango);
        mDepthDataHandle = loadTextureFromResource(R.drawable.mango_depthmap);

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);
    }


    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L - 5000L;
        float angleInDegrees = (90.0f / 10000.0f) * ((int) time);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);

        // Set program handles. These will later be used to pass in values to the program.
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mDepthUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Depth");
        int mMVPUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVP");

        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        int uScaleHandle = GLES20.glGetUniformLocation(mProgramHandle, "scale");
        int uOffsetHandle = GLES20.glGetUniformLocation(mProgramHandle, "offset");
        int uFocusHandle = GLES20.glGetUniformLocation(mProgramHandle, "focus");

        // focus config
        // GLES20.glUniform2f(uOffsetHandle, (float) Math.sin(angle), 0.0f);
        GLES20.glUniform2f(uOffsetHandle, (float) Math.sin(angleInDegrees), (float) Math.cos(angleInDegrees));
        GLES20.glUniform1f(uScaleHandle, 0.07f); // magic number
        GLES20.glUniform1f(uFocusHandle, 0.5f);

        // Load texture. Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPUniformHandle, 1, false, mMVPMatrix, 0);

        // Load depth. Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mDepthDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mDepthUniformHandle, 1);

        // Pass in the position information
        mSquarePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mSquarePositions);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the texture coordinate information
        mSquareTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mSquareTextureCoordinates);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6); // 6 length of mSquarePositions
    }
}
