# SIQ Media Format Test Cases

These cases verify every media extension accepted by the current SIPackages quality rules. Generate the packages from `devtools/siq-package-generator` and open them one at a time in the application.

## Common procedure

1. Run `./siq-tool generate cases/media-formats/<recipe>.json -o artifacts/<package>.siq --force`.
2. Run `./siq-tool validate artifacts/<package>.siq` and expect `Valid SIPackages document`.
3. Import the package, start its only question, and observe the media before revealing the answer.
4. Pass only if the expected behavior below occurs without a placeholder, decoding error, freeze, or crash.

## Individual cases

| ID | Recipe → package | Expected behavior |
| --- | --- | --- |
| MF-IMG-001 | `image-jpg.json` → `media-format-image-jpg.siq` | The full 640×360 JPG test pattern is visible with correct colors. |
| MF-IMG-002 | `image-jpe.json` → `media-format-image-jpe.siq` | The full 640×360 JPE test pattern is visible with correct colors. |
| MF-IMG-003 | `image-jpeg.json` → `media-format-image-jpeg.siq` | The full 640×360 JPEG test pattern is visible with correct colors. |
| MF-IMG-004 | `image-png.json` → `media-format-image-png.siq` | The full 640×360 PNG test pattern is visible with correct colors. |
| MF-IMG-005 | `image-gif.json` → `media-format-image-gif.siq` | The 320×180 GIF visibly animates; it is not reduced to a still frame. |
| MF-IMG-006 | `image-webp.json` → `media-format-image-webp.siq` | The full 640×360 WebP test pattern is visible with correct colors. |
| MF-IMG-007 | `image-avif.json` → `media-format-image-avif.siq` | The full 640×360 AVIF test pattern is visible with correct colors. |
| MF-AUD-001 | `audio-mp3.json` → `media-format-audio-mp3.siq` | A clean 1.5-second tone plays once; answer reveal waits for playback. |
| MF-AUD-002 | `audio-opus.json` → `media-format-audio-opus.siq` | A clean 1.5-second tone plays once; answer reveal waits for playback. |
| MF-VID-001 | `video-mp4.json` → `media-format-video-mp4.siq` | The 2-second 640×360 H.264/AAC pattern plays with picture and sound. |
| MF-HTML-001 | `html-html.json` → `media-format-html-html.siq` | The embedded page renders `SIQ HTML TEST ✓` and its color animation runs. |

Record the application version, platform, result, and any console error for every row. Keep failures isolated: do not replace a failed package with the combined all-formats package.
