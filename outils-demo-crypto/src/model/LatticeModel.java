package model;

public class LatticeModel {

    public final int[] u;
    public final int[] v;

    public LatticeModel(int[] u, int[] v) {
        this.u = u;
        this.v = v;
    }

    public int[] combine(int a, int b) {
        return new int[]{a * u[0] + b * v[0], a * u[1] + b * v[1]};
    }

    public int[] combine(int[] origine, int a, int b) {
        int[] c = combine(a, b);
        return new int[]{origine[0] + c[0], origine[1] + c[1]};
    }

    public double distance(int[] p, int[] q) {
        double dx = p[0] - q[0];
        double dy = p[1] - q[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distance(int[] p, double[] q) {
        double dx = p[0] - q[0];
        double dy = p[1] - q[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    public int[] closestCombination(int[] origine, int[] cible, int rangeMin, int rangeMax) {
        int meilleurA = 0, meilleurB = 0;
        double meilleureDistance = Double.MAX_VALUE;
        for (int a = rangeMin; a <= rangeMax; a++) {
            for (int b = rangeMin; b <= rangeMax; b++) {
                double d = distance(combine(origine, a, b), cible);
                if (d < meilleureDistance) { meilleureDistance = d; meilleurA = a; meilleurB = b; }
            }
        }
        return new int[]{meilleurA, meilleurB};
    }

    public int[] closestCombination(int[] origine, double[] cible, int rangeMin, int rangeMax) {
        int meilleurA = 0, meilleurB = 0;
        double meilleureDistance = Double.MAX_VALUE;
        for (int a = rangeMin; a <= rangeMax; a++) {
            for (int b = rangeMin; b <= rangeMax; b++) {
                double d = distance(combine(origine, a, b), cible);
                if (d < meilleureDistance) { meilleureDistance = d; meilleurA = a; meilleurB = b; }
            }
        }
        return new int[]{meilleurA, meilleurB};
    }
}
