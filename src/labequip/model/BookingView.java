package labequip.model;

/** Бронирование вместе с данными для отображения в таблице. */
public record BookingView(Booking booking, String invNumber, String equipmentName, String studentName) {
}
