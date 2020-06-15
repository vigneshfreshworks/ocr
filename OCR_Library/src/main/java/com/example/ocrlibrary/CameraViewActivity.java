package com.example.ocrlibrary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CameraViewActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener {

    CameraView camera;
    FrameLayout frameLayout;
    RelativeLayout layout, captureGallaryBtn, barcodeScannerOverlayView, cameraLayout, qrCodeLayout, barcodeLayout;
    ImageButton captureButton, gallary, closeBtn, flashButton;
    View qrCodeBoundry;
    boolean isDetected = false;
    FirebaseVisionBarcodeDetectorOptions options;
    FirebaseVisionBarcodeDetector detector;
    GestureDetector mGestureDetector;
    TextView centerTextView, scannerText;
    public static final int IMAGE_PICK_CODE = 1000;
    public static final int PERMISSION_CODE = 1001;
    static Date currentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cameraview);
        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this);
        frameLayout = findViewById(R.id.cameraView);
        LayoutInflater inflater = LayoutInflater.from(this);
        layout = (RelativeLayout) inflater.inflate(R.layout.layout_selectors, null, false);
        barcodeScannerOverlayView = (RelativeLayout) inflater.inflate(R.layout.layout_barcode, null, false);
        frameLayout.addView(layout);
        closeBtn = findViewById(R.id.close_Btn);
        gallary = findViewById(R.id.gallary);
        captureButton = findViewById(R.id.captureButton);
        qrCodeBoundry = findViewById(R.id.boundry_qrcode);
        captureGallaryBtn = findViewById(R.id.capture_gallary_Btn);
        cameraLayout = findViewById(R.id.overlay_camera);
        qrCodeLayout = findViewById(R.id.overlay_qrCode);
        barcodeLayout = findViewById(R.id.overlay_barcode);
        centerTextView = findViewById(R.id.center_textview_camera);
        flashButton = findViewById(R.id.flash_btn);
        scannerText = findViewById(R.id.scannerText);
        mGestureDetector = new GestureDetector(this, this);

        getSupportActionBar().hide();
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture();
            }
        });
        camera.setPictureFormat(PictureFormat.JPEG);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull final PictureResult result) {
                super.onPictureTaken(result);
                byte[] data = result.getData();
                result.toBitmap(new BitmapCallback() {
                    @Override
                    public void onBitmapReady(@Nullable Bitmap bitmap) {
                        Bitmap map = bitmap;
                        Log.i("Bitmap", "" + map);
                        Uri uri = getImageUri(bitmap);
                        Intent intent = new Intent(CameraViewActivity.this, CropActivity.class);
                        intent.putExtra("uri", uri);
                        intent.putExtra("absolutepath", getRealPathFromURI(uri));
                        startActivity(intent);
                    }
                });

            }
        });
        gallary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions, PERMISSION_CODE);

                    } else {
                        pickImageFromGallary();
                    }
                } else {
                    pickImageFromGallary();
                }
            }
        });
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flashButton.getBackground() == null) {
                    flashButton.setBackgroundColor(Color.WHITE);
                    camera.setFlash(Flash.ON);
                } else {
                    flashButton.setBackground(null);
                    camera.setFlash(Flash.OFF);
                }
            }
        });
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        camera.setOnTouchListener(this);
        barcodeLayout.setVisibility(View.INVISIBLE);
        qrCodeLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }

    public void processImage(FirebaseVisionImage image) {
        if (!isDetected) {
            detector.detectInImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                            String s = processResult(firebaseVisionBarcodes);
                            if (s != null) {
                                try {
                                    detector.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
//                                Intent intent = new Intent(CameraViewActivity.this, HighlightAndSelectActivity.class);
                                Intent intent = null;
                                try {
                                    intent = new Intent(CameraViewActivity.this,
                                            Class.forName("com.example.test.MainActivity"));
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                                intent.putExtra("result", s);
                                startActivity(intent);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CameraViewActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    private String processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        String result = null;
        if (firebaseVisionBarcodes.size() > 0) {
            isDetected = true;
            for (FirebaseVisionBarcode item : firebaseVisionBarcodes) {
                int value_type = item.getValueType();
                switch (value_type) {
                    case FirebaseVisionBarcode.TYPE_TEXT: {
                        result = item.getRawValue().toString();
                    }
                    break;
                    case FirebaseVisionBarcode.TYPE_URL: {
                        result = item.getRawValue();
                    }
                    break;
                    case FirebaseVisionBarcode.TYPE_CONTACT_INFO: {
                        result = item.getContactInfo().getName().getFormattedName();

                    }
                    break;
                    default:
                        result = item.getRawValue().toString();
                        break;
                }
            }
        }
        return result;
    }

    private FirebaseVisionImage getVisionImageFromFrame(Frame frame) {
        byte[] data = frame.getData();
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(frame.getSize().getHeight())
                .setWidth(frame.getSize().getWidth()).build();
        return FirebaseVisionImage.fromByteArray(data, metadata);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return false;
    }


    @Override
    public boolean onDown(MotionEvent e) {
        float[] positions = new float[2];
        positions[0] = e.getX();
        positions[1] = e.getY();
        Log.i("called", "onDown" + Arrays.toString(positions));
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float[] positions = new float[2];
        positions[0] = e.getX();
        positions[1] = e.getY();
        Log.i("called", "onDown" + positions.toString());
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        int SWIPE_THRESHOLD = 10;
        int SWIPE_VELOCITY_THRESHOLD = 100;
        boolean result = true;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        if (centerTextView.getText().toString().equals("BAR CODE")) {
                            barCodeToQRCodeTransition();
                        } else if (centerTextView.getText().toString().equals("QR CODE")) {
                            qrCodeToCameraTransition();
                        }
                    } else {
                        if (centerTextView.getText().toString().equals("CAMERA")) {
                            cameraToQRCodeTransition();
                        } else if (centerTextView.getText().toString().equals("QR CODE")) {
                            qrCodeToBarCodeTransition();
                        }
                    }
                    result = true;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    public void cameraToQRCodeTransition() {
        qrCodeBoundry.setVisibility(View.VISIBLE);
        cameraLayout.setVisibility(View.INVISIBLE);
        qrCodeLayout.setVisibility(View.VISIBLE);
        barcodeLayout.setVisibility(View.INVISIBLE);
        centerTextView = findViewById(R.id.center_textview_qrcode);
        scannerText.setVisibility(View.VISIBLE);
        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImage(getVisionImageFromFrame(frame));
            }
        });
        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    public void qrCodeToCameraTransition() {
        qrCodeBoundry.setVisibility(View.INVISIBLE);
        cameraLayout.setVisibility(View.VISIBLE);
        barcodeLayout.setVisibility(View.INVISIBLE);
        qrCodeLayout.setVisibility(View.INVISIBLE);
        centerTextView = findViewById(R.id.center_textview_camera);
        scannerText.setVisibility(View.INVISIBLE);
    }


    public void qrCodeToBarCodeTransition() {
        qrCodeBoundry.setVisibility(View.INVISIBLE);
        frameLayout.addView(barcodeScannerOverlayView);
        barcodeLayout.setVisibility(View.VISIBLE);
        qrCodeLayout.setVisibility(View.INVISIBLE);
        cameraLayout.setVisibility(View.INVISIBLE);
        centerTextView = findViewById(R.id.center_textview_barcode);
        scannerText.setVisibility(View.INVISIBLE);
        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImage(getVisionImageFromFrame(frame));
            }
        });
        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_CODABAR, FirebaseVisionBarcode.FORMAT_CODE_128)
                .build();

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    public void barCodeToQRCodeTransition() {
        frameLayout.removeView(barcodeScannerOverlayView);
        qrCodeBoundry.setVisibility(View.VISIBLE);
        qrCodeLayout.setVisibility(View.VISIBLE);
        cameraLayout.setVisibility(View.INVISIBLE);
        barcodeLayout.setVisibility(View.INVISIBLE);
        scannerText.setVisibility(View.VISIBLE);
        centerTextView = findViewById(R.id.center_textview_qrcode);
        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImage(getVisionImageFromFrame(frame));
            }
        });

        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    public Uri getUri(File file) {
        Uri uri = null;
        if (file != null) {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    public void pickImageFromGallary() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            Intent intent = new Intent(CameraViewActivity.this, CropActivity.class);
            intent.putExtra("uri", data.getData());
            intent.putExtra("absolutepath", getRealPathFromURI(data.getData()));
            startActivity(intent);
        }
    }

    public Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), inImage, "Title" + " - " + (currentTime = Calendar.getInstance().getTime()), null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

}
