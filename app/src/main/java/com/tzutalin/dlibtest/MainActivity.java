/*
 *  Copyright (C) 2015-present TzuTaLin
 */

package com.tzutalin.dlibtest;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.lang.String;

import hugo.weaving.DebugLog;
import timber.log.Timber;



public class MainActivity extends AppCompatActivity {
    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    static final int REQUEST_IMAGE_CAPTURE = 3;

    private static final String TAG = "MainActivity";

    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    // UI
    private ProgressDialog mDialog;
    private MaterialListView mListView;
    private FloatingActionButton mFabActionBt;
    private FloatingActionButton mFabCamActionBt;
    private Toolbar mToolbar;

    private Uri mUriPhotoTaken;
    private Bitmap imageBitmap;
    private String mTestImgPath;
    private FaceDet mFaceDet;
    private List<Card> mCard = new ArrayList<>();
    String imageFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        setSupportActionBar(mToolbar);
        // Just use hugo to print log
        isExternalStorageWritable();
        isExternalStorageReadable();

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }

        setupUI();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mTestImgPath = image.getAbsolutePath();
        return image;
    }

    private void Capture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d("photoFile",photoFile+"");
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("Captureabc","error");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.tzutalin.dlibtest.fileprovider",
                        photoFile);
                mUriPhotoTaken = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void saveImage(Bitmap finalBitmap){
        String imageFileForGalName = imageFileName;
        Log.d("sava gallery", imageFileForGalName+"");
        File myDir = new File(Environment.getExternalStorageDirectory().toString());
        //File myDir = new File(Environment.getExternalStorageDirectory()+"/Pictures", "AcneAnalyzer");
        myDir.mkdirs();
        File file = new File(myDir, imageFileForGalName);
        try{
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            mTestImgPath = file.getAbsolutePath();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected void setupUI() {
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        mFabActionBt = (FloatingActionButton) findViewById(R.id.fab);
        mFabCamActionBt = (FloatingActionButton) findViewById(R.id.fab_cam);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mFabActionBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // launch Gallery
                Toast.makeText(MainActivity.this, "Pick one image", Toast.LENGTH_SHORT).show();
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
            }
        });

        mFabCamActionBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, CameraActivity.class));
                Toast.makeText(MainActivity.this, "Take a picture", Toast.LENGTH_SHORT).show();

                Capture();
            }
        });

        mToolbar.setTitle(getString(R.string.app_name));
        Toast.makeText(MainActivity.this, getString(R.string.description_info), Toast.LENGTH_LONG).show();
    }

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    @DebugLog
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

    /* Checks if external storage is available for read and write */
    @DebugLog
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    @DebugLog
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @DebugLog
    protected void demoStaticImage() {
        if (mTestImgPath != null) {
            Timber.tag(TAG).d("demoStaticImage() launch a task to det");
            runDemosAsync(mTestImgPath);
        } else {
            Timber.tag(TAG).d("demoStaticImage() mTestImgPath is null, go to gallery");
            Toast.makeText(MainActivity.this, "Pick an image to run algorithms", Toast.LENGTH_SHORT).show();
            // Create intent to Open Image applications like Gallery, Google Photos
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            Toast.makeText(MainActivity.this, "Demo using static images", Toast.LENGTH_SHORT).show();
            demoStaticImage();
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Log.d("Rotate", angle+"");
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    public int exifOrientationToDegrees(int exifOrientation){
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90){
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180){
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270){
            return 270;
        }
        else{
            return 0;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {

            // When an Image is picked
            if ((requestCode == RESULT_LOAD_IMG || requestCode ==  REQUEST_IMAGE_CAPTURE) && resultCode == RESULT_OK ) {
                Uri selectedImage;
                // Get the Image from data
                if (data == null || data.getData() == null) {
                    File file = new File(mTestImgPath);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    imageBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    Log.d("Bitmap Decode", "OK");

                    if (imageBitmap != null) {
                        ExifInterface ei = new ExifInterface(mTestImgPath);
                        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        int exifDegree = exifOrientationToDegrees(orientation);

                        Bitmap rotatedBitmap = rotateImage(imageBitmap, exifDegree);
                        Log.d("Camera Image URI", mUriPhotoTaken+"");

                        saveImage(rotatedBitmap);   // 갤러리에 저장하는 함수
                    }

                    //File cropFile = createImageFile();  // 새 크롭 이미지 (덮어쓰기X)
                } else {
                    selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    mTestImgPath = cursor.getString(columnIndex);
                    cursor.close();
                }

                if (mTestImgPath != null) {
                    runDemosAsync(mTestImgPath);
                    Toast.makeText(this, "Img Path:" + mTestImgPath, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "You haven't picked Image picture", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }

    // ==========================================================
    // Tasks inner class
    // ==========================================================

    @NonNull
    private void runDemosAsync(@NonNull final String imgPath) {
        demoFaceDet(imgPath);
    }


    private void demoFaceDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.d("DemoFace","Detecting faces");
                showDiaglog("Detecting faces");
            }

            @Override
            protected void onPostExecute(List<VisionDetRet> faceList) {
                super.onPostExecute(faceList);
                if (faceList.size() > 0) {
                    Card card = new Card.Builder(MainActivity.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, faceList, Color.BLUE))
                            .setTitle("Face det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_LONG).show();
                }
                updateCardListView();
                dismissDialog();
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mFaceDet == null) {
                    mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
                }

                final String targetPath = Constants.getFaceShapeModelPath();
                if (!new File(targetPath).exists()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                        }
                    });
                    FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
                }
                List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
                return faceList;
            }
        }.execute();
    }

    private void updateCardListView() {
        mListView.clearAll();
        for (Card each : mCard) {
            mListView.add(each);
        }
    }

    private void showDiaglog(String title) {
        dismissDialog();
        mDialog = ProgressDialog.show(MainActivity.this, title, "process..", true);
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @DebugLog
    private BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) {
        int[] arr_x = new int[68] ;
        int[] arr_y = new int[68];
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Timber.tag(TAG).d("Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            Timber.tag(TAG).d("resizeRatio " + resizeRatio);
        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        // Loop result list
        for (VisionDetRet ret : results) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * resizeRatio);
            bounds.top = (int) (ret.getTop() * resizeRatio);
            bounds.right = (int) (ret.getRight() * resizeRatio);
            bounds.bottom = (int) (ret.getBottom() * resizeRatio);
            //canvas.drawRect(bounds, paint);
            // Get landmark
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            int i = 0;
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                arr_x[i] = pointX;
                arr_y[i] = pointY;
                Log.d("faceabc",i++ + "<"+ pointX + " , " + pointY+">");
                canvas.drawCircle(pointX, pointY, 1, paint);
            }
        }
        paint.setColor(Color.RED);
        canvas.drawLine(arr_x[27],arr_y[27],arr_x[8],arr_y[8],paint);
        canvas.drawLine(arr_x[19],arr_y[19],arr_x[24],arr_y[24],paint);
        canvas.drawLine(arr_x[36],arr_y[36],arr_x[45],arr_y[45],paint);
        canvas.drawLine(arr_x[48],arr_y[48],arr_x[54],arr_y[54],paint);
        return new BitmapDrawable(getResources(), bm);
    }

    @DebugLog
    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }
}