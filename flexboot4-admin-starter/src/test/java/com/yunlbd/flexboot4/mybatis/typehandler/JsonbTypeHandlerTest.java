package com.yunlbd.flexboot4.mybatis.typehandler;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JsonbTypeHandlerTest {

    @Test
    void setNonNullParameter_shouldWriteJsonbPgObject() throws Exception {
        JsonbTypeHandler handler = new JsonbTypeHandler();
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setNonNullParameter(ps, 1, Map.of("k", "v"), null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps, times(1)).setObject(eq(1), captor.capture());

        Object obj = captor.getValue();
        assertInstanceOf(PGobject.class, obj);
        PGobject pg = (PGobject) obj;
        assertEquals("jsonb", pg.getType());
        assertNotNull(pg.getValue());
        assertTrue(pg.getValue().contains("\"k\""));
        assertTrue(pg.getValue().contains("\"v\""));
    }
}

