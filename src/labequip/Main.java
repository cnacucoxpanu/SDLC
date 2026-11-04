package labequip;

import labequip.model.User;
import labequip.service.AppContext;
import labequip.ui.LoginFrame;
import labequip.ui.MainFrame;
import labequip.ui.Theme;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.LocalDate;

/** Точка входа: открывает окно входа, после успешного входа — главное окно (REQ-INST-001). */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            AppContext app = new AppContext(Path.of("data"), LocalDate::now);
            showLogin(app);
        });
    }

    private static void showLogin(AppContext app) {
        new LoginFrame(app, (User user) -> new MainFrame(app, user, () -> showLogin(app)).setVisible(true))
                .setVisible(true);
    }
}
