# Profiles and optional packs

- `strict`: the Wei.G baseline after every known reward-ad endpoint is removed.
- `balanced`: strict minus maintained compatibility exceptions that overlap the baseline.
- `reward`: not a profile. It is an optional blocking-pack union, disabled by default.

Selected reward packs are added at runtime. The ten-minute action temporarily removes only those selected packs. User allow/block/disabled lists remain the highest-priority runtime overrides.

Generated files are written to `rules/generated/`. Do not edit generated files manually.
