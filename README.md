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

### UI/UX Features
- **Modern UI**: Giao diện hiện đại với Jetpack Compose
- **Triple-row interface**: 3 hàng chính - Sàn đấu, Hàng chờ tướng và Shop
- **Color-coded system**: Phân biệt màu sắc cho từng khu vực và hệ tướng
- **Responsive design**: Tương thích với nhiều kích thước màn hình
- **Smooth animations**: Hiệu ứng mượt mà cho drag & drop
- **Intuitive controls**: Điều khiển trực quan với touch gestures
- **Visual effects**: Hiệu ứng hình ảnh đẹp mắt cho combat
- **Smart HUD**: Hiển thị tiến độ diệt quái (2/5) thay vì countdown timer

### Visual & Audio Effects
- **Muzzle flash**: Hiệu ứng lửa nòng súng khi bắn
- **Explosion effects**: Hiệu ứng nổ khi enemy chết
- **Hit sparks**: Tia lửa khi đạn trúng mục tiêu
- **Bullet trails**: Vệt đạn bay với màu sắc theo hệ ngũ hành
- **Smoke effects**: Hiệu ứng khói và debris
- **Sound system**: Hệ thống âm thanh cho mọi hành động (bắn theo hệ, va chạm, mua bán)

### Advanced Systems
- **Economy System**: Quản lý gold, XP, level với hệ thống kinh tế cân bằng
- **Mana System**: Hệ thống mana cho các kỹ năng đặc biệt
- **Difficulty System**: 3 mức độ khó (Easy, Normal, Hard) với modifiers khác nhau
- **Data Persistence**: Lưu trữ dữ liệu game với Room database
- **State Management**: Quản lý state phức tạp với StateFlow và Coroutines

## 🏗️ Kiến trúc dự án


### Công nghệ sử dụng
- **Jetpack Compose**: UI framework hiện đại với Material Design 3
- **MVVM Architecture**: Kiến trúc Model-View-ViewModel
- **Kotlin Coroutines**: Xử lý bất đồng bộ và game loop
- **StateFlow**: Quản lý state reactive
- **Room Database**: Local data persistence
- **DataStore**: Preferences storage
- **Navigation Compose**: Navigation system
- **Material Icons Extended**: Icon symbols

## 🎯 Cách chơi chi tiết

### Gameplay Flow
1. **Khởi động**: Chọn difficulty và bắt đầu game
2. **Mua tướng**: Sử dụng gold để mua tướng từ shop (luôn hoạt động)
3. **Sắp xếp tướng**: Chuẩn bị vô hạn thời gian để sắp xếp đội hình
4. **Deploy tướng**: Kéo thả tướng từ hàng chờ lên sàn đấu để chiến đấu
5. **Merge tướng**: Gộp 3 tướng cùng loại để nâng cấp (1★ → 2★ → 3★)
6. **Bắt đầu vòng**: Nhấn nút "Bắt đầu" khi sẵn sàng chiến đấu
7. **Combat**: Tướng tự động bắn và diệt quái
8. **Qua vòng**: Tiêu diệt tất cả 5 quái để qua vòng tiếp theo
9. **Quản lý kinh tế**: Cân bằng giữa mua tướng, nâng XP và roll shop

### Giao diện Layout
- **Sàn đấu** (màu tím): 5 slot generic có thể đặt bất kỳ tướng nào
- **Thông tin player**: Gold, Level, XP, Lives, Score, Deploy count
- **Controls**: Nút "Bắt đầu", "XP", "Roll" - luôn hoạt động
- **Hàng chờ tướng** (màu xanh lá): Nơi chứa tướng đã mua, có thể kéo lên sàn đấu
- **Shop** (màu xanh dương): 5 ô mua tướng ngẫu nhiên với roll system
- **HUD thông minh**: Hiển thị tiến độ diệt quái (2/5) thay vì countdown timer


### Note cách chỉnh thong so
- Chỉnh đốt Hp theo giây ở hệ hỏa: damageThisTick = 0.05f * e.maxHp * seconds
- Tăng chiều cao cột lửa val fireThickness = 150f
- Chỉnh tốc độ wave: durationMs = durationMs * 2L
### Hệ thống tướng
#### 5 Hệ ngũ hành
1. **Kim** (Vàng - #FFD700): Tướng kim loại, sức mạnh cao, damage physical
2. **Mộc** (Xanh lá - #228B22): Tướng gỗ, tốc độ nhanh, attack speed cao
3. **Thủy** (Xanh dương - #0000FF): Tướng nước, phép thuật mạnh, magic damage
4. **Hỏa** (Đỏ - #FF0000): Tướng lửa, damage cao, area damage
5. **Thổ** (Nâu - #8B4513): Tướng đất, phòng thủ cao, tank role

#### Kỹ năng theo hệ (role) và quy tắc tăng theo sao
- **Hỏa (Lửa)**: Tạo 1 hàng lửa gây sát thương cao, tồn tại trong N giây.
  - Tăng sao: Mỗi +1★ thêm +1 hàng lửa (stack theo hàng).

- **Thủy (Nước)**: Tạo 1 làn sóng nước đẩy lùi địch theo chiều cột.
  - Tăng sao: Mỗi +1★ thêm +1 cột ảnh hưởng (mở rộng phạm vi theo chiều ngang).

- **Mộc (Cây)**: Hồi máu cho nhà (base/core) theo chu kỳ.
  - Tăng sao: Tăng lượng hồi máu mỗi tick và/hoặc giảm cooldown giữa các lần hồi.

- **Băng**: Bắn đạn làm chậm, có tỷ lệ đóng băng địch trong thời gian ngắn.
  - Tăng sao: Tăng sát thương, tăng phần trăm làm chậm/đóng băng và tăng thời lượng hiệu ứng.

- **Kim (Màu vàng)**: Bắn mũi tên gây sát thương trực tiếp đơn mục tiêu.
  - Tăng sao: Tăng damage mỗi phát bắn và tăng tốc độ bắn (giảm thời gian hồi).

#### Hệ thống sao và tier
- **1 sao**: Tướng cơ bản (merge 3 tướng 1★ → 1 tướng 2★)
- **2 sao**: Tướng mạnh hơn (merge 3 tướng 2★ → 1 tướng 3★)
- **3 sao**: Tướng mạnh nhất
- **5 Tiers**: T1 (1 gold) → T5 (5 gold), tier cao hơn = tướng mạnh hơn

### Chiến thuật nâng cao
- **Positioning**: Sắp xếp tướng hợp lý trên 5 slots
- **Economy management**: Cân bằng gold cho mua tướng, XP, roll
- **Merge timing**: Biết khi nào nên merge, khi nào nên hold
- **Shop probability**: Hiểu tỷ lệ xuất hiện tướng theo level
- **Real-time strategy**: Tận dụng khả năng quản lý mọi lúc
- **Bench management**: Quản lý hiệu quả 9 slots bench

## 🚀 Cài đặt và chạy

### Yêu cầu hệ thống
- **Android Studio**: Arctic Fox (2020.3.1) trở lên
- **Android SDK**: API Level 27+ (Android 8.1)
- **Kotlin**: 1.8+
- **Gradle**: 7.0+
- **Java**: 11+

### Các bước cài đặt
1. **Clone repository**
   ```bash
   git clone <repository-url>
   cd Game2D
   ```

2. **Mở project trong Android Studio**
   - File → Open → Chọn thư mục Game2D
   - Đợi Gradle sync hoàn tất

3. **Chạy project**
   ```bash
   ./gradlew assembleDebug
   ```
   Hoặc nhấn Run trong Android Studio

4. **Test trên thiết bị**
   - Kết nối Android device hoặc start emulator
   - Chọn target device và run

### Build variants
- **Debug**: Development build với debug symbols
- **Release**: Production build với optimizations

## 🎨 Hệ thống visual

### Color Scheme
- **Kim**: Vàng (#FFD700) - Kim loại, sang trọng
- **Mộc**: Xanh lá (#228B22) - Tự nhiên, sinh trưởng
- **Thủy**: Xanh dương (#0000FF) - Nước, linh hoạt
- **Hỏa**: Đỏ (#FF0000) - Lửa, năng lượng
- **Thổ**: Nâu (#8B4513) - Đất, ổn định

### UI Zones
- **Board Area**: Tím - Khu vực chiến đấu chính
- **Bench Area**: Xanh lá - Khu vực chứa tướng
- **Shop Area**: Xanh dương - Khu vực mua sắm
- **HUD**: Trắng/Xám - Thông tin game

### Animations
- **Drag & Drop**: Smooth transitions với scale và alpha
- **Combat**: Muzzle flash, bullet trails, explosions
- **UI**: Fade in/out, scale animations
- **Effects**: Particle systems cho visual feedback

## 🔧 Phát triển

### Thêm tướng mới
1. **Thêm HeroType** vào `Enums.kt`
2. **Cập nhật Unit model** với stats mới
3. **Thêm logic combat** trong `CombatSystem.kt`
4. **Cập nhật UI components** để hiển thị đúng
5. **Test balance** và adjust stats

### Thêm tính năng mới
1. **Tạo model/data class** trong `model/`
2. **Thêm logic** trong `GameEngine.kt`
3. **Cập nhật GameViewModel** để expose state
4. **Tạo UI components** trong `ui/home/components/`
5. **Integrate** vào `HomeScreen.kt`

### Thêm hiệu ứng mới
1. **Thêm EffectType** vào `Enums.kt`
2. **Cập nhật Effect model** với properties mới
3. **Thêm logic** trong `EffectSystem.kt`
4. **Cập nhật PlayArea** để render hiệu ứng
5. **Test performance** với nhiều effects

### Thêm âm thanh mới
1. **Thêm file âm thanh** vào `res/raw/`
2. **Cập nhật SoundSystem** với sound mapping
3. **Tích hợp** vào các actions tương ứng
4. **Test audio quality** và volume balance

### Debugging
- **Logs**: Sử dụng `Log.d()` với tags riêng biệt
- **Breakpoints**: Set breakpoints trong Android Studio
- **Compose Inspector**: Debug UI với Compose Inspector
- **Performance**: Monitor với Profiler

## 📱 Screenshots

*Screenshots sẽ được thêm sau khi có UI hoàn chỉnh*

## 🤝 Đóng góp

### Cách đóng góp
1. **Fork** repository
2. **Tạo feature branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit changes** (`git commit -m 'Add some AmazingFeature'`)
4. **Push to branch** (`git push origin feature/AmazingFeature`)
5. **Tạo Pull Request**

### Guidelines
- **Code style**: Tuân thủ Kotlin coding conventions
- **Commits**: Sử dụng conventional commits
- **Testing**: Test thoroughly trước khi submit
- **Documentation**: Update README nếu cần

## 📄 License

Dự án này được phát hành dưới **MIT License**. Xem file `LICENSE` để biết thêm chi tiết.

## 👨‍💻 Tác giả

**Bảo Thành Bình**
- GitHub: [@baothanhbin](https://github.com/baothanhbin)
- Email: [contact email]

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