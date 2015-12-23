package ac.ulb.bezier;

public abstract class Surface {

    protected float[][][] controlPoints;
    protected int order;
    protected int resolutionU;
    protected int resolutionV;

    protected float[][] vertices;
    protected float[][] textureCoord;
    protected int[] elements;

    public Surface(float[][][] controlPoints, int order, int resolutionU, int resolutionV) {
        this.controlPoints = controlPoints;
        this.order = order;
        if (resolutionU < 1) {
            resolutionU = 1;
        }
        if (resolutionV < 1) {
            resolutionV = 1;
        }
        this.resolutionU = resolutionU;
        this.resolutionV = resolutionV;

        this.vertices = new float[resolutionU * resolutionV][3];
        this.textureCoord = new float[resolutionU * resolutionV][2];
        this.elements = new int[(resolutionU - 1) * (resolutionV - 1) * 2 * 3];

        this.computeSurface();
    }

    public void computeSurface() {
        for (int ru = 0; ru < resolutionU; ru++) {
            float u = (float) ru / (resolutionU - 1);
            for (int rv = 0; rv < resolutionV; rv++) {
                float v = (float) rv / (resolutionV - 1);
                vertices[(ru * resolutionV) + rv] = computePosition(controlPoints, u, v);
                textureCoord[(ru * resolutionV) + rv] = new float[] {u, v};
            }
        }
        int k = 0;
        for (int ru = 0; ru < resolutionU - 1; ru++) {
            for (int rv = 0; rv < resolutionV - 1; rv++) {
                // 1 square ABCD = 2 triangles ABC + CDA
                // ABC
                elements[k] = ru * resolutionV + rv;
                k++;
                elements[k] = ru * resolutionV + (rv + 1);
                k++;
                elements[k] = (ru + 1) * resolutionV + (rv + 1);
                k++;
                // CDA
                elements[k] = (ru + 1) * resolutionV + (rv + 1);
                k++;
                elements[k] = (ru + 1) * resolutionV + rv;
                k++;
                elements[k] = ru * resolutionV + rv;
                k++;
            }
        }
    }

    protected abstract float[] computePosition(float[][][] controlPoints, float u, float v);

    public float[][] getVertices() {
        return vertices;
    }

    public float[][] getTextureCoord() {
        return textureCoord;
    }

    public int[] getElements() {
        return elements;
    }

    public float[] getVerticesAsArray() {
        float[] array = new float[vertices.length * 3];
        int k = 0;
        for (int i = 0; i < vertices.length; i++) {
            array[k] = vertices[i][0];
            k++;
            array[k] = vertices[i][1];
            k++;
            array[k] = vertices[i][2];
        }
        return array;
    }
}
