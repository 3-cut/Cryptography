package model;

/**
 * Petit modèle de réseau euclidien (lattice) en 2D, pour les deux exercices
 * qui précèdent Kyber :
 *  1) combinaison linéaire entière exacte (aller d'un point du réseau à un
 *     autre avec a·u + b·v) ;
 *  2) problème du vecteur le plus proche (Closest Vector Problem, CVP) :
 *     s'approcher le plus possible d'un point qui n'est PAS sur le réseau.
 * La difficulté du CVP en grande dimension est ce sur quoi repose la
 * sécurité de Kyber (Module-LWE) -- ces deux exercices préparent ce point.
 */
public class LatticeModel {

    public final int[] u; // vecteur de base 1, {x, y}
    public final int[] v; // vecteur de base 2, {x, y}

    public LatticeModel(int[] u, int[] v) {
        this.u = u;
        this.v = v;
    }

    /** a·u + b·v (sans origine, vecteur pur). */
    public int[] combine(int a, int b) {
        return new int[]{a * u[0] + b * v[0], a * u[1] + b * v[1]};
    }

    /** origine + a·u + b·v. */
    public int[] combine(int[] origine, int a, int b) {
        int[] c = combine(a, b);
        return new int[]{origine[0] + c[0], origine[1] + c[1]};
    }

    public double distance(int[] p, int[] q) {
        double dx = p[0] - q[0];
        double dy = p[1] - q[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Recherche brute-force du couple (a, b), dans [rangeMin, rangeMax],
     * qui minimise la distance entre (origine + a·u + b·v) et cible.
     * Brute-force volontaire : c'est justement le fait que cette recherche
     * devienne intraitable en grande dimension qui fonde la sécurité de
     * Kyber -- ici, en 2D, on peut se permettre de tout essayer.
     */
    public int[] closestCombination(int[] origine, int[] cible, int rangeMin, int rangeMax) {
        int meilleurA = 0;
        int meilleurB = 0;
        double meilleureDistance = Double.MAX_VALUE;
        for (int a = rangeMin; a <= rangeMax; a++) {
            for (int b = rangeMin; b <= rangeMax; b++) {
                int[] p = combine(origine, a, b);
                double d = distance(p, cible);
                if (d < meilleureDistance) {
                    meilleureDistance = d;
                    meilleurA = a;
                    meilleurB = b;
                }
            }
        }
        return new int[]{meilleurA, meilleurB};
    }
}
