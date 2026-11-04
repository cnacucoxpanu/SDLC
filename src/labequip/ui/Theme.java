package labequip.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/** Единое оформление окон: шрифт, цвета, фабрики подписей и кнопок. */
public final class Theme {
    public static final String FONT = Font.SANS_SERIF;
    static final Color ACCENT = new Color(0x2F6DB5);
    static final Color MUTED = new Color(0x6B7480);
    static final Color ERROR = new Color(0xC0392B);
    static final Color OK = new Color(0x2E8B57);

    private Theme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // остаётся оформление Swing по умолчанию
        }
        Font base = new Font(FONT, Font.PLAIN, 13);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "CheckBox.font", "Table.font", "TableHeader.font", "TabbedPane.font", "Spinner.font"}) {
            UIManager.put(key, base);
        }
    }

    static JLabel title(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.BOLD, size));
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    static JLabel errorLabel() {
        JLabel l = new JLabel(" ");
        l.setForeground(ERROR);
        return l;
    }

    static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        if (primary) {
            b.setFont(new Font(FONT, Font.BOLD, 13));
            b.setForeground(ACCENT);
        }
        return b;
    }
}
