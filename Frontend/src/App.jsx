import { useState, useEffect, useRef } from 'react';
import { 
  Cpu, 
  Plus, 
  MessageSquare, 
  User, 
  Sparkles, 
  Menu, 
  Trash2, 
  ArrowUp, 
  HelpCircle, 
  Package, 
  UserPlus, 
  Store,
  X,
  Lock,
  Mail,
  Phone,
  ArrowRight,
  LogOut
} from 'lucide-react';
import MarkdownRenderer from './components/MarkdownRenderer';

export default function App() {
  // Authentication states
  const [token, setToken] = useState(() => localStorage.getItem('auth_token') || null);
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('auth_user');
    return saved ? JSON.parse(saved) : null;
  });

  // Login/Register Form states
  const [isRegistering, setIsRegistering] = useState(false);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authName, setAuthName] = useState('');
  const [authPhone, setAuthPhone] = useState('');
  const [authError, setAuthError] = useState('');
  const [authSuccess, setAuthSuccess] = useState('');
  const [authLoading, setAuthLoading] = useState(false);

  // Chat states
  const [chatSessions, setChatSessions] = useState([
    { id: 1, title: 'E-Commerce Guide', messages: [] }
  ]);
  const [activeSessionId, setActiveSessionId] = useState(1);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [backendStatus, setBackendStatus] = useState('checking'); // checking | active | inactive

  const messagesEndRef = useRef(null);

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

  // Scroll to bottom on updates
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatSessions, activeSessionId, isTyping]);

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
    setAuthEmail('');
    setAuthPassword('');
    setAuthName('');
    setAuthPhone('');
    setChatSessions([{ id: 1, title: 'E-Commerce Guide', messages: [] }]);
    setActiveSessionId(1);
  };

  // Auth Form handler
  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    setAuthError('');
    setAuthSuccess('');
    setAuthLoading(true);

    if (isRegistering) {
      if (!authName.trim()) {
        setAuthError('Name is required');
        setAuthLoading(false);
        return;
      }
      if (!authEmail.trim()) {
        setAuthError('Email is required');
        setAuthLoading(false);
        return;
      }
      if (!/^[0-9]{10}$/.test(authPhone.trim())) {
        setAuthError('Phone number must be exactly 10 digits');
        setAuthLoading(false);
        return;
      }
      if (authPassword.length < 6) {
        setAuthError('Password must be at least 6 characters');
        setAuthLoading(false);
        return;
      }

      try {
        const res = await fetch('/api/users/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: authName.trim(),
            email: authEmail.trim(),
            phone: authPhone.trim(),
            passwordHash: authPassword
          })
        });

        if (!res.ok) {
          const errMsg = await res.text();
          throw new Error(errMsg || 'Registration failed');
        }

        setAuthSuccess('Registration successful! You can now log in.');
        setIsRegistering(false);
        setAuthPassword('');
      } catch (err) {
        setAuthError(err.message);
      } finally {
        setAuthLoading(false);
      }
    } else {
      if (!authEmail.trim() || !authPassword) {
        setAuthError('Please fill in all fields');
        setAuthLoading(false);
        return;
      }

      try {
        const res = await fetch('/api/users/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: authEmail.trim(),
            password: authPassword
          })
        });

        if (!res.ok) {
          const errMsg = await res.text();
          throw new Error(errMsg || 'Invalid email or password');
        }

        const data = await res.json();
        setToken(data.token);
        setUser(data);
        localStorage.setItem('auth_token', data.token);
        localStorage.setItem('auth_user', JSON.stringify(data));
      } catch (err) {
        setAuthError(err.message);
      } finally {
        setAuthLoading(false);
      }
    }
  };

  // Send message handler
  const handleUserMessage = async (text) => {
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

      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({ message: trimmed })
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

  const handleFormSubmit = (e) => {
    e.preventDefault();
    handleUserMessage(inputValue);
  };

  // Render Login Form if not logged in
  if (!token) {
    return (
      <div className="flex min-h-screen w-screen items-center justify-center p-4 bg-[#18181b] select-none">
        <div className="w-full max-w-md bg-[#242427] border border-white/5 rounded-2xl p-8 shadow-xl flex flex-col items-center">
          {/* Logo Header */}
          <div className="flex items-center gap-2 mb-8">
            <Cpu className="w-6 h-6 text-zinc-400" />
            <span className="font-bold text-[1.2rem] tracking-tight text-white">
              E-Commerce Assistant
            </span>
          </div>

          <h2 className="text-[1.2rem] font-bold text-center text-white mb-2">
            {isRegistering ? 'Create your Account' : 'Welcome Back'}
          </h2>
          <p className="text-[0.8rem] text-zinc-400 text-center mb-6">
            {isRegistering ? 'Sign up to access the AI ecommerce assistant' : 'Sign in to access your chat workspace'}
          </p>

          {/* Feedback messages */}
          {authError && (
            <div className="w-full p-3 mb-4 text-[0.8rem] text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl text-left">
              {authError}
            </div>
          )}
          {authSuccess && (
            <div className="w-full p-3 mb-4 text-[0.8rem] text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-left">
              {authSuccess}
            </div>
          )}

          <form onSubmit={handleAuthSubmit} className="w-full flex flex-col gap-4">
            {isRegistering && (
              <>
                {/* Name */}
                <div className="flex flex-col gap-1 text-left">
                  <label className="text-[0.75rem] font-medium text-zinc-400">Full Name</label>
                  <input 
                    type="text" 
                    value={authName}
                    onChange={(e) => setAuthName(e.target.value)}
                    placeholder="John Doe" 
                    className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
                    required
                  />
                </div>

                {/* Phone */}
                <div className="flex flex-col gap-1 text-left">
                  <label className="text-[0.75rem] font-medium text-zinc-400">Phone Number</label>
                  <input 
                    type="tel" 
                    value={authPhone}
                    onChange={(e) => setAuthPhone(e.target.value)}
                    placeholder="10-digit number" 
                    className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
                    required
                  />
                </div>
              </>
            )}

            {/* Email */}
            <div className="flex flex-col gap-1 text-left">
              <label className="text-[0.75rem] font-medium text-zinc-400">Email Address</label>
              <input 
                type="email" 
                value={authEmail}
                onChange={(e) => setAuthEmail(e.target.value)}
                placeholder="name@example.com" 
                className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
                required
              />
            </div>

            {/* Password */}
            <div className="flex flex-col gap-1 text-left">
              <label className="text-[0.75rem] font-medium text-zinc-400">Password</label>
              <input 
                type="password" 
                value={authPassword}
                onChange={(e) => setAuthPassword(e.target.value)}
                placeholder="••••••••" 
                className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
                required
              />
            </div>

            {/* Submit Button */}
            <button 
              type="submit" 
              disabled={authLoading}
              className="mt-2 w-full py-2.5 bg-zinc-200 text-black hover:bg-white rounded-xl font-bold text-[0.9rem] flex items-center justify-center gap-2 transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {authLoading ? 'Processing...' : isRegistering ? 'Create Account' : 'Sign In'}
              {!authLoading && <ArrowRight className="w-4 h-4" />}
            </button>
          </form>

          {/* Switch Switcher Link */}
          <button 
            onClick={() => {
              setIsRegistering(!isRegistering);
              setAuthError('');
              setAuthSuccess('');
            }}
            className="mt-6 text-[0.8rem] text-zinc-400 hover:text-white transition-colors cursor-pointer"
          >
            {isRegistering ? 'Already have an account? Sign In' : "Don't have an account? Sign Up"}
          </button>
        </div>
      </div>
    );
  }

  // Render main Chat Workspace if logged in
  return (
    <div className="flex h-screen w-screen relative overflow-hidden bg-[#18181b] text-[#f4f4f5]">
      {/* Sidebar drawer back-overlay for mobile */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 md:hidden transition-opacity duration-300"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar navigation */}
      <aside 
        className={`fixed md:relative top-0 bottom-0 left-0 w-[260px] bg-[#0f0f11] border-r border-white/5 flex flex-col h-full z-50 transition-transform duration-200 ease-in-out md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Sidebar Header */}
        <div className="p-4 border-b border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Cpu className="w-5 h-5 text-zinc-400" />
            <span className="font-semibold text-[0.95rem] tracking-tight text-white">
              AI Chat Center
            </span>
          </div>
          {/* Close button for mobile */}
          <button 
            className="md:hidden text-zinc-400 hover:text-white"
            onClick={() => setSidebarOpen(false)}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* New Chat Button */}
        <button 
          onClick={handleNewChat}
          className="m-4 p-2.5 rounded-lg border border-zinc-700 bg-zinc-800/40 text-[#f4f4f5] font-medium text-[0.88rem] flex items-center justify-center gap-2 cursor-pointer hover:bg-zinc-800 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>New Chat</span>
        </button>

        {/* Sidebar Sessions List */}
        <div className="flex-1 overflow-y-auto px-4 py-2">
          <span className="text-[0.7rem] font-bold uppercase tracking-wider text-zinc-500 mb-2 block">
            Conversations
          </span>
          <div className="flex flex-col gap-1">
            {chatSessions.map((session) => {
              const isActive = session.id === activeSessionId;
              return (
                <div 
                  key={session.id}
                  onClick={() => {
                    setActiveSessionId(session.id);
                    setSidebarOpen(false);
                  }}
                  className={`flex items-center gap-2.5 py-2 px-3 rounded-lg cursor-pointer transition-colors ${
                    isActive 
                      ? 'bg-zinc-800 text-white font-medium' 
                      : 'text-zinc-400 hover:bg-zinc-900 hover:text-white'
                  }`}
                >
                  <MessageSquare className="w-4 h-4 flex-shrink-0 text-zinc-500" />
                  <span className="text-[0.85rem] truncate select-none w-full">
                    {session.title}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Sidebar Footer */}
        <div className="p-4 border-t border-white/5 bg-black/10 flex flex-col gap-3">
          {/* Status Indicator */}
          <div className="flex items-center gap-2 text-[0.75rem] text-zinc-400">
            <span className={`w-1.5 h-1.5 rounded-full ${
              backendStatus === 'active' 
                ? 'bg-emerald-500' 
                : backendStatus === 'checking'
                ? 'bg-amber-500'
                : 'bg-red-500'
            }`} />
            <span>
              {backendStatus === 'active' 
                ? 'Online' 
                : backendStatus === 'checking'
                ? 'Connecting...'
                : 'Offline'}
            </span>
          </div>

          {/* User profile details and Logout button */}
          <div className="flex items-center justify-between gap-2 border-t border-white/5 pt-3">
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-[32px] h-[32px] rounded-full bg-zinc-800 flex items-center justify-center text-xs font-bold text-zinc-300 uppercase flex-shrink-0">
                {user?.email ? user.email.substring(0, 2) : 'US'}
              </div>
              <div className="flex flex-col min-w-0">
                <span className="text-[0.8rem] font-medium text-white leading-tight truncate">
                  {user?.email || 'User'}
                </span>
                <span className="text-[0.65rem] text-zinc-500 font-medium tracking-wider">
                  {user?.role === 'ROLE_USER' ? 'Customer' : 'Merchant'}
                </span>
              </div>
            </div>
            
            <button 
              onClick={handleLogout}
              className="text-zinc-500 hover:text-red-400 cursor-pointer p-1 rounded hover:bg-zinc-800/50 transition-colors"
              title="Logout"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* Main Chat Workspace */}
      <main className="flex-1 flex flex-col h-full bg-[#18181b] relative">
        
        {/* Mobile Header (Hidden on md+) */}
        <header className="flex md:hidden h-14 border-b border-white/5 bg-[#0f0f11] px-4 items-center justify-between flex-shrink-0">
          <button 
            className="text-zinc-300 cursor-pointer"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="w-6 h-6" />
          </button>
          <div className="font-bold text-[1rem] text-white">
            Assistant
          </div>
          <div className="w-6" />
        </header>

        {/* Desktop Header */}
        <header className="hidden md:flex py-4 px-6 border-b border-white/5 items-center justify-between bg-[#141416] flex-shrink-0">
          <div>
            <h2 className="text-[0.98rem] font-bold text-white flex items-center gap-2">
              E-Commerce Assistant
            </h2>
            <p className="text-[0.75rem] text-zinc-400">
              Direct AI response console.
            </p>
          </div>
          <button 
            onClick={handleClearChat}
            disabled={messagesList.length === 0}
            className="w-[34px] h-[34px] rounded-lg bg-zinc-800/40 border border-zinc-700 text-zinc-400 hover:text-white flex items-center justify-center cursor-pointer transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            title="Clear Chat"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </header>

        {/* Message Window Container */}
        <div className="flex-1 overflow-y-auto p-4 md:p-6 flex flex-col">
          {messagesList.length === 0 ? (
            /* Welcome / Minimalist screen - suggestions removed */
            <div className="max-w-[700px] w-full mx-auto my-auto text-center px-5 py-10">
              <h1 className="text-2xl md:text-3xl font-semibold mb-2 tracking-tight text-white">
                How can I help you today?
              </h1>
              <p className="text-zinc-400 text-[0.88rem] max-w-[420px] mx-auto leading-relaxed">
                Query catalog database products, search prices, or onboarding endpoints using plain text queries.
              </p>
            </div>
          ) : (
            /* Feed of chat messages */
            <div className="max-w-[750px] w-full mx-auto flex flex-col gap-6">
              {messagesList.map((msg, index) => {
                const isUser = msg.sender === 'user';
                return (
                  <div 
                    key={index} 
                    className={`flex gap-4 w-full animate-[animateBubble_0.2s_ease-out_forwards] ${
                      isUser ? 'flex-row-reverse' : ''
                    }`}
                  >
                    {/* Avatar */}
                    <div className={`w-[32px] h-[32px] rounded-full flex items-center justify-center flex-shrink-0 text-xs font-semibold ${
                      isUser 
                        ? 'bg-zinc-800 text-zinc-300 border border-white/5' 
                        : 'bg-zinc-700 text-white'
                    }`}>
                      {isUser ? <User className="w-4 h-4" /> : <Sparkles className="w-4 h-4" />}
                    </div>

                    {/* Bubble */}
                    <div className={`max-w-[calc(100%-48px)] px-4 py-3.5 rounded-xl leading-relaxed text-[0.92rem] text-left text-zinc-100 ${
                      isUser 
                        ? 'bg-zinc-800/50 border border-zinc-700/50 text-right' 
                        : 'bg-transparent border-none'
                    }`}>
                      <MarkdownRenderer text={msg.text} />
                    </div>
                  </div>
                );
              })}

              {/* Typing indicator */}
              {isTyping && (
                <div className="flex gap-4 w-full text-left">
                  <div className="w-[32px] h-[32px] rounded-full flex items-center justify-center bg-zinc-700 text-white flex-shrink-0">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div className="bg-transparent px-2 py-3 flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-zinc-400 animate-[typingBounce_1.4s_infinite_-0.32s_ease-in-out_both]" />
                    <span className="w-1.5 h-1.5 rounded-full bg-zinc-400 animate-[typingBounce_1.4s_infinite_-0.16s_ease-in-out_both]" />
                    <span className="w-1.5 h-1.5 rounded-full bg-zinc-400 animate-[typingBounce_1.4s_infinite_ease-in-out_both]" />
                  </div>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        {/* Input box form */}
        <div className="p-4 md:px-6 md:pb-6 flex-shrink-0">
          <form 
            onSubmit={handleFormSubmit}
            className="max-w-[750px] w-full mx-auto"
          >
            <div className="relative flex bg-[#202023] border border-white/5 rounded-xl p-1.5 shadow-sm transition-all focus-within:border-zinc-500">
              <input 
                type="text" 
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                placeholder="Ask E-Commerce AI..." 
                autoComplete="off"
                className="flex-1 bg-transparent border-none outline-none text-white px-3 py-2.5 text-[0.9rem]"
              />
              <button 
                type="submit" 
                disabled={!inputValue.trim() || isTyping}
                className="w-10 h-10 rounded-lg bg-zinc-200 text-black hover:bg-white flex items-center justify-center cursor-pointer transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <ArrowUp className="w-4 h-4" />
              </button>
            </div>
          </form>
          <div className="text-center text-zinc-500 text-[0.68rem] mt-2">
            Spring Boot Console API Engine
          </div>
        </div>

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
  );
}


