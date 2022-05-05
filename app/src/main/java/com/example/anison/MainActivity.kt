package com.example.anison

/*
 * Peter Wanner
 * Final Project for CS 458
 *
 * It is a simple player for the website anison.fm
 *
 * The api is complete garbage but this should be able to get through all that and do it
 */

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var pause:Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val play = findViewById<Button>(R.id.play)

        //Here we are prepping the media player
        val url = "https://pool.anison.fm/AniSonFM(256)"
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(url)
            prepare()
        }

        //This is used to automatically update all the info on screen
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(1000)
                        runOnUiThread {
                            getInfo()
                        }
                    }
                } catch (e: InterruptedException) {
                }
            }
        }

        thread.start()

        //This just controls if the app is playing audio or not
        play.setOnClickListener {
            if (pause) {
                mediaPlayer.pause()
                pause = false
                play.text = "Play"
                //Toast.makeText(this, "radio paused", Toast.LENGTH_SHORT).show()
                Snackbar.make(it, "Radio Paused", Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                mediaPlayer.start()
                pause = true
                play.text = "Pause"
                //Toast.makeText(this, "radio playing", Toast.LENGTH_SHORT).show()
                Snackbar.make(it, "Radio Playing", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }

    }

    //If the app is closed, then the media player is shut down
    override fun onDestroy(){
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    @SuppressLint("SetTextI18n")
    fun getInfo() {

        val listenersText = findViewById<TextView>(R.id.listeners)
        val timeLeft = findViewById<TextView>(R.id.timeLeft)
        val titleAnime = findViewById<TextView>(R.id.titleAnime)
        val animeTitle = findViewById<TextView>(R.id.animeTitle)
        val sI = findViewById<ImageView>(R.id.songImage)

        val queue = Volley.newRequestQueue(this)
        val url = "http://anison.fm/status.php?widget=true"

        //Here we are doing our api call
        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                //Have to extract the number as its in Russian
                listenersText.text = "There are currently " + response.getString("listeners").filter{it.isDigit()} + " Listeners"

                //This is used to extract the title and anime the song is from
                val parsed = response.getString("on_air").replace("</?.*?>".toRegex(), "").replace("В эфире: ", "").replace(" &#151; ", "+").split("+")

                titleAnime.text = "Song: " + parsed[1]
                animeTitle.text = "Anime: \n" + parsed[0]

                //This is used to set the remaining time left in the song
                timeLeft.text = "Remaining Time: " + response.getString("duration") + " seconds"

                //Now this stupid thing is just to get an id and attach to the url to get the image
                val posterLink = "https://en.anison.fm/resources/poster/200/" + "<a(?:(?= )[^>]*)?>".toRegex().find(response.getString("on_air"))?.value.orEmpty().replace("<a href='http://anison.fm/catalog/", "").replace("' class='anime_link' target='_blank'>", "").replace("/" + parsed[0].replace("\\s".toRegex(), "+"), "") + ".jpg"

                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                var image: Bitmap?

                //Here we get the image
                executor.execute {
                    try {
                        val `in` = java.net.URL(posterLink).openStream()
                        image = BitmapFactory.decodeStream(`in`)

                        handler.post {
                            sI.setImageBitmap(image)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            }, { error ->
                Toast.makeText(this, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            })

        queue.add(jsonRequest)

    }
}