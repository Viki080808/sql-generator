package com.example.sql_generator.controller;

import com.example.sql_generator.dto.SQLDataResponse;
import com.example.sql_generator.dto.SQLRequest;
import com.example.sql_generator.dto.SQLResponse;
import com.example.sql_generator.service.SQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/sql")
public class SQLController {

    @Autowired
    SQLService sqlService;

//    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//    public Mono<ResponseEntity<SQLResponse>> generate(@Valid @RequestBody SQLRequest request) {
//        return sqlService.generateSql(request)
//            .map(sql -> {
//                String note = null;
//                // Basic safety: deny non-SELECT suggestions by quick check
//                if (!sql.trim().toLowerCase().startsWith("select")) {
//                    note = "Generated SQL is not a SELECT statement. Review before executing.";
//                }
//                return ResponseEntity.ok(new SQLResponse(sql, note));
//            });
//    }

    @PostMapping("/generate")
    public ResponseEntity<SQLResponse> generateSQL(@RequestBody SQLRequest request) {

        String  prompt = request.getMessage();
        SQLResponse sqlResponse = sqlService.generateSQLQuery(prompt);

        return ResponseEntity.ok(sqlResponse);
    }

    @PostMapping("/getData")
    public ResponseEntity<SQLDataResponse> getSQLData(@RequestBody SQLRequest request) {

        String  prompt = request.getMessage();
        SQLDataResponse sqlDataResponse = sqlService.getSQLDataResponse(prompt);

        return ResponseEntity.ok(sqlDataResponse);
    }

    @PostMapping("/downloadData")
    public ResponseEntity<StreamingResponseBody> exportData(@RequestBody SQLRequest request) {

        String  prompt = request.getMessage();

        StreamingResponseBody csvData = sqlService.exportData(prompt);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.csv\"")
            .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset-UTF-8")
            .body(csvData);
    }
}
