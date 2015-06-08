/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

package com.nextgis.maplibui.controlui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.TextView;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.util.GeoConstants;


@SuppressLint("ViewConstructor")
public class TextEditControl
        extends TextView
        implements IControl
{
    String mFieldName;


    public TextEditControl(Context context)
    {
        super(context);
    }


    public TextEditControl(
            Context context,
            Field field,
            Cursor featureCursor)
    {
        super(context);

        mFieldName = field.getName();

        if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);

            if (column >= 0) {
                String stringVal = featureCursor.getString(column);
                setText(stringVal);
            }
        }

        switch (field.getType()) {

            case GeoConstants.FTString:
                break;

            case GeoConstants.FTInteger:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER);
                break;

            case GeoConstants.FTReal:
                setSingleLine(true);
                setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
        }
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
    }


    @Override
    public Object getValue()
    {
        return getText().toString();
    }
}