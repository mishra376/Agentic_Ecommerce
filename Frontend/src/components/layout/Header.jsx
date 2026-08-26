import { Menu, Trash2 } from 'lucide-react';

export default function Header({
  setSidebarOpen,
  onClearChat,
  canClear
}) {
  return (
    <>
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
          onClick={onClearChat}
          disabled={!canClear}
          className="w-[34px] h-[34px] rounded-lg bg-zinc-800/40 border border-zinc-700 text-zinc-400 hover:text-white flex items-center justify-center cursor-pointer transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          title="Clear Chat"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </header>
    </>
  );
}
