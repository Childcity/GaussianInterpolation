package com.childcity.gaussianinterpolation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

class FileController {
    private static final String TAG = "FileController";

    private static final int REQUEST_READWRITE_STORAGE = 0;

    private static final int SAVE_FILE_REQUEST_CODE = 43; // Unique request code.
    private static final int LOAD_FILE_REQUEST_CODE = 42; // Unique request code.


    static boolean CheckPermissions(final Activity activity)
    {
        int permissionCheckWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheckRead = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheckWrite == PackageManager.PERMISSION_GRANTED
                && permissionCheckRead == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Необходимы дополнительные права")
                    .setMessage(Html.fromHtml("Для работы данной функции необходимо разрешить права на <b>чтение</b> и <b>запись</b> во внутренню память устройства."))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            RequestPermissions(activity);
                        }
                    });
            builder.create().show();
        } else {
            RequestPermissions(activity);
        }

        return false;
    }

    static void SaveAsCSV(final Activity activity)
    {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, String.format(Locale.US,"input_params_%.4s.csv", Math.abs(new Random(new Date().getTime()).nextInt())));
        activity.startActivityForResult(intent, SAVE_FILE_REQUEST_CODE);
    }

    static void LoadAsCSV(final Activity activity)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        activity.startActivityForResult(intent, LOAD_FILE_REQUEST_CODE);
    }

    static void processResult(final Activity activity, final InterpolationViewModel interpolationViewModel,
                              int requestCode, int resultCode, Intent data)
    {
        if(resultCode != Activity.RESULT_OK){
            return;
        }

        try {
            Uri uri = data.getData();
            if (requestCode == SAVE_FILE_REQUEST_CODE) {
                try (OutputStream output = activity.getContentResolver().openOutputStream(uri, "wt")) {

                    if(output == null){
                        Toast.makeText(activity, "Can't open file!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String alpha = "Alpha;" + interpolationViewModel.Alpha;

                    StringBuilder sb = new StringBuilder();
                    sb.append("\r\n\r\nNumber;     X;     Y\r\n");
                    for (int i = 0; i < interpolationViewModel.getInputPointCount(); i++) {
                        PointF point = interpolationViewModel.getInputPointAt(i);
                        sb.append(i+1).append(";").append(point.x).append(";").append(point.y).append("\r\n");
                    }

                    String csvFile = alpha + sb.toString().replaceAll("\\.", ",");

                    output.write(csvFile.getBytes());
                    output.flush();
                }
            }else if(requestCode == LOAD_FILE_REQUEST_CODE){
                try (InputStream inputStream = activity.getContentResolver().openInputStream(uri)) {

                    if(inputStream == null){
                        Toast.makeText(activity, "Can't open file!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // reading all lines from file
                    String[] lines = readAll(inputStream).split("\r\n");

                    // if in first line exist ALPHA -> set Alpha
                    if(lines.length > 0 && lines[0].contains("Alpha;")){
                        String[] alpha = lines[0].split(";");
                        if(alpha.length >= 2){
                            interpolationViewModel.Alpha = alpha[1];
                        }
                    }

                    if(lines.length > 3){
                        StringBuilder newInputPoints = new StringBuilder();
                        for (int i = 3; i < lines.length; i++) {
                            String[] inputPointLine = lines[i].replaceAll(",", "\\.").split(";");
                            if(inputPointLine.length >= 3){
                                try {
                                    float x = Float.parseFloat(inputPointLine[1]);
                                    float y = Float.parseFloat(inputPointLine[2]);
                                    newInputPoints.append(x).append(":").append(y).append(";");
                                } catch (NumberFormatException e) {
                                    Toast.makeText(activity, "Number format error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        interpolationViewModel.setInputPoints(newInputPoints.toString());
                    }
                }catch (Exception e){
                    Toast.makeText(activity, "Can't parse file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void RequestPermissions(final Activity activity)
    {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                , REQUEST_READWRITE_STORAGE);
    }

    private static String readAll(InputStream inputStream) throws IOException
    {
        try(ByteArrayOutputStream result = new ByteArrayOutputStream()){
            byte[] buffer = new byte[1024];

            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString("UTF-8");
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_READWRITE_STORAGE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
}
