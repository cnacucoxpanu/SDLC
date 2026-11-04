package labequip.model;

/** Единица лабораторного оборудования. */
public record Equipment(int id, String invNumber, String name, String category, String room, Status status) {

    public Equipment withStatus(Status newStatus) {
        return new Equipment(id, invNumber, name, category, room, newStatus);
    }
}
