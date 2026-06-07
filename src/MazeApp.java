import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MazeApp extends JFrame {
    private static final String BASE_URL = "https://backend-qcf9.onrender.com/fm1";

    private RenderConfig config;


    private JTextField apiKeyField;
    private JTextField widthField;
    private JTextField heightField;


    private JComboBox<String> wallColorCombo;
    private JComboBox<String> pathColorCombo;
    private JCheckBox drawGridCheck;
    private JTextField delayField;


    private final String[] colorNames = {"Black", " Red", "Blue", "Green", "Gray", "Orange"};
    private final String[] colorHexes = {"#000000", "#FF0000", "#0000FF", "#00FF00", "#808080", "#FFA500"};

    public MazeApp() {
        setTitle("Maze Settings");
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(9, 2, 10, 10));


        add(new JLabel(" Your API key:"));
        apiKeyField = new JTextField("0GmBa3FvOFCRTkd6WO30vHJLy2nMPRd6qWjEx0Sp0yU68PKw74DTtjJZeyJuvw5Z");
        add(apiKeyField);


        add(new JLabel(" Width (5-100):"));
        widthField = new JTextField("30");
        add(widthField);

        add(new JLabel(" Height(5-100):"));
        heightField = new JTextField("30");
        add(heightField);


        add(new JLabel(" Wall color:"));
        wallColorCombo = new JComboBox<>(colorNames);
        add(wallColorCombo);

        add(new JLabel(" Path color:"));
        pathColorCombo = new JComboBox<>(colorNames);
        pathColorCombo.setSelectedIndex(3);
        add(pathColorCombo);

        add(new JLabel(" View grid:"));
        drawGridCheck = new JCheckBox("Enable grid", true);
        add(drawGridCheck);

        add(new JLabel(" Animation speed (ms):"));
        delayField = new JTextField("80");
        add(delayField);


        JButton refreshBtn = new JButton(" Load default server");
        refreshBtn.addActionListener(e -> loadConfigFromServer());
        add(refreshBtn);

        JButton getMazeBtn = new JButton("GET MAZE");
        getMazeBtn.addActionListener(e -> downloadAndShowMaze());
        add(getMazeBtn);
    }

    private String getEnteredApiKey() {
        return apiKeyField.getText().trim();
    }


    private int getColorIndex(String hex) {
        for (int i = 0; i < colorHexes.length; i++) {
            if (colorHexes[i].equalsIgnoreCase(hex)) return i;
        }
        return 0;
    }


    private void loadConfigFromServer() {
        String currentKey = getEnteredApiKey();
        if (currentKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, сначала введите API-ключ!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
                    // Выставляем в UI то, что прислал сервер
                    wallColorCombo.setSelectedIndex(getColorIndex(config.wallCellColor));
                    pathColorCombo.setSelectedIndex(getColorIndex(config.pathColor));
                    drawGridCheck.setSelected(config.drawGrid);
                    delayField.setText(String.valueOf(config.animationDelayMs));
                    JOptionPane.showMessageDialog(MazeApp.this, "Конфигурация сервера успешно синхронизирована!");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MazeApp.this, "Ошибка сервера: " + e.getMessage(), "Ошибка API", JOptionPane.ERROR_MESSAGE);
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

                    // Создаем объект конфигурации прямо из того, что выбрано пользователем в окне!
                    RenderConfig customConfig = new RenderConfig();
                    customConfig.wallCellColor = colorHexes[wallColorCombo.getSelectedIndex()];
                    customConfig.pathColor = colorHexes[pathColorCombo.getSelectedIndex()];
                    customConfig.drawGrid = drawGridCheck.isSelected();
                    customConfig.gridColor = "#CCCCCC"; // Светло-серый цвет для сетки

                    try {
                        customConfig.animationDelayMs = Integer.parseInt(delayField.getText().trim());
                    } catch (NumberFormatException e) {
                        customConfig.animationDelayMs = 80;
                    }

                    openMazeWindow(image, customConfig);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MazeApp.this,
                            "Не удалось получить лабиринт: " + e.getMessage(), "Ошибка запроса", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private int parseDimension(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value >= 5 && value <= 100) return value;
        } catch (NumberFormatException e) {}
        return 30;
    }

    private void openMazeWindow(BufferedImage img, RenderConfig customConfig) {
        JFrame mazeFrame = new JFrame("Окно лабиринта (" + img.getWidth() + "x" + img.getHeight() + ")");
        mazeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        MazePanel mazePanel = new MazePanel(img, customConfig);
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