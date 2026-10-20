package com.vivo144.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebloatActivity extends Activity {
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Map<String, TextView> stateViews = new HashMap<>();
    private TextView log;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Debloat CN (via Shizuku)");
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView intro = text("Remove só para o seu usuário (o APK continua na partição do sistema, "
            + "então dá para restaurar a qualquer momento). Se a vivo bloquear a remoção, o app desativa. "
            + "Pacotes essenciais (gamewatch, pem, launcher, System UI…) são recusados sempre.", 13);
        intro.setAlpha(0.75f);
        root.addView(intro);

        log = text("", 12);
        log.setTypeface(Typeface.MONOSPACE);
        log.setPadding(0, dp(8), 0, dp(8));
        root.addView(log);

        // Custom package
        final EditText custom = new EditText(this);
        custom.setHint("outro pacote, ex: com.exemplo.app");
        custom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        custom.setSingleLine(true);
        root.addView(custom);
        LinearLayout cb = row();
        cb.addView(button("Remover", v -> confirmRemove(custom.getText().toString().trim())));
        cb.addView(button("Restaurar", v -> act(custom.getText().toString().trim(), false)));
        root.addView(cb);

        for (Debloat.App a : Debloat.CN_LIST) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(0, dp(14), 0, 0);
            TextView title = text(a.name, 16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(title);
            TextView sub = text(a.pkg + "\n" + a.desc, 12);
            sub.setAlpha(0.75f);
            card.addView(sub);
            TextView st = text("estado: …", 12);
            stateViews.put(a.pkg, st);
            card.addView(st);
            LinearLayout b = row();
            b.addView(button("Remover", v -> confirmRemove(a.pkg)));
            b.addView(button("Restaurar", v -> act(a.pkg, false)));
            card.addView(b);
            root.addView(card);
        }

        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        sv.addView(root);
        setContentView(sv);
        refreshStates();
    }

    @Override
    protected void onDestroy() {
        bg.shutdown();
        super.onDestroy();
    }

    private void confirmRemove(String pkg) {
        if (pkg.isEmpty()) return;
        new AlertDialog.Builder(this)
            .setTitle("Remover " + pkg + "?")
            .setMessage("Remove/desativa só para o usuário 0. Dá para desfazer em \"Restaurar\".")
            .setPositiveButton("Remover", (d, w) -> act(pkg, true))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void act(String pkg, boolean remove) {
        if (pkg.isEmpty()) return;
        say((remove ? "Removendo " : "Restaurando ") + pkg + "…");
        bg.execute(() -> {
            if (!Shell.ready()) {
                ui.post(() -> say("Shizuku não está rodando ou sem permissão. Volte à tela inicial do app e toque em \"Permitir Shizuku\"."));
                return;
            }
            String r = remove ? Debloat.remove(pkg) : Debloat.restore(pkg);
            ui.post(() -> say(r));
            refreshStates();
        });
    }

    private void refreshStates() {
        bg.execute(() -> {
            if (!Shell.ready()) {
                ui.post(() -> say("Sem Shizuku: não dá para ler o estado dos pacotes."));
                return;
            }
            Map<String, Debloat.State> m = Debloat.states();
            ui.post(() -> {
                for (Map.Entry<String, Debloat.State> e : m.entrySet()) {
                    TextView tv = stateViews.get(e.getKey());
                    if (tv == null) continue;
                    String s;
                    switch (e.getValue()) {
                        case INSTALLED: s = "instalado"; break;
                        case DISABLED: s = "desativado"; break;
                        case REMOVED: s = "removido (restaurável)"; break;
                        default: s = "não existe neste aparelho";
                    }
                    tv.setText("estado: " + s);
                }
            });
        });
    }

    private void say(String s) {
        log.setText(s);
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        return t;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private Button button(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
