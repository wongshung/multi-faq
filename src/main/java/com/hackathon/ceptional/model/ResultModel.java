package com.hackathon.ceptional.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * faq match result data model
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
@Setter
@Getter
public class ResultModel {

    private double answerScore;
    private int status;
    List<Answer> answer;

    /**
     * internal data model for ResultModel
     */
    @Setter
    @Getter
    public class Answer {
        String type;
        String subType;
        String value;
    }
}
