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

package nl.hnogames.domoticz.Fragments;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;

import java.util.ArrayList;

import hugo.weaving.DebugLog;
import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import nl.hnogames.domoticz.Adapters.UtilityAdapter;
import nl.hnogames.domoticz.Containers.SwitchLogInfo;
import nl.hnogames.domoticz.Containers.UtilitiesInfo;
import nl.hnogames.domoticz.Domoticz.Domoticz;
import nl.hnogames.domoticz.GraphActivity;
import nl.hnogames.domoticz.Interfaces.DomoticzFragmentListener;
import nl.hnogames.domoticz.Interfaces.SwitchLogReceiver;
import nl.hnogames.domoticz.Interfaces.UtilitiesReceiver;
import nl.hnogames.domoticz.Interfaces.UtilityClickListener;
import nl.hnogames.domoticz.Interfaces.setCommandReceiver;
import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.UI.PasswordDialog;
import nl.hnogames.domoticz.UI.SwitchLogInfoDialog;
import nl.hnogames.domoticz.UI.TemperatureDialog;
import nl.hnogames.domoticz.UI.UtilitiesInfoDialog;
import nl.hnogames.domoticz.Utils.UsefulBits;
import nl.hnogames.domoticz.app.DomoticzRecyclerFragment;

public class Utilities extends DomoticzRecyclerFragment implements DomoticzFragmentListener,
        UtilityClickListener {

    private ArrayList<UtilitiesInfo> mUtilitiesInfos;
    private double thermostatSetPointValue;
    private UtilityAdapter adapter;
    private Context mContext;
    private String filter = "";
    private LinearLayout lExtraPanel = null;
    private Animation animShow, animHide;

    @Override
    @DebugLog
    public void refreshFragment() {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
        processUtilities();
    }

    @Override
    @DebugLog
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (getActionBar() != null)
            getActionBar().setTitle(R.string.title_utilities);
        initAnimation();
    }

    @Override
    @DebugLog
    public void Filter(String text) {
        filter = text;
        try {
            if (adapter != null)
                adapter.getFilter().filter(text);
            super.Filter(text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    @DebugLog
    public void onConnectionOk() {
        super.showSpinner(true);
        processUtilities();
    }

    private void initAnimation() {
        animShow = AnimationUtils.loadAnimation(mContext, R.anim.enter_from_right);
        animHide = AnimationUtils.loadAnimation(mContext, R.anim.exit_to_right);
    }

    private void processUtilities() {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);

        final UtilityClickListener listener = this;
        mDomoticz.getUtilities(new UtilitiesReceiver() {

            @Override
            @DebugLog
            public void onReceiveUtilities(ArrayList<UtilitiesInfo> mUtilitiesInfos) {
                successHandling(mUtilitiesInfos.toString(), false);

                Utilities.this.mUtilitiesInfos = mUtilitiesInfos;
                adapter = new UtilityAdapter(mContext, mDomoticz, mUtilitiesInfos, listener);

                createListView();
            }

            @Override
            @DebugLog
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    private void createListView() {

        if (getView() != null) {
            SlideInBottomAnimationAdapter alphaSlideIn = new SlideInBottomAnimationAdapter(adapter);
            gridView.setAdapter(alphaSlideIn);
            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                @DebugLog
                public void onRefresh() {
                    processUtilities();
                }
            });
            super.showSpinner(false);
            this.Filter(filter);
        }
    }

    private void showInfoDialog(final UtilitiesInfo mUtilitiesInfo) {
        UtilitiesInfoDialog infoDialog = new UtilitiesInfoDialog(
                mContext,
                mUtilitiesInfo,
                R.layout.dialog_utilities_info);
        infoDialog.setIdx(String.valueOf(mUtilitiesInfo.getIdx()));
        infoDialog.setLastUpdate(mUtilitiesInfo.getLastUpdate());
        infoDialog.setIsFavorite(mUtilitiesInfo.getFavoriteBoolean());
        infoDialog.show();
        infoDialog.onDismissListener(new UtilitiesInfoDialog.DismissListener() {
            @Override
            @DebugLog
            public void onDismiss(boolean isChanged, boolean isFavorite) {
                if (isChanged) changeFavorite(mUtilitiesInfo, isFavorite);
            }
        });
    }

    private void changeFavorite(final UtilitiesInfo mUtilitiesInfo, final boolean isFavorite) {
        addDebugText("changeFavorite");
        addDebugText("Set idx " + mUtilitiesInfo.getIdx() + " favorite to " + isFavorite);

        if (isFavorite)
            UsefulBits.showSimpleSnackbar(mContext, coordinatorLayout, mUtilitiesInfo.getName() + " " + mContext.getString(R.string.favorite_added), Snackbar.LENGTH_SHORT);
        else
            UsefulBits.showSimpleSnackbar(mContext, coordinatorLayout, mUtilitiesInfo.getName() + " " + mContext.getString(R.string.favorite_removed), Snackbar.LENGTH_SHORT);

        int jsonAction;
        int jsonUrl = Domoticz.Json.Url.Set.FAVORITE;

        if (isFavorite) jsonAction = Domoticz.Device.Favorite.ON;
        else jsonAction = Domoticz.Device.Favorite.OFF;

        mDomoticz.setAction(mUtilitiesInfo.getIdx(),
                jsonUrl,
                jsonAction,
                0,
                null,
                new setCommandReceiver() {
                    @Override
                    @DebugLog
                    public void onReceiveResult(String result) {
                        successHandling(result, false);
                        mUtilitiesInfo.setFavoriteBoolean(isFavorite);
                    }

                    @Override
                    @DebugLog
                    public void onError(Exception error) {
                        errorHandling(error);
                    }
                });
    }

    /**
     * Updates the set point in the Utilities container
     *
     * @param idx         ID of the utility to be changed
     * @param newSetPoint The new set point value
     */
    private void updateThermostatSetPointValue(int idx, double newSetPoint) {
        addDebugText("updateThermostatSetPointValue");

        for (UtilitiesInfo info : mUtilitiesInfos) {
            if (info.getIdx() == idx) {
                info.setSetPoint(newSetPoint);
                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Notifies the list view adapter the data has changed and refreshes the list view
     */
    private void notifyDataSetChanged() {
        addDebugText("notifyDataSetChanged");
        adapter.notifyDataSetChanged();
    }

    @Override
    @DebugLog
    public void errorHandling(Exception error) {
        if (error != null) {
            // Let's check if were still attached to an activity
            if (isAdded()) {
                super.errorHandling(error);
            }
        }
    }

    @Override
    @DebugLog
    public void onPause() {
        super.onPause();
    }

    private UtilitiesInfo getUtility(int idx) {
        for (UtilitiesInfo info : mUtilitiesInfos) {
            if (info.getIdx() == idx) {
                return info;
            }
        }
        return null;
    }

    @Override
    @DebugLog
    public void onClick(UtilitiesInfo utility) {
    }

    @Override
    @DebugLog
    public void onLogClick(final UtilitiesInfo utility, final String range) {
        /*
            Replace so we get the right log
         */
        String graphType = utility.getSubType()
                .replace("Electric", "counter")
                .replace("kWh", "counter")
                .replace("Energy", "counter")
                .replace("SetPoint", "temp")
                .replace("YouLess counter", "counter");

        Intent intent = new Intent(mContext, GraphActivity.class);
        intent.putExtra("IDX", utility.getIdx());
        intent.putExtra("RANGE", range);
        intent.putExtra("TYPE", graphType);
        intent.putExtra("TITLE", utility.getSubType().toUpperCase());
        intent.putExtra("STEPS", 3);
        startActivity(intent);
    }

    @Override
    @DebugLog
    public void onThermostatClick(final int idx) {
        addDebugText("onThermostatClick");
        final UtilitiesInfo tempUtil = getUtility(idx);

        TemperatureDialog tempDialog = new TemperatureDialog(
                mContext,
                tempUtil.getSetPoint());

        tempDialog.onDismissListener(new TemperatureDialog.DialogActionListener() {
            @Override
            @DebugLog
            public void onDialogAction(final double newSetPoint, DialogAction dialogAction) {
                if (dialogAction == DialogAction.POSITIVE) {
                    addDebugText("Set idx " + idx + " to " + String.valueOf(newSetPoint));
                    if (tempUtil != null) {
                        if (tempUtil.isProtected()) {
                            PasswordDialog passwordDialog = new PasswordDialog(
                                    mContext, mDomoticz);
                            passwordDialog.show();
                            passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                                @Override
                                @DebugLog
                                public void onDismiss(String password) {
                                    setThermostatAction(tempUtil, newSetPoint, password);
                                }
                            });
                        } else {
                            setThermostatAction(tempUtil, newSetPoint, null);
                        }
                    }
                } else {
                    addDebugText("Not updating idx " + idx);
                }
            }
        });

        tempDialog.show();
    }

    public void setThermostatAction(final UtilitiesInfo tempUtil,
                                    double newSetPoint,
                                    String password) {
        thermostatSetPointValue = newSetPoint;
        int jsonUrl = Domoticz.Json.Url.Set.TEMP;

        int action = Domoticz.Device.Thermostat.Action.PLUS;
        if (newSetPoint < tempUtil.getSetPoint())
            action = Domoticz.Device.Thermostat.Action.MIN;

        mDomoticz.setAction(tempUtil.getIdx(),
                jsonUrl,
                action,
                newSetPoint,
                password,
                new setCommandReceiver() {
                    @Override
                    @DebugLog
                    public void onReceiveResult(String result) {
                        updateThermostatSetPointValue(tempUtil.getIdx(), thermostatSetPointValue);
                        successHandling(result, false);
                    }

                    @Override
                    @DebugLog
                    public void onError(Exception error) {
                        errorHandling(error);
                    }
                });
    }


    @Override
    @DebugLog
    public void onLogButtonClick(int idx) {
        mDomoticz.getTextLogs(idx, new SwitchLogReceiver() {
            @Override
            @DebugLog
            public void onReceiveSwitches(ArrayList<SwitchLogInfo> switchesLogs) {
                showLogDialog(switchesLogs);
            }

            @Override
            @DebugLog
            public void onError(Exception error) {
                UsefulBits.showSimpleSnackbar(mContext, coordinatorLayout, R.string.error_logs, Snackbar.LENGTH_SHORT);
            }
        });
    }

    @Override
    @DebugLog
    public void onLikeButtonClick(int idx, boolean checked) {
        changeFavorite(getUtility(idx), checked);
    }

    @Override
    @DebugLog
    public void onItemClicked(View v, int position) {
        LinearLayout extra_panel = (LinearLayout) v.findViewById(R.id.extra_panel);
        if (extra_panel != null) {
            if (extra_panel.getVisibility() == View.VISIBLE) {
                extra_panel.startAnimation(animHide);
                extra_panel.setVisibility(View.GONE);
            } else {
                extra_panel.setVisibility(View.VISIBLE);
                extra_panel.startAnimation(animShow);
            }

            if (extra_panel != lExtraPanel) {
                if (lExtraPanel != null) {
                    if (lExtraPanel.getVisibility() == View.VISIBLE) {
                        lExtraPanel.startAnimation(animHide);
                        lExtraPanel.setVisibility(View.GONE);
                    }
                }
            }

            lExtraPanel = extra_panel;
        }
    }

    @Override
    @DebugLog
    public boolean onItemLongClicked(int position) {
        showInfoDialog(adapter.filteredData.get(position));
        return true;
    }

    private void showLogDialog(ArrayList<SwitchLogInfo> switchLogs) {
        if (switchLogs.size() <= 0) {
            Toast.makeText(mContext, "No logs found.", Toast.LENGTH_LONG).show();
        } else {
            SwitchLogInfoDialog infoDialog = new SwitchLogInfoDialog(
                    mContext,
                    switchLogs,
                    R.layout.dialog_switch_logs);
            infoDialog.show();
        }
    }
}