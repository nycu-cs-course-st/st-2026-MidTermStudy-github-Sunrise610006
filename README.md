# Metamorphic Testing for HMAC-Signed URLs

以下內容由 AI 整理。
## 1. Overview

本專案為軟體測試課程的期中研究實作，主要研究 **Metamorphic Testing（MT）** 在 Web 安全測試中的應用，並參考論文 *Metamorphic Security Testing for Web Systems* 提出的 **Security Metamorphic Relation Language（SMRL）**。

本實驗自行建立一個以 HMAC-SHA256 驗證資源存取權限的簡易 Web Server，透過自訂的 Metamorphic Relation（MR），測試 HMAC 驗證碼是否正確綁定指定資源。

實驗包含兩個版本：

- **Correct Version**：驗證 HMAC 是否與請求的資源相符。
- **Faulty Version**：刻意引入資源綁定缺陷，只要 HMAC 對應任一合法資源，就允許存取目前請求的資源。

本專案的目的是展示如何使用 SMRL 定義安全性質、自動產生測試輸入，並偵測 Web 系統中的安全性缺陷。

## 2. Project Structure

```text
MT-HMAC-URLs/
├── HMAC-URLs/
│   ├── src/
│   │   └── LocalMockServer.java
│   ├── test/
│   │   ├── HmacResourceBinding.smrl
│   │   ├── TestHmac.java
│   │   └── HmacHttpCheck.java
│   ├── data/
│   │   ├── config.json
│   │   └── manual.json
│   ├── tools/
│   │   ├── eclipse-dsl-clean/
│   │   ├── SMRL-Plugin/
│   │   ├── SMRL-Windows.jar
│   │   └── chromedriver.exe
│   └── build/
├── ICST2020/
└── README.md
```

主要檔案說明：

| 檔案 | 說明 |
|---|---|
| `LocalMockServer.java` | 自製 HMAC-Signed URL Web Server，包含 Correct / Faulty Mode |
| `HmacResourceBinding.smrl` | 自訂的資源綁定 MR |
| `TestHmac.java` | 使用 JUnit 與 SMRL Framework 執行 MR |
| `HmacHttpCheck.java` | 驗證 Web Server 預期 HTTP 行為的輔助程式 |
| `manual.json` | 提供 SMRL 的 Source Input |
| `config.json` | SMRL 測試資料及錯誤頁面辨識設定 |
| `ICST2020/` | SMRL Eclipse Plugin 編譯 MR 使用的專案 |

其中 `tools/` 為本機實驗所使用的第三方工具，`build/` 為 Java 編譯結果。

## 3. Test Scenario

Web Server 提供以下端點：

```text
http://127.0.0.1:8000/download
```

使用 `file` 指定資源，並透過 `sig` 提供對應的 HMAC。

本實驗測試的主要安全性質為：

**資源 A 的合法 HMAC 不應授權存取資源 B。**

Source Input：

```text
/download?file=A&sig=SignatureA
```

Follow-up Input：

```text
/download?file=B&sig=SignatureA
```

MR 保留原始 HMAC，僅修改請求中的資源參數，並檢查 Follow-up Input 是否遭到拒絕。

## 4. Environment

本實驗使用以下環境：

| 項目 | 版本／工具 |
|---|---|
| OS | Windows |
| Java | OpenJDK 8 |
| IDE | Eclipse IDE for Java and DSL Developers 2020-06 |
| SMRL | 論文作者提供的 SMRL Eclipse Plugin 與 Framework |
| Browser Automation | Selenium / ChromeDriver |
| Test Framework | JUnit 4.13 |

需安裝 JDK 8，以及與 ChromeDriver 相容的 Chrome 版本。若下載的專案包含完整 `tools/`，可以使用其中的 Eclipse 與工具檔案。

## 5. How to Run

以下指令在 Windows PowerShell 中執行。

### Step 1: Compile the Web Server

進入 `HMAC-URLs` 目錄：

```powershell
cd HMAC-URLs

New-Item -ItemType Directory -Force build | Out-Null
javac -d build src\LocalMockServer.java
```

### Step 2: Compile the MR and JUnit Test

使用 Eclipse SMRL Plugin 將 `HmacResourceBinding.smrl` 轉換為 `HmacResourceBinding.java`。

本實驗生成的 Java MR 位於：

```text
ICST2020/src-gen/smrl/mr/hmac/HmacResourceBinding.java
```

接著在 `HMAC-URLs` 目錄設定 Classpath：

```powershell
$plugins = ".\tools\eclipse-dsl-clean\eclipse\plugins"

$junit = (Get-ChildItem $plugins -Filter "org.junit_4*.jar" |
    Select-Object -First 1).FullName

$hamcrest = (Get-ChildItem $plugins -Filter "org.hamcrest.core_*.jar" |
    Select-Object -First 1).FullName

$cp = "build;.\tools\SMRL-Windows.jar;$junit;$hamcrest"
```

編譯測試程式：

```powershell
javac -cp $cp -d build `
    test\TestHmac.java `
    ..\ICST2020\src-gen\smrl\mr\hmac\HmacResourceBinding.java
```

### Step 3: Run the Correct Version

在第一個終端機執行：

```powershell
java -cp build LocalMockServer
```

保持伺服器執行，在第二個 PowerShell 終端機設定相同的 Classpath，然後執行：

```powershell
java -cp $cp org.junit.runner.JUnitCore TestHmac
```

預期 JUnit 結果：

```text
OK (1 test)
```

### Step 4: Run the Faulty Version

先停止 Correct Version，再執行：

```powershell
java -cp build LocalMockServer --faulty
```

使用相同的 JUnit 測試：

```powershell
java -cp $cp org.junit.runner.JUnitCore TestHmac
```

預期偵測到 MR 違反：

```text
FAILURES!!!
Tests run: 1, Failures: 1
```

注意：測試前需確認 `manual.json` 中的合法 HMAC 與伺服器目前使用的金鑰一致。

## 6. Experimental Results

| 測試項目 | Correct Version | Faulty Version |
|---|---|---|
| A + Signature A | 200 OK | 200 OK |
| B + Signature A | 403 Forbidden | 200 OK |
| MR | Satisfied | Violated |
| JUnit | PASS | FAIL |

在本次實驗中，SMRL 成功產生修改資源參數的 Follow-up Input，並在 Faulty Version 中記錄 MR 違反。

這表示自訂 MR 能夠在此測試情境下偵測 HMAC 與請求資源未正確綁定的安全性缺陷。

## 7. Known Limitations

- 本實驗使用手動建立的 JSON 提供 Source Input，未使用 Crawljax 自動探索網站。
- SMRL Framework 在 Windows 上可能因 URL 包含 `?` 等字元，而無法正常儲存 HTML 快照。
- 原始 Framework 使用的 `killall` 指令不適用於 Windows，可能產生程序清理錯誤。
- 目前僅測試指定的 HMAC Resource Binding 情境，並未涵蓋其他 Web 安全漏洞。

## 8. References

- P. X. Mai, F. Pastore, A. Goknil and L. Briand, "Metamorphic Security Testing for Web Systems," 2020 IEEE 13th International Conference on Software Testing, Validation and Verification (ICST), Porto, Portugal, 2020, pp. 186-197, doi: 10.1109/ICST46399.2020.00028.
- SMRL Official Website: https://sntsvv.github.io/SMRL/
- SMRL Eclipse Plugin: https://github.com/SNTSVV/SMRL_EclipsePlugin

本專案中的 HMAC Web Server 與 HmacResourceBinding MR 為課程實作；SMRL Framework、Eclipse、ChromeDriver 等第三方工具的著作權與授權歸各自原作者所有。
