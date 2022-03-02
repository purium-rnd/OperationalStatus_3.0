package com.example.test

//import android.support.v4.content.ContextCompat
//import android.support.v7.app.AppCompatActivity

import android.R.attr.*
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.view.animation.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.test.LogUtil.e
import com.example.test.serial.UsbService
import com.example.test.serverApi.*
import com.github.mikephil.charting.charts.PieChart
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Field
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity(),ChangedTimeListener {
    // 2019.09.10 네오카텍 app 에서 보낸 action 값
    val ACTION_DATA = "neocartek.intent.action.DEVICE_DATA"

    var mTimeChangeReciver:TimeChangeReceiver? = null
    var timeIntentFilter: IntentFilter = IntentFilter()
    var mUiHandler: Handler = Handler(Looper.getMainLooper())
    var mSi:String = ""
    var mDo:String = ""
    var mDataBroadcastReceiver:DataBroadcastReceiver? = null
    var dataIntentFilter: IntentFilter = IntentFilter()

    private val PERMISSIONS_ACCESS_FINE_LOCATION = 1000
    private val PERMISSIONS_ACCESS_COARSE_LOCATION = 1001
    private val GPS_ENABLE_REQUEST_CODE = 2001
    private val WIFI_ENABLE_REQUEST_CODE = 2002

    private var isAccessFineLocation = false
    private var isAccessCoarseLocation = false
    private var isPermission = false
    private var mInputFiveClickGotoWifiSetting:InputFiveClickGotoWifiSetting = InputFiveClickGotoWifiSetting()

    private var usbService: UsbService? = null
    private var mHandler: MyHandler? = null

    lateinit var mPm10Chart:PieChart
    lateinit var mPm25Chart:PieChart
    var pIntent: PendingIntent? = null

    var isWating:Boolean = true
    var isDemo:Boolean = false

    var isRemove:Boolean = false    // 미세먼지 제거 중인가
    var bgNum:Int = 0
    private var isScoreImageFirst:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        var visibility = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = visibility.xor(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    xor View.SYSTEM_UI_FLAG_FULLSCREEN
                    xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_main)

        mHandler = MyHandler(this)
        //http://openapi.airkorea.or.kr/openapi/services/rest/ArpltnInforInqireSvc/getCtprvnMesureSidoLIst?serviceKey=OD2%2BtlZgxfskdgqzmT53GlLveUn58CX2m1TP8DIx8E2xLsS8Zh3MElKwcz0OeIh1DBPw%2FReN8Vxtr4x%2F5YUEbA%3D%3D&numOfRows=25&pageNo=1&sidoName=%EC%84%9C%EC%9A%B8&searchCondition=HOUR
        //serviceKey =
        //미세먼지 기준 0~30 좋음 , 31~80 보통, 81~150 나쁨, 151~ 매우나쁨
        //초미세먼지 기준 0~15 좋음 16~35 보통 36~75 나쁨 76~ 매우나쁨

        //tv_cur_time_hour.text = getCurrentHour(System.currentTimeMillis())
        //tv_cur_time_min.text = getCurrentMinute(System.currentTimeMillis())
        //tv_cur_time_am_pm.text = getCurrentAmPm(System.currentTimeMillis())
        tv_cur_time.text = getCurrentTime(System.currentTimeMillis())
        tv_cur_date.text = getCurrentDate(System.currentTimeMillis())

        img_logo.setOnClickListener {
            //removeVideo()
            // 22.02.23 : 로고 클릭시 매니저 앱 실행 (제품 테스트 용)
            //var intent = packageManager.getLaunchIntentForPackage("com.neocartek.purium.manager")
            //startActivity(intent)
            if (checkPackageInstalled("com.neocartek.purium.manager")) {
                val runtime = Runtime.getRuntime()
                try {
                    val cmd = "am start com.neocartek.purium.manager/.MainActivity"
                    val process = runtime.exec(cmd)
                    process.errorStream.close()
                    process.inputStream.close()
                    process.outputStream.close()
                } catch (e: Exception) {
                    e.fillInStackTrace()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MyApplication.prefs.load()
        //tv_subtitle.setSingleLine(true)
        //tv_subtitle.ellipsize = TextUtils.TruncateAt.MARQUEE
        tv_subtitle.startScroll()
        tv_subtitle.setTextColor(Color.WHITE)
        tv_subtitle.text = MyApplication.prefs.sub_title
        tv_subtitle.isSelected = true

        var anim = AnimationUtils.loadAnimation(this, R.anim.edgelight_alpha_anim)
        iv_5_in_one.startAnimation(anim)
        tv_score.startAnimation(anim)

        var anim2 = AnimationUtils.loadAnimation(this, R.anim.alpha_hide)
        var anim3 = AnimationUtils.loadAnimation(this, R.anim.alpha_show)
        var anim4 = AnimationUtils.loadAnimation(this, R.anim.alpha_hide2)
        var anim5 = AnimationUtils.loadAnimation(this, R.anim.alpha_show2)
        var anim6 = AnimationUtils.loadAnimation(this, R.anim.alpha_hide3)
        var anim7 = AnimationUtils.loadAnimation(this, R.anim.alpha_show3)
        var anim8 = AnimationUtils.loadAnimation(this, R.anim.alpha_hide4)
        var anim9 = AnimationUtils.loadAnimation(this, R.anim.alpha_show4)
        iv_phytoncide.startAnimation(anim2)
        //tv_phytoncide.startAnimation(anim3)

        iv_antibacterial.startAnimation(anim2)
        //tv_antibacterial.startAnimation(anim3)

        iv_air_purification.startAnimation(anim2)
        //tv_air_purification.startAnimation(anim3)

        iv_air_deodorization.startAnimation(anim2)
        //tv_air_deodorization.startAnimation(anim3)

        iv_ambient_light_left.startAnimation(anim3)
        iv_ambient_light_right.startAnimation(anim3)

        /*
        var count = 0
        timer(period = 2000) {
            count++
            if (count == 1) {
                iv_antibacterial.startAnimation(anim2)
            }
            if (count == 2) {
                iv_phytoncide.startAnimation(anim4)
            }
            if (count == 3) {
                iv_air_purification.startAnimation(anim6)
            }
            if (count == 4) {
                iv_air_deodorization.startAnimation(anim8)
                cancel()
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        initVideoView()


        mTimeChangeReciver = TimeChangeReceiver(this@MainActivity)
        timeIntentFilter.addAction(Intent.ACTION_TIME_TICK)
        registerReceiver(mTimeChangeReciver, timeIntentFilter)


        // 2019.09.10 네오카텍 app 에서 보낸 센서 data 값을 받기 위한 브로드캐스트 등록
        mDataBroadcastReceiver = DataBroadcastReceiver(this@MainActivity)
        dataIntentFilter.addAction(ACTION_DATA)
        registerReceiver(mDataBroadcastReceiver, dataIntentFilter)

    }

    override fun timeChanged2(hour: String, min: String) {
        //tv_cur_time_hour.text = hour
        //tv_cur_time_min.text = min

        //tv_cur_time_am_pm.text = getCurrentAmPm(System.currentTimeMillis())
        tv_cur_time.text = getCurrentTime(System.currentTimeMillis())
        tv_cur_date.text = getCurrentDate(System.currentTimeMillis())
    }

    override fun timeChanged(time: String) {
        Log.d("by_debug", "timeChanged!!")
        //tv_cur_time.text = time
        //tv_cur_date_address.text = getNowFullDate(System.currentTimeMillis())
    }



    private fun callPermission():Boolean {

        // Check the SDK version and whether the permission is already granted or not.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_ACCESS_FINE_LOCATION
            );
            return false
        }
//        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                && ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED){
//            requestPermissions(
//                    arrayOf(),
//                    PERMISSIONS_ACCESS_COARSE_LOCATION);
//
//        }
        else {
//            isPermission = true;
//            loadWeather()
            return true
        }
    }

    override fun onPause() {
        super.onPause()
        if(mTimeChangeReciver != null)
            unregisterReceiver(mTimeChangeReciver)

        if(mDataBroadcastReceiver != null)
            unregisterReceiver(mDataBroadcastReceiver)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    data class SidoInfo(var sidoName: String, var guOrSiName: String, var totalCount: Int)


    private val timeFormat: SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private val hourFormat: SimpleDateFormat = SimpleDateFormat("hh", Locale.ENGLISH)
    private val minFormat: SimpleDateFormat = SimpleDateFormat("mm", Locale.ENGLISH)
    private val aFormat: SimpleDateFormat = SimpleDateFormat("a", Locale.ENGLISH)
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy. MM. dd", Locale.ENGLISH)

    fun getCurrentTime(time: Long):String{
        return timeFormat.format(Date(time))
    }

    fun getCurrentHour(time: Long):String {
        return hourFormat.format(Date(time))
    }

    fun getCurrentMinute(time: Long):String {
        return minFormat.format(Date(time))
    }

    fun getCurrentAmPm(time: Long):String {
        return aFormat.format(Date(time))
    }

    fun getCurrentDate(time: Long):String {
        return dateFormat.format(Date(time))
    }


    class TimeChangeReceiver : BroadcastReceiver{
        private val timeFormat: SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        private val hourFormat: SimpleDateFormat = SimpleDateFormat("hh", Locale.ENGLISH)
        private val minFormat: SimpleDateFormat = SimpleDateFormat("mm", Locale.ENGLISH)
        private val aFormat: SimpleDateFormat = SimpleDateFormat("a", Locale.ENGLISH)
        private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy. MM. dd", Locale.ENGLISH)

        var changedListener:ChangedTimeListener? = null

        constructor(listener: ChangedTimeListener){
            changedListener = listener
        }

        fun getCurrentTime(time: Long):String{
            return timeFormat.format(Date(time))
        }

        fun getCurrentHour(time: Long):String {
            return hourFormat.format(Date(time))
        }

        fun getCurrentMinute(time: Long):String {
            return minFormat.format(Date(time))
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent!!.action

            when(action) {
                Intent.ACTION_TIME_TICK -> {
                    Log.d("by_debug", "ACTION_TIME_TICK!!")
                    //changedListener!!.timeChanged(getCurrentTime(System.currentTimeMillis()))
                    changedListener!!.timeChanged2(
                        getCurrentHour(System.currentTimeMillis()), getCurrentMinute(
                            System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private class MyHandler(activity: MainActivity) : Handler() {
        private val mActivity: MainActivity
        public var isRunningVideo = false
        init {
            mActivity = activity
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    Log.d("by_debug", "data = $data")
                    if (data == "F") {
                        if (!isRunningVideo) {
                            isRunningVideo = true
                            mActivity.removeVideo()
                        }
                    } else {

                    }
                }
            }
        }
    }

    private fun pm10ViewUpdate(data: Int) {
        tv_pm10.text = data.toString()
        if (data < 31) { // 0 ~ 30
            iv_pm10.background = getDrawable(R.drawable.pm10_verygood_1394_424)
        }
        else if (data > 30 && data < 81) { // 31 ~ 80
            iv_pm10.background = getDrawable(R.drawable.pm10_good_1394_424)
        }
        else if (data > 80 && data < 151) { // 81 ~ 150
            iv_pm10.background = getDrawable(R.drawable.pm10_normal_1394_424)
        }
        else { // 151 이상
            iv_pm10.background = getDrawable(R.drawable.pm10_bad_1394_424)
        }
    }

    private fun pm25ViewUpdate(data: Int) {
        tv_pm25.text = data.toString()

        if (data < 16) { // 0 ~ 15
            iv_pm25.background = getDrawable(R.drawable.pm25_verygood_1394_252)
        }
        else if (data > 15 && data < 36) { // 16 ~ 35
            iv_pm25.background = getDrawable(R.drawable.pm25_good_1394_252)
        }
        else if (data > 35 && data < 76) { // 36 ~ 75
            iv_pm25.background = getDrawable(R.drawable.pm25_normal_1394_252)
        }
        else { // 76 이상
            iv_pm25.background = getDrawable(R.drawable.pm25_bad_1394_252)
        }

        if (data < 26) {
            //iv_score.background = getDrawable(R.drawable.score_verygood_1386_78)

            if (isScoreImageFirst) {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_verygood_2)
            } else {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_verygood_1)
            }
            isScoreImageFirst = !isScoreImageFirst
            var score = 100 - data
            tv_score.text = score.toString()

        }
        else if (data > 25 && data < 51) {
            //iv_score.background = getDrawable(R.drawable.score_good_1386_78)

            if (isScoreImageFirst) {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_good_2)
            } else {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_good_1)
            }
            isScoreImageFirst = !isScoreImageFirst
            var score = 100 - data
            tv_score.text = score.toString()
        }
        else if (data > 50 && data < 76) {
            //iv_score.background = getDrawable(R.drawable.score_normal_1386_78)

            if (isScoreImageFirst) {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_normal_2)
            } else {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_normal_1)
            }
            isScoreImageFirst = !isScoreImageFirst
            var score = 100 - data
            tv_score.text = score.toString()
        }
        else {
            //iv_score.background = getDrawable(R.drawable.score_bad_1386_78)

            if (isScoreImageFirst) {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_bad_1)
            } else {
                iv_5_in_one.background = getDrawable(R.drawable.five_in_one_bad_1)
            }
            isScoreImageFirst = !isScoreImageFirst
            var score = 100 - data
            tv_score.text = score.toString()
            if (data > 100) {
                tv_score.text = "0"
            }

        }
    }

    private fun tempViewUpdate(data: Int) {
        tv_temp.text = data.toString()
    }

    private fun humiViewUpdate(data: Int) {
        tv_humi.text = data.toString()
    }

    private  fun co2ViewUpdate(data: Int) {
        tv_co2.text = data.toString()

        if (data < 451) { // 0 ~ 450
            tv_co2.textSize = resources.getDimension(R.dimen.voc_text_size_big)
            iv_co2.background = getDrawable(R.drawable.co2_verygood_1400_612)
        }
        else if (data > 450 && data < 1000) { // 450 ~ 1000
            tv_co2.textSize = resources.getDimension(R.dimen.voc_text_size_big)
            iv_co2.background = getDrawable(R.drawable.co2_good_1400_612)
        }
        else if (data > 999 && data < 2001) { // 1000 ~ 2000
            tv_co2.textSize = resources.getDimension(R.dimen.co2_text_size_small)
            iv_co2.background = getDrawable(R.drawable.co2_normal_1400_612)
        }
        else { // 2000 이상
            tv_co2.textSize = resources.getDimension(R.dimen.co2_text_size_small)
            iv_co2.background = getDrawable(R.drawable.co2_bad_1400_612)
        }
    }

    private fun vocViewUpdate(data: Int) {

        if (data == 0) {
            tv_voc.textSize = resources.getDimension(R.dimen.voc_text_size_big)
            tv_voc.text = getString(R.string.low)
            iv_voc.background = getDrawable(R.drawable.voc_verygood_1400_794)
        }
        else if (data == 1) {
            tv_voc.textSize = resources.getDimension(R.dimen.voc_text_size_big)
            tv_voc.text = getString(R.string.acceptable)
            iv_voc.background = getDrawable(R.drawable.voc_good_1400_794)
        }
        else if (data == 2) {
            tv_voc.textSize = resources.getDimension(R.dimen.voc_text_size_big)
            tv_voc.text = getString(R.string.marginal)
            iv_voc.background = getDrawable(R.drawable.voc_normal_1400_794)
        }
        else {
            tv_voc.textSize = resources.getDimension(R.dimen.voc_text_size_small)
            tv_voc.text = getString(R.string.high)
            iv_voc.background = getDrawable(R.drawable.voc_bad_1400_794)
        }
    }


    // 미세먼지 제거 대기중
    /*
    private fun pmRemoveReady() {
        //iv_fan_wing.visibility = View.INVISIBLE
        tv_air_shower_value.text = "0"
        tv_center_text.text = getString(R.string.waiting)
    }
    
    // 미세먼지 제거 시작
    private fun pmRemoveStart() {
        iv_fan_center.clearAnimation()
        val anim_fan = AnimationUtils.loadAnimation(this, R.anim.rotation_air_shower)
        anim_fan.setInterpolator(this, android.R.anim.linear_interpolator)
        iv_fan_center.startAnimation(anim_fan)

        iv_center_effect.visibility = View.VISIBLE
        val anim_scale = AnimationUtils.loadAnimation(this, R.anim.scale_air_shower)
        anim_scale.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                iv_center_effect.visibility = View.INVISIBLE
                iv_center_effect.clearAnimation()
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        iv_center_effect.startAnimation(anim_scale)
        //iv_fan_wing.visibility = View.VISIBLE
        //var anim = AnimationUtils.loadAnimation(this, R.anim.rotation_air_shower)
        //anim.setInterpolator(this, android.R.anim.linear_interpolator)
        //iv_fan_wing.startAnimation(anim)

        val mediaPlayer:MediaPlayer = MediaPlayer.create(this, R.raw.sound_pm_removing)
        mediaPlayer.start()



        var count = 0
        timer(period = 100) {
            count++
            if (count == 100) { // 100% 에서 stop
                cancel()
            }
            runOnUiThread {
                if (count == 1) {
                }
                tv_air_shower_value.text = count.toString()
                tv_center_text.text = getString(R.string.air_shower)

                if (count == 100) {
                    mediaPlayer.release()
                    val mediaPlayer2:MediaPlayer = MediaPlayer.create(
                        this@MainActivity,
                        R.raw.sound_pm_remove_complete
                    )
                    mediaPlayer2.start()

                    isRemove = false

                    val anim:Animation = AnimationUtils.loadAnimation(
                        applicationContext,
                        R.anim.complete_anim
                    )
                    anim.duration = 1000
                    anim.repeatCount = 3
                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {
                            Log.d("main", "complete anim start")
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            Log.d("main", "complete anim end")
                            mDataBroadcastReceiver!!.isRunningVideo = false
                            //iv_fan_wing.clearAnimation()

                            iv_fan_center.clearAnimation()

                            val anim = AnimationUtils.loadAnimation(
                                this@MainActivity,
                                R.anim.rotation_white_effect
                            )
                            anim.setInterpolator(
                                this@MainActivity,
                                android.R.anim.linear_interpolator
                            )
                            iv_fan_center.startAnimation(anim)

                            pmRemoveReady()
                            mediaPlayer2.release()
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                        }

                    })

                    tv_center_text.startAnimation(anim)
                    tv_center_text.text = getString(R.string.complete)

                    return@runOnUiThread
                }
            }
        }
    }*/

    // 2019.09.10 네오카텍 app 에서 보낸 센서 data 값을 브로드캐스트로 받은 후 값이 0보다 크면 remove 동영상 재생
    class DataBroadcastReceiver(activity: MainActivity) : BroadcastReceiver(){
        private val mActivity: MainActivity
        public var isRunningVideo = false
        init {
            mActivity = activity
        }

        override fun onReceive(context: Context, intent: Intent) {
            var action = intent!!.action

            when(action) {
                mActivity.ACTION_DATA -> {
                    val subTitleData = intent.getStringExtra("sub_title")
                    if (subTitleData != null) {
                        if (subTitleData.length > 0) {
                            MyApplication.prefs.sub_title = subTitleData
                            mActivity.tv_subtitle.text = MyApplication.prefs.sub_title
                            MyApplication.prefs.save()
                            mActivity.tv_subtitle.pauseScroll()
                            mActivity.tv_subtitle.resumeScroll()
                        }
                    }
                    val data = intent.getIntExtra("sensor", 0)
                    Log.d("by_debug", "receive data $data")
                    if (data > 0) {
                        if (!isRunningVideo) {
                            isRunningVideo = true

                            mActivity.removeVideo()
                        }
                    }

                    val timeData = intent.getStringExtra("info_date")
                    if (timeData != null) {
                        if (timeData.length > 0) {
                            //mActivity.timeChanged(mActivity.getCurrentTime(System.currentTimeMillis()))
                            mActivity.timeChanged2(
                                mActivity.getCurrentHour(System.currentTimeMillis()),
                                mActivity.getCurrentMinute(
                                    System.currentTimeMillis()
                                )
                            )
                            mActivity.waitVideo()

                        }
                    }

                    val tempData = intent.getIntExtra("info_temp", -1)
                    if (tempData != -1) {
                        //val temp:Int = tempData.toInt()
                        Log.d("by_debug", "air quality temp data $tempData")
                        mActivity.tempViewUpdate(tempData)
                    }

                    val humiData = intent.getIntExtra("info_humi", -1)
                    if (humiData != -1) {
                        //val humi:Int = humiData.toInt()
                        Log.d("by_debug", "air quality humi data $humiData")
                        mActivity.humiViewUpdate(humiData)
                    }

                    val vocData = intent.getIntExtra("info_voc", -1)
                    if (vocData != -1) {
                        //val voc:Int = vocData.toInt()
                        Log.d("by_debug", "air quality voc data $vocData")
                        mActivity.vocViewUpdate(vocData)
                    }

                    val co2Data = intent.getIntExtra("info_co2", -1)
                    if (co2Data != -1) {
                        //val co2:Int = co2Data.toInt()
                        Log.d("by_debug", "air quality co2 data $co2Data")
                        mActivity.co2ViewUpdate(co2Data)
                    }

                    val pm25Data = intent.getIntExtra("info_pm25", -1)
                    if (pm25Data != -1) {
                        //val pm25:Int = pm25Data.toInt()
                        Log.d("by_debug", "air quality pm25 data $pm25Data")
                        mActivity.pm25ViewUpdate(pm25Data)
                    }

                    val pm10Data = intent.getIntExtra("info_pm10", -1)
                    if (pm10Data != -1) {
                        //val pm10:Int = pm10Data.toInt()
                        Log.d("by_debug", "air quality pm10 data $pm10Data")
                        mActivity.pm10ViewUpdate(pm10Data)
                    }

                    val pm1_0Data = intent.getIntExtra("info_pm1_0", -1)
                    if (pm1_0Data != -1) {
                        Log.d("by_debug", "air quality pm1_0 data $pm1_0Data")
                        //mActivity.pm10ViewUpdate(pm10Data)
                    }
                }
            }
        }
    }

    private fun getIpAccess(useIPv4: Boolean): String? {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(
                                    0,
                                    delim
                                ).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        } // for now eat exceptions

        return ""
    }

    private fun initVideoView(){
        val videoRootPath = "android.resource://$packageName/"
        video_view.setVideoURI(Uri.parse(videoRootPath + R.raw.wait))
        if(!isDemo){
            video_view.setOnCompletionListener({ mp ->
                if (isWating) {
                    video_view.start()
                } else {
                    video_view.pause()
                    video_view.setVideoURI(Uri.parse(videoRootPath + R.raw.wait))
                    video_view.seekTo(0)
                    video_view.start()
                }
            })
            video_view.setOnErrorListener({ mp, what, extra ->
                Log.e("by_debug", "video error")
                true
            })
        }else{
            video_view.setOnCompletionListener({ mp ->
                if (isWating) {
                    video_view.pause()
                    video_view.setVideoURI(Uri.parse(videoRootPath + R.raw.remove))
                    video_view.seekTo(0)
                    video_view.start()
                } else {
                    video_view.pause()
                    video_view.setVideoURI(Uri.parse(videoRootPath + R.raw.wait))
                    video_view.seekTo(0)
                    video_view.start()
                }
                isWating = !isWating
            })
        }

        video_view.start()
    }

    private fun waitVideo() {
        val videoRootPath = "android.resource://$packageName/"
        video_view.pause()
        video_view.setVideoURI(Uri.parse(videoRootPath + R.raw.wait))
        video_view.seekTo(0)
        video_view.start()
        video_view.setOnErrorListener({ mp, what, extra ->
            Log.e("by_debug", "wait video error")
            true
        })
    }

    private fun removeVideo() {
        val mediaPlayer:MediaPlayer = MediaPlayer.create(this, R.raw.sound_pm_removing)
        mediaPlayer.start()
        val mediaPlayer2:MediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.sound_pm_remove_complete)

        var count = 0
        timer(period = 1000) {
            count++
            if (count == 11) {
                cancel()
                runOnUiThread {
                    mediaPlayer.release()
                    mediaPlayer2.start()
                }
            }
        }

        val videoRootPath = "android.resource://$packageName/"
        video_view.pause()
        video_view.setVideoURI(Uri.parse(videoRootPath + R.raw.remove))
        video_view.seekTo(0)
        video_view.start()
        video_view.setOnCompletionListener({ mp ->
            mDataBroadcastReceiver!!.isRunningVideo = false
            waitVideo()
            mediaPlayer2.release()
        })
        video_view.setOnErrorListener({ mp, what, extra ->
            Log.e("by_debug", "remove video error")
            true
        })
        /*
        video_view.setOnCompletionListener({ mp ->
            mHandler!!.isRunningVideo = false
            waitVideo()
        })*/

        /*
        Handler().postDelayed({
            waitVideo()
        }, 8000)*/
    }

    /**
     * 실행할 패키지가 존재하는지 체크하는 Method
     * @param packageName, 패키지 이름
     * @return 패키지여부 Boolean
     */
    private fun checkPackageInstalled(packageName: String): Boolean {
        try {
            val pm = this.packageManager
            val pi = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = pi.applicationInfo
            Log.e("checkPackage", "Package $packageName Installed")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("checkPackage", "Package $packageName Not Installed")
            // 다이얼로그
            val builder =
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_AppCompat_Light_Dialog))
            builder.setTitle("Cannot Find Package")
                .setMessage("Package Manager could not find package : $packageName")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.show()
            return false
        } catch (ne: NullPointerException) {
            Log.e("checkPackage", "Exception with Context")
            return false
        }
        return true
    }
}
