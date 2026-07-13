import { type InputHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

interface RadioProps extends InputHTMLAttributes<HTMLInputElement> {
  options: Array<{ label: string; value: string }>;
  name: string;
}

export function Radio({ options, name, className, ...props }: RadioProps) {
  return (
    <div className="space-y-2">
      {options.map((option) => (
        <label key={option.value} className="flex items-center gap-3 text-sm text-slate-700">
          <input
            type="radio"
            name={name}
            value={option.value}
            className={cn('h-4 w-4 border-slate-300 text-indigo-600 focus:ring-indigo-500', className)}
            {...props}
          />
          <span>{option.label}</span>
        </label>
      ))}
    </div>
  );
}
