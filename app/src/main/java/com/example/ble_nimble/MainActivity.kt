package com.example.ble_nimble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ble_nimble.ui.theme.BLE_NimBLETheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
            0)
        enableEdgeToEdge()
        setContent {
            val data: MutableList<ScanResult> = remember { mutableStateListOf() }
            var device:BluetoothDevice? = null
            var bluetoothGatt: BluetoothGatt? = null

            fun checkBluetoothPermission(): Boolean {
                return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
            val leScanCallback: ScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    if (checkBluetoothPermission()) {
                        if(result.device.name != null) {
                            if (data.none { it.device == result.device }){
                                data.add(result)
                            }
                        }
                        Log.d("result.device.name",result.device.name ?: "No Name")
                        println("result.device $${result.device.name} - ${result}")
                    }
                }
            }

            val bluetoothGattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        println(" successfully connected to the GATT Server")
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        println("disconnected from the GATT Server")
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if(status == BluetoothGatt.GATT_SUCCESS) {
//                        一覧の取得
                        val ser = gatt!!.services
                        ser.forEach { s ->
                            println("BEL - Service UUID: ${s.uuid}")
                            val chara = s.characteristics
                            chara.forEach{ c ->
                                println(" !- ${c.uuid}")
                                c.descriptors.forEach { d ->
                                    println("!--${d.uuid}")
                                }
                            }
                        }


//                        val service = gatt?.getService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"))
//                        val characteristic = service?.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"))
                        val services = gatt?.getServices()
                        var characteristic: BluetoothGattCharacteristic? = null
                        services!!.forEach { service ->
                            if (service.uuid.toString() == "00001815-0000-1000-8000-00805f9b34fb") {
                                service.characteristics.forEach {chara ->
                                    if (chara.uuid.toString() == "91a7598f-9df4-1ee4-f618-39b97e8d1971" ){
                                        characteristic = service?.getCharacteristic(UUID.fromString("91a7598f-9df4-1ee4-f618-39b97e8d1971"))
                                    }
                                }
                            }
                        }
                        if (checkBluetoothPermission()) {
                            if (services != null) {
                                if (characteristic != null) {
//                                    gatt.readCharacteristic(characteristic)
                                    gatt.setCharacteristicNotification(characteristic,true)
                                    val descriptor = characteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                                    descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                    gatt.writeDescriptor(descriptor)
                                } else {
                                    println("charaがない")
                                }
                            } else {
                                println("サービスが無い")
                            }
                        }
                    }
                }


                @Deprecated("Deprecated in Java")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    //                    super.onCharacteristicRead(gatt, characteristic, value, status)
                    if (status == BluetoothGatt.GATT_SUCCESS){
                        if (characteristic != null) {
                            val newV = characteristic.value.joinToString(separator = "") { byte -> byte.toInt().toString()}
                            println("読み取った値${newV}")
                        }
                    } else {
                        println("値が読み取れなかったがonCharacteristicReadは呼び出されたよ")
                    }

                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt?,
                    descriptor: BluetoothGattDescriptor?,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLE", "Descriptor write successful")
                    } else {
                        Log.e("BLE", "Descriptor write failed with status: $status")
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    super.onCharacteristicChanged(gatt, characteristic)

                    // 値を取得
                    if (characteristic != null) {
                        val newV = characteristic.value.joinToString(separator = "") { byte -> byte.toInt().toString()}
                        println("読み取った値${newV}")
                    }
                }
            }
            BLE_NimBLETheme {
                val ctx = LocalContext.current
                var scanning = false
                var scaner: BluetoothLeScanner? = null;
                val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                        Button(onClick = {
                            if (bluetoothAdapter == null) {
                                println("MainActivity.kt L36 BLEAdapterがありません。")
                            }else {
//                                BLEアダプタの取得が確認できたらBLEが有効か確認
                                println("${bluetoothAdapter}")

                                if (bluetoothAdapter.isEnabled == false) {
                                    println("L51 BLEが無効だよ")
                                } else {
                                    println(" BLEが有効だよ")
                                    data.clear()
                                    val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                                    val settings = ScanSettings.Builder()
                                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // スキャンモードは適宜調整
//                                        .setReportDelay(0)
                                        .build()

                                    bluetoothLeScanner?.let { scanner ->
                                        scaner = scanner
                                        scanning = true
                                        scanner.startScan(null, settings, leScanCallback)
                                    }
                                }
                            }
                        }) {
                            Text(text = "Get BLE")
                        }
                        Button(onClick = {
                            scanning = false
                            scaner?.stopScan(leScanCallback)
                        }) {
                            Text("Stop Scan")
                        }

                        if (data.isNotEmpty()) {
                            data.forEach{ dev ->
                                Row(
                                    modifier = Modifier
                                        .height(150.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val bleDev: BluetoothDevice = dev.device
                                    Column(
                                        modifier = Modifier.clickable {
                                            device = bleDev
                                        }
                                    ) {
                                        Text(dev.device?.name ?: "No-Name")
                                        Text(dev.device.address ?: "No-Name")
                                        Text("${ dev.device?.bluetoothClass ?: "No-Name" }")
                                        Text("${ dev.device?.uuids ?: "No-Name" }")
                                        Text("${ dev.device?.type ?: "No-Name" }")
                                        Text("${ dev.device?.bondState ?: "No-Name" }")
                                    }
                                }
                            }
                        }



                        Button(onClick = {
                            if(device != null) {
                                println("接続処理の開始")
                                bluetoothAdapter.let { adapter ->
                                    try {
                                        if (adapter != null) {
                                            val devic = adapter.getRemoteDevice(device!!.address)
                                            bluetoothGatt = devic.connectGatt(ctx, false, bluetoothGattCallback)
                                        }
                                    } catch (e:IllegalArgumentException) {
                                        println("接続に失敗")
                                    }
                                }
                            }
                        }) {
                            Text("GATTサーバーに接続")
                        }

                        Button(onClick = {
                            bluetoothGatt!!.discoverServices()
                        }) {
                            Text("GATTサービスの読み取り")
                        }

                        Button(onClick = {
                            if(bluetoothGatt != null) {
                                bluetoothGatt!!.disconnect()
                                bluetoothGatt!!.close()
                                bluetoothGatt = null
                            }
                        }) {
                            Text("STOP CONEECT")
                        }

//                        Button(onClick = {
//
//                        }) {
//                            Text("get TempRate")
//                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BLE_NimBLETheme {
        Greeting("Android")
    }
}