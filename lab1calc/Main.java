import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Устанавливаем системный вид интерфейса (чтобы не было серого стиля Java 90-х)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            PopulationModel model = new PopulationModel();
            PopulationController controller = new PopulationController(model);
            MainView mainView = new MainView(controller, model);
            mainView.setVisible(true);
        });
    }
}
