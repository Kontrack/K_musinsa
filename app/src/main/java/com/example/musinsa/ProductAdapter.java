package com.example.musinsa;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.content.Context;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.TableRow;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> productList;
    private final Sinchung sinchung;

    public ProductAdapter(List<Product> productList, Sinchung sinchung) { // 생성자 수정
        this.productList = productList;
        this.sinchung = sinchung;
    }

    private void populateSizeTable(ProductViewHolder holder, JSONArray sizesArray) {
        try {
            // TableLayout 초기화
            holder.specTable.removeAllViews();

            // 첫 번째 행에 속성명(예: "총장", "어깨너비" 등)을 추가
            TableRow headerRow = new TableRow(holder.itemView.getContext());
            headerRow.addView(createTextView("Size", holder.itemView.getContext())); // 사이즈 이름 헤더 추가
            if (sizesArray.length() > 0) {
                JSONArray itemsArray = sizesArray.getJSONObject(0).getJSONArray("items");
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject item = itemsArray.getJSONObject(i);
                    headerRow.addView(createTextView(item.getString("name"), holder.itemView.getContext())); // 각 속성 이름 추가
                }
            }
            holder.specTable.addView(headerRow); // 헤더 행 추가

            // 각 사이즈에 대해 데이터 행을 추가
            for (int i = 0; i < sizesArray.length(); i++) {
                JSONObject size = sizesArray.getJSONObject(i);
                TableRow dataRow = new TableRow(holder.itemView.getContext());

                // 사이즈 이름 추가
                dataRow.addView(createTextView(size.getString("name"), holder.itemView.getContext()));

                // 각 속성 값 추가
                JSONArray itemsArray = size.getJSONArray("items");
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject item = itemsArray.getJSONObject(j);
                    double value = item.getDouble("value");
                    dataRow.addView(createTextView(String.valueOf(value), holder.itemView.getContext()));
                }

                holder.specTable.addView(dataRow); // 데이터 행 추가
            }
        } catch (Exception e) {
            Log.e("ProductAdapter", "Error populating size table", e);
        }
    }

    // 텍스트뷰 생성 함수
    private TextView createTextView(String text, Context context) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        return textView;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // ViewHolder에 데이터 설정
        holder.brandName.setText(product.brandName);
        holder.productName.setText(product.productName);
        holder.normalPrice.setText("정가: " + product.normalPrice + "원");
        holder.saledPrice.setText("할인가: " + product.saledPrice + "원");

        // 이미지 슬라이더 설정
        Log.d("D", "Image URLs: " + product.imgs);
        ImageSliderAdapter imageSliderAdapter = new ImageSliderAdapter(product.imgs);
        holder.imageSlider.setAdapter(imageSliderAdapter);

        // 개별 옵션 스피너 설정
        List<String> optionList = new ArrayList<>();
        optionList.add("옵션을 선택하세요"); // 안내 문구를 첫 번째 옵션으로 추가

        try {
            JSONArray productsArray = product.productsArray;
            for (int i = 0; i < productsArray.length(); i++) {
                JSONObject productObj = productsArray.getJSONObject(i);
                String name = productObj.getString("name"); // 각 요소의 name 값 추출
                optionList.add(name);
            }
        } catch (JSONException e) {
            Log.e("JSONError", "Failed to parse productsArray for product: " + product.productName, e);
        }

        // 옵션 목록을 스피너 어댑터에 설정
        ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, optionList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.optionSpinner1.setAdapter(adapter);

        // 첫 번째 옵션 스피너에 리스너 설정
        holder.optionSpinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) { // 첫 번째 항목(안내 문구)이 아닌 경우에만 이벤트 처리
                    String selectedOption = optionList.get(position);
                    try {
                        product.selected_productName = selectedOption;
                        product.selected_uniqueId = product.productsArray.getJSONObject(position - 1).getString("uniqueId");

                        // 두 번째 드롭박스를 option1 값으로 설정
                        JSONArray option1Array = product.productsArray.getJSONObject(position - 1).getJSONArray("option1");
                        List<String> option1List = new ArrayList<>();
                        for (int i = 0; i < option1Array.length(); i++) {
                            option1List.add(option1Array.getString(i));
                        }
                        ArrayAdapter<String> option1Adapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, option1List);
                        option1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        holder.optionSpinner2.setAdapter(option1Adapter);

                        // 세 번째 드롭박스를 option2 값으로 설정
                        JSONArray option2Array = product.productsArray.getJSONObject(position - 1).getJSONArray("option2");
                        List<String> option2List = new ArrayList<>();
                        for (int i = 0; i < option2Array.length(); i++) {
                            option2List.add(option2Array.getString(i));
                        }
                        ArrayAdapter<String> option2Adapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, option2List);
                        option2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        holder.optionSpinner3.setAdapter(option2Adapter);

                    } catch (JSONException e) {
                        Toast.makeText(holder.itemView.getContext(), "옵션 선택에 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 첫 번째 옵션 스피너에서 안내 문구를 선택했을 때 초기화
                    product.selected_productName = "";
                    product.selected_uniqueId = "";
                    product.selected_option1 = "";
                    product.selected_option2 = "";
                    holder.optionSpinner2.setAdapter(null);
                    holder.optionSpinner3.setAdapter(null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무 것도 선택되지 않았을 때의 처리
            }
        });

        // 두 번째 옵션 스피너에 리스너 설정
        holder.optionSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption1 = parent.getItemAtPosition(position).toString();
                product.selected_option1 = selectedOption1;  // 선택된 option1 값을 product에 저장
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무 것도 선택되지 않았을 때의 처리
            }
        });

        // 세 번째 옵션 스피너에 리스너 설정
        holder.optionSpinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption2 = parent.getItemAtPosition(position).toString();
                product.selected_option2 = selectedOption2;  // 선택된 option2 값을 product에 저장
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무 것도 선택되지 않았을 때의 처리
            }
        });

        if (product.size != null && product.size.length() > 0) {
            populateSizeTable(holder, product.size); // 사이즈 테이블 채우기
        } else {
            holder.specTable.removeAllViews(); // 사이즈 정보가 없을 때 테이블 비우기
        }

        // 신청 버튼 클릭 이벤트
        holder.applyButton.setOnClickListener(v -> {
            // 신청 데이터가 유효하지 않은지 확인
            if (product.selected_productName.isEmpty() ||
                    product.selected_uniqueId.isEmpty() ||
                    product.selected_option1.isEmpty() ||
                    ((holder.optionSpinner3.getAdapter().getCount() > 0)) && product.selected_option2.isEmpty()) {
                // 하나 이상의 필드가 비어있거나 3번 드롭박스의 어댑터가 비어있는 경우
                Toast.makeText(v.getContext(), "모든 옵션을 선택해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                // 모든 필드가 유효할 때 apply 호출
                sinchung.apply(product, new Sinchung.ApplyCallback() {
                    @Override
                    public void onApplySuccess(String message) {
                        // 버튼 비활성화 및 텍스트 변경
                        holder.applyButton.setEnabled(false);
                        holder.applyButton.setText("신청 완료");
                        holder.applyButton.setBackgroundColor(Color.LTGRAY); // 회색으로 변경
                        Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onApplyFailure(String message) {
                        Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // ViewHolder 클래스 정의
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView brandName, productName, normalPrice, saledPrice;
        ViewPager2 imageSlider;
        Spinner optionSpinner1, optionSpinner2, optionSpinner3;
        Button applyButton;
        TableLayout specTable;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            brandName = itemView.findViewById(R.id.brand_name);
            productName = itemView.findViewById(R.id.product_name);
            normalPrice = itemView.findViewById(R.id.normal_price);
            saledPrice = itemView.findViewById(R.id.saled_price);
            imageSlider = itemView.findViewById(R.id.image_slider);
            optionSpinner1 = itemView.findViewById(R.id.option_spinner1);
            optionSpinner2 = itemView.findViewById(R.id.option_spinner2);
            optionSpinner3 = itemView.findViewById(R.id.option_spinner3);
            applyButton = itemView.findViewById(R.id.apply_button);
            specTable = itemView.findViewById(R.id.spec_table);
        }
    }
}