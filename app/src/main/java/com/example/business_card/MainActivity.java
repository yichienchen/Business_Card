package com.example.business_card;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static com.example.business_card.DBHelper.TB1;


public class MainActivity extends AppCompatActivity {

    static int ManufacturerData_size = 24;  //ManufacturerData長度
    static String TAG = "chien";

    public static byte[][] data_legacy;
    public static byte[][] data_extended;

    public static String card;

    static boolean version = false;  //true: 4.0 , false:5.0

    static byte[] id_byte = new byte[4];


    static List<String> list_device = new ArrayList<>();
    static List<String> list_device_detail = new ArrayList<>();


    static ArrayList<ArrayList<Object>> matrix = new ArrayList<>();
    static ArrayList<ArrayList<Object>> time_interval = new ArrayList<>();
    static ArrayList<Integer> num_total = new ArrayList<>();
    static ArrayList<Long> time_previous = new ArrayList<>();
    static ArrayList<Long> mean_total = new ArrayList<>();

    static ArrayList<ArrayList<Long>> num_list = new ArrayList<>();
    static  ArrayList<ArrayList<Long>> num_time = new ArrayList<>();

    static  ArrayList<ArrayList<String>> data_list = new ArrayList<>();

    static Map<Integer, AdvertiseCallback> AdvertiseCallbacks_map;
    static Map<Integer, AdvertisingSetCallback> extendedAdvertiseCallbacks_map;


    static BluetoothManager mBluetoothManager;
    static BluetoothAdapter mBluetoothAdapter;
    static BluetoothLeScanner mBluetoothLeScanner;
    static AdvertiseCallback mAdvertiseCallback;
    static BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    static Button startScanningButton;
    static Button stopScanningButton;
    static Button scan_list;
    static Button startAdvButton;
    static Button stopAdvButton;
    static Button name_card;
    public static TextView peripheralTextView;
    static TextView sql_Text;

    //    default mode: low power

    static NotificationManager notificationManager;
    static NotificationChannel mChannel;
    Intent intentMainActivity;
    static PendingIntent pendingIntent;
    Notification notification;
    static Intent received_id;

    Intent adv_service;
    Intent scan_service;

    public static DBHelper DH=null;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DH = new DBHelper(this,"BC_DB",null,1);
        card = "";
        new Random().nextBytes(id_byte);
        initialize();
        permission();
        element();

    }

    @Override
    public void onDestroy() {
        //TODO 回前頁會呼叫onDestroy
//        notificationManager.notify(1000, notification);
        stopService(adv_service);
        stopService(scan_service);
        Log.e(TAG, "onDestroy() called");
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.e(TAG, "onResume() called");
        permission();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initialize() {
        if (mBluetoothLeScanner == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
            }
        }
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                }
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        version = !mBluetoothAdapter.isLeExtendedAdvertisingSupported();
    }

    public void permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.VIBRATE}, 1);
        }

    }

    private void element() {
        /*---------------------------------------scan-----------------------------------------*/
        startScanningButton = findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(scan_service);
            }
        });
        stopScanningButton = findViewById(R.id.StopScanButton);
        stopScanningButton.setVisibility(View.INVISIBLE);
        scan_list = findViewById(R.id.scan_list);
        scan_list.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final SQLiteDatabase db = DH.getReadableDatabase();
//                for (int counter = 0 ; counter <card_list(db).length ; counter++) {
//                    Log.e(TAG,counter + " list: " + Arrays.toString(card_list(db)[counter]));
//                }

                if (v.getId() == R.id.scan_list) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Business Card List")
                            .setItems(card_list(db)[1], new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, final int which) {
                                    StringBuilder resultData = new StringBuilder("");
//                                    resultData.append("Name: ").append(card_list(db)[0][which]).append("\n");
                                    resultData.append("Phone: ").append(card_list(db)[2][which]).append("\n");
                                    resultData.append("E-mail: ").append(card_list(db)[3][which]).append("\n");
                                    resultData.append("Company: ").append(card_list(db)[4][which]).append("\n");
                                    resultData.append("Position: ").append(card_list(db)[5][which]).append("\n");
                                    resultData.append("Other: ").append(card_list(db)[6][which]);
                                    Toast.makeText(getApplicationContext(), resultData, Toast.LENGTH_SHORT).show();

                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(card_list(db)[1][which])
                                            .setMessage(resultData)
                                            .setPositiveButton("close", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int w) {

                                                }
                                            })
                                            .setNegativeButton("delete", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int w) {
                                                    //todo 刪掉
//                                                    Log.e(TAG,"刪掉");
                                                    delete(card_list(db)[0][which]);
                                                }
                                            })
                                            .show();

                                }
                            })
                            .setPositiveButton("close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();

                }
            }
        });
        sql_Text = findViewById(R.id.sql_Text);

        /*--------------------------------------advertise----------------------------------------*/
        startAdvButton = findViewById(R.id.StartAdvButton);
        startAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(card != null){
                    startService(adv_service);
                }else {
                    Toast.makeText(MainActivity.this,"Please enter your business card!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        stopAdvButton = findViewById(R.id.StopAdvButton);
        stopAdvButton.setVisibility(View.INVISIBLE);

        /*--------------------------------------intent----------------------------------------*/
        adv_service = new Intent(MainActivity.this, Service_Adv.class);
        scan_service = new Intent(MainActivity.this, Service_Scan.class);

        /*-------------------------------------Receiver---------------------------------------*/
        received_id = new Intent();

        /*--------------------------------------others----------------------------------------*/
        peripheralTextView = findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod()); //垂直滾動
        AdvertiseCallbacks_map = new TreeMap<>();
        extendedAdvertiseCallbacks_map = new TreeMap<>();

        /*---------------------------------------card----------------------------------------*/
        name_card = findViewById(R.id.name_card);
        final SharedPreferences SP = getApplicationContext().getSharedPreferences("NAME",0);
        name_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);

                View mView = getLayoutInflater().inflate(R.layout.dialog,null);
                final EditText name = mView.findViewById(R.id.etname);
                final EditText phone = mView.findViewById(R.id.etphone);
                final EditText email = mView.findViewById(R.id.etemail);
                final EditText company = mView.findViewById(R.id.etcompany);
                final EditText position = mView.findViewById(R.id.etposition);
                final EditText other = mView.findViewById(R.id.etorther);
                final Button btn_card = mView.findViewById(R.id.btncard);



                name.setText(SP.getString("NAME",null));
                phone.setText(SP.getString("PHONE",null));
                email.setText(SP.getString("EMAIL",null));
                company.setText(SP.getString("COMPANY",null));
                position.setText(SP.getString("POSITION",null));
                other.setText(SP.getString("OTHER",null));

                name.setEnabled(true);
                phone.setEnabled(true);
                email.setEnabled(true);
                company.setEnabled(true);
                position.setEnabled(true);
                other.setEnabled(true);
                btn_card.setEnabled(true);

                mBuilder.setView(mView);
                final AlertDialog dialog = mBuilder.create();

                btn_card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!name.getText().toString().isEmpty()){
                            if(name.getText().toString().equals(SP.getString("NAME",null)) &&
                                    phone.getText().toString().equals(SP.getString("PHONE",null)) &&
                                    email.getText().toString().equals(SP.getString("EMAIL",null)) &&
                                    company.getText().toString().equals(SP.getString("COMPANY",null)) &&
                                    position.getText().toString().equals(SP.getString("POSITION",null)) &&
                                    other.getText().toString().equals(SP.getString("OTHER",null)) ){
                                Toast.makeText(MainActivity.this,"Setting has no change",Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(MainActivity.this,"Setting Business Card Successfully",Toast.LENGTH_SHORT).show();

                                SharedPrefesSAVE(1,name.getText().toString());
                                SharedPrefesSAVE(2,phone.getText().toString());
                                SharedPrefesSAVE(3,email.getText().toString());
                                SharedPrefesSAVE(4,company.getText().toString());
                                SharedPrefesSAVE(5,position.getText().toString());
                                SharedPrefesSAVE(6,other.getText().toString());

                                card = name.getText().toString() + ":" + phone.getText().toString() + ":" + email.getText().toString() + ":"
                                        + company.getText().toString() + ":" + position.getText().toString() + ":" + other.getText().toString()+ ":";

                                new Random().nextBytes(id_byte);

                                SharedPrefesSAVE(7,card);
                                Log.e(TAG,"card: "+card.length()+card);
                            }
                            dialog.dismiss();

                        }else {
                            Toast.makeText(MainActivity.this,"Please fill the name field",Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                dialog.show();
            }
        });

        card = SP.getString("CARD",null);

        if(card != null){
            Log.e(TAG,"card: "+card.length()+card);
        }
    }

    private static String[][] card_list(SQLiteDatabase db){
        Cursor cursor = db.query(TB1,new String[]{"_id","NAME","PHONE","EMAIL","COMPANY","POSITION","OTHER"},
                null,null,null,null,null);

        String[][] list = new String[7][cursor.getCount()];


        while(cursor.moveToNext()){

            String _id = cursor.getString(0);
            String n = cursor.getString(1);
            String p = cursor.getString(2);
            String e = cursor.getString(3);
            String c = cursor.getString(4);
            String po = cursor.getString(5);
            String o = cursor.getString(6);

            list[0][cursor.getPosition()] = _id;
            list[1][cursor.getPosition()] = n;
            list[2][cursor.getPosition()] = p;
            list[3][cursor.getPosition()] = e;
            list[4][cursor.getPosition()] = c;
            list[5][cursor.getPosition()] = po;
            list[6][cursor.getPosition()] = o;

        }
        cursor.close();

        return list;
    }

    public static void delete(String _id){
        SQLiteDatabase db = DH.getWritableDatabase();
        db.delete(TB1,"_id=?",new String[]{_id});
    }

    public void SharedPrefesSAVE(int type,String value){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NAME",0);
        SharedPreferences.Editor prefEDIT = prefs.edit();
        switch (type){
            case 1 :
                prefEDIT.putString("NAME",value);
                break;
            case 2 :
                prefEDIT.putString("PHONE",value);
                break;
            case 3 :
                prefEDIT.putString("EMAIL",value);
                break;
            case 4 :
                prefEDIT.putString("COMPANY",value);
                break;
            case 5 :
                prefEDIT.putString("POSITION",value);
                break;
            case 6 :
                prefEDIT.putString("OTHER",value);
                break;
            case 7 :
                prefEDIT.putString("CARD",value);
                break;
        }

        prefEDIT.apply();
    }

}


