package eyes.blue;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.util.ArrayList;
import com.codekidlabs.storagechooser.*;
import com.google.firebase.analytics.FirebaseAnalytics;


//import android.support.v4.provider.DocumentFile;

public class StorageManageActivity extends AppCompatActivity {
    private static final int STORAGE_ACCESS_PERMISSION_REQUEST = 1;
    private static final int OPEN_DOCUMENT_TREE_ABV_23 = 1;
    private static final int OPEN_DOCUMENT_TREE_UND_23 = 2;
    FileSysManager fsm = null;
    TextView extSpeechPathInfo, intSpeechPathInfo, intFree, extFree, extAppUseage, intAppUseage, labelChoicePath;
    Button btnMoveAllToExt, btnMoveAllToInt, btnMoveToUserSpy, btnDelExtFiles, btnDelIntFiles, btnOk;
    ImageButton btnChoicePath;
    RadioGroup radioMgnType = null;
    EditText filePathInput;
    boolean isUseThirdDir = false;

    //	private PowerManager.WakeLock wakeLock = null;
    SharedPreferences runtime = null;

    long intFreeB, extFreeB, intTotal, extTotal, intUsed, extUsed;
    String userSpecDir, logTag=getClass().getName();
    Runnable PermissionGrantedJob = null;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_manage);
        Crashlytics.log(Log.DEBUG,getClass().getName(), "Into onCreate");
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

//		PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
//		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());

        runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
        fsm = new FileSysManager(this);
        //if(!wakeLock.isHeld()){wakeLock.acquire();}

        intFree = (TextView) findViewById(R.id.intFree);
        extFree = (TextView) findViewById(R.id.extFree);
        extAppUseage = (TextView) findViewById(R.id.extAppUseage);
        intAppUseage = (TextView) findViewById(R.id.intAppUseage);
        labelChoicePath = (TextView) findViewById(R.id.labelChoicePath);
        btnMoveAllToExt = (Button) findViewById(R.id.btnMoveAllToExt);
        btnMoveAllToInt = (Button) findViewById(R.id.btnMoveAllToInt);
        btnDelExtFiles = (Button) findViewById(R.id.btnDelExtFiles);
        btnDelIntFiles = (Button) findViewById(R.id.btnDelIntFiles);
        btnChoicePath = (ImageButton) findViewById(R.id.btnChoicePath);
        btnMoveToUserSpy = (Button) findViewById(R.id.moveToUserSpyDirBtn);
        btnOk = (Button) findViewById(R.id.btnOk);
        radioMgnType = (RadioGroup) findViewById(R.id.radioMgnType);
        filePathInput = (EditText) findViewById(R.id.fieldPathInput);
        extSpeechPathInfo = (TextView) findViewById(R.id.extSpeechPathInfo);
        intSpeechPathInfo = (TextView) findViewById(R.id.intSpeechPathInfo);

        // The ImageButton can't disable from xml.
        btnChoicePath.setClickable(false);
        btnChoicePath.setEnabled(false);
        btnMoveToUserSpy.setEnabled(false);

        isUseThirdDir = runtime.getBoolean(getString(R.string.isUseThirdDir), false);
        if (isUseThirdDir) {
            radioMgnType.check(R.id.radioUserMgnStorage);
            filePathInput.setEnabled(true);
            btnChoicePath.setClickable(true);
            btnChoicePath.setEnabled(true);
            labelChoicePath.setEnabled(true);
            btnMoveToUserSpy.setEnabled(true);
        }

        String thirdDir = runtime.getString(getString(R.string.userSpecifySpeechDir), null);
        if (thirdDir == null || thirdDir.length() == 0) thirdDir = fsm.getSysDefMediaDir();
        filePathInput.setText(thirdDir, null);
        filePathInput.setOnKeyListener(new View.OnKeyListener() { // 禁止讓 Enter 輸入多行內容。
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    //if the enter key was pressed, then hide the keyboard and do whatever needs doing.
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(filePathInput.getApplicationWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        btnMoveAllToExt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = getConfirmDialog();
                builder.setTitle(getString(R.string.dlgMoveFile));
                builder.setMessage(getString(R.string.dlgSureMoveFile));
                builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        fsm.moveAllFilesTo(FileSysManager.INTERNAL, FileSysManager.EXTERNAL, new CopyListener() {
                            @Override
                            public void copyFinish() {
                                refreshUsage();
                            }
                        });
                    }
                });
                builder.create().show();
            }
        });
        btnMoveAllToInt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = getConfirmDialog();
                builder.setTitle(getString(R.string.dlgMoveFile));
                builder.setMessage(getString(R.string.dlgSureMoveFile));
                builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fsm.moveAllFilesTo(FileSysManager.EXTERNAL, FileSysManager.INTERNAL, new CopyListener() {
                            @Override
                            public void copyFinish() {
                                refreshUsage();
                            }

                            @Override
                            public void copyFail(final File from, final File to) {
                                Util.showErrorToast(StorageManageActivity.this, String.format(getString(R.string.errMoveFile),from.getAbsolutePath(), to.getAbsolutePath()));
                            }
                        });
                    }
                });
                builder.create().show();
            }
        });

        btnDelExtFiles.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = getConfirmDialog();
                builder.setTitle(getString(R.string.dlgDelFile));
                builder.setMessage(String.format(getString(R.string.dlgDelWarnMsg), getString(R.string.file)));
                builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog pd = new ProgressDialog(StorageManageActivity.this);
                        pd.setTitle(getString(R.string.dlgDelFile));
                        pd.setMessage(getString(R.string.dlgDeling));
                        pd.show();

                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                fsm.deleteAllSpeechFiles(FileSysManager.EXTERNAL);
                                fsm.deleteAllSubtitleFiles(FileSysManager.EXTERNAL);
                                if (pd.isShowing()) pd.dismiss();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshUsage();
                                        btnDelExtFiles.setEnabled(false);
                                    }
                                });
                            }
                        });
                        t.start();
                    }
                });
                builder.create().show();
            }
        });

        btnDelIntFiles.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = getConfirmDialog();
                builder.setTitle(getString(R.string.dlgDelFile));
                builder.setMessage(String.format(getString(R.string.dlgDelWarnMsg), getString(R.string.file)));
                builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog pd = new ProgressDialog(StorageManageActivity.this);
                        pd.setTitle(getString(R.string.dlgDelFile));
                        pd.setMessage(getString(R.string.dlgDeling));
                        pd.show();

                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                fsm.deleteAllSpeechFiles(FileSysManager.INTERNAL);
                                fsm.deleteAllSubtitleFiles(FileSysManager.INTERNAL);
                                if (pd.isShowing()) pd.dismiss();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshUsage();
                                        btnDelIntFiles.setEnabled(false);
                                    }
                                });

                            }
                        });
                        t.start();
                    }
                });
                builder.create().show();
            }
        });

        btnMoveToUserSpy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {// 此處需要完整的寫入權限
                btnMoveToUserSpy.setEnabled(false);
                Crashlytics.setString("ButtonClick", "MoveFileToSpecifyFolder");

                // 把要執行的行為先封裝進 Runnable，若已經有寫入權限，則本地執行此Runnable，若無則要求寫入權限(isPermissionPass())，並在onRequestPermissionsResult中獲取權限後執行此Runnable。
                PermissionGrantedJob = new Runnable() {
                    @Override
                    public void run() {
                        String path = filePathInput.getText().toString();
                        // Check is the user specify folder is valid.
                        if (!isPathReadable(path)) {
                            btnMoveToUserSpy.setEnabled(true);
                            return;
                        }

                        Crashlytics.log(Log.DEBUG,getClass().getName(), "Create folder: " + path);
                        File filePath = new File(path);
                        filePath.mkdirs();
                        if (!filePath.exists() || !filePath.isDirectory() || !filePath.canWrite()) {
                            Util.showErrorToast(StorageManageActivity.this, getString(R.string.errWriteFile));
                            btnMoveToUserSpy.setEnabled(true);
                            return;
                        }

                        // Check the path is not external/internal default storage path.
                        ArrayList<String> srcList = new ArrayList<String>();
                        srcList.add(fsm.getSrcRootPath(FileSysManager.INTERNAL) + File.separator + getString(R.string.audioDirName));
                        String ext = fsm.getSrcRootPath(FileSysManager.EXTERNAL);
                        if (ext != null)
                            srcList.add(ext + File.separator + getString(R.string.audioDirName));

                        Crashlytics.log(Log.DEBUG,getClass().getName(), "There are " + srcList.size() + " src folder for move file.");
                        Intent intent = new Intent(StorageManageActivity.this, MoveFileService.class);
                        intent.putStringArrayListExtra("srcDirs", srcList);
                        intent.putExtra("destDir", path);

                        Crashlytics.log(Log.DEBUG,getClass().getName(), "Start move file service.");

                        // While user press the move button that mean the path is specified.
                        SharedPreferences.Editor editor = runtime.edit();
                        editor.putBoolean(getString(R.string.isUseThirdDir), true);
                        editor.putString(getString(R.string.userSpecifySpeechDir), filePathInput.getText().toString());
                        editor.commit();

                        //Util.showInfoPopupWindow(StorageManageActivity.this, "背景移動中，請檢視通知列以瞭解進度，移動過程中請勿執行其他操作。");
                        BaseDialogs.showSimpleMsgDialog(StorageManageActivity.this, getString(R.string.dlgMoveFileTitle), getString(R.string.dlgMoveFileMsg));
                        startService(intent);
                        Crashlytics.setString("ButtonClick", "MoveFileToSpecifyFolderStartService");
                        refreshUsage();
                    }
                };

                DialogInterface.OnClickListener doNothing=new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {dialog.dismiss();}};

                BaseDialogs.showDialog(StorageManageActivity.this, getString(R.string.dlgMoveFileTitle), getString(R.string.msgNoDelWarring), new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!isPermissionPass())
                            return;// The isPermissionPass() show grant dialog automatic, execute PermissionGrantJob.run() if grant.
                        PermissionGrantedJob.run();
                        PermissionGrantedJob = null;
                    }},doNothing,true);
                btnMoveToUserSpy.setEnabled(true);
            }
        });


        btnChoicePath.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {// 此處僅需要讀取權限

                // 把要執行的行為先封裝進 Runnable，若已經有寫入權限，則本地執行此Runnable，若無則要求寫入權限(isPermissionPass())，並在onRequestPermissionsResult中獲取權限後執行此Runnable。
                PermissionGrantedJob = new Runnable() {
                    @Override
                    public void run() {
                        File f;
                        String ind;
                        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {  // Check is external storage mounted, no matter read only or read/write.
                            f = Environment.getExternalStorageDirectory();
                            ind = getString(R.string.msgExternalStorage);
                        } else {
                            f = getFilesDir();
                            ind = getString(R.string.msgInternalStorage);
                        }

                        /*
                        if (Build.VERSION.SDK_INT < 19) {
                            Toast.makeText(StorageManageActivity.this, ind, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getBaseContext(), FileDialogActivity.class);
                            intent.putExtra(FileDialogActivity.TITLE, getString(R.string.msgSelectFolder));
                            intent.putExtra(FileDialogActivity.START_PATH, f.getAbsolutePath());
                            intent.putExtra(FileDialogActivity.CAN_SELECT_DIR, true);
                            startActivityForResult(intent, OPEN_DOCUMENT_TREE_UND_23);
                        }
                        */

                        /*
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                            startActivityForResult(intent, OPEN_DOCUMENT_TREE_ABV_23);
                        */

                       // else {
                            Content c = new Content();
                            c.setCancelLabel(getString(R.string.cancel));
                            c.setCreateLabel(getString(R.string.nnew));
                            c.setFolderCreatedToastText(getString(R.string.folderCreated));
                            c.setFolderErrorToastText(getString(R.string.folderErrorToastText));
                            c.setFreeSpaceText(getString(R.string.freeSpace));
                            c.setInternalStorageText(getString(R.string.msgInternalStorage));
                            c.setNewFolderLabel(getString(R.string.newFolder));
                            c.setTextfieldErrorText(getString(R.string.emptyTextFieldErr));
                            c.setTextfieldHintText(getString(R.string.folderNameHint));
                            c.setSelectLabel(getString(R.string.select));
                            c.setOverviewHeading(getString(R.string.choiceDrive));


                            // Initialize Builder
                            StorageChooser chooser = new StorageChooser.Builder()
                                    .withActivity(StorageManageActivity.this)
                                    .withFragmentManager(getFragmentManager())
                                    .withMemoryBar(true)
                                    .allowCustomPath(true)
                                    //.allowAddFolder(true)
                                    .setType(StorageChooser.DIRECTORY_CHOOSER)  // For pickup folder.
                                    // .setType(StorageChooser.FILE_PICKER) // For pickup file.
                                    .withContent(c)
                                    .build();

                            chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                                @Override
                                public void onSelect(String path) {
                                    //Toast.makeText(StorageManageActivity.this, path, Toast.LENGTH_LONG).show();
                                    Log.d(logTag, "User select specify folder: "+path);
                                    filePathInput.setText(path);
                                }
                            });

                            chooser.show();
                        //}
                    }
                };

                if (!isPermissionPass())
                    return;// The isPermissionPass() show grant dialog automatic, execute PermissionGrantJob.run() if grant.

                PermissionGrantedJob.run();
                PermissionGrantedJob = null;
            }
        });

        // 對於Ok button來講，只需要確認到該目錄能讀就可以pass。
        btnOk.setOnClickListener(new OnClickListener() {// 此處僅需要讀取權限
            @Override
            public void onClick(View v) {
                final SharedPreferences.Editor editor = runtime.edit();
                if (!isUseThirdDir) {
                    Crashlytics.log(Log.DEBUG,getClass().getName(), "is user specify the third dir? " + isUseThirdDir);
                    editor.putBoolean(getString(R.string.isUseThirdDir), false);
                    editor.apply();
                    finish();
                    return;
                }

                // 把要執行的行為先封裝進 Runnable，若已經有寫入權限，則本地執行此Runnable，若無則要求寫入權限(isPermissionPass())，並在onRequestPermissionsResult中獲取權限後執行此Runnable。
                PermissionGrantedJob = new Runnable() {
                    @Override
                    public void run() {
                        // Check is the user specify folder is valid.
                        String path = filePathInput.getText().toString();
                        if (!isPathReadable(path)) return;

                        editor.putBoolean(getString(R.string.isUseThirdDir), true);
                        editor.putString(getString(R.string.userSpecifySpeechDir), filePathInput.getText().toString());
                        editor.commit();

                        if (!new File(path).canWrite())
                            BaseDialogs.showDialog(
                                    StorageManageActivity.this,
                                    getString(R.string.msgNoDownload),
                                    getString(R.string.msgNoDownloadDesc),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    },
                                    null,
                                    false
                            );
                        else
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });
                    }
                };

                if (!isPermissionPass())
                    return;// The isPermissionPass() show grant dialog automatic, execute PermissionGrantJob.run() if grant.

                PermissionGrantedJob.run();
                PermissionGrantedJob = null;
            }
        });

        radioMgnType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch (checkedId) {
                    case R.id.radioAutoMgnStorage:
                        isUseThirdDir = false;
                        filePathInput.setEnabled(false);
                        btnChoicePath.setEnabled(false);
                        btnChoicePath.setClickable(false);
                        labelChoicePath.setEnabled(false);
                        btnMoveToUserSpy.setEnabled(false);
                        break;
                    case R.id.radioUserMgnStorage:
                        isUseThirdDir = true;
                        filePathInput.setEnabled(true);
                        btnChoicePath.setEnabled(true);
                        btnChoicePath.setClickable(true);
                        labelChoicePath.setEnabled(true);
                        btnMoveToUserSpy.setEnabled(true);
                        break;
                }
            }
        });

        String extSpeechDir = fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
        String extSubtitleDir = fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.SUBTITLE_TYPE));
        String intSpeechDir = fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
        String intSubtitleDir = fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.SUBTITLE_TYPE));

        extSpeechPathInfo.setText(((extSpeechDir != null) ? extSpeechDir : getString(R.string.noExtSpace)));
        intSpeechPathInfo.setText(intSpeechDir);

        Crashlytics.log(Log.DEBUG,getClass().getName(), "Leave onCreate");
    }

    // 注意！此處只檢查到讀取權限，不包含檢查寫入權限
    private boolean isPathReadable(String pathStr) {
        if (pathStr.length() == 0) {
            BaseDialogs.showSimpleErrorDialog(StorageManageActivity.this, getString(R.string.msgFolderErr), getString(R.string.msgFolderErrDesc));
            String path = fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
            if (path == null)
                path = fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
            filePathInput.setText(path);
            return false;
        }
        // Check is the path is FILE
        File f = new File(pathStr);
        if (f.isFile()) {
            BaseDialogs.showSimpleErrorDialog(StorageManageActivity.this, getString(R.string.msgFolderErr), getString(R.string.msgNotFolder));
            return false;
        }

        if (!f.exists()) {
            if (f.mkdirs())
                return true;
            else {
                BaseDialogs.showSimpleErrorDialog(StorageManageActivity.this, getString(R.string.msgFolderErr), getString(R.string.msgCantCreateFolder));
                return false;
            }
        }

        if (!f.canRead()) {
            BaseDialogs.showSimpleErrorDialog(StorageManageActivity.this, getString(R.string.msgPermErr), getString(R.string.msgPermErrDesc));
            return false;
        }
        return true;
    }

    private void refreshUsage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                intFreeB = fsm.getFreeMemory(FileSysManager.INTERNAL);
                extFreeB = fsm.getFreeMemory(FileSysManager.EXTERNAL);
                //intTotal = fsm.getTotalMemory(FileSysManager.INTERNAL);
                //extTotal = fsm.getTotalMemory(FileSysManager.EXTERNAL);
                intUsed = fsm.getAppUsed(FileSysManager.INTERNAL);
                extUsed = fsm.getAppUsed(FileSysManager.EXTERNAL);
                userSpecDir = runtime.getString(getString(R.string.userSpecifySpeechDir), null);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        extFree.setText(numToKMG(extFreeB) + "B");
                        intFree.setText(numToKMG(intFreeB) + "B");
                        extAppUseage.setText(numToKMG(extUsed) + "B");
                        intAppUseage.setText(numToKMG(intUsed) + "B");

                        if (intFreeB > extUsed && extUsed > 0) btnMoveAllToInt.setEnabled(true);
                        else btnMoveAllToInt.setEnabled(false);
                        if (extFreeB > intUsed && intUsed > 0) btnMoveAllToExt.setEnabled(true);
                        else btnMoveAllToExt.setEnabled(false);
                        if (intUsed > 0) btnDelIntFiles.setEnabled(true);
                        else btnDelIntFiles.setEnabled(false);
                        if (extUsed > 0) btnDelExtFiles.setEnabled(true);
                        else btnDelExtFiles.setEnabled(false);

                        if (userSpecDir != null) filePathInput.setText(userSpecDir);
                    }
                });

            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUsage();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        boolean isUserSpecifyDir = runtime.getBoolean(getString(R.string.isUseThirdDir), false);
        if (isUserSpecifyDir)
            Crashlytics.setString("UserSpecifyDirPath", runtime.getString(getString(R.string.userSpecifySpeechDir),null));
        else
            Crashlytics.setString("DefaultMediaPath", fsm.getSysDefMediaDir());

        super.finish();
    }

    // The function will not been call after use storage-chooser.
    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        switch (requestCode) {
            case OPEN_DOCUMENT_TREE_UND_23:

                final String filePath = data.getStringExtra(FileDialogActivity.RESULT_PATH);
                filePathInput.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        filePathInput.setText(filePath);
                    }
                }, 500);
                break;
            case OPEN_DOCUMENT_TREE_ABV_23:
                final Uri treeUri = data.getData();
                final DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);

                final File file = new File(treeUri.getPath());//create path from uri
                final String[] split = file.getPath().split(":");//split the path.


                filePathInput.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //filePathInput.setText(pickedDir.getUri().getPath());
                        filePathInput.setText(file.getPath());
                    }
                }, 200);
                break;
        }
/*
        // Avoid EditText bug,  the EditText will not change to the new value without the thread.
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					Crashlytics.log(Log.DEBUG,getClass().getName(),"Set path the EditText: "+filePath);
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							filePathInput.setText(filePath);
						}});
					
				} catch (InterruptedException e) {e.printStackTrace();}
			}}).start();
	*/
    }

    private boolean isPermissionPass() {
        if (Build.VERSION.SDK_INT < 23) return true;

        int permissionCheck = ContextCompat.checkSelfPermission(StorageManageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) return true;

        ActivityCompat.requestPermissions(StorageManageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_ACCESS_PERMISSION_REQUEST);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_ACCESS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (PermissionGrantedJob != null) {
                    PermissionGrantedJob.run();
                    PermissionGrantedJob = null;
                }
            }
        }
    }


    private void showAskMoveToSpecifyDialog(final String path) {
        final AlertDialog.Builder builder = getConfirmDialog();
        builder.setTitle(getString(R.string.dlgMoveFile));
        builder.setMessage(getString(R.string.dlgSureMoveAllAudioFile));
        builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final ProgressDialog pd = new ProgressDialog(StorageManageActivity.this);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setCancelable(false);
                pd.setTitle(getString(R.string.dlgMoveFile));
                pd.setMessage(getString(R.string.msgFileMoving));
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File destFile = new File(path);
                        if (!fsm.moveAllMediaFileToUserSpecifyDir(destFile, pd) || !fsm.moveAllMediaFileToUserSpecifyDir(destFile, pd))
                            Util.showErrorToast(StorageManageActivity.this, getString(R.string.errStorageNotReady));
                        if (pd.isShowing()) pd.dismiss();
                        refreshUsage();
                    }
                });
                t.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.show();
                    }
                });
            }
        });
        builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.cancel();
                    }
                });
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.create().show();
            }
        });
    }

    private String numToKMG(long num) {
        Crashlytics.log(Log.DEBUG,getClass().getName(), "Cac: " + num);
        String[] unit = {"", "K", "M", "G", "T"};
        String s = "" + num;
        int len = s.length();

        int sign = (int) (len / 3);
        if (sign * 3 == len) sign--;

        int index = sign * 3;
        String result = s.substring(0, s.length() - index) + '.' + s.charAt(index) + unit[sign];
        Crashlytics.log(Log.DEBUG,getClass().getName(), "Num= " + s + ", Length: " + s.length() + ", result=" + result);
        return result;
    }

    private AlertDialog.Builder getConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
//	        	if(wakeLock.isHeld())wakeLock.release();
                dialog.cancel();
            }
        });
        return builder;
    }
}
