# 系統架構與技術文件

本文件描述 Mini 待辦事項的技術架構、模組職責、資料模型、執行流程與建置方式。目標讀者為維護專案的開發者。

## 1. 技術概觀

| 類別 | 技術 |
| --- | --- |
| 語言 | Kotlin 2.2.20 |
| UI | Jetpack Compose + Material 3 |
| 導覽 | Navigation Compose |
| 狀態管理 | ViewModel + StateFlow + Channel |
| 本機資料庫 | Room 2.8.4（KSP） |
| 設定儲存 | DataStore Preferences 1.1.7 |
| 背景工作 | WorkManager 2.10.1 |
| 提醒排程 | AlarmManager + BroadcastReceiver |
| 桌面小工具 | Jetpack Glance 1.1.1 |
| 建置 | Gradle Kotlin DSL + Version Catalog |
| Java / JVM | 17 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 |

所有相依版本集中於 `gradle/libs.versions.toml`，`app/build.gradle.kts` 不直接寫死版本。

## 2. 分層架構

```text
┌─────────────────────────────────────────────────────────┐
│ UI Layer                                                │
│ Compose Screens（無狀態） + ViewModels（StateFlow/Event） │
└───────────────────────────┬─────────────────────────────┘
                            │ 呼叫 / 觀察 Flow
┌───────────────────────────▼─────────────────────────────┐
│ Domain Layer                                            │
│ Todo、TodoItem、TodoOrganizer、ItemDiff、ThemePalette     │
└───────────────────────────┬─────────────────────────────┘
                            │ 使用
┌───────────────────────────▼─────────────────────────────┐
│ Data Layer                                              │
│ TodoRepository、Room DAO、DataStore、BackupManager       │
└───────────────────────────┬─────────────────────────────┘
                            │ 平台整合
┌───────────────────────────▼─────────────────────────────┐
│ Platform Integration                                    │
│ AlarmManager / Notification / Glance / WorkManager       │
└─────────────────────────────────────────────────────────┘
```

### 2.1 UI Layer

- `ui/todo/TodoListScreen.kt`：待辦清單、分類區段、搜尋、滑動手勢、手動拖曳排序。
- `ui/edit/TodoEditScreen.kt`：新增／編輯待辦與清單項目、日期時間、顏色選擇。
- `ui/archive/ArchiveScreen.kt`：封存清單、還原、永久刪除。
- `ui/settings/SettingsScreen.kt`：排序、外觀、權限、備份、關於。
- `ui/components/ColorPicker.kt`：顏色選項。
- `ui/components/DateTimeField.kt`：日期、時間、全天切換。
- `ui/AppNavHost.kt`：路由與 ViewModel 建立。

畫面本身不直接存取資料層；所有狀態由 ViewModel 提供，所有操作透過回呼（callback）回傳 ViewModel。

### 2.2 Domain Layer

- `domain/Models.kt`
  - `Todo`、`TodoItem`、`TodoSection`、`SectionType`、`TodoColor`、`SortMode`、`ThemeMode`。
  - `TodoOrganizer`：分類與排序的純邏輯。
- `domain/ItemDiff.kt`：計算清單項目新增、更新、刪除差異，避免整批刪除重建。
- `domain/ThemePalette.kt`：主題色盤列舉。

Domain 層不依賴 Android framework，便於單元測試。

### 2.3 Data Layer

- `data/local/Entities.kt`：Room entity 與關聯查詢模型。
- `data/local/TodoDao.kt`：資料庫查詢與交易操作。
- `data/local/AppDatabase.kt`：資料庫建立與 migration。
- `data/repository/TodoRepository.kt`：Domain 與 Entity 轉換、交易、變更訊號。
- `data/prefs/SettingsRepository.kt`：DataStore 設定。
- `data/backup/BackupJson.kt`、`BackupManager.kt`：JSON 備份格式與檔案 I/O。

### 2.4 Platform Integration

- `notification/ReminderScheduler.kt`：排程與取消鬧鐘。
- `notification/ReminderReceiver.kt`：接收鬧鐘、通知動作、延後提醒。
- `notification/NotificationHelper.kt`：通知頻道與通知內容。
- `notification/RescheduleReceiver.kt`：開機、時間變更、時區變更、App 更新後重排。
- `widget/TodoWidget.kt`：Glance 小工具 UI 與互動。
- `widget/TodoWidgetReceiver.kt`：小工具生命週期。
- `widget/WidgetConfigActivity.kt`：選擇小工具顯示的待辦。
- `widget/WidgetRefreshWorker.kt`：每日背景刷新。
- `widget/WidgetStore.kt`：小工具選擇狀態與舊版資料遷移。

## 3. Application 組裝與生命週期

```text
MiniTodoApp (Application)
 └─ AppContainer
     ├─ AppDatabase
     ├─ TodoRepository
     ├─ SettingsRepository
     ├─ BackupManager
     ├─ WidgetStore
     └─ ReminderScheduler
 └─ SideEffectCoordinator（appScope 中執行）
```

`MiniTodoApp.onCreate()`：

1. 建立 `AppContainer`。
2. 建立通知頻道。
3. 排程每日小工具刷新工作。
4. 請求一次完整刷新（通知與小工具）。
5. 啟動 `SideEffectCoordinator` 觀察資料與主題變更。

`MainActivity`：

- 啟用 Edge-to-edge。
- 觀察設定 Flow，套用深色模式與主題色盤。
- 處理 `minitodo://todo/{todoId}` 深連結。
- 在 `onResume()` 偵測精確鬧鐘權限是否變更；變更時要求重新整理提醒。

## 4. 資料模型

### 4.1 Room Schema

```text
todos
  id            INTEGER PK AUTOINCREMENT
  title         TEXT
  colorKey      TEXT
  isCompleted   INTEGER
  createdAt     INTEGER
  completedAt   INTEGER NULL
  archivedAt    INTEGER NULL
  position      INTEGER

todo_items
  id            INTEGER PK AUTOINCREMENT
  todoId        INTEGER FK -> todos.id ON DELETE CASCADE
  title         TEXT
  startAt       INTEGER NULL
  startAllDay   INTEGER
  endAt         INTEGER NULL
  endAllDay     INTEGER
  isCompleted   INTEGER
  completedAt   INTEGER NULL
  position      INTEGER
```

索引：

- `todos.archivedAt`：加速封存／未封存查詢。
- `todo_items.todoId`：加速關聯查詢。

資料庫名稱：`minitodo.db`，目前版本 2。

Migration 1 → 2：建立 `index_todos_archivedAt`。Debug build 允許 destructive migration；Release / Prerelease 僅使用正式 migration。

### 4.2 Domain 模型

- `Todo` 聚合 `List<TodoItem>`，並提供 `completedItemCount`、`hasIncompleteItems`。
- `TodoItem` 可為空時間、僅開始、僅結束或完整區間。
- `TodoWithItems` 用於 Room `@Relation` 查詢。

### 4.3 時間表示

- 所有時間以 epoch milliseconds（`Long`）儲存。
- 顯示與分類時使用裝置目前時區 `ZoneId.systemDefault()`。
- 日期選擇器使用 UTC 午夜時間戳，避免時區造成日期偏移；轉換時使用 `datePickerMillis()` / `dateFromPickerMillis()`。
- 全天項目的提醒時間為當天 09:00（`ALL_DAY_REMINDER_HOUR`）。
- 全天項目的「有效結束」為當天 23:59:59.999，避免跨日誤判。

## 5. 資料流程

### 5.1 讀取

```text
Room Flow
  → TodoDao.observeActiveTodos()
  → TodoRepository.observeActiveTodos()
  → Entity → Domain 轉換
  → TodoListViewModel combine(settings, searchQuery, ticker)
  → TodoOrganizer.organize()
  → StateFlow<TodoListUiState>
  → Compose 畫面
```

`TodoListViewModel` 使用每分鐘 ticker 重新計算時間分類，讓「今天／逾期」在跨日或時間推進時自動更新。

### 5.2 寫入

```text
UI 操作
  → ViewModel
  → TodoRepository（withTransaction）
  → Room 寫入
  → notifyChanged()
  → DataChange(reminders, widget)
  → SideEffectCoordinator（debounce 400ms）
      ├─ ReminderScheduler.rescheduleAll()
      └─ TodoWidgetUpdater.updateAll()
```

`TodoRepository` 以 `AtomicReference` 合併待處理變更，並以 `Channel.CONFLATED` 發出訊號，避免短時間內重複排程。

### 5.3 清單項目更新

編輯既有待辦時，Repository 使用 `diffItems()` 比對現有與送出的項目：

- 新增：`id == 0` 或不存在於現有 ID。
- 更新：`id` 存在於現有資料。
- 刪除：現有但未出現在送出清單中的 ID。

這可保留既有項目 ID 與完成時間，避免不必要地重建資料。

## 6. 分類與排序演算法

### 6.1 分類

`TodoOrganizer.organize()` 先排除封存資料，再分出已完成與未完成。

未完成待辦的分類順序：

1. **逾期**：任一未完成項目的結束時間已過，或開始時間早於今天開始。
2. **今天**：最早開始時間小於明天開始。
3. **接下來**：最早開始時間在明天以後。
4. **無日期**：沒有任何開始時間。

「最早開始時間」優先取未完成項目的最小值；若沒有，才取所有項目的最小值。

### 6.2 排序

| 模式 | 比較方式 |
| --- | --- |
| `EARLIEST_START` | 最早開始時間 → 建立時間 → ID |
| `CREATED` | 建立時間（新到舊）→ ID（新到舊） |
| `MANUAL` | `position` → 建立時間 → ID |

手動模式的分類結果只有「全部」與「已完成」兩個區段；其他模式則包含逾期、今天、接下來、無日期、已完成。

已完成待辦一律依 `completedAt` 由新到舊排序，再依建立時間。

## 7. 完成狀態設計

- 待辦有清單項目時，`isCompleted` 由所有項目是否完成決定。
- 勾選待辦會同時更新所有項目。
- 勾選單一項目後，Repository 會重新計算所屬待辦是否全部完成。
- 完成時間 `completedAt`：
  - 首次完成時寫入目前時間。
  - 已完成狀態下再次更新時保留原完成時間。
  - 取消完成時設為 `null`。
- 沒有項目的待辦可使用「標記待辦完成」開關。

## 8. 提醒排程

### 8.1 排程來源

`TodoDao.getActiveReminderEntries()` 只取：

- 未封存的待辦。
- 未完成的待辦。
- 未完成的清單項目。
- 至少有一個開始或結束時間。

### 8.2 排程流程

```text
rescheduleAll()
  → 取消所有已排程項目（START / END）
  → 讀取有效提醒項目
  → 對每個項目的 startAt / endAt 建立 PendingIntent
  → 若觸發時間已過則略過
  → 可精確排程時使用 setExactAndAllowWhileIdle
  → 否則使用 setAndAllowWhileIdle
```

### 8.3 觸發與動作

`ReminderReceiver` 處理：

- `ACTION_REMINDER`：驗證項目仍有效且觸發時間合理，顯示通知。
- `ACTION_COMPLETE`：將項目標記完成並取消通知。
- `ACTION_SNOOZE`：安排 10 分鐘後的 snooze 鬧鐘並取消目前通知。

`snooze` 與一般提醒使用不同 URI，避免覆蓋原本的 PendingIntent。

### 8.4 重排時機

- App 資料變更後（`SideEffectCoordinator`）。
- 裝置開機完成。
- 系統時間變更。
- 時區變更。
- App 套件更新。
- `MainActivity.onResume()` 偵測到精確鬧鐘權限變更。

### 8.5 權限降級

Android 12 以上若無法使用精確鬧鐘，改用 `setAndAllowWhileIdle`。若排程時拋出 `SecurityException`，同樣退回非精確排程。這是刻意設計的降級路徑，避免因權限問題導致完全沒有提醒。

## 9. 通知設計

- 頻道 ID：`reminders`，重要性 `HIGH`。
- 通知 ID 由 `itemId` 與提醒類型組合，避免同一項目的開始／結束通知互相覆蓋。
- 通知內容包含項目名稱、事件類型與所屬待辦。
- 通知動作：
  - 完成
  - 延後 10 分鐘
- 點選通知使用 `minitodo://todo/{todoId}` 深連結開啟待辦。

## 10. 桌面小工具

### 10.1 顯示狀態

`WidgetTodoState` 分為：

- `Unselected`：尚未選擇待辦。
- `Loading`：讀取中。
- `Missing`：選擇的待辦已刪除或封存。
- `Ready`：正常顯示。

### 10.2 狀態儲存

- 每個小工具使用 Glance 的 `PreferencesGlanceStateDefinition` 儲存 `selected_todo_id`。
- `WidgetStore` 支援舊版以 `widget_todo_$widgetId` 或 `AppWidgetManager` options 儲存的資料，並自動遷移。
- 小工具刪除時清除舊版 DataStore 記錄。

### 10.3 更新策略

- 資料變更後由 `SideEffectCoordinator` 更新。
- 主題色盤變更後更新。
- `WidgetRefreshWorker` 每日約 00:05 執行一次背景刷新。
- `updatePeriodMillis = 0`，不使用系統高頻更新。

### 10.4 互動

- `ToggleItemAction` 為 Glance `ActionCallback`，使用 `@Keep` 避免 R8 移除。
- 點選小工具本體以 deep link 開啟 App。
- 每個小工具顯示一個待辦，最多 5 個項目。

## 11. 主題與外觀

- `MiniTodoTheme` 使用 Material 3 `MaterialTheme`。
- 深色模式由 `ThemeMode` 決定，`resolveDarkTheme()` 處理系統／淺色／深色。
- 主題色盤由 `ThemePalette` 決定：
  - `DYNAMIC`：Android 12 以上使用 dynamic color；舊版退回 `BLUE`。
  - `BLUE`、`GREEN`、`PURPLE`、`ORANGE`、`PINK`、`TEAL`：固定色盤。
- 系統列使用 `applySystemBarStyle()` 搭配 Edge-to-edge。
- 小工具使用相同色盤與深色模式設定。

## 12. 備份格式

格式版本：1。

```json
{
  "app": "MiniTodo",
  "version": 1,
  "exportedAt": 1762704000000,
  "todos": [
    {
      "title": "範例待辦",
      "color": "BLUE",
      "isCompleted": false,
      "createdAt": 1762704000000,
      "completedAt": null,
      "archivedAt": null,
      "position": 0,
      "items": [
        {
          "title": "範例項目",
          "startAt": 1762707600000,
          "startAllDay": false,
          "endAt": null,
          "endAllDay": false,
          "isCompleted": false,
          "completedAt": null,
          "position": 0
        }
      ]
    }
  ]
}
```

限制：

- 檔案大小上限 5 MB。
- 只接受 `version == 1`。
- 匯入時先刪除所有資料，再於同一交易中寫入。
- 匯入後重新通知提醒與小工具更新。

## 13. 執行緒與非同步

- UI 執行於 Main thread。
- Room 與 DataStore 操作透過 suspend / Flow 執行。
- `BackupManager` 使用 `Dispatchers.IO`。
- `SideEffectCoordinator` 與 `ReminderReceiver` 使用 Application 的 `appScope`（`SupervisorJob + Dispatchers.IO`）。
- `ReminderReceiver` 使用 `goAsync()` 延長廣播處理時間，並在 `finally` 呼叫 `finish()`。
- `WidgetRefreshWorker` 為 `CoroutineWorker`。

## 14. 錯誤處理

- 清單載入失敗：ViewModel 以 `catch` 回傳 `hasError = true`，畫面顯示錯誤狀態。
- 編輯儲存失敗：顯示 Snackbar「儲存失敗」。
- 匯出／匯入失敗：顯示對應 Snackbar。
- 通知權限被撤銷：`showReminder()` 檢查權限並捕捉 `SecurityException`。
- 精確鬧鐘不可用：降級為非精確排程。
- 小工具更新失敗：`TodoWidgetUpdater.updateWidget()` 回傳 `false`，設定畫面顯示 Toast。
- 深連結指向不存在的待辦：編輯 ViewModel 發出 `NotFound` 並返回。

## 15. 測試

單元測試位於 `app/src/test/java/com/vibecode/minitodo/`：

| 測試 | 內容 |
| --- | --- |
| `TodoOrganizerTest` | 分類、排序、逾期判斷 |
| `ItemDiffTest` | 清單項目差異計算 |
| `DateTimeUtilsTest` | 日期時間格式、全天處理、跨年顯示 |
| `ThemeTest` | 深色模式與色盤解析 |

執行：

```bash
./gradlew test
```

目前沒有 instrumentation / UI 測試。

## 16. 建置變體

| Build Type | R8 | 資源縮減 | 簽章 | 版本名稱後綴 | 用途 |
| --- | --- | --- | --- | --- | --- |
| `debug` | 否 | 否 | Debug | `-debug` | 開發 |
| `prerelease` | 是 | 是（繼承 release） | Debug 金鑰 | `-prerelease` | 測試發行 |
| `release` | 是 | 是 | 需自行設定 | 無 | 正式上架 |

`prerelease` 以 `initWith(getByName("release"))` 建立，因此啟用 `isMinifyEnabled` 與 `isShrinkResources`，但簽章改用 Debug 金鑰。

R8 保留規則：

- Glance `ActionCallback`：`widget.ToggleItemAction` 使用 `@Keep`。
- WorkManager：`widget.WidgetRefreshWorker` 使用 `@Keep`。
- Room、Compose、Glance、WorkManager 自帶 consumer rules。

## 17. 權限與安全

| 權限 | 使用位置 |
| --- | --- |
| `POST_NOTIFICATIONS` | `NotificationHelper`、編輯畫面權限請求 |
| `SCHEDULE_EXACT_ALARM` | `ReminderScheduler` |
| `RECEIVE_BOOT_COMPLETED` | `RescheduleReceiver` |
| `VIBRATE` | 通知震動 |

安全設計：

- 無網路權限。
- 備份使用 SAF，不申請儲存權限。
- 關閉 Android 自動備份（`allowBackup=false` 與 backup rules 全部排除）。
- 不記錄敏感資訊。
- 不包含 API Key、帳號或簽章金鑰。

## 18. 延伸開發指引

- 新增資料欄位：同步更新 Room entity、DAO、Domain model、BackupJson，並新增 migration 與 schema。
- 新增提醒類型：擴充 `ReminderType`、`ReminderContract`、`ReminderScheduler` 與 `ReminderReceiver`。
- 新增小工具選項：更新 `WidgetConfigActivity`、`WidgetStore` 與 `TodoWidget`。
- 新增設定：更新 `SettingsRepository`、`AppSettings`、`SettingsViewModel` 與 `SettingsScreen`。
- 新增畫面：在 `Routes` 與 `AppNavHost` 加入路由，並建立對應 ViewModel factory。
- 修改排序或分類：優先調整 `TodoOrganizer`，並補上單元測試。

## 19. 已知技術限制

- Room schema 版本目前為 2；後續欄位變更必須提供 migration，否則 release 版本會開啟失敗。
- 時間分類依裝置時區；跨時區可能造成日期位移。
- 精確提醒受系統權限與省電策略影響。
- 小工具更新非即時。
- 備份格式無加密，請妥善保管匯出的 JSON。
- Release build 未設定正式簽章，需由發行者自行提供。
