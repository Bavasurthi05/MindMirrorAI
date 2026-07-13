import { Link, useLocation } from 'react-router-dom';

export function Breadcrumbs() {
  const location = useLocation();
  const paths = location.pathname.split('/').filter(Boolean);

  return (
    <nav className="mb-4 flex flex-wrap items-center gap-2 text-sm text-slate-600">
      <Link to="/dashboard" className="font-medium text-indigo-600 hover:text-indigo-700">
        Home
      </Link>
      {paths.map((path, index) => {
        const href = '/' + paths.slice(0, index + 1).join('/');
        const label = path.charAt(0).toUpperCase() + path.slice(1);
        return (
          <span key={href} className="flex items-center gap-2">
            <span>/</span>
            <Link to={href} className="hover:text-slate-900">
              {label}
            </Link>
          </span>
        );
      })}
    </nav>
  );
}
