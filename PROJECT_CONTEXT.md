# Project Context

## Product

MurrLex is a personal language-learning app. The core unit is a card inside a lesson. Cards can represent mistakes, vocabulary, translation pairs, or practice prompts.

## Current Goal

This checkpoint is a stable Android_Main handoff after the Translate/Split online/offline translator and active language-pair work. The goal is to preserve a buildable state and make the next thread easy to start.

## Repository

- Local path: `C:\CodexProjects\murrlex`
- Remote: `https://github.com/milanagaldi-cyber/MurrLex`
- Main next branch: `Android_Main`
- Current Android version: `MurrLex v0.35`

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
26. Keep Android text share-import working from other apps: show a 100-word preview, then create sentence or vocabulary cards for the current Basic/Target pair.
