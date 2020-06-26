package com.example.ocrlibrary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CameraViewActivity extends AppCompatActivity implements View.OnTouchListener, GestureDetector.OnGestureListener, View.OnClickListener {

    CameraView camera;
    FrameLayout frameLayout;
    RelativeLayout layout, captureGallaryBtn, barcodeScannerOverlayView, cameraLayout, qrCodeLayout, barcodeLayout, dynamicLayout;
    ImageButton captureButton, gallary, closeBtn, flashButton;
    View qrCodeBoundry;
    boolean isDetected = false;
    FirebaseVisionBarcodeDetectorOptions options;
    FirebaseVisionBarcodeDetector detector;
    GestureDetector mGestureDetector;
    TextView centerTextView, scannerText, dynamic_center_text, dynamic_right_text, dynamic_left_text;
    public static final int IMAGE_PICK_CODE = 1000;
    public static final int PERMISSION_CODE = 1001;
    static Date currentTime;
    String scannerResult;
    //String[] scanner_options = {"QR CODE", "BAR CODE"};
    //String[] scanner_options = {"CAMERA", "QR CODE"};
    //String[] scanner_options = {"CAMERA", "BAR CODE"};
    //String[] scanner_options = {"QR CODE"};
    String[] scanner_options = {"CAMERA", "QR CODE", "BAR CODE"};

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
        dynamicLayout = findViewById(R.id.overlay_dynamic);
        centerTextView = findViewById(R.id.center_textview_camera);
        flashButton = findViewById(R.id.flash_btn);
        scannerText = findViewById(R.id.scannerText);
        dynamic_center_text = findViewById(R.id.center_textview_dynamic);
        dynamic_right_text = findViewById(R.id.right);
        dynamic_left_text = findViewById(R.id.left);

        mGestureDetector = new GestureDetector(this, this);
        getSupportActionBar().hide();
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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
                        Uri uri = getImageUri(bitmap);
                        Intent intent = new Intent(CameraViewActivity.this, CropActivity.class);
                        intent.putExtra("uri", uri);
                        intent.putExtra("absolutepath", getRealPathFromURI(uri));
                        startActivityForResult(intent, 6);
                    }
                });

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
        gallary.setOnClickListener(this);
        captureButton.setOnClickListener(this);
        if (scanner_options.length == 3) {
            cameraLayout.setVisibility(View.VISIBLE);
            barcodeLayout.setVisibility(View.INVISIBLE);
            qrCodeLayout.setVisibility(View.INVISIBLE);
            dynamicLayout.setVisibility(View.INVISIBLE);
        } else if (scanner_options.length < 3) {
            cameraLayout.setVisibility(View.INVISIBLE);
            barcodeLayout.setVisibility(View.INVISIBLE);
            qrCodeLayout.setVisibility(View.INVISIBLE);
            dynamicLayout.setVisibility(View.VISIBLE);
            if (scanner_options.length == 2) {
                dynamic_center_text.setText(scanner_options[0]);
                dynamic_right_text.setText(scanner_options[1]);
                if (scanner_options[0] == "QR CODE") {
                    setQrCodeLayout();
                }
                else if(scanner_options[0] == "BAR CODE") {
                    setBarcodeLayout();
                }
            }
            if (scanner_options.length == 1) {
                dynamic_center_text.setText(scanner_options[0]);
                if (scanner_options[0] == "QR CODE") {
                    setQrCodeLayout();
                } else if (scanner_options[0] == "BAR CODE") {
                    setBarcodeLayout();
                } else {
                    setCameraLayout();
                }
            }
        }
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
                            scannerResult = processResult(firebaseVisionBarcodes);
                            if (scannerResult != null) {
                                try {
                                    detector.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Intent intent = new Intent();
                                intent.putExtra("result", scannerResult);
                                setResult(101, intent);
                                finish();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Log
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
        int SWIPE_THRESHOLD = 180;
        int SWIPE_VELOCITY_THRESHOLD = 50;
        boolean result = true;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        if (scanner_options.length == 3) {
                            if (centerTextView.getText().toString().equals("BAR CODE")) {
                                barCodeToQRCodeTransition();
                            } else if (centerTextView.getText().toString().equals("QR CODE")) {
                                qrCodeToCameraTransition();
                            }
                        } else if (scanner_options.length == 2) {
                            if (dynamic_center_text.getText().toString().equals("QR CODE")
                                    && dynamic_left_text.getText().toString().equals("CAMERA")) {
                                qrCodeToCameraTransition();
                            } else if (dynamic_center_text.getText().toString().equals("BAR CODE")
                                    && dynamic_left_text.getText().toString().equals("QR CODE")) {
                                barCodeToQRCodeTransition();
                            } else if (dynamic_center_text.getText().toString().equals("BAR CODE")
                                    && dynamic_left_text.getText().toString().equals("CAMERA")) {
                                barCodeToCameraTransition();
                            }
                        }
                    } else {
                        if (scanner_options.length == 3) {
                            if (centerTextView.getText().toString().equals("CAMERA")) {
                                cameraToQRCodeTransition();
                            } else if (centerTextView.getText().toString().equals("QR CODE")) {
                                qrCodeToBarCodeTransition();
                            }
                        } else if (scanner_options.length == 2) {
                            if (dynamic_center_text.getText().equals("CAMERA")
                                    && dynamic_right_text.getText().toString().equals("QR CODE")) {
                                cameraToQRCodeTransition();
                            } else if (dynamic_center_text.getText().toString().equals("QR CODE")
                                    && dynamic_right_text.getText().toString().equals("BAR CODE")) {
                                qrCodeToBarCodeTransition();
                            } else if (dynamic_center_text.getText().equals("CAMERA")
                                    && dynamic_right_text.getText().toString().equals("BAR CODE")) {
                                cameraToBarCodeTransition();
                            }
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

    public void setCameraLayout() {
        captureButton.setEnabled(true);
        gallary.setEnabled(true);
    }

    public void setQrCodeLayout() {
        qrCodeBoundry.setVisibility(View.VISIBLE);
        scannerText.setVisibility(View.VISIBLE);
        captureButton.setEnabled(false);
        gallary.setEnabled(false);
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

    public void setBarcodeLayout() {
        frameLayout.addView(barcodeScannerOverlayView);
        scannerText.setVisibility(View.INVISIBLE);
        captureButton.setEnabled(false);
        gallary.setEnabled(false);
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

    public void cameraToQRCodeTransition() {
        if (scanner_options.length == 3) {
            cameraLayout.setVisibility(View.INVISIBLE);
            qrCodeLayout.setVisibility(View.VISIBLE);
            barcodeLayout.setVisibility(View.INVISIBLE);
            centerTextView = findViewById(R.id.center_textview_qrcode);
        } else {
            scannerText.setVisibility(View.VISIBLE);
            dynamic_center_text.setText("QR CODE");
            dynamic_left_text.setText("CAMERA");
            dynamic_right_text.setText("");
        }
        qrCodeBoundry.setVisibility(View.VISIBLE);
        captureButton.setEnabled(false);
        gallary.setEnabled(false);
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
        if (scanner_options.length == 3) {
            cameraLayout.setVisibility(View.VISIBLE);
            barcodeLayout.setVisibility(View.INVISIBLE);
            qrCodeLayout.setVisibility(View.INVISIBLE);
            centerTextView = findViewById(R.id.center_textview_camera);
        } else {
            scannerText.setVisibility(View.INVISIBLE);
            dynamic_center_text.setText("CAMERA");
            dynamic_right_text.setText("QR CODE");
            dynamic_left_text.setText("");
        }
        qrCodeBoundry.setVisibility(View.INVISIBLE);
        captureButton.setEnabled(true);
        gallary.setEnabled(true);
    }

    public void qrCodeToBarCodeTransition() {
        if (scanner_options.length == 3) {
            qrCodeLayout.setVisibility(View.INVISIBLE);
            cameraLayout.setVisibility(View.INVISIBLE);
            barcodeLayout.setVisibility(View.VISIBLE);
            centerTextView = findViewById(R.id.center_textview_barcode);
        } else {
            dynamic_center_text.setText("BAR CODE");
            dynamic_right_text.setText("");
            dynamic_left_text.setText("QR CODE");
        }
        qrCodeBoundry.setVisibility(View.INVISIBLE);
        frameLayout.addView(barcodeScannerOverlayView);
        scannerText.setVisibility(View.INVISIBLE);
        captureButton.setEnabled(false);
        gallary.setEnabled(false);
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
        if (scanner_options.length == 3) {
            qrCodeLayout.setVisibility(View.VISIBLE);
            cameraLayout.setVisibility(View.INVISIBLE);
            barcodeLayout.setVisibility(View.INVISIBLE);
            centerTextView = findViewById(R.id.center_textview_qrcode);
        } else {
            dynamic_center_text.setText("QR CODE");
            dynamic_right_text.setText("BAR CODE");
            dynamic_left_text.setText("");
        }
        frameLayout.removeView(barcodeScannerOverlayView);
        qrCodeBoundry.setVisibility(View.VISIBLE);
        scannerText.setVisibility(View.VISIBLE);
        captureButton.setEnabled(false);
        gallary.setEnabled(false);
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

    public void cameraToBarCodeTransition() {
        if (scanner_options.length == 2) {
            dynamic_center_text.setText("BAR CODE");
            dynamic_right_text.setText("");
            dynamic_left_text.setText("CAMERA");
        }
        frameLayout.addView(barcodeScannerOverlayView);
        scannerText.setVisibility(View.INVISIBLE);
        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImage(getVisionImageFromFrame(frame));
            }
        });
        captureButton.setEnabled(false);
        gallary.setEnabled(false);
        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_CODABAR, FirebaseVisionBarcode.FORMAT_CODE_128)
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    public void barCodeToCameraTransition() {
        if (scanner_options.length == 2) {
            dynamic_center_text.setText("CAMERA");
            dynamic_right_text.setText("BAR CODE");
            dynamic_left_text.setText("");
        }
        captureButton.setEnabled(true);
        gallary.setEnabled(true);
        frameLayout.removeView(barcodeScannerOverlayView);
    }

    public void pickImageFromGallary() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            Intent intent = new Intent(CameraViewActivity.this, CropActivity.class);
            intent.putExtra("uri", data.getData());
            intent.putExtra("absolutepath", getRealPathFromURI(data.getData()));
            startActivityForResult(intent, 6);
        }
        if (requestCode == 6 && resultCode == RESULT_OK) {
            Intent intent = new Intent();
            Uri uri = (Uri) data.getExtras().get("croppeduri");
            intent.putStringArrayListExtra("suggestionsList", data.getStringArrayListExtra("suggestionsList"));
            intent.putExtra("croppeduri", uri);
            setResult(Activity.RESULT_OK, intent);
            finish();
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
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.captureButton) {
            camera.takePicture();
        } else if (v.getId() == R.id.gallary) {
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
    }
}
