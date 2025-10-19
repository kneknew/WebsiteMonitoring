package websitemonitoring;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws UnsupportedLookAndFeelException {

        UIManager.setLookAndFeel(new FlatLightLaf());
        SwingUtilities.invokeLater(UiMain::createAndShowGui);
    }
}
