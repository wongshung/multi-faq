package com.hackathon.ceptional.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class HttpService {
    /* service URL */
    private String serivceURL;

    /* socket connect timeout */
    private final static String SOCKET_CONNECT_TIMEOUT = "socket.connect.timeout";
    /* socket read timeout */
    private final static String SOCKET_READ_TIMEOUT = "socket.read.timeout";

    /* milliseconds for socket connect timeout */
    private final static int CONNECT_TIMEOUT_MS = 1000;

    /* milliseconds for socket input stream read timeout */
    private final static int READ_TIMEOUT_MS = 1000;

    /* connection connect timeout */
    private static int socketConnectTimeout = CONNECT_TIMEOUT_MS;

    /* connection read timeout */
    private static int socketeReadTimeout = READ_TIMEOUT_MS;

    private CloseableHttpClient httpclient = HttpClients.createDefault();

    private String receivedBody;
    static {
        // get socket connect timeout
        String connectTimeout = System.getProperty(SOCKET_CONNECT_TIMEOUT);
        if (connectTimeout != null) {
            socketConnectTimeout = Integer.parseInt(connectTimeout);
        }
        // get socket read timeout
        String readTimeout = System.getProperty(SOCKET_READ_TIMEOUT);
        if (readTimeout != null) {
            socketeReadTimeout = Integer.parseInt(readTimeout);
        }
    }

    public HttpService(){}

    public HttpService(String serviceURL) {

        this.serivceURL = serviceURL;
    }

    public String doGet(URI uri) {
        String result = null;
        
        StopWatch timer = new StopWatch();
        timer.start();
        
        try {
            HttpGet httpget = new HttpGet(uri);
            // set time out
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(socketConnectTimeout).setConnectionRequestTimeout(socketeReadTimeout)
                    .setSocketTimeout(socketConnectTimeout).build();
            httpget.setConfig(requestConfig);

            CloseableHttpResponse response = httpclient.execute(httpget);

            result = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e){
            log.error("", e);
        } finally {
            timer.stop();
            log.info("cost time : [{}], request url : [{}]", timer.toString(), this.serivceURL);
        }

        return result;
    }

    public String get(String inputUrl,Map<String, String> header, int connectTimeout, int readTimeout){
        log.info("The URL through get is: " + inputUrl);

        HttpURLConnection conn = null;
        String result = "";

        try{
            URL url = new URL(inputUrl);
            conn = (HttpURLConnection)url.openConnection();

            // 设置超时时间
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept-charset", "UTF-8");
            if(header!=null){
                for(String key:header.keySet()){
                    conn.setRequestProperty(key,header.get(key));
                }
            }
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK){
                throw new RuntimeException("Failed: HTTP error code: " + responseCode);
            }
            else{
                InputStream inputStream = conn.getInputStream();
                byte[] data = readInputStream(inputStream);
                result = new String(data, StandardCharsets.UTF_8);
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }finally {
            if (conn != null){
                conn.disconnect();
            }
        }

        return result;
    }

    public String doPost(String query) {
        return doPost(query,socketConnectTimeout);
    }


    private String doPost(String query,int connTimeout) {
        String response = null;
        HttpURLConnection conn = null;

        if(StringUtils.isEmpty(this.serivceURL)) {
            return response;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        try {
            URL url = new URL(this.serivceURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(connTimeout);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("accept-charset", "UTF-8");
            // post request data
            OutputStream os = conn.getOutputStream();
            os.write(query.getBytes(StandardCharsets.UTF_8));
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                log.error("Failed : HTTP error code : [ " + conn.getResponseCode()
                        + " ] HTTP Url : [ " + this.serivceURL + " ] postData : [ " + query + " ]");
            } else {
                InputStream stream = conn.getInputStream();
                byte[] data = readInputStream(stream);
                this.receivedBody = new String(data, StandardCharsets.UTF_8);
                response = this.receivedBody;
            }
        } catch (Exception e) {
            //需要加日志
            log.error("", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            timer.stop();
            log.info("cost time : [{}], request url : [{}], post data : [{}], response : [{}]",timer.toString(),this.serivceURL,query,response);
        }

        return response;
    }

    public String doDelete(String query) {
        String response = null;
        HttpURLConnection conn = null;

        StopWatch timer = new StopWatch();
        timer.start();
        
        try {
            if(this.serivceURL == null || StringUtils.isBlank(this.serivceURL)) {
                return response;
            }
            URL url = new URL(this.serivceURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(socketConnectTimeout);
            conn.setReadTimeout(socketeReadTimeout);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("accept-charset", "UTF-8");
            // post request data
            OutputStream os = conn.getOutputStream();
            os.write(query.getBytes(StandardCharsets.UTF_8));
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                InputStream stream = conn.getInputStream();
                byte[] data = readInputStream(stream);
                this.receivedBody = new String(data, StandardCharsets.UTF_8);
                response = this.receivedBody;
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            timer.stop();
            log.info("cost time : [{}], request url : [{}], post data : [{}], response : [{}]",timer.toString(),this.serivceURL,query,response);
        }
        return response;
    }

    public String doPut(String query) {
        String response = null;
        HttpURLConnection conn = null;
        
        StopWatch timer = new StopWatch();
        timer.start();
        
        try {
            // get query put data
            String putData = query;
            if(this.serivceURL == null || StringUtils.isBlank(this.serivceURL)) {
                return response;
            }
            URL url = new URL(this.serivceURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(socketConnectTimeout);
            conn.setReadTimeout(socketeReadTimeout);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("accept-charset", "UTF-8");
            // post request data
            OutputStream os = conn.getOutputStream();
            os.write(putData.getBytes(StandardCharsets.UTF_8));
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                InputStream stream = conn.getInputStream();
                byte[] data = readInputStream(stream);
                this.receivedBody = new String(data, StandardCharsets.UTF_8);
                response = this.receivedBody;
            }
        } catch (Exception e) {
            //需要加日志
            log.error("", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            timer.stop();
            log.info("cost time : [{}], request url : [{}], post data : [{}], response : [{}]",timer.toString(),this.serivceURL,query,response);
        }
        return response;
    }

    private static byte[] readInputStream(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1024];
        byte[] result = null;
        int len = 0;
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();) {
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            result = outStream.toByteArray();
        } catch (Exception e) {
            log.error("", e);
        } finally {
            inStream.close();
            buffer = null;
        }
        return result;
    }

    public String doPostFormUrlencoded(String query, String skillId) {
        String response = null;
        HttpURLConnection conn = null;

        try {
            // get query post data
            String postData = query;
            if(this.serivceURL == null || StringUtils.isBlank(this.serivceURL)) {
                return response;
            }
            URL url = new URL(this.serivceURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(socketConnectTimeout);
            conn.setReadTimeout(socketeReadTimeout);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("accept-charset", "UTF-8");
            conn.setRequestProperty("X-Appid", skillId);
            // post request data
            conn.connect();
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(query);
            //流用完记得关
            out.flush();
            out.close();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                InputStream stream = conn.getInputStream();
                byte[] data = readInputStream(stream);
                this.receivedBody = new String(data, StandardCharsets.UTF_8);
                response = this.receivedBody;
            }
        } catch (Exception e) {
            //需要加日志
            log.error("", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }

    /**
     * multipart/form-data 格式发送数据时各个部分分隔符的前缀,必须为 --
     */
    private static final String BOUNDARY_PREFIX = "--";
    /**
     * 回车换行,用于一行的结尾
     */
    private static final String LINE_END = "\r\n";

    public String doPostFormData(Map<String, Object> keyValues) {
        String response = null;
        HttpURLConnection conn = null;
        try {
            if(this.serivceURL == null || StringUtils.isBlank(this.serivceURL)) {
                return response;
            }
            Map<String, Object> headers = new HashMap<>();
            conn = getHttpUrlConnection(this.serivceURL, headers);
            //分隔符，可以任意设置，这里设置为 MyBoundary+ 时间戳（尽量复杂点，避免和正文重复）
            String boundary = "MyBoundary" + System.currentTimeMillis();
            //设置 Content-Type 为 multipart/form-data; boundary=${boundary}
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            //发送参数数据
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                //发送普通参数
                if (keyValues != null && !keyValues.isEmpty()) {
                    for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
                        // 进行空处理
                        if (StringUtils.isNotBlank(entry.getKey()) && Objects.nonNull(entry.getValue())) {
                            writeSimpleFormField(boundary, out, entry);
                        }
                    }
                }

                //写结尾的分隔符--${boundary}--,然后回车换行
                String endStr = BOUNDARY_PREFIX + boundary + BOUNDARY_PREFIX + LINE_END;
                out.write(endStr.getBytes());
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                InputStream stream = conn.getInputStream();
                byte[] data = readInputStream(stream);
                this.receivedBody = new String(data, StandardCharsets.UTF_8);
                response = this.receivedBody;
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return response;
    }

    public String doGetFormUrlencoded(HttpGet httpGet) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String result = null;
        try {
            CloseableHttpResponse response = httpclient.execute(httpGet);

            result = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e){
            log.error("", e);
        }

        return result;
    }

    public String doDeleteFormUrlencoded(HttpDelete httpDelete) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String result = null;
        try {
            CloseableHttpResponse response = httpclient.execute(httpDelete);

            result = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e){
            log.error("", e);
        }

        return result;
    }

    public String doPutFormUrlencoded(HttpPut httpPut) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String result = null;
        try {
            CloseableHttpResponse response = httpClient.execute(httpPut);

            result = EntityUtils.toString(response.getEntity(), "UTF-8");
        }catch (IOException e){
            log.error("", e);
        }

        return result;
    }

    /**
     *
     * @param url
     * @param parameter
     * @param base64
     * @param method
     * @return
     */
    public String httpPost(String url, byte[] parameter, String base64, String method) {

        java.util.Date data = new java.util.Date();
        String timestamp = String.valueOf(data.getTime()/1000);
        String signature = "*********";

        //定义StringBuffer  方便后面读取网页返回字节流信息时的字符串拼接
        StringBuffer buffer = new StringBuffer();

        //创建url_connection
        URLConnection http_url_connection = null;
        try {
            http_url_connection = (new URL(url)).openConnection();
            //将URLConnection类强转为HttpURLConnection
            HttpURLConnection httpUrlConnection = (HttpURLConnection)http_url_connection;

            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);

            //设置请求方式。可以是DELETE PUT POST GET
            httpUrlConnection.setRequestMethod(method);
            //设置内容的长度
            httpUrlConnection.setRequestProperty("Content-Length", String.valueOf(parameter.length));
            //设置编码格式
            httpUrlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            //设置接收返回参数格式
            httpUrlConnection.setRequestProperty("accept", "application/json");

            httpUrlConnection.setRequestProperty("authkey","************");
            httpUrlConnection.setRequestProperty("signature",signature);
            httpUrlConnection.setRequestProperty("timestamp",timestamp);

            httpUrlConnection.setUseCaches(false);

            if (null != base64) {
                //如果传入的参数不为空，则通过base64将传入参数解码
                httpUrlConnection.setRequestProperty("Authorization", "Basic "+new String(java.util.Base64.getEncoder().encode(base64.getBytes("utf-8")), "utf-8"));
            }

            // write request.
            BufferedOutputStream output_stream = new BufferedOutputStream(httpUrlConnection.getOutputStream());
            output_stream.write(parameter);
            output_stream.flush();
            output_stream.close();
            InputStreamReader input_stream_reader = null;
            if (httpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                input_stream_reader = new InputStreamReader(httpUrlConnection.getInputStream(), "utf-8");

            }

            //java.io.InputStreamReader input_stream_reader = new java.io.InputStreamReader(httpUrlConnection.getInputStream(), "utf-8");
            BufferedReader buffered_reader = new BufferedReader(input_stream_reader);
            buffer = new StringBuffer();
            String line;
            while ((line = buffered_reader.readLine()) != null) {
                buffer.append(line);
            }

            input_stream_reader.close();
            buffered_reader.close();
            httpUrlConnection.disconnect();
        } catch (Exception e) {
            log.error("", e);
        }

        return buffer.toString();
    }

    private static HttpURLConnection getHttpUrlConnection(String urlStr, Map<String, Object> headers) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //设置超时时间
        conn.setConnectTimeout(socketConnectTimeout);
        conn.setReadTimeout(socketeReadTimeout);
        //允许输入流
        conn.setDoInput(true);
        //允许输出流
        conn.setDoOutput(true);
        //不允许使用缓存
        conn.setUseCaches(false);
        //请求方式
        conn.setRequestMethod("POST");
        //设置编码 utf-8
        conn.setRequestProperty("Charset", "UTF-8");
        //设置为长连接
        conn.setRequestProperty("connection", "keep-alive");

        //设置其他自定义 headers
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue().toString());
            }
        }

        return conn;
    }
    /**
     * 写普通的表单参数
     *
     * @param boundary 分隔符
     * @param out
     * @param entry    参数的键值对
     * @throws IOException
     */
    private static void writeSimpleFormField(String boundary, DataOutputStream out, Map.Entry<String, Object> entry) throws IOException {
        //写分隔符--${boundary}，并回车换行
        String boundaryStr = BOUNDARY_PREFIX + boundary + LINE_END;
        out.write(boundaryStr.getBytes());
        //写描述信息：Content-Disposition: form-data; name="参数名"，并两个回车换行
        String contentDispositionStr = String.format("Content-Disposition: form-data; name=\"%s\"", entry.getKey()) + LINE_END + LINE_END;
        out.write(contentDispositionStr.getBytes());
        //写具体内容：参数值，并回车换行
        String valueStr = entry.getValue().toString() + LINE_END;
        out.write(valueStr.getBytes());
    }

}
