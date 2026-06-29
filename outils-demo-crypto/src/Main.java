import controller.AppController;
import view.MainFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppController controller = new AppController();
            MainFrame frame = new MainFrame(controller);
            controller.setMainFrame(frame);
            frame.setVisible(true);
        });
    }
}
