package ac.ulb.bezier;

public abstract class Surface {

    protected float[][][] controlPoints;
    protected int order;
    protected int resolutionU;
    protected int resolutionV;

    protected float[][] vertices;
    protected float[][] tangents;
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
        this.tangents = new float[resolutionU * resolutionV][3];
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
                textureCoord[(ru * resolutionV) + rv] = new float[]{u, v};
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
        computeTangents();
    }

    private void computeTangents() {
        initTangents();
        for (int i = 0; i < elements.length - 2; i += 3) {
            float[] v0 = vertices[elements[i]];
            float[] v1 = vertices[elements[i + 1]];
            float[] v2 = vertices[elements[i + 2]];

            float[] uv0 = textureCoord[elements[i]];
            float[] uv1 = textureCoord[elements[i + 1]];
            float[] uv2 = textureCoord[elements[i + 2]];

            float[] deltaVer1 = {v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
            float[] deltaVer2 = {v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};

            float[] deltaUV1 = {uv1[0] - uv0[0], uv1[1] - uv0[1]};
            float[] deltaUV2 = {uv2[0] - uv0[0], uv2[1] - uv0[1]};

            float r = 1.0f / (deltaUV1[0] * deltaUV2[1] - deltaUV1[1] * deltaUV2[0]);

            for (int j = 0; j < 3; j++) {
                float prod1 = deltaVer1[j] * deltaUV2[1];
                float prod2 = deltaVer2[j] * deltaUV1[1];
                float t = (prod1 - prod2) * r;
                tangents[elements[i]][j] += t;
                tangents[elements[i + 1]][j] += t;
                tangents[elements[i + 2]][j] += t;
            }

        }
    }

    private void initTangents() {
        for (int i = 0; i < tangents.length; i++) {
            for (int j = 0; j < tangents[i].length; j++) {
                tangents[i][j] = 0;
            }
        }
    }

    protected abstract float[] computePosition(float[][][] controlPoints, float u, float v);

    public float[][] getVertices() {
        return vertices;
    }

    public float[][] getTangents() {
        return tangents;
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
        for (float[] vertex : vertices) {
            array[k] = vertex[0];
            k++;
            array[k] = vertex[1];
            k++;
            array[k] = vertex[2];
        }
        return array;
    }
}
