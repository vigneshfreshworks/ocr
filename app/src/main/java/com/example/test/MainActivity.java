package com.example.test;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.ocrlibrary.Helper.RescaleBitmap;
import com.example.ocrlibrary.HighlightAndSelectActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText assetName;
    LinearLayout suggestionsView;
    RelativeLayout suggestionsCard, imagePicker;
    ImageButton startImageActivity;
    ArrayList<String> suggestionsList = new ArrayList<String>();
    Intent intent;
    Uri uri;
    Bitmap imageBitmap;
    RescaleBitmap rescaleBitmap = new RescaleBitmap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imagePicker = findViewById(R.id.imagePicker);
        assetName = findViewById(R.id.name);
        startImageActivity = findViewById(R.id.start_image_activity);
        suggestionsView = findViewById(R.id.suggestionsList);
        suggestionsCard = findViewById(R.id.bottomView);

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
        assetName.addTextChangedListener(edtTextWatcher);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Intent intent = getIntent();
        imagePicker.setVisibility(View.INVISIBLE);
        suggestionsCard.setVisibility(View.INVISIBLE);
        if (intent.hasExtra("result")) {
            assetName.setText(intent.getStringExtra("result"));
        }
        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                if (isVisible) {
                    if (suggestionsList == null) {
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

        try {
            suggestionsList = intent.getStringArrayListExtra("suggestionsList");
        } catch (Exception e) {
        }

        startImageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(MainActivity.this, HighlightAndSelectActivity.class);
                Uri uri = (Uri) intent.getExtras().get("croppeduri");
                intent1.putExtra("uri", uri);
                startActivityForResult(intent1, 2);
            }
        });

        imagePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraViewActivity.class);
                startActivity(intent);
            }
        });
    }

    public void showCustomSuggestions(String s, ArrayList<String> entireList) {
        try {
            Intent intent = getIntent();
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
                        Log.i("text", tv.getText().toString());
                        assetName.setText(tv.getText().toString().trim());
                    }
                });
            }

        } catch (Exception e) {

        }
    }

    @SuppressLint("MissingSuperCall")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            assetName.setText(data.getStringExtra("captured Texts"));
            if (!data.getStringArrayListExtra("suggestionsList").isEmpty()) {
                suggestionsList = data.getStringArrayListExtra("suggestionsList");
            }
            this.uri = (Uri) data.getExtras().get("uri");
            imageBitmap = rescaleBitmap.getBitmap(uri, getContentResolver());
        }
    }
}
