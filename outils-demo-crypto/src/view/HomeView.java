package view;

import controller.AppController;

import javax.swing.*;
import java.awt.*;

/**
 * Page de lancement (accueil) : titre, et deux familles de cryptographie
 * (Pré-quantique -> RSA, Post-quantique -> Kyber).
 */
public class HomeView extends JPanel {

    public HomeView(AppController controller) {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        add(buildTitle(), BorderLayout.NORTH);
        add(buildCategoriesPanel(controller), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JComponent buildTitle() {
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Outils de démonstration pour la cryptographie", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Choisissez une famille de cryptographie", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        titlePanel.add(title);
        titlePanel.add(subtitle);
        return titlePanel;
    }

    private JComponent buildCategoriesPanel(AppController controller) {
        JPanel outer = new JPanel(new GridBagLayout());

        JPanel container = new JPanel(new GridLayout(1, 2, 40, 0));
        container.setBorder(BorderFactory.createTitledBorder("Cryptographie"));
        container.setPreferredSize(new Dimension(600, 260));

        container.add(buildCategoryCard(
            "Pré-quantique",
            "RSA",
            e -> controller.goToRsaModule()));

        container.add(buildCategoryCard(
            "Post-quantique",
            "Kyber",
            e -> controller.goToKyberModule()));

        outer.add(container);
        return outer;
    }

    private JPanel buildCategoryCard(String categoryName, String buttonLabel,
                                      java.awt.event.ActionListener onClick) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel categoryLabel = new JLabel(categoryName);
        categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        categoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton button = new JButton(buttonLabel);
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(160, 40));
        button.addActionListener(onClick);

        card.add(Box.createVerticalGlue());
        card.add(categoryLabel);
        card.add(Box.createVerticalStrut(15));
        card.add(button);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private JComponent buildFooter() {
        JLabel footer = new JLabel(
            "Réalisé par 3cut",
            SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footer.setForeground(Color.GRAY);
        return footer;
    }
}
