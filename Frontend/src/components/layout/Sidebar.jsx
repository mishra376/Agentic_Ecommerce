import { Cpu, Plus, MessageSquare, X, LogOut } from 'lucide-react';

export default function Sidebar({
  chatSessions,
  activeSessionId,
  setActiveSessionId,
  sidebarOpen,
  setSidebarOpen,
  backendStatus,
  user,
  onNewChat,
  onLogout
}) {
  return (
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
        onClick={onNewChat}
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
            onClick={onLogout}
            className="text-zinc-500 hover:text-red-400 cursor-pointer p-1 rounded hover:bg-zinc-800/50 transition-colors"
            title="Logout"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}
