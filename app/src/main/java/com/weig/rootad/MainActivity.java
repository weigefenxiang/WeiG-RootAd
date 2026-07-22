package com.weig.rootad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean zh;
    private int surface, card, primary, secondary, accent, accentSoft, divider;
    private TextView protectionText, countText, detailText, updateText;
    private TextView actionText, rewardCountdown;
    private ProgressBar progress, actionProgress;
    private Button protectionButton, strictButton, balancedButton, rewardButton;
    private CheckBox rewardTencent, rewardWechat, rewardShortVideo, rewardOther;
    private RootStatus latest;
    private boolean updatingUi;
    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            if (latest == null || !latest.rewardTemporarilyAllowed()) return;
            long remaining = latest.rewardExpiresAt() - System.currentTimeMillis() / 1000L;
            if (remaining <= 0) { refresh(); return; }
            rewardCountdown.setText(t("奖励广告临时放行  ", "Reward ads allowed  ") +
                    String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60));
            main.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        zh = Locale.getDefault().getLanguage().equals("zh");
        surface = getColor(R.color.surface); card = getColor(R.color.surface_card);
        primary = getColor(R.color.text_primary); secondary = getColor(R.color.text_secondary);
        accent = getColor(R.color.accent); accentSoft = getColor(R.color.accent_soft);
        divider = getColor(R.color.divider);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        setContentView(buildScreen());

        // Some vendor Android 16 builds do not create PhoneWindow's DecorView
        // until setContentView(). Querying the controller earlier crashes launch.
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(dark ? 0 : light, light);
        }
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    @Override protected void onDestroy() {
        main.removeCallbacks(countdownTick);
        worker.shutdownNow();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);
        LinearLayout body = column();
        int horizontal = dp(20), top = dp(24), bottom = dp(40);
        body.setPadding(horizontal, top, horizontal, bottom);
        scroll.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            body.setPadding(horizontal, top + bars.top, horizontal, bottom + bars.bottom);
            return windowInsets;
        });
        scroll.addView(body);

        TextView brand = text("Wei.G", 14, accent, Typeface.BOLD);
        body.addView(brand);
        TextView title = text("RootAd", 34, primary, Typeface.BOLD);
        body.addView(title, margins(-2, 2, 0, 20));

        LinearLayout hero = card();
        protectionText = text(t("正在检测 Root 核心", "Checking Root core"), 14, secondary, Typeface.BOLD);
        hero.addView(protectionText);
        countText = text("—", 42, primary, Typeface.BOLD);
        hero.addView(countText, margins(0, 8, 0, 0));
        hero.addView(text(t("条规则运行中", "rules running"), 14, secondary, Typeface.NORMAL));
        detailText = text("Android " + android.os.Build.VERSION.RELEASE + " · " + BuildConfig.VERSION_NAME,
                13, secondary, Typeface.NORMAL);
        hero.addView(detailText, margins(0, 14, 0, 0));
        actionText = text("", 13, accent, Typeface.BOLD);
        actionText.setVisibility(View.GONE);
        hero.addView(actionText, margins(0, 12, 0, 0));
        actionProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        actionProgress.setIndeterminate(true);
        actionProgress.setVisibility(View.GONE);
        hero.addView(actionProgress, margins(0, 8, 0, 0));
        protectionButton = button(t("关闭保护", "Disable protection"), true);
        protectionButton.setOnClickListener(v -> toggleProtection());
        hero.addView(protectionButton, margins(0, 18, 0, 0));
        body.addView(hero);

        body.addView(section(t("过滤模式", "Filter profile")));
        LinearLayout profiles = card();
        profiles.addView(text(t("严格与平衡都默认排除奖励广告规则；奖励拦截由下方单独控制。", "Both profiles exclude reward-ad rules by default; control reward blocking separately below."),
                14, secondary, Typeface.NORMAL));
        LinearLayout profileButtons = row();
        strictButton = actionButton(t("严格", "Strict"), v -> command("profile strict", t("正在切换到严格模式…", "Switching to Strict…")));
        balancedButton = actionButton(t("平衡", "Balanced"), v -> command("profile balanced", t("正在切换到平衡模式…", "Switching to Balanced…")));
        profileButtons.addView(strictButton, weightMargins(1, 0, 14, 6, 0));
        profileButtons.addView(balancedButton, weightMargins(1, 0, 14, 0, 0));
        profiles.addView(profileButtons);
        body.addView(profiles);

        body.addView(section(t("奖励广告拦截", "Reward-ad blocking")));
        LinearLayout rewards = card();
        rewards.addView(text(t("默认不拦截。勾选后才加入对应奖励广告规则；需要领取奖励时可临时放行。",
                "Disabled by default. Selected packs are blocked; temporarily allow them when you need a reward."),
                14, secondary, Typeface.NORMAL));
        rewardTencent = packCheckBox(t("腾讯 / QQ 奖励广告", "Tencent / QQ reward ads"), "reward.tencent");
        rewardWechat = packCheckBox(t("微信奖励广告", "WeChat reward ads"), "reward.wechat");
        rewardShortVideo = packCheckBox(t("短视频奖励广告", "Short-video reward ads"), "reward.short-video");
        rewardOther = packCheckBox(t("其他奖励广告", "Other reward ads"), "reward.other");
        rewards.addView(rewardTencent, margins(0, 12, 0, 0));
        rewards.addView(rewardWechat);
        rewards.addView(rewardShortVideo);
        rewards.addView(rewardOther);
        rewardCountdown = text("", 13, accent, Typeface.BOLD);
        rewardCountdown.setVisibility(View.GONE);
        rewards.addView(rewardCountdown, margins(0, 10, 0, 0));
        rewardButton = button(t("临时允许已选奖励广告 10 分钟", "Allow selected reward ads for 10 minutes"), false);
        rewardButton.setOnClickListener(v -> toggleRewardTimer());
        rewards.addView(rewardButton, margins(0, 10, 0, 0));
        body.addView(rewards);

        body.addView(section(t("自定义规则", "Custom rules")));
        LinearLayout custom = card();
        custom.addView(text(t("添加精确域名到拦截、放行或手动关闭列表。自定义内容不会被规则更新覆盖。",
                "Add exact domains to block, allow, or disabled lists. Updates never overwrite them."), 14, secondary, Typeface.NORMAL));
        Button edit = button(t("添加或调整域名", "Add or change a domain"), false);
        edit.setOnClickListener(v -> ruleDialog());
        custom.addView(edit, margins(0, 14, 0, 0));
        body.addView(custom);

        body.addView(section(t("更新", "Updates")));
        LinearLayout updates = card();
        updateText = text(t("规则、管理器和核心模块分别更新。", "Rules, manager, and core update independently."),
                14, secondary, Typeface.NORMAL);
        updates.addView(updateText);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        updates.addView(progress, margins(0, 12, 0, 4));
        Button rules = button(t("一键更新规则", "Update rules"), true);
        rules.setOnClickListener(v -> updateRules());
        updates.addView(rules, margins(0, 10, 0, 0));
        LinearLayout updateButtons = row();
        updateButtons.addView(actionButton(t("更新管理器", "Update app"), v -> updateApp()), weightMargins(1, 0, 10, 6, 0));
        updateButtons.addView(actionButton(t("更新核心", "Update core"), v -> updateCore()), weightMargins(1, 0, 10, 0, 0));
        updates.addView(updateButtons);
        Button rollback = button(t("回滚上一版规则", "Roll back rules"), false);
        rollback.setOnClickListener(v -> command("rules-rollback"));
        updates.addView(rollback, margins(0, 10, 0, 0));
        body.addView(updates);

        body.addView(section(t("支持与卸载", "Support & removal")));
        LinearLayout support = card();
        Button issue = button(t("一键提交 GitHub Issue", "Create GitHub issue"), false);
        issue.setOnClickListener(v -> openIssue());
        support.addView(issue);
        Button uninstall = button(t("完整卸载", "Complete uninstall"), false);
        uninstall.setTextColor(Color.rgb(210, 55, 60));
        uninstall.setOnClickListener(v -> confirmUninstall());
        support.addView(uninstall, margins(0, 10, 0, 0));
        body.addView(support);

        body.addView(text("Wei.G RootAd  " + BuildConfig.VERSION_NAME + "  ·  Android 12–16",
                12, secondary, Typeface.NORMAL), margins(0, 22, 0, 0));
        return scroll;
    }

    private void refresh() {
        busy(true, t("读取核心状态…", "Reading core status…"));
        worker.execute(() -> {
            RootStatus status = RootStatus.read();
            main.post(() -> showStatus(status));
        });
    }

    private void showStatus(RootStatus status) {
        latest = status;
        busy(false, null);
        updateText.setText(t("规则与管理器更新无需重启；只有核心安装或更新后需要重启。",
                "Rule and app updates need no reboot; only core installation or updates do."));
        main.removeCallbacks(countdownTick);
        if (status.requiresReboot()) {
            protectionText.setText(t("核心已安装，等待重启", "Core installed; reboot required"));
            countText.setText("0");
            detailText.setText(t("重启后广告过滤才会生效", "Ad filtering starts after reboot"));
            protectionButton.setText(t("立即重启", "Reboot now"));
            protectionButton.setEnabled(true);
            setRuleControlsEnabled(false);
            return;
        }
        if (!status.installed()) {
            protectionText.setText(t("尚未安装 Root 核心", "Root core is not installed"));
            countText.setText("0");
            detailText.setText(t("请安装一体包或 core-only 模块；若刚安装，请重启设备。",
                    "Install the all-in-one or core-only module. Reboot if it was just installed."));
            protectionButton.setEnabled(false);
            setRuleControlsEnabled(false);
            return;
        }
        setRuleControlsEnabled(true);
        protectionButton.setEnabled(true);
        protectionText.setText(status.protection() ? t("保护运行中", "Protection active") : t("保护已关闭", "Protection disabled"));
        countText.setText(String.format(Locale.US, "%,d", status.running()));
        String mode = status.profile().equals("balanced") ? t("平衡", "Balanced") : t("严格", "Strict");
        detailText.setText(mode + " · " + status.root() + " · rules " + status.ruleVersion() +
                "\n" + t("关闭 ", "disabled ") + status.disabled() + " · " +
                t("自定义拦截 ", "custom block ") + status.customBlock() + " · " +
                t("自定义放行 ", "custom allow ") + status.customAllow() +
                "\n" + t("奖励广告拦截 ", "reward blocks ") + status.rewardBlock());
        protectionButton.setText(status.protection() ? t("关闭保护", "Disable protection") : t("开启保护", "Enable protection"));
        styleChoice(strictButton, status.profile().equals("strict"));
        styleChoice(balancedButton, status.profile().equals("balanced"));
        updatingUi = true;
        rewardTencent.setChecked(status.packEnabled("reward.tencent"));
        rewardWechat.setChecked(status.packEnabled("reward.wechat"));
        rewardShortVideo.setChecked(status.packEnabled("reward.short-video"));
        rewardOther.setChecked(status.packEnabled("reward.other"));
        updatingUi = false;
        if (status.rewardTemporarilyAllowed()) {
            rewardCountdown.setVisibility(View.VISIBLE);
            rewardButton.setText(t("立即结束临时放行", "End temporary allowance"));
            main.post(countdownTick);
        } else {
            rewardCountdown.setVisibility(View.GONE);
            rewardButton.setText(t("临时允许已选奖励广告 10 分钟", "Allow selected reward ads for 10 minutes"));
        }
        rewardButton.setEnabled(status.rewardBlock() > 0 || status.rewardTemporarilyAllowed());
    }

    private void toggleProtection() {
        if (latest != null && latest.requiresReboot()) { confirmReboot(); return; }
        boolean disable = latest != null && latest.protection();
        command(disable ? "protection-off" : "protection-on",
                disable ? t("正在关闭保护…", "Disabling protection…") : t("正在开启保护…", "Enabling protection…"));
    }

    private void command(String command) { command(command, t("正在应用…", "Applying…")); }

    private void command(String command, String message) {
        busy(true, message);
        if (command.equals("profile strict")) {
            styleChoice(strictButton, true);
            styleChoice(balancedButton, false);
        } else if (command.equals("profile balanced")) {
            styleChoice(strictButton, false);
            styleChoice(balancedButton, true);
        } else if (command.equals("protection-on") || command.equals("protection-off")) {
            protectionText.setText(message);
            protectionButton.setText(t("处理中…", "Working…"));
        }
        protectionButton.setEnabled(false);
        setRuleControlsEnabled(false);
        worker.execute(() -> {
            RootShell.Result result = RootShell.runControl(command);
            RootStatus status = result.ok() ? RootStatus.fromResult(result) : RootStatus.read();
            main.post(() -> {
                if (!result.ok()) toast(result.output());
                showStatus(status);
            });
        });
    }

    private void toggleRewardTimer() {
        boolean stop = latest != null && latest.rewardTemporarilyAllowed();
        command(stop ? "reward-stop" : "reward 10",
                stop ? t("正在恢复奖励广告拦截…", "Restoring reward blocking…") :
                        t("正在临时放行奖励广告…", "Temporarily allowing reward ads…"));
    }

    private void setPack(String id, boolean enabled) {
        command((enabled ? "pack-enable " : "pack-disable ") + id,
                enabled ? t("正在加入奖励广告规则…", "Adding reward-ad rules…") :
                        t("正在移除奖励广告规则…", "Removing reward-ad rules…"));
    }

    private void confirmReboot() {
        new AlertDialog.Builder(this).setTitle(t("立即重启", "Reboot now"))
                .setMessage(t("设备将立即重启，使 Root 核心生效。", "The device will reboot now to activate the Root core."))
                .setPositiveButton(t("重启", "Reboot"), (dialog, which) ->
                        worker.execute(() -> RootShell.run("reboot")))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void ruleDialog() {
        EditText input = new EditText(this);
        input.setHint("ads.example.com");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        int padding = dp(22); input.setPadding(padding, dp(8), padding, 0);
        String[] choices = {t("加入拦截", "Add to block"), t("加入放行", "Add to allow"),
                t("关闭现有规则", "Disable existing rule"), t("恢复现有规则", "Enable existing rule")};
        new AlertDialog.Builder(this).setTitle(t("自定义域名", "Custom domain")).setView(input)
                .setItems(choices, (dialog, which) -> {
                    String domain = input.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (domain.isEmpty()) { toast(t("请输入域名", "Enter a domain")); return; }
                    String operation = switch (which) { case 0 -> "block-add"; case 1 -> "allow-add";
                        case 2 -> "domain-disable"; default -> "domain-enable"; };
                    command(operation + " " + RootShell.quote(domain));
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void updateRules() {
        busy(true, t("正在检查并验证规则…", "Checking and verifying rules…"));
        worker.execute(() -> {
            try {
                RuleUpdater.Result result = RuleUpdater.installLatest(this);
                main.post(() -> { toast(t("规则已更新：", "Rules updated: ") + result.version()); refresh(); });
            } catch (Exception error) { main.post(() -> failed(error)); }
        });
    }

    private void updateApp() {
        busy(true, t("正在检查管理器更新…", "Checking manager update…"));
        worker.execute(() -> {
            try {
                ReleaseClient.Release release = ReleaseClient.latestWithAsset(
                        BuildConfig.CODE_REPOSITORY, "rootad-manager", ".apk");
                ReleaseClient.Asset apk = release.endingWith(".apk");
                if (apk == null) throw new IllegalStateException("Release has no APK asset");
                File file = ReleaseClient.download(this, apk, 80L * 1024 * 1024);
                main.post(() -> {
                    busy(false, null);
                    try { ApkInstaller.install(this, file, (message, ok) -> main.post(() -> toast(message))); }
                    catch (Exception error) { failed(error); }
                });
            } catch (Exception error) { main.post(() -> failed(error)); }
        });
    }

    private void updateCore() {
        busy(true, t("正在下载并验证核心…", "Downloading and verifying core…"));
        worker.execute(() -> {
            try {
                ReleaseClient.Release release = ReleaseClient.latestWithAsset(
                        BuildConfig.CODE_REPOSITORY, "core-only", ".zip");
                ReleaseClient.Asset zip = null;
                for (ReleaseClient.Asset candidate : release.assets()) {
                    String name = candidate.name().toLowerCase(Locale.ROOT);
                    if (name.contains("core-only") && name.endsWith(".zip")) { zip = candidate; break; }
                }
                if (zip == null) throw new IllegalStateException("Release has no core ZIP asset");
                File file = ReleaseClient.download(this, zip, 50L * 1024 * 1024);
                String path = file.getAbsolutePath();
                String command = "if command -v magisk >/dev/null 2>&1; then magisk --install-module " + RootShell.quote(path) +
                        "; elif command -v ksud >/dev/null 2>&1; then ksud module install " + RootShell.quote(path) +
                        "; elif command -v apd >/dev/null 2>&1; then apd module install " + RootShell.quote(path) +
                        "; else echo 'No supported module installer found'; exit 1; fi";
                RootShell.Result result = RootShell.run(command);
                if (!result.ok()) throw new IllegalStateException(result.output());
                main.post(() -> { toast(t("核心已更新，重启后生效", "Core updated; reboot to apply")); refresh(); });
            } catch (Exception error) { main.post(() -> failed(error)); }
        });
    }

    private void openIssue() {
        RootStatus status = latest;
        String title = "[Rule] ";
        String body = "## Description / 问题描述\n\n\n## Safe diagnostics\n" +
                "- App: " + BuildConfig.VERSION_NAME + "\n- Android: " + android.os.Build.VERSION.RELEASE +
                "\n- Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
                (status == null ? "" : "\n- Root: " + status.root() + "\n- Rule version: " + status.ruleVersion() +
                        "\n- Profile: " + status.profile() + "\n- Running rules: " + status.running()) +
                "\n\nDo not attach account tokens, cookies, or full HTTPS payloads.";
        String url = "https://github.com/" + BuildConfig.GITHUB_OWNER + "/" + BuildConfig.CODE_REPOSITORY +
                "/issues/new?title=" + encode(title) + "&body=" + encode(body);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void confirmUninstall() {
        new AlertDialog.Builder(this).setTitle(t("完整卸载", "Complete uninstall"))
                .setMessage(t("将关闭 hosts 挂载，并删除核心模块、官方规则、自定义规则和全部 RootAd 状态。APK 随后交给系统卸载。",
                        "This removes the hosts mount, core module, official and custom rules, and all RootAd state. Android will then remove the app."))
                .setPositiveButton(t("全部删除", "Remove everything"), (d, w) -> fullUninstall())
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void fullUninstall() {
        busy(true, t("正在清理规则…", "Removing rules…"));
        worker.execute(() -> {
            RootShell.Result result = RootShell.runControl("cleanup-mount");
            RootShell.Result remove = RootShell.run(
                    "rm -rf /data/adb/weig_rootad /data/adb/modules/weig_rootad " +
                    "/data/adb/modules_update/weig_rootad");
            main.post(() -> {
                busy(false, null);
                if (!remove.ok()) { toast(result.output() + " " + remove.output()); return; }
                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName())));
            });
        });
    }

    private void failed(Exception error) { busy(false, null); toast(error.getMessage() == null ? error.toString() : error.getMessage()); }
    private void busy(boolean value, String message) {
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        actionProgress.setVisibility(value ? View.VISIBLE : View.GONE);
        actionText.setVisibility(value && message != null ? View.VISIBLE : View.GONE);
        if (message != null) actionText.setText(message);
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private String t(String chinese, String english) { return zh ? chinese : english; }
    private String encode(String value) {
        try { return URLEncoder.encode(value, StandardCharsets.UTF_8.name()); }
        catch (Exception impossible) { return value; }
    }

    private LinearLayout column() { LinearLayout value = new LinearLayout(this); value.setOrientation(LinearLayout.VERTICAL); return value; }
    private LinearLayout row() { LinearLayout value = column(); value.setOrientation(LinearLayout.HORIZONTAL); return value; }
    private LinearLayout card() { LinearLayout value = column(); value.setPadding(dp(18), dp(18), dp(18), dp(18)); value.setBackground(round(card, 20, divider)); return value; }
    private TextView section(String value) { TextView text = text(value, 16, primary, Typeface.BOLD); text.setPadding(0, dp(26), 0, dp(10)); return text; }
    private TextView text(String value, int size, int color, int style) { TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setTypeface(Typeface.create("sans", style)); text.setLineSpacing(0, 1.12f); return text; }
    private Button actionButton(String value, View.OnClickListener listener) { Button button = button(value, false); button.setOnClickListener(listener); return button; }
    private CheckBox packCheckBox(String label, String id) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setTextSize(14);
        checkBox.setTextColor(primary);
        checkBox.setMinHeight(dp(44));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            if (!updatingUi) setPack(id, checked);
        });
        return checkBox;
    }
    private void setRuleControlsEnabled(boolean enabled) {
        if (strictButton == null) return;
        strictButton.setEnabled(enabled);
        balancedButton.setEnabled(enabled);
        rewardTencent.setEnabled(enabled);
        rewardWechat.setEnabled(enabled);
        rewardShortVideo.setEnabled(enabled);
        rewardOther.setEnabled(enabled);
        rewardButton.setEnabled(enabled);
    }
    private void styleChoice(Button button, boolean selected) {
        button.setTextColor(selected ? Color.WHITE : accent);
        button.setBackground(round(selected ? accent : accentSoft, 14, selected ? accent : divider));
    }
    private Button button(String value, boolean filled) { Button button = new Button(this); button.setText(value); button.setTextSize(14); button.setAllCaps(false); button.setTypeface(Typeface.DEFAULT, Typeface.BOLD); button.setTextColor(filled ? Color.WHITE : accent); button.setGravity(Gravity.CENTER); button.setMinHeight(dp(48)); button.setBackground(round(filled ? accent : accentSoft, 14, filled ? accent : divider)); return button; }
    private GradientDrawable round(int fill, int radius, int stroke) { GradientDrawable value = new GradientDrawable(); value.setColor(fill); value.setCornerRadius(dp(radius)); value.setStroke(dp(1), stroke); return value; }
    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) { LinearLayout.LayoutParams value = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); value.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return value; }
    private LinearLayout.LayoutParams weightMargins(float weight, int left, int top, int right, int bottom) { LinearLayout.LayoutParams value = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight); value.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return value; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
