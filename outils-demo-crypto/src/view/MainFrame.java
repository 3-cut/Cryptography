package view;

import controller.AppController;

import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre principale : contient un CardLayout dans lequel on affiche un seul
 * écran (View) à la fois. À chaque navigation, l'ancien panel est retiré et
 * un panel tout neuf est créé pour l'écran cible (aucun état conservé).
 */
public class MainFrame extends JFrame {

    public static final String SCREEN_HOME = "HOME";
    public static final String SCREEN_SIEVE = "SIEVE";
    public static final String SCREEN_FACTORIZATION = "FACTORIZATION";
    public static final String SCREEN_RSA_PROTOCOL = "RSA_PROTOCOL";
    public static final String SCREEN_LATTICE_STANDARD = "LATTICE_STANDARD";
    public static final String SCREEN_LATTICE_COMBINATION = "LATTICE_COMBINATION";
    public static final String SCREEN_LATTICE_CVP = "LATTICE_CVP";
    public static final String SCREEN_KYBER_PROTOCOL = "KYBER_PROTOCOL";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);
    private final AppController controller;

    public MainFrame(AppController controller) {
        super("Outils de démonstration pour la cryptographie");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);

        setContentPane(cardsPanel);
        showScreen(SCREEN_HOME);
    }

    /**
     * Affiche l'écran demandé en créant une instance fraîche de la View
     * correspondante (aucun état conservé entre deux passages sur le même
     * écran).
     */
    public void showScreen(String screenName) {
        JPanel panel = switch (screenName) {
            case SCREEN_HOME -> new HomeView(controller);
            case SCREEN_SIEVE -> new SieveView(controller);
            case SCREEN_FACTORIZATION -> new FactorizationChallengeView(controller);
            case SCREEN_RSA_PROTOCOL -> new RsaProtocolView(controller);
            case SCREEN_LATTICE_STANDARD -> new LatticeStandardBasisView(controller);
            case SCREEN_LATTICE_COMBINATION -> new LatticeCombinationView(controller);
            case SCREEN_LATTICE_CVP -> new LatticeCvpView(controller);
            case SCREEN_KYBER_PROTOCOL -> new KyberProtocolView(controller);
            default -> placeholder("Écran inconnu : " + screenName, "Retour à l'accueil");
        };

        cardsPanel.removeAll();
        cardsPanel.add(panel, screenName);
        cardLayout.show(cardsPanel, screenName);
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    /** Écran temporaire pour les pages pas encore implémentées. */
    private JPanel placeholder(String message, String backButtonLabel) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 18));

        JButton backButton = new JButton(backButtonLabel);
        backButton.addActionListener(e -> controller.goHome());

        JPanel southPanel = new JPanel();
        southPanel.add(backButton);

        panel.add(label, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }
}
