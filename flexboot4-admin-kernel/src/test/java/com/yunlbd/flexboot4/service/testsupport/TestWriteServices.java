package com.yunlbd.flexboot4.service.testsupport;

import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;

public final class TestWriteServices {
    private TestWriteServices() {
    }

    public interface InterfaceAnnotatedWriteService {
        @BumpTableVersion(tables = "interface_table")
        boolean writeFromInterfaceAnnotation();
    }

    public static class InterfaceAnnotatedWriteServiceImpl implements InterfaceAnnotatedWriteService {
        @Override
        public boolean writeFromInterfaceAnnotation() {
            return true;
        }
    }

    public interface ImplementationAnnotatedWriteService {
        boolean writeFromImplementationAnnotation();
    }

    public static class ImplementationAnnotatedWriteServiceImpl implements ImplementationAnnotatedWriteService {
        @Override
        @BumpTableVersion(tables = "implementation_table")
        public boolean writeFromImplementationAnnotation() {
            return true;
        }
    }
}
