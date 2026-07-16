package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.KyberModel;
import model.LatticeModel;
import model.RSAModel;
import model.Ring;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Serveur web pour l'outil de démonstration. Sert le frontend (webapp/) et
 * expose la logique métier existante (model.*) via une petite API HTTP
 * (paramètres en x-www-form-urlencoded, réponses en JSON fait main).
 *
 * Un seul jeu de modèles est partagé par tous les clients (pas de session
 * par utilisateur) : convient à un usage local/démo, pas à un vrai
 * déploiement multi-utilisateurs.
 */
public class WebServer {

    private final RSAModel rsaModel = new RSAModel();
    private final KyberModel kyberModel = new KyberModel();

    private BigInteger lastRsaCiphertext;
    private KyberModel.Ciphertext lastKyberCipher;
    private int[] lastKyberBits;

    private int factoLevel = 0;
    private BigInteger factoN;
    private BigInteger[] factoFactors;

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new WebServer().start(port);
    }

    public void start(int port) throws IOException {
        rsaModel.genererCrible();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/sieve", this::handleSieve);
        server.createContext("/api/factorization/level", this::handleFactorizationLevel);
        server.createContext("/api/factorization/validate", this::handleFactorizationValidate);
        server.createContext("/api/factorization/giveup", this::handleFactorizationGiveup);
        server.createContext("/api/rsa/generate", this::handleRsaGenerate);
        server.createContext("/api/rsa/encrypt", this::handleRsaEncrypt);
        server.createContext("/api/rsa/decrypt", this::handleRsaDecrypt);
        server.createContext("/api/kyber/generate", this::handleKyberGenerate);
        server.createContext("/api/kyber/example", this::handleKyberExample);
        server.createContext("/api/kyber/encrypt", this::handleKyberEncrypt);
        server.createContext("/api/kyber/decrypt", this::handleKyberDecrypt);
        server.createContext("/api/lattice/standard", ex -> handleLatticeFixed(ex,
            new int[]{1, 0}, new int[]{0, 1}, new int[]{3, -2}, 3, -2));
        server.createContext("/api/lattice/combination", ex -> handleLatticeFixed(ex,
            new int[]{8, 3}, new int[]{7, 3}, new int[]{9, 3}, 2, -1));
        server.createContext("/api/lattice/cvp", this::handleLatticeCvp);

        server.createContext("/", this::handleStatic);

        server.setExecutor(null);
        server.start();
        System.out.println("Serveur démarré : http://localhost:" + port);
    }

    // ============================== CRIBLE ==============================

    private void handleSieve(HttpExchange ex) throws IOException {
        rsaModel.genererCrible();
        StringBuilder primes = new StringBuilder("[");
        boolean first = true;
        for (int p : rsaModel.getNombresPremiers()) {
            if (!first) primes.append(",");
            primes.append(p);
            first = false;
        }
        primes.append("]");
        String json = Json.obj()
            .putNum("max", RSAModel.MAX_CRIBLE)
            .put("primes", primes.toString())
            .build();
        sendJson(ex, json);
    }

    // ===================== CHALLENGE DE FACTORISATION =====================

    private void handleFactorizationLevel(HttpExchange ex) throws IOException {
        Map<String, String> params = parseParams(ex);
        factoLevel = Integer.parseInt(params.getOrDefault("level", "0"));

        BigInteger[] defi = rsaModel.genererDefi(factoLevel);
        factoN = defi[0];
        factoFactors = new BigInteger[]{defi[1], defi[2]};

        String json = Json.obj()
            .putNum("level", factoLevel)
            .putStr("n", factoN.toString())
            .putNum("totalNiveaux", RSAModel.NB_NIVEAUX_FACTORISATION)
            .build();
        sendJson(ex, json);
    }

    private void handleFactorizationValidate(HttpExchange ex) throws IOException {
        Map<String, String> params = parseParams(ex);
        BigInteger p = parseBigOrNull(params.get("p"));
        BigInteger q = parseBigOrNull(params.get("q"));

        boolean correct = p != null && q != null
            && p.multiply(q).equals(factoN)
            && ((p.equals(factoFactors[0]) && q.equals(factoFactors[1]))
                || (p.equals(factoFactors[1]) && q.equals(factoFactors[0])));

        String json = Json.obj()
            .putBool("correct", correct)
            .putStr("solutionP", factoFactors[0].toString())
            .putStr("solutionQ", factoFactors[1].toString())
            .build();
        sendJson(ex, json);
    }

    private void handleFactorizationGiveup(HttpExchange ex) throws IOException {
        String json = Json.obj()
            .putStr("solutionP", factoFactors[0].toString())
            .putStr("solutionQ", factoFactors[1].toString())
            .build();
        sendJson(ex, json);
    }

    // ============================ RSA ============================

    private void handleRsaGenerate(HttpExchange ex) throws IOException {
        rsaModel.genererPQ();
        BigInteger p = rsaModel.getP();
        BigInteger q = rsaModel.getQ();
        rsaModel.publicKeyInit(p, q);
        rsaModel.privateKeyInit(p, q);
        BigInteger[] publicKey = rsaModel.getPublicKey();
        BigInteger[] secretKey = rsaModel.getSecretKey();
        BigInteger phiN = rsaModel.phiEuler(p, q);
        lastRsaCiphertext = null;

        String json = Json.obj()
            .putStr("p", p.toString())
            .putStr("q", q.toString())
            .putStr("n", publicKey[0].toString())
            .putStr("phiN", phiN.toString())
            .putStr("e", publicKey[1].toString())
            .putStr("d", secretKey[0].toString())
            .build();
        sendJson(ex, json);
    }

    private void handleRsaEncrypt(HttpExchange ex) throws IOException {
        Map<String, String> params = parseParams(ex);
        String message = params.getOrDefault("message", "");
        BigInteger n = rsaModel.getPublicKey()[0];
        BigInteger e = rsaModel.getPublicKey()[1];

        if (message.isEmpty()) {
            sendJson(ex, Json.obj().putBool("ok", false).putStr("erreur", "Merci d'entrer un message.").build());
            return;
        }
        BigInteger m = new BigInteger(1, message.getBytes(StandardCharsets.UTF_8));
        if (m.compareTo(n) >= 0) {
            sendJson(ex, Json.obj().putBool("ok", false)
                .putStr("erreur", "Message trop long pour ce N : raccourcissez-le.").build());
            return;
        }
        lastRsaCiphertext = rsaModel.rsaEncryption(m, e, n);
        String json = Json.obj()
            .putBool("ok", true)
            .putStr("m", m.toString())
            .putStr("c", lastRsaCiphertext.toString())
            .build();
        sendJson(ex, json);
    }

    private void handleRsaDecrypt(HttpExchange ex) throws IOException {
        if (lastRsaCiphertext == null) {
            sendJson(ex, Json.obj().putBool("ok", false).putStr("erreur", "Chiffrez d'abord un message.").build());
            return;
        }
        BigInteger n = rsaModel.getPublicKey()[0];
        BigInteger d = rsaModel.getSecretKey()[0];
        BigInteger decrypted = rsaModel.rsaDecryption(lastRsaCiphertext, d, n);
        String texte = bigIntegerToMessage(decrypted);
        String json = Json.obj()
            .putBool("ok", true)
            .putStr("m", decrypted.toString())
            .putStr("texte", texte)
            .build();
        sendJson(ex, json);
    }

    // ============================ KYBER ============================

    private void handleKyberGenerate(HttpExchange ex) throws IOException {
        kyberModel.genererCles();
        lastKyberCipher = null;
        lastKyberBits = null;
        String json = Json.obj()
            .put("a", matrice3dToJson(kyberModel.getMatriceA()))
            .put("t", matrice2dToJson(kyberModel.getVecteurT()))
            .put("s", matrice2dToJson(kyberModel.getVecteurS()))
            .build();
        sendJson(ex, json);
    }

    private void handleKyberExample(HttpExchange ex) throws IOException {
        int nEx = 4;
        int kEx = 2;
        Ring ring = new Ring(nEx, KyberModel.Q);

        int[][][] a = new int[kEx][kEx][];
        for (int i = 0; i < kEx; i++) {
            for (int j = 0; j < kEx; j++) {
                a[i][j] = ring.createRandomPolynomial();
            }
        }
        int[][] s = new int[kEx][];
        int[][] e = new int[kEx][];
        for (int i = 0; i < kEx; i++) {
            s[i] = ring.createRandomNoisePolynomial();
            e[i] = ring.createRandomNoisePolynomial();
        }
        int[][] t = new int[kEx][];
        for (int i = 0; i < kEx; i++) {
            int[] somme = new int[nEx];
            for (int j = 0; j < kEx; j++) {
                somme = ring.sum(somme, ring.multiplication(a[i][j], s[j]));
            }
            t[i] = ring.sum(somme, e[i]);
        }

        String json = Json.obj()
            .put("a", matrice3dToJson(a))
            .put("t", matrice2dToJson(t))
            .put("s", matrice2dToJson(s))
            .build();
        sendJson(ex, json);
    }

    private void handleKyberEncrypt(HttpExchange ex) throws IOException {
        Map<String, String> params = parseParams(ex);
        String message = params.getOrDefault("message", "");
        if (message.isEmpty()) {
            sendJson(ex, Json.obj().putBool("ok", false).putStr("erreur", "Merci d'entrer un message.").build());
            return;
        }
        int[] bits = kyberModel.texteEnBits(message);
        if (bits.length > KyberModel.N) {
            sendJson(ex, Json.obj().putBool("ok", false)
                .putStr("erreur", "Message trop long (max " + (KyberModel.N / 8) + " caractères ASCII environ).")
                .build());
            return;
        }
        lastKyberBits = bits;
        lastKyberCipher = kyberModel.chiffrer(bits);
        String json = Json.obj()
            .putBool("ok", true)
            .put("u0", Json.arrInt(lastKyberCipher.u[0]))
            .put("v", Json.arrInt(lastKyberCipher.v))
            .build();
        sendJson(ex, json);
    }

    private void handleKyberDecrypt(HttpExchange ex) throws IOException {
        if (lastKyberCipher == null) {
            sendJson(ex, Json.obj().putBool("ok", false).putStr("erreur", "Chiffrez d'abord un message.").build());
            return;
        }
        int[] bitsDechiffres = kyberModel.dechiffrer(lastKyberCipher, lastKyberBits.length);
        String texte = kyberModel.bitsEnTexte(bitsDechiffres);
        boolean succes = Arrays.equals(lastKyberBits, bitsDechiffres);
        String json = Json.obj()
            .putBool("ok", true)
            .putStr("texte", texte)
            .putBool("succes", succes)
            .build();
        sendJson(ex, json);
    }

    // ============================ RÉSEAUX EUCLIDIENS ============================

    private void handleLatticeFixed(HttpExchange ex, int[] u, int[] v, int[] cible, int solA, int solB) throws IOException {
        LatticeModel lattice = new LatticeModel(u, v);
        int[] origine = {0, 0};

        if ("GET".equals(ex.getRequestMethod())) {
            String json = Json.obj()
                .put("u", Json.arrInt(u))
                .put("v", Json.arrInt(v))
                .put("a", Json.arrInt(cible))   // "a" = cible affichée comme A dans le frontend
                .put("origine", Json.arrInt(origine))
                .build();
            sendJson(ex, json);
            return;
        }

        Map<String, String> params = parseParams(ex);
        int propA = Integer.parseInt(params.getOrDefault("a", "0"));
        int propB = Integer.parseInt(params.getOrDefault("b", "0"));
        boolean correct = (propA == solA && propB == solB);
        String json = Json.obj()
            .putBool("correct", correct)
            .putNum("solutionA", solA)
            .putNum("solutionB", solB)
            .build();
        sendJson(ex, json);
    }

    private void handleLatticeCvp(HttpExchange ex) throws IOException {
        int[] u = {8, 3};
        int[] v = {7, 3};
        double[] cible = {4.2, 5.5};   // coordonnées flottantes → solution unique : a=-10, b=12
        int[] origine = {0, 0};
        LatticeModel lattice = new LatticeModel(u, v);
        int[] solution = lattice.closestCombination(origine, cible, -15, 15);

        if ("GET".equals(ex.getRequestMethod())) {
            String json = Json.obj()
                .put("u", Json.arrInt(u))
                .put("v", Json.arrInt(v))
                .put("ax", String.valueOf(cible[0]))
                .put("ay", String.valueOf(cible[1]))
                .put("origine", Json.arrInt(origine))
                .build();
            sendJson(ex, json);
            return;
        }

        Map<String, String> params = parseParams(ex);
        int propA = Integer.parseInt(params.getOrDefault("a", "0"));
        int propB = Integer.parseInt(params.getOrDefault("b", "0"));

        double distProp = lattice.distance(lattice.combine(origine, propA, propB), cible);
        double distOpt = lattice.distance(lattice.combine(origine, solution[0], solution[1]), cible);
        boolean correct = Math.abs(distProp - distOpt) < 1e-9;

        String json = Json.obj()
            .putBool("correct", correct)
            .putNum("solutionA", solution[0])
            .putNum("solutionB", solution[1])
            .putNum("distanceProposee", round2(distProp))
            .putNum("distanceOptimale", round2(distOpt))
            .build();
        sendJson(ex, json);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ============================ UTILITAIRES ============================

    private BigInteger parseBigOrNull(String s) {
        try {
            return new BigInteger(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String bigIntegerToMessage(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String matrice2dToJson(int[][] m) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(Json.arrInt(m[i]));
        }
        return sb.append("]").toString();
    }

    private String matrice3dToJson(int[][][] m) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(matrice2dToJson(m[i]));
        }
        return sb.append("]").toString();
    }

    private Map<String, String> parseParams(HttpExchange ex) throws IOException {
        Map<String, String> map = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        parseInto(map, query);
        if ("POST".equals(ex.getRequestMethod())) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ex.getRequestBody().transferTo(buffer);
            parseInto(map, buffer.toString(StandardCharsets.UTF_8));
        }
        return map;
    }

    private void parseInto(Map<String, String> map, String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
    }

    private void sendJson(HttpExchange ex, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    // ============================ FICHIERS STATIQUES ============================

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        String relatif = path.substring(1);

        byte[] contenu = lireRessourceStatique(relatif);
        if (contenu == null) {
            String message = "404 introuvable : " + path;
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
            return;
        }

        ex.getResponseHeaders().add("Content-Type", typeContenu(relatif));
        ex.sendResponseHeaders(200, contenu.length);
        ex.getResponseBody().write(contenu);
        ex.close();
    }

    private byte[] lireRessourceStatique(String relatif) throws IOException {
        String[] cheminsCandidats = {
            "webapp/" + relatif,
            "src/webapp/" + relatif,
            "../webapp/" + relatif
        };
        for (String chemin : cheminsCandidats) {
            File f = new File(chemin);
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    return fis.readAllBytes();
                }
            }
        }
        try (InputStream is = WebServer.class.getResourceAsStream("/webapp/" + relatif)) {
            if (is != null) {
                return is.readAllBytes();
            }
        }
        return null;
    }

    private String typeContenu(String chemin) {
        if (chemin.endsWith(".html")) return "text/html; charset=utf-8";
        if (chemin.endsWith(".css")) return "text/css; charset=utf-8";
        if (chemin.endsWith(".js")) return "text/javascript; charset=utf-8";
        return "application/octet-stream";
    }
}
