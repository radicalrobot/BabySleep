package com.example.babysleep

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.core.content.ContextCompat.getSystemService
import java.util.*
import kotlin.concurrent.schedule

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), AdapterView.OnItemSelectedListener {

    lateinit var player: MediaPlayer
    lateinit var fadeInTimer: CountDownTimer
    lateinit var fadeOutTimer: CountDownTimer
    lateinit var delayTimer: CountDownTimer
    lateinit var durationTimer: CountDownTimer
    var enabled = false

    final val mMAX_VOLUME: Float = 1.0f

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        player = MediaPlayer.create(this.context, R.raw.pinknoise_15)
        player.isLooping = true

        val delaySpinner: Spinner = view.findViewById(R.id.delay_spinner)
        val fadeInSpinner: Spinner = view.findViewById(R.id.fade_in_spinner)
        val fadeOutSpinner: Spinner = view.findViewById(R.id.fade_out_spinner)
        val lengthSpinner: Spinner = view.findViewById(R.id.duration_spinner)
        this.context?.let {
           // val audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val delayDurations = (0..90).toList()
            val delayAdapter = ArrayAdapter<Int>(it, R.layout.spinner_item, delayDurations)
            delayAdapter.setDropDownViewResource(R.layout.spinner_item)
            with(delaySpinner) {
                adapter = delayAdapter
                setSelection(27, false)
                onItemSelectedListener = this@FirstFragment
                gravity = Gravity.CENTER

            }

            val fadeDurations = listOf(0, 1, 2, 3, 5, 10)
            val fadeAdapter = ArrayAdapter<Int>(it, R.layout.spinner_item, fadeDurations)
            fadeAdapter.setDropDownViewResource(R.layout.spinner_item)
            with(fadeInSpinner) {
                adapter = fadeAdapter
                setSelection(3, false)
                onItemSelectedListener = this@FirstFragment
                gravity = Gravity.CENTER

            }

            with(fadeOutSpinner) {
                adapter = fadeAdapter
                setSelection(3, false)
                onItemSelectedListener = this@FirstFragment
                gravity = Gravity.CENTER
            }

            val lengthAdapter = ArrayAdapter<Int>(it, R.layout.spinner_item, delayDurations)
            lengthAdapter.setDropDownViewResource(R.layout.spinner_item)
            with(lengthSpinner) {
                adapter = lengthAdapter
                setSelection(25, false)
                onItemSelectedListener = this@FirstFragment
                gravity = Gravity.CENTER
            }
        }

        view.findViewById<Button>(R.id.play_button).setOnClickListener {
            if (player.isPlaying && enabled) {
                stopPlayer(view)
            } else if (!enabled) {
                val selectedDelay = delaySpinner.selectedItem as Int? ?:0
                val delay: Long = (((selectedDelay) * 60 * 1000) as Integer).toLong()
                val duration: Long = (((lengthSpinner.selectedItem as Int? ?:0) * 60 * 1000) as Integer).toLong()
                val fadeIn: Long = (((fadeInSpinner.selectedItem as Int? ?:0) * 60 * 1000) as Integer).toLong()
                val fadeOut: Long = (((fadeOutSpinner.selectedItem as Int? ?:0) * 60 * 1000) as Integer).toLong()
                if (delay > 0) {
                    Log.d("BabySleep","delay $delay")
                    activity?.runOnUiThread {
                        var tickCounter = 0
                        delayTimer = object : CountDownTimer(delay, 60000) {
                            override fun onFinish() {
                                delaySpinner.setSelection(selectedDelay - tickCounter++)
                                startPlayer(view, fadeIn)
                                stopPlayerIn(duration, fadeOut, view, lengthSpinner)
                                cancel()
                            }

                            override fun onTick(p0: Long) {
                                Log.d("BabySleep", "tick $p0 : $delay")
                                delaySpinner.setSelection(selectedDelay - tickCounter++)
                                Log.d("BabySleep", "$delaySpinner.selectedItem minutes until media played")
                            }
                        }
                        delayTimer.start()
                    }
//                    Timer("Delay").schedule(delay) {
//
//                    }
                } else {
                    startPlayer(view, fadeIn)
                    if (duration > 0) {
                        stopPlayerIn( duration, fadeOut, view, lengthSpinner)
                    }
                }
            }
            if(enabled) {
                view.findViewById<Button>(R.id.play_button).text = resources.getString(R.string.start)
                enabled = false
            } else {
                view.findViewById<Button>(R.id.play_button).text = resources.getString(R.string.stop)
                enabled = true
            }
        }
    }

    private fun stopPlayerIn(duration: Long, fadeOut: Long, view: View, spinner: Spinner) {
        Log.d("BabySleep","duration $duration")
        var playDuration = duration - fadeOut
        activity?.runOnUiThread {
            val selectedDuration = spinner.selectedItem as Int? ?:0
            var durationCounter = 0
            durationTimer = object : CountDownTimer(playDuration, 60000) {
                override fun onFinish() {
                    spinner.setSelection(selectedDuration - durationCounter++)
                    if (fadeOut > 0 ) {
                        fadeOutTimer = object : CountDownTimer(fadeOut, 1000) {
                            override fun onFinish() {
                                fadeInTimer.start()
                                stopPlayer(view)
                                cancel()
                            }

                            override fun onTick(p0: Long) {
                                Log.d("BabySleep", "tick $p0")
                                val volume: Float = mMAX_VOLUME * (p0.toFloat() / fadeOut).toFloat()
                                Log.d("BabySleep", "Setting volume to $volume")
                                player.setVolume(volume, volume)
                            }
                        }
                        fadeOutTimer.start()
                    }
                    else {
                        Log.d("BabySleep","Stopping Player")
                        stopPlayer(view)
                        cancel()
                    }
                }

                override fun onTick(p0: Long) {
                    spinner.setSelection(selectedDuration - durationCounter++)
                    Log.d("BabySleep", "$spinner.selectedItem until media stopped")
                }
            }
            durationTimer.start()
        }
    }

    private fun startPlayer(view: View, fadeIn: Long) {
        if(!player.isPlaying) {
            Log.d("BabySleep","Playing")
            if (fadeIn > 0) {
                activity?.runOnUiThread {
                    player.setVolume(0.0f, 0.0f);
                    fadeInTimer = object : CountDownTimer(fadeIn, 1000) {
                        override fun onFinish() {

                        }

                        override fun onTick(p0: Long) {
                            Log.d("BabySleep", "tick $p0, $fadeIn")
                            val volume =  mMAX_VOLUME - (mMAX_VOLUME * (p0.toFloat() / fadeIn).toFloat())
                            Log.d("BabySleep", "Setting volume to $volume")
                            player.setVolume(volume, volume)
                        }
                    }
                    fadeInTimer.start()
                }
            }
            player.start()
        }
    }

    private fun stopPlayer(view: View) {
        if (player.isPlaying) {
            Log.d("BabySleep","Pausing")
            player.pause()
            player.seekTo(0);
            activity?.runOnUiThread {
                fadeInTimer?.cancel()
                fadeOutTimer?.cancel()
                delayTimer?.cancel()
                durationTimer?.cancel()

                view.findViewById<Button>(R.id.play_button).text = resources.getString(R.string.start)
                enabled = false
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        print("Nothing Selected")
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        print("Value selected")
    }
}