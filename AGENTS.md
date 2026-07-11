# MurrLex Agent Handoff

This repository is the MurrLex monorepo. Use this file as the first stop for any clean Codex thread.

## Current Stable Checkpoint

- Local path: `C:\CodexProjects\murrlex`
- GitHub: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch for the next phase: `Android_Main`
- Android visible app version: `MurrLex v0.93`
- Latest verified Android build command: `gradlew.bat :app:assembleDebug --no-daemon` completed for v0.93
- Latest verified lint command: not rerun for v0.93
- Debug APK: `C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk`

## Current Android Translator Notes

- v0.92 removes retired game engines, screens, LibGDX native libraries, and Lottie assets. Only `Rive Runner Lab` and `Rive Letter Blocks` remain available.
- v0.93 adds authenticated two-way synchronization for all data, the current lesson, or the current card. The server provides personal web apps for lesson/card management, translation, TTS, STT, OCR, and sync checkpoints; access requires login plus an enabled AI grant.
- v0.89 moves online AI traffic behind authenticated MurrLex server endpoints. The mobile app stores only its server session and selected models; OpenAI and ElevenLabs provider keys stay in the encrypted server-side admin cabinet. Online text, STT, TTS, and OCR use `/api/ai/*`; offline behavior remains Android/ML Kit/device TTS and cached audio.

- Translate/Split defaults to online translation through the free Google Translate endpoint while backend/OpenAI translation is parked for later.
- Critical offline fallback uses Android on-device speech recognition and ML Kit Google Translate downloaded language models.
- The red offline title dot must drive behavior, not only UI: offline translation/recognition should run when `!isDeviceOnline` even if the manual critical offline toggle is off.
- Split mode includes a mirrored microphone on the translated side; the conversation partner speaks in the target language.
- RU and BY language options intentionally use a white flag glyph.
- Language roles are explicit: Basic/Native Language is the language the user knows and the default for interface, quick voice, Translate dictation, future AI explanations, and documentation; Target/Learning Language is the language being studied and the checked card side.
- LN cards should store Basic/Native text on the front/native side and Target/Learning text on the correct/checked side unless the user explicitly chooses a different pair.
- Quick Dictionary and Translate language buttons change only the current active Basic/Target pair, not global Settings; the active pair wins for current recognition, translation, and card/lesson creation.
- Quick Vocabulary lessons are separated by active Basic/Target pair, and Split keeps Basic input at the bottom with Target on the mirrored side.
- Belarusian speech recognition must route as `be-BY`/`language=be`; ElevenLabs Belarusian TTS failures should show and log safe HTTP diagnostics such as 403 without exposing the API key.
- Startup purr uses `mobile/android/app/src/main/res/raw/startup_purr.mp3`; ElevenLabs voice ID is a free text setting so paid/available voices can be entered manually.
- Card auto-translation and the manual `T` button should use the same configured translation provider as Translate/Quick Vocabulary; ElevenLabs is only for Belarusian TTS/audio.
- When OpenAI translation is enabled, online, and keyed, it has priority for automatic card/Translate paths even if critical offline mode is toggled on; local ML Kit is only the fallback.
- OpenAI voice silence timeout can be set as low as 1 second in Settings.
- Audio playback is single-instance: app taps, navigation away from Study, or a new playback request stop the current sound.
- Study card speech first reuses cached card-side audio, including while offline; card-side status dots blink every five seconds and show online/offline/cached-audio state.
- Latvian, Lithuanian, and Portuguese are available language choices. Settings has a Special ElevenLabs TTS language menu; enabled languages use the same ElevenLabs voice-generation path as Belarusian.
- Audio playback is last-started-wins. Card status blink interval is configurable, cached audio is shown with a black outline, and card logs include STT/translation/TTS model or voice details.
- OpenAI card translation writes a short explanation, rule, and examples into the card hint/log context; copy-to-input also writes to clipboard; answer input labels follow the current card side language.
- Tapping the `MurrLex` title or the Study card language/status area forces an immediate online/offline recheck.
- Same-language Quick Vocabulary microphone input creates a `Mistake` card and asks the configured OpenAI text model to correct it in the same language with explanation/rules/examples.
- Long-pressing a `Mistake` card opens a ten-option Train-card generation dialog. Generated cards are `Train` (`TR`) cards and must not generate more cards.
- Android text sharing into MurrLex is supported for `text/plain` posts. Shared text opens an import dialog, limits source text to 500 words, and creates one Basic -> Target retelling card per meaningful sentence.
- URL import is in the top action row beside image/camera actions. It opens a link-import popup, fetches page text when possible, limits it to 500 words for shared posts or 2000 words for articles, and uses the same shared-post/article card creation flow.
- Telegram URL import must prefer Telegram post metadata/widget text and reject navigation/embed/script boilerplate as card content.
- Shared text-post import sends up to 500 words to OpenAI and asks for one Basic -> Target retelling card per meaningful sentence.
- Manual URL import for ordinary websites extracts article/main/paragraph text, sends up to 2000 words to OpenAI, and asks for Basic -> Target article thesis cards at roughly one thesis per 50 words.
- Image/screenshot import is available from the photo icon next to the URL icon; it uses a separate OpenAI image-text model setting, caches OCR text by image hash, and sends recognized text into shared-post card generation.
- Image import can process up to five selected photos into one lesson. In Study, the photo icon next to the card language code opens the camera, captures one image, and sends it into the same OCR/card flow.
- Study language-code tap opens a display menu instead of changing the default card side. That menu owns sort mode, done-card visibility, and three-star-card visibility. Study gallery/camera image OCR adds one card per image/photo to the current lesson; global image import creates a new lesson.
- Study card top-right actions are grouped into one round cat-face menu with Share, cached side-audio export, and Log / progress actions.
- The Study cat-face card menu also keeps Info/rule, quick edit, and full edit actions. Catalog has an explicit image OCR action. Image cards appended from Study stay at the end of the lesson and navigate to the appended card with full lesson counts.
- Catalog image and camera actions must be separate matching toolbar icons. Study cards show Info and Edit as separate buttons beside the cat menu; cat menu owns Share, cached side-audio export, Log/progress, Full edit, and confirmed Delete. Study-created cards must append to the lesson end and navigate there.
- Cards can preserve an `original` text layer when typed/OCR/URL input is detected as a language different from the Basic side. Study marks the visible original language as Original, uses a small language-code toggle for switching Original <-> Basic translation, copies/moves cards from the cat menu, and empty OK flips the card.
- v0.49 keeps URL import beside image/camera actions, uses a dedicated Study display-settings icon for sort/done/star visibility without auto-closing the menu on item taps, and caches OpenAI STT original voice recordings for playback from the cat menu.
- v0.50 removes the duplicate title URL icon, makes the top action row denser, fixes Study display-menu dismissal, and auto-fills URL import from the clipboard with Clear/Paste controls.
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
- v0.62 makes Featured chat selections display in bold, orders the text-selection menu as Create Card then Featured, shows a quick card-created message, and adds an Animations entry in the cat dropdown using the makemistake-animations catalog.
- v0.61 moves Cat chat correction analysis after the cat answer, shortens it in the user's active message language, adds in-chat reply speed and reaction emoji controls, makes selected chat cards use Hint -> Answer sides, and makes Study sort direction more explicit.
- v0.60 fixes Cat chat voice recognition to use the active Basic language directly, removes full-message card buttons from chat bubbles, uses native text selection with Feature / Make card actions, moves Cat dialogs to the fifth mode icon beside dictionary modes, and adds Ascending/Descending direction for Study card sorting.
- v0.59 saves Cat dialogs for 30 days by default, keeps starred dialogs, shows dialog cards from the main UI, analyzes user replies separately from spoken cat answers, creates Mistake cards from the analysis, creates featured cards from selected chat fragments, and adds dialog retention plus reply-speed settings.
- v0.58 sends Cat chat microphone input automatically after recognition, restores the lesson catalog/list or map position after leaving Study, shows each lesson's last modified date, and adds catalog search plus sorting by created time, modified time, title, card count, and language pair.
- v0.57 adds Cat chat Basic-language voice input, speaker playback beside cat answers, automatic spoken cat replies, and an in-dialog Auto voice toggle.
- v0.56 makes Cat chat message card creation explicit with a visible Create card action and adds Feed the cat, an animated card game where correct answers fill the cat's bowl.
- v0.55 changes the title cat button into a dropdown menu with Cat chat plus three card games: Quick quiz, Match pairs, and Spell check, using current lesson cards or catalog cards.
- v0.54 makes Cat chat Create Card distinguish user text from cat answers: cat answers become Target-side language cards in the current Basic -> answer-language pair, with the Basic side translated automatically.
- v0.53 adds a Catalog lesson-island map opened from the main `MurrLex` title; tapping a lesson island opens that lesson directly into its card-island path.
- v0.52 fixes `MurrLex` title clicks so online/offline refresh remains on the status dot, keeps the Study title opening the island map, adds an animated Cat chat button beside the title, routes Cat chat through the configured OpenAI text model with a local fallback, and lets chat phrases create Basic -> Target cards.
- v0.51 adds a Duolingo-style Study map opened from the Study `MurrLex` title: card islands follow a lightly animated path, selecting an island opens that card, and Back from a map-opened card returns to the map.
- Normal Translate also keeps Target translation at the top and Basic input at the bottom.
- After onboarding the app offers to download local libraries for the default languages or continue online; Settings has a Local languages manager with translation/speech status icons and ML Kit translation-model removal.
- OpenAI online mode has separate settings for STT, translation/text, TTS, TTS voice, base URL, and API key. Never log the API key.
- OpenAI translation/TTS responses are cached in `cacheDir/openai_cache` with metadata. STT voice recognition is intentionally not cached.
- Card text content is mirrored into persistent `filesDir/card_cache`; it is not TTL-pruned and is cleared only when cards/lessons are deleted or the user explicitly clears app cache.
- OpenAI cache lookup ignores case and punctuation, and OpenAI STT has a configurable voice-signal silence timeout.
- Startup sound is a soft purr only; other app action sound effects are intentionally silent.
- Belarusian TTS can route through ElevenLabs with separate API key/model/voice settings; never log the ElevenLabs key.
- Study cards include a visible-side cached audio download/share action using a download plus music-note icon.
- Settings shows a two-day OpenAI activity log. Translation/TTS paths show short technical cache/API status messages; API keys must never be logged.

## Rules For The Next Thread

- Work only in `C:\CodexProjects\murrlex` unless the user explicitly says otherwise.
- Start with `git status --short --branch` and confirm the branch.
- Keep backend and connector preserved; do not pull server work into Android unless requested.
- Do not implement new features from this handoff step.
- Keep changes small and verified.
- Be careful with text encoding. Past UI/data corruption showed mojibake such as `Ã...` in some legacy strings.
- Commit only after explicit user approval.

## Useful Commands

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Debug APK:

```text
C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk
```
