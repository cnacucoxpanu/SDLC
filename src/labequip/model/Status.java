package labequip.model;

/** Статус единицы оборудования. */
public enum Status {
    FREE("Свободно"),
    IN_REPAIR("В ремонте");

    private final String title;

    Status(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
