package psyja2.coursework2;

/**
 * Callback interface to be used for listening to events from the MP3BoundService
 * Created by Josh on 03/11/2017.
 */
public interface CallbackInterface {

    /*
    Will be called when the progress of a playing MP3 changes
     */
    public void progressChangeEvent(int progress, int duration);

    /*
    Will be called whenever the state of the underlying MP3 player changes
     */
    public void playerStateChanged(MP3Player.MP3PlayerState state);

}
