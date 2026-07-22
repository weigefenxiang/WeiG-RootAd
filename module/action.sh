#!/system/bin/sh

MODDIR=${0%/*}
STATE_FILE=/data/adb/weig_rootad/state/protection

if [ "$(cat "$STATE_FILE" 2>/dev/null)" = "0" ]; then
  "$MODDIR/bin/rulectl" protection-on
  echo "WeiG ZeroAd protection enabled."
else
  "$MODDIR/bin/rulectl" protection-off
  echo "WeiG ZeroAd protection disabled."
fi

"$MODDIR/bin/rulectl" status
