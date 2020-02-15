package com.hackathon.ceptional;

import cn.hutool.Hutool;
import com.hackathon.ceptional.util.HuToolUtil;
import com.hackathon.ceptional.util.SimilarityUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Vector;

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
        String text1 = "出票失败怎么没赔付";
        String text2 = "购票失败红包没了怎么回事";
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

    @Test
    void testHuSim() throws Exception {
        String s1 = "购买汽车票电话号码错误";
        String s2 = "预订手机号写错了怎么办";
        Vector<String> v1 = HuToolUtil.participleChinese(s1);
        Vector<String> v2 = HuToolUtil.participleChinese(s2);
        Vector<String> v3 = HuToolUtil.participleHanLP(s1);
        Vector<String> v4 = HuToolUtil.participleHanLP(s2);
        Vector<String> v5 = HuToolUtil.participleIk(s1);
        Vector<String> v6 = HuToolUtil.participleIk(s2);
        Vector<String> v7 = HuToolUtil.participleJieBa(s1);
        Vector<String> v8 = HuToolUtil.participleJieBa(s2);

        double rawSim = HuToolUtil.findSimilarity(s1, s2);
        double chineseSim = HuToolUtil.getSimilarity(v1, v2);
        double hanlpSim = HuToolUtil.getSimilarity(v3, v4);
        double ikSim = HuToolUtil.getSimilarity(v5, v6);
        double jiebaSim = HuToolUtil.getSimilarity(v7, v8);

        Assert.assertTrue(rawSim > 0 && chineseSim > 0 && hanlpSim > 0 && ikSim > 0 && jiebaSim > 0);
    }
}
