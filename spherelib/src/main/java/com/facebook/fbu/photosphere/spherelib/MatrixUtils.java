// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.spherelib;



public class MatrixUtils {

    public static float dot(float[] x, float[] y) {
        if (x.length != y.length)  {
            throw new RuntimeException("Illegal dimensions.");
        }
        float sum = 0.0f;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }
        return sum;
    }

    public static float[] cross(float[] x, float[] y) {
        if (x.length != 3 || y.length != 3) {
            throw new RuntimeException("Illegal dimensions.");
        }
        float[] z = new float[3];
        for (int i = 0; i < x.length; i++) {
            z[i] += x[(i + 1) % 3] * y[(i + 2) % 3] - x[(i + 2) % 3] * y[(i + 1) % 3];
        }
        return z;
    }

    public static float normSquared(float[] x) {
        return dot(x, x);
    }

    public static float norm(float[] x) {
        return (float) Math.sqrt(normSquared(x));
    }

    public static float[][] add(float[][] A, float[][] B) {
        int m = A.length;
        int n = A[0].length;
        float[][] C = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    public static float[] add(float[] x, float[] y) {
        int m = x.length;
        float[] z = new float[m];
        for (int i = 0; i < m; i++) {
            z[i] = x[i] + y[i];
        }
        return z;
    }

    public static float[] subtract(float[] x, float[] y) {
        int m = x.length;
        float[] z = new float[m];
        for (int i = 0; i < m; i++) {
            z[i] = x[i] - y[i];
        }
        return z;
    }

    public static float[][] subtract(float[][] A, float[][] B) {
        int m = A.length;
        int n = A[0].length;
        float[][] C = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    public static float[][] transpose(float[][] A) {
        int m = A.length;
        int n = A[0].length;
        float[][] C = new float[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[j][i] = A[i][j];
            }
        }
        return C;
    }

    public static float[][] multiply(float[][] A, float[][] B) {
        int mA = A.length;
        int nA = A[0].length;
        int mB = B.length;
        int nB = B[0].length;
        if (nA != mB) {
            throw new RuntimeException("Illegal dimensions.");
        }
        float[][] C = new float[mA][nB];
        for (int i = 0; i < mA; i++) {
            for (int j = 0; j < nB; j++) {
                for (int k = 0; k < nA; k++) {
                    C[i][j] += (A[i][k] * B[k][j]);
                }
            }
        }
        return C;
    }

    public static float[] multiply(float[][] A, float[] x) {
        int m = A.length;
        int n = A[0].length;
        if (x.length != n) {
            throw new RuntimeException("Illegal dimensions.");
        }
        float[] y = new float[m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                y[i] += (A[i][j] * x[j]);
            }
        }
        return y;
    }

    public static float[] multiply(float[] x, float[][] A) {
        int m = A.length;
        int n = A[0].length;
        if (x.length != m) {
            throw new RuntimeException("Illegal dimensions.");
        }
        float[] y = new float[n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                y[j] += (A[i][j] * x[i]);
            }
        }
        return y;
    }

    // scalar multiplication
    public static float[] multiply(float[] x, float a) {
        int m = x.length;
        float[] z = new float[m];
        for (int i = 0; i < m; i++) {
            z[i] = x[i] * a;
        }
        return z;
    }

    public static float[][] multiply(float[][] A, float a) {
        int m = A.length;
        int n = A[0].length;
        float[][] C = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] * a;
            }
        }
        return C;
    }

    // useful for testing
    public static String toString(float[][] A) {
        StringBuilder stringBuilder = new StringBuilder();
        int m = A.length;
        int n = A[0].length;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                stringBuilder.append(Float.toString(A[i][j]));
                stringBuilder.append(" ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public static float[][] linearToRectangular(float[] A, int m, int n) {
        if (A.length != m * n) {
            throw  new RuntimeException("Illegal dimensions");
        }
        float[][] B = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                B[i][j] = A[n * i + j];
            }
        }
        return B;
    }

    public static float[] rectangularToLinear(float[][] A) {
        int m = A.length;
        int n = A[0].length;
        float[] B = new float[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                B[n * i + j] = A[i][j];
            }
        }
        return B;
    }

    public static float det(float[][] A) {
        if (A.length != 3 || A[0].length != 3) {
            throw new RuntimeException("This method is for 3x3 matrices only");
        }
        float s = 0;
        s += A[0][0] * A[1][1] * A[2][2];
        s += A[0][1] * A[1][2] * A[2][0];
        s += A[0][2] * A[1][0] * A[2][1];
        s -= A[0][2] * A[1][1] * A[2][0];
        s -= A[0][1] * A[1][0] * A[2][2];
        s -= A[0][0] * A[1][2] * A[2][1];

        return s;
    }

    public static float[][] identity(int n) {
        float[][] I = new float[n][n];
        for (int i = 0; i < n; i++) {
            I[i][i] = 1;
        }
        return I;
    }

}
