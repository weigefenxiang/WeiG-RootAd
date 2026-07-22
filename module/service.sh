#!/system/bin/sh

MODDIR=${0%/*}
"$MODDIR/bin/rulectl" boot >/dev/null 2>&1

