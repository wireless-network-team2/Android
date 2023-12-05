package com.myproject.smartbowl.fragment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myproject.smartbowl.R
import com.myproject.smartbowl.databinding.FragmentCTRLBinding
import com.myproject.smartbowl.view.MainActivity

class CTRLFragment : Fragment(), View.OnClickListener {
    private val WATER_DETECTION_CHANNEL_ID = "waterDetectionChannel"
    private val WATER_MEASUREMENT_CHANNEL_ID = "waterMeasurementChannel"

    private var _binding : FragmentCTRLBinding ?= null
    private val binding: FragmentCTRLBinding
        get() = _binding!!

    // DB 참조 객체 생성
    private lateinit var ref: DatabaseReference

    // 알림 권한 확인
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){
        if(it.all {permission-> permission.value }){
            Toast.makeText(requireContext(), "알림 권한이 허용 되었습니다.", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(requireContext(), "알림 권한이 거부 되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCTRLBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 클릭 이벤트 등록
        binding.btnMax.setOnClickListener(this)
        binding.btnMean.setOnClickListener(this)
        binding.btnMin.setOnClickListener(this)

        /**
         * google-service.json에 등록된
         * "firebase_url": "https://smartbowl-b05d9-default-rtdb.firebaseio.com" 호출
        **/
        ref = FirebaseDatabase.getInstance().reference

        // firebase에 Data를 넣거나 가져올 때 동작
        ref.addValueEventListener(object : ValueEventListener {
            // 초기값 호출,데이터가 변경될 때 동작
            override fun onDataChange(snapshot: DataSnapshot) {
                // snapshot : { value = {data={
                // watermeasurement=499,
                // waterdetection=15},
                // feeding={half=0, full=1, little=0}} }
                // 파이어베이스에 저장된 모든 값을 읽어옴
                try {
                    // 수질의 값을 가져옴
                    val waterDetectionString = snapshot.child("data").child("waterdetection").getValue(String::class.java)
                    // 수위의 값을 가져옴
                    val waterMeasurementString = snapshot.child("data").child("watermeasurement").getValue(String::class.java)

                    // UI에 수질 set
                    binding.tvWaterQuality.text = waterDetectionString
                    // UI에 수위 set
                    binding.tvWaterLevel.text = waterMeasurementString

                    // 수질, 수위값을 정수로 변경
                    val waterDetection: Int? = waterDetectionString?.toIntOrNull()
                    val waterMeasurement: Int? = waterMeasurementString?.toIntOrNull()

                    // 수질, 수위값이 기준치에 맞는지 확인
                    checkWaterStatus("waterDetection", waterDetection)
                    checkWaterStatus("waterMeasurement", waterMeasurement)
                } catch (e: Exception) {
                    Log.e("FirebaseException", "값을 읽어오는 중 오류 발생: ${e.message}")
                }
            }

            // Data 삽입 실패시 동작
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseException", "값을 읽어오는데 실패했습니다.")
            }
        })
    }

    // 수질 경고 메시지
    private fun createWaterDetectionAlarmMessage(value: Int): String {
        return when {
            value > 9 || value < 5 -> "비상!! 초비상 !! 물을 바꿔 주세요"
            else -> ""
        }
    }

    // 수위 경고 메시지
    private fun createWaterMeasurementAlarmMessage(value: Int): String {
        return when {
            value >= 500 -> "비상!! 초비상 !! 물이 너무 많아요!!"
            value <= 100 -> "비상!! 초비상 !! 물이 너무 적어요!!"
            else -> ""
        }
    }

    // 수질, 수위 값이 기준치에 맞는지 확인하는 함수
    // type: 수질, 수위
    // value: 측정값
    private fun checkWaterStatus(type: String, value: Int?) {

        value?.let {

            // 1. msg = type(수질, 수위)에 따라 알람에 보내줄 텍스트 생성
            val msg = when (type) {
                "waterDetection" -> createWaterDetectionAlarmMessage(it)
                "waterMeasurement" -> createWaterMeasurementAlarmMessage(it)
                else -> ""
            }

            // 2. 메시지가 있으면
            if (msg.isNotEmpty()) {

                // 3. 채널 ID 생성
                val channelId = when (type) {
                    "waterDetection" -> WATER_DETECTION_CHANNEL_ID
                    "waterMeasurement" -> WATER_MEASUREMENT_CHANNEL_ID
                    else -> ""
                }
                // 4. 알림 생성
                if (channelId.isNotEmpty()) {
                    handleNotifications(msg, channelId)
                }
            }
        }
    }

    //
    private fun handleNotifications(message: String, channelId: String) {
        // 안드로이드는 Android SDK 버전이 26 이상인 경우와 이하인 경우 알림 전송 방법이 다르다.
        // 26버전 이상에서는 알림을 위한 채널을 생성 해줘야 한다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 알람 권한이 허용되었는지 확인
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    "android.permission.POST_NOTIFICATIONS"
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // 권한이 허용되었으면 알림 생성
                sendNotification(message, channelId)
            } else {
                // 권한이 허용되지 않았으면 알림 허용 요청 전송
                permissionLauncher.launch(arrayOf("android.permission.POST_NOTIFICATIONS"))
            }
        } else {
            sendNotification(message, channelId)
        }
    }

    //
    private fun sendNotification(message: String, channelId: String) {
        // 알람을 관리하는 manager란 아이를 android system에서 호출
        val manager = requireContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 알림 생성을 위한 builder
        val builder: NotificationCompat.Builder = createNotificationBuilder(channelId)

        // 알람 클릭시 어플로 이동하기 위한 구문
        val intent = Intent(requireContext(), MainActivity::class.java)
        val pendingIntent: PendingIntent? =
            PendingIntent.getActivity(requireContext(), 10, intent, PendingIntent.FLAG_UPDATE_CURRENT)


        builder.apply {
            setContentText(message)// 전송하고 싶은 메시지
            setContentIntent(pendingIntent) // 알람 클릭시 실행고 싶은 어플
        }

        manager.notify(101, builder.build()) // 알람 전송!
    }


    // Notification Builder 객체 반환
    private fun createNotificationBuilder(channelId:String): NotificationCompat.Builder{
        val channelName = "My Bowl Alarm" // 알람 채널 anme
        val manager = requireContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "My Channel One Description"
                setShowBadge(true) // 알람이 쌓일 경우 개수를 보여줌

                // 알람 소리를 생성하기 위함
                val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(uri, audioAttributes)
                enableVibration(true) //진동
            }
            // 채널을 NotificationManager에 등록
            manager.createNotificationChannel(channel)

            // 채널을 이용 하여 builder 생성
            NotificationCompat.Builder(requireContext(), channelId)
        }else{
            NotificationCompat.Builder(requireContext())
        }.apply {
            setSmallIcon(R.drawable.noti_icon) // 알람 아이콘
            setWhen(System.currentTimeMillis()) // 알람 전송 시간
            priority = NotificationCompat.PRIORITY_DEFAULT // 알람 중요도
                                                           // PRIORITY_DEFAULT: 우리가 사용하는 일반적인 알람
        }
    }
    // Button Click Listener
    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_max -> onFeedingButtonClick("full")
            R.id.btn_mean -> onFeedingButtonClick("half")
            R.id.btn_min -> onFeedingButtonClick("little")
        }
    }

    private fun onFeedingButtonClick(feedType: String){
        val updateData = mapOf("feeding/$feedType" to 1) // little / hadf / full
        ref.updateChildren(updateData).addOnSuccessListener {
            Log.d("FirebaseUpdate", "업데이트 성공")
        }
        .addOnFailureListener {
            Log.e("FirebaseUpdate", "업데이트 실패: ${it.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}