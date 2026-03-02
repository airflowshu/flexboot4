package com.yunlbd.flexboot4.operlog;

import java.util.concurrent.CompletionStage;

public interface OperLogSink {
    CompletionStage<Void> write(OperLogRecord log);
}

