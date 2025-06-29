package com.vaultbi.vaultbi.controller;

import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/llm")
public class CopilotController {

    @PostMapping("/suggest")
    public Map<String, String> suggestSQL(@RequestBody Map<String, String> payload) throws IOException {
        String schema = payload.get("schema");
        String question = payload.get("question");

        String prompt = """
        <bos><start_of_turn>user
        Translate this question into DuckDB SQL.

        Table name: uploaded
        Schema:
        %s

        Question: %s
        SQL:
        """.formatted(schema, question);

        ProcessBuilder pb = new ProcessBuilder(
            "llama",
            "-m", "/Users/Shared/llama/gemma-3n-E2B-it-Q4_K_M.gguf",
            "-p", prompt,
            //"./llama.cpp/main",                         // Path to llama.cpp binary
            //"-m", "models/gemma-3n-E2B.gguf",           // Path to local Gemma model
            "-p", prompt,
            "-n", "200",
            "--temp", "0.0", 
            "--top-k", "64", 
            "--top-p", "0.95"
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
            if (line.trim().endsWith(";")) { // or another end condition
                break;
            }
        }
        proc.destroy(); // kill the process
        String output = sb.toString();

        // Extract generated SQL from output
        String sql = output.replaceAll("(?s).*SQL:\\s*", "").trim();
        System.out.println("LLM output: " + output);
        System.out.println("Extracted SQL: " + sql);
        return Map.of("sql", sql);
    }
}
