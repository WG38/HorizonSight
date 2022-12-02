package com.example.horizonsight

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SettingsActivity : AppCompatActivity() {

    private lateinit var acceptInput : Button
    lateinit var rayGetTarget : EditText
    lateinit var sampleGetTarget : EditText
    lateinit var startAngleGetTarget : EditText
    lateinit var endAngleGetTarget : EditText
    lateinit var artificialHorizonSwitch : Switch
    lateinit var artificialHorizon : EditText
    lateinit var artificialLocationSwitch : Switch
    lateinit var artificialLatitude : EditText
    lateinit var artificialLongitude : EditText
    var ray_str = "15"
    var sample_str = "10"
    var start_angle_str = "0.0"
    var end_angle_str = "360.0"
    var artf_horizon_check = "False"
    var artf_horizon_distance_str = "10.0"
    var artf_loc_check = "False"
    var artf_lat = "37.77923740957507"
    var artf_lon = "-122.4193054602799"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        acceptInput = findViewById(R.id.exitButton)
        acceptInput.setOnClickListener() {
            rayGetTarget = findViewById<View>(R.id.rayDensity) as EditText
            sampleGetTarget = findViewById<View>(R.id.sampleDensity) as EditText
            startAngleGetTarget = findViewById<View>(R.id.startAngle) as EditText
            endAngleGetTarget = findViewById<View>(R.id.endAngle) as EditText
            artificialHorizonSwitch = findViewById<View>(R.id.artfHozSwitch) as Switch
            artificialLocationSwitch = findViewById<View>(R.id.artifLocSwitch) as Switch


            ray_str = rayGetTarget.text.toString()
            sample_str = sampleGetTarget.text.toString()
            start_angle_str = startAngleGetTarget.text.toString()
            end_angle_str = endAngleGetTarget.text.toString()

            //if user checked an artifical horizon then its value is passed through intent
            if (artificialHorizonSwitch.isChecked) {
                artificialHorizon = findViewById(R.id.artifHoz)
                artf_horizon_distance_str = artificialHorizon.text.toString()
                artf_horizon_check = "true"
            }
            if (artificialLocationSwitch.isChecked) {
                artificialLatitude = findViewById(R.id.artifLat) as EditText
                artificialLongitude = findViewById(R.id.artifLon) as EditText
                artf_lat = artificialLatitude.text.toString()
                artf_lon = artificialLongitude.text.toString()
                artf_loc_check = "true"

            }

            Log.d("FUCK71","$artf_lat $artf_lon")
            val intent = Intent(this,MainActivity::class.java)
            intent.putExtra("ray_value",ray_str)
            intent.putExtra("sample_value",sample_str)
            intent.putExtra("start_angle",start_angle_str)
            intent.putExtra("end_angle",end_angle_str)
            intent.putExtra("artf_horizon",artf_horizon_distance_str)
            intent.putExtra("artf_horizon_check",artf_horizon_check)
            intent.putExtra("artf_lat",artf_lat)
            intent.putExtra("artf_lon",artf_lon)
            intent.putExtra("artf_loc_check",artf_loc_check)



            startActivity(intent)
        }


    }

}