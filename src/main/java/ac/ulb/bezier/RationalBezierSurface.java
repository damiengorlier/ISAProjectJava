package ac.ulb.bezier;

import ac.ulb.utils.MathUtil;

public class RationalBezierSurface extends Surface {

    public RationalBezierSurface(float[][][] controlPoints, int order, int resolutionU, int resolutionV) {
        super(controlPoints, order, resolutionU, resolutionV);
    }

    @Override
    protected float[] computePosition(float[][][] controlPoints, float u, float v) {
        float[] result = {0, 0, 0};
        float weightCoeff = 0;
        for (int i = 0; i <= order; i++) {
            float polyI = MathUtil.bernsteinPolynomial(i, order, u);
            for (int j = 0; j <= order; j++) {
                float polyJ = MathUtil.bernsteinPolynomial(j, order, v);
                result[0] += polyI * polyJ * controlPoints[i][j][0] * controlPoints[i][j][3];
                result[1] += polyI * polyJ * controlPoints[i][j][1] * controlPoints[i][j][3];
                result[2] += polyI * polyJ * controlPoints[i][j][2] * controlPoints[i][j][3];
                weightCoeff += polyI * polyJ * controlPoints[i][j][3];
            }
        }
        for (int k = 0; k < 3; k++) {
            result[k] = result[k] / weightCoeff;
        }
        return result;
    }
}
