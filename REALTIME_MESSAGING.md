# Real-time Messaging Integration

This document describes the real-time messaging features integrated into the KirjaSwappi backend.

## Features Added

### 1. **Notification Service Integration**
- **gRPC client** to communicate with the notification microservice
- **Automatic notifications** for swap request events:
  - New swap request created → notify receiver
  - Swap request status changed → notify sender

### 2. **Real-time Chat via WebSocket**
- **STOMP over WebSocket** for real-time chat messages
- **Bidirectional communication** between users in swap conversations
- **Message broadcasting** to both sender and receiver instantly

### 3. **Real-time Inbox Updates**
- **Live inbox refresh** when new messages arrive
- **Status change notifications** reflected immediately
- **Unread count updates** in real-time

## WebSocket Endpoints

### Connection
```javascript
// Connect to WebSocket with user authentication
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'userId': 'user-123'  // User authentication
}, function(frame) {
  console.log('Connected: ' + frame);
});
```

### Chat Messages
```javascript
// Subscribe to chat messages for a specific swap request
stompClient.subscribe('/user/queue/chat/swap-request-id', function(message) {
  const chatMessage = JSON.parse(message.body);
  // Handle incoming chat message
});

// Send a chat message
stompClient.send('/app/chat/swap-request-id/send', {}, JSON.stringify({
  message: 'Hello!',
  images: []
}));
```

### Inbox Updates
```javascript
// Subscribe to inbox updates
stompClient.subscribe('/user/queue/inbox/update', function(message) {
  const inboxItems = JSON.parse(message.body);
  // Update inbox UI with new data
});

// Subscribe to inbox and get initial data
stompClient.send('/app/inbox/subscribe', {}, {});

// Request inbox refresh
stompClient.send('/app/inbox/refresh', {}, {});
```

## Configuration

### Environment Variables
```yaml
# Notification service connection
NOTIFICATION_SERVICE_HOST=notify.kirjaswappi.fi
NOTIFICATION_SERVICE_PORT=50051
```

### Application Properties
```yaml
notification:
  service:
    host: ${NOTIFICATION_SERVICE_HOST:notify.kirjaswappi.fi}
    port: ${NOTIFICATION_SERVICE_PORT:50051}
```

## Architecture

### Components Added
1. **NotificationService** - gRPC client for sending notifications
2. **WebSocketConfig** - STOMP WebSocket configuration
3. **WebSocketSecurityConfig** - WebSocket authentication
4. **RealtimeChatController** - WebSocket chat message handling
5. **RealtimeInboxController** - WebSocket inbox updates

### Integration Points
- **SwapService** - Sends notifications on swap events
- **ChatService** - Broadcasts inbox updates on new messages
- **InboxService** - Broadcasts updates on status changes

## Message Flow

### New Chat Message
1. User sends message via WebSocket (`/app/chat/{id}/send`)
2. `RealtimeChatController` processes the message
3. `ChatService.sendMessage()` saves to database
4. Message broadcasted to both users via WebSocket
5. Inbox update signal sent to both users
6. Frontend refreshes inbox automatically

### Swap Request Events
1. User creates/updates swap request
2. `SwapService` calls `NotificationService`
3. gRPC call sent to notification microservice
4. Push notification delivered to target user
5. Real-time WebSocket notification (if connected)

## Security

- **User Authentication** via WebSocket headers
- **Message Validation** on all incoming WebSocket messages
- **Access Control** - users can only access their own chats
- **CORS Configuration** for WebSocket connections

## Error Handling

- **Graceful Degradation** - if notification service is down, core functionality continues
- **Connection Recovery** - WebSocket reconnection handled by client
- **Message Queuing** - STOMP handles message delivery guarantees

## Testing

Run integration tests:
```bash
mvn test -Dtest=RealtimeMessagingIntegrationTest
```

## Frontend Integration

The frontend needs to:
1. **Connect to WebSocket** on user login
2. **Subscribe to user-specific queues** for chat and inbox
3. **Handle real-time message updates** in chat UI
4. **Refresh inbox** when receiving update signals
5. **Reconnect on connection loss**

Example frontend integration available in the main application.