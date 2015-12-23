package ac.ulb.baby;

import ac.ulb.enums.*;
import ac.ulb.utils.ShaderControl;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
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

	private static final String LIGHT_POSITION = "lightPosition";
	private static final String UTERUS_TEXTURE = "uterusTexture";
	private static final String UTERUS_BUMP_MAP = "uterusBumpMap";

	// Sphere parameters
	private static final int R = 8;
	private static final int SLICES = 32;
	private static final int STACKS = 32;

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

	private float babyShift = (float) 4.5;

	private float angleX = 320;
	private float angleY = 0;
	private float angleZ = 90; // TODO

	private float positionX = 0;
	private float positionY = -babyShift;
	private float positionZ = 0;

	private float scaleX = (float) 1.1;
	private float scaleY = (float) 1.1;
	private float scaleZ = (float) 1.1;

	private float[] eyePosition = new float[] { 0, 0, R };

	private static final int NUM_LIGHTS = 6;
	private static final int R2 = 3 * R / 4;
	private static final float[] POS_X = { 1, (float) -0.5 };
	private static final float[] POS_Y = { (float) Math.sin(Math.PI / 4), (float) -Math.sin(Math.PI / 4) };
	private static final float[] POS_Z = { 0, (float) Math.sin(2 * Math.PI / 3), (float) -Math.sin(2 * Math.PI / 3) };

	private float[] lightPosition = { R2 * POS_X[0], R2 * POS_Y[0], R2 * POS_Z[0], R2 * POS_X[0], R2 * POS_Y[1],
			R2 * POS_Z[0], R2 * POS_X[1], R2 * POS_Y[0], R2 * POS_Z[1], R2 * POS_X[1], R2 * POS_Y[1], R2 * POS_Z[1],
			R2 * POS_X[1], R2 * POS_Y[0], R2 * POS_Z[2], R2 * POS_X[1], R2 * POS_Y[1], R2 * POS_Z[2] };

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

		drawSphere(drawable, R, SLICES, STACKS);

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

		sphereShaderControl = initShaderProgram(drawable, SPHERE_SHADERS_PATH);
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

	private void updateRotations(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glRotatef(angleX, 1, 0, 0);
		gl.glRotatef(angleY, 0, 1, 0);
		gl.glRotatef(angleZ, 0, 0, 1);
	}

	private void updateTranslation(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glTranslatef(positionX, positionY, positionZ);
	}

	private void updateScale(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glScalef(scaleX, scaleY, scaleZ);
	}

	private void drawSphere(GLAutoDrawable drawable, int radius, int slices, int stacks) {
		GL2 gl = drawable.getGL().getGL2();

		gl.glPushMatrix();

		GLUquadric sphere = glu.gluNewQuadric();
		glu.gluQuadricTexture(sphere, true);
		glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
		glu.gluQuadricNormals(sphere, GLU.GLU_FLAT);
		glu.gluQuadricOrientation(sphere, GLU.GLU_OUTSIDE);
		glu.gluSphere(sphere, radius, slices, stacks);
		glu.gluDeleteQuadric(sphere);

		gl.glPopMatrix();
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
			angleX = 320;
			angleY = 0;
			angleZ = 90;

			positionX = 0;
			positionY = -babyShift;
			positionZ = 0;
			
			eyePosition[2] = R;
		}
	}

	private class ActionStartAnimation extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			int STEP_TIME = 10000000;
			float END_BABY_POSITION = R;
			int END_EYE_POSITION = R*4;

			Instant first = Instant.now();
			Instant second = Instant.now();
			Duration duration = Duration.between(first, second);
			
			//move back view
//			while (eyePosition[2] != END_EYE_POSITION) {
//				first = Instant.now();
//				second = Instant.now();
//				duration = Duration.between(first, second);
//				while (duration.getNano() < STEP_TIME) {
//					second = Instant.now();
//					duration = Duration.between(first, second);
//				}
//				eyePosition[2] += 0.125;
//				display();
//			}
			
			//rotate the baby
			while (angleX != 0) {
				first = Instant.now();
				second = Instant.now();
				duration = Duration.between(first, second);
				while (duration.getNano() < STEP_TIME) {
					second = Instant.now();
					duration = Duration.between(first, second);
				}
				if (angleX < 180) {
					angleX -= 1;
					if (angleX < 0) {
						angleX += 360;
					}
				} else {
					angleX += 1;
					if (angleX >= 360) {
						angleX -= 360;
					}
				}
				display();
			}

			while (angleY != 0) {
				first = Instant.now();
				second = Instant.now();
				duration = Duration.between(first, second);
				while (duration.getNano() < STEP_TIME) {
					second = Instant.now();
					duration = Duration.between(first, second);
				}
				if (angleY < 180) {
					angleY -= 1;
					if (angleY < 0) {
						angleY += 360;
					}
				} else {
					angleY += 1;
					if (angleY >= 360) {
						angleY -= 360;
					}
				}
				display();
			}

			while (angleZ != 0) {
				first = Instant.now();
				second = Instant.now();
				duration = Duration.between(first, second);
				while (duration.getNano() < STEP_TIME) {
					second = Instant.now();
					duration = Duration.between(first, second);
				}
				if (angleZ < 180) {
					angleZ -= 1;
					if (angleZ < 0) {
						angleZ += 360;
					}
				} else {
					angleZ += 1;
					if (angleZ >= 360) {
						angleZ -= 360;
					}
				}
				display();
			}
			
			//put off the baby
			while (positionY != END_BABY_POSITION) {
				first = Instant.now();
				second = Instant.now();
				duration = Duration.between(first, second);
				while (duration.getNano() < STEP_TIME) {
					second = Instant.now();
					duration = Duration.between(first, second);
				}
				positionY += 0.125;
				display();
			}
		}
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
}
