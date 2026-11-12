package com.grafkom.projection;

import org.joml.Matrix4f;

public class PerspectiveProjection {
    public static final float MIN_FOV = 15f;
    public static final float MAX_FOV = 100f;

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = fovDegree;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    // Matriks proyeksi dihitung manual dari rumus frustum di modul
    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

        // Frustum asimetris (off-axis) untuk satu mata.
    // eyeOffset > 0 : mata kiri (mata berada di x = -eyeOffset pada view space)
    // eyeOffset < 0 : mata kanan
    // focalLength   : jarak bidang zero parallax
    public Matrix4f calculateOffAxisMatrix(float eyeOffset, float focalLength) {
        float top = zNear * (float) Math.tan(Math.toRadians(fovDegree) / 2.0);
        float bottom = -top;
        float right = top * aspectRatio;
        float left = -right;

        // geser jendela frustum di near plane
        float shift = eyeOffset * zNear / focalLength;
        left += shift;
        right += shift;

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(2f * zNear / (right - left));
        proj.m11(2f * zNear / (top - bottom));
        proj.m20((right + left) / (right - left));   // komponen asimetris
        proj.m21((top + bottom) / (top - bottom));
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1f);
        proj.m33(0f);
        return proj;
    }
    
    // ===== Setter dengan validasi =====
    public void setFovDegree(float fov) {
        this.fovDegree = Math.max(MIN_FOV, Math.min(MAX_FOV, fov));
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public void setZNear(float zNear) {
        // near harus > 0 dan selalu lebih kecil dari far
        this.zNear = Math.max(0.1f, Math.min(zFar - 1.0f, zNear));
    }

    // ===== Getter =====
    public float getFovDegree()   { return fovDegree; }
    public float getAspectRatio() { return aspectRatio; }
    public float getZNear()       { return zNear; }
    public float getZFar()        { return zFar; }
}