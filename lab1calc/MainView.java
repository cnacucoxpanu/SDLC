import javax.swing.*;
import java.awt.*;

public class MainView extends JFrame implements PopulationModel.ModelListener {
    private final PopulationController controller;
    private final PopulationModel model;

    private final JLabel lblDate = new JLabel("Дата не задана");
    private final JLabel lblPopulation = new JLabel("Численность населения: -");

    public MainView(PopulationController controller, PopulationModel model) {
        this.controller = controller;
        this.model = model;
        // ПОДПИСКА НА МОДЕЛЬ: Это делает модель "Активной". 
        // View не спрашивает данные, а ждет, когда модель сама скажет "Я обновилась!"
        this.model.addListener(this);

        initView();
    }

    private void initView() {
        setTitle("World Population Calculator (MVC Active Model)");
        setSize(480, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // Центральная панель с результатами
        JPanel panelInfo = new JPanel(new GridLayout(2, 1, 10, 10));
        panelInfo.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));
        
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblPopulation.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblPopulation.setForeground(new Color(0, 102, 204)); // Придадим цвет результату

        panelInfo.add(lblDate);
        panelInfo.add(lblPopulation);
        add(panelInfo, BorderLayout.CENTER);

        // Нижняя панель с кнопкой
        JButton btnOpenInput = new JButton("Ввести данные");
        btnOpenInput.setPreferredSize(new Dimension(150, 40));
        btnOpenInput.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JPanel panelButton = new JPanel();
        panelButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        panelButton.add(btnOpenInput);
        add(panelButton, BorderLayout.SOUTH);

        // Связь с контроллером
        btnOpenInput.addActionListener(e -> controller.openInputDialog(this));
    }

    @Override
    public void onModelChanged() {
        // Этот метод вызывается моделью автоматически при изменении данных
        lblDate.setText(String.format("Введенная дата: %02d.%02d.%d", 
                model.getDay(), model.getMonth(), model.getYear()));
        lblPopulation.setText(String.format("Население мира: %,d человек", 
                model.getCalculatedPopulation()));
    }
}
