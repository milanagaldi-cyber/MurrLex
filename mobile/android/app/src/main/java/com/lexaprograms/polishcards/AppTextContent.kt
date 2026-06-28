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
        Google Translate attribution - Translate mode can use on-device Google Translate via ML Kit. Google disclaims warranties related to translation accuracy and reliability. See https://cloud.google.com/translate and https://translate.google.com.
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
