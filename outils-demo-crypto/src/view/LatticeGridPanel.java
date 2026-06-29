package view;

import javax.swing.*;
import java.awt.*;

/**
 * Panneau de dessin d'un petit réseau euclidien 2D : grille de repère,
 * points du réseau engendré par u et v, vecteurs de base, points A/B et
 * point "courant" (aperçu de la combinaison en cours de saisie).
 * Partagé par LatticeCombinationView et LatticeCvpView.
 */
class LatticeGridPanel extends JPanel {

    private static final int ECHELLE = 12; // pixels par unité
    private static final int PORTEE = 16;   // unités visibles de chaque côté du centre

    private int[] u;
    private int[] v;
    private int[] pointA;
    private int[] pointB;
    private int[] pointCourant;

    LatticeGridPanel() {
        setPreferredSize(new Dimension(420, 420));
        setBackground(Color.WHITE);
    }

    void configurer(int[] u, int[] v, int[] pointA, int[] pointB) {
        this.u = u;
        this.v = v;
        this.pointA = pointA;
        this.pointB = pointB;
        repaint();
    }

    void setPointCourant(int[] p) {
        this.pointCourant = p;
        repaint();
    }

    private int px(double x) {
        return getWidth() / 2 + (int) Math.round(x * ECHELLE);
    }

    private int py(double y) {
        return getHeight() / 2 - (int) Math.round(y * ECHELLE);
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
            // points du réseau engendré par u, v, visibles dans le cadre
            g2.setColor(new Color(0xB0, 0xC4, 0xDE));
            for (int a = -10; a <= 10; a++) {
                for (int b = -10; b <= 10; b++) {
                    int x = a * u[0] + b * v[0];
                    int y = a * u[1] + b * v[1];
                    if (Math.abs(x) <= PORTEE && Math.abs(y) <= PORTEE) {
                        g2.fillOval(px(x) - 3, py(y) - 3, 6, 6);
                    }
                }
            }

            dessinerVecteur(g2, 0, 0, u[0], u[1], new Color(0x27, 0xAE, 0x60), "u");
            dessinerVecteur(g2, 0, 0, v[0], v[1], new Color(0xE6, 0x7E, 0x22), "v");
        }

        if (pointA != null) {
            dessinerPoint(g2, pointA, Color.BLACK, "A");
        }
        if (pointB != null) {
            dessinerPoint(g2, pointB, new Color(0x80, 0x00, 0x80), "B");
        }
        if (pointCourant != null) {
            dessinerPoint(g2, pointCourant, new Color(0xE7, 0x4C, 0x3C), "");
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
