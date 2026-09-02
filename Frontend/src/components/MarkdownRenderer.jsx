import React from 'react';
import ProductCard from './ProductCard';
import RazorpayCheckoutButton from './RazorpayCheckoutButton';

export default function MarkdownRenderer({ text }) {
  if (!text) return null;

  // Extract Razorpay tag: [PAY_WITH_RAZORPAY: orderId=X, razorpayOrderId=Y, amount=Z, autoOpen=true]
  let razorpayData = null;
  const razorpayTagRegex = /\[PAY_WITH_RAZORPAY:\s*orderId=([0-9]+),\s*razorpayOrderId=(order_[a-zA-Z0-9]+),\s*amount=([0-9.]+)(?:,\s*autoOpen=(true|false))?\]/i;
  const rzpMatch = razorpayTagRegex.exec(text);

  let cleanText = text;
  if (rzpMatch) {
    razorpayData = {
      orderId: rzpMatch[1],
      razorpayOrderId: rzpMatch[2],
      amount: rzpMatch[3],
      autoOpen: rzpMatch[4] === 'true' || true
    };
    cleanText = text.replace(razorpayTagRegex, '').trim();
  } else {
    // Fallback regex matching in case AI outputs plain text containing order & razorpay IDs
    const orderIdMatch = /Order ID:\s*(\d+)/i.exec(text);
    const rzpOrderIdMatch = /Razorpay Order ID:\s*(order_[a-zA-Z0-9]+)/i.exec(text) || /(order_[a-zA-Z0-9]{14,})/i.exec(text);
    const amountMatch = /Total Amount:\s*₹?\s*([0-9,.]+)/i.exec(text);
    if (orderIdMatch && rzpOrderIdMatch) {
      const rawAmount = amountMatch ? amountMatch[1].replace(/,/g, '') : '0';
      razorpayData = {
        orderId: orderIdMatch[1],
        razorpayOrderId: rzpOrderIdMatch[1],
        amount: rawAmount,
        autoOpen: true
      };
    }
  }

  // Split by code blocks: ```[lang]\n[code]\n```
  const parts = cleanText.split(/(```[\s\S]*?```)/g);

  // Extract any product IDs from product links (/api/products/{id})
  const productIds = [];
  const productRegex = /\/api\/products\/([a-zA-Z0-9_-]+)/g;
  let match;
  while ((match = productRegex.exec(text)) !== null) {
    const id = match[1];
    if (!productIds.includes(id)) {
      productIds.push(id);
    }
  }

  return (
    <div className="markdown-content">
      {parts.map((part, index) => {
        if (part.startsWith('```') && part.endsWith('```')) {
          // Code block
          const lines = part.split('\n');
          const firstLine = lines[0];
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

            if (trimmedLine.startsWith('- ') || trimmedLine.startsWith('* ')) {
              const content = trimmedLine.substring(2);
              currentList.push(
                <li key={`li-${lineIndex}`} className="text-[#f3f4f6]">
                  {parseInline(content)}
                </li>
              );
            } else {
              flushList(lineIndex);

              if (trimmedLine.startsWith('### ')) {
                renderedLines.push(
                  <h3 key={`h3-${lineIndex}`} className="text-[1.05rem] font-bold mt-4 mb-2 first:mt-0 text-left text-[#f3f4f6]">
                    {parseInline(trimmedLine.substring(4))}
                  </h3>
                );
              } else if (trimmedLine !== '') {
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

      {/* Interactive Razorpay Checkout Button inside Chat Feed */}
      {razorpayData && (
        <RazorpayCheckoutButton
          orderId={razorpayData.orderId}
          razorpayOrderId={razorpayData.razorpayOrderId}
          amount={razorpayData.amount}
          autoOpen={razorpayData.autoOpen}
        />
      )}

      {/* Dynamic Product Catalog listings below search result */}
      {productIds.length > 0 && (
        <div className="mt-4 pt-4 border-t border-white/5 text-left">
          <span className="text-[0.72rem] font-bold text-[#71717a] uppercase tracking-wider block mb-2 select-none">
            Database Catalog Results
          </span>
          <div className="flex flex-col gap-3">
            {productIds.map((id) => (
              <ProductCard key={id} id={id} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// Inline parser for **bold**, `code`, and [link text](url)
function parseInline(text) {
  const parts = text.split(/(\*\*.*?\*\*|`.*?`|\[[^\]]+\]\([^)]+\))/g);

  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index} className="font-bold text-white">{part.slice(2, -2)}</strong>;
    } else if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code key={index} className="font-mono bg-black/45 text-xs px-1.5 py-0.5 rounded text-[#818cf8] font-semibold border border-white/5">
          {part.slice(1, -1)}
        </code>
      );
    } else if (part.startsWith('[') && part.includes('](') && part.endsWith(')')) {
      const match = part.match(/\[([^\]]+)\]\(([^)]+)\)/);
      if (match) {
        const linkText = match[1];
        const url = match[2];

        if (url.startsWith('/api/products/')) {
          return (
            <a
              key={index}
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#818cf8] hover:underline font-semibold"
            >
              {linkText}
            </a>
          );
        }

        return (
          <a
            key={index}
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-[#818cf8] hover:underline font-medium"
          >
            {linkText}
          </a>
        );
      }
    }
    return part;
  });
}
