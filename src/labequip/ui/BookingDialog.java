package labequip.ui;

import labequip.model.Equipment;
import labequip.model.User;
import labequip.service.AppContext;
import labequip.service.BookingService;
import labequip.service.BusinessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Форма бронирования оборудования студентом (REQ-FUNC-006). */
public class BookingDialog extends JDialog {
    final JSpinner from = dateSpinner(null);
    final JSpinner to = dateSpinner(null);
    final JLabel error = Theme.errorLabel();
    final JButton book = Theme.button("Забронировать", true);
    private final AppContext app;
    private final User student;
    private final Equipment equipment;
    private boolean booked;

    public BookingDialog(Frame owner, AppContext app, User student, Equipment equipment, LocalDate defaultFrom) {
        super(owner, "Бронирование оборудования", true);
        this.app = app;
        this.student = student;
        this.equipment = equipment;
        from.setValue(toDate(defaultFrom));
        to.setValue(toDate(defaultFrom));

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 24, 16, 24));
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 0, 4, 12);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        p.add(Theme.title(equipment.name(), 15), g);
        g.gridy = 1;
        p.add(Theme.muted(equipment.invNumber() + " · " + equipment.category() + " · " + equipment.room()), g);
        g.gridwidth = 1;
        g.gridy = 2;
        g.insets = new Insets(16, 0, 4, 12);
        p.add(new JLabel("Дата начала"), g);
        g.gridx = 1;
        p.add(from, g);
        g.gridx = 0;
        g.gridy = 3;
        g.insets = new Insets(4, 0, 4, 12);
        p.add(new JLabel("Дата окончания (включительно)"), g);
        g.gridx = 1;
        p.add(to, g);
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        p.add(Theme.muted("Не более " + BookingService.MAX_DAYS + " дней; не более "
                + BookingService.MAX_ACTUAL_PER_STUDENT + " действующих броней на студента"), g);
        g.gridy = 5;
        p.add(error, g);
        JButton cancel = Theme.button("Отмена", false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(book);
        g.gridy = 6;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(buttons, g);

        setContentPane(p);
        getRootPane().setDefaultButton(book);
        book.addActionListener(e -> onBook());
        cancel.addActionListener(e -> dispose());
        pack();
        setMinimumSize(new Dimension(500, getHeight()));
        setLocationRelativeTo(owner);
    }

    static JSpinner dateSpinner(Date d) {
        JSpinner s = new JSpinner(new SpinnerDateModel(d == null ? new Date() : d, null, null, java.util.Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd.MM.yyyy"));
        s.setPreferredSize(new Dimension(150, 30));
        return s;
    }

    static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    static LocalDate toLocal(Object v) {
        return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    void setDates(LocalDate f, LocalDate t) {
        from.setValue(toDate(f));
        to.setValue(toDate(t));
    }

    void onBook() {
        try {
            app.bookings.book(student, equipment.id(), toLocal(from.getValue()), toLocal(to.getValue()));
            booked = true;
            dispose();
        } catch (BusinessException ex) {
            error.setText(ex.getMessage());
        }
    }

    public boolean isBooked() {
        return booked;
    }
}
