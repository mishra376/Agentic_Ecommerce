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
  X 
} from 'lucide-react';
import MarkdownRenderer from './components/MarkdownRenderer';

const SUGGESTIONS = [
  {
    prompt: 'Show available products',
    icon: <Package className="w-6 h-6 text-accent-color" />,
    title: 'Browse Products',
    desc: 'Query catalog items, prices, and schemas.'
  },
  {
    prompt: 'How do I register a new user?',
    icon: <UserPlus className="w-6 h-6 text-accent-color" />,
    title: 'User Onboarding',
    desc: 'Get user registration endpoint payloads.'
  },
  {
    prompt: 'How do I register a new merchant?',
    icon: <Store className="w-6 h-6 text-accent-color" />,
    title: 'Merchant Accounts',
    desc: 'View shop domain configurations.'
  },
  {
    prompt: 'Show help and API references',
    icon: <HelpCircle className="w-6 h-6 text-accent-color" />,
    title: 'API Cheat Sheet',
    desc: 'List standard REST endpoints available.'
  }
];

export default function App() {
  // State variables
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
      fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
    const interval = setInterval(checkStatus, 10000); // Check status every 10 seconds
    return () => clearInterval(interval);
  }, []);

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
      title: 'New Conversation',
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
            session.title === 'New Chat';
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
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
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

  return (
    <div className="flex h-screen w-screen relative overflow-hidden bg-bg-primary text-text-primary">
      {/* Sidebar drawer back-overlay for mobile */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-black/60 z-40 md:hidden transition-opacity duration-300"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar navigation */}
      <aside 
        className={`fixed md:relative top-0 bottom-0 left-0 w-[280px] bg-bg-secondary border-r border-border-color flex flex-col h-full z-50 transition-transform duration-250 ease-in-out md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0 shadow-[15px_0_30px_rgba(0,0,0,0.5)]' : '-translate-x-full'
        }`}
      >
        {/* Sidebar Header */}
        <div className="p-5 border-b border-border-color flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-[38px] h-[38px] rounded-lg bg-gradient-to-br from-[#6366f1] via-[#8b5cf6] to-[#d946ef] flex items-center justify-center text-white shadow-[0_0_15px_rgba(99,102,241,0.4)]">
              <Cpu className="w-5 h-5" />
            </div>
            <span className="font-bold text-[1.15rem] tracking-tight">
              E-Com <span className="bg-gradient-to-br from-[#6366f1] via-[#8b5cf6] to-[#d946ef] bg-clip-text text-transparent">AI Core</span>
            </span>
          </div>
          {/* Close button for mobile */}
          <button 
            className="md:hidden text-text-secondary hover:text-text-primary"
            onClick={() => setSidebarOpen(false)}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* New Chat Button */}
        <button 
          onClick={handleNewChat}
          className="m-5 p-3 rounded-xl border border-dashed border-[#6366f1]/30 bg-[#6366f1]/5 text-text-primary font-medium text-[0.95rem] flex items-center justify-center gap-2 cursor-pointer hover:bg-[#6366f1]/10 hover:border-accent-color hover:shadow-[0_0_15px_rgba(99,102,241,0.15)] transition-all duration-200"
        >
          <Plus className="w-4 h-4" />
          <span>New Chat</span>
        </button>

        {/* Sidebar Sessions List */}
        <div className="flex-1 overflow-y-auto px-5 py-2">
          <span className="text-[0.75rem] font-semibold uppercase tracking-wider text-text-muted mb-3 block">
            Recent Conversations
          </span>
          <div className="flex flex-col gap-1.5">
            {chatSessions.map((session) => {
              const isActive = session.id === activeSessionId;
              return (
                <div 
                  key={session.id}
                  onClick={() => {
                    setActiveSessionId(session.id);
                    setSidebarOpen(false);
                  }}
                  className={`flex items-center gap-3 py-2.5 px-3.5 rounded-lg cursor-pointer transition-all duration-200 ${
                    isActive 
                      ? 'bg-[#6366f1]/8 text-text-primary border-l-3 border-accent-color' 
                      : 'text-text-secondary hover:bg-bg-tertiary hover:text-text-primary'
                  }`}
                >
                  <MessageSquare className="w-4 h-4 flex-shrink-0" />
                  <span className="text-[0.9rem] truncate select-none w-full">
                    {session.title}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Sidebar Footer */}
        <div className="p-5 border-t border-border-color bg-black/10">
          {/* Status Indicator */}
          <div className="flex items-center gap-2 mb-4 text-[0.8rem] text-text-secondary">
            <span className={`w-2 h-2 rounded-full pulse-dot ${
              backendStatus === 'active' 
                ? 'bg-[#10b981] shadow-[0_0_8px_#10b981]' 
                : backendStatus === 'checking'
                ? 'bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.6)]'
                : 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]'
            }`} />
            <span>
              {backendStatus === 'active' 
                ? 'Backend Active' 
                : backendStatus === 'checking'
                ? 'Connecting to Backend...'
                : 'Backend Offline'}
            </span>
          </div>
          {/* User profile */}
          <div className="flex items-center gap-3">
            <div className="w-[38px] h-[38px] rounded-full bg-bg-tertiary border border-border-color flex items-center justify-center text-xs font-bold text-accent-color">
              DEV
            </div>
            <div className="flex flex-col">
              <span className="text-[0.85rem] font-semibold text-text-primary leading-tight">Developer Mode</span>
              <span className="text-[0.75rem] text-text-muted">Admin Console</span>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Chat Workspace */}
      <main className="flex-1 flex flex-col h-full relative bg-[radial-gradient(circle_at_50%_50%,#111827_0%,#0b0f17_100%)]">
        
        {/* Mobile Header (Hidden on md+) */}
        <header className="flex md:hidden h-14 border-b border-border-color bg-bg-secondary px-4 items-center justify-between flex-shrink-0">
          <button 
            className="text-text-primary cursor-pointer"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="w-6 h-6" />
          </button>
          <div className="font-bold text-[1.1rem] bg-gradient-to-br from-[#6366f1] via-[#8b5cf6] to-[#d946ef] bg-clip-text text-transparent">
            E-Com AI
          </div>
          <div className="w-6" /> {/* Spacer */}
        </header>

        {/* Desktop Header */}
        <header className="hidden md:flex py-5 px-8 border-b border-border-color items-center justify-between backdrop-blur-md bg-[#0b0f17]/70 flex-shrink-0">
          <div>
            <h2 className="text-[1.2rem] font-bold flex items-center gap-2">
              E-Com Assistant <span className="text-[0.7rem] bg-[#6366f1]/15 text-accent-color py-0.5 px-1.5 rounded border border-[#6366f1]/20">v1.0</span>
            </h2>
            <p className="text-[0.8rem] text-text-secondary mt-1">
              Interact with the user, product, and merchant backend APIs using natural language.
            </p>
          </div>
          <button 
            onClick={handleClearChat}
            disabled={messagesList.length === 0}
            className="w-[38px] h-[38px] rounded-lg bg-bg-secondary border border-border-color text-text-secondary hover:text-red-500 hover:border-red-500/30 hover:bg-red-500/5 flex items-center justify-center cursor-pointer transition-all duration-200 disabled:opacity-40 disabled:hover:text-text-secondary disabled:hover:border-border-color disabled:hover:bg-transparent disabled:cursor-not-allowed"
            title="Clear current chat"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </header>

        {/* Message Window Container */}
        <div className="flex-1 overflow-y-auto p-6 md:p-8 flex flex-col">
          {messagesList.length === 0 ? (
            /* Welcome / Starter Prompts screen */
            <div className="max-w-[800px] w-full mx-auto my-auto text-center px-5 py-10">
              <div className="w-16 h-16 bg-[#6366f1]/10 text-accent-color border border-[#6366f1]/20 rounded-2xl flex items-center justify-center mx-auto mb-6 shadow-[0_0_20px_rgba(99,102,241,0.1)]">
                <Sparkles className="w-6 h-6" />
              </div>
              <h1 className="text-3xl md:text-4xl font-extrabold mb-3 tracking-tight text-white leading-tight">
                How can I help you build today?
              </h1>
              <p className="text-text-secondary text-[0.95rem] md:text-[1rem] max-w-[500px] mx-auto mb-10 leading-relaxed">
                Ask anything about the e-commerce system configuration, API request bodies, or mock database queries.
              </p>

              {/* Suggestions Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-left">
                {SUGGESTIONS.map((card, i) => (
                  <div 
                    key={i}
                    onClick={() => handleUserMessage(card.prompt)}
                    className="bg-bg-secondary border border-border-color rounded-2xl p-5 cursor-pointer hover:bg-bg-tertiary hover:border-accent-color hover:-translate-y-0.5 hover:shadow-[0_10px_20px_rgba(0,0,0,0.2),0_0_15px_rgba(99,102,241,0.15)] transition-all duration-200 flex flex-col"
                  >
                    <div className="mb-3.5 flex items-center">
                      {card.icon}
                    </div>
                    <h3 className="text-[0.95rem] font-semibold mb-1.5 text-white">
                      {card.title}
                    </h3>
                    <p className="text-[0.8rem] text-text-secondary leading-relaxed mb-3 flex-1">
                      {card.desc}
                    </p>
                    <span className="text-[0.8rem] font-medium text-accent-color">
                      Ask Assistant &rarr;
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            /* Feed of chat messages */
            <div className="max-w-[850px] w-full mx-auto flex flex-col gap-6">
              {messagesList.map((msg, index) => {
                const isUser = msg.sender === 'user';
                return (
                  <div 
                    key={index} 
                    className={`flex gap-4 w-full animate-[animateBubble_0.35s_cubic-bezier(0.16,1,0.3,1)_forwards] ${
                      isUser ? 'flex-row-reverse' : ''
                    }`}
                  >
                    {/* Avatar */}
                    <div className={`w-[34px] h-[34px] rounded-full flex items-center justify-center flex-shrink-0 text-xs font-bold ${
                      isUser 
                        ? 'bg-bg-tertiary text-text-primary border border-border-color' 
                        : 'bg-gradient-to-br from-[#6366f1] via-[#8b5cf6] to-[#d946ef] text-white'
                    }`}>
                      {isUser ? <User className="w-4 h-4" /> : <Sparkles className="w-4 h-4" />}
                    </div>

                    {/* Bubble */}
                    <div className={`max-w-[calc(100%-50px)] px-5 py-4 rounded-2xl leading-relaxed text-[0.95rem] ${
                      isUser 
                        ? 'bg-[#6366f1]/10 border border-[#6366f1]/20 rounded-tr-[4px] text-text-primary text-right' 
                        : 'bg-bg-secondary border border-border-color rounded-tl-[4px] text-text-primary'
                    }`}>
                      <MarkdownRenderer text={msg.text} />
                    </div>
                  </div>
                );
              })}

              {/* Typing indicator inside container */}
              {isTyping && (
                <div className="flex gap-4 w-full text-left">
                  <div className="w-[34px] h-[34px] rounded-full flex items-center justify-center bg-gradient-to-br from-[#6366f1] via-[#8b5cf6] to-[#d946ef] text-white flex-shrink-0">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div className="bg-bg-secondary border border-border-color rounded-2xl rounded-tl-[4px] px-5 py-4 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-text-secondary animate-[typingBounce_1.4s_infinite_-0.32s_ease-in-out_both]" />
                    <span className="w-2 h-2 rounded-full bg-text-secondary animate-[typingBounce_1.4s_infinite_-0.16s_ease-in-out_both]" />
                    <span className="w-2 h-2 rounded-full bg-text-secondary animate-[typingBounce_1.4s_infinite_ease-in-out_both]" />
                  </div>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        {/* Input box form */}
        <div className="p-5 md:px-8 md:pb-8 flex-shrink-0">
          <form 
            onSubmit={handleFormSubmit}
            className="max-w-[850px] w-full mx-auto"
          >
            <div className="relative flex bg-bg-secondary border border-border-color rounded-2xl p-2 shadow-[0_10px_30px_rgba(0,0,0,0.15)] focus-within:border-accent-color focus-within:shadow-[0_0_20px_rgba(99,102,241,0.15),0_10px_30px_rgba(0,0,0,0.25)] focus-within:-translate-y-[1px] transition-all duration-200">
              <input 
                type="text" 
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                placeholder="Ask E-Com AI about users, products, or merchant APIs..." 
                autoComplete="off"
                className="flex-1 bg-transparent border-none outline-none text-text-primary px-4 py-3 text-[0.95rem]"
              />
              <button 
                type="submit" 
                disabled={!inputValue.trim() || isTyping}
                className="w-11 h-11 rounded-xl bg-text-primary text-bg-primary hover:bg-gradient-to-br hover:from-[#6366f1] hover:via-[#8b5cf6] hover:to-[#d946ef] hover:text-white flex items-center justify-center cursor-pointer transition-all duration-250 hover:shadow-[0_0_15px_rgba(99,102,241,0.4)] disabled:opacity-40 disabled:hover:bg-text-primary disabled:hover:text-bg-primary disabled:cursor-not-allowed"
              >
                <ArrowUp className="w-5 h-5" />
              </button>
            </div>
          </form>
          <div className="text-center text-text-muted text-[0.72rem] mt-3">
            Connected to Spring Boot Server at localhost:8080.
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
