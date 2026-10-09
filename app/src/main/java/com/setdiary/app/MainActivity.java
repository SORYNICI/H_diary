package com.setdiary.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(24, 32, 46), green = Color.rgb(23, 111, 82);
    private final String[] parts = {"가슴", "어깨", "등", "이두", "삼두", "하체", "복근"};
    private final String[][] exercises = {
        {"벤치 프레스", "인클라인 벤치 프레스", "덤벨 프레스", "체스트 프레스", "플라이", "푸시업"},
        {"숄더 프레스", "덤벨 숄더 프레스", "사이드 레터럴 레이즈", "리어 델트 플라이"},
        {"랫 풀다운", "시티드 로우", "바벨 로우", "덤벨 로우", "풀업", "데드리프트"},
        {"바벨 컬", "덤벨 컬", "해머 컬", "프리처 컬"},
        {"케이블 푸시다운", "오버헤드 익스텐션", "스컬 크러셔", "딥스"},
        {"스쿼트", "레그 프레스", "런지", "레그 익스텐션", "레그 컬", "카프 레이즈"},
        {"크런치", "레그 레이즈", "케이블 크런치", "싯업"}
    };
    private SharedPreferences prefs;
    private JSONObject days;
    private JSONArray entries;
    private LocalDate date = LocalDate.now();
    private LinearLayout root;
    private int part = 0, reps = 10;
    private double weight = 20;
    private String exercise = "벤치 프레스";
    private boolean history = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(244, 247, 249));
        getWindow().setNavigationBarColor(Color.rgb(244, 247, 249));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("diary", MODE_PRIVATE);
        try { days = new JSONObject(prefs.getString("days", "{}")); }
        catch (JSONException e) { days = new JSONObject(); toast("저장한 기록을 읽지 못했습니다."); }
        if (state != null) {
            date = LocalDate.parse(state.getString("date", date.toString()));
            part = state.getInt("part", 0); weight = state.getDouble("weight", 20);
            reps = state.getInt("reps", 10); exercise = state.getString("exercise", exercises[part][0]);
            history = state.getBoolean("history", false);
        }
        loadDay(); render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("date", date.toString()); state.putInt("part", part);
        state.putDouble("weight", weight); state.putInt("reps", reps);
        state.putString("exercise", exercise); state.putBoolean("history", history);
        super.onSaveInstanceState(state);
    }
    private void loadDay() { entries = days.optJSONArray(date.toString()); if (entries == null) entries = new JSONArray(); }
    private boolean persist() {
        try { days.put(date.toString(), entries); }
        catch (JSONException e) { toast("기록을 저장하지 못했습니다."); return false; }
        boolean saved = prefs.edit().putString("days", days.toString()).commit();
        if (!saved) toast("저장 공간을 확인해 주세요. 기록을 저장하지 못했습니다.");
        return saved;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable surface(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView label(String text, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(text); t.setTextColor(ink); t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(7), 0, dp(7)); return t;
    }
    private Button button(String text, boolean active, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(active ? Color.WHITE : ink); b.setBackground(surface(active ? green : Color.rgb(232, 238, 242), 12));
        b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void addButton(LinearLayout row, Button button) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(dp(3), dp(4), dp(3), dp(4)); row.addView(button, p);
    }
    private void spacer(int height) { View v = new View(this); root.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private String number(double value) { return value == Math.floor(value) ? String.valueOf((long)value) : String.format(Locale.US, "%.1f", value); }
    private void render() {
        int previousScroll = root != null && root.getParent() instanceof ScrollView ? ((ScrollView) root.getParent()).getScrollY() : 0;
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(244, 247, 249));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root = column(); root.setPadding(dp(18), dp(20), dp(18), dp(28)); scroll.addView(root); setContentView(scroll);
        scroll.requestApplyInsets();
        scroll.post(() -> scroll.scrollTo(0, previousScroll));
        root.addView(label("SET DIARY", 13, true));
        root.addView(label("세트일지", 30, true));
        LinearLayout tabs = row();
        addButton(tabs, button("운동 기록", !history, () -> { history = false; render(); }));
        addButton(tabs, button("지난 일지", history, () -> { history = true; render(); })); root.addView(tabs);
        spacer(12);
        LinearLayout dates = row();
        addButton(dates, button("이전", false, () -> changeDate(date.minusDays(1))));
        Button dateButton = button(date.getMonthValue() + "월 " + date.getDayOfMonth() + "일", false, this::pickDate);
        dateButton.setContentDescription(date.toString() + " 날짜 선택"); addButton(dates, dateButton);
        addButton(dates, button("다음", false, () -> changeDate(date.plusDays(1)))); root.addView(dates);
        root.addView(label(date.toString() + (date.equals(LocalDate.now()) ? " · 오늘" : ""), 14, false));
        if (history) { renderHistory(); return; }
        root.addView(label("01  운동 부위", 18, true));
        for (int start = 0; start < parts.length; start += 4) {
            LinearLayout r = row();
            for (int i = start; i < Math.min(start + 4, parts.length); i++) {
                final int index = i;
                addButton(r, button(parts[i], part == i, () -> { part = index; exercise = exercises[index][0]; render(); }));
            } root.addView(r);
        }
        spacer(10); root.addView(label("02  운동 선택", 18, true));
        for (int start = 0; start < exercises[part].length; start += 2) {
            LinearLayout r = row();
            for (int i = start; i < Math.min(start + 2, exercises[part].length); i++) {
                final String name = exercises[part][i];
                addButton(r, button(name, exercise.equals(name), () -> { exercise = name; render(); }));
            } root.addView(r);
        }
        spacer(10); root.addView(label("03  세트 기록", 18, true));
        LinearLayout card = column(); card.setPadding(dp(14), dp(10), dp(14), dp(14)); card.setBackground(surface(Color.WHITE, 18));
        card.addView(label(exercise + " · " + (setCount(exercise) + 1) + "세트", 18, true));
        card.addView(label("무게 · 맨몸은 0 kg", 14, false));
        LinearLayout weights = row();
        addButton(weights, button("− 2.5", false, () -> { weight = Math.max(0, weight - 2.5); render(); }));
        addButton(weights, button(number(weight) + " kg", false, () -> exactValue(true)));
        addButton(weights, button("+ 2.5", false, () -> { weight = Math.min(1000, weight + 2.5); render(); })); card.addView(weights);
        LinearLayout presets = row();
        for (int w : new int[]{0, 10, 20, 40, 60}) addButton(presets, button("" + w, weight == w, () -> { weight = w; render(); }));
        card.addView(presets); card.addView(label("횟수", 14, false));
        LinearLayout counts = row();
        addButton(counts, button("−", false, () -> { reps = Math.max(1, reps - 1); render(); }));
        addButton(counts, button(reps + " 회", false, () -> exactValue(false)));
        addButton(counts, button("+", false, () -> { reps = Math.min(999, reps + 1); render(); })); card.addView(counts);
        LinearLayout quick = row();
        for (int count : new int[]{5, 8, 10, 12, 15}) addButton(quick, button("" + count, reps == count, () -> { reps = count; render(); }));
        card.addView(quick);
        Button add = button("+  세트 기록", true, this::addSet);
        LinearLayout.LayoutParams full = new LinearLayout.LayoutParams(-1, -2); full.topMargin = dp(12); card.addView(add, full);
        card.addView(label("기록 후 같은 무게·횟수로 다음 세트를 추가할 수 있어요.", 14, false)); root.addView(card);
        renderEntries();
    }
    private void changeDate(LocalDate next) { date = next; loadDay(); render(); }
    private void pickDate() {
        new DatePickerDialog(this, (v, y, m, d) -> changeDate(LocalDate.of(y, m + 1, d)), date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()).show();
    }
    private int setCount(String name) {
        int count = 0; for (int i = 0; i < entries.length(); i++) if (name.equals(entries.optJSONObject(i).optString("exercise"))) count++; return count;
    }
    private void exactValue(boolean isWeight) {
        EditText input = new EditText(this); input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | (isWeight ? android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        input.setText(isWeight ? number(weight) : String.valueOf(reps)); input.selectAll();
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(isWeight ? "무게 (kg)" : "횟수")
            .setView(input).setNegativeButton("취소", null).setPositiveButton("적용", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            try {
                double value = Double.parseDouble(input.getText().toString());
                if (!Double.isFinite(value) || value < (isWeight ? 0 : 1) || value > (isWeight ? 1000 : 999) || (!isWeight && value != Math.floor(value))) throw new NumberFormatException();
                if (isWeight) weight = Math.round(value * 10) / 10.0; else reps = (int)value;
                dialog.dismiss(); render();
            } catch (NumberFormatException e) { input.setError(isWeight ? "0~1000 사이의 무게를 입력해 주세요." : "1~999 사이의 정수를 입력해 주세요."); }
        })); dialog.show();
    }
    private void addSet() {
        try {
            JSONObject set = new JSONObject(); set.put("part", parts[part]); set.put("exercise", exercise); set.put("weight", weight); set.put("reps", reps);
            entries.put(set); if (persist()) toast("" + setCount(exercise) + "세트 저장 완료"); render();
        } catch (JSONException e) { toast("기록을 추가하지 못했습니다."); }
    }
    private double volume() {
        double total = 0; for (int i = 0; i < entries.length(); i++) { JSONObject s = entries.optJSONObject(i); total += s.optDouble("weight") * s.optInt("reps"); } return total;
    }
    private void renderEntries() {
        spacer(18); root.addView(label("하루 운동 일지", 22, true));
        if (entries.length() == 0) { root.addView(label("아직 기록한 운동이 없어요. 첫 세트를 추가해 주세요.", 16, false)); return; }
        Set<String> names = new LinkedHashSet<>(); for (int i = 0; i < entries.length(); i++) names.add(entries.optJSONObject(i).optString("exercise"));
        root.addView(label(names.size() + "개 운동  ·  " + entries.length() + "세트  ·  " + number(volume()) + " kg 총 볼륨", 15, true));
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            final int index = i; JSONObject s = entries.optJSONObject(i); String name = s.optString("exercise");
            int n = counts.containsKey(name) ? counts.get(name) + 1 : 1; counts.put(name, n);
            LinearLayout r = row(); r.setPadding(dp(10), dp(8), dp(10), dp(8));
            TextView t = label(s.optString("part") + " · " + name + "\n" + n + "세트   " + number(s.optDouble("weight")) + " kg × " + s.optInt("reps") + "회", 16, false);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            Button more = button("수정", false, () -> editSet(index)); r.addView(more); root.addView(r);
        }
        LinearLayout actions = row();
        addButton(actions, button("이미지 저장", false, () -> exportImage(false)));
        addButton(actions, button("이미지 공유", true, () -> exportImage(true))); root.addView(actions);
        root.addView(label("기록은 이 휴대폰에 자동 저장됩니다.", 14, false));
    }
    private void editSet(int index) {
        JSONObject set = entries.optJSONObject(index);
        new AlertDialog.Builder(this).setTitle(set.optString("exercise"))
            .setItems(new String[]{"무게·횟수 수정", "같은 세트 추가", "세트 삭제"}, (dialog, which) -> {
                if (which == 2) {
                    new AlertDialog.Builder(this).setMessage("이 세트를 삭제할까요?").setNegativeButton("취소", null)
                        .setPositiveButton("삭제", (d, w) -> { entries.remove(index); persist(); render(); }).show();
                } else if (which == 1) {
                    try { entries.put(new JSONObject(set.toString())); persist(); render(); } catch (JSONException e) { toast("복사하지 못했습니다."); }
                } else {
                    LinearLayout fields = column(); fields.setPadding(dp(20), 0, dp(20), 0);
                    EditText w = new EditText(this); w.setInputType(8194); w.setText(number(set.optDouble("weight")));
                    EditText r = new EditText(this); r.setInputType(2); r.setText("" + set.optInt("reps"));
                    fields.addView(label("무게 (kg)", 14, false)); fields.addView(w); fields.addView(label("횟수", 14, false)); fields.addView(r);
                    AlertDialog editor = new AlertDialog.Builder(this).setTitle("세트 수정").setView(fields).setNegativeButton("취소", null).setPositiveButton("저장", null).create();
                    editor.setOnShowListener(v -> editor.getButton(-1).setOnClickListener(b -> {
                        try {
                            double ww = Double.parseDouble(w.getText().toString()); int rr = Integer.parseInt(r.getText().toString());
                            if (!Double.isFinite(ww) || ww < 0 || ww > 1000 || rr < 1 || rr > 999) throw new NumberFormatException();
                            set.put("weight", Math.round(ww * 10) / 10.0); set.put("reps", rr); persist(); editor.dismiss(); render();
                        } catch (Exception e) { toast("무게 0~1000, 횟수 1~999로 입력해 주세요."); }
                    })); editor.show();
                }
            }).show();
    }
    private void renderHistory() {
        root.addView(label("지난 일지", 22, true));
        List<String> dates = new ArrayList<>(); Iterator<String> it = days.keys();
        while (it.hasNext()) { String key = it.next(); if (days.optJSONArray(key).length() > 0) dates.add(key); }
        Collections.sort(dates, Collections.reverseOrder());
        if (dates.isEmpty()) root.addView(label("저장한 일지가 없어요. 운동 기록에서 첫 세트를 남겨 보세요.", 16, false));
        for (String key : dates) {
            JSONArray sets = days.optJSONArray(key);
            Button day = button(key + "   ·   " + sets.length() + "세트", key.equals(date.toString()), () -> { history = false; changeDate(LocalDate.parse(key)); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(8); root.addView(day, p);
        }
    }
    private void exportImage(boolean share) {
        if (entries.length() == 0) return;
        try {
            int height = 330 + entries.length() * 92;
            if (height > 20000) { toast("세트 수가 너무 많아 이미지를 만들 수 없습니다."); return; }
            Bitmap image = Bitmap.createBitmap(1080, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image); canvas.drawColor(Color.rgb(244, 247, 249));
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(green); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(28);
            canvas.drawText("SET DIARY / 세트일지", 60, 65, paint); paint.setColor(ink); paint.setTextSize(48);
            canvas.drawText(date.toString() + " 운동 일지", 60, 140, paint); paint.setTextSize(28);
            canvas.drawText(entries.length() + "세트 · 총 볼륨 " + number(volume()) + " kg", 60, 195, paint);
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject s = entries.optJSONObject(i); String name = s.optString("exercise");
                int n = counts.containsKey(name) ? counts.get(name) + 1 : 1; counts.put(name, n);
                paint.setTextSize(30); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                canvas.drawText(s.optString("part") + " · " + name, 60, 270 + i * 92, paint);
                paint.setTextSize(27); paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                canvas.drawText(n + "세트    " + number(s.optDouble("weight")) + " kg × " + s.optInt("reps") + "회", 60, 308 + i * 92, paint);
            }
            String filename = "diary-" + date + ".png";
            if (share) {
                File file = new File(getCacheDir(), filename);
                try (OutputStream out = new FileOutputStream(file)) { if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException(); }
                Uri uri = Uri.parse("content://com.setdiary.app.images/" + filename);
                Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("image/png"); intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setClipData(ClipData.newRawUri("운동 일지", uri)); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "운동 일지 공유"));
            } else if (android.os.Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues(); values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png"); values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SetDiary");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException();
                try {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) { if (out == null || !image.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException(); }
                    values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); getContentResolver().update(uri, values, null, null);
                } catch (Exception e) { getContentResolver().delete(uri, null, null); throw e; }
                toast("갤러리에 운동 일지 이미지를 저장했습니다.");
            } else {
                File file = new File(getCacheDir(), filename);
                try (OutputStream out = new FileOutputStream(file)) { image.compress(Bitmap.CompressFormat.PNG, 100, out); }
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("image/png").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, filename);
                prefs.edit().putString("pendingImage", filename).apply(); startActivityForResult(intent, 10);
            }
            image.recycle();
        } catch (Exception e) { toast("이미지를 만들지 못했습니다. 저장 공간과 공유 앱을 확인해 주세요."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 10 && result == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream in = new FileInputStream(new File(getCacheDir(), prefs.getString("pendingImage", "")));
                 OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out == null) throw new IOException();
                byte[] buffer = new byte[8192]; int length; while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
                toast("운동 일지 이미지를 저장했습니다.");
            } catch (Exception e) { toast("이미지를 저장하지 못했습니다."); }
        }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
