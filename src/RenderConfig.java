
   class RenderConfig {
        public String wallCellColor;
        public String pathColor;
        public boolean drawGrid;
        public String gridColor;
        public int animationDelayMs;

        public static RenderConfig fromJson(String json) {
            RenderConfig config = new RenderConfig();
            config.wallCellColor = extractJsonValue(json, "wallCellColor");
            config.pathColor = extractJsonValue(json, "pathColor");
            config.drawGrid = Boolean.parseBoolean(extractJsonValue(json, "drawGrid"));
            config.gridColor = extractJsonValue(json, "gridColor");
            config.animationDelayMs = Integer.parseInt(extractJsonValue(json, "animationDelayMs"));
            return config;
        }
        private static String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\":";
            int index = json.indexOf(pattern);
            if (index == -1) return "";
            int start = index + pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim().replace("\"", "");
        }
    }


