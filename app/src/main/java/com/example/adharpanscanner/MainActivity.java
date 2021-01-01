package com.example.adharpanscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.logging.XMLFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.poi.ss.usermodel.WorkbookFactory.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button buttonAdhar, buttonPan, btnClear, btnExport;
    CheckBox checkBoxAdhar, checkBoxPan;
    UserData userData;
    private int WRITE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
        buttonAdhar = findViewById(R.id.btnAdhar);
        buttonPan = findViewById(R.id.btnPan);
        btnClear = findViewById(R.id.btnClear);
        btnExport = findViewById(R.id.btnExport);
        checkBoxAdhar = findViewById(R.id.cbAdhar);
        checkBoxPan = findViewById(R.id.cbPan);
        buttonAdhar.setOnClickListener(this);
        buttonPan.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnExport.setOnClickListener(this);
        checkBoxAdhar.setOnClickListener(this);
        checkBoxPan.setOnClickListener(this);
        userData = new UserData();

    }


    private void getNameAndAdharNo(@NotNull Text result) {
        String resultText = result.getText();
        boolean saveName = false;
        String userAdharNo = null;
        if (resultText.contains(getString(R.string.govt_of_india))) {
            for (Text.TextBlock block : result.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    if (saveName && userData.userName == null) {
                        userData.userName = line.getText();
                    }
                    if (line.getText().contains(getString(R.string.govt_of_india))) {
                        saveName = true;
                    }
                    if (line.getText().length() == 14) {
                        if (isValidAadharNumber(line.getText())) {
                            userAdharNo = line.getText();
                            userData.userAdharNo = userAdharNo;
                        }
                    }
                }
            }
            if (userData.userName != null && userAdharNo != null) {
                Log.d("pavan", "user name is : " + userData.userName + " " + userAdharNo);
                checkBoxAdhar.setChecked(true);
            } else {
                //Log.d("pavan", "could'nt find  Name and Adhar Id ");
            }
        } else {
            //Log.d("pavan", "onSuccess:Not Adhar Card\n ");
        }
    }

    private boolean isValidAadharNumber(String str) {
        String regex = "^[2-9]{1}[0-9]{3}\\s[0-9]{4}\\s[0-9]{4}$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    int adharCount = 1, panCount = 1;

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(@NotNull View v) {
        switch (v.getId()) {
            case R.id.btnAdhar:
                Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.a1);
                Toast.makeText(this, "Adhar Button Clicked " + adharCount, Toast.LENGTH_SHORT).show();
                switch (adharCount) {
                    case 1:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.a1);
                        adharCount++;
                        break;
                    case 2:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.a2);
                        adharCount++;
                        break;
                    case 3:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.a3);
                        adharCount++;
                        break;
                    case 4:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.a4);
                        adharCount = 1;
                        break;
                }
                InputImage image = null;
                try {
                    image = InputImage.fromFilePath(this, uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                TextRecognizer recognizer = TextRecognition.getClient();
                recognizer.process(image)
                        .addOnSuccessListener(this::getNameAndAdharNo)
                        .addOnFailureListener(
                                e -> {

                                });

                break;
            case R.id.btnPan:
                uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.a1);
                Toast.makeText(this, "Adhar Button Clicked " + adharCount, Toast.LENGTH_SHORT).show();
                switch (panCount) {
                    case 1:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.p1);
                        panCount++;
                        break;
                    case 2:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.p2);
                        panCount++;
                        break;
                    case 3:
                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.p3);
                        panCount = 1;
                        break;

                }
                image = null;
                try {
                    image = InputImage.fromFilePath(this, uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recognizer = TextRecognition.getClient();
                recognizer.process(image)
                        .addOnSuccessListener(this::getDOBFromPan)
                        .addOnFailureListener(
                                e -> {

                                });
                break;
            case R.id.btnExport:
                if (userData.userName == null || userData.userAdharNo == null) {
                    Toast.makeText(this, "Adhar Card data not added", Toast.LENGTH_SHORT).show();

                } else if (userData.userDOB == null) {
                    Toast.makeText(this, "PAN Card data not added", Toast.LENGTH_SHORT).show();
                } else {
                    //Log.d("pavan", "onClick: "+userData.userName+" "+userData.userAdharNo+" "+userData.userDOB);
                    saveUserData();
                }

                break;
            case R.id.btnClear:
                Toast.makeText(this, "Clear Button Clicked", Toast.LENGTH_SHORT).show();

                break;
            case R.id.cbAdhar:
                checkBoxAdhar.setChecked(checkBoxAdhar.isChecked());
                Toast.makeText(this, "Adhar Checkbox Clicked", Toast.LENGTH_SHORT).show();

                break;
            case R.id.cbPan:
                Toast.makeText(this, "Pan Checkbox Clicked", Toast.LENGTH_SHORT).show();
                checkBoxPan.setChecked(checkBoxPan.isChecked());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    private void saveUserData() {
        if(checkStoragePermission()){
            Log.d("pavan", "saveUserData: Permission Present");
            excelTransaction();
        }
    }

    private boolean checkStoragePermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);

            return false;
        } else {
            return true;
        }
    }

    private void getDOBFromPan(@NotNull Text visionText) {
        String resultText = visionText.getText();
        if (resultText.contains(getString(R.string.ITI))) {
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String lineText = line.getText();
                    if (lineText.length() == 10) {
                        if (isValidDOB(lineText)) {
                            userData.userDOB = lineText;
                            break;
                        }
                    }
                }
            }
            if (userData.userDOB != null) {
                Log.d("pavan", "dob: " + userData.userDOB);
                checkBoxPan.setChecked(true);
            } else {
                //Log.d("pavan", "dob: No DOB");
            }
        } else {
            //Log.d("pavan", "not Adhar card: ");
        }


    }

    private boolean isValidDOB(@NotNull String input) {
        return input.matches("([0-9]{2})/([0-9]{2})/([0-9]{4})");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_PERMISSION) {
            if (grantResults.length > 0) {
                boolean writePermissionGiven = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (writePermissionGiven) {
                    Log.d("pavan", "onRequestPermissionsResult: Permission Given");
                    excelTransaction();
                } else {
                    Log.d("pavan", "onRequestPermissionsResult: Permission Not Given");

                }
            }
        }
    }

    private void excelTransaction(){
        int rowCount=0;
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        HSSFSheet hssfSheet = hssfWorkbook.createSheet("Custom Sheet");
        File filePath = new File(Environment.getExternalStorageDirectory() + "/Demo.xls");
        if (!filePath.exists()){
            try {
                filePath.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            rowCount=readExcel();
        }

        Log.d("pavan", "excelTransaction: "+rowCount);
        for(Row row: hssfSheet)     //iteration over row using for each loop
        {
            for (Cell cell : row)    //iteration over cell using for each loop
            Log.d("pavan", "excelTransaction: "+cell.getStringCellValue());
        }
        HSSFRow hssfRow = hssfSheet.createRow(rowCount);

        HSSFCell hssfCell = hssfRow.createCell(0);
        hssfCell.setCellValue(userData.userName);
         hssfCell = hssfRow.createCell(1);
        hssfCell.setCellValue(userData.userDOB);
         hssfCell = hssfRow.createCell(2);
        hssfCell.setCellValue(userData.userAdharNo);




        try {
            FileOutputStream fileOutputStream= new FileOutputStream(filePath);
            hssfWorkbook.write(fileOutputStream);

            if (fileOutputStream!=null){
                fileOutputStream.flush();
                fileOutputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int readExcel() {
        HSSFWorkbook wb = null;
        try {
            FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory() + "/Demo.xls"));
            wb = new HSSFWorkbook(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HSSFSheet sheet = wb.getSheetAt(0);         
        return sheet.getPhysicalNumberOfRows();

        
    }
}
