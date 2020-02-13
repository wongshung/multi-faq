package com.hackathon.ceptional.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hackathon.ceptional.config.Constants;
import com.hackathon.ceptional.model.CcsResponse;
import com.hackathon.ceptional.model.RespMessage;
import com.hackathon.ceptional.model.ResultModel;
import com.hackathon.ceptional.service.FaqMatchService;
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
public class FaqController {

    private FaqMatchService faqMatchService;
    @Autowired
    private void setFaqMatchService(FaqMatchService service) {
        this.faqMatchService = service;
    }

    @PostMapping("/doMatch")
    public Object doMatch(@RequestBody String request) {
        String question = "";
        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
        if (jsonObject.has(Constants.QUESTION)) {
            question = jsonObject.get(Constants.QUESTION).getAsString();
        }

        if (StringUtils.isNotBlank(question)) {
            ResultModel resultModel = faqMatchService.doMatch(question);
            return CcsResponse.success(resultModel);
        } else {
            return CcsResponse.error(RespMessage.REQUEST_ERROR);
        }
    }
}
