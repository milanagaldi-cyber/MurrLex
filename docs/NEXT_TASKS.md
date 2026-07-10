# Next Tasks For Android_Main

Start with stabilization around the working Translate/Split behavior, not new features.

## Recommended First Task

Verify a clean Android install from the current debug APK:

1. Uninstall existing app or clear app data.
2. Install `mobile/android/app/build/outputs/apk/debug/app-debug.apk`.
3. Confirm the app name is MurrLex.
4. Confirm Settings shows version `0.88`.
5. Confirm bundled lessons show readable text.
6. Confirm opening a lesson displays cards.
7. Confirm Translate works online through the free Google Translate path.
8. Confirm offline speech recognition still captures speech after disabling network.
9. Confirm offline ML Kit translation returns translated text after speech recognition and after manual text input.
10. Confirm Split mode has microphone buttons on both sides and the translated-side microphone listens in the target language.
11. Confirm Basic/Native and Target/Learning language labels are clear in Settings, onboarding, quick vocabulary, and Translate.
12. Confirm a quick voice card stores Basic text on the front/native side and Target text on the checked/correct side.
13. Change the language pair in Quick Dictionary or Translate and confirm Settings does not change.
14. Confirm the changed active pair is used for recognition, translation, and new card/lesson creation.
15. Confirm Split and normal Translate keep the Basic input panel at the bottom.
16. Confirm onboarding offers local language downloads or Later after default languages are chosen.
17. Confirm Settings Local languages lists downloaded translation/speech status and can remove a downloaded ML Kit translation model.
18. Confirm Belarusian OpenAI speech recognition sends `language=be` and does not fall back to Russian.
19. If ElevenLabs returns 403 for Belarusian TTS, confirm the app shows/logs the safe HTTP diagnostic without exposing the API key.
20. Confirm the startup purr is audible and does not stall the splash screen.
21. Confirm Settings accepts a manually typed ElevenLabs voice ID for Belarusian TTS.
22. Confirm card auto-translation and the `T` button use OpenAI when OpenAI translation is enabled.
23. Confirm ElevenLabs is not used for text translation and remains only Belarusian TTS/audio.
24. Confirm automatic card translation uses OpenAI when OpenAI is enabled, keyed, and online, even if critical offline mode is toggled on.
25. Confirm Polish to Belarusian automatic card translation fills the target side without pressing `T`.
26. Confirm Settings can set OpenAI voice silence timeout to `1 second` and recognition stops sooner after silence.
27. Confirm OpenAI voice silence timeout offers 1 through 10 seconds.
28. Confirm the online indicator switches green quickly after network returns.
29. Confirm current audio stops on app tap, navigation away from Study, or another playback request.
30. Confirm a Study card plays cached TTS audio while the phone is offline.
31. Confirm the Study card status dot blinks every five seconds and shows green online, red offline, and yellow when the visible side audio is cached while online.
32. Confirm Latvian, Lithuanian, and Portuguese are available in onboarding, Settings language choices, Quick Dictionary, and Translate.
33. Confirm Settings Special ElevenLabs TTS language chips can add/remove languages and enabled languages use ElevenLabs for generated speech.
34. Confirm starting a new audio playback stops the previous audio immediately.
35. Confirm card status blink interval supports Off, 0.5, 1, 2, 3, 4, and 5 seconds, with 2 seconds as the default.
36. Confirm cached audio draws a thin black outline instead of a yellow dot, and offline dots do not blink.
37. Confirm card info logs include recognition, translation, and speech model/voice details.
38. Confirm OpenAI card translation writes a short explanation, rule, and examples into the card hint.
39. Confirm the copy icon both fills the answer input and copies the value to the system clipboard.
40. Confirm answer input language labels match the current side, including Polish/Lithuanian pairs.
41. Confirm tapping `MurrLex` and the Study card language/status area immediately refreshes online/offline status.
42. Confirm same-language microphone Quick Vocabulary input creates a Mistake card with OpenAI correction, explanation, rules, and examples.
43. Confirm long-press on a Mistake card opens ten Train-card options, creates `TR` cards, and Train cards do not open the generator.
44. Confirm sharing a text post from Telegram/Instagram/YouTube/Viber into MurrLex opens the import dialog and creates retelling cards from up to 500 words.
45. Confirm the URL icon left of `MurrLex` opens a link popup and creates a lesson from a pasted Telegram/web URL when page text is readable.
46. Confirm `https://t.me/headlines_for_traders/80333` imports the Russian post text, not Telegram navigation or `<script data-telegram-post...>` content.
47. Confirm a long shared post sends up to 500 words to OpenAI and returns one Basic -> Target retelling card per meaningful sentence.
48. Confirm an ordinary article URL, such as `https://iz.ru/2123208/2026-06-28/politico-soobshchila-o-rastushchei-izoliatcii-izrailia-na-mirovoi-arene`, extracts readable article text and returns Basic -> Target thesis cards from up to 2000 words.
18. Confirm OpenAI mode uses separate STT/text/TTS models, requires API key, and does not show Google attribution for OpenAI translations.
19. Confirm OpenAI translation/TTS cache hits avoid duplicate API calls and Clear app cache removes cached entries.
20. Confirm OpenAI cache hits ignore case and punctuation differences in the input.
21. Confirm OpenAI STT waveform moves only with voice signal, goes flat during silence, and stops recording after the configured silence timeout.
22. Confirm OpenAI STT does not cache recognition requests.
23. Confirm OpenAI translation/TTS shows cache/API status and the Settings log keeps only the last two days.
24. Confirm card content remains cached across normal OpenAI TTL expiry and is removed only by card/lesson deletion or explicit app cache clearing.
25. Confirm startup plays one soft purr and tap/swipe/success actions stay silent.
26. Confirm Belarusian TTS uses ElevenLabs only when BY provider is ElevenLabs, the phone is online, and the ElevenLabs key is present.
27. Confirm the card download+music-note action shares or saves the mp3 for the currently visible card side.

## Then Fix Safely

1. Centralize localization/settings text.
2. Remove or replace remaining corrupted legacy localized string literals.
3. Split `MainActivity.kt` in small verified chunks.
4. Split `MainViewModel.kt` only after UI extraction is stable.
5. Add a tiny smoke checklist for manual Android testing, including online/offline translator checks.
6. Continue performance work by moving heavy list filtering/sorting out of Composables before doing cosmetic file splitting.

## Defer

- New features.
- Backend integration into mobile.
- Package id migration.
- Server deployment.
- Large UI redesign.
