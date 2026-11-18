# Thiết kế database: trang Giải trí

Room schema **v5 → v9** (migrations `MIGRATION_5_6` … `MIGRATION_8_9`, xem mục "Nhiều ngôn ngữ"; v7 bỏ bảng thể lệ, không xóa dữ liệu học viên). Nội dung danh mục được nạp
từ `assets/data/entertainment.json` bởi `EntertainmentSeeder` (có `CONTENT_VERSION` riêng, upsert nên không
làm mất đăng ký/tiến độ của học viên).

## Nguyên tắc

| Cái gì | Nằm ở đâu |
| --- | --- |
| Luật chơi, màn hình, thuật toán ra đề của từng trò chơi / chế độ luyện tập | **Code trong app** (`code` của activity là khóa nối DB với code) |
| Danh sách để hiển thị: tên, mô tả, hình, màu, thứ tự, bật/tắt | **DB** (`ent_activities`) |
| Carousel, sự kiện, cuộc thi, đăng ký | **DB** |
| Từ dùng để ra đề (Nối từ, Đoán từ, Chép siêu tốc, Ôn lỗi sai) | **Query từ `words` + `word_progress`** (chỉ từ học viên đã học), không có bảng riêng |
| Từ đồng nghĩa | **Bảng riêng**, không phụ thuộc ngôn ngữ |

## 1. Danh mục hiển thị

**`ent_activities`** – một dòng cho mỗi trò chơi *hoặc* chế độ luyện tập (chỉ là dữ liệu hiển thị).
- `id` PK · `section` (`GAME` | `PRACTICE`) · `code` UNIQUE (khóa mà app map sang màn hình: `WORD_MATCH`,
  `GUESS_WORD`, `SYNONYM_MATCH`, `COPY_SPEED`, `MISTAKES`, `TUNG`, `SLEEPING`)
- `title`, `subtitle`, `image` (tên drawable), `accent` (`GREEN|BLUE|PURPLE|AMBER`), `position`, `enabled`
- Trang chủ hiện tối đa 4 game; **nhiều hơn 4 thì hiện nút "Tất cả"** mở danh sách đầy đủ.
- Code không có màn hình cho một `code` lạ thì bỏ qua dòng đó (không crash).

**`ent_game_plays`** – mỗi lần học viên mở một activity: `id`, `user_id` FK, `activity_code`, `played_at`.
Số "người tham gia" = `COUNT(DISTINCT user_id)`; ẩn khi bằng 0 (không còn số giả).

**`ent_banners`** – các thẻ carousel: `id`, `tag`, `title`, `subtitle`, `button_label`, `image`, `accent`,
`action_type` (`ACTIVITY` | `CONTEST` | `NONE`), `action_target` (code activity hoặc id cuộc thi),
`position`, `starts_at`/`ends_at` (nullable, ngoài khoảng này thì ẩn), `enabled`.

**`ent_events`** – banner sự kiện: `id`, `tag`, `title`, `description`, `image`, `accent`, `starts_at`,
`ends_at`, `enabled`. Hiện sự kiện đang diễn ra có `ends_at` gần nhất ("kết thúc sau N ngày").

## 2. Cuộc thi

**`ent_contests`**: `id`, `title`, `description`, `image`, `accent`,
`registration_opens_at`, `registration_closes_at`, `starts_at`, `ends_at`, `question_count`,
`duration_minutes`, `enabled`.
Trạng thái **không lưu**, suy ra từ giờ hiện tại:
`now < registration_opens_at` → sắp mở đăng ký · `now < starts_at` → đang đăng ký (chờ thi) ·
`now < ends_at` → đang diễn ra · sau đó → đã kết thúc.
Ràng buộc thời gian: `registration_opens_at ≤ registration_closes_at ≤ ends_at` và `starts_at < ends_at`
(cho phép đăng ký muộn khi cuộc thi đã bắt đầu).

**`ent_contest_packs`** (`contest_id`, `pack_id`, `position`) – PK `(contest_id, pack_id)`, FK tới `ent_contests`
và `packs`. Đề thi lấy từ từ vựng của các pack này.

**`ent_contest_prizes`** (`contest_id`, `rank_from`, `rank_to`, `reward_gems`) – PK `(contest_id, rank_from)`.
Hạng 1 = `1..1`, top 4–10 = `4..10`, top 20 trở đi = `20..NULL`.

**`ent_contest_registrations`** (`contest_id`, `user_id`, `registered_at`) – PK `(contest_id, user_id)`, FK
CASCADE. Số người tham gia = `COUNT(*)`. Chỉ đăng ký được khi `registration_opens_at ≤ now < registration_closes_at`; chỉ vào thi được khi
đã đăng ký **và** đang trong `[starts_at, ends_at)`.

## 3. Từ đồng nghĩa (không giới hạn ngôn ngữ)

Dùng **nhóm** chứ không dùng cặp: đồng nghĩa là quan hệ giữa nhiều từ, lưu cặp sẽ bùng nổ (n·(n-1)/2 dòng,
trùng lặp, khó giữ nhất quán) và không biểu diễn được "cùng nghĩa" xuyên ngôn ngữ.

**`synonym_groups`**: `id` PK autogenerate, `note` (nullable, ghi chú/sắc thái).

**`synonym_members`**: `group_id` FK CASCADE, `lang` (BCP-47: `en`, `vi`, `fr`…), `text`,
`word_id` (nullable, FK `words` ON DELETE SET NULL – liên kết tới từ vựng của app khi có).
PK `(group_id, lang, text)`; index `(lang, text)` và `(word_id)`.

- Hai từ đồng nghĩa ⇔ cùng `group_id`. Nhóm có thể chứa nhiều ngôn ngữ: `big/large/huge` (en) cùng
  `to/lớn` (vi) cùng `grand` (fr).
- Trò chơi chọn **ngôn ngữ của hai cột** (mặc định `en`–`en`; đặt `en`–`vi` thì ra đề dịch nghĩa) bằng query
  trên `lang`, không cần đổi schema.
- Ưu tiên nhóm có từ học viên đã học (`word_id` ∈ `word_progress`).

## 4. Từ vựng cho các trò chơi

"Đã học" = có dòng trong `word_progress` của học viên (cùng định nghĩa với màn Từ vựng).
- **Nghĩa của một từ** (`GameWord.meaning`) = bản dịch `sense_translations(lang='vi', sense 0)` nếu có, nếu không thì
  `definition_en` rút gọn về mệnh đề đầu. (Hiện chỉ 36 từ của gói Tết/Noel có bản dịch `vi`, nên phần lớn từ dùng định nghĩa tiếng Anh.)
- **Nối từ**: `words` + nghĩa như trên.
- **Đoán từ**: `word_senses.definition_en` của sense 0; đáp án nhiễu lấy từ từ khác.
- **Chép siêu tốc**: nghĩa → gõ `term` (chỉ từ một chữ, từ 3 chữ cái).
- **Ôn lỗi sai**: `word_progress.times_wrong > 0`, nhiều lỗi nhất trước.
- **Cuộc thi**: từ của `ent_contest_packs`. **Tụng / Sleeping**: `packs` + từ đã học.
- Không đủ từ đã học thì màn game báo "Học thêm vài từ để chơi" thay vì dùng dữ liệu giả.

## Nhiều ngôn ngữ (schema 9)

Mỗi học viên có **hai ngôn ngữ độc lập** trong `user_settings`:

| cột | ý nghĩa | ảnh hưởng |
| --- | --- | --- |
| `display_language` | ngôn ngữ hiển thị | nghĩa của từ (`sense_translations.lang`), chữ của catalogue (`ent_texts.lang`) |
| `learning_language` | ngôn ngữ đang học (**một ngôn ngữ tại một thời điểm**, đổi được) | từ, pack, game, đồng nghĩa, cuộc thi, giọng đọc |

Học Hàn với giao diện tiếng Anh là `learning_language = 'ko'`, `display_language = 'en'`. Đổi `learning_language`
không xóa gì: `word_progress` theo từng `word_id` nên quay lại ngôn ngữ cũ vẫn còn tiến độ.

- **`words.lang`, `packs.lang`:** ngôn ngữ của từ / pack. Id từ tiếng Anh giữ nguyên (`bed`); ngôn ngữ khác dùng
  `<lang>:<slug>` (`ko:annyeong`) để không trùng nhau.
- **`levels(system, code, position)`:** cấp độ tách riêng, mỗi ngôn ngữ chấm theo hệ của mình (`CEFR` A1..C2,
  `TOPIK` 1..6, `HSK`, `THEME` cho pack theo chủ đề). `words` và `packs` trỏ tới nó bằng
  `(level_system, level_code)` (khóa ngoại), `position` quyết định thứ tự.
- **`word_senses.definition`:** định nghĩa viết bằng ngôn ngữ **của chính từ đó**. Nghĩa bằng ngôn ngữ khác nằm
  ở `sense_translations`. Game dùng nghĩa = bản dịch theo `display_language` nếu có, không thì `definition`.
- **`ent_activities.design_for`:** các **ngôn ngữ đang học** mà game được thiết kế cho (`["en"]`). Chỉ hiện khi
  `learning_language` nằm trong danh sách. Banner trỏ tới game không hợp lệ cũng bị ẩn.
- **`ent_texts(owner_type, owner_id, field, lang, text)`:** chữ của game, banner, sự kiện, cuộc thi theo
  `display_language`. Cột trên chính dòng đó giữ bản mặc định (tiếng Anh).
- **`audience_lang`** (banner, sự kiện): chỉ hiện cho người học ngôn ngữ đó; `NULL` là mọi người. Cuộc thi lấy
  ngôn ngữ từ các pack của nó.
- **Đồng nghĩa** theo `learning_language` (xem `synonym-rules.md`).

### Thêm một ngôn ngữ mới (ví dụ tiếng Hàn)
1. Thêm file `assets/data/vocabulary.ko.json` cùng dạng `vocabulary.en.json`: `"lang": "ko"`, `levels` (TOPIK 1..6),
   pack và từ với `level_system: "TOPIK"`.
2. Thêm `assets/data/synonyms.ko.json` (`{"lang":"ko","groups":[{"note":"..","words":[..]}]}`); seeder đọc mọi file `synonyms.<lang>.json`.
3. Bật `design_for` cho game phù hợp (thêm `"ko"`); thêm `ent_texts` nếu cần.
4. Ngôn ngữ xuất hiện trong lựa chọn "Learning language" của Profile khi `words` có từ của nó.

### Migration 8 → 9
SQLite trên minSdk 26 không đổi tên hay xóa cột được, nên các bảng nội dung được dựng lại. `word_progress` được
sao lưu rồi khôi phục (xóa `words` sẽ cascade sang nó); sense, bản dịch, thành viên pack được seeder nạp lại.
