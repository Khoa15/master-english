# Master English

Ứng dụng học tiếng Anh cho Android, viết bằng **Java 17**, dành cho người đi làm trong ngành IT.
Mỗi ngày vài phút, người học đi qua lộ trình từ A1 đến C2 gồm từ vựng, ngữ pháp, nghe và đọc, được
nhắc học đúng giờ và có mascot AI đối thoại, chấm lỗi và ghi nhớ điểm yếu.

## Tính năng

- **Onboarding và đăng nhập:** 3 màn giới thiệu với mascot Cú Minh, khảo sát mục tiêu, trình độ và thời
  lượng học mỗi ngày. Đăng nhập Email/Mật khẩu hoặc Google (Firebase), hoặc học thử không cần tài khoản.
- **Lộ trình zig-zag:** mỗi stage là một cấp CEFR, các node từ vựng, ngữ pháp, nghe, đọc xen kẽ và kết
  thúc bằng boss. Node có trạng thái khóa, đang học (nhịp đập), hoàn thành (viền vàng) và số sao.
- **Bài học 5 dạng câu hỏi:** trắc nghiệm, điền từ, nối cặp, sắp xếp câu, và nói (SpeechRecognizer).
  Đúng hoặc sai có âm thanh, rung và phản ứng mascot. Kết thúc có pháo giấy Konfetti, XP và đá quý.
- **Ôn tập:** flashcard theo thuật toán SuperMemo-2, Weakness Clinic hiển thị các mẫu đang yếu
  (`mastery_score < 0.6`), luyện nghe có đổi tốc độ và karaoke transcript, luyện đọc có balloon tra từ.
- **Hồ sơ:** cấp Bronze/Silver/Gold/Diamond, streak, lưới huy hiệu lật 3D, biểu đồ 7 ngày, cài đặt âm
  thanh, giờ nhắc, độ khó AI, đồng bộ và đăng xuất.
- **Mascot AI (Gemini):** nhập vai thử thách theo điểm yếu, bắt lỗi trực tiếp và ghi vào Weakness Clinic.
  Có bộ nhớ dài hạn (mục tiêu, lỗi hay gặp) đưa vào system prompt.
- **Offline-first:** mọi dữ liệu đọc từ Room. Đồng bộ với máy chủ MySQL chạy nền bằng WorkManager.

## Công nghệ

Java 17 · Clean Architecture + MVVM · Single Activity với Navigation Component · Hilt · Room (kèm
converter JSON) · Retrofit 2 + OkHttp 3 · WorkManager · Firebase Auth · Google Gemini REST ·
Lottie · Konfetti · MPAndroidChart · SoundPool · AlarmManager (exact) · Gson.

## Cấu trúc

| Package | Nội dung |
| --- | --- |
| `data/local` | Entity, DAO, `AppDatabase`, `Converters`, `LocalDataSeeder` (nạp 4 file JSON trong `assets/data/`) |
| `data/remote` | `ApiService` (đồng bộ), `GeminiApi` và DTO |
| `data/repository` | Repository offline-first cho nội dung, tiến độ, người dùng, AI, xác thực, đồng bộ |
| `service/ai` | `AiTutorService`, `ChatMemoryManager`, `WeaknessDetector`, `GeminiClient` |
| `service/reminder` | `ReminderScheduler`, `AlarmReceiver`, `NotificationHelper`, `BootReceiver` |
| `service/sync`, `service/gamification` | `SyncWorker`, `SyncScheduler`, `BadgeEvaluator` |
| `ui/roadmap`, `ui/lesson`, `ui/review`, `ui/profile`, `ui/auth` | Fragment và ViewModel |
| `utils` | `SoundManager`, `SuperMemo2Helper`, `SrsAlgorithm`, `AnimationUtils`, `Levels`, ... |
| `tools/` | `generate_assets.py` sinh audio, SFX và animation Lottie |
| `db/mysql/` | `schema.sql`, schema tham chiếu phía server khớp với entity Room |
| `data/` | Bộ JSON gốc: từ vựng, ngữ pháp, đọc, nghe |
| `screenshots/` | Ảnh chụp chế độ học thử (render từ test Robolectric) |

## Cấu hình (không commit)

`local.properties` đã có trong `.gitignore`:

```properties
sdk.dir=/path/to/Android/Sdk
gemini.api.key=YOUR_GEMINI_KEY          # tuỳ chọn, cũng có thể nhập trong Hồ sơ → Cài đặt
gemini.model=gemini-1.5-flash           # đổi nếu model này đã ngừng hỗ trợ
api.base.url=https://your-server/api/   # máy chủ MySQL (tuỳ chọn)
```

Firebase (Email/Google): đặt `app/google-services.json` và thay `google_web_client_id` trong
`res/values/strings.xml` bằng Web client ID. Nếu không có file này, app vẫn chạy ở chế độ Học thử.

## Build và kiểm thử

Yêu cầu: JDK 17 trở lên (đã test với JDK 21), Android SDK platform 34.

```bash
./gradlew assembleDebug          # APK debug
./gradlew testDebugUnitTest      # unit test và Robolectric (Room in-memory, Lottie, luồng học thử)
./gradlew lintDebug
python3 tools/generate_assets.py # sinh lại audio, SFX và Lottie (cần espeak-ng, ffmpeg)
```

Test Robolectric `GuestModeScreenshotTest` đi qua luồng học thử và ghi ảnh vào `screenshots/guest-mode/`.

## Đồng bộ

`SyncWorker` đẩy các dòng tiến độ, SM-2, điểm yếu và bộ nhớ AI có `updated_at` lên
`POST sync/progress`, rồi kéo về `GET sync/progress`. Xung đột giải quyết theo last-write-wins từng dòng.
Phần máy chủ chưa có trong repo này.

## Trạng thái

- Đã build APK debug, 46 unit test pass, lint không có lỗi.
- Đã kiểm chứng luồng học thử bằng Robolectric và chụp 13 ảnh màn hình.
- Chưa chạy trên thiết bị hoặc emulator thật.
- Chưa có máy chủ MySQL và chưa cấu hình Firebase/Gemini trong repo.
