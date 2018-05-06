package mo.com.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;

import java.lang.reflect.Method;
import java.util.List;

public class MyHandler extends Handler {
    public MyHandler(Activity activity) {
        this.activity = activity;
    }
    private Activity activity;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case 0x09:{
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("请选择设备");
                final String[] names = (String[]) msg.obj;
                //    设置一个单项选择下拉框
                /**
                 * 第一个参数指定我们要显示的一组下拉单选框的数据集合
                 * 第二个参数代表索引，指定默认哪一个单选框被勾选上，1表示默认'女' 会被勾选上
                 * 第三个参数给每一个单选项绑定一个监听器
                 */
                builder.setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity mainActivity = (MainActivity) activity;
                        List<BluetoothDevice> devices = mainActivity.getDevices();
                        for(BluetoothDevice device: devices) {
                            if(names[i].contains(device.getAddress())){
                                System.out.println("即将连接设备：" + names[i]);
                                mainActivity.setDevice(device);
                                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                                    //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                                    try {
                                        Method createBondMethod = BluetoothDevice.class
                                                .getMethod("createBond");
                                    } catch (NoSuchMethodException e) {
                                        e.printStackTrace();
                                    }

                                }
                                synchronized (mainActivity.btst_LOCK){
                                    mainActivity.btst_LOCK.notify();
//                                    ToastTool.showToast(mainActivity,"唤醒主线程蓝牙绑定",300);
                                }
                            }
                        }
                    }
                });
                builder.show();
                break;
            }
        }
    }
}
