package com.print.test

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.print.test.PrinterControl.BixolonPrinter
import com.print.test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener, View.OnTouchListener {

    private lateinit var bxlPrinter: BixolonPrinter
    private val checkBoxAsyncMode: Boolean = true
    private val bondedDevices: ArrayList<CharSequence> = ArrayList()
    private var binding: ActivityMainBinding? = null
    private val DEVICE_ADDRESS_START = " ("
    private val DEVICE_ADDRESS_END = ")"
    private var arrayAdapter: ArrayAdapter<CharSequence>? = null
    private val portType = 0//BXLConfigLoader.DEVICE_BUS_BLUETOOTH
    private var address = ""
    private var logicalName = ""


    fun setDeviceLog(data: String?) {
        mHandler2.obtainMessage(0, 0, 0, data).sendToTarget()
    }

    val mHandler2 = Handler { msg ->
        when (msg.what) {
            0 -> {
                deviceMessagesTextView!!.append(
                    """
                          ${msg.obj as String}
                          
                          """.trimIndent()
                )
                val layout = deviceMessagesTextView!!.layout
                if (layout != null) {
                    val y = layout.getLineTop(
                        deviceMessagesTextView!!.lineCount
                    ) - deviceMessagesTextView!!.height
                    if (y > 0) {
                        deviceMessagesTextView!!.scrollTo(0, y)
                        deviceMessagesTextView!!.invalidate()
                    }
                }
            }
        }
        false
    }

    private var textData: EditText? = null

    private var checkBold: CheckBox? = null
    private var checkUnderline: CheckBox? = null
    private var checkReverse: CheckBox? = null

    private var deviceMessagesTextView: TextView? = null

    private var spinnerAlignment = 0
    private var spinnerFont = 0
    private var spinnerSize = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        permissionHandle()

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        bxlPrinter = BixolonPrinter(this)


        arrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, bondedDevices)

        binding?.listViewPairedDevices?.apply {
            adapter = arrayAdapter
            setChoiceMode(ListView.CHOICE_MODE_SINGLE)
            setOnItemClickListener(this@MainActivity)
            setOnTouchListener(this@MainActivity)
        }
        val modelList: Spinner = findViewById(R.id.spinnerModelList)

        val modelAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(
            this,
            R.array.modelList,
            android.R.layout.simple_spinner_dropdown_item
        )
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelList.adapter = modelAdapter
        modelList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                logicalName = parent.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }






        setPairedDevices()
        handleEvents()

        frag()
    }

    private fun frag() {
        findViewById<View>(R.id.buttonPrintText).setOnClickListener {
            printThe()
        }

        textData = findViewById<EditText>(R.id.editTextData)
        textData?.setText("test text")

        checkBold = findViewById<CheckBox>(R.id.checkBoxBold)
        checkUnderline = findViewById<CheckBox>(R.id.checkBoxUnderline)
        checkReverse = findViewById<CheckBox>(R.id.checkBoxReverse)

        deviceMessagesTextView = findViewById<TextView>(R.id.textViewDeviceMessages)
        deviceMessagesTextView?.setMovementMethod(ScrollingMovementMethod())
        deviceMessagesTextView?.setVerticalScrollBarEnabled(true)

        val textAlignment: Spinner = findViewById<Spinner>(R.id.textAlignment)
        val textFont: Spinner = findViewById<Spinner>(R.id.textFont)
        val textSize: Spinner = findViewById<Spinner>(R.id.textSize)

        val alignmentAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(
            this@MainActivity,
            R.array.Alignment,
            android.R.layout.simple_spinner_dropdown_item
        )
        alignmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textAlignment.adapter = alignmentAdapter
        textAlignment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                spinnerAlignment = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val fontAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(
            this@MainActivity,
            R.array.textFont,
            android.R.layout.simple_spinner_dropdown_item
        )
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textFont.adapter = fontAdapter
        textFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                spinnerFont = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val sizeAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(
            this@MainActivity,
            R.array.textSize,
            android.R.layout.simple_spinner_dropdown_item
        )
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textSize.adapter = sizeAdapter
        textSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                spinnerSize = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }

    private fun printThe() {
        val strData = textData!!.text.toString() /* + "\n"*/
        var attribute = 0

        val alignment: Int = when (spinnerAlignment) {
            0 -> BixolonPrinter.ALIGNMENT_LEFT
            1 -> BixolonPrinter.ALIGNMENT_CENTER
            2 -> BixolonPrinter.ALIGNMENT_RIGHT
            else -> BixolonPrinter.ALIGNMENT_LEFT
        }

        attribute = when (spinnerFont) {
            0 -> attribute or BixolonPrinter.ATTRIBUTE_FONT_A
            1 -> attribute or BixolonPrinter.ATTRIBUTE_FONT_B
            2 -> attribute or BixolonPrinter.ATTRIBUTE_FONT_C
            else -> attribute or BixolonPrinter.ATTRIBUTE_FONT_A
        }

        if (checkBold!!.isChecked) {
            attribute = attribute or BixolonPrinter.ATTRIBUTE_BOLD
        }

        if (checkUnderline!!.isChecked) {
            attribute = attribute or BixolonPrinter.ATTRIBUTE_UNDERLINE
        }

        if (checkReverse!!.isChecked) {
            attribute = attribute or BixolonPrinter.ATTRIBUTE_REVERSE
        }

        bxlPrinter.printText(
            strData, alignment, attribute,
            spinnerSize + 1
        )

        Toast.makeText(this, "trying to print", Toast.LENGTH_SHORT).show()
    }

    private fun permissionHandle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    requestPermissions(
                        arrayOf<String>(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ), 22
                    )
                }
            }
        }
    }

    private fun handleEvents() {

        binding?.btConnect?.setOnClickListener {
            printNow()
        }
    }

    private val mHandler = Handler { msg ->
        when (msg.what) {
            0 -> {
                binding?.mProgressLarge?.setVisibility(ProgressBar.VISIBLE)
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }

            1 -> {
                val data = msg.obj as String
                if (data != null && data.length > 0) {
                    Toast.makeText(applicationContext, data, Toast.LENGTH_LONG).show()
                }
                binding?.mProgressLarge?.setVisibility(ProgressBar.GONE)
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
        false
    }

    @SuppressLint("MissingPermission")
    private fun setPairedDevices() {
        bondedDevices.clear()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bondedDeviceSet = bluetoothAdapter.bondedDevices
        for (device in bondedDeviceSet) {
            bondedDevices.add(device.name + DEVICE_ADDRESS_START + device.address + DEVICE_ADDRESS_END)
        }
        logThis(" ${bondedDeviceSet.size} setPairedDevices: $bondedDevices")
        arrayAdapter?.notifyDataSetChanged()
    }

    private fun printNow() {
        mHandler.obtainMessage(0).sendToTarget()
        Thread {

            if (bxlPrinter.printerOpen(
                    portType,
                    logicalName,
                    address,
                    checkBoxAsyncMode
                )
            ) {
                finish()
            } else {
                mHandler.obtainMessage(1, 0, 0, "Fail to printer open!!").sendToTarget()
            }
        }.start()
    }


    @SuppressLint("MissingPermission")
    private fun stopBluetoothDiscovery(isReceiver: Boolean) {
        if (isReceiver) {
            try {
                applicationContext.unregisterReceiver(bluetoothReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && device.bondState == BluetoothDevice.BOND_NONE) {
                    val devName = device.name
                    val address = device.address
                    if (address.startsWith("74:F0:7D") || address.startsWith("40:19:20")) {
                        // Bixolon Printer
                        // 1. stop scan
                        stopBluetoothDiscovery(true)
                        // 2. Request pairing
                        try {
                            val method =
                                device.javaClass.getMethod("createBond", null)
                            method.invoke(device, null as Array<Any?>?)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else if (action == BluetoothDevice.ACTION_PAIRING_REQUEST) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val devName = device.name
                    val address = device.address
                    try {
                        val pin = "0000"
                        val pinBytes =
                            BluetoothDevice::class.java.getMethod(
                                "convertPinToBytes",
                                String::class.java
                            ).invoke(
                                BluetoothDevice::class.java, pin
                            ) as ByteArray
                        val m = device.javaClass.getMethod("setPin", ByteArray::class.java)
                        m.invoke(device, arrayOf(pinBytes))
                        device.javaClass.getMethod(
                            "setPairingConfirmation",
                            Boolean::class.javaPrimitiveType
                        ).invoke(device, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            22 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(applicationContext, "permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onItemClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) { //            setOnItemClickListener(this@MainActivity) on listVIew
        val device = (view as TextView).text.toString()

        address = device.substring(
            device.indexOf(DEVICE_ADDRESS_START) + DEVICE_ADDRESS_START.length,
            device.indexOf(DEVICE_ADDRESS_END)
        )

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP)
            binding?.listViewPairedDevices?.requestDisallowInterceptTouchEvent(
                false
            ) else binding?.listViewPairedDevices?.requestDisallowInterceptTouchEvent(true)
        return false
    }

}

fun logThis(m: Any?) {
    Log.d("logThis", "::: $m")
}