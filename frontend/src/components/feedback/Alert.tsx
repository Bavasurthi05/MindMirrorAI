import { type ReactNode } from 'react';
import { cn } from '../../lib/cn';

interface AlertProps {
  title?: string;
  children?: ReactNode;
  tone?: 'info' | 'success' | 'warning' | 'error';
  className?: string;
}

const toneClasses = {
  info: 'border-sky-200 bg-sky-50 text-sky-800',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  warning: 'border-amber-200 bg-amber-50 text-amber-800',
  error: 'border-rose-200 bg-rose-50 text-rose-800',
};

export function Alert({ title, children, tone = 'info', className }: AlertProps) {
  return (
    <div className={cn('rounded-2xl border p-4 text-sm', toneClasses[tone], className)}>
      {title ? <div className="mb-1 font-semibold">{title}</div> : null}
      {children ? <div>{children}</div> : null}
    </div>
  );
}
