package labequip.ui;

import labequip.model.User;
import labequip.service.AppContext;
import labequip.service.BusinessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Окно входа в систему (REQ-FUNC-001). */
public class LoginFrame extends JFrame {
    final JTextField login = new JTextField(22);
    final JPasswordField password = new JPasswordField(22);
    final JLabel error = Theme.errorLabel();
    private final AppContext app;
    private final Consumer<User> onSuccess;

    public LoginFrame(AppContext app, Consumer<User> onSuccess) {
        super("Учет лабораторного оборудования — вход");
        this.app = app;
        this.onSuccess = onSuccess;

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 0, 5, 12);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        p.add(Theme.title("Учет лабораторного оборудования", 18), g);
        g.gridy = 1;
        p.add(Theme.muted("Войдите под своей учётной записью"), g);
        g.gridwidth = 1;
        g.gridy = 2;
        p.add(new JLabel("Логин"), g);
        g.gridx = 1;
        p.add(login, g);
        g.gridx = 0;
        g.gridy = 3;
        p.add(new JLabel("Пароль"), g);
        g.gridx = 1;
        p.add(password, g);
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        p.add(error, g);
        JButton enter = Theme.button("Войти", true);
        g.gridy = 5;
        g.anchor = GridBagConstraints.EAST;
        p.add(enter, g);

        setContentPane(p);
        getRootPane().setDefaultButton(enter);
        enter.addActionListener(e -> submit());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    void submit() {
        try {
            User user = app.auth.login(login.getText(), new String(password.getPassword()));
            dispose();
            onSuccess.accept(user);
        } catch (BusinessException ex) {
            error.setText(ex.getMessage());
            password.setText("");
        }
    }
}
