package ac.ulb.baby;

import ac.ulb.enums.*;
import ac.ulb.bezier.RationalBezierSurface;
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
import java.time.Duration;
import java.time.Instant;

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
	private static final String BABY_SHADERS_PATH = "/baby";
	private static final String TEST_SHADERS_PATH = "/test";

//    private static final String VERTEX_COORD = "vertexCoord";
    private static final String LIGHT_POSITION = "lightPosition";
    private static final String UTERUS_TEXTURE = "uterusTexture";
    private static final String UTERUS_BUMP_MAP = "uterusBumpMap";

    // Sphere parameters
    private static final int R = 8;

	// Outils & Containers
	private GLU glu;
	private Texture uterusTexture;
	private Texture uterusBump;
	private OBJModel modelBaby;

    // Sphere
//    private int sphereAttributeVertexCoordLocation;
    private int sphereLightPositionLocation;
    private int sphereUterusTextureLocation;
    private int sphereUterusBumpMapLocation;
    private ShaderControl sphereShaderControl;

	// Baby
	private int babyLightPositionLocation;
	private int babyUterusTextureLocation;
	private ShaderControl babyShaderControl;

	private float babyShift = (float) 4.5;

	private float[] babyAngle = new float[] {0,0,0};

	private float[] babyPosition = new float[] {0,-babyShift,0};

	private float scaleX = (float) 1.1;
	private float scaleY = (float) 1.1;
	private float scaleZ = (float) 1.1;

	private float[] eyePosition = new float[] { 0, 0, R };

	private static final int NUM_LIGHTS = 6;
	private static final int R2 = 3 * R / 4;
	private static final float[] POS_X = { 1, (float) -0.5 };
	private static final float[] POS_Y = { (float) Math.sin(Math.PI / 4), (float) -Math.sin(Math.PI / 4) };
	private static final float[] POS_Z = { 0, (float) Math.sin(2 * Math.PI / 3), (float) -Math.sin(2 * Math.PI / 3) };

    private float[] lightPosition = {
            R2 * POS_X[0], R2 * POS_Y[0], R2 * POS_Z[0],
            R2 * POS_X[0], R2 * POS_Y[1], R2 * POS_Z[0],
            R2 * POS_X[1], R2 * POS_Y[0], R2 * POS_Z[1],
            R2 * POS_X[1], R2 * POS_Y[1], R2 * POS_Z[1],
            R2 * POS_X[1], R2 * POS_Y[0], R2 * POS_Z[2],
            R2 * POS_X[1], R2 * POS_Y[1], R2 * POS_Z[2]
    };

    // Bezier
    private static final float[][][] CP_SPHERE_UP_4D = {
            {{ 0, 0, 1, 9},   { 0, 0, 1, 3},   { 0, 0, 1, 3},   { 0, 0, 1, 9}},
            {{ 2, 0, 1, 3},   { 2, 4, 1, 1},   {-2, 4, 1, 1},   {-2, 0, 1, 3}},
            {{ 2, 0,-1, 3},   { 2, 4,-1, 1},   {-2, 4,-1, 1},   {-2, 0,-1, 3}},
            {{ 0, 0,-1, 9},   { 0, 0,-1, 3},   { 0, 0,-1, 3},   { 0, 0,-1, 9}}
    };
    private static final float[][][] CP_SPHERE_DOWN_4D = {
            {{ 0, 0, 1, 9},   { 0, 0, 1, 3},   { 0, 0, 1, 3},   { 0, 0, 1, 9}},
            {{ 2, 0, 1, 3},   { 2,-4, 1, 1},   {-2,-4, 1, 1},   {-2, 0, 1, 3}},
            {{ 2, 0,-1, 3},   { 2,-4,-1, 1},   {-2,-4,-1, 1},   {-2, 0,-1, 3}},
            {{ 0, 0,-1, 9},   { 0, 0,-1, 3},   { 0, 0,-1, 3},   { 0, 0,-1, 9}}
    };
    private static final int NBR_SAMPLE_POINTS = 20;

    private float[][][] controlPointsSphereUP4D;
    private float[][][] controlPointsSphereDOWN4D;

    private RationalBezierSurface sphereSurfaceUp;
    private RationalBezierSurface sphereSurfaceDown;

//    // Buffers
//    private static final int NBR_BUFFERS = 4;
//    private int[] buffers = new int[NBR_BUFFERS];
//    private static final int VERTICES_UP = 0;
//    private static final int ELEMENTS_UP = 1;
//    private static final int VERTICES_DOWN = 2;
//    private static final int ELEMENTS_DOWN = 3;

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
        gl.glEnable(GL2.GL_AUTO_NORMAL);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl.glClearColor(0f, 0f, 0f, 1f);
        gl.glClearDepth(1f);

		initPerspective(drawable);

        initBezierControlPoints();
        computeRationalBezierSphere();
//        initBuffers(drawable);
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

		updateEye();

		sphereShaderControl.useShaderProgram(gl);
		gl.glUniform3fv(sphereLightPositionLocation, NUM_LIGHTS, FloatBuffer.wrap(lightPosition));
		gl.glUniform1i(sphereUterusTextureLocation, 0);
		gl.glUniform1i(sphereUterusBumpMapLocation, 1);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		uterusTexture.bind(gl);

		gl.glActiveTexture(GL.GL_TEXTURE1);
		uterusBump.bind(gl);

        renderRationalBezierSphere(drawable);

		babyShaderControl.useShaderProgram(gl);
		gl.glUniform3fv(babyLightPositionLocation, NUM_LIGHTS, FloatBuffer.wrap(lightPosition));
		gl.glUniform1i(babyUterusTextureLocation, 0);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		uterusTexture.bind(gl);

		gl.glPushMatrix();
		updateRotations(drawable);
		updateTranslation(drawable);
		updateScale(drawable);
		renderModel(drawable, modelBaby);
		gl.glPopMatrix();

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

//    private void initBuffers(GLAutoDrawable drawable) {
//        GL2 gl = drawable.getGL().getGL2();
//
//        gl.glGenBuffers(1, buffers, VERTICES_UP);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[VERTICES_UP]);
//        {
//            FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(sphereVerticesArrayUP);
//            int size = sphereVerticesUP.length * GLBuffers.SIZEOF_FLOAT;
//            gl.glBufferData(GL4.GL_ARRAY_BUFFER, size, vertexBuffer, GL4.GL_STATIC_DRAW);
//        }
//
//        gl.glGenBuffers(1, buffers, ELEMENTS_UP);
//        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffers[ELEMENTS_UP]);
//        {
//            IntBuffer elementBuffer = GLBuffers.newDirectIntBuffer(sphereElementsUP);
//            int size = sphereElementsUP.length * GLBuffers.SIZEOF_FLOAT;
//            gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size, elementBuffer, GL4.GL_STATIC_DRAW);
//        }
//
//        gl.glGenBuffers(1, buffers, VERTICES_DOWN);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[VERTICES_DOWN]);
//        {
//            FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(sphereVerticesArrayDOWN);
//            int size = sphereVerticesDOWN.length * GLBuffers.SIZEOF_FLOAT;
//            gl.glBufferData(GL4.GL_ARRAY_BUFFER, size, vertexBuffer, GL4.GL_STATIC_DRAW);
//        }
//
//        gl.glGenBuffers(1, buffers, ELEMENTS_DOWN);
//        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffers[ELEMENTS_DOWN]);
//        {
//            IntBuffer elementBuffer = GLBuffers.newDirectIntBuffer(sphereElementsDOWN);
//            int size = sphereElementsDOWN.length * GLBuffers.SIZEOF_FLOAT;
//            gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size, elementBuffer, GL4.GL_STATIC_DRAW);
//        }
//    }

    private void initBezierControlPoints() {
        controlPointsSphereUP4D = CP_SPHERE_UP_4D;
        for(int i = 0; i < controlPointsSphereUP4D.length; i++) {
            for(int j = 0; j < controlPointsSphereUP4D[i].length; j++) {
                controlPointsSphereUP4D[i][j][0] *= R;
                controlPointsSphereUP4D[i][j][1] *= R;
                controlPointsSphereUP4D[i][j][2] *= R;
            }
        }

        controlPointsSphereDOWN4D = CP_SPHERE_DOWN_4D;
        for(int i = 0; i < controlPointsSphereDOWN4D.length; i++) {
            for(int j = 0; j < controlPointsSphereDOWN4D[i].length; j++) {
                controlPointsSphereDOWN4D[i][j][0] *= R;
                controlPointsSphereDOWN4D[i][j][1] *= R;
                controlPointsSphereDOWN4D[i][j][2] *= R;
            }
        }
    }

	private void initTextures(GLAutoDrawable drawable) {
		uterusTexture = loadTexture(drawable, UTERUS_TEXTURE_PATH);
		uterusBump = loadTexture(drawable, UTERUS_BUMP_PATH);
	}

    private void initModel() {
        InputStream objStream = MainWindow.class.getResourceAsStream(BABY_MODEL_OBJ_PATH);
        IOBJParser objParser = new OBJParser();
        try {
            modelBaby = objParser.parse(objStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initShaders(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        sphereShaderControl = initShaderProgram(drawable, SPHERE_SHADERS_PATH);
//        sphereAttributeVertexCoordLocation = gl.glGetAttribLocation(sphereShaderControl.getShaderProgram(),VERTEX_COORD );
        sphereLightPositionLocation = gl.glGetUniformLocation(sphereShaderControl.getShaderProgram(), LIGHT_POSITION);
        sphereUterusTextureLocation = gl.glGetUniformLocation(sphereShaderControl.getShaderProgram(), UTERUS_TEXTURE);
        sphereUterusBumpMapLocation = gl.glGetUniformLocation(sphereShaderControl.getShaderProgram(), UTERUS_BUMP_MAP);

		babyShaderControl = initShaderProgram(drawable, BABY_SHADERS_PATH);
		babyLightPositionLocation = gl.glGetUniformLocation(babyShaderControl.getShaderProgram(), LIGHT_POSITION);
		babyUterusTextureLocation = gl.glGetUniformLocation(babyShaderControl.getShaderProgram(), UTERUS_TEXTURE);
	}

	private ShaderControl initShaderProgram(GLAutoDrawable drawable, String shadersPath) {
		GL2 gl = drawable.getGL().getGL2();
		String absolutePath = SHADERS_ROOT + "/" + shadersPath;
		String vertexPath = absolutePath + "/" + VERTEX_SHADER + "." + SHADER_EXT;
		String fragmentPath = absolutePath + "/" + FRAGMENT_SHADER + "." + SHADER_EXT;
		ShaderControl shaderControl = new ShaderControl();
		shaderControl.setVertexSrc(ShaderControl.loadShader(vertexPath));
		shaderControl.setFragmentSrc(ShaderControl.loadShader(fragmentPath));
		shaderControl.init(gl);

		return shaderControl;
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

    private void updateEye() {
        glu.gluLookAt(eyePosition[0], eyePosition[1], eyePosition[2], eyePosition[0], eyePosition[1], 0, 0, 1, 0);
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

    private void updateRotations(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glRotatef(babyAngle[0], 1, 0, 0);
        gl.glRotatef(babyAngle[1], 0, 1, 0);
        gl.glRotatef(babyAngle[2], 0, 0, 1);
//        System.out.println("angleX : " + babyAngle[0]);
//        System.out.println("angleY : " + babyAngle[1]);
//        System.out.println("angleZ : " + babyAngle[2]);
    }

	private void updateTranslation(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glTranslatef(babyPosition[0], babyPosition[1], babyPosition[2]);
	}

	private void updateScale(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glScalef(scaleX, scaleY, scaleZ);
	}

	private void computeRationalBezierSphere() {
		sphereSurfaceUp = new RationalBezierSurface(controlPointsSphereUP4D, 3, NBR_SAMPLE_POINTS, NBR_SAMPLE_POINTS);
		sphereSurfaceDown = new RationalBezierSurface(controlPointsSphereDOWN4D, 3, NBR_SAMPLE_POINTS, NBR_SAMPLE_POINTS);
	}

	private void renderRationalBezierSphere(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		float[][] sphereVertices = sphereSurfaceUp.getVertices();
		float[][] sphereTextureCoord = sphereSurfaceUp.getTextureCoord();
		int[] sphereElements = sphereSurfaceUp.getElements();

		gl.glPushMatrix();
		gl.glRotatef(90, 0, 1, 0);
		gl.glBegin(GL.GL_TRIANGLES);
		for(int i = 0; i < sphereElements.length - 2; i+=3) {
			gl.glTexCoord2f(sphereTextureCoord[sphereElements[i]][0], sphereTextureCoord[sphereElements[i]][1]);
			gl.glVertex3f(sphereVertices[sphereElements[i]][0], sphereVertices[sphereElements[i]][1], sphereVertices[sphereElements[i]][2]);

			gl.glTexCoord2f(sphereTextureCoord[sphereElements[i+1]][0], sphereTextureCoord[sphereElements[i+1]][1]);
			gl.glVertex3f(sphereVertices[sphereElements[i+1]][0], sphereVertices[sphereElements[i+1]][1], sphereVertices[sphereElements[i+1]][2]);

			gl.glTexCoord2f(sphereTextureCoord[sphereElements[i+2]][0], sphereTextureCoord[sphereElements[i+2]][1]);
			gl.glVertex3f(sphereVertices[sphereElements[i+2]][0], sphereVertices[sphereElements[i+2]][1], sphereVertices[sphereElements[i+2]][2]);
		}
		gl.glEnd();
		gl.glPopMatrix();

		sphereVertices = sphereSurfaceDown.getVertices();
		sphereTextureCoord = sphereSurfaceDown.getTextureCoord();
		sphereElements = sphereSurfaceDown.getElements();

		gl.glPushMatrix();
		gl.glRotatef(90, 0, 1, 0);
		gl.glBegin(GL.GL_TRIANGLES);
		for(int i = 0; i < sphereElements.length - 2; i+=3) {
			gl.glTexCoord2f(sphereTextureCoord[sphereElements[i]][0], sphereTextureCoord[sphereElements[i]][1]);
			gl.glVertex3f(sphereVertices[sphereElements[i]][0], sphereVertices[sphereElements[i]][1], sphereVertices[sphereElements[i]][2]);

			gl.glTexCoord2f(sphereTextureCoord[sphereElements[i+1]][0], sphereTextureCoord[sphereElements[i+1]][1]);
			gl.glVertex3f(sphereVertices[sphereElements[i+1]][0], sphereVertices[sphereElements[i+1]][1], sphereVertices[sphereElements[i+1]][2]);

			gl.glTexCoord2f(sphereTextureCoord[sphereElements[i+2]][0], sphereTextureCoord[sphereElements[i+2]][1]);
			gl.glVertex3f(sphereVertices[sphereElements[i+2]][0], sphereVertices[sphereElements[i+2]][1], sphereVertices[sphereElements[i+2]][2]);
		}
		gl.glEnd();
		gl.glPopMatrix();

//        gl.glEnableVertexAttribArray(sphereAttributeVertexCoordLocation);
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[VERTICES_UP]);
//        gl.glVertexAttribPointer(
//                sphereAttributeVertexCoordLocation,
//                3,
//                GL.GL_FLOAT,
//                false,
//                0,
//                null
//        );
//        gl.glDrawElements(GL.GL_TRIANGLE_STRIP, sphereElementsUP.length, GL.GL_UNSIGNED_INT, 0);
	}

	private void initInputMap() {
		// Reset
		// space bar
		this.getInputMap().put(KeyStroke.getKeyStroke(KeyEnum.SPACE_BAR.key()), ActionEnum.RESET.action());

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
		// Reset
		this.getActionMap().put(ActionEnum.RESET.action(), new ActionReset());

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

	private class ActionReset extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle = new float[] {0,0,0};

			babyPosition = new float[] {0,-babyShift,0};

			eyePosition = new float[] {0,0,R};
		}
	}

	private class ActionStartAnimation extends AbstractAction {

		private final int STEP_TIME = 10000000;
		private final float[] END_EYE_POSITION = {0,0,R*4};
		private final float[] OUT_ANGLE = {270,0,180};
		private final float[] END_ANGLE = {0,0,0};
		private final float[] END_POSITION = {0, R, 0};
        private final float MAX_OPENING = 0.75f;
        private final float STEP_OPENING = 0.01f;

		private void openUterus(float maxOpening, float stepOpening, int stepTime) {
            Instant first;
            Instant second;
            Duration duration;
            while(controlPointsSphereUP4D[1][3][1] < (maxOpening * R)) {
                first = Instant.now();
                second = Instant.now();
                duration = Duration.between(first, second);
                while (duration.getNano() < stepTime) {
                    second = Instant.now();
                    duration = Duration.between(first, second);
                }
                controlPointsSphereUP4D[1][3][1] += (stepOpening * R);
                controlPointsSphereUP4D[2][3][1] += (stepOpening * R);
                controlPointsSphereDOWN4D[1][3][1] -= (stepOpening * R);
                controlPointsSphereDOWN4D[2][3][1] -= (stepOpening * R);
                computeRationalBezierSphere();
                display();
            }

		}

		private void rotateBaby(float[] newAngle, int stepTime){
			Instant first;
			Instant second;
			Duration duration;
			for (int i =0; i<3; i++ ) {
                while (babyAngle[i] != newAngle[i]) {
                    first = Instant.now();
                    second = Instant.now();
                    duration = Duration.between(first, second);
                    while (duration.getNano() < stepTime) {
                        second = Instant.now();
                        duration = Duration.between(first, second);
                    }
                    float inf = newAngle[i] - 180;
                    float sup = newAngle[i];
                    if (inf < 0) {
                        sup = inf + 360;
                        inf = newAngle[i];
                    }

                    if (inf < babyAngle[i] & babyAngle[i] < sup) {
                        babyAngle[i] += 1;
                        if (babyAngle[i] >= 360) {
                            babyAngle[i] -= 360;
                        }
                    } else {
                        babyAngle[i] -= 1;
                        if (babyAngle[i] < 0) {
                            babyAngle[i] += 360;
                        }
                    }
                    display();
                }
			}
		}

		private void translateBaby(float[] newPosition, int stepTime){
			Instant first;
			Instant second;
			Duration duration;
			for (int i =0; i <3; i++){
                while (babyPosition[i] != newPosition[i]) {
                    first = Instant.now();
                    second = Instant.now();
                    duration = Duration.between(first, second);
                    while (duration.getNano() < stepTime) {
                        second = Instant.now();
                        duration = Duration.between(first, second);
                    }
                    babyPosition[i] += 0.125;
                    display();
                }
			}
		}

		private void moveView(float[] newEyePosition, int stepTime){
			Instant first;
			Instant second;
			Duration duration;
			for (int i=0; i<3; i++){
				while (eyePosition[i]!= newEyePosition[i]) {
					first = Instant.now();
					second = Instant.now();
					duration = Duration.between(first, second);
					while (duration.getNano() < stepTime) {
						second = Instant.now();
						duration = Duration.between(first, second);
					}
					eyePosition[i] += 0.125;
					display();
				}
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {


			//move back view
			moveView(END_EYE_POSITION, STEP_TIME);

            //cut in the uterus
            openUterus(MAX_OPENING, STEP_OPENING, STEP_TIME);

			//head of baby in direction of sphere opening
			rotateBaby(OUT_ANGLE, STEP_TIME);

			//put off the baby
			translateBaby(END_POSITION, STEP_TIME);

			//baby in initial position
			//rotateBaby(END_ANGLE, STEP_TIME); // je dois v�rifier les conditions sur le changement des angles
		}
	}

	private class ActionLessAngleX extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle[0] -= 5;
			if (babyAngle[0] < 0) {
				babyAngle[0] += 360;
			}
		}
	}

	private class ActionPlusAngleX extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle[0] += 5;
			if (babyAngle[0] > 360) {
				babyAngle[0] -= 360;
			}
		}
	}

	private class ActionLessAngleY extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle[1] -= 5;
			if (babyAngle[1] < 0) {
				babyAngle[1] += 360;
			}
		}
	}

	private class ActionPlusAngleY extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle[1] += 5;
			if (babyAngle[1] > 360) {
				babyAngle[1] -= 360;
			}
		}
	}

	private class ActionLessAngleZ extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle[2] -= 5;
			if (babyAngle[2] < 0) {
				babyAngle[2] += 360;
			}
		}
	}

	private class ActionPlusAngleZ extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			babyAngle[2] += 5;
			if (babyAngle[2] > 360) {
				babyAngle[2] -= 360;
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
			eyePosition[2] -= 1;
		}
	}

	private class ActionPlusEyeZ extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			eyePosition[2] += 1;
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
				babyAngle[0] += thetaX;
				babyAngle[1] += thetaY;
			}
			prevMouseX = x;
			prevMouseY = y;
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			eyePosition[2] += e.getWheelRotation() * 5;
		}
	}
}
