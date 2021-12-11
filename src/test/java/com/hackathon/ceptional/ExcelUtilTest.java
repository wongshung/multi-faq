package com.hackathon.ceptional;

import com.hackathon.ceptional.util.ExcelUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    void testReadExcel() throws IOException {
        Resource resource = new ClassPathResource("templates/Hackathon_P1_trainingSet_1.xlsx");
        InputStream inputStream = resource.getInputStream();
        List<ArrayList<Object>> contents = ExcelUtil.readExcelFromInputStream(inputStream, "xlsx");

        Assertions.assertFalse(contents.isEmpty());
    }
}
