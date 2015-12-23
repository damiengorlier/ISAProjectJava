package ac.ulb.bezier;

public class MathUtil {

    public static int factorial(int n) {
        assert(n >= 0);
        int result = 1;
        for (int i = n; i > 1; i--)
            result *= i;
        return result;
    }

    public static float binomialCoefficient(int i, int n) {
        assert(i >= 0);
        assert(n >= 0);
        return 1.0f * factorial(n) / (factorial(i) * factorial(n-i));
    }

    public static float bernsteinPolynomial(int i, int n, float u) {
        return (float) (binomialCoefficient(i, n) * Math.pow(u, i) * Math.pow(1-u, n-i));
    }
}
