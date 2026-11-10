# 建置與發行指南

本文件說明 Mini 待辦事項的開發建置、測試、簽章與 GitHub Release 流程。

## 1. 環境需求

| 項目 | 版本 |
| --- | --- |
| JDK | 17 |
| Android SDK | 36 |
| Android Gradle Plugin | 8.13.2 |
| Gradle Wrapper | 由專案提供 |
| Git | 建議 2.40 以上 |
| GitHub CLI（發行用） | 2.x |

Windows PowerShell 請使用 `.\gradlew.bat`；macOS / Linux 使用 `./gradlew`。

## 2. 建置變體

| Build Type | R8 | 資源縮減 | 簽章 | 版本名稱後綴 | 輸出 |
| --- | --- | --- | --- | --- | --- |
| `debug` | 否 | 否 | Debug | `-debug` | `app-debug.apk` |
| `prerelease` | 是 | 是 | Debug 金鑰 | `-prerelease` | `app-prerelease.apk` |
| `release` | 是 | 是 | 需設定 | 無 | `app-release-unsigned.apk` |

## 3. 常用指令

```bash
# 清理
./gradlew clean

# 建置 Debug
./gradlew assembleDebug

# 執行單元測試
./gradlew test

# Android Lint
./gradlew lint

# 建置測試發行版（R8 + Debug 簽章）
./gradlew assemblePrerelease

# 建置正式版（需簽章設定）
./gradlew assembleRelease
```

APK 輸出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/prerelease/app-prerelease.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

## 4. Prerelease 設定

`app/build.gradle.kts` 中的 `prerelease` 以 `release` 為基礎：

```kotlin
create("prerelease") {
    initWith(getByName("release"))
    isMinifyEnabled = true
    signingConfig = signingConfigs.getByName("debug")
    versionNameSuffix = "-prerelease"
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
    )
}
```

特性：

- 啟用 R8 程式碼壓縮。
- 繼承 release 的資源縮減設定。
- 使用 Android Debug 金鑰簽署，可直接安裝測試。
- 版本名稱附加 `-prerelease`。
- 不可作為正式上架版本。

## 5. R8 與保留規則

大部分保留規則由函式庫自帶的 consumer rules 提供。專案內需明確保留的反射入口：

- `widget.ToggleItemAction`（Glance `ActionCallback`）
- `widget.WidgetRefreshWorker`（WorkManager）

兩者皆以 `@Keep` 標記。`app/proguard-rules.pro` 僅保留說明，不重複定義函式庫規則。

若新增以下類型的類別，請確認 R8 不會移除：

- 由系統或函式庫反射建立的元件。
- WorkManager Worker。
- Glance ActionCallback。
- BroadcastReceiver、Service、Activity（通常由 Manifest 保留）。

## 6. 簽章

### Debug

Android Gradle Plugin 自動使用本機 Debug 金鑰。無需額外設定。

### Prerelease

使用 Debug 金鑰簽署，適合內部測試與 GitHub Pre-release。不建議長期保存重要資料。

### Release

專案未提供正式簽章設定。發行前請自行建立 keystore，並以安全方式提供給建置環境。切勿將 keystore 或密碼提交到版本控制。

建議的 `keystore.properties`（不納入版本控制）：

```properties
storeFile=../keystore/release.jks
storePassword=***
keyAlias=***
keyPassword=***
```

並在 `app/build.gradle.kts` 讀取後設定 `signingConfigs`。目前專案維持未設定狀態，避免誤將測試金鑰當作正式簽章。

## 7. 版本規則

- `versionName`：`主.次.修`。
- `versionCode`：每次功能變更遞增。
- 版本設定位於 `app/build.gradle.kts` 的 `defaultConfig`。
- 目前版本：`versionName = "1.0.0"`、`versionCode = 13`。
- `prerelease` 會輸出 `1.0.0-prerelease`。

更新版本時請同步更新：

- `README.md`
- `CHANGELOG.md`
- `RELEASE_NOTES.md`
- `docs/DEV_LOG.md`

## 8. 測試與檢查

發行前建議執行：

```bash
./gradlew test
./gradlew lint
./gradlew assemblePrerelease
```

檢查項目：

- 單元測試全數通過。
- Lint 無阻斷性錯誤。
- APK 可安裝並啟動。
- 新增／編輯／完成／封存流程正常。
- 通知與精確鬧鐘權限流程正常。
- 小工具可新增、勾選、重新配置。
- 備份匯出與匯入正常。
- 深色模式與主題切換正常。

## 9. GitHub 發行流程

### 9.1 首次建立儲存庫

```bash
git init
git config user.name "mark216tw"
git config user.email "mark216tw@gmail.com"
git add .
git commit -m "Initial commit"
gh repo create mark216tw/android-minitodo-01 --public --source=. --remote=origin --push
```

### 9.2 建立 Pre-release

```bash
./gradlew assemblePrerelease
gh release create v1.0.0-prerelease \
  app/build/outputs/apk/prerelease/app-prerelease.apk \
  --title "v1.0.0-prerelease" \
  --prerelease \
  --notes "此為 Pre-release 測試版本，非正式上線版本。"
```

Windows PowerShell 可將換行改為單行，或使用反引號續行。

### 9.3 Release 注意事項

- Pre-release 必須勾選「Pre-release」。
- 說明中需明確標示「測試版本，非正式上線版本」。
- 確認 APK 檔名為 `app-prerelease.apk`。
- 確認版本名稱顯示 `1.0.0-prerelease`。
- 確認儲存庫授權為 MIT License。

## 10. 發行前檢查清單

- [ ] `versionCode` 已遞增。
- [ ] `versionName` 正確。
- [ ] 文件與版本資訊一致。
- [ ] `./gradlew test` 通過。
- [ ] `./gradlew lint` 無阻斷錯誤。
- [ ] `./gradlew assemblePrerelease` 成功。
- [ ] APK 檔案存在且可安裝。
- [ ] 無 `local.properties`、keystore 或建置產物被提交。
- [ ] README、CHANGELOG、RELEASE_NOTES、DEV_LOG 已更新。
- [ ] GitHub Release 已標記為 Pre-release。
- [ ] 授權顯示為 MIT License。

## 11. 常見問題

### Q：`prerelease` APK 可以上架嗎？

不行。`prerelease` 使用 Debug 金鑰簽署，僅供測試。正式上架需使用 `release` 與正式簽章。

### Q：為什麼 `assembleRelease` 產出 unsigned APK？

專案未設定正式簽章，避免誤用測試金鑰。請依第 6 節自行設定。

### Q：R8 造成執行時崩潰怎麼辦？

先確認反射入口是否已保留。可暫時在 `proguard-rules.pro` 加入保留規則，並在 `DEV_LOG.md` 記錄原因。不要任意關閉 R8。

### Q：可以提交 APK 到 Git 儲存庫嗎？

不建議。`build/` 已列入 `.gitignore`。APK 應透過 GitHub Release 附件提供，而不是 commit 進版本控制。
