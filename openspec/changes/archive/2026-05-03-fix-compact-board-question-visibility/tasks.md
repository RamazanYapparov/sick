## 1. QuestionBoard: adaptive inner card layout

- [x] 1.1 In `QuestionBoard.kt`, change the inner `Column` inside each theme `Card` to use `padding(horizontal = 8.dp, vertical = 4.dp)` when `fillHeight=true`, keeping `padding(12.dp)` when `fillHeight=false`
- [x] 1.2 Apply `Modifier.fillMaxSize()` to that inner `Column` when `fillHeight=true` (leave it as `fillMaxWidth()` otherwise) and set `verticalArrangement = Arrangement.SpaceBetween` when `fillHeight=true`
- [x] 1.3 Remove the `Spacer(Modifier.height(8.dp))` between the theme name `Text` and the question `Row` when `fillHeight=true`; keep the spacer when `fillHeight=false`
