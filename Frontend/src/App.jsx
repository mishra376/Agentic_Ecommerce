import { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import AuthForm from './components/auth/AuthForm';
import Navbar from './components/layout/Navbar';
import SingleChatView from './components/chat/SingleChatView';
import ProfileView from './components/profile/ProfileView';
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

  // Single Chat state
  const [messagesList, setMessagesList] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);

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

Your order **#${orderId}** has been confirmed and is being prepared for dispatch! View order details in your profile.`;

      const newMsg = { sender: 'assistant', text: successMessageText };
      setMessagesList((prev) => [...prev, newMsg]);
    };

    const handleFailure = (e) => {
      const { orderId } = e.detail;
      const failureMessageText = `### ❌ Payment Failed

- **Order ID:** \`#${orderId}\`
- **Status:** Cancelled / Failed

Payment was not completed. Order **#${orderId}** has been marked as failed and no charges were incurred.`;

      const newMsg = { sender: 'assistant', text: failureMessageText };
      setMessagesList((prev) => [...prev, newMsg]);
    };

    window.addEventListener('razorpay-payment-success', handleSuccess);
    window.addEventListener('razorpay-payment-failed', handleFailure);

    return () => {
      window.removeEventListener('razorpay-payment-success', handleSuccess);
      window.removeEventListener('razorpay-payment-failed', handleFailure);
    };
  }, []);

  // Logout handler
  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    setToken(null);
    setUser(null);
    setMessagesList([]);
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

    // Append user message
    const userMsg = { sender: 'user', text: trimmed };
    const updatedMessages = [...messagesList, userMsg];
    setMessagesList(updatedMessages);
    setInputValue('');
    setIsTyping(true);

    try {
      const headers = {
        'Content-Type': 'application/json'
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      // Format history messages
      const historyMessages = messagesList.map(m => ({
        sender: m.sender,
        text: m.text
      }));

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

      setMessagesList((prev) => [...prev, assistantMsg]);
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
      setMessagesList((prev) => [...prev, errorMsg]);
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
        path="/*" 
        element={
          <ProtectedRoute token={token}>
            <div className="flex flex-col h-screen w-screen relative overflow-hidden bg-[#18181b] text-[#f4f4f5]">
              {/* Top Navigation Bar */}
              <Navbar 
                user={user} 
                onLogout={handleLogout} 
              />

              {/* Page Contents */}
              <main className="flex-1 flex overflow-hidden">
                <Routes>
                  <Route 
                    path="/chat" 
                    element={
                      <SingleChatView 
                        messagesList={messagesList}
                        inputValue={inputValue}
                        setInputValue={setInputValue}
                        onSendMessage={handleSendMessage}
                        isTyping={isTyping}
                      />
                    } 
                  />

                  <Route 
                    path="/profile" 
                    element={
                      <ProfileView 
                        user={user} 
                        token={token} 
                      />
                    } 
                  />

                  <Route path="*" element={<Navigate to="/chat" replace />} />
                </Routes>
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
    </Routes>
  );
}
