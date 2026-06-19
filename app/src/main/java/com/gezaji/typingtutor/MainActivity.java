package com.gezaji.typingtutor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.input.InputManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements InputManager.InputDeviceListener {

    static final int C_BG         = 0xFF0A0A0A;
    static final int C_PANEL      = 0xFF111111;
    static final int C_KEY_BODY   = 0xFF1A1A1A;
    static final int C_KEY_BORDER = 0xFF2E2E2E;
    static final int C_KEY_LABEL  = 0xFFE8E8E8;
    static final int C_KEY_CORRECT= 0xFF22C55E;
    static final int C_KEY_ERROR  = 0xFFEF4444;
    static final int C_CURSOR     = 0xFF58A6FF;
    static final int C_TYPED      = 0xFF22C55E;
    static final int C_UNTYPED    = 0xFF606060;
    static final int C_LED_CAPS   = 0xFFEF4444;
    static final int C_LED_NUM    = 0xFF22C55E;
    static final int C_LED_SCROLL = 0xFF58A6FF;
    static final int C_LED_OFF    = 0xFF252525;
    static final int C_ACCENT     = 0xFF58A6FF;

    // Lesson types
    static final int TYPE_CHAR    = 0; // match by unicode character
    static final int TYPE_KEYCODE = 1; // match by keycode sequence

    static final String[] LESSON_TITLES = {
        "Home Row - Left Hand",   "Home Row - Right Hand",  "Full Home Row",
        "Top Row - Left Hand",    "Top Row - Right Hand",   "Full Top Row",
        "Bottom Row - Left Hand", "Bottom Row - Right Hand","Full Bottom Row",
        "Numbers 1-5",            "Numbers 6-0",            "All Numbers",
        "Capital Letters",        "Common Punctuation",     "Short Words",
        "Medium Words",           "Long Words",             "Short Sentences",
        "Paragraphs",             "Speed Challenge",
        "Number Row - Left",      "Number Row - Right",     "Full Number Row",
        "Numpad - Top Row",       "Numpad - Middle Row",    "Numpad - Bottom Row",
        "Full Numpad",            "Arrow Keys",             "Special Keys",
        "Function Keys F1-F6",    "Function Keys F7-F12",
    };

    static final int[] LESSON_TYPES = {
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR, TYPE_CHAR, TYPE_CHAR,
        TYPE_CHAR,
        TYPE_KEYCODE, TYPE_KEYCODE,
        TYPE_KEYCODE, TYPE_KEYCODE,
    };

    // For TYPE_CHAR lessons — normal text
    static final String[] LESSONS = {
        "asdf asdf asdf fdsa fdsa fdsa afsd dafs sfda fads asdf fdsa",
        "jkl; jkl; ;lkj ;lkj jkl; klj; ljk; ;lkj jkl; ;lkj",
        "asdfjkl; asdf jkl; fdsa ;lkj ask lad fall glad flask slab",
        "qwert qwert trewq trewq qwer wert erqw rqew qwerty",
        "yuiop yuiop poiuy poiuy yui iop opu pou yuiop poiuy",
        "qwerty uiop type rope quit your pour riot true pout",
        "zxcvb zxcvb bvcxz bvcxz zxcv xcvb cvbz vbzx zxcvb",
        "nm,./ nm,./ /.,mn /.,mn nm., .,nm nm,./ /.,mn",
        "zxcvbnm,./ vex numb zinc calm back exam next",
        "12345 54321 1234 2345 3451 4512 5123 11 22 33 44 55",
        "67890 09876 6789 7890 8906 9067 0678 66 77 88 99 00",
        "1234567890 0987654321 246 135 864 753 9801 5274 3690",
        "The Quick Brown Fox Ada Alan Grace Linus Ken Dennis",
        "Hello, world! How are you? It's fine. Wait: really; yes.",
        "cat bat sat hat mat rat fat pat vat can fan man ran tan",
        "finger garden mother finger window butter garden follow",
        "keyboard programming technology education motivation discipline",
        "The cat sat on the mat. A dog ran by the park. She can type fast.",
        "Practice makes perfect. Every expert was once a beginner. Keep your eyes on the screen and your fingers on the home row. Speed will follow accuracy.",
        "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. How vexingly quick daft zebras jump!",
        "1 2 3 4 5 1 2 3 4 5 12 23 34 45 51 15 24 35 41 52",
        "6 7 8 9 0 6 7 8 9 0 67 78 89 90 06 60 79 68 90 07",
        "1234567890 9876543210 13579 24680 11 22 33 44 55 66 77 88 99 00",
        "7 8 9 7 8 9 78 89 97 79 88 77 99 789 987 798 879",
        "4 5 6 4 5 6 45 56 64 46 55 44 66 456 654 465 546",
        "1 2 3 1 2 3 12 23 31 13 22 11 33 123 321 132 213",
        "789 456 123 0 789 456 123 0 7410 8520 9630 147 258 369",
        "", // Arrow keys — keycode lesson, text unused
        "", // Special keys — keycode lesson
        "", // F1-F6
        "", // F7-F12
    };

    // For TYPE_KEYCODE lessons — sequence of keycodes
    static final int[][] KEYCODE_LESSONS = new int[LESSON_TITLES.length][];
    static final String[][] KEYCODE_LABELS = new String[LESSON_TITLES.length][];

    static {
        // Index 27: Arrow keys
        KEYCODE_LESSONS[27] = new int[]{
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT,
        };
        KEYCODE_LABELS[27] = new String[]{
            "Up","Up","Dn","Dn","Lt","Lt","Rt","Rt",
            "Up","Dn","Lt","Rt","Up","Lt","Dn","Rt",
            "Up","Dn","Rt","Lt",
        };

        // Index 28: Special keys
        KEYCODE_LESSONS[28] = new int[]{
            KeyEvent.KEYCODE_TAB,   KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DEL,   KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_FORWARD_DEL,
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,  KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,   KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_TAB,   KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DEL,   KeyEvent.KEYCODE_ESCAPE,
        };
        KEYCODE_LABELS[28] = new String[]{
            "Tab","Tab","Enter","Enter","BkSp","BkSp","Del","Del",
            "Esc","Esc","LShift","RShift","LCtrl","RCtrl","LAlt","RAlt",
            "Tab","Enter","BkSp","Esc",
        };

        // Index 29: F1-F6
        KEYCODE_LESSONS[29] = new int[]{
            KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_F3, KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_F6,
            KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F3,
            KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_F4, KeyEvent.KEYCODE_F6,
            KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_F3, KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_F6,
            KeyEvent.KEYCODE_F6, KeyEvent.KEYCODE_F1,
        };
        KEYCODE_LABELS[29] = new String[]{
            "F1","F2","F3","F4","F5","F6",
            "F1","F3","F5","F2","F4","F6",
            "F1","F2","F3","F4","F5","F6",
            "F6","F1",
        };

        // Index 30: F7-F12
        KEYCODE_LESSONS[30] = new int[]{
            KeyEvent.KEYCODE_F7,  KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F9,  KeyEvent.KEYCODE_F10,
            KeyEvent.KEYCODE_F11, KeyEvent.KEYCODE_F12,
            KeyEvent.KEYCODE_F7,  KeyEvent.KEYCODE_F9,
            KeyEvent.KEYCODE_F11, KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F10, KeyEvent.KEYCODE_F12,
            KeyEvent.KEYCODE_F7,  KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F9,  KeyEvent.KEYCODE_F10,
            KeyEvent.KEYCODE_F11, KeyEvent.KEYCODE_F12,
            KeyEvent.KEYCODE_F12, KeyEvent.KEYCODE_F7,
        };
        KEYCODE_LABELS[30] = new String[]{
            "F7","F8","F9","F10","F11","F12",
            "F7","F9","F11","F8","F10","F12",
            "F7","F8","F9","F10","F11","F12",
            "F12","F7",
        };
    }

    // Numpad lesson indices
    static final int NUMPAD_LESSON_START = 23;
    static final int NUMPAD_LESSON_END   = 26;

    private enum Screen { NO_KEYBOARD, MENU, LESSON, RESULTS }
    private Screen currentScreen = Screen.NO_KEYBOARD;
    private int currentLessonIndex = 0;
    private int cursorPos = 0;
    private int errorCount = 0;
    private int totalTyped = 0;
    private boolean lessonRunning = false;
    private long startTimeMs = 0;
    private boolean capsLockOn = false;
    private boolean numLockOn  = true;
    private boolean scrollLockOn = false;

    private TextView tvLessonText;
    private TextView tvTypingBox;
    private TextView tvWpm, tvAcc, tvProg;
    private KeyboardView keyboardView;
    private LedView ledView;
    private LinearLayout layoutNoKeyboard, layoutMenu, layoutLesson, layoutResults;
    private TextView tvResultsText;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private InputManager inputManager;
    private boolean externalKeyboardConnected = false;

    private SoundPool soundPool;
    private int sndNormal, sndSpace, sndWrong, sndCaps, sndNum, sndScroll;
    private boolean soundsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        initSounds();
        buildUI();
        inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, null);
        checkKeyboard();
        syncLockKeys();
    }

    private void syncLockKeys() {
        // Sync caps lock state from system on startup
        android.view.KeyCharacterMap kcm = android.view.KeyCharacterMap.load(
                android.view.KeyCharacterMap.VIRTUAL_KEYBOARD);
        // We can't directly read lock state on Android without root,
        // so we default to off and let toggling handle it
        capsLockOn = false;
        numLockOn  = true;
        scrollLockOn = false;
        if (ledView != null) ledView.update(capsLockOn, numLockOn, scrollLockOn);
    }

    private void initSounds() {
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(aa)
                .build();
        soundPool.setOnLoadCompleteListener((sp, id, status) -> {
            if (status == 0) soundsLoaded = true;
        });
        sndNormal = soundPool.load(this, R.raw.normal_key_press, 1);
        sndSpace  = soundPool.load(this, R.raw.space_key_press,  1);
        sndWrong  = soundPool.load(this, R.raw.wrong_keypress,   1);
        sndCaps   = soundPool.load(this, R.raw.caps_on,          1);
        sndNum    = soundPool.load(this, R.raw.numlock_on,       1);
        sndScroll = soundPool.load(this, R.raw.scroll_lock,      1);
    }

    private void playSound(int soundId) {
        if (soundsLoaded && soundId > 0)
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    private void buildUI() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(C_BG);

        // No-keyboard screen
        layoutNoKeyboard = new LinearLayout(this);
        layoutNoKeyboard.setOrientation(LinearLayout.VERTICAL);
        layoutNoKeyboard.setGravity(android.view.Gravity.CENTER);
        layoutNoKeyboard.setBackgroundColor(C_BG);
        layoutNoKeyboard.setPadding(dp(40), dp(40), dp(40), dp(40));
        TextView nkIcon = makeTv("[ No Keyboard ]", 24, C_KEY_ERROR, Typeface.BOLD);
        nkIcon.setGravity(android.view.Gravity.CENTER);
        TextView nkTitle = makeTv("No External Keyboard Detected", 20, 0xFFF0F0F0, Typeface.BOLD);
        nkTitle.setGravity(android.view.Gravity.CENTER);
        nkTitle.setPadding(0, dp(12), 0, dp(8));
        TextView nkMsg = makeTv(
            "Connect a keyboard via USB OTG or Bluetooth.\nThe app will continue automatically.",
            14, 0xFF888888, Typeface.NORMAL);
        nkMsg.setGravity(android.view.Gravity.CENTER);
        layoutNoKeyboard.addView(nkIcon);
        layoutNoKeyboard.addView(nkTitle);
        layoutNoKeyboard.addView(nkMsg);
        root.addView(layoutNoKeyboard, matchParent());

        // Menu screen
        layoutMenu = new LinearLayout(this);
        layoutMenu.setOrientation(LinearLayout.VERTICAL);
        layoutMenu.setBackgroundColor(C_BG);
        layoutMenu.setPadding(dp(32), dp(24), dp(32), dp(24));
        layoutMenu.setVisibility(View.GONE);
        TextView menuTitle = makeTv("GEZAJI Typing Tutor v2.0 - Select Lesson", 18, C_ACCENT, Typeface.BOLD);
        menuTitle.setPadding(0, 0, 0, dp(16));
        layoutMenu.addView(menuTitle);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        LinearLayout lessonList = new LinearLayout(this);
        lessonList.setOrientation(LinearLayout.VERTICAL);

        int[] sectionStarts = { 0, 20, 23, 27, 29 };
        String[] sectionHeaders = {
            "-- CLASSIC LESSONS --",
            "-- NUMBER ROW --",
            "-- NUMPAD --",
            "-- NAVIGATION & SPECIAL --",
            "-- FUNCTION KEYS --",
        };

        for (int i = 0; i < LESSON_TITLES.length; i++) {
            for (int s = 0; s < sectionStarts.length; s++) {
                if (sectionStarts[s] == i) {
                    TextView sec = makeTv(sectionHeaders[s], 11, 0xFF555555, Typeface.BOLD);
                    sec.setPadding(dp(16), dp(10), dp(16), dp(4));
                    lessonList.addView(sec);
                }
            }
            final int idx = i;
            TextView btn = makeTv(String.format("  %02d.  %s", i+1, LESSON_TITLES[i]),
                    15, 0xFFCCCCCC, Typeface.NORMAL);
            btn.setBackgroundColor(C_PANEL);
            btn.setPadding(dp(16), dp(10), dp(16), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(4));
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> startLesson(idx));
            lessonList.addView(btn);
        }

        // Info sections
        TextView divider = makeTv("-----------------------------------", 10, 0xFF333333, Typeface.NORMAL);
        divider.setPadding(dp(16), dp(12), dp(16), dp(12));
        lessonList.addView(divider);

        String[][] sections = {
            {"[ HELP ]",
             "GETTING STARTED\n" +
             "1. Connect a USB keyboard via OTG adapter.\n" +
             "2. Select a lesson from the menu.\n" +
             "3. Type the text shown. Keystrokes are highlighted in real time.\n" +
             "4. Complete the lesson to see your results.\n\n" +
             "LESSONS\n" +
             "31 progressive lessons covering all keyboard zones.\n\n" +
             "WPM & ACCURACY\n" +
             "- WPM: characters typed divided by 5, per minute\n" +
             "- Accuracy: correct keystrokes as a percentage of total\n" +
             "- Progress: how far along the current lesson you are\n\n" +
             "LED INDICATORS\n" +
             "- Caps Lock: Red\n" +
             "- Num Lock: Green\n" +
             "- Scroll Lock: Blue\n\n" +
             "NAVIGATION\n" +
             "- Use Arrow Keys to move and Enter to select a lesson.\n" +
             "- Press F5 or the X Menu button to return to menu at any time."},
            {"[ VERSION INFO ]",
             "GEZAJI Typing Tutor v2.0\n\n" +
             "31 lessons - Full 104-key visual keyboard - OTG support\n" +
             "WPM & accuracy tracking - LED indicators - Sound feedback\n" +
             "Number row, Numpad, Arrow & Special key trainers\n" +
             "Fully offline\n\n" +
             "A significant portion of this app was written with the\n" +
             "assistance of Claude AI."},
            {"[ PRIVACY POLICY ]",
             "No data collected. No ads. No trackers. No analytics.\n" +
             "No internet required or requested.\n" +
             "No background or network permissions.\n" +
             "Everything runs locally. Nothing leaves your device."},
            {"[ USER AGREEMENT & LICENSE ]",
             "GEZAJI Typing Tutor is free and open source software.\n" +
             "Use, modify, and redistribute freely for any purpose.\n\n" +
             "Built out of frustration - no simple truly offline OTG\n" +
             "typing tutor existed for Android. Available options hid\n" +
             "features behind paywalls, faked offline support, or\n" +
             "bundled ads and trackers.\n\n" +
             "Bugs? Fix it or report it:\n" +
             "github.com/gezaji404/GezajiTypingTutor\n\n" +
             "(C) GEZAJI_LABS 2026"},
        };

        for (String[] sec : sections) {
            final String title = sec[0];
            final String body  = sec[1];
            TextView header = makeTv(title, 14, C_ACCENT, Typeface.BOLD);
            header.setBackgroundColor(C_PANEL);
            header.setPadding(dp(16), dp(10), dp(16), dp(10));
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            hlp.setMargins(0, dp(4), 0, 0);
            header.setLayoutParams(hlp);
            header.setFocusable(false);
            TextView content = makeTv(body, 13, 0xFFAAAAAA, Typeface.NORMAL);
            content.setBackgroundColor(0xFF0D0D0D);
            content.setPadding(dp(20), dp(10), dp(20), dp(12));
            content.setVisibility(View.GONE);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            content.setLayoutParams(clp);
            header.setOnClickListener(v -> content.setVisibility(
                    content.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
            lessonList.addView(header);
            lessonList.addView(content);
        }

        sv.addView(lessonList);
        layoutMenu.addView(sv);
        root.addView(layoutMenu, matchParent());

        // Lesson screen
        layoutLesson = new LinearLayout(this);
        layoutLesson.setOrientation(LinearLayout.VERTICAL);
        layoutLesson.setBackgroundColor(C_BG);
        layoutLesson.setVisibility(View.GONE);

        tvLessonText = makeTv("", 15, 0xFFFFFFFF, Typeface.NORMAL);
        tvLessonText.setPadding(dp(16), dp(8), dp(16), dp(8));
        tvLessonText.setBackgroundColor(C_PANEL);
        tvLessonText.setSingleLine(false);
        tvLessonText.setMaxLines(3);
        LinearLayout.LayoutParams ltLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ltLP.setMargins(dp(8), dp(6), dp(8), 0);
        layoutLesson.addView(tvLessonText, ltLP);

        tvTypingBox = makeTv("", 18, 0xFFFFFFFF, Typeface.NORMAL);
        tvTypingBox.setPadding(dp(14), dp(8), dp(14), dp(8));
        tvTypingBox.setBackgroundColor(0xFF0D0D0D);
        tvTypingBox.setSingleLine(false);
        tvTypingBox.setMaxLines(2);
        tvTypingBox.setMinHeight(dp(48));
        LinearLayout.LayoutParams tbLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tbLP.setMargins(dp(8), dp(4), dp(8), dp(4));
        layoutLesson.addView(tvTypingBox, tbLP);

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(dp(8), dp(2), dp(8), dp(2));
        tvWpm  = makeStatChip("0 WPM",  C_ACCENT);
        tvAcc  = makeStatChip("100%",   C_KEY_CORRECT);
        tvProg = makeStatChip("0 / 0",  0xFFD29922);
        statsRow.addView(tvWpm);
        statsRow.addView(spacer(dp(6)));
        statsRow.addView(tvAcc);
        statsRow.addView(spacer(dp(6)));
        statsRow.addView(tvProg);
        layoutLesson.addView(statsRow);

        ledView = new LedView(this);
        LinearLayout.LayoutParams ledLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(26));
        ledLP.setMargins(dp(8), dp(2), dp(8), dp(2));
        layoutLesson.addView(ledView, ledLP);

        keyboardView = new KeyboardView(this);
        LinearLayout.LayoutParams kvLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        kvLP.setMargins(dp(4), 0, dp(4), dp(4));
        layoutLesson.addView(keyboardView, kvLP);

        TextView btnBack = makeTv("X Menu", 12, 0xFF666666, Typeface.NORMAL);
        btnBack.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnBack.setBackgroundColor(0xFF1A1A1A);
        btnBack.setOnClickListener(v -> showMenu());
        btnBack.setFocusable(false);
        btnBack.setFocusableInTouchMode(false);
        FrameLayout.LayoutParams backLP = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        backLP.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        backLP.setMargins(0, dp(6), dp(6), 0);

        FrameLayout lessonFrame = new FrameLayout(this);
        lessonFrame.addView(layoutLesson, matchParent());
        lessonFrame.addView(btnBack, backLP);
        lessonFrame.setVisibility(View.GONE);
        root.addView(lessonFrame, matchParent());
        layoutLesson.setTag(lessonFrame);

        // Results screen
        layoutResults = new LinearLayout(this);
        layoutResults.setOrientation(LinearLayout.VERTICAL);
        layoutResults.setGravity(android.view.Gravity.CENTER);
        layoutResults.setBackgroundColor(C_BG);
        layoutResults.setPadding(dp(48), dp(32), dp(48), dp(32));
        layoutResults.setVisibility(View.GONE);

        tvResultsText = makeTv("", 16, 0xFFCCCCCC, Typeface.NORMAL);
        tvResultsText.setBackgroundColor(C_PANEL);
        tvResultsText.setPadding(dp(24), dp(20), dp(24), dp(20));
        layoutResults.addView(tvResultsText);

        TextView btnNext = makeTv("  Next Lesson ->  ", 15, 0xFFFFFFFF, Typeface.BOLD);
        btnNext.setBackgroundColor(0xFF1A6B2E);
        btnNext.setPadding(dp(20), dp(12), dp(20), dp(12));
        LinearLayout.LayoutParams bnLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bnLP.setMargins(0, dp(16), 0, dp(8));
        bnLP.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        btnNext.setLayoutParams(bnLP);
        btnNext.setOnClickListener(v -> {
            currentLessonIndex = Math.min(currentLessonIndex + 1, LESSONS.length - 1);
            startLesson(currentLessonIndex);
        });
        layoutResults.addView(btnNext);

        TextView btnRetry = makeTv("  Retry  ", 14, C_ACCENT, Typeface.NORMAL);
        btnRetry.setBackgroundColor(C_PANEL);
        btnRetry.setPadding(dp(20), dp(10), dp(20), dp(10));
        LinearLayout.LayoutParams brLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brLP.setMargins(0, 0, 0, dp(8));
        brLP.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        btnRetry.setLayoutParams(brLP);
        btnRetry.setOnClickListener(v -> startLesson(currentLessonIndex));
        layoutResults.addView(btnRetry);

        TextView btnMenuR = makeTv("  <- Menu  ", 14, 0xFF888888, Typeface.NORMAL);
        btnMenuR.setBackgroundColor(C_PANEL);
        btnMenuR.setPadding(dp(20), dp(10), dp(20), dp(10));
        LinearLayout.LayoutParams bmLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bmLP.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        btnMenuR.setLayoutParams(bmLP);
        btnMenuR.setOnClickListener(v -> showMenu());
        layoutResults.addView(btnMenuR);

        root.addView(layoutResults, matchParent());
        setContentView(root);
    }

    private boolean isNumpadLesson() {
        return currentLessonIndex >= NUMPAD_LESSON_START &&
               currentLessonIndex <= NUMPAD_LESSON_END;
    }

    private boolean isKeycodeLesson() {
        return LESSON_TYPES[currentLessonIndex] == TYPE_KEYCODE;
    }

    private void startLesson(int idx) {
        if (!externalKeyboardConnected) { showScreen(Screen.NO_KEYBOARD); return; }
        currentLessonIndex = idx;
        cursorPos = 0; errorCount = 0; totalTyped = 0;
        lessonRunning = false; startTimeMs = 0;
        keyboardView.setShowNumpad(isNumpadLesson());
        renderLessonText();
        renderTypingBox();
        int total = isKeycodeLesson()
                ? KEYCODE_LESSONS[idx].length
                : LESSONS[idx].length();
        tvWpm.setText("0 WPM"); tvAcc.setText("100%");
        tvProg.setText("0 / " + total);
        showScreen(Screen.LESSON);
        stopTimer();
    }

    private void renderLessonText() {
        if (isKeycodeLesson()) {
            renderKeycodeText();
        } else {
            renderCharText();
        }
    }

    private void renderCharText() {
        String lesson = LESSONS[currentLessonIndex];
        SpannableStringBuilder sb = new SpannableStringBuilder(lesson);
        for (int i = 0; i < lesson.length(); i++) {
            int color = i < cursorPos ? C_TYPED :
                        i == cursorPos ? 0xFFFFFFFF : C_UNTYPED;
            sb.setSpan(new ForegroundColorSpan(color), i, i+1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (i == cursorPos) {
                sb.setSpan(new BackgroundColorSpan(0xFF1A3A5C), i, i+1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tvLessonText.setText(sb);
    }

    private void renderKeycodeText() {
        String[] labels = KEYCODE_LABELS[currentLessonIndex];
        if (labels == null) return;
        StringBuilder full = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) full.append(" ");
            full.append("[").append(labels[i]).append("]");
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(full.toString());
        // Build per-token coloring
        int pos = 0;
        for (int i = 0; i < labels.length; i++) {
            int len = labels[i].length() + 2; // +2 for [ ]
            int color = i < cursorPos ? C_TYPED :
                        i == cursorPos ? 0xFFFFFFFF : C_UNTYPED;
            sb.setSpan(new ForegroundColorSpan(color), pos, pos+len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (i == cursorPos) {
                sb.setSpan(new BackgroundColorSpan(0xFF1A3A5C), pos, pos+len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            pos += len + 1; // +1 for space
        }
        tvLessonText.setText(sb);
    }

    private void renderTypingBox() {
        if (isKeycodeLesson()) {
            renderKeycodeTypingBox();
        } else {
            renderCharTypingBox();
        }
    }

    private void renderCharTypingBox() {
        String lesson = LESSONS[currentLessonIndex];
        if (cursorPos == 0) {
            SpannableStringBuilder sb = new SpannableStringBuilder(" ");
            sb.setSpan(new BackgroundColorSpan(C_CURSOR), 0, 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvTypingBox.setText(sb);
            return;
        }
        String typed = lesson.substring(0, cursorPos);
        SpannableStringBuilder sb = new SpannableStringBuilder(typed + " ");
        sb.setSpan(new ForegroundColorSpan(C_TYPED), 0, cursorPos,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new BackgroundColorSpan(C_CURSOR), cursorPos, cursorPos+1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTypingBox.setText(sb);
    }

    private void renderKeycodeTypingBox() {
        String[] labels = KEYCODE_LABELS[currentLessonIndex];
        if (labels == null || cursorPos == 0) {
            SpannableStringBuilder sb = new SpannableStringBuilder(" ");
            sb.setSpan(new BackgroundColorSpan(C_CURSOR), 0, 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvTypingBox.setText(sb);
            return;
        }
        StringBuilder typed = new StringBuilder();
        for (int i = 0; i < cursorPos; i++) {
            if (i > 0) typed.append(" ");
            typed.append("[").append(labels[i]).append("]");
        }
        typed.append(" ");
        SpannableStringBuilder sb = new SpannableStringBuilder(typed.toString());
        sb.setSpan(new ForegroundColorSpan(C_TYPED), 0, typed.length()-1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new BackgroundColorSpan(C_CURSOR), typed.length()-1, typed.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTypingBox.setText(sb);
    }

    // Lock keys that should NOT trigger wrong sound
    private boolean isLockKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_CAPS_LOCK ||
               keyCode == KeyEvent.KEYCODE_NUM_LOCK  ||
               keyCode == KeyEvent.KEYCODE_SCROLL_LOCK;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!externalKeyboardConnected) return super.onKeyDown(keyCode, event);

        // Lock key handling — no wrong sound
        if (keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            capsLockOn = !capsLockOn;
            ledView.update(capsLockOn, numLockOn, scrollLockOn);
            if (currentScreen == Screen.LESSON && keyboardView != null)
                keyboardView.flashKey(keyCode, false);
            playSound(sndCaps);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_NUM_LOCK) {
            numLockOn = !numLockOn;
            ledView.update(capsLockOn, numLockOn, scrollLockOn);
            if (currentScreen == Screen.LESSON && keyboardView != null)
                keyboardView.flashKey(keyCode, false);
            playSound(sndNum);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_SCROLL_LOCK) {
            scrollLockOn = !scrollLockOn;
            ledView.update(capsLockOn, numLockOn, scrollLockOn);
            if (currentScreen == Screen.LESSON && keyboardView != null)
                keyboardView.flashKey(keyCode, false);
            playSound(sndScroll);
            return true;
        }

        // Menu navigation
        if (keyCode == KeyEvent.KEYCODE_ESCAPE ||
           (keyCode == KeyEvent.KEYCODE_F5 && !isKeycodeLesson())) {
            if (currentScreen == Screen.LESSON || currentScreen == Screen.RESULTS) showMenu();
            return true;
        }
        if (currentScreen == Screen.MENU) {
            return super.onKeyDown(keyCode, event);
        }
        if (currentScreen != Screen.LESSON) return super.onKeyDown(keyCode, event);

        // Flash key on keyboard view
        if (keyboardView != null) keyboardView.flashKey(keyCode, false);

        // Handle keycode lessons
        if (isKeycodeLesson()) {
            handleKeycodeLesson(keyCode);
            return true;
        }

        // Handle char lessons
        handleCharLesson(keyCode, event);
        return true;
    }

    private void handleKeycodeLesson(int keyCode) {
        int[] seq = KEYCODE_LESSONS[currentLessonIndex];
        if (seq == null || cursorPos >= seq.length) return;

        if (!lessonRunning) {
            lessonRunning = true;
            startTimeMs = System.currentTimeMillis();
            startTimer();
        }

        totalTyped++;
        boolean correct = (keyCode == seq[cursorPos]);

        if (keyboardView != null) keyboardView.flashKey(keyCode, !correct);

        if (correct) {
            playSound(sndNormal);
            cursorPos++;
            updateStats(seq.length);
            renderLessonText();
            renderTypingBox();
            if (cursorPos >= seq.length) lessonComplete();
        } else {
            if (!isLockKey(keyCode)) playSound(sndWrong);
            errorCount++;
            updateStats(seq.length);
            flashTypingBoxError();
        }
    }

    private void handleCharLesson(int keyCode, KeyEvent event) {
        String lesson = LESSONS[currentLessonIndex];
        if (cursorPos >= lesson.length()) return;

        char expected = lesson.charAt(cursorPos);
        char typed = getChar(event);

        if (typed == 0) {
            // Non-printable key pressed during char lesson — wrong
            if (!isLockKey(keyCode)) {
                totalTyped++;
                errorCount++;
                playSound(sndWrong);
                updateStats(lesson.length());
                flashTypingBoxError();
                if (keyboardView != null) keyboardView.flashKey(keyCode, true);
            }
            return;
        }

        if (!lessonRunning) {
            lessonRunning = true;
            startTimeMs = System.currentTimeMillis();
            startTimer();
        }

        totalTyped++;
        boolean correct = (typed == expected);
        if (keyboardView != null) keyboardView.flashKey(keyCode, !correct);

        if (correct) {
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                playSound(sndSpace);
            } else {
                playSound(sndNormal);
            }
            cursorPos++;
            updateStats(lesson.length());
            renderLessonText();
            renderTypingBox();
            if (cursorPos >= lesson.length()) lessonComplete();
        } else {
            playSound(sndWrong);
            errorCount++;
            updateStats(lesson.length());
            flashTypingBoxError();
        }
    }

    private char getChar(KeyEvent e) {
        int u = e.getUnicodeChar(e.getMetaState());
        if (u != 0) return (char) u;
        if (e.getKeyCode() == KeyEvent.KEYCODE_SPACE) return ' ';
        return 0;
    }

    private void flashTypingBoxError() {
        tvTypingBox.setBackgroundColor(0xFF3D1010);
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> tvTypingBox.setBackgroundColor(0xFF0D0D0D), 120);
    }

    private void updateStats(int total) {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        double mins = elapsed / 60000.0;
        int wpm = (mins > 0.01) ? (int)((cursorPos / 5.0) / mins) : 0;
        int acc = (totalTyped > 0) ? (int)(((totalTyped - errorCount) * 100.0) / totalTyped) : 100;
        tvWpm.setText(wpm + " WPM");
        tvAcc.setText(acc + "%");
        tvProg.setText(cursorPos + " / " + total);
        tvAcc.setTextColor(acc >= 90 ? C_KEY_CORRECT : acc >= 70 ? 0xFFD29922 : C_KEY_ERROR);
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override public void run() {
                if (lessonRunning) {
                    int total = isKeycodeLesson()
                            ? KEYCODE_LESSONS[currentLessonIndex].length
                            : LESSONS[currentLessonIndex].length();
                    updateStats(total);
                    timerHandler.postDelayed(this, 500);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 500);
    }

    private void stopTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private void lessonComplete() {
        stopTimer(); lessonRunning = false;
        int total = isKeycodeLesson()
                ? KEYCODE_LESSONS[currentLessonIndex].length
                : LESSONS[currentLessonIndex].length();
        long elapsed = System.currentTimeMillis() - startTimeMs;
        double mins = elapsed / 60000.0;
        int wpm = (mins > 0) ? (int)((total / 5.0) / mins) : 0;
        int acc = (totalTyped > 0) ? (int)(((totalTyped-errorCount)*100.0)/totalTyped) : 100;
        int secs = (int)(elapsed / 1000);
        String grade = wpm >= 60 && acc >= 95 ? "Excellent!" :
                       wpm >= 40 && acc >= 90 ? "Good" :
                       wpm >= 20 && acc >= 80 ? "Keep Practising" : "Try Again";
        tvResultsText.setText(
            "Lesson Complete!\n\n" +
            "  Lesson   : " + LESSON_TITLES[currentLessonIndex] + "\n\n" +
            "  WPM      : " + wpm + "\n" +
            "  Accuracy : " + acc + "%\n" +
            "  Time     : " + secs + "s\n" +
            "  Errors   : " + errorCount + "\n\n" +
            "  Result   : " + grade
        );
        showScreen(Screen.RESULTS);
    }

    private void showMenu() { stopTimer(); lessonRunning = false; showScreen(Screen.MENU); }

    private void showScreen(Screen s) {
        currentScreen = s;
        layoutNoKeyboard.setVisibility(s == Screen.NO_KEYBOARD ? View.VISIBLE : View.GONE);
        layoutMenu.setVisibility(s == Screen.MENU ? View.VISIBLE : View.GONE);
        layoutLesson.setVisibility(s == Screen.LESSON ? View.VISIBLE : View.GONE);
        View lessonFrame = (View) layoutLesson.getTag();
        if (lessonFrame != null) lessonFrame.setVisibility(s == Screen.LESSON ? View.VISIBLE : View.GONE);
        layoutResults.setVisibility(s == Screen.RESULTS ? View.VISIBLE : View.GONE);
    }

    private void checkKeyboard() {
        externalKeyboardConnected = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || d.isVirtual()) continue;
            if ((d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0 &&
                d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                externalKeyboardConnected = true;
                break;
            }
        }
        if (externalKeyboardConnected) {
            if (currentScreen == Screen.NO_KEYBOARD) showScreen(Screen.MENU);
        } else {
            showScreen(Screen.NO_KEYBOARD);
        }
    }

    @Override public void onInputDeviceAdded(int id) {
        checkKeyboard();
        if (externalKeyboardConnected)
            Toast.makeText(this, "Keyboard connected!", Toast.LENGTH_SHORT).show();
    }
    @Override public void onInputDeviceRemoved(int id) {
        checkKeyboard();
        if (!externalKeyboardConnected) {
            stopTimer();
            Toast.makeText(this, "Keyboard disconnected.", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onInputDeviceChanged(int id) { checkKeyboard(); }

    @Override protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (soundPool != null) soundPool.release();
        if (inputManager != null) inputManager.unregisterInputDeviceListener(this);
    }

    private TextView makeTv(String text, float sp, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(color); tv.setTextSize(sp);
        tv.setTypeface(Typeface.MONOSPACE, style);
        return tv;
    }

    private TextView makeStatChip(String text, int color) {
        TextView tv = makeTv(text, 13, color, Typeface.BOLD);
        tv.setPadding(dp(10), dp(4), dp(10), dp(4));
        tv.setBackgroundColor(C_PANEL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(lp);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    private View spacer(int w) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(w, 1));
        return v;
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // LED VIEW
    class LedView extends View {
        boolean caps, num, scroll;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        LedView(Context ctx) {
            super(ctx);
            setBackgroundColor(C_PANEL);
        }

        void update(boolean c, boolean n, boolean s) {
            caps = c; num = n; scroll = s;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int h = getHeight(); int w = getWidth();
            int dotR = h / 4;
            int y = h / 2;
            int startX = w / 2 - dp(90);
            drawLed(canvas, startX,           y, dotR, caps,   "Caps Lock",   C_LED_CAPS);
            drawLed(canvas, startX + dp(90),  y, dotR, num,    "Num Lock",    C_LED_NUM);
            drawLed(canvas, startX + dp(180), y, dotR, scroll, "Scroll Lock", C_LED_SCROLL);
        }

        private void drawLed(Canvas c, int x, int y, int r, boolean on, String label, int onColor) {
            if (on) {
                p.setColor(onColor & 0x55FFFFFF); p.setStyle(Paint.Style.FILL);
                c.drawCircle(x, y, r * 2.2f, p);
            }
            p.setColor(on ? onColor : C_LED_OFF); p.setStyle(Paint.Style.FILL);
            c.drawCircle(x, y, r, p);
            p.setColor(on ? 0xFFCCCCCC : 0xFF555555);
            p.setTextSize(dp(10)); p.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, x, y + r + dp(10), p);
        }
    }

    // KEYBOARD VIEW
    class KeyboardView extends View {

        private boolean showNumpad = false;

        void setShowNumpad(boolean show) {
            showNumpad = show;
            laid = false;
            invalidate();
        }

        private class Key {
            int code; float widthU; String label, sub;
            float x, y, w, h;
            boolean isNumpad;
            Key(int code, float w, String label, String sub, boolean isNumpad) {
                this.code=code; this.widthU=w; this.label=label;
                this.sub=sub; this.isNumpad=isNumpad;
            }
        }

        private final Key[][] ROWS = buildRows();

        private Key[][] buildRows() {
            return new Key[][] {
                // Row 0: Function row
                {
                    k(KeyEvent.KEYCODE_ESCAPE,     1f,  "Esc",   "", false),
                    gap(0.5f),
                    k(KeyEvent.KEYCODE_F1,         1f,  "F1",    "", false),
                    k(KeyEvent.KEYCODE_F2,         1f,  "F2",    "", false),
                    k(KeyEvent.KEYCODE_F3,         1f,  "F3",    "", false),
                    k(KeyEvent.KEYCODE_F4,         1f,  "F4",    "", false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_F5,         1f,  "F5",    "", false),
                    k(KeyEvent.KEYCODE_F6,         1f,  "F6",    "", false),
                    k(KeyEvent.KEYCODE_F7,         1f,  "F7",    "", false),
                    k(KeyEvent.KEYCODE_F8,         1f,  "F8",    "", false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_F9,         1f,  "F9",    "", false),
                    k(KeyEvent.KEYCODE_F10,        1f,  "F10",   "", false),
                    k(KeyEvent.KEYCODE_F11,        1f,  "F11",   "", false),
                    k(KeyEvent.KEYCODE_F12,        1f,  "F12",   "", false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_SYSRQ,      1f,  "PrtSc", "", false),
                    k(KeyEvent.KEYCODE_SCROLL_LOCK,1f,  "Scrl",  "", false),
                    k(KeyEvent.KEYCODE_BREAK,      1f,  "Pause", "", false),
                },
                // Row 1: Number row
                {
                    k(KeyEvent.KEYCODE_GRAVE,      1f,  "`",     "~",  false),
                    k(KeyEvent.KEYCODE_1,          1f,  "1",     "!",  false),
                    k(KeyEvent.KEYCODE_2,          1f,  "2",     "@",  false),
                    k(KeyEvent.KEYCODE_3,          1f,  "3",     "#",  false),
                    k(KeyEvent.KEYCODE_4,          1f,  "4",     "$",  false),
                    k(KeyEvent.KEYCODE_5,          1f,  "5",     "%",  false),
                    k(KeyEvent.KEYCODE_6,          1f,  "6",     "^",  false),
                    k(KeyEvent.KEYCODE_7,          1f,  "7",     "&",  false),
                    k(KeyEvent.KEYCODE_8,          1f,  "8",     "*",  false),
                    k(KeyEvent.KEYCODE_9,          1f,  "9",     "(",  false),
                    k(KeyEvent.KEYCODE_0,          1f,  "0",     ")",  false),
                    k(KeyEvent.KEYCODE_MINUS,      1f,  "-",     "_",  false),
                    k(KeyEvent.KEYCODE_EQUALS,     1f,  "=",     "+",  false),
                    k(KeyEvent.KEYCODE_DEL,        2f,  "Back",  "",   false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_INSERT,     1f,  "Ins",   "",   false),
                    k(KeyEvent.KEYCODE_HOME,       1f,  "Home",  "",   false),
                    k(KeyEvent.KEYCODE_PAGE_UP,    1f,  "PgUp",  "",   false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUM_LOCK,      1f, "Num",  "",  true),
                    k(KeyEvent.KEYCODE_NUMPAD_DIVIDE, 1f, "/",   "",   true),
                    k(KeyEvent.KEYCODE_NUMPAD_MULTIPLY,1f,"*",   "",   true),
                    k(KeyEvent.KEYCODE_NUMPAD_SUBTRACT,1f,"-",  "",    true),
                },
                // Row 2: QWERTY
                {
                    k(KeyEvent.KEYCODE_TAB,        1.5f,"Tab",   "",   false),
                    k(KeyEvent.KEYCODE_Q,          1f,  "Q",     "",   false),
                    k(KeyEvent.KEYCODE_W,          1f,  "W",     "",   false),
                    k(KeyEvent.KEYCODE_E,          1f,  "E",     "",   false),
                    k(KeyEvent.KEYCODE_R,          1f,  "R",     "",   false),
                    k(KeyEvent.KEYCODE_T,          1f,  "T",     "",   false),
                    k(KeyEvent.KEYCODE_Y,          1f,  "Y",     "",   false),
                    k(KeyEvent.KEYCODE_U,          1f,  "U",     "",   false),
                    k(KeyEvent.KEYCODE_I,          1f,  "I",     "",   false),
                    k(KeyEvent.KEYCODE_O,          1f,  "O",     "",   false),
                    k(KeyEvent.KEYCODE_P,          1f,  "P",     "",   false),
                    k(KeyEvent.KEYCODE_LEFT_BRACKET, 1f,"[",     "{",  false),
                    k(KeyEvent.KEYCODE_RIGHT_BRACKET,1f,"]",     "}",  false),
                    k(KeyEvent.KEYCODE_BACKSLASH,  1.5f,"\\",    "|",  false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_FORWARD_DEL,1f,  "Del",   "",   false),
                    k(KeyEvent.KEYCODE_MOVE_END,   1f,  "End",   "",   false),
                    k(KeyEvent.KEYCODE_PAGE_DOWN,  1f,  "PgDn",  "",   false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_7,   1f,  "7",     "Hm", true),
                    k(KeyEvent.KEYCODE_NUMPAD_8,   1f,  "8",     "Up", true),
                    k(KeyEvent.KEYCODE_NUMPAD_9,   1f,  "9",     "Pu", true),
                    k(KeyEvent.KEYCODE_NUMPAD_ADD, 1f,  "+",     "",   true),
                },
                // Row 3: ASDF
                {
                    k(KeyEvent.KEYCODE_CAPS_LOCK,  1.75f,"Caps", "",   false),
                    k(KeyEvent.KEYCODE_A,          1f,  "A",     "",   false),
                    k(KeyEvent.KEYCODE_S,          1f,  "S",     "",   false),
                    k(KeyEvent.KEYCODE_D,          1f,  "D",     "",   false),
                    k(KeyEvent.KEYCODE_F,          1f,  "F",     "",   false),
                    k(KeyEvent.KEYCODE_G,          1f,  "G",     "",   false),
                    k(KeyEvent.KEYCODE_H,          1f,  "H",     "",   false),
                    k(KeyEvent.KEYCODE_J,          1f,  "J",     "",   false),
                    k(KeyEvent.KEYCODE_K,          1f,  "K",     "",   false),
                    k(KeyEvent.KEYCODE_L,          1f,  "L",     "",   false),
                    k(KeyEvent.KEYCODE_SEMICOLON,  1f,  ";",     ":",  false),
                    k(KeyEvent.KEYCODE_APOSTROPHE, 1f,  "'",     "\"", false),
                    k(KeyEvent.KEYCODE_ENTER,      2.25f,"Enter","",   false),
                    gap(0.25f),
                    gap(1f), gap(1f), gap(1f),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_4,   1f,  "4",     "Lt", true),
                    k(KeyEvent.KEYCODE_NUMPAD_5,   1f,  "5",     "",   true),
                    k(KeyEvent.KEYCODE_NUMPAD_6,   1f,  "6",     "Rt", true),
                },
                // Row 4: ZXCV
                {
                    k(KeyEvent.KEYCODE_SHIFT_LEFT, 2.25f,"Shift","",   false),
                    k(KeyEvent.KEYCODE_Z,          1f,  "Z",     "",   false),
                    k(KeyEvent.KEYCODE_X,          1f,  "X",     "",   false),
                    k(KeyEvent.KEYCODE_C,          1f,  "C",     "",   false),
                    k(KeyEvent.KEYCODE_V,          1f,  "V",     "",   false),
                    k(KeyEvent.KEYCODE_B,          1f,  "B",     "",   false),
                    k(KeyEvent.KEYCODE_N,          1f,  "N",     "",   false),
                    k(KeyEvent.KEYCODE_M,          1f,  "M",     "",   false),
                    k(KeyEvent.KEYCODE_COMMA,      1f,  ",",     "<",  false),
                    k(KeyEvent.KEYCODE_PERIOD,     1f,  ".",     ">",  false),
                    k(KeyEvent.KEYCODE_SLASH,      1f,  "/",     "?",  false),
                    k(KeyEvent.KEYCODE_SHIFT_RIGHT,2.75f,"Shift","",   false),
                    gap(0.25f),
                    gap(1f),
                    k(KeyEvent.KEYCODE_DPAD_UP,    1f,  "Up",    "",   false),
                    gap(1f),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_1,   1f,  "1",     "En", true),
                    k(KeyEvent.KEYCODE_NUMPAD_2,   1f,  "2",     "Dn", true),
                    k(KeyEvent.KEYCODE_NUMPAD_3,   1f,  "3",     "Pd", true),
                    k(KeyEvent.KEYCODE_NUMPAD_ENTER,1f, "Ent",   "",   true),
                },
                // Row 5: Bottom row
                {
                    k(KeyEvent.KEYCODE_CTRL_LEFT,  1.25f,"Ctrl", "",   false),
                    k(KeyEvent.KEYCODE_META_LEFT,  1.25f,"Win",  "",   false),
                    k(KeyEvent.KEYCODE_ALT_LEFT,   1.25f,"Alt",  "",   false),
                    k(KeyEvent.KEYCODE_SPACE,      6.25f,"",     "",   false),
                    k(KeyEvent.KEYCODE_ALT_RIGHT,  1.25f,"AltGr","",   false),
                    k(KeyEvent.KEYCODE_META_RIGHT, 1.25f,"Win",  "",   false),
                    k(KeyEvent.KEYCODE_MENU,       1.25f,"Menu", "",   false),
                    k(KeyEvent.KEYCODE_CTRL_RIGHT, 1.25f,"Ctrl", "",   false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_DPAD_LEFT,  1f,  "Lt",    "",   false),
                    k(KeyEvent.KEYCODE_DPAD_DOWN,  1f,  "Dn",    "",   false),
                    k(KeyEvent.KEYCODE_DPAD_RIGHT, 1f,  "Rt",    "",   false),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_0,   2f,  "0",     "Ins",true),
                    k(KeyEvent.KEYCODE_NUMPAD_DOT, 1f,  ".",     "Del",true),
                },
            };
        }

        private Key k(int code, float w, String label, String sub, boolean isNumpad) {
            return new Key(code, w, label, sub, isNumpad);
        }
        private Key gap(float w) { return new Key(-1, w, "", "", false); }

        private final Map<Integer, Long> pressedAt  = new HashMap<>();
        private final Map<Integer, Boolean> pressedErr = new HashMap<>();
        private static final long FLASH_MS = 180;
        private boolean laid = false;
        private float keyH, keyUnit, padX, padY;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        KeyboardView(Context ctx) {
            super(ctx);
            setBackgroundColor(0xFF080808);
        }

        void flashKey(int keyCode, boolean error) {
            pressedAt.put(keyCode, System.currentTimeMillis());
            pressedErr.put(keyCode, error);
            invalidate();
            postDelayed(this::invalidate, FLASH_MS + 20);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!laid || getWidth() == 0) computeLayout();
            long now = System.currentTimeMillis();
            for (Key[] row : ROWS) {
                for (Key key : row) {
                    if (key.code < 0) continue;
                    if (key.isNumpad && !showNumpad) continue;
                    drawKey(canvas, key, now);
                }
            }
        }

        private void computeLayout() {
            if (getWidth() == 0 || getHeight() == 0) return;
            int W = getWidth(); int H = getHeight();
            float totalU = showNumpad ? 18.75f : 14.75f;
            padX = dp(2); padY = dp(2);
            float availW = W - padX * 2;
            float availH = H - padY * 2;
            keyUnit = availW / totalU;
            keyH = Math.min(availH / (ROWS.length + 0.5f), keyUnit * 1.0f);
            float rowSpacing = keyH + dp(2);
            float y = padY;
            for (int r = 0; r < ROWS.length; r++) {
                float x = padX;
                for (Key key : ROWS[r]) {
                    float kw = key.widthU * keyUnit - dp(2);
                    key.x = x + dp(1); key.y = y + dp(1);
                    key.w = kw; key.h = keyH - dp(2);
                    if (!key.isNumpad || showNumpad) {
                        x += key.widthU * keyUnit;
                    }
                }
                y += rowSpacing;
                if (r == 0) y += dp(3);
            }
            laid = true;
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            laid = false;
            computeLayout();
        }

        private void drawKey(Canvas canvas, Key key, long now) {
            long pressTime = pressedAt.getOrDefault(key.code, 0L);
            boolean active = (now - pressTime) < FLASH_MS;
            RectF rect = new RectF(key.x, key.y, key.x + key.w, key.y + key.h);
            float r = dp(2);
            if (active) {
                boolean err = Boolean.TRUE.equals(pressedErr.get(key.code));
                paint.setColor(err ? 0x44EF4444 : 0x4422C55E);
                paint.setStyle(Paint.Style.FILL);
                RectF glow = new RectF(rect.left-dp(2), rect.top-dp(2),
                        rect.right+dp(2), rect.bottom+dp(2));
                canvas.drawRoundRect(glow, r+dp(2), r+dp(2), paint);
                paint.setColor(err ? 0xFF5A1010 : 0xFF0D3D1A);
            } else {
                paint.setColor(C_KEY_BODY);
            }
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, r, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(active ? (Boolean.TRUE.equals(pressedErr.get(key.code))
                    ? C_KEY_ERROR : C_KEY_CORRECT) : C_KEY_BORDER);
            canvas.drawRoundRect(rect, r, r, paint);
            if (key.label.isEmpty()) return;
            paint.setStyle(Paint.Style.FILL);
            float labelSize = Math.min(key.h * 0.38f, key.w * 0.45f);
            labelSize = Math.max(labelSize, dp(5));
            paint.setTextSize(labelSize);
            paint.setColor(active ? 0xFFFFFFFF : C_KEY_LABEL);
            paint.setTypeface(Typeface.MONOSPACE);
            float cx = key.x + key.w / 2;
            float cy = key.y + key.h / 2 + labelSize * 0.35f;
            if (!key.sub.isEmpty() && key.w <= keyUnit * 1.1f) {
                paint.setTextSize(labelSize * 0.75f);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(key.label, key.x + dp(2), key.y + key.h - dp(2), paint);
                paint.setTextSize(labelSize * 0.60f);
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setColor(active ? 0xFFCCFFCC : 0xFF909090);
                canvas.drawText(key.sub, key.x + key.w - dp(2),
                        key.y + labelSize * 0.65f + dp(1), paint);
            } else {
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(key.label, cx, cy, paint);
            }
        }
    }
}
