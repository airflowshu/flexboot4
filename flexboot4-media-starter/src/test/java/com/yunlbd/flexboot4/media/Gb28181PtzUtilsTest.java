package com.yunlbd.flexboot4.media.gateway.gb28181;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Gb28181PtzUtilsTest {

    @Test
    void shouldBuildDirectionalPtzCommand() {
        assertEquals("A50F01011F0000D5", Gb28181PtzUtils.buildCommand("RIGHT", 31));
        assertEquals("A50F0108001F00DC", Gb28181PtzUtils.buildCommand("UP", 31));
    }

    @Test
    void shouldBuildZoomCommand() {
        assertEquals("A50F0110000006CB", Gb28181PtzUtils.buildCommand("ZOOM_IN", 96));
        assertEquals("A50F0120000006DB", Gb28181PtzUtils.buildCommand("ZOOM_OUT", 96));
    }
}
