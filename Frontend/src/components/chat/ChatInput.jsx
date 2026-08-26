import { ArrowUp } from 'lucide-react';

export default function ChatInput({
  inputValue,
  setInputValue,
  onSendMessage,
  isTyping
}) {
  const handleSubmit = (e) => {
    e.preventDefault();
    const trimmed = inputValue.trim();
    if (trimmed) {
      onSendMessage(trimmed);
    }
  };

  return (
    <div className="p-4 md:px-6 md:pb-6 flex-shrink-0">
      <form 
        onSubmit={handleSubmit}
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
  );
}
