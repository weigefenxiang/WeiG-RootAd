#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

MODULE_DIR="$TEMP_DIR/module"
mkdir -p "$MODULE_DIR/bin" "$MODULE_DIR/rules" "$TEMP_DIR/data" "$TEMP_DIR/system"
cp "$ROOT/module/bin/rulectl" "$MODULE_DIR/bin/rulectl"
cp "$ROOT/rules/generated/strict.domains" "$MODULE_DIR/rules/strict.domains"
cp "$ROOT/rules/generated/balanced.domains" "$MODULE_DIR/rules/balanced.domains"
cp "$ROOT/rules/generated/reward.domains" "$MODULE_DIR/rules/reward.domains"
cp "$ROOT/rules/generated/reward-tencent.domains" "$MODULE_DIR/rules/reward-tencent.domains"
cp "$ROOT/rules/generated/reward-wechat.domains" "$MODULE_DIR/rules/reward-wechat.domains"
cp "$ROOT/rules/generated/reward-short-video.domains" "$MODULE_DIR/rules/reward-short-video.domains"
cp "$ROOT/rules/generated/reward-other.domains" "$MODULE_DIR/rules/reward-other.domains"
cp "$ROOT/rules/generated/manifest.json" "$MODULE_DIR/rules/manifest.json"
cp "$ROOT/rules/generated/packs.json" "$MODULE_DIR/rules/packs.json"
touch "$TEMP_DIR/system/hosts"

export ADSHIELD_TEST_MODE=1
export ADSHIELD_TEST_DATA_DIR="$TEMP_DIR/data"
export ADSHIELD_TEST_HOSTS_TARGET="$TEMP_DIR/system/hosts"

RULECTL="$MODULE_DIR/bin/rulectl"

# Simulate an older persisted profile that still contained a reward endpoint.
# The runtime must always remove it from the base before opt-in packs are added.
echo "wxsnsad.tc.qq.com" >> "$MODULE_DIR/rules/strict.domains"

bash "$RULECTL" boot
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"protection_enabled":true'
echo "$status" | grep -q '"pending_reboot":false'
echo "$status" | grep -q '"healthy":true'
echo "$status" | grep -q '"profile":"strict"'
echo "$status" | grep -q '"compiled_rules":17041'
echo "$status" | grep -q '"running_rules":17041'
echo "$status" | grep -q '"reward_block_rules":0'

bash "$RULECTL" domain-disable 0.r.msn.com >/dev/null
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"running_rules":17040'
echo "$status" | grep -q '"disabled_rules":1'

bash "$RULECTL" domain-enable 0.r.msn.com >/dev/null
bash "$RULECTL" protection-off >/dev/null
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"protection_enabled":false'
echo "$status" | grep -q '"running_rules":0'

bash "$RULECTL" protection-on >/dev/null
bash "$RULECTL" profile balanced >/dev/null
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"profile":"balanced"'
echo "$status" | grep -q '"running_rules":17041'

bash "$RULECTL" pack-enable reward.wechat >/dev/null
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"reward_block_rules":8'
echo "$status" | grep -q '"running_rules":17049'
echo "$status" | grep -q '"enabled_packs":"reward.wechat"'

bash "$RULECTL" reward 1 >/dev/null
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"reward_temporarily_allowed":true'
echo "$status" | grep -q '"running_rules":17041'
bash "$RULECTL" reward-stop >/dev/null
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"reward_temporarily_allowed":false'
echo "$status" | grep -q '"running_rules":17049'

bash "$RULECTL" pack-disable reward.wechat >/dev/null

bash "$RULECTL" block-add custom-ad.example >/dev/null
bash "$RULECTL" list-block | grep -qx 'custom-ad.example'
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"running_rules":17042'

bash "$RULECTL" allow-add custom-ad.example >/dev/null
bash "$RULECTL" list-allow | grep -qx 'custom-ad.example'
status=$(bash "$RULECTL" status)
echo "$status" | grep -q '"running_rules":17041'

echo "module runtime simulation passed"
