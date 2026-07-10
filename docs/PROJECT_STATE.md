# Project State

## Stable Checkpoint

This is the stable handoff checkpoint after the Android_Main Translate/Split online/offline translator and active language-pair work.

Confirmed:

- Local project path: `C:\CodexProjects\murrlex`
- GitHub repository: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch: `Android_Main`
- Android app name: `MurrLex`
- Android visible version: `0.89`
- Android namespace/package remains legacy: `com.lexaprograms.polishcards`
- Backend and connector are preserved in the monorepo but are not the focus of this checkpoint.

## Validation

Latest successful command:

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

`compileDebugKotlin --no-daemon` and `packageDebug --no-daemon` completed for v0.89. `lintDebug` was not rerun for this checkpoint.

## What Works

Confirmed from code and build:

- Android project compiles.
- Debug APK builds.
- `16-bit Ginger Spin Runner` opens as a separate full-screen native Canvas speed-runner with parallax depth, textured terrain, jump/spin movement, opponents, three lives, and four-choice word recovery.
- `8-bit Ginger Platformer` opens as a separate full-screen native Canvas game with pixel-art ginger cat movement, jumping, platforms, question blocks, and card-answer choices.
- `Pseudo 3D Cat Walk` opens as a separate full-screen native Canvas game with a ginger fluffy cat, three-lane movement, perspective gates, and card-answer progression.
- The cat menu now includes a ginger fluffy cat game lab with Compose UI, Compose Canvas, and local HTML5 Canvas/WebView prototypes.
- Cat chat now shows each stage as soon as it is available: recognized user text appears immediately, the cat answer appears before analysis finishes, the list scrolls to the latest phrase, and tapped words toggle quick selection for Feature/Create Card.
- The cat menu has a separate `Анимация (оригинал)` entry that opens the original makemistake-animations ANIM-001 through ANIM-010 demos as a full screen.
- Lesson and dialog screens use compact expandable Search; lesson search includes card content, and sorting scrolls lesson list/map views back to the top.
- Lesson tiles show temporary orange card-content match counts while searching; opening a searched lesson jumps to the first matching card. Study quick Edit pre-fills the visible card text, and T translates toward the active card-side language.
- Back navigation resets sticky Settings return state when opening lessons/catalog/dialogs; the Study translate action shows the target language code and manual side edits clear cached side audio.
- Belarusian OpenAI speech recognition routes through `be-BY`/`language=be`.
- ElevenLabs Belarusian TTS now reports safe HTTP diagnostics such as 403 instead of a generic failure.
- Startup purr uses a packaged mp3 resource.
- ElevenLabs Belarusian voice ID is editable by hand in Settings.
- Card auto-translation and the manual T action use the configured translation provider, including OpenAI when enabled.
- ElevenLabs remains scoped to Belarusian TTS/audio only.
- OpenAI translation has priority for automatic card and Translate paths whenever it is enabled, keyed, and online.
- OpenAI voice silence timeout can be configured down to 1 second.
- Online status detection uses internet capability directly instead of waiting for delayed Android validation.
- Audio playback is single-instance and stops on app taps, navigation away from Study, or a new playback request.
- Study card speech reuses cached side audio even while offline, and cards show blinking online/offline/cached-audio status dots.
- Latvian, Lithuanian, and Portuguese language choices exist, and Settings can add/remove Special ElevenLabs TTS languages.
- Audio playback is last-started-wins, card status blink timing is configurable, cached audio shows as a black outline, and card logs record STT/translation/TTS model or voice details.
- OpenAI card translation can write explanation, rule, and examples into the card; copy-to-input also copies to clipboard; answer labels follow the current side language.
- Tapping the `MurrLex` title or the Study card language/status area forces an immediate online/offline recheck.
- Same-language Quick Vocabulary microphone input creates a Mistake card and asks the configured OpenAI text model for a same-language correction with explanation, all applicable rules, and examples.
- Long-pressing a Mistake card opens ten selectable Train-card generation options. Generated Train cards use card kind `TR` and do not react as generation sources.
- Android text sharing into MurrLex is supported for `text/plain` posts. The import dialog previews up to 500 words and creates one Basic -> Target retelling card per meaningful sentence.
- A URL icon next to the `MurrLex` title opens a manual link-import popup and uses the same shared-post card creation flow after fetching readable page text.
- Telegram URL parsing prefers post metadata/widget content and filters out service navigation/embed/script text before sending content to OpenAI/shared-post card creation.
- Shared post and URL imports now send up to 500 words to OpenAI and generate one Basic -> Target retelling card per meaningful sentence.
- Manual URL import for ordinary websites now extracts article/main/paragraph content, sends up to 2000 words to OpenAI, and generates roughly one Basic -> Target thesis card per 50 words.
- Photo/screenshot import now uses a separate OpenAI image-text model setting, caches recognized text by image hash, and sends extracted text into shared-post card generation.
- Image import can process up to five selected photos into one lesson, and Study has a camera action next to the card language code.
- Study language-code tap opens the display menu for sorting, done-card visibility, and three-star-card visibility. Study gallery/camera OCR appends one card per image/photo to the current lesson; global image import creates a new lesson.
- Study card top-right actions are grouped into one round cat-face menu for sharing, cached visible-side audio export, and card log/progress.
- Study cat menu keeps Info/rule, quick edit, full edit, share, cached side-audio export, and log/progress. Catalog has explicit image OCR access. Study image cards append to the lesson end and navigate to that appended card with full lesson counts.
- Catalog gallery/camera OCR actions are separate matching icons. Study card Info/Edit are separate UI buttons beside the cat menu, which includes confirmed Delete. Study-created cards append at lesson end and navigate there.
- Lesson/card data model exists.
- Local repository logic exists.
- Built-in lesson assets exist.
- Study session model exists.
- Study ordering includes Original, Alphabetical, and Random.
- Card, Test, Translate, and Split UI code exists.
- v0.89 introduces the authenticated MurrLex server AI gateway. Online translation/text work, STT, TTS, and OCR route through `/api/ai/*`; provider keys are server-side only. Offline translation, recognition, TTS, and cached audio remain local.
- Critical offline translation uses ML Kit Google Translate with downloaded models.
- Offline speech recognition works through Android on-device SpeechRecognizer after the selected language model is downloaded.
- The red offline title dot now drives behavior: offline recognition/translation run when the device is offline even if the manual critical offline toggle is off.
- Split mode has mirrored microphone input on the translated side, listening in the target language.
- Settings/Translate/Test/Catalog paths avoid several repeated recomposition computations after the v0.12 performance pass.
- Notification card selection uses weighted random selection without building duplicated weighted card lists.
- Basic/Native Language is the known language and front/native card side.
- Target/Learning Language is the studied language and correct/checked card side.
- Quick vocabulary and Translate dictation default to Basic/Native unless the user explicitly chooses a different pair.
- Quick Dictionary and Translate language buttons change only the current active Basic/Target pair, not global Settings.
- The active Basic/Target pair wins for current speech recognition, translation, and card/lesson creation.
- Quick Vocabulary lessons are separated by strict active Basic/Target lesson metadata.
- Split and normal Translate keep Basic input at the bottom and Target translation at the top.
- After onboarding, users are offered local language downloads for default Basic/Target languages or can continue online.
- Settings includes a Local languages manager for downloaded translation/speech status, extra downloads, and ML Kit translation-model removal.
- Optional OpenAI online mode has separate settings for STT, translation/text, TTS, TTS voice, base URL, and API key.
- OpenAI translation/TTS caching exists with configurable lifetime, cache clearing, and metadata. STT is not cached.
- OpenAI translation/TTS shows cache/API technical status and Settings exposes a two-day auto-pruned activity log.
- Card content cache is persistent in app files storage and is removed only with card/lesson deletion or explicit app cache clearing.
- OpenAI cache lookup ignores punctuation and case. OpenAI STT waveform follows actual voice signal and has a configurable silence timeout.
- Startup sound is a soft purr and other app action sound effects are silent.
- Belarusian TTS has separate ElevenLabs provider/model/voice/API-key settings.
- Study cards have a visible-side cached audio download/share action.
- Voice recognition and TTS code paths exist.
- Notification worker exists.
- Quick voice widget provider exists.
- Settings and version log exist.

## Partially Implemented

- Offline speech recognition depends on Android device support and downloaded models.
- Google/ML Kit offline translation depends on downloaded language models and device capability.
- Backend and connector are prototypes for later server/ChatGPT integration.
- Localization exists but is not cleanly centralized.

## Inferred From Recent Testing

- The user has a stable debug build to test.
- A clean install or app data reset may be needed if old local data still shows corrupted text.

## Uncertain / Needs User Confirmation

- Whether to rename the Android package id away from `com.lexaprograms.polishcards`.
- Which features form the v0.02 MVP versus later experimental paths.
- Whether backend sync should become active in v0.02 or stay parked.
