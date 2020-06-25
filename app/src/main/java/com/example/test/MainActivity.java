package com.example.test;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.example.ocrlibrary.CameraViewActivity;
import com.example.ocrlibrary.Helper.HeightProvider;
import com.example.ocrlibrary.Helper.KeyboardUtils;
import com.example.ocrlibrary.HighlightAndSelectActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText assetName, assetId, assetTag, focusedView;
    LinearLayout suggestionsView;
    RelativeLayout suggestionsCard, imagePicker;
    ImageButton startImageActivity;
    ArrayList<String> suggestionsList = new ArrayList<String>();
    Uri croppedUri;
    int CAMERA_VIEW_REQUESTCODE = 5;
    int HIGHLIGHT_VIEW_REQESTCODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imagePicker = findViewById(R.id.imagePicker);
        assetName = findViewById(R.id.asset_name);
        assetId = findViewById(R.id.asset_id);
        assetTag = findViewById(R.id.asset_tag);
        startImageActivity = findViewById(R.id.start_image_activity);
        suggestionsView = findViewById(R.id.suggestionsList);
        suggestionsCard = findViewById(R.id.bottomView);

        List<EditText> editTextList = new ArrayList<EditText>();
        editTextList.add(assetName);
        editTextList.add(assetId);
        editTextList.add(assetTag);

        View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    focusedView = (EditText) v;
                } else {
                    focusedView = null;
                }
            }
        };

        TextWatcher edtTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String characters = s.toString();
                if (characters.length() >= 2) {
                    showCustomSuggestions(characters, suggestionsList);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        for (EditText assetFields : editTextList) {
            assetFields.setOnFocusChangeListener(focusListener);
            assetFields.addTextChangedListener(edtTextWatcher);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        imagePicker.setVisibility(View.INVISIBLE);
        suggestionsCard.setVisibility(View.INVISIBLE);
        suggestionsView.removeAllViews();

        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                if (isVisible) {
                    if (suggestionsList.isEmpty()) {
                        imagePicker.setVisibility(View.VISIBLE);
                    } else {
                        suggestionsCard.setVisibility(View.VISIBLE);
                    }
                } else {
                    imagePicker.setVisibility(View.INVISIBLE);
                    suggestionsCard.setVisibility(View.INVISIBLE);
                }
            }
        });
        new HeightProvider(MainActivity.this).init().setHeightListener(new HeightProvider.HeightListener() {
            @Override
            public void onHeightChanged(int height) {
                suggestionsCard.setTranslationY(-height);
                imagePicker.setTranslationY(-height);
            }
        });

        startImageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(MainActivity.this, HighlightAndSelectActivity.class);
                Uri uri = croppedUri;
                intent1.putExtra("uri", uri);
                startActivityForResult(intent1, HIGHLIGHT_VIEW_REQESTCODE);
            }
        });

        imagePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraViewActivity.class);
                startActivityForResult(intent, CAMERA_VIEW_REQUESTCODE);
            }
        });
    }

    public void showCustomSuggestions(String s, ArrayList<String> entireList) {
        try {
            ArrayList<String> filteredList = new ArrayList<String>();
            ArrayList<TextView> li = new ArrayList<TextView>();
            filteredList.clear();
            li.clear();
            suggestionsView.removeAllViews();
            for (String text : entireList) {
                if (text.trim().toLowerCase().startsWith(s.trim().toLowerCase())) {
                    filteredList.add(text);
                }
            }
            for (int i = 0; i < filteredList.size(); i++) {
                final TextView tv = new TextView(this);
                TextView bar = new TextView(this);
                bar.setText(" | ");
                bar.setTextColor(Color.parseColor("#60ffffff"));
                bar.setTextSize(22);
                tv.setText(filteredList.get(i));
                tv.setTextSize(18);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                suggestionsView.addView(tv);
                suggestionsView.addView(bar);
                li.add(tv);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        focusedView.setText(tv.getText().toString().trim());
                    }
                });
            }

        } catch (Exception e) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HIGHLIGHT_VIEW_REQESTCODE && resultCode == Activity.RESULT_OK && data.hasExtra("suggestionsList")) {
            focusedView.setText(data.getStringExtra("captured Texts"));
            if (!data.getStringArrayListExtra("suggestionsList").isEmpty()) {
                suggestionsList = data.getStringArrayListExtra("suggestionsList");
                croppedUri = (Uri) data.getExtras().get("uri");
            }
        }
        if (requestCode == HIGHLIGHT_VIEW_REQESTCODE && resultCode == 101 ) {
            focusedView.setText(data.getStringExtra("result"));
        }

        if (requestCode == CAMERA_VIEW_REQUESTCODE && resultCode == 101) {
                focusedView.setText(data.getStringExtra("result"));
        }
        if (requestCode == CAMERA_VIEW_REQUESTCODE && resultCode == Activity.RESULT_OK && data.hasExtra("suggestionsList")) {
            suggestionsList = data.getStringArrayListExtra("suggestionsList");
            croppedUri = (Uri) data.getExtras().get("croppeduri");
        }
    }
}
