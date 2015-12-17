package ac.ulb.baby;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.IOException;
import java.io.InputStream;

public class MainWindow implements GLEventListener, KeyListener, MouseListener {

    private static final String WINDOW_TITLE = "Baby Project";

    private static final String TEXTURES_ROOT = "/textures";
    private static final String UTERUS_TEXTURE_PATH = TEXTURES_ROOT + "/" + "uterus_text.png";
    private static final String UTERUS_BUMP_PATH = TEXTURES_ROOT + "/" + "uterus_bump.png";

    private static final String SHADERS_ROOT = "/shaders";

    private static final Dimension WINDOW_SIZE = new Dimension(1024, 768);
    private static final int FPS = 60;

    private static int screenIdx = 0;

    private static boolean undecorated = false;
    private static boolean alwaysOnTop = false;
    private static boolean fullscreen = false;
    private static boolean mouseVisible = true;
    private static boolean mouseConfined = false;
    public static GLWindow glWindow;
    public static FPSAnimator animator;

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, screenIdx);
        GLProfile.initSingleton();
        GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(WINDOW_SIZE.getWidth(), WINDOW_SIZE.getHeight());
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);
        glWindow.setVisible(true);
        glWindow.setTitle(WINDOW_TITLE);

        MainWindow mainWindow = new MainWindow();
        glWindow.addGLEventListener(mainWindow);
        glWindow.addKeyListener(mainWindow);

        animator = new FPSAnimator(glWindow, FPS);
        animator.start();
    }

    private static final int TEXTURE_NBR = 1;

    private int[] textures = new int[TEXTURE_NBR];

    private int program;

    private GLU glu;
    private Texture uterusTexture;
    private Texture uterusBump;

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        drawable.setGL(new DebugGL2(gl));
        glu = new GLU();

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClearDepth(1f);

        initShaders(drawable);

        uterusTexture = loadTexture(drawable, UTERUS_TEXTURE_PATH);
        uterusBump = loadTexture(drawable, UTERUS_BUMP_PATH);

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        animator.stop();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        if (!animator.isAnimating()) {
            return;
        }

        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        setCamera(drawable, 100);

        drawSphere(drawable, glu, 10, 16, 16);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glViewport(0, 0, width, height);
    }

    private void setCamera(GLAutoDrawable drawable, float distance) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();

        float aspect = (float) glWindow.getWidth() / (float) glWindow.getHeight();
        glu.gluPerspective(45, aspect, 1, 1000);
        glu.gluLookAt(0, 0, distance, 0, 0, 0, 0, 1, 0);

        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

//    private void initTextures(GLAutoDrawable drawable, GLU glu) {
//        GL2 gl = drawable.getGL().getGL2();
//
//        gl.glGenTextures(TEXTURE_NBR, textures, 0);
//
//        Texture texture = loadTexture(drawable, UTERUS_TEXTURE_PATH);
//
//        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
//        gl.glTexParameteri();
//    }

    private void initShaders(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        ShaderCode vertShader = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                SHADERS_ROOT, null, "vs", "glsl", null, true);
        ShaderCode fragShader = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                SHADERS_ROOT, null, "fs", "glsl", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl);

        program = shaderProgram.program();

        /**
         These links don't go into effect until you link the program. If you want
         to change index, you need to link the program again.
         */
        gl.glBindAttribLocation(program, 0, "position");
        gl.glBindAttribLocation(program, 3, "color");
        gl.glBindFragDataLocation(program, 0, "outputColor");

        shaderProgram.link(gl, System.out);
    }

    private Texture loadTexture(GLAutoDrawable drawable, String path) {
        System.out.println("loadTexture : " + path);
        GL2 gl = drawable.getGL().getGL2();
        try {
            InputStream stream = MainWindow.class.getResourceAsStream(path);
            TextureData data = TextureIO.newTextureData(gl.getGLProfile(), stream, false, TextureIO.PNG);
            return TextureIO.newTexture(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Error when loading texture " + path);
    }

    private void drawSphere(GLAutoDrawable drawable, GLU glu, int radius, int slices, int stacks) {
        GL2 gl = drawable.getGL().getGL2();

        if (uterusTexture == null) {
            throw new RuntimeException("Error : no texture for the sphere");
        } else {
            uterusTexture.enable(gl);
            uterusTexture.bind(gl);
        }

        GLUquadric sphere = glu.gluNewQuadric();
        glu.gluQuadricTexture(sphere, true);
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
        glu.gluQuadricNormals(sphere, GLU.GLU_FLAT);
        glu.gluQuadricOrientation(sphere, GLU.GLU_OUTSIDE);
        glu.gluSphere(sphere, radius, slices, stacks);
        glu.gluDeleteQuadric(sphere);
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }
}