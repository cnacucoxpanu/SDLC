import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PopulationModel {
    private int day;
    private int month;
    private int year;
    private long calculatedPopulation;
    
    private final List<ModelListener> listeners = new ArrayList<>();

    public interface ModelListener {
        void onModelChanged();
    }

    public void addListener(ModelListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (ModelListener listener : listeners) {
            listener.onModelChanged();
        }
    }

    public void setData(int day, int month, int year) throws IllegalArgumentException {
        if (month < 1 || month > 12 || day < 1 || day > 31 || year < 1800 || year > 2100) {
            throw new IllegalArgumentException("Введены некорректные данные даты (выход за допустимые диапазоны)!");
        }
        try {
            LocalDate.of(year, month, day);
        } catch (Exception e) {
            throw new IllegalArgumentException("Указанной даты не существует в григорианском календаре!");
        }

        this.day = day;
        this.month = month;
        this.year = year;
        
        calculatePopulation();
        notifyListeners();
    }

    private void calculatePopulation() {
        double basePopulation = 7800000000.0;
        int baseYear = 2020;
        double targetYearDecimal = year + (month - 1) / 12.0 + (day - 1) / 365.0;
        double yearsDiff = targetYearDecimal - baseYear;
        
        this.calculatedPopulation = (long) (basePopulation * Math.exp(0.0105 * yearsDiff));
    }

    public int getDay() { return day; }x
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public long getCalculatedPopulation() { return calculatedPopulation; }
}