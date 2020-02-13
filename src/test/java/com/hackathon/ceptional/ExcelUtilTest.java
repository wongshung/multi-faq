package com.hackathon.ceptional;

import com.hackathon.ceptional.util.ExcelUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.ArrayList;

/**
 * Test methods for ExcelUtil
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
@SpringBootTest
class ExcelUtilTest {

    @Test
    void testReadExcel() throws Exception {
        Resource resource = new ClassPathResource("static/Hackathon_P1_trainingSet_1.xlsx");
        File excelFile = resource.getFile();
        ArrayList<ArrayList<Object>>  contents = ExcelUtil.readExcel(excelFile);

        Assert.assertNotNull(contents);
    }
}
