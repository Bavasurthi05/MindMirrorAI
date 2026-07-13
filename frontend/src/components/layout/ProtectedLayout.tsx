import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Footer } from './Footer';
import { Sidebar } from './Sidebar';
import { TopNav } from './TopNav';

export function ProtectedLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="flex min-h-screen">
        <Sidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
        <div className="flex min-h-screen flex-1 flex-col">
          <TopNav onMenuToggle={() => setIsSidebarOpen((value) => !value)} />
          <main className="flex-1 p-4 sm:p-6 lg:p-8">
            <Outlet />
          </main>
          <Footer />
        </div>
      </div>
    </div>
  );
}
