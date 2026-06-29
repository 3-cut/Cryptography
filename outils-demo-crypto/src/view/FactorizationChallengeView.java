package view;

import controller.AppController;
import model.RSAModel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.math.BigInteger;

public class FactorizationChallengeView extends JPanel {

    private final AppController controller;
    private final RSAModel model;

    private int levelIndex = 0;
    private BigInteger currentN;
    private BigInteger[] currentFactors;
    private boolean validated = false;

    private JLabel levelLabel;
    private JLabel nLabel;
    private JTextField pField;
    private JTextField qField;
    private JLabel messageLabel;
    private JButton actionButton;
    private JButton giveUpButton;
    private JButton voirEnEntierButton;

    private static final Border DEFAULT_FIELD_BORDER =
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(4, 6, 4, 6));
    private static final Border ERROR_FIELD_BORDER =
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE7, 0x4C, 0x3C), 2),
            BorderFactory.createEmptyBorder(4, 6, 4, 6));

    public FactorizationChallengeView(AppController controller) {
        this.controller = controller;
        this.model = controller.getRsaModel();

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadLevel();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JButton homeButton = new JButton("← Accueil");
        homeButton.addActionListener(e -> controller.goHome());

        levelLabel = new JLabel("Niveau 1", SwingConstants.CENTER);
        levelLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        header.add(homeButton, BorderLayout.WEST);
        header.add(levelLabel, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel subtitle = new JLabel(
            "Challenge de factorisation en 2 nombres premiers", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        nLabel = new JLabel("N = ?", SwingConstants.CENTER);
        nLabel.setFont(new Font("Monospaced", Font.BOLD, 16));

        voirEnEntierButton = new JButton("Voir en entier");
        voirEnEntierButton.addActionListener(e -> onVoirEnEntierClicked());
        voirEnEntierButton.setVisible(false);

        JPanel nRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        nRow.add(nLabel);
        nRow.add(voirEnEntierButton);
        nRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        nRow.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JPanel inputsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        pField = new JTextField(10);
        qField = new JTextField(10);
        pField.setBorder(DEFAULT_FIELD_BORDER);
        qField.setBorder(DEFAULT_FIELD_BORDER);
        pField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        qField.setFont(new Font("Monospaced", Font.PLAIN, 16));

        inputsRow.add(new JLabel("p ="));
        inputsRow.add(pField);
        inputsRow.add(new JLabel("q ="));
        inputsRow.add(qField);
        inputsRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel(
            "Trouvez les deux nombres premiers p et q tels que p × q = N.",
            SwingConstants.CENTER);
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        giveUpButton = new JButton("Je ne sais pas...");
        giveUpButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        giveUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        giveUpButton.setMaximumSize(new Dimension(280, 44));
        giveUpButton.setPreferredSize(new Dimension(280, 44));
        giveUpButton.addActionListener(e -> onGiveUpClicked());
        giveUpButton.setVisible(false);

        center.add(subtitle);
        center.add(hint);
        center.add(nRow);
        center.add(inputsRow);
        center.add(giveUpButton);
        return center;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));

        JButton skipButton = new JButton("Passer →");
        skipButton.setToolTipText("Aller directement à la présentation du protocole RSA");
        skipButton.addActionListener(e -> controller.goToRsaProtocolPresentation());

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        actionButton = new JButton("Valider");
        actionButton.addActionListener(e -> onActionButtonClicked());

        JPanel left = new JPanel();
        left.add(skipButton);

        JPanel right = new JPanel();
        right.add(actionButton);

        footer.add(left, BorderLayout.WEST);
        footer.add(messageLabel, BorderLayout.CENTER);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void loadLevel() {
        BigInteger[] pull;
        BigInteger[] facteurs;
        switch (levelIndex) {
            case 0 -> {
                pull = toBigIntegerArray(model.getPull1());
                facteurs = toBigIntegerArray(model.getFactors1());
            }
            case 1 -> {
                pull = toBigIntegerArray(model.getPull2());
                facteurs = toBigIntegerArray(model.getFactors2());
            }
            default -> {
                pull = model.getPull3();
                facteurs = model.getFactors3();
            }
        }
        int index = model.chooseRandomInd(pull);
        currentN = pull[index];
        currentFactors = new BigInteger[]{facteurs[2 * index], facteurs[2 * index + 1]};

        levelLabel.setText("Niveau " + (levelIndex + 1) + " / 3");
        nLabel.setText("N = " + formatGrand(currentN));
        pField.setText("");
        qField.setText("");
        pField.setBorder(DEFAULT_FIELD_BORDER);
        qField.setBorder(DEFAULT_FIELD_BORDER);
        pField.setEnabled(true);
        qField.setEnabled(true);
        messageLabel.setText(" ");
        actionButton.setText("Valider");
        giveUpButton.setVisible(levelIndex == 2);
        giveUpButton.setEnabled(true);
        voirEnEntierButton.setVisible(levelIndex == 2);
        validated = false;
    }

    private BigInteger[] toBigIntegerArray(int[] valeurs) {
        BigInteger[] resultat = new BigInteger[valeurs.length];
        for (int i = 0; i < valeurs.length; i++) {
            resultat[i] = BigInteger.valueOf(valeurs[i]);
        }
        return resultat;
    }

    /** Tronque l'affichage des très grands nombres (niveau 3 : N fait ~617 chiffres). */
    private String formatGrand(BigInteger valeur) {
        String texte = valeur.toString();
        if (texte.length() <= 40) {
            return texte;
        }
        return texte.substring(0, 12) + "…(" + texte.length() + " chiffres)…"
            + texte.substring(texte.length() - 6);
    }

    private void onVoirEnEntierClicked() {
        JTextArea area = new JTextArea(currentN.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(480, 250));
        JOptionPane.showMessageDialog(this, scroll, "N en entier", JOptionPane.PLAIN_MESSAGE);
    }

    private void onActionButtonClicked() {
        if (!validated) {
            validateAnswer();
        } else {
            advance();
        }
    }

    private void validateAnswer() {
        BigInteger p = parseOrNull(pField.getText());
        BigInteger q = parseOrNull(qField.getText());

        boolean correct = p != null && q != null
            && p.multiply(q).equals(currentN)
            && ((p.equals(currentFactors[0]) && q.equals(currentFactors[1]))
                || (p.equals(currentFactors[1]) && q.equals(currentFactors[0])));

        pField.setEnabled(false);
        qField.setEnabled(false);
        giveUpButton.setEnabled(false);
        actionButton.setText(levelIndex < 2 ? "Suite" : "Suite (protocole RSA)");
        validated = true;

        if (correct) {
            messageLabel.setForeground(new Color(0x1E, 0x8E, 0x3E));
            messageLabel.setText("Bien joué !");
            pField.setBorder(DEFAULT_FIELD_BORDER);
            qField.setBorder(DEFAULT_FIELD_BORDER);
        } else {
            pField.setText(formatGrand(currentFactors[0]));
            qField.setText(formatGrand(currentFactors[1]));
            pField.setBorder(ERROR_FIELD_BORDER);
            qField.setBorder(ERROR_FIELD_BORDER);
            messageLabel.setForeground(new Color(0xE7, 0x4C, 0x3C));
            messageLabel.setText(
                "Il y a des erreurs. La solution était p = " + formatGrand(currentFactors[0])
                    + ", q = " + formatGrand(currentFactors[1]) + ".");
        }
    }

    private void onGiveUpClicked() {
        pField.setText(formatGrand(currentFactors[0]));
        qField.setText(formatGrand(currentFactors[1]));
        pField.setEnabled(false);
        qField.setEnabled(false);
        pField.setBorder(DEFAULT_FIELD_BORDER);
        qField.setBorder(DEFAULT_FIELD_BORDER);
        giveUpButton.setEnabled(false);
        actionButton.setText("Suite (protocole RSA)");
        validated = true;

        messageLabel.setForeground(new Color(0x29, 0x80, 0xB9));
        JOptionPane.showMessageDialog(
            this,
            "Solution : p = " + formatGrand(currentFactors[0])
                + ", q = " + formatGrand(currentFactors[1]) + ".\n\n"
                + "Avec le General Number Field Sieve (GNFS, le meilleur algorithme\n"
                + "classique connu pour factoriser de grands nombres), un module RSA\n"
                + "réel de 2048 bits prendrait environ 6 × 10¹⁴ années-cœur de calcul :\n"
                + "même avec un cluster d'un million de cœurs dédiés exclusivement à ce\n"
                + "calcul, ça prendrait encore environ 600 millions d'années.",
            "Solution et estimation",
            JOptionPane.INFORMATION_MESSAGE);
        messageLabel.setText("Solution révélée (voir la fenêtre).");
    }

    private void advance() {
        if (levelIndex < 2) {
            levelIndex++;
            loadLevel();
        } else {
            controller.goToRsaProtocolPresentation();
        }
    }

    private BigInteger parseOrNull(String text) {
        try {
            return new BigInteger(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
