package psyja2.coursework2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Request code for the Open MP3 activity
    static final int OPEN_MP3_REQUEST_CODE = 1;

    // UI Components:
    private Button openButton;
    private Button pauseButton;
    private Button stopButton;
    private TextView songTitle;
    private TextView songProgressTime;
    private TextView songTime;
    private SeekBar songProgress;
    private LinearLayout playerControls;

    // Service
    private MP3BoundService.MP3Binder mp3Service = null;

    // Pre-load this MP3 file when the service is binded
    private String preloadFile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get references to all the UI components
        openButton = (Button) findViewById(R.id.openButton);
        pauseButton = (Button) findViewById(R.id.pauseButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        songTitle = (TextView) findViewById(R.id.songTitle);
        songProgressTime = (TextView) findViewById(R.id.songProgressTime);
        songTime = (TextView) findViewById(R.id.songTime);
        songProgress = (SeekBar) findViewById(R.id.songProgress);
        playerControls = (LinearLayout) findViewById(R.id.playerControls);

        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an intent and open the OpenMP3Activity and wait for a result
                Intent intent = new Intent(MainActivity.this, OpenMP3Activity.class);
                startActivityForResult(intent, OPEN_MP3_REQUEST_CODE);
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle the playing or pausing of the MP3 player
                if (mp3Service.getPlayerState() == MP3Player.MP3PlayerState.PLAYING) {
                    mp3Service.pauseMP3();
                } else if (mp3Service.getPlayerState() == MP3Player.MP3PlayerState.PAUSED) {
                    mp3Service.playMP3();
                }
            }
        });

        // Stop the MP3 player when the stop button is pressed
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mp3Service.stopMP3();
            }
        });

        // Listen for when the user drags the seek bar
        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                // Check it was actually changed by the user
                if(fromUser)
                {
                    // Work out the time the MP3 should be set to
                    mp3Service.setProgress((int) ((progress / 100f) * mp3Service.getDuration()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        // Start and bind the service
        this.startService(new Intent(this, MP3BoundService.class));
        this.bindService(new Intent(this, MP3BoundService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle)
    {
        super.onRestoreInstanceState(bundle);

        // Nothing needs to be restored, all state is in the service
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle)
    {
        super.onSaveInstanceState(bundle);

        // Nothing needs to be saved, all state is in the service
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(serviceConnection != null)
        {
            // If the MP3 player is stopped - stop the service, otherwise it will continue in the background
            if(mp3Service.getPlayerState() == MP3Player.MP3PlayerState.STOPPED)
            {
                stopService(new Intent(this, MP3BoundService.class));
            }

            // Unbind the service
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == OPEN_MP3_REQUEST_CODE) // Open MP3 activity closed
        {
            if (resultCode == RESULT_OK) // Check the result is OK
            {
                if(mp3Service == null)  // If the service has not been binded yet, save the file path in a string
                    preloadFile = data.getExtras().getString("mp3File");
                else
                {
                    // Otherwise, stop the current MP3 and then load and play the new file
                    mp3Service.stopMP3();
                    mp3Service.loadMP3(data.getExtras().getString("mp3File"));
                    mp3Service.playMP3();
                }
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // Register the callback object to the mp3Service
            mp3Service = (MP3BoundService.MP3Binder) service;
            mp3Service.registerCallback(callback);

            // Update the UI
            updateUI(mp3Service.getPlayerState());
            updateProgress(mp3Service.getProgress(), mp3Service.getDuration());

            // Load the MP3 specified in the preload variable
            if(!preloadFile.equalsIgnoreCase(""))
            {
                mp3Service.stopMP3();
                mp3Service.loadMP3(preloadFile);
                mp3Service.playMP3();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            // Unregister the callback
            mp3Service.unregisterCallback(callback);
            mp3Service = null;
        }
    };

    // Callback listener
    CallbackInterface callback = new CallbackInterface()
    {

        @Override
        public void progressChangeEvent(final int progress, final int duration)
        {
            // Update the progress bar - perform it on the UI thread
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    updateProgress(progress, duration);
                }
            });
        }

        @Override
        public void playerStateChanged(final MP3Player.MP3PlayerState state)
        {
            // Update the buttons - perform it on the UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI(state);
                }
            });
        }
    };

    /*
    Will update the progress bar progress and the duration/progress time labels
     */
    private void updateProgress(int progress, int duration)
    {
        if(duration >= 0)
        {
            songProgressTime.setText(milisecondsToMinutesSeconds(progress));
            songTime.setText(milisecondsToMinutesSeconds(duration));
            songProgress.setProgress((int) (((double) progress / (double) duration) * 100));
        }
    }

    /*
    Updates the GUI buttons and labels to reflect the state of the MP3 player
     */
    private void updateUI(MP3Player.MP3PlayerState state)
    {
        if (state == MP3Player.MP3PlayerState.ERROR)
        {
            // Disable all components and display an error message
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            songProgress.setEnabled(false);
            playerControls.setAlpha(0.3f);
            songTitle.setText("");
            Toast.makeText(MainActivity.this, "Error loading MP3!", Toast.LENGTH_LONG).show();
        }
        else if (state == MP3Player.MP3PlayerState.STOPPED)
        {
            // Disable all components
            stopButton.setEnabled(false);
            pauseButton.setEnabled(false);
            songProgress.setEnabled(false);
            playerControls.setAlpha(0.3f);
            songTitle.setText("");
        }
        else if (state == MP3Player.MP3PlayerState.PAUSED)
        {
            // Change the pause button text, enable components
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            songProgress.setEnabled(true);
            playerControls.setAlpha(1f);
            pauseButton.setText("Play");
            songTitle.setText(mp3Service.getMP3Name());
        }
        else if (state == MP3Player.MP3PlayerState.PLAYING)
        {
            // Change the play button text, enable components
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            songProgress.setEnabled(true);
            playerControls.setAlpha(1f);
            pauseButton.setText("Pause");
            songTitle.setText(mp3Service.getMP3Name());
        }
    }

    /*
    Converts miliseconds to a nicely formatted string of minutes:seconds
    Eg. 05:12
     */
    private static String milisecondsToMinutesSeconds(int miliseconds)
    {
        int totalSeconds = miliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }
}
