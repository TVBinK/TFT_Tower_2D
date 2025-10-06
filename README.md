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
- Chỉnh tốc độ wave: val frame = getGifFrameForTime(currentWaveFrames, durationMs = 6000L)

### Cơ chế enemy
Quái có 3 loại:
- monter_basic: quái bình thường
- monter_big_hp_slow_walk: quái đi chậm, máu trâu
- monter_paper_hp_fast_walk: quái đi nhanh, máu mỏng
Boss xuất hiện ở vòng 5, 10, 15:
- **Vòng 5**: Boss1 - Kỹ năng đóng băng 1 tướng
- **Vòng 10**: Boss2 - Kỹ năng gọi thêm đệ tử  
- **Vòng 15**: Boss3 - Kỹ năng đóng băng nhiều tướng
#### Kỹ năng theo hệ (role) và quy tắc tăng theo sao
Fire (handleFireSkill, updateEffects)
★☆☆: Tạo “hàng lửa” tại vị trí kẻ địch gần nhất, gây sát thương theo thời gian, tồn tại 5s. Hồi chiêu sau khi hiệu ứng kết thúc: 5s. đốt 5% máu kẻ địch mỗi giây.
★★☆: Thời gian tồn tại tăng ~7s, tăng lên đốt 7% máu kẻ địch mỗi giây.
★★★: Thời gian tồn tại tăng ~9s, tăng lên đốt 10% máu kẻ địch mỗi giây.
Water (handleWaterSkill, updateEffects)
★☆☆: Gọi “sóng nước” theo hàng ngang đẩy lùi quái. Sóng xuất phát gần vị trí tướng, chạy lên trên. Thời gian tồn tại: ~5s.
★★☆: Sóng mạnh hơn (tăng thời gian tồn tại ~6.5s), +1 hàng sóng
★★★: Tăng thời gian tồn tại ~8s và +1 hàng sóng nữa (tổng 3 hàng)
Ice ->     freezeChance, freezeDuration
★☆☆: Damage cơ bản (x1.0), slow 2s (còn ~30% tốc), đóng băng 20% trong 1s                 -> sát thương 10
★★☆: Tăng damage x2, slow 2s, đóng băng 40% trong 2s                                      -> sát thương 20
★★★: Tăng damage x3, slow 2s, đóng băng 60% trong 3s                                      -> sát thương 30
Metal (handleMetalShot)
★☆☆: Tăng sức bắn cơ bản (actualDamage theo sao), bắn thẳng. 2000ms = 0.5 viên/giây       -> sát thương 15
★★☆: Damage x2 và tăng tốc bắn, 1600 = 0.625 viên/giây                                    -> sát thương 30
★★★: Damage x3 và tăng tốc bắn mạnh, 1200 = 0.833 viên/giây                               -> sát thương 45

#### Hệ thống sao và tier
- **1 sao**: Tướng cơ bản (merge 3 tướng 1★ → 1 tướng 2★)
- **2 sao**: Tướng mạnh hơn (merge 3 tướng 2★ → 1 tướng 3★)
- **3 sao**: Tướng mạnh nhất

### Note
- Phát sound và nháy khi boss chuẩn bị xuất hiện: SoundEvent.BEFORE_BOSS 
                                                  Mỗi nháy: alpha 0 -> 0.7 trong 160ms, rồi 0.7 -> 0 trong 220ms. Nghỉ 120ms giữa hai nháy.
- 