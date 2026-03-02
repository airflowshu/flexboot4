package com.yunlbd.flexboot4.mybatis.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.file.parse.ParsedBlock;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ParsedBlocksJsonbTypeHandler extends BaseTypeHandler<List<ParsedBlock>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<ParsedBlock>> LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ParsedBlock> parameter, JdbcType jdbcType) throws SQLException {
        PGobject jsonbObject = new PGobject();
        jsonbObject.setType("jsonb");
        try {
            jsonbObject.setValue(objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize JSONB value", e);
        }
        ps.setObject(i, jsonbObject);
    }

    @Override
    public List<ParsedBlock> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<ParsedBlock> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<ParsedBlock> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private static List<ParsedBlock> parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            throw new SQLException("Failed to parse JSONB value", e);
        }
    }
}

