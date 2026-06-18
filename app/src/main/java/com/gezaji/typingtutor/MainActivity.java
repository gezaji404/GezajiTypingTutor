package com.gezaji.typingtutor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements InputManager.InputDeviceListener {

    // ── Colors ───────────────────────────────────────────────────────────────
    static final int C_BG         = 0xFF0A0A0A;  // near-black app background
    static final int C_PANEL      = 0xFF111111;  // slightly lighter panel
    static final int C_KEY_BODY   = 0xFF1A1A1A;  // key face — dark
    static final int C_KEY_BORDER = 0xFF2E2E2E;  // key border
    static final int C_KEY_LABEL  = 0xFFE8E8E8;  // white key label
    static final int C_KEY_PRESS  = 0xFF3A3AFF;  // blue flash on press
    static final int C_KEY_CORRECT= 0xFF22C55E;  // green — correct key
    static final int C_KEY_ERROR  = 0xFFEF4444;  // red — wrong key
    static final int C_CURSOR     = 0xFF58A6FF;  // cursor highlight
    static final int C_TYPED      = 0xFF22C55E;  // correctly typed text
    static final int C_UNTYPED    = 0xFF606060;  // untyped text
    static final int C_LED_ON     = 0xFF22C55E;  // LED active
    static final int C_LED_OFF    = 0xFF252525;  // LED inactive
    static final int C_ACCENT     = 0xFF58A6FF;  // blue accent

    // ── Lessons ──────────────────────────────────────────────────────────────
    static final String[] LESSON_TITLES = {
        "Home Row — Left Hand",   "Home Row — Right Hand",  "Full Home Row",
        "Top Row — Left Hand",    "Top Row — Right Hand",   "Full Top Row",
        "Bottom Row — Left Hand", "Bottom Row — Right Hand","Full Bottom Row",
        "Numbers 1–5",            "Numbers 6–0",            "All Numbers",
        "Capital Letters",        "Common Punctuation",     "Short Words",
        "Medium Words",           "Long Words",             "Short Sentences",
        "Paragraphs",             "Speed Challenge",
    };
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
    };

    // ── State ────────────────────────────────────────────────────────────────
    private enum Screen { NO_KEYBOARD, MENU, LESSON, RESULTS }
    private Screen currentScreen = Screen.NO_KEYBOARD;
    private int currentLessonIndex = 0;
    private int cursorPos = 0;
    private int errorCount = 0;
    private int totalTyped = 0;
    private boolean lessonRunning = false;
    private long startTimeMs = 0;

    private boolean capsLockOn = false;
    private boolean numLockOn  = true;  // default on
    private boolean scrollLockOn = false;

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView tvLessonText;   // words to type (top)
    private TextView tvTypingBox;    // input mirror (middle)
    private TextView tvWpm, tvAcc, tvProg;
    private KeyboardView keyboardView;
    private LedView ledView;
    private LinearLayout layoutNoKeyboard, layoutMenu, layoutLesson, layoutResults;
    private TextView tvResultsText;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private InputManager inputManager;
    private boolean externalKeyboardConnected = false;

    // ── onCreate ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        buildUI();

        inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(this, null);
        checkKeyboard();
    }

    // ── UI construction ───────────────────────────────────────────────────────
    private void buildUI() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(C_BG);

        // ── No-keyboard overlay ─────────────────────────────────────────────
        layoutNoKeyboard = new LinearLayout(this);
        layoutNoKeyboard.setOrientation(LinearLayout.VERTICAL);
        layoutNoKeyboard.setGravity(android.view.Gravity.CENTER);
        layoutNoKeyboard.setBackgroundColor(C_BG);
        layoutNoKeyboard.setPadding(dp(40), dp(40), dp(40), dp(40));

        TextView nkIcon = makeTv("⌨", 48, C_KEY_ERROR, Typeface.NORMAL);
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

        // ── Menu ────────────────────────────────────────────────────────────
        layoutMenu = new LinearLayout(this);
        layoutMenu.setOrientation(LinearLayout.VERTICAL);
        layoutMenu.setBackgroundColor(C_BG);
        layoutMenu.setPadding(dp(32), dp(24), dp(32), dp(24));
        layoutMenu.setVisibility(View.GONE);

        TextView menuTitle = makeTv("GEZAJI Typing Tutor — Select Lesson", 18, C_ACCENT, Typeface.BOLD);
        menuTitle.setPadding(0, 0, 0, dp(16));
        layoutMenu.addView(menuTitle);

        // Scrollable lesson list
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        LinearLayout lessonList = new LinearLayout(this);
        lessonList.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < LESSON_TITLES.length; i++) {
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
        sv.addView(lessonList);
        layoutMenu.addView(sv);
        root.addView(layoutMenu, matchParent());

        // ── Lesson screen ───────────────────────────────────────────────────
        layoutLesson = new LinearLayout(this);
        layoutLesson.setOrientation(LinearLayout.VERTICAL);
        layoutLesson.setBackgroundColor(C_BG);
        layoutLesson.setVisibility(View.GONE);

        // 1) Lesson text (words to type) — top strip
        tvLessonText = makeTv("", 17, 0xFFFFFFFF, Typeface.NORMAL);
        tvLessonText.setPadding(dp(16), dp(10), dp(16), dp(6));
        tvLessonText.setBackgroundColor(C_PANEL);
        tvLessonText.setSingleLine(false);
        tvLessonText.setMaxLines(2);
        LinearLayout.LayoutParams ltLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ltLP.setMargins(dp(8), dp(8), dp(8), 0);
        layoutLesson.addView(tvLessonText, ltLP);

        // 2) Typing box — mid/upper
        tvTypingBox = makeTv("", 20, 0xFFFFFFFF, Typeface.NORMAL);
        tvTypingBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        tvTypingBox.setBackgroundColor(0xFF0D0D0D);
        tvTypingBox.setSingleLine(false);
        tvTypingBox.setMaxLines(3);
        tvTypingBox.setMinHeight(dp(64));
        LinearLayout.LayoutParams tbLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tbLP.setMargins(dp(8), dp(6), dp(8), dp(4));
        layoutLesson.addView(tvTypingBox, tbLP);

        // 3) Stats row
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

        // 4) LED indicators + keyboard — fill remaining space
        // LED bar
        ledView = new LedView(this);
        LinearLayout.LayoutParams ledLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28));
        ledLP.setMargins(dp(8), dp(2), dp(8), dp(2));
        layoutLesson.addView(ledView, ledLP);

        // 5) Keyboard visual — takes remaining height
        keyboardView = new KeyboardView(this);
        LinearLayout.LayoutParams kvLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        kvLP.setMargins(dp(4), 0, dp(4), dp(4));
        layoutLesson.addView(keyboardView, kvLP);

        // back button — tiny, top right corner overlay
        TextView btnBack = makeTv("✕ Menu", 12, 0xFF666666, Typeface.NORMAL);
        btnBack.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnBack.setBackgroundColor(0xFF1A1A1A);
        btnBack.setOnClickListener(v -> showMenu());
        FrameLayout.LayoutParams backLP = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        backLP.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        backLP.setMargins(0, dp(8), dp(8), 0);

        FrameLayout lessonFrame = new FrameLayout(this);
        lessonFrame.addView(layoutLesson, matchParent());
        lessonFrame.addView(btnBack, backLP);
        lessonFrame.setVisibility(View.GONE);
        root.addView(lessonFrame, matchParent());
        // store reference so we can show/hide
        layoutLesson.setTag(lessonFrame);

        // ── Results screen ───────────────────────────────────────────────────
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

        TextView btnNext = makeTv("  Next Lesson →  ", 15, 0xFFFFFFFF, Typeface.BOLD);
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

        TextView btnMenuR = makeTv("  ← Menu  ", 14, 0xFF888888, Typeface.NORMAL);
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

    // ── Lesson flow ───────────────────────────────────────────────────────────
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
        // Show lesson name + the full target text with colour coding
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
        // Mirror of what the user has typed so far, with cursor block
        String lesson = LESSONS[currentLessonIndex];
        if (cursorPos == 0) {
            // Show cursor waiting
            SpannableStringBuilder sb = new SpannableStringBuilder(" ");
            sb.setSpan(new BackgroundColorSpan(C_CURSOR), 0, 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvTypingBox.setText(sb);
            return;
        }
        String typed = lesson.substring(0, cursorPos);
        SpannableStringBuilder sb = new SpannableStringBuilder(typed + " ");
        // All typed text green
        sb.setSpan(new ForegroundColorSpan(C_TYPED), 0, cursorPos,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Cursor
        sb.setSpan(new BackgroundColorSpan(C_CURSOR), cursorPos, cursorPos+1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTypingBox.setText(sb);
    }

    // ── Key input ─────────────────────────────────────────────────────────────
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!externalKeyboardConnected) return super.onKeyDown(keyCode, event);

        // Toggle LED states
        if (keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            capsLockOn = !capsLockOn;
            ledView.update(capsLockOn, numLockOn, scrollLockOn);
            if (currentScreen == Screen.LESSON && keyboardView != null)
                keyboardView.flashKey(keyCode, false);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_NUM_LOCK) {
            numLockOn = !numLockOn;
            ledView.update(capsLockOn, numLockOn, scrollLockOn);
            if (currentScreen == Screen.LESSON && keyboardView != null)
                keyboardView.flashKey(keyCode, false);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_SCROLL_LOCK) {
            scrollLockOn = !scrollLockOn;
            ledView.update(capsLockOn, numLockOn, scrollLockOn);
            if (currentScreen == Screen.LESSON && keyboardView != null)
                keyboardView.flashKey(keyCode, false);
            return true;
        }

        // Navigate away
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (currentScreen == Screen.LESSON || currentScreen == Screen.RESULTS) showMenu();
            return true;
        }

        if (currentScreen == Screen.MENU) {
            // Allow pressing number keys to pick lessons
            // (keyboard users can just use the screen for now)
            return super.onKeyDown(keyCode, event);
        }

        if (currentScreen != Screen.LESSON) return super.onKeyDown(keyCode, event);

        String lesson = LESSONS[currentLessonIndex];
        if (cursorPos >= lesson.length()) return true;

        char expected = lesson.charAt(cursorPos);
        char typed = getChar(event);
        if (typed == 0) {
            // Still animate the key press on the visual
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
            cursorPos++;
            updateStats();
            renderLessonText();
            renderTypingBox();
            if (cursorPos >= lesson.length()) lessonComplete();
        } else {
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

    // ── Stats ─────────────────────────────────────────────────────────────────
    private void updateStats() {
        String lesson = LESSONS[currentLessonIndex];
        long elapsed = System.currentTimeMillis() - startTimeMs;
        double mins = elapsed / 60000.0;
        int wpm = (mins > 0.01) ? (int)((cursorPos / 5.0) / mins) : 0;
        int acc = (totalTyped > 0) ? (int)(((totalTyped - errorCount) * 100.0) / totalTyped) : 100;
        tvWpm.setText(wpm + " WPM");
        tvAcc.setText(acc + "%");
        tvProg.setText(cursorPos + " / " + lesson.length());
        // tint accuracy chip
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

    // ── Lesson complete ───────────────────────────────────────────────────────
    private void lessonComplete() {
        stopTimer(); lessonRunning = false;
        String lesson = LESSONS[currentLessonIndex];
        long elapsed = System.currentTimeMillis() - startTimeMs;
        double mins = elapsed / 60000.0;
        int wpm = (mins > 0) ? (int)((lesson.length() / 5.0) / mins) : 0;
        int acc = (totalTyped > 0) ? (int)(((totalTyped-errorCount)*100.0)/totalTyped) : 100;
        int secs = (int)(elapsed / 1000);
        String grade = wpm >= 60 && acc >= 95 ? "⭐ Excellent!" :
                       wpm >= 40 && acc >= 90 ? "✅ Good" :
                       wpm >= 20 && acc >= 80 ? "📈 Keep Practising" : "🔄 Try Again";
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

    // ── Navigation ────────────────────────────────────────────────────────────
    private void showMenu() { stopTimer(); lessonRunning = false; showScreen(Screen.MENU); }

    private void showScreen(Screen s) {
        currentScreen = s;
        layoutNoKeyboard.setVisibility(s == Screen.NO_KEYBOARD ? View.VISIBLE : View.GONE);
        layoutMenu.setVisibility(s == Screen.MENU ? View.VISIBLE : View.GONE);
        // lesson is wrapped in a FrameLayout stored as tag
        View lessonFrame = (View) layoutLesson.getTag();
        if (lessonFrame != null) lessonFrame.setVisibility(s == Screen.LESSON ? View.VISIBLE : View.GONE);
        layoutResults.setVisibility(s == Screen.RESULTS ? View.VISIBLE : View.GONE);
    }

    // ── Keyboard detection ────────────────────────────────────────────────────
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
        if (inputManager != null) inputManager.unregisterInputDeviceListener(this);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
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
        View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w, 1)); return v;
    }
    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }
    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LED INDICATOR VIEW
    // ════════════════════════════════════════════════════════════════════════
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

            drawLed(canvas, startX,          y, dotR, caps,   "Caps Lock");
            drawLed(canvas, startX + dp(90), y, dotR, num,    "Num Lock");
            drawLed(canvas, startX + dp(180),y, dotR, scroll, "Scroll Lock");
        }

        private void drawLed(Canvas c, int x, int y, int r, boolean on, String label) {
            // Glow
            if (on) {
                p.setColor(0x4422C55E); p.setStyle(Paint.Style.FILL);
                c.drawCircle(x, y, r * 2.2f, p);
            }
            // Dot
            p.setColor(on ? C_LED_ON : C_LED_OFF); p.setStyle(Paint.Style.FILL);
            c.drawCircle(x, y, r, p);
            // Label
            p.setColor(on ? 0xFFCCCCCC : 0xFF555555);
            p.setTextSize(dp(10)); p.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, x, y + r + dp(10), p);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  104-KEY KEYBOARD VIEW
    // ════════════════════════════════════════════════════════════════════════
    class KeyboardView extends View {

        // Key descriptor
        static final int W1  = 1;   // 1u
        static final int W15 = 2;   // 1.5u
        static final int W175= 3;   // 1.75u
        static final int W2  = 4;   // 2u
        static final int W225= 5;   // 2.25u
        static final int W275= 6;   // 2.75u
        static final int W625= 7;   // 6.25u (spacebar)
        static final int WSPC= 8;   // small spacer (0.5u gap between clusters)

        // Each key: { keyCode, widthType, "label", "shiftLabel" }
        // widthType < 0 means spacer of abs(widthType)*0.5u

        private class Key {
            int code; float widthU; String label, sub;
            float x, y, w, h;
            Key(int code, float w, String label, String sub) {
                this.code=code; this.widthU=w; this.label=label; this.sub=sub;
            }
        }

        // Rows of keys. We'll lay them out manually.
        // Using actual Android KeyEvent keycodes.

        private final Key[][] ROWS = buildRows();

        private Key[][] buildRows() {
            return new Key[][] {
                // Row 0: Function row
                {
                    k(KeyEvent.KEYCODE_ESCAPE,     1f,  "Esc",    ""),
                    gap(0.5f),
                    k(KeyEvent.KEYCODE_F1,         1f,  "F1",     ""),
                    k(KeyEvent.KEYCODE_F2,         1f,  "F2",     ""),
                    k(KeyEvent.KEYCODE_F3,         1f,  "F3",     ""),
                    k(KeyEvent.KEYCODE_F4,         1f,  "F4",     ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_F5,         1f,  "F5",     ""),
                    k(KeyEvent.KEYCODE_F6,         1f,  "F6",     ""),
                    k(KeyEvent.KEYCODE_F7,         1f,  "F7",     ""),
                    k(KeyEvent.KEYCODE_F8,         1f,  "F8",     ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_F9,         1f,  "F9",     ""),
                    k(KeyEvent.KEYCODE_F10,        1f,  "F10",    ""),
                    k(KeyEvent.KEYCODE_F11,        1f,  "F11",    ""),
                    k(KeyEvent.KEYCODE_F12,        1f,  "F12",    ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_SYSRQ,      1f,  "PrtSc",  ""),
                    k(KeyEvent.KEYCODE_SCROLL_LOCK,1f,  "Scrl",   ""),
                    k(KeyEvent.KEYCODE_BREAK,      1f,  "Pause",  ""),
                },
                // Row 1: Number row
                {
                    k(KeyEvent.KEYCODE_GRAVE,      1f,  "`",      "~"),
                    k(KeyEvent.KEYCODE_1,          1f,  "1",      "!"),
                    k(KeyEvent.KEYCODE_2,          1f,  "2",      "@"),
                    k(KeyEvent.KEYCODE_3,          1f,  "3",      "#"),
                    k(KeyEvent.KEYCODE_4,          1f,  "4",      "$"),
                    k(KeyEvent.KEYCODE_5,          1f,  "5",      "%"),
                    k(KeyEvent.KEYCODE_6,          1f,  "6",      "^"),
                    k(KeyEvent.KEYCODE_7,          1f,  "7",      "&"),
                    k(KeyEvent.KEYCODE_8,          1f,  "8",      "*"),
                    k(KeyEvent.KEYCODE_9,          1f,  "9",      "("),
                    k(KeyEvent.KEYCODE_0,          1f,  "0",      ")"),
                    k(KeyEvent.KEYCODE_MINUS,      1f,  "-",      "_"),
                    k(KeyEvent.KEYCODE_EQUALS,     1f,  "=",      "+"),
                    k(KeyEvent.KEYCODE_DEL,        2f,  "⌫ Back", ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_INSERT,     1f,  "Ins",    ""),
                    k(KeyEvent.KEYCODE_HOME,       1f,  "Home",   ""),
                    k(KeyEvent.KEYCODE_PAGE_UP,    1f,  "PgUp",   ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUM_LOCK,   1f,  "Num",    ""),
                    k(KeyEvent.KEYCODE_NUMPAD_DIVIDE,1f, "/",     ""),
                    k(KeyEvent.KEYCODE_NUMPAD_MULTIPLY,1f,"*",   ""),
                    k(KeyEvent.KEYCODE_NUMPAD_SUBTRACT,1f,"-",   ""),
                },
                // Row 2: QWERTY
                {
                    k(KeyEvent.KEYCODE_TAB,        1.5f,"Tab",    ""),
                    k(KeyEvent.KEYCODE_Q,          1f,  "Q",      ""),
                    k(KeyEvent.KEYCODE_W,          1f,  "W",      ""),
                    k(KeyEvent.KEYCODE_E,          1f,  "E",      ""),
                    k(KeyEvent.KEYCODE_R,          1f,  "R",      ""),
                    k(KeyEvent.KEYCODE_T,          1f,  "T",      ""),
                    k(KeyEvent.KEYCODE_Y,          1f,  "Y",      ""),
                    k(KeyEvent.KEYCODE_U,          1f,  "U",      ""),
                    k(KeyEvent.KEYCODE_I,          1f,  "I",      ""),
                    k(KeyEvent.KEYCODE_O,          1f,  "O",      ""),
                    k(KeyEvent.KEYCODE_P,          1f,  "P",      ""),
                    k(KeyEvent.KEYCODE_LEFT_BRACKET,1f, "[",      "{"),
                    k(KeyEvent.KEYCODE_RIGHT_BRACKET,1f,"]",      "}"),
                    k(KeyEvent.KEYCODE_BACKSLASH,  1.5f,"\\",     "|"),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_FORWARD_DEL,1f,  "Del",    ""),
                    k(KeyEvent.KEYCODE_MOVE_END,        1f,  "End",    ""),
                    k(KeyEvent.KEYCODE_PAGE_DOWN,  1f,  "PgDn",   ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_7,   1f,  "7",      "Home"),
                    k(KeyEvent.KEYCODE_NUMPAD_8,   1f,  "8",      "↑"),
                    k(KeyEvent.KEYCODE_NUMPAD_9,   1f,  "9",      "PgUp"),
                    k(KeyEvent.KEYCODE_NUMPAD_ADD, 1f,  "+",      ""),
                },
                // Row 3: ASDF
                {
                    k(KeyEvent.KEYCODE_CAPS_LOCK,  1.75f,"Caps",  ""),
                    k(KeyEvent.KEYCODE_A,          1f,  "A",      ""),
                    k(KeyEvent.KEYCODE_S,          1f,  "S",      ""),
                    k(KeyEvent.KEYCODE_D,          1f,  "D",      ""),
                    k(KeyEvent.KEYCODE_F,          1f,  "F",      ""),
                    k(KeyEvent.KEYCODE_G,          1f,  "G",      ""),
                    k(KeyEvent.KEYCODE_H,          1f,  "H",      ""),
                    k(KeyEvent.KEYCODE_J,          1f,  "J",      ""),
                    k(KeyEvent.KEYCODE_K,          1f,  "K",      ""),
                    k(KeyEvent.KEYCODE_L,          1f,  "L",      ""),
                    k(KeyEvent.KEYCODE_SEMICOLON,  1f,  ";",      ":"),
                    k(KeyEvent.KEYCODE_APOSTROPHE, 1f,  "'",      "\""),
                    k(KeyEvent.KEYCODE_ENTER,      2.25f,"Enter", ""),
                    gap(0.25f),
                    gap(1f), gap(1f), gap(1f), // placeholders (nav cluster offset)
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_4,   1f,  "4",      "←"),
                    k(KeyEvent.KEYCODE_NUMPAD_5,   1f,  "5",      ""),
                    k(KeyEvent.KEYCODE_NUMPAD_6,   1f,  "6",      "→"),
                    // numpad+ is 2u tall — represented once here
                },
                // Row 4: ZXCV
                {
                    k(KeyEvent.KEYCODE_SHIFT_LEFT, 2.25f,"⇧ Shift",""),
                    k(KeyEvent.KEYCODE_Z,          1f,  "Z",      ""),
                    k(KeyEvent.KEYCODE_X,          1f,  "X",      ""),
                    k(KeyEvent.KEYCODE_C,          1f,  "C",      ""),
                    k(KeyEvent.KEYCODE_V,          1f,  "V",      ""),
                    k(KeyEvent.KEYCODE_B,          1f,  "B",      ""),
                    k(KeyEvent.KEYCODE_N,          1f,  "N",      ""),
                    k(KeyEvent.KEYCODE_M,          1f,  "M",      ""),
                    k(KeyEvent.KEYCODE_COMMA,      1f,  ",",      "<"),
                    k(KeyEvent.KEYCODE_PERIOD,     1f,  ".",      ">"),
                    k(KeyEvent.KEYCODE_SLASH,      1f,  "/",      "?"),
                    k(KeyEvent.KEYCODE_SHIFT_RIGHT,2.75f,"⇧ Shift",""),
                    gap(0.25f),
                    gap(1f),
                    k(KeyEvent.KEYCODE_DPAD_UP,    1f,  "↑",      ""),
                    gap(1f),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_1,   1f,  "1",      "End"),
                    k(KeyEvent.KEYCODE_NUMPAD_2,   1f,  "2",      "↓"),
                    k(KeyEvent.KEYCODE_NUMPAD_3,   1f,  "3",      "PgDn"),
                    k(KeyEvent.KEYCODE_NUMPAD_ENTER,1f, "Ent",    ""),
                },
                // Row 5: Bottom row
                {
                    k(KeyEvent.KEYCODE_CTRL_LEFT,  1.25f,"Ctrl",  ""),
                    k(KeyEvent.KEYCODE_META_LEFT,  1.25f,"Win",   ""),
                    k(KeyEvent.KEYCODE_ALT_LEFT,   1.25f,"Alt",   ""),
                    k(KeyEvent.KEYCODE_SPACE,      6.25f,"",      ""),
                    k(KeyEvent.KEYCODE_ALT_RIGHT,  1.25f,"AltGr", ""),
                    k(KeyEvent.KEYCODE_META_RIGHT, 1.25f,"Win",   ""),
                    k(KeyEvent.KEYCODE_MENU,       1.25f,"Menu",  ""),
                    k(KeyEvent.KEYCODE_CTRL_RIGHT, 1.25f,"Ctrl",  ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_DPAD_LEFT,  1f,  "←",      ""),
                    k(KeyEvent.KEYCODE_DPAD_DOWN,  1f,  "↓",      ""),
                    k(KeyEvent.KEYCODE_DPAD_RIGHT, 1f,  "→",      ""),
                    gap(0.25f),
                    k(KeyEvent.KEYCODE_NUMPAD_0,   2f,  "0",      "Ins"),
                    k(KeyEvent.KEYCODE_NUMPAD_DOT, 1f,  ".",      "Del"),
                },
            };
        }

        private Key k(int code, float w, String label, String sub) {
            return new Key(code, w, label, sub);
        }
        private Key gap(float w) { return new Key(-1, w, "", ""); }

        // Active/flash state
        private final Map<Integer, Long>  pressedAt  = new HashMap<>();
        private final Map<Integer, Boolean> pressedErr = new HashMap<>();
        private static final long FLASH_MS = 180;

        // Computed layout
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
                    if (key.code < 0) continue; // gap
                    drawKey(canvas, key, now);
                }
            }
        }

        private void computeLayout() {
            if (getWidth() == 0 || getHeight() == 0) return;
            int W = getWidth(); int H = getHeight();
            // Keyboard has ~18.75u wide (standard 104-key with numpad), 6 rows
            // We fit it to the view
            float totalU = 18.75f; // approximate full width in units
            int numRows = ROWS.length;
            padX = dp(4); padY = dp(4);
            float availW = W - padX * 2;
            float availH = H - padY * 2;
            keyUnit = availW / totalU;
            keyH = Math.min(availH / (numRows + 0.5f), keyUnit * 1.1f);

            float rowSpacing = keyH + dp(2);
            // Row 0 (Fn) is slightly shorter visually
            float y = padY;
            for (int r = 0; r < ROWS.length; r++) {
                float x = padX;
                Key[] row = ROWS[r];
                for (Key key : row) {
                    float kw = key.widthU * keyUnit - dp(2);
                    key.x = x + dp(1); key.y = y + dp(1);
                    key.w = kw; key.h = keyH - dp(2);
                    x += key.widthU * keyUnit;
                }
                y += rowSpacing;
                if (r == 0) y += dp(4); // extra gap after fn row
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
            long age = now - pressTime;
            boolean active = age < FLASH_MS;

            // Key body
            RectF rect = new RectF(key.x, key.y, key.x + key.w, key.y + key.h);
            float r = dp(3);

            if (active) {
                boolean err = Boolean.TRUE.equals(pressedErr.get(key.code));
                // Glow halo
                paint.setColor(err ? 0x44EF4444 : 0x4422C55E);
                paint.setStyle(Paint.Style.FILL);
                RectF glow = new RectF(rect.left-dp(2), rect.top-dp(2),
                        rect.right+dp(2), rect.bottom+dp(2));
                canvas.drawRoundRect(glow, r+dp(2), r+dp(2), paint);
                // Key face
                paint.setColor(err ? 0xFF5A1010 : 0xFF0D3D1A);
            } else {
                paint.setColor(C_KEY_BODY);
            }
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, r, r, paint);

            // Border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(active ? (Boolean.TRUE.equals(pressedErr.get(key.code))
                    ? C_KEY_ERROR : C_KEY_CORRECT) : C_KEY_BORDER);
            canvas.drawRoundRect(rect, r, r, paint);

            // Label
            if (key.label.isEmpty()) return;
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);

            // Main label size — scale to key width
            float labelSize = Math.min(key.h * 0.38f, key.w * 0.5f);
            labelSize = Math.max(labelSize, dp(6));
            paint.setTextSize(labelSize);
            paint.setColor(active ? 0xFFFFFFFF : C_KEY_LABEL);
            paint.setTypeface(Typeface.MONOSPACE);

            float cx = key.x + key.w / 2;
            float cy = key.y + key.h / 2 + labelSize * 0.35f;

            // If there's a sub-label (shift char), put label bottom-left, sub top-right
            if (!key.sub.isEmpty() && key.w <= keyUnit * 1.1f) {
                float smallSz = labelSize * 0.65f;
                // main label bottom-left
                paint.setTextSize(labelSize * 0.75f);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(key.label, key.x + dp(3), key.y + key.h - dp(3), paint);
                // sub top-right
                paint.setTextSize(smallSz);
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setColor(active ? 0xFFCCFFCC : 0xFF909090);
                canvas.drawText(key.sub, key.x + key.w - dp(3), key.y + smallSz + dp(1), paint);
            } else {
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(key.label, cx, cy, paint);
            }
        }
    }
}
