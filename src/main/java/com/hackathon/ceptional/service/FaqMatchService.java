package com.hackathon.ceptional.service;

import cn.hutool.extra.tokenizer.TokenizerEngine;
import cn.hutool.extra.tokenizer.engine.jieba.JiebaEngine;
import com.hackathon.ceptional.config.Constants;
import com.hackathon.ceptional.model.ResultModel;
import com.hackathon.ceptional.model.ResultModel.Answer;
import com.hackathon.ceptional.model.SimilarityModel;
import com.hackathon.ceptional.util.SimilarityUtil;
import com.hackathon.ceptional.util.ThreadPoolUtil;
import com.qianxinyao.analysis.jieba.keyword.Keyword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * faq match service
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
@Service
@Slf4j
public class FaqMatchService {

    /**
     * match threshold, default 60
     */
    @Value("${faq.threshold}")
    private double threshold = 60.0;

    private FaqDataService faqDataService;
    @Autowired
    private void setDataInitService(FaqDataService service) {
        this.faqDataService = service;
    }

    public ResultModel doMatch(String question) {
        log.info("doMatch running for q: {}, match threshold: {}", question, threshold);
        List<Keyword> questionKeyWord = faqDataService.getKeywords(question);
        ConcurrentHashMap<Integer, Double> resultMap = new ConcurrentHashMap<>(Constants.THREAD_COUNT);
        // using async runner to do match
        CountDownLatch latchCounter = new CountDownLatch(Constants.THREAD_COUNT);
        for (int x = 0; x < Constants.THREAD_COUNT; x++) {
            int xInt = x;
            ThreadPoolUtil.executeMultiThread(() -> faqDataService.calcSimilarity(question, questionKeyWord, xInt, latchCounter, resultMap));
        }

        try {
            latchCounter.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Error waiting threads on calculating similarity, q: {}, error: {}", question, ex.getMessage());
        }

        // do ranking
        int size = resultMap.size();
        int key = -1;
        double highestScore = 0;
        log.debug("ranking result, size: {}", size);
        Iterator<Map.Entry<Integer, Double>> entries = resultMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, Double> entry = entries.next();
            double score = entry.getValue();
            if (score > highestScore) {
                highestScore = score;
                key = entry.getKey();
            }
        }


        ResultModel result = new ResultModel();
        List<Answer> resultAnswers = new ArrayList<>();
        result.setStatus(0);
        // adjust to hundred scale
        highestScore *= 100;
        BigDecimal b = new BigDecimal(highestScore);
        highestScore = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        result.setAnswer_score(highestScore);

        String finalAnswer = "";
        if (highestScore >= threshold) {
            // only return the result with 60 or higher score
            finalAnswer = faqDataService.getAnswers().get(key);
            Answer answer = result.new Answer();
            answer.setSubType("text");
            answer.setType("text");
            answer.setValue(finalAnswer);
            answer.setData(new ArrayList());
            resultAnswers.add(answer);
        }

        result.setAnswer(resultAnswers);

        log.info("doMatch done for q: {}, score: {}, answer: {}", question, highestScore, finalAnswer);

        return result;
    }

    public SimilarityModel similarity(String s1, String s2) {
        return new SimilarityModel(
                SimilarityUtil.jaroSimilarity(s1, s2), SimilarityUtil.sim(s1, s2),
                SimilarityUtil.damerauSimilarity(s1, s2), SimilarityUtil.jacCardSimilarity(s1, s2),
                SimilarityUtil.metricLcsSimilarity(s1, s2), SimilarityUtil.nGramSimilarity(s1, s2)
        );
    }
}
