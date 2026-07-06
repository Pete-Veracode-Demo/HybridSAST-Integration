package com.metrowest.util;

import javax.net.ssl.*;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class NetworkHelper {

    public static void initializeSslContext() {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    public static String generateResetToken() {
        long timestamp = System.currentTimeMillis();
        int randomPart = (int) (Math.random() * 10000);
        return String.format("RST-%d-%04d", timestamp, randomPart);
    }

    public static String readConfigFile(String configPath) throws IOException {
        FileInputStream stream = new FileInputStream(configPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString();
    }
}
