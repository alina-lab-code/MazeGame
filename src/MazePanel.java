

    class MazePanel extends JPanel {
        private final int width;
        private final int height;
        private final boolean[][] walls;
        private final RenderConfig config;

        private final int cellSize = 15; // Размер одного квадрата в окне Swing
        private List<Point> solutionPath = new ArrayList<>();
        private int animationIndex = -1;
        private javax.swing.Timer animationTimer;

        public MazePanel(BufferedImage img, RenderConfig config) {
            this.config = config;
            this.width = img.getWidth();
            this.height = img.getHeight();
            this.walls = new boolean[width][height];

            // Анализ пикселей исходного изображения
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Если пиксель абсолютно белый — это проход, иначе — стена сервера
                    walls[x][y] = !(r == 255 && g == 255 && b == 255);
                }
            }
            setPreferredSize(new Dimension(width * cellSize, height * cellSize));
        }

        public void startSolving() {
            solutionPath = MazeSolver.findPath(walls, width, height);

            if (solutionPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Путь от верхнего левого угла к нижнему правому не существует!", "Нет решения", JOptionPane.WARNING_MESSAGE);
                return;
            }


            animationIndex = 0;
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            animationTimer = new javax.swing.Timer(config.animationDelayMs, e -> {
                if (animationIndex < solutionPath.size()) {
                    animationIndex++;
                    repaint();
                } else {
                    animationTimer.stop();
                }
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            Color wallColor = Color.decode(config.wallCellColor);
            Color pathColor = Color.decode(config.pathColor);
            Color gridColor = Color.decode(config.gridColor);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (walls[x][y]) {
                        g2d.setColor(wallColor);
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                    g2d.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);

                    if (config.drawGrid) {
                        g2d.setColor(gridColor);
                        g2d.drawRect(x * cellSize, y * cellSize, cellSize, cellSize);
                    }
                }
            }

            if (animationIndex >= 0) {
                g2d.setColor(pathColor);
                for (int i = 0; i < animationIndex; i++) {
                    Point p = solutionPath.get(i);
                    // Отрисовка с небольшим внутренним отступом для эстетики пути
                    g2d.fillRect(p.x * cellSize + 2, p.y * cellSize + 2, cellSize - 4, cellSize - 4);
                }
            }
        }
    }
}
