# Game2D - Tower Defense Auto Chess Game

Một game tower defense 2D được phát triển bằng Android Jetpack Compose, lấy cảm hứng từ Auto Chess và TFT (Teamfight Tactics) với hệ thống ngũ hành độc đáo.

## 🎮 Tính năng chính

### Gameplay Core
- **Hệ thống ngũ hành**: 5 hệ tướng (Kim, Mộc, Thủy, Hỏa, Băng) với đặc điểm riêng biệt
- **Merge system**: Gộp 3 tướng cùng loại để nâng cấp sao (1★ → 2★ → 3★)
- **Shop system**: Mua tướng ngẫu nhiên từ shop với giá cố định (1 gold) - luôn hoạt động
- **Board management**: Quản lý 5 slot generic trên bàn cờ chiến đấu
- **Infinite prep time**: Chuẩn bị vô hạn thời gian để sắp xếp tướng
- **Fixed enemy waves**: Mỗi vòng có số lượng quái cố định (5 quái)
- **Round completion**: Chỉ qua vòng khi diệt hết tất cả quái
- **Drag & Drop**: Kéo thả tướng từ hàng chờ lên sàn đấu một cách trực quan
- **Combat system**: Hệ thống chiến đấu real-time với bắn đạn và va chạm
- **Real-time management**: Quản lý tướng và mua sắm ngay cả trong lúc chiến đấu


### Note cách chỉnh thong so
- Chỉnh đốt Hp theo giây ở hệ hỏa: damageThisTick = 0.05f * e.maxHp * seconds
- Tăng chiều cao cột lửa val fireThickness = 150f
- Chỉnh tốc độ wave: durationMs = durationMs * 2L

### Cơ chế enemy
Quái có 3 loại:
- monter_basic: quái bình thường
- monter_big_hp_slow_walk: quái đi chậm, máu trâu
- monter_paper_hp_fast_walk: quái đi nhanh, máu mỏng
Boss xuất hiện ở vòng 5, 10, 15:
- **Vòng 5**: Boss1 - Kỹ năng bắn đạn hoặc đóng băng 1 tướng
- **Vòng 10**: Boss2 - Kỹ năng gọi thêm đệ tử  
- **Vòng 15**: Boss3 - Kỹ năng đóng băng nhiều tướng
#### Kỹ năng theo hệ (role) và quy tắc tăng theo sao
- **Hỏa (Lửa)**: Tạo 1 hàng lửa gây sát thương cao, tồn tại trong N giây.
  - Tăng sao: Mỗi +1★ thêm +1 hàng lửa (stack theo hàng).

- **Thủy (Nước)**: Tạo 1 làn sóng nước đẩy lùi địch theo chiều cột.
  - Tăng sao: Mỗi +1★ thêm +1 làn sóng

- **Mộc (Cây)**: Hồi máu cho nhà (base/core) theo chu kỳ.
  - Tăng sao: Tăng lượng hồi máu mỗi tick và/hoặc giảm cooldown giữa các lần hồi.

- **Băng**: Bắn đạn làm chậm, có tỷ lệ đóng băng địch trong thời gian ngắn.
  - Tăng sao: Tăng sát thương, tăng phần trăm làm chậm/đóng băng và tăng thời lượng hiệu ứng.

- **Kim (Màu vàng)**: Bắn mũi tên gây sát thương trực tiếp đơn mục tiêu.
  - Tăng sao: Tăng damage mỗi phát bắn và tăng tốc độ bắn 

#### Hệ thống sao và tier
- **1 sao**: Tướng cơ bản (merge 3 tướng 1★ → 1 tướng 2★)
- **2 sao**: Tướng mạnh hơn (merge 3 tướng 2★ → 1 tướng 3★)
- **3 sao**: Tướng mạnh nhất
- 
### Animations
- **Drag & Drop**: Smooth transitions với scale và alpha
- **Combat**: Muzzle flash, bullet trails, explosions
- **UI**: Fade in/out, scale animations
- **Effects**: Particle systems cho visual feedback

## 👨‍💻 Tác giả

**Bao Thanh Bin**
- GitHub: [@baothanhbin](https://github.com/baothanhbin)

## 🆕 Changelog

### Version 4.0 - Generic Board Slots & Enhanced Drag & Drop
- ✅ **Generic board slots**: 5 slots có thể đặt bất kỳ tướng nào (không còn fix theo HeroType)
- ✅ **Enhanced drag & drop**: Cải thiện hệ thống kéo thả với hit testing chính xác
- ✅ **Auto-drop system**: Tự động drop tướng khi kéo vào slot hợp lệ
- ✅ **Visual feedback**: Cải thiện visual feedback cho drag operations
- ✅ **Debug logging**: Thêm extensive logging để debug drag & drop
- ✅ **Board management**: Logic deploy thông minh hơn với available slot detection

### Version 3.0 - Infinite Prep & Fixed Waves
- ✅ **Infinite prep time**: Chuẩn bị vô hạn thời gian để sắp xếp tướng
- ✅ **Fixed enemy waves**: Mỗi vòng có số lượng quái cố định (5 quái)
- ✅ **Round completion**: Chỉ qua vòng khi diệt hết tất cả quái
- ✅ **Smart HUD**: Hiển thị tiến độ diệt quái (2/5) thay vì countdown timer
- ✅ **Start button**: Nút "Bắt đầu" để bắt đầu vòng chiến đấu
- ✅ **Optimized UI**: Sàn đấu nằm trên phần thông tin player
- ✅ **Real-time management**: Mua sắm và quản lý tướng mọi lúc
- ✅ **Color-coded system**: Phân biệt màu sắc cho từng khu vực

### Version 2.0 - Core Systems
- ✅ **Combat system**: Hệ thống chiến đấu real-time
- ✅ **Merge system**: Gộp tướng để nâng cấp
- ✅ **Shop system**: Mua tướng với probability
- ✅ **Economy system**: Quản lý gold, XP, level
- ✅ **Visual effects**: Hiệu ứng đạn, nổ, sparks
- ✅ **Sound system**: Âm thanh cho actions

### Version 1.0 - Foundation
- ✅ **Basic UI**: Giao diện cơ bản với Compose
- ✅ **Player system**: Quản lý tướng và board
- ✅ **Game loop**: 60 FPS game engine
- ✅ **State management**: MVVM với StateFlow

---

*Game được phát triển với ❤️ bằng Jetpack Compose và Kotlin*