# A Pack is recognised by its package id, not its file

Played and Finished history is stored under the `id` attribute of the pack's `content.xml` (written by the SIGame editor), or under its file name and size when the pack has no id. The point of the history is to stop the host replaying a Pack with the same group, and the usual way that happens is re-downloading a Pack that was played and deleted, which lands under a new file name (`quiz (1).siq`). For the same reason, deleting a pack file keeps its history.

## Considered Options

- **File path**: simplest, but a re-downloaded or renamed copy shows as unplayed, which defeats the feature.
- **Hash of the file contents**: exact, but packs run to hundreds of MB and every scan would read them whole.

## Consequences

An author who builds a new pack by copying an old one in the editor may keep its id, so two different Packs can share a history. Accepted: rare, and the host sees the name on the row.
