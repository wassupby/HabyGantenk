package com.grafkom.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera3D {
    private static final float MIN_RADIUS = 2.0f;
    private static final float MAX_RADIUS = 30.0f;
    private static final float MAX_ELEVATION = 89.0f; // hindari up-vector sejajar arah pandang

    private final Vector3f eye = new Vector3f();
    private final Vector3f target;
    private final Vector3f up;

    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    // nilai awal, untuk reset
    private final float initRadius, initAzimuth, initElevation;

    public Camera3D(Vector3f target, Vector3f up, float radius, float azimuthDeg, float elevationDeg) {
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);
        this.radius = radius;
        this.azimuthDeg = azimuthDeg;
        this.elevationDeg = elevationDeg;
        this.initRadius = radius;
        this.initAzimuth = azimuthDeg;
        this.initElevation = elevationDeg;
        updateEye();
    }

    // Koordinat sferis -> posisi eye (mengelilingi target)
    private void updateEye() {
        float az = (float) Math.toRadians(azimuthDeg);
        float el = (float) Math.toRadians(elevationDeg);
        eye.x = target.x + radius * (float) (Math.cos(el) * Math.sin(az));
        eye.y = target.y + radius * (float) Math.sin(el);
        eye.z = target.z + radius * (float) (Math.cos(el) * Math.cos(az));
    }

    // ===== Navigasi =====
    public void rotate(float deltaAzimuth, float deltaElevation) {
        azimuthDeg += deltaAzimuth;
        elevationDeg = Math.max(-MAX_ELEVATION, Math.min(MAX_ELEVATION, elevationDeg + deltaElevation));
        updateEye();
    }

    public void zoom(float deltaRadius) {
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius + deltaRadius));
        updateEye();
    }

    public void reset() {
        radius = initRadius;
        azimuthDeg = initAzimuth;
        elevationDeg = initElevation;
        updateEye();
    }

    // ===== Matriks LookAt (View Matrix), dihitung manual =====
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();      // n = (E - At) / ||E - At||

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();          // u = (Up x n) / ||Up x n||

        Vector3f v = new Vector3f();
        n.cross(u, v);                       // v = n x u

        Matrix4f view = new Matrix4f();
        view.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        view.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        view.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        view.m03(0f).m13(0f).m23(0f).m33(1f);
        return view;
    }

        // Untuk preset mode proyeksi: set target & sudut sekaligus (radius dipertahankan)
    public void applyPreset(Vector3f newTarget, float azimuth, float elevation) {
        target.set(newTarget);
        azimuthDeg = azimuth;
        elevationDeg = Math.max(-MAX_ELEVATION, Math.min(MAX_ELEVATION, elevation));
        updateEye();
    }
    
    // ===== Getter =====
    public Vector3f getEye()   { return new Vector3f(eye); }
    public Vector3f getTarget(){ return new Vector3f(target); }
    public Vector3f getUp()    { return new Vector3f(up); }
    public float getRadius()   { return radius; }
}