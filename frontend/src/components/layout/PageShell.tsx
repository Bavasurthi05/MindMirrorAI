import { type ReactNode } from 'react';
import { Breadcrumbs } from './Breadcrumbs';
import { MobileNav } from './MobileNav';

interface PageShellProps {
  title: string;
  description?: string;
  children: ReactNode;
}

export function PageShell({ title, description, children }: PageShellProps) {
  return (
    <div className="space-y-6 pb-20 md:pb-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
        {description ? <p className="mt-2 text-sm text-slate-600">{description}</p> : null}
      </div>
      <Breadcrumbs />
      {children}
      <MobileNav />
    </div>
  );
}
