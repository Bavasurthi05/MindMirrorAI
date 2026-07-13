import { PageShell } from '../components/layout/PageShell';

interface PlaceholderPageProps {
  title: string;
  description: string;
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <PageShell title={title} description={description}>
      <div className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
        <p className="text-sm text-slate-600">Placeholder page for {title.toLowerCase()}.</p>
      </div>
    </PageShell>
  );
}
