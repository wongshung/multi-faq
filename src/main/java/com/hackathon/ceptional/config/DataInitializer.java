package com.hackathon.ceptional.config;

import com.hackathon.ceptional.service.FaqDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * data initialization
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private FaqDataService dataInitService;
    @Autowired
    private void setDataInitService(FaqDataService service) {
        this.dataInitService = service;
    }

    @Override
    public void run(String... args) {
        log.info("DataInitializer start to initialize ...");
        Resource resource = new ClassPathResource("static/Hackathon_P1_trainingSet.xlsx");
        try {
            File excelFile = resource.getFile();
            dataInitService.initData(excelFile);
        } catch (IOException ex) {
            log.error("DataInitializer failed to init training excel file!");
        }

    }
}
