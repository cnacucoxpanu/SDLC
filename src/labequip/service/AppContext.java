package labequip.service;

import labequip.model.*;
import labequip.storage.Store;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Supplier;

/** Собирает хранилище и сервисы; при первом запуске заполняет БД демонстрационными данными. */
public final class AppContext {
    public final Store store;
    public final AuthService auth;
    public final EquipmentService equipment;
    public final BookingService bookings;
    public final Supplier<LocalDate> clock;

    public AppContext(Path dataDir, Supplier<LocalDate> clock) {
        this.clock = clock;
        this.store = new Store(dataDir);
        this.auth = new AuthService(store);
        this.equipment = new EquipmentService(store, clock);
        this.bookings = new BookingService(store, clock);
        if (store.isEmpty()) {
            seed();
        }
    }

    private void seed() {
        store.users().add(new User(1, "lab", PasswordHasher.hash("lab123"), "Петрова Анна Сергеевна", Role.LAB_ASSISTANT));
        store.users().add(new User(2, "ivanov", PasswordHasher.hash("student1"), "Иванов Иван Петрович", Role.STUDENT));
        store.users().add(new User(3, "sidorova", PasswordHasher.hash("student2"), "Сидорова Мария Олеговна", Role.STUDENT));
        String[][] eq = {
                {"ЛО-001", "Осциллограф цифровой Rigol DS1054Z", "Измерительные приборы", "ауд. 312-4", "FREE"},
                {"ЛО-002", "Мультиметр Fluke 87V", "Измерительные приборы", "ауд. 312-4", "FREE"},
                {"ЛО-003", "Генератор сигналов Tektronix AFG1022", "Измерительные приборы", "ауд. 312-4", "IN_REPAIR"},
                {"ЛО-004", "Анализатор спектра Siglent SSA3021X", "Измерительные приборы", "ауд. 314-4", "FREE"},
                {"ЛО-005", "Лабораторный блок питания Korad KA3005P", "Источники питания", "ауд. 312-4", "FREE"},
                {"ЛО-006", "Лабораторный блок питания Mastech HY3005D", "Источники питания", "ауд. 314-4", "IN_REPAIR"},
                {"ЛО-007", "Паяльная станция Lukey 702", "Паяльное оборудование", "ауд. 316-4", "FREE"},
                {"ЛО-008", "Тепловизор Fluke TiS20", "Измерительные приборы", "ауд. 314-4", "FREE"},
                {"ЛО-009", "Логический анализатор Saleae Logic Pro 8", "Измерительные приборы", "ауд. 312-4", "FREE"},
                {"ЛО-010", "3D-принтер Prusa i3 MK3S+", "Прототипирование", "ауд. 318-4", "FREE"},
                {"ЛО-011", "Микроскоп Биолам Р-15", "Оптика", "ауд. 320-4", "FREE"},
                {"ЛО-012", "Отладочная плата STM32 Nucleo-F446RE", "Микроконтроллеры", "ауд. 316-4", "FREE"},
        };
        for (int i = 0; i < eq.length; i++) {
            store.equipment().add(new Equipment(i + 1, eq[i][0], eq[i][1], eq[i][2], eq[i][3], Status.valueOf(eq[i][4])));
        }
        LocalDate t = clock.get();
        store.bookings().add(new Booking(1, 1, 2, t.plusDays(1), t.plusDays(3), true, ""));
        store.bookings().add(new Booking(2, 7, 3, t.plusDays(2), t.plusDays(2), true, ""));
        store.bookings().add(new Booking(3, 5, 2, t.minusDays(6), t.minusDays(4), true, ""));
        store.bookings().add(new Booking(4, 10, 3, t.plusDays(5), t.plusDays(7), false, "Отменено студентом"));
        store.save();
    }
}
