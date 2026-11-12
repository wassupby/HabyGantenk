package com.grafkom.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Membungkus satu program GLSL (vertex + fragment shader).
 * Shader dibaca dari folder resources, misal "/shaders/phong.vert".
 */
public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();
    private final float[] matBuf = new float[16];

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentPath), fragmentPath);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Gagal link shader: " + glGetProgramInfoLog(programId));
        }

        // Setelah di-link, objek shader tidak diperlukan lagi
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    // ===== Pemakaian =====
    public void bind()    { glUseProgram(programId); }
    public void unbind()  { glUseProgram(0); }
    public void cleanup() { glDeleteProgram(programId); }

    // ===== Uniform setter =====
    public void setInt(String name, int value)       { glUniform1i(loc(name), value); }
    public void setFloat(String name, float value)   { glUniform1f(loc(name), value); }
    public void setBool(String name, boolean value)  { glUniform1i(loc(name), value ? 1 : 0); }
    public void setVec3(String name, Vector3f v)     { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }

    public void setMat4(String name, Matrix4f m) {
        glUniformMatrix4fv(loc(name), false, m.get(matBuf));
    }

    // ===== Internal =====
    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    private static int compile(int type, String source, String label) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Gagal compile " + label + ":\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("File shader tidak ditemukan: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca shader: " + path, e);
        }
    }
}