package com.yunlbd.flexboot4.media.gateway.gb28181;

import com.yunlbd.flexboot4.media.dto.PlaybackRecordItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

final class Gb28181XmlUtils {

    private Gb28181XmlUtils() {
    }

    static Gb28181Message parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return Gb28181Message.empty();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            String cmdType = text(root, "CmdType");
            String deviceId = text(root, "DeviceID");
            String deviceName = firstNonBlank(text(root, "DeviceName"), text(root, "Name"));
            String status = text(root, "Status");
            Integer sn = integerValue(text(root, "SN"));
            Integer sumNum = integerValue(text(root, "SumNum"));
            Integer startNum = integerValue(text(root, "StartNum"));
            String result = text(root, "Result");
            String event = text(root, "Event");
            String manufacturer = text(root, "Manufacturer");
            String model = text(root, "Model");
            String owner = text(root, "Owner");
            String civilCode = text(root, "CivilCode");
            String address = text(root, "Address");
            String firmware = text(root, "Firmware");
            String alarmPriority = text(root, "AlarmPriority");
            String alarmMethod = text(root, "AlarmMethod");
            String alarmTime = text(root, "AlarmTime");
            String alarmDescription = text(root, "AlarmDescription");
            List<Gb28181CatalogItem> items = new ArrayList<>();
            List<PlaybackRecordItem> records = new ArrayList<>();
            Integer deviceListNum = null;
            Integer recordListNum = null;
            NodeList deviceLists = root.getElementsByTagName("DeviceList");
            for (int i = 0; i < deviceLists.getLength(); i++) {
                Node listNode = deviceLists.item(i);
                if (!(listNode instanceof Element listElement)) {
                    continue;
                }
                if (deviceListNum == null) {
                    deviceListNum = integerValue(listElement.getAttribute("Num"));
                }
                NodeList itemNodes = listElement.getElementsByTagName("Item");
                for (int j = 0; j < itemNodes.getLength(); j++) {
                    Node itemNode = itemNodes.item(j);
                    if (!(itemNode instanceof Element itemElement)) {
                        continue;
                    }
                    items.add(new Gb28181CatalogItem(
                            text(itemElement, "DeviceID"),
                            text(itemElement, "Name"),
                            text(itemElement, "Manufacturer"),
                            text(itemElement, "Model"),
                            text(itemElement, "Owner"),
                            text(itemElement, "CivilCode"),
                            text(itemElement, "Address"),
                            text(itemElement, "ParentID"),
                            text(itemElement, "Status"),
                            text(itemElement, "Longitude"),
                            text(itemElement, "Latitude"),
                            text(itemElement, "Event")
                    ));
                }
            }
            NodeList recordLists = root.getElementsByTagName("RecordList");
            for (int i = 0; i < recordLists.getLength(); i++) {
                Node listNode = recordLists.item(i);
                if (!(listNode instanceof Element listElement)) {
                    continue;
                }
                if (recordListNum == null) {
                    recordListNum = integerValue(listElement.getAttribute("Num"));
                }
                NodeList itemNodes = listElement.getElementsByTagName("Item");
                for (int j = 0; j < itemNodes.getLength(); j++) {
                    Node itemNode = itemNodes.item(j);
                    if (!(itemNode instanceof Element itemElement)) {
                        continue;
                    }
                    records.add(new PlaybackRecordItem(
                            text(itemElement, "DeviceID"),
                            text(itemElement, "Name"),
                            text(itemElement, "Address"),
                            parseTime(text(itemElement, "StartTime")),
                            parseTime(text(itemElement, "EndTime")),
                            text(itemElement, "Secrecy")
                    ));
                }
            }
            return new Gb28181Message(
                    cmdType,
                    deviceId,
                    deviceName,
                    status,
                    sn,
                    sumNum,
                    startNum,
                    deviceListNum,
                    recordListNum,
                    result,
                    event,
                    manufacturer,
                    model,
                    owner,
                    civilCode,
                    address,
                    firmware,
                    alarmPriority,
                    alarmMethod,
                    alarmTime,
                    alarmDescription,
                    items,
                    records
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse GB28181 XML", e);
        }
    }

    static String catalogQuery(String deviceId, int sn) {
        return catalogQuery(deviceId, sn, 1, 100);
    }

    static String catalogQuery(String deviceId, int sn, int startNum, int limit) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Query>
                  <CmdType>Catalog</CmdType>
                  <SN>%d</SN>
                  <DeviceID>%s</DeviceID>
                  <StartNum>%d</StartNum>
                  <Limit>%d</Limit>
                </Query>
                """.formatted(sn, deviceId, Math.max(1, startNum), Math.max(1, limit));
    }

    static String deviceInfoQuery(String deviceId, int sn) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Query>
                  <CmdType>DeviceInfo</CmdType>
                  <SN>%d</SN>
                  <DeviceID>%s</DeviceID>
                </Query>
                """.formatted(sn, deviceId);
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node == null ? null : node.getTextContent();
    }

    private static Integer integerValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private static java.time.LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    record Gb28181Message(
            String cmdType,
            String deviceId,
            String deviceName,
            String status,
            Integer sn,
            Integer sumNum,
            Integer startNum,
            Integer deviceListNum,
            Integer recordListNum,
            String result,
            String event,
            String manufacturer,
            String model,
            String owner,
            String civilCode,
            String address,
            String firmware,
            String alarmPriority,
            String alarmMethod,
            String alarmTime,
            String alarmDescription,
            List<Gb28181CatalogItem> items,
            List<PlaybackRecordItem> records
    ) {
        static Gb28181Message empty() {
            return new Gb28181Message(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of()
            );
        }
    }

    record Gb28181CatalogItem(
            String deviceId,
            String name,
            String manufacturer,
            String model,
            String owner,
            String civilCode,
            String address,
            String parentId,
            String status,
            String longitude,
            String latitude,
            String event
    ) {
    }
}
