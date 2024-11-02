package com.example.musinsa;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements Sinchung.LoginCallback, Sinchung.ProductCallback, Sinchung.ApplyCallback {

    private RecyclerView recyclerView;
    private TextView textView;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Sinchung sinchung;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // findViewById로 뷰 참조
        recyclerView = findViewById(R.id.recyclerView);
        textView = findViewById(R.id.text1);

        // RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Sinchung 인스턴스 생성 및 콜백 설정
        sinchung = new Sinchung(this, this, mainHandler, textView);

        // 로그인 및 데이터 파싱
        sinchung.login();
    }

    @Override
    public void onLoginSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d("D", message);
        sinchung.fetchAddressInfo();
        sinchung.productParser();
    }

    @Override
    public void onLoginFailure(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e("E", message);
    }

    @Override
    public void onSuccess(List<Product> products) {
        Log.d("D", "Number of products: " + products.size());

        // 데이터가 비어 있는지 확인
        if (products.isEmpty()) {
            Log.e("RecyclerView", "Product list is empty");
            return;
        }

        // 파싱된 Product 리스트 저장
        this.productList = products;
        productAdapter = new ProductAdapter(productList, sinchung);
        recyclerView.setAdapter(productAdapter);

        // 초기 메시지 제거
        ViewGroup parent = (ViewGroup) textView.getParent();
        if (parent != null) {
            parent.removeView(textView);
        }
    }

    @Override
    public void onFailure(Exception e) {
        Log.e("ProductParser", "Failed to load products", e);
    }

    @Override
    public void onApplySuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onApplyFailure(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}