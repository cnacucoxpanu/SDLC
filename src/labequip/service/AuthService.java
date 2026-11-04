package labequip.service;

import labequip.model.User;
import labequip.storage.Store;

/** Вход в систему (REQ-FUNC-001). */
public final class AuthService {
    private final Store store;

    public AuthService(Store store) {
        this.store = store;
    }

    public User login(String login, String password) throws BusinessException {
        String l = login == null ? "" : login.trim();
        if (l.isEmpty() || password == null || password.isEmpty()) {
            throw new BusinessException("Введите логин и пароль");
        }
        for (User u : store.users()) {
            if (u.login().equalsIgnoreCase(l) && PasswordHasher.matches(password, u.passwordHash())) {
                return u;
            }
        }
        throw new BusinessException("Неверный логин или пароль");
    }
}
