package org.jlab.epsci.ersap.matrix;

//import org.apache.commons.math3.linear.Array2DRowRealMatrix;
//import org.apache.commons.math3.linear.DecompositionSolver;
//import org.apache.commons.math3.linear.LUDecomposition;
//import org.apache.commons.math3.linear.RealMatrix;

public class MatrixInverse {

    public static void main(String[] args) {

        double[][] values = {
                {2.789647, -4.606063, 0.061517, -0.035477, 0.111700},
                {-4.606063, 61.534596, -0.038914, 0.520977, -0.002434},
                {0.061517, -0.038914, 0.001614, -0.000268, 0.003235},
                {-0.035477, 0.520977, -0.000268, 0.004420, 0.000072},
                {0.111700, -0.002434, 0.003235, 0.000072, 0.006787}

        };
        double[][] rhs = {
                {1, 0, 0, 0, 0},
                {0, 1, 0, 0, 0},
                {0, 0, 1, 0, 0},
                {0, 0, 0, 1, 0},
                {0, 0, 0, 0, 1}
        };

        // Solving AB = I for given A
//        RealMatrix A = new Array2DRowRealMatrix(values);
//        System.out.println("Input A: " + A);
//        DecompositionSolver solver = new LUDecomposition(A).getSolver();

//        RealMatrix I = new Array2DRowRealMatrix(rhs);
//        RealMatrix B = solver.solve(I);
//        System.out.println("Inverse B: " + B);
    }

}
