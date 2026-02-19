Rework button animation - it should work always the wiggle and not just if the user clicks for longer


there's a issue I don't know wcpde .hy, when the user leaves the reels the timer overlay shows the total time he spent on reels like 30sec, but then I open the app and it shows 29s total

If theres a interval timer going on and user changes the timer, keep the watched timer

Show the progress bar by parts of viewing time, meaning that the first part can be reels, then shorts if the user then saw shorts, than back reels and after shorts etc

Automatically show Rate app after X days

https://github.com/r0adkll/upload-google-play´

Brasil support

Facebook reels

Post on /r/androidapps /r/android and /r/androiddev

publicidade aqui https://www.reddit.com/r/androidapps/

indicar ad free / no track na descrição da app playstore e talvez na splash art

Bar with progress time spent on reels/shorts at the top of the screen

On the accessibility explainer dialog, put the github link on the top to avoid missclicks there

Daily notification telling you watch less 2h of reels compared to yesterday

Prompt for review after notification of success
Dialog when user opens app telling that he used less X compared to certain ime

Animated visibility on the Daily limit chip

Vibration

• Findings

- Medium: Accessibility explainer LaunchedEffect doesn’t include uiState.timerOverlayEnabled as a key, so toggling that flag (e.g., from persisted settings) won’t retrigger the effect even though the body checks it.
  Consider adding it to the keys or splitting into its own effect. mobile/src/main/java/com/scrolless/app/ui/home/HomeScreen.kt:217
- Low: A11y labels look mismatched: the Help icon uses cd_add, and the config icon uses go_to_accessibility_settings. Screen readers will announce the wrong action. mobile/src/main/java/com/scrolless/app/ui/home/
  HomeScreen.kt:792, mobile/src/main/java/com/scrolless/app/ui/home/HomeScreen.kt:1521

Questions/Assumptions

- Should the config button be described as “Configure daily limit” (or similar) rather than “Go to accessibility settings”?
- Do you want the accessibility explainer to trigger whenever timer overlay is enabled from persisted state (not just user toggles)?

If you want, I can patch the a11y labels, preview wrappers, and the LaunchedEffect keys.


Next! make sure to clean usersettings reels usage etc and use usagetracker

Upload to other play store etc 