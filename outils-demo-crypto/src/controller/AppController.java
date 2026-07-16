package controller;

import model.KyberModel;
import model.RSAModel;
import view.MainFrame;

public class AppController {

    private final RSAModel rsaModel = new RSAModel();
    private final KyberModel kyberModel = new KyberModel();
    private MainFrame mainFrame;
    private boolean sieveModeSelection = true;

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public RSAModel getRsaModel() {
        return rsaModel;
    }

    public KyberModel getKyberModel() {
        return kyberModel;
    }

    public boolean isSieveModeSelection() {
        return sieveModeSelection;
    }

    public void toggleSieveMode() {
        sieveModeSelection = !sieveModeSelection;
    }

    // ---- Navigation ---------------------------------------------------------

    public void goHome() {
        mainFrame.showScreen(MainFrame.SCREEN_HOME);
    }

    public void goToRsaModule() {
        mainFrame.showScreen(MainFrame.SCREEN_SIEVE);
    }

    public void goToFactorizationChallenge() {
        mainFrame.showScreen(MainFrame.SCREEN_FACTORIZATION);
    }

    public void goToRsaProtocolPresentation() {
        mainFrame.showScreen(MainFrame.SCREEN_RSA_PROTOCOL);
    }

    public void goToKyberModule() {
        mainFrame.showScreen(MainFrame.SCREEN_LATTICE_STANDARD);
    }

    public void goToLatticeCombination() {
        mainFrame.showScreen(MainFrame.SCREEN_LATTICE_COMBINATION);
    }

    public void goToLatticeCvp() {
        mainFrame.showScreen(MainFrame.SCREEN_LATTICE_CVP);
    }

    public void goToKyberProtocolPresentation() {
        mainFrame.showScreen(MainFrame.SCREEN_KYBER_PROTOCOL);
    }
}
