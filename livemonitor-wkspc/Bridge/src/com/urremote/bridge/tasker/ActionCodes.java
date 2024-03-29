package com.urremote.bridge.tasker;

public class ActionCodes {

	// Meta

	public static final int				 NONE = -1;

	// Normal codes shouldn't be greater than this
	public final static int				 FIRST_PLUGIN_CODE = 1000;
	
	// Deprecated

	public static final int				 LIST_VARIABLES = 594;
	public static final int				 TEST_FILE = 411;
	public static final int				 SET_SPEECH_PARAMS = 876;
	public static final int				 NOTIFY = 500;	
	public static final int				 NOTIFY_LED_RED = 510;	
	public static final int				 NOTIFY_LED_GREEN = 520;
	public static final int				 PERMANENT_NOTIFY = 777;
	public static final int				 NOTIFY_VIBRATE = 530;
	public static final int				 NOTIFY_SOUND = 540;
	public static final int				 SAY_AFTER = 696;  
	public static final int				 SHUT_UP = 697;
	public static final int				 POPUP_IMAGE_TASK_NAMES = 557;	
	public static final int				 TORCH = 511;
	public static final int				 POPUP_TASK_ICONS = 560;
	public static final int				 POPUP_TASK_NAMES = 565;
	
	// UI
	
	public static final int				 DPAD = 701;
	public static final int				 TYPE = 702;
	public static final int				 BUTTON = 703;
	
	// Active 

	public static final int				 NOTIFY_PLAIN = 523;
	public static final int				 NOTIFY_LED = 525;
	public static final int				 NOTIFY_VIBRATE_2 = 536;
	public static final int				 NOTIFY_SOUND_2 = 538;

	public static final int				 CANCEL_NOTIFY = 779;

	public static final int				 FLASH = 548;
	public static final int				 POPUP = 550;
	public static final int				 MENU = 551;
	public static final int				 POPUP_TASK_BUTTONS = 552;
	
	public static final int				 SET_ALARM = 566;
	public static final int				 CALENDAR_INSERT = 567;
	
	public static final int				 REBOOT = 59;
	public static final int				 VIBRATE = 61;
	public static final int				 VIBRATE_PATTERN = 62;

	public static final int				 STATUS_BAR = 512;
	public static final int				 CLOSE_SYSTEM_DIALOGS = 513;
	
	public static final int				 MICROPHONE_MUTE = 301;
	public static final int				 VOLUME_ALARM = 303;
	public static final int				 VOLUME_RINGER = 304;
	public static final int				 VOLUME_NOTIFICATION = 305;
	public static final int				 VOLUME_CALL = 306;
	public static final int				 VOLUME_MUSIC = 307;
	public static final int				 VOLUME_SYSTEM = 308;
	public static final int				 VOLUME_DTMF = 309;
	public static final int				 VOLUME_BT_VOICE = 311;
	public static final int				 SILENT_MODE = 310;

	public static final int				 SOUND_EFFECTS_ENABLED = 136;
	public static final int				 HAPTIC_FEEDBACK = 177;
	public static final int				 SPEAKERPHONE_STATUS = 254;
	public static final int				 RINGER_VIBRATE = 256;
	public static final int				 NOTIFICATION_VIBRATE = 258;
	public static final int				 NOTIFICATION_PULSE = 259;
	
	public static final int				 DIALOG_SETTINGS = 200;
	// API 5+
	public static final int				 DIALOG_ACCESSIBILITY_SETTINGS = 236;
	public static final int				 DIALOG_PRIVACY_SETTINGS = 238;
	
	public static final int				 DIALOG_AIRPLANE_MODE_SETTINGS = 201;
	public static final int				 DIALOG_ADD_ACCOUNT_SETTINGS = 199;
	public static final int				 DIALOG_APN_SETTINGS = 202;
	public static final int				 DIALOG_BATTERY_INFO_SETTINGS = 251;
	public static final int				 DIALOG_DATE_SETTINGS = 203;
	public static final int				 DIALOG_DEVICE_INFO_SETTINGS = 198;
	public static final int				 DIALOG_INTERNAL_STORAGE_SETTINGS = 204;
	public static final int				 DIALOG_DEVELOPMENT_SETTINGS = 205;
	public static final int				 DIALOG_WIFI_SETTINGS = 206;
	public static final int				 DIALOG_LOCATION_SOURCE_SETTINGS = 208;
	public static final int				 DIALOG_INPUT_METHOD_SETTINGS = 210;
	public static final int				 DIALOG_SYNC_SETTINGS = 211;
	public static final int				 DIALOG_WIFI_IP_SETTINGS = 212;
	public static final int				 DIALOG_WIRELESS_SETTINGS = 214;
	public static final int				 DIALOG_APPLICATION_SETTINGS = 216;
	public static final int				 DIALOG_BLUETOOTH_SETTINGS = 218;
	public static final int				 DIALOG_ROAMING_SETTINGS = 220;
	public static final int				 DIALOG_DISPLAY_SETTINGS = 222;
	public static final int				 DIALOG_LOCALE_SETTINGS = 224;
	public static final int				 DIALOG_MANAGE_APPLICATION_SETTINGS = 226;
	public static final int				 DIALOG_MEMORY_CARD_SETTINGS = 227;
	public static final int				 DIALOG_NETWORK_OPERATOR_SETTINGS = 228;
	public static final int				 DIALOG_POWER_USAGE_SETTINGS = 257;
	public static final int				 DIALOG_QUICK_LAUNCH_SETTINGS = 229;
	public static final int				 DIALOG_SECURITY_SETTINGS = 230;
	public static final int				 DIALOG_SEARCH_SETTINGS = 231;
	public static final int				 DIALOG_SOUND_SETTINGS = 232;
	public static final int				 DIALOG_USER_DICTIONARY_SETTINGS = 234;

	public static final int				 INPUT_METHOD_SELECT = 804;
	public static final int				 POKE_DISPLAY = 806;
	public static final int				 SCREEN_BRIGHTNESS_AUTO = 808;	
	public static final int				 SCREEN_BRIGHTNESS = 810;	
	public static final int				 SCREEN_OFF_TIMEOUT = 812;
	public static final int				 ACCELEROMETER_ROTATION = 822;
	public static final int				 STAY_ON_WHILE_PLUGGED_IN = 820;
	public static final int 			 KEYGUARD_ENABLED = 150;	
	public static final int 			 KEYGUARD_PATTERN = 151;	
	public static final int				 LOCK = 15;  
	public static final int				 SYSTEM_LOCK = 16;  
	public static final int				 SHOW_SOFT_KEYBOARD = 987;  
	public static final int				 CAR_MODE = 988;
	public static final int				 NIGHT_MODE = 989;
	
	public static final int				SET_LIGHT = 999;
	
	public static final int				 READ_LINE = 415;
	public static final int				 READ_PARA = 416;
	public static final int				 MOVE_FILE = 400;
	public static final int				 COPY_FILE = 404;
	public static final int				 DELETE_FILE = 406;
	public static final int				 DELETE_DIR = 408;
	public static final int				 MAKE_DIR = 409;
	
	public static final int				 WRITE_TO_FILE = 410;

	public static final int				 LIST_FILES = 412;

	
	public static final int				 ZIP_FILE = 420;
	public static final int				 UNZIP_FILE = 422;
	public static final int				 VIEW_FILE = 102;

	public static final int				 BROWSE_FILES = 900;
	
	public static final int				 GET_FIX = 902;
	public static final int				 GET_VOICE = 903;
	public static final int				 VOICE_COMMAND = 904;
	
	
	// encrypt
	
	public static final int				 ENCRYPT_FILE = 434;
	public static final int				 DECRYPT_FILE = 435;
	public static final int				 ENTER_PASSPHRASE = 436;  

	public static final int				 CLEAR_PASSPHRASE = 437;  
	public static final int				 SET_PASSPHRASE = 423;  
	public static final int				 ENCRYPT_DIR = 428;
	public static final int				 DECRYPT_DIR = 429;
	
	public static final int				 TAKE_PHOTO = 10;  
	public static final int				 TAKE_PHOTO_SERIES = 11;  
	public static final int				 TAKE_PHOTO_CHRON = 12;  

	public static final int				 KILL_APP = 18;  
	public static final int				 LOAD_APP = 20;  
	public static final int				 LOAD_LAST_APP = 22;  
	
	public static final int				SETCPU = 915;
	public static final int				 GO_HOME = 25;  
	public static final int				 WAIT = 30;  	
	public static final int				 WAIT_UNTIL = 35;  	

	public static final int				 IF = 37;  	
	public static final int				 ENDIF = 38;  	
	public static final int				 ELSE = 43;

	public static final int				 FOR = 39;  	
	public static final int				 ENDFOR = 40;  	

	public static final int				 SEARCH = 100;
	
	public static final int				 RUN_SCRIPT = 112;
	public static final int				 RUN_SHELL = 123;
	public static final int				 REMOUNT = 124;
	public static final int				 RETURN = 126;
	
	public static final int				 TEST = 115;
	public static final int				 SCENE_ELEMENT_TEST = 195;
	
	public static final int				 SAY = 559;  
	public static final int				 SAY_TO_FILE = 699;  
	
	public static final int				 SEND_ACTION_INTENT = 878;
	public static final int				 SEND_COMPONENT_INTENT = 879;
	
	public static final int				 VIEW_URL = 104;
	public static final int				 SET_CLIPBOARD = 105;
	public static final int				 SET_WALLPAPER = 109;
	public static final int				 HTTP_GET = 118;
	public static final int				 HTTP_POST = 116;
	
	public static final int				 OPEN_MAP = 119;
	
	public static final int				 ANDROID_MEDIA_CONTROL = 443;  
	public static final int				 GRAB_MEDIA_BUTTON = 490;
	
	public static final int				 PLAY_RINGTONE = 192;  
	
	public static final int				 MUSIC_PLAY = 445;  
	public static final int				 MUSIC_PLAY_DIR = 447;  
	public static final int				 MUSIC_STOP = 449;  
	public static final int				 MUSIC_FORWARD = 451;  
	public static final int				 MUSIC_BACK = 453;  
	
	public static final int				 SCAN_CARD = 459;  
	
	public static final int				 RINGTONE = 457;  

	public static final int				 SOUND_RECORD = 455;  
	public static final int				 SOUND_RECORD_STOP = 657;  

	public static final int				 AUTO_SYNC = 331;	
	
	public static final int				 AIRPLANE_MODE = 333;	
	
	public static final int				 GPS_STATUS = 332;	

	public static final int				 BLUETOOTH_STATUS = 294;
	public static final int				 BLUETOOTH_NAME = 295;
	public static final int				 BLUETOOTH_SCO = 296;
	public static final int				 BLOCK_CALLS = 95;
	public static final int				 DIVERT_CALLS = 97;
	public static final int				 REVERT_CALLS = 99;
	
	public final static int				 CONTACTS = 909;
	public final static int				 CALL_LOG = 910;
	
	public static final int				 EMAIL_COMPOSE = 125;
	public static final int				 SMS_COMPOSE = 250;
	public static final int				 MMS_COMPOSE = 111;
	
	public static final int				 TETHER_WIFI = 113;
	public static final int				 TETHER_USB = 114;
	
	public static final int				 TAKE_CALL = 731;
	public static final int				 RADIO_STATUS = 732;
	public static final int				 END_CALL = 733;
	public static final int				 SILENCE_RINGER = 734;
	public static final int				 MOBILE_NETWORK_MODE = 735;
	public static final int				 MAKE_PHONECALL = 90;

	public static final int				 MOBILE_DATA_STATUS = 450;
	public static final int				 MOBILE_DATA_STATUS_DIRECT = 433;
	
	public static final int				 SEND_TEXT_SMS = 41;
	public static final int				 SEND_DATA_SMS = 42;

	public static final int				 WIFI_STATUS = 425;	
	public static final int				 WIFI_NET_CONTROL = 426;	
	public static final int				 WIFI_SLEEP_POLICY = 427;	

	public static final int				 WIMAX_STATUS = 439;	
	public static final int				 SET_TIMEZONE = 440;
	
	// tasker

	public static final int				 CHANGE_ICON_SET = 140;
	public static final int				 CHANGE_WIDGET_TEXT = 155;
	public static final int				 CHANGE_WIDGET_ICON = 152;
	
	public static final int				 TOGGLE_PROFILE = 159;
	public static final int				 QUERY_ACTION = 134;
	
	public static final int				 RUN_TASK = 130;
	public static final int				 GOTO = 135;
	public static final int				 STOP = 137;
	
	public static final int				 DISABLE_TASKER = 139;

	// scene
	
	public static final int				 CREATE_SCENE = 46;
	public static final int				 SHOW_SCENE = 47;
	public static final int				 HIDE_SCENE = 48;
	public static final int				 DESTROY_SCENE = 49;





	public static final int				 SCENE_ELEMENT_VALUE = 50;
	public static final int				 SCENE_ELEMENT_TEXT = 51;
	public static final int				 SCENE_ELEMENT_TEXT_COLOUR = 54;
	public static final int				 SCENE_ELEMENT_BACKGROUND_COLOUR = 55;
	public static final int				 SCENE_ELEMENT_BORDER = 56;
	public static final int				 SCENE_ELEMENT_POSITION = 57;
	public static final int				 SCENE_ELEMENT_SIZE = 58;
	public static final int				 SCENE_ELEMENT_ADD_GEOMARKER = 60;
	public static final int				 SCENE_ELEMENT_DELETE_GEOMARKER = 63;
	public static final int				 SCENE_ELEMENT_WEB_CONTROL = 53;
	public static final int				 SCENE_ELEMENT_MAP_CONTROL = 64;
	public static final int				 SCENE_ELEMENT_VISIBILITY = 65;
	public static final int				 SCENE_ELEMENT_IMAGE = 66;
	
	// variables
	
	public static final int				 ARRAY_PUSH = 355;
	public static final int				 ARRAY_POP = 356;
	public static final int				 ARRAY_CLEAR = 357;
	public static final int				 SET_VARIABLE = 547;
	public static final int				 SET_VARIABLE_RANDOM = 545;
	public static final int				 INC_VARIABLE = 888;
	public static final int				 DEC_VARIABLE = 890;
	public static final int				 CLEAR_VARIABLE = 549;
	public static final int				 SPLIT_VARIABLE = 590;
	public static final int				 JOIN_VARIABLE = 592;
	public static final int				 QUERY_VARIABLE = 595;
	public static final int				 CONVERT_VARIABLE = 596;
	public static final int				 SECTION_VARIABLE = 597;
	
	// 3rd party
	
	public static final int				 ASTRID = 371;
	public static final int				 BEYONDPOD = 555;
	public static final int				 DAILYROADS = 568;
	public static final int				 DUETODAY = 599;
	public static final int				 ANDROID_NOTIFIER = 558;
	public static final int				 NEWSROB = 556;
	public static final int				 OFFICETALK = 643;
	public static final int				 JUICE_DEFENDER_DATA = 456;
	public static final int				 JUICE_DEFENDER_STATUS = 395;
	public static final int				 SLEEPBOT = 442;
	public static final int				 SMSBACKUP = 553;
	public static final int				 TESLALED = 444;
	public static final int				 WIDGETLOCKER = 458;
	public static final int				 GENTLEALARM = 911;
	
    // all elements
    public static final int				ZOOM_ELEMENT_STATE = 793; 
    public static final int				ZOOM_ELEMENT_POSITION = 794;
    public static final int				ZOOM_ELEMENT_SIZE = 795;
    public static final int				ZOOM_ELEMENT_VISIBILITY = 721;

    // image
    public static final int				ZOOM_ELEMENT_ALPHA = 760;
    public static final int				ZOOM_ELEMENT_IMAGE = 761;
    
    // shapes
    public static final int				ZOOM_ELEMENT_COLOUR = 762;
  
    // text/ button
    public static final int				ZOOM_ELEMENT_TEXT = 740;
    public static final int				ZOOM_ELEMENT_TEXT_SIZE = 741;
    public static final int				ZOOM_ELEMENT_TEXT_COLOUR = 742;
}
