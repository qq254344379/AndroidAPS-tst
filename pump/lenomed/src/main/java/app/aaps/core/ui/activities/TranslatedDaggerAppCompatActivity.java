package app.aaps.core.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * In older AAPS builds this base class provided Dagger field-injection plus a
 * couple of injected helpers (ResourceHelper, AAPSLogger, ...). The current
 * build uses Metro DI and the driver's own activities already declare their
 * injected fields locally, so a plain {@link AppCompatActivity} subclass is
 * sufficient for compilation. Runtime injection of those fields is out of scope
 * for this porting effort.
 */
public class TranslatedDaggerAppCompatActivity extends AppCompatActivity {
}
