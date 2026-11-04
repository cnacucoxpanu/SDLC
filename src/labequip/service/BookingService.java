package labequip.service;

import labequip.model.*;
import labequip.storage.Store;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** Бронирование оборудования студентами (REQ-FUNC-006, REQ-FUNC-007). */
public final class BookingService {
    public static final int MAX_DAYS = 7;
    public static final int MAX_ACTUAL_PER_STUDENT = 3;

    private final Store store;
    private final Supplier<LocalDate> clock;

    public BookingService(Store store, Supplier<LocalDate> clock) {
        this.store = store;
        this.clock = clock;
    }

    public Booking book(User student, int equipmentId, LocalDate from, LocalDate to) throws BusinessException {
        if (student == null || student.role() != Role.STUDENT) {
            throw new BusinessException("Бронировать оборудование могут только студенты");
        }
        Equipment e = store.equipment().stream().filter(x -> x.id() == equipmentId).findFirst()
                .orElseThrow(() -> new BusinessException("Оборудование не найдено"));
        if (e.status() == Status.IN_REPAIR) {
            throw new BusinessException("Оборудование находится в ремонте и недоступно для бронирования");
        }
        if (from == null || to == null) {
            throw new BusinessException("Укажите даты начала и окончания");
        }
        if (to.isBefore(from)) {
            throw new BusinessException("Дата окончания раньше даты начала");
        }
        if (from.isBefore(clock.get())) {
            throw new BusinessException("Нельзя бронировать на прошедшую дату");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new BusinessException("Максимальный срок бронирования — " + MAX_DAYS + " дней");
        }
        long mine = store.bookings().stream()
                .filter(b -> b.userId() == student.id() && b.isActual(clock.get())).count();
        if (mine >= MAX_ACTUAL_PER_STUDENT) {
            throw new BusinessException("Не более " + MAX_ACTUAL_PER_STUDENT + " действующих бронирований на студента");
        }
        boolean busy = store.bookings().stream()
                .anyMatch(b -> b.equipmentId() == equipmentId && b.active() && b.overlaps(from, to));
        if (busy) {
            throw new BusinessException("Оборудование уже забронировано на пересекающийся период");
        }
        Booking b = new Booking(store.nextBookingId(), equipmentId, student.id(), from, to, true, "");
        store.bookings().add(b);
        store.save();
        return b;
    }

    public void cancel(User actor, int bookingId) throws BusinessException {
        Booking b = store.bookings().stream().filter(x -> x.id() == bookingId).findFirst()
                .orElseThrow(() -> new BusinessException("Бронирование не найдено"));
        boolean owner = actor != null && actor.role() == Role.STUDENT && actor.id() == b.userId();
        boolean lab = actor != null && actor.role() == Role.LAB_ASSISTANT;
        if (!owner && !lab) {
            throw new BusinessException("Можно отменять только свои бронирования");
        }
        if (!b.isActual(clock.get())) {
            throw new BusinessException("Бронирование уже отменено или завершено");
        }
        store.replaceBooking(b.cancelled(lab ? "Отменено лаборантом" : "Отменено студентом"));
        store.save();
    }

    /** Студент видит только свои брони, лаборант — все. */
    public List<BookingView> list(User viewer, boolean onlyActual) {
        return store.bookings().stream()
                .filter(b -> viewer.role() == Role.LAB_ASSISTANT || b.userId() == viewer.id())
                .filter(b -> !onlyActual || b.isActual(clock.get()))
                .sorted(Comparator.comparing(Booking::from).reversed().thenComparing(Booking::id))
                .map(b -> {
                    Equipment e = store.equipment().stream().filter(x -> x.id() == b.equipmentId()).findFirst().orElse(null);
                    User u = store.users().stream().filter(x -> x.id() == b.userId()).findFirst().orElse(null);
                    return new BookingView(b, e == null ? "?" : e.invNumber(), e == null ? "?" : e.name(),
                            u == null ? "?" : u.fullName());
                }).toList();
    }

    public String statusText(Booking b) {
        if (!b.active()) {
            return "Отменена";
        }
        return b.to().isBefore(clock.get()) ? "Завершена" : "Активна";
    }
}
