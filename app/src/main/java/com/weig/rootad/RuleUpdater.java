package com.weig.rootad;

import android.content.Context;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class RuleUpdater {
    record Result(long version, int strict, int balanced, int reward, String statusJson) {}

    private static final Set<String> ALLOWED = Set.of(
            "manifest.json", "packs.json", "strict.domains", "balanced.domains", "reward.domains",
            "reward-tencent.domains", "reward-wechat.domains", "reward-short-video.domains",
            "reward-other.domains");

    private RuleUpdater() {}

    static Result installLatest(Context context) throws Exception {
        ReleaseClient.Release release = ReleaseClient.latestWithAsset(
                BuildConfig.RULES_REPOSITORY, "rootad-rules", ".zip");
        ReleaseClient.Asset asset = release.endingWith(".zip");
        if (asset == null) throw new IllegalStateException("Rules release has no ZIP asset");
        File archive = ReleaseClient.download(context, asset, 40L * 1024 * 1024);
        File extracted = new File(context.getCacheDir(), "rules-staging");
        deleteTree(extracted);
        if (!extracted.mkdir()) throw new IllegalStateException("Cannot create rule staging directory");
        extractDataOnly(archive, extracted);

        File manifestFile = new File(extracted, "manifest.json");
        JSONObject manifest = new JSONObject(new String(
                Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8));
        long version = manifest.getLong("version");
        if (version < 1) throw new SecurityException("Invalid rule version");
        Validated strictFile = validateFile(extracted, "strict.domains",
                manifest.getJSONObject("profiles").getJSONObject("strict"));
        Validated balancedFile = validateFile(extracted, "balanced.domains",
                manifest.getJSONObject("profiles").getJSONObject("balanced"));
        Validated rewardFile = validateFile(extracted, "reward.domains", manifest.getJSONObject("reward"));
        if (!strictFile.domains().containsAll(balancedFile.domains()))
            throw new SecurityException("Balanced is not a subset of strict");
        if (!disjoint(strictFile.domains(), rewardFile.domains()) ||
                !disjoint(balancedFile.domains(), rewardFile.domains()))
            throw new SecurityException("Reward rules overlap a base profile");

        Set<String> packUnion = new HashSet<>();
        JSONArray packs = manifest.getJSONArray("packs");
        for (int index = 0; index < packs.length(); index++) {
            JSONObject pack = packs.getJSONObject(index);
            String name = pack.getString("file");
            if (!ALLOWED.contains(name) || !name.startsWith("reward-"))
                throw new SecurityException("Unknown rule pack file");
            Validated validated = validateFile(extracted, name, pack);
            for (String domain : validated.domains()) {
                if (!packUnion.add(domain)) throw new SecurityException("Reward packs overlap");
            }
        }
        if (!packUnion.equals(rewardFile.domains()))
            throw new SecurityException("Reward pack union mismatch");

        String remote = "/data/local/tmp/weig_rootad-rules-" + version;
        RootShell.Result stage = RootShell.run("rm -rf " + RootShell.quote(remote) +
                " && mkdir -p " + RootShell.quote(remote) + " && chmod 0700 " + RootShell.quote(remote));
        if (!stage.ok()) throw new IllegalStateException(stage.output());
        for (String name : ALLOWED) {
            File local = new File(extracted, name);
            String command = "cp " + RootShell.quote(local.getAbsolutePath()) + " " +
                    RootShell.quote(remote + "/" + name) + " && chmod 0600 " + RootShell.quote(remote + "/" + name);
            RootShell.Result copied = RootShell.run(command);
            if (!copied.ok()) throw new IllegalStateException("Cannot stage " + name + ": " + copied.output());
        }
        RootShell.Result activated = RootShell.runControl("rules-install-dir " + RootShell.quote(remote));
        if (!activated.ok()) throw new IllegalStateException(activated.output());
        deleteTree(extracted);
        archive.delete();
        return new Result(version, strictFile.count(), balancedFile.count(), rewardFile.count(), activated.output());
    }

    private record Validated(int count, Set<String> domains) {}

    private static Validated validateFile(File directory, String name, JSONObject metadata) throws Exception {
        File file = new File(directory, name);
        String expected = metadata.getString("domains_sha256").toLowerCase(Locale.ROOT);
        String actual = hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())));
        if (!expected.equals(actual)) throw new SecurityException(name + " checksum mismatch");
        Set<String> domains = new HashSet<>();
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) continue;
            if (value.length() > 253 || !value.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+"))
                throw new SecurityException("Invalid exact domain in " + name);
            if (!domains.add(value)) throw new SecurityException("Duplicate domain in " + name);
            if (domains.size() > 500_000) throw new SecurityException("Too many rules");
        }
        if (domains.size() != metadata.getInt("rules")) throw new SecurityException(name + " count mismatch");
        return new Validated(domains.size(), domains);
    }

    private static boolean disjoint(Set<String> first, Set<String> second) {
        for (String domain : second) if (first.contains(domain)) return false;
        return true;
    }

    private static void extractDataOnly(File archive, File directory) throws Exception {
        Set<String> seen = new HashSet<>();
        long total = 0;
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !ALLOWED.contains(name) || !seen.add(name))
                    throw new SecurityException("Unsafe rules archive entry: " + name);
                File target = new File(directory, name);
                try (FileOutputStream output = new FileOutputStream(target)) {
                    byte[] buffer = new byte[16 * 1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > 96L * 1024 * 1024) throw new SecurityException("Expanded rules are too large");
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
        if (!seen.equals(ALLOWED)) throw new SecurityException("Rules archive is incomplete");
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format(Locale.ROOT, "%02x", item));
        return value.toString();
    }

    private static void deleteTree(File file) {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
    }
}
