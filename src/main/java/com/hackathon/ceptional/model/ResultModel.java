package com.hackathon.ceptional.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

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

    private int type = 1;
    List<Answer> answer;
    private double answer_score;
    private int status;
    private int module_type = 0;
    private String robot_module = "faq";
    private String robot_source = "ml";

    /**
     * internal data model for ResultModel
     */
    @Setter
    @Getter
    public class Answer {
        String type;
        String subType;
        String value;
        List data;
    }

    @Override
    public String toString() {
        String result = "score=".concat(String.valueOf(answer_score)).concat(", answer=");
        if (Objects.nonNull(answer) && answer.size() > 0) {
            result = result.concat(answer.get(0).getValue());
        }
        return result;
    }
}
