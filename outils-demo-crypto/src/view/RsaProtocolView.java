package view;

import controller.AppController;
import model.RSAModel;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Écran ③ : Présentation du protocole RSA.
 * Génère deux vrais nombres premiers p et q (512 bits chacun, donc N fait
 * environ 1024 bits, via RSAModel.genererPQ() -> PrimeGenerator), affiche
 * la clé publique (N, e) et la clé privée (d, p, q), puis permet de
 * chiffrer un message texte et de le déchiffrer pour vérifier que tout
 * fonctionne.
 *
 * Les clés sont régénérées à chaque arrivée sur cet écran (pas d'état
 * conservé entre deux visites). À 512 bits par nombre premier, la
 * génération reste rapide : pas besoin de thread d'arrière-plan ici. (Si
 * la taille de clé augmente significativement plus tard -- 4096 bits par
 * nombre premier par exemple --, il faudrait déplacer generateFreshKeys()
 * dans un SwingWorker pour ne pas geler l'interface.)
 */
public class RsaProtocolView extends JPanel {

    private final AppController controller;
    private final RSAModel model;

    private BigInteger n;
    private BigInteger phiN;
    private BigInteger e;
    private BigInteger d;
    private BigInteger lastCiphertext;

    private JTextField messageField;
    private JLabel encryptionResultLabel;
    private JLabel decryptionResultLabel;

    public RsaProtocolView(AppController controller) {
        this.controller = controller;
        this.model = controller.getRsaModel();

        generateFreshKeys();

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildKeysPanel(), BorderLayout.CENTER);
        add(buildCipherPanel(), BorderLayout.SOUTH);
    }

    /** Génère p, q (512 bits chacun) puis la paire de clés publique/privée. */
    private void generateFreshKeys() {
        model.genererPQ(); // 512 bits par nombre premier (RSAModel.TAILLE_PREMIER_PAR_DEFAUT)

        BigInteger p = model.getP();
        BigInteger q = model.getQ();

        model.publicKeyInit(p, q);
        model.privateKeyInit(p, q);

        BigInteger[] publicKey = model.getPublicKey();
        BigInteger[] secretKey = model.getSecretKey();

        n = publicKey[0];
        e = publicKey[1];
        d = secretKey[0];
        phiN = model.phiEuler(p, q);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JButton homeButton = new JButton("← Accueil");
        homeButton.addActionListener(e -> controller.goHome());

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Présentation du protocole RSA", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBlock.add(title);

        JButton regenerateButton = new JButton("⟳ Régénérer les clés");
        regenerateButton.addActionListener(ev -> controller.goToRsaProtocolPresentation());

        header.add(homeButton, BorderLayout.WEST);
        header.add(titleBlock, BorderLayout.CENTER);
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

        card.add(infoLine(
            "N = p × q = " + formatGrand(n),
            "N = p × q",
            "N"));
        card.add(infoLine(
            "φ(N) = (p−1)(q−1) = " + formatGrand(phiN),
            "φ : l'indicatrice d'Euler, nombre d'entiers inférieurs à N tels que\n"
                + "N soit premier avec eux (pgcd(x, N) = 1, avec x < N).",
            "φ"));
        card.add(infoLine(
            "e = " + formatGrand(e),
            "e est choisi aléatoirement tel que 1 < e < φ(N) et pgcd(e, φ(N)) = 1.",
            "e"));

        JButton detailButtonPub = new JButton("Voir les valeurs complètes");
        detailButtonPub.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailButtonPub.addActionListener(ev -> showFullValueDialog(
            "Clé publique complète",
            "N =\n" + n + "\n\nφ(N) =\n" + phiN + "\n\ne =\n" + e));
        card.add(Box.createVerticalStrut(6));
        card.add(detailButtonPub);

        card.add(Box.createVerticalStrut(10));
        card.add(boldLine("Clé publique = (N, e)"));
        return card;
    }

    private JPanel buildPrivateKeyCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createTitledBorder("Clé privée"));

        card.add(infoLine(
            "p = " + formatGrand(model.getP()),
            "p et q sont des nombres premiers de 512 bits, générés à l'aide\n"
                + "de l'algorithme de Miller-Rabin.",
            "p"));
        card.add(formulaLine("q = " + formatGrand(model.getQ())));
        card.add(infoLine(
            "d ≡ e⁻¹ (mod φ(N))",
            "d, inverse modulaire de e : d × e ≡ 1 (mod φ(N)).\n"
                + "Le reste de la division euclidienne de d × e par φ(N) est 1.",
            "d"));
        card.add(formulaLine("d = " + formatGrand(d)));

        JButton detailButtonPriv = new JButton("Voir les valeurs complètes");
        detailButtonPriv.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailButtonPriv.addActionListener(ev -> showFullValueDialog(
            "Clé privée complète",
            "p =\n" + model.getP() + "\n\nq =\n" + model.getQ() + "\n\nd =\n" + d));
        card.add(Box.createVerticalStrut(6));
        card.add(detailButtonPriv);

        card.add(Box.createVerticalStrut(10));
        card.add(boldLine("Clé privée = (d, p, q)"));
        return card;
    }

    /**
     * Pour l'affichage : un BigInteger de 1024 bits a ~309 chiffres, illisible
     * (et ça dépasserait la largeur de la fenêtre) si on l'affiche en entier.
     * Au-delà de 50 chiffres, on tronque avec un aperçu début/fin.
     */
    private String formatGrand(BigInteger valeur) {
        String texte = valeur.toString();
        if (texte.length() <= 50) {
            return texte;
        }
        return texte.substring(0, 24) + "…(" + texte.length() + " chiffres)…"
            + texte.substring(texte.length() - 10);
    }

    private void showFullValueDialog(String title, String contenu) {
        JTextArea area = new JTextArea(contenu);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 250));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.PLAIN_MESSAGE);
    }

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

    /** Ligne de formule avec un petit bouton "i" qui affiche une explication. */
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
            "N fait ~1024 bits : message jusqu'à environ 120 caractères ASCII.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel alice = new JLabel("Alice  →  message  →  chiffrer  →  message chiffré");
        alice.setFont(new Font("SansSerif", Font.ITALIC, 12));
        alice.setForeground(Color.DARK_GRAY);
        alice.setAlignmentX(Component.LEFT_ALIGNMENT);
        alice.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));

        JLabel captionChiffrer = new JLabel(
            "À l'aide de la clé publique (de Bob) :  C ≡ m^e (mod N)");
        captionChiffrer.setFont(new Font("Monospaced", Font.PLAIN, 12));
        captionChiffrer.setForeground(Color.BLACK);
        captionChiffrer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inputRow.add(new JLabel("Entrer un message à chiffrer :"));
        messageField = new JTextField(20);
        inputRow.add(messageField);
        JButton encryptButton = new JButton("Chiffrer");
        encryptButton.addActionListener(e -> onEncryptClicked());
        inputRow.add(encryptButton);

        encryptionResultLabel = new JLabel(" ");
        encryptionResultLabel.setFont(new Font("Monospaced", Font.PLAIN, 13));
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
            "À l'aide de la clé privée (de Bob) :  m ≡ c^d (mod N)");
        captionDechiffrer.setFont(new Font("Monospaced", Font.PLAIN, 12));
        captionDechiffrer.setForeground(Color.BLACK);
        captionDechiffrer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel decryptRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton decryptButton = new JButton("Déchiffrer");
        decryptButton.addActionListener(e -> onDecryptClicked());
        decryptRow.add(decryptButton);

        decryptionResultLabel = new JLabel(" ");
        decryptionResultLabel.setFont(new Font("Monospaced", Font.PLAIN, 13));
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
            encryptionResultLabel.setForeground(new Color(0xE7, 0x4C, 0x3C));
            encryptionResultLabel.setText("Merci d'entrer un message.");
            return;
        }

        BigInteger m = messageToBigInteger(message);
        if (m.compareTo(n) >= 0) {
            encryptionResultLabel.setForeground(new Color(0xE7, 0x4C, 0x3C));
            encryptionResultLabel.setText(
                "Message trop long pour ce N (1024 bits) : raccourcissez-le.");
            lastCiphertext = null;
            return;
        }

        lastCiphertext = model.rsaEncryption(m, e, n);
        encryptionResultLabel.setForeground(Color.BLACK);
        encryptionResultLabel.setText(
            "m = " + formatGrand(m) + "   →   C ≡ m^e mod(N) = " + formatGrand(lastCiphertext));
        decryptionResultLabel.setText(" ");
    }

    private void onDecryptClicked() {
        if (lastCiphertext == null) {
            decryptionResultLabel.setForeground(new Color(0xE7, 0x4C, 0x3C));
            decryptionResultLabel.setText("Chiffrez d'abord un message.");
            return;
        }

        BigInteger decrypted = model.rsaDecryption(lastCiphertext, d, n);
        String decryptedText = bigIntegerToMessage(decrypted);
        decryptionResultLabel.setForeground(Color.BLACK);
        decryptionResultLabel.setText(
            "m ≡ c^d mod(N) = " + formatGrand(decrypted) + "   →   message déchiffré : \"" + decryptedText + "\"");
    }

    private BigInteger messageToBigInteger(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        return new BigInteger(1, bytes);
    }

    private String bigIntegerToMessage(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
