package com.hackathon.ceptional.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hackathon.ceptional.config.Constants;
import com.hackathon.ceptional.model.CcsResponse;
import com.hackathon.ceptional.model.RespMessage;
import com.hackathon.ceptional.model.ResultModel;
import com.hackathon.ceptional.model.SimilarityModel;
import com.hackathon.ceptional.service.FaqDataService;
import com.hackathon.ceptional.service.FaqMatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    private FaqDataService faqDataService;
    @Autowired
    private void setFaqDataService(FaqDataService service) {
        this.faqDataService = service;
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

    @PostMapping("/jiebaTfidf")
    public Object jiebaTfidf(@RequestBody String request) {
        log.info("jiebaTfidf request, param: {}", request);
        String text = "";
        int mode = 0;
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has("text")) {
            text = jsonObject.get("text").getAsString();
        }
        if (jsonObject.has("mode")) {
            mode = jsonObject.get("mode").getAsInt();
        }

        String result = faqMatchService.jiebaTfidf(text, mode);
        log.info("jiebaTfidf request success, result: {}", result);
        return CcsResponse.success(result);
    }

    @PostMapping("/wordSegment")
    public Object wordSegment(@RequestBody String request) {
        log.info("wordSegment request, param: {}", request);
        String text = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has("text")) {
            text = jsonObject.get("text").getAsString();
        }

        String result = faqMatchService.wordSegment(text);
        log.info("wordSegment request success, result: {}", result);
        return CcsResponse.success(result);
    }

    @PostMapping("/faqTfidfSim")
    public Object faqTfidfSim(@RequestBody String request) {
        log.info("faqTfidfSim request, param: {}", request);
        String faq = "";
        String q = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has("faq")) {
            faq = jsonObject.get("faq").getAsString();
        }
        if (jsonObject.has("q")) {
            q = jsonObject.get("q").getAsString();
        }

        double simResult = faqMatchService.faqTfidfSim(faq, q);
        log.info("faqTfidfSim request success, result: {}", simResult);
        return CcsResponse.success(simResult);
    }

    @PostMapping("/symmetricTfidfSim")
    public Object symmetricTfidfSim(@RequestBody String request) {
        log.info("symmetricTfidfSim request, param: {}", request);
        String text1 = "";
        String text2 = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has("text1")) {
            text1 = jsonObject.get("text1").getAsString();
        }
        if (jsonObject.has("text2")) {
            text2 = jsonObject.get("text2").getAsString();
        }

        double simResult = faqMatchService.symmetricTfidfSim(text1, text2);
        log.info("symmetricTfidfSim request success, result: {}", simResult);
        return CcsResponse.success(simResult);
    }

    @PostMapping("/getSynonym")
    public Object getSynonym(@RequestBody String request) {
        log.info("getSynonym request, param: {}", request);
        String text = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has("text")) {
            text = jsonObject.get("text").getAsString();
        }

        List<String> result = faqDataService.getSynonyms(text);
        log.info("getSynonym request success, result: {}", result);
        return CcsResponse.success(result);
    }
}
