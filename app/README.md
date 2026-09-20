# Shortcut

Android-приложение для автоматизаций от [bina.co](https://bina.co). Сценарий — это **обычный JavaScript-файл**: он запускается по событию (зарядка, Wi-Fi, Bluetooth, загрузка телефона…) и управляет телефоном через встроенный SDK — камера, уведомления, приложения, сеть, датчики и т. д.

Сценарий можно написать руками, импортировать из `.js`, сгенерировать через AI (свой ключ) или подправить значения в визуальном конструкторе. Во всех случаях источником истины остаётся JS-текст.

```js
on("charging", async () => {
    await camera.takePhoto({ camera: "front" });
    await wait(1000);
    await camera.takePhoto({ camera: "front" });
});
```

## Сборка

Нужно:

| | |
|---|---|
| Android Studio | свежая версия, поддерживающая AGP 9.4 |
| JDK | 17 |
| Gradle | 9.6.0 или новее |
| Android SDK | Platform **37**, Build-Tools **36.0.0** |

Проект — один Gradle-модуль: эта папка одновременно корень сборки и приложение (`settings.gradle.kts` лежит рядом с `src/`).

Gradle Wrapper настроен на Gradle 9.6.0 (`gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`), но бинарного `gradle-wrapper.jar` в репозитории нет. Поэтому:

- **Android Studio** открывает проект сразу: она читает `gradle-wrapper.properties` и сама скачивает Gradle 9.6.0.
- **Из терминала** сначала один раз создайте jar: `gradle wrapper --gradle-version 9.6.0` (или задача `wrapper` в панели Gradle: Tasks → build setup). Команда заодно заменит `gradlew` и `gradlew.bat` официальными копиями Gradle. После этого закоммитьте `gradle/wrapper/gradle-wrapper.jar`.

Собрать debug-версию:

```
./gradlew assembleDebug
```

`minSdk 26`, `targetSdk 36`, `compileSdk 37`. Версия приложения задаётся в `build.gradle.kts` (`versionName`) и должна совпадать с `AppConfig.APP_VERSION_NAME`.

## Структура

Пакет `com.sunflower.shortcut`:

| Пакет | Что внутри |
|---|---|
| `ui/` | Compose-экраны: главная, автоматизации, AI, конструктор, настройки, помощь, о приложении; навигация и тема |
| `automation/` | движок (`AutomationEngine`), запуск (`ExecutionCoordinator`, `JsRuntime`), триггеры, планировщик на WorkManager, проверка разрешений, журнал запусков |
| `javascript/` | парсер, AST, анализатор, валидатор, аудит безопасности, экспорт |
| `sdk/` | 16 модулей, доступных сценарию: `android`, `apps`, `ui`, `camera`, `location`, `files`, `network`, `notifications`, `audio`, `bluetooth`, `wifi`, `sensors`, `media`, `contacts`, `phone`, `system` |
| `accessibility/` | AccessibilityService для модуля `ui.*` |
| `android/` | приёмник загрузки, сервис мониторинга триггеров, интенты, системные помощники |
| `ai/` | провайдеры (Gemini, Groq, OpenRouter, свой OpenAI-совместимый), сборка промпта, генерация кода, история |
| `constructor/` | `FlowBuilder`, `FlowGraph`, `FlowNode`, `EditableValue` |
| `data/` | Room (`database/`, `models/`), DataStore (`datastore/`), репозитории |
| `permissions/` | запрос разрешений по требованию |
| `sharing/` | отправка `.js`-файла в другие приложения |
| `utils/` | константы приложения, форматирование дат, расширения `Context` |

Ресурсы: `res/drawable` (иконка-молния и иконка уведомлений), `res/mipmap-anydpi` (адаптивная иконка), `res/values` (+ `values-night`), `res/xml` (конфиг Accessibility, FileProvider, правила бэкапа, сетевая политика).

## Как это работает

1. **Разбор.** Текст сценария → `JsParser` → AST → `ScenarioValidator` (структура `on(событие, обработчик)` и допустимые обращения) → `ScenarioSecurityAuditor` → `ScenarioAnalyzer` (название, триггер, шаги, нужные разрешения).
2. **Установка.** Сценарий сохраняется в Room; его триггер регистрирует `TriggerRegistry`. События идут через динамические приёмники, а `TriggerMonitorService` (foreground) поднимается только пока есть сценарии, которым он нужен. Единственный приёмник в манифесте — загрузка телефона.
3. **Запуск.** Триггер → `AutomationEngine` → `ExecutionCoordinator` → `JsRuntime`. Собственного JS-движка в APK нет: исполняет Chromium через headless WebView, по одному новому WebView на запуск. Вызовы SDK идут в Kotlin по `NativeBridge` и попадают в `SdkModuleDispatcher`.
4. **Конструктор.** `FlowBuilder` строит граф из сохранённого JS. Правка значения в конструкторе не хранится отдельно, а патчит сам текст (`ScenarioValuePatcher`), после чего код заново проверяется.

Триггеры: `charging`, `charging_disconnected`, `battery_low`, `wifi_connected`, `wifi_disconnected`, `bluetooth_connected`, `bluetooth_disconnected`, `boot_completed`, `manual`.

## AI (свой ключ)

Приложение не содержит ключей: пользователь подставляет свой ключ Gemini, Groq или OpenRouter либо адрес и модель собственного OpenAI-совместимого сервера. Ключи лежат в DataStore и исключены из облачного бэкапа и переноса на другое устройство (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).

## Решения, которые не очевидны из кода

- **Foreground-сервис имеет тип `specialUse`, а не `dataSync`.** Начиная с Android 15 сервис `dataSync` живёт не дольше 6 часов в сутки и не может стартовать из приёмника загрузки. Для публикации в Google Play тип нужно описать в Play Console.
- **Разрешён HTTP без шифрования** (`network_security_config.xml`): цели сценариев — устройства в локальной сети без HTTPS-сертификата. Встроенные AI-провайдеры работают по `https://`.
- **`canRetrieveWindowContent="true"`** в `accessibility_service_config.xml` обязателен: возможности службы нельзя выдать из кода, без них `ui.*` ничего не находит.
- **У debug-сборки тот же `applicationId`**: проверка службы Accessibility сравнивает имя пакета `com.sunflower.shortcut` с точностью до строки.
- **R8 в release выключен.** Перед включением добавьте правило для моста WebView: `-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }` — иначе сценарии перестанут работать только в release.
- **Объявлены только используемые разрешения.** `RECORD_AUDIO` и `READ_MEDIA_*` намеренно отсутствуют: `audio.*` только воспроизводит, `files.*` работает в собственной папке приложения.

## Известные ограничения

- В настройках нет экрана для ввода AI-ключа и адреса своего провайдера: сеттеры в `SettingsDataStore` есть, интерфейса пока нет.
- `PermissionMapping`: `PHONE` не включает `CALL_PHONE`, который проверяет `phone.call`; `LOCATION` требует точную и приблизительную геолокацию одновременно, хотя `location.*` принимает любую.
- В обзоре разрешений есть «Микрофон» и «Файлы», но SDK их не использует.
- Камеру и геолокацию из фонового сервиса Android 11+ может блокировать независимо от манифеста — проверьте `camera.takePhoto` на реальном устройстве.

## Контакты и документы

- Разработчик: bina.co, hello@bina.co
- Документация: https://bina.co/shortcut/docs
- Политика конфиденциальности: https://bina.co/shortcut/privacy
- Условия использования: https://bina.co/shortcut/terms
