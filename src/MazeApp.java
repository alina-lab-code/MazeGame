import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;

public class MazeApp extends JFrame {
    private static final String BASE_URL = "https://backend-qcf9.onrender.com/fm1";

    private RenderConfig config;


    private JTextField apiKeyField;
    private JTextField widthField;
    private JTextField heightField;


    private JLabel wallColorLabel;
    private JLabel pathColorLabel;
    private JLabel gridLabel;
    private JLabel delayLabel;

    public MazeApp() {
        setTitle("Labyrinth/maze settings");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(9, 2, 10, 10));


        add(new JLabel(" Your API key:"));
        apiKeyField = new JTextField("0GmBa3FvOFCRTkd6WO30vHJLy2nMPRd6qWjEx0Sp0yU68PKw74DTtjJZeyJuvw5Z");
        add(apiKeyField);


        add(new JLabel("Width (5-100):"));
        widthField = new JTextField("30");
        add(widthField);

        add(new JLabel("  Height (5-100):"));
        heightField = new JTextField("30");
        add(heightField);


        add(new JLabel("  Wall color:"));
        wallColorLabel = new JLabel("Enter key and refresh...");
        add(wallColorLabel);

        add(new JLabel("  Path color:"));
        pathColorLabel = new JLabel("-");
        add(pathColorLabel);

        add(new JLabel("  draw grid:"));
        gridLabel = new JLabel("-");
        add(gridLabel);

        add(new JLabel("  animation rate (ms):"));
        delayLabel = new JLabel("-");
        add(delayLabel);


        JButton refreshBtn = new JButton("Refresh configuration");
        refreshBtn.addActionListener(e -> loadConfig());
        add(refreshBtn);

        JButton getMazeBtn = new JButton("GET MAZE");
        getMazeBtn.addActionListener(e -> downloadAndShowMaze());
        add(getMazeBtn);
    }

    private String getEnteredApiKey() {
        return apiKeyField.getText().trim();
    }

    private void loadConfig() {
        String currentKey = getEnteredApiKey();
        if (currentKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your API key first!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        wallColorLabel.setText("Loading...");

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
                    gridLabel.setText(config.drawGrid ? "Да (" + config.gridColor + ")" : "No");
                    delayLabel.setText(String.valueOf(config.animationDelayMs));
                } catch (Exception e) {
                    wallColorLabel.setText("Error");
                    JOptionPane.showMessageDialog(MazeApp.this,
                            "Network error or wrong API key: " + e.getMessage(), "API error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void downloadAndShowMaze() {
        String currentKey = getEnteredApiKey();
        if (currentKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, " Enter API key first and get maze!");
            return;
        }

        if (config == null) {
            JOptionPane.showMessageDialog(this, "Press key first 'Refresh configuration' for password check.");
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
                    if (image == null) throw new Exception(" Wrong format answer from server");

                    openMazeWindow(image, config);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MazeApp.this,
                            //added MeowError from Alina :3
                            "Sorry we can't get your maze + e.getMessage()", "MeowError(Error) request", JOptionPane.ERROR_MESSAGE);
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

    private void openMazeWindow(BufferedImage img, RenderConfig config) {
        JFrame mazeFrame = new JFrame("Maze window(" + img.getWidth() + "x" + img.getHeight() + ")");
        mazeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        MazePanel mazePanel = new MazePanel(img, config);
        JButton solveBtn = new JButton("Check solution");
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