#!/system/bin/sh

MODDIR=${0%/*}
"$MODDIR/bin/rulectl" cleanup-mount >/dev/null 2>&1

# A normal removal is complete by default: no active rules or ZeroAd state remain.
# The manager can create this marker before uninstalling when the user explicitly
# chooses to retain custom allow/block entries for a later reinstall.
DATA_DIR=/data/adb/weig_rootad
KEEP_DIR=/data/adb/weig_rootad-user-backup
if [ -f "$DATA_DIR/state/keep_user_rules" ]; then
  rm -rf "$KEEP_DIR"
  mkdir -p "$KEEP_DIR"
  cp -af "$DATA_DIR/user/." "$KEEP_DIR/" 2>/dev/null
  chmod 0700 "$KEEP_DIR" 2>/dev/null
else
  rm -rf "$KEEP_DIR"
fi
rm -rf "$DATA_DIR"
