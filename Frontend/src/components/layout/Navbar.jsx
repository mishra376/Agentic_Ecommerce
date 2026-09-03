import { User, LogOut, Cpu, ArrowLeft } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';

export default function Navbar({ user, onLogout }) {
  const location = useLocation();
  const navigate = useNavigate();
  const isProfilePage = location.pathname === '/profile';

  return (
    <header className="h-16 bg-[#0f0f11] border-b border-white/10 px-4 md:px-8 flex items-center justify-between sticky top-0 z-50 backdrop-blur-md bg-[#0f0f11]/90">
      {/* Brand Logo & Name */}
      <div 
        onClick={() => navigate('/chat')} 
        className="flex items-center gap-3 cursor-pointer group"
      >
        <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 via-purple-600 to-pink-500 flex items-center justify-center shadow-lg shadow-indigo-500/20 group-hover:scale-105 transition-transform">
          <Cpu className="w-5 h-5 text-white" />
        </div>
        <div>
          <span className="font-bold text-[1.05rem] tracking-tight text-white block leading-tight">
            Agentic Ecommerce
          </span>
        </div>
      </div>

      {/* Right User Actions */}
      <div className="flex items-center gap-3">
        {isProfilePage ? (
          /* When on Profile page: simple Back to Chat button */
          <button
            onClick={() => navigate('/chat')}
            className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 border border-indigo-500/30 text-indigo-300 text-xs font-semibold cursor-pointer transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Chat</span>
          </button>
        ) : (
          /* When on Chat page: Clickable User Profile Avatar card */
          <button
            onClick={() => navigate('/profile')}
            className="flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-800 border border-white/10 text-white cursor-pointer transition-all duration-150 group"
            title="View Profile & Orders"
          >
            <div className="w-7 h-7 rounded-lg bg-indigo-600/80 group-hover:bg-indigo-600 text-white flex items-center justify-center text-xs font-extrabold uppercase">
              {user?.email ? user.email.substring(0, 2) : 'US'}
            </div>
            <div className="flex flex-col text-left hidden sm:flex">
              <span className="text-[0.78rem] font-semibold text-white leading-tight max-w-[120px] truncate">
                {user?.email ? user.email.split('@')[0] : 'My Profile'}
              </span>
              <span className="text-[0.62rem] text-indigo-400 font-medium">
                View Orders & Profile →
              </span>
            </div>
            <User className="w-4 h-4 text-zinc-400 group-hover:text-indigo-400 sm:hidden" />
          </button>
        )}

        {/* Logout button */}
        <button
          onClick={onLogout}
          className="p-2 rounded-xl border border-red-500/20 bg-red-500/10 text-red-400 hover:bg-red-500/20 cursor-pointer transition-colors"
          title="Logout"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
}
