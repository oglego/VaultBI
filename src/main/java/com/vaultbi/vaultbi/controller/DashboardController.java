package com.vaultbi.vaultbi.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Path temp = Files.createTempFile("vaultbi-", ".csv");
        Files.write(temp, file.getBytes());
        return Map.of("csvPath", temp.toAbsolutePath().toString());
    }

    @PostMapping("/query")
    public List<Map<String, Object>> runQuery(
            @RequestParam String csvPath,
            @RequestParam String sql
    ) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE OR REPLACE TABLE uploaded AS SELECT * FROM read_csv_auto('" + csvPath + "')");
            ResultSet rs = stmt.executeQuery(sql);

            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
            return results;
        }
    }
}
