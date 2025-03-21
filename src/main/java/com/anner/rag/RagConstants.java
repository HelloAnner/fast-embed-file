package com.anner.rag;


/**
 * @author Anner
 * @since 12.0
 * Created on 2025/3/20
 */
public class RagConstants {

    public final static class EMBED_FILE_PATH {
        // 公式内容向量化文件
        public final static String FORMULA = "static/formula.json";
        // 知识库向量化文件
        public final static String KNOWLEDGE = "static/knowledge.json";
        // JS向量化文件
        public final static String JS = "static/js.json";
    }


    public final static class SPLIT_PARAMS {
        // 每一个 chunk 最大多少个 token
        public final static int MAX_TOKENS_PER_CHUNK = 1000;

        // 重叠的token，防止语义割裂
        public final static int OVER_LAP_TOKENS = 10;
    }
}
