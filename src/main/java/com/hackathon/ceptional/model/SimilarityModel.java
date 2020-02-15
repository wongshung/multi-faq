package com.hackathon.ceptional.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * similarity model
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/14
 */
@Getter
@Setter
@AllArgsConstructor
public class SimilarityModel {
    /**
     * debatty result
     */
    private double jaro;
    private double editDistance;
    private double damerau;
    private double jacCard;
    private double metricLcs;
    private double nGram;

    @Override
    public String toString() {
        return "jaro:".concat(String.valueOf(jaro))
                .concat(", editDistance:").concat(String.valueOf(editDistance))
                .concat(", damerau:").concat(String.valueOf(damerau))
                .concat(", jacCard:").concat(String.valueOf(jacCard))
                .concat(", metricLcs:").concat(String.valueOf(metricLcs))
                .concat(", nGram:").concat(String.valueOf(nGram))
                .concat(", rawSim:").concat(String.valueOf(rawSim))
                .concat(", chineseSim:").concat(String.valueOf(chineseSim))
                .concat(", hanlpSim:").concat(String.valueOf(hanlpSim))
                .concat(", ikeaSim:").concat(String.valueOf(ikeaSim))
                .concat(", jiebaSim:").concat(String.valueOf(jiebaSim));
    }

    /**
     * hu tool result
     */
    private double rawSim;
    private double chineseSim;
    private double hanlpSim;
    private double ikeaSim;
    private double jiebaSim;
}
