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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import static java.util.stream.Collectors.toList;

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
    private ConcurrentHashMap<String, List<Keyword> > keyWordMap = new ConcurrentHashMap<>();

    private static final int SHEET_COUNT = 2;

    /**
     * fixed key map in regarding to thread count
     */
    private ConcurrentHashMap<Integer, HashSet<Integer> > keyMap = new ConcurrentHashMap<>(Constants.THREAD_COUNT);

    private TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();
    private static final int TOP_N = 5;

    private static final NumberFormat NF = NumberFormat.getInstance();
    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private static final String NUMERIC_SPLIT_COMMA = ",";

    /**
     * algorithm method & weight on calculation
     */
    @Value("${faq.algorithm.conf}")
    private int algorithm = 2;
    @Value("${faq.sim.method}")
    private String simMethod = "hutool";
    @Value("${faq.tfidf.ratio}")
    private int tfidfRatio = 7;
    @Value("${faq.sim.ratio}")
    private int simRatio = 3;
    @Value("${faq.segment.method}")
    private String segmentMethod = "ikea";

    /**
     * faq adjustment parameters
     */
    @Value("${faq.threshold}")
    private double threshold= 60.0;

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
                List<Keyword> faqKeyWords = tfidfAnalyzer.analyze(s, TOP_N);
                keyWordMap.put(s, faqKeyWords);
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
        String finalSimInfo = "";
        String matchFaq = "";
        HashSet<Integer> keys = keyMap.get(hash);
        for (Integer i : keys) {
            List<String> faqs = faqMap.get(i);
            double sectionHighSim = 0;
            String sectionResultFaq = "";
            String sectionSimInfo = "";
            for (String s : faqs) {
                Pair<String, Double> simResult = similarityCalc(question, qKeyWord, s);
                if (simResult.getValue() > sectionHighSim) {
                    sectionHighSim = simResult.getValue();
                    sectionResultFaq = s;
                    sectionSimInfo = simResult.getKey();
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
                finalSimInfo = sectionSimInfo;
            }
            if (finalSim > 0.95) {
                // very high similarity, also end this loop
                break;
            }
        }

        counter.countDown();
        map.put(finalKey, finalSim);
        log.debug("calcSimilarity done on thread: {}, similarity: {}, sim-tf: {}, " +
                        "matched key&sentence: {} - {}, now result count: {}",
                Thread.currentThread().getName(), DF.format(finalSim), finalSimInfo, finalKey, matchFaq, map.size());
    }

    private Pair<String, Double> similarityCalc(String question, List<Keyword> qKeyWord, String faq) {
        double sim = 0;
        if ("debatty".equals(simMethod)) {
            // use previous method
            double sim1 = SimilarityUtil.jaroSimilarity(question, faq);
            double sim2 = SimilarityUtil.sim(question, faq);
            double sim3 = SimilarityUtil.jacCardSimilarity(question, faq);
            sim = (5 * sim1 + 3 * sim2 + 2 * sim3) / 10;
        } else {
            // use hutool method
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

        String simInfo = "";
        // algorithm != 0, need to use tfidf
        if (algorithm != 0) {
            List<Keyword> faqKeyWords = keyWordMap.get(faq);
            // calculate tfidf
            double tfidfSim = duplexKeywordSim(faqKeyWords, qKeyWord);
            List<String> qKeys = qKeyWord.parallelStream().map(Keyword::getName).collect(toList());
            List<String> faqKeys = faqKeyWords.parallelStream().map(Keyword::getName).collect(toList());
            long hitCount = faqKeys.parallelStream().filter(item -> qKeys.contains(item)).count();

            if (algorithm == 2) {
                // dynamic ratio
                int qLen = question.length();
                int minLen = faq.length() < qLen ? faq.length() : qLen;
                int tfRatio = 1 + (minLen - 1) / 4;
                tfRatio += hitCount * 2;
                if (tfRatio > 9) {
                    tfRatio = 9;
                }

                int simRatio = 10 - tfRatio;
                simInfo = DF.format(sim) + " * " + simRatio + " & " + DF.format(tfidfSim) + " * " + tfRatio
                        + ", count: " + hitCount;
                // final sim
                sim = (sim * simRatio + tfidfSim * tfRatio) / 10;
            } else {
                // only use tfidf similarity
                sim = tfidfSim;
                simInfo = DF.format(sim) + " * 0 & " + DF.format(tfidfSim) + " * 10, count: " + hitCount;
            }
        } else {
            // else, just use sim value
            simInfo = DF.format(sim) + " * 10 & 0 * 0";
        }

        return new Pair<>(simInfo, sim);
    }

    /**
     * for faq usage
     * @param faq - faq
     * @param q - question
     * @return normalized result
     */
    double faqTfidfSim(String faq, String q) {
        List<Keyword> faqKeys = tfidfAnalyzer.analyze(faq, TOP_N);
        List<Keyword> qKeys = tfidfAnalyzer.analyze(q, TOP_N);

        return simplexKeywordSim(faqKeys, qKeys);
    }

    /**
     * normal usage of tfidf like symmetric similarity caculation
     * @param s1 - text 1
     * @param s2 - text 2
     * @return normalized result
     */
    double tfidfSim(String s1, String s2) {
        List<Keyword> keys1 = tfidfAnalyzer.analyze(s1, TOP_N);
        List<Keyword> keys2 = tfidfAnalyzer.analyze(s2, TOP_N);

        return duplexKeywordSim(keys1, keys2);
    }

    /**
     * simplex keyword similarity
     * @param faq - target faq
     * @param q - question
     * @return similarity result
     */
    private double simplexKeywordSim(List<Keyword> faq, List<Keyword> q) {
        List<String> faqName = new ArrayList<>();
        double faqTotalTfidf = 0;
        for (Keyword key : faq) {
            faqTotalTfidf += key.getTfidfvalue();
            faqName.add(key.getName());
        }

        double result = 0;
        for (Keyword key : q) {
            if (faqName.contains(key.getName())) {
                result += key.getTfidfvalue();
            } else {
                result -= key.getTfidfvalue();
            }
        }

        // normalization
        if (result > faqTotalTfidf) {
            result = faqTotalTfidf;
        } else if (result < 0) {
            result = 0;
        }
        // prevent NaN result
        if (faqTotalTfidf <= 0) {
            faqTotalTfidf = 1.0;
        }
        result = result / faqTotalTfidf;

        return result;
    }

    /**
     * bidirectional key word similarity,
     * @param keys1 - keyword list 1
     * @param keys2 - keyword list 2
     * @return similarity result
     */
    private double duplexKeywordSim(List<Keyword> keys1, List<Keyword> keys2) {
        List<String> keys1NameList = new ArrayList<>();
        List<Double> keys1ValueList = new ArrayList<>();
        List<String> keys2NameList = new ArrayList<>();
        List<Double> keys2ValueList = new ArrayList<>();
        for (Keyword key : keys1) {
            keys1NameList.add(key.getName());
            keys1ValueList.add(key.getTfidfvalue());
        }

        for (Keyword key : keys2) {
            keys2NameList.add(key.getName());
            keys2ValueList.add(key.getTfidfvalue());
        }

        double result = 0.0;
        double total = 0.0;
        Pair<Double, Double> p = sumTfidf(keys1NameList, keys1ValueList, keys2NameList, result, total);
        result = p.getKey();
        total = p.getValue();

        p = sumTfidf(keys2NameList, keys2ValueList, keys1NameList, result, total);
        result = p.getKey();
        total = p.getValue();

        // normalization
        if (total == 0) {
            total = 1.0;
        }
        double min = (-2) * total;
        result = (result - min) / (total - min);

        return result;
    }

    private Pair<Double, Double> sumTfidf(List<String> l1, List<Double> d1, List<String> l2, Double result, Double total) {
        for (int i = 0; i < l1.size(); i++) {
            if (l2.contains(l1.get(i))) {
                result += d1.get(i);
            } else {
                result -= 2 * d1.get(i);
            }
            total += d1.get(i);
        }

        return new Pair<>(result, total);
    }
}
