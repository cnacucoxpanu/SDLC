package labequip.ui;

import labequip.model.*;
import labequip.service.AppContext;
import labequip.service.BusinessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Главное окно: каталог оборудования и бронирования; набор действий зависит от роли (REQ-FUNC-002 — 007). */
public class MainFrame extends JFrame {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int STATUS_COLUMN = 4;

    final JTabbedPane tabs = new JTabbedPane();
    final JTable equipmentTable;
    final JTable bookingTable;
    final JComboBox<Object> statusFilter = new JComboBox<>(new Object[]{"Все статусы", Status.FREE, Status.IN_REPAIR});
    final JCheckBox onlyActual = new JCheckBox("Только действующие", true);

    private final JTextField search = new JTextField(24);
    private final JLabel statusBar = Theme.muted(" ");
    private final JButton add = Theme.button("Добавить", false);
    private final JButton edit = Theme.button("Изменить", false);
    private final JButton toggleStatus = Theme.button("Перевести в ремонт", false);
    private final JButton delete = Theme.button("Удалить", false);
    private final JButton book = Theme.button("Забронировать", true);
    private final JButton cancelBooking = Theme.button("Отменить бронь", false);
    private final DefaultTableModel equipmentModel = readOnlyModel(
            "Инв. номер", "Наименование", "Категория", "Помещение", "Статус");
    private final DefaultTableModel bookingModel = readOnlyModel(
            "Инв. номер", "Оборудование", "Студент", "С", "По", "Статус", "Примечание");
    private final AppContext app;
    private final User user;
    private final Runnable onLogout;
    private List<Equipment> shownEquipment = List.of();
    private List<BookingView> shownBookings = List.of();

    public MainFrame(AppContext app, User user, Runnable onLogout) {
        super("Учет лабораторного оборудования — " + user.fullName() + " (" + user.role().title() + ")");
        this.app = app;
        this.user = user;
        this.onLogout = onLogout;
        equipmentTable = table(equipmentModel);
        bookingTable = table(bookingModel);
        equipmentTable.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusRenderer());

        tabs.addTab("Оборудование", equipmentTab());
        tabs.addTab(isLab() ? "Бронирования" : "Мои бронирования", bookingTab());

        JButton logout = Theme.button("Выйти", false);
        logout.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(4, 10, 6, 10));
        bottom.add(statusBar, BorderLayout.CENTER);
        bottom.add(logout, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        bindActions();
        refreshEquipment();
        refreshBookings();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
    }

    private boolean isLab() {
        return user.role() == Role.LAB_ASSISTANT;
    }

    private JPanel equipmentTab() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.add(new JLabel("Поиск:"));
        filters.add(search);
        filters.add(statusFilter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (isLab()) {
            actions.add(add);
            actions.add(edit);
            actions.add(toggleStatus);
            actions.add(delete);
        } else {
            actions.add(book);
        }

        JPanel p = new JPanel(new BorderLayout());
        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(equipmentTable), BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private JPanel bookingTab() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.add(onlyActual);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.add(cancelBooking);

        JPanel p = new JPanel(new BorderLayout());
        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(bookingTable), BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private void bindActions() {
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshEquipment();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshEquipment();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshEquipment();
            }
        });
        statusFilter.addActionListener(e -> refreshEquipment());
        onlyActual.addActionListener(e -> refreshBookings());
        equipmentTable.getSelectionModel().addListSelectionListener(e -> updateButtons());
        bookingTable.getSelectionModel().addListSelectionListener(e -> updateButtons());
        add.addActionListener(e -> openEquipmentDialog(null));
        edit.addActionListener(e -> openEquipmentDialog(selectedEquipment()));
        toggleStatus.addActionListener(e -> toggleSelectedStatus());
        delete.addActionListener(e -> deleteSelected());
        book.addActionListener(e -> bookSelected());
        cancelBooking.addActionListener(e -> cancelSelectedBooking());
    }

    void refreshEquipment() {
        Object filter = statusFilter.getSelectedItem();
        Status status = filter instanceof Status s ? s : null;
        shownEquipment = app.equipment.search(search.getText(), status);
        equipmentModel.setRowCount(0);
        for (Equipment e : shownEquipment) {
            equipmentModel.addRow(new Object[]{e.invNumber(), e.name(), e.category(), e.room(), e.status()});
        }
        updateButtons();
    }

    void refreshBookings() {
        shownBookings = app.bookings.list(user, onlyActual.isSelected());
        bookingModel.setRowCount(0);
        for (BookingView v : shownBookings) {
            Booking b = v.booking();
            bookingModel.addRow(new Object[]{v.invNumber(), v.equipmentName(), v.studentName(),
                    DATE.format(b.from()), DATE.format(b.to()), app.bookings.statusText(b), b.note()});
        }
        updateButtons();
    }

    void updateButtons() {
        Equipment e = selectedEquipment();
        boolean selected = e != null;
        edit.setEnabled(selected);
        delete.setEnabled(selected);
        toggleStatus.setEnabled(selected);
        book.setEnabled(selected);
        toggleStatus.setText(selected && e.status() == Status.IN_REPAIR ? "Вернуть в строй" : "Перевести в ремонт");
        cancelBooking.setEnabled(bookingTable.getSelectedRow() >= 0);
    }

    Equipment selectedEquipment() {
        int row = equipmentTable.getSelectedRow();
        return row >= 0 && row < shownEquipment.size() ? shownEquipment.get(row) : null;
    }

    void info(String message) {
        statusBar.setForeground(Theme.MUTED);
        statusBar.setText(message);
    }

    private void error(String message) {
        statusBar.setForeground(Theme.ERROR);
        statusBar.setText(message);
    }

    private void openEquipmentDialog(Equipment existing) {
        EquipmentDialog d = new EquipmentDialog(this, app, user, existing);
        d.setVisible(true);
        if (d.isSaved()) {
            refreshEquipment();
            info(existing == null ? "Оборудование добавлено" : "Изменения сохранены");
        }
    }

    void toggleSelectedStatus() {
        Equipment e = selectedEquipment();
        if (e == null) {
            return;
        }
        Status next = e.status() == Status.FREE ? Status.IN_REPAIR : Status.FREE;
        try {
            int cancelled = app.equipment.setStatus(user, e.id(), next);
            refreshEquipment();
            refreshBookings();
            info(next == Status.IN_REPAIR
                    ? "Оборудование переведено в ремонт, отменено броней: " + cancelled
                    : "Оборудование возвращено в строй");
        } catch (BusinessException ex) {
            error(ex.getMessage());
        }
    }

    private void deleteSelected() {
        Equipment e = selectedEquipment();
        if (e == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Удалить «" + e.name() + "» (" + e.invNumber() + ")?\nИстория его бронирований также будет удалена.",
                "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            app.equipment.delete(user, e.id());
            refreshEquipment();
            refreshBookings();
            info("Оборудование удалено");
        } catch (BusinessException ex) {
            error(ex.getMessage());
        }
    }

    private void bookSelected() {
        Equipment e = selectedEquipment();
        if (e == null) {
            return;
        }
        BookingDialog d = new BookingDialog(this, app, user, e, app.clock.get());
        d.setVisible(true);
        if (d.isBooked()) {
            refreshBookings();
            info("Бронирование оформлено: " + e.name());
        }
    }

    private void cancelSelectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0 || row >= shownBookings.size()) {
            return;
        }
        try {
            app.bookings.cancel(user, shownBookings.get(row).booking().id());
            refreshBookings();
            info("Бронирование отменено");
        } catch (BusinessException ex) {
            error(ex.getMessage());
        }
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(26);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    /** Выделяет статус оборудования цветом (REQ-FUNC-002). */
    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected) {
                c.setForeground(value == Status.IN_REPAIR ? Theme.ERROR : Theme.OK);
            }
            return c;
        }
    }
}
