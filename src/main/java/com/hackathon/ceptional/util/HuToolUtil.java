package com.hackathon.ceptional.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.tokenizer.Result;
import cn.hutool.extra.tokenizer.TokenizerEngine;
import cn.hutool.extra.tokenizer.TokenizerUtil;
import cn.hutool.extra.tokenizer.Word;
import cn.hutool.extra.tokenizer.engine.hanlp.HanLPEngine;
import cn.hutool.extra.tokenizer.engine.jieba.JiebaEngine;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hankcs.hanlp.HanLP;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import javax.util.streamex.EntryStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map.Entry;

/**
 * hu tool util for word segment and similarity
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/14
 */
@Slf4j
public class HuToolUtil {

    /**
     * threshold
     */
    private static double THRESHOLD = 0.2;

    /**
     * 通过Ik 进行将句子分词
     *
     * @param text - input text
     * @return segment list
     */
    public static Vector<String> participleIk(String text) {
        //对输入进行分词
        Vector<String> str = new Vector<>();
        try {
            StringReader reader = new StringReader(text);
            //当为true时，分词器进行最大词长切分
            IKSegmenter ik = new IKSegmenter(reader, false);
            Lexeme lexeme;
            while ((lexeme = ik.next()) != null) {
                str.add(lexeme.getLexemeText());
            }
            if (str.size() == 0) {
                return null;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return str;
    }

    /**
     * 结巴分词
     *
     * @param text - input text
     * @return segment result
     */
    public static Vector<String> participleJieBa(String text) {
        JiebaEngine engine = new JiebaEngine();
        Result results = engine.parse(text);
        //输出：这 两个 方法 的 区别 在于 返回 值
        String result = CollUtil.join((Iterator<Word>) results, ",");
        return new Vector<>(Arrays.asList(result.split(",")));
    }

    /**
     * 中文分词
     *
     * @param text - input text
     * @return segment result list
     */
    public static Vector<String> participleChinese(String text) {
        //自动根据用户引入的分词库的jar来自动选择使用的引擎
        TokenizerEngine engine = TokenizerUtil.createEngine();
        //解析文本
        //String text = "这两个方法的区别在于返回值";
        Result results = engine.parse(text);
        //输出：这 两个 方法 的 区别 在于 返回 值
        String result = CollUtil.join((Iterator<Word>) results, ",");
        return new Vector<>(Arrays.asList(result.split(",")));
    }

    /**
     * 采用 HanLP 进行自定义分词
     */
    public static Vector<String> participleHanLP(String text) {
        TokenizerEngine engine = new HanLPEngine();
        //解析文本
        //String text = "这两个方法的区别在于返回值";
        Result results = engine.parse(text);
        //输出：这 两个 方法 的 区别 在于 返回 值
        String result = CollUtil.join((Iterator<Word>) results, ",");
        return new Vector<>(Arrays.asList(result.split(",")));
    }

    /**
     * 返回百分比计算
     *
     * @param TOne - list one
     * @param TTwo - list 2
     * @return similarity value
     */
    public static double getSimilarity(Vector<String> TOne, Vector<String> TTwo) {
        int sizeOne, sizeTwo;
        if (TOne != null && (sizeOne = TOne.size()) > 0 && TTwo != null && (sizeTwo = TTwo.size()) > 0) {
            Map<String, double[]> T = new HashMap<>();
            //T1和T2的并集T
            String index;
            for (int i = 0; i < sizeOne; i++) {
                index = TOne.get(i);
                if (index != null) {
                    double[] c;
                    c = new double[2];
                    //T1的语义分数Ci
                    c[0] = 1;
                    //T2的语义分数Ci
                    c[1] = THRESHOLD;
                    T.put(index, c);
                }
            }
            for (int i = 0; i < sizeTwo; i++) {
                index = TTwo.get(i);
                if (index != null) {
                    double[] c = T.get(index);
                    if (c != null && c.length == 2) {
                        //T2中也存在，T2的语义分数=1
                        c[1] = 1;
                    } else {
                        c = new double[2];
                        //T1的语义分数Ci
                        c[0] = THRESHOLD;
                        //T2的语义分数Ci
                        c[1] = 1;
                        T.put(index, c);
                    }
                }
            }
            //开始计算，百分比
            Iterator<String> it = T.keySet().iterator();
            //S1、S2
            double sOne = 0, sTwo = 0, Ssum = 0;
            while (it.hasNext()) {
                double[] c = T.get(it.next());
                Ssum += c[0] * c[1];
                sOne += c[0] * c[0];
                sTwo += c[1] * c[1];
            }
            //百分比
            return Ssum / Math.sqrt(sOne * sTwo);
        } else {
            log.error("HuToolUtil-getSimilarity, 传入参数有问题！");
        }

        return 0;
    }

    /**
     * Java利用hanlp完成语句相似度分析
     *
     * @param sentenceOne - sentence 1
     * @param sentenceTwo - sentence 2
     * @return similarity value
     */
    public static double findSimilarity(String sentenceOne, String sentenceTwo) {
        List<String> sentOneWords = getSplitWords(sentenceOne);
        List<String> sentTwoWords = getSplitWords(sentenceTwo);
        List<String> allWords = mergeList(sentOneWords, sentTwoWords);
        int[] statisticOne = statistic(allWords, sentOneWords);
        int[] statisticTwo = statistic(allWords, sentTwoWords);
        double dividend = 0;
        double divisor1 = 0;
        double divisor2 = 0;
        int length = statisticOne.length;
        for (int i = 0; i < length; i++) {
            dividend += statisticOne[i] * statisticTwo[i];
            divisor1 += Math.pow(statisticOne[i], 2);
            divisor2 += Math.pow(statisticTwo[i], 2);
        }
        return dividend / (Math.sqrt(divisor1) * Math.sqrt(divisor2));
    }

    private static int[] statistic(List<String> allWords, List<String> sentWords) {
        int[] result = new int[allWords.size()];
        int size = allWords.size();
        for (int i = 0; i < size; i++) {
            result[i] = Collections.frequency(sentWords, allWords.get(i));
        }
        return result;
    }

    /**
     * 去重
     *
     * @param listOne - list one
     * @param listTwo - list two
     * @return merged list
     */
    private static List<String> mergeList(List<String> listOne, List<String> listTwo) {
        List<String> result = new ArrayList<>();
        result.addAll(listOne);
        result.addAll(listTwo);
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 过滤标签
     *
     * @param sentence
     * @return
     */
    private static List<String> getSplitWords(String sentence) {
        // 去除掉html标签
        sentence = Jsoup.parse(sentence.replace(" ", "")).body().text();
        // 标点符号会被单独分为一个Term，去除之
        return HanLP.segment(sentence).stream().map(a -> a.word).filter(s -> !("`~!@#$^&*()=|{}':;',\\[\\]" +
                ".<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？ ").contains(s)).collect(Collectors.toList());
    }

    /**
     * IKSegmenter分词计算文章关键字
     * @param text - input text
     * @return map
     */
    public static Map getWordFreqMap(String text) {
        Map<String, Integer> wordMap = new HashMap<>(16);
        IKSegmenter ikSegmenter = new IKSegmenter(new StringReader(text), false);
        Lexeme lexeme;
        try {
            while ((lexeme = ikSegmenter.next()) != null) {
                if (lexeme.getLexemeText().length() > 1) {
                    if (wordMap.containsKey(lexeme.getLexemeText())) {
                        wordMap.put(lexeme.getLexemeText(), wordMap.get(lexeme.getLexemeText()) + 1);
                    } else {
                        wordMap.put(lexeme.getLexemeText(), 1);
                    }
                }
            }
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }

        return wordMap;
    }

    @SuppressWarnings("unchecked")
    public static Map mergeMap(Map<String, Integer> map1, Map<String, Integer> map2) {
        return EntryStream.of(map1).append(EntryStream.of(map2)).toMap((e1, e2) -> (e1 + e2));
    }

    /**
     * Map 按value值从大到小排序
     * @param map - input map
     * @param flag - order flag, 1 - asc, 0 - desc
     * @return sorted map
     */
    public static Map<String, Integer> sortMapByValue(Map<String, Integer> map, int flag) {
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        if (flag == 1) {
            map.entrySet().stream().sorted(Comparator.comparing(Entry::getValue))
                    .forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));
        } else {
            map.entrySet().stream().sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                    .forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));
        }

        return sortedMap;
    }

    /**
     * 取出sorted map前n个元素
     * @param map - input sorted map, must be sorted
     * @param n - number
     * @return sub map
     */
    public static Map<String, Integer> subMap(Map<String, Integer> map, int n) {
        List<Entry<String, Integer> > lists = new ArrayList<>(map.entrySet());
        Map<String, Integer> subMap = new HashMap<>(n);
        if (lists.size() > n) {
            lists.subList(0, n).forEach(x -> subMap.put(x.getKey(), x.getValue()));
        } else {
            lists.forEach(x -> subMap.put(x.getKey(), x.getValue()));
        }

        return subMap;
    }

    /**
     * 取Map集合的交集（String, Integer）
     *
     * @param map1 大集合
     * @param map2 小集合
     * @return 两个集合的交集, 并且value相加
     */
    public static Map<String, Integer> getIntersectionSetByGuava(Map<String, Integer> map1, Map<String, Integer> map2) {
        Set<String> bigMapKey = map1.keySet();
        Set<String> smallMapKey = map2.keySet();
        Set<String> differenceSet = Sets.intersection(bigMapKey, smallMapKey);
        Map<String, Integer> result = Maps.newHashMap();
        for (String key : differenceSet) {
            result.put(key, map1.get(key) + map2.get(key));
        }
        return result;
    }
}
