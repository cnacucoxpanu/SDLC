package labequip.service;

import labequip.model.*;
import labequip.storage.Store;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Каталог оборудования и его статусы (REQ-FUNC-002 — REQ-FUNC-005). */
public final class EquipmentService {
    private final Store store;
    private final Supplier<LocalDate> clock;

    public EquipmentService(Store store, Supplier<LocalDate> clock) {
        this.store = store;
        this.clock = clock;
    }

    /** Поиск по инвентарному номеру, названию, категории и помещению; status == null — любой статус. */
    public List<Equipment> search(String text, Status status) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return store.equipment().stream()
                .filter(e -> status == null || e.status() == status)
                .filter(e -> q.isEmpty()
                        || e.invNumber().toLowerCase(Locale.ROOT).contains(q)
                        || e.name().toLowerCase(Locale.ROOT).contains(q)
                        || e.category().toLowerCase(Locale.ROOT).contains(q)
                        || e.room().toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing(Equipment::invNumber))
                .toList();
    }

    public Equipment find(int id) {
        return store.equipment().stream().filter(e -> e.id() == id).findFirst().orElse(null);
    }

    public List<String> categories() {
        return store.equipment().stream().map(Equipment::category).distinct().sorted().toList();
    }

    public Equipment add(User actor, String inv, String name, String category, String room) throws BusinessException {
        requireLab(actor);
        String[] f = validate(inv, name, category, room, -1);
        Equipment e = new Equipment(store.nextEquipmentId(), f[0], f[1], f[2], f[3], Status.FREE);
        store.equipment().add(e);
        store.save();
        return e;
    }

    public Equipment update(User actor, int id, String inv, String name, String category, String room)
            throws BusinessException {
        requireLab(actor);
        Equipment old = require(id);
        String[] f = validate(inv, name, category, room, id);
        Equipment e = new Equipment(id, f[0], f[1], f[2], f[3], old.status());
        store.replaceEquipment(e);
        store.save();
        return e;
    }

    public void delete(User actor, int id) throws BusinessException {
        requireLab(actor);
        Equipment e = require(id);
        boolean hasActual = store.bookings().stream()
                .anyMatch(b -> b.equipmentId() == id && b.isActual(clock.get()));
        if (hasActual) {
            throw new BusinessException("Нельзя удалить оборудование с действующими бронированиями");
        }
        store.bookings().removeIf(b -> b.equipmentId() == id);
        store.equipment().remove(e);
        store.save();
    }

    /**
     * Меняет статус. При переводе в ремонт все действующие брони автоматически отменяются.
     *
     * @return число отмененных бронирований
     */
    public int setStatus(User actor, int id, Status status) throws BusinessException {
        requireLab(actor);
        Equipment e = require(id);
        int cancelled = 0;
        if (status == Status.IN_REPAIR) {
            for (Booking b : List.copyOf(store.bookings())) {
                if (b.equipmentId() == id && b.isActual(clock.get())) {
                    store.replaceBooking(b.cancelled("Оборудование переведено в ремонт"));
                    cancelled++;
                }
            }
        }
        store.replaceEquipment(e.withStatus(status));
        store.save();
        return cancelled;
    }

    private Equipment require(int id) throws BusinessException {
        Equipment e = find(id);
        if (e == null) {
            throw new BusinessException("Оборудование не найдено");
        }
        return e;
    }

    static void requireLab(User actor) throws BusinessException {
        if (actor == null || actor.role() != Role.LAB_ASSISTANT) {
            throw new BusinessException("Действие доступно только лаборанту");
        }
    }

    private String[] validate(String inv, String name, String category, String room, int selfId)
            throws BusinessException {
        String i = inv == null ? "" : inv.trim();
        String n = name == null ? "" : name.trim();
        String c = category == null ? "" : category.trim();
        String r = room == null ? "" : room.trim();
        if (i.isEmpty() || i.length() > 20) {
            throw new BusinessException("Инвентарный номер: от 1 до 20 символов");
        }
        if (n.isEmpty() || n.length() > 100) {
            throw new BusinessException("Наименование: от 1 до 100 символов");
        }
        if (c.isEmpty() || c.length() > 50) {
            throw new BusinessException("Категория: от 1 до 50 символов");
        }
        if (r.isEmpty() || r.length() > 30) {
            throw new BusinessException("Помещение: от 1 до 30 символов");
        }
        boolean duplicate = store.equipment().stream()
                .anyMatch(e -> e.id() != selfId && e.invNumber().equalsIgnoreCase(i));
        if (duplicate) {
            throw new BusinessException("Инвентарный номер «" + i + "» уже используется");
        }
        return new String[]{i, n, c, r};
    }
}
