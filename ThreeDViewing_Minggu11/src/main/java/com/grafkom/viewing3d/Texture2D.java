package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture2D {
    private final int textureID;

    // path berupa lokasi di resources, misal "/textures/checker.png"
    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        stbi_set_flip_vertically_on_load(true);   // gambar: baris atas = v tinggi

        // Baca file dari classpath (resources) ke memori
        byte[] bytes;
        try (InputStream in = Texture2D.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new RuntimeException("Tekstur tidak ditemukan: " + resourcePath);
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca tekstur: " + resourcePath, e);
        }

        ByteBuffer raw = MemoryUtil.memAlloc(bytes.length);
        raw.put(bytes).flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load_from_memory(raw, width, height, channels, 4);
            if (data == null) {
                throw new RuntimeException("Gagal decode tekstur: " + stbi_failure_reason());
            }
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                         GL_RGBA, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(data);
        } finally {
            MemoryUtil.memFree(raw);
        }

        setFilter(false);   // awal: Bilinear + Mipmap
    }

    // true  = Nearest Neighbor (blocky, tanpa mipmap)
    // false = Bilinear + Mipmap (halus)
    public void setFilter(boolean nearest) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (nearest) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}