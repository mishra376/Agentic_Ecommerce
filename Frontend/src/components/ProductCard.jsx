import { useState, useEffect } from 'react';
import { Package, Tag, ArrowRight } from 'lucide-react';

export default function ProductCard({ id }) {
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetch(`/api/products/${id}`)
      .then((res) => {
        if (res.ok) return res.json();
        throw new Error('Not found');
      })
      .then((data) => {
        setProduct(data);
        setLoading(false);
      })
      .catch(() => {
        setProduct(null);
        setLoading(false);
      });
  }, [id]);

  if (loading) {
    return (
      <div className="w-full max-w-sm bg-zinc-800/30 border border-white/5 rounded-xl p-4 my-2 animate-pulse text-left">
        <div className="h-4 bg-white/10 rounded w-2/3 mb-2" />
        <div className="h-3 bg-white/10 rounded w-1/2 mb-3" />
        <div className="h-10 bg-white/10 rounded w-full" />
      </div>
    );
  }

  if (!product) return null;

  return (
    <div className="w-full max-w-md bg-zinc-850 border border-white/5 rounded-xl p-4 my-3 text-left transition-all hover:bg-zinc-800/60 hover:border-white/10">
      <div className="flex items-start justify-between gap-3 mb-2">
        <div className="flex items-center gap-2">
          <Package className="w-4 h-4 text-[#818cf8] flex-shrink-0" />
          <h4 className="font-bold text-white text-[0.95rem]">{product.name}</h4>
        </div>
        <span className="text-[0.9rem] font-semibold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-lg border border-emerald-500/20">
          ${product.price?.toFixed(2)}
        </span>
      </div>

      <div className="text-[0.8rem] text-[#a1a1aa] mb-3 leading-relaxed">
        {product.description || 'No description available for this catalog item.'}
      </div>

      <div className="flex items-center justify-between border-t border-white/5 pt-3">
        <span className="text-[0.72rem] font-medium text-[#71717a] uppercase tracking-wider bg-white/5 px-2 py-0.5 rounded">
          {product.category || 'General'}
        </span>
        <a
          href={`/api/products/${product.id}`}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-1.5 text-[0.78rem] font-semibold text-[#818cf8] hover:text-white transition-colors"
        >
          <span>View Catalog Item</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </a>
      </div>
    </div>
  );
}
