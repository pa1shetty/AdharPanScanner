package com.example.adharpanscanner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.example.adharpanscanner.OtherFunctionalities.isValidAadharNumber;
import static com.example.adharpanscanner.OtherFunctionalities.isValidPANNumber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA_ADHAR = 3;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA_PAN = 4;
    private static final int REQUEST_CAMERA_ACTIVITY = 10;

    private static int CURRENT_REQUEST;
    File fileTemp = null;
    Button buttonAdhar, buttonPan, btnClear, btnExport;
    CheckBox checkBoxAdhar, checkBoxPan;
    UserData userData;
    Snackbar mSnackBar;
    ConstraintLayout constraintLayout;
    File filePath;
    UserDatabase userDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        filePath = new File(this.getExternalFilesDir(null).getAbsolutePath() + "/user_data.xls");
        Log.d("pavan", "onCreate: " + filePath);
        constraintLayout = findViewById(R.id.cl_main);
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
        disableCheckBoxClick();
        CURRENT_REQUEST = MY_PERMISSIONS_REQUEST_CAMERA_ADHAR;
        createDBObjectRX();
        getLastUserDataRX();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            Log.d("pavan", "onActivityResult: " + data);
            Log.d("pavan3", "onActivityResult: " + requestCode + " " + resultCode);

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert result != null;
                Uri croppedImageURI = result.getUri();
                if (croppedImageURI != null) {
                    if (CURRENT_REQUEST == MY_PERMISSIONS_REQUEST_CAMERA_ADHAR) {
                        getDataFromAdharImage(croppedImageURI);
                    } else if (CURRENT_REQUEST == MY_PERMISSIONS_REQUEST_CAMERA_PAN) {
                        getDataFromPanImage(croppedImageURI);
                    }
                    deleteTempFile();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                setUpSnackBar("e", getString(R.string.no_crop_image));
            } else {
                deleteTempFile();

            }

        } else if (requestCode == REQUEST_CAMERA_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                // int req_type=data.getIntExtra("req_type",1);

                String message = data.getStringExtra("file_path");

                fileTemp = new File(message);
                Uri uri = Uri.fromFile(new File(message));
                CropImage.activity(uri).setAutoZoomEnabled(true).setAllowFlipping(false)
                        .start(this);
            } else {

                setUpSnackBar("e", getString(R.string.try_gain));

            }
        }

    }

    private void deleteTempFile() {
        if (fileTemp != null) {
            fileTemp.delete();
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(@NotNull View v) {
        switch (v.getId()) {
            case R.id.btnAdhar:
                captureCard(MY_PERMISSIONS_REQUEST_CAMERA_ADHAR);
                break;
            case R.id.btnPan:
                captureCard(MY_PERMISSIONS_REQUEST_CAMERA_PAN);
                break;
            case R.id.btnExport:
                getAllUserDataRX();
                break;
            case R.id.btnClear:
                deleteExcelSheet();
                break;
            default:
                break;
        }
    }


    //Take Image of Adhar card or PAN card
    private void captureCard(int requestId) {
        CURRENT_REQUEST = requestId;
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        intent.putExtra("req_type", requestId);
        startActivityForResult(intent, REQUEST_CAMERA_ACTIVITY);
    }

    //Get Text from Adhar Card
    private void getDataFromAdharImage(Uri croppedAdharURI) {
        try {
            InputImage inputImageCroppedAdhar = InputImage.fromFilePath(this, croppedAdharURI);
            TextRecognizer recognizer = TextRecognition.getClient();
            recognizer.process(inputImageCroppedAdhar)
                    .addOnSuccessListener(this::getNameAndAdharNo)
                    .addOnFailureListener(
                            e -> {

                                setUpSnackBar("e", getString(R.string.no_adhar_data));
                            });
        } catch (IOException e) {

            setUpSnackBar("e", getString(R.string.no_adhar_data));
            e.printStackTrace();
        }

    }

    //Get Text from PAN card
    private void getDataFromPanImage(Uri croppedPanURI) {
        try {
            InputImage inputImageCroppedPan = InputImage.fromFilePath(this, croppedPanURI);
            TextRecognizer recognizer = TextRecognition.getClient();
            recognizer.process(inputImageCroppedPan)
                    .addOnSuccessListener(this::getPanNumber)
                    .addOnFailureListener(
                            e -> {
                                setUpSnackBar("e", getString(R.string.no_pan_data));
                            });
        } catch (IOException e) {
            setUpSnackBar("e", getString(R.string.no_pan_data));
            e.printStackTrace();
        }
    }


    //Get Name and Adhar number from Adhar card
    private void getNameAndAdharNo(@NotNull Text result) {
        String resultText = result.getText();
        boolean saveName = false;
        String userName = null;

        if (resultText.contains(getString(R.string.govt)) || resultText.contains(getString(R.string.ind))) {
            for (Text.TextBlock block : result.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    if (saveName && userName == null) {
                        userName= line.getText();
                        userData.userName = line.getText();
                        saveName=false;
                    }
                    if ((line.getText().contains(getString(R.string.govt)) || line.getText().contains(getString(R.string.ind)))) {
                        saveName = true;
                    }
                    if (line.getText().length() == 14) {
                        if (isValidAadharNumber(line.getText())) {
                            userData.userAdharNo = line.getText();
                        }
                    }
                }
            }

            if (userData.userName != null && userData.userAdharNo != null) {
                checkBoxAdhar.setChecked(true);
                getLastUserDataAdharRX(userData.userName, userData.userAdharNo);
                // addDataToModel();

                setUpSnackBar("s", getString(R.string.adhar_success));
            } else {
                setUpSnackBar("e", getString(R.string.no_adhar_data));
            }
        } else {
            setUpSnackBar("e", getString(R.string.no_adhar_data));
        }
    }

    private void addDataToModel() {
        Log.d("pavan", "addDataToModel: " + userData.userName + " " + userData.userAdharNo + " " + userData.userPanNo);
        if (userData.userName != null && userData.userAdharNo != null && userData.userPanNo != null) {
            addDataToDBRX(userData);
            resetUserData();
        }
    }


    //Get PAN number from PAN card
    private void getPanNumber(@NotNull Text visionText) {
        String resultText = visionText.getText();
        if (resultText.contains(getString(R.string.income)) || resultText.contains(getString(R.string.tax))) {
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String lineText = line.getText();
                    if (lineText.length() == 10) {
                        if (isValidPANNumber(lineText)) {
                            userData.userPanNo = lineText;
                            break;
                        }
                    }
                }
            }
            if (userData.userPanNo != null) {
                checkBoxPan.setChecked(true);
                getLastUserDataPanRX(userData.userPanNo);

               // addDataToModel();
                setUpSnackBar("s", getString(R.string.pan_success));

            } else {
                setUpSnackBar("e", getString(R.string.no_pan_number));
            }
        } else {
            setUpSnackBar("e", getString(R.string.no_pan_data));
        }

    }




    // Exporting Data to Excel
    private void excelTransaction(List<UserData> userDataList) {
        if (!filePath.exists()) {
            try {
                filePath.createNewFile();
                createNewSheet(filePath, userDataList);
            } catch (IOException e) {

                setUpSnackBar("e", getString(R.string.no_export_data));
                e.printStackTrace();
            }
        } else {
            try {
                filePath.delete();
                filePath.createNewFile();
                createNewSheet(filePath, userDataList);
            } catch (IOException e) {

                setUpSnackBar("e", getString(R.string.no_export_data));
                e.printStackTrace();
            }
          //  updateSheet(filePath, userDataList);
        }
    }

    //Create new Excel sheet
    private void createNewSheet(File filePath, List<UserData> userDataList) {
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        HSSFSheet hssfSheet = hssfWorkbook.createSheet(getString(R.string.user_data_sheet));
        Row header = hssfSheet.createRow(0);
        header.createCell(0).setCellValue(getString(R.string.user_name));
        header.createCell(1).setCellValue(getString(R.string.adhar_number));
        header.createCell(2).setCellValue(getString(R.string.pan_number));
        for (int userDataCount = 0; userDataCount < userDataList.size(); userDataCount++) {
            int rowNo = userDataCount + 1;
            HSSFRow hssfRow = hssfSheet.createRow(rowNo);
            HSSFCell hssfCell = hssfRow.createCell(0);
            hssfCell.setCellValue(userDataList.get(userDataCount).getUserName());
            hssfCell = hssfRow.createCell(1);
            hssfCell.setCellValue(userDataList.get(userDataCount).getUserAdharNo());
            hssfCell = hssfRow.createCell(2);
            hssfCell.setCellValue(userDataList.get(userDataCount).getUserPanNo());
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            hssfWorkbook.write(fileOutputStream);

            fileOutputStream.flush();
            fileOutputStream.close();
            resetUserData();
            setUpSnackBar("s", getString(R.string.export_success));
        } catch (Exception e) {
            setUpSnackBar("e", getString(R.string.no_export_data));
            e.printStackTrace();
        }

    }

    //Update existing  Excel sheet
    private void updateSheet(File filePath, List<UserData> userDataList) {
        HSSFWorkbook hssfWorkbook;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            hssfWorkbook = new HSSFWorkbook();
            HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(0);
            for (int userDataCount = 0; userDataCount < userDataList.size(); userDataCount++) {
                HSSFRow hssfRow = hssfSheet.createRow(hssfSheet.getPhysicalNumberOfRows() + userDataCount);
                HSSFCell hssfCell = hssfRow.createCell(0);
                hssfCell.setCellValue(userDataList.get(userDataCount).getUserName());
                hssfCell = hssfRow.createCell(1);
                hssfCell.setCellValue(userDataList.get(userDataCount).getUserAdharNo());
                hssfCell = hssfRow.createCell(2);
                hssfCell.setCellValue(userDataList.get(userDataCount).getUserPanNo());
            }
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            hssfWorkbook.write(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            resetUserData();
            setUpSnackBar("s", getString(R.string.export_success));
        } catch (IOException e) {
            setUpSnackBar("e", getString(R.string.no_export_data));
            e.printStackTrace();
        }

    }


    //Delete Excel Sheet
    private void deleteExcelSheet() {
        if (filePath.exists()) {
            filePath.delete();
            setUpSnackBar("s", getString(R.string.data_cleared));
        } else {
            setUpSnackBar("c", getString(R.string.no_user_data_present));
        }
        deleteAllUsersRX();
    }

    //Disable checkbox click
    private void disableCheckBoxClick() {
        checkBoxAdhar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("pavan14", "disableCheckBoxClick: "+userData.userName+" "+userData.userAdharNo+" "+userData.userPanNo);

            if(userData.userName != null && userData.userAdharNo != null && userData.userPanNo != null){
                checkBoxAdhar.setChecked(false);
                Log.d("pavan14", "disableCheckBoxClick: Don't check Adhar");
            }
            else if(userData.userName == null && userData.userAdharNo == null && userData.userPanNo== null)  {
                checkBoxAdhar.setChecked(false);
                Log.d("pavan14", "disableCheckBoxClick: Don't check Adhar");

            }
            else if(userData.userName != null ){
                checkBoxAdhar.setChecked(true);
                Log.d("pavan14", "disableCheckBoxClick:  check Adhar");

            }
            else {
                checkBoxAdhar.setChecked(false);

            }
           // checkBoxAdhar.setChecked(userData.userName != null && userData.userAdharNo != null && userData.userPanNo == null);
        });
        checkBoxPan.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            Log.d("pavan14", "disableCheckBoxClick: "+userData.userName+" "+userData.userAdharNo+" "+userData.userPanNo);
            if(userData.userName != null && userData.userAdharNo != null && userData.userPanNo != null){
                checkBoxPan.setChecked(false);
                Log.d("pavan14", "disableCheckBoxClick: Don't check pan");

            }
            else if(userData.userName == null && userData.userAdharNo == null && userData.userPanNo== null)  {
                checkBoxPan.setChecked(false);
                Log.d("pavan14", "disableCheckBoxClick: Don't check pan");

            }
            else if(userData.userPanNo != null ){
                checkBoxPan.setChecked(true);
                Log.d("pavan14", "disableCheckBoxClick:  check pan");

            }
            else {
                checkBoxPan.setChecked(false);

            }
            //checkBoxPan.setChecked(userData.userPanNo != null && userData.userName == null && userData.userAdharNo == null);
        });
    }


    private void setUpSnackBar(@NotNull String status, String message) {
        switch (status) {
            case "s":
                mSnackBar = Snackbar
                        .make(constraintLayout, message, Snackbar.LENGTH_LONG);
                mSnackBar.setBackgroundTint(ContextCompat.getColor(this, R.color.green));
                mSnackBar.show();
                break;
            case "e":
                mSnackBar = Snackbar
                        .make(constraintLayout, message, Snackbar.LENGTH_LONG);
                mSnackBar.setBackgroundTint(ContextCompat.getColor(this, R.color.red));
                mSnackBar.show();
                break;
            default:
                mSnackBar = Snackbar
                        .make(constraintLayout, message, Snackbar.LENGTH_LONG);
                mSnackBar.show();
                break;
        }
    }


    //Clear user model
    private void resetUserData() {
        if(userData.userAdharNo!=null&&userData.userPanNo!=null&&userData.userName!=null){
            userData=new UserData();
        }
        checkBoxAdhar.setChecked(false);
        checkBoxPan.setChecked(false);

    }


    //Database  operation using RXJava
    private void createDBObjectRX() {
        Observable.fromCallable(this::createDBObject).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UserDatabase>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull UserDatabase db) {
                        userDatabase = db;
                        Log.d("pavan ", "onNext:db created ");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void addDataToDBRX(UserData userData) {
        Observable.fromCallable(() -> addDataToDB(userData)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Boolean bool) {
                        if (bool) {
                            setUpSnackBar("s", getString(R.string.user_data_retrived));
                        } else {
                            setUpSnackBar("e", "Please try again.");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void getAllUserDataRX() {
        Observable.fromCallable(this::getAllUserData).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<UserData>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<UserData> userDataList) {
                        Log.d("pavan5", "onNext: size " + userDataList.size());
                        if (userDataList.size() == 0) {
                            setUpSnackBar("c", getString(R.string.no_data_to_export));
                        } else {
                            excelTransaction(userDataList);
                            //deleteAllUsersRX();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void deleteAllUsersRX() {
        Observable.fromCallable(this::deleteAllUserData).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Boolean bool) {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }


    private void getLastUserDataAdharRX(String userName, String userAdharNo) {
        Log.d("pavan5", "getLastUserDataRX: ");
        Observable.fromCallable(() -> getLastUserDataAdhar(userName, userAdharNo)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UserData>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(UserData userData) {
                        Log.d("pavan10", "onNext: Adhar data updated");
                        resetUserData();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("pavan5", "onError: " + e);

                    }

                    @Override
                    public void onComplete() {
                        Log.d("pavan5", "onComplete: ");

                    }
                });
    }

    private void getLastUserDataPanRX(String userPanNo) {
        Observable.fromCallable(() -> getLastUserDataPan(userPanNo)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean bool) {
                        Log.d("pavan10", "onNext:Pan data updated");
                        resetUserData();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("pavan5", "onError: " + e);

                    }

                    @Override
                    public void onComplete() {
                        Log.d("pavan5", "onComplete: ");

                    }
                });
    }

    private void getLastUserDataRX() {
        Observable.fromCallable(() -> getLastUserData()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UserData>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(UserData lastUserData) {
                        Log.d("pavan11", "onNext: Adhar data updated"+lastUserData.userName);
                        userData=lastUserData;
                        checkBoxAdhar.setChecked(true);
                        checkBoxPan.setChecked(true);
                        }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("pavan11", "onError: " + e);

                    }

                    @Override
                    public void onComplete() {
                        Log.d("pavan5", "onComplete: ");

                    }
                });
    }



    private void insertAdharDataRX(String userName, String userAdharNo) {
        Log.d("pavan5", "insertAdharDataRX: ");
        Observable<Boolean> booleanObservable;
        booleanObservable = Observable.fromCallable(() -> insertAdharData(userName, userAdharNo));
        booleanObservable.subscribeOn(Schedulers.io());
        booleanObservable.observeOn(AndroidSchedulers.mainThread());
        booleanObservable.subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(@NonNull Boolean bool) {
                Log.d("pavan5", "onNext: " + bool);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d("pavan5", "onError: " + e);
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private boolean insertAdharData(String userName, String userAdharNo) {
        if (userDatabase != null) {
            UserDao userDao = userDatabase.userDao();
            userDao.insertAdharData(userName, userAdharNo);
            return true;
        } else {
            createDBObjectRX();
            return false;
        }
    }


    @NotNull
    private UserDatabase createDBObject() {
        return Room.databaseBuilder(getApplicationContext(),
                UserDatabase.class, "user_data").build();
    }


    private boolean addDataToDB(UserData userData) {
        if (userDatabase != null) {
            UserDao userDao = userDatabase.userDao();
            userDao.insertAll(userData);
            return true;
        } else {
            createDBObjectRX();
            return false;
        }

    }

    private UserData getLastUserData() {
        if (userDatabase == null) {
            userDatabase= Room.databaseBuilder(getApplicationContext(),
                    UserDatabase.class, "user_data").build();
        }
            UserDao userDao = userDatabase.userDao();
            Log.d("pavan11", "getLastUserData: " + userDao.getCount());

            UserData userData = userDao.getRecentUser();
            if (userData != null) {
                return userData;
            } else {
                userData = new UserData();
            }
            return userData;

    }


    private UserData getLastUserDataAdhar(String userName, String userAdharNo) {
        if (userDatabase != null) {

            UserDao userDao = userDatabase.userDao();
            if (userDao.getCount() == 0) {
                Log.d("pavan5", "getLastUserDataAdhar:table has no data " + userDao.getCount());

                userDao.insertAdharData(userName,userAdharNo);
            } else {
                UserData userDataTemp = userDao.getRecentUser();
                if (userDataTemp.userPanNo == null) {
                    Log.d("pavan5", "getLastUserDataAdhar: user doesnt have pan data " + userData.uid + " " + userData.userName + " " + userData.userPanNo);
                    userDao.updateUseAdhar(userDataTemp.uid, userName, userAdharNo);
                }else if(userDataTemp.userAdharNo==null){
                    Log.d("pavan5", "getLastUserDataAdhar: user doesnt have adhar data");
                    userDao.updateUseAdhar(userDataTemp.uid, userName, userAdharNo);

                }

                else {
                    Log.d("pavan5", "getLastUserDataAdhar: user has both data "+userDataTemp.userPanNo);
                    userDao.insertAdharData(userName,userAdharNo);
                }
            }

        } else {
            createDBObjectRX();
        }
        return userData;
    }

    private boolean getLastUserDataPan(String userPan) {
        if (userDatabase != null) {
            UserDao userDao = userDatabase.userDao();
            if (userDao.getCount() == 0) {
                Log.d("pavan5", "getLastUserDataPan: Table has zero data " + userDao.getCount());
                userDao.insertPanData(userPan);
            } else {
                UserData userDataTemp = userDao.getRecentUser();
                if (userDataTemp.userName == null) {
                    Log.d("pavan5", "getLastUserDataPan: Table doesnot have adhar info ");
                    userDao.updateUsePan(userDataTemp.uid, userPan);
                } else if(userDataTemp.userPanNo==null) {
                    Log.d("pavan5", "getLastUserDataPan: Table doesnot  has pan info "+userDataTemp.uid+" "+userPan);
                    userDao.updateUsePan(userDataTemp.uid, userPan);
                }
                else {
                    Log.d("pavan5", "getLastUserDataPan: Table  has both info "+userDataTemp.userName+" "+userDataTemp.userPanNo);
                    userDao.insertPanData(userPan);
                }
            }

        } else {
            createDBObjectRX();
        }
        return true;
    }

    private List<UserData> getAllUserData() {
        if (userDatabase != null) {
            UserDao userDao = userDatabase.userDao();
            Log.d("pavan5", "getAllUserData:count "+userDao.getCount());
            return userDao.getAll();
        } else {
            createDBObjectRX();
            return null;
        }

    }

    private boolean deleteAllUserData() {
        if (userDatabase != null) {
            UserDao userDao = userDatabase.userDao();
            userDao.delete();
            return true;
        } else {
            createDBObjectRX();
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("pavan2", "onDestroy: ");
        deleteTempFile();
        super.onDestroy();
    }

    //To handle orientation change.
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (userData != null) {
            if (userData.userName != null) {
                savedInstanceState.putString("userName", userData.userName);
            }
            if (userData.userAdharNo != null) {
                savedInstanceState.putString("userAdharNo", userData.userAdharNo);
            }
            if (userData.getUserPanNo() != null) {
                savedInstanceState.putString("userPan", userData.userPanNo);
            }
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    //onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userData = new UserData();
        userData.userName = savedInstanceState.getString("userName");
        userData.userAdharNo = savedInstanceState.getString("userAdharNo");
        userData.userPanNo = savedInstanceState.getString("userPanNo");
       // updateCheckBox();
    }

    private void updateCheckBox() {
        Log.d("pavan", "updateCheckBox: ");
        if (userData.userName != null && userData.userAdharNo != null && userData.userPanNo == null) {
            checkBoxAdhar.setChecked(true);
        }

    }
}