Reduced UIAutomator captures from TikTok 46.8.3. Only the player and button/editor
properties relevant to this rule are kept. Personal labels are redacted; the DM
reply label uses the literal synthetic value [recipient]. No names, device
identifiers or capture dates are retained.

Tests replay geometry and control roles using synthetic labels, including
translated templates, scaling, mirrored geometry and an editable comment
composer. These are unit fixtures, not end-to-end scanner or device tests.
The feed sharing-shortcut capture had identical reduced properties to feed.xml;
only that fixture is retained. There is no verified open share-sheet fixture.

TikTokDmReplyLabels contains 87 distinct templates from the packaged NxString
resources in TikTok 46.7.3 and 46.8.3. Both versions have the same templates.
58 language packs define these entries; other packs omit them and use fallbacks.

ReplyLabels and DmExemptionRule.replyLabelsBelowPlayer are shared APIs. Another
app can supply its own translated templates with one {recipient} placeholder.
The scanner requires a visible, enabled, clickable, non-editable Android Button
with matching text, content description or hint directly below the detected
player. This rule requires a content cover and combines with any configured ID
rules. Apps with different reply layouts need a separately validated rule.

Unknown labels remain blocked. Wording changes, downloaded language overrides,
or a redesigned viewer may still need updates. These fixtures do not establish
support on every device. The existing player_view detection remains unchanged.
