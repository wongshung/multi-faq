package com.hackathon.ceptional.config;

import com.hackathon.ceptional.service.FaqDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.File;

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

    /**
     * training set
     */
    @Value("${faq.training.set}")
    private String trainingSet = "Hackathon_P1_trainingSet.xlsx";

    private FaqDataService dataInitService;
    @Autowired
    private void setDataInitService(FaqDataService service) {
        this.dataInitService = service;
    }

    @Override
    public void run(String... args) {
        log.info("DataInitializer start to initialize ...");
        String configPath = System.getProperty("user.dir")+File.separator+"config/";
        String filePath = configPath.concat(trainingSet);
        try {
            File excelFile = new File(filePath);
            dataInitService.initData(excelFile);
        } catch (Exception ex) {
            log.error("DataInitializer failed to init training excel file: {}", ex.getMessage());
        }
    }
}
