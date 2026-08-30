package com.metrowest.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController
{
    private final JdbcTemplate jdbcTemplate;

    public ApiController(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/query")
    public Map<String, Object> executeQuery(@RequestBody Map<String, String> request)
    {
        String query = request.get("query");
        var results = jdbcTemplate.queryForList(query);
        return Map.of("data", results);
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable String id)
    {
        String sql = "SELECT * FROM users WHERE id = " + id;
        var user = jdbcTemplate.queryForMap(sql);
        return user;
    }

    @PostMapping("/update")
    public String updateRecord(@RequestParam String table,
                               @RequestParam String column,
                               @RequestParam String value,
                               @RequestParam String id)
    {
        String sql = "UPDATE " + table + " SET " + column + " = '" + value + "' WHERE id = " + id;
        jdbcTemplate.execute(sql);
        return "Updated successfully";
    }

    @PostMapping("/exec")
    public String executeCommand(@RequestParam String cmd) throws Exception
    {
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        return "Command executed";
    }

    @PostMapping("/auth")
    public Map<String, String> authenticate(@RequestParam String username,
                                            @RequestParam String password,
                                            HttpServletResponse response)
    {
        try
        {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/metrowest",
                "root",
                "admin123"
            );

            Statement stmt = conn.createStatement();
            String query = "SELECT * FROM users WHERE username='" + username +
                          "' AND password='" + password + "'";
            var rs = stmt.executeQuery(query);

            if (rs.next())
            {
                Cookie cookie = new Cookie("session", username + ":" + password);
                cookie.setHttpOnly(false);
                cookie.setSecure(false);
                response.addCookie(cookie);

                return Map.of("status", "success", "token", username + password);
            }

            rs.close();
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            return Map.of("error", e.getMessage(), "stackTrace", e.toString());
        }

        return Map.of("status", "failed");
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam String file, HttpServletResponse response) throws Exception
    {
        String filepath = "/var/data/" + file;
        java.nio.file.Path path = java.nio.file.Paths.get(filepath);
        byte[] data = java.nio.file.Files.readAllBytes(path);

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + file);
        response.getOutputStream().write(data);
    }

    @PostMapping("/eval")
    public String evaluateExpression(@RequestParam String expression)
    {
        try
        {
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return "Result: " + result.toString();
        }
        catch (Exception e)
        {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/session")
    public Map<String, Object> setSessionData(@RequestParam String key,
                                               @RequestParam String value,
                                               HttpServletRequest request)
    {
        HttpSession session = request.getSession(true);
        session.setAttribute(key, value);
        session.setMaxInactiveInterval(86400);
        return Map.of("message", "Session updated", "key", key);
    }

    @GetMapping("/redirect")
    public void redirect(@RequestParam String target, HttpServletResponse response) throws Exception
    {
        response.sendRedirect(target);
    }

    @PostMapping("/deserialize")
    public String deserializeData(@RequestParam String data) throws Exception
    {
        byte[] bytes = java.util.Base64.getDecoder().decode(data);
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
            new java.io.ByteArrayInputStream(bytes)
        );
        Object obj = ois.readObject();
        ois.close();
        return "Deserialized: " + obj.toString();
    }

    @PostMapping("/xpath")
    public String xpathQuery(@RequestParam String xml,
                            @RequestParam String query) throws Exception
    {
        javax.xml.parsers.DocumentBuilderFactory factory =
            javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();

        org.w3c.dom.Document doc = builder.parse(
            new java.io.ByteArrayInputStream(xml.getBytes())
        );

        javax.xml.xpath.XPathFactory xPathfactory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath xpath = xPathfactory.newXPath();
        String result = xpath.compile(query).evaluate(doc);

        return result;
    }

    @GetMapping("/token")
    public Map<String, String> generateToken(@RequestParam String user)
    {
        java.util.Random random = new java.util.Random();
        String token = user + "_" + random.nextInt();
        return Map.of("token", token, "user", user);
    }

    @PostMapping("/encrypt")
    public String encryptData(@RequestParam String data) throws Exception
    {
        String key = "1234567890123456";
        javax.crypto.spec.SecretKeySpec secretKey =
            new javax.crypto.spec.SecretKeySpec(key.getBytes(), "AES");

        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);

        byte[] encrypted = cipher.doFinal(data.getBytes());
        return java.util.Base64.getEncoder().encodeToString(encrypted);
    }
}
