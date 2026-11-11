package dagger.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * TEMPORARY compilation shim — see {@link HasAndroidInjector} for context.
 *
 * The ported LenomedService overrides Android lifecycle callbacks and calls
 * super.onCreate() etc., which resolve to android.app.Service. No Dagger injection
 * is performed here (the real DaggerService did AndroidInjection.inject(this) in
 * onCreate; that behaviour must be replaced by Metro wiring in a full port).
 */
public class DaggerService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
