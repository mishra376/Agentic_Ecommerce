import { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import AuthForm from './components/auth/AuthForm';
import Sidebar from './components/layout/Sidebar';
import Header from './components/layout/Header';
import ChatFeed from './components/chat/ChatFeed';
import ChatInput from './components/chat/ChatInput';
import ProtectedRoute from './components/auth/ProtectedRoute';
import PublicRoute from './components/auth/PublicRoute';

export default function App() {
  const navigate = useNavigate();

  // Authentication states
  const [token, setToken] = useState(() => localStorage.getItem('auth_token') || null);
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('auth_user');
    return saved ? JSON.parse(saved) : null;
  });

  // Chat states
  const [chatSessions, setChatSessions] = useState([
    { id: 1, title: 'E-Commerce Guide', messages: [] }
  ]);
  const [activeSessionId, setActiveSessionId] = useState(1);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [backendStatus, setBackendStatus] = useState('checking'); // checking | active | inactive

  // Poll backend health status
  useEffect(() => {
    const checkStatus = () => {
      const headers = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      fetch('/api/chat', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({ message: 'ping' })
      })
      .then((res) => {
        if (res.ok) {
          setBackendStatus('active');
        } else {
          setBackendStatus('inactive');
        }
      })
      .catch(() => {
        setBackendStatus('inactive');
      });
    };

    checkStatus();
    const interval = setInterval(checkStatus, 15000); // Check status every 15 seconds
    return () => clearInterval(interval);
  }, [token]);

  // Listen for Razorpay payment success / failure events to push order confirmation messages into chat
  useEffect(() => {
    const handleSuccess = (e) => {
      const { orderId, amount } = e.detail;
      const successMessageText = `### 🎉 Payment Successful!

**Order Summary:**
- **Order ID:** \`#${orderId}\`
- **Amount Paid:** ${amount}
- **Payment Method:** Razorpay
- **Status:** Paid & Processing

Your order **#${orderId}** has been confirmed and is being prepared for dispatch!`;

      const newMsg = { sender: 'assistant', text: successMessageText };
      setChatSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionId ? { ...s, messages: [...s.messages, newMsg] } : s
        )
      );
    };

    const handleFailure = (e) => {
      const { orderId } = e.detail;
      const failureMessageText = `### ❌ Payment Failed

- **Order ID:** \`#${orderId}\`
- **Status:** Cancelled

Payment was not completed. Order **#${orderId}** has been cancelled and no charges were made.`;

      const newMsg = { sender: 'assistant', text: failureMessageText };
      setChatSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionId ? { ...s, messages: [...s.messages, newMsg] } : s
        )
      );
    };

    window.addEventListener('razorpay-payment-success', handleSuccess);
    window.addEventListener('razorpay-payment-failed', handleFailure);

    return () => {
      window.removeEventListener('razorpay-payment-success', handleSuccess);
      window.removeEventListener('razorpay-payment-failed', handleFailure);
    };
  }, [activeSessionId]);

  const activeSession = chatSessions.find((s) => s.id === activeSessionId) || chatSessions[0];
  const messagesList = activeSession ? activeSession.messages : [];

  // Create new chat session
  const handleNewChat = () => {
    const newId = Date.now();
    const newSession = {
      id: newId,
      title: 'New Chat',
      messages: []
    };
    setChatSessions((prev) => [...prev, newSession]);
    setActiveSessionId(newId);
    setSidebarOpen(false);
  };

  // Clear current active chat feed
  const handleClearChat = () => {
    setChatSessions((prev) =>
      prev.map((s) => (s.id === activeSessionId ? { ...s, messages: [] } : s))
    );
    setIsTyping(false);
  };

  // Logout handler
  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    setToken(null);
    setUser(null);
    setChatSessions([{ id: 1, title: 'E-Commerce Guide', messages: [] }]);
    setActiveSessionId(1);
    navigate('/login');
  };

  // Handle successful login
  const handleLoginSuccess = (newToken, userData) => {
    setToken(newToken);
    setUser(userData);
    localStorage.setItem('auth_token', newToken);
    localStorage.setItem('auth_user', JSON.stringify(userData));
    navigate('/chat');
  };

  // Send message handler
  const handleSendMessage = async (text) => {
    const trimmed = text.trim();
    if (!trimmed) return;

    // Append user message to active session
    const userMsg = { sender: 'user', text: trimmed };
    
    // Update messages and potentially title if it is a new conversation
    setChatSessions((prev) =>
      prev.map((session) => {
        if (session.id === activeSessionId) {
          const updatedMsgs = [...session.messages, userMsg];
          const isDefaultTitle = 
            session.title === 'New Conversation' || 
            session.title === 'New Chat' ||
            session.title === 'E-Commerce Guide';
          const newTitle = isDefaultTitle
            ? (trimmed.length > 25 ? trimmed.substring(0, 22) + '...' : trimmed)
            : session.title;
          return { ...session, title: newTitle, messages: updatedMsgs };
        }
        return session;
      })
    );

    setInputValue('');
    setIsTyping(true);

    try {
      const headers = {
        'Content-Type': 'application/json'
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      // Get current messages for the active session to send as history
      const currentSession = chatSessions.find((s) => s.id === activeSessionId);
      const historyMessages = currentSession ? currentSession.messages.map(m => ({
        sender: m.sender,
        text: m.text
      })) : [];

      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({ message: trimmed, history: historyMessages })
      });

      if (!response.ok) {
        throw new Error(`Server returned status: ${response.status}`);
      }

      const data = await response.json();
      setIsTyping(false);

      const assistantMsg = {
        sender: 'assistant',
        text: data.reply || 'Received an empty response from server.'
      };

      setChatSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionId ? { ...s, messages: [...s.messages, assistantMsg] } : s
        )
      );
    } catch (err) {
      setIsTyping(false);
      
      const errorHtml = `### ⚠️ Connection Failed
 
I was unable to reach the Spring Boot backend server at \`/api/chat\`.
 
**Please verify that:**
- The Java backend is compiled and running successfully.
- The server is bound to port \`8080\`.
- Database services (PostgreSQL / MongoDB) are active.
 
*Error details: ${err.message}*`;

      const errorMsg = { sender: 'assistant', text: errorHtml };

      setChatSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionId ? { ...s, messages: [...s.messages, errorMsg] } : s
        )
      );
    }
  };

  return (
    <Routes>
      <Route 
        path="/login" 
        element={
          <PublicRoute token={token}>
            <AuthForm onLoginSuccess={handleLoginSuccess} />
          </PublicRoute>
        } 
      />

      <Route 
        path="/chat" 
        element={
          <ProtectedRoute token={token}>
            <div className="flex h-screen w-screen relative overflow-hidden bg-[#18181b] text-[#f4f4f5]">
              {/* Sidebar drawer back-overlay for mobile */}
              {sidebarOpen && (
                <div 
                  className="fixed inset-0 bg-black/50 z-40 md:hidden transition-opacity duration-300"
                  onClick={() => setSidebarOpen(false)}
                />
              )}

              {/* Sidebar navigation */}
              <Sidebar 
                chatSessions={chatSessions}
                activeSessionId={activeSessionId}
                setActiveSessionId={setActiveSessionId}
                sidebarOpen={sidebarOpen}
                setSidebarOpen={setSidebarOpen}
                backendStatus={backendStatus}
                user={user}
                onNewChat={handleNewChat}
                onLogout={handleLogout}
              />

              {/* Main Chat Workspace */}
              <main className="flex-1 flex flex-col h-full bg-[#18181b] relative">
                
                {/* Header (contains mobile & desktop templates) */}
                <Header 
                  setSidebarOpen={setSidebarOpen}
                  onClearChat={handleClearChat}
                  canClear={messagesList.length > 0}
                />

                {/* Message Window Container */}
                <div className="flex-1 overflow-y-auto p-4 md:p-6 flex flex-col">
                  <ChatFeed 
                    messagesList={messagesList}
                    isTyping={isTyping}
                  />
                </div>

                {/* Input box form */}
                <ChatInput 
                  inputValue={inputValue}
                  setInputValue={setInputValue}
                  onSendMessage={handleSendMessage}
                  isTyping={isTyping}
                />

              </main>

              {/* Embedded CSS animation for bubble entry and typing bounces */}
              <style>{`
                @keyframes animateBubble {
                  to {
                    opacity: 1;
                    transform: translateY(0);
                  }
                }
                @keyframes typingBounce {
                  0%, 80%, 100% {
                    transform: scale(0);
                  }
                  40% {
                    transform: scale(1.0);
                  }
                }
              `}</style>
            </div>
          </ProtectedRoute>
        } 
      />

      {/* Fallback routing */}
      <Route path="/" element={<Navigate to="/chat" replace />} />
      <Route path="*" element={<Navigate to="/chat" replace />} />
    </Routes>
  );
}
