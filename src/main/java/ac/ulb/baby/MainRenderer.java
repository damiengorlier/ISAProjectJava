package ac.ulb.baby;

import ac.ulb.enums.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;
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
import java.nio.FloatBuffer;

public class MainRenderer extends GLJPanel implements GLEventListener {


    // Constantes
    private static final String TEXTURES_ROOT = "/textures";
    private static final String UTERUS_TEXTURE_PATH = TEXTURES_ROOT + "/" + "uterus_text.png";
    private static final String UTERUS_BUMP_PATH = TEXTURES_ROOT + "/" + "uterus_bump.png";

    private static final String MODEL_ROOT = "/model";
    private static final String BABY_MODEL_OBJ_PATH = MODEL_ROOT + "/baby_original_triangles.obj";

    private static final String SHADERS_ROOT = "/shaders";
    private static final String SHADER_EXT = "glsl";
    private static final String VERTEX_SHADER = "vs";
    private static final String FRAGMENT_SHADER = "fs";
    private static final String SPHERE_SHADERS_PATH = "/sphere";

    // Sphere radius
    private static final int R = 10;

    // Outils & Containers
    private GLU glu;
    private Texture uterusTexture;
    private Texture uterusBump;
    private OBJModel modelBaby;

    // Sphere
    private ShaderLocation sphereShaderLocation;
    private int sphereLightPositionLocation;

    private float angleX = 0;
    private float angleY = 0;
    // TODO
    private float angleZ = 0;

    private float[] eyePosition = new float[]{0, 0, 30};

    private static final int NUM_LIGHTS = 6;
    private static final float[] POS_X = {1, (float) -0.5};
    private static final float[] POS_Y = {(float) Math.sin(Math.PI / 4), (float) -Math.sin(Math.PI / 4)};
    private static final float[] POS_Z = {0, (float) Math.sin(2 * Math.PI / 3), (float) -Math.sin(2 * Math.PI / 3)};

    private float[] lightPosition = {
            R * POS_X[0], R * POS_Y[0], R * POS_Z[0],
            R * POS_X[0], R * POS_Y[1], R * POS_Z[0],
            R * POS_X[1], R * POS_Y[0], R * POS_Z[1],
            R * POS_X[1], R * POS_Y[1], R * POS_Z[1],
            R * POS_X[1], R * POS_Y[0], R * POS_Z[2],
            R * POS_X[1], R * POS_Y[1], R * POS_Z[2]
    };

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

        initPerspective(drawable);

        initShaders(drawable);
        initTextures(drawable);
        initBaby();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        cleanShader(drawable, sphereShaderLocation);

        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        updateEye();

        gl.glUseProgram(sphereShaderLocation.programLocation);

        gl.glUniform3fv(sphereLightPositionLocation, NUM_LIGHTS, FloatBuffer.wrap(lightPosition));

//        drawSphere(drawable, R, 32, 32);
        gl.glPushMatrix();
        updateRotations(drawable);
        renderModel(drawable, modelBaby);
        gl.glPopMatrix();

        // Pour visualiser les lumières
        gl.glUseProgram(0);
//        gl.glLoadIdentity();
//        for(int i = 0; i < NUM_LIGHTS; ++i) {
//		/* render sphere with the light's color/position */
//            gl.glPushMatrix();
//            gl.glTranslatef(lightPosition[i * 3], lightPosition[i * 3 + 1], lightPosition[i * 3 + 2]);
//            gl.glColor3fv(FloatBuffer.wrap(new float[]{1, 1, 1}));
//            glut.glutSolidSphere(0.1, 36, 36);
//            gl.glPopMatrix();
//        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glViewport(0, 0, width, height);
    }

    private void initPerspective(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();

        float aspect = (float) getWidth() / (float) getHeight();
        glu.gluPerspective(60, aspect, 0.1, 200);

        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    private void updateEye() {
        glu.gluLookAt(eyePosition[0], eyePosition[1], eyePosition[2], eyePosition[0], eyePosition[1], 0, 0, 1, 0);
    }

    private void initTextures(GLAutoDrawable drawable) {
        uterusTexture = loadTexture(drawable, UTERUS_TEXTURE_PATH);
        uterusBump = loadTexture(drawable, UTERUS_BUMP_PATH);
    }

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

        for (OBJObject object : model.getObjects()) {
            for (OBJMesh mesh : object.getMeshes()) {
                for (OBJFace face : mesh.getFaces()) {
                    gl.glBegin(GL2.GL_TRIANGLES);
                    for (OBJDataReference reference : face.getReferences()) {
                        final OBJVertex vertex = model.getVertex(reference);
                        if (reference.hasTexCoordIndex()) {
                            final OBJTexCoord texCoord = model.getTexCoord(reference);
                            gl.glTexCoord3f(texCoord.u, texCoord.v, texCoord.w);
                        }
                        if (reference.hasNormalIndex()) {
                            final OBJNormal normal = model.getNormal(reference);
                            gl.glNormal3f(normal.x, normal.y, normal.z);
                        } else {

                        }
                        gl.glVertex3f(vertex.x, vertex.y, vertex.z);
                    }
                    gl.glEnd();
                }
            }
        }
    }

    private void initShaders(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        sphereShaderLocation = initShader(drawable, SPHERE_SHADERS_PATH);
        sphereLightPositionLocation = gl.glGetUniformLocation(sphereShaderLocation.programLocation, "lightPosition");
    }

    private ShaderLocation initShader(GLAutoDrawable drawable, String shadersPath) {
        GL2 gl = drawable.getGL().getGL2();
        ShaderCode vertShader = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                SHADERS_ROOT, null, shadersPath + "/" + VERTEX_SHADER, SHADER_EXT, null, true);
        ShaderCode fragShader = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                SHADERS_ROOT, null, shadersPath + "/" + FRAGMENT_SHADER, SHADER_EXT, null, true);

        vertShader.compile(gl);
        fragShader.compile(gl);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl);
        shaderProgram.link(gl, System.out);

        ShaderLocation location = new ShaderLocation();
        location.vShaderLocation = vertShader.shaderBinaryFormat();
        location.fShaderLocation = fragShader.shaderBinaryFormat();
        location.programLocation = shaderProgram.program();

        return location;
    }

    private void cleanShader(GLAutoDrawable drawable, ShaderLocation location) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glDetachShader(location.programLocation, location.vShaderLocation);
        gl.glDetachShader(location.programLocation, location.fShaderLocation);
        gl.glDeleteShader(location.vShaderLocation);
        gl.glDeleteShader(location.fShaderLocation);
        gl.glDeleteProgram(location.programLocation);
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
        gl.glRotatef(angleX, 1, 0, 0);
        gl.glRotatef(angleY, 0, 1, 0);
        gl.glRotatef(angleZ, 0, 0, 1);
    }

    private void drawSphere(GLAutoDrawable drawable, int radius, int slices, int stacks) {
        GL2 gl = drawable.getGL().getGL2();

        if (uterusTexture == null) {
            throw new RuntimeException("Error : no texture for the sphere");
        } else {
            uterusTexture.enable(gl);
            uterusTexture.bind(gl);
        }

        gl.glRotatef(90, 0, 1, 0);
        gl.glRotatef(90, 0, 0, 1);

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
        this.getActionMap().put(ActionEnum.LESS_DIST_X.action(), new ActionLessEyeX());
        this.getActionMap().put(ActionEnum.PLUS_DIST_X.action(), new ActionPlusEyeX());
        this.getActionMap().put(ActionEnum.LESS_DIST_Y.action(), new ActionLessEyeY());
        this.getActionMap().put(ActionEnum.PLUS_DIST_Y.action(), new ActionPlusEyeY());
        this.getActionMap().put(ActionEnum.LESS_DIST_Z.action(), new ActionLessEyeZ());
        this.getActionMap().put(ActionEnum.PLUS_DIST_Z.action(), new ActionPlusEyeZ());
    }

    private class ActionLessAngleX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleX -= 5;
            if (angleX < 0) {
                angleX += 360;
            }
        }
    }

    private class ActionPlusAngleX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleX += 5;
            if (angleX > 360) {
                angleX -= 360;
            }
        }
    }

    private class ActionLessAngleY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleY -= 5;
            if (angleY < 0) {
                angleY += 360;
            }
        }
    }

    private class ActionPlusAngleY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleY += 5;
            if (angleY > 360) {
                angleY -= 360;
            }
        }
    }

    private class ActionLessAngleZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleZ -= 5;
            if (angleZ < 0) {
                angleZ += 360;
            }
        }
    }

    private class ActionPlusAngleZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            angleZ += 5;
            if (angleZ > 360) {
                angleZ -= 360;
            }
        }
    }

    private class ActionLessEyeX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            eyePosition[0] -= 1;
        }
    }

    private class ActionPlusEyeX extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            eyePosition[0] += 1;
        }
    }

    private class ActionLessEyeY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            eyePosition[1] -= 1;
        }
    }

    private class ActionPlusEyeY extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            eyePosition[1] += 1;
        }
    }

    private class ActionLessEyeZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            eyePosition[2] -= 2;
        }
    }

    private class ActionPlusEyeZ extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            eyePosition[2] += 2;
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
                eyePosition[0] += (prevMouseX - x) / 10;
                eyePosition[1] += (y - prevMouseY) / 10;
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
            eyePosition[2] += e.getWheelRotation() * 5;
        }
    }

    private class ShaderLocation {
        public int vShaderLocation;
        public int fShaderLocation;
        public int programLocation;
    }
}
