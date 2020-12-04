package com.tspl.timer_relay;

class Constants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = "com.tspl.timer_relay.Disconnect";
    static final String NOTIFICATION_CHANNEL = "com.tspl.timer_relay.Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = "com.tspl.timer_relay.MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {}
}
