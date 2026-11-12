package com.grafkom.projection;

import com.grafkom.projection.PerspectiveProjection;
import com.grafkom.camera.Camera3D;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ProjectionTaxonomyManager {

    public enum ProjectionType {
        //                 label,                              perspektif, azimuth, elevasi, targetX, targetY
        ORTHOGRAPHIC      ("Ortografik (Tampak Depan)",        false,  0f,  0f,      0f,   0f),
        ISOMETRIC         ("Isometrik",                        false, 45f, 35.264f,  0f,   0f),
        OBLIQUE_CAVALIER  ("Oblik Cavalier (45 derajat)",      false,  0f,  0f,      0f,   0f),
        PERSPECTIVE_1POINT("Perspektif 1 Titik Hilang",        true,   0f,  0f,      2f,   1.5f),
        PERSPECTIVE_2POINT("Perspektif 2 Titik Hilang",        true,  45f,  0f,      0f,   1.5f),
        PERSPECTIVE_3POINT("Perspektif 3 Titik Hilang",        true,  45f, 35f,      0f,   0f);

        private final String label;
        private final boolean perspective;
        private final float azimuth, elevation, targetX, targetY;

        ProjectionType(String label, boolean perspective, float azimuth, float elevation,
                       float targetX, float targetY) {
            this.label = label;
            this.perspective = perspective;
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public String getLabel()       { return label; }
        public boolean isPerspective() { return perspective; }
        public String getCategory()    { return perspective ? "Perspektif" : "Paralel"; }
        public float getAzimuth()      { return azimuth; }
        public float getElevation()    { return elevation; }
        public Vector3f getTarget()    { return new Vector3f(targetX, targetY, 0f); }
    }

    private static final float OBLIQUE_ANGLE_DEG = 45f; // sudut sumbu kedalaman
    private static final float OBLIQUE_SCALE = 1.0f;    // Cavalier = 1.0 (Cabinet = 0.5)

    private ProjectionType type = ProjectionType.PERSPECTIVE_3POINT;

    public ProjectionType getType() { return type; }
    public void setType(ProjectionType type) { this.type = type; }

    // Pindahkan kamera ke posisi preset sesuai mode yang dipilih
    public void applyPreset(Camera3D camera) {
        camera.applyPreset(type.getTarget(), type.getAzimuth(), type.getElevation());
    }

    // Matriks proyeksi sesuai mode aktif
    public Matrix4f getProjectionMatrix(float aspect, float radius, PerspectiveProjection perspective) {
        if (type.isPerspective()) {
            return perspective.calculateProjectionMatrix();
        }

        // Proyeksi paralel: ukuran volume pandang mengikuti radius, sehingga zoom tetap bekerja
        float halfH = radius * 0.5f;
        float halfW = halfH * aspect;
        Matrix4f ortho = new Matrix4f().ortho(-halfW, halfW, -halfH, halfH, 0.1f, 50f);

        if (type == ProjectionType.OBLIQUE_CAVALIER) {
            float a = (float) Math.toRadians(OBLIQUE_ANGLE_DEG);

            // Shear: x' = x - L*cos(a)*(z + r), y' = y - L*sin(a)*(z + r)
            // (z + r) membuat bidang target (z = -r) tidak bergeser
            Matrix4f shear = new Matrix4f();
            shear.m20(-OBLIQUE_SCALE * (float) Math.cos(a));
            shear.m21(-OBLIQUE_SCALE * (float) Math.sin(a));
            shear.m30(shear.m20() * radius);
            shear.m31(shear.m21() * radius);
            return ortho.mul(shear);
        }
        return ortho;
    }
}