package com.vivo144.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int REQ_SHIZUKU = 144;

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView rateView, modeView, reasonView, logView;
    private EditText fpsInput;
    private Button permButton, overlayButton;

    private final Shizuku.OnRequestPermissionResultListener permListener = (code, result) -> {
        if (code != REQ_SHIZUKU) return;
        boolean ok = result == PackageManager.PERMISSION_GRANTED;
        log(ok ? "Permissão do Shizuku concedida." : "Permissão do Shizuku negada.");
        if (ok) refresh();
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        rateView = findViewById(R.id.rate);
        modeView = findViewById(R.id.mode);
        reasonView = findViewById(R.id.reason);
        logView = findViewById(R.id.log);
        fpsInput = findViewById(R.id.fps);
        permButton = findViewById(R.id.perm);
        overlayButton = findViewById(R.id.overlay);
        CheckBox reapply = findViewById(R.id.reapply);

        fpsInput.setText(String.valueOf(Prefs.fps(this)));
        ((TextView) findViewById(R.id.modes)).setText("Taxas do painel: " + Rms.supportedRates(this) + " Hz");
        reapply.setChecked(Prefs.reapply(this));
        reapply.setOnCheckedChangeListener((b, v) -> Prefs.reapply(this, v));

        findViewById(R.id.on).setOnClickListener(v -> run("on"));
        findViewById(R.id.off).setOnClickListener(v -> run("off"));
        findViewById(R.id.refresh).setOnClickListener(v -> refresh());
        permButton.setOnClickListener(v -> requestShizuku());
        overlayButton.setOnClickListener(v -> toggleOverlay());
        findViewById(R.id.debloat).setOnClickListener(v ->
            startActivity(new Intent(this, DebloatActivity.class)));

        Shizuku.addRequestPermissionResultListener(permListener);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlayButton.setText(OverlayService.running
            ? "Monitor flutuante de Hz: desligar" : "Monitor flutuante de Hz: ligar");
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener);
        bg.shutdown();
        super.onDestroy();
    }

    private void toggleOverlay() {
        if (OverlayService.running) {
            OverlayService.stop(this);
            overlayButton.setText("Monitor flutuante de Hz: ligar");
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            log("Permita \"Exibir sobre outros apps\" e toque de novo.");
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())));
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        OverlayService.start(this);
        overlayButton.setText("Monitor flutuante de Hz: desligar");
    }

    private void requestShizuku() {
        if (!Shizuku.pingBinder()) {
            log("Shizuku não está rodando. Abra o app Shizuku e toque em Iniciar (depuração sem fio).");
            return;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            log("Permissão do Shizuku já concedida.");
            permButton.setVisibility(View.GONE);
        } else {
            Shizuku.requestPermission(REQ_SHIZUKU);
        }
    }

    private int readFps() {
        try {
            int v = Integer.parseInt(fpsInput.getText().toString().trim());
            if (v >= 30 && v <= 240) {
                Prefs.fps(this, v);
                return v;
            }
        } catch (NumberFormatException ignored) {
        }
        fpsInput.setText(String.valueOf(Prefs.fps(this)));
        return Prefs.fps(this);
    }

    private void refresh() {
        run("get");
    }

    private void run(String what) {
        final int fps = readFps();
        log(what.equals("on") ? "Ligando " + fps + " Hz…" : what.equals("off") ? "Liberando…" : "Lendo taxa…");
        bg.execute(() -> {
            try {
                float rate;
                if (what.equals("on")) {
                    rate = Rms.set(fps);
                    Prefs.on(this, true);
                } else if (what.equals("off")) {
                    rate = Rms.release();
                    Prefs.on(this, false);
                } else {
                    rate = Rms.getRate();
                }
                final String reason = Rms.reason(this);
                ui.post(() -> {
                    rateView.setText("rate=" + Rms.fmt(rate));
                    modeView.setText("Modo: " + Rms.mode);
                    reasonView.setText("reason: " + reason);
                    permButton.setVisibility(View.GONE);
                    String msg = "OK: rate=" + Rms.fmt(rate);
                    if (what.equals("on") && Math.round(rate) < fps) {
                        msg += "\nA taxa ainda não chegou em " + fps + " Hz. Veja o reason: se for reqFps:…, "
                            + "o app em primeiro plano está forçando a taxa dele.";
                    }
                    log(msg);
                });
                RefreshTile.update(this);
            } catch (Rms.NeedPermission e) {
                ui.post(() -> {
                    permButton.setVisibility(View.VISIBLE);
                    log(e.getMessage() + ". Toque em \"Permitir Shizuku\".");
                });
            } catch (Throwable e) {
                ui.post(() -> {
                    if (e instanceof Rms.NeedShizuku) permButton.setVisibility(View.VISIBLE);
                    log("Erro: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                });
            }
        });
    }

    private void log(String s) {
        logView.setText(s + "\n" + logView.getText());
    }
}
