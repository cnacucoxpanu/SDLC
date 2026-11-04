package labequip.ui;

import labequip.model.*;
import labequip.service.AppContext;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Служебная утилита: проходит основной сценарий работы и сохраняет снимки окон в PNG.
 * Запуск: java -cp out labequip.ui.ScreenshotTool screenshots
 */
public final class ScreenshotTool {
    private static final double SCALE = 2.0;
    private static File outDir;

    public static void main(String[] args) throws Exception {
        outDir = new File(args.length > 0 ? args[0] : "screenshots");
        outDir.mkdirs();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Theme.install();
                run();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
        System.exit(0);
    }

    static void run() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 20);
        AppContext app = new AppContext(Files.createTempDirectory("labequip-shots"), () -> today);
        User lab = app.auth.login("lab", "lab123");
        User ivanov = app.auth.login("ivanov", "student1");

        // 1-2. Вход
        LoginFrame login = new LoginFrame(app, u -> { });
        snap(login, "01_login", new Dimension(620, 470));
        login.login.setText("ivanov");
        login.password.setText("wrong");
        login.submit();
        snap(login, "02_login_error", new Dimension(620, 470));

        // 3-4. Лаборант: каталог
        MainFrame lf = new MainFrame(app, lab, () -> { });
        Dimension win = new Dimension(1000, 640);
        lf.equipmentTable.setRowSelectionInterval(0, 0);
        snap(lf, "03_lab_catalog", win);
        lf.statusFilter.setSelectedItem(Status.IN_REPAIR);
        lf.updateButtons();
        snap(lf, "04_lab_filter_repair", win);
        lf.statusFilter.setSelectedIndex(0);

        // 5-6. Форма оборудования
        EquipmentDialog ed = new EquipmentDialog(lf, app, lab, null);
        ed.inv.setText("ЛО-013");
        ed.name.setText("Осциллограф Siglent SDS1104X-E");
        ed.category.setSelectedItem("Измерительные приборы");
        ed.room.setText("ауд. 312-4");
        snap(ed, "05_equipment_form", null);
        ed.inv.setText("ЛО-001");
        ed.save.doClick();
        snap(ed, "06_equipment_form_error", null);
        ed.inv.setText("ЛО-013");
        ed.save.doClick();
        lf.refreshEquipment();
        lf.info("Оборудование добавлено");
        selectByInv(lf, "ЛО-013");
        snap(lf, "07_lab_after_add", win);

        // 8. Перевод в ремонт с автоматической отменой броней
        selectByInv(lf, "ЛО-001");
        lf.toggleSelectedStatus();
        snap(lf, "08_lab_to_repair", win);

        // 9. Все бронирования
        lf.onlyActual.setSelected(false);
        lf.refreshBookings();
        lf.tabs.setSelectedIndex(1);
        lf.bookingTable.setRowSelectionInterval(1, 1);
        snap(lf, "09_lab_bookings", win);
        lf.tabs.setSelectedIndex(0);

        // 10. Подтверждение удаления
        selectByInv(lf, "ЛО-013");
        JOptionPane pane = new JOptionPane(
                "Удалить «Осциллограф Siglent SDS1104X-E» (ЛО-013)?\nИстория его бронирований также будет удалена.",
                JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog confirm = pane.createDialog(lf, "Подтверждение удаления");
        confirm.pack();
        snap(confirm, "10_delete_confirm", null);
        app.equipment.delete(lab, lf.selectedEquipment().id());
        lf.refreshEquipment();

        // 11-14. Студент
        MainFrame sf = new MainFrame(app, ivanov, () -> { });
        selectByInv(sf, "ЛО-007");
        snap(sf, "11_student_catalog", win);
        BookingDialog bd = new BookingDialog(sf, app, ivanov, app.equipment.find(7), today.plusDays(1));
        bd.setDates(today.plusDays(4), today.plusDays(5));
        snap(bd, "12_booking_form", null);
        bd.setDates(today.plusDays(2), today.plusDays(3));
        bd.book.doClick();
        snap(bd, "13_booking_error", null);
        bd.setDates(today.plusDays(4), today.plusDays(5));
        bd.book.doClick();
        sf.refreshBookings();
        sf.info("Бронирование оформлено: " + app.equipment.find(7).name());
        sf.tabs.setSelectedIndex(1);
        sf.bookingTable.setRowSelectionInterval(0, 0);
        snap(sf, "14_student_bookings", win);
    }

    static void selectByInv(MainFrame f, String inv) {
        for (int i = 0; i < f.equipmentTable.getRowCount(); i++) {
            if (inv.equals(f.equipmentTable.getValueAt(i, 0))) {
                f.equipmentTable.setRowSelectionInterval(i, i);
                f.equipmentTable.scrollRectToVisible(f.equipmentTable.getCellRect(i, 0, true));
                return;
            }
        }
        throw new IllegalStateException(inv);
    }

    /** Рисует окно вместе с имитацией строки заголовка в PNG. */
    static void snap(Window w, String name, Dimension size) throws Exception {
        if (size != null) {
            w.setSize(size);
        }
        if (!w.isDisplayable()) {
            w.addNotify();
        }
        w.validate();
        layoutAll(w);
        int bar = 32;
        int width = w.getWidth();
        int height = w.getHeight();
        String title = w instanceof Frame f ? f.getTitle() : ((Dialog) w).getTitle();
        BufferedImage img = new BufferedImage((int) (width * SCALE), (int) ((height + bar) * SCALE), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(SCALE, SCALE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xE9ECF1));
        g.fillRect(0, 0, width, bar);
        g.setColor(new Color(0x2A2F36));
        g.setFont(new Font(Theme.FONT, Font.PLAIN, 13));
        g.drawString(title, 14, 21);
        g.setStroke(new BasicStroke(1.3f));
        int x = width - 100;
        g.drawLine(x, 16, x + 10, 16);
        g.drawRect(x + 36, 11, 9, 9);
        g.drawLine(x + 72, 11, x + 82, 21);
        g.drawLine(x + 72, 21, x + 82, 11);
        g.setColor(new Color(0xB8BEC8));
        g.drawLine(0, bar - 1, width, bar - 1);
        g.translate(0, bar);
        JRootPane root = ((RootPaneContainer) w).getRootPane();
        root.setSize(width, height);
        layoutAll(root);
        root.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(outDir, name + ".png"));
        System.out.println("saved " + name);
    }

    static void layoutAll(Component c) {
        if (c instanceof Container ct) {
            ct.doLayout();
            for (Component ch : ct.getComponents()) {
                layoutAll(ch);
            }
        }
    }
}
