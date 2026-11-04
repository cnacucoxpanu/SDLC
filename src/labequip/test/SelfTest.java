package labequip.test;

import labequip.model.*;
import labequip.service.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Автоматические проверки бизнес-правил. Запуск: java -cp out labequip.test.SelfTest */
public final class SelfTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final List<String> results = new ArrayList<>();
    private static int failed = 0;

    interface Check {
        void run() throws Exception;
    }

    static void check(String id, String title, Check c) {
        try {
            c.run();
            results.add("PASS  " + id + "  " + title);
        } catch (Throwable t) {
            failed++;
            results.add("FAIL  " + id + "  " + title + "  -> " + t);
        }
    }

    static void expectError(Check c, String part) throws Exception {
        try {
            c.run();
        } catch (BusinessException e) {
            if (!e.getMessage().contains(part)) {
                throw new AssertionError("Ожидалось «" + part + "», получено «" + e.getMessage() + "»");
            }
            return;
        }
        throw new AssertionError("Ожидалась ошибка «" + part + "»");
    }

    static void eq(Object a, Object b) {
        if (!a.equals(b)) {
            throw new AssertionError("Ожидалось " + b + ", получено " + a);
        }
    }

    static AppContext fresh() throws Exception {
        return new AppContext(Files.createTempDirectory("labequip-test"), () -> TODAY);
    }

    public static void main(String[] args) throws Exception {
        AppContext app = fresh();
        User lab = app.auth.login("lab", "lab123");
        User ivanov = app.auth.login("ivanov", "student1");
        User sidorova = app.auth.login("sidorova", "student2");

        check("REQ-FUNC-001", "вход: верные данные; неверный пароль и пустые поля отклоняются", () -> {
            eq(lab.role(), Role.LAB_ASSISTANT);
            expectError(() -> app.auth.login("lab", "xxx"), "Неверный логин или пароль");
            expectError(() -> app.auth.login("", ""), "Введите логин и пароль");
        });
        check("REQ-FUNC-002", "каталог: поиск по тексту и фильтр по статусу", () -> {
            eq(app.equipment.search("", null).size(), 12);
            eq(app.equipment.search("fluke", null).size(), 2);
            eq(app.equipment.search("", Status.IN_REPAIR).size(), 2);
            eq(app.equipment.search("ауд. 320", Status.FREE).size(), 1);
        });
        check("REQ-FUNC-003", "добавление/изменение: валидация, уникальность инв. номера, новый статус «Свободно»", () -> {
            Equipment e = app.equipment.add(lab, "ЛО-013", "Термостат", "Прочее", "ауд. 301");
            eq(e.status(), Status.FREE);
            expectError(() -> app.equipment.add(lab, "ло-013", "Другое", "Прочее", "ауд. 301"), "уже используется");
            expectError(() -> app.equipment.add(lab, "X1", "   ", "Прочее", "ауд. 301"), "Наименование");
            expectError(() -> app.equipment.add(lab, "X1", "a".repeat(101), "Прочее", "ауд. 301"), "Наименование");
            eq(app.equipment.update(lab, e.id(), "ЛО-013", "Термостат ТС-80", "Прочее", "ауд. 302").name(), "Термостат ТС-80");
            expectError(() -> app.equipment.add(ivanov, "ЛО-099", "Х", "Х", "Х"), "только лаборанту");
        });
        check("REQ-FUNC-004", "удаление: запрещено при действующих бронях, иначе удаляет вместе с историей", () -> {
            expectError(() -> app.equipment.delete(lab, 1), "действующими бронированиями");
            Equipment e = app.equipment.add(lab, "ЛО-014", "Весы", "Прочее", "ауд. 301");
            app.equipment.delete(lab, e.id());
            eq(app.equipment.find(e.id()) == null, true);
        });
        check("REQ-FUNC-005", "статус: перевод в ремонт отменяет действующие брони, возврат — «Свободно»", () -> {
            int cancelled = app.equipment.setStatus(lab, 1, Status.IN_REPAIR);
            eq(cancelled, 1);
            eq(app.equipment.find(1).status(), Status.IN_REPAIR);
            eq(app.bookings.list(ivanov, true).stream().anyMatch(v -> v.booking().equipmentId() == 1), false);
            app.equipment.setStatus(lab, 1, Status.FREE);
            eq(app.equipment.find(1).status(), Status.FREE);
        });
        check("REQ-FUNC-006", "бронирование: ремонт, прошлое, >7 дней, пересечение, лимит 3 брони", () -> {
            expectError(() -> app.bookings.book(ivanov, 3, TODAY.plusDays(1), TODAY.plusDays(2)), "в ремонте");
            expectError(() -> app.bookings.book(ivanov, 2, TODAY.minusDays(1), TODAY), "прошедшую дату");
            expectError(() -> app.bookings.book(ivanov, 2, TODAY.plusDays(1), TODAY.plusDays(8)), "Максимальный срок");
            expectError(() -> app.bookings.book(ivanov, 2, TODAY.plusDays(3), TODAY.plusDays(2)), "раньше");
            app.bookings.book(sidorova, 2, TODAY.plusDays(1), TODAY.plusDays(2));
            expectError(() -> app.bookings.book(ivanov, 2, TODAY.plusDays(2), TODAY.plusDays(4)), "пересекающийся");
            expectError(() -> app.bookings.book(lab, 4, TODAY.plusDays(1), TODAY.plusDays(2)), "только студенты");
            app.bookings.book(ivanov, 4, TODAY.plusDays(10), TODAY.plusDays(10));
            app.bookings.book(ivanov, 8, TODAY.plusDays(10), TODAY.plusDays(10));
            eq(app.bookings.list(ivanov, true).size(), 2); // бронь на оборудование №1 была отменена переводом в ремонт
            app.bookings.book(ivanov, 9, TODAY.plusDays(12), TODAY.plusDays(12));
            expectError(() -> app.bookings.book(ivanov, 11, TODAY.plusDays(10), TODAY.plusDays(10)), "Не более 3");
        });
        check("REQ-FUNC-007", "брони: студент видит и отменяет только свои, лаборант видит все", () -> {
            int mine = app.bookings.list(ivanov, false).size();
            int all = app.bookings.list(lab, false).size();
            eq(all > mine, true);
            BookingView foreign = app.bookings.list(sidorova, true).get(0);
            expectError(() -> app.bookings.cancel(ivanov, foreign.booking().id()), "только свои");
            app.bookings.cancel(sidorova, foreign.booking().id());
            expectError(() -> app.bookings.cancel(sidorova, foreign.booking().id()), "уже отменено");
        });
        check("REQ-SEC-001", "пароли хранятся только в виде соленого хеша", () -> {
            for (User u : app.store.users()) {
                eq(u.passwordHash().contains("lab123") || u.passwordHash().contains("student"), false);
                eq(u.passwordHash().matches("[0-9a-f]{32}:[0-9a-f]{64}"), true);
            }
            eq(PasswordHasher.hash("a").equals(PasswordHasher.hash("a")), false);
        });
        check("REQ-REL-001", "данные сохраняются между запусками (повторное открытие хранилища)", () -> {
            Path dir = Files.createTempDirectory("labequip-persist");
            AppContext a = new AppContext(dir, () -> TODAY);
            User l = a.auth.login("lab", "lab123");
            a.equipment.add(l, "ЛО-777", "Тест\tс табуляцией", "Прочее", "ауд. 1");
            a.equipment.setStatus(l, 2, Status.IN_REPAIR);
            AppContext b = new AppContext(dir, () -> TODAY);
            eq(b.equipment.search("ЛО-777", null).get(0).name(), "Тест\tс табуляцией");
            eq(b.equipment.find(2).status(), Status.IN_REPAIR);
            eq(b.store.bookings().size(), a.store.bookings().size());
        });

        results.forEach(System.out::println);
        System.out.println("Итого: " + (results.size() - failed) + " из " + results.size() + " проверок пройдено");
        System.exit(failed == 0 ? 0 : 1);
    }
}
