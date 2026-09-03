import { useState } from 'react';
import { 
  CheckCircle2, 
  XCircle, 
  Clock, 
  ChevronDown, 
  ChevronUp, 
  Package, 
  MapPin, 
  CreditCard,
  Hash,
  AlertTriangle
} from 'lucide-react';

export default function OrderCard({ order }) {
  const [expanded, setExpanded] = useState(false);

  // Status mapping
  const isPaid = order.paymentStatus === 'PAID' || order.status === 'PAID' || order.status === 'DELIVERED' || order.status === 'SHIPPED';
  const isFailed = order.paymentStatus === 'FAILED' || order.status === 'CANCELLED' || order.status === 'FAILED';
  const isPending = !isPaid && !isFailed;

  const statusConfig = isPaid
    ? {
        badge: 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400',
        icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" />,
        label: order.status || 'PAID',
        border: 'border-emerald-500/20'
      }
    : isFailed
    ? {
        badge: 'bg-red-500/15 border-red-500/30 text-red-400',
        icon: <XCircle className="w-4 h-4 text-red-400" />,
        label: order.status || 'FAILED',
        border: 'border-red-500/20'
      }
    : {
        badge: 'bg-amber-500/15 border-amber-500/30 text-amber-400',
        icon: <Clock className="w-4 h-4 text-amber-400" />,
        label: order.status || 'PENDING',
        border: 'border-amber-500/20'
      };

  const formattedDate = order.orderDate || order.createdAt
    ? new Date(order.orderDate || order.createdAt).toLocaleString('en-US', {
        dateStyle: 'medium',
        timeStyle: 'short'
      })
    : 'Unknown Date';

  const orderItems = order.orderItems || [];

  return (
    <div className={`bg-zinc-900/80 border ${statusConfig.border} hover:border-zinc-700 rounded-2xl p-5 md:p-6 transition-all duration-200 shadow-md`}>
      {/* Top Bar: Order ID, Date, Status */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-zinc-800 border border-white/5 text-indigo-400">
            <Package className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-bold text-white tracking-wide">
                Order #{order.id || order.razorpayOrderId?.substring(0, 10) || 'ORD'}
              </span>
              <span className={`px-2.5 py-0.5 rounded-full border text-[0.72rem] font-semibold flex items-center gap-1.5 ${statusConfig.badge}`}>
                {statusConfig.icon}
                {statusConfig.label}
              </span>
            </div>
            <span className="text-[0.78rem] text-zinc-400 block mt-0.5">
              {formattedDate}
            </span>
          </div>
        </div>

        <div className="flex items-center justify-between sm:justify-end gap-4">
          <div className="text-left sm:text-right">
            <span className="text-[0.7rem] text-zinc-400 uppercase font-semibold tracking-wider block">
              Total Amount
            </span>
            <span className="text-lg font-extrabold text-white">
              ₹{Number(order.totalAmount || 0).toFixed(2)}
            </span>
          </div>

          <button
            onClick={() => setExpanded(!expanded)}
            className="p-2 rounded-xl bg-zinc-800/80 border border-white/5 text-zinc-400 hover:text-white hover:bg-zinc-800 cursor-pointer transition-colors"
            title={expanded ? 'Hide Details' : 'View Details'}
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Main summary: Shipping, Payment info */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4 text-[0.83rem]">
        {/* Payment & Razorpay Info */}
        <div className="flex items-start gap-2.5 bg-zinc-950/40 p-3 rounded-xl border border-white/5">
          <CreditCard className="w-4 h-4 text-indigo-400 flex-shrink-0 mt-0.5" />
          <div className="min-w-0">
            <span className="text-zinc-400 font-medium block">Payment Method & Reference</span>
            <span className="text-zinc-200 font-semibold truncate block">
              {order.paymentMethod || 'Razorpay Online'} ({order.paymentStatus || statusConfig.label})
            </span>
            {order.razorpayPaymentId && (
              <span className="text-[0.72rem] font-mono text-zinc-400 truncate block mt-0.5">
                Txn ID: {order.razorpayPaymentId}
              </span>
            )}
          </div>
        </div>

        {/* Shipping Address */}
        <div className="flex items-start gap-2.5 bg-zinc-950/40 p-3 rounded-xl border border-white/5">
          <MapPin className="w-4 h-4 text-indigo-400 flex-shrink-0 mt-0.5" />
          <div className="min-w-0">
            <span className="text-zinc-400 font-medium block">Shipping Address</span>
            <span className="text-zinc-200 truncate block">
              {order.shippingAddress || 'Digital Product / Instant Delivery'}
            </span>
          </div>
        </div>
      </div>

      {/* Expanded Items List Section */}
      {expanded && (
        <div className="mt-4 pt-4 border-t border-white/10 animate-fadeIn">
          <h4 className="text-[0.78rem] font-bold uppercase tracking-wider text-zinc-400 mb-3 flex items-center gap-2">
            <Package className="w-3.5 h-3.5" />
            Order Items ({orderItems.length})
          </h4>

          {orderItems.length === 0 ? (
            <div className="text-center py-4 bg-zinc-950/30 rounded-xl text-zinc-400 text-xs">
              No individual line items detailed for this transaction.
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {orderItems.map((item, idx) => (
                <div 
                  key={idx} 
                  className="flex items-center justify-between p-3 rounded-xl bg-zinc-950/60 border border-white/5 hover:bg-zinc-950 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-zinc-800 border border-white/5 flex items-center justify-center text-zinc-400 font-mono text-xs">
                      #{idx + 1}
                    </div>
                    <div>
                      <span className="text-[0.88rem] font-medium text-white block">
                        {item.productName || `Product #${item.productId || idx + 1}`}
                      </span>
                      <span className="text-[0.72rem] text-zinc-400">
                        Qty: {item.quantity || 1} × ₹{Number(item.price || 0).toFixed(2)}
                      </span>
                    </div>
                  </div>

                  <span className="text-[0.88rem] font-bold text-indigo-300">
                    ₹{(Number(item.quantity || 1) * Number(item.price || 0)).toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
