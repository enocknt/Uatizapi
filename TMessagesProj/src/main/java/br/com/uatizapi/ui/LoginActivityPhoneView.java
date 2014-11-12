/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.PhoneFormat.PhoneFormat;
import br.com.uatizapi.messenger.BuildVars;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.TLObject;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.messenger.ConnectionsManager;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.messenger.R;
import br.com.uatizapi.messenger.RPCRequest;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;
import br.com.uatizapi.ui.Views.SlideView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

public class LoginActivityPhoneView extends SlideView implements AdapterView.OnItemSelectedListener {
    private EditText codeField;
    private EditText phoneField;
    private TextView countryButton;

    private int countryState = 0;

    private ArrayList<String> countriesArray = new ArrayList<String>();
    private HashMap<String, String> countriesMap = new HashMap<String, String>();
    private HashMap<String, String> codesMap = new HashMap<String, String>();
    private HashMap<String, String> languageMap = new HashMap<String, String>();

    private boolean ignoreSelection = false;
    private boolean ignoreOnTextChange = false;
    private boolean ignoreOnPhoneChange = false;
    private boolean nextPressed = false;

    public LoginActivityPhoneView(Context context) {
        super(context);
    }

    public LoginActivityPhoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoginActivityPhoneView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        TextView textView = (TextView)findViewById(R.id.login_confirm_text);
        textView.setText(LocaleController.getString("StartText", R.string.StartText));

        countryButton = (TextView)findViewById(R.id.login_coutry_textview);
        countryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate == null) {
                    return;
                }
                BaseFragment activity = (BaseFragment)delegate;
                CountrySelectActivity fragment = new CountrySelectActivity();
                fragment.setCountrySelectActivityDelegate(new CountrySelectActivity.CountrySelectActivityDelegate() {
                    @Override
                    public void didSelectCountry(String name) {
                        selectCountry(name);
                        phoneField.requestFocus();
                    }
                });
                activity.presentFragment(fragment);
            }
        });

        codeField = (EditText)findViewById(R.id.login_county_code_field);
        codeField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (ignoreOnTextChange) {
                    ignoreOnTextChange = false;
                    return;
                }
                ignoreOnTextChange = true;
                String text = PhoneFormat.stripExceptNumbers(codeField.getText().toString());
                codeField.setText(text);
                if (text.length() == 0) {
                    countryButton.setText(LocaleController.getString("ChooseCountry", R.string.ChooseCountry));
                    countryState = 1;
                } else {
                    String country = codesMap.get(text);
                    if (country != null) {
                        int index = countriesArray.indexOf(country);
                        if (index != -1) {
                            ignoreSelection = true;
                            countryButton.setText(countriesArray.get(index));

                            updatePhoneField();
                            countryState = 0;
                        } else {
                            countryButton.setText(LocaleController.getString("WrongCountry", R.string.WrongCountry));
                            countryState = 2;
                        }
                    } else {
                        countryButton.setText(LocaleController.getString("WrongCountry", R.string.WrongCountry));
                        countryState = 2;
                    }
                    codeField.setSelection(codeField.getText().length());
                }
            }
        });
        codeField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    phoneField.requestFocus();
                    return true;
                }
                return false;
            }
        });
        phoneField = (EditText)findViewById(R.id.login_phone_field);
        phoneField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (ignoreOnPhoneChange) {
                    return;
                }
                if (count == 1 && after == 0 && s.length() > 1) {
                    String phoneChars = "0123456789";
                    String str = s.toString();
                    String substr = str.substring(start, start + 1);
                    if (!phoneChars.contains(substr)) {
                        ignoreOnPhoneChange = true;
                        StringBuilder builder = new StringBuilder(str);
                        int toDelete = 0;
                        for (int a = start; a >= 0; a--) {
                            substr = str.substring(a, a + 1);
                            if(phoneChars.contains(substr)) {
                                break;
                            }
                            toDelete++;
                        }
                        builder.delete(Math.max(0, start - toDelete), start + 1);
                        str = builder.toString();
                        if (PhoneFormat.strip(str).length() == 0) {
                            phoneField.setText("");
                        } else {
                            phoneField.setText(str);
                            updatePhoneField();
                        }
                        ignoreOnPhoneChange = false;
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (ignoreOnPhoneChange) {
                    return;
                }
                updatePhoneField();
            }
        });

        if(!isInEditMode()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().getAssets().open("countries.txt")));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] args = line.split(";");
                    countriesArray.add(0, args[2]);
                    countriesMap.put(args[2], args[0]);
                    codesMap.put(args[0], args[2]);
                    languageMap.put(args[1], args[2]);
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }

            Collections.sort(countriesArray, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            });

            String country = null;

            try {
                TelephonyManager telephonyManager = (TelephonyManager)ApplicationLoader.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    country = telephonyManager.getSimCountryIso().toUpperCase();
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }

            if (country != null) {
                String countryName = languageMap.get(country);
                if (countryName != null) {
                    int index = countriesArray.indexOf(countryName);
                    if (index != -1) {
                        codeField.setText(countriesMap.get(countryName));
                        countryState = 0;
                    }
                }
            }
            if (codeField.length() == 0) {
                countryButton.setText(LocaleController.getString("ChooseCountry", R.string.ChooseCountry));
                countryState = 1;
            }
        }

        if (codeField.length() != 0) {
            AndroidUtilities.showKeyboard(phoneField);
            phoneField.requestFocus();
        } else {
            AndroidUtilities.showKeyboard(codeField);
            codeField.requestFocus();
        }
        phoneField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    delegate.onNextAction();
                    return true;
                }
                return false;
            }
        });
    }

    public void selectCountry(String name) {
        int index = countriesArray.indexOf(name);
        if (index != -1) {
            ignoreOnTextChange = true;
            codeField.setText(countriesMap.get(name));
            countryButton.setText(name);
            countryState = 0;
        }
    }

    private void updatePhoneField() {
        ignoreOnPhoneChange = true;
        String codeText = codeField.getText().toString();
        String phone = PhoneFormat.getInstance().format("+" + codeText + phoneField.getText().toString());
        int idx = phone.indexOf(" ");
        if (idx != -1) {
            String resultCode = PhoneFormat.stripExceptNumbers(phone.substring(0, idx));
            if (!codeText.equals(resultCode)) {
                phone = PhoneFormat.getInstance().format(phoneField.getText().toString()).trim();
                phoneField.setText(phone);
                int len = phoneField.length();
                phoneField.setSelection(phoneField.length());
            } else {
                phoneField.setText(phone.substring(idx).trim());
                int len = phoneField.length();
                phoneField.setSelection(phoneField.length());
            }
        } else {
            phoneField.setSelection(phoneField.length());
        }
        ignoreOnPhoneChange = false;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (ignoreSelection) {
            ignoreSelection = false;
            return;
        }
        ignoreOnTextChange = true;
        String str = countriesArray.get(i);
        codeField.setText(countriesMap.get(str));
        updatePhoneField();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onNextPressed() {
        if (nextPressed) {
            return;
        }
        if (countryState == 1) {
            delegate.needShowAlert(LocaleController.getString("ChooseCountry", R.string.ChooseCountry));
            return;
        } else if (countryState == 2) {
            delegate.needShowAlert(LocaleController.getString("WrongCountry", R.string.WrongCountry));
            return;
        }
        if (codeField.length() == 0) {
            delegate.needShowAlert(LocaleController.getString("InvalidPhoneNumber", R.string.InvalidPhoneNumber));
            return;
        }
        TLRPC.TL_auth_sendCode req = new TLRPC.TL_auth_sendCode();
        String phone = PhoneFormat.stripExceptNumbers("" + codeField.getText() + phoneField.getText());
        ConnectionsManager.getInstance().applyCountryPortNumber(phone);
        req.api_hash = BuildVars.APP_HASH;
        req.api_id = BuildVars.APP_ID;
        req.sms_type = 0;
        req.phone_number = phone;
        req.lang_code = LocaleController.getLocaleString(Locale.getDefault());
        if (req.lang_code == null || req.lang_code.length() == 0) {
            req.lang_code = "en";
        }

        final Bundle params = new Bundle();
        params.putString("phone", "+" + codeField.getText() + phoneField.getText());
        params.putString("phoneFormated", phone);
        nextPressed = true;
        if (delegate != null) {
            delegate.needShowProgress();
        }
        ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
            @Override
            public void run(final TLObject response, final TLRPC.TL_error error) {
                AndroidUtilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        nextPressed = false;
                        if (error == null) {
                            final TLRPC.TL_auth_sentCode res = (TLRPC.TL_auth_sentCode)response;
                            params.putString("phoneHash", res.phone_code_hash);
                            params.putInt("calltime", res.send_call_timeout * 1000);
                            if (res.phone_registered) {
                                params.putString("registered", "true");
                            }
                            if (delegate != null) {
                                delegate.setPage(1, true, params, false);
                            }
                        } else {
                            if (delegate != null && error.text != null) {
                                if (error.text.contains("PHONE_NUMBER_INVALID")) {
                                    delegate.needShowAlert(LocaleController.getString("InvalidPhoneNumber", R.string.InvalidPhoneNumber));
                                } else if (error.text.contains("PHONE_CODE_EMPTY") || error.text.contains("PHONE_CODE_INVALID")) {
                                    delegate.needShowAlert(LocaleController.getString("InvalidCode", R.string.InvalidCode));
                                } else if (error.text.contains("PHONE_CODE_EXPIRED")) {
                                    delegate.needShowAlert(LocaleController.getString("CodeExpired", R.string.CodeExpired));
                                } else if (error.text.startsWith("FLOOD_WAIT")) {
                                    delegate.needShowAlert(LocaleController.getString("FloodWait", R.string.FloodWait));
                                } else {
                                    delegate.needShowAlert(error.text);
                                }
                            }
                        }
                        if (delegate != null) {
                            delegate.needHideProgress();
                        }
                    }
                });
            }
        }, true, RPCRequest.RPCRequestClassGeneric | RPCRequest.RPCRequestClassFailOnServerErrors | RPCRequest.RPCRequestClassWithoutLogin | RPCRequest.RPCRequestClassTryDifferentDc | RPCRequest.RPCRequestClassEnableUnauthorized);
    }

    @Override
    public void onShow() {
        super.onShow();
        if (phoneField != null) {
            phoneField.requestFocus();
            phoneField.setSelection(phoneField.length());
        }
    }

    @Override
    public String getHeaderName() {
        return LocaleController.getString("YourPhone", R.string.YourPhone);
    }

    @Override
    public void saveStateParams(Bundle bundle) {
        String code = codeField.getText().toString();
        if (code != null && code.length() != 0) {
            bundle.putString("phoneview_code", code);
        }
        String phone = phoneField.getText().toString();
        if (phone != null && phone.length() != 0) {
            bundle.putString("phoneview_phone", phone);
        }
    }

    @Override
    public void restoreStateParams(Bundle bundle) {
        String code = bundle.getString("phoneview_code");
        if (code != null) {
            codeField.setText(code);
        }
        String phone = bundle.getString("phoneview_phone");
        if (phone != null) {
            phoneField.setText(phone);
        }
    }
}
