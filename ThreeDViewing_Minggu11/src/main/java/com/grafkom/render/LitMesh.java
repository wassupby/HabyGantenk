package com.grafkom.render;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh segitiga dengan atribut: posisi(3) + normal(3) + UV(2) per vertex.
 * Menyimpan dua versi normal: FLAT (per-face) dan SMOOTH (rata-rata per-vertex).
 */
public class LitMesh {
    private static final int FLOATS_PER_VERTEX = 8;

    private final int vaoFlat, vboFlat;
    private final int vaoSmooth, vboSmooth;
    private final int vertexCount;

    /**
     * @param positions xyz tiap vertex
     * @param uvs       uv tiap vertex
     * @param indices   tiga indeks per segitiga (urutan CCW saat dilihat dari luar)
     */
    public LitMesh(float[] positions, float[] uvs, int[] indices) {
        float[][] tri = buildTriangles(positions, indices);   // validasi + buang segitiga degenerat
        int triCount = tri.length;
        vertexCount = triCount * 3;

        // ---------- 1. Normal tiap segitiga (untuk FLAT) ----------
        float[][] faceNormal = new float[triCount][];
        for (int t = 0; t < triCount; t++) {
            int i0 = (int) tri[t][0], i1 = (int) tri[t][1], i2 = (int) tri[t][2];
            faceNormal[t] = cross(positions, i0, i1, i2);     // belum dinormalisasi (bobot = luas)
        }

        // ---------- 2. Normal vertex (untuk SMOOTH) ----------
        // Vertex yang posisinya sama (misal sambungan UV pada bola) dikelompokkan,
        // supaya normalnya dirata-ratakan bersama dan tidak muncul garis jahitan.
        Map<String, float[]> sum = new HashMap<>();
        for (int t = 0; t < triCount; t++) {
            for (int k = 0; k < 3; k++) {
                int idx = (int) tri[t][k];
                float[] s = sum.computeIfAbsent(key(positions, idx), x -> new float[3]);
                s[0] += faceNormal[t][0];
                s[1] += faceNormal[t][1];
                s[2] += faceNormal[t][2];
            }
        }

        // ---------- 3. Susun array interleaved ----------
        float[] flat = new float[vertexCount * FLOATS_PER_VERTEX];
        float[] smooth = new float[vertexCount * FLOATS_PER_VERTEX];
        int o = 0;
        for (int t = 0; t < triCount; t++) {
            float[] fn = normalize(faceNormal[t]);
            for (int k = 0; k < 3; k++) {
                int idx = (int) tri[t][k];
                float[] sn = normalize(sum.get(key(positions, idx)));

                put(flat, o, positions, uvs, idx, fn);
                put(smooth, o, positions, uvs, idx, sn);
                o += FLOATS_PER_VERTEX;
            }
        }

        // ---------- 4. Kirim ke GPU ----------
        vaoFlat = glGenVertexArrays();
        vboFlat = glGenBuffers();
        upload(vaoFlat, vboFlat, flat);

        vaoSmooth = glGenVertexArrays();
        vboSmooth = glGenBuffers();
        upload(vaoSmooth, vboSmooth, smooth);
    }

    // ===== Menggambar =====
    public void draw(boolean flatShading) {
        glBindVertexArray(flatShading ? vaoFlat : vaoSmooth);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vboFlat);
        glDeleteBuffers(vboSmooth);
        glDeleteVertexArrays(vaoFlat);
        glDeleteVertexArrays(vaoSmooth);
    }

    // ===== Factory: Bola =====
    public static LitMesh createSphere(float radius, int stacks, int slices) {
        int cols = slices + 1;
        int vCount = (stacks + 1) * cols;
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];

        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;            // 0 (kutub atas) .. PI (kutub bawah)
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * j / slices;
                int v = i * cols + j;
                pos[v * 3]     = (float) (radius * Math.sin(phi) * Math.cos(theta));
                pos[v * 3 + 1] = (float) (radius * Math.cos(phi));
                pos[v * 3 + 2] = (float) (radius * Math.sin(phi) * Math.sin(theta));
                uv[v * 2]      = (float) j / slices;
                uv[v * 2 + 1]  = 1f - (float) i / stacks;
            }
        }

        int[] idx = new int[stacks * slices * 6];
        int n = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * cols + j;
                int b = a + cols;
                int c = b + 1;
                int d = a + 1;
                idx[n++] = a; idx[n++] = d; idx[n++] = b;   // CCW dilihat dari luar
                idx[n++] = d; idx[n++] = c; idx[n++] = b;
            }
        }
        return new LitMesh(pos, uv, idx);
    }

    // ===== Factory: Kubus =====
    public static LitMesh createCube(float size) {
        float h = size / 2f;
        float[][] corner = {
            {-h, -h,  h}, { h, -h,  h}, { h,  h,  h}, {-h,  h,  h},
            {-h, -h, -h}, { h, -h, -h}, { h,  h, -h}, {-h,  h, -h}
        };
        int[][] faces = {
            {0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7},
            {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}
        };
        float[][] quadUv = { {0, 0}, {1, 0}, {1, 1}, {0, 1} };

        float[] pos = new float[24 * 3];
        float[] uv = new float[24 * 2];
        int[] idx = new int[36];
        for (int f = 0; f < 6; f++) {
            for (int k = 0; k < 4; k++) {
                int v = f * 4 + k;
                float[] p = corner[faces[f][k]];
                pos[v * 3] = p[0]; pos[v * 3 + 1] = p[1]; pos[v * 3 + 2] = p[2];
                uv[v * 2] = quadUv[k][0]; uv[v * 2 + 1] = quadUv[k][1];
            }
            int b = f * 4;
            int i = f * 6;
            idx[i] = b;     idx[i + 1] = b + 1; idx[i + 2] = b + 2;
            idx[i + 3] = b; idx[i + 4] = b + 2; idx[i + 5] = b + 3;
        }
        return new LitMesh(pos, uv, idx);
    }

        // ===== Factory: Lantai (bidang datar, normal +Y) =====
    public static LitMesh createFloor(float halfSize, float y) {
        float s = halfSize;
        float[] pos = {
            -s, y, -s,
            -s, y,  s,
             s, y,  s,
             s, y, -s
        };
        float[] uv = {
            0, 0,
            0, 1,
            1, 1,
            1, 0
        };
        int[] idx = { 0, 1, 2,   0, 2, 3 };
        return new LitMesh(pos, uv, idx);
    }
    
    // ===== Helper internal =====
    private static float[][] buildTriangles(float[] pos, int[] indices) {
        java.util.List<float[]> list = new java.util.ArrayList<>();
        for (int t = 0; t + 2 < indices.length; t += 3) {
            float[] n = cross(pos, indices[t], indices[t + 1], indices[t + 2]);
            float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (len < 1e-8f) continue;                 // segitiga degenerat (kutub bola)
            list.add(new float[] { indices[t], indices[t + 1], indices[t + 2] });
        }
        return list.toArray(new float[0][]);
    }

    private static float[] cross(float[] p, int i0, int i1, int i2) {
        float ax = p[i1 * 3] - p[i0 * 3], ay = p[i1 * 3 + 1] - p[i0 * 3 + 1], az = p[i1 * 3 + 2] - p[i0 * 3 + 2];
        float bx = p[i2 * 3] - p[i0 * 3], by = p[i2 * 3 + 1] - p[i0 * 3 + 1], bz = p[i2 * 3 + 2] - p[i0 * 3 + 2];
        return new float[] { ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx };
    }

    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1e-8f) return new float[] { 0f, 1f, 0f };
        return new float[] { v[0] / len, v[1] / len, v[2] / len };
    }

    private static String key(float[] p, int idx) {
        return Math.round(p[idx * 3] * 10000f) + "," + Math.round(p[idx * 3 + 1] * 10000f)
                + "," + Math.round(p[idx * 3 + 2] * 10000f);
    }

    private static void put(float[] dst, int o, float[] pos, float[] uv, int idx, float[] n) {
        dst[o]     = pos[idx * 3];
        dst[o + 1] = pos[idx * 3 + 1];
        dst[o + 2] = pos[idx * 3 + 2];
        dst[o + 3] = n[0];
        dst[o + 4] = n[1];
        dst[o + 5] = n[2];
        dst[o + 6] = uv[idx * 2];
        dst[o + 7] = uv[idx * 2 + 1];
    }

    private static void upload(int vao, int vbo, float[] data) {
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);                    // aPos
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);      // aNormal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);      // aTexCoords
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
}