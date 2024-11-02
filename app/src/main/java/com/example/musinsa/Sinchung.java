package com.example.musinsa;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Sinchung {
    private String key;
    private String token;
    private String hashed_id;
    private String hashed_pw;
    private UserInfo userInfo;
    private Handler mainHandler = new Handler(Looper.getMainLooper()); // 메인 스레드 핸들러
    private final HttpRequest httpRequest = new HttpRequest();
    private final Handler backgroundHandler;
    private final LoginCallback loginCallback;
    private final ProductCallback productCallback;
    private final TextView productInfoTextView;

    private LinkedList<String> textLines = new LinkedList<>();
    private static final int MAX_LINES = 20; // 최대 줄 수를 설정합니다.

    public static final String API_PROTOCOL = "https";
    public static final String API_HOST = "www.musinsa.com";
    public static final String API_URL = API_PROTOCOL + "://" + API_HOST;
    public static final String LOGIN_URL = API_URL + "/auth/login";
    public static final String LOGIN_STATUS_URL = API_PROTOCOL + "://my.musinsa.com/api/member/v1/login-status";
    private static final String GET_ADDRESS_URL = API_URL + "/order-service/my/addresses/getMemberAddresses";
    private static final String LIKE_URL = "https://like.musinsa.com/like/api/v1/members/liketypes/brand/relations/%s"; // 좋아요 링크
    public static final String APPLY_URL = API_URL + "/preuser/api/v1/registration";

    public Sinchung(LoginCallback loginCallback, ProductCallback productCallback, Handler mainHandler, TextView productInfoTextView) {
        this.loginCallback = loginCallback;
        this.productCallback = productCallback;
        this.mainHandler = mainHandler;
        this.productInfoTextView = productInfoTextView;

        // HandlerThread 생성 및 시작
        HandlerThread handlerThread = new HandlerThread("BackgroundHandlerThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }
    // 새 텍스트를 추가하는 메서드
    private void addText(String newText) {
        textLines.add(newText);

        // 최대 줄 수 초과 시 가장 오래된 줄 제거
        if (textLines.size() > MAX_LINES) {
            textLines.removeFirst();
        }

        // TextView에 업데이트된 텍스트 설정
        productInfoTextView.setText(String.join("\n", textLines));
    }

    // 로그인 메서드
    public void login() {
        backgroundHandler.post(() -> {
            String loginPageResponse = httpRequest.get(LOGIN_URL);
            if (loginPageResponse.startsWith("Error")) {
                mainHandler.post(() -> loginCallback.onLoginFailure("Login page request failed"));
                return;
            }

            try {
                // 로그인 페이지에서 cipherKey와 csrfToken 추출
                Document doc = Jsoup.parse(loginPageResponse);
                key = doc.select("input#cipherKey").attr("value");
                token = doc.select("input#csrfToken").attr("value");

                // AESCipher 객체를 사용해 ID와 PW 암호화
                AESCipher aesCipher = new AESCipher(key);
                hashed_id = aesCipher.encrypt("");
                hashed_pw = aesCipher.encrypt("");

                // 로그인 데이터 맵 구성
                Map<String, String> loginData = Map.of(
                        "cipherKey", key,
                        "cipherVersion", "V1",
                        "csrfToken", token,
                        "eventPage", "",
                        "eventCode", "",
                        "referer", API_URL,
                        "inviteKey", "",
                        "encryptMemberId", hashed_id,
                        "encryptPassword", hashed_pw,
                        "isCheckGoogleRecaptcha", "false"
                );

                // 로그인 요청
                String postResponse = httpRequest.post(LOGIN_URL, loginData, 0);
                if (postResponse.startsWith("Error")) {
                    mainHandler.post(() -> loginCallback.onLoginFailure("Login request failed"));
                    return;
                }

                // 로그인 상태 확인
                String loginStatusResponse = httpRequest.get(LOGIN_STATUS_URL);
                JSONObject jsonResponse = new JSONObject(loginStatusResponse);
                boolean isLogin = jsonResponse.getJSONObject("data").getBoolean("loggedIn");

                mainHandler.post(() -> {
                    if (isLogin) {
                        loginCallback.onLoginSuccess("Login Completed");
                    } else {
                        loginCallback.onLoginFailure("Failed to login");
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> loginCallback.onLoginFailure("Error during login process"));
            }
        });
    }

    // Product parser 메서드
    public void productParser() {
        backgroundHandler.post(() -> {
            int num = 0;
            List<Product> products = new ArrayList<>();

            while (true) {
                try {
                    String response = httpRequest.get("https://www.musinsa.com/preuser/api/v1/event/cursor?&cursorId=" + num + "&state=OPEN");
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONArray events = data.getJSONArray("events");
                    boolean hasNext = data.getBoolean("hasNext");

                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        String eventId = event.getString("eventId");

                        String eventResponse = httpRequest.get("https://www.musinsa.com/preuser/api/v1/event/events/" + eventId);
                        JSONObject eventJson = new JSONObject(eventResponse);
                        JSONObject eventData = eventJson.getJSONObject("data");

                        if (eventData.getBoolean("isRegistration")) {
                            continue;
                        }

                        Product prod = new Product(event.getString("brandName"), event.getString("brand"), event.getString("productName"));
                        prod.eventId = eventId;
                        prod.productsArray = eventData.getJSONArray("products");

                        String f_uniqueId = prod.productsArray.getJSONObject(0).getString("uniqueId");
                        String productResponse = httpRequest.get("https://www.musinsa.com/products/" + f_uniqueId);
                        Document doc = Jsoup.parse(productResponse);

                        for (Element metaTag : doc.select("meta")) {
                            if ("product:price:amount".equals(metaTag.attr("property"))) {
                                prod.saledPrice = metaTag.attr("content");
                            } else if ("product:price:normal_price".equals(metaTag.attr("property"))) {
                                prod.normalPrice = metaTag.attr("content");
                                break;
                            }
                        }

                        for (Element scriptTag : doc.select("script")) {
                            if (scriptTag.html().contains("thumbnailImageUrl")) {
                                try {
                                    String scriptContent = scriptTag.html().split("window.__MSS__.product.state = ", 2)[1];
                                    scriptContent = scriptContent.substring(0, scriptContent.lastIndexOf(";"));
                                    scriptContent = scriptContent.replace("true", "true")
                                            .replace("false", "false")
                                            .replace("null", "null")
                                            .replace("\\/", "/");

                                    JSONObject productJson = new JSONObject(scriptContent);
                                    prod.imgs.add("https://image.msscdn.net/thumbnails"+productJson.getString("thumbnailImageUrl"));
                                    JSONArray goodsImages = productJson.getJSONArray("goodsImages");
                                    for (int j = 0; j < goodsImages.length(); j++) {
                                        prod.imgs.add("https://image.msscdn.net/thumbnails"+goodsImages.getJSONObject(j).getString("imageUrl"));
                                    }
                                } catch (Exception e) {
                                    Log.e("ProductParser", "Error parsing images", e);
                                }
                                break;
                            }
                        }

                        // JSON 데이터 파싱 후 populateSizeTable 호출
                        try {
                            String sizeResponse = httpRequest.get("https://goods-detail.musinsa.com/api2/goods/" + f_uniqueId + "/actual-size");
                            JSONObject sizeJson = new JSONObject(sizeResponse);

                            // "data" 필드가 null인지 확인
                            if (!sizeJson.isNull("data")) {
                                JSONObject dataObject = sizeJson.getJSONObject("data");
                                JSONArray sizesArray = dataObject.getJSONArray("sizes");
                                prod.size = sizesArray;
                            }
                        } catch (JSONException e) {
                            Log.e("ProductAdapter", "Error fetching size data", e);
                        }

                        products.add(prod);

                        // TextView에 실시간 업데이트
                        mainHandler.post(() -> {
                            String updatedText = "Brand: " + prod.brandName + ", Name: " + prod.productName;
                            addText(updatedText);
                        });
                    }

                    if (!hasNext) {
                        products.sort((p1, p2) -> Integer.compare(
                                Integer.parseInt(p2.normalPrice), Integer.parseInt(p1.normalPrice)
                        ));
                        mainHandler.post(() -> productCallback.onSuccess(products));
                        break;
                    } else {
                        num += 50;
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> productCallback.onFailure(e));
                    break;
                }
            }
        });
    }

    // 주소 정보 가져오기 메서드
    public void fetchAddressInfo() {
        backgroundHandler.post(() -> {
            try {
                String addressResponse = httpRequest.get(GET_ADDRESS_URL);
                JSONObject jsonResponse = new JSONObject(addressResponse);
                JSONObject address = jsonResponse.getJSONObject("data").getJSONObject("defaultAddress");

                // UserInfo 객체 생성 및 초기화
                userInfo = new UserInfo(
                        address.getString("address1"),
                        address.getString("address2"),
                        address.getString("phone"),
                        address.getString("name"),
                        address.getString("zipcode")
                );

                Log.d("Sinchung", "Address info fetched successfully");

            } catch (Exception e) {
                Log.e("Sinchung", "Failed to fetch address info", e);
            }
        });
    }

    // 신청 메서드
    public void apply(Product product, ApplyCallback applyCallback) {
        backgroundHandler.post(() -> {
            try {
                // 신청 데이터를 담은 Map 구성
                Map<String, String> applyData = Map.of(
                        "address1", userInfo.address1,
                        "address2", userInfo.address2,
                        "eventId", product.eventId,
                        "phoneNumber", userInfo.phoneNumber,
                        "productName", product.productName,
                        "productOption1", product.selected_option1,
                        "productOption2", product.selected_option2,
                        "productUniqueId", product.selected_uniqueId,
                        "receiver", userInfo.name,
                        "zipcode", userInfo.zipcode
                );

                // 신청 요청
                String likeResponse = httpRequest.post(String.format(LIKE_URL, product.brandEngName), null, 1); //좋아요
                Log.d("Sinchung", "Like response: " + likeResponse);

                String applyResponse = httpRequest.post(APPLY_URL, applyData, 1);
                Log.d("Sinchung", "Apply response: " + applyResponse);
                if (applyResponse.startsWith("Error")) {
                    mainHandler.post(() -> applyCallback.onApplyFailure("Apply request failed"));
                    return;
                }

                // 신청 상태 확인
                JSONObject jsonResponse = new JSONObject(applyResponse);
                boolean isSuccess = jsonResponse.getBoolean("data"); // true이면 신청 완료

                mainHandler.post(() -> {
                    if (isSuccess) {
                        applyCallback.onApplySuccess("Apply Completed Successfully");
                    } else {
                        applyCallback.onApplyFailure("Failed to apply");
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> applyCallback.onApplyFailure("Error during apply process"));
            }
        });
    }

    public interface LoginCallback {
        void onLoginSuccess(String message);
        void onLoginFailure(String message);
    }

    public interface ProductCallback {
        void onSuccess(List<Product> products);
        void onFailure(Exception e);
    }

    // ApplyCallback 인터페이스 정의
    public interface ApplyCallback {
        void onApplySuccess(String message);
        void onApplyFailure(String message);
    }
}