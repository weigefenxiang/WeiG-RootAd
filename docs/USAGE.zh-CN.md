# 命令行使用

Android 管理器调用同一套稳定控制接口，也可手动执行：

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

自定义精确域名命令包括 `block-add/remove`、`allow-add/remove`、`domain-disable/enable`。

用户规则保存在 `/data/adb/weig_rootad/user/`，更新核心或官方规则不会覆盖。管理器中的“完整卸载”会同时删除用户规则、官方规则、模块与全部状态。

奖励广告包默认关闭，并从严格和平衡模式中排除。`reward 10` 只会临时放行用户已经勾选的奖励包。
