import UserProfileCard from './UserProfileCard';
import OrderHistory from './OrderHistory';

export default function ProfileView({ user, token }) {
  return (
    <div className="flex-1 overflow-y-auto bg-[#18181b] min-h-[calc(100vh-4rem)] p-4 sm:p-6 md:p-10 space-y-8 max-w-6xl mx-auto w-full">
      {/* Top Profile Card Header */}
      <section>
        <UserProfileCard user={user} token={token} />
      </section>

      {/* Orders Section (visible on scrolling down) */}
      <section className="space-y-4">
        <OrderHistory token={token} />
      </section>
    </div>
  );
}
