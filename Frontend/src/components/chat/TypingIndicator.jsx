import { Sparkles } from 'lucide-react';

export default function TypingIndicator() {
  return (
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
  );
}
