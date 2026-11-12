package com.grafkom.viewing3d;

import com.grafkom.render.StereoAnaglyphRenderer;
import com.grafkom.projection.ProjectionTaxonomyManager;
import com.grafkom.projection.PerspectiveProjection;
import com.grafkom.render.Mesh3D;
import com.grafkom.camera.Camera3D;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import com.grafkom.render.LitMesh;
import com.grafkom.render.ShaderProgram;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class ThreeDViewing {
    private long window;
    private int fbWidth = 800, fbHeight = 600;

    private Camera3D camera;
    private PerspectiveProjection projection;
    private ProjectionTaxonomyManager taxonomy;
    private StereoAnaglyphRenderer stereo;
    private Mesh3D cube;

        // ===== Visual Realism (Minggu 11) =====
    private ShaderProgram shader;
    private ShaderProgram depthShader;
    private ShadowMapFBO shadowMap;
    private final Vector3f lightPos = new Vector3f(3f, 4f, 3f);
    private final Matrix4f lightProj = new Matrix4f();
    private final Matrix4f lightView = new Matrix4f();
    private final Matrix4f lightSpaceMatrix = new Matrix4f();
    private LitMesh sphere;
    private LitMesh floor;
    private final Matrix4f modelMat = new Matrix4f();
    private final Matrix4f viewMat = new Matrix4f();
    private final Matrix4f projMat = new Matrix4f();
    private final float[] glMat = new float[16];
    private boolean blinnPhong = true;
    private boolean flatShading = false;
    private boolean depthTest = true;
    private boolean cullFace = true;
    private Texture2D texture;
    private boolean useTexture = true;
    private boolean useShadow = true;
    private int fps = 0;
    private int frameCount = 0;
    private double fpsTimer = 0;
    private boolean vsync = true;
    private boolean nearestFilter = false;
    private boolean dollyZoom = false;
    private boolean stereoOn = false;

    private boolean dragging = false, firstDrag = true;
    private double lastX, lastY;

    public static void main(String[] args) {
        new ThreeDViewing().run();
        
    }

    private void run() {
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(800, 600, "3D Viewing Visualizer", 0, 0);
        if (window == 0) throw new RuntimeException("Gagal membuat window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glfwShowWindow(window);
        glEnable(GL_DEPTH_TEST);

        camera = new Camera3D(new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), 7f, 30f, 25f);
        projection = new PerspectiveProjection(45f, 800f / 600f, 0.1f, 50f);
        taxonomy = new ProjectionTaxonomyManager();
        taxonomy.applyPreset(camera);
        stereo = new StereoAnaglyphRenderer();
        cube = Mesh3D.createColorCube(1.5f);
        shader = new ShaderProgram("/shaders/phong.vert.txt", "/shaders/phong.frag.txt");
        depthShader = new ShaderProgram("/shaders/depth.vert.txt", "/shaders/depth.frag.txt");
        shadowMap = new ShadowMapFBO();
        sphere = LitMesh.createSphere(1.0f, 32, 64);
        floor = LitMesh.createFloor(5f, -1.02f);
        texture = new Texture2D("/textures/checker.png");
        setupCallbacks();

        float[] buf = new float[16];
        double lastTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;
            frameCount++;
            fpsTimer += dt;
            if (fpsTimer >= 0.5) {              // diperbarui tiap 0,5 detik
                fps = (int) (frameCount / fpsTimer);
                frameCount = 0;
                fpsTimer = 0;
            }
            handleKeyboard(dt);
            renderShadowPass();

            glClearColor(0.1f, 0.1f, 0.15f, 1f);
            HSRManager.setupDepthTest(depthTest);
            HSRManager.setupBackFaceCulling(cullFace);

            float aspect = (float) fbWidth / Math.max(1, fbHeight);
            projection.setAspectRatio(aspect);

            if (stereoOn && taxonomy.getType().isPerspective()) {
                // Mode stereo anaglyph (hanya untuk proyeksi perspektif)
                stereo.renderAnaglyph(camera, projection, this::drawScene);
            } else {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                glMatrixMode(GL_PROJECTION);
                glLoadMatrixf(taxonomy.getProjectionMatrix(aspect, camera.getRadius(), projection).get(buf));

                glMatrixMode(GL_MODELVIEW);
                glLoadMatrixf(camera.calculateLookAtMatrix().get(buf));

                drawScene();
            }

            updateTitle();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        
        shadowMap.cleanup();
        depthShader.cleanup();
        shader.cleanup();
        sphere.cleanup();
        floor.cleanup();
        texture.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

        // Render pass 1: gambar scene dari sudut pandang lampu, hasilnya hanya depth
    private void renderShadowPass() {
        lightProj.identity().ortho(-9f, 9f, -9f, 9f, 1f, 20f);
        lightView.identity().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        lightSpaceMatrix.set(lightProj).mul(lightView);

        shadowMap.bindForWriting();
        depthShader.bind();
        depthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        depthShader.setMat4("model", modelMat);
        floor.draw(false);
        sphere.draw(false);
        depthShader.unbind();
        shadowMap.unbind();

        glViewport(0, 0, fbWidth, fbHeight);   // kembalikan viewport layar
    }
    
        private void drawScene() {
        drawGrid();   // grid pakai pipeline lama

        glGetFloatv(GL_PROJECTION_MATRIX, glMat);
        projMat.set(glMat);
        glGetFloatv(GL_MODELVIEW_MATRIX, glMat);
        viewMat.set(glMat);

        shader.bind();
        shader.setMat4("model", modelMat);
        shader.setMat4("view", viewMat);
        shader.setMat4("projection", projMat);
        shader.setVec3("lightPos", lightPos);
        shader.setVec3("viewPos", camera.getEye());
        shader.setVec3("lightColor", 1f, 1f, 1f);
        shader.setBool("useBlinnPhong", blinnPhong);
        shader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        shadowMap.bindTexture(1);
        shader.setInt("shadowMap", 1);
        shader.setBool("useShadow", useShadow);

        // --- Lantai: warna polos, tanpa tekstur ---
        shader.setBool("useTexture", false);
        shader.setVec3("objectColor", 0.55f, 0.55f, 0.6f);
        floor.draw(false);

        // --- Bola: bertekstur ---
        shader.setVec3("objectColor", 0.9f, 0.3f, 0.3f);
        texture.bind(0);
        shader.setInt("textureSampler", 0);
        shader.setBool("useTexture", useTexture);
        sphere.draw(flatShading);

        shader.unbind();
    }

    private void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            fbWidth = width;
            fbHeight = height;
            glViewport(0, 0, width, height);
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) firstDrag = true;
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (!dragging) return;
            if (firstDrag) {
                lastX = x; lastY = y; firstDrag = false;
                return;
            }
            float dx = (float) (x - lastX);
            float dy = (float) (y - lastY);
            lastX = x; lastY = y;
            camera.rotate(-dx * 0.4f, dy * 0.4f);
        });

        glfwSetScrollCallback(window, (w, xoff, yoff) -> camera.zoom(-(float) yoff * 0.5f));

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;

            if (key == GLFW_KEY_X) dollyZoom = !dollyZoom;       // dipindah dari Z
            if (key == GLFW_KEY_B) blinnPhong = !blinnPhong;     // Phong <-> Blinn-Phong
            if (key == GLFW_KEY_F) flatShading = !flatShading;   // Flat <-> Smooth
            if (key == GLFW_KEY_Z) depthTest = !depthTest;       // Z-Buffer on/off
            if (key == GLFW_KEY_C) cullFace = !cullFace;         // Back-Face Culling on/off
            if (key == GLFW_KEY_U) useTexture = !useTexture;     // tekstur on/off
            if (key == GLFW_KEY_P) useShadow = !useShadow;       // bayangan on/off
            if (key == GLFW_KEY_V) {                             // VSync on/off
                vsync = !vsync;
                glfwSwapInterval(vsync ? 1 : 0);
            }
            if (key == GLFW_KEY_L) {                             // Nearest <-> Mipmap
                nearestFilter = !nearestFilter;
                texture.setFilter(nearestFilter);
            }
            if (key == GLFW_KEY_Y) stereo.reset();
            if (key == GLFW_KEY_T) stereoOn = !stereoOn;

            // Tombol 1-6: pilih mode proyeksi
            if (key >= GLFW_KEY_1 && key <= GLFW_KEY_6) {
                int index = key - GLFW_KEY_1;
                taxonomy.setType(ProjectionTaxonomyManager.ProjectionType.values()[index]);
                taxonomy.applyPreset(camera);
            }
        });
    }

    private void handleKeyboard(float dt) {
        float rotSpeed = 60f * dt;
        float zoomSpeed = 5f * dt;
        float fovSpeed = 30f * dt;
        float nearSpeed = 4f * dt;

        // Kamera
        if (glfwGetKey(window, GLFW_KEY_LEFT)  == GLFW_PRESS) camera.rotate(-rotSpeed, 0);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.rotate(rotSpeed, 0);
        if (glfwGetKey(window, GLFW_KEY_UP)    == GLFW_PRESS) camera.rotate(0, rotSpeed);
        if (glfwGetKey(window, GLFW_KEY_DOWN)  == GLFW_PRESS) camera.rotate(0, -rotSpeed);
        if (glfwGetKey(window, GLFW_KEY_W)     == GLFW_PRESS) camera.zoom(-zoomSpeed);
        if (glfwGetKey(window, GLFW_KEY_S)     == GLFW_PRESS) camera.zoom(zoomSpeed);
        if (glfwGetKey(window, GLFW_KEY_ESCAPE)== GLFW_PRESS) glfwSetWindowShouldClose(window, true);

        // R: kembali ke preset mode aktif
        if (glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS) {
            camera.reset();
            taxonomy.applyPreset(camera);
        }

        // FOV (Q = sempit, E = lebar)
        float oldFov = projection.getFovDegree();
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) projection.setFovDegree(oldFov - fovSpeed);
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) projection.setFovDegree(oldFov + fovSpeed);
        float newFov = projection.getFovDegree();

        // Dolly zoom (hanya bermakna pada mode perspektif)
        if (dollyZoom && taxonomy.getType().isPerspective() && newFov != oldFov) {
            double ratio = Math.tan(Math.toRadians(oldFov / 2.0)) / Math.tan(Math.toRadians(newFov / 2.0));
            float newRadius = (float) (camera.getRadius() * ratio);
            camera.zoom(newRadius - camera.getRadius());
        }

        // Near plane (N = dekat, M = jauh)
        if (glfwGetKey(window, GLFW_KEY_N) == GLFW_PRESS) projection.setZNear(projection.getZNear() - nearSpeed);
        if (glfwGetKey(window, GLFW_KEY_M) == GLFW_PRESS) projection.setZNear(projection.getZNear() + nearSpeed);

        // Stereo: I/K = IPD naik/turun, F/G = bidang zero parallax jauh/dekat
        if (glfwGetKey(window, GLFW_KEY_I) == GLFW_PRESS) stereo.adjustIpd(0.5f * dt);
        if (glfwGetKey(window, GLFW_KEY_K) == GLFW_PRESS) stereo.adjustIpd(-0.5f * dt);
                if (glfwGetKey(window, GLFW_KEY_H) == GLFW_PRESS) stereo.adjustFocalOffset(3f * dt);
        if (glfwGetKey(window, GLFW_KEY_J) == GLFW_PRESS) stereo.adjustFocalOffset(-3f * dt);
    }

        private void updateTitle() {
        ProjectionTaxonomyManager.ProjectionType t = taxonomy.getType();
        String stereoText;
        if (!stereoOn) stereoText = "OFF";
        else if (!t.isPerspective()) stereoText = "ON (hanya mode perspektif)";
        else stereoText = String.format("ON IPD=%.2f Fokus%+.1f", stereo.getIpd(), stereo.getFocalOffset());

            glfwSetWindowTitle(window, String.format(
            "FPS:%d VSync:%s | %s | %s | Z:%s | C:%s | Tex:%s %s | Shadow:%s | Stereo: %s",
            fps,
            vsync ? "ON" : "OFF",
            blinnPhong ? "Blinn-Phong" : "Phong",
            flatShading ? "Flat" : "Smooth",
            depthTest ? "ON" : "OFF",
            cullFace ? "ON" : "OFF",
            useTexture ? "ON" : "OFF",
            nearestFilter ? "Nearest" : "Mipmap",
            useShadow ? "ON" : "OFF",
            stereoText));
    }

    private void drawGrid() {
        glColor3f(0.35f, 0.35f, 0.4f);
        glLineWidth(1.0f);
        glBegin(GL_LINES);
        for (int i = -5; i <= 5; i++) {
            glVertex3f(i, -1f, -5f); glVertex3f(i, -1f, 5f);
            glVertex3f(-5f, -1f, i); glVertex3f(5f, -1f, i);
        }
        glEnd();
    }
}