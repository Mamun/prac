package dadysql;

import com.cognitect.transit.*;
import com.cognitect.transit.Reader;
import com.cognitect.transit.Writer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class Client {

    Gson gson = new GsonBuilder().create();

    public Reader reader(String s) {
        try {
            InputStream in = new ByteArrayInputStream(s.getBytes());
            return TransitFactory.reader(TransitFactory.Format.JSON, in);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public String push(Object o, TransitFactory.Format format, Map<Class, WriteHandler<?, ?>> customHandlers) {
        try {
            OutputStream out = new ByteArrayOutputStream();
            Writer w = TransitFactory.writer(format, out, customHandlers);
            w.write(o);
            return out.toString();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public String writeJson(Object o) {
        try {
            return push(o, TransitFactory.Format.JSON, null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void enableDebug() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.conn", "DEBUG");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.client", "DEBUG");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.client", "DEBUG");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
    }

    public String buildBatchRequest(List<String> nameCollection, Map<String, Object> params) {

        Map<Keyword, Object> request = new HashMap<Keyword, Object>();

        List<Keyword> namekeywordCollection = new ArrayList<Keyword>(nameCollection.size());
        for (String name : nameCollection) {
            namekeywordCollection.add(TransitFactory.keyword(name));
        }


        Map<Keyword, Object> keywordParamsMap = new HashMap<Keyword, Object>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            keywordParamsMap.put(TransitFactory.keyword(entry.getKey()), entry.getValue());
        }

        request.put(TransitFactory.keyword("name"), namekeywordCollection);
        request.put(TransitFactory.keyword("params"), keywordParamsMap);

        Map<Keyword, Object> options = new HashMap<Keyword, Object>();
        options.put(TransitFactory.keyword("input"), TransitFactory.keyword("string"));
        options.put(TransitFactory.keyword("output"), TransitFactory.keyword("string"));
        request.put(TransitFactory.keyword("options"), options);

        return writeJson(request);

    }


    public String buildOneRequest(String name, Map<String, Object> params) {

        Map<Keyword, Object> request = new HashMap<Keyword, Object>();


        Map<Keyword, Object> keywordParamsMap = new HashMap<Keyword, Object>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            keywordParamsMap.put(TransitFactory.keyword(entry.getKey()), entry.getValue());
        }

        request.put(TransitFactory.keyword("name"), TransitFactory.keyword(name));
        request.put(TransitFactory.keyword("params"), keywordParamsMap);

        Map<Keyword, Object> options = new HashMap<Keyword, Object>();
        options.put(TransitFactory.keyword("input"), TransitFactory.keyword("string"));
        options.put(TransitFactory.keyword("output"), TransitFactory.keyword("string"));
        request.put(TransitFactory.keyword("options"), options);

        return writeJson(request);

    }


    public String callHttpService(String url, String request) throws IOException {
        ResponseHandler<String> responseHandler = new TieResponseHandler();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/transit+json");
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");

        StringEntity inputEntity = new StringEntity(request);
        inputEntity.setContentType("application/transit+json");

        httpPost.setEntity(inputEntity);

        return httpclient.execute(httpPost, responseHandler);
    }

    public List callHttpServiceForData(String url, String request) throws IOException {
        ResponseHandler<String> responseHandler = new TieResponseHandler();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/transit+json");
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/transit+json");

        StringEntity inputEntity = new StringEntity(request);
        inputEntity.setContentType("application/transit+json");

        httpPost.setEntity(inputEntity);

        String response = httpclient.execute(httpPost, responseHandler);

        return reader(response).read();
    }


    private String pullBatch = "/pullbatch";
    private String pullOne = "/pullone";
    private String pushBatch = "/pushbatch";

    public String pullBatchAsString(String url, List<String> nameCollection, Map<String, Object> params) throws IOException {
        url = url + pullBatch;
        String request = buildBatchRequest(nameCollection, params);
        return callHttpService(url, request);
    }

    public List pullOneAsData(String url, String name, Map<String, Object> params) throws IOException {
        url = url + pullOne;
        String request = buildOneRequest(name, params);
        return callHttpServiceForData(url, request);
    }

    public List pullBatchAsData(String url, List<String> nameCollection, Map<String, Object> params) throws IOException {
        url = url + pullBatch;
        String request = buildBatchRequest(nameCollection, params);
        return callHttpServiceForData(url, request);
    }

    public List pushBatch(String url, List<String> nameCollection, Map<String, Object> params) throws IOException {
        url = url + pushBatch;
        String request = buildBatchRequest(nameCollection, params);
        return callHttpServiceForData(url, request);
    }

    public List pushOne(String url, String name, Map<String, Object> params) throws IOException {
        url = url + pullOne;
        String request = buildOneRequest(name, params);
        return callHttpServiceForData(url, request);
    }

    public String pushBatch(String url, List<String> nameCollection, String jsonStr) throws IOException {
        Map params = gson.fromJson(jsonStr, Map.class);
        url = url + pushBatch;
        String request = buildBatchRequest(nameCollection, params);
        return callHttpService(url, request);
    }


    public class TieResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(
                final HttpResponse response) throws ClientProtocolException, IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }
    }


    public class Response<T> {

        private String OK;
        private String Error;

        public Response(String OK, String Error) {
            this.OK = OK;
            this.Error = Error;
        }

        public String getOK() {
            return this.OK;
        }

        public String getError() {
            return this.Error;
        }
    }

}