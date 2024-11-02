package com.example.musinsa;

import android.util.Log;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class HttpRequest {

    private OkHttpClient client;
    private List<Cookie> cookieStore = new ArrayList<>(); // 쿠키 저장소

    public HttpRequest() {
        client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        //cookieStore.clear();
                        cookieStore.addAll(cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return cookieStore;
                    }
                })
                .build();
    }

    // 동기 GET 요청
    public String get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Content-Type", "application/json+sua; charset=utf-8")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                return "Error: " + response.code();
            }
        } catch (IOException e) {
            Log.e("E", "Network request failed", e);
            return "Network request failed: " + e.getMessage();
        }
    }

    // 동기 POST 요청
    public String post(String url, Map<String, String> payload, int encoding) {
        RequestBody body = null;

        if (payload != null && !payload.isEmpty()) {
            if (encoding == 0) {
                // application/x-www-form-urlencoded 방식
                FormBody.Builder formBuilder = new FormBody.Builder();
                for (Map.Entry<String, String> entry : payload.entrySet()) {
                    formBuilder.add(entry.getKey(), entry.getValue());
                }
                body = formBuilder.build();
            } else if (encoding == 1) {
                // application/json 방식
                JSONObject jsonObject = new JSONObject(payload);
                body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));
            }
        } else {
            // payload가 없을 경우 빈 RequestBody 생성
            body = RequestBody.create("", null);
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0");

        // encoding에 따라 Content-Type 헤더 설정
        if (encoding == 0) {
            requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded");
        } else if (encoding == 1) {
            requestBuilder.addHeader("Content-Type", "application/json+sua; charset=utf-8");
        }

        // body가 null이 아니면 POST로 설정
        requestBuilder.post(body);

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                return "Error: " + response.code();
            }
        } catch (IOException e) {
            Log.e("E", "Network request failed", e);
            return "Network request failed: " + e.getMessage();
        }
    }
}