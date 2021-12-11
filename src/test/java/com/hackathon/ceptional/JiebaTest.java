package com.hackathon.ceptional;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.qianxinyao.analysis.jieba.keyword.Keyword;
import com.qianxinyao.analysis.jieba.keyword.TFIDFAnalyzer;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * test method for jieba
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
@SpringBootTest
class JiebaTest {
    @Test
    void testWordExtract() {
        String content="汽车上可以拿几个行李箱";
        int topN = 5;
        TFIDFAnalyzer tfidfAnalyzer=new TFIDFAnalyzer();
        List<Keyword> list=tfidfAnalyzer.analyzeEx(content, topN, 1);
        for(Keyword word:list)
            System.out.println(word.getName()+":"+word.getTfidfvalue()+",");

        Assertions.assertTrue(list.size() > 0);
    }

    @Test
    void testSegment() {
        JiebaSegmenter wordSplit = new JiebaSegmenter();
        String[] sentences =
                new String[] {"这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。", "我不喜欢日本和服。", "雷猴回归人间。",
                        "工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作", "结果婚的和尚未结过婚的"};
        for (String sentence : sentences) {
            System.out.println(wordSplit.process(sentence, SegMode.INDEX).toString());
        }

        Assertions.assertTrue(true);
    }
}
