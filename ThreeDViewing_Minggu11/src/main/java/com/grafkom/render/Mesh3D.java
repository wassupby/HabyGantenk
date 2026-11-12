package com.grafkom.render;

import static org.lwjgl.opengl.GL11.*;

public class Mesh3D {
    private final float[][] vertices;    // posisi tiap titik (x, y, z)
    private final int[][] faces;         // tiap sisi = 4 indeks vertex
    private final float[][] faceColors;  // warna RGB tiap sisi
    private boolean showEdges = true;    // garis tepi hitam

    public Mesh3D(float[][] vertices, int[][] faces, float[][] faceColors) {
        this.vertices = vertices;
        this.faces = faces;
        this.faceColors = faceColors;
    }

    // Factory: kubus berwarna berpusat di (0,0,0), panjang sisi = size
    public static Mesh3D createColorCube(float size) {
        float h = size / 2.0f;
        float[][] v = {
            {-h, -h,  h}, { h, -h,  h}, { h,  h,  h}, {-h,  h,  h},  // 0-3 (depan, z+)
            {-h, -h, -h}, { h, -h, -h}, { h,  h, -h}, {-h,  h, -h}   // 4-7 (belakang, z-)
        };
        int[][] f = {
            {0, 1, 2, 3},  // depan
            {5, 4, 7, 6},  // belakang
            {4, 0, 3, 7},  // kiri
            {1, 5, 6, 2},  // kanan
            {3, 2, 6, 7},  // atas
            {4, 5, 1, 0}   // bawah
        };
        float[][] c = {
            {1.0f, 0.2f, 0.2f},  // merah
            {0.2f, 1.0f, 0.2f},  // hijau
            {0.2f, 0.4f, 1.0f},  // biru
            {1.0f, 1.0f, 0.2f},  // kuning
            {1.0f, 0.5f, 0.1f},  // oranye
            {0.8f, 0.2f, 1.0f}   // ungu
        };
        return new Mesh3D(v, f, c);
    }

    public void setShowEdges(boolean showEdges) { this.showEdges = showEdges; }

    public void draw() {
        // Isi sisi (digeser sedikit ke belakang agar garis tepi tidak "bertabrakan")
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        glBegin(GL_QUADS);
        for (int i = 0; i < faces.length; i++) {
            float[] col = faceColors[i];
            glColor3f(col[0], col[1], col[2]);
            for (int idx : faces[i]) {
                float[] p = vertices[idx];
                glVertex3f(p[0], p[1], p[2]);
            }
        }
        glEnd();
        glDisable(GL_POLYGON_OFFSET_FILL);

        // Garis tepi
        if (showEdges) {
            glColor3f(0.0f, 0.0f, 0.0f);
            glLineWidth(2.0f);
            for (int[] face : faces) {
                glBegin(GL_LINE_LOOP);
                for (int idx : face) {
                    float[] p = vertices[idx];
                    glVertex3f(p[0], p[1], p[2]);
                }
                glEnd();
            }
        }
    }
}