package com.example.denys.newvr;

import android.os.Bundle;
import android.widget.FrameLayout;

import org.artoolkit.ar.base.ARActivity;
import org.artoolkit.ar.base.rendering.ARRenderer;

/**
 * This is the activity that gets called from the Android Framework, extended by the
 * ARToolKit Framework to add AR capability.
 * A very simple example of extending ARActivity to create a new AR application.
 */
public class MainActivity extends ARActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    /**
     * Tell the ARToolKit which renderer to use. In this case we provide a subclass of
     * {@link org.artoolkit.ar.base.rendering.gles20.ARRendererGLES20} renderer.
     */
    @Override
    protected ARRenderer supplyRenderer() {
        return new SimpleGLES20Renderer();
    }

    /**
     * Use the FrameLayout in this Activity's UI.
     */
    @Override
    protected FrameLayout supplyFrameLayout() {
        return (FrameLayout) this.findViewById(R.id.mainLayout);
    }
}