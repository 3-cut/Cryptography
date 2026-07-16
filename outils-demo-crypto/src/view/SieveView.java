package view;

import controller.AppController;
import model.RSAModel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SieveView extends JPanel {

    private final AppController controller;
    private final RSAModel model;
    private final boolean modeSelection;
    private final Map<Integer, JButton> buttonsByNumber = new LinkedHashMap<>();
    private final Map<Integer, Boolean> selection = new LinkedHashMap<>();
    private JLabel messageLabel;
    private JButton validerButton;

    private int totalNonPremiers;
    private int nonPremiersTraites = 0;
    private boolean termine = false;

    private static final Color COULEUR_PREMIER = new Color(0x27, 0xAE, 0x60);
    private static final Color COULEUR_SUPPRIME = new Color(0xD5, 0xD8, 0xDC);
    private static final Color COULEUR_SUCCES = new Color(0x1E, 0x8E, 0x3E);
    private static final Color COULEUR_ERREUR = new Color(0xE7, 0x4C, 0x3C);
    private static final Color COULEUR_BLEU = new Color(0x29, 0x80, 0xB9);
    private static final Color COULEUR_SELECTION = new Color(0xFD, 0xEB, 0xD0);

    public SieveView(AppController controller) {
        this.controller = controller;
        this.model = controller.getRsaModel();
        this.modeSelection = controller.isSieveModeSelection();
        model.genererCrible();
        totalNonPremiers = RSAModel.MAX_CRIBLE - model.getNombresPremiers().size();

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JButton homeButton = new JButton("← Accueil");
        homeButton.addActionListener(e -> controller.goHome());

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("RSA — Crible d'Ératosthène", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel(
            modeSelection
                ? "Sélectionnez les nombres que vous pensez premiers, puis validez."
                : "Trouvez les nombres premiers et supprimez leurs multiples.",
            SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBlock.add(title);
        titleBlock.add(subtitle);

        JButton infoButton = new JButton("i");
        infoButton.setToolTipText("Qu'est-ce qu'un nombre premier ?");
        infoButton.addActionListener(e -> JOptionPane.showMessageDialog(
            this,
            "Un nombre premier est un entier naturel admettant exactement\n"
                + "2 diviseurs : 1 et lui-même.",
            "Définition",
            JOptionPane.INFORMATION_MESSAGE
        ));

        JButton modeButton = new JButton("mode");
        modeButton.setToolTipText("Changer de mode de jeu");
        modeButton.addActionListener(e -> {
            controller.toggleSieveMode();
            controller.goToRsaModule();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.add(infoButton);
        right.add(modeButton);

        header.add(homeButton, BorderLayout.WEST);
        header.add(titleBlock, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildGrid() {
        int total = RSAModel.MAX_CRIBLE;
        int cols = 10;
        int rows = (int) Math.ceil(total / (double) cols);

        JPanel grid = new JPanel(new GridLayout(rows, cols, 6, 6));
        for (int nombre = 1; nombre <= RSAModel.MAX_CRIBLE; nombre++) {
            JButton button = new JButton(String.valueOf(nombre));
            button.setFont(new Font("Monospaced", Font.BOLD, 14));
            final int valeur = nombre;
            selection.put(valeur, false);
            if (modeSelection) {
                button.addActionListener(e -> onNumberToggled(valeur, button));
            } else {
                button.addActionListener(e -> onNumberClicked(valeur, button));
            }
            buttonsByNumber.put(nombre, button);
            grid.add(button);
        }
        return grid;
    }

    // ---- Mode "sélection" (nouveau, par défaut) --------------------------------

    private void onNumberToggled(int nombre, JButton button) {
        if (termine) {
            return;
        }
        boolean nouvelEtat = !selection.get(nombre);
        selection.put(nombre, nouvelEtat);
        if (nouvelEtat) {
            button.setOpaque(true);
            button.setBackground(COULEUR_SELECTION);
        } else {
            button.setOpaque(false);
            button.setBackground(null);
        }
    }

    private void onValiderClicked() {
        if (termine) {
            controller.goToFactorizationChallenge();
            return;
        }
        termine = true;

        boolean parfait = true;
        for (int nombre = 1; nombre <= RSAModel.MAX_CRIBLE; nombre++) {
            JButton button = buttonsByNumber.get(nombre);
            boolean choisi = selection.get(nombre);
            boolean premier = nombre != 1 && model.estPremier(nombre);

            button.setEnabled(false);
            button.setOpaque(true);
            button.setBorderPainted(false);

            if (choisi && premier) {
                button.setBackground(COULEUR_PREMIER);
                button.setText(htmlBlanc(nombre));
            } else if (choisi && !premier) {
                button.setBackground(COULEUR_ERREUR);
                button.setText(htmlBlanc(nombre));
                parfait = false;
            } else if (!choisi && premier) {
                button.setBackground(COULEUR_BLEU);
                button.setText(htmlBlanc(nombre));
                parfait = false;
            } else {
                button.setOpaque(false);
                button.setBackground(null);
                button.setForeground(Color.BLACK);
            }
        }

        if (parfait) {
            messageLabel.setForeground(COULEUR_SUCCES);
            messageLabel.setText("Bien joué ! Tous les nombres premiers ont été correctement sélectionnés.");
        } else {
            messageLabel.setForeground(COULEUR_ERREUR);
            messageLabel.setText(
                "Il y a des erreurs : vert = premier sélectionné, rouge = non premier sélectionné, "
                    + "bleu = premier non sélectionné.");
        }
        validerButton.setText("Suite");
    }

    // ---- Mode "crible classique" (cliquer sur un premier supprime ses multiples) ----

    private void onNumberClicked(int nombre, JButton button) {
        if (!button.isEnabled() || termine) {
            return;
        }

        if (nombre == 1) {
            marquerSupprime(button, " ");
            messageLabel.setForeground(Color.DARK_GRAY);
            messageLabel.setText("1 n'est ni premier ni composé : il est retiré du crible.");
            nonPremiersTraites++;
        } else if (model.estPremier(nombre)) {
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setBackground(COULEUR_PREMIER);
            button.setText(htmlBlanc(nombre));
            button.setEnabled(false);
            griserMultiplesDe(nombre);
            messageLabel.setForeground(COULEUR_PREMIER);
            messageLabel.setText(nombre + " est un nombre premier : ses multiples sont supprimés.");
        } else {
            marquerSupprime(button, String.valueOf(nombre));
            messageLabel.setForeground(Color.DARK_GRAY);
            messageLabel.setText(nombre + " est un multiple d'un nombre premier : supprimé du crible.");
            nonPremiersTraites++;
        }

        verifierFin();
    }

    private void griserMultiplesDe(int premier) {
        for (int multiple = 2 * premier; multiple <= RSAModel.MAX_CRIBLE; multiple += premier) {
            JButton boutonMultiple = buttonsByNumber.get(multiple);
            if (boutonMultiple != null && boutonMultiple.isEnabled()) {
                marquerSupprime(boutonMultiple, String.valueOf(multiple));
                nonPremiersTraites++;
            }
        }
    }

    /** HTML force le blanc même quand le bouton est désactivé (sinon le L&F le grise). */
    private String htmlBlanc(int nombre) {
        return "<html><font color='white'>" + nombre + "</font></html>";
    }

    private void marquerSupprime(JButton button, String numeroAffiche) {
        button.setText("<html><strike>" + numeroAffiche + "</strike></html>");
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setBackground(COULEUR_SUPPRIME);
        button.setForeground(Color.GRAY);
        button.setEnabled(false);
    }

    private void verifierFin() {
        if (termine || nonPremiersTraites < totalNonPremiers) {
            return;
        }
        termine = true;

        messageLabel.setForeground(COULEUR_SUCCES);
        messageLabel.setText(
            "Bravo, vous avez terminé le crible ! Tous les nombres non premiers sont supprimés.");

        Timer timer = new Timer(1800, e -> controller.goToFactorizationChallenge());
        timer.setRepeats(false);
        timer.start();
    }

    // ---- Pied de page commun ----------------------------------------------------

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));

        JButton skipButton = new JButton("Passer →");
        skipButton.setToolTipText("Aller directement au challenge de factorisation");
        skipButton.addActionListener(e -> controller.goToFactorizationChallenge());

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel left = new JPanel();
        left.add(skipButton);

        footer.add(left, BorderLayout.WEST);
        footer.add(messageLabel, BorderLayout.CENTER);

        if (modeSelection) {
            validerButton = new JButton("Valider");
            validerButton.addActionListener(e -> onValiderClicked());
            JPanel right = new JPanel();
            right.add(validerButton);
            footer.add(right, BorderLayout.EAST);
        }

        return footer;
    }
}
