import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkService {

        // Вспомогательный метод для подстановки заголовков авторизации
        private static HttpURLConnection prepareConnection(String urlStr, String apiKey) throws Exception {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Передаем токен в стандартном формате Bearer
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            // На случай, если твой бэкенд ждет ключ в кастомном заголовке, раскомментируй строку ниже:
            // conn.setRequestProperty("X-API-Key", apiKey);

            return conn;
        }

        public static String sendGet(String urlStr, String apiKey) throws Exception {
            HttpURLConnection conn = prepareConnection(urlStr, apiKey);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP Error: " + responseCode);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }

        public static BufferedImage downloadImage(String urlStr, String apiKey) throws Exception {
            HttpURLConnection conn = prepareConnection(urlStr, apiKey);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP Error: " + responseCode);
            }

            return ImageIO.read(conn.getInputStream());
        }
    }

