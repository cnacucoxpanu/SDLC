import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InputDialog extends JDialog {
    private final JTextField txtDay = new JTextField(5);
    private final JTextField txtMonth = new JTextField(5);
    private final JTextField txtYear = new JTextField(7);

    public InputDialog(JFrame parent, PopulationController controller) {
        super(parent, "Ввод даты", true);
        setSize(350, 220);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(4, 2, 10, 10));

        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(new JLabel(" День (1-31):"));
        add(txtDay);
        add(new JLabel(" Месяц (1-12):"));
        add(txtMonth);
        add(new JLabel(" Год (1800-2100):"));
        add(txtYear);

        JButton btnSubmit = new JButton("OK");
        add(new JLabel());
        add(btnSubmit);

        // Создаем один общий обработчик нажатия
        ActionListener submitAction = e -> controller.processInput(
                txtDay.getText(), txtMonth.getText(), txtYear.getText()
        );

        // Привязываем его к кнопке OK
        btnSubmit.addActionListener(submitAction);

        // Привязываем его к каждому текстовому полю. 
        // В JTextField нажатие Enter вызывает actionPerformed, что идеально нам подходит.
        txtDay.addActionListener(submitAction);
        txtMonth.addActionListener(submitAction);
        txtYear.addActionListener(submitAction);
    }

    public void setValues(int day, int month, int year) {
        if (year != 0) {
            txtDay.setText(String.valueOf(day));
            txtMonth.setText(String.valueOf(month));
            txtYear.setText(String.valueOf(year));
        }
    }
}
