import { useState } from 'react';
import { Cpu, ArrowRight } from 'lucide-react';

export default function AuthForm({ onLoginSuccess }) {
  const [isRegistering, setIsRegistering] = useState(false);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authName, setAuthName] = useState('');
  const [authPhone, setAuthPhone] = useState('');
  const [authError, setAuthError] = useState('');
  const [authSuccess, setAuthSuccess] = useState('');
  const [authLoading, setAuthLoading] = useState(false);

  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    setAuthError('');
    setAuthSuccess('');
    setAuthLoading(true);

    if (isRegistering) {
      if (!authName.trim()) {
        setAuthError('Name is required');
        setAuthLoading(false);
        return;
      }
      if (!authEmail.trim()) {
        setAuthError('Email is required');
        setAuthLoading(false);
        return;
      }
      if (!/^[0-9]{10}$/.test(authPhone.trim())) {
        setAuthError('Phone number must be exactly 10 digits');
        setAuthLoading(false);
        return;
      }
      if (authPassword.length < 6) {
        setAuthError('Password must be at least 6 characters');
        setAuthLoading(false);
        return;
      }

      try {
        const res = await fetch('/api/users/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: authName.trim(),
            email: authEmail.trim(),
            phone: authPhone.trim(),
            passwordHash: authPassword
          })
        });

        if (!res.ok) {
          const errMsg = await res.text();
          throw new Error(errMsg || 'Registration failed');
        }

        setAuthSuccess('Registration successful! You can now log in.');
        setIsRegistering(false);
        setAuthPassword('');
      } catch (err) {
        setAuthError(err.message);
      } finally {
        setAuthLoading(false);
      }
    } else {
      if (!authEmail.trim() || !authPassword) {
        setAuthError('Please fill in all fields');
        setAuthLoading(false);
        return;
      }

      try {
        const res = await fetch('/api/users/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: authEmail.trim(),
            password: authPassword
          })
        });

        if (!res.ok) {
          const errMsg = await res.text();
          throw new Error(errMsg || 'Invalid email or password');
        }

        const data = await res.json();
        onLoginSuccess(data.token, data);
      } catch (err) {
        setAuthError(err.message);
      } finally {
        setAuthLoading(false);
      }
    }
  };

  return (
    <div className="flex min-h-screen w-screen items-center justify-center p-4 bg-[#18181b] select-none">
      <div className="w-full max-w-md bg-[#242427] border border-white/5 rounded-2xl p-8 shadow-xl flex flex-col items-center">
        {/* Logo Header */}
        <div className="flex items-center gap-2 mb-8">
          <Cpu className="w-6 h-6 text-zinc-400" />
          <span className="font-bold text-[1.2rem] tracking-tight text-white">
            E-Commerce Assistant
          </span>
        </div>

        <h2 className="text-[1.2rem] font-bold text-center text-white mb-2">
          {isRegistering ? 'Create your Account' : 'Welcome Back'}
        </h2>
        <p className="text-[0.8rem] text-zinc-400 text-center mb-6">
          {isRegistering ? 'Sign up to access the AI ecommerce assistant' : 'Sign in to access your chat workspace'}
        </p>

        {/* Feedback messages */}
        {authError && (
          <div className="w-full p-3 mb-4 text-[0.8rem] text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl text-left">
            {authError}
          </div>
        )}
        {authSuccess && (
          <div className="w-full p-3 mb-4 text-[0.8rem] text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-left">
            {authSuccess}
          </div>
        )}

        <form onSubmit={handleAuthSubmit} className="w-full flex flex-col gap-4">
          {isRegistering && (
            <>
              {/* Name */}
              <div className="flex flex-col gap-1 text-left">
                <label className="text-[0.75rem] font-medium text-zinc-400">Full Name</label>
                <input 
                  type="text" 
                  value={authName}
                  onChange={(e) => setAuthName(e.target.value)}
                  placeholder="John Doe" 
                  className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
                  required
                />
              </div>

              {/* Phone */}
              <div className="flex flex-col gap-1 text-left">
                <label className="text-[0.75rem] font-medium text-zinc-400">Phone Number</label>
                <input 
                  type="tel" 
                  value={authPhone}
                  onChange={(e) => setAuthPhone(e.target.value)}
                  placeholder="10-digit number" 
                  className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
                  required
                />
              </div>
            </>
          )}

          {/* Email */}
          <div className="flex flex-col gap-1 text-left">
            <label className="text-[0.75rem] font-medium text-zinc-400">Email Address</label>
            <input 
              type="email" 
              value={authEmail}
              onChange={(e) => setAuthEmail(e.target.value)}
              placeholder="name@example.com" 
              className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
              required
            />
          </div>

          {/* Password */}
          <div className="flex flex-col gap-1 text-left">
            <label className="text-[0.75rem] font-medium text-zinc-400">Password</label>
            <input 
              type="password" 
              value={authPassword}
              onChange={(e) => setAuthPassword(e.target.value)}
              placeholder="••••••••" 
              className="w-full px-3 py-2 bg-zinc-800 border border-zinc-700 rounded-xl outline-none text-white text-[0.85rem] focus:border-zinc-500 transition-colors"
              required
            />
          </div>

          {/* Submit Button */}
          <button 
            type="submit" 
            disabled={authLoading}
            className="mt-2 w-full py-2.5 bg-zinc-200 text-black hover:bg-white rounded-xl font-bold text-[0.9rem] flex items-center justify-center gap-2 transition-all duration-200 cursor-pointer disabled:opacity-50"
          >
            {authLoading ? 'Processing...' : isRegistering ? 'Create Account' : 'Sign In'}
            {!authLoading && <ArrowRight className="w-4 h-4" />}
          </button>
        </form>

        {/* Switch Switcher Link */}
        <button 
          onClick={() => {
            setIsRegistering(!isRegistering);
            setAuthError('');
            setAuthSuccess('');
          }}
          className="mt-6 text-[0.8rem] text-zinc-400 hover:text-white transition-colors cursor-pointer"
        >
          {isRegistering ? 'Already have an account? Sign In' : "Don't have an account? Sign Up"}
        </button>
      </div>
    </div>
  );
}
