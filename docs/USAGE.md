# Command-line use

The manager calls the same stable Root API. Manual examples:

```bash
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl status
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl cn-profile lean
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl cn-profile balanced
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl cn-profile strict
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl global-profile off
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl global-profile lean
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl reward 10
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl reward-stop
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl pack-disable reward.tencent
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl pack-enable reward.tencent
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl protection-off
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl protection-on
```

Custom exact domains:

```bash
rulectl block-add ads.example.com
rulectl block-remove ads.example.com
rulectl allow-add safe.example.com
rulectl allow-remove safe.example.com
rulectl domain-disable sdk.example.com
rulectl domain-enable sdk.example.com
```

User overrides live in `/data/adb/weig_rootad/user/` and survive core/rule updates. Complete uninstall removes them together with all ZeroAd rules and state.

The default is Domestic Lean, Global Off, and all four reward packs enabled. Reward domains are excluded from every normal profile; `reward 10` temporarily allows only the currently enabled reward packs.
