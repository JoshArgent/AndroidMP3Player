package psyja2.coursework2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

public class OpenMP3Activity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_mp3);

        // Load the MP3 files from the music folder
        final ListView lv = (ListView) findViewById(R.id.listView);
        File musicDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/");
        File list[] = musicDir.listFiles();

        // Check the user has permission and the list is not null
        if(checkReadExternalPermission())
        {
            if(list != null)
                lv.setAdapter(new ArrayAdapter<File>(this, android.R.layout.simple_list_item_1, list));
        }
        else
        {
            Toast.makeText(this, "You do not have External Storage Read permission enabled!", Toast.LENGTH_LONG).show();
        }

        // Handle when an item is pressed
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng)
            {
                File selectedFromList = (File) (lv.getItemAtPosition(myItemInt));

                // Add the mp3 file details to the bundle
                Bundle bundle = new Bundle();
                bundle.putString("mp3File", selectedFromList.getAbsolutePath());

                // Add the bundle to the result and finish the activity
                Intent result = new Intent();
                result.putExtras(bundle);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

    }

    /*
    Check if the app has READ permission
    Source: https://stackoverflow.com/questions/7203668/how-permission-can-be-checked-at-runtime-without-throwing-securityexception
     */
    private boolean checkReadExternalPermission()
    {
        String permission = "android.permission.READ_EXTERNAL_STORAGE";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }
}
