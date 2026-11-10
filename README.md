# Mini 待辦事項

以 Kotlin 與 Jetpack Compose 開發的離線待辦事項 App。支援多層清單項目、日期與時間、逾期分類、多種排序、搜尋、封存、JSON 備份、時間提醒，以及可勾選項目的 Jetpack Glance 桌面小工具。

所有資料只儲存在裝置本機，不需要帳號，也不會上傳到任何伺服器。

- 平台：Android 8.0（API 26）以上
- 語言：Kotlin
- UI：Jetpack Compose + Material 3
- 授權：MIT License
- 版本：1.0.0（預覽測試版 `1.0.0-prerelease`）

## 功能特色

### 待辦與清單項目

- 建立、編輯、刪除待辦事項，並可設定標題與顏色（16 種選項，含「無顏色」）。
- 每個待辦可新增多個清單項目；項目可各自設定標題、開始時間、結束時間與完成狀態。
- 支援整天事件；整天提醒預設在當天 09:00 觸發。
- 待辦完成狀態會依清單項目自動計算：所有項目完成時，待辦即完成；沒有項目時則使用「標記待辦完成」開關。
- 刪除清單項目後可透過 Snackbar 復原。

### 瀏覽、排序與搜尋

- 自動分類為「逾期」、「今天」、「接下來」、「無日期」與「已完成」。
- 排序方式：最早開始時間、建立時間、手動排序。
- 手動排序支援長按拖曳，亦可使用 TalkBack 的「上移／下移」自訂動作。
- 可依待辦標題或清單項目標題搜尋。
- 已完成區段可收合；標題列提供「封存已完成」。

### 封存

- 可封存單一待辦，封存後可立即復原。
- 封存頁可還原或永久刪除待辦；永久刪除前會二次確認。
- 可一次封存所有已完成的待辦。

### 提醒

- 清單項目的開始與結束時間可各自觸發通知。
- 通知提供「完成」與「延後 10 分鐘」動作。
- 使用 `AlarmManager` 精確鬧鐘；若未取得精確鬧鐘權限，會退回非精確排程。
- 開機、時間變更、時區變更或 App 更新後會自動重新排程。

### 桌面小工具

- 使用 Jetpack Glance 實作，每個小工具顯示一個指定待辦。
- 顯示待辦顏色、標題、前 5 個清單項目與完成進度。
- 可直接在小工具勾選清單項目，完成狀態與 App 同步。
- 可透過長按小工具「重新配置」更換顯示的待辦。
- 小工具會跟隨 App 的佈景主題、深色模式與主題色彩。

### 外觀與設定

- 深色模式：跟隨系統、淺色、深色。
- 主題色彩：跟隨桌布（Android 12+ 動態色彩）、預設藍、森林綠、薰衣草紫、暖陽橘、玫瑰粉、湖水綠。
- 設定頁集中管理排序、外觀、通知權限、精確鬧鐘、備份與還原、版本與隱私資訊。
- 自訂深色模式、動態色彩與 Edge-to-edge 系統列。

### 備份與還原

- 可將所有待辦（含封存）匯出為 JSON 檔案。
- 可從 JSON 備份還原；匯入會覆蓋目前所有資料，操作前會二次確認。
- 備份透過 Android 系統檔案選擇器（SAF）處理，App 不需要儲存權限。

## 快速開始

### 環境需求

- Android Studio（支援 AGP 8.13.2）
- JDK 17
- Android SDK 36
- Windows PowerShell 請使用 `.\gradlew.bat`；macOS / Linux 使用 `./gradlew`

### 建置 Debug APK

```bash
./gradlew assembleDebug
```

輸出：`app/build/outputs/apk/debug/app-debug.apk`

### 執行單元測試

```bash
./gradlew test
```

### 靜態檢查

```bash
./gradlew lint
```

### 建置測試發行版（Prerelease）

```bash
./gradlew assemblePrerelease
```

輸出：`app/build/outputs/apk/prerelease/app-prerelease.apk`

`prerelease` 特性：

- 啟用 R8 程式碼壓縮與資源縮減
- 使用 Debug 金鑰簽署
- 版本名稱附加 `-prerelease`
- 僅供測試，不可作為正式上架版本

### 建置正式版（Release）

```bash
./gradlew assembleRelease
```

正式版需要自行設定正式簽章金鑰；本專案不包含任何金鑰檔案。

## 專案結構

```text
app/src/main/java/com/vibecode/minitodo/
  MainActivity.kt            進入點、Edge-to-edge、深連結與權限狀態偵測
  MiniTodoApp.kt             Application、AppContainer 依賴組裝
  SideEffectCoordinator.kt   資料變更後的通知重排與小工具更新
  data/
    local/                   Room entities、DAO、資料庫與 migration
    prefs/                   DataStore 設定
    repository/              待辦資料儲存、交易與變更訊號
    backup/                  JSON 匯出／匯入
  domain/                    領域模型、分類與排序、項目差異計算、主題色盤
  notification/              AlarmManager 排程、通知、開機重排
  widget/                    Glance 桌面小工具與設定 Activity
  ui/
    todo/                    待辦清單畫面與 ViewModel
    edit/                    新增／編輯待辦畫面與 ViewModel
    archive/                 封存畫面與 ViewModel
    settings/                設定畫面與 ViewModel
    theme/                   Material 3 主題、色盤與系統列
    components/              顏色選擇器、日期時間欄位
  util/                      日期時間工具
app/src/test/                單元測試
app/schemas/                 Room schema
docs/                        使用指南、架構、設計、隱私與建置文件
```

## 文件

- [使用指南](docs/USER_GUIDE.md)
- [系統架構與技術文件](docs/ARCHITECTURE.md)
- [系統設計文件](docs/DESIGN.md)
- [隱私說明](docs/PRIVACY.md)
- [建置與發行指南](docs/BUILD_AND_RELEASE.md)
- [開發紀錄](docs/DEV_LOG.md)
- [版本異動](CHANGELOG.md)
- [版本說明](RELEASE_NOTES.md)

## 資料與隱私

- 待辦、封存與設定資料皆儲存在裝置本機（Room + DataStore）。
- App 不包含帳號、廣告、分析或追蹤 SDK。
- App 關閉 Android 自動雲端備份；跨裝置搬移請使用內建的 JSON 匯出／匯入。
- 詳細說明請見 [隱私說明](docs/PRIVACY.md)。

## 權限

| 權限 | 用途 |
| --- | --- |
| `POST_NOTIFICATIONS` | Android 13 以上顯示提醒通知 |
| `SCHEDULE_EXACT_ALARM` | 精確時間提醒；未授權時改用非精確排程 |
| `RECEIVE_BOOT_COMPLETED` | 裝置重新開機後重新排程提醒 |
| `VIBRATE` | 通知震動 |

## 技術版本

| 項目 | 版本 |
| --- | --- |
| Kotlin | 2.2.20 |
| Android Gradle Plugin | 8.13.2 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 |
| Jetpack Compose BOM | 2025.02.00 |
| Material 3 | 由 Compose BOM 管理 |
| Room | 2.8.4 |
| DataStore Preferences | 1.1.7 |
| WorkManager | 2.10.1 |
| Jetpack Glance | 1.1.1 |
| Java / JVM Target | 17 |

所有版本集中管理於 `gradle/libs.versions.toml`。

## 已知限制

- 純日期項目以裝置目前時區儲存；跨時區移動時可能產生日期位移。
- 精確提醒需要使用者授權；未授權或系統省電限制下，提醒可能延遲。
- 桌面小工具非即時秒級更新，於資料變更、主題變更或每日背景工作時刷新。
- 備份匯入採「覆蓋全部資料」策略，無法合併兩份備份。
- 本專案目前不提供雲端同步與多裝置即時同步。

## 授權

本專案原始碼採用 [MIT License](LICENSE)。

App 使用之開放原始碼元件依其原授權提供，主要為 Apache License 2.0。

## 版本

- `versionName`：1.0.0
- `versionCode`：13
- 首個 GitHub 公開發行版本為 `1.0.0-prerelease`，屬預覽測試版，非正式上線版本。
