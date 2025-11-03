package com.example.sql_generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SQLResponse {

    private String prompt;

    private String sql;

    private String note;
}
