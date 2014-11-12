/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.Utilities;
import br.com.uatizapi.ui.Adapters.BaseFragmentAdapter;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenu;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenuItem;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.PinnedHeaderListView;
import br.com.uatizapi.ui.Views.SectionedBaseAdapter;
import br.com.uatizapi.ui.Views.SettingsSectionLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class CountrySelectActivity extends BaseFragment {

    public static interface CountrySelectActivityDelegate {
        public abstract void didSelectCountry(String name);
    }

    private SectionedBaseAdapter listViewAdapter;
    private PinnedHeaderListView listView;
    private boolean searchWas;
    private boolean searching;
    private BaseAdapter searchListViewAdapter;
    private TextView emptyTextView;
    private HashMap<String, ArrayList<Country>> countries = new HashMap<String, ArrayList<Country>>();
    private ArrayList<String> sortedCountries = new ArrayList<String>();
    private CountrySelectActivityDelegate delegate;

    private Timer searchTimer;
    public ArrayList<Country> searchResult;

    public static class Country {
        public String name;
        public String code;
        public String shortname;
    }

    @Override
    public boolean onFragmentCreate() {
        try {
            InputStream stream = ApplicationLoader.applicationContext.getResources().getAssets().open("countries.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] args = line.split(";");
                Country c = new Country();
                c.name = args[2];
                c.code = args[0];
                c.shortname = args[1];
                String n = c.name.substring(0, 1).toUpperCase();
                ArrayList<Country> arr = countries.get(n);
                if (arr == null) {
                    arr = new ArrayList<Country>();
                    countries.put(n, arr);
                    sortedCountries.add(n);
                }
                arr.add(c);
            }
            reader.close();
            stream.close();
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }

        Collections.sort(sortedCountries, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareTo(rhs);
            }
        });

        for (ArrayList<Country> arr : countries.values()) {
            Collections.sort(arr, new Comparator<Country>() {
                @Override
                public int compare(Country country, Country country2) {
                    return country.name.compareTo(country2.name);
                }
            });
        }

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);
            actionBarLayer.setTitle(LocaleController.getString("ChooseCountry", br.com.uatizapi.messenger.R.string.ChooseCountry));

            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            ActionBarMenu menu = actionBarLayer.createMenu();
            menu.addItem(0, br.com.uatizapi.messenger.R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                @Override
                public void onSearchExpand() {
                    searching = true;
                }

                @Override
                public void onSearchCollapse() {
                    search(null);
                    searching = false;
                    searchWas = false;
                    ViewGroup group = (ViewGroup) listView.getParent();
                    listView.setAdapter(listViewAdapter);
                    if (!LocaleController.isRTL) {
                        listView.setPadding(AndroidUtilities.dp(16), listView.getPaddingTop(), AndroidUtilities.dp(30), listView.getPaddingBottom());
                    } else {
                        listView.setPadding(AndroidUtilities.dp(30), listView.getPaddingTop(), AndroidUtilities.dp(16), listView.getPaddingBottom());
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 11) {
                        listView.setFastScrollAlwaysVisible(true);
                    }
                    listView.setFastScrollEnabled(true);
                    listView.setVerticalScrollBarEnabled(false);

                    emptyTextView.setText(LocaleController.getString("ChooseCountry", br.com.uatizapi.messenger.R.string.ChooseCountry));
                }

                @Override
                public void onTextChanged(EditText editText) {
                    String text = editText.getText().toString();
                    search(text);
                    if (text.length() != 0) {
                        searchWas = true;
                        if (listView != null) {
                            listView.setPadding(AndroidUtilities.dp(16), listView.getPaddingTop(), AndroidUtilities.dp(16), listView.getPaddingBottom());
                            listView.setAdapter(searchListViewAdapter);
                            if(android.os.Build.VERSION.SDK_INT >= 11) {
                                listView.setFastScrollAlwaysVisible(false);
                            }
                            listView.setFastScrollEnabled(false);
                            listView.setVerticalScrollBarEnabled(true);
                        }
                        if (emptyTextView != null) {
                            emptyTextView.setText(LocaleController.getString("NoResult", br.com.uatizapi.messenger.R.string.NoResult));
                        }
                    }
                }
            });

            searching = false;
            searchWas = false;

            fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.country_select_layout, container, false);

            emptyTextView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.searchEmptyView);
            searchListViewAdapter = new SearchAdapter(getParentActivity());

            listView = (PinnedHeaderListView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.listView);
            listView.setEmptyView(emptyTextView);
            emptyTextView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            listView.setVerticalScrollBarEnabled(false);

            listView.setAdapter(listViewAdapter = new ListAdapter(getParentActivity()));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (searching && searchWas) {
                        if (i < searchResult.size()) {
                            Country c = searchResult.get(i);
                            if (delegate != null) {
                                delegate.didSelectCountry(c.name);
                            }
                            finishFragment();
                        }
                    } else {
                        int section = listViewAdapter.getSectionForPosition(i);
                        int row = listViewAdapter.getPositionInSectionForPosition(i);
                        if (section < sortedCountries.size()) {
                            String n = sortedCountries.get(section);
                            ArrayList<Country> arr = countries.get(n);
                            if (row < arr.size()) {
                                Country c = arr.get(row);
                                if (delegate != null) {
                                    delegate.didSelectCountry(c.name);
                                }
                                finishFragment();
                            }
                        }
                    }
                }
            });

            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_TOUCH_SCROLL && searching && searchWas) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }
            });
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listViewAdapter != null) {
            listViewAdapter.notifyDataSetChanged();
        }
    }

    public void search(final String query) {
        if (query == null) {
            searchResult = null;
        } else {
            try {
                if (searchTimer != null) {
                    searchTimer.cancel();
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
            searchTimer = new Timer();
            searchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        searchTimer.cancel();
                        searchTimer = null;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    processSearch(query);
                }
            }, 100, 300);
        }
    }

    private void processSearch(final String query) {
        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {

                String q = query.trim().toLowerCase();
                if (q.length() == 0) {
                    updateSearchResults(new ArrayList<Country>());
                    return;
                }
                long time = System.currentTimeMillis();
                ArrayList<Country> resultArray = new ArrayList<Country>();

                String n = query.substring(0, 1);
                ArrayList<Country> arr = countries.get(n.toUpperCase());
                if (arr != null) {
                    for (Country c : arr) {
                        if (c.name.toLowerCase().startsWith(query)) {
                            resultArray.add(c);
                        }
                    }
                }

                updateSearchResults(resultArray);
            }
        });
    }

    public void setCountrySelectActivityDelegate(CountrySelectActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void updateSearchResults(final ArrayList<Country> arrCounties) {
        AndroidUtilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                searchResult = arrCounties;
                searchListViewAdapter.notifyDataSetChanged();
            }
        });
    }

    private class SearchAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public SearchAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            if (searchResult == null) {
                return 0;
            }
            return searchResult.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = li.inflate(br.com.uatizapi.messenger.R.layout.country_row_layout, viewGroup, false);
            }
            TextView textView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
            TextView detailTextView = (TextView)view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);
            View divider = view.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);

            Country c = searchResult.get(i);
            textView.setText(c.name);
            detailTextView.setText("+" + c.code);
            if (i == searchResult.size() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return searchResult == null || searchResult.size() == 0;
        }
    }

    private class ListAdapter extends SectionedBaseAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object getItem(int section, int position) {
            return null;
        }

        @Override
        public long getItemId(int section, int position) {
            return 0;
        }

        @Override
        public int getSectionCount() {
            return sortedCountries.size();
        }

        @Override
        public int getCountForSection(int section) {
            String n = sortedCountries.get(section);
            ArrayList<Country> arr = countries.get(n);
            return arr.size();
        }

        @Override
        public View getItemView(int section, int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = li.inflate(br.com.uatizapi.messenger.R.layout.country_row_layout, parent, false);
            }
            TextView textView = (TextView)convertView.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text);
            TextView detailTextView = (TextView)convertView.findViewById(br.com.uatizapi.messenger.R.id.settings_row_text_detail);
            View divider = convertView.findViewById(br.com.uatizapi.messenger.R.id.settings_row_divider);

            String n = sortedCountries.get(section);
            ArrayList<Country> arr = countries.get(n);
            Country c = arr.get(position);
            textView.setText(c.name);
            detailTextView.setText("+" + c.code);
            if (position == arr.size() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }

            return convertView;
        }

        @Override
        public int getItemViewType(int section, int position) {
            return 0;
        }

        @Override
        public int getItemViewTypeCount() {
            return 1;
        }

        @Override
        public int getSectionHeaderViewType(int section) {
            return 0;
        }

        @Override
        public int getSectionHeaderViewTypeCount() {
            return 1;
        }

        @Override
        public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new SettingsSectionLayout(mContext);
                convertView.setBackgroundColor(0xfffafafa);
            }
            ((SettingsSectionLayout) convertView).setText(sortedCountries.get(section).toUpperCase());
            return convertView;
        }
    }
}
