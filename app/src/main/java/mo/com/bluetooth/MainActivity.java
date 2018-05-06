package mo.com.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private EditText editbox_send;
    private TextView textView_show;
    private ImageView button_open, button_select, button_send, button_clean, button_run;
    private BluetoothAdapter adapter;
    private BroadcastReceiver searchDevices;
    private List<BluetoothDevice> devices;
    private BluetoothDevice device;
    private MyHandler myHandler;
    private Thread connectThread;
    private OutputStream btos;
    private InputStream btis;
    private BluetoothSocket btst;
    private static SQLiteDatabase mainDB;
    public Object btst_LOCK = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = BluetoothAdapter.getDefaultAdapter();
        devices = new ArrayList<>();
        myHandler = new MyHandler(MainActivity.this);
        setView();
        setOnClickListener();
        setDataBase();
        setRegister();
        setBluetooth();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(searchDevices);
    }

    private void setView() {
        editbox_send = findViewById(R.id.editbox_send);
        textView_show = findViewById(R.id.textView_show);
        textView_show.setMovementMethod(ScrollingMovementMethod.getInstance());
        button_open = findViewById(R.id.button_open);
        button_select = findViewById(R.id.button_select);
        button_send = findViewById(R.id.button_send);
        button_clean = findViewById(R.id.button_clean);
        button_run = findViewById(R.id.button_run);

    }

    private void setDataBase() {
        mainDB = SQLiteDatabase.openOrCreateDatabase(getResources().getString(R.string.SQLiteDB) +
                getResources().getString(R.string.DBName), null);

    }

    private void setOnClickListener() {
        button_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapter.isEnabled()) {//关闭
                    adapter.cancelDiscovery();
                    adapter.disable();
                    button_open.setImageDrawable(getResources().getDrawable(R.drawable.open1));
                } else {//打开
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    MainActivity.this.startActivity(enableBtIntent);
                    button_open.setImageDrawable(getResources().getDrawable(R.drawable.open2));

//                    if (adapter.isEnabled()) {
//                        if (adapter.getScanMode() !=
//                                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//                            Intent discoverableIntent = new Intent(
//                                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                            discoverableIntent.putExtra(
//                                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//                            startActivity(discoverableIntent);
//                        }
//                    }
                    //开始搜索蓝牙设备
                    adapter.startDiscovery();


                    if (connectThread == null) {
                        connectThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (true) {
                                        Thread.sleep(7);
                                        //等待连接
                                        synchronized (btst_LOCK) {
                                            try {
                                                btst_LOCK.wait();

                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //开始连接设备
//                                    ToastTool.showToast(MainActivity.this, "已选定设备", 300);
                                        try {
                                            //开始通讯
                                            btst = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                                            btst.connect();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    textView_show.append("蓝牙连接成功，Socket已建立！\n");
                                                    adapter.cancelDiscovery();
                                                }
                                            });

                                            btos = btst.getOutputStream();
                                            btis = btst.getInputStream();
                                            System.out.println(btos);
                                            while (true) {
                                                final String content = readData();
                                                if (!content.equals("")) {
                                                    System.out.println("接受到Arduino数据" + content);
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
//                                                            textView_show.append(content);
                                                            refreshLogView(content);
                                                        }
                                                    });
                                                }
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    System.out.println("Interrupted When Sleep ...");
                                    // Thread.sleep()方法由于中断抛出异常。
                                    // Java虚拟机会先将该线程的中断标识位清除，然后抛出InterruptedException，
                                    // 因为在发生InterruptedException异常的时候，会清除中断标记
                                    // 如果不加处理，那么下一次循环开始的时候，就无法捕获这个异常。
                                    // 故在异常处理中，再次设置中断标记位
                                    Thread.currentThread().interrupt();
                                }


                            }
                        });
                        connectThread.start();
                    } else {
                        if (!connectThread.isInterrupted()) {
                            connectThread.interrupt();
                        }
                        if (btst != null) {
                            if (btst.isConnected()) {
                                try {
                                    btst.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                btos.close();
                                btis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }

            }
        });
        button_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] names = new String[devices.size()];
                int index = 0;
                for (BluetoothDevice s : devices) {
                    names[index] = s.getName() + "\n" + s.getAddress();
                    index++;
                }
                Message msg = new Message();
                msg.what = 0x09;
                msg.obj = names;
                myHandler.handleMessage(msg);
            }
        });
        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (btos == null) {
                        ToastTool.showToast(MainActivity.this, "Socket未建立", 300);
                    } else {
                        btos.write(editbox_send.getText().toString().getBytes());
                        btos.flush();
                    }
                    editbox_send.setText("");
                } catch (IOException e) {
                    textView_show.append(e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        button_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_show.setText("");
                    }
                });
            }
        });
        button_run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Cursor rs = mainDB.query("Students", null, null, null, null, null, null);
                final int peopleNum = rs.getColumnCount();
                Student[] stus = new Student[peopleNum];
                int index = 0;
                while (rs.moveToNext()) {
                    stus[index] = new Student();
                    stus[index].setName(rs.getString(1));
                    stus[index].setUid(rs.getString(2));
                    System.out.println(rs.getString(2));
                    stus[index].setSignToday(false);
                    index++;
                }
                String data = textView_show.getText().toString();
                String datas[] = data.split("Card UID:");
                //提取uid
                for (int i = 0; i < datas.length; i++) {
                    datas[i] = datas[i].substring(0, 12);
                    datas[i] = datas[i].replace(" ", "");
                    System.out.println(datas[i]);
                }
                //检测数据库
                for (int i = 0; i < datas.length; i++) {
                    for (int j = 0; j < peopleNum; j++) {
                        if (datas[i].equals(stus[j].getUid())) {
                            stus[j].setSignToday(true);
                            System.out.println(datas[i]);
                        }
                    }
                }
                //打印未签到
                int in = 0;
                for (int j = 0; j < peopleNum; j++) {
                    if (stus[j].isSignToday() == false) {
                        final String name = stus[j].getName();
                        in++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView_show.append("本日" + name + "未签到！\n");
                            }
                        });
                    }
                }
                final int finalIn = in;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_show.append("物联网1151共" + peopleNum + "人，签到" + (peopleNum - finalIn) + "人，未签到" + finalIn + "人!\n");
                    }
                });
            }
        });
    }


    private void setBluetooth() {
        IntentFilter intent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intent.addAction(BluetoothDevice.ACTION_FOUND);  //发现新设备
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);  //绑定状态改变
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);  //开始扫描
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  //结束扫描
        intent.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);  //连接状态改变
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);  //蓝牙开关状态改变
        registerReceiver(searchDevices, intent);
    }

    private void setRegister() {
        searchDevices = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND: {
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        ToastTool.showToast(MainActivity.this, device.getName() + ":" + device.getAddress(), 300);
                        System.out.println(device.getName() + ":" + device.getAddress());
                        devices.add(device);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView_show.append("发现蓝牙设备！" + device.getName() + ":" + device.getAddress() + "\n");
                            }
                        });
                        break;
                    }
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        String content = "";
                        switch (device.getBondState()) {
                            case BluetoothDevice.BOND_BONDING://正在配对
                                Log.d("BlueToothTestActivity", "正在配对......");
                                content = "正在配对......";
                                break;
                            case BluetoothDevice.BOND_BONDED://配对结束
                                Log.d("BlueToothTestActivity", "完成配对");
                                content = "完成配对!";
                                break;
                            case BluetoothDevice.BOND_NONE://取消配对/未配对
                                Log.d("BlueToothTestActivity", "取消配对");
                                content = "取消配对!";
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView_show.append("开始搜索蓝牙设备...\n");
                            }
                        });
                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView_show.append("开始搜索蓝牙设备...\n");
                            }
                        });
                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                        devices.clear();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView_show.append("搜索结束！\n");
                            }
                        });
                        break;
                    }
                }
            }
        };
    }

    private String readData() throws IOException {
        byte[] localObject = new byte[1024];
        String content = "";
        if (btis.available() > 0) {
            btis.read(localObject);
            int i = 0;
            while ((i < (localObject).length) && (localObject[i] != 0)) {
                i += 1;
            }
            content = new String(localObject, 0, i);

        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return content;
    }

    public List<BluetoothDevice> getDevices() {
        return devices;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public TextView getTextView_show() {
        return textView_show;
    }

    private void refreshLogView(String msg) {
        textView_show.append(msg);
        int offset = textView_show.getLineCount() * textView_show.getLineHeight();
        if (offset > textView_show.getHeight()) {
            textView_show.scrollTo(0, offset - textView_show.getHeight());
        }
    }
}
