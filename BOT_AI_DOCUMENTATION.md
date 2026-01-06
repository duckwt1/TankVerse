# 🤖 BOT AI SYSTEM DOCUMENTATION

## 📋 Mục lục
1. [Tổng quan](#tổng-quan)
2. [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
3. [State Machine](#state-machine)
4. [Thuật toán AI](#thuật-toán-ai)
5. [Spawn System](#spawn-system)
6. [Cấu hình](#cấu-hình)
7. [API & Integration](#api--integration)

---

## 🎯 Tổng quan

Hệ thống Bot AI sử dụng **Finite State Machine (FSM)** để tạo ra các bot tank có khả năng:
- ✅ Tự động tìm kiếm và tấn công player
- ✅ Đuổi theo mục tiêu
- ✅ Tránh vật cản và điều hướng
- ✅ Chạy trốn khi HP thấp
- ✅ Di chuyển vòng quanh mục tiêu khi tấn công
- ✅ Tự động respawn sau khi chết

---

## 🏗️ Kiến trúc hệ thống

### Cấu trúc file

```
TankVerse/
├── entity/
│   └── BotPlayer.java          # Logic AI của bot
├── core/
│   ├── BotManager.java         # Quản lý tất cả bot
│   └── PlayPanel.java          # Tích hợp bot vào game
└── ui/
    └── WaitingRoomController.java  # Chọn số bot
```

### Flow diagram

```
Server                          Client
  │                              │
  ├─→ START_GAME                 │
  │   (botCount: 2)             │
  │                              ├─→ PlayPanel.spawnBots(2)
  │                              │
  │                              ├─→ BotManager.spawnBotsNearPlayer()
  │                              │
  │                              ├─→ Bot_1, Bot_2 created
  │                              │
  │                              └─→ Update loop (60 FPS)
  │                                  │
  │                                  ├─ updateAI()
  │                                  ├─ updateMovement()
  │                                  ├─ draw()
  │                                  └─ loop...
```

---

## 🎮 State Machine

### Các trạng thái

```
                    ┌─────────────┐
                    │    IDLE     │
                    │ (Quét tìm)  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐   ┌──────────┐
    │  WANDER  │    │  CHASE   │   │  ATTACK  │
    │(Lang thang)   │(Đuổi theo)│  │  (Bắn)   │
    └──────────┘    └──────────┘   └──────────┘
           │               │               │
           └───────────────┼───────────────┘
                           │
                           ▼
                    ┌──────────┐
                    │ RETREAT  │
                    │(Chạy trốn)│
                    └──────────┘
                    (HP < 30%)
```

### Quy tắc chuyển trạng thái

| Điều kiện | Trạng thái mới |
|-----------|---------------|
| HP < 30% | **RETREAT** |
| Khoảng cách ≤ 350px | **ATTACK** |
| Khoảng cách ≤ 400px | **CHASE** |
| Không có target | **WANDER** |
| Không di chuyển | **IDLE** |

---

## 🧠 Thuật toán AI

### 1. IDLE - Đứng yên quét tìm

```java
private void doIdle() {
    gunAngle += Math.toRadians(1);  // Xoay súng 1°/frame
}
```

**Mục đích:** Quét tìm target khi không có hoạt động

---

### 2. WANDER - Lang thang ngẫu nhiên

```java
private void doWander() {
    if (now - stateChangeTime > 3000ms) {
        // Chọn điểm mục tiêu mới mỗi 3 giây
        wanderTargetX = x + random(-200, 200);
        wanderTargetY = y + random(-200, 200);
    }
    moveToward(wanderTargetX, wanderTargetY);
}
```

**Thuật toán:** Random Walk
- Chọn điểm ngẫu nhiên trong bán kính 200px
- Di chuyển đến điểm đó
- Sau 3 giây chọn điểm mới

---

### 3. CHASE - Đuổi theo mục tiêu

```java
private void doChase() {
    moveToward(targetPlayer.x, targetPlayer.y);
    aimAt(targetPlayer.x, targetPlayer.y);
}
```

**Thuật toán:** Pursuit (Đuổi bắt)

```
Step 1: Tính góc đến target
    θ = atan2(targetY - botY, targetX - botX)

Step 2: Xoay thân xe (smooth rotation)
    if (|currentAngle - θ| > rotateSpeed):
        currentAngle += sign(θ - currentAngle) * rotateSpeed
    else:
        currentAngle = θ

Step 3: Di chuyển thẳng
    nextX = x + cos(θ) * speed
    nextY = y + sin(θ) * speed

Step 4: Kiểm tra va chạm
    if (willCollide(nextX, nextY)):
        currentAngle += 30°  // Xoay tránh vật cản
```

---

### 4. ATTACK - Tấn công vòng quanh

```java
private void doAttack() {
    aimAt(target.x, target.y);
    
    // Circle strafe
    angleToTarget = atan2(dy, dx);
    strafeAngle = angleToTarget + 90°;
    
    move(strafeAngle, speed);
    
    if (now - lastShoot > 1000ms) {
        shoot();
    }
}
```

**Thuật toán:** Circle Strafe

```
        Target ●
              ╱│╲
             ╱ │ ╲
            ╱  │  ╲
    Bot ●──────┼───────● 
        ╲  90° │      ╱
         ╲     │     ╱   Bot di chuyển vòng
          ╲    │    ╱    quanh target
           ╲   │   ╱
            ╲  │  ╱
             ╲ │ ╱
              ╲│╱
         Bot movement
```

**Logic:**
1. Tính góc từ bot đến target: `θ`
2. Offset 90° để di chuyển vuông góc: `θ + 90°`
3. Di chuyển theo hướng đó → tạo quỹ đạo vòng
4. Vừa di chuyển vừa bắn

---

### 5. RETREAT - Chạy trốn

```java
private void doRetreat() {
    angleToTarget = atan2(dy, dx);
    fleeAngle = angleToTarget + 180°;  // Ngược hướng
    
    moveToward(fleeX, fleeY);
    aimAt(target.x, target.y);  // Vẫn ngắm bắn
}
```

**Thuật toán:** Flee (Bỏ chạy)

```
Player ●───────────────────→
         ←───────● Bot
         (Bắn lùi)
```

**Kích hoạt:** Khi HP < 30% maxHP

---

## 📍 Spawn System

### Spawn gần player

```java
public void spawnBotNearPlayer(Player player) {
    // Random góc 360°
    double angle = random() * 2π;
    
    // Random khoảng cách 300-600px
    double distance = 300 + random() * 300;
    
    // Tính tọa độ
    spawnX = player.x + cos(angle) * distance;
    spawnY = player.y + sin(angle) * distance;
    
    BotPlayer bot = new BotPlayer(spawnX, spawnY, ...);
}
```

### Hình ảnh minh họa

```
              Bot ●
             /     \
            /       \
           /   400   \
          /    px     \
         /             \
    Bot ●    Player    ● Bot
         \     ●      /
          \          /
           \        /
            \      /
             \    /
              Bot ●
```

Bot spawn **ngẫu nhiên** trong vòng tròn quanh player (300-600px)

---

## ⚙️ Cấu hình

### AI Parameters

```java
// Detection & Combat
private final double DETECTION_RANGE = 400;    // Tầm nhìn
private final double ATTACK_RANGE = 350;       // Tầm bắn
private final double SAFE_DISTANCE = 150;      // Khoảng cách an toàn
private final double RETREAT_HP_PERCENT = 0.3; // HP retreat (30%)

// Timing
private final long SHOOT_COOLDOWN = 1000;      // 1 giây/phát
private final long WANDER_DURATION = 3000;     // Wander 3 giây
private final int RESPAWN_SECONDS = 10;        // Respawn sau 10s

// Movement
private final double rotateSpeed = 2.0;        // Tốc độ xoay (độ/frame)
public double speed = 2.0;                     // Tốc độ di chuyển
```

### Bot Stats

```java
public int maxHp = 25;      // Máu tối đa
public int hp = 25;         // Máu hiện tại
public int dmg = 4;         // Sát thương
public int bullet = 999;    // Đạn (vô hạn cho bot)
```

---

## 🔌 API & Integration

### 1. Tạo và quản lý bot

```java
// Trong PlayPanel
PlayPanel panel = new PlayPanel(...);

// Spawn 1 bot
panel.spawnBot();

// Spawn nhiều bot
panel.spawnBots(5);

// Spawn bot tại vị trí cụ thể
panel.spawnBotAt(100, 200, "CustomBot");

// Xóa tất cả bot
panel.clearBots();

// Lấy số bot hiện tại
int count = panel.getBotManager().getBotCount();
```

### 2. Cấu hình từ server

**Server gửi bot count:**
```java
// ClientHandler.java
Packet start = new Packet(PacketType.START_GAME);
start.data.put("botCount", 2);  // Gửi số bot
client.send(start);
```

**Client nhận và spawn:**
```java
// WaitingRoomController.java
int botCount = (int) packet.data.get("botCount");
playPanel.spawnBots(botCount);
```

### 3. UI cho người chơi

**Waiting Room - Chọn số bot:**
```xml
<ComboBox fx:id="cmbBotCount">
    <items>
        <FXCollections fx:factory="observableArrayList">
            <String fx:value="0"/>
            <String fx:value="2"/>
            <String fx:value="4"/>
            <String fx:value="6"/>
        </FXCollections>
    </items>
</ComboBox>
```

**Khởi tạo:**
```java
cmbBotCount.setValue("2");  // Mặc định 2 bot
cmbBotCount.setDisable(!isHost);  // Chỉ host chọn được
```

---

## 📊 Hiệu suất

### Độ phức tạp thuật toán

| Thao tác | Tần suất | Độ phức tạp | Chi phí |
|----------|----------|-------------|---------|
| `updateAI()` | 60 FPS | O(n) | n = số player |
| `findNearestPlayer()` | 60 FPS | O(n) | Tìm target gần nhất |
| `moveToward()` | Khi di chuyển | O(1) | Tính toán góc |
| `collisionCheck()` | Khi di chuyển | O(m) | m = số tile |
| `shoot()` | 1 lần/giây | O(1) | Tạo bullet |
| `draw()` | 60 FPS | O(1) | Vẽ sprite |

### Tối ưu hóa

✅ **Cooldown system** - Giảm tần suất bắn
✅ **Off-screen culling** - Không vẽ bot ngoài màn hình
✅ **State caching** - Cache target gần nhất
✅ **Lazy collision** - Chỉ check khi di chuyển

### Memory footprint

- **1 bot:** ~2KB (instance + images cached)
- **10 bots:** ~20KB
- **Images:** Loaded 1 lần, shared giữa các bot

---

## 🎯 Use Cases

### 1. Single Player Practice
```java
// Tạo 5 bot để luyện tập
playPanel.spawnBots(5);
```

### 2. Fill Empty Slots
```java
// 2 người chơi + 4 bot = 6 players
int playerCount = 2;
int desiredTotal = 6;
playPanel.spawnBots(desiredTotal - playerCount);
```

### 3. Bot-only Match
```java
// Tạo trận đấu chỉ có bot
playPanel.spawnBots(10);
// Player quan sát hoặc tham gia
```

---

## 🐛 Troubleshooting

### Bot không spawn?

**Kiểm tra:**
1. Server có gửi `botCount` trong START_GAME packet?
   ```java
   System.out.println("Bot count: " + packet.data.get("botCount"));
   ```

2. Client có nhận được `botCount`?
   ```java
   [Client] Bot count from server: 2
   ```

3. BotManager có được khởi tạo?
   ```java
   if (botManager == null) {
       System.out.println("ERROR: BotManager is null!");
   }
   ```

### Bot không hiển thị?

**Kiểm tra:**
1. Hình ảnh đã load?
   ```java
   [Bot] Loaded images for: Bot_1
   ```

2. Bot có trên màn hình?
   ```java
   [Bot] Bot_1 at world(2500,2600) screen(640,360)
   ```

3. Thứ tự vẽ đúng?
   ```java
   // draw() method
   mapLoader.draw(gc, player);
   player.draw(gc);
   botManager.drawAll(gc, player);  // ← Phải sau map
   ```

### Bot không di chuyển?

**Kiểm tra:**
1. `updateAI()` có được gọi?
2. Target có được tìm thấy?
3. Collision detection hoạt động?

**Debug:**
```java
System.out.println("[Bot] State: " + currentState);
System.out.println("[Bot] Target: " + (targetPlayer != null));
System.out.println("[Bot] Position: " + x + "," + y);
```

---

## 🚀 Future Improvements

### Có thể thêm:

1. **A* Pathfinding** - Bot tìm đường thông minh hơn
2. **Team Coordination** - Bot hợp tác chiến đấu
3. **Difficulty Levels** - Easy/Normal/Hard
4. **Behavior Trees** - AI phức tạp hơn FSM
5. **Prediction** - Dự đoán vị trí player
6. **Cover System** - Bot tìm chỗ ẩn nấp
7. **Formation Movement** - Bot di chuyển theo đội hình

### Hiện tại đã có:

✅ State Machine (FSM)
✅ Pursuit & Flee
✅ Circle Strafe
✅ Collision Avoidance
✅ Smooth Movement
✅ Respawn System
✅ Multiplayer Support

---

## 📝 Code Examples

### Example 1: Tùy chỉnh bot stats

```java
BotPlayer bot = new BotPlayer(x, y, mapLoader, "SuperBot");
bot.maxHp = 50;      // Tăng máu
bot.hp = 50;
bot.dmg = 10;        // Tăng sát thương
bot.speed = 3.0;     // Nhanh hơn
```

### Example 2: Custom spawn pattern

```java
// Spawn bot theo hình vuông quanh player
double[] angles = {0, 90, 180, 270};
for (double angle : angles) {
    double rad = Math.toRadians(angle);
    double x = player.x + Math.cos(rad) * 500;
    double y = player.y + Math.sin(rad) * 500;
    botManager.spawnBotAt(x, y, "Guard_" + angle);
}
```

### Example 3: Event listening

```java
// Lắng nghe khi bot chết
@Override
public void update(PlayPanel panel) {
    if (hp <= 0 && isAlive) {
        onBotDied();  // Trigger event
    }
}

private void onBotDied() {
    System.out.println("[Bot] " + botName + " has died!");
    // Có thể gửi packet lên server
    // Có thể spawn loot
}
```

---

## 📖 References

### Design Patterns sử dụng:
- **Finite State Machine** - Game AI
- **Observer Pattern** - Event handling
- **Manager Pattern** - BotManager
- **Component Pattern** - Entity system

### Algorithms:
- **Pursuit Algorithm** - Chase behavior
- **Flee Algorithm** - Retreat behavior
- **Random Walk** - Wander behavior
- **Circle Strafe** - Attack movement

### Math:
- **atan2()** - Tính góc giữa 2 điểm
- **Trigonometry** - Sin/Cos cho di chuyển
- **Vector Math** - Tính khoảng cách
- **Angle Normalization** - Xử lý góc 0-360°

---

## 👥 Credits

**Developed by:** DACS Team
**Course:** Distributed Application & Cloud Services
**University:** VKU
**Year:** 2026

---

## 📄 License

This documentation is part of the TankVerse project.
Copyright © 2026 DACS Team. All rights reserved.
