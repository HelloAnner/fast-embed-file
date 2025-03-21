package com.anner.rag.step.load;

import com.anner.rag.step.RagGenerateStep;
import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public interface RagLoadGenerateStep extends RagGenerateStep {

    /**
     * 从文件夹中加载文档内容
     *
     * @param dir 文件夹内容
     * @return documents
     */
    List<Document> loadDocuments(String dir) throws Exception;

    enum Type {
        JSON, MARKDOWN
    }
}
