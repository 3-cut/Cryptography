package model;

import java.util.Random;

/**
 * Anneau quotient Zq[X]/(X^n+1) ("anneau négacyclique"), utilisé par Kyber.
 * Un élément de l'anneau est représenté par un tableau de n coefficients
 * (int[], coefficients réduits dans [0, q[).
 */
public class Ring {
    private final int n;
    private final int q;
    private final Random random = new Random();

    public Ring(int n, int q) {
        this.n = n;
        this.q = q;
    }

    public int getN() { return n; }
    public int getQ() { return q; }

    /** Ramène x dans [0, q[, quel que soit le signe de x. */
    private int mod(long x) {
        int r = (int) (x % q);
        return r < 0 ? r + q : r;
    }

    public int[] sum(int[] a, int[] b) {
        int[] resultat = new int[n];
        for (int i = 0; i < n; i++) {
            resultat[i] = mod((long) a[i] + b[i]);
        }
        return resultat;
    }

    public int[] sub(int[] a, int[] b) {
        int[] resultat = new int[n];
        for (int i = 0; i < n; i++) {
            resultat[i] = mod((long) a[i] - b[i]);
        }
        return resultat;
    }

    /**
     * Produit dans Zq[X]/(X^n+1) : convolution négacyclique. Dans cet
     * anneau, X^n = -1, donc un terme dont le degré "dépasse" n se
     * soustrait au lieu de s'additionner.
     */
    public int[] multiplication(int[] a, int[] b) {
        long[] acc = new long[n];
        for (int i = 0; i < n; i++) {
            if (a[i] == 0) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                long produit = (long) a[i] * b[j];
                int indice = i + j;
                if (indice < n) {
                    acc[indice] += produit;
                } else {
                    acc[indice - n] -= produit;
                }
            }
        }
        int[] resultat = new int[n];
        for (int i = 0; i < n; i++) {
            resultat[i] = mod(acc[i]);
        }
        return resultat;
    }

    /** Polynôme de "bruit" à petits coefficients, dans [-2, 2]. */
    public int[] createRandomNoisePolynomial() {
        int[] e = new int[n];
        for (int i = 0; i < n; i++) {
            e[i] = random.nextInt(5) - 2;
        }
        return e;
    }

    /** Polynôme aléatoire "uniforme", coefficients dans [0, q[. */
    public int[] createRandomPolynomial() {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = random.nextInt(q);
        }
        return p;
    }
}
