import React from 'react';

export default function MarkdownRenderer({ text }) {
  if (!text) return null;

  // Split by code blocks: ```[lang]\n[code]\n```
  const parts = text.split(/(```[\s\S]*?```)/g);

  return (
    <div className="markdown-content">
      {parts.map((part, index) => {
        if (part.startsWith('```') && part.endsWith('```')) {
          // It's a code block
          const lines = part.split('\n');
          // Try to detect language
          const firstLine = lines[0];
          const lang = firstLine.replace('```', '').trim();
          const code = lines.slice(1, -1).join('\n');
          return (
            <pre
              key={index}
              className="bg-black/30 border border-white/5 rounded-xl p-4 my-3 overflow-x-auto font-mono text-[0.85rem] text-left text-[#f3f4f6]"
            >
              <code className="bg-transparent p-0 block whitespace-pre">{code}</code>
            </pre>
          );
        } else {
          // Inline parsing for headers, bold, list items, inline code
          const lines = part.split('\n');
          let currentList = [];
          const renderedLines = [];

          const flushList = (key) => {
            if (currentList.length > 0) {
              renderedLines.push(
                <ul key={`list-${key}`} className="list-disc pl-5 mb-3 text-left space-y-1">
                  {currentList}
                </ul>
              );
              currentList = [];
            }
          };

          lines.forEach((line, lineIndex) => {
            const trimmedLine = line.trim();

            // List item: - text
            if (trimmedLine.startsWith('- ') || trimmedLine.startsWith('* ')) {
              const content = trimmedLine.substring(2);
              currentList.push(
                <li key={`li-${lineIndex}`} className="text-[#f3f4f6]">
                  {parseInline(content)}
                </li>
              );
            } else {
              flushList(lineIndex);

              // Header: ### text
              if (trimmedLine.startsWith('### ')) {
                renderedLines.push(
                  <h3 key={`h3-${lineIndex}`} className="text-[1.05rem] font-bold mt-4 mb-2 first:mt-0 text-left text-[#f3f4f6]">
                    {parseInline(trimmedLine.substring(4))}
                  </h3>
                );
              }
              // Normal line
              else if (trimmedLine !== '') {
                renderedLines.push(
                  <p key={`p-${lineIndex}`} className="mb-3 text-left text-[#f3f4f6] last:mb-0 leading-relaxed">
                    {parseInline(line)}
                  </p>
                );
              }
            }
          });

          flushList(lines.length);
          return <React.Fragment key={index}>{renderedLines}</React.Fragment>;
        }
      })}
    </div>
  );
}

// Simple inline parser for **bold** and `code`
function parseInline(text) {
  // Regex to split by bold (**text**) and inline code (`text`)
  const parts = text.split(/(\*\*.*?\*\*|`.*?`)/g);

  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index} className="font-bold text-white">{part.slice(2, -2)}</strong>;
    } else if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code key={index} className="font-mono bg-black/45 text-xs px-1.5 py-0.5 rounded text-[#6366f1] font-semibold border border-white/5">
          {part.slice(1, -1)}
        </code>
      );
    }
    return part;
  });
}
