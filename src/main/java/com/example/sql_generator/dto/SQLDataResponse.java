package com.example.sql_generator.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class SQLDataResponse extends SQLResponse {

    private Integer rows;

    private List<Map<String, Object>> data;

}
