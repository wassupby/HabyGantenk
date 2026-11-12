package com.grafkom.render;

import com.grafkom.projection.PerspectiveProjection;
import com.grafkom.camera.Camera3D;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.*;

public class StereoAnaglyphRenderer {
    private static final float MIN_IPD = 0.0f;
    private static final float MAX_IPD = 1.5f;
    private static final float MIN_FOCAL_OFFSET = -5.0f;
    private static final float MAX_FOCAL_OFFSET = 10.0f;

    private float ipd = 0.3f;          // Interpupillary Distance (satuan dunia)
    private float focalOffset = 0.0f;  // geser bidang zero parallax relatif terhadap objek

    private final float[] buf = new float[16];

    public void renderAnaglyph(Camera3D camera, PerspectiveProjection projection, Runnable renderScene) {
        float half = ipd / 2f;
        // Bidang zero parallax = jarak kamera ke target (+ offset)
        float focal = Math.max(0.5f, camera.getRadius() + focalOffset);
        Matrix4f centerView = camera.calculateLookAtMatrix();

        // Bersihkan seluruh kanal warna + depth dengan mask penuh
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: mata kiri -> kanal Merah saja
        renderEye(+half, true, false, false, centerView, projection, focal, renderScene);

        // PASS 2: mata kanan -> kanal Hijau + Biru (Cyan)
        renderEye(-half, false, true, true, centerView, projection, focal, renderScene);

        // Kembalikan mask ke normal
        glColorMask(true, true, true, true);
    }

    private void renderEye(float eyeOffset, boolean r, boolean g, boolean b,
                           Matrix4f centerView, PerspectiveProjection projection,
                           float focal, Runnable renderScene) {
        glColorMask(r, g, b, true);
        glClear(GL_DEPTH_BUFFER_BIT);   // depth dibersihkan, warna di-blend dengan pass sebelumnya

        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(projection.calculateOffAxisMatrix(eyeOffset, focal).get(buf));

        // Kamera digeser sejajar (bukan diputar): translasi di view space
        Matrix4f eyeView = new Matrix4f().translation(eyeOffset, 0f, 0f).mul(centerView);
        glMatrixMode(GL_MODELVIEW);
        glLoadMatrixf(eyeView.get(buf));

        renderScene.run();
    }

    // ===== Kontrol dinamis =====
    public void adjustIpd(float delta) {
        ipd = Math.max(MIN_IPD, Math.min(MAX_IPD, ipd + delta));
    }

    public void adjustFocalOffset(float delta) {
        focalOffset = Math.max(MIN_FOCAL_OFFSET, Math.min(MAX_FOCAL_OFFSET, focalOffset + delta));
    }

        public void reset() {
        ipd = 0.3f;
        focalOffset = 0.0f;
    }
        
    public float getIpd()         { return ipd; }
    public float getFocalOffset() { return focalOffset; }
}