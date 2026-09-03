import { useState, useEffect } from 'react';
import { User, Mail, Phone, ShieldCheck, Calendar, CheckCircle2, Clock } from 'lucide-react';

export default function UserProfileCard({ user, token }) {
  const [profileDetails, setProfileDetails] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user?.email && token) {
      setLoading(true);
      fetch(`/api/users/email/${encodeURIComponent(user.email)}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })
        .then((res) => (res.ok ? res.json() : null))
        .then((data) => {
          if (data) {
            setProfileDetails(data);
          }
        })
        .catch(() => {
          // fallback to auth user if fetch fails
        })
        .finally(() => setLoading(false));
    }
  }, [user, token]);

  const displayName = profileDetails?.name || user?.name || user?.email?.split('@')[0] || 'User Profile';
  const email = profileDetails?.email || user?.email || 'N/A';
  const phone = profileDetails?.phone || user?.phone || 'Not provided';
  const isVerified = profileDetails?.isVerified ?? true;
  const role = user?.role === 'ROLE_USER' ? 'Customer Account' : 'Merchant Account';
  const joinedDate = profileDetails?.createdAt 
    ? new Date(profileDetails.createdAt).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
    : 'Recent Member';

  return (
    <div className="bg-gradient-to-r from-zinc-900 via-[#18181b] to-zinc-900 border border-white/10 rounded-2xl p-6 md:p-8 shadow-xl relative overflow-hidden">
      {/* Decorative ambient background glow */}
      <div className="absolute -top-24 -right-24 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-24 -left-24 w-64 h-64 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative z-10">
        
        {/* Left Column: Avatar & Main Info */}
        <div className="flex items-center gap-5">
          <div className="relative">
            <div className="w-20 h-20 md:w-24 md:h-24 rounded-2xl bg-gradient-to-br from-indigo-500 via-purple-600 to-pink-500 p-1 shadow-lg shadow-indigo-500/20">
              <div className="w-full h-full bg-zinc-900 rounded-[14px] flex items-center justify-center text-2xl font-black text-white uppercase">
                {displayName.substring(0, 2)}
              </div>
            </div>
            {isVerified && (
              <div className="absolute -bottom-1 -right-1 bg-emerald-500 text-black p-1 rounded-full border-2 border-[#18181b]" title="Verified User">
                <CheckCircle2 className="w-4 h-4 text-zinc-950 fill-emerald-400" />
              </div>
            )}
          </div>

          <div className="flex flex-col">
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight">
                {displayName}
              </h1>
              <span className="px-2.5 py-0.5 rounded-full text-[0.7rem] font-semibold bg-indigo-500/15 border border-indigo-500/30 text-indigo-300">
                {role}
              </span>
            </div>

            <div className="mt-2 flex flex-col sm:flex-row sm:items-center gap-3 text-[0.85rem] text-zinc-400">
              <div className="flex items-center gap-1.5">
                <Mail className="w-4 h-4 text-indigo-400" />
                <span>{email}</span>
              </div>
              <div className="hidden sm:block text-zinc-600">•</div>
              <div className="flex items-center gap-1.5">
                <Phone className="w-4 h-4 text-indigo-400" />
                <span>{phone}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Account Badges & Status */}
        <div className="flex flex-wrap md:flex-col gap-2.5 self-stretch md:self-auto justify-end border-t md:border-t-0 md:border-l border-white/10 pt-4 md:pt-0 md:pl-6">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-zinc-800/60 border border-white/5 text-[0.8rem] text-zinc-300">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Status: <strong className="text-emerald-400 font-semibold">Active & Verified</strong></span>
          </div>

          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-zinc-800/60 border border-white/5 text-[0.8rem] text-zinc-300">
            <Calendar className="w-4 h-4 text-indigo-400" />
            <span>Member since: <strong className="text-white font-medium">{joinedDate}</strong></span>
          </div>
        </div>

      </div>
    </div>
  );
}
