package com.yunlbd.flexboot4.service.kb;

import java.util.Collection;

public interface KnowledgeBaseIndexingService {

    int indexFiles(String kbId, Collection<String> fileIds);
}

