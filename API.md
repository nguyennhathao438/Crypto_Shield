# API Documentation — Crypto Transaction Demo

Tài liệu mô tả chi tiết các API của hệ thống, tổng hợp từ Postman collection `Crypto app`. Toàn bộ endpoint đều gọi qua **API Gateway**, thống nhất tại **`http://localhost:8000`**.

> Import file [`cryptoshield.postman_collection.json`](docs/postman/cryptoshield.postman_collection.json) vào Postman để test trực tiếp thay vì gõ lại từng request.

## Mục lục

- [Quy ước chung](#quy-ước-chung)
- [Bảng tổng hợp endpoint](#bảng-tổng-hợp-endpoint)
- [1. Auth](#1-auth)
- [2. Order](#2-order)
- [3. Market Data](#3-market-data)
## Quy ước chung

- **Base URL:** `http://localhost:8000`
- **Content-Type:** `application/json` cho tất cả request có body
- **Auth:** Các endpoint cần đăng nhập dùng **Bearer Token** — gắn JWT lấy được từ API đăng nhập vào header:
  ```
  Authorization: Bearer <access_token>
  ```
- `{userId}` là UUID định danh người dùng, ví dụ `22222222-2222-2222-2222-222222222222`.
- `{positionId}` / `{orderId}` / `{conditionId}` là UUID định danh vị thế / lệnh / điều kiện lệnh tương ứng.
- Response mẫu (success/error) hiện chưa có trong Postman collection gốc — sẽ bổ sung khi backend hoàn thiện phần chuẩn hoá response.

## Bảng tổng hợp endpoint

| # | Method | Endpoint | Auth | Mô tả ngắn |
|---|--------|----------|------|------------|
| 1 | POST | `/api/auth/register` | Không | Đăng ký tài khoản |
| 2 | POST | `/api/auth/login` | Không | Đăng nhập, lấy JWT |
| 3 | POST | `/api/order/{userId}` | Bearer | Tạo lệnh Market/Limit |
| 4 | POST | `/api/order/close/{userId}` | Bearer | Đóng vị thế |
| 5 | POST | `/api/order-conditions` | Bearer | Tạo điều kiện TP/SL |
| 6 | DELETE | `/api/order/{orderId}` | Bearer | Xoá lệnh Limit chưa khớp |
| 7 | DELETE | `/api/order-conditions/{conditionId}` | Bearer | Xoá điều kiện TP/SL |
| 8 | GET | `/api/klines?interval=&symbol=` | Không | Dữ liệu nến lịch sử |
| 9 | GET | `/api/price/stream/{symbol}` | Không | Stream giá real-time |
| 10 | GET | `/api/price/{symbol}` | Không | Giá hiện tại |
---

## 1. Auth

### 1.1 Đăng ký

**`POST /api/auth/register`** · Auth: không cần

**Request body:**

| Field | Kiểu | Mô tả |
|---|---|---|
| `email` | string | Email đăng ký, dùng để đăng nhập |
| `password` | string | Mật khẩu |
| `username` | string | Tên hiển thị của user |

```json
{
  "email": "admin@gmail.com",
  "password": "123456abc",
  "username": "admin"
}
```

**Mô tả:** Tạo tài khoản người dùng mới trong hệ thống.

---

### 1.2 Đăng nhập

**`POST /api/auth/login`** · Auth: không cần

**Request body:**

| Field | Kiểu | Mô tả |
|---|---|---|
| `email` | string | Email đã đăng ký |
| `password` | string | Mật khẩu |

```json
{
  "email": "admin@gmail.com",
  "password": "123456abc"
}
```

**Mô tả:** Xác thực email/password, trả về JWT access token. Token này dùng làm Bearer Token cho các API cần xác thực phía sau.

---

## 2. Order

### 2.1 Tạo lệnh (Market / Limit)

**`POST /api/order/{userId}`** · Auth: Bearer Token

**Request body:**

| Field | Kiểu | Mô tả |
|---|---|---|
| `symbol` | string | Mã cặp coin, VD `BTCUSDT` |
| `type` | string | `MARKET` hoặc `LIMIT` |
| `side` | string | `BUY` hoặc `SELL` |
| `quantity` | number | Khối lượng đặt lệnh |
| `margin` | number | Số tiền ký quỹ |
| `price` | number | Giá đặt lệnh (giá thị trường hiện tại nếu MARKET, giá kỳ vọng nếu LIMIT) |
| `leverage` | number | Đòn bẩy, VD `20` (x20) |

```json
{
  "symbol": "BTCUSDT",
  "type": "MARKET",
  "side": "SELL",
  "quantity": 0.02,
  "margin": 6645,
  "price": 66450,
  "leverage": 20
}
```

**Mô tả:** Tạo một lệnh giao dịch mới cho user. Order-service kiểm tra số dư (gọi wallet-service) và giá (gọi market-data-service) trước khi khớp lệnh. Nếu là lệnh `MARKET`, khớp ngay; nếu `LIMIT`, lệnh chờ đến khi giá thị trường chạm mức `price`.

---

### 2.2 Đóng lệnh (đóng vị thế)

**`POST /api/order/close/{userId}`** · Auth: Bearer Token

**Request body:**

| Field | Kiểu | Mô tả |
|---|---|---|
| `positionId` | string (UUID) | ID vị thế cần đóng |
| `symbol` | string | Mã cặp coin |
| `currentPrice` | number | Giá hiện tại dùng để tất toán lời/lỗ |
| `closedQuantity` | number | Khối lượng muốn đóng (có thể đóng một phần) |

```json
{
  "positionId": "4d01874d-d49f-4ce6-b12e-ef14b2675d49",
  "symbol": "BTCUSDT",
  "currentPrice": 63093,
  "closedQuantity": 0.02
}
```

**Mô tả:** Đóng một phần hoặc toàn bộ vị thế đang mở tại `currentPrice`, tính lời/lỗ và cộng/trừ về ví của user.

> **Lưu ý:** HTTP method của endpoint này hiện đang giả định là `POST` theo pattern chung của các API có body — kiểm tra lại đúng method thật trong code trước khi dùng chính thức (có thể là `PUT`/`PATCH`).

---

### 2.3 Tạo điều kiện lệnh (Take Profit / Stop Loss)

**`POST /api/order-conditions`** · Auth: Bearer Token

**Request body:**

| Field | Kiểu | Mô tả |
|---|---|---|
| `positionId` | string (UUID) | ID vị thế được gắn điều kiện |
| `userId` | string (UUID) | ID user sở hữu vị thế |
| `type` | string | `TAKE_PROFIT` hoặc `STOP_LOSS` |
| `triggerPrice` | number | Giá kích hoạt điều kiện |
| `quantity` | number | Khối lượng sẽ đóng khi điều kiện được kích hoạt |

```json
{
  "positionId": "4d01874d-d49f-4ce6-b12e-ef14b2675d49",
  "userId": "22222222-2222-2222-2222-222222222222",
  "type": "TAKE_PROFIT",
  "triggerPrice": 64795.3,
  "quantity": 0.03
}
```

**Mô tả:** Gắn điều kiện TP/SL cho một vị thế đang mở. Order-service liên tục lắng nghe giá từ Kafka; khi giá thị trường chạm `triggerPrice`, hệ thống tự động đóng `quantity` tương ứng của vị thế.

---

### 2.4 Xoá lệnh Limit (chưa khớp)

**`DELETE /api/order/{orderId}`** · Auth: Bearer Token

**Ví dụ:**
```
DELETE http://localhost:8000/api/order/55dc2781-608f-4e7c-a844-023f9fe108b6
```

**Mô tả:** Huỷ một lệnh Limit chưa được khớp.

---

### 2.5 Xoá điều kiện lệnh (TP/SL)

**`DELETE /api/order-conditions/{conditionId}`** · Auth: Bearer Token

**Headers:**

| Key | Value |
|---|---|
| `X-User-Id` | UUID của user, ví dụ `22222222-2222-2222-2222-222222222222` |

**Ví dụ:**
```
DELETE http://localhost:8000/api/order-conditions/202eeff7-ca07-40f2-939f-7cdce5a240e7
```

**Mô tả:** Huỷ điều kiện Take Profit / Stop Loss đã gắn cho một vị thế.

---

## 3. Market Data

### 3.1 Lấy dữ liệu nến (klines)

**`GET /api/klines`** · Auth: không cần

**Query params:**

| Param | Kiểu | Mô tả |
|---|---|---|
| `symbol` | string | Mã cặp coin, VD `BTCUSDT` |
| `interval` | string | Khung thời gian nến, VD `1h`, `15m`, `1d` |

**Ví dụ:**
```
GET http://localhost:8000/api/klines?interval=1h&symbol=BTCUSDT
```

**Mô tả:** Trả về dữ liệu nến lịch sử (OHLCV) cho `symbol` theo khung thời gian `interval`, lấy từ Binance API.

---

### 3.2 Stream giá real-time

**`GET /api/price/stream/{symbol}`** · Auth: không cần

**Ví dụ:**
```
GET http://localhost:8000/api/price/stream/ethUSDT
```

**Mô tả:** Trả về stream giá real-time (Server-Sent Events) của `symbol` được subscribe, phục vụ hiển thị giá live trên client.

---

### 3.3 Lấy giá hiện tại

**`GET /api/price/{symbol}`** · Auth: không cần

**Ví dụ:**
```
GET http://localhost:8000/api/price/ethUSDT
```

**Mô tả:** Trả về giá hiện tại (giá tức thời, không stream) của `symbol`.
