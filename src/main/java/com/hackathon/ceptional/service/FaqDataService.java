package com.hackathon.ceptional.service;

import com.hackathon.ceptional.config.Constants;
import com.hackathon.ceptional.util.ExcelUtil;
import com.hackathon.ceptional.util.HuToolUtil;
import com.hackathon.ceptional.util.SimilarityUtil;
import com.qianxinyao.analysis.jieba.keyword.Keyword;
import com.qianxinyao.analysis.jieba.keyword.TFIDFAnalyzer;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
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

    /**
     * fixed key map in regarding to thread count
     */
    private ConcurrentHashMap<Integer, HashSet<Integer> > keyMap = new ConcurrentHashMap<>(Constants.THREAD_COUNT);

    private TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();
    private static final int TOP_N = 5;

    private static final NumberFormat NF = NumberFormat.getInstance();
    private static final String NUMERIC_SPLIT_COMMA = ",";

    /**
     * algorithm weight on calculation
     */
    @Value("${faq.jaro.ratio}")
    private int jaroRatio = 4;
    @Value("${faq.sim.ratio}")
    private int simRatio = 3;
    @Value("${faq.jac.ratio}")
    private int jacRatio = 3;

    @Value("${faq.segment.method}")
    private String segmentMethod = "ikea";

    /**
     * faq adjustment parameters
     */
    @Value("${faq.threshold}")
    private double threshold= 60.0;
    @Value("${faq.miss.key.ratio}")
    private double missKeyRatio = 1.0;

    private static final String EXCEL_2007 = "xlsx";

    /**
     * init data from provided excel, now only supports 2007 format and the file content must be correct
     * @param dataFile - training set file
     */
    public void initData(File dataFile) {
        log.info("Init data begin, file : {}", dataFile.getPath());
        if (!dataFile.getName().endsWith(EXCEL_2007)) {
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

        // init keyMap
        initKeyMap();

        log.info("InitData done.");
    }

    private void initKeyMap() {
        for (int i = 0; i < Constants.THREAD_COUNT; i++) {
            HashSet<Integer> keySet = new HashSet<>();
            for (int j = 0; j < answers.size(); j++) {
                int hash = j % Constants.THREAD_COUNT;
                if (hash == i) {
                    keySet.add(j);
                }
            }
            keyMap.put(i, keySet);
        }
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
                String faq = getCellText(cell);
                if (StringUtils.isNotBlank(faq)) {
                    // valid faq, now check answer
                    cell = row.getCell(3);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String answer = getCellText(cell);
                        if (!answers.contains(faq) && StringUtils.isNotBlank(answer)) {
                            /*
                             * hackathon special requirement, set faq as answer now.
                             * normally should add answer to answers
                             */
                            answers.add(faq);
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
                String faq = getCellText(cell);
                for (List<String> faqs : faqMap.values()) {
                    if (faqs.contains(faq)) {
                        // faq found, now add related question
                        cell = row.getCell(0);
                        if (cell != null && cell.getCellType() != CellType.BLANK) {
                            String question = getCellText(cell);
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

    private String getCellText(XSSFCell cell) {
        String text;
        if (cell.getCellType().equals(CellType.NUMERIC)) {
            // handle numeric cell
            text = NF.format(cell.getNumericCellValue());
            if (text.contains(NUMERIC_SPLIT_COMMA)) {
                text = text.replace(NUMERIC_SPLIT_COMMA, "");
            }
        } else {
            text = cell.toString();
        }

        return text;
    }

    List<Keyword> getKeywords(String text) {
        return tfidfAnalyzer.analyze(text, TOP_N);
    }

    /**
     * method to calculate sentence similarity for faqs
     * @param question - input question
     * @param hash - thread hash
     * @param counter - CountDownLatch
     * @param map - result map
     */
    void calcSimilarity(String question, List<Keyword> qKeyWord, int hash,
                               CountDownLatch counter, ConcurrentHashMap<Integer, Double> map) {
        log.debug("calcSimilarity running on thread: {}, question: {}, hash: {}, result count: {}",
                Thread.currentThread().getName(), question, hash, map.size());

        double finalSim = 0;
        int finalKey = -1;
        int finalHitCount = 0;
        String matchFaq = "";
        HashSet<Integer> keys = keyMap.get(hash);
        for (Integer i : keys) {
            List<String> faqs = faqMap.get(i);
            double sectionHighSim = 0;
            String sectionResultFaq = "";
            int sectionHitCount = 0;
            for (String s : faqs) {
                Pair<Integer, Double> simResult = similarityCalc(question, qKeyWord, s);
                if (simResult.getValue() > sectionHighSim) {
                    sectionHighSim = simResult.getValue();
                    sectionResultFaq = s;
                    sectionHitCount = simResult.getKey();
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
        map.put(finalKey, finalSim);
        log.debug("calcSimilarity done on thread: {}, similarity: {}, keyword: {}, " +
                        "matched key&sentence: {} - {}, now result count: {}",
                Thread.currentThread().getName(), finalSim, finalHitCount, finalKey, matchFaq, map.size());
    }

    private Pair<Integer, Double> similarityCalc(String question, List<Keyword> qKeyWord, String faq) {
        double sim = 0;
        if (jacRatio >= 5) {
            // use previous method
            double sim1 = SimilarityUtil.jaroSimilarity(question, faq);
            double sim2 = SimilarityUtil.sim(question, faq);
            double sim3 = SimilarityUtil.jacCardSimilarity(question, faq);
            sim = (jaroRatio * sim1 + simRatio * sim2 + jacRatio * sim3) / 10;
        } else {
            // use new method
            Vector<String> v1 = null;
            Vector<String> v2 = null;
            if (segmentMethod.equals("ikea")) {
                v1 = HuToolUtil.participleIk(question);
                v2 = HuToolUtil.participleIk(faq);
            } else if (segmentMethod.equals("hanlp")) {
                v1 = HuToolUtil.participleHanLP(question);
                v2 = HuToolUtil.participleHanLP(faq);
            } else if (segmentMethod.equals("jieba")) {
                v1 = HuToolUtil.participleJieBa(question);
                v2 = HuToolUtil.participleJieBa(faq);
            } else if (segmentMethod.equals("chn")) {
                v1 = HuToolUtil.participleChinese(question);
                v2 = HuToolUtil.participleChinese(faq);
            }
            sim = HuToolUtil.getSimilarity(v1, v2);
        }

        // key word aspect
        HashSet<String> faqKeyWords = keyWordMap.get(faq);
        Set<String> hitSet = new HashSet<>();
        int hitCount = 0;
        int hitTextLen = 0;
        for (Keyword key : qKeyWord) {
            if (faqKeyWords.contains(key.getName())) {
                hitCount++;
                hitTextLen += key.getName().length();
                hitSet.add(key.getName());
            }
        }
        if (hitCount > 0) {
            double ratio = (double)(hitTextLen / faq.length() + hitTextLen / question.length()) / 2;
            sim += (1 - sim) * hitCount / faqKeyWords.size() * ratio;
        } else {
            sim *= 0.8;
        }

        // find missing faq keyword
        Set<String> excludeSet = new HashSet<>(faqKeyWords);
        excludeSet.removeAll(hitSet);
        int misLen = 0;
        for (String misKey : excludeSet) {
            misLen += misKey.length();
        }
        double misRatio = missKeyRatio * misLen / faq.length();
        if (misRatio > 0.5) {
            misRatio = 0.5;
        }
        double misSim = 1 - sim;
        if (misSim < 0.1) {
            misSim = 0.1;
        } else if (misSim > 0.4) {
            misSim = 0.4;
        }
        if (sim > threshold / 100) {
            sim -= misSim * misRatio;
        }

        return new Pair<>(hitCount, sim);
    }
}
