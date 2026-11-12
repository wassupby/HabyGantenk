package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

/**
 * Framebuffer yang hanya berisi depth texture (shadow map).
 * Dipakai untuk merender scene dari sudut pandang lampu.
 */
public class ShadowMapFBO {
    public static final int SIZE = 2048;

    private final int fbo;
    private final int depthTex;

    public ShadowMapFBO() {
        depthTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, SIZE, SIZE, 0,
                     GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // Di luar jangkauan shadow map dianggap "jauh" (depth 1.0) sehingga tidak ada bayangan
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[] { 1f, 1f, 1f, 1f });

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTex, 0);
        glDrawBuffer(GL_NONE);   // tidak ada color buffer
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("ShadowMapFBO tidak lengkap");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Mulai menggambar ke shadow map. */
    public void bindForWriting() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, SIZE, SIZE);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /** Kembali menggambar ke layar. Viewport harus di-restore oleh pemanggil. */
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Dipakai di 9d: shader membaca shadow map lewat texture unit ini. */
    public void bindTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTex);
    }

    public void cleanup() {
        glDeleteFramebuffers(fbo);
        glDeleteTextures(depthTex);
    }
}