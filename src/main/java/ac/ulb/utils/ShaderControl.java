package ac.ulb.utils;

import com.jogamp.opengl.GL2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ShaderControl {

    private int vertexShaderProgram;
    private int fragmentShaderProgram;
    private int shaderProgram;
    private String[] vsrc;
    private String[] fsrc;

    public void init(GL2 gl) {
        try {
            attachShaders(gl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] loadShader(String name) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = ShaderControl.class.getResourceAsStream(name);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[]
                {sb.toString()};
    }

    private void attachShaders(GL2 gl) throws Exception {
        vertexShaderProgram = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        fragmentShaderProgram = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        gl.glShaderSource(vertexShaderProgram, 1, vsrc, null, 0);
        gl.glCompileShader(vertexShaderProgram);
        gl.glShaderSource(fragmentShaderProgram, 1, fsrc, null, 0);
        gl.glCompileShader(fragmentShaderProgram);
        shaderProgram = gl.glCreateProgram();

        gl.glAttachShader(shaderProgram, vertexShaderProgram);
        gl.glAttachShader(shaderProgram, fragmentShaderProgram);
        gl.glLinkProgram(shaderProgram);
        gl.glValidateProgram(shaderProgram);
        IntBuffer intBuffer = IntBuffer.allocate(1);
        gl.glGetProgramiv(shaderProgram, GL2.GL_LINK_STATUS, intBuffer);

        if (intBuffer.get(0) != 1) {
            gl.glGetProgramiv(shaderProgram, GL2.GL_INFO_LOG_LENGTH, intBuffer);
            int size = intBuffer.get(0);
            System.err.println("Program link error: ");
            if (size > 0) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                gl.glGetProgramInfoLog(shaderProgram, size, intBuffer, byteBuffer);
                for (byte b : byteBuffer.array()) {
                    System.err.print((char) b);
                }
            } else {
                System.out.println("Unknown");
            }
            System.exit(1);
        }
    }

    public int useShaderProgram(GL2 gl) {
        gl.glUseProgram(shaderProgram);
        return shaderProgram;
    }

    public void stopUsingShaderProgram(GL2 gl) {
        gl.glUseProgram(0);
    }

    public void cleanShaderProgram(GL2 gl) {
        gl.glDetachShader(shaderProgram, vertexShaderProgram);
        gl.glDetachShader(shaderProgram, fragmentShaderProgram);
        gl.glDeleteShader(vertexShaderProgram);
        gl.glDeleteShader(fragmentShaderProgram);
        gl.glDeleteProgram(shaderProgram);
    }

    public void setVertexSrc(String[] vsrc) {
        this.vsrc = vsrc;
    }

    public void setFragmentSrc(String[] fsrc) {
        this.fsrc = fsrc;
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    public int getVertexShaderProgram() {
        return shaderProgram;
    }

    public int getFragmentShaderProgram() {
        return vertexShaderProgram;
    }
}