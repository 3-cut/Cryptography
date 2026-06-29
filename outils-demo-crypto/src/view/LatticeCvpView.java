package view;

import controller.AppController;
import model.LatticeModel;

import javax.swing.*;
import java.awt.*;

/**
 * Exercice 2 (réseaux euclidiens, avant Kyber) : on part du point A (qui
 * appartient au réseau, comme dans l'exercice 1) et on cherche à s'approcher
 * le plus possible du point B -- qui n'appartient pas forcément au réseau.
 * C'est le problème du vecteur le plus proche (Closest Vector Problem, CVP).
 * En 2D on peut chercher la solution par force brute
 * (LatticeModel.closestCombination) ; en grande dimension ce problème
 * devient intraitable, ce qui fonde la sécurité de Kyber.
 */
public class LatticeCvpView extends JPanel {

    private static final Color VERT = new Color(0x1E, 0x8E, 0x3E);
    private static final Color ROUGE = new Color(0xE7, 0x4C, 0x3C);

    private final AppController controller;
    private LatticeModel lattice;
    private int[] pointA; // sur le réseau (départ)
    private int[] pointB; // hors réseau (cible à approcher)
    private int solutionA;
    private int solutionB;
    private boolean validated = false;

    private LatticeGridPanel grille;
    private JSpinner spinnerA;
    private JSpinner spinnerB;
    private JLabel messageLabel;
    private JButton actionButton;

    public LatticeCvpView(AppController controller) {
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
        // Même base "longue et proche" que l'exercice 1, pour la continuité.
        lattice = new LatticeModel(new int[]{8, 3}, new int[]{7, 3});

        pointA = new int[]{0, 0}; // l'origine, sur le réseau
        pointB = new int[]{10, 4}; // hors réseau (pas atteignable exactement)

        int[] solution = lattice.closestCombination(pointA, pointB, -8, 8);
        solutionA = solution[0];
        solutionB = solution[1];
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JButton homeButton = new JButton("← Accueil");
        homeButton.addActionListener(e -> controller.goHome());

        JLabel title = new JLabel("Kyber — Réseaux euclidiens (2/2 : CVP)", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        header.add(homeButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout(15, 15));

        JLabel but = new JLabel(
            "<html>But : trouver la plus petite combinaison linéaire pour s'approcher<br>"
                + "du point B, en partant du point A (\"Closest Vector Problem\", C.V.P.).</html>");
        but.setFont(new Font("SansSerif", Font.PLAIN, 13));
        but.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        grille = new LatticeGridPanel();
        grille.configurer(lattice.u, lattice.v, pointA, pointB);

        JPanel droite = new JPanel();
        droite.setLayout(new BoxLayout(droite, BoxLayout.Y_AXIS));
        droite.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        droite.add(ligne("u = (" + lattice.u[0] + ", " + lattice.u[1] + ")"));
        droite.add(ligne("v = (" + lattice.v[0] + ", " + lattice.v[1] + ")"));
        droite.add(Box.createVerticalStrut(10));
        droite.add(ligne("A = (" + pointA[0] + ", " + pointA[1] + ")  (sur le réseau)"));
        droite.add(ligne("B = (" + pointB[0] + ", " + pointB[1] + ")  (hors réseau)"));
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

        JLabel formule = new JLabel("A + a·u + b·v = ?  (point rouge)");
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
        grille.setPointCourant(lattice.combine(pointA, a, b));
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));

        JButton skipButton = new JButton("Passer →");
        skipButton.setToolTipText("Aller directement à la présentation du protocole Kyber");
        skipButton.addActionListener(e -> controller.goToKyberProtocolPresentation());

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
            int[] proposition = lattice.combine(pointA, a, b);
            double distanceProposition = lattice.distance(proposition, pointB);

            int[] meilleur = lattice.combine(pointA, solutionA, solutionB);
            double distanceOptimale = lattice.distance(meilleur, pointB);

            validated = true;
            spinnerA.setEnabled(false);
            spinnerB.setEnabled(false);
            actionButton.setText("Suite (protocole Kyber)");

            if (Math.abs(distanceProposition - distanceOptimale) < 1e-9) {
                messageLabel.setForeground(VERT);
                messageLabel.setText(String.format(
                    "Bien joué ! C'est le vecteur le plus proche (distance %.2f).", distanceOptimale));
            } else {
                spinnerA.setValue(solutionA);
                spinnerB.setValue(solutionB);
                mettreAJourApercu();
                messageLabel.setForeground(ROUGE);
                messageLabel.setText(String.format(
                    "Pas optimal (distance %.2f). Le plus proche : a=%d, b=%d (distance %.2f).",
                    distanceProposition, solutionA, solutionB, distanceOptimale));
            }
        } else {
            controller.goToKyberProtocolPresentation();
        }
    }
}
