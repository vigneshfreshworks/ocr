package com.example.ocrlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CropActivity extends AppCompatActivity {
    com.theartofdev.edmodo.cropper.CropImageView cropImageView;
    Bitmap bitmap, modifiedBitmap;
    ImageButton rotateLeft, rotateRight, flipHorizontal, flipVertical;
    Button useButton, BackButton;
    Uri uri;
    ArrayList<String> suggestionsList = new ArrayList<String>();
    FirebaseVisionText textsFromImage;
    static Date currentTime;
    String isStartedFromHighlightActivity = "false";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_activity);
        cropImageView = findViewById(R.id.cropImageView);
        rotateLeft = findViewById(R.id.rotate_left);
        rotateRight = findViewById(R.id.rotate_right);
        flipHorizontal = findViewById(R.id.flip_horizontal);
        flipVertical = findViewById(R.id.flip_vertical);
        useButton = findViewById(R.id.use);
        BackButton = findViewById(R.id.backButton);
        getSupportActionBar().hide();
        isStartedFromHighlightActivity = "false";
        Intent intent = getIntent();
        uri = (Uri) intent.getExtras().get("uri");
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            modifiedBitmap = modifyOrientation(bitmap, intent.getExtras().getString("absolutepath"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        cropImageView.setImageBitmap(modifiedBitmap);
        cropImageView.setAspectRatio(18, 18);

        useButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = null;
                Bitmap croppedBitmap = cropImageView.getCroppedImage();
                final Uri croppedUri = getImageUri(croppedBitmap);
//                try {
//                    intent1 = new Intent(CropActivity.this,
//                          Class.forName("com.example.test.MainActivity"));
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
                intent1 = new Intent();
                //final Intent intent1 = new Intent(CropActivity.this, HighlightAndSelectActivity.class);
                if (croppedBitmap != null) {
                    final FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(croppedBitmap);
                    FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
                    textRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(final FirebaseVisionText firebaseVisionText) {
                            suggestionsList.clear();
                            textsFromImage = firebaseVisionText;
                            suggestionsList = createSuggestionsFromTexts(textsFromImage);
                            if (isStartedFromHighlightActivity.equalsIgnoreCase("false")) {
                                Intent mainActivityIntent = new Intent();
                                mainActivityIntent.putStringArrayListExtra("suggestionsList", suggestionsList);
                                mainActivityIntent.putExtra("croppeduri", croppedUri);
                                setResult(Activity.RESULT_OK, mainActivityIntent);
                                finish();
                            }
                            else {
                                Intent highlightActivityIntent = new Intent(CropActivity.this, HighlightAndSelectActivity.class);
                                highlightActivityIntent.putStringArrayListExtra("suggestionsList", suggestionsList);
                                highlightActivityIntent.putExtra("croppeduri", croppedUri);
                                setResult(Activity.RESULT_OK, highlightActivityIntent);
                                startActivity(highlightActivityIntent);
                                isStartedFromHighlightActivity = "false";
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CropActivity.this, "Error " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

                } else {
                    Toast.makeText(CropActivity.this, "Please capture a Image !", Toast.LENGTH_LONG).show();
                }
            }
        });

        rotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImageView.rotateImage(-90);
            }
        });

        rotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImageView.rotateImage(90);
            }
        });

        flipHorizontal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImageView.flipImageHorizontally();
            }
        });

        flipVertical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImageView.flipImageVertically();
            }
        });

        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private ArrayList<String> createSuggestionsFromTexts(final FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        String text = "";
        ArrayList<String> results = new ArrayList<String>();
        if (blocks.size() == 0) {
            Toast.makeText(CropActivity.this, "Image contains No Text !", Toast.LENGTH_SHORT).show();
        } else {
            for (int block = 0; block < blocks.size(); block++) {
                List<FirebaseVisionText.Line> lines = blocks.get(block).getLines();
                for (int line = 0; line < lines.size(); line++) {
                    String[] textsInLine = lines.get(line).getText().split(" ");
                    for (int eachText = 0; eachText < textsInLine.length; eachText++) {
                        for (int i = eachText; i < textsInLine.length; i++) {
                            text = text + " " + textsInLine[i];
                            results.add(text);
                        }
                        text = "";
                    }
                }
            }
        }
        return results;
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), inImage, "Title" + " - " + (currentTime = Calendar.getInstance().getTime()), null);
        return Uri.parse(path);
    }
}
