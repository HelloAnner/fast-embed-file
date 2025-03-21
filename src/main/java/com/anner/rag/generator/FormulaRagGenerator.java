package com.anner.rag.generator;


import com.anner.rag.step.load.JsonLoadGenerateStep;
import com.anner.rag.step.load.MarkdownLoadGenerateStep;
import com.anner.rag.step.load.RagLoadGenerateStep;

import java.util.Arrays;
import java.util.List;

/**
 * 公式向量库生成器
 *
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class FormulaRagGenerator extends BaseRagGenerator {

    public FormulaRagGenerator(String sourceDir, String targetFile) {
        super(sourceDir, targetFile);
    }

    @Override
    public List<RagLoadGenerateStep> loadStep() {
        return Arrays.asList(
                // 公式的原始信息是json文件 和 部分知识库的md文件信息
                new JsonLoadGenerateStep(),
                new MarkdownLoadGenerateStep()
        );
    }
}
