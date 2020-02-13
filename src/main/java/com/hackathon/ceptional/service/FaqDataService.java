package com.hackathon.ceptional.service;

import com.hackathon.ceptional.util.ExcelUtil;
import com.hackathon.ceptional.util.SimilarityUtil;
import com.qianxinyao.analysis.jieba.keyword.Keyword;
import com.qianxinyao.analysis.jieba.keyword.TFIDFAnalyzer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * data init methods, from provided training set
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
@Service
@Slf4j
@Setter
@Getter
public class FaqDataService {
    /**
     * ArrayList to store answers.
     */
    private List<String> answers = new ArrayList<>();

    /**
     * map to store answers and related questions
     * key - index of answers, value - related question
     */
    private ConcurrentHashMap<Integer, List<String> > faqMap = new ConcurrentHashMap<>();

    /**
     * key word map, using tf-idf method
     */
    private ConcurrentHashMap<String, HashSet<String> > keyWordMap = new ConcurrentHashMap<>();

    private static final int SHEET_COUNT = 2;

    private ConcurrentHashMap<Integer, Double> resultMap = new ConcurrentHashMap<>();

    private TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();
    private static final int TOP_N = 5;

    /**
     * init data from provided excel, now only supports 2007 format and the file content must be correct
     * @param dataFile - training set file
     */
    public void initData(File dataFile) {
        log.info("Init data begin, file : {}", dataFile.getPath());
        if (!dataFile.getName().endsWith("xlsx")) {
            log.error("incorrect training data file format!");
            return;
        }

        XSSFWorkbook workBook = (XSSFWorkbook)ExcelUtil.readAsWordBook(dataFile);
        if (workBook.getNumberOfSheets() != SHEET_COUNT) {
            log.error("wrong data file!");
            return;
        }

        // read faq and answer
        XSSFSheet sheet = workBook.getSheetAt(0);
        readFaqAndAnswer(sheet);

        // read related questions
        sheet = workBook.getSheetAt(1);
        readRelateQuestion(sheet);

        // set keyWord Map
        setKeyWordMap();

        log.info("initData done");
    }

    private void setKeyWordMap() {
        faqMap.forEach((k, v) ->
            v.forEach(s -> {
                HashSet<String> keyWordSet = new HashSet<>();
                List<Keyword> faqKeyWords = tfidfAnalyzer.analyze(s, TOP_N);
                faqKeyWords.forEach(keyword -> keyWordSet.add(keyword.getName()));
                keyWordMap.put(s, keyWordSet);
            })
        );
    }

    private void readFaqAndAnswer(XSSFSheet sheet) {
        XSSFRow row;
        XSSFCell cell;
        int rowCount = sheet.getPhysicalNumberOfRows();
        log.info("begin to read faq, num rows = {}", rowCount);
        // read from 3rd row
        for (int i = 2, rowIndex = 2; rowIndex < rowCount; i++) {
            row = sheet.getRow(i);
            if (row == null) {
                continue;
            } else {
                rowIndex++;
            }

            // read faq and answer
            cell = row.getCell(1);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String faq = cell.toString();
                if (StringUtils.isNotBlank(faq)) {
                    // valid faq, now check answer
                    cell = row.getCell(3);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String answer = cell.toString();
                        if (!answers.contains(answer) && StringUtils.isNotBlank(answer)) {
                            // valid answer, now add answers list and faqMap
                            answers.add(answer);
                            int key = answers.size() - 1;
                            if (faqMap.containsKey(key)) {
                                List<String> value = faqMap.get(key);
                                if (!value.contains(faq)) {
                                    value.add(faq);
                                }
                            } else {
                                List<String> value = new ArrayList<>();
                                value.add(faq);
                                faqMap.put(key, value);
                            }
                        }
                    }
                }
            }
        }
    }

    private void readRelateQuestion(XSSFSheet sheet) {
        XSSFRow row;
        XSSFCell cell;
        int rowCount = sheet.getPhysicalNumberOfRows();
        log.info("begin to read related questions, num rows = {}", rowCount);
        // read from 2nd row
        for (int i = 1, rowIndex = 1; rowIndex < sheet.getPhysicalNumberOfRows(); i++) {
            row = sheet.getRow(i);
            if (row == null) {
                continue;
            } else {
                rowIndex++;
            }

            // read faq
            cell = row.getCell(1);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String faq = cell.toString();
                for (List<String> faqs : faqMap.values()) {
                    if (faqs.contains(faq)) {
                        // faq found, now add related question
                        cell = row.getCell(0);
                        if (cell != null && cell.getCellType() != CellType.BLANK) {
                            String question = cell.toString();
                            if (StringUtils.isNotBlank(question)) {
                                // valid question, add to faqMap
                                faqs.add(question);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    void clearResultMap() {
        resultMap.clear();
    }

    List<Keyword> getKeywords(String text) {
        return tfidfAnalyzer.analyze(text, TOP_N);
    }

    /**
     * method to calculate sentence similarity for faqs
     * @param question - input question
     * @param keys - key list in faqMap
     * @param counter - CountDownLatch
     */
    public void calcSimilarity(String question, List<Keyword> qKeyWord, List<Integer> keys, CountDownLatch counter) {
        log.info("calcSimilarity running on thread: {}, question: {}, keyList: {}",
                Thread.currentThread().getName(), question, StringUtils.join(keys, ","));

        double finalSim = 0;
        int finalKey = -1;
        int finalHitCount = 0;
        String matchFaq = "";
        for (Integer i : keys) {
            List<String> faqs = faqMap.get(i);
            double sectionHighSim = 0;
            String sectionResultFaq = "";
            int sectionHitCount = 0;
            for (String s : faqs) {
                double sim1 = SimilarityUtil.sim(question, s);
                double sim2 = SimilarityUtil.jacCardSimilarity(question, s);
                double sim3 = SimilarityUtil.metricLcsSimilarity(question, s);
                double sim4 = SimilarityUtil.nGramSimilarity(question, s);

                double sim = (sim1 + sim2 + sim3 + sim4) / 4;

                // key word aspect
                HashSet<String> faqKeyWords = keyWordMap.get(s);
                int hitCount = 0;
                int hitTextLen = 0;
                for (Keyword key : qKeyWord) {
                    if (faqKeyWords.contains(key.getName())) {
                        hitCount++;
                        hitTextLen += key.getName().length();
                    }
                }
                if (hitCount > 0) {
                    double ratio = (double)hitTextLen / s.length();
                    sim += (1 - sim) * hitCount / faqKeyWords.size() * ratio;
                } else {
                    sim *= 0.8;
                }

                if (sim > sectionHighSim) {
                    sectionHighSim = sim;
                    sectionResultFaq = s;
                    sectionHitCount = hitCount;
                }
                if (sectionHighSim > 0.95) {
                    // very high similarity, directly end this loop
                    break;
                }
            }

            if (sectionHighSim > finalSim) {
                finalSim = sectionHighSim;
                finalKey = i;
                matchFaq = sectionResultFaq;
                finalHitCount = sectionHitCount;
            }
            if (finalSim > 0.95) {
                // very high similarity, also end this loop
                break;
            }
        }

        counter.countDown();
        log.info("calcSimilarity done on thread: {}, similarity: {}, keyword: {}, " +
                        "matched key&sentence: {} - {}, now result count: {}",
                Thread.currentThread().getName(), finalSim, finalHitCount, finalKey, matchFaq, resultMap.size());
        resultMap.put(finalKey, finalSim);
    }
}
