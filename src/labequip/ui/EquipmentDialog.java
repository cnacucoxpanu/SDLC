package labequip.ui;

import labequip.model.Equipment;
import labequip.model.User;
import labequip.service.AppContext;
import labequip.service.BusinessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Форма добавления и редактирования оборудования (REQ-FUNC-003). */
public class EquipmentDialog extends JDialog {
    final JTextField inv = new JTextField(24);
    final JTextField name = new JTextField(24);
    final JComboBox<String> category = new JComboBox<>();
    final JTextField room = new JTextField(24);
    final JLabel error = Theme.errorLabel();
    final JButton save = Theme.button("Сохранить", true);
    private final AppContext app;
    private final User actor;
    private final Equipment existing;
    private boolean saved;

    public EquipmentDialog(Frame owner, AppContext app, User actor, Equipment existing) {
        super(owner, existing == null ? "Новое оборудование" : "Редактирование оборудования", true);
        this.app = app;
        this.actor = actor;
        this.existing = existing;

        category.setEditable(true);
        for (String c : app.equipment.categories()) {
            category.addItem(c);
        }
        if (existing != null) {
            inv.setText(existing.invNumber());
            name.setText(existing.name());
            category.setSelectedItem(existing.category());
            room.setText(existing.room());
        } else {
            category.setSelectedItem("");
        }

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 24, 16, 24));
        p.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 0, 5, 12);
        g.anchor = GridBagConstraints.WEST;
        String[] labels = {"Инвентарный номер", "Наименование", "Категория", "Помещение"};
        JComponent[] fields = {inv, name, category, room};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.fill = GridBagConstraints.NONE;
            g.weightx = 0;
            p.add(new JLabel(labels[i]), g);
            g.gridx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            p.add(fields[i], g);
        }
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        p.add(error, g);

        JButton cancel = Theme.button("Отмена", false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        g.gridy = 5;
        p.add(buttons, g);

        setContentPane(p);
        getRootPane().setDefaultButton(save);
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());
        pack();
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(owner);
    }

    void onSave() {
        try {
            String cat = String.valueOf(category.getEditor().getItem());
            if (existing == null) {
                app.equipment.add(actor, inv.getText(), name.getText(), cat, room.getText());
            } else {
                app.equipment.update(actor, existing.id(), inv.getText(), name.getText(), cat, room.getText());
            }
            saved = true;
            dispose();
        } catch (BusinessException ex) {
            error.setText(ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
