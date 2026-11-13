//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TB {

    private static final String BOT_TOKEN = "9145171668:AAGZTLjksnbAKZvN8KkVv_R5PxR69OQGMuw";
    private static final String API_BASE = "https://green-band-8899.sevinnasiri85.workers.dev/bot" + BOT_TOKEN + "/";

    public static void main(String[] args) {

        String[] randomReplies = {
                "لطفا مزاحم نشوید. (لطفا موزاحیم اولمیین)",
                "خستم، خوابم میاد (یورگونام ، یوخوم گلیر)",
                "برو فردا بیا. (گت سابا گل)"
        };

        Random random = new Random();
        long lastUpdateId = 0;

        System.out.println(" ربات تلگرام(فارسی به ترکی) شروع به کار کرد...");
        System.out.println("منتظر دریافت پیام هستم...");

        while (true) {
            try {
                String updatesJson = getUpdates(lastUpdateId + 1);
                if (updatesJson == null || updatesJson.trim().equals("[]") || updatesJson.contains("\"ok\":false")) {
                    Thread.sleep(1000);
                    continue;
                }

                if (updatesJson.contains("\"update_id\"")) {
                    int idIndex = updatesJson.indexOf("\"update_id\":");
                    int idEnd = updatesJson.indexOf(",", idIndex);
                    if (idEnd == -1) idEnd = updatesJson.indexOf("}", idIndex);
                    String idStr = updatesJson.substring(idIndex + 12, idEnd).trim();
                    long currentUpdateId = Long.parseLong(idStr);

                    String chatId = extractField(updatesJson, "\"chat\":{\"id\":");
                    String text = extractField(updatesJson, "\"text\":\"");

                    if (chatId != null && text != null) {
                        System.out.println("📩 دریافت پیام از " + chatId + ": " + text);
                        String reply = randomReplies[random.nextInt(randomReplies.length)];
                        sendMessage(chatId, reply);
                    }

                    lastUpdateId = currentUpdateId;
                } else {
                    Thread.sleep(2000);
                }

            } catch (Exception e) {
                System.err.println(" خطا در حلقه اصلی: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    private static String getUpdates(long offset) throws IOException {
        String urlStr = API_BASE + "getUpdates?offset=" + offset + "&timeout=30";

        return sendGetRequest(urlStr);
    }

    private static void sendMessage(String chatId, String text) throws IOException {
        String urlStr = API_BASE + "sendMessage";
        String postData = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8) +
                "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
        sendPostRequest(urlStr, postData);
    }

    private static String sendGetRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(35000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            return response.toString();
        }
        return null;
    }

    private static void sendPostRequest(String urlStr, String postData) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("ارسال ناموفق بود، کد وضعیت: " + responseCode);
        }
        conn.getInputStream().close();
    }

    private static String extractField(String json, String fieldPrefix) {
        int start = json.indexOf(fieldPrefix);
        if (start == -1) return null;
        start += fieldPrefix.length();
        int end;
        if (fieldPrefix.endsWith("\"")) {
            end = json.indexOf("\"", start);
        } else {
            int comma = json.indexOf(",", start);
            int brace = json.indexOf("}", start);
            if (comma == -1) end = brace;
            else if (brace == -1) end = comma;
            else end = Math.min(comma, brace);
        }
        if (end == -1) return null;
        return json.substring(start, end).trim();
    }
}