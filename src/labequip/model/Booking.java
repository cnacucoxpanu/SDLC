package labequip.model;

import java.time.LocalDate;

/** Бронирование оборудования студентом на период [from; to] включительно. */
public record Booking(int id, int equipmentId, int userId, LocalDate from, LocalDate to,
                      boolean active, String note) {

    /** Бронь активна и еще не закончилась на указанную дату. */
    public boolean isActual(LocalDate today) {
        return active && !to.isBefore(today);
    }

    public boolean overlaps(LocalDate otherFrom, LocalDate otherTo) {
        return !otherTo.isBefore(from) && !otherFrom.isAfter(to);
    }

    public Booking cancelled(String reason) {
        return new Booking(id, equipmentId, userId, from, to, false, reason);
    }
}
