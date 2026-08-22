# SIQ Scenario Patterns

Use these mappings after reading the live `recipe.schema.json`.

| Scenario | Recipe shape | Verify |
|---|---|---|
| Text rendering | One text `content` item | Exact Unicode, multiline, empty/long boundary |
| Image | Text plus image file | `Images/<name>` entry and rendered dimensions |
| Audio | Audio file, usually `waitForFinish: true` | `Audio/<name>`, playback, timer start |
| Video | Video file | `Video/<name>`, playback/end/disposal behavior |
| Mixed media | Ordered content items | Reveal order and timer/media coordination |
| Answer media | `answerContent` | Media appears only in answer phase |
| External media | `url` instead of `file` | Allowed scheme and failure behavior |
| Secret question | `type: secret` plus parameters | selection mode, price range, theme override |
| Select answer | `answerType` and grouped `answerOptions` | option labels and correct key mapping |
| Final round | `type: final` | theme selection and final-question flow |
| Malformed input | Canonical baseline plus one mutation | Expected rejection or safe fallback |

## Special question parameters

Secret question:

```json
"type": "secret",
"parameters": {
  "selectionMode": { "type": "simple", "value": "exceptCurrent" },
  "price": {
    "type": "numberSet",
    "minimum": 100,
    "maximum": 500,
    "step": 100
  },
  "theme": { "type": "simple", "value": "Override" }
}
```

Select answer:

```json
"parameters": {
  "answerType": { "type": "simple", "value": "select" },
  "answerOptions": {
    "type": "group",
    "parameters": {
      "a": {
        "type": "content",
        "content": [{ "type": "text", "text": "Option A" }]
      },
      "b": {
        "type": "content",
        "content": [{ "type": "text", "text": "Option B" }]
      }
    }
  }
},
"right": ["a"]
```

## Controlled malformed derivatives

Use a temporary extraction directory. Preserve the canonical `.siq`, edit only the intended file
with `apply_patch`, and rebuild a separate artifact. Examples include:

- remove a referenced media entry;
- change a media reference to a missing name;
- remove a required XML element;
- duplicate or alter a question parameter;
- add an archive traversal entry for extractor security tests.

Name the mutation in the artifact, such as `missing-audio.siq`. Document whether official
`siq-tool validate` should fail. Never use a malformed derivative as evidence of SIQuester output.
