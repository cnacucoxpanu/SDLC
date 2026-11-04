package labequip.model;

/** Учетная запись пользователя. Пароль хранится только в виде соленого хеша. */
public record User(int id, String login, String passwordHash, String fullName, Role role) {
}
