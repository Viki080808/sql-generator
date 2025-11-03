package com.example.sql_generator.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.example.sql_generator.dto.SQLDataResponse;
import com.example.sql_generator.dto.SQLResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SQLService {

    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.api-key}")
    private String apiKey;

    @Value("${azure.openai.deployment-name}")
    private String deploymentName;

    private final JdbcTemplate jdbcTemplate;

    public SQLService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SQLResponse generateSQLQuery(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a SQL generator. Return only the SQL statement. Do not include any explanation, commentary, markdown, or code fences. Output the SQL exactly as text.\n\n");
//        prompt.append("Generate only the SELECT statement");
        prompt.append("Schema public has table order_details");
        prompt.append("order_details has following columns Order_identifier,Trade_identifier,Transaction_Type,Trade_Date,Settle_Date,Transaction_Currency,Settlement_Currency,Broker_Id,Broker_Email,Clearing_Broker,Cusip,ISIN,Security_id,Execution_Price,Order_Quantity,Commission_Amount,Product_Code");
        prompt.append("Schema public has table broker_details");
        prompt.append("broker_details has following columns Broker_Id,Broker_name,Broker_Email,Broker_address");
        prompt.append("table order_details is related to broker_details table using column Broker_Id from both tables");
        prompt.append("Column clearing_broker in order_details table and Broker_id column in broker_details table can be used to get clearing broker details");
        prompt.append("Consider the above prompt as just information and use broker_details only when requested");
        prompt.append("Generate the SQL only for requested details and avoid unnecessary joins");
        prompt.append(message);

        SQLResponse sqlResponse = new SQLResponse();

        String sqlGenerated = null;
        try {
            OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(endpoint)
                .apiKey(apiKey)
                .build();

            ResponseCreateParams params = ResponseCreateParams.builder()
                .input(prompt.toString())
                .model(deploymentName)
                .build();

            Response response = client.responses().create(params);

            System.out.println("Response: " + response);

            sqlGenerated = response.output().get(0).message().get().content().get(0).outputText().get().text();

            String note = null;
            if (!sqlGenerated.trim().toLowerCase().startsWith("select")) {
                note = "Generated SQL is not a SELECT statement. Review before executing.";
            }

            sqlResponse.setSql(sqlGenerated);
            sqlResponse.setPrompt(message);
            sqlResponse.setNote(note);

        }
        catch (Exception ex) {
            System.err.println("Error: "+ ex.getMessage());
        }

        return sqlResponse;

    }

    public SQLDataResponse getSQLDataResponse(String message) {
        SQLDataResponse sqlDataResponse = new SQLDataResponse();

        SQLResponse sqlResponse = generateSQLQuery(message);

        String sql = sqlResponse.getSql();
        String note = sqlResponse.getNote();

        sqlDataResponse.setPrompt(sqlResponse.getPrompt());
        sqlDataResponse.setSql(sql);
        sqlDataResponse.setNote(note);

        if (note == null) {
            List<Map<String, Object>> dataResults = executeSQLQuery(sql);
            sqlDataResponse.setRows(dataResults.size());
            sqlDataResponse.setData(dataResults);
        }

        return sqlDataResponse;
    }

    private List<Map<String, Object>> executeSQLQuery(String sql) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            var meta = rs.getMetaData();
            for (int i= 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            return row;
        });
    }

    public StreamingResponseBody exportData(String message) {
        SQLResponse sqlResponse = generateSQLQuery(message);

        String sql = sqlResponse.getSql();
        String note = sqlResponse.getNote();
        if (note != null) {
            throw new IllegalArgumentException("Only SELECT queries are allowed for export");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        return outputStream -> {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                jdbcTemplate.query(sql, rs -> {
                    var meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        writer.print(meta.getColumnName(i));
                        if (i < columnCount)
                            writer.print(",");
                    }
                    writer.println();

                    do {
                        for (int i = 1; i <= columnCount; i++) {
                            Object value = rs.getObject(i);
                            writer.print(escapeCSV(value == null ? "" : value.toString()));
                            if (i < columnCount)
                                writer.print(",");
                        }
                        writer.println();
                        writer.flush();
                    } while (rs.next());
                });
            }
            catch (Exception e) {
                throw new RuntimeException("Error generating CSV ", e);
            }
        };
    }

    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"","\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    //    @Autowired
//    CopilotClient copilotClient;
//
//    public Mono<String> generateSql(SQLRequest req) {
//        // Build a structured prompt for SQL generation
//        StringBuilder prompt = new StringBuilder();
//        prompt.append("Generate a parameterized SQL query for the following request. ");
//        prompt.append("Return only the SQL statement (no explanations). ");
//        prompt.append("Use named parameters (e.g., :param). ");
//        prompt.append("User request: ").append(req.getMessage());
//
//        CopilotRequest apiRequest = new CopilotRequest();
//        apiRequest.setPrompt(prompt.toString());
//        apiRequest.setMaxTokens(512);
//        apiRequest.setTemperature(0.0);
//
//        return copilotClient.generateSql(apiRequest)
//            .map(CopilotResponse::getGeneratedText)
//            .map(this::stripExtraneousText);
//    }
//
//    private String stripExtraneousText(String text) {
//        // Basic cleanup to attempt to extract SQL string only.
//        // Improve with regex or parser for production.
//        if (text == null) return "";
//        return text.trim();
//    }
}
