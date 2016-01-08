package ac.ulb.baby;

import ac.ulb.enums.*;
import ac.ulb.bezier.RationalBezierSurface;
import ac.ulb.utils.Const;
import ac.ulb.utils.MathUtil;
import ac.ulb.utils.ShaderControl;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
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

    /**
	 * 
	 */
	private static final long serialVersionUID = 8443847388978895476L;
	// Outils & Containers
    private GLU glu;
    private Texture uterusTexture;
    private Texture uterusBump;
    private OBJModel modelBaby;

    // Sphere
    private int sphereLightPositionLocation;
    private int sphereUterusTextureLocation;
    private int sphereUterusBumpMapLocation;
    private ShaderControl sphereShaderControl;

    // Baby
    private int babyLightPositionLocation;
    private int babyUterusTextureLocation;
    private ShaderControl babyShaderControl;

    private float babyShift = 4.5f;
    private float[] babyAngle = new float[]{0, 0, 0};
    private float[] babyPosition = new float[]{0, -babyShift, 0};

    // Eye
    private float[] eyePosition = new float[]{0, 0, Const.Sphere.R};

    // Bezier
    private boolean computeBezier = true;

    private float[][][] controlPointsSphereUP4D;
    private float[][][] controlPointsSphereDOWN4D;

    private RationalBezierSurface sphereSurfaceUp;
    private RationalBezierSurface sphereSurfaceDown;

    // Actions
    //private boolean actionAllowed = true;

    private boolean mouseFirstPressed = false;
    private boolean mouseSecondPressed = false;

    private float prevMouseX;
    private float prevMouseY;

    private boolean animation = false;
    private AnimationStep animationStep = Const.Anim.FIRST_STEP;

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
        gl.glEnable(GL2.GL_AUTO_NORMAL);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClearDepth(1f);

        initPerspective(drawable);

        initBezierControlPoints();
        initShaders(drawable);
        initTextures(drawable);
        initModel();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        sphereShaderControl.cleanShaderProgram(gl);
        babyShaderControl.cleanShaderProgram(gl);
        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        manageAnimation();

        // View
        updateEye();

        // Uterus
        sphereShaderControl.useShaderProgram(gl);
        gl.glUniform3fv(sphereLightPositionLocation, Const.Light.NUM_LIGHTS, FloatBuffer.wrap(Const.Light.LIGHTS_POSITIONS));
        gl.glUniform1i(sphereUterusTextureLocation, 0);
        gl.glUniform1i(sphereUterusBumpMapLocation, 1);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        uterusTexture.bind(gl);
        gl.glActiveTexture(GL.GL_TEXTURE1);
        uterusBump.bind(gl);

        renderSphere(drawable);

        sphereShaderControl.stopUsingShaderProgram(gl);

        // Baby
        babyShaderControl.useShaderProgram(gl);
        gl.glUniform3fv(babyLightPositionLocation, Const.Light.NUM_LIGHTS, FloatBuffer.wrap(Const.Light.LIGHTS_POSITIONS));
        gl.glUniform1i(babyUterusTextureLocation, 0);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        uterusTexture.bind(gl);

        renderBaby(drawable);

        babyShaderControl.stopUsingShaderProgram(gl);
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

    private void initBezierControlPoints() {
        controlPointsSphereUP4D = Const.Sphere.CP_SPHERE_UP_4D;
        for (int i = 0; i < controlPointsSphereUP4D.length; i++) {
            for (int j = 0; j < controlPointsSphereUP4D[i].length; j++) {
                controlPointsSphereUP4D[i][j][0] *= Const.Sphere.R;
                controlPointsSphereUP4D[i][j][1] *= Const.Sphere.R;
                controlPointsSphereUP4D[i][j][2] *= Const.Sphere.R;
            }
        }

        controlPointsSphereDOWN4D = Const.Sphere.CP_SPHERE_DOWN_4D;
        for (int i = 0; i < controlPointsSphereDOWN4D.length; i++) {
            for (int j = 0; j < controlPointsSphereDOWN4D[i].length; j++) {
                controlPointsSphereDOWN4D[i][j][0] *= Const.Sphere.R;
                controlPointsSphereDOWN4D[i][j][1] *= Const.Sphere.R;
                controlPointsSphereDOWN4D[i][j][2] *= Const.Sphere.R;
            }
        }
        computeBezier = true;
    }

    private void initTextures(GLAutoDrawable drawable) {
        uterusTexture = loadTexture(drawable, Const.Path.UTERUS_TEXTURE_PATH);
        uterusBump = loadTexture(drawable, Const.Path.UTERUS_NORMAL_PATH);
    }

    private void initModel() {
        InputStream objStream = MainWindow.class.getResourceAsStream(Const.Path.BABY_MODEL_OBJ_PATH);
        IOBJParser objParser = new OBJParser();
        try {
            modelBaby = objParser.parse(objStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initShaders(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        sphereShaderControl = initShaderProgram(drawable, Const.Path.SPHERE_SHADERS_PATH);
        gl.glBindAttribLocation(sphereShaderControl.getShaderProgram(), Const.Attrib.ATTR_TANGENT_LOCATION, Const.Attrib.ATTR_TANGENT);
        sphereShaderControl.linkProgram(gl);
        sphereLightPositionLocation = gl.glGetUniformLocation(sphereShaderControl.getShaderProgram(), Const.Uniform.LIGHT_POSITION);
        sphereUterusTextureLocation = gl.glGetUniformLocation(sphereShaderControl.getShaderProgram(), Const.Uniform.UTERUS_TEXTURE);
        sphereUterusBumpMapLocation = gl.glGetUniformLocation(sphereShaderControl.getShaderProgram(), Const.Uniform.UTERUS_BUMP_MAP);


        babyShaderControl = initShaderProgram(drawable, Const.Path.BABY_SHADERS_PATH);
        babyShaderControl.linkProgram(gl);
        babyLightPositionLocation = gl.glGetUniformLocation(babyShaderControl.getShaderProgram(), Const.Uniform.LIGHT_POSITION);
        babyUterusTextureLocation = gl.glGetUniformLocation(babyShaderControl.getShaderProgram(), Const.Uniform.UTERUS_TEXTURE);
    }

    private ShaderControl initShaderProgram(GLAutoDrawable drawable, String shadersPath) {
        GL2 gl = drawable.getGL().getGL2();
        String absolutePath = Const.Path.SHADERS_ROOT + "/" + shadersPath;
        String vertexPath = absolutePath + "/" + Const.Path.VERTEX_SHADER + "." + Const.Path.SHADER_EXT;
        String fragmentPath = absolutePath + "/" + Const.Path.FRAGMENT_SHADER + "." + Const.Path.SHADER_EXT;
        ShaderControl shaderControl = new ShaderControl();
        shaderControl.setVertexSrc(ShaderControl.loadShader(vertexPath));
        shaderControl.setFragmentSrc(ShaderControl.loadShader(fragmentPath));
        shaderControl.init(gl);

        return shaderControl;
    }

    private Texture loadTexture(GLAutoDrawable drawable, String path) {
        //System.out.println("loadTexture : " + path);
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

    private void updateEye() {
        glu.gluLookAt(eyePosition[0], eyePosition[1], eyePosition[2], eyePosition[0], eyePosition[1], 0, 0, 1, 0);
    }

    private void renderBaby(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glPushMatrix();
        updateBabyGeometry(drawable);
        renderModel(drawable, modelBaby);
        gl.glPopMatrix();
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
                        }
                        gl.glVertex3f(vertex.x, vertex.y, vertex.z);
                    }
                    gl.glEnd();
                }
            }
        }
    }

    private void updateBabyGeometry(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glScalef(Const.Baby.SCALE[0], Const.Baby.SCALE[1], Const.Baby.SCALE[2]);

        gl.glRotatef(babyAngle[0], 1, 0, 0);
        gl.glRotatef(babyAngle[1], 0, 1, 0);
        gl.glRotatef(babyAngle[2], 0, 0, 1);

        gl.glTranslatef(babyPosition[0], babyPosition[1], babyPosition[2]);
    }

    private void computeRationalBezierSphere() {
        if (computeBezier) {
            sphereSurfaceUp = new RationalBezierSurface(controlPointsSphereUP4D, 3, Const.Sphere.NBR_SAMPLE_POINTS, Const.Sphere.NBR_SAMPLE_POINTS);
            sphereSurfaceDown = new RationalBezierSurface(controlPointsSphereDOWN4D, 3, Const.Sphere.NBR_SAMPLE_POINTS, Const.Sphere.NBR_SAMPLE_POINTS);
            computeBezier = false;
        }
    }

    private void renderSphere(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        computeRationalBezierSphere();

        float[][] sphereVertices = sphereSurfaceUp.getVertices();
        float[][] sphereTangents = sphereSurfaceUp.getTangents();
        float[][] sphereTextureCoord = sphereSurfaceUp.getTextureCoord();
        int[] sphereElements = sphereSurfaceUp.getElements();

        gl.glPushMatrix();
        gl.glRotatef(90, 0, 1, 0);
        gl.glBegin(GL.GL_TRIANGLES);
        for (int i = 0; i < sphereElements.length - 2; i += 3) {
            gl.glTexCoord2f(sphereTextureCoord[sphereElements[i]][0], sphereTextureCoord[sphereElements[i]][1]);
            gl.glVertexAttrib3f(Const.Attrib.ATTR_TANGENT_LOCATION, sphereTangents[sphereElements[i]][0], sphereTangents[sphereElements[i]][1], sphereTangents[sphereElements[i]][2]);
            gl.glVertex3f(sphereVertices[sphereElements[i]][0], sphereVertices[sphereElements[i]][1], sphereVertices[sphereElements[i]][2]);

            gl.glTexCoord2f(sphereTextureCoord[sphereElements[i + 1]][0], sphereTextureCoord[sphereElements[i + 1]][1]);
            gl.glVertexAttrib3f(Const.Attrib.ATTR_TANGENT_LOCATION, sphereTangents[sphereElements[i + 1]][0], sphereTangents[sphereElements[i + 1]][1], sphereTangents[sphereElements[i + 1]][2]);
            gl.glVertex3f(sphereVertices[sphereElements[i + 1]][0], sphereVertices[sphereElements[i + 1]][1], sphereVertices[sphereElements[i + 1]][2]);

            gl.glTexCoord2f(sphereTextureCoord[sphereElements[i + 2]][0], sphereTextureCoord[sphereElements[i + 2]][1]);
            gl.glVertexAttrib3f(Const.Attrib.ATTR_TANGENT_LOCATION, sphereTangents[sphereElements[i + 2]][0], sphereTangents[sphereElements[i + 2]][1], sphereTangents[sphereElements[i + 2]][2]);
            gl.glVertex3f(sphereVertices[sphereElements[i + 2]][0], sphereVertices[sphereElements[i + 2]][1], sphereVertices[sphereElements[i + 2]][2]);
        }
        gl.glEnd();
        gl.glPopMatrix();

        sphereVertices = sphereSurfaceDown.getVertices();
        sphereTextureCoord = sphereSurfaceDown.getTextureCoord();
        sphereElements = sphereSurfaceDown.getElements();

        gl.glPushMatrix();
        gl.glRotatef(90, 0, 1, 0);
        gl.glBegin(GL.GL_TRIANGLES);
        for (int i = 0; i < sphereElements.length - 2; i += 3) {
            gl.glTexCoord2f(sphereTextureCoord[sphereElements[i]][0], sphereTextureCoord[sphereElements[i]][1]);
            gl.glVertex3f(sphereVertices[sphereElements[i]][0], sphereVertices[sphereElements[i]][1], sphereVertices[sphereElements[i]][2]);

            gl.glTexCoord2f(sphereTextureCoord[sphereElements[i + 1]][0], sphereTextureCoord[sphereElements[i + 1]][1]);
            gl.glVertex3f(sphereVertices[sphereElements[i + 1]][0], sphereVertices[sphereElements[i + 1]][1], sphereVertices[sphereElements[i + 1]][2]);

            gl.glTexCoord2f(sphereTextureCoord[sphereElements[i + 2]][0], sphereTextureCoord[sphereElements[i + 2]][1]);
            gl.glVertex3f(sphereVertices[sphereElements[i + 2]][0], sphereVertices[sphereElements[i + 2]][1], sphereVertices[sphereElements[i + 2]][2]);
        }
        gl.glEnd();
        gl.glPopMatrix();
    }

    private void manageAnimation() {
        if (!animation) {
            return;
        }
        switch (animationStep) {
            case MOVE_VIEW:
                translate(eyePosition, Const.Anim.EYE_END_POSITION, Const.Anim.EYE_MAX_STEP);
                break;
            case OPEN_UTERUS:
                openUterus(Const.Anim.UTERUS_MAX_OPENING, Const.Anim.UTERUS_STEP_OPENING);
                break;
            case ROTATE_BABY_OUT:
                rotate(babyAngle, Const.Anim.BABY_OUT_ANGLE, Const.Anim.BABY_ANGLE_MAX_STEP);
                break;
            case TRANSLATE_BABY:
                translate(babyPosition, Const.Anim.BABY_END_POSITION, Const.Anim.BABY_MAX_STEP);
                break;
            case END:
                animation = false;
                break;
        }
    }

    private void openUterus(float maxOpening, float stepOpening) {
        if (controlPointsSphereUP4D[1][3][1] < (maxOpening * Const.Sphere.R)) {
            controlPointsSphereUP4D[1][3][1] += (stepOpening * Const.Sphere.R);
            controlPointsSphereUP4D[2][3][1] += (stepOpening * Const.Sphere.R);
            controlPointsSphereDOWN4D[1][3][1] -= (stepOpening * Const.Sphere.R);
            controlPointsSphereDOWN4D[2][3][1] -= (stepOpening * Const.Sphere.R);
            computeBezier = true;
        } else {
            animationStep = animationStep.next();
        }
    }

    private void rotate(float[] currentAngle, float[] newAngle, float[] maxStep) {
        boolean[] finished = {false, false, false};
        for (int i = 0; i < 3; i++) {
            float diff = MathUtil.diffAngle(newAngle[i], currentAngle[i]);
            if (diff != 0) {
                if (diff > maxStep[i]) {
                    diff = maxStep[i];
                }
                if (diff < -maxStep[i]) {
                    diff = -maxStep[i];
                }
                currentAngle[i] += diff;
            } else {
                finished[i] = true;
            }
        }
        if (finished[0] && finished[1] && finished[2]) {
            animationStep = animationStep.next();
        }
    }

    private void translate(float[] currentPosition, float[] newPosition, float[] maxStep) {
        boolean[] finished = {false, false, false};
        for (int i = 0; i < 3; i++) {
            if (currentPosition[i] != newPosition[i]) {
                float diff = newPosition[i] - currentPosition[i];
                if (diff > maxStep[i]) {
                    diff = maxStep[i];
                }
                if (diff < -maxStep[i]) {
                    diff = -maxStep[i];
                }
                currentPosition[i] += diff;
            } else {
                finished[i] = true;
            }
        }
        if (finished[0] && finished[1] && finished[2]) {
            animationStep = animationStep.next();
        }
    }

    private void initInputMap() {
        // Animation
        // Aa
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.LOWER_A.key()), ActionEnum.START_ANIMATION.action());
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.UPPER_A.key()), ActionEnum.START_ANIMATION.action());
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
        // Animation
        this.getActionMap().put(ActionEnum.START_ANIMATION.action(), new ActionStartAnimation());

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

    private class ActionStartAnimation extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = -354019436736980750L;

		@Override
        public void actionPerformed(ActionEvent e) {
            animation = true;
        }
    }

    private class ActionLessAngleX extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 3294047342248235008L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            babyAngle[0] -= 5;
            if (babyAngle[0] < 0) {
                babyAngle[0] += 360;
            }
        }
    }

    private class ActionPlusAngleX extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = -2499921987077721087L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            babyAngle[0] += 5;
            if (babyAngle[0] > 360) {
                babyAngle[0] -= 360;
            }
        }
    }

    private class ActionLessAngleY extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 8309233735369022564L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            babyAngle[1] -= 5;
            if (babyAngle[1] < 0) {
                babyAngle[1] += 360;
            }
        }
    }

    private class ActionPlusAngleY extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 5098708437418690435L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            babyAngle[1] += 5;
            if (babyAngle[1] > 360) {
                babyAngle[1] -= 360;
            }
        }
    }

    private class ActionLessAngleZ extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 4062862260166480331L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            babyAngle[2] -= 5;
            if (babyAngle[2] < 0) {
                babyAngle[2] += 360;
            }
        }
    }

    private class ActionPlusAngleZ extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 4409692529566539583L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            babyAngle[2] += 5;
            if (babyAngle[2] > 360) {
                babyAngle[2] -= 360;
            }
        }
    }

    private class ActionLessEyeX extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = -711825904656924758L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            eyePosition[0] -= 1;
        }
    }

    private class ActionPlusEyeX extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = -305703779048755283L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            eyePosition[0] += 1;
        }
    }

    private class ActionLessEyeY extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 555938576940049691L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            eyePosition[1] -= 1;
        }
    }

    private class ActionPlusEyeY extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = -4309837865083032875L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            eyePosition[1] += 1;
        }
    }

    private class ActionLessEyeZ extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 5765332598910126800L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            eyePosition[2] -= 1;
        }
    }

    private class ActionPlusEyeZ extends AbstractAction {

        /**
		 * 
		 */
		private static final long serialVersionUID = 4663064995435527376L;

		@Override
        public void actionPerformed(ActionEvent e) {
            if(animation) return;
            eyePosition[2] += 1;
        }
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(final MouseEvent e) {
            if(animation) return;
            if (SwingUtilities.isLeftMouseButton(e)) {
                mouseFirstPressed = true;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseSecondPressed = true;
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if(animation) return;
            if (SwingUtilities.isLeftMouseButton(e)) {
                mouseFirstPressed = false;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseSecondPressed = false;
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if(animation) return;
            prevMouseX = e.getX();
            prevMouseY = e.getY();
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            if(animation) return;
            float x = e.getX();
            float y = e.getY();
            if (mouseFirstPressed) {
                eyePosition[0] += (prevMouseX - x) / 10;
                eyePosition[1] += (y - prevMouseY) / 10;
            } else if (mouseSecondPressed) {
                float thetaY = 360 * (x - prevMouseX) / getWidth();
                float thetaX = 360 * (y - prevMouseY) / getHeight();
                babyAngle[0] += thetaX;
                babyAngle[1] += thetaY;
            }
            prevMouseX = x;
            prevMouseY = y;
        }

        @Override
        public void mouseWheelMoved(final MouseWheelEvent e) {
            if(animation) return;
            eyePosition[2] += e.getWheelRotation() * 5;
        }
    }
}
