# M3-T007 Community Chat

## Delivered

- Community direct chat persists messages in the `community_chat_message` table.
- Only active community users with a mutual personal-blog follow relationship can send, read, or mark a direct conversation as read.
- Conversation queries are authorized for the requested user pair before the message store is queried.
- The existing WebSocket ticket service now scopes single-use tickets to either `ADMIN` or `COMMUNITY`, preventing an ID overlap between `sys_user` and `community_user` from crossing channels.
- `/ws/community-chat` has its own handshake interceptor, connection registry, and post-commit delivery listener. WebSocket clients receive events only after the HTTP write transaction commits; the HTTP API remains the source of truth.
- The `/chat` page loads mutual-follow contacts from the community API, handles loading, empty, error, and offline states, and synchronizes new messages through the community socket.

## API

- `POST /api/v1/chat/messages`
- `GET /api/v1/chat/messages/{peerId}?limit=50`
- `POST /api/v1/chat/messages/{peerId}/read`
- `POST /api/v1/chat/websocket-ticket`
- `GET /ws/community-chat?ticket={single-use-community-ticket}`

## Database Evidence

`V028__m3_community_chat.sql` was applied to the isolated MySQL 8.0.46 database `pxczxn_m3_t003`.

- Verify output: table `1`, required index-column entries `6` for the two pair and recipient indexes.
- `R028__rollback_m3_community_chat.sql` removed the table successfully.
- Migration and verify were applied again with the same result.

## Validation

- Focused backend tests passed: `WebSocketTicketServiceTest` and `CommunityChatServiceImplTest`.
- Full backend Maven test suite passed.
- Web `typecheck`, `lint`, `test`, and production `build` passed.
- Browser validation confirmed the authenticated-entry fallback layout for `/chat` at desktop and 390px mobile viewports.
