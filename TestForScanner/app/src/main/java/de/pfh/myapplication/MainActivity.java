package de.pfh.myapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

import de.pfh.myapplication.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private static final String FILENAME_FORMAT = "yyyyMMddHHmmssSSS";
    private ActivityMainBinding viewBinding;


    private ImageCapture imageCapture;

    private ExecutorService cameraExecutor;
    private String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private Button scan_button;


    //NEW
    private ImageView capturedImageView;
    private TextView barcodeTextView;


    private final ActivityResultLauncher<String[]> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    new ActivityResultCallback<Map<String, Boolean>>() {
                        @Override
                        public void onActivityResult(Map<String, Boolean> permissions) {
                            boolean permissionGranted = true;
                            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                                if (Arrays.asList(REQUIRED_PERMISSIONS).contains(entry.getKey()) &&
                                        !entry.getValue()) {
                                    permissionGranted = false;
                                    break;
                                }
                            }
                            if (!permissionGranted) {
                                Toast.makeText(MainActivity.this,
                                        "Permission request denied",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                startCamera();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        capturedImageView = findViewById(R.id.capturedImageView);
        barcodeTextView = findViewById(R.id.barcodeTextView);
        scan_button = findViewById(R.id.scan_button);

/*
        //Barcodescanner
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);
*/

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }

        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT, Locale.US);
        String name = sdf.format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Fehler beim Aufnehmen des Fotos: " + exc.getMessage(), exc);
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        String msg = "Fotoaufnahme erfolgreich: " + output.getSavedUri();
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                        //NEW
                        if (capturedImageView != null && output.getSavedUri() != null) {
                            Uri imageUri = output.getSavedUri();
                            capturedImageView.setImageURI(imageUri);
                        }
                    }
                });
        scan_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanBarcode();


            }
        });
    }


    private void scanBarcode() {
        //NEW
        capturedImageView = findViewById(R.id.capturedImageView);
        barcodeTextView = findViewById(R.id.barcodeTextView);
        BitmapDrawable drawable = (BitmapDrawable) capturedImageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .enableAllPotentialBarcodes()
                        .build();

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient();
        barcodeScanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        StringBuilder scannedInfo = new StringBuilder();
                        for (Barcode barcode : barcodes) {
                            String barcodeData = barcode.getRawValue();
                            scannedInfo.append("Barcode erkannt: ").append(barcodeData).append("\n");
                        }
                        barcodeTextView.setText(scannedInfo.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        barcodeTextView.setText("");
                    }
                });
    }





    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 10);
        activityResultLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private File getOutputDirectory() {
        return getFilesDir();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }


    /*
    //Barcodescanner Erster Versuch
    private void scanBarcode(InputImage image) {
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient();

        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {

                        for (Barcode barcode: barcodes) {

                            String rawValue = barcode.getRawValue();

                            Rect bounds = barcode.getBoundingBox();
                            Point[] corners = barcode.getCornerPoints();

                            int valueType = barcode.getValueType();


                            switch (valueType) {
                                case Barcode.TYPE_WIFI:
                                    String ssid = barcode.getWifi().getSsid();
                                    String password = barcode.getWifi().getPassword();
                                    int type = barcode.getWifi().getEncryptionType();
                                    break;
                                case Barcode.TYPE_URL:
                                    String title = barcode.getUrl().getTitle();
                                    String url = barcode.getUrl().getUrl();
                                    break;
                            }
                            String scannedInfo = "Barcode erkannt: " + rawValue;
                            Toast.makeText(MainActivity.this, scannedInfo, Toast.LENGTH_SHORT).show();
                            barcodeTextView.setText(scannedInfo);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });*/
/*
    //Barcodescanner Zweiter Versuch
    @androidx.camera.core.ExperimentalGetImage
    private class YourAnalyzer implements ImageAnalysis.Analyzer {
        BarcodeScanner scanner = BarcodeScanning.getClient();
        @Override
        public void analyze(ImageProxy imageProxy) {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                Task<List<Barcode>> result = scanner.process(image)
                        .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                            @Override
                            public void onSuccess(List<Barcode> barcodes) {

                                for (Barcode barcode: barcodes) {
                                    String rawValue = barcode.getRawValue();

                                    Rect bounds = barcode.getBoundingBox();
                                    Point[] corners = barcode.getCornerPoints();

                                    int valueType = barcode.getValueType();

                                    switch (valueType) {
                                        case Barcode.TYPE_WIFI:
                                            String ssid = barcode.getWifi().getSsid();
                                            String password = barcode.getWifi().getPassword();
                                            int type = barcode.getWifi().getEncryptionType();
                                            break;
                                        case Barcode.TYPE_URL:
                                            String title = barcode.getUrl().getTitle();
                                            String url = barcode.getUrl().getUrl();
                                            break;
                                    }

                                    //NEW
                                    String scannedInfo = "Barcode erkannt: " + rawValue;
                                    Toast.makeText(MainActivity.this, scannedInfo, Toast.LENGTH_SHORT).show();
                                    barcodeTextView.setText(scannedInfo);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
            }

            InputImage image;
            try {
                Context context = getApplicationContext();
                Uri uri = null;
                image = InputImage.fromFilePath(MainActivity.this, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

}

