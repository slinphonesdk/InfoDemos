package server.com.masterapp2;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


public class BootActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService();
    }

    private void startService() {
        Intent i = new Intent(getBaseContext(), MasterService.class);
        i.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startService(i);
    }
}
