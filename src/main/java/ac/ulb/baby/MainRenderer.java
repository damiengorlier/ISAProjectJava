package ac.ulb.baby;

import ac.ulb.enums.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.momchil_atanasov.data.front.parser.*;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;

public class MainRenderer extends GLJPanel implements GLEventListener {

    private static final String TEXTURES_ROOT = "/textures";
    private static final String UTERUS_TEXTURE_PATH = TEXTURES_ROOT + "/" + "uterus_text.png";
    private static final String UTERUS_BUMP_PATH = TEXTURES_ROOT + "/" + "uterus_bump.png";

    private static final String MODEL_ROOT = "/model";
    private static final String BABY_MODEL_OBJ_PATH = MODEL_ROOT + "/baby_original_triangles.obj";

    private static final String SHADERS_ROOT = "/shaders";

    private int program;
    private GLU glu;
    private Texture uterusTexture;
    private Texture uterusBump;

    private OBJModel modelBaby;

    static float angleX = 0;
    static float angleY = 0;
    static float angleZ = 0;

    static float distX = 0;
    static float distY = 0;
    static float distZ = 100;

    private boolean mouseFirstPressed = false;
    private boolean mouseSecondPressed = false;

    private float prevMouseX;
    private float prevMouseY;

    public MainRenderer() {
        this.addGLEventListener(this);

        initInputMap();
        initActionMap();

        MouseHandler mouseHandler = new MouseHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
        this.addMouseWheelListener(mouseHandler);
    }

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

        initBaby();

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glDeleteProgram(program);

        System.exit(0);

    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        updateCamera(drawable);
        updateRotations(drawable);
        // drawSphere(drawable, glu, 10, 32, 32);
        gl.glUseProgram(program);
        renderModel(drawable, modelBaby);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glViewport(0, 0, width, height);
    }

    private void updateCamera(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();

        float aspect = (float) getWidth() / (float) getHeight();
        glu.gluPerspective(60, aspect, 0.1, 1000);
        glu.gluLookAt(distX, distY, distZ, distX, distY, 0, 0, 1, 0);

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

    private void initBaby() {

        InputStream objStream = MainWindow.class.getResourceAsStream(BABY_MODEL_OBJ_PATH);
        IOBJParser objParser = new OBJParser();
        try {
            modelBaby = objParser.parse(objStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderModel(GLAutoDrawable drawable, OBJModel model) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glBegin(GL2.GL_TRIANGLES);
        for (OBJObject object : model.getObjects()) {
            for (OBJMesh mesh : object.getMeshes()) {
                for (OBJFace face : mesh.getFaces()) {
                    for (OBJDataReference reference : face.getReferences()) {
                        final OBJVertex vertex = model.getVertex(reference);
                        gl.glVertex3f(vertex.x, vertex.y, vertex.z);
                        if (reference.hasNormalIndex()) {
                            final OBJNormal normal = model.getNormal(reference);
                            gl.glNormal3f(normal.x, normal.y, normal.z);
                        }
                        if (reference.hasTexCoordIndex()) {
                            final OBJTexCoord texCoord = model.getTexCoord(reference);
                            gl.glTexCoord3f(texCoord.u, texCoord.v, texCoord.w);
                        }
                    }
                }
            }
        }
        gl.glEnd();
    }

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

    private void updateRotations(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glRotatef(angleZ, 0, 0, 1);
        gl.glRotatef(angleY, 0, 1, 0);
        gl.glRotatef(angleX, 1, 0, 0);
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

    private void initInputMap() {
        // Rotations : Xx Yy Zz
        // Xx
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_X.key()), ActionEnum.LESS_ANGLE_X.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_X.key()), ActionEnum.PLUS_ANGLE_X.action());
        // Yy
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_Y.key()), ActionEnum.LESS_ANGLE_Y.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_Y.key()), ActionEnum.PLUS_ANGLE_Y.action());
        // Zz
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_Z.key()), ActionEnum.LESS_ANGLE_Z.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_Z.key()), ActionEnum.PLUS_ANGLE_Z.action());

        // Translations : Left Right Up Down
        // LEFT L l
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LEFT.key()), ActionEnum.LESS_DIST_X.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_L.key()), ActionEnum.LESS_DIST_X.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_L.key()), ActionEnum.LESS_DIST_X.action());
        // RIGHT R r
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.RIGHT.key()), ActionEnum.PLUS_DIST_X.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_R.key()), ActionEnum.PLUS_DIST_X.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_R.key()), ActionEnum.PLUS_DIST_X.action());
        // UP U u
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UP.key()), ActionEnum.PLUS_DIST_Y.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_U.key()), ActionEnum.PLUS_DIST_Y.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_U.key()), ActionEnum.PLUS_DIST_Y.action());
        // DOWN D d
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.DOWN.key()), ActionEnum.LESS_DIST_Y.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_D.key()), ActionEnum.LESS_DIST_Y.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_D.key()), ActionEnum.LESS_DIST_Y.action());

        // Zoom : In Out
        // I i
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_I.key()), ActionEnum.LESS_DIST_Z.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_I.key()), ActionEnum.LESS_DIST_Z.action());
        // O o
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_O.key()), ActionEnum.PLUS_DIST_Z.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_O.key()), ActionEnum.PLUS_DIST_Z.action());
    }

    private void initActionMap() {
        // Angles
        this.getActionMap().put(ActionEnum.LESS_ANGLE_X.action(), new ActionLessAngleX());
        this.getActionMap().put(ActionEnum.PLUS_ANGLE_X.action(), new ActionPlusAngleX());
        this.getActionMap().put(ActionEnum.LESS_ANGLE_Y.action(), new ActionLessAngleY());
        this.getActionMap().put(ActionEnum.PLUS_ANGLE_Y.action(), new ActionPlusAngleY());
        this.getActionMap().put(ActionEnum.LESS_ANGLE_Z.action(), new ActionLessAngleZ());
        this.getActionMap().put(ActionEnum.PLUS_ANGLE_Z.action(), new ActionPlusAngleZ());

        // Distances
        this.getActionMap().put(ActionEnum.LESS_DIST_X.action(), new ActionLessDistX());
        this.getActionMap().put(ActionEnum.PLUS_DIST_X.action(), new ActionPlusDistX());
        this.getActionMap().put(ActionEnum.LESS_DIST_Y.action(), new ActionLessDistY());
        this.getActionMap().put(ActionEnum.PLUS_DIST_Y.action(), new ActionPlusDistY());
        this.getActionMap().put(ActionEnum.LESS_DIST_Z.action(), new ActionLessDistZ());
        this.getActionMap().put(ActionEnum.PLUS_DIST_Z.action(), new ActionPlusDistZ());
    }

    private static class ActionLessAngleX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleX -= 5;
            if (angleX < 0) {
                angleX += 360;
            }
        }
    }

    private static class ActionPlusAngleX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleX += 5;
            if (angleX > 360) {
                angleX -= 360;
            }
        }
    }

    private static class ActionLessAngleY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleY -= 5;
            if (angleY < 0) {
                angleY += 360;
            }
        }
    }

    private static class ActionPlusAngleY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleY += 5;
            if (angleY > 360) {
                angleY -= 360;
            }
        }
    }

    private static class ActionLessAngleZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleZ -= 5;
            if (angleZ < 0) {
                angleZ += 360;
            }
        }
    }

    private static class ActionPlusAngleZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleZ += 5;
            if (angleZ > 360) {
                angleZ -= 360;
            }
        }
    }

    private static class ActionLessDistX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            distX -= 1;
        }
    }

    private static class ActionPlusDistX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            distX += 1;
        }
    }

    private static class ActionLessDistY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            distY -= 1;
        }
    }

    private static class ActionPlusDistY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            distY += 1;
        }
    }

    private static class ActionLessDistZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            distZ -= 5;
        }
    }

    private static class ActionPlusDistZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            distZ += 5;
        }
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(final MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                mouseFirstPressed = true;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseSecondPressed = true;
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                mouseFirstPressed = false;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseSecondPressed = false;
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            prevMouseX = e.getX();
            prevMouseY = e.getY();
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            float x = e.getX();
            float y = e.getY();
            if (mouseFirstPressed) {
                distX += (prevMouseX - x) / 10;
                distY += (y - prevMouseY) / 10;
            } else if (mouseSecondPressed) {
                float thetaY = 360 * (x - prevMouseX) / getWidth();
                float thetaX = 360 * (y - prevMouseY) / getHeight();
                angleX += thetaX;
                angleY += thetaY;
            }
            prevMouseX = x;
            prevMouseY = y;
        }

        @Override
        public void mouseWheelMoved(final MouseWheelEvent e) {
            distZ += e.getWheelRotation() * 5;
        }
    }
}
