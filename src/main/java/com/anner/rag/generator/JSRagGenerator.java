package com.anner.rag.generator;



import com.anner.rag.step.load.MarkdownLoadGenerateStep;
import com.anner.rag.step.load.RagLoadGenerateStep;

import java.util.Collections;
import java.util.List;

/**
 * JS 的 markdown 向量化
 *
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class JSRagGenerator extends BaseRagGenerator {
    public JSRagGenerator(String sourceDir, String targetFile) {
        super(sourceDir, targetFile);
    }

    @Override
    public List<RagLoadGenerateStep> loadStep() {
        return Collections.singletonList(
                new MarkdownLoadGenerateStep()
        );
    }
}
