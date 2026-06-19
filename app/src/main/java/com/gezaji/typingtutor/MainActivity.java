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
    static final int C_ACCENT     = 0xFF58A6FF;

    static final String[] LESSON_TITLES = {
        "Home Row - Left Hand",   "Home Row - Right Hand",  "Full Home Row",
        "Top Row - Left Hand",    "Top Row - Right Hand",   "Full Top Row",
        "Bottom Row - Left Hand", "Bottom Row - Right Hand","Full Bottom Row",
        "Numbers 1-5",            "Numbers 6-0",            "All Numbers",
        "Capital Letters",        "Common Punctuation",     "Short Words",
        "Medium Words",           "Long Words",             "Short Sentences",
        "Paragraphs",             "Speed Challenge",
        "Number Row - Left",      "Number Row - Right",     "Full Number Row",
    };

    static final String[] LESSONS = {
        "asdf asdf asdf fdsa fdsa fdsa afsd dafs sfda fads asdf fdsa",
        "jkl; jkl; ;lkj ;lkj jkl; klj; ljk; ;lkj jkl; ;lkj",
        "asdfjkl; asdf jkl; fdsa ;lkj ask lad fall glad flask slab",
        "qwert qwert trewq trewq qwer wert erqw rqew qwerty",
        "yuiop yuiop poiuy poiuy yui iop opu pou yuiop poiuy",
        "qwerty uiop type rope quit your pour riot true pout",
        "zxcvb zxcvb bvcxz bvcxz zxcv xcvb cvbz vbzx zxcvb",
        "nm,./ nm,./ /.,mn /.,mn nm., .,mn nm,./ /.,mn",
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
    };

    private enum Screen { NO_KEYBOARD, MENU, LESSON, RESULTS }
    private Screen currentScreen = Screen.NO_KEYBOARD;
    private int currentLessonIndex = 0;
    private int cursorPos = 0;
    private int errorCount = 0;
    private int totalTyped = 0;
    private boolean lessonRunning = false;
    private long startTimeMs = 0;

    private TextView tvLessonText;
    private TextView tvTypingBox;
    private TextView tvWpm, tvAcc, tvProg;
    private KeyboardView keyboardView;
    private LinearLayout layoutNoKeyboard, layoutMenu, layoutLesson, layoutResults;
    private TextView tvResultsText;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private InputManager inputManager;
    private boolean externalKeyboardConnected = false;

    private SoundPool soundPool;
    private int sndNormal, sndSpace, sndWrong;
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
    }

    private void playSound(int soundId) {
        if (soundsLoaded) soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
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

        int[] sectionStarts = { 0, 20 };
        String[] sectionHeaders = {
            "-- CLASSIC LESSONS --",
            "-- NUMBER ROW --",
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
             "23 progressive lessons covering the core typing keys.\n\n" +
             "WPM & ACCURACY\n" +
             "- WPM: characters typed divided by 5, per minute\n" +
             "- Accuracy: correct keystrokes as a percentage of total\n" +
             "- Progress: how far along the current lesson you are\n\n" +
             "NAVIGATION\n" +
             "- Use Arrow Keys to move and Enter to select a lesson.\n" +
             "- Press F5 or the X Menu button to return to menu at any time."},
            {"[ VERSION INFO ]",
             "GEZAJI Typing Tutor v2.0\n\n" +
             "23 lessons - Compact 60% visual keyboard - OTG support\n" +
             "WPM & accuracy tracking - Sound feedback - Fully offline\n\n" +
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

        tvLessonText = makeTv("", 17, 0xFFFFFFFF, Typeface.NORMAL);
        tvLessonText.setPadding(dp(16), dp(10), dp(16), dp(8));
        tvLessonText.setBackgroundColor(C_PANEL);
        tvLessonText.setSingleLine(false);
        tvLessonText.setMaxLines(3);
        LinearLayout.LayoutParams ltLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ltLP.setMargins(dp(8), dp(10), dp(8), 0);
        layoutLesson.addView(tvLessonText, ltLP);

        tvTypingBox = makeTv("", 20, 0xFFFFFFFF, Typeface.NORMAL);
        tvTypingBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        tvTypingBox.setBackgroundColor(0xFF0D0D0D);
        tvTypingBox.setSingleLine(false);
        tvTypingBox.setMaxLines(2);
        tvTypingBox.setMinHeight(dp(56));
        LinearLayout.LayoutParams tbLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tbLP.setMargins(dp(8), dp(8), dp(8), dp(8));
        layoutLesson.addView(tvTypingBox, tbLP);

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(dp(8), dp(2), dp(8), dp(6));
        tvWpm  = makeStatChip("0 WPM",  C_ACCENT);
        tvAcc  = makeStatChip("100%",   C_KEY_CORRECT);
        tvProg = makeStatChip("0 / 0",  0xFFD29922);
        statsRow.addView(tvWpm);
        statsRow.addView(spacer(dp(6)));
        statsRow.addView(tvAcc);
        statsRow.addView(spacer(dp(6)));
        statsRow.addView(tvProg);
        layoutLesson.addView(statsRow);

        keyboardView = new KeyboardView(this);
        LinearLayout.LayoutParams kvLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        kvLP.setMargins(dp(6), 0, dp(6), dp(8));
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

    private void startLesson(int idx) {
        if (!externalKeyboardConnected) { showScreen(Screen.NO_KEYBOARD); return; }
        currentLessonIndex = idx;
        cursorPos = 0; errorCount = 0; totalTyped = 0;
        lessonRunning = false; startTimeMs = 0;
        renderLessonText();
        renderTypingBox();
        tvWpm.setText("0 WPM"); tvAcc.setText("100%");
        tvProg.setText("0 / " + LESSONS[idx].length());
        showScreen(Screen.LESSON);
        stopTimer();
    }

    private void renderLessonText() {
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

    private void renderTypingBox() {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!externalKeyboardConnected) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_F5) {
            if (currentScreen == Screen.LESSON || currentScreen == Screen.RESULTS) showMenu();
            return true;
        }
        if (currentScreen == Screen.MENU) {
            return super.onKeyDown(keyCode, event);
        }
        if (currentScreen != Screen.LESSON) return super.onKeyDown(keyCode, event);

        String lesson = LESSONS[currentLessonIndex];
        if (cursorPos >= lesson.length()) return true;

        char expected = lesson.charAt(cursorPos);
        char typed = getChar(event);
        if (typed == 0) {
            if (keyboardView != null) keyboardView.flashKey(keyCode, false);
            return super.onKeyDown(keyCode, event);
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
            updateStats();
            renderLessonText();
            renderTypingBox();
            if (cursorPos >= lesson.length()) lessonComplete();
        } else {
            playSound(sndWrong);
            errorCount++;
            updateStats();
            flashTypingBoxError();
        }
        return true;
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

    private void updateStats() {
        String lesson = LESSONS[currentLessonIndex];
        long elapsed = System.currentTimeMillis() - startTimeMs;
        double mins = elapsed / 60000.0;
        int wpm = (mins > 0.01) ? (int)((cursorPos / 5.0) / mins) : 0;
        int acc = (totalTyped > 0) ? (int)(((totalTyped - errorCount) * 100.0) / totalTyped) : 100;
        tvWpm.setText(wpm + " WPM");
        tvAcc.setText(acc + "%");
        tvProg.setText(cursorPos + " / " + lesson.length());
        tvAcc.setTextColor(acc >= 90 ? C_KEY_CORRECT : acc >= 70 ? 0xFFD29922 : C_KEY_ERROR);
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override public void run() {
                if (lessonRunning) { updateStats(); timerHandler.postDelayed(this, 500); }
            }
        };
        timerHandler.postDelayed(timerRunnable, 500);
    }

    private void stopTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private void lessonComplete() {
        stopTimer(); lessonRunning = false;
        String lesson = LESSONS[currentLessonIndex];
        long elapsed = System.currentTimeMillis() - startTimeMs;
        double mins = elapsed / 60000.0;
        int wpm = (mins > 0) ? (int)((lesson.length() / 5.0) / mins) : 0;
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

    // KEYBOARD VIEW — 60% layout only
    class KeyboardView extends View {

        private class Key {
            int code; float widthU; String label, sub;
            float x, y, w, h;
            Key(int code, float w, String label, String sub) {
                this.code=code; this.widthU=w; this.label=label; this.sub=sub;
            }
        }

        private final Key[][] ROWS = buildRows();

        private Key[][] buildRows() {
            return new Key[][] {
                // Number row
                {
                    k(KeyEvent.KEYCODE_GRAVE,      1f,  "`",     "~"),
                    k(KeyEvent.KEYCODE_1,          1f,  "1",     "!"),
                    k(KeyEvent.KEYCODE_2,          1f,  "2",     "@"),
                    k(KeyEvent.KEYCODE_3,          1f,  "3",     "#"),
                    k(KeyEvent.KEYCODE_4,          1f,  "4",     "$"),
                    k(KeyEvent.KEYCODE_5,          1f,  "5",     "%"),
                    k(KeyEvent.KEYCODE_6,          1f,  "6",     "^"),
                    k(KeyEvent.KEYCODE_7,          1f,  "7",     "&"),
                    k(KeyEvent.KEYCODE_8,          1f,  "8",     "*"),
                    k(KeyEvent.KEYCODE_9,          1f,  "9",     "("),
                    k(KeyEvent.KEYCODE_0,          1f,  "0",     ")"),
                    k(KeyEvent.KEYCODE_MINUS,      1f,  "-",     "_"),
                    k(KeyEvent.KEYCODE_EQUALS,     1f,  "=",     "+"),
                    k(KeyEvent.KEYCODE_DEL,        2f,  "Back",  ""),
                },
                // QWERTY
                {
                    k(KeyEvent.KEYCODE_TAB,        1.5f,"Tab",   ""),
                    k(KeyEvent.KEYCODE_Q,          1f,  "Q",     ""),
                    k(KeyEvent.KEYCODE_W,          1f,  "W",     ""),
                    k(KeyEvent.KEYCODE_E,          1f,  "E",     ""),
                    k(KeyEvent.KEYCODE_R,          1f,  "R",     ""),
                    k(KeyEvent.KEYCODE_T,          1f,  "T",     ""),
                    k(KeyEvent.KEYCODE_Y,          1f,  "Y",     ""),
                    k(KeyEvent.KEYCODE_U,          1f,  "U",     ""),
                    k(KeyEvent.KEYCODE_I,          1f,  "I",     ""),
                    k(KeyEvent.KEYCODE_O,          1f,  "O",     ""),
                    k(KeyEvent.KEYCODE_P,          1f,  "P",     ""),
                    k(KeyEvent.KEYCODE_LEFT_BRACKET, 1f,"[",     "{"),
                    k(KeyEvent.KEYCODE_RIGHT_BRACKET,1f,"]",     "}"),
                    k(KeyEvent.KEYCODE_BACKSLASH,  1.5f,"\\",    "|"),
                },
                // ASDF
                {
                    k(KeyEvent.KEYCODE_SHIFT_LEFT, 1.75f,"Caps", ""),
                    k(KeyEvent.KEYCODE_A,          1f,  "A",     ""),
                    k(KeyEvent.KEYCODE_S,          1f,  "S",     ""),
                    k(KeyEvent.KEYCODE_D,          1f,  "D",     ""),
                    k(KeyEvent.KEYCODE_F,          1f,  "F",     ""),
                    k(KeyEvent.KEYCODE_G,          1f,  "G",     ""),
                    k(KeyEvent.KEYCODE_H,          1f,  "H",     ""),
                    k(KeyEvent.KEYCODE_J,          1f,  "J",     ""),
                    k(KeyEvent.KEYCODE_K,          1f,  "K",     ""),
                    k(KeyEvent.KEYCODE_L,          1f,  "L",     ""),
                    k(KeyEvent.KEYCODE_SEMICOLON,  1f,  ";",     ":"),
                    k(KeyEvent.KEYCODE_APOSTROPHE, 1f,  "'",     "\""),
                    k(KeyEvent.KEYCODE_ENTER,      2.25f,"Enter",""),
                },
                // ZXCV
                {
                    k(KeyEvent.KEYCODE_SHIFT_LEFT, 2.25f,"Shift",""),
                    k(KeyEvent.KEYCODE_Z,          1f,  "Z",     ""),
                    k(KeyEvent.KEYCODE_X,          1f,  "X",     ""),
                    k(KeyEvent.KEYCODE_C,          1f,  "C",     ""),
                    k(KeyEvent.KEYCODE_V,          1f,  "V",     ""),
                    k(KeyEvent.KEYCODE_B,          1f,  "B",     ""),
                    k(KeyEvent.KEYCODE_N,          1f,  "N",     ""),
                    k(KeyEvent.KEYCODE_M,          1f,  "M",     ""),
                    k(KeyEvent.KEYCODE_COMMA,      1f,  ",",     "<"),
                    k(KeyEvent.KEYCODE_PERIOD,     1f,  ".",     ">"),
                    k(KeyEvent.KEYCODE_SLASH,      1f,  "/",     "?"),
                    k(KeyEvent.KEYCODE_SHIFT_RIGHT,2.75f,"Shift",""),
                },
                // Bottom row
                {
                    k(KeyEvent.KEYCODE_CTRL_LEFT,  1.25f,"Ctrl", ""),
                    k(KeyEvent.KEYCODE_META_LEFT,  1.25f,"Win",  ""),
                    k(KeyEvent.KEYCODE_ALT_LEFT,   1.25f,"Alt",  ""),
                    k(KeyEvent.KEYCODE_SPACE,      6.25f,"",     ""),
                    k(KeyEvent.KEYCODE_ALT_RIGHT,  1.25f,"AltGr",""),
                    k(KeyEvent.KEYCODE_META_RIGHT, 1.25f,"Win",  ""),
                    k(KeyEvent.KEYCODE_MENU,       1.25f,"Menu", ""),
                    k(KeyEvent.KEYCODE_CTRL_RIGHT, 1.25f,"Ctrl", ""),
                },
            };
        }

        private Key k(int code, float w, String label, String sub) {
            return new Key(code, w, label, sub);
        }

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
                    drawKey(canvas, key, now);
                }
            }
        }

        private void computeLayout() {
            if (getWidth() == 0 || getHeight() == 0) return;
            int W = getWidth(); int H = getHeight();
            float totalU = 14.75f;
            padX = dp(4); padY = dp(4);
            float availW = W - padX * 2;
            float availH = H - padY * 2;
            keyUnit = availW / totalU;
            keyH = Math.min(availH / ROWS.length, keyUnit * 1.1f);
            float rowSpacing = keyH + dp(3);
            float y = padY;
            for (int r = 0; r < ROWS.length; r++) {
                float x = padX;
                for (Key key : ROWS[r]) {
                    float kw = key.widthU * keyUnit - dp(3);
                    key.x = x + dp(1); key.y = y + dp(1);
                    key.w = kw; key.h = keyH - dp(3);
                    x += key.widthU * keyUnit;
                }
                y += rowSpacing;
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
            float r = dp(4);
            if (active) {
                boolean err = Boolean.TRUE.equals(pressedErr.get(key.code));
                paint.setColor(err ? 0x44EF4444 : 0x4422C55E);
                paint.setStyle(Paint.Style.FILL);
                RectF glow = new RectF(rect.left-dp(3), rect.top-dp(3),
                        rect.right+dp(3), rect.bottom+dp(3));
                canvas.drawRoundRect(glow, r+dp(3), r+dp(3), paint);
                paint.setColor(err ? 0xFF5A1010 : 0xFF0D3D1A);
            } else {
                paint.setColor(C_KEY_BODY);
            }
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, r, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(active ? (Boolean.TRUE.equals(pressedErr.get(key.code))
                    ? C_KEY_ERROR : C_KEY_CORRECT) : C_KEY_BORDER);
            canvas.drawRoundRect(rect, r, r, paint);
            if (key.label.isEmpty()) return;
            paint.setStyle(Paint.Style.FILL);
            float labelSize = Math.min(key.h * 0.4f, key.w * 0.4f);
            labelSize = Math.max(labelSize, dp(8));
            paint.setTextSize(labelSize);
            paint.setColor(active ? 0xFFFFFFFF : C_KEY_LABEL);
            paint.setTypeface(Typeface.MONOSPACE);
            float cx = key.x + key.w / 2;
            float cy = key.y + key.h / 2 + labelSize * 0.35f;
            if (!key.sub.isEmpty() && key.w <= keyUnit * 1.1f) {
                paint.setTextSize(labelSize * 0.8f);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(key.label, key.x + dp(4), key.y + key.h - dp(4), paint);
                paint.setTextSize(labelSize * 0.65f);
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setColor(active ? 0xFFCCFFCC : 0xFF909090);
                canvas.drawText(key.sub, key.x + key.w - dp(4),
                        key.y + labelSize * 0.7f + dp(2), paint);
            } else {
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(key.label, cx, cy, paint);
            }
        }
    }
}
