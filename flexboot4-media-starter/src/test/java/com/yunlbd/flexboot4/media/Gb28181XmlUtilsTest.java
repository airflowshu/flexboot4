package com.yunlbd.flexboot4.media.gateway.gb28181;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Gb28181XmlUtilsTest {

    @Test
    void shouldParseRecordInfoResponse() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                  <CmdType>RecordInfo</CmdType>
                  <SN>7</SN>
                  <DeviceID>34020000001320000001</DeviceID>
                  <RecordList Num="1">
                    <Item>
                      <DeviceID>34020000001320000001</DeviceID>
                      <Name>cam-1</Name>
                      <Address>floor-1</Address>
                      <StartTime>20260327T100000</StartTime>
                      <EndTime>20260327T103000</EndTime>
                      <Secrecy>0</Secrecy>
                    </Item>
                  </RecordList>
                </Response>
                """;

        Gb28181XmlUtils.Gb28181Message message = Gb28181XmlUtils.parse(xml);

        assertEquals("RecordInfo", message.cmdType());
        assertEquals(7, message.sn());
        assertEquals(1, message.records().size());
        assertEquals("cam-1", message.records().getFirst().name());
    }

    @Test
    void shouldParseCatalogPaginationFields() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                  <CmdType>Catalog</CmdType>
                  <SN>11</SN>
                  <DeviceID>34020000001320000001</DeviceID>
                  <StartNum>101</StartNum>
                  <SumNum>180</SumNum>
                  <DeviceList Num="80">
                    <Item>
                      <DeviceID>34020000001320000002</DeviceID>
                      <Name>cam-2</Name>
                      <Status>ON</Status>
                      <Event>ADD</Event>
                    </Item>
                  </DeviceList>
                </Response>
                """;

        Gb28181XmlUtils.Gb28181Message message = Gb28181XmlUtils.parse(xml);

        assertEquals("Catalog", message.cmdType());
        assertEquals(11, message.sn());
        assertEquals(101, message.startNum());
        assertEquals(180, message.sumNum());
        assertEquals(80, message.deviceListNum());
        assertEquals(1, message.items().size());
        assertEquals("ADD", message.items().getFirst().event());
        assertEquals("34020000001320000002", message.items().getFirst().deviceId());
        assertTrue(Gb28181XmlUtils.catalogQuery("34020000001320000001", 12, 201, 50).contains("<StartNum>201</StartNum>"));
    }
}
