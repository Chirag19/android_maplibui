/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.api.ILayerUI;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;

public class NGWSettingsActivity extends PreferenceActivity implements OnAccountsUpdateListener
{
    protected AccountManager mAccountManager;
    protected final        Handler mHandler       = new Handler();
    protected static final String  ACCOUNT_ACTION = "com.nextgis.maplibui.ACCOUNT";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(null == mAccountManager){
            mAccountManager = AccountManager.get(this);
        }

        ViewGroup root = ((ViewGroup) findViewById(android.R.id.content));
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer =
                (LinearLayout) View.inflate(this, R.layout.activity_settings, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        Toolbar toolbar = (Toolbar) toolbarContainer.findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                NGWSettingsActivity.this.finish();
            }
        });

        String action = getIntent().getAction();
        if (action != null && action.equals(ACCOUNT_ACTION)) {
            Account account = getIntent().getParcelableExtra("account");
            fillAccountPreferences(this, getPreferenceScreen(), account);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            fillPreferences();
        }
    }

    public static void fillAccountPreferences(final Activity activity, PreferenceScreen screen, final Account account)
    {
        //add sync settings group
        PreferenceCategory syncCategory = new PreferenceCategory(activity);
        syncCategory.setTitle(R.string.sync);
        screen.addPreference(syncCategory);

        //add auto sync property
        CheckBoxPreference enablePeriodicSync = new CheckBoxPreference(activity);
        enablePeriodicSync.setTitle(R.string.auto_sync);
        enablePeriodicSync.setSummary(R.string.auto_sync_summary);
        //IGISApplication application = (IGISApplication)activity.getApplicationContext();
        //boolean isYourAccountSyncEnabled = ContentResolver.getSyncAutomatically(account, application.getAuthority());
        //TODO: enablePeriodicSync.setChecked();
        //TODO: enablePeriodicSync.setKey();
        syncCategory.addPreference(enablePeriodicSync);
        //add time for auto sync
        ListPreference timeInterval = new ListPreference(activity);
        timeInterval.setTitle(R.string.sync_interval);
        //TODO: timeInterval.setSummary(); //set a time from properties
        //TODO: timeInterval.setKey();
        syncCategory.addPreference(timeInterval);

        List<NGWVectorLayer> layers = getLayersForAccount(activity, account);
        if(!layers.isEmpty()) {
            PreferenceCategory layersCategory = new PreferenceCategory(activity);
            layersCategory.setTitle(R.string.sync_layers);
            layersCategory.setSummary(R.string.sync_layers_summary);
            screen.addPreference(layersCategory);


            for (NGWVectorLayer layer : layers) {
                CheckBoxPreference layerSync = new CheckBoxPreference(activity);
                layerSync.setTitle(layer.getName());
                layerSync.setChecked(0 == (layer.getSyncType() & Constants.SYNC_NONE));
                layerSync.setKey("" + layer.getId());

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && layer instanceof ILayerUI) {
                    ILayerUI layerUI = (ILayerUI) layer;
                    layerSync.setIcon(layerUI.getIcon());
                }

                layersCategory.addPreference(layerSync);
            }
        }

        PreferenceCategory actionCategory = new PreferenceCategory(activity);
        actionCategory.setTitle(R.string.actions);
        screen.addPreference(actionCategory);

        Preference preferenceDelete = new Preference(activity);
        preferenceDelete.setTitle(R.string.delete_account);
        preferenceDelete.setSummary(R.string.delete_account_summary);
        if(actionCategory.addPreference(preferenceDelete)){
            preferenceDelete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                public boolean onPreferenceClick(Preference preference)
                {
                    final AccountManager accountManager = AccountManager.get(activity);
                    accountManager.removeAccount(account, null, new Handler());

                    List<NGWVectorLayer> layers = getLayersForAccount(activity, account);
                    for (NGWVectorLayer layer : layers)
                        layer.delete();
                    IGISApplication application = (IGISApplication)activity.getApplicationContext();
                    application.getMap().save();

                    activity.onBackPressed();
                    return true;
                }
            });
        }
    }

    protected static List<NGWVectorLayer> getLayersForAccount(Context context, Account account)
    {
        List<NGWVectorLayer> out = new ArrayList<>();

        IGISApplication application = (IGISApplication)context.getApplicationContext();
        MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, out);

        return out;
    }

    protected void fillPreferences()
    {
        if (null != mAccountManager) {
            for (Account account : mAccountManager.getAccountsByType(
                    NGW_ACCOUNT_TYPE)) {
                Preference preference = new Preference(this);
                preference.setTitle(account.name);

                Bundle bundle = new Bundle();
                bundle.putParcelable("account", account);
                Intent intent = new Intent(this, NGWSettingsActivity.class);
                intent.putExtras(bundle);
                intent.setAction(ACCOUNT_ACTION);

                preference.setIntent(intent);

                getPreferenceScreen().addPreference(preference);
            }
            //add "Add account" preference
            Preference preference = new Preference(this);
            preference.setTitle(R.string.add_account);
            preference.setSummary(R.string.add_account_summary);
            Intent intent = new Intent(this, NGWLoginActivity.class);
            preference.setIntent(intent);

            getPreferenceScreen().addPreference(preference);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        if(null == mAccountManager){
            mAccountManager = AccountManager.get(this);
        }

        if(null != mAccountManager){
            for(Account account : mAccountManager.getAccountsByType(
                    NGW_ACCOUNT_TYPE)){
                Header header = new Header();
                header.title = account.name;
                header.fragment = com.nextgis.maplibui.NGWSettingsFragment.class.getName();

                Bundle bundle = new Bundle();
                bundle.putParcelable("account", account);
                header.fragmentArguments = bundle;
                target.add(header);
            }
            //add "Add account" header
            Header header = new Header();
            header.title = getString(R.string.add_account);
            header.summary = getString(R.string.add_account_summary);
            header.intent = new Intent(this, NGWLoginActivity.class);
            target.add(header);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(null != mAccountManager)
            mAccountManager.addOnAccountsUpdatedListener(this, mHandler, true);
    }
    @Override
    public void onPause() {
        if(null != mAccountManager)
            mAccountManager.removeOnAccountsUpdatedListener(this);
        super.onPause();
    }


    @Override
    public void onAccountsUpdated(Account[] accounts)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getPreferenceScreen().removeAll();
            fillPreferences();
            //onContentChanged();
        }
        else {
            invalidateHeaders();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return NGWSettingsFragment.class.getName().equals(fragmentName);
    }
}
