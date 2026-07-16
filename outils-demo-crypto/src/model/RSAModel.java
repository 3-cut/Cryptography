package model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RSAModel {

    public static final int MAX_CRIBLE = 50;
    private final List<Integer> primes = new ArrayList<>();

    // Pools de nombres premiers pour le challenge de factorisation
    private static final int[] POOL1 = {2, 3, 5, 7};      // primes ≤ 7
    private static final int[] POOL2 = {11, 13, 17};       // primes 7 < p ≤ 17 (disjoint de POOL1)

    // Niveau 4 : un vrai module RSA de 2048 bits -- volontairement infactorisable à la main.
    private final BigInteger FACTO_N4 = new BigInteger(
        "11882145478027758036938939741117477846351651668528421667813977112298898248747202311931504485840315208643545644298737836036572693210302865026310191868453241223579040354185595689283832759168938149821900170190367675629873961176672598886762264557795307597411849052208684062103125302783756469574196892775220196450937856123950505698845429103671295774463600416892866206468044102728886195792533816810102891122871955216980063580845291909925458961066879722224353698111834077950914174902526422825277183745361458442835001353283594963483709755577455785534356604379381234877864424126003897838684689222845185008723878700316845859153");
    private final BigInteger FACTO_P4 = new BigInteger("127572756602984173788982586565109595743735548208073331253338421101588348816752460069167005488518443779096420273610167109233365632346450482673206829832331126122596845476111987730755438793334420376938345022669661523796667709125783940624949060333973965563506134695483099277412034301317399027755835393261940853547");
    private final BigInteger FACTO_Q4 = new BigInteger("93140148370438298167729085499692849022789597514230786240144240084348210274146938453487785479134848263347121000186971010297105930076916233265942619850705337754244365201150726164708079621160068222475462573048045451882696123476675662703136056396098927789100656040121036521240197739052560834973190273709551114099");

    public static final int NB_NIVEAUX_FACTORISATION = 4;

    public BigInteger[] genererDefi(int niveau) {
        if (niveau == 3) {
            return new BigInteger[]{FACTO_N4, FACTO_P4, FACTO_Q4};
        }
        int[] poolA = (niveau == 0 || niveau == 1) ? POOL1 : POOL2;
        int[] poolB = (niveau == 0) ? POOL1 : POOL2;

        int p, q;
        do {
            p = poolA[random.nextInt(poolA.length)];
            q = poolB[random.nextInt(poolB.length)];
        } while (p == q);

        BigInteger bp = BigInteger.valueOf(p);
        BigInteger bq = BigInteger.valueOf(q);
        return new BigInteger[]{bp.multiply(bq), bp, bq};
    }


    public static final int TAILLE_PREMIER_PAR_DEFAUT = 512;

    private final PrimeGenerator primeGenerator = new PrimeGenerator();

    private BigInteger p;
    private BigInteger q;
    private BigInteger n;

    private BigInteger[] publicKey;
    private BigInteger[] secretKey;

    private final Random random = new Random();

    public void genererPQ(int bitsParPremier) {
        BigInteger nouveauP = primeGenerator.genererPremier(bitsParPremier);
        BigInteger nouveauQ;
        do {
            nouveauQ = primeGenerator.genererPremier(bitsParPremier);
        } while (nouveauQ.equals(nouveauP));

        this.p = nouveauP;
        this.q = nouveauQ;
        this.n = p.multiply(q);
    }

    public void genererPQ() {
        genererPQ(TAILLE_PREMIER_PAR_DEFAUT);
    }

    // =========================================================================
    // CRIBLE D'ÉRATOSTHÈNE
    // =========================================================================

    public void genererCrible() {
        primes.clear();
        for (int i = 2; i <= MAX_CRIBLE; i++) {
            boolean isPrime = true;
            for (int j = 2; (long) j * j <= i; j++) {
                if (i % j == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primes.add(i);
            }
        }
    }

    public List<Integer> getNombresPremiers() {
        return primes;
    }

    public boolean estPremier(int nombre) {
        return primes.contains(nombre);
    }

    // =========================================================================
    // CHALLENGE DE FACTORISATION
    // =========================================================================


    // =========================================================================
    // OUTILS ARITHMÉTIQUES (Bézout, pgcd, indicatrice d'Euler)
    // =========================================================================

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

    // =========================================================================
    // GÉNÉRATION DES CLÉS RSA
    // =========================================================================

    private BigInteger randomBigIntegerInRange(BigInteger min, BigInteger max) {
        BigInteger range = max.subtract(min);
        BigInteger resultat;
        do {
            resultat = new BigInteger(range.bitLength(), random);
        } while (resultat.compareTo(range) >= 0);
        return resultat.add(min);
    }

    public BigInteger[] publicKeyInit(BigInteger p, BigInteger q) {
        BigInteger phiN = phiEuler(p, q);
        BigInteger n = p.multiply(q);
        BigInteger e;
        while (true) {
            e = randomBigIntegerInRange(BigInteger.TWO, phiN);
            if (pgcd(e, phiN).equals(BigInteger.ONE)) {
                break;
            }
        }
        publicKey = new BigInteger[]{n, e};
        return publicKey;
    }

    public BigInteger[] privateKeyInit(BigInteger p, BigInteger q) {
        if (publicKey == null) {
            throw new IllegalStateException("Appeler publicKeyInit avant privateKeyInit.");
        }
        BigInteger d = invModulus(publicKey[1], phiEuler(p, q));
        secretKey = new BigInteger[]{d, p, q};
        return secretKey;
    }

    public BigInteger[] getPublicKey() { return publicKey; }
    public BigInteger[] getSecretKey() { return secretKey; }
    public BigInteger getP() { return p; }
    public BigInteger getQ() { return q; }
    public BigInteger getN() { return n; }

    // =========================================================================
    // CHIFFREMENT / DÉCHIFFREMENT
    // =========================================================================

    public BigInteger rsaEncryption(BigInteger m, BigInteger e, BigInteger n) {
        return m.modPow(e, n);
    }

    public BigInteger rsaDecryption(BigInteger c, BigInteger d, BigInteger n) {
        return c.modPow(d, n);
    }
}
