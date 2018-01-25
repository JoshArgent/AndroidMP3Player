package psyja2.coursework2;

/**
 * Created by Josh on 09/11/2017.
 */
public class MP3PlayerExtended extends MP3Player {

    /*
    Set the progress of the mp3 player
     */
    public void setProgress(int time)
    {
        mediaPlayer.seekTo(time);
    }
}
