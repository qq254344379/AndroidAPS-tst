package dagger.android.support;

import androidx.fragment.app.Fragment;

/**
 * TEMPORARY compilation shim — see dagger.android.HasAndroidInjector for context.
 *
 * Lets the decompiled LenomedFragment compile under the Metro-based dev branch.
 * The real DaggerFragment injected in onAttach; that must be replaced by Metro
 * wiring in a full port.
 */
public class DaggerFragment extends Fragment {
}
