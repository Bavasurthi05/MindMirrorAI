import { NavLink } from 'react-router-dom';
import { cn } from '../../lib/cn';
import { useAuth } from '../../context/AuthContext';

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

const navItems = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Journal', to: '/journal' },
  { label: 'Questionnaire', to: '/questionnaire' },
  { label: 'Social Media', to: '/social-analysis' },
  { label: 'Social Accounts', to: '/social-accounts' },
  { label: 'Mirror', to: '/mirror' },
  { label: 'Analytics', to: '/analytics' },
  { label: 'Insights', to: '/weekly-insights' },
  { label: 'Reports', to: '/reports' },
  { label: 'Goals & Profile', to: '/profile' },
  { label: 'Feedback', to: '/feedback' },
  { label: 'Emergency Help', to: '/help' },
  { label: 'Settings', to: '/settings' },
];

// Admin-only. Previously rendered for everyone, so a regular user clicking "Admin" was
// bounced straight back to the dashboard by AdminLayout — a link that did nothing.
const adminItems = [
  { label: 'Overview', to: '/admin' },
  // The model management, retraining and feedback tools live here; nothing linked to
  // this route before, so the whole page was reachable only by typing the URL.
  { label: 'Models & Feedback', to: '/admin/analytics' },
];

export function Sidebar({ isOpen = true, onClose }: SidebarProps) {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  return (
    <aside
      className={cn(
        'fixed inset-y-0 left-0 z-40 w-72 border-r border-slate-200 bg-white/95 p-6 shadow-sm backdrop-blur transition-transform duration-300 lg:static lg:translate-x-0',
        isOpen ? 'translate-x-0' : '-translate-x-full',
      )}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-indigo-600">Mindscape</p>
          <h2 className="mt-1 text-lg font-semibold text-slate-900">Wellness Portal</h2>
        </div>
        <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden" onClick={onClose}>
          ✕
        </button>
      </div>

      <nav className="mt-8 space-y-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'block rounded-xl px-4 py-3 text-sm font-medium transition',
                isActive ? 'bg-indigo-600 text-white' : 'text-slate-700 hover:bg-slate-100',
              )
            }
            onClick={onClose}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      {isAdmin ? (
      <div className="mt-8 border-t border-slate-200 pt-6">
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-slate-500">Administration</p>
        <div className="mt-3 space-y-2">
          {adminItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              // Without `end`, "/admin" also matches "/admin/analytics" and both
              // links highlight at once.
              end={item.to === '/admin'}
              className={({ isActive }) =>
                cn(
                  'block rounded-xl px-4 py-3 text-sm font-medium transition',
                  isActive ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100',
                )
              }
              onClick={onClose}
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      </div>
      ) : null}
    </aside>
  );
}
