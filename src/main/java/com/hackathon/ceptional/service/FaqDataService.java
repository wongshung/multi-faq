package com.hackathon.ceptional.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     * map to store word frequency for faq, collect data from faq and its related questions
     */
    private ConcurrentHashMap<Integer, Map<String, Integer> > wordFreqMap = new ConcurrentHashMap<>();

    /**
     * key word map, using tf-idf method, for single sentence mode
     */
    private ConcurrentHashMap<String, List<Keyword> > keyWordMap = new ConcurrentHashMap<>();

    /**
     * key word map, using tf-idf method, for combine sentence mode
     */
    private ConcurrentHashMap<Integer, List<Keyword> > combineKeywordMap = new ConcurrentHashMap<>();

    /**
     * synonym map, to store synonyms for keywords
     */
    private ConcurrentHashMap<String, List<String> > synonymMap = new ConcurrentHashMap<>();

    private static final int SHEET_COUNT = 2;

    /**
     * fixed key map in regarding to thread count
     */
    private ConcurrentHashMap<Integer, HashSet<Integer> > keyMap = new ConcurrentHashMap<>(Constants.THREAD_COUNT);

    private TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();

    private static final NumberFormat NF = NumberFormat.getInstance();
    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private static final String NUMERIC_SPLIT_COMMA = ",";
    private Pattern symbolPattern = Pattern.compile("[`~☆★!@#$%^&*()+=|{}':;,\\[\\]》·.<>/?~！@#￥%……（）——+|{}【】‘；：”“’。，、？]");
    private Pattern numPattern = Pattern.compile("^[-\\+]?[\\d]*$");

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
    private String segmentMethod = IKEA;
    @Value("${faq.jaro.ratio}")
    private int jaroRatio = 5;
    @Value("${faq.jac.ratio}")
    private int jacRatio = 3;
    @Value("${faq.ed.ratio}")
    private int edRatio = 2;

    /**
     * 0-single sentence mode, 1-combine sentence mode
     */
    @Value("${faq.tfidf.mode}")
    private int tfidfMode = 0;

    /**
     * synonym mode, 1-enable synonym, 0-disable synonym
     */
    @Value("${faq.synonym.mode}")
    private int synonymMode = 0;
    /**
     * synonym url
     */
    @Value("${faq.synonym.url}")
    private String synonymUrl = "";

    private final HttpService httpService = new HttpService();

    /**
     * faq adjustment parameters
     */
    @Value("${faq.top.count}")
    private int topCount = 5;
    @Value("${faq.threshold}")
    private double threshold = 60.0;
    @Value("${faq.freq.ratio}")
    private double freqRatio = 0.4;
    @Value("${faq.exclude.threshold}")
    private double excludeThreshold = 0.85;

    private static final String EXCEL_2007 = "xlsx";

    /**
     * word segment methods
     */
    private static final String JIEBA = "jieba";
    private static final String IKEA = "ikea";
    private static final String IKEA2 = "ikea2";
    private static final String HANLP = "hanlp";
    private static final String CHN = "chn";
    private static final String COMBO = "combo";

    /**
     * ikea分词模式，1-最小细分，2-智能合并模式
     */
    private int iKeaMode = 1;

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

        // set iKeaMode
        if (segmentMethod.equals(IKEA2)) {
            iKeaMode = 2;
        } else if (COMBO.equals(segmentMethod)) {
            iKeaMode = 4;
        }

        // set keyWord Map
        setKeyWordMap();

        // init keyMap
        initKeyMap();

        // init word frequency map
        initWordFreqMap();

        // init synonym map
        initSynonymMap();

        log.info("InitData done. Now config: segmentMethod-{}, simMethod-{}, tfidfRatio-{}, freqRatio-{}, " +
                        "jaroRatio-{}, tfidfMode-{}, topN-{}, synonymMode-{}",
                segmentMethod, simMethod, tfidfRatio, freqRatio, jaroRatio, tfidfMode, topCount, synonymMode);
    }

    private void initSynonymMap() {
        if (synonymMode == 1) {
            // need to init synonym map
            HashSet<String> keywordSet = new HashSet<>();
            if (tfidfMode == 0) {
                // single sentence mode
                keyWordMap.forEach((k, v) ->
                        v.forEach(keyword -> keywordSet.add(keyword.getName())));
            } else if (tfidfMode == 1) {
                // combine sentence mode
                combineKeywordMap.forEach((k, v) ->
                        v.forEach(keyword -> keywordSet.add(keyword.getName())));
            }

            keywordSet.forEach(s -> {
                List<String> synonymList = getSynonyms(s);
                synonymMap.put(s, synonymList);
            });
            log.info("init synonym map done!");
        }
    }

    public List<String> getSynonyms(String s) {
        List<String> resultList = new ArrayList<>();
        s = purge(s);
        if (StringUtils.isNotBlank(s) && s.length() > 1 && !isInteger(s)) {
            String getSynonymUrl = synonymUrl.concat(s);
            try {
                String getResult = httpService.doGet(URI.create(getSynonymUrl));
                JsonObject jsonObject = JsonParser.parseString(getResult).getAsJsonObject();
                if (jsonObject.has("word")) {
                    JsonArray jArray = jsonObject.get("word").getAsJsonArray();
                    int count = 0;
                    for (JsonElement je : jArray) {
                        resultList.add(je.getAsString());
                        count++;
                        if (count >= topCount) {
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("s={}, msg={}", s, ex);
            }
        }

        return resultList;
    }

    @SuppressWarnings("unchecked")
    private void initWordFreqMap() {
        for (int i = 0; i < faqMap.size(); i++) {
            List<String> material = faqMap.get(i);
            Map<String, Integer> finalMap = new HashMap<>(16);
            for (String s : material) {
                Map<String, Integer> wordMap;
                if (segmentMethod.equals(IKEA) || segmentMethod.equals(COMBO)) {
                    wordMap = HuToolUtil.getWordFreqMap(s, 0);
                } else {
                    wordMap = HuToolUtil.getWordFreqMap(s, 1);
                }

                finalMap = HuToolUtil.mergeMap(wordMap, finalMap);
            }
            finalMap = HuToolUtil.sortMapByValue(finalMap, 0);
            finalMap = HuToolUtil.subMap(finalMap, 2 * topCount);
            wordFreqMap.put(i, finalMap);
        }
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
        if (tfidfMode == 0) {
            // single sentence mode
            faqMap.forEach((k, v) ->
                    v.forEach(s -> {
                        List<Keyword> faqKeyWords;
                        if (segmentMethod.equals(IKEA) || segmentMethod.equals(IKEA2) || segmentMethod.equals(COMBO)) {
                            faqKeyWords = tfidfAnalyzer.analyzeEx(s, topCount, iKeaMode);
                        } else {
                            faqKeyWords = tfidfAnalyzer.analyze(s, topCount);
                        }
                        keyWordMap.put(s, faqKeyWords);
                    })
            );
        } else if (tfidfMode == 1) {
            // combine sentence mode
            faqMap.forEach((k, v) -> {
                String combineStr = "";
                for (String s : v) {
                    combineStr = combineStr.concat("|").concat(s);
                }
                List<Keyword> combineKeyWords;
                if (segmentMethod.equals(IKEA) || segmentMethod.equals(IKEA2) || segmentMethod.equals(COMBO)) {
                    combineKeyWords = tfidfAnalyzer.analyzeEx(combineStr, topCount, iKeaMode);
                } else {
                    combineKeyWords = tfidfAnalyzer.analyze(combineStr, topCount);
                }
                combineKeywordMap.put(k, combineKeyWords);
            });
        }
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
                            faq = faq.toUpperCase();
                            if (faqMap.containsKey(key)) {
                                List<String> value = faqMap.get(key);
                                if (!value.contains(faq)) {
                                    // to upper case
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
                faq = faq.toUpperCase();
                for (List<String> faqs : faqMap.values()) {
                    if (faqs.contains(faq)) {
                        // faq found, now add related question
                        cell = row.getCell(0);
                        if (cell != null && cell.getCellType() != CellType.BLANK) {
                            String question = getCellText(cell);
                            if (StringUtils.isNotBlank(question)) {
                                // valid question, to uppercase and add to faqMap
                                question = question.toUpperCase();
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
        List<Keyword> keys;
        if (segmentMethod.equals(IKEA) || segmentMethod.equals(IKEA2) || segmentMethod.equals(COMBO)) {
            keys = tfidfAnalyzer.analyzeEx(text, topCount, iKeaMode);
        } else {
            keys = tfidfAnalyzer.analyze(text, topCount);
        }
        return keys;
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
        question = question.toUpperCase();
        log.debug("calcSimilarity running on thread: {}, question: {}, hash: {}, result count: {}",
                Thread.currentThread().getName(), question, hash, map.size());

        double finalSim = 0;
        int finalKey = -1;
        String finalSimInfo = "";
        String matchFaq = "";
        int finalFreqCount = 0;
        HashSet<Integer> keys = keyMap.get(hash);
        for (Integer i : keys) {
            List<String> faqs = faqMap.get(i);
            double sectionHighSim = 0;
            String sectionResultFaq = "";
            String sectionSimInfo = "";
            for (String s : faqs) {
                Pair<String, Double> simResult = similarityCalc(i, question, qKeyWord, s);
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
            // frequency map
            int freqCount = handleWordFreq(i, question);
            if(sectionHighSim >= 0.95) {
                // very high similarity, break directly
                finalSim = sectionHighSim;
                finalKey = i;
                matchFaq = sectionResultFaq;
                finalSimInfo = sectionSimInfo;
                finalFreqCount = freqCount;
                break;
            }

            double adjustRatio = (double)(freqCount % 20) / 20;
            sectionHighSim += (1-sectionHighSim) * adjustRatio * freqRatio * sectionHighSim;
            if (sectionHighSim > finalSim) {
                finalSim = sectionHighSim;
                finalKey = i;
                matchFaq = sectionResultFaq;
                finalSimInfo = sectionSimInfo;
                finalFreqCount = freqCount;
            }
        }

        counter.countDown();
        map.put(finalKey, finalSim);
        log.debug("calcSimilarity done on thread: {}, similarity: {}, sim-tf: {}, freqCount: {}," +
                        " matched key&q: {} - {}, now result count: {}",
                Thread.currentThread().getName(), DF.format(finalSim), finalSimInfo, finalFreqCount,
                finalKey, matchFaq, map.size());
    }

    @SuppressWarnings("unchecked")
    private int handleWordFreq(int key, String question) {
        Map<String, Integer> freqMap = wordFreqMap.get(key);
        Map<String, Integer> questionMap;
        if (segmentMethod.equals(IKEA) || segmentMethod.equals(COMBO)) {
            questionMap = HuToolUtil.getWordFreqMap(question, 0);
        } else {
            questionMap = HuToolUtil.getWordFreqMap(question, 1);
        }
        questionMap = HuToolUtil.getIntersectionSetByGuava(freqMap, questionMap);
        int total = 0;
        for (Entry<String, Integer> e : questionMap.entrySet()) {
            total += e.getValue();
        }

        return total;
    }

    private Pair<String, Double> similarityCalc(int key, String question, List<Keyword> qKeyWord, String faq) {
        double sim;

        List<Keyword> faqKeyWords;
        // get keywords by tfidf mode
        if (tfidfMode == 0) {
            faqKeyWords = keyWordMap.get(faq);
        } else {
            faqKeyWords = combineKeywordMap.get(key);
        }
        if (synonymMode == 1) {
            // check if question could be adjusted
            String oldQuestion = question;
            boolean bReplaced = false;
            for (Keyword keyword : qKeyWord) {
                if (!faq.contains(keyword.getName())) {
                    for (Keyword faqKeyword : faqKeyWords) {
                        List<String> faqKeywordSynonym = synonymMap.get(faqKeyword.getName());
                        if (faqKeywordSynonym.contains(keyword.getName())) {
                            // replace question
                            question = question.replaceAll(keyword.getName(), faqKeyword.getName());
                            bReplaced = true;
                            break;
                        }
                    }
                }
                if (bReplaced) {
                    // only replace one
                    break;
                }
            }
            if (bReplaced) {
                log.info("q replaced on thread: {}, old q: {}, new q: {}", Thread.currentThread().getName(),
                        oldQuestion, question);
            }

        }

        if ("debatty".equals(simMethod)) {
            // use previous method
            double sim1 = SimilarityUtil.jaroSimilarity(question, faq);
            double sim2 = SimilarityUtil.sim(question, faq);
            double sim3 = SimilarityUtil.jacCardSimilarity(question, faq);
            sim = (jaroRatio * sim1 + edRatio * sim2 + jacRatio * sim3) / 10;
        } else {
            // use hutool method
            Vector<String> v1 = null;
            Vector<String> v2 = null;
            if (IKEA.equals(segmentMethod) || IKEA2.equals(segmentMethod)) {
                v1 = HuToolUtil.participleIk(question);
                v2 = HuToolUtil.participleIk(faq);
            } else if (HANLP.equals(segmentMethod)) {
                v1 = HuToolUtil.participleHanLP(question);
                v2 = HuToolUtil.participleHanLP(faq);
            } else if (JIEBA.equals(segmentMethod)) {
                v1 = HuToolUtil.participleJieBa(question);
                v2 = HuToolUtil.participleJieBa(faq);
            } else if (CHN.equals(segmentMethod)) {
                v1 = HuToolUtil.participleChinese(question);
                v2 = HuToolUtil.participleChinese(faq);
            }
            sim = HuToolUtil.getSimilarity(v1, v2);
        }

        String simInfo;
        // algorithm != 0, need to use tfidf
        if (algorithm != 0) {
            // calculate tfidf
            double tfidfSim = normalizedKeywordSim(faqKeyWords, qKeyWord);
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
                // final sim, if sim very high, use it directly, do not do ratio calculation
                boolean flag = sim > excludeThreshold && tfidfSim > 0.1;
                if (!flag) {
                    sim = (sim * simRatio + tfidfSim * tfRatio) / 10;
                } else {
                    log.debug("reach exclude threshold, sim: {}, tfidfSim: {}", sim, tfidfSim);
                }

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
        faq = faq.toUpperCase();
        q = q.toUpperCase();
        List<Keyword> faqKeys = getKeywords(faq);
        List<Keyword> qKeys = getKeywords(q);
        return simplexKeywordSim(faqKeys, qKeys);
    }

    /**
     * symmetric tfidf similarity calculation
     * @param s1 - text 1
     * @param s2 - text 2
     * @return normalized tfidf result
     */
    double tfidfSim(String s1, String s2) {
        s1 = s1.toUpperCase();
        s2 = s2.toUpperCase();
        List<Keyword> keys1 = getKeywords(s1);
        List<Keyword> keys2 = getKeywords(s2);
        return duplexKeywordSim(keys1, keys2);
    }

    /**
     * tfidf sim after normalized and remove duplicated info
     * @param s1 - text1
     * @param s2 - text2
     * @return result sim
     */
    public double normalizedTfidfSim(String s1, String s2) {
        s1 = s1.toUpperCase();
        s2 = s2.toUpperCase();
        List<Keyword> keys1 = getKeywords(s1);
        List<Keyword> keys2 = getKeywords(s2);
        return normalizedKeywordSim(keys1, keys2);
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
     * bidirectional & normalized key word similarity,
     * @param keys1 - keyword list 1
     * @param keys2 - keyword list 2
     * @return similarity result
     */
    private double normalizedKeywordSim(List<Keyword> keys1, List<Keyword> keys2) {
        Map<String, Double> keyMap1 = keys1.stream().collect(Collectors.toMap(Keyword::getName, Keyword::getTfidfvalue));
        Map<String, Double> keyMap2 = keys2.stream().collect(Collectors.toMap(Keyword::getName, Keyword::getTfidfvalue));

        List<String> removeList1 = getRemoveList(keyMap1, keyMap2);
        List<String> removeList2 = getRemoveList(keyMap2, keyMap1);

        removeList1.forEach(keyMap1::remove);
        removeList2.forEach(keyMap2::remove);

        double total1 = 0;
        double total2 = 0;
        double tfidf1 = 0;
        double tfidf2 = 0;
        Set<Entry<String, Double>> set1 = keyMap1.entrySet();
        for (Entry<String, Double> entry : set1) {
            total1 += entry.getValue();
            if (keyMap2.containsKey(entry.getKey())) {
                tfidf1 += entry.getValue();
            }
        }
        Set<Entry<String, Double>> set2 = keyMap2.entrySet();
        for (Entry<String, Double> entry : set2) {
            total2 += entry.getValue();
            if (keyMap1.containsKey(entry.getKey())) {
                tfidf2 += entry.getValue();
            }
        }
        // normalized calculation
        total1 = (total1 == 0 ? 1 : total1);
        total2 = (total2 == 0 ? 1 : total2);
        return (tfidf1 / total1 + tfidf2 / total2) / 2;
    }

    private List<String> getRemoveList(Map<String, Double> keyMap1, Map<String, Double> keyMap2) {
        List<String> toRemoveList1 = new ArrayList<>();
        Set<Entry<String, Double>> set1 = keyMap1.entrySet();
        for (Entry<String, Double> entry : set1) {
            String s = entry.getKey();
            for (Entry<String, Double> entry2 : set1) {
                if (entry2.getKey().equals(s)) {
                    continue;
                }
                if (entry2.getKey().contains(s)) {
                    // longer key found
                    if (keyMap2.containsKey(entry2.getKey())) {
                        toRemoveList1.add(s);
                    } else if (keyMap2.containsKey(s)){
                        toRemoveList1.add(entry2.getKey());
                    } else {
                        // no related, remove shorter one
                        toRemoveList1.add(s);
                    }
                }
            }
        }

        return toRemoveList1;
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

    private String purge(String s) {
        // remove space
        s = s.replaceAll(" ", "");
        // match special characters
        Matcher m = symbolPattern.matcher(s);
        // remove special characters
        s = m.replaceAll("").trim().replace(" ", "").replace("\\", "");
        return s;
    }


    /**
     * 判断是否为整数
     * @param str 传入的字符串
     * @return 是整数返回true,否则返回false
     */
    private boolean isInteger(String str) {
        return numPattern.matcher(str).matches();
    }

}
