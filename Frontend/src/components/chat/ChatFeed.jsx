import { useEffect, useRef } from 'react';
import { User, Sparkles } from 'lucide-react';
import MarkdownRenderer from '../MarkdownRenderer';
import TypingIndicator from './TypingIndicator';

export default function ChatFeed({ messagesList, isTyping }) {
  const messagesEndRef = useRef(null);

  // Scroll to bottom on messages list updates or typing state change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messagesList, isTyping]);

  if (messagesList.length === 0) {
    return (
      <div className="max-w-[700px] w-full mx-auto my-auto text-center px-5 py-10 flex-grow flex flex-col justify-center">
        <h1 className="text-2xl md:text-3xl font-semibold mb-2 tracking-tight text-white">
          How can I help you today?
        </h1>
        <p className="text-zinc-400 text-[0.88rem] max-w-[420px] mx-auto leading-relaxed">
          Search for products, check prices, or ask for help with placing your orders.
        </p>
      </div>
    );
  }

  return (
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
      {isTyping && <TypingIndicator />}
      
      <div ref={messagesEndRef} />
    </div>
  );
}
