## 1. Controller — pause/resume sync

- [x] 1.1 In `DesktopSessionController.pauseTimer()`, set `mediaPaused = true` before calling `process(PauseTimer)` (and roll back on error)
- [x] 1.2 In `DesktopSessionController.resumeTimer()`, set `mediaPaused = false` before calling `process(ResumeTimer)` (and roll back on error)

## 2. Verification

- [x] 2.1 Manually verify: load a pack with audio/video question, click Pause — media stops; click Resume — media continues from same position
- [x] 2.2 Verify buzz-triggered pause still works independently (buzz in, wrong answer, media resumes)
