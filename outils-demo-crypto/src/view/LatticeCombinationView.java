package view;

import controller.AppController;
import model.LatticeModel;

import javax.swing.*;
import java.awt.*;

/**
 * Exercice 2 (réseaux euclidiens, avant Kyber) : trouver la combinaison
 * linéaire entière a·u + b·v permettant d'aller exactement du point A au
 * point B, tous deux appartenant au réseau engendré par u et v.
 * Même comportement "un seul clic sur Valider" que le challenge de
 * factorisation RSA : juste ou faux, le bouton devient "Suite", et en cas
 * d'erreur la solution est révélée.
 */
public class LatticeCombinationView extends JPanel {

    private static final Color VERT = new Color(0x1E, 0x8E, 0x3E);
    private static final Color ROUGE = new Color(0xE7, 0x4C, 0x3C);

    private final AppController controller;
    private LatticeModel lattice;
    private int[] pointA;
    private int[] pointB;
    private int solutionA;
    private int solutionB;
    private boolean validated = false;

    private LatticeGridPanel grille;
    private JSpinner spinnerA;
    private JSpinner spinnerB;
    private JLabel messageLabel;
    private JButton actionButton;

    public LatticeCombinationView(AppController controller) {
        this.controller = controller;
        genererExercice();

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        mettreAJourApercu();
    }

    private void genererExercice() {
        lattice = new LatticeModel(new int[]{8, 3}, new int[]{7, 3});
        // Départ : origine (0,0). Cible A = 2*u + (-1)*v = (9, 3).
        solutionA = 2;
        solutionB = -1;
        pointA = new int[]{solutionA * lattice.u[0] + solutionB * lattice.v[0],
                           solutionA * lattice.u[1] + solutionB * lattice.v[1]};
        pointB = null;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JButton homeButton = new JButton("← Accueil");
        homeButton.addActionListener(e -> controller.goHome());

        JLabel title = new JLabel("Kyber — Réseaux euclidiens (2/3)", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        header.add(homeButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout(15, 15));

        JLabel but = new JLabel(
            "<html>But : trouver la combinaison linéaire (entière) pour aller<br>"
                + "de l'origine au point A, à l'aide des vecteurs u et v.</html>");
        but.setFont(new Font("SansSerif", Font.PLAIN, 13));
        but.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        grille = new LatticeGridPanel();
        grille.configurer(lattice.u, lattice.v, pointA, null);

        JPanel droite = new JPanel();
        droite.setLayout(new BoxLayout(droite, BoxLayout.Y_AXIS));
        droite.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        droite.add(ligne("u = (" + lattice.u[0] + ", " + lattice.u[1] + ")"));
        droite.add(ligne("v = (" + lattice.v[0] + ", " + lattice.v[1] + ")"));
        droite.add(Box.createVerticalStrut(10));
        droite.add(ligne("Origine = (0, 0)  (départ)"));
        droite.add(ligne("A = (" + pointA[0] + ", " + pointA[1] + ")  (cible)"));
        droite.add(Box.createVerticalStrut(15));

        JPanel coeffRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        spinnerA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        spinnerB = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        spinnerA.addChangeListener(e -> mettreAJourApercu());
        spinnerB.addChangeListener(e -> mettreAJourApercu());
        coeffRow.add(new JLabel("a ="));
        coeffRow.add(spinnerA);
        coeffRow.add(new JLabel("b ="));
        coeffRow.add(spinnerB);
        coeffRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        droite.add(coeffRow);

        JLabel formule = new JLabel("(0,0) + a·u + b·v = ?  (point rouge)");
        formule.setFont(new Font("Monospaced", Font.PLAIN, 12));
        formule.setForeground(Color.GRAY);
        formule.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        droite.add(formule);

        center.add(but, BorderLayout.NORTH);
        center.add(grille, BorderLayout.CENTER);
        center.add(droite, BorderLayout.EAST);
        return center;
    }

    private JComponent ligne(String texte) {
        JLabel label = new JLabel(texte);
        label.setFont(new Font("Monospaced", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void mettreAJourApercu() {
        int a = (Integer) spinnerA.getValue();
        int b = (Integer) spinnerB.getValue();
        grille.setPointCourant(lattice.combine(new int[]{0, 0}, a, b));
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));

        JButton skipButton = new JButton("Passer →");
        skipButton.setToolTipText("Aller directement au troisième exercice (CVP)");
        skipButton.addActionListener(e -> controller.goToLatticeCvp());

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

    private void onActionButtonClicked() {
        if (!validated) {
            int a = (Integer) spinnerA.getValue();
            int b = (Integer) spinnerB.getValue();
            boolean correct = (a == solutionA && b == solutionB);

            validated = true;
            spinnerA.setEnabled(false);
            spinnerB.setEnabled(false);
            actionButton.setText("Suite");

            if (correct) {
                messageLabel.setForeground(VERT);
                messageLabel.setText("Bien joué !");
            } else {
                spinnerA.setValue(solutionA);
                spinnerB.setValue(solutionB);
                mettreAJourApercu();
                messageLabel.setForeground(ROUGE);
                messageLabel.setText(
                    "Il y a des erreurs. La solution était a = " + solutionA + ", b = " + solutionB + ".");
            }
        } else {
            controller.goToLatticeCvp();
        }
    }
}
