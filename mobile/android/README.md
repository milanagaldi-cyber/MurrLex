# MurrLex Android

Android app built with Kotlin and Jetpack Compose for local-first language learning, voice input, translation, and universal flashcards.

Current app version: `0.89`.

## Project Location

```text
C:\CodexProjects\murrlex\mobile\android
```

## Current Translator State

- Online AI requests use the authenticated MurrLex server gateway. The app keeps selected models and its MurrLex session only; OpenAI and ElevenLabs provider keys are server-side.
- Critical offline fallback uses Android on-device SpeechRecognizer and ML Kit Google Translate downloaded models.
- The effective offline condition is `state.useLocalTranslation || !isDeviceOnline`; keep the red offline indicator aligned with actual behavior.
- Split mode has microphone buttons on both sides. The translated-side microphone listens in the target language.
- RU and BY language options intentionally use a white flag glyph.
- v0.12 reduces repeated Compose recomposition work in Settings/Translate/Test/Catalog and avoids duplicated weighted notification-card lists.
- v0.13 normalizes Basic/Native and Target/Learning language roles across Settings, onboarding, quick voice, Translate, and card creation.
- v0.14 keeps Quick Dictionary and Translate language-pair buttons session-scoped, lets the active pair override recognition/translation/card creation, separates Quick Vocabulary lessons by pair, and keeps Split input at the bottom.
- v0.15 makes Quick Vocabulary lesson matching strict by Basic/Target pair and puts normal Translate output on top with input at the bottom.
- v0.16 adds local language setup after onboarding and a Settings manager for downloaded translation/speech languages, extra downloads, status hints, and translation-model removal.
- v0.17 adds optional OpenAI online models with separate settings for speech-to-text, translation/text, text-to-speech, TTS voice, base URL, and API key.
- v0.18 adds configurable OpenAI translation/TTS caching, cache clearing, cached audio metadata, and OpenAI STT waveform/progress feedback.
- v0.19 adds OpenAI cache/API technical messages and a two-day OpenAI activity log in Settings.
- v0.20 adds persistent card content cache, punctuation-insensitive OpenAI cache lookup, and configurable OpenAI STT silence timeout based on voice signal.
- v0.21 replaces the startup beep with a soft purr and keeps other app action sounds silent.
- v0.22 makes the startup purr more audible and adds ElevenLabs Belarusian TTS provider/model/voice/API-key settings.
- v0.23 adds a visible-side card audio download/share action for cached or newly generated TTS mp3 files.
- v0.24 fixes Belarusian OpenAI speech recognition routing to `be-BY` and logs safe ElevenLabs HTTP diagnostics for Belarusian TTS failures.
- v0.25 replaces the generated startup purr with the provided mp3 resource and makes the ElevenLabs BY voice ID manually editable.
- v0.26 routes card auto-translation and the manual T button through the configured translation provider, using OpenAI when enabled and keeping ElevenLabs audio-only.
- v0.27 gives enabled online OpenAI translation priority for automatic card and Translate paths, including Polish to Belarusian, even when critical offline mode is toggled on.
- v0.28 adds a 1 second OpenAI voice silence timeout option for faster speech recognition cutoff.
- v0.29 adds 1-10 second OpenAI voice silence timeout options, speeds online status detection, and stops current audio on app taps/navigation/new playback.
- v0.30 plays cached card-side TTS audio while offline and adds blinking card status dots for online, offline, and cached-audio states.
- v0.31 adds Latvian, Lithuanian, and Portuguese and lets Settings choose which languages use the special ElevenLabs TTS path.
- v0.32 makes new audio stop older audio, adds configurable card status blinking, shows cached audio with a black outline, expands ElevenLabs TTS selection to all app languages, and logs STT/translation/TTS model details on cards.
- v0.33 enriches OpenAI card translation with short explanation/rule/examples, copies answer-bar copy values to the clipboard, and fixes current-side answer language labels.
- v0.34 adds forced online/offline refresh taps, same-language microphone Mistake correction through the configured OpenAI text model, and Mistake long-press generation of Train cards.
- v0.35 adds Android text share-import from other apps. The current import behavior now previews up to 500 words and creates Basic -> Target retelling sentence cards.
- v0.36 adds a URL import button beside the app title so pasted Telegram/web links can create lessons through the same shared-post flow.
- v0.37 fixes Telegram URL parsing so real post text is sent into card generation instead of page navigation or embed script text.
- v0.38 sends up to 500 imported words to OpenAI for retelling-card generation.
- v0.39 changes imported post generation to one Basic -> Target retelling card per meaningful sentence.
- v0.40 adds ordinary website article import with up to 2000 words sent to OpenAI and roughly one Basic -> Target thesis card per 50 words.
- v0.41 adds OpenAI image/screenshot text recognition with a separate image-text model setting and a photo import button beside URL import.
- v0.42 swaps the Study clear/speaker button positions, moves the card status dot under the language code, adds camera capture beside the Study language code, and imports up to five selected photos into one lesson.
- v0.43 changes the Study language code into a display menu for sorting/done/three-star visibility, separates gallery and camera OCR actions, and appends one card per Study image/photo to the current lesson while global image import creates a new lesson.
- v0.44 replaces separate Study card action buttons with one round cat-face menu for sharing the card, exporting cached side audio, and viewing card log/progress.
- v0.45 restores Info/rule and edit actions inside the cat menu, adds Catalog image OCR access, toggles the Study language menu from the language code, and fixes Study image-card append/navigation/counts.
- v0.46 separates Catalog gallery/camera OCR icons, restores Study card Info/Edit as separate buttons beside the cat menu, adds confirmed Delete, moves the status dot beside the language code, and forces Study-created cards to append at lesson end.
- v0.47 preserves original typed/OCR/URL text when the detected language differs from Basic, adds O/T switching on cards, lets Study copy/move cards from the cat menu, and makes empty OK flip the card.
- v0.48 replaces O/T original-card toggles with compact destination language-code circles: original text shows the Basic code to switch to, and translated text shows the original language code to switch back.
- v0.49 places URL import beside image/camera actions, changes the Study display dropdown root to a display-settings icon that stays open after item taps, and lets the cat menu play cached original OpenAI STT voice recordings.
- v0.50 tightens the top action row, removes the duplicate title URL icon, fixes Study display-menu dismissal from outside/root taps, and auto-fills URL import from clipboard URLs with Clear/Paste controls.
- v0.89 adds MurrLex server login/registration with rotating persistent sessions and moves online text, STT, TTS, ElevenLabs TTS, and image OCR to protected server endpoints. Offline Android/ML Kit/device speech paths remain local.
- v0.88 improves `Rive Letter Blocks` with non-overlapping block placement, tap-to-place/tap-to-return controls, letters-only tokens, a second word-order sentence stage, and a light green summer meadow background.
- v0.87 adds `Rive Letter Blocks`, a card-word game where the user drags letter cubes into word slots, can rearrange or return letters, checks OK/Not OK with TTS feedback, advances to the next card word on success, and sees the ginger cat react beside the board.
- v0.86 replaces the visible Rive sample vehicle in `Rive Runner Lab` with an expressive ginger cat actor, adds Spin/Dance/Crawl/Meow/Scratch/Talk/Stand actions, and wires Meow/Talk to the app TTS so the cat can speak the current target-language word aloud.
- v0.85 expands `Rive Runner Lab` into a brighter side-scroller experiment: the Rive character auto-runs through a platform scene, jumps into `?` blocks to pop language words, answers popped-word choices, and gains lives by landing on opponents.
- v0.84 explicitly initializes the Rive native runtime in `MurrLexApplication` before any `RiveAnimationView` is created, fixing the `FileAssetLoader.constructor()` `UnsatisfiedLinkError` when opening `Rive Runner Lab`.
- v0.83 adds `Rive Runner Lab`, a real `rive-android 9.13.10` runtime experiment with a local `.riv` asset, vivid neon Compose game shell, movement/boost controls, and the one explicit `Задание` +1-life rule. Newer `rive-android 11.7.1` was checked but requires Android SDK 36 / AGP 8.9.1, so this build uses the latest compatible 9.x line.
- v0.82 adds `Flutter Runner Lab`, a Flutter-style game prototype hosted in Compose because Flutter SDK is not installed on this machine; it has a widget-tree themed scene, movement/jump controls, and the one explicit `Задание` +1-life rule.
- v0.81 makes the title cat dropdown menu scrollable so lower game entries such as `Lottie Runner Lab` stay reachable on small screens.
- v0.80 adds `Lottie Runner Lab`, a Compose game-logic experiment with a local Lottie animated cat, movement buttons, and one explicit `Задание` card for +1 life.
- v0.79 adds `Godot Runner Lab`, a Godot-style walker experiment with movement buttons and one explicit `Задание` card that gives +1 life when answered correctly.
- v0.78 makes the LibGDX runner use a real Android overlay `Задание` button while keeping the one-task, +1-life action-game rule.
- v0.77 changes action-game card tasks so they appear only from the explicit `Задание`/TASK button; each game run has one task and a correct answer gives +1 life.
- v0.76 fixes a LibGDX startup `NullPointerException` by creating GL resources inside `create()` and adds Settings crash diagnostics for the last two app crashes.
- v0.75 fixes `LibGDX Ginger Runner` startup by packaging Android LibGDX native libraries and showing a safe startup failure message if the engine cannot initialize.
- v0.74 adds `LibGDX Ginger Runner`, a separate landscape LibGDX speed-runner experiment opened from the cat game menu and fed by the current lesson cards.
- v0.73 adds `16-bit Ginger Spin Runner`, a full-screen native Canvas speed-runner experiment with parallax depth, textured terrain, jump/spin movement, opponents, three lives, and four-choice word recovery.
- v0.72 adds `8-bit Ginger Platformer`, a full-screen native Canvas retro platformer with pixel art, side movement, jumping, platforms, question blocks, and card-answer choices.
- v0.71 adds `Pseudo 3D Cat Walk`, a separate full-screen native Canvas brawler/walker experiment with three lanes, perspective gates, card answers, and a ginger fluffy cat.
- v0.70 adds a ginger fluffy cat game lab with three experimental technologies: Compose UI, Compose Canvas, and local HTML5 Canvas inside WebView.
- v0.69 makes Cat chat progressive: recognized user text, cat replies, and later language checks appear as soon as each piece is available; chat scroll follows the latest phrase, and tapping words toggles quick selection for Feature/Create Card.
- v0.68 hardens back navigation so Settings cannot become a sticky return screen, replaces the Study T button with the target language code, and clears cached side audio when a card side is manually edited.
- v0.67 makes Search toggle size by tapping the search icon, shows temporary orange card-content match counts on lesson tiles, opens searched lessons at the first matching card, keeps T translation aimed at the card-side language, and pre-fills quick edit with the visible card text.
- v0.66 expands `Анимация (оригинал)` to the full original ANIM-001 through ANIM-010 set, adds compact expandable Search for lessons/dialogs, searches lesson card content, and scrolls lesson views to top after sorting.
- v0.65 adds a separate `Анимация (оригинал)` cat-menu item that opens the original makemistake-animations ANIM-007 talking character renderer on its own screen.
- v0.64 restores the text-selection menu order to Feature first and Create Cards second, moves Animation Lab out of the popup into a full screen with Back navigation, and adds explicit Asc/Desc controls for lesson and Study sorting.
- v0.63 turns Featured chat selections into clickable links with a mini explanation/create-card popup, shows card-created confirmation inside the Cat chat popup, and opens live preview screens for makemistake-animations ANIM-001 through ANIM-010.
- v0.62 makes Featured chat selections display in bold, orders the native text-selection menu as Create Card then Featured, shows a quick card-created message, and adds an Animations catalog entry in the cat dropdown from makemistake-animations.
- v0.61 moves Cat chat correction analysis after the cat answer, shortens it in the active message language, adds in-chat reply speed and reaction emoji controls, creates selected chat cards as Hint -> Answer, and exposes Study sort direction as an explicit menu item.
- v0.60 fixes Cat chat voice recognition to use the active Basic language directly, removes full-message card buttons from chat bubbles, uses native text selection with Feature / Make card actions, moves Cat dialogs to the fifth mode icon beside dictionary modes, and adds Ascending/Descending direction for Study card sorting.
- v0.59 saves Cat dialogs for 30 days by default, keeps starred dialogs, shows dialog cards from the main UI, analyzes user replies separately from spoken cat answers, creates Mistake cards from the analysis, creates featured cards from selected chat fragments, and adds dialog retention plus reply-speed settings.
- v0.58 sends Cat chat microphone input automatically after recognition, restores the lesson catalog/list or map position after leaving Study, shows each lesson's last modified date, and adds catalog search plus sorting by created time, modified time, title, card count, and language pair.
- v0.57 adds Cat chat Basic-language voice input, speaker playback beside cat answers, automatic spoken cat replies, and an in-dialog Auto voice toggle.
- v0.56 makes Cat chat message card creation explicit with a visible Create card action and adds Feed the cat, an animated card game where correct answers fill the cat's bowl.
- v0.55 changes the title cat button into a dropdown menu with Cat chat plus three card games: Quick quiz, Match pairs, and Spell check, using current lesson cards or catalog cards.
- v0.54 makes Cat chat Create Card distinguish user text from cat answers: cat answers become Target-side language cards in the current Basic -> answer-language pair, with the Basic side translated automatically.
- v0.53 adds a Catalog lesson-island map opened from the main `MurrLex` title; tapping a lesson island opens that lesson directly into its card-island path.
- v0.52 fixes `MurrLex` title clicks so status refresh stays on the status dot, adds an animated Cat chat button beside the title, and lets chat phrases create Basic -> Target cards.
- v0.51 adds a Duolingo-style Study map opened from the Study `MurrLex` title, with lightly animated card islands, island-to-card navigation, and Back returning from a map-opened card to the map.

## Core Features

- Lesson catalog with tile cards.
- Built-in lessons loaded from `app/src/main/assets/lessons`.
- Local lesson import and editing.
- Card, Test, Translate, and Split modes.
- Voice input, text-to-speech, offline speech model downloads, and translation model downloads.
- Settings screen with language, translator, notification, and version log controls.

## Building APK

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

Debug APK path:

```text
C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk
```

## Manual Smoke Checks

1. Confirm Settings shows `MurrLex v0.88`.
2. Confirm online Translate works with network enabled.
3. Disable network and confirm the title indicator is red.
4. Confirm manual text in Translate uses offline ML Kit translation.
5. Confirm microphone speech is recognized offline and translated offline.
6. Confirm Split mode microphones work on both sides.
7. Confirm Basic/Native text becomes the card front and Target/Learning text becomes the checked side.
8. Confirm changing the Quick Dictionary or Translate pair does not change Settings but does affect immediate recognition, translation, and new card/lesson creation.
9. Confirm OpenAI cache lookup ignores case and punctuation and the waveform stops during silence.
10. Confirm only the startup screen plays a soft purr; other app actions stay silent.
11. Confirm Belarusian playback uses ElevenLabs with voice `q19tj6dG7gitafffmfLO` when selected and keyed.
12. Confirm the card-side audio action exports only the side currently visible on the card.
13. Confirm Belarusian OpenAI speech recognition does not fall back to Russian.
14. Confirm ElevenLabs 403 appears as a safe diagnostic in the app log without exposing the API key.
15. Confirm startup purr is audible on app launch.
16. Confirm a new ElevenLabs voice ID can be typed and saved.
17. Confirm card auto-translation and the `T` button use OpenAI when enabled.
18. Confirm automatic Polish to Belarusian card translation fills without pressing `T` when OpenAI is enabled.
19. Confirm OpenAI voice silence timeout can be set to `1 second`.
20. Confirm OpenAI voice silence timeout offers 1 through 10 seconds.
21. Confirm app audio stops on tap, navigation away from Study, or another playback request.
22. Confirm cached card-side TTS audio plays while offline.
23. Confirm the Study card status dot blinks every five seconds and shows green online, red offline, and yellow when visible-side audio is cached while online.
24. Confirm Latvian, Lithuanian, and Portuguese appear in language pickers.
25. Confirm Special ElevenLabs TTS language chips can add/remove languages and enabled languages route generated speech through ElevenLabs.
26. Confirm new audio playback stops any older audio.
27. Confirm card status blink interval options include Off, 0.5, 1, 2, 3, 4, and 5 seconds.
28. Confirm cached audio uses a black outline and offline dots do not blink.
29. Confirm card info logs show STT, translation, and TTS model/voice details.
30. Confirm OpenAI card translation fills the card hint with explanation, rule, and examples.
31. Confirm the answer-bar copy icon also writes to the system clipboard.
32. Confirm Polish/Lithuanian answer input labels match the side being typed.
33. Confirm tapping `MurrLex` and the Study card language/status area refreshes online/offline status immediately.
34. Confirm same-language Quick Vocabulary microphone input creates a Mistake card with OpenAI correction details.
35. Confirm long-press on a Mistake card opens ten Train-card options and generated Train cards do not open the generator.
36. Confirm sharing text from another app into MurrLex opens the post import dialog and creates retelling cards from up to 500 words.
37. Confirm the URL icon next to `MurrLex` opens the link import popup and creates a lesson from a pasted URL.
38. Confirm Telegram post URLs create cards from post text, not Telegram navigation or embed script text.
39. Confirm imported posts create one retelling card per meaningful sentence from up to 500 words.
40. Confirm ordinary article URLs create thesis cards from up to 2000 words, roughly one card per 50 words.
