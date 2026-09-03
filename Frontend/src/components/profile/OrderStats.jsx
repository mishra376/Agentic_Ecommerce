import { ShoppingBag, CheckCircle, XCircle, DollarSign, Clock } from 'lucide-react';

export default function OrderStats({ orders = [] }) {
  const totalOrders = orders.length;

  const successfulOrders = orders.filter(
    (o) => o.status === 'PAID' || o.status === 'DELIVERED' || o.status === 'SHIPPED' || o.paymentStatus === 'PAID'
  ).length;

  const failedOrders = orders.filter(
    (o) => o.status === 'CANCELLED' || o.status === 'FAILED' || o.paymentStatus === 'FAILED'
  ).length;

  const pendingOrders = orders.filter(
    (o) => (o.status === 'PENDING' || o.status === 'PROCESSING') && o.paymentStatus !== 'FAILED'
  ).length;

  const totalSpent = orders
    .filter((o) => o.paymentStatus === 'PAID' || o.status === 'PAID' || o.status === 'DELIVERED')
    .reduce((sum, o) => sum + (Number(o.totalAmount) || 0), 0);

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {/* Total Orders Card */}
      <div className="bg-zinc-900/90 border border-white/10 rounded-xl p-4 flex items-center gap-3">
        <div className="p-3 rounded-xl bg-indigo-500/15 text-indigo-400">
          <ShoppingBag className="w-5 h-5" />
        </div>
        <div>
          <span className="text-[0.75rem] font-medium text-zinc-400 block">Total Orders</span>
          <span className="text-xl font-bold text-white">{totalOrders}</span>
        </div>
      </div>

      {/* Successful Orders Card */}
      <div className="bg-zinc-900/90 border border-emerald-500/20 rounded-xl p-4 flex items-center gap-3">
        <div className="p-3 rounded-xl bg-emerald-500/15 text-emerald-400">
          <CheckCircle className="w-5 h-5" />
        </div>
        <div>
          <span className="text-[0.75rem] font-medium text-zinc-400 block">Placed & Paid</span>
          <span className="text-xl font-bold text-emerald-400">{successfulOrders}</span>
        </div>
      </div>

      {/* Failed / Cancelled Card */}
      <div className="bg-zinc-900/90 border border-red-500/20 rounded-xl p-4 flex items-center gap-3">
        <div className="p-3 rounded-xl bg-red-500/15 text-red-400">
          <XCircle className="w-5 h-5" />
        </div>
        <div>
          <span className="text-[0.75rem] font-medium text-zinc-400 block">Failed / Cancelled</span>
          <span className="text-xl font-bold text-red-400">{failedOrders}</span>
        </div>
      </div>

      {/* Total Spent Card */}
      <div className="bg-zinc-900/90 border border-purple-500/20 rounded-xl p-4 flex items-center gap-3">
        <div className="p-3 rounded-xl bg-purple-500/15 text-purple-400">
          <DollarSign className="w-5 h-5" />
        </div>
        <div>
          <span className="text-[0.75rem] font-medium text-zinc-400 block">Total Amount Spent</span>
          <span className="text-xl font-bold text-purple-300">₹{totalSpent.toFixed(2)}</span>
        </div>
      </div>
    </div>
  );
}
