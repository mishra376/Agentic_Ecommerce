import ChatFeed from './ChatFeed';
import ChatInput from './ChatInput';

export default function SingleChatView({
  messagesList,
  inputValue,
  setInputValue,
  onSendMessage,
  isTyping
}) {
  return (
    <div className="flex-1 flex flex-col h-[calc(100vh-4rem)] max-w-4xl mx-auto w-full bg-[#18181b] relative">
      {/* Message Feed Window */}
      <div className="flex-1 overflow-y-auto p-4 md:p-6 flex flex-col">
        <ChatFeed
          messagesList={messagesList}
          isTyping={isTyping}
        />
      </div>

      {/* Input box */}
      <div className="p-4 bg-[#18181b]">
        <ChatInput
          inputValue={inputValue}
          setInputValue={setInputValue}
          onSendMessage={onSendMessage}
          isTyping={isTyping}
        />
      </div>
    </div>
  );
}
