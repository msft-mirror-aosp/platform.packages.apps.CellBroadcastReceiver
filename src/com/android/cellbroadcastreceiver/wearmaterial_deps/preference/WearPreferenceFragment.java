package com.google.android.clockwork.common.wearable.wearmaterial.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
// import android.support.v7.widget.ConcatAdapter;
//import android.support.v7.widget.ConcatAdapter.Config.StableIdMode;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import com.google.android.clockwork.common.time.DefaultClock;
import com.google.android.clockwork.common.wearable.wearmaterial.list.FadingWearableRecyclerView;
import com.google.android.clockwork.common.wearable.wearmaterial.time.WearTimeText;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Wear-specific variant of PreferenceFragment. */
public abstract class WearPreferenceFragment extends PreferenceFragmentCompat {

  private static final String TAG = "WearPreferenceFragment";

  public static final int HEADER_INDEX = 0;

  private boolean useWearMaterialPreferences;
  private boolean showTitle = true;
  private boolean showClock = true;
  @Nullable WearPreferenceScreenTitleAdapter titleAdapter;

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (showClock) {
      WearTimeText timeText = view.findViewById(R.id.wear_preference_time_text);
      if (timeText != null) {
        timeText.setClock(DefaultClock.INSTANCE.get(view.getContext()));
        OnScrollListener headerOnScrollListener =
            new OnScrollListener() {
              @Override
              public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                timeText.applyScrollPositionFade(recyclerView.computeVerticalScrollOffset());
              }
            };
        getListView().addOnScrollListener(headerOnScrollListener);
      }
    }
  }

  // Incompatible parameter type for savedInstanceState.
  @SuppressWarnings("nullness:override.param")
  @Override
  public RecyclerView onCreateRecyclerView(
      LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    FadingWearableRecyclerView recyclerView =
        (FadingWearableRecyclerView)
            inflater.inflate(R.layout.wear_preference_recyclerview, parent, false);
    recyclerView.setLayoutManager(onCreateLayoutManager());
    recyclerView.addItemDecoration(new WearPreferenceItemDecoration(parent.getContext()));
    return recyclerView;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Adapter<ViewHolder> onCreateAdapter(PreferenceScreen preferenceScreen) {
    RecyclerView.Adapter<PreferenceViewHolder> preferenceAdapter =
        super.onCreateAdapter(preferenceScreen);
    WearPreferenceScreenTitleAdapter titleAdapter =
        new WearPreferenceScreenTitleAdapter(getTitle(preferenceScreen), showTitle);
    this.titleAdapter = titleAdapter;
    ConcatAdapter.Config config =
        new ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(true)
            .setStableIdMode(
                preferenceAdapter.hasStableIds()
                    ? StableIdMode.ISOLATED_STABLE_IDS
                    : StableIdMode.NO_STABLE_IDS)
            .build();
    return new ConcatAdapter(config, titleAdapter, preferenceAdapter);
  }

  @Override
  public void addPreferencesFromResource(int preferencesResId) {
    if (useWearMaterialPreferences) {
      setPreferenceScreen(inflateFromResource(preferencesResId, getPreferenceScreen()));
    } else {
      super.addPreferencesFromResource(preferencesResId);
    }
  }

  /**
   * Inflates the given XML resource and replaces the current preference hierarchy (if any) with the
   * preference hierarchy rooted at {@code key}.
   *
   * @param preferencesResId The XML resource ID to inflate
   * @param key The preference key of the {@link PreferenceScreen} to use as the root of the
   *     preference hierarchy, or {@code null} to use the root {@link PreferenceScreen}.
   */
  @Override
  public void setPreferencesFromResource(@XmlRes int preferencesResId, @Nullable String key) {
    if (!useWearMaterialPreferences) {
      super.setPreferencesFromResource(preferencesResId, key);
      return;
    }
    // When inflating wear material preferences instead of AndroidX preference, this uses similar
    // code to the super class method, but instead delegates to a different implementation of
    // inflateFromResource

    PreferenceScreen xmlRoot =
        inflateFromResource(preferencesResId, /* rootPreferenceScreen= */ null);

    Preference root = xmlRoot;
    if (key != null) {
      root = xmlRoot.findPreference(key);
      if (!(root instanceof PreferenceScreen)) {
        throw new IllegalArgumentException(
            "Preference object with key " + key + " is not a PreferenceScreen");
      }
    }

    setPreferenceScreen((PreferenceScreen) root);
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof WearDialogPreference) {
      ((WearDialogPreference) preference).show(getParentFragmentManager());
    } else if (preference instanceof WearListPreference) {
      ((WearListPreference) preference).show(getParentFragmentManager());
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  /**
   * Sets whether the fragment should show the {@link PreferencePage}'s title at the top.
   *
   * <p>{@code true} by default.
   */
  public void setShowTitle(boolean showTitle) {
    this.showTitle = showTitle;
    if (titleAdapter != null) {
      titleAdapter.setShowTitle(showTitle);
    }
  }

  /**
   * Sets whether the fragment should show the Clock at the top.
   *
   * <p>{@code true} by default.
   */
  public void setShowClock(boolean showClock) {
    this.showClock = showClock;
  }

  /**
   * Re-implementation of {@link PreferenceManager#inflateFromResource(Context, int,
   * PreferenceScreen)}. We cannot re-use the implementation in {@link PreferenceManager} because we
   * do not have visibility into {@code PreferenceInflater}, as there is some non-ideal behavior
   */
  @SuppressWarnings("nullness:argument") // suppress error from setPreferenceDataStore
  private PreferenceScreen inflateFromResource(
      int preferencesResId, @Nullable PreferenceScreen rootPreferenceScreen) {
    PreferenceManager preferenceManager = getPreferenceManager();
    if (preferenceManager == null) {
      throw new IllegalStateException("This should be called after super.onCreate.");
    }

    // If preference data store is not set before this method called, assume that it's not set
    // before this method returns. This is a safe assumption, as if this is not done, either the
    // initial persistence of preferences does not happen, or preferences which have not been
    // explicitly marked as non-persistent will attempt to commit to shared prefs. Both of those
    // are not desirable to the app.
    PreferenceDataStore originalDataStore = preferenceManager.getPreferenceDataStore();
    BatchedSharedPreferenceDataStore compatibilityDataStore = null;
    SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
    if (originalDataStore == null && sharedPreferences != null) {
      compatibilityDataStore = new BatchedSharedPreferenceDataStore(sharedPreferences);
      preferenceManager.setPreferenceDataStore(compatibilityDataStore);
    }

    WearPreferenceInflater inflater = new WearPreferenceInflater(getContext(), preferenceManager);
    PreferenceScreen inflated =
        (PreferenceScreen) inflater.inflate(preferencesResId, rootPreferenceScreen);
    new PreferenceReflectionHelper(inflated).onAttachedToHierarchy(preferenceManager);

    // Restore the original preference data store state.
    if (compatibilityDataStore != null) {
      compatibilityDataStore.apply();
      preferenceManager.setPreferenceDataStore(null);
    }

    return inflated;
  }

  /**
   * Sets whether or not the preference inflation should treat preferences declared in XML as their
   * wear-specific equivalent instead of the AndroidX preference.
   */
  public void setUseWearMaterialPreferences(boolean useWearMaterialPreferences) {
    this.useWearMaterialPreferences = useWearMaterialPreferences;
  }

  private CharSequence getTitle(PreferenceScreen preferenceScreen) {
    CharSequence title = preferenceScreen.getTitle();
    if (TextUtils.isEmpty(title)) {
      // Fall back to the activity title if the preference screen doesn't have one. We have no
      // title/action bar on Wear, so for apps optimized across multiple platforms this is the most
      // platform-agnostic approach.
      FragmentActivity activity = getActivity();
      title = activity != null ? activity.getTitle() : "";
    }
    return title;
  }
}
