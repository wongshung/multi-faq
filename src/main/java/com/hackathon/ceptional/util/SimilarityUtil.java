package com.hackathon.ceptional.util;

import info.debatty.java.stringsimilarity.Damerau;
import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.MetricLCS;
import info.debatty.java.stringsimilarity.NGram;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * methods to calculate sentence similarity
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
@Slf4j
public class SimilarityUtil {
    private static int min(int one, int two, int three)
    {
        int min = one;
        if (two < min)
        {
            min = two;
        }
        if (three < min)
        {
            min = three;
        }
        return min;
    }

    private static int ld(String str1, String str2)
    {
        int d[][]; // 矩阵
        int n = str1.length();
        int m = str2.length();
        int i; // 遍历str1的
        int j; // 遍历str2的
        char ch1; // str1的
        char ch2; // str2的
        int temp; // 记录相同字符,在某个矩阵位置值的增量,不是0就是1
        if (n == 0)
        {
            return m;
        }
        if (m == 0)
        {
            return n;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++)
        { // 初始化第一列
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++)
        { // 初始化第一行
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++)
        { // 遍历str1
            ch1 = str1.charAt(i - 1);
            // 去匹配str2
            for (j = 1; j <= m; j++)
            {
                ch2 = str2.charAt(j - 1);
                if (ch1 == ch2)
                {
                    temp = 0;
                }
                else
                {
                    temp = 1;
                }
                // 左边+1,上边+1, 左上角+temp取最小
                d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + temp);
            }
        }
        return d[n][m];
    }

    /**
     * edit distance 算法计算两个string相似度
     */
    public static double sim(String str1, String str2)
    {
        try
        {
            double ld = (double) ld(str1, str2);
            return (1 - ld / (double) Math.max(str1.length(), str2.length()));
        }
        catch (Exception e)
        {
            log.error("", e);
            return 0.1;
        }
    }


    /**
     * jac card算法计算两个string相似度
     * @param str1 - sentence 1
     * @param str2 - sentence 2
     * @return similarity result
     */
    public static double jacCardSimilarity(String str1, String str2){
        //set元素不可重复
        Set<Character> s1 = new HashSet<>();
        Set<Character> s2 = new HashSet<>();

        for (int i = 0; i < str1.length(); i++) {
            s1.add(str1.charAt(i));
        }
        for (int j = 0; j < str2.length(); j++) {
            s2.add(str2.charAt(j));
        }

        // 并集元素个数
        float mergeNum = 0;
        // 相同元素个数（交集）
        float commonNum = 0;

        for(Character ch1:s1) {
            for(Character ch2:s2) {
                if(ch1.equals(ch2)) {
                    commonNum++;
                }
            }
        }

        mergeNum = s1.size()+s2.size()-commonNum;

        return commonNum/mergeNum;
    }

    /**
     * Damerau算法计算两个string相似度
     * @param str1 - sentence 1
     * @param str2 - sentence 2
     * @return similarity result
     */
    public static double damerauSimilarity(String str1, String str2) {
        Damerau d = new Damerau();
        return d.distance(str1, str2);
    }

    /**
     * Jaro算法计算两个string相似度
     * @param str1 - sentence 1
     * @param str2 - sentence 2
     * @return similarity result
     */
    public static double jaroSimilarity(String str1, String str2) {
        JaroWinkler jw = new JaroWinkler();
        return jw.similarity(str1, str2);
    }

    /**
     * 度量最长公共子序列算法计算两个string相似度
     * @param str1 - sentence 1
     * @param str2 - sentence 2
     * @return similarity result
     */
    public static double metricLcsSimilarity(String str1, String str2) {
        MetricLCS lcs = new MetricLCS();
        return lcs.distance(str1, str2);
    }

    /**
     * nGram算法计算两个string相似度
     * @param str1 - sentence 1
     * @param str2 - sentence 2
     * @return similarity result
     */
    public static double nGramSimilarity(String str1, String str2) {
        NGram ngram = new NGram();
        return ngram.distance(str1, str2);
    }
}
