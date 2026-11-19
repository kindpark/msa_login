# SNS Feed API 명세서

| 항목 | 내용 |
|---|---|
| 문서 버전 | v0.3 (2026-11-11) |
| 대상 | Spring Boot 백엔드 (`com.example.login`), Vue 프론트엔드 |
| 현재 구조 | 모듈러 모놀리스 (단일 서버 `:8081`, MySQL `user_db`) |
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

### 4.1 회원가입 `POST /users/signup`

```json
{ "email": "a@example.com", "password": "password123", "nickname": "tester" }
```

| 필드 | 규칙 |
|---|---|
| email | 필수, 이메일 형식, 100자 이하, 중복 불가 |
| password | 필수, 8~72자 (BCrypt 한계 72바이트) |
| nickname | 필수, 50자 이하 |

응답 `200` 문자열 `"회원가입 완료"` (🚧 §10: `201` + `{ "id" }` 로 변경 권장)

에러: `400 VALIDATION_ERROR`, `409 DUPLICATE_EMAIL`

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

`rootId` 는 서버 내부용이라 응답에 넣지 않는다.

### 6.2 작성 `POST /posts`

```json
{ "content": "본문", "imageUrl": null }
```

| 필드 | 규칙 |
|---|---|
| content | 필수(공백 불가) |
| imageUrl | 선택 |

응답 `200` `PostResponse` (🚧 `201` 권장)

### 6.3 수정 `PUT /posts/{postId}`

요청은 6.2와 같다. 응답 `200` `PostResponse`.
에러: `403 FORBIDDEN`(작성자 아님), `404 POST_NOT_FOUND`

### 6.4 삭제 `DELETE /posts/{postId}`

- **soft delete**: `is_deleted = true` 로만 표시한다. 이후 모든 조회에서 자동으로 제외된다(`@SQLRestriction`).
- 답글을 지우면 부모의 `commentCount` 를 1 줄인다. 원글을 지워도 그 답글들은 남는다(X 방식). 프론트는 불러오지 못한 부모를 "삭제되었거나 볼 수 없는 게시물"로 표시한다.

응답 `200` `PostResponse` (🚧 `204` 권장). 에러: `403`, `404`.

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
| 동시에 두 번 누름 | 두 번째 insert 가 unique 에 걸린다 → [결정] `409` / "이미 재게시됨"으로 보고 `200` | |
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
| 에러 | `400 VALIDATION_ERROR` (형식·크기), `413` (요청 크기 초과) |

- 파일 이름은 서버가 UUID 로 새로 짓는다. 사용자가 보낸 이름을 경로에 쓰면 `../` 로 다른 폴더에 쓸 수 있다.
- 확장자·Content-Type 은 믿지 말고 허용 목록과 비교한다.
- 저장 위치는 처음에는 서버 로컬 폴더, 서비스를 나누면 객체 저장소(S3 등)로 옮긴다. 응답이 주소만 주므로 프론트는 바뀌지 않는다.
- `url` 을 `/api/v1/...` 아래로 두면 개발(Vite 프록시)과 배포(게이트웨이) 모두 같은 출처라 추가 설정이 필요 없다.

### 6.10 인용 재게시 📝 `POST /posts/{postId}/quotes`

다른 글을 첨부해서 내 글을 쓰는 것. 일반 재게시(§6.8)와 달리 **인용은 새 게시글**이다. 자기 id, 좋아요, 답글을 가지므로 `reposts` 가 아니라 `posts` 에 저장한다.

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
  "followerCount": 12, "followingCount": 30, "postCount": 57,
  "me": false, "followedByMe": true
}
```

프로필 수정 요청 (보낸 필드만 바꾼다)

```json
{ "nickname": "새이름", "handle": "new_handle", "bio": "소개", "profileImageUrl": "https://...", "bannerImageUrl": "https://..." }
```

| 필드 | 규칙 |
|---|---|
| handle | 4~15자, 영문 소문자·숫자·`_`, 중복 불가 → `409 DUPLICATE_HANDLE` |
| nickname | 50자 이하 |
| bio | 255자 이하 |

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

- 모두 **커밋 후** 발행한다.
- 필드 이름과 모양은 §9.2 봉투를 따른다. 알림 받을 사람의 id(`recipientId`)를 이벤트에 담는다. 알림 서비스는 posts 테이블을 모르기 때문이다.
- 사실로 발행하지만, 알림 서비스가 걸러낸다.

### 9.1 메시지 브로커 📝

**RabbitMQ** 로 정했다 (2026-11-17). Spring 은 `spring-boot-starter-amqp` 를 쓰고, `build.gradle` 의 Kafka 의존성은 지운다.

| 항목 | 값 |
|---|---|
| exchange | `sns.events` — topic exchange, durable |
| routing key | 이벤트 `type` 그대로. 예: `follow.created` |
| 알림 서비스 큐 | `notification.events` (durable) — `follow.created`, `reply.created`, `like.created`, `repost.created`, `quote.created` 를 바인딩 |
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

- `actorId == recipientId`(내 글에 내가 좋아요·답글)여도 **발행한다**. 이벤트는 "일어난 일"이고, 누구에게 알릴지는 알림 서비스가 정한다. 알림 서비스는 `actorId == recipientId` 이면 저장하지 않는다 (셀프 팔로우는 API 에서 막는다).
- 이름(닉네임)은 이벤트에 넣지 않는다. 닉네임은 바뀔 수 있으므로 프론트가 `GET /users?ids=` 로 그때그때 받는다 (게시글 작성자 이름과 같은 방식).

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
| posts | `content` 를 TEXT 또는 VARCHAR(1000) | 기본 255자는 짧음 |
| posts | `repost_count INT NOT NULL DEFAULT 0` | 재게시 수 |
| reposts | id, user_id, post_id, created_at / unique(user_id, post_id), index(user_id, id) | 재게시 |
| posts | `quote_id BIGINT NULL`, `quote_count INT NOT NULL DEFAULT 0`, 인덱스 `(quote_id, id)` | 인용 재게시 (§6.10) |
| users | `handle VARCHAR(15) UNIQUE`, `banner_image_url VARCHAR(500)` | 프로필 |
| post_likes 인덱스 | `(post_id, id)` (지금의 이름 잘못된 인덱스를 대체) | 좋아요 누른 사람 목록 |

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
| 6 | 서비스 분리 | user / post-feed 별도 기동, `FollowReader` HTTP 구현 |

**범위 밖**: 여러 장 이미지·동영상(이미지 1장 업로드는 §6.9), 외부 SNS(X API) 연동과 리다이렉션, 해시태그, 검색.

**외부 SNS 연동을 하게 되면** (§11): 별도 `integration-service`가 OAuth 토큰(암호화 저장)·rate limit·외부 장애를 맡고, 피드와는 이벤트로만 연결한다. 피드 항목 `type`에 `EXTERNAL`을 추가하면 피드 구조는 그대로 쓸 수 있다. 6단계(서비스 분리) 이후에 진행한다.

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
