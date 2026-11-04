package labequip.storage;

import labequip.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Встроенная база данных: три таблицы (users, equipment, bookings), каждая хранится
 * в отдельном текстовом файле .tsv. Запись выполняется атомарно (временный файл + замена),
 * поэтому данные сохраняются между запусками программы.
 */
public final class Store {
    private final Path dir;
    private final List<User> users = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();

    public Store(Path dir) {
        this.dir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        load();
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    // ---------- users ----------
    public List<User> users() {
        return users;
    }

    public int nextUserId() {
        return users.stream().mapToInt(User::id).max().orElse(0) + 1;
    }

    // ---------- equipment ----------
    public List<Equipment> equipment() {
        return equipment;
    }

    public int nextEquipmentId() {
        return equipment.stream().mapToInt(Equipment::id).max().orElse(0) + 1;
    }

    public void replaceEquipment(Equipment e) {
        for (int i = 0; i < equipment.size(); i++) {
            if (equipment.get(i).id() == e.id()) {
                equipment.set(i, e);
                return;
            }
        }
    }

    // ---------- bookings ----------
    public List<Booking> bookings() {
        return bookings;
    }

    public int nextBookingId() {
        return bookings.stream().mapToInt(Booking::id).max().orElse(0) + 1;
    }

    public void replaceBooking(Booking b) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).id() == b.id()) {
                bookings.set(i, b);
                return;
            }
        }
    }

    // ---------- persistence ----------
    public synchronized void save() {
        write("users.tsv", users.stream().map(u -> row(String.valueOf(u.id()), u.login(), u.passwordHash(),
                u.fullName(), u.role().name())).toList());
        write("equipment.tsv", equipment.stream().map(e -> row(String.valueOf(e.id()), e.invNumber(), e.name(),
                e.category(), e.room(), e.status().name())).toList());
        write("bookings.tsv", bookings.stream().map(b -> row(String.valueOf(b.id()), String.valueOf(b.equipmentId()),
                String.valueOf(b.userId()), b.from().toString(), b.to().toString(),
                String.valueOf(b.active()), b.note())).toList());
    }

    private void load() {
        for (String[] f : read("users.tsv")) {
            users.add(new User(Integer.parseInt(f[0]), f[1], f[2], f[3], Role.valueOf(f[4])));
        }
        for (String[] f : read("equipment.tsv")) {
            equipment.add(new Equipment(Integer.parseInt(f[0]), f[1], f[2], f[3], f[4], Status.valueOf(f[5])));
        }
        for (String[] f : read("bookings.tsv")) {
            bookings.add(new Booking(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]),
                    LocalDate.parse(f[3]), LocalDate.parse(f[4]), Boolean.parseBoolean(f[5]), f[6]));
        }
    }

    private void write(String name, List<String> lines) {
        try {
            Path tmp = dir.resolve(name + ".tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            Files.move(tmp, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String[]> read(String name) {
        Path p = dir.resolve(name);
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(p)) {
            return rows;
        }
        try {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (!line.isEmpty()) {
                    rows.add(split(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }

    private static String row(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('\t');
            }
            sb.append(fields[i] == null ? "" : fields[i]
                    .replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", ""));
        }
        return sb.toString();
    }

    private static String[] split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                char n = line.charAt(++i);
                cur.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else if (c == '\t') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
