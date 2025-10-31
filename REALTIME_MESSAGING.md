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
  stompClient.subscribe(`/user/queue/chat/${swapRequestId}`, function(message) {
    const chatMessage = JSON.parse(message.body);
    displayMessage(chatMessage);
  });
  
  // Subscribe to inbox refresh signals
  stompClient.subscribe('/user/queue/inbox/refresh', function() {
    refreshInbox();
  });
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
| `/user/queue/chat/{id}` | Subscribe | Receive chat messages |
| `/user/queue/inbox/refresh` | Subscribe | Inbox update signals |

## Security

- JWT validates platform is authorized
- userId identifies end user
- `ChatService.hasAccessToChat()` validates user is sender/receiver
