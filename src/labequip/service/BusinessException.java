package labequip.service;

/** Нарушение бизнес-правила; сообщение предназначено для показа пользователю. */
public class BusinessException extends Exception {
    public BusinessException(String message) {
        super(message);
    }
}
