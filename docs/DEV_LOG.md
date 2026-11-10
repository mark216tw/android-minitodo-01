# 開發紀錄

最新紀錄放最上面。每次開發工作都要更新，不要事後補寫。

---

## 2026-11-10 - v1.0.0-prerelease（versionCode 13）

### 目標

建立完整的繁體中文專案文件、MIT License 與公開 GitHub 儲存庫，並產出可供測試的 `1.0.0-prerelease` APK，發佈為 GitHub Pre-release。

### 變更

- 新增 `LICENSE`（MIT License，著作權人 mark216tw）。
- 改寫 `README.md`：功能、技術棧、建置方式、專案結構、文件索引、隱私與授權。
- 新增 `docs/USER_GUIDE.md`：安裝、待辦與項目操作、搜尋排序、封存、小工具、通知、設定、備份與常見問題。
- 新增 `docs/ARCHITECTURE.md`：分層架構、資料模型、資料流、分類演算法、提醒流程、小工具、備份、測試與建置變體。
- 新增 `docs/DESIGN.md`：設計目標、需求、資訊架構、畫面與互動設計、核心邏輯、邊界條件與設計決策。
- 新增 `docs/PRIVACY.md`：本機資料、權限、通知、備份與第三方元件說明。
- 新增 `docs/BUILD_AND_RELEASE.md`：建置指令、R8、簽章、版本規則與 GitHub Release 流程。
- 更新 `CHANGELOG.md`、`RELEASE_NOTES.md` 與 `README.md` 的版本資訊。
- `app/src/main/res/values/strings.xml`：關於頁授權說明改為 MIT License 與 Apache License 2.0 元件授權。
- `app/build.gradle.kts`：`versionCode` 13、`versionName` 1.0.0；`prerelease` 輸出 `1.0.0-prerelease`。
- 前一筆顏色區塊順序調整一併納入本測試版。

### 問題與解法

- 對外版號需為 1.0.0，但先前內部版本已到 1.5.1：`versionName` 改為 1.0.0，`versionCode` 維持遞增為 13，避免版本代碼倒退。
- `prerelease` 需同時具備 R8 與 Debug 簽章：沿用 `initWith(release)` 並覆寫 `signingConfig`，輸出 `app-prerelease.apk`。
- GitHub 發行需明確標示測試版：Release 設定為 Pre-release，說明標註「非正式上線版本」。

### 待辦

- 尚未在實機完整驗證所有流程。
- 尚未設定正式簽章與自動化 CI。

## 2026-11-10 - v1.5.1（versionCode 12）

### 目標

調整編輯待辦頁面的欄位順序，讓顏色選擇移到「標記待辦完成」下方，完成狀態與顏色兩者操作更連貫。

### 變更

- `ui/edit/TodoEditScreen.kt`：將顏色選擇區塊（`colors`）移至「標記待辦完成」（`master-complete`）之後。
- `app/build.gradle.kts`：`versionCode` 12、`versionName` 1.5.1。

### 問題與解法

- 僅調整 LazyColumn 項目順序，未涉及狀態或儲存邏輯。

### 待辦

- 尚未實機確認版面間距。

## 2026-11-10 - v1.5.0（versionCode 11）

### 目標

執行一輪「改動小、效益高」的介面與功能優化：改善時間顯示與結束時間預設、補強無障礙與滑動提示、加入搜尋與備份，並調整通知權限詢問時機。

### 變更

- `util/DateTimeUtils.kt`：`formatRange()` 同日只顯示一次日期、今年省略年份、跨年份保留年份；新增 `formatDateShort()`。
- `ui/edit/TodoEditViewModel.kt`：結束日期／時間預設跟隨開始（未設定結束時使用開始時間 +1 小時）；新增 `ItemRemoved` 事件與 `restoreItem()`；加入通知權限詢問狀態。
- `ui/components/DateTimeField.kt`：新增 `fallbackDate`，結束日期選擇預設帶入開始日期；日期對話框加入今天／明天／下週快選。
- `ui/edit/TodoEditScreen.kt`：新待辦自動聚焦標題；標題／項目欄位支援鍵盤 Next／Done；儲存中顯示「儲存中…」；刪除項目可 Snackbar 復原；第一次建立含時間項目時詢問通知權限。
- `MainActivity.kt`：移除冷啟動自動詢問通知權限。
- `ui/todo/TodoListViewModel.kt`：新增搜尋查詢與標題／項目內容篩選。
- `ui/todo/TodoListScreen.kt`：加入搜尋列與無結果狀態；空白狀態新增「新增待辦」按鈕；滑動背景依方向變色並顯示對應文字；已完成待辦右滑顯示「標記未完成」；卡片加入 TalkBack 封存動作；「已完成」區段可收合。
- `ui/components/ColorPicker.kt`、`ui/settings/SettingsScreen.kt`：色塊觸控範圍擴大至 48dp。
- `widget/TodoWidget.kt`：項目顯示前先排序，未完成項目優先。
- `widget/WidgetConfigActivity.kt`：小工具設定頁空狀態改用專用文案。
- `data/local/TodoDao.kt`、`data/repository/TodoRepository.kt`：新增備份用全量查詢、清空與覆寫匯入。
- `data/backup/BackupJson.kt`、`data/backup/BackupManager.kt`、`AppContainer`：新增 JSON 匯出／匯入。
- `ui/settings/SettingsViewModel.kt`、`ui/settings/SettingsScreen.kt`、`ui/AppNavHost.kt`：設定頁新增備份與還原區段、匯入確認與 Snackbar。
- `ui/archive/ArchiveScreen.kt`、`ArchiveViewModel.kt`：移除封存頁重複的「封存已完成」按鈕。
- `app/build.gradle.kts`：`versionCode` 11、`versionName` 1.5.0。
- 更新 `DateTimeUtilsTest`、README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 同日時間區間原本會重複完整日期，窄螢幕容易截斷 → 同日改為只顯示一次日期；今年省略年份，跨年份仍完整顯示。
- 結束時間在未設定時預設為 09:00，若開始時間在下午會直接觸發「結束早於開始」→ 改以開始時間 +1 小時為預設，時間選擇也優先沿用開始日期。
- 已完成待辦右滑實際是取消完成，但背景仍顯示「已完成」→ 依完成狀態顯示「完成」或「標記未完成」。
- TalkBack 無法透過滑動封存 → 在卡片加入自訂無障礙動作。
- 小工具前 5 項可能都被完成項目佔滿 → 顯示前先依完成狀態排序。
- 通知權限原本在冷啟動詢問，與使用情境無關 → 改在第一次建立含時間項目時詢問，並以 DataStore 記錄是否問過。
- 備份需要跨版本穩定格式 → 使用固定 `version` 欄位與 Android 內建 `org.json`，不新增依賴。

### 待辦／已知限制

- 目前仍無實機或模擬器，搜尋、備份／匯入、通知權限、滑動提示、TalkBack 與小工具排序需實機驗證。
- 匯入備份會覆蓋目前所有待辦與封存資料，尚未提供合併匯入。
- 備份檔大小上限為 5 MB，超過會匯入失敗。
- 結束時間若剛好跨午夜，仍可能被驗證為「結束早於開始」，需使用者調整日期。

### 日期與版本（本次）

- 日期：2026-11-10
- versionCode：11
- versionName：1.5.0

---

## 2026-11-10 - v1.4.2（versionCode 10）

### 目標

將設定頁的主題色彩選擇改為標題下方的行內圓形色塊，移除選擇視窗與色名，並在已選取的色塊中央顯示勾勾。

### 變更

- `ui/settings/SettingsScreen.kt`：移除 `showPaletteDialog` 與 `ThemePaletteDialog`；「主題色彩」改為 `ListItem` 標題加上 `FlowRow` 圓形色塊。
- `ui/settings/SettingsScreen.kt`：`ThemePaletteSwatch` 改為可點選、`selectable` + `Role.RadioButton`，保留色盤名稱作為無障礙描述；選取時中央顯示勾勾。
- `ui/settings/SettingsScreen.kt`：Android 12 以下仍隱藏「跟隨桌布」，選取狀態以 `effectivePalette` 為準。
- `ui/theme/Color.kt`：新增 `ThemePalette.onThemePaletteColor()`，依 `previewColor` 亮度選擇深／淺勾勾；跟隨桌布使用白色勾勾。
- `ui/settings/SettingsScreen.kt`：跟隨桌布的漸層色塊在勾勾後加半透明深色圓底，提升亮色區域的對比。
- `app/build.gradle.kts`：`versionCode` 10、`versionName` 1.4.2。
- 更新 README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 原本色盤選擇需要開啟對話框，且顯示色名與 RadioButton，與行內色塊的需求不符 → 改為 `FlowRow` 直接排列，每個色塊本身即為選取控制項。
- 勾勾在深色與淺色色盤上的對比不同 → 固定色盤依亮度自動選色；漸層色盤加上半透明深色圓底，確保白色勾勾可辨識。
- 小螢幕上 7 個色塊可能超出寬度 → 使用 `FlowRow` 自動換行，維持 36dp 圓形與 8dp 間距。

### 待辦／已知限制

- 目前仍無實機或模擬器，需確認小螢幕換行、勾勾對比與 TalkBack 的選取狀態朗讀。
- 色塊觸控範圍目前為 36dp，與既有待辦顏色選擇器一致，未額外擴大到 48dp。

### 日期與版本（本次）

- 日期：2026-11-10
- versionCode：10
- versionName：1.4.2

---

## 2026-11-10 - v1.4.1（versionCode 9）

### 目標

完成一輪「改動小、效益高」的品質修正：冷啟動 deep link、存檔可靠性、通知權限、無障礙、拖曳排序、小工具主題與文案一致性。

### 變更

- `MainActivity.kt`：`onCreate` 在首次建立時把啟動 `Intent` 交給 NavHost，修正 App 被系統結束後從通知／小工具開啟仍停在列表的問題；通知權限改為只在首次啟動詢問一次。
- `data/prefs/SettingsRepository.kt`：新增 `notification_permission_asked` 與 `notificationPermissionAsked`，避免每次啟動或旋轉螢幕重複詢問。
- `MainActivity.kt`：設定載入完成前不組合主要內容，減少預設主題一閃而過。
- `ui/edit/TodoEditViewModel.kt`：`save()` 加入 `isSaving` 防連點；資料庫寫入失敗時重設狀態並送出 `SaveFailed`，不再卡在儲存中。
- `ui/edit/TodoEditScreen.kt`：顯示儲存失敗提示；儲存中禁用返回；拖曳排序加入邊緣自動捲動與 TalkBack「上移／下移」動作。
- `ui/todo/TodoListScreen.kt`：沒有清單項目的待辦不顯示「0/0」；勾選框描述加入待辦標題；分隔符改用字串資源；手動排序加入自動捲動與 TalkBack 排序動作。
- `ui/archive/ArchiveScreen.kt`、`widget/WidgetConfigActivity.kt`：沒有清單項目時不顯示「0/0」。
- `ui/settings/SettingsScreen.kt`：重複的「設定」區段標題改為「一般」；移除主題色塊重複的無障礙描述。
- `ui/components/ColorPicker.kt`：色點改用 `selectable` + `Role.RadioButton`，TalkBack 可辨識已選取狀態。
- `widget/TodoWidget.kt`：小工具依 App 的 `themeMode` 選擇淺色／深色，不再只跟隨系統。
- `notification/NotificationHelper.kt`、`util/DateTimeUtils.kt`：通知內容與日期區間分隔符改為字串資源或可傳入參數。
- `res/values/strings.xml`：新增一般區段、儲存失敗、上移／下移、分隔符與通知內容字串。
- `app/build.gradle.kts`：`versionCode` 9、`versionName` 1.4.1。
- 更新 README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 通知權限原本每次 `onCreate` 都呼叫，旋轉螢幕會再次跳出 → 以 `savedInstanceState` 與 DataStore 標記只詢問一次；後續狀態由設定頁顯示與引導。
- 儲存按鈕在狀態尚未重組前仍可點擊，快速連點可能建立兩筆 → 在 `save()` 入口檢查 `isSaving`，並在啟動協程前同步設定。
- 拖曳排序原本只能在畫面內換位 → 依拖曳中心與視窗上下緣距離調整自動捲動方向，並在捲動時補償拖曳位移。
- TalkBack 無法透過長按拖曳排序 → 在拖曳把手加入 `CustomAccessibilityAction` 的上移／下移動作。
- 小工具的 `ColorProviders` 原本只交給 Glance 依系統深色模式選擇 → 改依 `themeMode` 決定要傳入系統配對或固定同一套配色。

### 待辦／已知限制

- 目前仍無連接裝置或模擬器，deep link、通知權限、自動捲動、TalkBack 動作與小工具主題需實機驗證。
- 首次啟動仍需等待 DataStore 第一次讀取；系統列在該瞬間可能先以系統主題顯示。
- 手動排序自動捲動為固定速度，尚未依拖曳速度或裝置密度微調。
- v1.4.0 以前的舊小工具狀態仍保留 fallback 讀取。

### 日期與版本（本次）

- 日期：2026-11-10
- versionCode：9
- versionName：1.4.1

---

## 2026-11-09 - v1.4.0（versionCode 8）

### 目標

修正 App 主題與系統列顏色不一致的問題，讓小工具設定頁跟隨 App 主題，並把「動態色彩」開關改為可選的主題色彩。

### 變更

- `ui/theme/SystemBars.kt`（新增）：`applySystemBarStyle(darkTheme)` 以明確的 `SystemBarStyle` 套用狀態列／導覽列圖示與透明底色。
- `MainActivity.kt`、`widget/WidgetConfigActivity.kt`：以 `resolveDarkTheme()` 計算目前主題，於 `LaunchedEffect(darkTheme)` 重新套用系統列樣式。
- `widget/WidgetConfigActivity.kt`：改為收集 `settingsRepository.settings`，套用 App 的 `themeMode` 與 `themePalette`。
- `domain/ThemePalette.kt`（新增）：`DYNAMIC`、`BLUE`、`GREEN`、`PURPLE`、`ORANGE`、`PINK`、`TEAL`。
- `ui/theme/Color.kt`：新增各主題的 light/dark `ColorScheme`、`colorScheme()`、`colorSchemes()` 與 `previewColor`。
- `ui/theme/Theme.kt`：`MiniTodoTheme` 改以 `palette` 決定配色；新增純函式 `resolveDarkTheme()` 與 `effectivePalette()`。
- `data/prefs/SettingsRepository.kt`：`AppSettings.dynamicColor` 改為 `themePalette`；新增 `theme_palette` key，舊 `dynamic_color` 僅作一次性遷移。
- `ui/settings/SettingsScreen.kt`、`SettingsViewModel.kt`、`ui/AppNavHost.kt`：設定頁改為主題色彩列與色塊對話框。
- `widget/TodoWidget.kt`：小工具讀取設定並以所選主題色彩提供 `ColorProviders`。
- `SideEffectCoordinator.kt`：新增主題色彩變更監聽，切換色彩後立即刷新桌面小工具。
- `res/values/strings.xml`：移除動態色彩字串，新增主題色彩名稱。
- `app/build.gradle.kts`：`versionCode` 8、`versionName` 1.4.0。
- 新增 `ThemeTest` 驗證 `resolveDarkTheme()`。
- 更新 README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 無參數的 `enableEdgeToEdge()` 會依系統深色模式決定系統列圖示，導致 App 設為淺色但系統為深色時出現白字與黑色導覽列 → 改為依 App 的 `darkTheme` 明確指定 `SystemBarStyle`。
- 小工具設定頁原本直接使用系統主題，與 App 內設定不一致 → 改為收集 App 設定並套用相同主題與主題色彩。
- Material Icons Core 沒有 `Palette` 圖示 → 「跟隨桌布」以色相漸層圓點呈現，避免新增 icons-extended 依賴。
- 舊版預設 `dynamic_color=true`，直接移除會讓使用者配色改變 → 無 `theme_palette` 時依舊值遷移為 `DYNAMIC` 或 `BLUE`。

### 待辦／已知限制

- 仍需實機驗證 7 種主題色彩在淺色／深色下的對比度，以及三鍵／手勢導覽列的呈現。
- 桌面小工具的 light/dark 仍跟隨系統，不強制跟隨 App 的 `themeMode`。
- 首次啟動在 DataStore 讀取完成前，系統列可能先以系統主題顯示一帧。

### 日期與版本（本次）

- 日期：2026-11-09
- versionCode：8
- versionName：1.4.0

---

## 2026-11-09 - v1.3.0（versionCode 7）

### 目標

降低底層資料存取與背景工作的重複成本，並補強資料庫升級安全與 release 縮減效果。

### 變更

- `data/repository/TodoRepository.kt`：移除每次寫入都直接執行副作用的 `onDataChanged`，改為 `DataChange` 累積器 + `Channel.CONFLATED` 喚醒訊號；`reorderTodos()` 不再觸發提醒或小工具更新。
- `SideEffectCoordinator.kt`（新增）：單一協調器以 400ms 去抖動消費變更，連續操作只重排一次提醒；沒有小工具時跳過 `updateAll`。
- `data/local/TodoDao.kt`：新增提醒 JOIN 單筆查詢、有效提醒查詢、已排程項目 ID 查詢、`COUNT` 批次封存查詢、項目 `@Update` 與批次刪除。
- `TodoRepository.kt`：提醒改走單筆／有效查詢；批次封存改用 COUNT；編輯項目改為 diff / upsert，保留既有 ID 與完成時間。
- `notification/ReminderScheduler.kt`：改用有效提醒清單排程，並依已排程項目 ID 取消舊鬧鐘。
- `data/local/AppDatabase.kt`：schema 升為 v2、啟用 `exportSchema`、加入 `MIGRATION_1_2`（`archivedAt` 索引）；release 不再破壞性遷移，debug 仍保留。
- `data/local/Entities.kt`：`todos.archivedAt` 加索引。
- `MiniTodoApp.kt`、`MainActivity.kt`：啟動與回前景不再各自全量重排；改由變更管線處理，onResume 僅在精確鬧鐘權限狀態改變時要求重排。
- `domain/Models.kt`：`Todo`、`TodoItem`、`TodoSection` 加 `@Immutable`；`TodoOrganizer` 的分日邊界改為每輪只計算一次。
- `ui/todo/TodoListViewModel.kt`：每分鐘 ticker 對齊分鐘邊界並 `conflate()`。
- `app/proguard-rules.pro`：移除整包 `-keep`，改以 `@Keep` 標記 `ToggleItemAction` 與 `WidgetRefreshWorker`。
- `app/build.gradle.kts`：匯出 Room schema、`versionCode` 7、`versionName` 1.3.0。
- 新增 `ItemDiffTest` 單元測試；更新 CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 變更事件若只靠 `StateFlow` 搭配重置，會被等值去重而漏掉處理中的新事件 → 改用 `AtomicReference` 累積 + `Channel.CONFLATED` 喚醒，確保處理期間的新變更一定再被消費。
- 將提醒查詢改為只取有效項目後，完成／封存項目的舊鬧鐘不會再被取消 → 另加「已排程項目 ID」查詢，先取消所有舊鬧鐘再排程有效項目。
- `exportSchema` 先前關閉，沒有 v1 schema 可做自動遷移 → 以手動 `MIGRATION_1_2` 建立與 Room 產生命名一致的 `index_todos_archivedAt`。
- 編輯待辦原本刪除全部項目再重插，導致 ID 一直變動且完成時間遺失 → 改為 diff / upsert，由資料層依前次狀態保留 `completedAt`。

### 待辦／已知限制

- 目前無連接裝置或模擬器，v1→v2 遷移、通知重排、小工具勾選與 release R8 行為仍需實機驗證。
- v1 schema 未匯出，無法用 `MigrationTestHelper` 自動化驗證；本次以手動 SQL 與命名比對確認。
- 舊儲存格式（WidgetStore legacy / WidgetOptions）仍保留作為小工具 fallback。

### 日期與版本（本次）

- 日期：2026-11-09
- versionCode：7
- versionName：1.3.0

---

## 2026-11-09 - v1.2.3（versionCode 6）

### 目標

修正 Glance session 持續使用第一次渲染時捕捉的空待辦狀態，導致小工具即使成功儲存仍顯示「尚未選擇待辦」的問題。

### 變更

- `widget/TodoWidget.kt`：在 `provideContent` 內以 `currentState<Preferences>()` 讀取選擇，讓 Glance state 更新能觸發重組。
- `widget/TodoWidget.kt`：依待辦 ID 訂閱 `TodoRepository.observeTodo()`，以 Compose state 即時反映重新配置、標題、顏色、項目與完成狀態變化。
- `widget/TodoWidget.kt`：加入未選擇、載入中、待辦不存在與正常顯示四種明確狀態。
- `widget/WidgetStore.kt`：共用 `selectedTodoIdKey`，並將舊儲存讀取限制為 fallback。
- `widget/WidgetConfigActivity.kt`：避免非同步載入舊選擇時覆蓋使用者剛點選的待辦；讀取失敗時顯示提示。
- `app/build.gradle.kts`：`versionCode` 6、`versionName` 1.2.3。
- 更新 CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- `provideGlance()` 原本在 `provideContent` 外讀取待辦 ID 與 Room 資料；Glance 沿用既有 session 時，`update()` 只重組 content，不會重跑外層讀取，因此 content 一直保留首次捕捉的 `null` → 改在 composition 內讀取 `currentState`。
- Room 待辦資料也在外層只讀取一次，後續更新可能持續顯示舊資料 → 在 composition 內以 Flow `collectAsState()` 監聽。

### 待辦／已知限制

- 目前無連接裝置或模擬器，仍需實機驗證新增、原生重新配置、多個小工具與程序重啟流程。
- 舊儲存格式暫時保留作既有小工具 fallback。

### 日期與版本（本次）

- 日期：2026-11-09
- versionCode：6
- versionName：1.2.3

---

## 2026-11-09 - v1.2.2（versionCode 5）

### 目標

徹底修正新增與重新配置小工具後，選擇的待辦未顯示或未更新的問題。

### 變更

- `widget/WidgetStore.kt`：改用 Glance 官方 per-widget Preferences state，以 `GlanceId` 儲存每個小工具的 `selected_todo_id`。
- `widget/TodoWidget.kt`：設定 `PreferencesGlanceStateDefinition`，渲染時從所屬小工具的 Glance state 讀取待辦 ID。
- `widget/WidgetConfigActivity.kt`：先寫入 Glance state 並完成 `TodoWidget.update()`，成功後才關閉設定頁；失敗時保留畫面並顯示錯誤。
- `widget/WidgetOptions.kt`、`widget/WidgetStore.kt`：舊 DataStore 與 widget options 僅作既有小工具的一次性資料遷移。
- `res/values/strings.xml`：新增小工具更新失敗提示。
- `app/build.gradle.kts`：`versionCode` 5、`versionName` 1.2.2。
- 更新 CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 先前以平台 `appWidgetId` 手動維護 DataStore 與 widget options 兩份資料，實機上設定頁與 Glance 渲染流程仍可能讀到空值 → 改由 Glance 依 `GlanceId` 管理每個 widget instance 的專屬狀態。
- 更新失敗時仍回傳 `RESULT_OK` 並關閉設定頁，造成使用者看到設定成功但小工具內容未變 → 寫入或更新失敗時不再關閉，改為顯示錯誤並允許重試。

### 待辦／已知限制

- 目前無連接裝置或模擬器，小工具新增、重新配置、多個 instance 與程序重啟後的狀態仍需實機驗證。
- 舊儲存格式暫時保留作一次性遷移，確認已升級後可於後續版本移除。

### 日期與版本（本次）

- 日期：2026-11-09
- versionCode：5
- versionName：1.2.2

---

## 2026-11-09 - v1.2.1（versionCode 4）

### 目標

修正新增小工具後仍顯示「尚未選擇待辦」，並取消小工具內自訂「變更」按鈕，改用系統原生的「重新配置」入口。

### 變更

- `widget/TodoWidget.kt`：移除小工具底部的「變更」按鈕與空狀態的自訂「重新選擇」入口；空狀態改為提示長按小工具使用原生「重新配置」。
- `res/xml/todo_widget_info.xml`：加入 `android:widgetFeatures="reconfigurable"`，長按小工具即可重新配置。
- `AndroidManifest.xml`：`WidgetConfigActivity` 補上 `APPWIDGET_CONFIGURE` intent-filter 與 `excludeFromRecents`。
- `widget/WidgetConfigActivity.kt`：建立時先設定 `RESULT_CANCELED`；儲存後先等待 DataStore 寫入並直接更新小工具，再回傳 `RESULT_OK` 並結束，避免第一次建立時漏掉更新。
- `res/values/strings.xml`：更新小工具空狀態提示，移除不再使用的「變更／重新選擇」字串。
- `app/build.gradle.kts`：`versionCode` 4、`versionName` 1.2.1。
- 更新 CHANGELOG、RELEASE_NOTES、README 與本紀錄。

### 問題與解法

- 使用設定 Activity 時系統不會自動送出 `ACTION_APPWIDGET_UPDATE`，第一次建立需由設定頁自行更新；原本在 `finish()` 後才非同步刷新，容易錯過時機 → 改為在 `finish()` 前直接更新小工具。
- 自訂「變更」按鈕以 `NEW_TASK` 開啟設定頁，會併入 App 既有 task，結束後回到主畫面 → 移除自訂按鈕，改用系統原生「重新配置」。

### 待辦／已知限制

- 原生「重新配置」需 Android 12 以上；Android 11 以下仍會使用新增時的設定流程，無法長按重新配置。
- 小工具建立與重新配置流程仍需實機驗證。

### 日期與版本（本次）

- 日期：2026-11-09
- versionCode：4
- versionName：1.2.1

---

## 2026-11-09 - v1.2.0（versionCode 3）

### 目標

擴充待辦顏色選擇，修正編輯頁鍵盤空白與小工具變更待辦後內容不更新的問題，並縮小小工具「變更」按鈕。

### 變更

- `domain/Models.kt`：`TodoColor` 新增 `LIME`、`CYAN`、`INDIGO`、`DEEP_PURPLE`、`BROWN`、`BLUE_GREY`、`DEEP_ORANGE`，連同「無顏色」共 16 個選項。
- `ui/theme/Color.kt`：新增 7 個顏色常數、`todoColor()` 對應，並更新 `onTodoColor()` 的深色勾勾判斷。
- `ui/components/ColorPicker.kt`、`res/values/strings.xml`：新增 7 個顏色名稱與標籤對應。
- `ui/edit/TodoEditScreen.kt`：內容加上 `consumeWindowInsets(padding)` 後再 `imePadding()`，避免與 Scaffold 底部系統列留白重複計算。
- `widget/TodoWidget.kt`：待辦 ID 改為 DataStore 優先、widget options 備援；「變更」文字縮小為 12sp 並減少 padding。
- `widget/WidgetConfigActivity.kt`：初始選取與儲存皆以 DataStore 為主，寫入完成後才刷新小工具。
- `app/build.gradle.kts`：`versionCode` 3、`versionName` 1.2.0。
- 更新 README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 顏色圓點數量增加後 `FlowRow` 會自動換行，不需額外調整版面。
- 鍵盤與編輯內容間多出一段空白 → Scaffold 內距已含導覽列高度，`imePadding()` 又加了一次鍵盤高度；以 `consumeWindowInsets` 標記已消耗的內距後再補鍵盤高度。
- 小工具變更待辦後仍顯示舊內容 → `provideGlance` 優先讀取可能過期的 widget options，刷新成功但資料是舊的；改以 DataStore 為唯一優先來源，options 僅作備援。

### 待辦／已知限制

- 小工具刷新與鍵盤留白仍需實機驗證（目前無連接裝置或模擬器）。
- 顏色選項增至 16 個後，小螢幕可能顯示為 3 列。

### 日期與版本（本次）

- 日期：2026-11-09
- versionCode：3
- versionName：1.2.0

---

## 2026-11-09 - v1.1.0（versionCode 2）

### 目標

改善編輯待辦的操作體驗，並修正桌面小工具選擇待辦後無法正確顯示的問題。

### 變更

- `ui/components/ColorPicker.kt`：顏色選項由水平 `LazyRow` 改為會自動折行的 `FlowRow`。
- `ui/edit/TodoEditScreen.kt`：`LazyColumn` 加上 `imePadding()`，並在 Manifest 設定 `adjustResize`，讓鍵盤開啟時可捲到最底部。
- `ui/edit/TodoEditScreen.kt`：拖曳視覺效果（`zIndex` / `graphicsLayer`）改套用在整個 `ItemEditorCard`，拖曳把手只保留 `pointerInput`。
- `ui/edit/TodoEditScreen.kt`、`ui/todo/TodoListScreen.kt`：換位時以 `dragOffset += info.offset - target.offset` 補償位置，避免卡片瞬間跳位；非拖曳中的項目加上 `animateItem()`。
- `ui/todo/TodoListScreen.kt`：手動排序改為先在畫面本地更新順序，拖曳結束才寫入資料庫，避免資料庫 Flow 延遲造成抖動。
- `widget/WidgetOptions.kt`：新增以 `AppWidgetManager.updateAppWidgetOptions()` 同步保存小工具選擇的待辦。
- `widget/TodoWidget.kt`：小工具優先讀取 widget options，找不到才退回 DataStore。
- `widget/WidgetConfigActivity.kt`：儲存選擇後同時寫入 options 與 DataStore，並在 Activity 結束後以 application scope 重試刷新指定小工具。
- `widget/TodoWidget.kt`、`widget/WidgetConfigActivity.kt`：小工具底部新增「變更」按鈕，可重新開啟設定頁選擇其他待辦。
- `app/build.gradle.kts`：`versionCode` 2、`versionName` 1.1.0。
- 更新 README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 顏色選項使用 `LazyRow` 導致只能水平滑動 → 改用 `FlowRow` 自動換行。
- 鍵盤開啟後畫面底部被遮住 → `imePadding()` 搭配 `adjustResize` 讓內容可捲動。
- 拖曳時只有把手在移動 → 原本 `graphicsLayer` 誤套在 `IconButton`，改為套在卡片根節點。
- 每次換位都把位移歸零造成跳位 → 改用位置差補償，並在換位前先更新畫面本地順序。
- 小工具可能先渲染再寫入 DataStore，且設定完成後沒有可靠刷新 → 同步寫入 widget options，並在設定頁結束後延遲重試刷新指定 widget。

### 待辦／已知限制

- 目前仍無連接裝置或模擬器，尚未實機驗證鍵盤、拖曳手感與小工具變更流程。
- 拖曳排序尚未加入自動捲動（拖到畫面邊緣時不會自動捲動）。
- 小工具刷新重試最多約 5 秒；若裝置在此期間強制停止 App，可能需重新整理小工具。

### 日期與版本

- 日期：2026-11-09
- versionCode：2
- versionName：1.1.0

---

## 2026-11-09 - v1.0.0（versionCode 1）

### 目標

從零建立「Mini 待辦事項」Android App，一次完成待辦與清單項目管理、時間提醒、排序、封存及桌面小工具。

### 變更

- 建立 Gradle Kotlin DSL 專案、Version Catalog 與 wrapper；版本為 AGP 8.13.2、Kotlin 2.2.20、Compose BOM 2025.02.00、compileSdk/targetSdk 36、minSdk 26。
- `data/local`：Room 資料表 `todos`、`todo_items`（外鍵 CASCADE）、`TodoDao`、`AppDatabase`。
- `data/repository`：待辦 CRUD、完成狀態同步、封存／還原／批次封存、手動排序位置更新；每次寫入後重排提醒並刷新小工具。
- `data/prefs`：DataStore 儲存排序模式、主題模式與動態色彩。
- `domain`：`Todo`、`TodoItem`、`TodoColor`、`SortMode`、`TodoSection` 與 `TodoOrganizer`（逾期／今天／接下來／無日期／全部／已完成分區與排序）。
- `ui`：Material 3 主列表、編輯、封存、設定畫面；支援滑動完成／封存、Undo Snackbar、待辦內項目與主列表的手動拖曳排序、日期／時間／整天選擇。
- `notification`：AlarmManager 精確鬧鐘（未授權時退化為不精確）、通知完成／延後 10 分鐘、開機／時間／時區變更重排。
- `widget`：Glance 桌面小工具、待辦選擇 Activity、`appWidgetId → todoId` 映射、每日跨日刷新 Worker。
- 單元測試：分區與排序邏輯、日期時間與 09:00 提醒換算。
- 文件：README、CHANGELOG、RELEASE_NOTES 與本紀錄。

### 問題與解法

- 專案目錄沒有 Gradle 設定，無法直接產生 wrapper → 先建立 `settings.gradle.kts` 與建置檔，再以快取中的 Gradle 8.14.3 執行 `wrapper`。
- Glance 1.1.1 的元件命名為 `CheckBox`（大寫 B），與 Material3 的 `Checkbox` 不同 → 依實際 AAR API 調整。
- `Icons.Filled.ArrowBack`、`Icons.Filled.List` 已棄用 → 改用 `Icons.AutoMirrored.Filled.*`。
- Lint 回報受保護廣播可能被偽造 → `RescheduleReceiver` 加入允許 action 白名單檢查。
- `rescheduleAll()` 原本會取消所有 snooze 鬧鐘，導致其他資料異動時誤取消延後提醒 → 改為只取消一般提醒，snooze 保留至觸發或項目完成／刪除。
- ViewModel 事件原使用 `SharedFlow`，在畫面尚未訂閱時可能遺失（例如編輯不存在待辦）→ 改用 `Channel` + `receiveAsFlow()`。
- 主列表手動排序需與日期分區並存 → 手動模式改為單一「全部」區段加「已完成」區段，拖曳只影響未完成待辦。
- 授予精確鬧鐘權限後回到 App 不會自動重排 → `MainActivity.onResume` 重新校正提醒排程。
- 為符合「資料只留在裝置」的隱私說明，關閉 Android 自動備份（`allowBackup=false`）。
- 編譯器對 `LazyListItemInfo.key` 的 smart cast 失敗 → 先取出區域變數再判斷型別。

### 待辦／已知限制

- 目前環境沒有連接裝置或模擬器，尚未實機驗證通知到點、通知動作、重開機重排、時區變更、小工具新增／縮放／勾選與跨日刷新。
- 純日期項目以本地時區的午夜儲存，跨時區移動時日期可能位移。
- 精確鬧鐘受 Android 12+ 權限與 Play 政策限制；未授權時提醒可能延遲。
- 小工具透過資料異動與每日 Worker 刷新，非即時秒級更新。
- 尚未加入重複任務、標籤、搜尋與雲端同步。

### 日期與版本

- 日期：2026-11-09
- versionCode：1
- versionName：1.0.0
