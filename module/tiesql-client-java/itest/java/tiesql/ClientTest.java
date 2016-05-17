package dadysql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ClientTest {


    @Test
    public void joinTest() {
        String jsonStr = "{\"department\":{\"id\":3,\"transaction_id\":0,\"dept_name\":\"HR\"}}";
        Gson gson = new GsonBuilder().create();
        Map<String, Object> result = gson.fromJson(jsonStr, Map.class);
        System.out.println(result.get("department"));
        System.out.println(result);

        jsonStr = "[null,{\"error\":\" [:insert-dept1] Name not found\"}]";
        gson = new GsonBuilder().create();
        Object[] result1 = gson.fromJson(jsonStr, Object[].class);

        System.out.println(result1[0]);
        System.out.println(result1[1]);

    }


    @Test
    public void pushTest() throws IOException {
        String jsonStr = "{\"department\":{\"dept_name\":\"HR1\"}}";

        String url = "http://localhost:3000";

        List<String> nameList = new ArrayList<String>();
        nameList.add("insert-dept1");

        Client client = new Client();
        String response = client.pushBatch("http://localhost:3000/tie", nameList, jsonStr);
        System.out.println(response);
    }


    @Test
    public void pushWithDataTest() throws IOException {
        //String jsonStr = "{\"department\":{\"dept_name\":\"HR1\"}}";

        Map<String, Object> department = new HashMap<String, Object>();
        Map<String, Object> departmentValueMap = new HashMap<String, Object>();
        departmentValueMap.put("dept_name", "HR");
        department.put("department", departmentValueMap);


        List<String> nameList = new ArrayList<String>();
        nameList.add("insert-dept");

        Client client = new Client();
        List response = client.pushBatch("http://localhost:3001/tie", nameList, department);
        System.out.println(response);
    }


    @Test
    public void pullBatchTest() throws IOException {
        Client client = new Client();

        String url = "http://localhost:3000";

        List<String> nameList = new ArrayList<String>();
        nameList.add("get-dept-by-id");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", 3);

        //client.ma(nameList, params);

        String response = client.pullBatchAsString("http://localhost:3000/tie", nameList, params);
        //Gson gson = new GsonBuilder().create();

        //List responseList = gson.fromJson(response.getOK(), );
        //String responseStr = gson.toJson(response);

        // System.out.println(responseStr);
        System.out.println(response);
        //client.checkTransit();
        assertNotNull(response);
        //assertEquals(6, 5);
    }

    @Test
    public void pullOneTest() throws IOException {
        Client client = new Client();


        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", 3);

        //client.ma(nameList, params);

        List response = client.pullOneAsData("http://localhost:3000/tie", "get-dept-by-id", params);
        //Gson gson = new GsonBuilder().create();

        //List responseList = gson.fromJson(response.getOK(), );
        //String responseStr = gson.toJson(response);

        // System.out.println(responseStr);
        System.out.println(response);
        //client.checkTransit();
        assertNotNull(response);
        //assertEquals(6, 5);
    }


    @Test
    public void pullDataTest() throws IOException {
        Client client = new Client();

        String url = "http://localhost:3000";

        List<String> nameList = new ArrayList<String>();
        nameList.add("get-dept-by-id");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", 3);

        //client.ma(nameList, params);

        List response = client.pullBatchAsData("http://localhost:3000/tie", nameList, params);
        Gson gson = new GsonBuilder().create();

        //List responseList = gson.fromJson(response.getOK(), );
        String responseStr = gson.toJson(response);

        System.out.println(responseStr);
//        System.out.println(response.get(0));
        //client.checkTransit();
        assertNotNull(response.get(0));
        assertNull(response.get(1));
        //assertEquals(6, 5);
    }


}