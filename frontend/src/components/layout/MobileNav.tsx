import { NavLink } from 'react-router-dom';
import { cn } from '../../lib/cn';

const items = [
  { label: 'Home', to: '/dashboard' },
  { label: 'Journal', to: '/journal' },
  { label: 'Mirror', to: '/mirror' },
  { label: 'Reports', to: '/reports' },
];

export function MobileNav() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-30 border-t border-slate-200 bg-white/95 backdrop-blur md:hidden">
      <div className="grid grid-cols-4 gap-1 px-2 py-2">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn('rounded-xl px-2 py-2 text-center text-xs font-medium', isActive ? 'bg-indigo-600 text-white' : 'text-slate-700')
            }
          >
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
