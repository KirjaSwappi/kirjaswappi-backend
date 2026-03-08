# Real-time Messaging

## Authentication

WebSocket connections require **both**:
- `Authorization: Bearer <admin-jwt>` - Platform authentication
- `userId: <user-id>` - End user identification

## WebSocket Connection

```javascript
const socket = new SockJS('/ws', null, {
  headers: {
    'Authorization': 'Bearer ' + adminJwtToken,
    'userId': currentUserId
  }
});

const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  // Subscribe to chat messages
  stompClient.subscribe(`/user/queue/chat.${swapRequestId}`, function(message) {
    const chatMessage = JSON.parse(message.body);
    displayMessage(chatMessage);
  });

  // Subscribe to inbox delta updates (Single Item)
  stompClient.subscribe('/user/queue/inbox.item-update', function(message) {
    const inboxItem = JSON.parse(message.body);
    updateInboxItem(inboxItem);
  });

  // Subscribe to full inbox updates (Initial Load/Refresh)
  stompClient.subscribe('/user/queue/inbox.update', function(message) {
    const inboxList = JSON.parse(message.body);
    setInboxList(inboxList);
  });

  // Subscribe to inbox refresh signals
  stompClient.subscribe('/user/queue/inbox.refresh', function() {
    // Re-fetch inbox from REST API
    fetchInbox();
  });

  // Trigger initial load
  stompClient.send("/app/inbox/subscribe", {}, {});
});

// Send message
stompClient.send(`/app/chat/${swapRequestId}/send`, {}, JSON.stringify({
  message: 'Hello!'
}));
```

## Endpoints

| Destination | Type | Purpose |
|------------|------|---------|
| `/ws` | Connect | WebSocket endpoint |
| `/app/chat/{id}/send` | Send | Send chat message |
| `/app/inbox/subscribe` | Send | Request initial inbox state |
| `/app/inbox/refresh` | Send | Request inbox refresh |
| `/user/queue/chat.{id}` | Subscribe | Receive chat messages |
| `/user/queue/inbox.item-update` | Subscribe | **Delta Updates**: Single inbox item changes |
| `/user/queue/inbox.update` | Subscribe | **Full Sync**: Complete inbox list |
| `/user/queue/inbox.refresh` | Subscribe | **Refresh Signal**: Re-fetch inbox via REST |

## Security

- JWT validates platform is authorized
- userId identifies end user
- `ChatService.hasAccessToChat()` validates user is sender/receiver

## Queue Naming Convention

Destination names use **dots** as separators (e.g. `inbox.item-update`, `chat.{id}`) rather than slashes. RabbitMQ's STOMP plugin does not allow forward slashes inside queue names — Spring appends `-user{sessionId}` to derive the final queue name, so slashes would produce an invalid RabbitMQ queue like `inbox/item-update-user{id}`.
