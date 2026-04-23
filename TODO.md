# TODO

## Current checkpoint
- [x] Recovered and rebuilt battery overlay/card functionality after lost progress
- [x] Added battery debug override controls in settings for live-state testing
- [x] Restored dense rotating battery insights with live/runtime/session fallbacks
- [x] Moved battery color treatment to stroke-only on the live pill and expanded shell
- [x] Switched battery discharge color from stepped/gradient fill to smooth hue shift
- [x] Restored cycle footer visibility and live projected remaining/runtime behavior

## Battery work finished
- [x] Live battery pill uses stroke-only color instead of filled battery states
- [x] Expanded battery card uses the same battery stroke color treatment
- [x] Battery insight icon/glyph set now responds to the metric being shown
- [x] Battery drawer supports rotating insight cards plus debug presets/manual override controls
- [x] Battery snapshot polling/projection keeps unplugged values moving between sparse system updates

## Next battery fixes
- [ ] Fix timer behavior end-to-end; current timer feature is still bugged
- [ ] Restore timer progress stroke after the rounded-outline refactor
- [ ] Remove the remaining dead space under the battery cycle footer
- [ ] Tighten the expanded battery shell/footer layout after the final spacing cut
- [ ] Verify live low power mode changes propagate instantly outside debug override mode
- [ ] Re-check battery insight tap swap behavior on-device after the current battery pass

## Battery follow-up ideas
- [ ] Add app/day/week battery usage insights once there is a real usage/history pipeline
- [ ] Expand insight pool with previous-day or previous-week comparisons
- [ ] Add quantified Status Quarites/self-drain insight once collection exists
- [ ] Surface top battery-using apps when usage stats/history is wired in

## Remaining overlay and weather follow-ups
- [ ] Make transfer/moving pills less horizontally long
- [ ] Fade moving pill contents near the end of expand and during exit back into the live pill
- [ ] Refine weather reconciliation so near-freezing flurries, melt, or hail do not stay stuck on snowy when the live/current hour is cloudy, breezy, clear, or windy
- [ ] Ensure long condition labels like `Overcast & Gusty` display fully, either by fitting or marquee behavior
- [ ] Wire status bar taps to the system press sound with the newer chill sound setup
- [ ] Sync sunny forecast flare/icon coloring so highlighted sun rows match the card presentation consistently

## Notes
- Timer stroke/path work was adjusted to follow rounded pill outlines, but the timer progress is currently not visible and should be treated as an active regression.
- Battery cycle footer data is present in debug mode and live mode now; the remaining issue is layout spacing, not missing cycle data.
- `version.properties` is modified from local build activity and was not part of the functional battery/timer note updates.
