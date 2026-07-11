package com.lexaprograms.polishcards

internal fun sampleLessonJson(): String {
    return """
        {
          "prompt": "Export all my mistakes or lesson translation cards into this JSON format. Use cardKind MK for mistake cards and LN for normal lesson/translation cards. Fill lessonInfo with instructions for how the learner should work with this specific lesson or card type. For LN fill sourceLanguage and targetLanguage so the app can show language names on the card.",
          "id": "replace_with_unique_lesson_id",
          "title": "Lesson title shown on the main screen",
          "lessonInfo": "Instructions shown from the lesson info icon. Use this for the purpose of the lesson, how to answer, what to pay attention to, and any workflow notes for this set of cards.",
          "sourceLanguage": "Front side input language selected from the app settings list, for example Russian.",
          "targetLanguage": "Back side input language selected from the app settings list, for example Polish.",
          "createdAt": "When the lesson was first saved, for example 2026-06-29 18:40.",
          "updatedAt": "When the lesson was last changed, for example 2026-06-29 18:45.",
          "timesCompleted": 0,
          "editable": true,
          "cards": [
            {
              "id": 1,
              "nativeValue": "Meaning in the native language, for example Russian translation or explanation.",
              "correctValue": "The correct form the learner should type.",
              "wrongAnswers": [
                {
                  "answer": "The exact wrong answer the learner wrote.",
                  "date": "When the mistake was made, for example 2026-06-20 10:30."
                }
              ],
              "hint": "A rule, explanation, or memory hint. Long text is allowed.",
              "madeAt": "Date and time when this mistake card was created or when the mistake happened.",
              "where": "Source of the mistake, for example chat, lesson, exercise, video, or conversation.",
              "log": [
                "Work history for this card, for example created, answered correctly, skipped, or star changes."
              ],
              "mistake": "The current or most important wrong version shown on the front of the card.",
              "type": "card",
              "cardKind": "MK means Mistakes. LN means Lesson. If this field is missing or blank, the app treats it as MK.",
              "sourceLanguage": "For LN cards: language shown on the front side, for example Russian. For MK cards this may be blank.",
              "targetLanguage": "For LN cards: language shown on the back side, for example Polish. For MK cards this may be blank.",
              "stars": 0
            },
            {
              "id": 2,
              "nativeValue": "Text in the source language.",
              "correctValue": "Correct translation in the target language.",
              "wrongAnswers": [],
              "hint": "Optional grammar note, translation hint, or explanation.",
              "madeAt": "",
              "where": "lesson source",
              "log": [
                "created lesson card"
              ],
              "mistake": "",
              "type": "card",
              "cardKind": "LN",
              "sourceLanguage": "Russian",
              "targetLanguage": "Polish",
              "stars": 0
            }
          ]
        }
    """.trimIndent()
}

internal fun versionLogText(): String {
    return """
        MurrLex v0.93 - Added authenticated two-way synchronization for all lessons, one lesson, or one card, backed by the personal MurrLex web applications.
        MurrLex v0.92 - Removed retired game engines, screens, native LibGDX libraries, and Lottie assets. Rive Runner Lab and Rive Letter Blocks remain available.
        MurrLex v0.89 - Online text, speech recognition, text-to-speech, ElevenLabs speech, and image text recognition now use the authenticated MurrLex server gateway. Provider keys remain on the server; the device keeps only an Android Keystore-encrypted renewable session. Offline Android and ML Kit paths remain local.
        Google Translate attribution - Translate mode can use on-device Google Translate via ML Kit. Google disclaims warranties related to translation accuracy and reliability. See https://cloud.google.com/translate and https://translate.google.com.
        MurrLex v0.69 - Cat chat now shows recognized user text, cat replies, and later language checks as soon as each stage is available, keeps focus on the latest phrase, and lets taps toggle word selection for Feature/Create Card.
        MurrLex v0.68 - Back navigation now resets sticky Settings return state, the Study translate button shows the target language code, and manual card-side edits clear cached side audio for that side.
        MurrLex v0.67 - Search toggles by tapping the icon, lesson tiles show orange card-content match counts, searched lessons open at the first matching card, T translates into the card-side language, and quick Edit pre-fills the visible card text.
        MurrLex v0.66 - Анимация (оригинал) now includes the full original ANIM-001 through ANIM-010 set, lessons/dialogs use compact expandable Search, lesson search includes card content, and sorting returns lesson views to the top.
        MurrLex v0.65 - The cat menu now includes Анимация (оригинал), a separate full-screen view using the original makemistake-animations ANIM-007 talking character renderer.
        MurrLex v0.64 - The chat text-selection menu now shows Feature first and Create Cards second, Animation Lab opens as a full screen with Back navigation instead of a popup, and lesson/Study sorting have explicit Asc/Desc controls.
        MurrLex v0.63 - Featured chat selections are now clickable links with a mini explanation and create-card popup, Cat chat card creation confirms inside the dialog, and the Animations menu opens live preview screens for makemistake-animations ANIM-001 through ANIM-010.
        MurrLex v0.62 - Featured chat selections now display in bold, the text-selection menu shows Create Card before Featured, card creation from Cat chat shows a quick confirmation, and the cat dropdown opens a makemistake-animations catalog.
        MurrLex v0.61 - Cat chat correction analysis now appears after the cat answer, stays shorter and in the active message language, the chat has reply-speed and reaction emoji controls, selected chat cards become Hint-to-Answer cards, and Study sorting shows an explicit direction option.
        MurrLex v0.60 - Cat chat voice recognition now uses the active Basic language directly, chat bubbles use native text selection with Feature and Make card actions instead of extra card buttons, Cat dialogs moved to the fifth mode icon beside dictionary modes, and Study card sorting now has Ascending/Descending direction.
        MurrLex v0.59 - Cat chat now saves dialogs, keeps starred dialogs from auto-pruning, analyzes user replies separately from spoken cat answers, creates Mistake cards from analysis, creates featured cards from selected chat fragments, adds cat dialog cards on the main UI, and adds dialog retention plus reply-speed settings.
        MurrLex v0.58 - Cat chat voice input now sends automatically after recognition, lesson list/map navigation restores the previous position, lessons show their last modified date, and the catalog has search plus sorting by created time, modified time, title, card count, and language pair.
        MurrLex v0.57 - Cat chat now supports Basic-language voice input, speaker playback beside cat answers, automatic spoken cat replies, and an in-dialog Auto voice toggle.
        MurrLex v0.54 - Cat chat Create Card now distinguishes user text from cat answers: cat answers become Target-side language cards in the current Basic -> answer-language pair, with the Basic side translated automatically.
        MurrLex v0.53 - Added a Catalog lesson-island map opened from the main MurrLex title; tapping a lesson island opens that lesson directly into its card-island path.
        MurrLex v0.52 - Fixed MurrLex title clicks so the online/offline status refresh stays on the status dot, added an animated Cat chat button beside the title, and lets chat phrases create Basic -> Target cards.
        MurrLex v0.51 - Added a Duolingo-style Study map opened from the Study MurrLex title, with lightly animated card islands, island-to-card navigation, and Back returning from a map-opened card to the map.
        MurrLex v0.50 - Tightened the top action row, removed the duplicate title URL icon, fixed Study display-menu dismissal from outside/root taps, and added clipboard URL auto-fill with Clear/Paste controls.
        MurrLex v0.49 - Moved URL import beside the image/camera actions, changed the Study display menu to a dedicated display-settings icon that stays open after option taps, and added original OpenAI STT voice playback from the card cat menu.
        MurrLex v0.48 - Replaced O/T original-card toggles with compact destination language-code circles, so original text can switch to Basic and translated Basic can switch back to the original language.
        MurrLex v0.47 - Preserved original typed/OCR/URL text when its detected language differs from Basic, added original/translation card switching, Study card copy/move actions, and empty-OK card flipping.
        MurrLex v0.46 - Fixed Catalog image/camera actions, restored separate Study card Info/Edit buttons beside the cat menu, added confirmed Delete to the cat menu, forced Study-created cards to append at the lesson end, and moved the card status dot back beside the language code.
        MurrLex v0.45 - Restored full Study card actions inside the cat menu, added the image OCR button to the Catalog action bar, made the Study language menu toggle from the language code, and fixed image cards appended to a lesson to stay at the end with correct navigation/counts.
        MurrLex v0.44 - Replaced separate Study card action buttons with one round cat-face menu for sharing the card, exporting cached side audio, and viewing card log/progress.
        MurrLex v0.43 - Changed the Study language code into a display menu for sorting/done/three-star visibility, separated gallery and camera image actions, and makes Study image OCR add one card per image to the current lesson while global image import creates a new lesson.
        MurrLex v0.42 - Moved the Study card online dot under the language code, swapped the clear and speaker answer buttons, added a Study camera photo action, and lets image import process up to five photos into one lesson.
        MurrLex v0.41 - Added OpenAI image/screenshot text recognition with a separate image-text model setting and a photo import button next to the URL import action.
        MurrLex v0.40 - Added ordinary website article import: URL text extraction now supports article/main/paragraph HTML and sends up to 2000 words to OpenAI for Basic-to-Target thesis cards.
        MurrLex v0.39 - Changed shared post import so each meaningful sentence becomes one Basic-to-Target retelling card.
        MurrLex v0.38 - Changed shared post and URL import to send up to 500 words to OpenAI and create Basic-to-Target retelling cards.
        MurrLex v0.37 - Fixed Telegram URL import so post text is extracted from Telegram metadata/widget content instead of navigation or embed script text.
        MurrLex v0.36 - Added a URL import button next to the MurrLex title so shared links can be pasted manually and turned into a lesson through the same post-card flow.
        MurrLex v0.35 - Added Android share-target import for text posts from other apps, with a 100-word preview and automatic sentence/vocabulary card creation for the current Basic/Target pair.
        MurrLex v0.34 - Added manual online/offline refresh from the title and card status, same-language microphone Mistake correction with OpenAI, and long-press Mistake generation of Train cards.
        MurrLex v0.33 - OpenAI card translation now writes a short explanation, rule, and examples into the card, copy-to-input also copies to clipboard, and the answer input label follows the current card side language.
        MurrLex v0.32 - Made newer audio playback stop older audio, added configurable card status blink timing, changed cached audio to a black outline, expanded ElevenLabs TTS selection to all app languages, and logs STT/translation/TTS model details on cards.
        MurrLex v0.31 - Added Latvian, Lithuanian, and Portuguese language choices and a Settings menu for choosing which languages use ElevenLabs special TTS.
        MurrLex v0.30 - Plays cached card-side TTS audio even while offline and adds blinking card status dots for online, offline, and cached-audio states.
        MurrLex v0.29 - Added OpenAI voice silence timeout options from 1 to 10 seconds, sped up online status detection, and made app audio stop on taps, navigation, or a new playback request.
        MurrLex v0.28 - Added a 1 second OpenAI voice silence timeout option so speech recognition can stop sooner after the user stops speaking.
        MurrLex v0.27 - Gave enabled OpenAI translation priority for automatic card and Translate paths even when critical offline mode is toggled on, falling back to local models only when OpenAI is unavailable.
        MurrLex v0.26 - Routed card auto-translation and the manual T button through the configured translation provider, using OpenAI when enabled and keeping ElevenLabs only for Belarusian audio.
        MurrLex v0.25 - Replaced the generated startup purr with the provided mp3 asset and made the ElevenLabs Belarusian voice ID editable by hand.
        MurrLex v0.24 - Fixed Belarusian OpenAI speech recognition routing to be-BY and added clear ElevenLabs HTTP error diagnostics for Belarusian TTS without logging API keys.
        MurrLex v0.23 - Added a card-side cached audio download/share action with a download and music-note icon, generating TTS audio first when needed.
        MurrLex v0.22 - Made the startup purr audible on phone speakers and added ElevenLabs Belarusian TTS settings with a separate API key, model, and voice.
        MurrLex v0.21 - Replaced the startup beep with a soft purr and removed sound effects from other app actions.
        MurrLex v0.20 - Added persistent card content cache, punctuation-insensitive OpenAI cache lookup, and configurable voice-signal silence timeout for OpenAI speech recognition.
        MurrLex v0.19 - Added OpenAI cache/API technical status messages and a two-day OpenAI activity log in Settings with automatic pruning.
        MurrLex v0.18 - Added OpenAI response caching with configurable lifetime, cache clearing, cached translation/TTS reuse, cached audio metadata, and a voice waveform while OpenAI speech recognition waits.
        MurrLex v0.17 - Added optional OpenAI online models with separate settings for speech-to-text, translation/text, text-to-speech, TTS voice, base URL, and API key; OpenAI translation no longer shows Google attribution.
        MurrLex v0.16 - Added local language setup after onboarding and a Settings manager for downloaded translation/speech languages, extra downloads, status hints, and translation-model removal.
        MurrLex v0.15 - Quick Vocabulary now treats each Basic/Target pair as a strict lesson boundary, and normal Translate keeps translation on top with input at the bottom.
        MurrLex v0.14 - Split language-pair buttons now override only the current dictionary/Translate session, keep Settings unchanged, keep Split input at the bottom, and create separate lessons per active Basic/Target pair.
        MurrLex v0.13 - Normalized language roles around Basic/Native and Target/Learning languages so quick voice, Translate, card creation, speech recognition, and handoff docs use the same direction.
        MurrLex v0.12 - Reduced repeated Compose recomposition work in Translate/Settings/Test/Catalog paths and made notification card weighting avoid duplicated in-memory lists for weaker phones.
        MurrLex v0.11 - Confirmed Split/Translate offline speech and offline ML Kit translation handoff, added mirrored target-side microphone support, defaulted translation to free online Google Translate when available, and kept critical offline fallback using downloaded models.
        MurrLex v0.05 - Prepared the MurrLex v0.02 working thread build, keeping backend and connector preserved while updating the visible Android version.
        MurrLex v0.03 - Started the clean MurrLex repository checkpoint, kept backend and connector preserved, and aligned the visible Android version with the new product line.
        v1.04 - Hotfixed corrupted localization strings by routing damaged UI labels and lesson summary text through clean safe English labels.
        v1.03 - Renamed the app to MurrLex, replaced the splash M with an animated professor cat, refreshed the launcher icon, and added first-launch onboarding for interface, known, learning, and explanation languages.
        v1.02 - Equalized normal Translate card heights, added tap-to-expand for the translation result, removed Source/Target picker labels, aligned swap controls, fixed mirrored Split result padding, and added a soft border pulse when a translation updates.
        v1.01 - Moved translation results directly below the language header with scrolling, added tap-to-expand Split results, restored source-side lesson card defaults, and labels mistake cards as Mistake instead of a language code.
        v1.00 - Refined Translate and Split layout spacing, removed input field outlines, aligned language labels with speakers at the card top-left, made the normal translation result card larger, and replaced study SL/TL and card numbers with language codes.
        v0.99 - Added a connectivity broadcast refresh so the online/offline title dot updates more reliably, and lowered the Translate/Split action controls to give the translation cards more usable space.
        v0.98 - Improved online/offline status detection and color, cleaned the voice recording language label, refined Translate/Split card layout and speaker placement, synced quick vocabulary languages with microphone controls, replaced the study star menu with SL/TL side switching, and made answer checking target the hidden side.
        v0.97 - Added tap-to-swap quick vocabulary languages, moved network status into a red/green title dot, hid the version from the main header while keeping it in Settings, compacted offline speech language cards to codes with localized hints, and gave Translate more input space with larger swap arrows.
        v0.96 - Added optional translator auto-save, refreshed Translate and Split panels with matching rounded input/output surfaces and speakers, changed Translate header download access into Online/Offline status, and added quick vocabulary language-code pickers beside the catalog microphone.
        v0.95 - Kept quick microphone capture on the lesson catalog in every work mode, so it always creates a quick vocabulary card using the Settings speech language instead of opening Translate.
        v0.94 - Rebuilt voice input around Android on-device SpeechRecognizer, added explicit offline speech model downloads by language in Settings, and blocked microphone capture until the selected language is Ready.
        v0.93 - Translator results now automatically create or update vocabulary cards while staying in Translate, language swap preserves text by swapping fields, and source input has its own speaker.
        v0.92 - Added downloaded Google Translate language tracking, single-language downloads, visible download progress, Translate auto-read toggle, safer native language labels, and quick microphone vocabulary capture without opening Translate.
        v0.91 - Added Google Translate language download popup access from Translate mode and Settings, improved the Translate microphone button contrast, added a G action for empty card sides, and auto-fills available Google translations for newly saved or captured cards.
        v0.90 - Restored offline Google Translate for Translate/Split mode with downloadable language models, Google attribution, quieter language labels, no empty translation placeholder, and a clearer Translate microphone.

        v0.89 - Uses native language labels in Translate panels, tuned the Translate microphone color and size, lets answered or empty-side Test cards flip freely, and adds a star after a correct Test answer.
        v0.88 - Compact language pickers to codes, emphasized the Translate microphone, made Tests reveal the answer only after a correct choice without auto-navigation, randomized choices with A-D markers, and made Test quick edit save the displayed side.
        v0.87 - Clears Translate input on entry, adds language swapping, includes the target vocabulary lesson in Add messages, and makes quick Edit work from Tests with refreshed answers.
        v0.86 - Simplified Translate controls so Add is left, Speak stays centered, Clear is right, and the buttons use icons only.
        v0.85 - Moved Translate original input below the translation result and added a plus action that saves the current translation pair as a vocabulary card.
        v0.84 - Moved Translate/Split language selectors to the bottom, made Tests use a compact card with scrollable answer choices, and added Very small display/control size settings.
        v0.83 - Replaced the Check action with Clear input, made OK handle answer checking, added Cards/Tests/Translate/Split work modes, introduced multiple-choice tests, and added placeholder Translate/Split voice screens.
        v0.82 - Updated notification selection to use 1/2/3-star weighted cards, downgraded unanswered 2-star notification cards, made the lesson editor fully scrollable with card controls underneath, and softened the red/mint brand colors.
        v0.81 - Made lesson opening atomic so the study screen receives a ready card portion immediately instead of briefly rendering an empty lesson state.
        v0.80 - Made lesson opening reload the latest saved lesson, discard broken empty restored sessions, and rebuild the study portion immediately so newly recorded voice cards appear on the first open.
        v0.79 - Made quick voice vocabulary save each card immediately at the top of the target lesson, keep the latest spoken card first, use synchronous lesson persistence, and show Added bubbles longer.
        v0.78 - Added lesson-level source/target input languages with editor dropdowns and JSON export support, used lesson languages as voice fallback for Mixed cards, and refreshed quick Edit taps so the second tap opens the full editor.
        v0.77 - Made the second quick Edit tap open the full card editor, reset lesson stars together with progress, and kept study-plus cards appended while quick voice vocabulary cards stay at the top.
        v0.76 - Restored study counter navigation, moved reset progress into the lesson info popup, placed Add next to lesson info, added study-plus cards at the end, renamed quick vocabulary lessons with language codes and creation date, and kept quick voice lessons opening at the first card.
        v0.75 - Kept quick vocabulary voice capture on the current screen for bulk entry, made Done jump to the last card, created new lessons and study-plus cards with an empty starter card, and changed Refresh into confirmed progress reset.
        v0.74 - Made only cards with an actually missing side auto-open on the filled side; completed normal cards now return to the configured start side during navigation and session restore.
        v0.73 - Added quick visible-side editing from the study card Edit button, kept long-press full editing, shortened transient messages, and made OK require a correct typed answer.
        v0.69 - Reverted segmented voice recognition to the previous single-pass flow for stability, keeping the pending-translation and lesson action refinements.
        v0.68 - Extended voice capture pauses to six seconds with segmented recognition, restored pending-translation cards so the missing side remains visible after flipping, and refined lesson delete/share actions with confirmation.
        v0.67 - Changed quick vocabulary languages to dropdowns, made quick voice capture listen in the target language, showed pending-translation cards on their filled side, and removed voice input from card meta/log fields.
        v0.66 - Added a 1x1 quick voice home-screen widget with a white microphone tile and red MM mark that opens directly into quick vocabulary capture.
        v0.65 - Fixed show-filter toggle semantics, made answer input grow for multiple lines, switched interface language to a dropdown, saved quick voice vocabulary as target-language text, and added bulk card copy/delete tools.
        v0.64 - Improved settings back navigation, added fast white service bubbles, voice input for card editor fields, and clearer filter toggle icons.
        v0.63 - Disabled the unreliable on-device translator, removed the heavy ML Kit translation dependency, and made voice vocabulary cards save with a clear pending-translation message when no server translator is configured.
        v0.62 - Added study text/control size settings, star and done filters, better answer feedback, and quieter study interactions.
        v0.61 - Added local on-device translation as the default quick vocabulary translation path, with server translation kept as a fallback when configured.
        v0.60 - Moved study order, restart, and hide-starred controls into the top bar, replaced the hide-done icon with a crossed star state, enlarged the study card, centered the answer action row, and added server-backed translation settings for quick voice vocabulary.
        v0.59 - Moved quick vocabulary voice capture below the lesson list, fixed quick voice lesson creation and source-language recognition, tightened the OK button, changed Correct feedback into a flying star, refined hide-completed controls, and improved Left-counter navigation.
        v0.58 - Fixed answer-bar spacing, replaced Show all text with an icon, made study counters navigable, added quick voice capture into a New vocabulary lesson with configurable source/target languages, and extended voice silence retry behavior.
        v0.57 - Centered and enlarged voice controls, moved speech playback to the answer bar, added hold-to-keep-recording voice behavior, hid the lesson title behind the info popup, refined missing-letter hints, and kept technical bubbles in English.
        v0.56 - Simplified the Copy action to an icon-only button, kept Check active for empty answers with a white Enter answer bubble, and changed order switching feedback to show the active mode name.
        v0.55 - Made Original the default study order, added a next-order hint when changing order, changed voice input to tap-to-record/tap-to-stop with silence timeout, and added a Check action with visual answer highlighting.
        v0.54 - Added text-to-speech playback on study cards with a speaker icon that reads the currently visible side using the card language when available.
        v0.53 - Replaced the study order label with a compact cycling icon, added a Refresh action to restart the current lesson session, and saved unfinished lesson sessions so returning to a lesson restores the last card, order, answer, and completed progress.
        v0.52 - Made voice recognition choose the answer language from the card, with fallbacks from card text and interface language, and added a five-second silence timeout while keeping press-and-hold microphone behavior.
        v0.51 - Added press-and-hold voice input with recording status, timer, and speech recognition into the answer field; restored a lighter study top area with a single cycling order button; and moved lesson info to the top bar beside Settings.
        v0.50 - Added the Original study order, changed the study order controls to a segmented switch, and made mode changes reorder the current lesson immediately.
        v0.49 - Fixed the header version to read from BuildConfig, added lessonInfo to bundled lessons so info icons are visible immediately, and kept the lesson instructions feature visible in default content.
        v0.48 - Added lesson-level lessonInfo JSON support with info icons on lesson tiles and the study screen, added lesson info editing, preserved lessonInfo in exports, and updated the sample JSON template.
        v0.47 - Localized settings descriptions and lesson tile summaries, kept action button labels in English for stable layout, added a Lessons action to the completed-lesson screen, and made the Left counter jump to the first remaining card.
        v0.46 - Added an in-app interface language selector for EN/DE/BY/ES/UA/RU/PL with saved preference and system-language fallback, and shortened the Make Mistake title color transition to three seconds.
        v0.45 - Added EN/DE/BE/ES/UK/RU/PL interface language support based on Android app/device locale, exposed Android app-language configuration, made the red Make Mistake title fade to the brand salad color over five seconds, and added Repeat / Next lesson actions on the completed-lesson screen while skipping hidden lessons.
        v0.44 - Fixed study counters so they only count cards in the visible portion, finished lessons when every visible card has been accepted with OK, added an in-lesson Show all chip for hiding three-star cards, and added All done / Nothing to show empty states.
        v0.43 - Made card slide transitions respect forward/back direction, moved study-card editing into an in-practice editor dialog, returned to the next or previous study card after deleting the edited card, and replaced the Correct snackbar with a one-second animated bubble.
        v0.42 - Kept OK advancing to the next card while adding a swipe-like visual transition, limited success sound to correct typed answers, showed Correct after typed success, recalculated lesson/card counters after card deletion, and added a Delete action inside the card edit dialog with confirmation.
        v0.41 - Made the app title start red after launch and smoothly transition to the soft salad brand color after the first action, moved hidden lesson markers to the lower-right corner, and recolored hidden eye indicators with the same brand accent.
        v0.40 - Showed only the current card number on study cards, moved the hidden lesson eye to the lower-left, made swipe feedback a soft rustle, matched completed-card framing to the Alphabetical chip color, used the card back label in the answer field, added a configurable active notification maximum, and closed notification practice after a correct answer.
        v0.39 - Made disabled OK read as Done, added a subtle card number in the top-left corner, strengthened completed-card salad highlighting, prewarmed splash audio, renamed the splash title to Make Mistake, added a soft swipe sound, and made MK card editing use the mistake JSON field instead of rule text.
        v0.38 - Replaced the completed-card outline with a soft salad glow, added left/right swipe navigation between study cards, and matched the app title and splash M to the same fresh accent color.
        v0.37 - Defaulted card log off while sound and vibration are on, marked completed cards with a green outline, disabled OK on already completed cards until new letters are typed, and made splash audio/haptics stronger.
        v0.36 - Shared JSON is sent as a file through FileProvider, added settings for showing the card log icon, added sound and vibration options defaulting off, and added an optional splash sound.
        v0.35 - Added a Share action directly on study cards that exports one card as a one-entry Lesson/Mistakes JSON with a copied-from-lesson log entry, and restored the answer bar to standard Scaffold bottom-bar keyboard behavior.
        v0.34 - Kept the study card visible while typing by floating the answer bar above the keyboard, changed the catalog eye to a plain/bright visibility state, added up/down order buttons to configured lesson cards, and added a small crossed-out eye marker on hidden lesson cards.
        v0.33 - Kept Edit only on the study card, made configured lessons drag immediately while unconfigured lessons still use long-press pickup, added Show Hidden in the catalog top bar and Hide/Unhide actions on configured lesson cards, excluded hidden lessons from notifications, cleaned LN/MK title suffixes on load/save, and added IME padding so the study card remains visible above the keyboard.
        v0.32 - Added edit current card actions from the study screen, returns to the same card after saving, removed the 20-card portion limit, cleaned LN/MK suffixes from built-in lesson titles, hid Lesson/Mistakes text on study cards while keeping language labels, made answer checking ignore punctuation/spaces/case, and extended the splash to 1.3 seconds.
        v0.31 - Restored long-press lesson edit/drag mode with centered active action icons, highlighted configured lesson cards, replaced bundled Belarusian lessons with the new MK/LN JSON files, allowed notification intervals down to 1 minute using one-time rescheduling work, shortened the splash to about one second, and made the app title green.
        v0.30 - Kept lesson action icons always visible but inactive until a press-and-hold on the icon zone, restored delayed drag pickup so list scrolling works, and added an animated splash where a red M joins into a green M with the Make Mistake title.
        v0.29 - Replaced hidden edit mode with a gear menu on each lesson card, kept direct drag reordering, added MK/LN card kinds with source/target language labels, and expanded settings to show the full version log.
        v0.28 - Made lesson dragging immediate and disabled it in edit mode, kept edit mode behind a one-second hold, and moved add/edit card fields into a dialog opened from the editor toolbar.
        v0.27 - Kept lesson cards the same color in edit mode, removed the selection checkbox, changed edit actions to a floating cloud overlay, separated drag pickup from one-second edit hold, and replaced bundled Belarusian JSON lessons.
        v0.26 - Fixed lesson tile hit areas by removing hidden action overlays, made the lesson list scroll to all built-in lessons, and added a delayed long-press pickup for lesson dragging.
        v0.25 - Improved lesson card layout with bookmark-style actions, kept completed cards available inside the current portion, added card copy and delete confirmations, made the editor card list visible, and bundled Belarusian mistake lessons by default.
        v0.24 - Made lesson dragging available without entering configuration mode, kept lesson cards visually stable while action icons appear, preserved card color during drag, and made star ratings fill sequentially from left to right.
        v0.23 - Stabilized lesson drag reordering so configured lesson cards stay within the list, and added automatic star progress after a correct typed answer.
        v0.22 - Made Share visible on configured lesson cards, improved download icons, made Copy fill the input with the currently visible card side, added outside-tap exit for lesson configuration, and improved lesson list spacing/highlight.
    """.trimIndent()
}
