/*
 * Copyright (C) 2015 Domoticz
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package nl.hnogames.domoticz.Domoticz;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import nl.hnogames.domoticz.Containers.SettingsInfo;
import nl.hnogames.domoticz.Interfaces.JSONParserInterface;
import nl.hnogames.domoticz.Interfaces.SettingsReceiver;

public class SettingsParser implements JSONParserInterface {

    private static final String TAG = SettingsParser.class.getSimpleName();
    private SettingsReceiver receiver;

    public SettingsParser(SettingsReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void parseResult(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            SettingsInfo SettingsInfo = new SettingsInfo(jsonObject);
            receiver.onReceiveSettings(SettingsInfo);
        } catch (JSONException e) {
            Log.e(TAG, "SettingsParser JSON exception");
            e.printStackTrace();
            receiver.onError(e);
        }
    }

    @Override
    public void onError(Exception error) {
        Log.e(TAG, "SettingsParser of JSONParserInterface exception");
        receiver.onError(error);
    }
}