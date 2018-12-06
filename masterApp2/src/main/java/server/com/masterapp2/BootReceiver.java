package server.com.masterapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(final Context context, Intent intent) {

        Intent i = new Intent(context, MasterService.class);
        i.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startService(i);
    }
}
