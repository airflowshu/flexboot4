package com.yunlbd.flexboot4.apikey;

import java.util.List;

public record ApiKeySnapshot(
        long version,
        List<ApiKeyRule> keys
) {
}

