package model;

import java.nio.charset.StandardCharsets;

/**
 * Implémentation pédagogique d'un schéma de chiffrement de type Kyber
 * (Module-LWE) : n=256, q=3389, k=2. Les opérations sur les polynômes
 * (somme, produit) viennent de Ring.
 *
 * Représentation :
 *  - un polynôme               = int[N]      (coefficients dans [0, Q[)
 *  - un vecteur de k polynômes = int[K][N]
 *  - une matrice k×k de poly.  = int[K][K][N]
 */
public class KyberModel {

    public static final int N = 256;
    public static final int Q = 3389;
    public static final int K = 2;

    private final Ring ring = new Ring(N, Q);

    private int[][][] matriceA; // k x k, publique
    private int[][] vecteurS;   // k x 1, secrète
    private int[][] vecteurT;   // k x 1, publique (t = A·s + e)

    public Ring getRing() { return ring; }
    public int[][][] getMatriceA() { return matriceA; }
    public int[][] getVecteurT() { return vecteurT; }
    public int[][] getVecteurS() { return vecteurS; }

    /** Génère une nouvelle paire de clés (A, t) / s. Pas d'état conservé entre deux appels. */
    public void genererCles() {
        matriceA = new int[K][K][];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < K; j++) {
                matriceA[i][j] = ring.createRandomPolynomial();
            }
        }

        vecteurS = new int[K][];
        int[][] vecteurE = new int[K][];
        for (int i = 0; i < K; i++) {
            vecteurS[i] = ring.createRandomNoisePolynomial();
            vecteurE[i] = ring.createRandomNoisePolynomial();
        }

        // t = A·s + e
        vecteurT = new int[K][];
        for (int i = 0; i < K; i++) {
            int[] somme = new int[N];
            for (int j = 0; j < K; j++) {
                somme = ring.sum(somme, ring.multiplication(matriceA[i][j], vecteurS[j]));
            }
            vecteurT[i] = ring.sum(somme, vecteurE[i]);
        }
    }

    /** Couple (u, v) renvoyé par chiffrer(...). */
    public static class Ciphertext {
        public final int[][] u; // k x 1
        public final int[] v;   // 1 polynôme

        public Ciphertext(int[][] u, int[] v) {
            this.u = u;
            this.v = v;
        }
    }

    /** Chiffre un message de bits (0/1), de longueur au plus N. */
    public Ciphertext chiffrer(int[] bits) {
        if (bits.length > N) {
            throw new IllegalArgumentException("Message trop long (max " + N + " bits).");
        }

        int[] messageEtale = new int[N];
        for (int i = 0; i < bits.length; i++) {
            messageEtale[i] = bits[i] * (Q / 2);
        }

        int[][] r = new int[K][];
        int[][] e1 = new int[K][];
        for (int i = 0; i < K; i++) {
            r[i] = ring.createRandomNoisePolynomial();
            e1[i] = ring.createRandomNoisePolynomial();
        }
        int[] e2 = ring.createRandomNoisePolynomial();

        // u = A^T · r + e1   (A^T[i][j] = A[j][i])
        int[][] u = new int[K][];
        for (int i = 0; i < K; i++) {
            int[] somme = new int[N];
            for (int j = 0; j < K; j++) {
                somme = ring.sum(somme, ring.multiplication(matriceA[j][i], r[j]));
            }
            u[i] = ring.sum(somme, e1[i]);
        }

        // v = t^T · r + e2 + messageEtale
        int[] v = new int[N];
        for (int j = 0; j < K; j++) {
            v = ring.sum(v, ring.multiplication(vecteurT[j], r[j]));
        }
        v = ring.sum(v, e2);
        v = ring.sum(v, messageEtale);

        return new Ciphertext(u, v);
    }

    /** Déchiffre (u, v) et renvoie les nombreBits bits récupérés. */
    public int[] dechiffrer(Ciphertext ct, int nombreBits) {
        int[] messageDechiffreEtale = new int[N];
        for (int j = 0; j < K; j++) {
            messageDechiffreEtale = ring.sum(messageDechiffreEtale,
                ring.multiplication(vecteurS[j], ct.u[j]));
        }
        messageDechiffreEtale = ring.sub(ct.v, messageDechiffreEtale);

        int[] bits = new int[nombreBits];
        for (int i = 0; i < nombreBits; i++) {
            int c = messageDechiffreEtale[i];
            if (c > Q / 2) {
                c -= Q;
            }
            bits[i] = Math.abs(c) > Q / 4 ? 1 : 0;
        }
        return bits;
    }

    // =========================================================================
    // Encodage texte <-> bits
    // =========================================================================

    /** Encode un texte en bits (UTF-8, 8 bits par octet, poids fort en premier). */
    public int[] texteEnBits(String texte) {
        byte[] octets = texte.getBytes(StandardCharsets.UTF_8);
        int[] bits = new int[octets.length * 8];
        for (int i = 0; i < octets.length; i++) {
            int valeur = octets[i] & 0xFF;
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = (valeur >> (7 - j)) & 1;
            }
        }
        return bits;
    }

    /** Décode des bits (multiple de 8) en texte UTF-8. */
    public String bitsEnTexte(int[] bits) {
        int nbOctets = bits.length / 8;
        byte[] octets = new byte[nbOctets];
        for (int i = 0; i < nbOctets; i++) {
            int valeur = 0;
            for (int j = 0; j < 8; j++) {
                valeur = (valeur << 1) | bits[i * 8 + j];
            }
            octets[i] = (byte) valeur;
        }
        return new String(octets, StandardCharsets.UTF_8);
    }
}
