package com.myproject.smartbowl.fragment

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myproject.smartbowl.R
import com.myproject.smartbowl.databinding.FragmentCTRLBinding
import com.myproject.smartbowl.view.MainActivity
import com.myproject.smartbowl.viewModel.CTRLViewModel

class CTRLFragment : Fragment(), View.OnClickListener {
    private var _binding : FragmentCTRLBinding ?= null
    private val binding: FragmentCTRLBinding
        get() = _binding!!

    private lateinit var viewModel: CTRLViewModel

    // DB 레퍼런스 객체 생성
    private lateinit var ref: DatabaseReference

    val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){
        if(it.all {permission-> permission.value == true }){
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
        binding.btnMax.setOnClickListener(this)
        binding.btnMax.setOnClickListener(this)
        binding.btnMax.setOnClickListener(this)

        ref = FirebaseDatabase.getInstance().reference
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {

                    val waterDetectionString = snapshot.child("data").child("waterdetection").getValue(String::class.java)
                    val waterMeasurementString = snapshot.child("data").child("watermeasurement").getValue(String::class.java)

                    binding.tvWaterQuality.text = waterDetectionString
                    binding.tvWaterLevel.text = waterMeasurementString

                    val waterDetection: Int? = waterDetectionString?.toIntOrNull()
                    val waterMeasurement: Int? = waterMeasurementString?.toIntOrNull()

                    waterMeasurement?.let {
                        val message = when{
                            it >= 500 -> "비상!! 초비상 !! 물이 너무 많아요!!"
                            it <= 100 -> "비상!! 초비상 !! 물이 너무 적어요!!"
                            else -> return@let // 아무 작업 안하고 종료
                        }

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                            if(ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    "android.permission.POST_NOTIFICATIONS"
                                ) == PackageManager.PERMISSION_GRANTED){
                                noti(message)
                            }else{
                                permissionLauncher.launch(
                                    arrayOf(
                                        "android.permission.POST_NOTIFICATIONS"
                                    )
                                )
                            }
                        }else{
                            noti(message)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseException", "값을 읽어오는 중 오류 발생: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseException", "값을 읽어오는 데 실패했습니다.")
            }
        })
    }

    fun noti(message: String) {
        val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder: NotificationCompat.Builder
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            // 26 버전 이상
            val channelId = "bowlAlarm"
            val channelName = "My Bowl Alarm"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                .apply {
                    // 채널에 다양한 정보 설정
                    description = "My Channel One Description"
                    setShowBadge(true)
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

                // 채널을 이용하여 builder 생성
                builder = NotificationCompat.Builder(requireContext(), channelId)
        }else{
            builder = NotificationCompat.Builder(requireContext())
        }

        val intent = Intent(requireContext(), MainActivity::class.java)
        val pendingIntent: PendingIntent? = PendingIntent.getActivity(
            requireContext(), 10, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.run {
            setSmallIcon(R.drawable.noti_icon)
            setWhen(System.currentTimeMillis())
            setContentText(message)
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setContentIntent(pendingIntent)
        }

        // 알림 전송
        manager.notify(101, builder.build())
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_max -> {
                Log.d("clickListerner", "클릭")
                val updateData = mapOf("feeding/full" to 1)
                // 데이터베이스에 업데이트 적용
                ref.updateChildren(updateData)
                    .addOnSuccessListener {
                        // 업데이트 성공
                        Log.d("FirebaseUpdate", "Update successful")
                    }
                    .addOnFailureListener {
                        // 업데이트 실패
                        Log.e("FirebaseUpdate", "Update failed: ${it.message}")
                    }
            }

            R.id.btn_mean -> {
                val updateData = mapOf("feeding/half" to 1)
                // 데이터베이스에 업데이트 적용
                ref.updateChildren(updateData)
                    .addOnSuccessListener {
                        // 업데이트 성공
                        Log.d("FirebaseUpdate", "Update successful")
                    }
                    .addOnFailureListener {
                        // 업데이트 실패
                        Log.e("FirebaseUpdate", "Update failed: ${it.message}")
                    }
            }

            R.id.btn_min -> {
                val updateData = mapOf("feeding/little" to 1)
                // 데이터베이스에 업데이트 적용
                ref.updateChildren(updateData)
                    .addOnSuccessListener {
                        // 업데이트 성공
                        Log.d("FirebaseUpdate", "Update successful")
                    }
                    .addOnFailureListener {
                        // 업데이트 실패
                        Log.e("FirebaseUpdate", "Update failed: ${it.message}")
                    }
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}