package com.hackathon.ceptional.service;

import com.hackathon.ceptional.config.Constants;
import com.hackathon.ceptional.model.ResultModel;
import com.hackathon.ceptional.model.ResultModel.Answer;
import com.hackathon.ceptional.util.ConcurrentArrayList;
import com.hackathon.ceptional.util.ThreadPoolUtil;
import com.qianxinyao.analysis.jieba.keyword.Keyword;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

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

    private FaqDataService faqDataService;
    @Autowired
    private void setDataInitService(FaqDataService service) {
        this.faqDataService = service;
    }

    public ResultModel doMatch(String question) {
        faqDataService.clearResultMap();
        Enumeration<Integer> keys = faqDataService.getFaqMap().keys();
        // split to 16 sections
        List<List<Integer> > keyList = new ArrayList<>(Constants.THREAD_COUNT);
        for (int i = 0; i < Constants.THREAD_COUNT; i++) {
            List<Integer> intList = new ArrayList<>();
            keyList.add(intList);
        }
        int counter = 0;
        while (keys.hasMoreElements()) {
            int index = counter % Constants.THREAD_COUNT;
            Integer key = keys.nextElement();
            keyList.get(index).add(key);
            counter++;
        }

        List<Keyword> questionKeyWord = faqDataService.getKeywords(question);
        // using async runner to do match
        CountDownLatch latchCounter = new CountDownLatch(Constants.THREAD_COUNT);
        keyList.forEach(x ->
                ThreadPoolUtil.executeMultiThread(() -> faqDataService.calcSimilarity(question, questionKeyWord, x, latchCounter))
        );

        try {
            latchCounter.await();
        } catch (InterruptedException ex) {
            log.error("Error waiting threads on calculating similarity: {}", ex.getMessage());
        }

        // do ranking
        ConcurrentHashMap<Integer, Double> simResultList = faqDataService.getResultMap();
        int size = simResultList.size();
        int key = -1;
        double highestScore = 0;
        log.info("ranking result, size: {}", size);
        Iterator<Map.Entry<Integer, Double>> entries = simResultList.entrySet().iterator();
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
        BigDecimal b = new BigDecimal(highestScore);
        highestScore = b.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
        result.setAnswerScore(highestScore * 100);
        log.info("match score: {}", highestScore);

        if (highestScore >= Constants.FAQ_THRESHOLD) {
            // only return the result with 60 or higher score
            String finalAnswer = faqDataService.getAnswers().get(key);
            log.info("faq hit, answer: {}", finalAnswer);
            Answer answer = result.new Answer();
            answer.setSubType("text");
            answer.setType("text");
            answer.setValue(finalAnswer);
            resultAnswers.add(answer);
        }

        result.setAnswer(resultAnswers);

        return result;
    }
}
