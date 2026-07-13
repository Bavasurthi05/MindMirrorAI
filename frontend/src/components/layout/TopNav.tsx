import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { cn } from '../../lib/cn';

interface TopNavProps {
  onMenuToggle?: () => void;
}

export function TopNav({ onMenuToggle }: TopNavProps) {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="flex items-center justify-between px-4 py-4 sm:px-6">
        <div className="flex items-center gap-3">
          <button className="rounded-lg p-2 text-slate-700 hover:bg-slate-100 lg:hidden" onClick={onMenuToggle}>
            ☰
          </button>
          <Link to="/dashboard" className="text-lg font-semibold text-slate-900">
            Mental Health Analytics Platform
          </Link>
        </div>

        <div className="flex items-center gap-3">
          <nav className="hidden items-center gap-2 md:flex">
            <NavLink to="/dashboard" className={({ isActive }) => cn('rounded-lg px-3 py-2 text-sm', isActive ? 'bg-indigo-600 text-white' : 'text-slate-700 hover:bg-slate-100')}>
              Dashboard
            </NavLink>
            <NavLink to="/reports" className={({ isActive }) => cn('rounded-lg px-3 py-2 text-sm', isActive ? 'bg-indigo-600 text-white' : 'text-slate-700 hover:bg-slate-100')}>
              Reports
            </NavLink>
          </nav>

          <button
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
          >
            {theme === 'light' ? '🌙' : '☀️'}
          </button>
        </div>
      </div>
    </header>
  );
}
