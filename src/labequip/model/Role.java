package labequip.model;

/** Роль пользователя системы. */
public enum Role {
    LAB_ASSISTANT("Лаборант"),
    STUDENT("Студент");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
