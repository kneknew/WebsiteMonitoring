package websitemonitoring;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo FlatLaf");
        }
        SwingUtilities.invokeLater(UiMain::createAndShowGui);
    }
}
