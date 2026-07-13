import { Outlet } from 'react-router-dom';
import { Footer } from './Footer';

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <main className="px-4 py-10 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-5xl rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
          <Outlet />
        </div>
      </main>
      <Footer />
    </div>
  );
}
