package com.anner.rag.generator;



import com.anner.rag.step.load.MarkdownLoadGenerateStep;
import com.anner.rag.step.load.RagLoadGenerateStep;

import java.util.Collections;
import java.util.List;

/**
 * 知识库向量库生成器
 *
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class KnowledgeRagGenerator extends BaseRagGenerator {

    public KnowledgeRagGenerator(String sourceDir, String targetFile) {
        super(sourceDir, targetFile);
    }

    @Override
    public List<RagLoadGenerateStep> loadStep() {
        // 知识库内容来自md文件
        return Collections.singletonList(
                new MarkdownLoadGenerateStep()
        );
    }
}
