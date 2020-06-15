package com.example.ocrlibrary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ocrlibrary.Helper.GraphicOverlay;
import com.example.ocrlibrary.Helper.RescaleBitmap;
import com.example.ocrlibrary.Helper.TextGraphic;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.ArrayList;
import java.util.List;

public class HighlightAndSelectActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    ImageView iv;
    Button saveBtn, reTake, backButton;
    Bitmap imageBitmap;
    GraphicOverlay graphicOverlay;
    Intent intent;
    View captureView;
    ArrayList<String> points = new ArrayList<String>();
    ArrayList<String> textFromElement = new ArrayList<String>();
    ArrayList<String> selectedTexts = new ArrayList<String>();
    ArrayList<FirebaseVisionText.Element> selectedElements = new ArrayList<FirebaseVisionText.Element>();
    List<FirebaseVisionText.Element> elements;
    ArrayList<String> suggestionsList = new ArrayList<String>();
    TextView resultView;
    FirebaseVisionText textsFromImage;
    GestureDetector mGestureDetector;
    Uri uri;
    RescaleBitmap rescaleBitmap = new RescaleBitmap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highlight_and_select);
        intent = getIntent();
        iv = findViewById(R.id.imageView);
        captureView = findViewById(R.id.captureView);
        resultView = findViewById(R.id.resultView);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        saveBtn = findViewById(R.id.saveButton);
        reTake = findViewById(R.id.retake);
        backButton = findViewById(R.id.backButton);
        uri = (Uri) intent.getExtras().get("uri");
        getSupportActionBar().hide();
        try {

            imageBitmap = rescaleBitmap.getBitmap((Uri) intent.getExtras().get("uri"), getContentResolver());

        } catch (Exception e) {
            e.printStackTrace();
        }
        iv.setImageBitmap(imageBitmap);
        mGestureDetector = new GestureDetector(this, this);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("captured Texts", resultView.getText().toString().trim());
                intent.putExtra("uri", uri);
                intent.putStringArrayListExtra("suggestionsList", suggestionsList);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        reTake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HighlightAndSelectActivity.this, CameraViewActivity.class);
                startActivity(intent);
            }
        });

        captureView.setOnTouchListener(this);
        iv.setOnTouchListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (iv.getDrawable() != null) {
            final FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
            FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            textRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                @Override
                public void onSuccess(final FirebaseVisionText firebaseVisionText) {
                    textsFromImage = firebaseVisionText;
                    elements = getAllElementsFromFirebaseTexts(firebaseVisionText);
                    getTextPositionsOnScreen(elements);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(HighlightAndSelectActivity.this, "Error " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } else {
            Toast.makeText(HighlightAndSelectActivity.this, "Please capture a Image !", Toast.LENGTH_LONG).show();
        }
    }

    public List<FirebaseVisionText.Element> getAllElementsFromFirebaseTexts(final FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        List<FirebaseVisionText.Element> elements = new ArrayList<FirebaseVisionText.Element>();
        for (int block = 0; block < blocks.size(); block++) {
            List<FirebaseVisionText.Line> lines = blocks.get(block).getLines();
            for (int line = 0; line < lines.size(); line++) {
                for (int i = 0; i < lines.get(line).getElements().size(); i++) {
                    elements.add(lines.get(line).getElements().get(i));
                }
            }
        }
        return elements;
    }

    public void getTextPositionsOnScreen(List<FirebaseVisionText.Element> elements) {
        points.clear();
        textFromElement.clear();
        graphicOverlay.clear();
        for (int i = 0; i < elements.size(); i++) {
            Point[] p = elements.get(i).getCornerPoints();
            points.add(p[0].toString().substring(6, p[0].toString().length() - 1) +
                    ", " + p[1].toString().substring(6, p[1].toString().length() - 1) +
                    ", " + p[2].toString().substring(6, p[2].toString().length() - 1) +
                    ", " + p[3].toString().substring(6, p[3].toString().length() - 1));
            textFromElement.add(elements.get(i).getText());
        }
    }

    public void updateResultView(float[] coOrdinates) {
        StringBuffer str = new StringBuffer();
        for (String val : selectedTexts) {
            str.append(" ").append(val);
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        lp.setMargins((int) coOrdinates[0] - 100, (int) coOrdinates[1] + 300, 0, 0);
        resultView.setLayoutParams(lp);
        resultView.setText(str.toString());
    }

    public String getValueAtPointer(float[] coOrdinates) {
        String currentValue = null;
        for (int i = 0; i < points.size(); i++) {
            String[] str = points.get(i).split(",");
            int startX = Integer.parseInt(str[0].trim());
            int endX = Integer.parseInt(str[2].trim());
            int startY = Integer.parseInt(str[1].trim());
            int endY = Integer.parseInt(str[7].trim());
            if (coOrdinates[0] > startX && coOrdinates[0] < endX) {
                if (coOrdinates[1] > startY && coOrdinates[1] < endY) {
                    currentValue = textFromElement.get(i);
                    break;
                }
            }
        }
        return currentValue;
    }

    public String selectValuesOnScroll(float[] coOrdinates, List<FirebaseVisionText.Element> elements) {
        String currentValue = getValueAtPointer(coOrdinates);
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getText().equals(currentValue)) {
                if (!selectedTexts.contains(currentValue)) {
                    TextGraphic graphic = new TextGraphic(graphicOverlay, elements.get(i));
                    graphicOverlay.add(graphic);
                    selectedElements.add(elements.get(i));
                    selectedTexts.add(currentValue);
                }
                updateResultView(coOrdinates);
            }
        }
        return currentValue;
    }


    public void selectOrDeselectAValueOnTap(float[] coOrdinates, List<FirebaseVisionText.Element> elements) {
        String currentValue = getValueAtPointer(coOrdinates);
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getText().equals(currentValue)) {
                if (!selectedTexts.contains(currentValue)) {
                    TextGraphic graphic = new TextGraphic(graphicOverlay, elements.get(i));
                    graphicOverlay.add(graphic);
                    selectedElements.add(elements.get(i));
                    selectedTexts.add(currentValue);
                } else {
                    selectedTexts.remove(currentValue);
                    selectedElements.remove(elements.get(i));
                    graphicOverlay.clear();
                    for (FirebaseVisionText.Element element : selectedElements) {
                        TextGraphic graphic1 = new TextGraphic(graphicOverlay, element);
                        graphicOverlay.add(graphic1);
                    }
                }
                updateResultView(coOrdinates);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        float[] positions = new float[2];
        positions[0] = e.getX();
        positions[1] = e.getY();
        selectOrDeselectAValueOnTap(positions, elements);
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float[] positions = new float[2];
        positions[0] = e2.getX();
        positions[1] = e2.getY();
        if (e2.getAction() == MotionEvent.ACTION_MOVE) {
            String s = selectValuesOnScroll(positions, elements);
            if (e2.getAction() == MotionEvent.ACTION_UP) {
                String s1 = selectValuesOnScroll(positions, elements);
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

}
