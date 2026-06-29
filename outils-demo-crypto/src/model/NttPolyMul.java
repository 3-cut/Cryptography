package model;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Multiplication de polynômes dans Zq[X]/(X^n+1) par NTT (Cooley-Tukey /
 * Gentleman-Sande), avec q = 3389, n = 256, racine = 17.
 */
public class NttPolyMul {

    // ---- parameters ----
    static final BigInteger Q = BigInteger.valueOf(3389);
    static final int N = 256;
    static final BigInteger ROOT = BigInteger.valueOf(17);

    // log2(N) : N doit être une puissance de deux pour que ce soit valable.
    static final int LOG_N = Integer.numberOfTrailingZeros(N);

    BigInteger nInv;
    BigInteger[] psis;
    BigInteger[] invPsis;
    int[] positions;

    public static void main(String[] args) {
        new NttPolyMul().run();
    }

    void run() {
        precompute();

        Random rnd = new Random();
        BigInteger[] a = randomPoly(rnd);
        BigInteger[] b = randomPoly(rnd);

        // calcul direct (schoolbook) de a(x) * b(x) mod (x^N + 1) mod Q, pour vérification
        BigInteger[] reference = negacyclicConvolution(a, b);

        // NTT-based multiplication (operates in place, so clone first)
        BigInteger[] aNtt = a.clone();
        BigInteger[] bNtt = b.clone();

        ntt(aNtt);
        ntt(bNtt);

        BigInteger[] c = new BigInteger[N];
        for (int i = 0; i < N; i++) {
            c[i] = aNtt[i].multiply(bNtt[i]).mod(Q);
        }

        invNtt(c);

        System.out.println("reference  : " + Arrays.toString(reference));
        System.out.println("ntt result : " + Arrays.toString(c));
        System.out.println("match      : " + Arrays.equals(reference, c));
    }

    BigInteger[] randomPoly(Random rnd) {
        BigInteger[] p = new BigInteger[N];
        for (int i = 0; i < N; i++) {
            long v = Math.floorMod(rnd.nextLong(), Q.longValue());
            p[i] = BigInteger.valueOf(v);
        }
        return p;
    }

    // ------------------- provided helpers (kept as given) -------------------

    public BigInteger[] bezout(BigInteger x, BigInteger y) {
        BigInteger[] u = {BigInteger.ONE, BigInteger.ZERO};
        BigInteger[] v = {BigInteger.ZERO, BigInteger.ONE};
        while (!y.equals(BigInteger.ZERO)) {
            BigInteger[] qr = x.divideAndRemainder(y);
            BigInteger quotient = qr[0];
            BigInteger reste = qr[1];
            x = y;
            y = reste;
            BigInteger[] nouveauU = {u[1], u[0].subtract(quotient.multiply(u[1]))};
            BigInteger[] nouveauV = {v[1], v[0].subtract(quotient.multiply(v[1]))};
            u = nouveauU;
            v = nouveauV;
        }
        if (x.signum() < 0) {
            return new BigInteger[]{x.negate(), u[0].negate(), v[0].negate()};
        } else {
            return new BigInteger[]{x, u[0], v[0]};
        }
    }

    public BigInteger pgcd(BigInteger x, BigInteger y) {
        x = x.abs();
        y = y.abs();
        while (!y.equals(BigInteger.ZERO)) {
            BigInteger r = x.mod(y);
            x = y;
            y = r;
        }
        return x;
    }

    /**
     * Inverse de x modulo n, calcule via Bezout (pour rester pedagogique :
     * on pourrait aussi utiliser directement x.modInverse(n)).
     */
    public BigInteger invModulus(BigInteger x, BigInteger n) {
        if (!pgcd(x, n).equals(BigInteger.ONE)) {
            throw new ArithmeticException(
                "x et n ne sont pas premiers entre eux : pas d'inverse modulaire.");
        }
        BigInteger[] resultat = bezout(x, n);
        return resultat[1].mod(n);
    }

    public BigInteger phiEuler(BigInteger p, BigInteger q) {
        return p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
    }

    // -------------------------------------------------------------------------

    // inversion des bits de x, sur LOG_N bits
    int reverseBits(int x) {
        int result = 0;
        for (int i = 0; i < LOG_N; i++) {
            result = (result << 1) | (x & 1);
            x >>= 1;
        }
        return result;
    }

    void precompute() {
        nInv = invModulus(BigInteger.valueOf(N), Q);

        positions = new int[N];
        for (int x = 0; x < N; x++) {
            positions[x] = reverseBits(x);
        }

        // tmp[x] = root^x mod q, for x = 0 .. n-1
        BigInteger[] tmp = new BigInteger[N];
        BigInteger psi = BigInteger.ONE;
        for (int x = 0; x < N; x++) {
            tmp[x] = psi;
            psi = psi.multiply(ROOT).mod(Q);
        }

        // psis / invPsis are tmp[], permuted into bit-reversed order
        psis = new BigInteger[N];
        invPsis = new BigInteger[N];
        for (int x = 0; x < N; x++) {
            BigInteger val = tmp[positions[x]];
            psis[x] = val;
            invPsis[x] = invModulus(val, Q);
        }
    }

    // Cooley-Tukey (CT) forward NTT, in place
    void ntt(BigInteger[] a) {
        int t = N;
        int m = 1;
        while (m < N) {
            t /= 2;
            for (int i = 0; i < m; i++) {
                int j1 = 2 * i * t;
                int j2 = j1 + t - 1;
                BigInteger S = psis[m + i];
                for (int j = j1; j <= j2; j++) {
                    BigInteger U = a[j];
                    BigInteger V = a[j + t].multiply(S).mod(Q);
                    a[j] = U.add(V).mod(Q);
                    a[j + t] = U.subtract(V).mod(Q); // BigInteger.mod always returns a value in [0, Q)
                }
            }
            m *= 2;
        }
    }

    // Gentleman-Sande (GS) inverse NTT, in place
    void invNtt(BigInteger[] a) {
        int t = 1;
        int m = N;
        while (m > 1) {
            int j1 = 0;
            int h = m / 2;
            for (int i = 0; i < h; i++) {
                int j2 = j1 + t - 1;
                BigInteger S = invPsis[h + i];
                for (int j = j1; j <= j2; j++) {
                    BigInteger U = a[j];
                    BigInteger V = a[j + t];
                    a[j] = U.add(V).mod(Q);
                    a[j + t] = U.subtract(V).mod(Q).multiply(S).mod(Q);
                }
                j1 += 2 * t;
            }
            t *= 2;
            m /= 2;
        }
        for (int i = 0; i < N; i++) {
            a[i] = a[i].multiply(nInv).mod(Q);
        }
    }

    // calcul direct de a(x) * b(x) mod (x^N + 1) mod Q, pour vérification
    BigInteger[] negacyclicConvolution(BigInteger[] a, BigInteger[] b) {
        BigInteger[] result = new BigInteger[N];
        Arrays.fill(result, BigInteger.ZERO);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                BigInteger prod = a[i].multiply(b[j]).mod(Q);
                int idx = i + j;
                if (idx < N) {
                    result[idx] = result[idx].add(prod).mod(Q);
                } else {
                    idx -= N;
                    result[idx] = result[idx].subtract(prod).mod(Q);
                }
            }
        }
        return result;
    }
}
