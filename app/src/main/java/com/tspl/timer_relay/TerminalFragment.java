package com.tspl.timer_relay;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Arrays;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private RadioGroup rg;
    private RadioButton rb_hold, rb_reset;
    private Button status1, status2, status3, status4;
    private Button btn_start, btn_hold, btn_reset, radio_send;
    private Button setupBtn, configBtn;
    private Button btn_load, btn_save;
    private Spinner list_of_relay;
    private EditText edt_txt_on, edt_txt_off, select_time;
    private Button relay_on, active_relay, total_time_cycle;
    private String selected_relay = null;
    private CheckBox ch1, ch2, ch3, ch4, ch5, ch6, ch7, ch8, auto_start_check;
    private RadioButton hr, min, sec;
    private RadioGroup time_radio_group;
    private LinearLayout mainPageline;

    private EditText mOutEditText, edt_txt_password, edt_txt_new_password, select_on_time, select_span_time, select_cycle_time, select_sequence;
    private Button mSendButton, status_connected, logout_btn, dcs_settings, relay_status, login_to_device, config_btn, setup_btn;
    private Button reset_password_btn, reset_password, scan, disc, clear_eeprom_btn, btn_dcs_send;
    private LinearLayout relay_status_linear_view, login_linear_page, reset_password_linear_page, setup_linear_page, config_linear_page, dcs_linear_page;

    private String deviceAddress;
    private String newline = "\n";

    private TextView receiveText;

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    public TerminalFragment() {
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );
        setRetainInstance( true );
        deviceAddress = getArguments().getString( "device" );
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService( new Intent( getActivity(), SerialService.class ) );
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach( this );
        else
            getActivity().startService( new Intent( getActivity(), SerialService.class ) ); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach( activity );
        getActivity().bindService( new Intent( getActivity(), SerialService.class ), this, Context.BIND_AUTO_CREATE );
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService( this );
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread( this::connect );
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread( this::connect );
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate( R.layout.fragment_terminal, container, false );

        String[] listOfRelay = {"Select Relay", "0", "1", "2", "3", "4", "5", "6", "7"};
        dcs_settings = view.findViewById( R.id.dcs_settings );
        relay_status = (Button) view.findViewById( R.id.relay_status );
        login_to_device = (Button) view.findViewById( R.id.login_to_device );
        status_connected = (Button) view.findViewById( R.id.status_connected );
        config_btn = (Button) view.findViewById( R.id.config_btn );
        setup_btn = (Button) view.findViewById( R.id.setup_btn );
        reset_password = (Button) view.findViewById( R.id.reset_password );
        reset_password_btn = (Button) view.findViewById( R.id.reset_password_btn );
        select_time = (EditText) view.findViewById( R.id.select_time );

        edt_txt_password = (EditText) view.findViewById( R.id.edt_txt_password );
        edt_txt_password.setFilters( new InputFilter[]{new InputFilter.LengthFilter( 4 )} );


        edt_txt_new_password = (EditText) view.findViewById( R.id.edt_txt_new_password );
        edt_txt_new_password.setFilters( new InputFilter[]{new InputFilter.LengthFilter( 4 )} );


        login_linear_page = (LinearLayout) view.findViewById( R.id.login_linear_page );
        mainPageline = (LinearLayout) view.findViewById( R.id.mainPageline );
        reset_password_linear_page = (LinearLayout) view.findViewById( R.id.reset_password_linear_page );
        setup_linear_page = (LinearLayout) view.findViewById( R.id.setup_linear_page );
        config_linear_page = (LinearLayout) view.findViewById( R.id.config_linear_page );
        relay_status_linear_view = (LinearLayout) view.findViewById( R.id.relay_status_linear_view );

        rg = (RadioGroup) view.findViewById( R.id.radioGroup );
        rb_hold = (RadioButton) view.findViewById( R.id.Radio_hold );
        rb_reset = (RadioButton) view.findViewById( R.id.Radio_reset );

        status1 = (Button) view.findViewById( R.id.Relay_Status_1 );
        status2 = (Button) view.findViewById( R.id.Relay_Status_2 );
        status3 = (Button) view.findViewById( R.id.Relay_Status_3 );
        status4 = (Button) view.findViewById( R.id.Relay_Status_4 );

        btn_start = (Button) view.findViewById( R.id.btn_start );
        btn_hold = (Button) view.findViewById( R.id.btn_hold );
        btn_reset = (Button) view.findViewById( R.id.btn_reset );
        //radio_send = (Button)  findViewById(R.id.send_radio);

        btn_load = (Button) view.findViewById( R.id.btn_load );
        btn_save = (Button) view.findViewById( R.id.btn_save );
        logout_btn = (Button) view.findViewById( R.id.logout_btn );
        clear_eeprom_btn = (Button) view.findViewById( R.id.clear_eeprom_btn );

        list_of_relay = (Spinner) view.findViewById( R.id.list_of_relay );
        ArrayAdapter<String> adapter = new ArrayAdapter<>( getActivity(), android.R.layout.simple_list_item_1, listOfRelay );
        list_of_relay.setAdapter( adapter );

        edt_txt_on = (EditText) view.findViewById( R.id.edt_txt_on );

        relay_on = (Button) view.findViewById( R.id.relay_on );
        active_relay = (Button) view.findViewById( R.id.active_relay );
        total_time_cycle = (Button) view.findViewById( R.id.total_time_cycle );

        edt_txt_on.setFilters( new InputFilter[]{new InputFilter.LengthFilter( 15 )} );
        //  edt_txt_off.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) });

        ch1 = (CheckBox) view.findViewById( R.id.checked1 );
        ch2 = (CheckBox) view.findViewById( R.id.checked2 );
        ch3 = (CheckBox) view.findViewById( R.id.checked3 );
        ch4 = (CheckBox) view.findViewById( R.id.checked4 );
        ch5 = (CheckBox) view.findViewById( R.id.checked5 );
        ch6 = (CheckBox) view.findViewById( R.id.checked6 );
        ch7 = (CheckBox) view.findViewById( R.id.checked7 );
        ch8 = (CheckBox) view.findViewById( R.id.checked8 );
        auto_start_check = (CheckBox) view.findViewById( R.id.autostart_check );

        dcs_linear_page = (LinearLayout) view.findViewById( R.id.dcs_linear_page );
        select_on_time = (EditText) view.findViewById( R.id.select_on_time );
        select_span_time = (EditText) view.findViewById( R.id.select_span_time );
        select_cycle_time = (EditText) view.findViewById( R.id.select_cycle_time );
        select_sequence = (EditText) view.findViewById( R.id.select_sequence );
        select_sequence.setFilters( new InputFilter[]{new InputFilter.LengthFilter( 8 )} );

        btn_dcs_send = (Button) view.findViewById( R.id.btn_dcs_send );

        status_connected = (Button) view.findViewById( R.id.status_connected );

        receiveText = view.findViewById( R.id.receive_text );                          // TextView performance decreases with number of spans
        receiveText.setTextColor( getResources().getColor( R.color.colorRecieveText ) ); // set as default color to reduce number of spans
        receiveText.setMovementMethod( ScrollingMovementMethod.getInstance() );
        TextView sendText = view.findViewById( R.id.send_text );

        View sendBtn = view.findViewById( R.id.send_btn );
        sendBtn.setOnClickListener( v -> {
            send( sendText.getText().toString() );
            sendText.setText( "" );
        } );

        /******************* CLEAR EEPROM BUTTON CLICK EVENT *******************/
        clear_eeprom_btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "CLEAR";
                send( message );
                receiveText.setText( "" );
            }
        } );

        /******************* LOGIN BUTTON CLICK EVENT *******************/
        logout_btn.setOnClickListener( v -> {
            Intent mainPage = new Intent( getContext(), MainActivity.class );
            startActivity( mainPage );
        } );

        login_to_device.setOnClickListener( v -> {

            String get_pwd = edt_txt_password.getText().toString();

            if (get_pwd.length() == 0) {
                edt_txt_password.requestFocus();
                edt_txt_password.setError( "Please enter password" );
            }

            String message = "PWD:" + get_pwd;
            send( message );

        } );

        /******************* RESET PASSWORD CLICK EVENT *******************/

        reset_password_btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset_password_linear_page.setVisibility( View.VISIBLE );
                config_linear_page.setVisibility( View.GONE );
                setup_linear_page.setVisibility( View.GONE );
                relay_status_linear_view.setVisibility( View.GONE );
                dcs_linear_page.setVisibility( View.GONE );
            }
        } );

        /******************* RESET PASSWORD CLICK EVENT *******************/

        reset_password.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String New_Password = edt_txt_new_password.getText().toString();
                String message = "NPWD:" + New_Password;
                send( message );
                Toast.makeText( getContext(), "Password Changed Successfully", Toast.LENGTH_LONG ).show();
                reset_password_linear_page.setVisibility( View.GONE );
                edt_txt_new_password.setText( "" );
                dcs_linear_page.setVisibility( View.GONE );
            }
        } );

        /******************* RELAY STATUS CLICK EVENT *******************/

        relay_status.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                relay_status_linear_view.setVisibility( View.VISIBLE );
                String message = "READ";
                byte[] bytes = message.getBytes( Charset.defaultCharset() );
                config_linear_page.setVisibility( View.GONE );
                setup_linear_page.setVisibility( View.GONE );
                reset_password_linear_page.setVisibility( View.GONE );
                dcs_linear_page.setVisibility( View.GONE );
                send( message );
            }
        } );

        /******************* CONFIG BUTTON CLICK EVENT *******************/

        config_btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                config_linear_page.setVisibility( View.VISIBLE );
                setup_linear_page.setVisibility( View.GONE );
                reset_password_linear_page.setVisibility( View.GONE );
                relay_status_linear_view.setVisibility( View.GONE );
                dcs_linear_page.setVisibility( View.GONE );

            }
        } );

        /******************* SETUP BUTTON CLICK EVENT *******************/
        setup_btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setup_linear_page.setVisibility( View.VISIBLE );
                config_linear_page.setVisibility( View.GONE );
                reset_password_linear_page.setVisibility( View.GONE );
                relay_status_linear_view.setVisibility( View.GONE );
                dcs_linear_page.setVisibility( View.GONE );
            }
        } );

        /******************* DCS SETTING BUTTON CLICK EVENT *******************/

        dcs_settings.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dcs_linear_page.setVisibility( View.VISIBLE );
                setup_linear_page.setVisibility( View.GONE );
                config_linear_page.setVisibility( View.GONE );
                reset_password_linear_page.setVisibility( View.GONE );
                relay_status_linear_view.setVisibility( View.GONE );
            }
        } );

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getContext() );

        /******************* DCS SETTING BUTTON CLICK EVENT *******************/
        select_on_time.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate( getContext(), R.layout.time_dialog, null );
                final NumberPicker numberPickerHour = view.findViewById( R.id.numpicker_hours );
                numberPickerHour.setMaxValue( 23 );
                numberPickerHour.setValue( sharedPreferences.getInt( "Hours", 0 ) );
                final NumberPicker numberPickerMinutes = view.findViewById( R.id.numpicker_minutes );
                numberPickerMinutes.setMaxValue( 59 );
                numberPickerMinutes.setValue( sharedPreferences.getInt( "Minutes", 0 ) );
                final NumberPicker numberPickerSeconds = view.findViewById( R.id.numpicker_seconds );
                numberPickerSeconds.setMaxValue( 59 );
                numberPickerSeconds.setValue( sharedPreferences.getInt( "Seconds", 0 ) );
                Button cancel = view.findViewById( R.id.cancel );
                Button ok = view.findViewById( R.id.ok );
                AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
                builder.setView( view );
                final AlertDialog alertDialog = builder.create();
                cancel.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                } );
                ok.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //select_time.setText(numberPickerHour.getValue() + ":" + numberPickerMinutes.getValue() + ":" + numberPickerSeconds.getValue());
                        select_on_time.setText( String.format( "%02d:%02d:%02d", numberPickerHour.getValue(), numberPickerMinutes.getValue(), numberPickerSeconds.getValue() ) );
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt( "Hours", numberPickerHour.getValue() );
                        editor.putInt( "Minutes", numberPickerMinutes.getValue() );
                        editor.putInt( "Seconds", numberPickerSeconds.getValue() );
                        editor.apply();
                        alertDialog.dismiss();
                    }
                } );
                alertDialog.show();
            }
        } );

        /******************* DCS SETTING BUTTON CLICK EVENT *******************/
        select_span_time.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate( getContext(), R.layout.time_dialog, null );
                final NumberPicker numberPickerHour = view.findViewById( R.id.numpicker_hours );
                numberPickerHour.setMaxValue( 23 );
                numberPickerHour.setValue( sharedPreferences.getInt( "Hours", 0 ) );
                final NumberPicker numberPickerMinutes = view.findViewById( R.id.numpicker_minutes );
                numberPickerMinutes.setMaxValue( 59 );
                numberPickerMinutes.setValue( sharedPreferences.getInt( "Minutes", 0 ) );
                final NumberPicker numberPickerSeconds = view.findViewById( R.id.numpicker_seconds );
                numberPickerSeconds.setMaxValue( 59 );
                numberPickerSeconds.setValue( sharedPreferences.getInt( "Seconds", 0 ) );
                Button cancel = view.findViewById( R.id.cancel );
                Button ok = view.findViewById( R.id.ok );
                AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
                builder.setView( view );
                final AlertDialog alertDialog = builder.create();
                cancel.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                } );
                ok.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //select_time.setText(numberPickerHour.getValue() + ":" + numberPickerMinutes.getValue() + ":" + numberPickerSeconds.getValue());
                        select_span_time.setText( String.format( "%02d:%02d:%02d", numberPickerHour.getValue(), numberPickerMinutes.getValue(), numberPickerSeconds.getValue() ) );
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt( "Hours", numberPickerHour.getValue() );
                        editor.putInt( "Minutes", numberPickerMinutes.getValue() );
                        editor.putInt( "Seconds", numberPickerSeconds.getValue() );
                        editor.apply();
                        alertDialog.dismiss();
                    }
                } );
                alertDialog.show();
            }
        } );


        /******************* DCS SETTING BUTTON CLICK EVENT *******************/
        select_cycle_time.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate( getContext(), R.layout.time_dialog, null );
                final NumberPicker numberPickerHour = view.findViewById( R.id.numpicker_hours );
                numberPickerHour.setMaxValue( 23 );
                numberPickerHour.setValue( sharedPreferences.getInt( "Hours", 0 ) );
                final NumberPicker numberPickerMinutes = view.findViewById( R.id.numpicker_minutes );
                numberPickerMinutes.setMaxValue( 59 );
                numberPickerMinutes.setValue( sharedPreferences.getInt( "Minutes", 0 ) );
                final NumberPicker numberPickerSeconds = view.findViewById( R.id.numpicker_seconds );
                numberPickerSeconds.setMaxValue( 59 );
                numberPickerSeconds.setValue( sharedPreferences.getInt( "Seconds", 0 ) );
                Button cancel = view.findViewById( R.id.cancel );
                Button ok = view.findViewById( R.id.ok );
                AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
                builder.setView( view );
                final AlertDialog alertDialog = builder.create();
                cancel.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                } );
                ok.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //select_time.setText(numberPickerHour.getValue() + ":" + numberPickerMinutes.getValue() + ":" + numberPickerSeconds.getValue());
                        select_cycle_time.setText( String.format( "%02d:%02d:%02d", numberPickerHour.getValue(), numberPickerMinutes.getValue(), numberPickerSeconds.getValue() ) );
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt( "Hours", numberPickerHour.getValue() );
                        editor.putInt( "Minutes", numberPickerMinutes.getValue() );
                        editor.putInt( "Seconds", numberPickerSeconds.getValue() );
                        editor.apply();
                        alertDialog.dismiss();
                    }
                } );
                alertDialog.show();
            }
        } );

        btn_dcs_send.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String onTime, spanTime, cycleTime, sequence;

                /**** ON TIME ****/
                onTime = select_on_time.getText().toString();
                String on_Time[] = onTime.split( ":" );
                int h = Integer.parseInt( on_Time[0] );
                h = h * 3600;
                int m = Integer.parseInt( on_Time[1] );
                m = m * 60;
                int s = Integer.parseInt( on_Time[2] );
                int Full_On_Time = h + m + s;
                String dcs_on_time = String.valueOf( Full_On_Time );

                /**** SPAN TIME ****/
                spanTime = select_span_time.getText().toString();
                String span_Time[] = spanTime.split( ":" );
                h = Integer.parseInt( span_Time[0] );
                h = h * 3600;
                m = Integer.parseInt( span_Time[1] );
                m = m * 60;
                s = Integer.parseInt( span_Time[2] );
                int Full_Span_Time = h + m + s;
                String dcs_span_time = String.valueOf( Full_Span_Time );

                /**** CYCLE TIME ****/
                cycleTime = select_cycle_time.getText().toString();
                String cycle_Time[] = cycleTime.split( ":" );
                h = Integer.parseInt( cycle_Time[0] );
                h = h * 3600;
                m = Integer.parseInt( cycle_Time[1] );
                m = m * 60;
                s = Integer.parseInt( cycle_Time[2] );
                int Full_Cycle_Time = h + m + s;
                String dcs_cycle_time = String.valueOf( Full_Cycle_Time );

                /**** SEQUENCE TIME ****/
                sequence = select_sequence.getText().toString();

                String message = "DCS:" + dcs_on_time + "," + dcs_span_time + "," + dcs_cycle_time + "," + sequence;
                send( message );
                select_on_time.setText( "" );
                select_span_time.setText( "" );
                select_cycle_time.setText( "" );
                select_sequence.setText( "" );
            }
        } );

        /******************* SETUP HOLD RESET BUTTON CLICK EVENT *******************/
        rg.setOnCheckedChangeListener( new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                View radioButton = rg.findViewById( checkedId );
                int index = rg.indexOfChild( radioButton );
                switch (index) {
                    case 0:
                        String selectedRadioButtonText1 = "CONFIG:HOLD";
                        send( selectedRadioButtonText1 );
                        break;
                    case 1:
                        String selectedRadioButtonText2 = "CONFIG:RESET";
                        send( selectedRadioButtonText2 );
                        break;
                }
            }
        } );

        /******************* SETUP START BUTTON CLICK EVENT *******************/
        btn_start.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "START";
                send( message );
            }
        } );

        /******************* SETUP HOLD BUTTON CLICK EVENT *******************/
        btn_hold.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "HOLD";
                send( message );
            }
        } );

        /******************* SETUP RESET BUTTON CLICK EVENT *******************/
        btn_reset.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "RESET";
                send( message );
            }
        } );

        /******************* SETUP AUTO START CLICK EVENT *******************/
        auto_start_check.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (auto_start_check.isChecked()) {
                    String message = "CONFIG>AUTOSTART:1";
                    send( message );
                } else {
                    String message = "CONFIG>AUTOSTART:0";
                    send( message );
                }
            }
        } );


        /******************* CONFIG SAVE BUTTON CLICK EVENT *******************/
        btn_save.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "SAVE";
                send( message );
            }
        } );

        /******************* CONFIG LOAD BUTTON CLICK EVENT *******************/
        btn_load.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "LOAD";
                send( message );
            }
        } );

        /******************* CONFIG TRANSITION TIME CLICK EVENT *******************/
        relay_on.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selected_relay = list_of_relay.getSelectedItem().toString();
                String[] strings = edt_txt_on.getText().toString().split( "," );
                Arrays.sort( strings );
                String output = TextUtils.join( ",", strings );
                String message = "CONFIG>T:" + selected_relay + ":" + output;
                send( message );
                edt_txt_on.setText( "" );
            }
        } );

        /******************* CONFIG ACTIVE RELAY CLICK EVENT *******************/
        active_relay.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "";
                if (ch1.isChecked()) {
                    msg = msg + "1,";
                } else {
                    msg = msg + "0,";
                }
                if (ch2.isChecked()) {
                    msg = msg + "1,";
                } else {
                    msg = msg + "0,";
                }
                if (ch3.isChecked()) {
                    msg = msg + "1,";
                } else {
                    msg = msg + "0,";
                }
                if (ch4.isChecked()) {
                    msg = msg + "1";
                } else {
                    msg = msg + "0,";
                }
                if (ch5.isChecked()) {
                    msg = msg + "1";
                } else {
                    msg = msg + "0,";
                }
                if (ch6.isChecked()) {
                    msg = msg + "1";
                } else {
                    msg = msg + "0,";
                }
                if (ch7.isChecked()) {
                    msg = msg + "1";
                } else {
                    msg = msg + "0,";
                }
                if (ch8.isChecked()) {
                    msg = msg + "1";
                } else {
                    msg = msg + "0";
                }
                String message = "CONFIG>ACTIVE:" + msg;
                send( message );
            }
        } );

        /******************* CONFIG TOTAL TIME CLICK EVENT *******************/
        select_time.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate( getActivity(), R.layout.time_dialog, null );
                final NumberPicker numberPickerHour = view.findViewById( R.id.numpicker_hours );
                numberPickerHour.setMaxValue( 23 );
                numberPickerHour.setValue( sharedPreferences.getInt( "Hours", 0 ) );
                final NumberPicker numberPickerMinutes = view.findViewById( R.id.numpicker_minutes );
                numberPickerMinutes.setMaxValue( 59 );
                numberPickerMinutes.setValue( sharedPreferences.getInt( "Minutes", 0 ) );
                final NumberPicker numberPickerSeconds = view.findViewById( R.id.numpicker_seconds );
                numberPickerSeconds.setMaxValue( 59 );
                numberPickerSeconds.setValue( sharedPreferences.getInt( "Seconds", 0 ) );
                Button cancel = view.findViewById( R.id.cancel );
                Button ok = view.findViewById( R.id.ok );
                AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
                builder.setView( view );
                final AlertDialog alertDialog = builder.create();
                cancel.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                } );
                ok.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //select_time.setText(numberPickerHour.getValue() + ":" + numberPickerMinutes.getValue() + ":" + numberPickerSeconds.getValue());
                        select_time.setText( String.format( "%02d:%02d:%02d", numberPickerHour.getValue(), numberPickerMinutes.getValue(), numberPickerSeconds.getValue() ) );
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt( "Hours", numberPickerHour.getValue() );
                        editor.putInt( "Minutes", numberPickerMinutes.getValue() );
                        editor.putInt( "Seconds", numberPickerSeconds.getValue() );
                        editor.apply();
                        alertDialog.dismiss();
                    }
                } );
                alertDialog.show();
            }
        } );

        /******************* CONFIG TOTAL TIME CLICK EVENT *******************/
        total_time_cycle.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = select_time.getText().toString();
                String total_time[] = message.split( ":" );
                int h = Integer.parseInt( total_time[0] );
                h = h * 3600;
                int m = Integer.parseInt( total_time[1] );
                m = m * 60;
                int s = Integer.parseInt( total_time[2] );
                int Full_Total_Time = h + m + s;
                String send_time = String.valueOf( Full_Total_Time );
                message = "CONFIG>CYCLE:" + send_time;
                send( message );
                select_time.setText( "" );
                Toast.makeText( getContext(), "Total Time : " + Full_Total_Time, Toast.LENGTH_SHORT ).show();

            }
        } );

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate( R.menu.menu_terminal, menu );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText( "" );
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray( R.array.newline_names );
            String[] newlineValues = getResources().getStringArray( R.array.newline_values );
            int pos = java.util.Arrays.asList( newlineValues ).indexOf( newline );
            AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
            builder.setTitle( "Newline" );
            builder.setSingleChoiceItems( newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            } );
            builder.create().show();
            return true;
        } else {
            return super.onOptionsItemSelected( item );
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice( deviceAddress );
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status( "connecting..." );
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect( this, "Connected to " + deviceName );
            socket.connect( getContext(), service, device );
        } catch (Exception e) {
            onSerialConnectError( e );
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText( getActivity(), "not connected", Toast.LENGTH_SHORT ).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder( str + '\n' );
            spn.setSpan( new ForegroundColorSpan( getResources().getColor( R.color.colorSendText ) ), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
            receiveText.append( spn );
            byte[] data = (str + '\n').getBytes();
            socket.write( data );
        } catch (Exception e) {
            onSerialIoError( e );
        }
    }

    private void receive(byte[] data) {
        receiveText.append( new String( data ) );
    }

    private void status(String str) {
        if (str.equals( "connected" )) {
            SpannableStringBuilder spn = new SpannableStringBuilder( str + '\n' );
            spn.setSpan( new ForegroundColorSpan( getResources().getColor( R.color.colorStatusText ) ), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
            receiveText.append( spn );
            status_connected.setBackgroundDrawable( getResources().getDrawable( R.drawable.connection_on ) );
            //login_linear_page.setVisibility( View.VISIBLE );
            mainPageline.setVisibility( View.VISIBLE );
            Toast.makeText( service, "Device connected...", Toast.LENGTH_SHORT ).show();
        } else {
            SpannableStringBuilder spn = new SpannableStringBuilder( str + '\n' );
            spn.setSpan( new ForegroundColorSpan( getResources().getColor( R.color.colorStatusText ) ), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
            receiveText.append( spn );
        }
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status( "connected" );
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status( "connection failed: " + e.getMessage() );
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive( data );
    }

    @Override
    public void onSerialIoError(Exception e) {
        status( "connection lost: " + e.getMessage() );
        disconnect();
    }

}
