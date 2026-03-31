package com.yunlbd.flexboot4.media.gateway.gb28181;

final class Gb28181PtzUtils {

    private Gb28181PtzUtils() {
    }

    static String buildCommand(String command, Integer speed) {
        String normalized = command == null ? "STOP" : command.trim().toUpperCase();
        int value = Math.max(0, Math.min(speed == null ? 0x20 : speed, 0xFF));
        int zoom = Math.max(0, Math.min(speed == null ? 0x0F : speed / 16, 0x0F));
        int commandByte;
        int horizontal = 0;
        int vertical = 0;
        int zoomByte = 0;
        switch (normalized) {
            case "RIGHT" -> {
                commandByte = 0x01;
                horizontal = value;
            }
            case "LEFT" -> {
                commandByte = 0x02;
                horizontal = value;
            }
            case "DOWN" -> {
                commandByte = 0x04;
                vertical = value;
            }
            case "DOWN_RIGHT" -> {
                commandByte = 0x05;
                horizontal = value;
                vertical = value;
            }
            case "DOWN_LEFT" -> {
                commandByte = 0x06;
                horizontal = value;
                vertical = value;
            }
            case "UP" -> {
                commandByte = 0x08;
                vertical = value;
            }
            case "UP_RIGHT" -> {
                commandByte = 0x09;
                horizontal = value;
                vertical = value;
            }
            case "UP_LEFT" -> {
                commandByte = 0x0A;
                horizontal = value;
                vertical = value;
            }
            case "ZOOM_IN" -> {
                commandByte = 0x10;
                zoomByte = zoom;
            }
            case "ZOOM_OUT" -> {
                commandByte = 0x20;
                zoomByte = zoom;
            }
            default -> commandByte = 0x00;
        }
        int[] bytes = {0xA5, 0x0F, 0x01, commandByte, horizontal, vertical, zoomByte};
        int checksum = 0;
        for (int item : bytes) {
            checksum += item;
        }
        checksum &= 0xFF;
        return String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], checksum);
    }
}
