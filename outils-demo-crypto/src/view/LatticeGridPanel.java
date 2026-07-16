package view;

import javax.swing.*;
import java.awt.*;

/**
 * Panneau de dessin d'un petit réseau euclidien 2D : grille de repère,
 * vecteurs de base u et v, points A/B et point "courant" (aperçu de la
 * combinaison en cours de saisie).
 * Partagé par LatticeCombinationView, LatticeCvpView et
 * LatticeStandardBasisView.
 */
class LatticeGridPanel extends JPanel {

    private static final int PORTEE = 16; // unités visibles de chaque côté du centre

    private int[] u;
    private int[] v;
    private int[] pointA;
    private double[] pointADouble; // pour la cible CVP à coordonnées flottantes
    private int[] pointB;
    private int[] pointCourant;

    LatticeGridPanel() {
        setPreferredSize(new Dimension(750, 750));
        setBackground(Color.WHITE);
    }

    void configurer(int[] u, int[] v, int[] pointA, int[] pointB) {
        this.u = u;
        this.v = v;
        this.pointA = pointA;
        this.pointADouble = null;
        this.pointB = pointB;
        repaint();
    }

    void configurerCvp(int[] u, int[] v, double[] pointADouble) {
        this.u = u;
        this.v = v;
        this.pointA = null;
        this.pointADouble = pointADouble;
        this.pointB = null;
        repaint();
    }

    void setPointCourant(int[] p) {
        this.pointCourant = p;
        repaint();
    }

    /** Échelle recalculée à chaque dessin : le contenu remplit toujours tout le panneau. */
    private double echelle() {
        int cote = Math.min(getWidth(), getHeight());
        return cote / (double) (2 * PORTEE + 2);
    }

    private int px(double x) {
        return getWidth() / 2 + (int) Math.round(x * echelle());
    }

    private int py(double y) {
        return getHeight() / 2 - (int) Math.round(y * echelle());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // grille de repère
        g2.setColor(new Color(0xEE, 0xEE, 0xEE));
        for (int i = -PORTEE; i <= PORTEE; i++) {
            g2.drawLine(px(i), py(-PORTEE), px(i), py(PORTEE));
            g2.drawLine(px(-PORTEE), py(i), px(PORTEE), py(i));
        }

        // axes
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(px(-PORTEE), py(0), px(PORTEE), py(0));
        g2.drawLine(px(0), py(-PORTEE), px(0), py(PORTEE));

        if (u != null && v != null) {
            dessinerVecteur(g2, 0, 0, u[0], u[1], new Color(0x27, 0xAE, 0x60), "u");
            dessinerVecteur(g2, 0, 0, v[0], v[1], new Color(0xE6, 0x7E, 0x22), "v");
        }

        if (pointA != null) {
            dessinerPoint(g2, pointA, Color.BLACK, "A");
        }
        if (pointADouble != null) {
            dessinerPointDouble(g2, pointADouble, Color.BLACK, "A");
        }
        if (pointB != null) {
            dessinerPoint(g2, pointB, new Color(0x80, 0x00, 0x80), "B");
        }
        if (pointCourant != null) {
            dessinerPoint(g2, pointCourant, new Color(0xE7, 0x4C, 0x3C), "");
        }
    }

    private void dessinerPointDouble(Graphics2D g2, double[] p, Color couleur, String etiquette) {
        g2.setColor(couleur);
        int x = px(p[0]);
        int y = py(p[1]);
        g2.fillOval(x - 5, y - 5, 10, 10);
        if (!etiquette.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString(etiquette, x + 8, y - 8);
        }
    }

    private void dessinerVecteur(Graphics2D g2, int x0, int y0, int x1, int y1, Color couleur, String etiquette) {
        g2.setColor(couleur);
        g2.setStroke(new BasicStroke(2.2f));
        int xa = px(x0);
        int ya = py(y0);
        int xb = px(x1);
        int yb = py(y1);
        g2.drawLine(xa, ya, xb, yb);

        double angle = Math.atan2(yb - ya, xb - xa);
        int taille = 8;
        g2.drawLine(xb, yb,
            (int) (xb - taille * Math.cos(angle - Math.PI / 6)),
            (int) (yb - taille * Math.sin(angle - Math.PI / 6)));
        g2.drawLine(xb, yb,
            (int) (xb - taille * Math.cos(angle + Math.PI / 6)),
            (int) (yb - taille * Math.sin(angle + Math.PI / 6)));

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(etiquette, xb + 4, yb - 4);
    }

    private void dessinerPoint(Graphics2D g2, int[] p, Color couleur, String etiquette) {
        g2.setColor(couleur);
        int x = px(p[0]);
        int y = py(p[1]);
        g2.fillOval(x - 5, y - 5, 10, 10);
        if (!etiquette.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString(etiquette, x + 8, y - 8);
        }
    }
}
