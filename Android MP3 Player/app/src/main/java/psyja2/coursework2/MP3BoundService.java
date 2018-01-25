package psyja2.coursework2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.support.v7.app.NotificationCompat;

/**
 * Created by Josh on 03/11/2017.
 */
public class MP3BoundService extends Service {

    protected MP3PlayerExtended mp3Player;
    protected RemoteCallbackList<MP3Binder> remoteCallbackList = new RemoteCallbackList<MP3Binder>();
    protected MP3Listener mp3Listener;

    @Override
    public void onCreate()
    {
        // Initialise the MP3 listener thread and the MP3 player
        mp3Player = new MP3PlayerExtended();
        mp3Listener = new MP3Listener();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // Return a new binder object
        return new MP3Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // A sticky service will remain running always
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        // Stop the listener thread
        mp3Listener.running = false;
        mp3Listener = null;
        // Stop the mp3 player
        mp3Player.stop();

        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent)
    {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    private void showNotification()
    {
        // Pending intent to launch main activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        // Create the notification
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker(("message"))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("MP3 Player")
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .build();
        // Make the service run in the foreground with this notification
        startForeground(1, notification);
    }

    private void hideNotification()
    {
        // Get rid of the notification
        stopForeground(true);
    }

    public class MP3Binder extends Binder implements IInterface
    {
        /*
        Contains methods to interface with the MP3 player object
         */

        @Override
        public IBinder asBinder()
        {
            return this;
        }

        public void loadMP3(String file)
        {
            mp3Player.load(file);
        }

        public void stopMP3()
        {
            mp3Player.stop();
            hideNotification();
        }

        public void pauseMP3()
        {
            mp3Player.pause();
        }

        public void playMP3()
        {
            showNotification();
            mp3Player.play();
        }

        public MP3Player.MP3PlayerState getPlayerState()
        {
            return mp3Player.getState();
        }

        public String getMP3Name()
        {
            // Get just the filename from the full path
            String name = mp3Player.getFilePath().substring(0, mp3Player.getFilePath().lastIndexOf("."));
            name = name.substring(name.lastIndexOf('/') + 1, name.length());
            return name;
        }

        public int getProgress()
        {
            return mp3Player.getProgress();
        }

        public void setProgress(int time)
        {
            if(mp3Player.getState() == MP3Player.MP3PlayerState.PLAYING || mp3Player.getState() == MP3Player.MP3PlayerState.PAUSED)
                mp3Player.setProgress(time);
        }

        public int getDuration()
        {
            return mp3Player.getDuration();
        }

        public void registerCallback(CallbackInterface callback)
        {
            this.callback = callback;
            remoteCallbackList.register(MP3Binder.this);
        }

        public void unregisterCallback(CallbackInterface callback)
        {
            remoteCallbackList.unregister(MP3Binder.this);
        }

        CallbackInterface callback;
    }

    /*
    This thread will continually check to see if the progress or state of the MP3Player changes
    It will then use the callback methods to communicate with the listeners
     */
    public class MP3Listener extends Thread implements Runnable
    {
        MP3Player.MP3PlayerState previousState;
        int previousProgress = -1;
        boolean running = true;

        public MP3Listener()
        {
            this.start();
            previousState = null;
        }

        public void run()
        {
            while(this.running)
            {
                // Sleep for 0.1 seconds
                try {Thread.sleep(100);} catch(Exception e) {return;}

                // Check to see if the mp3 has ended - if it has stop it
                if(mp3Player.getProgress() >= mp3Player.getDuration())
                {
                    mp3Player.stop();
                    hideNotification();
                }

                // Check to see if the mp3player state has changed
                if(previousState != mp3Player.getState())
                {
                    previousState = mp3Player.getState();
                    callbackStateChanged(previousState);
                }

                // Check to see if the mp3 player progress has changed
                if(previousProgress != mp3Player.getProgress())
                {
                    previousProgress = mp3Player.getProgress();
                    callbackProgressChange(mp3Player.getProgress(), mp3Player.getDuration());
                }
            }
        }
    }

    public void callbackStateChanged(MP3Player.MP3PlayerState state)
    {
        // Broadcast to all callback listeners
        final int n = remoteCallbackList.beginBroadcast();
        for (int i=0; i<n; i++)
        {
            remoteCallbackList.getBroadcastItem(i).callback.playerStateChanged(state);
        }
        remoteCallbackList.finishBroadcast();
    }

    public void callbackProgressChange(int progress, int duration)
    {
        // Broadcast to all callback listeners
        final int n = remoteCallbackList.beginBroadcast();
        for (int i=0; i<n; i++)
        {
            remoteCallbackList.getBroadcastItem(i).callback.progressChangeEvent(progress, duration);
        }
        remoteCallbackList.finishBroadcast();
    }
}
