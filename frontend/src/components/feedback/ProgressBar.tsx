import { cn } from '../../lib/cn';

interface ProgressBarProps {
  value: number;
  max?: number;
  className?: string;
  label?: string;
}

export function ProgressBar({ value, max = 100, className, label }: ProgressBarProps) {
  const percent = Math.min(100, Math.max(0, (value / max) * 100));

  return (
    <div className={cn('w-full', className)}>
      {label ? <div className="mb-2 text-sm font-medium text-slate-700">{label}</div> : null}
      <div className="h-2.5 w-full rounded-full bg-slate-200">
        <div className="h-2.5 rounded-full bg-indigo-600 transition-all" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}
