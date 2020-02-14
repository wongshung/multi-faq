package com.hackathon.ceptional.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hackathon.ceptional.config.Constants;
import com.hackathon.ceptional.model.CcsResponse;
import com.hackathon.ceptional.model.RespMessage;
import com.hackathon.ceptional.model.ResultModel;
import com.hackathon.ceptional.model.SimilarityModel;
import com.hackathon.ceptional.service.FaqMatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * faq methods for client
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
@RestController
@Slf4j
public class FaqController {

    private FaqMatchService faqMatchService;

    @Autowired
    private void setFaqMatchService(FaqMatchService service) {
        this.faqMatchService = service;
    }

    @PostMapping("/match")
    public Object match(@RequestBody String request) {
        log.info("match request, param: {}", request);
        String question = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has(Constants.QUESTION)) {
            question = jsonObject.get(Constants.QUESTION).getAsString();
        }

        if (StringUtils.isBlank(question)) {
            log.error("incorrect request data, match failed!");
            return CcsResponse.error(RespMessage.REQUEST_ERROR);
        }

        ResultModel resultModel = faqMatchService.doMatch(question);
        log.info("match request success, result: {}", resultModel.toString());
        return CcsResponse.success(resultModel);
    }

    @PostMapping("/similarity")
    public Object similarity(@RequestBody String request) {
        log.info("similarity request, param: {}", request);
        String text1 = "";
        String text2 = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has("text1")) {
            text1 = jsonObject.get("text1").getAsString();
        }
        if (jsonObject.has("text2")) {
            text2 = jsonObject.get("text2").getAsString();
        }

        SimilarityModel simResult = faqMatchService.similarity(text1, text2);
        log.info("similarity request success, result: {}", simResult.toString());
        return CcsResponse.success(simResult);
    }
}
