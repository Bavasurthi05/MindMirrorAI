import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

interface SwitchProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
}

export const Switch = forwardRef<HTMLInputElement, SwitchProps>(
  ({ className, label, ...props }, ref) => {
    return (
      <label className="flex items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
        {label ? <span className="font-medium">{label}</span> : null}
        <input
          ref={ref}
          type="checkbox"
          className={cn('peer sr-only', className)}
          {...props}
        />
        <span className="relative h-6 w-11 rounded-full bg-slate-300 transition peer-checked:bg-indigo-600">
          <span className="absolute left-1 top-1 h-4 w-4 rounded-full bg-white transition peer-checked:translate-x-5" />
        </span>
      </label>
    );
  },
);

Switch.displayName = 'Switch';
