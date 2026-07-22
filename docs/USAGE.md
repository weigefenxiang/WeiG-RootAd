# Command-line use

The Android manager calls the same stable control API. Manual examples:

```bash
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl status
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl profile strict
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl profile balanced
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl reward 10
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl reward-stop
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl pack-enable reward.tencent
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl pack-disable reward.tencent
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

User overrides are stored in `/data/adb/weig_rootad/user/` and survive core/rule updates. **Complete uninstall** in the manager removes them together with every RootAd rule and state file.

Reward packs are disabled by default and excluded from both Strict and Balanced. `reward 10` temporarily allows only the reward packs the user enabled.
