package com.anner.rag.generator;


import com.anner.rag.step.embed.RagEmbedGenerateStep;
import com.anner.rag.step.load.RagLoadGenerateStep;
import com.anner.rag.step.split.RagSplitGenerateStep;

import java.util.List;

/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public interface RagGenerator {

    RagSplitGenerateStep splitStep();

    /**
     * 支持多个不同格式的数据一起加载进来
     *
     * @return 多个不同的加载器
     */
    List<RagLoadGenerateStep> loadStep();

    RagEmbedGenerateStep embedStep();

    String sourceDirPath();

    String outputFilePath();


    void generate() throws Exception;
}
