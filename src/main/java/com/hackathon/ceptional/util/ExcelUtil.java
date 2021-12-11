package com.hackathon.ceptional.util;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * util for excel file handling
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/12
 */
public class ExcelUtil {
    /**
     * 默认单元格内容为数字时格式
     */
    private static DecimalFormat df = new DecimalFormat("0");

    /**
     * 默认单元格格式化日期字符串
     */
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 格式化数字
     */
    private static DecimalFormat nf = new DecimalFormat("0.00");

    public static final String XLSX = "xlsx";

    public static List<ArrayList<Object>> readExcel(File file) throws FileNotFoundException {
        if (file == null) {
            return Collections.emptyList();
        }
        if (file.getName().endsWith("xlsx")) {
            // 处理excel2007, default read first sheet
            return readExcel2007(file, 0);
        } else {
            // 处理excel2003, default first sheet
            return readExcel2003(file, 0);
        }
    }

    public static List<ArrayList<Object>> readExcelFromInputStream(InputStream inputStream, String type) {
        if (inputStream == null) {
            return Collections.emptyList();
        }
        if (XLSX.equals(type)) {
            // 处理excel2007, default read first sheet
            return readExcel2007FromStream(inputStream, 0);
        } else {
            // 处理excel2003, default first sheet
            return readExcel2003FromStream(inputStream, 0);
        }
    }

    public static Object readAsWordBook(File file) {
        if (file == null) {
            return null;
        }
        if (file.getName().endsWith("xlsx")) {
            // 处理excel2007
            return readXSSFWorkbook(file);
        } else {
            // 处理excel2003
            return readHSSFWorkbook(file);
        }
    }

    /**
     * for excel 2007
     * @param file - excel file
     * @return XSSFWorkbook object
     */
    private static XSSFWorkbook readXSSFWorkbook(File file) {
        try {
            return new XSSFWorkbook(new FileInputStream(file));
        } catch (Exception e) {
            return null;
        }
    }

    private static XSSFWorkbook readXSSFWorkbookFromStream(InputStream inputStream) {
        try {
            return new XSSFWorkbook(inputStream);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * for excel 2003
     * @param file - excel file
     * @return HSSFWorkbook object
     */
    private static HSSFWorkbook readHSSFWorkbook(File file) {
        try {
            return new HSSFWorkbook(new FileInputStream(file));
        } catch (Exception e) {
            return null;
        }
    }

    private static HSSFWorkbook readHSSFWorkbookFromStream(InputStream inputStream) {
        try {
            return new HSSFWorkbook(inputStream);
        } catch (Exception e) {
            return null;
        }
    }

    private static ArrayList<ArrayList<Object>> readExcel2007(File file, int sheetIndex) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        return readExcel2007FromStream(fileInputStream, sheetIndex);
    }

    /**
     * @return 将返回结果存储在ArrayList内，存储结构与二位数组类似
     * lists.get(0).get(0)表示过去Excel中0行0列单元格
     */
    private static ArrayList<ArrayList<Object>> readExcel2007FromStream(InputStream inputStream, int sheetIndex) {
        try {
            ArrayList<ArrayList<Object>> rowList = new ArrayList<>();
            ArrayList<Object> colList;
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(sheetIndex);
            XSSFRow row;
            XSSFCell cell;
            for (int i = sheet.getFirstRowNum(), rowCount = 0; rowCount < sheet.getPhysicalNumberOfRows(); i++) {
                row = sheet.getRow(i);
                colList = new ArrayList<Object>();
                if (row == null) {
                    //当读取行为空时, 判断是否是最后一行
                    if (i != sheet.getPhysicalNumberOfRows()) {
                        rowList.add(colList);
                    }
                    continue;
                } else {
                    rowCount++;
                }
                for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
                    cell = row.getCell(j);
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        //当该单元格为空, 判断是否是该行中最后一个单元格
                        if (j != row.getLastCellNum()) {
                            colList.add("");
                        }
                        continue;
                    }

                    colList.add(cell.toString());
                }//end for j
                rowList.add(colList);
            }//end for i

            return rowList;
        } catch (Exception e) {
            System.out.println("exception");
            return null;
        }
    }

    private static ArrayList<ArrayList<Object>> readExcel2003(File file, int sheetIndex) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        return readExcel2003FromStream(fileInputStream, sheetIndex);
    }

    private static ArrayList<ArrayList<Object>> readExcel2003FromStream(InputStream inputStream, int sheetIndex) {
        try {
            ArrayList<ArrayList<Object>> rowList = new ArrayList<>();
            ArrayList<Object> colList;
            HSSFWorkbook wb = new HSSFWorkbook(inputStream);
            HSSFSheet sheet = wb.getSheetAt(sheetIndex);
            HSSFRow row;
            HSSFCell cell;
            Object value;
            for (int i = sheet.getFirstRowNum(), rowCount = 0; rowCount < sheet.getPhysicalNumberOfRows(); i++) {
                row = sheet.getRow(i);
                colList = new ArrayList<Object>();
                if (row == null) {
                    //当读取行为空时, 先判断是否是最后一行
                    if (i != sheet.getPhysicalNumberOfRows()) {
                        rowList.add(colList);
                    }
                    continue;
                } else {
                    rowCount++;
                }
                for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
                    cell = row.getCell(j);
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        //当该单元格为空, 判断是否是该行中最后一个单元格
                        if (j != row.getLastCellNum()) {
                            colList.add("");
                        }
                        continue;
                    }
                    switch (cell.getCellType()) {
                        case STRING:
                            System.out.println(i + "行" + j + " 列 is String type");
                            value = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            if ("@".equals(cell.getCellStyle().getDataFormatString())) {
                                value = df.format(cell.getNumericCellValue());
                            } else if ("General".equals(cell.getCellStyle()
                                    .getDataFormatString())) {
                                value = nf.format(cell.getNumericCellValue());
                            } else {
                                value = sdf.format(HSSFDateUtil.getJavaDate(cell
                                        .getNumericCellValue()));
                            }
                            System.out.println(i + "行" + j
                                    + " 列 is Number type ; DateFormt:"
                                    + value.toString());
                            break;
                        case BOOLEAN:
                            System.out.println(i + "行" + j + " 列 is Boolean type");
                            value = cell.getBooleanCellValue();
                            break;
                        case BLANK:
                            System.out.println(i + "行" + j + " 列 is Blank type");
                            value = "";
                            break;
                        default:
                            System.out.println(i + "行" + j + " 列 is default type");
                            value = cell.toString();
                    }// end switch
                    colList.add(value);
                }//end for j
                rowList.add(colList);
            }//end for i

            return rowList;
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeExcel(ArrayList<ArrayList<Object>> result, String path) {
        if (result == null) {
            return;
        }
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("sheet1");
        for (int i = 0; i < result.size(); i++) {
            HSSFRow row = sheet.createRow(i);
            if (result.get(i) != null) {
                for (int j = 0; j < result.get(i).size(); j++) {
                    HSSFCell cell = row.createCell(j);
                    cell.setCellValue(result.get(i).get(j).toString());
                }
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            wb.write(os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] content = os.toByteArray();
        //Excel文件生成后存储的位置。
        File file = new File(path);
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(content);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DecimalFormat getDf() {
        return df;
    }

    public static void setDf(DecimalFormat df) {
        ExcelUtil.df = df;
    }

    public static SimpleDateFormat getSdf() {
        return sdf;
    }

    public static void setSdf(SimpleDateFormat sdf) {
        ExcelUtil.sdf = sdf;
    }

    public static DecimalFormat getNf() {
        return nf;
    }

    public static void setNf(DecimalFormat nf) {
        ExcelUtil.nf = nf;
    }

}
