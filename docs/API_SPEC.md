# SNS Feed API 명세서

| 항목 | 내용                                                                   |
|---|----------------------------------------------------------------------|
| 문서 버전 | v0.4 (2026-11-18)                                                    |
| 대상 | Spring Boot 백엔드 (`com.example.login`), Vue 프론트엔드                     |
| 현재 구조 | 모듈러 모놀리스 (단일 서버 `:8081`, MySQL `user_db`)                            |
| 목표 구조 | user-service / post-feed-service / notification-service(Spring) 로 분리 |

**상태 표기**

| 표기 | 의미 |
|---|---|
| ✅ 구현 | 코드가 있고 동작 확인됨 |
| 🚧 진행 | 코드가 있으나 수정이 남음 (§10 참고) |
| 📝 설계 | 이 문서에서 정의, 아직 코드 없음 |

---

## 목차

- [1. 서비스 경계](#1-서비스-경계)
- [2. 공통 규약](#2-공통-규약)
  - [2.1 기본](#21-기본)
  - [2.2 인증](#22-인증)
  - [2.3 에러 응답](#23-에러-응답)
  - [2.4 커서 페이지네이션](#24-커서-페이지네이션)
  - [2.5 동시성 규칙](#25-동시성-규칙-)
- [3. 인증 (Auth)](#3-인증-auth)
  - [3.1 로그인](#31-로그인-post-userslogin)
  - [3.2 로그아웃](#32-로그아웃-post-userslogout)
  - [3.3 아이디 찾기](#33-아이디-찾기-post-usersfind-id)
  - [3.4 비밀번호 변경](#34-비밀번호-변경-patch-userspassword)
  - [3.5 비밀번호 재설정](#35-비밀번호-재설정-)
- [4. 사용자 (User)](#4-사용자-user)
  - [4.1 회원가입](#41-회원가입-post-userssignup)
  - [4.2 내 정보](#42-내-정보-get-usersme)
  - [4.3 공개 정보 일괄 조회](#43-공개-정보-일괄-조회-get-usersids123)
  - [4.4 회원 탈퇴](#44-회원-탈퇴--delete-usersme)
- [5. 팔로우 (Follow)](#5-팔로우-follow)
  - [5.1 팔로우 토글](#51-팔로우-토글-post-followstargetuserid)
  - [5.2 팔로잉 id 목록](#52-팔로잉-id-목록-get-followsfollowings)
- [6. 게시글 (Post)](#6-게시글-post)
  - [6.1 게시글 응답](#61-게시글-응답-postresponse)
  - [6.2 작성](#62-작성-post-posts)
  - [6.3 수정](#63-수정-put-postspostid)
  - [6.4 삭제](#64-삭제-delete-postspostid)
  - [6.5 답글 작성](#65-답글-작성-post-postspostidreplies)
  - [6.6 답글 목록](#66-답글-목록-get-postspostidrepliescursorsize)
  - [6.7 좋아요 토글](#67-좋아요-토글-post-postspostidlikes)
  - [6.8 재게시 토글](#68-재게시-토글--post-postspostidreposts)
  - [6.9 이미지 업로드](#69-이미지-업로드--post-images)
  - [6.10 인용 재게시](#610-인용-재게시--post-postspostidquotes)
- [7. 피드 (Feed)](#7-피드-feed-)
  - [7.1 요구사항](#71-요구사항)
  - [7.2 Redis 자료구조](#72-redis-자료구조)
  - [7.3 쓰기 (fan-out)](#73-쓰기-fan-out)
  - [7.4 팔로우 관계 얻기 (서비스 경계)](#74-팔로우-관계-얻기-서비스-경계)
  - [7.5 읽기](#75-읽기-get-feedscursorsize)
- [8. 프로필](#8-프로필-)
  - [8.1 화면 구성](#81-화면-구성)
  - [8.2 탭 규칙](#82-탭-규칙)
  - [8.3 API](#83-api)
- [9. 이벤트 (RabbitMQ 대비)](#9-이벤트-rabbitmq-대비-)
  - [9.1 메시지 브로커](#91-메시지-브로커-)
  - [9.2 이벤트 메시지 형식](#92-이벤트-메시지-형식-)
  - [9.3 Outbox (이벤트 유실 방지)](#93-outbox-이벤트-유실-방지-)
- [10. 데이터 모델](#10-데이터-모델)
  - [10.1 현재 테이블](#101-현재-테이블)
  - [10.2 추가할 것](#102-추가할-것-)
  - [10.3 현재 코드에 남은 작업](#103-현재-코드에-남은-작업-)
- [11. 미결 사항](#11-미결-사항)
- [12. 진행 순서](#12-진행-순서)
- [13. 알림 서비스 (notification-service)](#13-알림-서비스-notification-service-)
  - [13.1 인증](#131-인증)
  - [13.2 API](#132-api)
  - [13.3 SSE 형식](#133-sse-형식)
  - [13.4 저장](#134-저장)
  - [13.5 에러](#135-에러)
- [14. 외부 피드: X 홈 타임라인](#14-외부-피드-x-홈-타임라인-)
  - [14.1 X API 조건](#141-x-api-조건-2026-11-21-확인-바뀌므로-구현-직전에-다시-확인)
  - [14.2 구조](#142-구조)
  - [14.3 API (우리 서버)](#143-api-우리-서버)
  - [14.4 보안 체크](#144-보안-체크)
  - [14.5 비용 안전장치](#145-비용-안전장치-기능보다-먼저-만든다)
  - [14.6 비공식 서비스를 쓰지 않는 이유](#146-비공식-서비스를-쓰지-않는-이유-2026-11-21-조사)
- [부록 A. DB 피드 테이블 (Redis 대신 쓸 경우)](#부록-a-db-피드-테이블-redis-대신-쓸-경우)

---

## 1. 서비스 경계

| 서비스 | 소유 데이터 | 패키지 (모놀리스 기준) |
|---|---|---|
| user-service | users, follows, 인증(JWT 쿠키) | `auth`, `user`, `follow`, `security` |
| post-feed-service | posts(답글 포함), post_likes, reposts, 피드(Redis) | `post`, `postlike`, `repost`, `feed` |
| notification-service (Spring) | notifications | 별도 프로젝트 (포트 8084) |
| 공통 | 에러 형식, 예외 처리 | `common` |

**경계 규칙**

- post-feed 쪽 패키지는 user 쪽 클래스(Entity, Repository, Service)를 **import 하지 않는다.** 서비스 사이에는 **id(숫자)만** 주고받는다.
- 경계를 넘어야 하는 곳은 **인터페이스 하나**로 모은다. 예: 피드가 팔로워·팔로잉 목록을 얻는 `FollowReader` (§7.4).
- 닉네임 등 사용자 표시 정보는 프론트가 `GET /users?ids=` 로 따로 조회한다.

**지금 경계를 넘는 곳** (서비스 분리 전에 정리, 2026-11-21 확인)

| 위치 | 넘는 방향 | 분리 후 |
|---|---|---|
| `UsersService.deleteAccount` 가 Post·PostLike·Repost 리포지토리 사용 | user → post | `user.deleted` 이벤트로 넘긴다 (§4.4) |
| ~~`ProfileService.getPostCount` 가 `postRepository` 의 글 수 쿼리 사용~~ | user → post | ✅ 2026-11-23 정리: 헤더에서 빼고 post-feed 의 `GET /users/{id}/posts/count` 를 프론트가 따로 받는다 (§8.3) |
| 피드가 팔로우 목록 사용 | post → user | `FollowReader` (§7.4) |

---

## 2. 공통 규약

### 2.1 기본

| 항목 | 규칙 |
|---|---|
| Base URL | `/api/v1` |
| 형식 | 요청·응답 모두 `application/json; charset=UTF-8` |
| 시간 | ISO-8601 로컬 시각 문자열 (예: `2026-11-11T09:30:00`) |
| ID | 64bit 정수 (`Long`) |

### 2.2 인증

- 로그인에 성공하면 서버가 **httpOnly 쿠키** `user_token` (JWT, HS256)을 설정한다.
- 쿠키 속성: `HttpOnly; SameSite=Strict; Path=/; Max-Age=7200` (2시간). 운영 환경에서는 `cookie.secure=true` 로 `Secure` 를 붙인다.
- 인증이 필요한 API에서는 서버가 쿠키의 JWT에서 사용자 id를 꺼낸다(`@AuthenticationPrincipal Long userId`). **요청 본문이나 쿼리로 받은 userId는 신뢰하지 않는다.**
- 인증 없이 호출할 수 있는 API는 표의 "인증" 칸에 `-` 로 표시한다.

### 2.3 에러 응답

모든 에러는 같은 형식으로 응답한다.

```json
{ "code": "POST_NOT_FOUND", "message": "존재하지 않는 게시글입니다.", "status": 404 }
```

| HTTP | code | 언제 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 입력 검증 실패, 본문 JSON 오류, 경로 변수 타입 오류 |
| 400 | `INVALID_CURRENT_PASSWORD` | 비밀번호 변경 시 현재 비밀번호 불일치 |
| 400 | `SELF_FOLLOW` | 자기 자신 팔로우 |
| 400 | `TOO_MANY_IDS` | `GET /users?ids=` 가 100개 초과 |
| 401 | `INVALID_CREDENTIALS` | 로그인 실패 (이메일·비밀번호 구분 없이 같은 응답) |
| 401 | `UNAUTHORIZED` | 쿠키 없음, 만료, 위조 |
| 403 | `FORBIDDEN` | 남의 게시글 수정·삭제 |
| 404 | `USER_NOT_FOUND` | 없는 사용자 |
| 404 | `POST_NOT_FOUND` | 없는 글, 삭제된 글 |
| 404 | `NOT_FOUND` | 없는 경로 |
| 409 | `DUPLICATE_EMAIL` | 이미 가입된 이메일 |
| 409 | `DUPLICATE_RESOURCE` | DB unique 제약 위반 (동시 요청 등) |
| 409 | `LOCK_CONFLICT` | 락 충돌: 데드락(MySQL 1213), 락 대기 시간 초과(1205). 롤백됐으므로 다시 시도하면 된다 (§2.5) |
| 500 | `INTERNAL_ERROR` | 그 외 (서버 로그에만 상세 기록) |

📝 추가 예정: `DUPLICATE_HANDLE`(409), `INVALID_CURSOR`(400).

### 2.4 커서 페이지네이션

목록 API는 오프셋(page 번호) 대신 **커서**를 쓴다.

| 파라미터 | 의미 |
|---|---|
| `cursor` | 이전 응답의 `nextCursor`. 첫 요청은 생략 |
| `size` | 한 번에 가져올 개수. 기본값은 API마다 다르며 **최대 50** |

응답 공통 형태:

```json
{ "posts": [ ... ], "nextCursor": 123 }
```

- `nextCursor` 가 `null` 이면 마지막 페이지다.
- 서버는 `size + 1` 개를 조회해 다음 페이지가 있는지 판단하고, 넘친 1개는 버린다(COUNT 쿼리 불필요).
- 커서 값은 정렬 기준 컬럼(보통 `id`)의 마지막 값이다.

### 2.5 동시성 규칙 📝

같은 글에 좋아요·재게시가 동시에 몰리거나, 같은 사용자가 여러 탭에서 동시에 눌러도 기록과 카운트가 어긋나지 않게 한다 (2026-11-19 결정: **DB 비관적 락**).

| 규칙 | 내용 | 이유 |
|---|---|---|
| 카운트 | DB 에서 직접 `+1/-1` 하는 쿼리, `@Modifying(clearAutomatically = true, flushAutomatically = true)` | 읽고 더해서 저장하면 동시 요청에서 하나가 사라진다 |
| 토글의 첫 조회 | 글 행을 **쓰기 락**으로 읽는다 (`@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`) | 같은 글에 대한 토글이 그 자리에서 줄을 선다. 두 요청이 모두 "기록 없음"을 보고 insert 하는 경쟁(unique 위반)과, delete·insert 가 섞인 데드락이 생기지 않는다 |
| 락 순서 | 한 트랜잭션에서 여러 글 행을 잠그면 **자식(답글·인용) → 부모(원글)** 한 방향만 쓴다. 부모에서 자식을 업데이트하는 코드는 만들지 않는다 | 서로 반대 순서로 잠그면 데드락. 원글 삭제 때 재게시·인용 행을 건드리지 않는 것(§6.8)도 이 규칙 |
| 대기 시간 | 토글 트랜잭션에 짧은 타임아웃 (예: `@Transactional(timeout = 3)`) | MySQL 기본 락 대기는 50초 |
| 실패 응답 | unique 위반 → `409 DUPLICATE_RESOURCE`, 데드락·대기 초과 → `409 LOCK_CONFLICT` (§2.3) | 데이터는 롤백되어 안전. 500 대신 다시 시도할 수 있는 응답 |
| 이벤트 | 커밋 후 발행 (`AFTER_COMMIT`) | 트랜잭션 안에서 외부 호출을 하지 않아 락을 오래 잡지 않는다 |

- **분산 락(Redis)은 쓰지 않는다.** DB 가 하나라 서버가 여러 대여도 DB 락이 모든 요청을 줄 세운다. 지키려는 작업이 DB 트랜잭션 하나로 묶이지 않을 때(예: DB + Redis 를 함께 원자적으로) 다시 검토한다. 그때는 락이 트랜잭션을 **감싸야** 한다(락 획득 → 트랜잭션 → 커밋 → 락 해제).
- 인기 글 하나에 요청이 몰리면(핫 로우) 같은 행을 기다리느라 느려진다. 규모가 커지면 카운트를 Redis 에서 올리고 주기적으로 DB 에 반영하는 방식을 검토한다 (범위 밖).
- 확인: 같은 글에 같은 사용자 10개 + 다른 사용자 100개 요청을 동시에 보내 `repost_count` 와 `reposts` 행 수가 같고 500 이 없는지 (테스트 단계).

---

## 3. 인증 (Auth)

| 상태 | 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|---|
| ✅ | POST | `/users/login` | - | 로그인, 쿠키 발급 |
| ✅ | POST | `/users/logout` | - | 쿠키 삭제 |
| ✅ | POST | `/users/find-id` | - | 닉네임으로 이메일 찾기 |
| ✅ | PATCH | `/users/password` | 필요 | 비밀번호 변경 |
| 📝 | POST | `/users/password-reset` | - | 재설정 메일 요청 |
| 📝 | POST | `/users/password-reset/confirm` | - | 토큰으로 새 비밀번호 설정 |

### 3.1 로그인 `POST /users/login`

요청

```json
{ "email": "a@example.com", "password": "password123" }
```

| 필드 | 규칙 |
|---|---|
| email | 필수 |
| password | 필수 |

응답 `200`, 본문 없음, `Set-Cookie: user_token=...`

에러: `400 VALIDATION_ERROR`, `401 INVALID_CREDENTIALS`

### 3.2 로그아웃 `POST /users/logout`

응답 `200`, `Set-Cookie: user_token=; Max-Age=0`

### 3.3 아이디 찾기 `POST /users/find-id`

요청 `{ "nickname": "tester" }` (필수, 50자 이하)

응답 `200` — 닉네임은 중복될 수 있으므로 목록으로, 마스킹해서 준다.

```json
{ "emails": ["tes***@example.com", "a***@other.com"] }
```

일치하는 계정이 없으면 `{ "emails": [] }`.

### 3.4 비밀번호 변경 `PATCH /users/password`

요청

```json
{ "currentPassword": "old-password", "newPassword": "new-password" }
```

| 필드 | 규칙 |
|---|---|
| currentPassword | 필수 |
| newPassword | 필수, 8~72자 |

응답 `200`, 본문 없음. 에러: `400 INVALID_CURRENT_PASSWORD`, `401`.

### 3.5 비밀번호 재설정 📝

1. `POST /users/password-reset` `{ "email" }` → 항상 `204` (가입 여부를 노출하지 않음).
2. 서버는 일회용 토큰을 만들어 **해시만** 저장하고(유효 15분), 링크 `/password-reset/confirm#token=...` 를 메일로 보낸다(개발: Mailpit).
3. `POST /users/password-reset/confirm` `{ "token", "newPassword" }` → `204`. 만료·사용된 토큰은 `400 INVALID_RESET_TOKEN`.

프론트 화면은 구현되어 있다.

---

## 4. 사용자 (User)

| 상태 | 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|---|
| ✅ | POST | `/users/signup` | - | 회원가입 |
| ✅ | GET | `/users/me` | 필요 | 내 정보 (로그인 상태 복원용) |
| ✅ | GET | `/users?ids=1,2,3` | 필요 | 공개 정보 일괄 조회 |
| 📝 | GET | `/users/{userId}/profile` | 필요 | 프로필 헤더 (§8) |
| 📝 | PATCH | `/users/me` | 필요 | 프로필 수정 (§8) |
| 📝 | DELETE | `/users/me` | 필요 | 회원 탈퇴 (§4.4) |

### 4.1 회원가입 `POST /users/signup`

```json
{ "email": "a@example.com", "password": "password123", "nickname": "tester", "handle": "sns_user1" }
```

| 필드 | 규칙 |
|---|---|
| email | 필수, 이메일 형식, 100자 이하, 중복 불가 |
| password | 필수, 8~72자 (BCrypt 한계 72바이트) |
| nickname | 필수, 50자 이하 |
| handle | 📝 필수, 4~15자, 영문 소문자·숫자·`_`, 첫 글자는 영문 (`^[a-z][a-z0-9_]{3,14}$`), 중복 불가. 대문자는 프론트가 소문자로 바꿔 보낸다 |

응답 `200` 문자열 `"회원가입 완료"` (🚧 §10: `201` + `{ "id" }` 로 변경 권장)

에러: `400 VALIDATION_ERROR`, `409 DUPLICATE_EMAIL`, 📝 `409 DUPLICATE_HANDLE`

### 4.2 내 정보 `GET /users/me`

```json
{ "id": 1, "email": "a@example.com", "nickname": "tester", "role": "USER" }
```

### 4.3 공개 정보 일괄 조회 `GET /users?ids=1,2,3`

- 최대 100개. 초과하면 `400 TOO_MANY_IDS`. `ids` 가 없으면 `[]`.
- 없는 id는 결과에서 빠진다.

```json
[ { "id": 1, "nickname": "tester", "profileImageUrl": null, "bio": null } ]
```

### 4.4 회원 탈퇴 📝 `DELETE /users/me`

요청 (쿠키만 탈취당해도 계정이 지워지지 않게 비밀번호를 다시 받는다)

```json
{ "password": "current-password" }
```

응답 `204`, 본문 없음. 로그인 쿠키를 만료시킨다 (로그아웃 §3.2 와 같은 `Set-Cookie`).
에러: `400 INVALID_CURRENT_PASSWORD` (비밀번호 틀림, §3.4 와 같은 코드), `401` (로그인 안 됨).

**처리** ([결정] 은 제안만 해 둔 것)

| 대상 | 처리 | 이유 |
|---|---|---|
| 사용자 행 | soft delete (`deleted_at` 기록) [결정] | 다른 테이블의 id 가 계속 유효하다. 조회에서는 `@SQLRestriction` 으로 뺀다 |
| 같은 이메일로 재가입 | 허용 [결정]. 탈퇴할 때 `email` 을 `deleted_{id}_{원래 이메일}` 처럼 바꿔 unique 를 비운다 | unique 제약을 그대로 둘 수 있다 |
| 내 글 | 함께 soft delete | X 와 같다. 답글 스레드·인용 상자는 이미 "삭제된 게시물" 처리가 있다 (§6.4, §6.10) |
| 내가 누른 좋아요·재게시 | 행 삭제 + **그 글들의 `like_count`·`repost_count` 를 DB 에서 `-1`** | "카운트 = 행 수" 가 깨지지 않게 (§6.7, §6.8) |
| 내 인용 글 | 글과 함께 삭제 + 원글 `quote_count - 1` | §6.10 |
| 팔로우 | 내가 한 것·받은 것 모두 삭제 | 상대의 팔로워·팔로잉 수가 바로 맞아야 한다 |
| 알림 | `user.deleted` 이벤트 발행 → 알림 서비스가 그 사용자의 알림(받은 것·보낸 것)을 지운다 | 알림 서비스는 users 테이블을 모른다 (§9, §13) |
| 30일 후 완전 삭제 | 미정. 배포 뒤 매일 도는 작업으로 (`@SQLRestriction` 을 우회하는 네이티브 쿼리) | 개인정보 보관 기간 |
| 이미 발급된 JWT | 토큰은 서버가 무효화할 수 없으므로, 인증 필터가 탈퇴한 사용자면 `401` | 쿠키를 지워도 다른 기기의 토큰이 남아 있을 수 있다 |

- 프론트: 프로필 수정 창 아래 "계정 삭제" → 비밀번호 입력 확인 창 → 성공하면 로그아웃 상태로 로그인 화면.

**누가 무엇을 정리하나** (2026-11-21 결정: 서비스 경계 §1 에 맞춰 `user.deleted` 이벤트로 나눈다)

처음 구현(2026-11-20)은 `UsersService.deleteAccount` 한 트랜잭션에서 posts·post_likes·reposts 까지 직접 지웠다. 서비스를 나누면 user-service 는 그 테이블에 접근할 수 없으므로, **각 데이터의 주인이 자기 것을 정리**한다.

```
[user-service] deleteAccount (한 트랜잭션)          [post-feed-service] user.deleted 리스너 (한 트랜잭션)
 1. 비밀번호 확인                                    1. 좋아요: 그 글들 like_count -1   → 행 삭제
 2. user.withdraw()                                  2. 재게시: 그 글들 repost_count -1 → 행 삭제
 3. 팔로우 삭제 (follows 는 user 쪽)                 3. 답글: 부모마다 comment_count -n (집계 → 반복)
 4. user.deleted 발행 (커밋 후) ── RabbitMQ ──▶      4. 인용: 원글마다 quote_count -n   (집계 → 반복)
                                                     5. 그 사용자 글 전부 soft delete   ← 3·4 다음
                                              ──▶ [notification-service] 그 사용자가 받은·보낸 알림 삭제
```

| 규칙 | 이유 |
|---|---|
| 최종 일관성 | 탈퇴 직후 잠깐 그 사용자의 글·숫자가 남았다가 정리된다. 허용 |
| 리스너 전체를 **한 트랜잭션** | 중간에 실패하면 전부 롤백 → 메시지를 다시 받아 처음부터 |
| 두 번 받아도 안전 (멱등) | 첫 실행에서 좋아요·재게시 행이 지워지고 글이 soft delete 되므로, 두 번째 실행의 서브쿼리·집계는 0건이라 아무것도 빠지지 않는다. **"빼기 먼저 → 지우기 나중", "soft delete 는 집계 다음" 순서가 이 성질을 만든다** |
| 이벤트 유실 | 커밋 후 발행 전에 죽으면 정리가 영영 안 된다 → 서비스 분리 직후 **Outbox** (§12) |

**분리 전 중간 단계** (모놀리스 안에서 경계부터 지킨다): `UsersService` 는 위 user-service 1~4 만 하고 post 쪽 리포지토리를 주입하지 않는다. post 패키지에 `UserDeletedListener` 를 두고 `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)` 로 정리한다. 분리할 때는 이 리스너의 입구만 `@RabbitListener` 로 바꾼다.

---

## 5. 팔로우 (Follow)

| 상태 | 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|---|
| ✅ | POST | `/follows/{targetUserId}` | 필요 | 팔로우 토글 |
| ✅ | GET | `/follows/followings` | 필요 | 내가 팔로우한 id 목록 |
| 📝 | GET | `/users/{userId}/followers` | 필요 | 팔로워 목록 (공개, 커서) |
| 📝 | GET | `/users/me/followings` | 필요 | 팔로잉 목록 (본인만, 커서) |

### 5.1 팔로우 토글 `POST /follows/{targetUserId}`

- 팔로우 중이면 취소, 아니면 팔로우한다.
- 동시에 두 번 눌러 unique 제약에 걸리면 `following: true` 로 응답한다.

응답 `200` `{ "following": true }`

에러: `400 SELF_FOLLOW`, `404 USER_NOT_FOUND`

📝 피드 연동: 팔로우·언팔로우 시 이벤트 발행 (§7.3).

### 5.2 팔로잉 id 목록 `GET /follows/followings`

응답 `200` `[2, 5, 9]` — 페이지 없음. 📝 §5 표의 커서 버전으로 대체 예정.

**공개 범위 (결정)**: 팔로잉 목록은 본인만, 팔로워 목록은 누구나 볼 수 있다.

---

## 6. 게시글 (Post)

X 방식을 따른다. **답글도 게시글**이며, `parentId` 로 관계를 표현한다.

```
원글 1  parentId=null rootId=null
답글 2  parentId=1    rootId=1
답글 3  parentId=2    rootId=1   ← 답글의 답글
```

| 상태 | 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|---|
| ✅ | POST | `/posts` | 필요 | 원글 작성 |
| ✅ | GET | `/posts/{postId}` | 필요 | 상세 |
| ✅ | PUT | `/posts/{postId}` | 필요 | 수정 (작성자만) |
| ✅ | DELETE | `/posts/{postId}` | 필요 | 삭제 (작성자만, soft delete) |
| ✅ | POST | `/posts/{postId}/replies` | 필요 | 답글 작성 |
| ✅ | GET | `/posts/{postId}/replies` | 필요 | 답글 목록 (커서) |
| ✅ | POST | `/posts/{postId}/likes` | 필요 | 좋아요 토글 |
| 📝 | POST | `/posts/{postId}/reposts` | 필요 | 재게시 토글 (§6.8) |
| 📝 | POST | `/posts/{postId}/quotes` | 필요 | 인용 재게시 작성 (§6.10) |
| 🚧 | GET | `/posts/me` | 필요 | 내 글 목록 → 프로필 탭(§8)으로 대체 |
| 🚧 | GET | `/posts/all` | 필요 | 전체 글 (개발용) → 피드(§7)로 대체 |

### 6.1 게시글 응답 `PostResponse`

```json
{
  "id": 10,
  "userId": 1,
  "parentId": null,
  "content": "본문",
  "imageUrl": null,
  "likeCount": 3,
  "commentCount": 2,
  "liked": false,
  "createdAt": "2026-11-11T09:30:00"
}
```

| 필드 | 설명 |
|---|---|
| parentId | 원글이면 `null`, 답글이면 부모 글 id |
| commentCount | **직접** 달린 답글 수 |
| liked | 요청한 사용자가 좋아요 했는지. 🚧 아직 응답에 없음 (§10) |
| repostCount, reposted | 📝 재게시 수, 요청한 사용자가 재게시했는지. `liked` 와 같은 방식(left join + projection)으로 채운다 (§6.8) |
| quoteId, quotedPost, quoteCount | 📝 인용 재게시 (§6.10) |
| imageUrls | 📝 이미지 주소 목록 (0~4장, 올린 순서). `imageUrl` 은 첫 장과 같다 (§6.11) |

`rootId` 는 서버 내부용이라 응답에 넣지 않는다.

### 6.2 작성 `POST /posts`

```json
{ "content": "본문", "imageUrl": null }
```

| 필드 | 규칙 |
|---|---|
| content | 필수(공백 불가), **1000자 이하** → `400 VALIDATION_ERROR`. 답글·인용·수정도 같다 |
| imageUrl | 선택. 📝 `imageUrls` 가 오면 무시한다 |
| imageUrls | 📝 선택. 최대 4개, 각 255자 이하 (§6.11) |

응답 `200` `PostResponse` (🚧 `201` 권장)

### 6.3 수정 `PUT /posts/{postId}`

요청은 6.2와 같다. 응답 `200` `PostResponse`.
에러: `403 FORBIDDEN`(작성자 아님), `404 POST_NOT_FOUND`

### 6.4 삭제 `DELETE /posts/{postId}`

- **soft delete**: `is_deleted = true` 로만 표시한다. 이후 모든 조회에서 자동으로 제외된다(`@SQLRestriction`).
- 답글을 지우면 부모의 `commentCount` 를 1 줄인다. 원글을 지워도 그 답글들은 남는다(X 방식). 프론트는 불러오지 못한 부모를 "삭제되었거나 볼 수 없는 게시물"로 표시한다.

응답 `204` (본문 없음). 에러: `403`, `404`.

### 6.5 답글 작성 `POST /posts/{postId}/replies`

- `{postId}` 가 부모 글이다. 본문은 6.2와 같다.
- 서버 처리: 부모 존재 확인(없거나 삭제됐으면 404) → `parentId = postId`, `rootId = 부모.rootId ?? 부모.id` → 부모 `commentCount + 1` → 저장.

응답 `200` `PostResponse` (새 답글, `parentId = postId`)

### 6.6 답글 목록 `GET /posts/{postId}/replies?cursor=&size=`

- 정렬: **오래된 순** (`id` 오름차순), 조건 `id > cursor`
- `size` 기본 10, 최대 50

응답 `200` `{ "posts": [PostResponse...], "nextCursor": 25 }`

### 6.7 좋아요 토글 `POST /posts/{postId}/likes`

- 눌렀으면 취소, 아니면 좋아요. 기록(`post_likes`)과 카운트(`posts.like_count`)를 **한 트랜잭션**에서 함께 바꾼다.
- 카운트는 DB에서 직접 `+1/-1` 하는 쿼리로 바꿔 동시 요청에도 숫자가 틀어지지 않게 한다. 0 아래로 내려가지 않는다.

응답 `200` `{ "liked": true }`. 에러: `404 POST_NOT_FOUND`

📝 재게시(§6.8)처럼 `{ "liked": true, "likeCount": 4 }` 로 맞추기 [결정]. 두 버튼이 같은 방식이면 프론트 코드가 하나로 된다.

📝 좋아요 누른 사람 목록 `GET /posts/{postId}/likes?cursor=&size=` → `{ "userIds": [...], "nextCursor" }` (부가 기능, 피드 이후).

### 6.8 재게시 토글 📝 `POST /posts/{postId}/reposts`

- 좋아요와 같은 구조. 기록은 `reposts` 테이블, 카운트는 `posts.repost_count`.
- 인용 재게시(내 글을 덧붙이는 재게시)는 **새 게시글**이라 따로 둔다 → §6.10. `reposts` 테이블에는 본문 컬럼을 넣지 않는다.
- 이미 재게시했으면 취소, 아니면 재게시. 기록과 카운트를 **한 트랜잭션**에서 바꾼다.
- 카운트는 DB 에서 직접 `+1/-1` 하는 쿼리로 바꾸고, 0 아래로 내려가지 않는다. 이 쿼리는 `@Modifying(clearAutomatically = true, flushAutomatically = true)` 로 둔다 (2026-11-18 좋아요 버그: flush 없이 clear 하면 같은 트랜잭션의 `delete` 가 사라졌다).

응답 `200` `{ "reposted": true, "repostCount": 6 }`. 에러: `404 POST_NOT_FOUND`

- `repostCount` 는 **업데이트한 뒤 DB 에서 다시 읽은 값**이다. 화면 숫자에 ±1 만 하면 그 사이 다른 사람이 재게시한 만큼 어긋나기 때문이다 (2026-11-19 결정).
- 메서드 앞에서 `findById` 로 읽어 둔 엔티티의 값을 쓰면 안 된다. 벌크 UPDATE 는 이미 읽어 둔 엔티티를 바꾸지 않으므로 **업데이트 전 숫자**가 나온다. 카운트만 읽는 쿼리를 따로 둔다.
- 그 뒤 다른 사람이 누른 것은 새로고침해야 보인다 (실시간 숫자는 범위 밖).
- 프론트는 응답의 `repostCount` 를 그대로 쓴다.

**단계**

| 단계 | 내용 |
|---|---|
| 1차 | 토글, 게시글 응답의 `repostCount` · `reposted`(§6.1), 알림(`repost.created`), 프로필 **재게시** 탭(§8.2) |
| 1.5차 | 인용 재게시 (§6.10). 1차 토글이 끝난 뒤. 게시물 탭 합치기와 상관없다 |
| 2차 | 프로필 **게시물** 탭에 원글 + 재게시 합치기(아래), 피드 `REPOST` 항목(§7) |

**규칙** ([결정] 은 제안만 해 둔 것)

| 항목 | 제안 | 이유 |
|---|---|---|
| 답글 재게시 | 허용 [결정] | X 와 같다. 답글도 게시글이다 |
| 내 글 재게시 | 허용 [결정] | X 와 같다. 알림은 알림 서비스가 셀프로 거른다 (§9.2) |
| 삭제된 글 | `404 POST_NOT_FOUND` | soft delete 된 글은 조회에서 빠진다. 좋아요와 같다 |
| 원글이 삭제되면 | 재게시 행은 남기고, 읽을 때 원글이 없으면 건너뛴다 | 피드의 "삭제된 글" 규칙(§7.1)과 같다. 글 삭제가 reposts 까지 지우면 다른 기능의 테이블을 알아야 한다 |
| 동시에 두 번 누름 | 글 행을 쓰기 락으로 먼저 읽어 두 요청이 줄을 선다 → 두 번째는 "기록 있음"을 보고 취소한다 (§2.5). 그래도 unique 에 걸리면 `409 DUPLICATE_RESOURCE` | 토글이므로 결과는 "재게시 → 취소" |
| 이벤트 | 재게시했을 때만 `repost.created` (취소 때는 안 보냄). 셀프 재게시도 발행 | 피드 단계에서 `repost.deleted` 추가 |

**2차: 게시물 탭 합치기 (미결 → §11)**

원글(`posts`)과 재게시(`reposts`)를 한 최신순 목록으로 보여준다. 정렬은 원글은 작성 시각, 재게시는 **재게시한 시각**(X 와 같음). 두 테이블의 id 는 서로 비교할 수 없어서 커서를 따로 정해야 한다.

| 방법 | 내용 | 장점 | 단점 |
|---|---|---|---|
| A. 시각 + id 복합 커서 | 두 쿼리를 각각 `size+1` 개씩 가져와 시각순으로 합친다. 커서는 `(시각, 종류, id)` | 테이블 추가 없음 | 커서가 복잡하고 같은 시각 처리 규칙이 필요 |
| B. 활동 테이블 | 원글 작성 · 재게시 때 `profile_items(user_id, type, post_id, created_at)` 에 한 줄씩 쓴다 | 조회가 한 쿼리, 커서가 단순 | 쓰기가 두 곳 |
| C. 피드 재사용 | 피드(§7)처럼 Redis 에 사용자별 활동 목록을 둔다 | 피드와 모양이 같음 | 캐시가 비면 다시 만들어야 함 |

내 글을 내가 재게시하면 같은 글이 두 번 나온다 → 원글만 보여준다 [결정].

**확인 목록**

- [ ] 재게시 → 취소 → 재게시 후 `repost_count` 와 `reposts` 행 수가 같다
- [ ] 다른 사람 글 재게시 → 작성자에게 알림 1개 / 내 글 재게시 · 취소 → 알림 없음
- [ ] 삭제된 글 → `404` / 원글 삭제 후 재게시 탭에서 그 항목이 빠진다
- [ ] 동시에 두 번 누르기 → 행 1개, 카운트 1

### 6.9 이미지 업로드 📝 `POST /images`

글쓰기 창에서 내 컴퓨터의 이미지를 끌어다 놓거나 붙여넣으면 프론트가 이 API 로 올리고, 받은 `url` 을 게시글 `imageUrl` 로 쓴다. (다른 웹페이지의 이미지를 끌어다 놓으면 업로드 없이 그 주소를 그대로 쓴다)

| 항목 | 내용 |
|---|---|
| 요청 | `multipart/form-data`, 필드 `file` 하나 |
| 허용 | `image/png`, `image/jpeg`, `image/gif`, `image/webp`, 5MB 이하 |
| 응답 `201` | `{ "url": "/api/v1/images/2f1c….png" }` (255자 이하, `posts.image_url` 에 그대로 저장) |
| 조회 | `GET /images/{fileName}` 이 파일을 돌려준다 |
| 에러 | `400 INVALID_IMAGE` (형식·빈 파일), `400 VALIDATION_ERROR` (파일 없음), `413 FILE_TOO_LARGE` (5MB 초과), 조회 `404 IMAGE_NOT_FOUND` |

- 파일 이름은 서버가 UUID 로 새로 짓는다. 사용자가 보낸 이름을 경로에 쓰면 `../` 로 다른 폴더에 쓸 수 있다.
- 확장자·Content-Type 은 믿지 말고 허용 목록과 비교한다.
- 저장 위치는 처음에는 서버 로컬 폴더, 서비스를 나누면 객체 저장소(S3 등)로 옮긴다. 응답이 주소만 주므로 프론트는 바뀌지 않는다.
- `url` 을 `/api/v1/...` 아래로 두면 개발(Vite 프록시)과 배포(게이트웨이) 모두 같은 출처라 추가 설정이 필요 없다.

### 6.10 인용 재게시 📝 `POST /posts/{postId}/quotes`

다른 글을 첨부해서 내 글을 쓰는 것. 일반 재게시(§6.8)와 달리 **인용은 새 게시글**이다. 자기 id, 좋아요, 답글을 가지므로 `reposts` 가 아니라 `posts` 에 저장한다.

> **결정 (2026-11-20): 재게시와 인용을 데이터로 묶지 않는다.** X 처럼 재게시도 `posts` 의 한 행(`repost_of_id`)으로 두면 프로필 게시물 탭 합치기(§6.8 2차)가 쉬워지지만, 모든 글 쿼리가 "본문 없는 재게시 행"을 구분해야 하고 좋아요·답글을 원글로 돌려야 한다. 1차 재게시(`reposts` 토글, 재게시 탭, 동시성 검증)를 다시 짜는 비용이 커서 테이블은 나눈 채로 두고, 화면에서만 🔁 메뉴(재게시 / 인용하기)로 묶는다.

| | 일반 재게시 §6.8 | 인용 재게시 |
|---|---|---|
| 저장 | `reposts` 행 | `posts` 행 + `quote_id` |
| 동작 | 토글 (한 글에 한 번) | 작성 (여러 번 가능, 취소는 글 삭제) |
| 본문 | 없음 | 있음 (§6.2 규칙) |
| 카운트 | 원글 `repost_count` | 원글 `quote_count` |
| 프로필 탭 | 재게시 탭 | 게시물 탭 (`parent_id IS NULL` 이라 그대로 들어간다) |

- 요청: §6.2 와 같다 `{ "content", "imageUrl" }`. `{postId}` 가 인용할 글이다.
- 서버 처리: 인용할 글 존재 확인(없거나 삭제됐으면 404) → `quoteId = postId`, `parentId = null` → 인용할 글 `quoteCount + 1` → 저장. **답글 작성(§6.5)과 같은 흐름**이다.
- 응답 `200` `PostResponse` (새 글, `quoteId = postId`). 에러: `404 POST_NOT_FOUND`, `400 VALIDATION_ERROR`

### 6.11 이미지 여러 장 📝 (2026-11-23)

한 글에 이미지를 **최대 4장**(X 와 같음) 붙인다. 업로드 API(§6.9)는 그대로 한 번에 한 장이고, 프론트가 장마다 따로 올려 받은 주소들을 `imageUrls` 로 보낸다.

| 항목 | 내용 |
|---|---|
| 요청 (§6.2·§6.3·§6.10) | `{ "content": "본문", "imageUrls": ["/api/v1/images/a.png", "https://..."] }` |
| 규칙 | 0~4개, 각 255자 이하, http(s) 또는 `/api/v1/images/...`. 5개 이상이면 `400 VALIDATION_ERROR` |
| 응답 (§6.1) | `imageUrls` (항상 배열, 없으면 `[]`) + `imageUrl` (= 첫 장, 없으면 `null`) |
| 수정 | `imageUrls` 를 보내면 목록 전체를 바꾼다 (부분 수정 없음) |

**저장**
- 새 테이블 `post_images(post_id, sort_order, url)`, PK `(post_id, sort_order)`. JPA 는 `@ElementCollection` + `@OrderColumn(name = "sort_order")` 로 간단히 만들 수 있다.
- `posts.image_url` 은 **첫 장을 그대로 복사해 둔다** (대표 이미지). 미디어 탭 쿼리(`image_url IS NOT NULL`, §8)와 인용 미리보기가 바뀌지 않는다. 두 값은 엔티티의 한 메서드에서만 같이 바꿔서 어긋나지 않게 한다.
- 목록 API 는 글마다 `post_images` 를 따로 읽으면 N+1 이 된다. `@BatchSize(size = 100)` 또는 `hibernate.default_batch_fetch_size=100` 으로 한 번에 묶어 읽는다.

**호환 (전환 기간)**
- 프론트는 `imageUrls` 와 `imageUrl`(첫 장)을 **둘 다** 보낸다. 백엔드가 아직 `imageUrls` 를 모르면 첫 장만 저장되고 깨지지는 않는다.
- 프론트는 응답에 `imageUrls` 가 없으면 `imageUrl` 하나짜리 목록으로 본다.
- 기존 글: `insert into post_images (post_id, sort_order, url) select id, 0, image_url from posts where image_url is not null;` 로 한 번 옮긴다.

**PostResponse 추가 필드** (§6.1)

| 필드 | 설명 |
|---|---|
| quoteId | 인용한 글 id. 인용이 아니면 `null` |
| quotedPost | 인용한 글의 `PostResponse`. **한 단계만** 넣는다 (그 안의 `quotedPost` 는 항상 `null`). 인용한 글이 삭제됐으면 `null` → 프론트는 `quoteId != null && quotedPost == null` 이면 "삭제된 게시물" 상자를 그린다 |
| quoteCount | 이 글을 인용한 글 수 |

`quotedPost` 를 응답에 넣는 이유: `parentId` 처럼 id 만 주면 목록 한 페이지에 프론트 요청이 글 수만큼 더 나간다. 서버는 페이지의 `quoteId` 들을 모아 IN 쿼리 한 번으로 채운다(`liked` 와 같은 방식).

**규칙** ([결정] 은 제안만 해 둔 것)

| 항목 | 제안 | 이유 |
|---|---|---|
| 답글 인용 | 허용 [결정] | 답글도 게시글이다 |
| 내 글 인용 | 허용 | 알림은 셀프로 걸러진다 (§9.2) |
| 인용의 인용 | 허용, 화면은 한 단계만 | X 와 같다. 끝없이 중첩되지 않게 |
| 인용이면서 답글 | 안 됨 | 인용은 원글이다 (`parent_id = null`) |
| 수정 (§6.3) | 본문만. `quoteId` 는 못 바꾼다 | |
| 인용 글 삭제 | 원글 `quoteCount - 1` | 답글 삭제 때 `commentCount - 1` 과 같다 |
| 원글이 삭제되면 | 인용 글은 남는다. `quotedPost = null` | §6.4 답글 규칙과 같다 |
| 버튼 숫자 | 🔁 옆에 `repostCount + quoteCount` [결정] | X 와 같다. 나눠 보려면 나중에 "인용 목록" |
| 카운트 쿼리 | `+1/-1` 쿼리, `@Modifying(clearAutomatically = true, flushAutomatically = true)` | §6.8 과 같다 |
| 이벤트 | `quote.created` (§9). 피드는 이 이벤트를 `POST` 항목으로 넣는다 | |

📝 인용 목록 `GET /posts/{postId}/quotes?cursor=&size=` → `{ posts, nextCursor }` (부가 기능, 나중에). 인덱스 `(quote_id, id)`.

**확인 목록**

- [ ] 인용 → 새 글이 생기고 원글 `quote_count` 가 1 늘어난다 / 인용 글 삭제 → 1 줄어든다
- [ ] 원글 삭제 후 인용 글 조회 → `quoteId` 는 있고 `quotedPost` 는 `null`
- [ ] 인용의 인용 → `quotedPost.quotedPost` 가 `null`
- [ ] 다른 사람 글 인용 → 작성자에게 알림 1개 (알림을 누르면 인용 글로 간다)

---

## 7. 피드 (Feed) 📝

### 7.1 요구사항

타임라인에서 **팔로우한 사람과 나의 활동**을 최신순으로 보여준다.

| 활동 (type) | 피드에 들어가는 조건 | 카드 위 표시 |
|---|---|---|
| `POST` | 원글 작성 | 없음 |
| `REPLY` | 답글 작성 | "○○님이 답글을 남김" |
| `REPOST` | 재게시 | "○○님이 재게시함" |
| `LIKE` | 좋아요 | "○○님이 좋아함" |

**결정 사항**

| 항목 | 결정 |
|---|---|
| 방식 | **push (fan-out on write)**: 활동이 일어날 때 팔로워들의 피드에 미리 넣는다 |
| 저장소 | **Redis** (기획서). 피드는 posts·likes·reposts·follows 로부터 **다시 만들 수 있는 파생 데이터**이므로 Redis에 둬도 원본 손실이 없다 |
| 내 활동 | 내 피드에도 넣는다 |
| 중복 | 같은 글을 팔로우한 B와 D가 좋아요하면 **두 항목**으로 넣는다 |
| 취소 | 좋아요·재게시 취소 시 해당 항목을 팔로워 피드에서 지운다 |
| 언팔로우 | 그 사람이 만든 항목을 내 피드에서 지운다 |
| 팔로우 | 그 사람의 최근 원글 20개를 내 피드에 채운다 (미결 → §11) |
| 삭제된 글 | 피드 항목은 남겨 두고, **읽을 때** 글 조회에서 빠지면 건너뛴다 |

### 7.2 Redis 자료구조

```
feed:seq                 INCR 로 만드는 전역 순번 (피드 항목의 정렬 기준이자 커서)

feed:{ownerId}           Sorted Set (피드 주인별 1개)
  score  = seq
  member = "{type}:{postId}:{actorId}"     예) "LIKE:10:3"
```

| 설계 선택 | 이유 |
|---|---|
| 시간(ms) 대신 `seq` 를 score 로 | 같은 밀리초의 항목끼리 순서가 겹치지 않고, 커서로 쓰기에 안전하다 |
| member 에 type·actor 포함 | "B의 좋아요" 와 "D의 좋아요" 가 서로 다른 member 가 되어 두 항목으로 저장된다. 같은 사람의 같은 행동은 같은 member 라 중복되지 않는다 |
| 피드당 최대 **800**개 | 넣을 때마다 `ZREMRANGEBYRANK feed:{ownerId} 0 -801` 로 오래된 것을 잘라 메모리를 제한한다 |

> 처음 설계한 DB `feed` 테이블은 Redis 로 대체되므로 만들지 않는다. 같은 내용을 DB 에 둘 경우의 스키마는 부록 A 참고.

### 7.3 쓰기 (fan-out)

활동이 생기면 **트랜잭션 커밋 후** 이벤트를 발행하고, 피드 핸들러가 Redis 를 갱신한다.

```
PostCreated / ReplyCreated / Liked / Reposted
  → @TransactionalEventListener(phase = AFTER_COMMIT)
  → owners = 작성자(actor)의 팔로워 id 목록 + actor 자신
  → 각 owner 에 ZADD feed:{owner} {seq} "{type}:{postId}:{actorId}" + 800개로 자르기

Unliked / Unreposted
  → 각 owner 에 ZREM feed:{owner} "{type}:{postId}:{actorId}"

Unfollowed(follower, followee)
  → feed:{follower} 를 ZSCAN 해서 actorId == followee 인 member 를 ZREM

Followed(follower, followee)
  → followee 의 최근 원글 20개를 feed:{follower} 에 추가 (score 는 새 seq)
```

- 커밋 **후** 발행해야 롤백된 활동이 피드에 들어가지 않는다.
- 지금은 Spring 이벤트로 같은 서버에서 처리하고, 서비스를 나누면 **RabbitMQ** 로 바꾼다. 이벤트 이름·필드는 그대로 유지한다 (§9).
- 팔로워가 매우 많은 계정(수만 명)은 fan-out 비용이 커진다. 이 프로젝트 규모에서는 고려하지 않는다.

### 7.4 팔로우 관계 얻기 (서비스 경계)

```java
// feed 패키지에 둔다
public interface FollowReader {
    List<Long> followerIds(Long userId);   // userId 를 팔로우하는 사람들 → push(fan-out) 대상
    List<Long> followingIds(Long userId);  // userId 가 팔로우하는 사람들 → pull(캐시 재구성) 대상
}
```

- 지금: `follow` 패키지가 FollowRepository 로 구현한다. (`findFollowingIds` 는 이미 있음, 팔로워 쪽 쿼리는 추가 필요)
- MSA 분리 후: user-service 를 HTTP 로 호출하는 구현으로 바꾼다. 피드 코드는 바뀌지 않는다.

### 7.5 읽기 `GET /feeds?cursor=&size=`

| 파라미터 | 설명 |
|---|---|
| cursor | 이전 응답의 `nextCursor` (seq). 생략하면 최신부터 |
| size | 기본 20, 최대 50 |

**처리**

1. `ZREVRANGEBYSCORE feed:{me} (cursor -inf LIMIT 0 size+1` (첫 요청은 `+inf`)
2. member 를 `type / postId / actorId` 로 나눈다.
3. postId 들로 글을 **한 번에** 조회한다 (`findAllById`). 삭제된 글은 결과에 없으므로 그 항목은 건너뛴다.
4. 그 글들 중 내가 좋아요 한 것을 **IN 쿼리 한 번**으로 구해 `liked` 를 채운다.
5. `size+1` 규칙으로 `nextCursor` 를 정한다.

**캐시가 비었을 때** (새 사용자, Redis 재시작): 팔로잉과 나의 **원글**을 DB 에서 최신순으로 조회해 응답하고(pull), 그 결과로 `feed:{me}` 를 다시 채운다.

**응답** `200`

```json
{
  "items": [
    {
      "cursor": 1042,
      "type": "LIKE",
      "actorId": 3,
      "post": { "id": 10, "userId": 7, "parentId": null, "content": "...", "likeCount": 4, "commentCount": 0, "liked": true, "createdAt": "..." }
    }
  ],
  "nextCursor": 1031
}
```

- 피드 항목은 글이 아니라 **활동**이므로 `posts` 대신 `items` 를 쓴다. 같은 글이 여러 번 나올 수 있다.
- 프론트는 `actorId`, `post.userId` 를 모아 `GET /users?ids=` 로 닉네임을 구한다.
- 프론트 `FeedView` 는 현재 `{ posts, nextCursor }` 를 기대한다. 이 형태가 확정되면 프론트를 맞춘다.

---

## 8. 프로필 📝

### 8.1 화면 구성

X 와 Threads 를 섞는다.

| 영역 | 내용 | 참고 |
|---|---|---|
| 상단 바 (sticky) | 뒤로가기, 이름, 게시물 수 / 모바일에서는 이름·게시물 수 대신 로고 | X |
| 배너, 프로필 사진 | 누르면 크게 보기 | X |
| 이름, `@handle`, 소개 | | X |
| 가입일 | "2026년 11월 가입" | X |
| 팔로잉 / 팔로워 수 | 팔로잉 목록은 본인만, 팔로워 목록은 공개 | 결정 사항 |
| 버튼 | 본인: "프로필 수정" / 타인: "팔로우" | Threads |
| 탭 | 게시물 · 답글 · 재게시 · 미디어 | X |

본인 프로필의 "프로필 수정"에서 이름·@handle·소개·사진을 바꾸고, 비밀번호 변경(§3.4)으로 이동할 수 있다.

### 8.2 탭 규칙

| 탭 | 보여줄 것 | 조회 조건 |
|---|---|---|
| 게시물 | 원글 + 재게시 (최신순) | 원글: `user_id = ? AND parent_id IS NULL` / 재게시: `reposts.user_id = ?`. 재게시 합치기는 2차 (§6.8) |
| 답글 | 그 사용자가 쓴 답글 | `user_id = ? AND parent_id IS NOT NULL` |
| 재게시 | 재게시만, **재게시한 순서**(최신순). 커서는 `reposts.id`, `posts` 는 원글의 `PostResponse` | `reposts.user_id = ?`. 원글이 삭제된 항목은 건너뛴다 (한 페이지가 `size` 보다 적을 수 있음). **두 단계 조회**(피드 §7.5 와 같은 방식): ① `reposts` 를 커서로 `size+1` 개 ② 그 `post_id` 들로 글 조회 + `liked`·`reposted` IN 쿼리 ③ ① 의 순서로 정렬. `nextCursor` 는 ① 의 마지막 `reposts.id` (2026-11-19 결정) |
| 미디어 | 이미지가 있는 글 | `user_id = ? AND image_url IS NOT NULL` |

### 8.3 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/users/{userId}/profile` | 프로필 헤더 |
| PATCH | `/users/me` | 프로필 수정 |
| GET | `/users/{userId}/posts?tab=posts\|replies\|reposts\|media&cursor=&size=` | 탭 목록 (`{ posts, nextCursor }`) |
| GET | `/users/{userId}/posts/count` | 글 수 `{ "count": 57 }` (원글 + 답글 + 인용. 재게시는 posts 행이 아니라 제외, 삭제된 글 제외). **post-feed** 가 주인 |
| GET | `/users/handle/{handle}` | @아이디로 사용자 찾기 → `PublicUser` (§4.3 한 명 모양). 없거나 탈퇴했으면 `404 USER_NOT_FOUND` |
| GET | `/users/{userId}/followers?cursor=&size=` | 팔로워 (공개) |
| GET | `/users/me/followings?cursor=&size=` | 팔로잉 (본인만) |

팔로워·팔로잉 목록 응답 (최근에 팔로우한 순, `nextCursor` 는 follows 행 id)

```json
{
  "users": [
    { "id": 7, "handle": "tester", "nickname": "테스터", "bio": "소개", "profileImageUrl": null, "followedByMe": true }
  ],
  "nextCursor": 5012
}
```

`followedByMe` 는 보는 사람 기준의 버튼 상태다. 보는 사람 자신이 목록에 있으면 `null` 이고, 프론트는 그 줄에 버튼을 그리지 않는다.

프로필 헤더 응답

```json
{
  "id": 1, "handle": "tester", "nickname": "테스터", "bio": "소개",
  "profileImageUrl": null, "bannerImageUrl": null, "createdAt": "2026-11-01T10:00:00",
  "followerCount": 12, "followingCount": 30,
  "me": false, "followedByMe": true
}
```

- **글 수는 헤더에 없다** (2026-11-23 결정). 헤더는 users·follows(user-service) 데이터이고 글 수만 posts(post-feed) 데이터라, 프론트가 `GET /users/{userId}/posts/count` 를 따로 불러 합친다. user-service 가 post-feed 를 HTTP 로 부르게 하면(`FollowReader` 의 반대 방향) 두 서비스가 **서로를** 호출하게 되어 피했다.
- 분리 후 게이트웨이는 `/users/{id}/profile`·`/followers`·`/followings` 는 user-service 로, `/users/{id}/posts/**` 는 post-feed 로 보낸다.

프로필 수정 요청 (보낸 필드만 바꾼다)

```json
{ "nickname": "새이름", "handle": "new_handle", "bio": "소개", "profileImageUrl": "https://...", "bannerImageUrl": "https://..." }
```

| 필드 | 규칙 |
|---|---|
| handle | 4~15자, 영문 소문자·숫자·`_`, **첫 글자는 영문** (`^[a-z][a-z0-9_]{3,14}$`), 중복 불가 → `409 DUPLICATE_HANDLE`. 지울 수 없음 |
| nickname | 2~50자. 지울 수 없음 |
| bio | 255자 이하 |
| profileImageUrl, bannerImageUrl | 500자 이하 (`@Size(max = 500)`). http(s) 주소 또는 업로드 주소 `/api/v1/images/...` |

**보내지 않음 / 지우기 구분** (2026-11-20 결정, PATCH 의 null 문제)

| 요청 | 뜻 |
|---|---|
| 필드를 보내지 않음 (또는 `null`) | 그대로 둔다 |
| `""` (빈 문자열, 공백만 포함) | **지운다** → DB 에 `null` 저장. `bio`, `profileImageUrl`, `bannerImageUrl` 만 |
| 그 밖의 값 | 그 값으로 바꾼다 |

- `nickname`, `handle` 은 지울 수 없다. 빈 문자열이 오면 `400 VALIDATION_ERROR`.
- 서버는 규칙을 메서드 하나(`merge(들어온 값, 현재 값)`)에 모으고 필드마다 한 줄로 적용한다. 더 엄격하게 하려면 `JsonNullable` 로 "없음 / null / 값"을 타입으로 구분할 수 있다.

**필요한 변경**: `users` 에 `handle`(unique), `banner_image_url` 추가. 기존 계정은 handle 이 없으므로 컬럼을 NULL 허용으로 추가한 뒤 채운다(§10 마이그레이션 주의). 카운트는 처음에는 COUNT 쿼리로 구하고, 느려지면 컬럼으로 옮긴다. 프로필·배너 사진은 당분간 URL 문자열을 받는다 (게시글 이미지 업로드 §6.9 가 생기면 같은 API 를 쓸 수 있다).

---

## 9. 이벤트 (RabbitMQ 대비) 📝

| 이벤트            | 발행 위치 | 필드 | 소비자 |
|----------------|---|---|---|
| `post.created` | PostService | postId, actorId | 피드 |
| `reply.created` | PostService | actorId, recipientId(부모 글 작성자), postId(새 답글), parentId | 피드, 알림 |
| `like.created` | PostLikeService | actorId, recipientId(글 작성자), postId | 피드, 알림 |
| `repost.created` | RepostService | actorId, recipientId(원글 작성자), postId. 재게시했을 때만 (§6.8) | 피드, 알림 |
| `quote.created` | PostService | actorId, recipientId(인용된 글 작성자), postId(새 인용 글) (§6.10) | 피드(`POST` 항목), 알림 |
| `follow.created` | FollowService | actorId(팔로우한 사람), recipientId(팔로우 받은 사람) | 피드, 알림 |
| `user.deleted` | UsersService | actorId(탈퇴한 사람). 나머지 null (§4.4) | post-feed(글·좋아요·재게시 정리), 알림(그 사용자 알림 삭제), 피드 |

- 모두 **커밋 후** 발행한다.
- 필드 이름과 모양은 §9.2 봉투를 따른다. 알림 받을 사람의 id(`recipientId`)를 이벤트에 담는다. 알림 서비스는 posts 테이블을 모르기 때문이다.
- 사실로 발행하지만, 알림 서비스가 걸러낸다.

### 9.1 메시지 브로커 📝

**RabbitMQ** 로 정했다 (2026-11-17). Spring 은 `spring-boot-starter-amqp` 를 쓰고, `build.gradle` 의 Kafka 의존성은 지운다.

| 항목 | 값 |
|---|---|
| exchange | `sns.events` — topic exchange, durable |
| routing key | 이벤트 `type` 그대로. 예: `follow.created` |
| 알림 서비스 큐 | `notification.events` (durable) — `follow.created`, `reply.created`, `like.created`, `repost.created`, `quote.created`, `user.deleted` 를 바인딩 |
| post-feed 큐 | 서비스 분리 후 `post-feed.events` (durable) — `user.deleted`. 분리 전에는 모놀리스 안의 스프링 이벤트로 처리 (§4.4) |
| 처리 실패 | 알림 서비스가 처리하지 못한 메시지는 `notification.events.dlq` 로 보낸다 (dead-letter) |
| 메시지 | JSON, UTF-8, `content-type: application/json`, persistent |
| 전달 보장 | **최소 한 번**. 같은 메시지가 두 번 올 수 있으므로 소비자는 `eventId` 로 중복을 거른다 |

### 9.2 이벤트 메시지 형식 📝

모든 이벤트가 같은 모양(봉투)을 쓴다. 알림에 필요 없는 필드는 `null` 이다.

```json
{
  "eventId": "6f1c2a1e-0b7c-4c8e-9a4e-1d2f3a4b5c6d",
  "type": "reply.created",
  "occurredAt": "2026-11-17T09:30:00Z",
  "actorId": 7,
  "recipientId": 8,
  "postId": 120,
  "parentId": 101
}
```

| 필드 | 타입 | 뜻 |
|---|---|---|
| `eventId` | string (UUID) | 이벤트마다 새로 만든다. 중복 제거용 |
| `type` | string | §9 표의 이벤트 이름 |
| `occurredAt` | string (ISO-8601, UTC, `Z`) | 커밋된 시각 |
| `actorId` | number | 행동한 사람 |
| `recipientId` | number \| null | 알림 받을 사람. 피드 전용 이벤트(`post.created`)는 `null` |
| `postId` | number \| null | 관련 글 (팔로우는 `null`) |
| `parentId` | number \| null | 답글일 때 부모 글 |

이벤트별로 채우는 값

| type | actorId | recipientId | postId | parentId |
|---|---|---|---|---|
| `follow.created` | followerId | followeeId | null | null |
| `reply.created` | 답글 쓴 사람 | 부모 글 작성자 | 새 답글 id | 부모 글 id |
| `like.created` | 누른 사람 | 글 작성자 | 글 id | null |
| `repost.created` | 재게시한 사람 | 원글 작성자 | 원글 id | null |
| `quote.created` | 인용한 사람 | 인용된 글 작성자 | **새 인용 글 id** (알림을 누르면 인용 글로 간다) | null |
| `user.deleted` | 탈퇴한 사람 | null | null | null |

- `user.deleted` 는 알림을 만드는 이벤트가 아니라 **정리 요청**이다. 알림 서비스는 저장하지 않고 `recipient_id = actorId` 또는 `actor_id = actorId` 인 알림을 지운다.

- `actorId == recipientId`(내 글에 내가 좋아요·답글)여도 **발행한다**. 이벤트는 "일어난 일"이고, 누구에게 알릴지는 알림 서비스가 정한다. 알림 서비스는 `actorId == recipientId` 이면 저장하지 않는다 (셀프 팔로우는 API 에서 막는다).
- 이름(닉네임)은 이벤트에 넣지 않는다. 닉네임은 바뀔 수 있으므로 프론트가 `GET /users?ids=` 로 그때그때 받는다 (게시글 작성자 이름과 같은 방식).

### 9.3 Outbox (이벤트 유실 방지) 📝

**문제**: 지금은 커밋 후(`AFTER_COMMIT`)에 RabbitMQ 로 보낸다. 커밋과 발행 사이에 서버가 죽거나 RabbitMQ 가 꺼져 있으면 이벤트가 사라진다 (2026-11-19 동시성 테스트 로그의 "rabbitmq 이벤트 발행 실패"). 알림 하나는 작은 손실이지만, **`user.deleted` 가 사라지면 탈퇴자의 좋아요·재게시가 남의 글 숫자에 영원히 남는다** (§4.4). 모놀리스 안의 `UserDeletedListener` 도 메모리 이벤트라 커밋 직후 재시작되면 같은 일이 생긴다.

**방식**: 이벤트를 "보내는" 대신 **같은 트랜잭션에서 DB 에 저장**하고, 별도 작업이 나중에 보낸다.

```
[업무 트랜잭션]                                   [릴레이: @Scheduled, 몇 초마다]
 users / posts 변경                               outbox 에서 PENDING 을 오래된 순으로 N개
 outbox 에 insert (PENDING)   ── 커밋 ──▶          → RabbitMQ 발행 → SENT, sent_at 기록
 (둘 다 저장되거나, 둘 다 안 되거나)                 → 실패하면 PENDING 그대로, attempts + 1 → 다음 주기
```

| 항목 | 내용 |
|---|---|
| 테이블 | `outbox(id, event_id unique, type, payload JSON, status varchar(20) pending/sent/failed, attempts, created_at, sent_at)` / 인덱스 `(status, id)`. 처음 이름 `outbox_events` 는 2026-11-23 에 `outbox` 로 바꿨다 (`ddl-auto=update` 는 이름을 바꾸지 않고 새 테이블을 만드므로 옛 테이블은 직접 지운다) |
| 쓰는 곳 | 지금 `eventPublisher.publishEvent(...)` 하는 곳을 그대로 두고, 리스너를 `BEFORE_COMMIT`(같은 트랜잭션)에서 outbox insert 하도록 바꾼다. 서비스 코드는 거의 안 바뀐다 |
| 릴레이 | 서버가 여러 대면 같은 행을 두 번 보내지 않게 `SELECT ... FOR UPDATE SKIP LOCKED`. 한 대일 때는 단순 조회로 시작 |
| 포기 | `attempts` 가 N(예: 10) 넘으면 `FAILED` 로 두고 로그 → 사람이 확인 (무한 재시도 금지, §14.5 와 같은 원칙) |
| 정리 | `SENT` 는 7일 뒤 삭제 |
| 전달 보장 | **최소 한 번**. 발행 후 `SENT` 표시 전에 죽으면 다시 보낸다 → 받는 쪽이 중복에 안전해야 한다. 알림은 `event_id` unique, 탈퇴 정리는 두 번 실행해도 결과가 같다 (§4.4) — **받는 쪽은 이미 준비됨** |
| 모놀리스 안 리스너 | `UserDeletedListener` 도 메모리 이벤트 대신 **RabbitMQ 에서 받게** 바꾼다 (서비스 분리 때 어차피 할 일). 그래야 재시작에도 정리가 남는다 |

**확인 방법**: RabbitMQ 를 끈 채 탈퇴 → outbox 에 `PENDING` 1행 → RabbitMQ 를 켜면 몇 초 안에 `SENT` 가 되고, 카운트 = 행 수 확인 쿼리(§4.4)가 0건.

---

## 10. 데이터 모델

### 10.1 현재 테이블

| 테이블 | 주요 컬럼 | 제약·인덱스 |
|---|---|---|
| users | id, email, password_hash, nickname, role, profile_image_url(500), bio(255), created_at | email unique |
| follows | id, follower_id, following_id, created_at | (follower_id, following_id) unique |
| posts | id, user_id, parent_id, root_id, content, image_url, like_count, comment_count, is_deleted, created_at, updated_at | 📝 아래 인덱스 추가 |
| post_likes | id, post_id, user_id, liked_at | (post_id, user_id) unique |

### 10.2 추가할 것 📝

| 대상 | 내용 | 용도 |
|---|---|---|
| posts 인덱스 | `(parent_id, id)` | 답글 목록 |
| posts 인덱스 | `(user_id, id)` | 프로필 탭, 피드 재구성 |
| posts | `content` VARCHAR(1000) (2026-11-24 결정) | 기본 255자는 짧음. `ddl-auto=update` 는 기존 컬럼 길이를 안 바꾸므로 `alter table posts modify content varchar(1000)` 직접 실행 |
| posts | `repost_count INT NOT NULL DEFAULT 0` | 재게시 수 |
| reposts | id, user_id, post_id, created_at / unique(user_id, post_id), index(user_id, id) | 재게시 |
| posts | `quote_id BIGINT NULL`, `quote_count INT NOT NULL DEFAULT 0`, 인덱스 `(quote_id, id)` | 인용 재게시 (§6.10) |
| users | `handle VARCHAR(15) UNIQUE`, `banner_image_url VARCHAR(500)` | 프로필 |
| post_likes 인덱스 | `(post_id, id)` (지금의 이름 잘못된 인덱스를 대체) | 좋아요 누른 사람 목록 |
| post_images | post_id, sort_order, url(255) / PK (post_id, sort_order) | 이미지 여러 장 (§6.11). `posts.image_url` 은 첫 장 복사본 |

**마이그레이션 주의**: `ddl-auto=update` 는 행이 있는 테이블에 NOT NULL 컬럼(특히 `@CreationTimestamp`)을 추가하지 못한다. NULL 허용으로 직접 추가 → 값 채우기 순서로 진행한다.

### 10.3 현재 코드에 남은 작업 🚧

| 위치 | 내용 |
|---|---|
| PostResponse | `liked` 필드 없음. 답글 목록·상세·피드에서 IN 쿼리로 채운다 |
| `GET /posts/{postId}/likes` | 경로 변수 이름 불일치, `List<Long>` 파생 쿼리 → 주석 처리 후 6.7 형태로 재구현 |
| `PostRequestDto` | `content` 에 `@NotBlank` 가 있어도 `POST /posts` 에 `@Valid` 가 없다 |
| 응답 코드 | 생성 `201`, 삭제 `204` 로 정리 권장 (프론트는 2xx 모두 성공 처리) |
| `/posts/all`, `/posts/me` | 페이지 없음, 답글 포함 → 피드·프로필 탭으로 대체 후 삭제 |
| 설정 | `jwt.secret`, DB 비밀번호를 환경변수로 이동. 테스트용 JSON 파일 `.gitignore` |
| user 내부 | `UsersService` 가 `AuthRepository` 를 사용 → `UsersRepository` 로 통합 |

---

## 11. 미결 사항

| 질문 | 선택지 | 제안 |
|---|---|---|
| 팔로우 시 과거 글 채우기 | 안 함 / 최근 N개 | 최근 원글 20개 |
| 같은 글 LIKE 항목 묶기 | 항목마다 표시 / "B님 외 1명" | 지금은 항목마다, 나중에 프론트에서 묶기 |
| 피드에 LIKE 포함 | 포함 / 제외 | 포함(결정). 시끄러우면 설정으로 끄기 |
| 🔁 버튼 숫자 (§6.10) | 재게시만 / 재게시 + 인용 | 재게시 + 인용 (X 와 같음) |
| 삭제된 글 복구 (관리자 모드) | 예전 재게시·좋아요를 그대로 살림 / 복구 때 지움 | 미정. soft delete 라 `reposts`·`post_likes` 행과 unique 가 남아 있으므로, 복구하면 예전 기록이 그대로 살아난다. 관리자 모드를 만들 때 정한다 (§6.8) |
| 프로필 게시물 탭 합치기 (§6.8) | 시각+id 복합 커서 / 활동 테이블 / Redis 활동 목록 | 미정. 1차는 재게시 탭만 |
| 기존 계정 handle | 이메일 앞부분 + 숫자로 자동 생성 / 첫 로그인 때 입력 | 자동 생성 후 수정 가능 |
| 외부 SNS 연동 범위 | 크로스포스팅(우리 글을 X·Threads에도 게시) / 외부 글을 프로필에 가져오기 / 팔로잉의 외부 글 모으기 | 크로스포스팅부터. 팔로잉 글 모으기는 공식 API로 대부분 불가. 시작 전에 각 개발자 문서에서 가격·약관(저장 허용 범위) 재확인 |

---

## 12. 진행 순서

| 단계 | 내용 | 완료 기준 |
|---|---|---|
| 0 | 남은 작업 정리 (§10.3) | `liked` 표시, 좋아요 목록 주석 처리 |
| 1 | 피드 1단계: Redis 연결, `post.created` fan-out, `GET /feeds` | 팔로우한 사람의 새 글이 내 피드에 최신순으로 |
| 2 | REPLY·LIKE 항목, 취소·언팔로우 시 제거, 캐시 비었을 때 재구성 | 좋아요 취소하면 피드에서 사라짐 |
| 3 | 재게시 (§6.8) + 인용 재게시 (§6.10) + REPOST 항목 | 재게시가 피드·프로필에 보임, 인용 글에 원글 상자가 보임 |
| 4 | 프로필 (§8) | 4개 탭, 프로필 수정 |
| 5 | 알림 서비스 (Spring, RabbitMQ, SSE) | 답글·좋아요·팔로우 알림 실시간 수신 |
| 5.5 | 회원 탈퇴 (§4.4) ✅ | 탈퇴 후 모든 글의 카운트 = 실제 행 수, 알림 정리 (2026-11-21 확인) |

**앞으로의 순서** (2026-11-21 정리)

| 단계 | 내용 | 완료 기준 |
|---|---|---|
| 6 | 알림 마무리: SSE 구독 직후 첫 이벤트(주석) 전송, 15초 ping (§13.3) | 새로고침해도 🔔 가 바로 "실시간 연결됨" |
| 7 | 외부 피드: X 홈 타임라인 (§14). **Fake 로 개발**, 실제 호출은 확인용만 | X 연결 → X 탭에 내 홈 타임라인. 안전장치(§14.5) 동작 |
| 8 | 테스트 (JUnit + Testcontainers) | 동시성(카운트 = 행 수), 탈퇴 정리, 재게시 탭 페이지 넘김이 자동으로 검증됨 |
| 9 | **Outbox** (§9.3) | RabbitMQ 를 끈 채 탈퇴 → 다시 켜면 정리가 끝까지 됨 |
| 10 | 서비스 분리 | user / post-feed 별도 기동, `FollowReader`·글 수 조회 HTTP, `user.deleted` 를 RabbitMQ 로 수신 |
| 11 | Docker Compose → CI → 배포 (HTTPS, 모바일 브라우저) | 공개 주소에서 동작 |

- **Outbox 를 서비스 분리보다 앞에 둔 이유**: 탈퇴 정리가 `user.deleted` 이벤트에 의존하게 되면서(§4.4), 이벤트가 사라지면 남의 글 숫자가 영원히 틀어진다. 지금의 모놀리스 안 리스너도 메모리 이벤트라 커밋 직후 재시작되면 같은 문제가 생긴다.
- **테스트를 Outbox·분리보다 앞에 둔 이유**: 둘 다 큰 리팩터링이라, 지금 손으로 확인한 시나리오를 자동 테스트로 먼저 고정해 둔다.
- X 연동은 읽기 전용 동기 호출이라 이벤트 순서와 상관없다. 탈퇴 시 토큰 폐기만 `user.deleted` 를 받는다.

**범위 밖**: 여러 장 이미지·동영상(이미지 1장 업로드는 §6.9), X 에 글쓰기·크로스포스팅, 외부 피드를 우리 피드와 섞기(1차는 별도 탭), 해시태그, 검색.

---

## 13. 알림 서비스 (notification-service) 📝

별도 Spring Boot 프로젝트 (포트 `8084`, DB `notification_db`). 처음에는 .NET 으로 만들었고 Java 로 다시 만들기로 했다 (2026-11-17). 아래 API·이벤트 계약은 언어와 상관없이 같다.

### 13.1 인증

User-Service 가 발급한 JWT 를 **같은 비밀키로 직접 검증**한다. 알림 서비스는 토큰을 발급하지 않는다.

| 항목 | 값 |
|---|---|
| 토큰 위치 | 쿠키 `user_token` (httpOnly, `SameSite=Strict`, `Path=/`). `Authorization` 헤더는 쓰지 않는다 |
| 서명 | HMAC-SHA. jjwt 의 `Keys.hmacShaKeyFor` 는 키 길이로 알고리즘을 고른다 (32~47바이트 HS256, 48~63바이트 HS384, 64바이트 이상 HS512). 알림 서비스는 세 가지를 모두 허용하고 키로 검증한다 |
| 비밀키 | 환경변수 `JWT_SECRET` — Spring 의 `jwt.secret` 과 **같은 값**. 코드·설정 파일에 적지 않는다 |
| 사용자 id | 표준 클레임 `sub` (문자열 숫자, 예: `"7"`) |
| 그 밖의 클레임 | `nickname`, `role`, `iat`, `exp` (만료 검증 필수, 시계 오차 30초 허용) |
| 실패 | `401 {"code":"UNAUTHORIZED", ...}` |

### 13.2 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/notifications/subscribe` | SSE 구독 (실시간) |
| GET | `/api/v1/notifications?cursor=&size=` | 지난 알림 목록 (최신순, 커서 = 알림 id) |
| GET | `/api/v1/notifications/unread-count` | 안 읽은 개수 `{ "count": 3 }` |
| POST | `/api/v1/notifications/read-all` | 모두 읽음 → `204` |
| PATCH | `/api/v1/notifications/{id}/read` | 하나 읽음 → `204` (남의 알림이면 `404`) |

알림 한 개

```json
{ "id": 42, "type": "reply.created", "actorId": 7, "postId": 120, "createdAt": "2026-11-17T09:30:00Z", "read": false }
```

목록 응답: `{ "notifications": [...], "nextCursor": 30 }`

- 문구("OO님이 답글을 남겼습니다")는 **프론트가** `type` 과 `actorId` 의 현재 닉네임으로 만든다. 서버는 문구를 저장하지 않는다.

### 13.3 SSE 형식

```
id: 42
event: notification
data: {"id":42,"type":"reply.created","actorId":7,"postId":120,"createdAt":"2026-11-17T09:30:00Z","read":false}

: ping
```

- 이벤트 이름은 `notification`. 15초마다 `: ping` 주석을 보내 프록시가 연결을 끊지 않게 한다.
- 한 사용자가 여러 탭으로 접속하면 모든 연결에 보낸다.
- 다시 연결했을 때 놓친 알림은 SSE 로 다시 보내지 않는다. 프론트가 목록 API 로 채운다.

### 13.4 저장

- 알림 서비스 **전용 DB** (예: MySQL `notification_db`). user/post 테이블과 join 하지 않는다.
- `notifications`: id, recipient_id, type, actor_id, post_id, event_id(unique), is_read, created_at. 인덱스 `(recipient_id, id)`.
- `event_id` unique 로 같은 이벤트가 두 번 와도 알림은 한 번만 생긴다.

### 13.5 에러

다른 서비스와 같은 `{ "code", "message", "status" }` 형식. 응답 헤더 `X-Trace-Id` 를 붙인다.

---

## 14. 외부 피드: X 홈 타임라인 📝

사용자가 X 계정을 연결하면, 우리 앱 안에서 **그 사람의 X 홈 타임라인**(X 에서 팔로우한 사람들의 글, 최신순)을 본다. 읽기만 한다 (X 에 글쓰기는 범위 밖). 플랫폼 선택: X (2026-11-21 결정. 비교한 후보: Bluesky·Mastodon 은 무료, Threads 는 홈 피드 API 없음).

**방식: X 공식 API + 개발용 Fake** (2026-11-21 결정). 비공식 서비스를 쓰지 않는 이유는 §14.6.

### 14.1 X API 조건 (2026-11-24 재확인)

| 항목 | 내용 |
|---|---|
| 엔드포인트 | `GET /2/users/{id}/timelines/reverse_chronological` — 팔로우한 계정의 글, 알고리즘 정렬 없음, 최근 3,200개(또는 7일) |
| 인증 | **사용자 컨텍스트** 필수: OAuth 2.0 Authorization Code + **PKCE** (또는 OAuth 1.0a User Context). 앱 토큰(Bearer) 만으로는 안 된다 |
| 스코프 | `tweet.read`, `users.read` (+ 토큰 갱신용 `offline.access`) |
| rate limit | 사용자당 15분에 180회 |
| 요금 | 2026-02 부터 **사용량 과금(pay-per-use)** 이 기본, 일반 무료 등급 없음. 개발자 콘솔에서 크레딧을 먼저 사서 요청마다 차감. 글 읽기는 **돌려받은 글 1개당** $0.005, "내 데이터"(owned read) 는 $0.001 — 홈 타임라인이 어느 쪽인지는 문서에 없음 → 첫 호출 후 차감액으로 확인. 월 상한 200만~300만 글. 신규 가입 일회성 크레딧이 있다는 정보가 있으나 금액은 출처마다 다름 → 콘솔에서 확인. **(2026-11-26 문서 재확인)** 홈 타임라인은 owned read 목록에 없다 → 글 1개당 $0.005 로 본다. 사용자 읽기는 1명당 $0.010. `expansions=author_id` 로 함께 오는 작성자(`includes.users`)가 따로 과금되는지는 문서에 없음. 같은 글·사용자는 **UTC 하루 안에 다시 받아도 한 번만** 과금(중복 제거) |
| `exclude` | (2026-11-26 실측) `exclude=replies,retweets` 를 쓰면 첫 페이지에서 6개만 오고 `next_token` 이 없었다 → 더 보기가 끊긴다. **쓰지 않는다.** RT·답글이 섞여 보이는 것은 받아들임 |
| 약관 | 가져온 글을 표시할 때 작성자 표시, X 에서 삭제된 글은 우리 쪽에서도 지워야 한다(저장한다면). → **저장하지 않고 짧게 캐시만** 한다 |

**비용 예상** ($0.005 기준. owned read 면 1/5)

| 상황 | 비용 |
|---|---|
| 한 번 열기 (10개) | 약 $0.05 |
| 개발: Fake 로 만들고 확인용 실제 호출 20번 | **약 $1~2** |
| 개발: Fake 없이 실제 API 로 2주 (하루 50번 열기, 캐시로 절반) | 약 $17~35 |
| 배포 후 시연 (내 계정만 연결, 60초 캐시) | 한 달 $1 미만 |
| **무한 재시도 버그** (예: 2026-11-21 알림 서비스가 몇 초에 4,277번 재시도한 것과 같은 일) | **수백 달러** |

→ 비용 위험의 대부분은 사용량이 아니라 **버그로 인한 반복 호출**이다. 그래서 §14.5 안전장치를 기능보다 먼저 만든다.

### 14.2 구조

| 항목 | 결정 |
|---|---|
| 위치 | `integration` 패키지 (분리 후 `integration-service`). `@ConditionalOnProperty(name = "integration.x.enabled")` 로 켜고 끈다. 꺼져 있으면 프론트에 탭을 숨긴다 |
| X 호출 | **인터페이스 `XTimelineClient` 뒤로 숨긴다** (아래). 서비스는 어느 구현인지 모른다 |
| 화면 | 우리 피드와 섞지 않고 **"X" 탭** 으로 따로 [결정]. 섞으면 커서(우리: 글 id, X: `pagination_token` 문자열)를 합쳐야 한다 (§6.8 2차와 같은 문제) |
| 저장 | `external_accounts(user_id, provider, external_user_id, username, access_token_enc, refresh_token_enc, expires_at, created_at)` / unique(user_id, provider). 토큰 컬럼은 nullable (fake 행은 토큰 없음, §14.3 토글). 토큰은 **AES-GCM 등으로 암호화**, 키는 환경변수 |
| 글 저장 | 하지 않는다. 사용자별로 **60초 캐시**(Redis) [결정] — 비용과 rate limit 보호 |
| 실패 격리 | 서비스가 X 호출을 3초에서 포기한다. 클라이언트 타임아웃은 그보다 짧게(타임라인: 연결 1초 + 읽기 2초) — 길면 사용자는 실패를 보는데 X 는 응답하고 **과금**된다. 실패하면 X 탭만 "불러오지 못했습니다", 우리 피드는 영향 없음 |
| 탈퇴·연결 해제 | 토큰 삭제 + X 의 토큰 폐기(revoke) 호출. 탈퇴는 `user.deleted` 이벤트를 integration 쪽도 받는다 (§4.4) |

**X 호출 인터페이스**

```
XTimelineClient                      ← 서비스는 이것만 안다
 ├ OfficialXClient   실제 X API (사용자 OAuth 토큰으로 reverse_chronological)
 └ FakeXClient       고정된 가짜 글 목록 · 일부러 실패/지연시키는 모드 (비용 0)
```

| 설정 | 값 | 뜻 |
|---|---|---|
| `integration.x.client` | `fake` (**기본값**) / `official` | 어느 구현을 쓸지. `@ConditionalOnProperty` 로 하나만 빈으로 등록 |
| `integration.x.fake.mode` | `ok` / `slow` / `error` / `expired` | Fake 가 정상·지연(타임아웃 확인)·장애(503)·토큰 만료(401)를 흉내 내게 → 실패 처리를 무료로 테스트 |

- 화면·캐시·커서·실패 처리·연결 해제는 **전부 Fake 로 완성**하고, `official` 은 마지막 확인 때만 켠다.
- 다른 플랫폼(Bluesky 등)을 붙이게 되면 이 인터페이스를 `ExternalFeedClient` 로 일반화한다.

### 14.3 API (우리 서버)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/integrations/x/connect` | 연동 켜기 (토글 ON). 응답은 아래 **두 형태** 중 하나 |
| GET | `/integrations/x/callback?code=&state=` | (official 만) `state` 확인 → `code` 를 토큰으로 교환 → 암호화 저장 → 프론트 설정 화면으로 리다이렉트 |
| GET | `/integrations/x` | 연결 상태 `{ "connected": true, "username": "jack" }` (연결 안 됨: `{ "connected": false, "username": null }`) |
| DELETE | `/integrations/x` | 연동 끄기 (토글 OFF) → `204` |
| GET | `/integrations/x/timeline?cursor=&size=` | `{ "items": [XPost...], "nextCursor": "문자열 또는 null" }` |

**연동 토글** [결정, 임시] — 연동할지는 사용자가 스위치로 정한다. **"켜짐 = 연동됨"** 하나로 본다.

| | fake | official |
|---|---|---|
| `POST /connect` 응답 | `200 { "connected": true, "username": "fake_user" }` — 바로 `external_accounts` 행 저장 (토큰 컬럼 null) | `200 { "authorizeUrl": "https://x.com/i/oauth2/authorize?..." }` — `state`·PKCE `code_verifier` 를 Redis(10분)에 두고 주소만 준다. 프론트가 그 주소로 이동, 행은 callback 에서 저장 |
| `DELETE` | 행 삭제 | 행 삭제 + X 토큰 폐기(revoke) |

- 프론트는 응답에 `authorizeUrl` 이 있으면 이동, 없으면 바로 켜짐으로 바꾼다 → fake → official 로 바꿔도 화면 코드는 그대로.
- **멱등**: 이미 연동돼 있는데 `connect` 가 오면 에러 없이 현재 상태(`connected: true`)를 돌려준다. 연동이 없는데 `DELETE` 가 오면 그냥 `204`. 스위치 연타 대비로 `unique(user_id, provider)` 가 마지막 방어선 (동시에 두 번 오면 하나는 제약 위반 → 다시 조회해 같은 응답).
- 프론트는 응답이 올 때까지 스위치를 잠근다.
- "연동은 유지하고 X 탭만 숨기기"(`enabled` 컬럼)는 두지 않는다. 상태가 연동×표시 4가지로 늘어난다. 필요해지면 그때 추가.
- 행은 fake 든 official 이든 **같은 `external_accounts` 테이블**. 메모리(Map)에 두지 않는다 (재시작하면 전부 꺼짐).

`XPost`: `{ "id", "text", "createdAt", "author": { "id", "username", "name", "profileImageUrl" }, "url" }` — 우리 `PostResponse` 와 모양이 다르므로 프론트는 별도 카드로 그린다. 누르면 X 원문(`url`)으로 간다.

에러: `409 X_NOT_CONNECTED`(연결 안 됨), `401 X_TOKEN_EXPIRED`(갱신도 실패 → 다시 연결), `503 X_UNAVAILABLE`(타임아웃·장애), `429 X_RATE_LIMITED`.

### 14.4 보안 체크

- `state` 로 CSRF 를 막고, PKCE 로 code 탈취를 막는다. `redirect_uri` 는 X 개발자 콘솔에 등록한 값과 **정확히** 같아야 한다 (로컬: `http://127.0.0.1:5173/api/v1/integrations/x/callback`. X 문서가 로컬은 `localhost` 말고 `127.0.0.1` 을 쓰라고 한다. 쿠키는 둘을 다른 사이트로 보므로 접속 주소도 `127.0.0.1` 로 통일. 배포 후 HTTPS 주소 추가).
- **callback 은 로그인 쿠키 없이 온다** (2026-11-24): `user_token` 이 `SameSite=Strict` 라 X 에서 돌아오는 이동에는 붙지 않는다. 그래서 callback 은 permitAll, 사용자는 Redis 의 `state` → `{ userId, codeVerifier }` 로 찾는다. `state` 는 한 번 쓰면 바로 지운다(GETDEL).
- **시작한 브라우저 확인**: state 만으로는 공격자가 자기 callback 주소를 피해자에게 열게 할 수 있다. connect 때 `x_oauth` 쿠키(httpOnly, `SameSite=Lax`, 10분, path `/api/v1/integrations/x/callback`, 값 = 별도 난수)를 심고, callback 에서 Redis 에 둔 값과 같은지 본다.
- 사용자가 X 화면에서 취소하면 `code` 대신 `error` 가 온다 → 저장하지 않고 설정 화면으로 돌려보낸다.
- 인증 코드는 **30초** 안에 토큰으로 바꿔야 한다. access token 수명은 2시간.
- 토큰은 응답·로그에 절대 내보내지 않는다. 클라이언트 ID·시크릿, 암호화 키는 `application-secret.properties`·환경변수.
- access token 은 2시간 뒤 만료된다 → `refresh_token` 으로 갱신 (2026-11-26 구현). 저장한 만료 시각(받은 `expires_in` − 60초)이 지났으면 **타임라인을 부르기 전에** 갱신하고 새 토큰으로 부른다 — 401 을 받은 뒤 갱신하면 같은 요청을 다시 보내야 해서.
  - X 는 갱신할 때마다 refresh token 도 새로 주고 옛것은 무효로 한다(회전) → **새 refresh token 도 저장**. 응답에 없으면 기존 값 유지.
  - 갱신 실패: 4xx(권한 끊김·만료) → `401 X_TOKEN_EXPIRED`(다시 연결), 5xx·통신 실패 → `503 X_UNAVAILABLE`(잠시 후). 다시 연결은 `/2/users/me` 요금이 들기 때문에 X 장애를 "다시 연결" 로 안내하지 않는다.
  - 알려진 한계: 같은 사용자의 두 요청이 동시에 갱신하면 회전 때문에 하나는 실패한다(1인 사용이라 두지 않음).
- callback 이 실패하면(취소·시간 초과·다른 브라우저·토큰 교환 실패) `/?tab=x&xError=1` 로 보낸다. 프론트는 안내를 띄우고 주소에서 `xError` 를 지운다.

### 14.5 비용 안전장치 (기능보다 먼저 만든다)

| # | 장치 | 내용 |
|---|---|---|
| 1 | 기본값 Fake | `integration.x.client` 기본 `fake`. 실제 API 는 설정을 바꿨을 때만 |
| 2 | 소액 선불 | 크레딧은 $5~10 만 충전, **자동 충전 끔** + 콘솔의 **Spending limit** 설정. 이 둘이 최대 손실을 정한다 |
| 3 | 우리 쪽 일일 상한 | 서버 전체 X 호출 하루 100회(설정값). 넘으면 `429 X_RATE_LIMITED`. X 의 한도(15분 180회)보다 **우리 상한이 먼저** 걸린다 |
| 4 | **자동 재시도 금지** | X 호출이 실패하면 서버도 프론트도 다시 부르지 않는다. 사용자가 "다시 시도" 를 눌러야만. (알림 서비스의 `requeue` 무한 반복과 같은 일을 처음부터 막는다) |
| 5 | 캐시·크기 | 사용자별 60초 캐시, 한 번에 10개(`size` 최대 20), X 탭 자동 새로고침 없음 |
| 6 | 호출 기록 | 실제 호출마다 로그 한 줄(사용자 id, 받은 글 수). 토큰은 남기지 않는다. 하루 사용량을 눈으로 확인 |

### 14.6 비공식 서비스를 쓰지 않는 이유 (2026-11-21 조사)

| 후보 | 결과 |
|---|---|
| twitterapi.io | 공개 데이터만, 계정 단위 감시 월정액. **홈 타임라인 없음**. "X 와 제휴 아님" |
| APICK X 게시물 조회 | 글 주소 하나 → 글 하나. 공개 글만, 2~5초. 출처 미공개 |
| FxTwitter(FxEmbed) | 무료·무인증. 글·프로필·팔로잉 목록은 있으나 **홈 타임라인 없음**. 운영자 X 계정 쿠키로 내부 API 호출 |
| GetXAPI 등 호출당 과금 대행 | 홈 타임라인 지원 확인 안 됨. 비교 글 대부분이 업체 홍보 |
| twscrape·twikit (로그인 세션) | 홈 타임라인까지 가능하지만 **X 개발자 가이드라인: 스크래핑·브라우저 자동화 금지, 위반 시 영구 정지**. 쿠키(`auth_token`)는 계정 전체 권한이라 유출 = 계정 탈취 |

- "본인 계정의 홈 타임라인" 을 정식으로 주는 것은 **X 공식 API 뿐**이다. 팔로잉 목록 + 계정별 글로 흉내 내면 팔로잉 수만큼 호출이 필요하고 결과도 다르다.
- 비공식 서비스는 언제든 막히거나 사라질 수 있고, 쓰는 쪽도 약관 위반 위험을 진다. 포트폴리오에서도 "공식 OAuth 2.0 + PKCE, 토큰 암호화, 비용 안전장치" 가 설명하기 좋다.
- X 글 **링크 미리보기**가 필요해지면 공식 oEmbed(무료, 무인증)를 쓴다.

참고: [X timelines 문서](https://docs.x.com/x-api/posts/timelines), [v2 인증 매핑](https://docs.x.com/fundamentals/authentication/guides/v2-authentication-mapping)

---

## 부록 A. DB 피드 테이블 (Redis 대신 쓸 경우)

```sql
CREATE TABLE feed_items (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,   -- 항목 순번 = 커서
    owner_id  BIGINT      NOT NULL,                -- 이 피드의 주인 (누구의 타임라인인가)
    type      VARCHAR(10) NOT NULL,                -- POST / REPLY / LIKE / REPOST
    post_id   BIGINT      NOT NULL,
    actor_id  BIGINT      NOT NULL,                -- 그 활동을 한 사람
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_feed_item (owner_id, type, post_id, actor_id),
    INDEX idx_owner_id (owner_id, id),             -- 읽기: WHERE owner_id=? AND id<? ORDER BY id DESC
    INDEX idx_post (post_id)                       -- 글 삭제 시 정리
);
```

- `id` 는 행 자체의 번호이고 `owner_id` 는 "누구의 피드인가" 이므로 둘은 겹치지 않는다. 한 사람의 피드는 여러 행(여러 `id`)으로 이루어진다.
- `type` 은 값이 4가지뿐이라 `VARCHAR(10)` 이면 충분하다.
