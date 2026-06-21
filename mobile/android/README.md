# Make Mistake

Android app built with Kotlin and Jetpack Compose for practicing mistakes with universal flashcards.

Current app version: `0.47`.

## Project location

```text
C:\CodexProjects\MakeMistake\mobile\android
```

## Features

- Lesson catalog with tile cards.
- One JSON file equals one lesson.
- Each lesson tile shows its title, question count, and completion count.
- Study modes: alphabetical and random.
- Each lesson uses all cards from its JSON file.
- Card front shows the mistake/source value, and the back shows the correct value.
- Tap a card to flip to the value side.
- Type the expected value and check the answer.
- Copy the current value or mark a card as OK/completed.
- `?` button opens information or a rule. Long text is scrollable.
- `M` button opens the wrong-answer history with dates.
- The history icon opens the work log: created, checked, skipped, and when each action happened.
- Built-in lessons loaded from `assets/lessons`.
- Import one or many external JSON lesson files from the device.
- Simple lesson editor: create lessons, add cards, edit cards, delete cards, reorder cards, save lessons.
- Toggle three independent stars on each card.
- Settings screen with default card side: Mistake side or Value side.
- Configurable periodic notifications for weighted 0-2 star cards.
- 0.8-second splash screen on app launch.
- Interface language selector: EN, DE, BY, ES, UA, RU, PL.
- English action buttons to keep the mobile layout stable across languages.

## Built-in lessons

Built-in lesson JSON files live here:

```text
app/src/main/assets/lessons/
```

Example:

```text
app/src/main/assets/lessons/basic_phrases.json
```

## Lesson JSON format

Preferred format:

```json
{
  "id": "common_mistakes",
  "title": "Common Mistakes",
  "cards": [
    {
      "id": 1,
      "nativeValue": "ÃƒÂÃ‚Â¯ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â» ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡Ãƒâ€˜Ã†â€™",
      "correctValue": "SkoÃƒâ€¦Ã¢â‚¬Å¾czyÃƒâ€¦Ã¢â‚¬Å¡em zadanie",
      "wrongAnswers": [{ "answer": "Ja skoÃƒâ€¦Ã¢â‚¬Å¾czyÃƒâ€¦Ã¢â‚¬Å¡ zadanie", "date": "2026-06-19 10:30" }],
      "hint": "Use 'have' with I/you/we/they in the present perfect.",
      "madeAt": "2026-06-19 10:30",
      "where": "Speaking practice",
      "type": "grammar",
      "stars": 1,
      "log": []
    }
  ]
}
```

For imports, the app also accepts a simple array of cards:

```json
[
  {
    "id": 1,
    "mistake": "She go to work every day",
    "value": "She goes to work every day",
    "hint": "Add -s or -es for he/she/it in present simple.",
    "madeAt": "2026-06-19 11:05",
    "where": "Writing exercise",
    "type": "grammar",
    "stars": 0,
    "log": []
  }
]
```

Backward compatibility: old cards with `pl` and `ru` still import. `pl` is treated as the mistake side, and `ru` is treated as the value side.

## Importing lessons

1. Open the app.
2. Tap `Import JSON`.
3. Select one or many `.json` files from the device.
4. Imported lessons appear in the lesson catalog.

Imported and edited lessons are stored locally in the app data.

## Editing lessons

1. Tap the edit icon on a lesson tile.
2. Change the lesson title if needed.
3. Add `Native value`, `Correct Polish`, `Hint`, `Made at`, and `Where`.
4. Delete cards with the trash icon.
5. Tap `Save lesson`.

## Notifications

1. Open `Settings`.
2. Set `Interval minutes` and tap `Save notification interval`.
3. Android asks for notification permission on first launch on Android 13+.
4. Notifications choose a random card from any lesson where exactly one of the three stars is filled.
5. The notification text shows the value side, and tapping it opens the app on that card.

Android WorkManager uses a minimum periodic interval of 15 minutes, so lower values are saved as 15 minutes. The default interval is 30 minutes.

## Opening in Android Studio

1. Open Android Studio.
2. Choose `File -> Open`.
3. Select `C:\CodexProjects\PolishCards`.
4. Wait for Gradle Sync.
5. Run the app on an emulator or Android device.

## Building APK

In Android Studio:

1. Choose `Build -> Build Bundle(s) / APK(s) -> Build APK(s)`.
2. Click `locate` after the build finishes.

From terminal:

```bat
gradlew.bat assembleDebug
```

Debug APK path:

```text
app\build\outputs\apk\debug\app-debug.apk
```








