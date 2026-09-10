import javax.swing.*;

public class PopulationController {
    private final PopulationModel model;
    private InputDialog inputDialog;

    public PopulationController(PopulationModel model) {
        this.model = model;
    }

    public void openInputDialog(JFrame parent) {
        if (inputDialog == null || !inputDialog.isDisplayable()) {
            inputDialog = new InputDialog(parent, this);
        }
        // Восстановление последних данных из Модели
        inputDialog.setValues(model.getDay(), model.getMonth(), model.getYear());
        inputDialog.setVisible(true);
    }

    // Метод обработки ввода - сердце Контроллера
    public void processInput(String dayStr, String monthStr, String yearStr) {
        try {
            // 1. Парсинг (преобразование строк в числа)
            int d = Integer.parseInt(dayStr.trim());
            int m = Integer.parseInt(monthStr.trim());
            int y = Integer.parseInt(yearStr.trim());

            // 2. Передача данных в Модель. 
            // Модель сама проверит валидность и обновит View (активная модель)
            model.setData(d, m, y);
            
            if (inputDialog != null) {
                inputDialog.dispose();
            }
        } catch (NumberFormatException e) {
            showError("Ошибка: Пожалуйста, введите целые числа!");
        } catch (IllegalArgumentException e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
    }
}
