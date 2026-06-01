import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;

public class MazeApp extends JFrame {
    private static final String BASE_URL = "https://backend-qcf9.onrender.com/fm1";

    private RenderConfig config;

    // Текстовые поля ввода
    private JTextField apiKeyField;
    private JTextField widthField;
    private JTextField heightField;

    // Элементы UI для текущих настроек от сервера
    private JLabel wallColorLabel;
    private JLabel pathColorLabel;
    private JLabel gridLabel;
    private JLabel delayLabel;

    public MazeApp() {
        setTitle("Настройки лабиринта");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(9, 2, 10, 10));

        // 1. Поле для ввода API-ключа
        add(new JLabel(" Ваш API-Ключ:"));
        apiKeyField = new JTextField("0GmBa3FvOFCRTkd6WO30vHJLy2nMPRd6qWjEx0Sp0yU68PKw74DTtjJZeyJuvw5Z");
        add(apiKeyField);

        // 2. Размеры лабиринта
        add(new JLabel(" Ширина (5-100):"));
        widthField = new JTextField("30");
        add(widthField);

        add(new JLabel("  Высота (5-100):"));
        heightField = new JTextField("30");
        add(heightField);

        // 3. Информационные лейблы конфигурации
        add(new JLabel("  Цвет стен:"));
        wallColorLabel = new JLabel("Введите ключ и обновите...");
        add(wallColorLabel);

        add(new JLabel("  Цвет пути:"));
        pathColorLabel = new JLabel("-");
        add(pathColorLabel);

        add(new JLabel("  Рисовать сетку:"));
        gridLabel = new JLabel("-");
        add(gridLabel);

        add(new JLabel("  Задержка анимации (мс):"));
        delayLabel = new JLabel("-");
        add(delayLabel);

        // 4. Кнопки управления
        JButton refreshBtn = new JButton("Обновить конфигурацию");
        refreshBtn.addActionListener(e -> loadConfig());
        add(refreshBtn);

        JButton getMazeBtn = new JButton("ПОЛУЧИТЬ ЛАБИРИНТ");
        getMazeBtn.addActionListener(e -> downloadAndShowMaze());
        add(getMazeBtn);
    }

    private String getEnteredApiKey() {
        return apiKeyField.getText().trim();
    }

    private void loadConfig() {
        String currentKey = getEnteredApiKey();
        if (currentKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, сначала введите API-ключ!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        wallColorLabel.setText("Загрузка...");

        SwingWorker<RenderConfig, Void> worker = new SwingWorker<>() {
            @Override
            protected RenderConfig doInBackground() throws Exception {
                String json = NetworkService.sendGet(BASE_URL + "/get-render-config", currentKey);
                return RenderConfig.fromJson(json);
            }

            @Override
            protected void done() {
                try {
                    config = get();
                    wallColorLabel.setText(config.wallCellColor);
                    pathColorLabel.setText(config.pathColor);
                    gridLabel.setText(config.drawGrid ? "Да (" + config.gridColor + ")" : "Нет");
                    delayLabel.setText(String.valueOf(config.animationDelayMs));
                } catch (Exception e) {
                    wallColorLabel.setText("Ошибка");
                    JOptionPane.showMessageDialog(MazeApp.this,
                            "Ошибка сети или неверный API-ключ: " + e.getMessage(), "Ошибка API", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void downloadAndShowMaze() {
        String currentKey = getEnteredApiKey();
        if (currentKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите API-ключ перед получением лабиринта!");
            return;
        }

        if (config == null) {
            JOptionPane.showMessageDialog(this, "Сначала нажмите кнопку 'Обновить конфигурацию' для проверки ключа.");
            return;
        }

        final int targetWidth = parseDimension(widthField.getText());
        final int targetHeight = parseDimension(heightField.getText());

        widthField.setText(String.valueOf(targetWidth));
        heightField.setText(String.valueOf(targetHeight));

        String mazeUrl = BASE_URL + "/get-maze-image?width=" + targetWidth + "&height=" + targetHeight;

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                BufferedImage originalImage = NetworkService.downloadImage(mazeUrl, currentKey);
                if (originalImage == null) return null;

                BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resizedImage.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                g.dispose();

                return resizedImage;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image == null) throw new Exception("Неверный формат ответа от сервера");

                    openMazeWindow(image, config);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MazeApp.this,
                            "Не удалось получить лабиринт: " + e.getMessage(), "Ошибка запроса", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Тот самый метод, которого не хватало на скриншоте:
    private int parseDimension(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value >= 5 && value <= 100) return value;
        } catch (NumberFormatException e) {}
        return 30;
    }

    private void openMazeWindow(BufferedImage img, RenderConfig config) {
        JFrame mazeFrame = new JFrame("Окно лабиринта (" + img.getWidth() + "x" + img.getHeight() + ")");
        mazeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        MazePanel mazePanel = new MazePanel(img, config);
        JButton solveBtn = new JButton("Проверить решение");
        solveBtn.addActionListener(e -> mazePanel.startSolving());

        mazeFrame.add(mazePanel, BorderLayout.CENTER);
        mazeFrame.add(solveBtn, BorderLayout.SOUTH);

        mazeFrame.pack();
        mazeFrame.setLocationRelativeTo(this);
        mazeFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeApp().setVisible(true));
    }
}