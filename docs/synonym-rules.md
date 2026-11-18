# Quy tắc từ đồng nghĩa

Áp dụng cho bảng `synonym_groups` và `synonym_members`, dữ liệu seed `assets/data/synonyms.<lang>.json` và
trò chơi "Ghép cặp từ đồng nghĩa" (`SYNONYM_MATCH`). Thiết kế bảng xem `entertainment-database.md`.

## 1. Đồng nghĩa chỉ nằm trong một ngôn ngữ

- Hai từ là đồng nghĩa khi chúng **cùng một nhóm**, và cả nhóm viết **cùng một ngôn ngữ**.
- Ngôn ngữ (BCP-47: `en`, `vi`, `fr`...) lưu ở **nhóm** (`synonym_groups.lang`), không lưu ở từng thành viên.
  Vì vậy về cấu trúc không thể có nhóm trộn ngôn ngữ.
- **Bản dịch không phải đồng nghĩa.** `go` và `đi` không bao giờ nằm chung một nhóm. Quan hệ giữa các ngôn ngữ
  thuộc về bảng dịch (`sense_translations`), không thuộc về từ đồng nghĩa.

| Hợp lệ (`lang = en`) | Không hợp lệ |
| --- | --- |
| `go`, `travel`, `proceed`, `journey` | `go`, `đi` |
| `go`, `went`, `gone` | `big`, `grand` (en + fr) |

## 2. Một nhóm là một tập từ cùng nghĩa

- Dùng **nhóm**, không lưu từng cặp: n từ sẽ thành n·(n−1)/2 cặp, dễ trùng và khó giữ nhất quán.
- Một nhóm có **từ 2 từ trở lên**. Nhóm 1 từ không có ý nghĩa và game bỏ qua.
- Dạng biến cách (`go` / `went` / `gone`) và từ gần nghĩa (`go` / `travel`) đều được ghi nhận. Nên đặt
  `note` để nói rõ loại quan hệ (ví dụ `"forms of go"`, `"travel"`).
- Dùng `note` khi cần ghi sắc thái, mức độ trang trọng hoặc loại quan hệ. Không có sắc thái thì để trống.

## 3. Một từ có thể nằm trong nhiều nhóm

- Từ nhiều nghĩa nằm ở nhiều nhóm: `go` ở nhóm "đi lại" và ở nhóm "biến cách của go".
- Trong một nhóm, một từ chỉ xuất hiện **một lần** (khóa chính `(group_id, text)`).
- Viết thường, không dấu cách thừa; cụm từ giữ nguyên khoảng trắng giữa các chữ.

## 4. Liên kết với kho từ vựng

- `synonym_members.word_id` nối tới `words.id` khi từ có trong kho từ của app; để `NULL` nếu không có.
- Seeder tự tìm `word_id` bằng `words.term` (không phân biệt hoa thường) và **chỉ thử với nhóm `en`**,
  vì `words` hiện chỉ có tiếng Anh.
- Từ bị xóa khỏi `words` thì `word_id` về `NULL`, thành viên vẫn còn.

## 5. Cách trò chơi dùng dữ liệu

- Ngôn ngữ của trò chơi là **ngôn ngữ đang học** của học viên (`user_settings.learning_language`, mặc định `en`),
  không phải ngôn ngữ hiển thị. Học Hàn với giao diện tiếng Anh thì ghép các từ đồng nghĩa tiếng Hàn.
- Trò chơi được đánh dấu `design_for` (`ent_activities`); chỉ hiện khi ngôn ngữ đang học nằm trong danh sách.
  `SYNONYM_MATCH` hiện chỉ có `["en"]`.
- Mỗi lượt chọn các nhóm có ≥ 2 thành viên, **ưu tiên nhóm có từ học viên đã học** (`word_id` ∈ `word_progress`),
  rồi lấy hai từ khác nhau trong mỗi nhóm làm một cặp.
- **Không để một chữ xuất hiện hai lần trong cùng lượt**, dù nó thuộc hai nhóm khác nhau, để mỗi đáp án chỉ có
  một cách đúng.
- Học viên chọn chữ bên trái rồi chữ bên phải; cặp đúng khi hai chữ cùng nhóm.

## 6. Thêm dữ liệu mới

1. Sửa `app/src/main/assets/data/synonyms.<lang>.json` (mỗi ngôn ngữ một file, `lang` lấy từ tên file), mục `groups`:
   ```json
   { "note": "forms of go", "words": ["go", "went", "gone"] }
   ```
2. Tăng `EntertainmentSeeder.CONTENT_VERSION` để app nạp lại (nhóm đồng nghĩa được xóa và nạp lại hoàn toàn;
   đăng ký cuộc thi của học viên không bị ảnh hưởng).
3. Thêm ngôn ngữ mới: tạo file `synonyms.<lang>.json` mới, đặt `design_for` của trò chơi gồm ngôn ngữ đó. **Không** đặt từ
   của hai ngôn ngữ vào cùng một nhóm.

## 7. Kiểm tra nhanh khi thêm dữ liệu

- Mỗi nhóm có `lang` và ít nhất 2 từ.
- Mọi từ trong nhóm đúng ngôn ngữ của nhóm; không có bản dịch lẫn vào.
- Không lặp từ trong cùng một nhóm.
- Từ tiếng Anh muốn liên kết kho từ vựng phải trùng `words.term` (viết thường).
