package com.anner.rag;

import com.anner.rag.generator.FormulaRagGenerator;
import com.anner.rag.generator.JSRagGenerator;

/**
 * 管理一下目前存在的多个 generator
 *
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class RagGeneratorManager {

    private final static RagGeneratorManager instance = new RagGeneratorManager();

    public static RagGeneratorManager getInstance() {
        return instance;
    }

    private RagGeneratorManager() {
    }



    /**
     * 调用重新生成向量文件
     */
    public void generateFormulaEmbedFile() {
        try {
            FormulaRagGenerator formulaRagGenerator = new FormulaRagGenerator("docs/formula/", RagConstants.EMBED_FILE_PATH.FORMULA);
            formulaRagGenerator.generate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用重新生成知识库的向量文件
     */
    public void generateKnowledgeEmbedFile() {
        try {
            FormulaRagGenerator formulaRagGenerator = new FormulaRagGenerator("docs/knowledge/", RagConstants.EMBED_FILE_PATH.KNOWLEDGE);
            formulaRagGenerator.generate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void generateJSEmbedFile() {
        try {
            JSRagGenerator jsRagGenerator = new JSRagGenerator("docs/js/", RagConstants.EMBED_FILE_PATH.JS);
            jsRagGenerator.generate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
