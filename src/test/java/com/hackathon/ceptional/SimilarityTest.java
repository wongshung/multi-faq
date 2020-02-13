package com.hackathon.ceptional;

import com.hackathon.ceptional.util.SimilarityUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * test methods for similarity
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
@SpringBootTest
class SimilarityTest {

    @Test
    void testSim() {
        String text1 = "昨晚上买当天白天的汽车票，怎么会购买成功，还扣钱二次，，38元39元各一次成功了？？？";
        String text2 = "昨晚上买当天白天的汽车票，怎么会购买成功，还扣钱二次，，38元39元各一次成功了？？？";
        /**
         * jaro, edit distance, damerau, jacCard, metricLcs, nGram
         */
        double sim1 = SimilarityUtil.jaroSimilarity(text1, text2);
        double sim2 = SimilarityUtil.sim(text1, text2);
        double sim3 = SimilarityUtil.damerauSimilarity(text1, text2);
        double sim4 = SimilarityUtil.jacCardSimilarity(text1, text2);
        double sim5 = SimilarityUtil.metricLcsSimilarity(text1, text2);
        double sim6 = SimilarityUtil.nGramSimilarity(text1, text2);


        Assert.assertTrue(sim1 > 0);
    }
}