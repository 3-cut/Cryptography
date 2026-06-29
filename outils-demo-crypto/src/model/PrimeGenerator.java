package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Génération de nombres premiers "de taille cryptographique" : division
 * d'essai par les 100 000 premiers nombres premiers (rapide, élimine la
 * plupart des candidats composés), puis test de Miller-Rabin sur 40 témoins
 * fixes si la division d'essai ne conclut pas. genererPremier(n) renvoie
 * toujours un nombre d'exactement n bits (bit de poids fort forcé à 1).
 */
public class PrimeGenerator {

    private static final int[] BASES_MILLER_RABIN = {
        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
        73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163,
        167, 173
    };

    private final SecureRandom random = new SecureRandom();
    private final long[] petitsPremiers;

    public PrimeGenerator() {
        this.petitsPremiers = chargerPetitsPremiers();
    }

    /** Nombre de petits nombres premiers effectivement chargés (0 si le fichier est introuvable). */
    public int getNombrePetitsPremiers() {
        return petitsPremiers.length;
    }

    private long[] chargerPetitsPremiers() {
        // Chemins essayés dans l'ordre : ça couvre "java -cp out Main" lancé
        // depuis src/ (notre workflow documenté), depuis la racine du projet,
        // ou avec le fichier copié à côté du .class.
        String[] cheminsCandidats = {
            "model/primes_100000.txt",
            "src/model/primes_100000.txt",
            "primes_100000.txt",
            "../primes_100000.txt"
        };
        for (String chemin : cheminsCandidats) {
            File f = new File(chemin);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    return lire(br);
                } catch (IOException ignored) {
                    // on essaie le chemin suivant
                }
            }
        }
        try (InputStream is = PrimeGenerator.class.getResourceAsStream("/model/primes_100000.txt")) {
            if (is != null) {
                return lire(new BufferedReader(new InputStreamReader(is)));
            }
        } catch (IOException ignored) {
            // tant pis, on retombe sur Miller-Rabin seul
        }
        System.err.println(
            "Avertissement : primes_100000.txt introuvable. "
                + "La génération de nombres premiers utilisera uniquement Miller-Rabin "
                + "(plus lente, mais toujours correcte).");
        return new long[0];
    }

    private long[] lire(BufferedReader br) throws IOException {
        List<Long> valeurs = new ArrayList<>();
        String ligne;
        while ((ligne = br.readLine()) != null) {
            ligne = ligne.trim();
            if (!ligne.isEmpty()) {
                valeurs.add(Long.parseLong(ligne));
            }
        }
        long[] resultat = new long[valeurs.size()];
        for (int i = 0; i < resultat.length; i++) {
            resultat[i] = valeurs.get(i);
        }
        return resultat;
    }

    // ------------------- Miller-Rabin -------------------

    /** p-1 = q * 2^k, q impair. */
    private static final class Decomposition {
        final int k;
        final BigInteger q;
        Decomposition(int k, BigInteger q) { this.k = k; this.q = q; }
    }

    private Decomposition decompose(BigInteger p) {
        BigInteger q = p.subtract(BigInteger.ONE);
        int k = 0;
        while (!q.testBit(0)) { // tant que q est pair
            q = q.shiftRight(1);
            k++;
        }
        return new Decomposition(k, q);
    }

    private boolean temoinMillerRabin(BigInteger a, BigInteger p) {
        Decomposition d = decompose(p);
        int k = d.k;
        BigInteger q = d.q;

        BigInteger b = a.modPow(q, p);
        if (b.equals(BigInteger.ONE) || b.equals(p.subtract(BigInteger.ONE))) {
            return true;
        }
        while (k > 0) {
            b = b.modPow(BigInteger.TWO, p);
            if (b.equals(p.subtract(BigInteger.ONE))) {
                return true;
            }
            k--;
        }
        return false;
    }

    private boolean millerRabin40(BigInteger p) {
        for (int a : BASES_MILLER_RABIN) {
            if (!temoinMillerRabin(BigInteger.valueOf(a), p)) {
                return false;
            }
        }
        return true;
    }

    /** Division d'essai par les petits premiers, puis Miller-Rabin si nécessaire. */
    public boolean testPrimalite(BigInteger p) {
        for (long premier : petitsPremiers) {
            BigInteger bp = BigInteger.valueOf(premier);
            if (p.equals(bp)) {
                return true;
            }
            if (p.mod(bp).equals(BigInteger.ZERO)) {
                return false;
            }
        }
        return millerRabin40(p);
    }

    /**
     * Génère un nombre premier (probable) d'EXACTEMENT n bits : le bit de
     * poids fort est forcé à 1 (voir la note de correction en tête de classe).
     */
    public BigInteger genererPremier(int n) {
        BigInteger candidat = new BigInteger(n, random);
        candidat = candidat.setBit(n - 1);
        if (!candidat.testBit(0)) {
            candidat = candidat.add(BigInteger.ONE); // le rendre impair
        }
        while (!testPrimalite(candidat)) {
            candidat = candidat.add(BigInteger.TWO);
        }
        return candidat;
    }
}
