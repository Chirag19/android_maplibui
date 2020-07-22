/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017-2018, 2020 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.activity.NGIDLoginActivity;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.NGIDUtils;

public class NGIDLoginFragment extends Fragment implements View.OnClickListener {
    protected EditText mLogin, mPassword;
    protected Button mSignInButton;
    protected TextView mServer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == getParentFragment())
            setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_ngid_login, container, false);
        mLogin = view.findViewById(R.id.login);
        mPassword = view.findViewById(R.id.password);
        mSignInButton = view.findViewById(R.id.signin);
        mSignInButton.setOnClickListener(this);
        mServer = view.findViewById(R.id.server);
        setUpServerInfo();
        TextView signUp = view.findViewById(R.id.signup);
        signUp.setText(signUp.getText().toString().toUpperCase());
        signUp.setOnClickListener(this);
        view.findViewById(R.id.onpremise).setOnClickListener(this);
        return view;
    }

    private void setUpServerInfo() {
        Activity activity = getActivity();
        if (activity == null)
            return;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String url = preferences != null ? preferences.getString("ngid_url", NGIDUtils.NGID_MY) : NGIDUtils.NGID_MY;
        url = NetworkUtil.trimSlash(url);
        if (mServer != null)
            mServer.setText(getString(R.string.ngid_server, url));
    }

    @Override
    public void onClick(View v) {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        if (v.getId() == R.id.signin) {
            boolean loginPasswordFilled = checkEditText(mLogin) && checkEditText(mPassword);
            if (!loginPasswordFilled) {
                Toast.makeText(activity, R.string.field_not_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            IGISApplication application = (IGISApplication) activity.getApplication();
            application.sendEvent(ConstantsUI.GA_NGID, ConstantsUI.GA_CONNECT, ConstantsUI.GA_USER);
            mSignInButton.setEnabled(false);
            String login = mLogin.getText().toString().trim();
            String password = mPassword.getText().toString();
            NGIDUtils.getToken(activity, login, password, new NGIDUtils.OnFinish() {
                @Override
                public void onFinish(HttpResponse response) {
                    mSignInButton.setEnabled(true);

                    if (response.isOk()) {
                        activity.getIntent().putExtra(NGIDLoginActivity.EXTRA_SUCCESS, true);
                        activity.finish();
                    } else {
                        Toast.makeText(
                                activity,
                                NetworkUtil.getError(activity, response.getResponseCode()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (v.getId() == R.id.signup) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(NGIDUtils.NGID_MY));
            startActivity(browser);
        } else if (v.getId() == R.id.onpremise) {
            createDialog();
        }
    }

    private void createDialog() {
        if (getContext() == null)
            return;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String url = preferences != null ? preferences.getString("ngid_url", NGIDUtils.NGID_MY) : NGIDUtils.NGID_MY;
        final EditText editText = new EditText(getContext());
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(NGIDUtils.NGID_MY);
        if (!url.equals(NGIDUtils.NGID_MY))
            editText.setText(url);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.ngid_type_url).setView(editText).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String url = editText.getText().toString();
                if (url.isEmpty()) {
                    url = NGIDUtils.NGID_MY;
                }
                if (!NetworkUtil.isValidUri(url)) {
                    Toast.makeText(getContext(), R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                    return;
                }
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("ngid_url", url).apply();
                setUpServerInfo();
            }
        }).setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    private boolean checkEditText(EditText edit) {
        return edit.getText().length() > 0;
    }

}
