import { useState, useEffect, useRef } from 'react';
import { CreditCard, CheckCircle2, AlertCircle, Loader2, ShieldCheck } from 'lucide-react';

export default function RazorpayCheckoutButton({ orderId, razorpayOrderId, amount, autoOpen = false }) {
  const [status, setStatus] = useState('IDLE'); // IDLE | LOADING | VERIFYING | SUCCESS | FAILED
  const [errorMsg, setErrorMsg] = useState('');
  const [paymentDetails, setPaymentDetails] = useState(null);
  const autoOpenedRef = useRef(false);

  const loadRazorpayScript = () => {
    return new Promise((resolve) => {
      if (window.Razorpay) {
        resolve(true);
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  const reportFailureToBackend = async (reason) => {
    try {
      const token = localStorage.getItem('auth_token');
      const headers = { 'Content-Type': 'application/json' };
      if (token) headers['Authorization'] = `Bearer ${token}`;
      await fetch('/api/payment/fail', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          orderId: Number(orderId),
          errorMessage: reason || 'Payment transaction failed or closed without completion'
        })
      });
    } catch (e) {
      console.error('Failed to report payment failure to backend:', e);
    }
  };

  const handlePay = async () => {
    setStatus('LOADING');
    setErrorMsg('');

    try {
      const isLoaded = await loadRazorpayScript();
      if (!isLoaded) {
        throw new Error('Failed to load Razorpay SDK. Please check your network connection.');
      }

      // Fetch Razorpay Key ID from backend
      const token = localStorage.getItem('auth_token');
      const headers = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const configRes = await fetch('/api/payment/config', { headers });
      let keyId = 'rzp_test_TUUowhWnpaMALb'; // Default fallback matching test key
      if (configRes.ok) {
        const configData = await configRes.json();
        if (configData.keyId) keyId = configData.keyId;
      }

      const parsedAmount = parseFloat(amount) || 0;
      const amountInPaise = Math.round(parsedAmount * 100);
      const formattedAmountStr = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR'
      }).format(parsedAmount);

      const options = {
        key: keyId,
        amount: amountInPaise,
        currency: 'INR',
        name: 'Agentic E-Commerce Platform',
        description: `Payment for Order #${orderId}`,
        order_id: razorpayOrderId,
        handler: async function (response) {
          setStatus('VERIFYING');
          try {
            const verifyToken = localStorage.getItem('auth_token');
            const verifyHeaders = { 'Content-Type': 'application/json' };
            if (verifyToken) {
              verifyHeaders['Authorization'] = `Bearer ${verifyToken}`;
            }

            const verifyRes = await fetch('/api/payment/verify', {
              method: 'POST',
              headers: verifyHeaders,
              body: JSON.stringify({
                orderId: Number(orderId),
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature
              })
            });

            const verifyData = await verifyRes.json();
            if (verifyRes.ok && verifyData.status === 'SUCCESS') {
              setStatus('SUCCESS');
              setPaymentDetails({
                paymentId: response.razorpay_payment_id,
                orderId: response.razorpay_order_id
              });

              // Dispatch success event for chat notification
              window.dispatchEvent(
                new CustomEvent('razorpay-payment-success', {
                  detail: {
                    orderId: Number(orderId),
                    razorpayPaymentId: response.razorpay_payment_id,
                    razorpayOrderId: response.razorpay_order_id,
                    amount: formattedAmountStr
                  }
                })
              );
            } else {
              const failReason = verifyData.message || 'Payment signature verification failed.';
              setStatus('FAILED');
              setErrorMsg(failReason);
              await reportFailureToBackend(failReason);
              window.dispatchEvent(
                new CustomEvent('razorpay-payment-failed', {
                  detail: { orderId: Number(orderId), errorMessage: failReason }
                })
              );
            }
          } catch (err) {
            const failReason = err.message || 'Error verifying payment with server.';
            setStatus('FAILED');
            setErrorMsg(failReason);
            await reportFailureToBackend(failReason);
            window.dispatchEvent(
              new CustomEvent('razorpay-payment-failed', {
                detail: { orderId: Number(orderId), errorMessage: failReason }
              })
            );
          }
        },
        modal: {
          ondismiss: function () {
            if (status !== 'SUCCESS') {
              setStatus('IDLE');
            }
          }
        },
        theme: {
          color: '#6366f1'
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', async function (response) {
        const failDesc = response.error?.description || 'Razorpay payment transaction failed.';
        setStatus('FAILED');
        setErrorMsg(failDesc);
        await reportFailureToBackend(failDesc);
        window.dispatchEvent(
          new CustomEvent('razorpay-payment-failed', {
            detail: { orderId: Number(orderId), errorMessage: failDesc }
          })
        );
      });

      rzp.open();
    } catch (err) {
      const failDesc = err.message || 'Unable to launch Razorpay checkout.';
      setStatus('FAILED');
      setErrorMsg(failDesc);
      await reportFailureToBackend(failDesc);
      window.dispatchEvent(
        new CustomEvent('razorpay-payment-failed', {
          detail: { orderId: Number(orderId), errorMessage: failDesc }
        })
      );
    }
  };

  useEffect(() => {
    if (autoOpen && !autoOpenedRef.current && status === 'IDLE') {
      autoOpenedRef.current = true;
      const timer = setTimeout(() => {
        handlePay();
      }, 400);
      return () => clearTimeout(timer);
    }
  }, [autoOpen]);

  const formattedAmount = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR'
  }).format(parseFloat(amount) || 0);

  if (status === 'SUCCESS') {
    return (
      <div className="w-full max-w-md bg-emerald-950/40 border border-emerald-500/30 rounded-xl p-4 my-3 text-left animate-fadeIn">
        <div className="flex items-center gap-2 text-emerald-400 font-semibold text-[0.95rem] mb-1">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <span>Payment Successful!</span>
        </div>
        <p className="text-xs text-emerald-200/80 leading-relaxed">
          Order <strong>#{orderId}</strong> has been confirmed and sent for processing.
        </p>
      </div>
    );
  }

  return (
    <div className="w-full max-w-md bg-zinc-900/80 border border-indigo-500/30 rounded-xl p-4 my-3 text-left shadow-lg backdrop-blur-sm">
      <div className="flex items-center justify-between gap-3 mb-2">
        <div className="flex items-center gap-2">
          <CreditCard className="w-4 h-4 text-indigo-400" />
          <span className="font-bold text-white text-[0.9rem]">Razorpay Secure Checkout</span>
        </div>
        <span className="text-[0.72rem] font-semibold uppercase tracking-wider text-indigo-300 bg-indigo-500/20 px-2 py-0.5 rounded border border-indigo-500/30 flex items-center gap-1">
          <ShieldCheck className="w-3 h-3 text-indigo-400" />
          Test Mode
        </span>
      </div>

      <div className="text-xs text-zinc-400 mb-3 space-y-1">
        <div className="flex justify-between">
          <span>Order ID:</span>
          <span className="font-semibold text-white">#{orderId}</span>
        </div>
        <div className="flex justify-between">
          <span>Total Amount:</span>
          <span className="font-bold text-emerald-400">{formattedAmount}</span>
        </div>
      </div>

      {status === 'FAILED' && (
        <div className="flex items-start gap-2 bg-red-950/40 border border-red-500/30 text-red-300 text-xs p-2.5 rounded-lg mb-3">
          <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
          <span>{errorMsg || 'Payment attempt failed. You can try again below.'}</span>
        </div>
      )}

      <button
        onClick={handlePay}
        disabled={status === 'LOADING' || status === 'VERIFYING'}
        className="w-full py-2.5 px-4 bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold text-sm rounded-lg shadow-md hover:shadow-indigo-500/25 transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {status === 'LOADING' || status === 'VERIFYING' ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin text-white" />
            <span>{status === 'VERIFYING' ? 'Verifying HMAC Signature...' : 'Opening Checkout...'}</span>
          </>
        ) : (
          <>
            <CreditCard className="w-4 h-4 text-white" />
            <span>Pay {formattedAmount} via Razorpay</span>
          </>
        )}
      </button>

      <div className="mt-2 text-center text-[0.68rem] text-zinc-500">
        Supports UPI, Cards, NetBanking, Wallets (Razorpay Test API)
      </div>
    </div>
  );
}
