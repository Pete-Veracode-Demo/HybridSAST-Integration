package com.metrowest.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/customer")
    public List<Map<String, Object>> getCustomerReport(
            @RequestParam String customerName,
            @RequestParam(required = false) String orderStatus) {

        String sql = "SELECT o.*, c.name, c.email FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.id " +
                     "WHERE c.name LIKE '%" + customerName + "%'";

        if (orderStatus != null && !orderStatus.isEmpty()) {
            sql += " AND o.status = '" + orderStatus + "'";
        }

        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/export")
    public byte[] exportReport(@RequestParam String reportId, @RequestParam String format) throws IOException {
        String baseDir = "/var/reports/generated/";
        String filename = reportId + "." + format;
        return Files.readAllBytes(Paths.get(baseDir + filename));
    }

    @PostMapping("/invoice/process")
    public String processInvoiceXml(@RequestBody String xmlData) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

        String invoiceNumber = doc.getElementsByTagName("invoice_number").item(0).getTextContent();
        return "Processed invoice: " + invoiceNumber;
    }

    @GetMapping("/legacy/verify")
    public String verifyChecksum(@RequestParam String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(data.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @PostMapping("/analytics/load")
    public Object loadAnalyticsData(@RequestBody byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }
}
