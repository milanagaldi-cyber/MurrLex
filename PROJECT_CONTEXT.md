# Project Context

## Product

MurrLex is a personal language-learning app. The core unit is a card inside a lesson. Cards can represent mistakes, vocabulary, translation pairs, or practice prompts.

## Current Goal

This checkpoint is a stable Android_Main handoff after the Translate/Split online/offline translator and active language-pair work. The goal is to preserve a buildable state and make the next thread easy to start.

## Repository

- Local path: `C:\CodexProjects\murrlex`
- Remote: `https://github.com/milanagaldi-cyber/MurrLex`
- Main next branch: `Android_Main`
- Current Android version: `MurrLex v0.88`

## Current Priority

Stability first:

1. Verify clean Android build.
2. Preserve the working Translate/Split online/offline behavior.
3. Preserve the Basic/Native -> Target/Learning language direction across quick voice, Translate, cards, and future AI-generated content.
4. Preserve the current active language-pair override in Quick Dictionary and Translate without mutating global Settings.
5. Preserve the small performance wins in Compose recomposition and notification weighting.
6. Keep docs accurate.
7. Avoid touching backend/connector unless requested.
8. Reduce the large Android files gradually in future work.
9. Preserve persistent card cache behavior and OpenAI voice-signal timeout settings.
10. Keep app action sounds silent except for the soft startup purr.
11. Keep Belarusian TTS provider settings separate, including the ElevenLabs API key.
12. Preserve card-side cached audio sharing/downloading from the visible side only.
13. Keep Belarusian speech recognition pinned to `be-BY` for OpenAI STT and surface ElevenLabs HTTP failures safely in Settings logs.
14. Keep startup purr as the provided mp3 resource and keep ElevenLabs BY voice ID manually editable.
15. Keep card auto-translation and the manual T action aligned with the configured translation provider; do not use ElevenLabs for text translation.
16. Keep OpenAI translation as the priority provider for automatic card and Translate paths whenever it is enabled, keyed, and online.
17. Keep OpenAI voice silence timeout configurable down to 1 second.
18. Keep audio playback single-instance and stoppable by app taps or navigation away from Study.
19. Keep cached card-side TTS playable while offline and keep the Study card online/offline/cache indicator visible and blinking.
20. Keep Special ElevenLabs TTS language selection configurable in Settings; enabled languages use the ElevenLabs voice path before OpenAI/device TTS.
21. Keep audio last-started-wins, card status blink timing configurable, and card logs enriched with STT/translation/TTS model details.
22. Keep OpenAI card translations enriched with short explanation/rule/examples, copy-to-input mirrored to clipboard, and answer input labels aligned to the current card side language.
23. Keep manual online/offline refresh available from the `MurrLex` title and Study card language/status area.
24. Keep same-language microphone captures as Mistake cards corrected by the configured OpenAI text model.
25. Keep long-press Train-card generation limited to Mistake cards; generated Train cards are not generation sources.
26. Keep Android text share-import working from other apps: show a 500-word preview, then create one Basic -> Target retelling card per meaningful sentence.
27. Keep manual URL import available from the icon left of `MurrLex`, using the same 100-word shared-post card flow.
28. Keep Telegram URL import extracting real post text from metadata/widget HTML instead of Telegram navigation or embed script boilerplate.
29. Keep shared post/URL imports as retelling lessons: up to 500 words into OpenAI, one Basic -> Target card per meaningful sentence.
30. Keep manual URL import for ordinary websites as article-thesis mode: extract readable article/main/paragraph text, send up to 2000 words, and generate roughly one Basic -> Target thesis card per 50 words.
31. Keep image/screenshot import beside the URL icon: OCR uses the separate OpenAI image-text model, caches by image hash, and feeds recognized text into shared-post card generation.
32. Keep Study image UX: selected image import accepts up to five photos into one lesson, and the Study photo icon beside the language code opens the camera for one captured photo.
33. Keep Study header controls compact: tapping the card language code opens the display menu for sorting, done visibility, and three-star visibility; gallery/camera OCR from Study appends one card per image/photo to the current lesson, while global image OCR creates a new lesson.
34. Keep Study card actions grouped under the round cat-face menu: share card, export cached visible-side audio, and show card log/progress.
35. Keep all previous Study card actions inside the cat menu, including Info/rule, quick edit, and full edit. Catalog must expose image OCR. Image cards appended from Study stay at lesson end and navigate to that appended card with correct full counts.
36. Keep Catalog gallery/camera actions as separate matching icons; Study card Info/Edit are separate buttons beside the cat menu; cat menu includes confirmed Delete. Study-created cards append at lesson end and navigate there.
