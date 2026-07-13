import { Alert } from './Alert';
import { EmptyState } from './EmptyState';
import { ProgressBar } from './ProgressBar';
import { Spinner } from './Spinner';
import { Skeleton } from './Skeleton';

export function FeedbackDemo() {
  return (
    <div className="space-y-6">
      <Alert title="Info" tone="info">This is an informational message.</Alert>
      <Alert title="Success" tone="success">The action completed successfully.</Alert>
      <Alert title="Warning" tone="warning">Please review your input before continuing.</Alert>
      <Alert title="Error" tone="error">Something went wrong. Please try again.</Alert>

      <div className="flex items-center gap-4 rounded-2xl border border-slate-200 bg-white p-4">
        <Spinner />
        <span className="text-sm text-slate-600">Loading your wellness insights…</span>
      </div>

      <div className="space-y-3 rounded-2xl border border-slate-200 bg-white p-4">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-20 w-full" />
      </div>

      <ProgressBar value={72} label="Weekly progress" />

      <EmptyState
        title="No entries yet"
        description="Start by adding your first journal reflection or questionnaire response."
      />
    </div>
  );
}
