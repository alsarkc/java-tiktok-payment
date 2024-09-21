package com.alsark.paydemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
public class PayController {

    //sign签名
    public final static List<String> REQUEST_NOT_NEED_SIGN_PARAMS = Arrays.asList("app_id", "thirdparty_id", "sign", "other_settle_params");
    private static final String SALT = "JEwfiklDXIfPw4nl0lT15cpmJlreMPIMQOJbTOpT";

    public static String requestSign(Map<String, Object> paramsMap) {
        List<String> paramsArr = new ArrayList<>();
        for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
            String key = entry.getKey();
            if (REQUEST_NOT_NEED_SIGN_PARAMS.contains(key)) {
                continue;
            }
            String value = entry.getValue().toString();

            value = value.trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            value = value.trim();
            if (value.equals("") || value.equals("null")) {
                continue;
            }
            paramsArr.add(value);
        }
        paramsArr.add(SALT);
        Collections.sort(paramsArr);
        StringBuilder signStr = new StringBuilder();
        String sep = "";
        for (String s : paramsArr) {
            signStr.append(sep).append(s);
            sep = "&";
        }
        return md5FromStr(signStr.toString());
    }

    private static String md5FromStr(String inStr) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }

        byte[] byteArray = inStr.getBytes(StandardCharsets.UTF_8);
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuilder hexValue = new StringBuilder();
        for (byte md5Byte : md5Bytes) {
            int val = ((int) md5Byte) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    //预下单接口
    @GetMapping("/pay")
    public String pay() {

        long currentTimestamp = System.currentTimeMillis();
        Map<String, Object> testCase = new HashMap<>() {
            {
                put("app_id", "ttb905cfb8263a12dc01");
                put("out_order_no", "noncestr" + currentTimestamp);
                put("total_amount", 1);
                put("subject", "抖音商品");
                put("body", "抖音商品");
                put("valid_time", 300);
                put("notify_url", "https://app.mujuapi.cn/api/pay/notify");
            }
        };
        testCase.put("sign", requestSign(testCase));

        HttpResponse<String> response = null;
        try {
            String url = "https://developer.toutiao.com/api/apps/ecpay/v1/create_order";
            HttpClient httpClient = HttpClient.newHttpClient();
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(testCase);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }


        if (response != null) {
            return response.body();
        } else {
            return null;
        }

    }


}
