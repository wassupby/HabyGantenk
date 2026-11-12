package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class HSRManager {

    // Z-Buffer: fragment yang lebih dekat ke kamera yang menang
    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    // Back-Face Culling: buang polygon yang membelakangi kamera
    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glFrontFace(GL_CCW);   // urutan titik counter-clockwise = sisi depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}