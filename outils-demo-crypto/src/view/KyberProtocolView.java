package view;

import controller.AppController;
import model.KyberModel;
import model.Ring;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Présentation du protocole Kyber (post-quantique), dans le même esprit que
 * RsaProtocolView : section "Cadre" (définition de l'anneau Rq), clé
 * publique / clé privée dans deux cartes, puis un panneau Alice → Bob pour
 * chiffrer/déchiffrer un message avec les formules explicites.
 *
 * Différence avec RSA : les clés sont des matrices/vecteurs de polynômes à
 * 256 coefficients (pas des nombres uniques), donc affichées de façon
 * condensée (quelques coefficients + "...") avec un bouton "Voir le détail
 * complet" pour les curieux.
 *
 * Les clés sont régénérées à chaque arrivée sur cet écran (pas d'état
 * conservé entre deux visites).
 */
public class KyberProtocolView extends JPanel {

    private static final Color ROUGE = new Color(0xE7, 0x4C, 0x3C);

    private final AppController controller;
    private final KyberModel model;

    private int[] lastBits;
    private KyberModel.Ciphertext lastCipher;

    private int[][][] exempleA;
    private int[][] exempleS;
    private int[][] exempleE;
    private int[][] exempleT;

    private JTextField messageField;
    private JLabel encryptionResultLabel;
    private JLabel decryptionResultLabel;

    public KyberProtocolView(AppController controller) {
        this.controller = controller;
        this.model = controller.getKyberModel();
        model.genererCles();
        genererExempleN4();

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(10, 10));
        centre.add(buildCadre(), BorderLayout.NORTH);
        centre.add(buildKeysPanel(), BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        add(buildCipherPanel(), BorderLayout.SOUTH);
    }

    private JComponent buildCadre() {
        JPanel cadre = new JPanel();
        cadre.setLayout(new BoxLayout(cadre, BoxLayout.Y_AXIS));
        cadre.setBorder(BorderFactory.createTitledBorder("Cadre"));

        JLabel formule = new JLabel(
            "Rq = Zq[X] / (Xⁿ + 1)     où     q = " + KyberModel.Q + "     n = " + KyberModel.N);
        formule.setFont(new Font("Monospaced", Font.PLAIN, 13));
        formule.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel explication = new JLabel(
            "<html>Anneau de polynômes modulo Xⁿ+1 (donc de degré inférieur à n−1), dont les<br>"
                + "coefficients sont modulo q (reste de la division euclidienne du coefficient par q).</html>");
        explication.setFont(new Font("SansSerif", Font.PLAIN, 12));
        explication.setForeground(Color.DARK_GRAY);
        explication.setAlignmentX(Component.LEFT_ALIGNMENT);
        explication.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        cadre.add(formule);
        cadre.add(explication);
        return cadre;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JButton homeButton = new JButton("← Accueil");
        homeButton.addActionListener(e -> controller.goHome());

        JLabel title = new JLabel(
            "Présentation du protocole Kyber (post-quantique)", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JButton regenerateButton = new JButton("⟳ Régénérer les clés");
        regenerateButton.addActionListener(e -> controller.goToKyberProtocolPresentation());

        header.add(homeButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(regenerateButton, BorderLayout.EAST);
        return header;
    }

    private JComponent buildKeysPanel() {
        JPanel keysPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        keysPanel.add(buildPublicKeyCard());
        keysPanel.add(buildPrivateKeyCard());
        return keysPanel;
    }

    private JPanel buildPublicKeyCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createTitledBorder("Clé publique"));

        card.add(formulaLine("A : matrice " + KyberModel.K + "×" + KyberModel.K + " de polynômes de Rq"));
        card.add(infoLine(
            "t = A×s + e",
            "e : polynôme de bruit, coefficients appartenant à {-2, -1, 0, 1, 2}.",
            "e"));

        JButton detailButton = new JButton("Voir le détail complet (A, t)");
        detailButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailButton.addActionListener(e -> showFullArrayDialog(
            "Clé publique complète",
            "Matrice A :\n\n" + formatMatriceComplete(model.getMatriceA())
                + "\nVecteur t :\n\n" + formatVecteurComplet("t", model.getVecteurT())));

        card.add(Box.createVerticalStrut(6));
        card.add(detailButton);
        card.add(Box.createVerticalStrut(10));
        card.add(boldLine("Clé publique = (A, t)"));
        card.add(exempleCaption());
        card.add(exempleLine("A ="));
        card.add(buildMatriceExempleA());
        card.add(Box.createVerticalStrut(4));
        card.add(exempleLine("t ="));
        card.add(buildVecteurExemple(exempleT));
        return card;
    }

    /** Affiche A sous forme de matrice classique 2x2 (les colonnes s'alignent automatiquement). */
    private JComponent buildMatriceExempleA() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 2));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        grid.add(matriceCellule(exempleA[0][0]));
        grid.add(matriceCellule(exempleA[0][1]));
        grid.add(matriceCellule(exempleA[1][0]));
        grid.add(matriceCellule(exempleA[1][1]));
        return grid;
    }

    /** Affiche un vecteur (t ou s) comme une colonne classique, une ligne par composante. */
    private JComponent buildVecteurExemple(int[][] vecteur) {
        JPanel grid = new JPanel(new GridLayout(vecteur.length, 1, 0, 2));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        for (int[] poly : vecteur) {
            grid.add(matriceCellule(poly));
        }
        return grid;
    }

    private JLabel matriceCellule(int[] poly) {
        JLabel label = new JLabel(Arrays.toString(poly));
        label.setFont(new Font("Monospaced", Font.PLAIN, 11));
        label.setForeground(Color.GRAY);
        return label;
    }

    private JPanel buildPrivateKeyCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createTitledBorder("Clé privée"));

        card.add(infoLine(
            "s : vecteur de " + KyberModel.K + " polynômes de Rq",
            "Coefficients appartenant à {-2, -1, 0, 1, 2}.",
            "s"));

        JButton detailButton = new JButton("Voir le détail complet (s)");
        detailButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailButton.addActionListener(e -> showFullArrayDialog(
            "Clé privée complète",
            formatVecteurComplet("s", model.getVecteurS())));

        card.add(Box.createVerticalStrut(6));
        card.add(detailButton);
        card.add(Box.createVerticalStrut(10));
        card.add(boldLine("Clé privée = (s)"));
        card.add(exempleCaption());
        card.add(exempleLine("s ="));
        card.add(buildVecteurExemple(exempleS));
        return card;
    }

    /** Exemple concret avec n=4 (au lieu de 256), calculé une fois et affiché en ligne dans les cartes. */
    private void genererExempleN4() {
        int nExemple = 4;
        int kExemple = 2;
        Ring ringExemple = new Ring(nExemple, KyberModel.Q);

        exempleA = new int[kExemple][kExemple][];
        for (int i = 0; i < kExemple; i++) {
            for (int j = 0; j < kExemple; j++) {
                exempleA[i][j] = ringExemple.createRandomPolynomial();
            }
        }

        exempleS = new int[kExemple][];
        exempleE = new int[kExemple][];
        for (int i = 0; i < kExemple; i++) {
            exempleS[i] = ringExemple.createRandomNoisePolynomial();
            exempleE[i] = ringExemple.createRandomNoisePolynomial();
        }

        exempleT = new int[kExemple][];
        for (int i = 0; i < kExemple; i++) {
            int[] somme = new int[nExemple];
            for (int j = 0; j < kExemple; j++) {
                somme = ringExemple.sum(somme, ringExemple.multiplication(exempleA[i][j], exempleS[j]));
            }
            exempleT[i] = ringExemple.sum(somme, exempleE[i]);
        }
    }

    private JComponent exempleCaption() {
        JLabel label = new JLabel("Exemple avec n = 4 :");
        label.setFont(new Font("SansSerif", Font.ITALIC, 11));
        label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));
        return label;
    }

    private JComponent exempleLine(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 11));
        label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        return label;
    }

    // ---- petits composants réutilisables (même style que RsaProtocolView) ----

    private JComponent formulaLine(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return label;
    }

    private JComponent boldLine(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return label;
    }

    private void showInfoDialog(String explanation, String title) {
        JTextArea area = new JTextArea(explanation);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, area, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private JComponent infoLine(String text, String explanation, String dialogTitle) {
        JPanel line = new JPanel(new BorderLayout(6, 0));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JButton infoButton = new JButton("i");
        infoButton.setMargin(new Insets(0, 4, 0, 4));
        infoButton.addActionListener(e -> showInfoDialog(explanation, dialogTitle));

        line.add(label, BorderLayout.CENTER);
        line.add(infoButton, BorderLayout.EAST);
        return line;
    }

    private void showFullArrayDialog(String title, String contenuComplet) {
        JTextArea area = new JTextArea(contenuComplet);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(550, 400));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.PLAIN_MESSAGE);
    }

    /** "[12, 847, 3201, 55, ..., 1942]" : aperçu condensé d'un polynôme à n coefficients. */
    private String formatPolynomial(int[] poly) {
        StringBuilder sb = new StringBuilder("[");
        int montrer = Math.min(5, poly.length);
        for (int i = 0; i < montrer; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(poly[i]);
        }
        if (poly.length > montrer) {
            sb.append(", ..., ").append(poly[poly.length - 1]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatMatriceComplete(int[][][] matrice) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrice.length; i++) {
            for (int j = 0; j < matrice[i].length; j++) {
                sb.append("A[").append(i).append("][").append(j).append("] = ")
                    .append(Arrays.toString(matrice[i][j])).append("\n\n");
            }
        }
        return sb.toString();
    }

    private String formatVecteurComplet(String nom, int[][] vecteur) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vecteur.length; i++) {
            sb.append(nom).append("[").append(i).append("] = ")
                .append(Arrays.toString(vecteur[i])).append("\n\n");
        }
        return sb.toString();
    }

    // ---- chiffrement / déchiffrement ----

    private JComponent buildCipherPanel() {
        JPanel container = new JPanel(new GridLayout(1, 2, 15, 0));
        container.add(buildChiffrementPanel());
        container.add(buildDechiffrementPanel());
        return container;
    }

    private JComponent buildChiffrementPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Chiffrement"));

        JLabel hint = new JLabel(
            "Message court (" + (KyberModel.N / 8) + " caractères ASCII maximum, encodé en bits).");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel alice = new JLabel("Alice  →  message  →  chiffrer  →  message chiffré");
        alice.setFont(new Font("SansSerif", Font.ITALIC, 12));
        alice.setForeground(Color.DARK_GRAY);
        alice.setAlignmentX(Component.LEFT_ALIGNMENT);
        alice.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));

        JLabel captionChiffrer = new JLabel(
            "<html>À l'aide de la clé publique (de Bob) :  u = A<sup>T</sup>·r + e1,&nbsp;&nbsp; v = t<sup>T</sup>·r + e2 + m</html>");
        captionChiffrer.setFont(new Font("Monospaced", Font.PLAIN, 12));
        captionChiffrer.setForeground(Color.BLACK);
        captionChiffrer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inputRow.add(new JLabel("Entrer un message à chiffrer :"));
        messageField = new JTextField(12);
        inputRow.add(messageField);
        JButton encryptButton = new JButton("Chiffrer");
        encryptButton.addActionListener(e -> onEncryptClicked());
        inputRow.add(encryptButton);

        encryptionResultLabel = new JLabel(" ");
        encryptionResultLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        encryptionResultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(hint);
        panel.add(alice);
        panel.add(captionChiffrer);
        panel.add(inputRow);
        panel.add(encryptionResultLabel);
        return panel;
    }

    private JComponent buildDechiffrementPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Déchiffrement"));

        JLabel bob = new JLabel("message chiffré  →  déchiffrer  →  message déchiffré  →  Bob");
        bob.setFont(new Font("SansSerif", Font.ITALIC, 12));
        bob.setForeground(Color.DARK_GRAY);
        bob.setAlignmentX(Component.LEFT_ALIGNMENT);
        bob.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));

        JLabel captionDechiffrer = new JLabel(
            "<html>À l'aide de la clé privée (de Bob) :  m = v − s<sup>T</sup>·u</html>");
        captionDechiffrer.setFont(new Font("Monospaced", Font.PLAIN, 12));
        captionDechiffrer.setForeground(Color.BLACK);
        captionDechiffrer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel decryptRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton decryptButton = new JButton("Déchiffrer");
        decryptButton.addActionListener(e -> onDecryptClicked());
        decryptRow.add(decryptButton);

        decryptionResultLabel = new JLabel(" ");
        decryptionResultLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        decryptionResultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(bob);
        panel.add(captionDechiffrer);
        panel.add(decryptRow);
        panel.add(decryptionResultLabel);
        return panel;
    }

    private void onEncryptClicked() {
        String message = messageField.getText();
        if (message == null || message.isEmpty()) {
            encryptionResultLabel.setForeground(ROUGE);
            encryptionResultLabel.setText("Merci d'entrer un message.");
            return;
        }

        int[] bits = model.texteEnBits(message);
        if (bits.length > KyberModel.N) {
            encryptionResultLabel.setForeground(ROUGE);
            encryptionResultLabel.setText(
                "Message trop long (max " + (KyberModel.N / 8) + " caractères ASCII environ).");
            lastCipher = null;
            return;
        }

        lastBits = bits;
        lastCipher = model.chiffrer(bits);
        encryptionResultLabel.setForeground(Color.BLACK);
        encryptionResultLabel.setText(
            "u[0] = " + formatPolynomial(lastCipher.u[0]) + "    v = " + formatPolynomial(lastCipher.v));
        decryptionResultLabel.setText(" ");
    }

    private void onDecryptClicked() {
        if (lastCipher == null) {
            decryptionResultLabel.setForeground(ROUGE);
            decryptionResultLabel.setText("Chiffrez d'abord un message.");
            return;
        }

        int[] bitsDechiffres = model.dechiffrer(lastCipher, lastBits.length);
        String texteDechiffre = model.bitsEnTexte(bitsDechiffres);
        boolean succes = Arrays.equals(lastBits, bitsDechiffres);

        decryptionResultLabel.setForeground(succes ? Color.BLACK : ROUGE);
        decryptionResultLabel.setText(
            "m = v − sᵗ·u   →   message déchiffré : \"" + texteDechiffre + "\""
                + (succes ? "" : "  (erreur : les bits diffèrent)"));
    }
}
