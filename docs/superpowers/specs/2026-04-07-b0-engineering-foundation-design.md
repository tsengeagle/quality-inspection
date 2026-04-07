# B0 Engineering Foundation — Design Spec

> Created: 2026-04-07
> Phase: B0 底座
> Scope: Domain Event 基礎設施、Audit Trail、HospitalId 解析、Scope Guard

---

## 1. Goal

建立 B1/B2 所需的共用基礎設施：

- Domain Event base class + Spring ApplicationEvent 發布機制
- Audit Trail 表 + EventListener 自動記錄
- HospitalId 解析（環境變數 > JWT claim > empty）
- Scope Guard 工具方法（ActorContext 擴充）
- TestTokenFactory 更新（支援 hospitalId）

---

## 2. Domain Event 基礎設施

### 2.1 DomainEvent Base Class

位置：`domain/common/DomainEvent.java`

```java
public abstract class DomainEvent extends ApplicationEvent {
    private final String eventId;       // UUID
    private final Instant occurredAt;
    private final String actorUserId;
    private final String actorUserName;
    private final String hospitalId;    // nullable
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String detail;        // JSON, nullable
}
```

- 建構子需傳入 `Object source`（Spring ApplicationEvent 要求），通常傳 `this`（application service）

- 繼承 Spring `ApplicationEvent`，直接用 Spring 的 event 機制
- `eventId` 由建構時自動產生 UUID
- `occurredAt` 由建構時自動取 `Instant.now()`
- `entityType` / `entityId` / `action` 由子類提供
- `actorUserId` / `actorUserName` / `hospitalId` 由 application service 傳入

### 2.2 DomainEventPublisher Interface

位置：`application/common/DomainEventPublisher.java`

```java
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
```

### 2.3 SpringDomainEventPublisher

位置：`infrastructure/event/SpringDomainEventPublisher.java`

```java
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
```

---

## 3. Audit Trail 基礎設施

### 3.1 audit_trail 表 Schema

```sql
CREATE TABLE audit_trail (
    id              VARCHAR(36)  PRIMARY KEY,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       VARCHAR(36)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    actor_user_id   VARCHAR(50)  NOT NULL,
    actor_user_name VARCHAR(100) NOT NULL,
    hospital_id     VARCHAR(50),
    occurred_at     TIMESTAMP    NOT NULL,
    detail          TEXT
);
```

- `detail`：JSON TEXT，可 nullable，存放事件相關的額外資訊
- `hospital_id`：nullable，SYSTEM_ADMIN 全局操作時為 null

### 3.2 AuditTrailEntity

位置：`infrastructure/audit/AuditTrailEntity.java`

JPA entity 對應 audit_trail 表。

### 3.3 AuditTrailJpaRepository

位置：`infrastructure/audit/AuditTrailJpaRepository.java`

Spring Data JPA repository。

### 3.4 AuditTrailListener

位置：`infrastructure/audit/AuditTrailListener.java`

```java
@Component
public class AuditTrailListener {
    private final AuditTrailJpaRepository repository;

    @EventListener
    public void handle(DomainEvent event) {
        AuditTrailEntity entity = new AuditTrailEntity();
        entity.setId(event.getEventId());
        entity.setEntityType(event.getEntityType());
        entity.setEntityId(event.getEntityId());
        entity.setAction(event.getAction());
        entity.setActorUserId(event.getActorUserId());
        entity.setActorUserName(event.getActorUserName());
        entity.setHospitalId(event.getHospitalId());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setDetail(event.getDetail());
        repository.save(entity);
    }
}
```

- 使用 `@EventListener`（同步，同一交易內）
- 不做 outbox / async consumer

### 3.5 流程

```
Application Service
    → eventPublisher.publish(new XxxEvent(...))
        → Spring ApplicationEventPublisher
            → AuditTrailListener.handle(event)
                → AuditTrailEntity → save to DB
```

---

## 4. HospitalId 解析

### 4.1 TzJwtParser 變更

新增解析 JWT 的 `hospital_id` claim。

引入 Spring property：
```yaml
quality-inspection:
  hospital-id: ${QUALITY_INSPECTION_HOSPITAL_ID:}
```

優先順序：
1. 環境變數 `QUALITY_INSPECTION_HOSPITAL_ID`（透過 Spring property）
2. JWT `hospital_id` claim
3. `Optional.empty()`

```java
// TzJwtParser 邏輯
@Value("${quality-inspection.hospital-id:}")
private String configuredHospitalId;

// 在解析 JWT 時
Optional<String> hospitalId;
if (configuredHospitalId != null && !configuredHospitalId.isBlank()) {
    hospitalId = Optional.of(configuredHospitalId);
} else {
    String jwtHospitalId = claims.get("hospital_id");
    if (jwtHospitalId != null && !jwtHospitalId.isBlank()) {
        hospitalId = Optional.of(jwtHospitalId);
    } else {
        hospitalId = Optional.empty();
    }
}
```

### 4.2 部署模式

每個 hospital 一個 instance。環境變數設定後，該 instance 所有使用者都套用該 hospitalId。

---

## 5. Scope Guard（ActorContext 擴充）

### 5.1 新增方法

```java
public String requireHospitalId() {
    return hospitalId.orElseThrow(() ->
        new AccessDeniedException("Hospital context required"));
}

public void requireHospitalScope(String targetHospitalId) {
    String myHospitalId = requireHospitalId();
    if (!myHospitalId.equals(targetHospitalId)) {
        throw new AccessDeniedException("Hospital scope mismatch");
    }
}
```

### 5.2 AccessDeniedException

新增或複用既有的 `AccessDeniedException`（domain exception），由 `GlobalExceptionHandler` 映射為 HTTP 403。

---

## 6. TestTokenFactory 更新

```java
public static String hospitalManagerToken(String hospitalId) {
    // JWT 中包含 HOSPITAL_MANAGER role + hospital_id claim
}

public static String systemAdminToken() {
    // JWT 中包含 SYSTEM_ADMIN role，hospital_id 可帶可不帶
}

public static String systemAdminToken(String hospitalId) {
    // 指定 hospitalId 的 SYSTEM_ADMIN
}
```

測試慣例：
- 標準 hospitalId: `"H001"`
- 跨院測試: `"H001"` vs `"H002"`

---

## 7. application.yml 變更

```yaml
quality-inspection:
  hospital-id: ${QUALITY_INSPECTION_HOSPITAL_ID:}
```

---

## 8. 測試清單

| 測試 | 類型 | 驗證 |
|---|---|---|
| DomainEvent base class | Unit | eventId/occurredAt 自動填入 |
| AuditTrailListener | Integration | event → audit_trail 表寫入 |
| TzJwtParser hospitalId 解析 | Unit | 環境變數 > JWT > empty |
| ActorContext.requireHospitalId | Unit | 有值返回 / 無值 throw |
| ActorContext.requireHospitalScope | Unit | 匹配 pass / 不匹配 throw |
| GlobalExceptionHandler AccessDeniedException | API | 403 response |
| TestTokenFactory hospitalId | Unit | token 含 hospital_id claim |

---

## 9. 不包含

- Outbox pattern
- Async event consumer
- Notification
- 複雜 event routing
- Domain event persistence（只透過 audit_trail 間接保存）
