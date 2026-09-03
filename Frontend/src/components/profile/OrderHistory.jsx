import { useState, useEffect } from 'react';
import { RefreshCw, ShoppingBag, PackageX } from 'lucide-react';
import OrderCard from './OrderCard';

export default function OrderHistory({ token }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('ALL'); // ALL | PAID | FAILED | PENDING

  const fetchOrders = () => {
    setLoading(true);
    setError(null);

    fetch('/api/orders/my-orders', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`Failed to load orders (Status: ${res.status})`);
        }
        return res.json();
      })
      .then((data) => {
        if (Array.isArray(data)) {
          setOrders(data);
        } else {
          setOrders([]);
        }
      })
      .catch((err) => {
        console.warn('Backend order fetch failed:', err.message);
        const savedOrders = localStorage.getItem('user_cached_orders');
        if (savedOrders) {
          try {
            setOrders(JSON.parse(savedOrders));
          } catch (e) {
            setOrders([]);
          }
        } else {
          setOrders([]);
        }
        setError(err.message);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (token) {
      fetchOrders();
    } else {
      setLoading(false);
    }
  }, [token]);

  // Filter orders based on active tab
  const filteredOrders = orders.filter((order) => {
    const isPaid = order.paymentStatus === 'PAID' || order.status === 'PAID' || order.status === 'DELIVERED' || order.status === 'SHIPPED';
    const isFailed = order.paymentStatus === 'FAILED' || order.status === 'CANCELLED' || order.status === 'FAILED';
    const isPending = !isPaid && !isFailed;

    if (activeTab === 'PAID' && !isPaid) return false;
    if (activeTab === 'FAILED' && !isFailed) return false;
    if (activeTab === 'PENDING' && !isPending) return false;

    return true;
  });

  return (
    <div className="bg-zinc-900/60 border border-white/10 rounded-2xl p-6 shadow-xl space-y-6">
      
      {/* Header & Refresh */}
      <div className="flex items-center justify-between gap-4 border-b border-white/10 pb-5">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-indigo-400" />
            Order History
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Scroll down to view details of your placed, paid, and failed orders.
          </p>
        </div>

        <button
          onClick={fetchOrders}
          disabled={loading}
          className="p-2 rounded-xl bg-zinc-800 border border-white/5 text-zinc-300 hover:text-white hover:bg-zinc-700 cursor-pointer transition-colors disabled:opacity-50"
          title="Refresh Orders"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-indigo-400' : ''}`} />
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-white/5 pb-3 overflow-x-auto">
        {[
          { id: 'ALL', label: 'All Orders' },
          { id: 'PAID', label: 'Placed & Paid' },
          { id: 'FAILED', label: 'Failed / Cancelled' },
          { id: 'PENDING', label: 'Pending' }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all duration-150 cursor-pointer ${
              activeTab === tab.id
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'bg-zinc-800/40 text-zinc-400 hover:text-white hover:bg-zinc-800'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Orders List / States */}
      {loading ? (
        <div className="py-16 text-center space-y-3">
          <RefreshCw className="w-8 h-8 text-indigo-500 animate-spin mx-auto" />
          <p className="text-sm text-zinc-400 font-medium">Fetching order records...</p>
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="py-16 text-center space-y-4 border border-dashed border-white/10 rounded-2xl bg-zinc-950/40">
          <div className="w-14 h-14 rounded-2xl bg-zinc-800/80 border border-white/5 flex items-center justify-center mx-auto text-zinc-500">
            <PackageX className="w-7 h-7" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">No Orders Found</h3>
            <p className="text-xs text-zinc-400 max-w-sm mx-auto mt-1">
              {activeTab !== 'ALL'
                ? `No orders matching the ${activeTab} filter.`
                : 'No order history available yet.'}
            </p>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredOrders.map((order, index) => (
            <OrderCard key={order.id || index} order={order} />
          ))}
        </div>
      )}

    </div>
  );
}
